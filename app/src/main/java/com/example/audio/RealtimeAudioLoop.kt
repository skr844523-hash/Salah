package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class RealtimeAudioLoop(
    private val context: Context,
    val dspEngine: VoiceDspEngine = VoiceDspEngine(44100)
) {
    private val TAG = "RealtimeAudioLoop"

    val sampleRate = 44100
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var workerThread: Thread? = null

    private val isRunning = AtomicBoolean(false)
    val isProcessingFlow = MutableStateFlow(false)

    // UI telemetry
    private val _rmsLevelFlow = MutableStateFlow(0f)
    val rmsLevelFlow = _rmsLevelFlow.asStateFlow()

    private val _waveformFlow = MutableStateFlow(FloatArray(32))
    val waveformFlow = _waveformFlow.asStateFlow()

    // Recording transformed clip for Soundboard / WhatsApp / Discord
    private val isRecordingClip = AtomicBoolean(false)
    private val recordedChunks = mutableListOf<ShortArray>()
    private val _isRecordingClipFlow = MutableStateFlow(false)
    val isRecordingClipFlow = _isRecordingClipFlow.asStateFlow()

    @SuppressLint("MissingPermission")
    @Synchronized
    fun start(): Boolean {
        if (isRunning.get()) return true

        try {
            val minRecordBufferSize = AudioRecord.getMinBufferSize(
                sampleRate, channelConfigIn, audioFormat
            )
            val minTrackBufferSize = AudioTrack.getMinBufferSize(
                sampleRate, channelConfigOut, audioFormat
            )

            if (minRecordBufferSize <= 0 || minTrackBufferSize <= 0) {
                Log.e(TAG, "Invalid buffer sizes for sampleRate $sampleRate")
                return false
            }

            val bufferSize = minRecordBufferSize.coerceAtLeast(1024)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfigIn,
                audioFormat,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfigIn,
                    audioFormat,
                    bufferSize * 2
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val audioFormatSpec = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfigOut)
                .setEncoding(audioFormat)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormatSpec)
                .setBufferSizeInBytes(minTrackBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()

            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "AudioTrack failed to initialize")
                audioRecord?.release()
                audioTrack?.release()
                audioRecord = null
                audioTrack = null
                return false
            }

            dspEngine.resetBuffers()
            audioRecord?.startRecording()
            audioTrack?.play()
            isRunning.set(true)
            isProcessingFlow.value = true

            workerThread = Thread {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                val buffer = ShortArray(bufferSize / 2)
                val waveformPoints = FloatArray(32)

                while (isRunning.get()) {
                    val record = audioRecord ?: break
                    val track = audioTrack ?: break

                    val readShorts = record.read(buffer, 0, buffer.size)
                    if (readShorts > 0) {
                        // Apply Realtime DSP algorithms
                        val rms = dspEngine.process(buffer, readShorts)
                        _rmsLevelFlow.value = rms

                        val step = (readShorts / 32).coerceAtLeast(1)
                        for (w in 0 until 32) {
                            val sampleIndex = (w * step).coerceAtMost(readShorts - 1)
                            waveformPoints[w] = (buffer[sampleIndex].toFloat() / 32768.0f)
                        }
                        _waveformFlow.value = waveformPoints.copyOf()

                        // Record clip if requested
                        if (isRecordingClip.get()) {
                            synchronized(recordedChunks) {
                                recordedChunks.add(buffer.copyOfRange(0, readShorts))
                            }
                        }

                        track.write(buffer, 0, readShorts)
                    }
                }
            }.apply {
                name = "GamingVoiceDspThread"
                start()
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting RealtimeAudioLoop", e)
            stop()
            return false
        }
    }

    @Synchronized
    fun stop() {
        isRunning.set(false)
        isProcessingFlow.value = false
        _rmsLevelFlow.value = 0f
        _waveformFlow.value = FloatArray(32)

        try {
            workerThread?.interrupt()
            workerThread = null

            audioRecord?.apply {
                if (state == AudioRecord.STATE_INITIALIZED) {
                    stop()
                }
                release()
            }
            audioRecord = null

            audioTrack?.apply {
                if (state == AudioTrack.STATE_INITIALIZED) {
                    stop()
                }
                release()
            }
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping RealtimeAudioLoop", e)
        }
    }

    fun startRecordingClip() {
        synchronized(recordedChunks) {
            recordedChunks.clear()
        }
        isRecordingClip.set(true)
        _isRecordingClipFlow.value = true

        // Ensure loop is actively recording
        if (!isRunning.get()) {
            start()
        }
    }

    fun stopAndSaveClip(outputFile: File): Boolean {
        isRecordingClip.set(false)
        _isRecordingClipFlow.value = false

        val combined: ShortArray
        synchronized(recordedChunks) {
            val totalShorts = recordedChunks.sumOf { it.size }
            if (totalShorts == 0) return false
            combined = ShortArray(totalShorts)
            var pos = 0
            for (chunk in recordedChunks) {
                System.arraycopy(chunk, 0, combined, pos, chunk.size)
                pos += chunk.size
            }
            recordedChunks.clear()
        }

        if (combined.isEmpty()) return false

        // Encode to M4A AAC (WhatsApp & Discord voice message standard)
        val m4aSuccess = AudioEncoder.encodePcmToM4a(combined, outputFile, sampleRate, 1)
        if (m4aSuccess) return true

        // Fallback to WAV
        return AudioEncoder.savePcmToWav(combined, outputFile, sampleRate, 1)
    }

    fun isRunning(): Boolean = isRunning.get()
}
