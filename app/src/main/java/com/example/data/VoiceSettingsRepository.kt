package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.audio.PresetType
import com.example.audio.VoicePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "voice_changer_settings")

data class UserVoiceSettings(
    val selectedPreset: PresetType = PresetType.FEMALE,
    val isLiveProcessingEnabled: Boolean = false,
    val pitchFactor: Float = 1.35f,
    val speedFactor: Float = 1.0f,
    val bassDb: Float = 0f,
    val trebleDb: Float = 0f,
    val robotFreq: Float = 0f,
    val robotDepth: Float = 0f,
    val echoDelayMs: Int = 40,
    val echoFeedback: Float = 0.08f,
    val reverbLevel: Float = 0f,
    val distortion: Float = 0f,
    val noiseGate: Float = 0.008f,
    val masterGain: Float = 1.25f,
    val isMuted: Boolean = false,
    val routingMode: String = "MONITOR"
)

class VoiceSettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val SELECTED_PRESET = stringPreferencesKey("selected_preset")
        val IS_PROCESSING = booleanPreferencesKey("is_processing")
        val PITCH_FACTOR = floatPreferencesKey("pitch_factor")
        val SPEED_FACTOR = floatPreferencesKey("speed_factor")
        val BASS_DB = floatPreferencesKey("bass_db")
        val TREBLE_DB = floatPreferencesKey("treble_db")
        val ROBOT_FREQ = floatPreferencesKey("robot_freq")
        val ROBOT_DEPTH = floatPreferencesKey("robot_depth")
        val ECHO_DELAY = intPreferencesKey("echo_delay")
        val ECHO_FEEDBACK = floatPreferencesKey("echo_feedback")
        val REVERB_LEVEL = floatPreferencesKey("reverb_level")
        val DISTORTION = floatPreferencesKey("distortion")
        val NOISE_GATE = floatPreferencesKey("noise_gate")
        val MASTER_GAIN = floatPreferencesKey("master_gain")
        val IS_MUTED = booleanPreferencesKey("is_muted")
        val ROUTING_MODE = stringPreferencesKey("routing_mode")
    }

    val settingsFlow: Flow<UserVoiceSettings> = context.dataStore.data.map { prefs ->
        val presetName = prefs[PreferencesKeys.SELECTED_PRESET] ?: PresetType.FEMALE.name
        val preset = try {
            PresetType.valueOf(presetName)
        } catch (e: Exception) {
            PresetType.FEMALE
        }
        val defaultPresetValues = VoicePreset.getPreset(preset)

        UserVoiceSettings(
            selectedPreset = preset,
            isLiveProcessingEnabled = prefs[PreferencesKeys.IS_PROCESSING] ?: false,
            pitchFactor = prefs[PreferencesKeys.PITCH_FACTOR] ?: defaultPresetValues.pitchFactor,
            speedFactor = prefs[PreferencesKeys.SPEED_FACTOR] ?: defaultPresetValues.speedFactor,
            bassDb = prefs[PreferencesKeys.BASS_DB] ?: defaultPresetValues.bassBoostDb,
            trebleDb = prefs[PreferencesKeys.TREBLE_DB] ?: defaultPresetValues.trebleBoostDb,
            robotFreq = prefs[PreferencesKeys.ROBOT_FREQ] ?: defaultPresetValues.robotFreq,
            robotDepth = prefs[PreferencesKeys.ROBOT_DEPTH] ?: defaultPresetValues.robotDepth,
            echoDelayMs = prefs[PreferencesKeys.ECHO_DELAY] ?: defaultPresetValues.echoDelayMs,
            echoFeedback = prefs[PreferencesKeys.ECHO_FEEDBACK] ?: defaultPresetValues.echoFeedback,
            reverbLevel = prefs[PreferencesKeys.REVERB_LEVEL] ?: defaultPresetValues.reverbLevel,
            distortion = prefs[PreferencesKeys.DISTORTION] ?: defaultPresetValues.distortion,
            noiseGate = prefs[PreferencesKeys.NOISE_GATE] ?: defaultPresetValues.noiseGate,
            masterGain = prefs[PreferencesKeys.MASTER_GAIN] ?: defaultPresetValues.masterGain,
            isMuted = prefs[PreferencesKeys.IS_MUTED] ?: false,
            routingMode = prefs[PreferencesKeys.ROUTING_MODE] ?: "MONITOR"
        )
    }

    suspend fun savePreset(preset: PresetType) {
        val p = VoicePreset.getPreset(preset)
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_PRESET] = preset.name
            prefs[PreferencesKeys.PITCH_FACTOR] = p.pitchFactor
            prefs[PreferencesKeys.SPEED_FACTOR] = p.speedFactor
            prefs[PreferencesKeys.BASS_DB] = p.bassBoostDb
            prefs[PreferencesKeys.TREBLE_DB] = p.trebleBoostDb
            prefs[PreferencesKeys.ROBOT_FREQ] = p.robotFreq
            prefs[PreferencesKeys.ROBOT_DEPTH] = p.robotDepth
            prefs[PreferencesKeys.ECHO_DELAY] = p.echoDelayMs
            prefs[PreferencesKeys.ECHO_FEEDBACK] = p.echoFeedback
            prefs[PreferencesKeys.REVERB_LEVEL] = p.reverbLevel
            prefs[PreferencesKeys.DISTORTION] = p.distortion
            prefs[PreferencesKeys.NOISE_GATE] = p.noiseGate
            prefs[PreferencesKeys.MASTER_GAIN] = p.masterGain
        }
    }

    suspend fun updateCustomSettings(
        pitch: Float,
        speed: Float,
        bassDb: Float,
        trebleDb: Float,
        robotFreq: Float,
        robotDepth: Float,
        echoDelay: Int,
        echoFeedback: Float,
        reverb: Float,
        distortion: Float,
        noiseGate: Float,
        gain: Float
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_PRESET] = PresetType.CUSTOM.name
            prefs[PreferencesKeys.PITCH_FACTOR] = pitch
            prefs[PreferencesKeys.SPEED_FACTOR] = speed
            prefs[PreferencesKeys.BASS_DB] = bassDb
            prefs[PreferencesKeys.TREBLE_DB] = trebleDb
            prefs[PreferencesKeys.ROBOT_FREQ] = robotFreq
            prefs[PreferencesKeys.ROBOT_DEPTH] = robotDepth
            prefs[PreferencesKeys.ECHO_DELAY] = echoDelay
            prefs[PreferencesKeys.ECHO_FEEDBACK] = echoFeedback
            prefs[PreferencesKeys.REVERB_LEVEL] = reverb
            prefs[PreferencesKeys.DISTORTION] = distortion
            prefs[PreferencesKeys.NOISE_GATE] = noiseGate
            prefs[PreferencesKeys.MASTER_GAIN] = gain
        }
    }

    suspend fun setProcessingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_PROCESSING] = enabled
        }
    }

    suspend fun setMuted(muted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_MUTED] = muted
        }
    }

    suspend fun setRoutingMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ROUTING_MODE] = mode
        }
    }
}
