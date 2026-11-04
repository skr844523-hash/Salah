package com.example.audio

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavHelper {
    /**
     * Writes 16-bit PCM raw samples to a valid RIFF/WAV audio file.
     */
    fun savePcmToWav(
        pcmData: List<ShortArray>,
        outputFile: File,
        sampleRate: Int = 44100,
        channels: Short = 1
    ): Boolean {
        return try {
            val totalShorts = pcmData.sumOf { it.size }
            val totalAudioBytes = totalShorts * 2
            val totalDataLen = totalAudioBytes + 36

            FileOutputStream(outputFile).use { fos ->
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

                // RIFF chunk descriptor
                header.put("RIFF".toByteArray())
                header.putInt(totalDataLen)
                header.put("WAVE".toByteArray())

                // fmt sub-chunk
                header.put("fmt ".toByteArray())
                header.putInt(16) // SubChunk1Size (16 for PCM)
                header.putShort(1) // AudioFormat (1 for PCM)
                header.putShort(channels)
                header.putInt(sampleRate)
                header.putInt(sampleRate * channels * 2) // ByteRate
                header.putShort((channels * 2).toShort()) // BlockAlign
                header.putShort(16) // BitsPerSample

                // data sub-chunk
                header.put("data".toByteArray())
                header.putInt(totalAudioBytes)

                fos.write(header.array())

                val sampleByteBuffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN)
                for (chunk in pcmData) {
                    for (sample in chunk) {
                        if (!sampleByteBuffer.hasRemaining()) {
                            fos.write(sampleByteBuffer.array())
                            sampleByteBuffer.clear()
                        }
                        sampleByteBuffer.putShort(sample)
                    }
                }
                if (sampleByteBuffer.position() > 0) {
                    fos.write(sampleByteBuffer.array(), 0, sampleByteBuffer.position())
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
