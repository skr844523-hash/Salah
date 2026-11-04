package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.sqrt

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED
}

class VoiceRecorder(
    val sampleRate: Int = 44100
) {
    private val TAG = "VoiceRecorder"
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    private val isRunning = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    private val _state = MutableStateFlow(RecordingState.IDLE)
    val state = _state.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude = _amplitude.asStateFlow()

    private val _waveformPoints = MutableStateFlow(FloatArray(32))
    val waveformPoints = _waveformPoints.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs = _durationMs.asStateFlow()

    private val recordedChunks = mutableListOf<ShortArray>()
    private var recordingStartTime = 0L
    private var accumulatedDuration = 0L

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (isRunning.get()) return true

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBufferSize <= 0) {
                Log.e(TAG, "Invalid buffer size")
                return false
            }

            val bufferSize = minBufferSize.coerceAtLeast(2048)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            synchronized(recordedChunks) {
                recordedChunks.clear()
            }

            audioRecord?.startRecording()
            isRunning.set(true)
            isPaused.set(false)
            _state.value = RecordingState.RECORDING

            recordingStartTime = System.currentTimeMillis()
            accumulatedDuration = 0L

            timerJob = coroutineScope.launch {
                while (isActive && isRunning.get()) {
                    if (!isPaused.get()) {
                        val currentElapsed = System.currentTimeMillis() - recordingStartTime + accumulatedDuration
                        _durationMs.value = currentElapsed
                    }
                    delay(50)
                }
            }

            recordingThread = Thread {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                val buffer = ShortArray(bufferSize / 2)
                val points = FloatArray(32)

                while (isRunning.get()) {
                    if (isPaused.get()) {
                        try {
                            Thread.sleep(20)
                        } catch (e: InterruptedException) {
                            break
                        }
                        continue
                    }

                    val record = audioRecord ?: break
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        synchronized(recordedChunks) {
                            recordedChunks.add(buffer.copyOfRange(0, read))
                        }

                        // Compute Amplitude and Waveform
                        var sumSquare = 0.0
                        val step = (read / 32).coerceAtLeast(1)
                        for (w in 0 until 32) {
                            val idx = (w * step).coerceAtMost(read - 1)
                            points[w] = buffer[idx].toFloat() / 32768.0f
                        }
                        _waveformPoints.value = points.copyOf()

                        for (s in 0 until read) {
                            val norm = buffer[s].toFloat() / 32768.0f
                            sumSquare += (norm * norm)
                        }
                        val rms = sqrt(sumSquare / read).toFloat()
                        _amplitude.value = (rms * 2.5f).coerceIn(0f, 1f)
                    }
                }
            }.apply {
                name = "VoiceRecorderThread"
                start()
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recorder", e)
            stop()
            return false
        }
    }

    fun pause() {
        if (isRunning.get() && !isPaused.get()) {
            isPaused.set(true)
            accumulatedDuration += System.currentTimeMillis() - recordingStartTime
            _state.value = RecordingState.PAUSED
        }
    }

    fun resume() {
        if (isRunning.get() && isPaused.get()) {
            recordingStartTime = System.currentTimeMillis()
            isPaused.set(false)
            _state.value = RecordingState.RECORDING
        }
    }

    fun stop(): ShortArray {
        isRunning.set(false)
        isPaused.set(false)
        _state.value = RecordingState.STOPPED
        timerJob?.cancel()

        try {
            recordingThread?.interrupt()
            recordingThread = null

            audioRecord?.apply {
                if (state == AudioRecord.STATE_INITIALIZED) {
                    stop()
                }
                release()
            }
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder", e)
        }

        // Combine chunks
        val combined: ShortArray
        synchronized(recordedChunks) {
            val totalSize = recordedChunks.sumOf { it.size }
            combined = ShortArray(totalSize)
            var destPos = 0
            for (chunk in recordedChunks) {
                System.arraycopy(chunk, 0, combined, destPos, chunk.size)
                destPos += chunk.size
            }
            recordedChunks.clear()
        }

        return combined
    }

    fun reset() {
        stop()
        _state.value = RecordingState.IDLE
        _durationMs.value = 0L
        _amplitude.value = 0f
        _waveformPoints.value = FloatArray(32)
    }

    /**
     * Verifies if recorded PCM has real sound energy (not dead silence).
     */
    fun hasAudibleSound(pcm: ShortArray, threshold: Float = 0.005f): Boolean {
        if (pcm.isEmpty()) return false
        var peak = 0
        for (sample in pcm) {
            val a = abs(sample.toInt())
            if (a > peak) peak = a
        }
        val normalizedPeak = peak.toFloat() / 32768.0f
        return normalizedPeak >= threshold
    }
}
