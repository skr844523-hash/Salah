package com.example.audio

import com.example.R

enum class PresetType {
    DEEP_MALE,     // صوت رجل عميق
    CHILD,         // صوت طفل
    ROBOT,         // صوت روبوت
    ALIEN,         // صوت فضائي
    DEMON,         // صوت شيطان
    FEMALE,        // صوت أنثوي
    CARTOON,       // صوت كرتوني
    BROADCASTER,   // صوت مذيع
    GAMER_PRO,     // صوت احترافي للألعاب
    FUNNY,         // صوت مضحك
    CUSTOM         // مخصص
}

data class VoicePreset(
    val id: PresetType,
    val titleAr: String,
    val titleEn: String,
    val descAr: String,
    val descEn: String,
    val pitchFactor: Float = 1.0f,      // 0.5f to 2.0f
    val speedFactor: Float = 1.0f,      // 0.7f to 1.5f
    val bassBoostDb: Float = 0f,        // -10dB to +12dB
    val trebleBoostDb: Float = 0f,      // -10dB to +12dB
    val robotFreq: Float = 0f,          // 0 = off, 30 to 150 Hz
    val robotDepth: Float = 0f,         // 0.0 to 1.0
    val alienModFreq: Float = 0f,       // Vibrato / Frequency warble
    val echoDelayMs: Int = 0,           // 0 to 400 ms
    val echoFeedback: Float = 0f,       // 0.0 to 0.7f
    val reverbLevel: Float = 0f,        // 0.0 to 0.8f
    val distortion: Float = 0f,         // 0.0 to 1.0 (overdrive saturation)
    val noiseReduction: Float = 0.01f,  // 0.001f to 0.04f (noise gate)
    val masterGain: Float = 1.25f       // 0.5f to 2.5f
) {
    val nameArabic: String get() = titleAr
    val nameEnglish: String get() = titleEn
    val noiseGate: Float get() = noiseReduction

    companion object {
        val allPresets: List<VoicePreset> get() = ALL_PRESETS
        val ALL_PRESETS = listOf(
            VoicePreset(
                id = PresetType.DEEP_MALE,
                titleAr = "صوت رجل عميق",
                titleEn = "Deep Male",
                descAr = "نبرة باريتون فخمة مع تضخيم الباس وصوت الصدر",
                descEn = "Deep baritone with rich chest resonance and bass boost",
                pitchFactor = 0.72f,
                speedFactor = 0.95f,
                bassBoostDb = 8f,
                trebleBoostDb = -2f,
                reverbLevel = 0.20f,
                noiseReduction = 0.012f,
                masterGain = 1.4f
            ),
            VoicePreset(
                id = PresetType.CHILD,
                titleAr = "صوت طفل",
                titleEn = "Child",
                descAr = "طبقة طفولية عالية ونقية مع سرعة خفيفة",
                descEn = "High clear tone with bright presence and gentle pace",
                pitchFactor = 1.48f,
                speedFactor = 1.08f,
                bassBoostDb = -4f,
                trebleBoostDb = 6f,
                reverbLevel = 0.10f,
                noiseReduction = 0.008f,
                masterGain = 1.3f
            ),
            VoicePreset(
                id = PresetType.ROBOT,
                titleAr = "صوت روبوت",
                titleEn = "Cyborg Robot",
                descAr = "تأثير إلكتروني معدني وتشكيل حلقي خوارزمي",
                descEn = "Metallic electronic ring modulation with bit-crush",
                pitchFactor = 1.02f,
                speedFactor = 1.0f,
                robotFreq = 74f,
                robotDepth = 0.88f,
                echoDelayMs = 110,
                echoFeedback = 0.25f,
                reverbLevel = 0.20f,
                distortion = 0.15f,
                noiseReduction = 0.015f,
                masterGain = 1.3f
            ),
            VoicePreset(
                id = PresetType.ALIEN,
                titleAr = "صوت فضائي",
                titleEn = "Alien Creature",
                descAr = "نبرة كائن فضائي مع اهتزاز ترددي ومؤثرات فضاء",
                descEn = "Extraterrestrial vibrato modulation and cosmic echo",
                pitchFactor = 1.25f,
                alienModFreq = 8.5f,
                echoDelayMs = 180,
                echoFeedback = 0.38f,
                reverbLevel = 0.40f,
                noiseReduction = 0.012f,
                masterGain = 1.35f
            ),
            VoicePreset(
                id = PresetType.DEMON,
                titleAr = "صوت شيطان / وحش",
                titleEn = "Demon / Monster",
                descAr = "طبقة سفلية مرعبة مع تشبع صوتي Overdrive وتكرار",
                descEn = "Fierce sub-octave growl with heavy overdrive distortion",
                pitchFactor = 0.60f,
                speedFactor = 0.90f,
                bassBoostDb = 10f,
                trebleBoostDb = -4f,
                distortion = 0.70f,
                echoDelayMs = 150,
                echoFeedback = 0.40f,
                reverbLevel = 0.35f,
                noiseReduction = 0.020f,
                masterGain = 1.5f
            ),
            VoicePreset(
                id = PresetType.FEMALE,
                titleAr = "صوت أنثوي",
                titleEn = "Female / Girl",
                descAr = "طبقة صوت نسائية ناعمة مع تصفية ترددات الوضوح",
                descEn = "Smooth feminine pitch with presence EQ enhancement",
                pitchFactor = 1.35f,
                speedFactor = 1.03f,
                bassBoostDb = -3f,
                trebleBoostDb = 5f,
                echoDelayMs = 35,
                echoFeedback = 0.06f,
                reverbLevel = 0.15f,
                noiseReduction = 0.008f,
                masterGain = 1.35f
            ),
            VoicePreset(
                id = PresetType.CARTOON,
                titleAr = "صوت كرتوني",
                titleEn = "Cartoon Chipmunk",
                descAr = "نبرة سنجاب كرتوني فكاهية وسريعة",
                descEn = "High energetic chipmunk tone with comic brightness",
                pitchFactor = 1.75f,
                speedFactor = 1.15f,
                bassBoostDb = -6f,
                trebleBoostDb = 8f,
                reverbLevel = 0.12f,
                noiseReduction = 0.008f,
                masterGain = 1.3f
            ),
            VoicePreset(
                id = PresetType.BROADCASTER,
                titleAr = "صوت مذيع",
                titleEn = "Broadcaster",
                descAr = "صوت إذاعي استوديو احترافي دافئ ومضخم النقاء",
                descEn = "Studio broadcast compression with warm presence EQ",
                pitchFactor = 0.96f,
                speedFactor = 1.0f,
                bassBoostDb = 5f,
                trebleBoostDb = 4f,
                reverbLevel = 0.18f,
                noiseReduction = 0.015f,
                masterGain = 1.45f
            ),
            VoicePreset(
                id = PresetType.GAMER_PRO,
                titleAr = "صوت احترافي للألعاب",
                titleEn = "Esports Gamer Pro",
                descAr = "عزل كامل لضوضاء الألعاب وضغط صوتي نقي لديسكورد",
                descEn = "Total fan noise isolation with crisp discord squad comms",
                pitchFactor = 1.0f,
                speedFactor = 1.0f,
                bassBoostDb = 3f,
                trebleBoostDb = 4f,
                noiseReduction = 0.022f,
                reverbLevel = 0.05f,
                masterGain = 1.4f
            ),
            VoicePreset(
                id = PresetType.FUNNY,
                titleAr = "صوت مضحك",
                titleEn = "Funny / Hilarious",
                descAr = "مؤثرات كوميدية مضحكة مع تردد متأرجح",
                descEn = "Comic warble pitch shift with playful echo",
                pitchFactor = 1.62f,
                alienModFreq = 5.0f,
                echoDelayMs = 80,
                echoFeedback = 0.22f,
                reverbLevel = 0.25f,
                noiseReduction = 0.010f,
                masterGain = 1.3f
            )
        )

        fun getPreset(type: PresetType): VoicePreset {
            return ALL_PRESETS.find { it.id == type } ?: ALL_PRESETS[0]
        }
    }
}
