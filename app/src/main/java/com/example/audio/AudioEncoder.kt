package com.example.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioEncoder {
    private const val TAG = "AudioEncoder"

    /**
     * Encodes 16-bit PCM raw samples to a standard AAC M4A audio file using MediaCodec & MediaMuxer.
     * Compatible with WhatsApp, Discord, Telegram, and native Android media players.
     */
    fun encodePcmToM4a(
        pcmData: ShortArray,
        outputFile: File,
        sampleRate: Int = 44100,
        channels: Int = 1,
        bitRate: Int = 128000
    ): Boolean {
        if (pcmData.isEmpty()) {
            Log.e(TAG, "PCM data is empty, cannot encode")
            return false
        }

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        try {
            val mimeType = MediaFormat.MIMETYPE_AUDIO_AAC
            val format = MediaFormat.createAudioFormat(mimeType, sampleRate, channels).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            codec = MediaCodec.createEncoderByType(mimeType)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var audioTrackIndex = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcmData) {
                byteBuffer.putShort(s)
            }
            byteBuffer.flip()

            var inputEos = false
            var outputEos = false
            val timeoutUs = 10000L

            while (!outputEos) {
                if (!inputEos) {
                    val inputBufferIndex = codec.dequeueInputBuffer(timeoutUs)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            val bytesToCopy = minOf(inputBuffer.remaining(), byteBuffer.remaining())
                            if (bytesToCopy > 0) {
                                val tempArray = ByteArray(bytesToCopy)
                                byteBuffer.get(tempArray)
                                inputBuffer.put(tempArray)
                                codec.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    bytesToCopy,
                                    System.nanoTime() / 1000,
                                    0
                                )
                            } else {
                                inputEos = true
                                codec.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    0,
                                    System.nanoTime() / 1000,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                            }
                        }
                    }
                }

                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                when {
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (muxerStarted) {
                            throw RuntimeException("Format changed after muxer started")
                        }
                        val newFormat = codec.outputFormat
                        audioTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outputBufferIndex >= 0 -> {
                        val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                        if (outputBuffer != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            if (bufferInfo.size > 0 && muxerStarted) {
                                outputBuffer.position(bufferInfo.offset)
                                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                            }
                        }
                        codec.releaseOutputBuffer(outputBufferIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputEos = true
                        }
                    }
                }
            }

            return outputFile.exists() && outputFile.length() > 100
        } catch (e: Exception) {
            Log.e(TAG, "M4A AAC encoding failed, falling back to WAV", e)
            return false
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (ignored: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Saves 16-bit PCM samples to a RIFF/WAV format file with proper 44-byte headers.
     */
    fun savePcmToWav(
        pcmData: ShortArray,
        outputFile: File,
        sampleRate: Int = 44100,
        channels: Short = 1
    ): Boolean {
        if (pcmData.isEmpty()) return false

        return try {
            val totalShorts = pcmData.size
            val totalAudioBytes = totalShorts * 2
            val totalDataLen = totalAudioBytes + 36

            FileOutputStream(outputFile).use { fos ->
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

                // RIFF descriptor
                header.put("RIFF".toByteArray())
                header.putInt(totalDataLen)
                header.put("WAVE".toByteArray())

                // fmt chunk
                header.put("fmt ".toByteArray())
                header.putInt(16) // SubChunk1Size (16 for PCM)
                header.putShort(1) // AudioFormat (1 for PCM)
                header.putShort(channels)
                header.putInt(sampleRate)
                header.putInt(sampleRate * channels * 2) // ByteRate
                header.putShort((channels * 2).toShort()) // BlockAlign
                header.putShort(16) // BitsPerSample

                // data chunk
                header.put("data".toByteArray())
                header.putInt(totalAudioBytes)

                fos.write(header.array())

                val sampleByteBuffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
                for (sample in pcmData) {
                    if (!sampleByteBuffer.hasRemaining()) {
                        fos.write(sampleByteBuffer.array())
                        sampleByteBuffer.clear()
                    }
                    sampleByteBuffer.putShort(sample)
                }
                if (sampleByteBuffer.position() > 0) {
                    fos.write(sampleByteBuffer.array(), 0, sampleByteBuffer.position())
                }
            }
            outputFile.exists() && outputFile.length() > 44
        } catch (e: Exception) {
            Log.e(TAG, "Error writing WAV file", e)
            false
        }
    }
}
