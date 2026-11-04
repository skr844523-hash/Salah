package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayerManager(private val context: Context) {
    private val TAG = "AudioPlayerManager"

    private var mediaPlayer: MediaPlayer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs = _currentPositionMs.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(0L)
    val totalDurationMs = _totalDurationMs.asStateFlow()

    private var currentPlayingFile: File? = null

    fun play(file: File, onCompletion: (() -> Unit)? = null): Boolean {
        if (!file.exists() || file.length() == 0L) {
            Log.e(TAG, "Audio file does not exist or is empty: ${file.absolutePath}")
            return false
        }

        try {
            stop()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.fromFile(file))
                prepare()
                start()

                _isPlaying.value = true
                _totalDurationMs.value = duration.toLong().coerceAtLeast(0L)
                currentPlayingFile = file

                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0L
                    progressJob?.cancel()
                    onCompletion?.invoke()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    progressJob?.cancel()
                    true
                }
            }

            startProgressTracker()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file", e)
            stop()
            return false
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing playback", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                    _isPlaying.value = true
                    startProgressTracker()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming playback", e)
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.let {
                val clamped = positionMs.coerceIn(0L, _totalDurationMs.value)
                it.seekTo(clamped.toInt())
                _currentPositionMs.value = clamped
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking playback", e)
        }
    }

    fun stop() {
        try {
            progressJob?.cancel()
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            _isPlaying.value = false
            _currentPositionMs.value = 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping player", e)
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let {
                    try {
                        if (it.isPlaying) {
                            _currentPositionMs.value = it.currentPosition.toLong()
                        }
                    } catch (ignored: Exception) {}
                }
                delay(60)
            }
        }
    }

    fun release() {
        stop()
    }
}
