package com.example

import com.example.audio.AudioEncoder
import com.example.audio.PresetType
import com.example.audio.VoiceDspEngine
import com.example.audio.VoicePreset
import com.example.audio.VoiceRecorder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.sin

class ExampleUnitTest {

    @Test
    fun testAllTenPresetsDefined() {
        val presets = VoicePreset.allPresets
        assertEquals(10, presets.size)

        val presetIds = presets.map { it.id }.toSet()
        assertTrue(presetIds.contains(PresetType.DEEP_MALE))
        assertTrue(presetIds.contains(PresetType.CHILD))
        assertTrue(presetIds.contains(PresetType.ROBOT))
        assertTrue(presetIds.contains(PresetType.ALIEN))
        assertTrue(presetIds.contains(PresetType.DEMON))
        assertTrue(presetIds.contains(PresetType.FEMALE))
        assertTrue(presetIds.contains(PresetType.CARTOON))
        assertTrue(presetIds.contains(PresetType.BROADCASTER))
        assertTrue(presetIds.contains(PresetType.GAMER_PRO))
        assertTrue(presetIds.contains(PresetType.FUNNY))
    }

    @Test
    fun testVoiceRecorderAudibleCheck() {
        val recorder = VoiceRecorder(44100)

        // Empty or dead silent buffer should return false
        val silentBuffer = ShortArray(1000) { 0 }
        assertFalse(recorder.hasAudibleSound(silentBuffer))

        // Buffer with genuine speech/sine wave signal should return true
        val audibleBuffer = ShortArray(1000) { i ->
            (sin(i * 0.1) * 8000).toInt().toShort()
        }
        assertTrue(recorder.hasAudibleSound(audibleBuffer))
    }

    @Test
    fun testDspEngineBufferProcessing() {
        val engine = VoiceDspEngine(44100)
        engine.applyPreset(VoicePreset.getPreset(PresetType.DEEP_MALE))

        val input = ShortArray(2048) { (sin(it * 0.05) * 10000).toInt().toShort() }
        val output = engine.processEntireBuffer(input)

        assertNotNull(output)
        assertTrue(output.isNotEmpty())
        val expectedLength = (input.size / engine.speedFactor).toInt()
        assertEquals(expectedLength, output.size)

        // Make sure it wasn't zeroed out and was actually processed
        var hasSignal = false
        for (s in output) {
            if (s.toInt() != 0) {
                hasSignal = true
                break
            }
        }
        assertTrue(hasSignal)
    }

    @Test
    fun testDspEngineMuteZeroesBuffer() {
        val engine = VoiceDspEngine(44100)
        engine.isMuted = true

        val buffer = ShortArray(256) { 1000.toShort() }
        val rms = engine.process(buffer, buffer.size)

        assertEquals(0f, rms, 0.0001f)
        for (sample in buffer) {
            assertEquals(0.toShort(), sample)
        }
    }

    @Test
    fun testAudioEncoderWavExport() {
        val tempFile = File.createTempFile("test_audio", ".wav")
        try {
            val pcmData = ShortArray(100) { 500 }
            val success = AudioEncoder.savePcmToWav(pcmData, tempFile, 44100, 1)
            assertTrue(success)
            assertTrue(tempFile.exists())
            // 44 bytes header + 200 bytes data (100 * 2 bytes/sample) = 244 bytes
            assertEquals(244L, tempFile.length())
        } finally {
            tempFile.delete()
        }
    }
}
