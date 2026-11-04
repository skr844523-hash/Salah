package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.PresetType
import com.example.audio.RealtimeAudioLoop
import com.example.audio.VoiceDspEngine
import com.example.audio.VoicePreset
import com.example.data.VoiceSettingsRepository
import com.example.service.FloatingVoiceService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val repository = VoiceSettingsRepository(context)

    // Local in-app audio loop for testing directly inside app when service is not running
    private val inAppDspEngine = VoiceDspEngine(44100)
    private val inAppAudioLoop = RealtimeAudioLoop(context, inAppDspEngine)

    // Observable states
    val isFloatingServiceRunning = FloatingVoiceService.isServiceRunningFlow
    val isServiceProcessing = FloatingVoiceService.isProcessingActiveFlow
    val activePreset = FloatingVoiceService.activePresetFlow
    val isMuted = FloatingVoiceService.isMutedFlow

    private val _isInAppTesting = MutableStateFlow(false)
    val isInAppTesting = _isInAppTesting.asStateFlow()

    // DSP active telemetry
    val rmsLevel: StateFlow<Float> = MutableStateFlow(0f)
    val waveform: StateFlow<FloatArray> = MutableStateFlow(FloatArray(32))

    // Realtime telemetry router (reads from Service loop if active, else in-app test loop)
    private val _activeRms = MutableStateFlow(0f)
    val activeRms = _activeRms.asStateFlow()

    private val _activeWaveform = MutableStateFlow(FloatArray(32))
    val activeWaveform = _activeWaveform.asStateFlow()

    // DataStore settings
    val userSettings = repository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.example.data.UserVoiceSettings()
    )

    // Custom slider states
    val currentPitch = MutableStateFlow(1.34f)
    val currentEcho = MutableStateFlow(0.08f)
    val currentDistortion = MutableStateFlow(0.0f)
    val currentRobot = MutableStateFlow(0.0f)
    val currentNoiseGate = MutableStateFlow(0.008f)
    val currentGain = MutableStateFlow(1.25f)

    init {
        viewModelScope.launch {
            userSettings.collect { settings ->
                currentPitch.value = settings.pitchFactor
                currentEcho.value = settings.echoFeedback
                currentDistortion.value = settings.distortion
                currentRobot.value = settings.robotDepth
                currentNoiseGate.value = settings.noiseGate
                currentGain.value = settings.masterGain

                inAppDspEngine.applyPreset(VoicePreset.getPreset(settings.selectedPreset))
                inAppDspEngine.pitchFactor = settings.pitchFactor
                inAppDspEngine.echoFeedback = settings.echoFeedback
                inAppDspEngine.distortion = settings.distortion
                inAppDspEngine.robotDepth = settings.robotDepth
                inAppDspEngine.noiseGateThreshold = settings.noiseGate
                inAppDspEngine.masterGain = settings.masterGain
            }
        }

        // Telemetry updater
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(40)
                val loop = FloatingVoiceService.audioLoopInstance ?: if (inAppAudioLoop.isRunning()) inAppAudioLoop else null
                if (loop != null) {
                    _activeRms.value = loop.rmsLevelFlow.value
                    _activeWaveform.value = loop.waveformFlow.value
                } else {
                    _activeRms.value = 0f
                }
            }
        }
    }

    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun getOverlaySettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    fun startFloatingService() {
        val intent = Intent(context, FloatingVoiceService::class.java).apply {
            action = FloatingVoiceService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopFloatingService() {
        val intent = Intent(context, FloatingVoiceService::class.java).apply {
            action = FloatingVoiceService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun toggleInAppTest(): Boolean {
        return if (inAppAudioLoop.isRunning()) {
            inAppAudioLoop.stop()
            _isInAppTesting.value = false
            false
        } else {
            val started = inAppAudioLoop.start()
            _isInAppTesting.value = started
            started
        }
    }

    fun selectPreset(type: PresetType) {
        FloatingVoiceService.activePresetFlow.value = type
        val p = VoicePreset.getPreset(type)
        currentPitch.value = p.pitchFactor
        currentEcho.value = p.echoFeedback
        currentDistortion.value = p.distortion
        currentRobot.value = p.robotDepth
        currentNoiseGate.value = p.noiseGate
        currentGain.value = p.masterGain

        FloatingVoiceService.audioLoopInstance?.dspEngine?.applyPreset(p)
        inAppDspEngine.applyPreset(p)

        viewModelScope.launch {
            repository.savePreset(type)
        }
    }

    fun updatePitch(newPitch: Float) {
        currentPitch.value = newPitch
        FloatingVoiceService.audioLoopInstance?.dspEngine?.pitchFactor = newPitch
        inAppDspEngine.pitchFactor = newPitch
        persistCustomParams()
    }

    fun updateEcho(newEcho: Float) {
        currentEcho.value = newEcho
        FloatingVoiceService.audioLoopInstance?.dspEngine?.echoFeedback = newEcho
        inAppDspEngine.echoFeedback = newEcho
        persistCustomParams()
    }

    fun updateDistortion(newDist: Float) {
        currentDistortion.value = newDist
        FloatingVoiceService.audioLoopInstance?.dspEngine?.distortion = newDist
        inAppDspEngine.distortion = newDist
        persistCustomParams()
    }

    fun updateRobot(newDepth: Float) {
        currentRobot.value = newDepth
        val engine = FloatingVoiceService.audioLoopInstance?.dspEngine ?: inAppDspEngine
        engine.robotDepth = newDepth
        if (newDepth > 0f && engine.robotFreq <= 0f) {
            engine.robotFreq = 70f
        }
        inAppDspEngine.robotDepth = newDepth
        if (newDepth > 0f && inAppDspEngine.robotFreq <= 0f) {
            inAppDspEngine.robotFreq = 70f
        }
        persistCustomParams()
    }

    fun updateNoiseGate(newGate: Float) {
        currentNoiseGate.value = newGate
        FloatingVoiceService.audioLoopInstance?.dspEngine?.noiseGateThreshold = newGate
        inAppDspEngine.noiseGateThreshold = newGate
        persistCustomParams()
    }

    fun updateMasterGain(newGain: Float) {
        currentGain.value = newGain
        FloatingVoiceService.audioLoopInstance?.dspEngine?.masterGain = newGain
        inAppDspEngine.masterGain = newGain
        persistCustomParams()
    }

    fun resetToDefaults() {
        val p = VoicePreset.getPreset(activePreset.value)
        selectPreset(p.id)
    }

    private fun persistCustomParams() {
        viewModelScope.launch {
            repository.updateCustomSettings(
                pitch = currentPitch.value,
                robotFreq = 70f,
                robotDepth = currentRobot.value,
                echoDelay = 100,
                echoFeedback = currentEcho.value,
                distortion = currentDistortion.value,
                noiseGate = currentNoiseGate.value,
                gain = currentGain.value
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        inAppAudioLoop.stop()
    }
}
