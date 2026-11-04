package com.example.audio

import com.example.R

enum class PresetType {
    GIRL,
    ROBOT,
    DEEP,
    CHILD,
    MONSTER,
    FUNNY,
    CUSTOM
}

data class VoicePreset(
    val id: PresetType,
    val titleRes: Int,
    val descRes: Int,
    val pitchFactor: Float = 1.0f,      // 0.5f to 2.0f (1.0 = original)
    val robotFreq: Float = 0f,          // 0 = off, 30 to 150 Hz
    val robotDepth: Float = 0f,         // 0.0 to 1.0
    val echoDelayMs: Int = 0,           // 0 to 400 ms
    val echoFeedback: Float = 0f,       // 0.0 to 0.8f
    val distortion: Float = 0f,         // 0.0 to 1.0 (overdrive saturation)
    val noiseGate: Float = 0.01f,       // Noise suppression threshold
    val masterGain: Float = 1.2f        // Output amplification
) {
    companion object {
        val ALL_PRESETS = listOf(
            VoicePreset(
                id = PresetType.GIRL,
                titleRes = R.string.preset_girl,
                descRes = R.string.preset_girl_desc,
                pitchFactor = 1.34f,
                robotFreq = 0f,
                robotDepth = 0f,
                echoDelayMs = 40,
                echoFeedback = 0.08f,
                distortion = 0f,
                noiseGate = 0.008f,
                masterGain = 1.35f
            ),
            VoicePreset(
                id = PresetType.ROBOT,
                titleRes = R.string.preset_robot,
                descRes = R.string.preset_robot_desc,
                pitchFactor = 1.02f,
                robotFreq = 72f,
                robotDepth = 0.85f,
                echoDelayMs = 120,
                echoFeedback = 0.28f,
                distortion = 0.15f,
                noiseGate = 0.015f,
                masterGain = 1.25f
            ),
            VoicePreset(
                id = PresetType.DEEP,
                titleRes = R.string.preset_deep,
                descRes = R.string.preset_deep_desc,
                pitchFactor = 0.74f,
                robotFreq = 0f,
                robotDepth = 0f,
                echoDelayMs = 90,
                echoFeedback = 0.20f,
                distortion = 0.12f,
                noiseGate = 0.012f,
                masterGain = 1.4f
            ),
            VoicePreset(
                id = PresetType.CHILD,
                titleRes = R.string.preset_child,
                descRes = R.string.preset_child_desc,
                pitchFactor = 1.48f,
                robotFreq = 0f,
                robotDepth = 0f,
                echoDelayMs = 50,
                echoFeedback = 0.10f,
                distortion = 0f,
                noiseGate = 0.008f,
                masterGain = 1.3f
            ),
            VoicePreset(
                id = PresetType.MONSTER,
                titleRes = R.string.preset_monster,
                descRes = R.string.preset_monster_desc,
                pitchFactor = 0.62f,
                robotFreq = 42f,
                robotDepth = 0.40f,
                echoDelayMs = 160,
                echoFeedback = 0.45f,
                distortion = 0.65f,
                noiseGate = 0.018f,
                masterGain = 1.45f
            ),
            VoicePreset(
                id = PresetType.FUNNY,
                titleRes = R.string.preset_funny,
                descRes = R.string.preset_funny_desc,
                pitchFactor = 1.72f,
                robotFreq = 0f,
                robotDepth = 0f,
                echoDelayMs = 60,
                echoFeedback = 0.15f,
                distortion = 0f,
                noiseGate = 0.008f,
                masterGain = 1.3f
            )
        )

        fun getPreset(type: PresetType): VoicePreset {
            return ALL_PRESETS.find { it.id == type } ?: ALL_PRESETS[0]
        }
    }
}
