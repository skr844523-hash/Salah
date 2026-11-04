package com.example

import com.example.audio.PresetType
import com.example.audio.VoiceDspEngine
import com.example.audio.VoicePreset
import com.example.audio.WavHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExampleUnitTest {

    @Test
    fun testAllSixPresetsDefined() {
        assertEquals(6, VoicePreset.ALL_PRESETS.size)

        val presetIds = VoicePreset.ALL_PRESETS.map { it.id }.toSet()
        assertTrue(presetIds.contains(PresetType.GIRL))
        assertTrue(presetIds.contains(PresetType.ROBOT))
        assertTrue(presetIds.contains(PresetType.DEEP))
        assertTrue(presetIds.contains(PresetType.CHILD))
        assertTrue(presetIds.contains(PresetType.MONSTER))
        assertTrue(presetIds.contains(PresetType.FUNNY))
    }

    @Test
    fun testDspEngineProcessesAudio() {
        val engine = VoiceDspEngine(44100)
        engine.applyPreset(VoicePreset.getPreset(PresetType.ROBOT))

        val buffer = ShortArray(512) { (it * 30).toShort() }
        val rms = engine.process(buffer, buffer.size)

        // RMS should be computed and in valid range
        assertTrue(rms >= 0f)
        assertTrue(rms <= 1f)
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
    fun testWavHelperHeaderGeneration() {
        val tempFile = File.createTempFile("test_audio", ".wav")
        try {
            val pcmData = listOf(ShortArray(100) { 500 })
            val success = WavHelper.savePcmToWav(pcmData, tempFile, 44100, 1)
            assertTrue(success)
            assertTrue(tempFile.exists())
            // 44 bytes header + 200 bytes data = 244 bytes
            assertEquals(244L, tempFile.length())
        } finally {
            tempFile.delete()
        }
    }
}
