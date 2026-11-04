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
    val selectedPreset: PresetType = PresetType.GIRL,
    val isLiveProcessingEnabled: Boolean = false,
    val pitchFactor: Float = 1.34f,
    val robotFreq: Float = 0f,
    val robotDepth: Float = 0f,
    val echoDelayMs: Int = 40,
    val echoFeedback: Float = 0.08f,
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
        val ROBOT_FREQ = floatPreferencesKey("robot_freq")
        val ROBOT_DEPTH = floatPreferencesKey("robot_depth")
        val ECHO_DELAY = intPreferencesKey("echo_delay")
        val ECHO_FEEDBACK = floatPreferencesKey("echo_feedback")
        val DISTORTION = floatPreferencesKey("distortion")
        val NOISE_GATE = floatPreferencesKey("noise_gate")
        val MASTER_GAIN = floatPreferencesKey("master_gain")
        val IS_MUTED = booleanPreferencesKey("is_muted")
        val ROUTING_MODE = stringPreferencesKey("routing_mode")
    }

    val settingsFlow: Flow<UserVoiceSettings> = context.dataStore.data.map { prefs ->
        val presetName = prefs[PreferencesKeys.SELECTED_PRESET] ?: PresetType.GIRL.name
        val preset = try {
            PresetType.valueOf(presetName)
        } catch (e: Exception) {
            PresetType.GIRL
        }
        val defaultPresetValues = VoicePreset.getPreset(preset)

        UserVoiceSettings(
            selectedPreset = preset,
            isLiveProcessingEnabled = prefs[PreferencesKeys.IS_PROCESSING] ?: false,
            pitchFactor = prefs[PreferencesKeys.PITCH_FACTOR] ?: defaultPresetValues.pitchFactor,
            robotFreq = prefs[PreferencesKeys.ROBOT_FREQ] ?: defaultPresetValues.robotFreq,
            robotDepth = prefs[PreferencesKeys.ROBOT_DEPTH] ?: defaultPresetValues.robotDepth,
            echoDelayMs = prefs[PreferencesKeys.ECHO_DELAY] ?: defaultPresetValues.echoDelayMs,
            echoFeedback = prefs[PreferencesKeys.ECHO_FEEDBACK] ?: defaultPresetValues.echoFeedback,
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
            prefs[PreferencesKeys.ROBOT_FREQ] = p.robotFreq
            prefs[PreferencesKeys.ROBOT_DEPTH] = p.robotDepth
            prefs[PreferencesKeys.ECHO_DELAY] = p.echoDelayMs
            prefs[PreferencesKeys.ECHO_FEEDBACK] = p.echoFeedback
            prefs[PreferencesKeys.DISTORTION] = p.distortion
            prefs[PreferencesKeys.NOISE_GATE] = p.noiseGate
            prefs[PreferencesKeys.MASTER_GAIN] = p.masterGain
        }
    }

    suspend fun updateCustomSettings(
        pitch: Float,
        robotFreq: Float,
        robotDepth: Float,
        echoDelay: Int,
        echoFeedback: Float,
        distortion: Float,
        noiseGate: Float,
        gain: Float
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_PRESET] = PresetType.CUSTOM.name
            prefs[PreferencesKeys.PITCH_FACTOR] = pitch
            prefs[PreferencesKeys.ROBOT_FREQ] = robotFreq
            prefs[PreferencesKeys.ROBOT_DEPTH] = robotDepth
            prefs[PreferencesKeys.ECHO_DELAY] = echoDelay
            prefs[PreferencesKeys.ECHO_FEEDBACK] = echoFeedback
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
