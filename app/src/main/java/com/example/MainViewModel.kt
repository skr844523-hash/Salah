package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEncoder
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioShareHelper
import com.example.audio.PresetType
import com.example.audio.RealtimeAudioLoop
import com.example.audio.RecordingState
import com.example.audio.VoiceDspEngine
import com.example.audio.VoicePreset
import com.example.audio.VoiceRecorder
import com.example.data.SoundboardClip
import com.example.data.SoundboardRepository
import com.example.data.VoiceSettingsRepository
import com.example.service.FloatingVoiceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "MainViewModel"
    private val context: Context get() = getApplication()
    private val repository = VoiceSettingsRepository(context)
    val soundboardRepo = SoundboardRepository(context)

    // Dedicated components
    val recorder = VoiceRecorder(sampleRate = 44100)
    val player = AudioPlayerManager(context)

    // Realtime in-app audio loop (for live mic testing inside app)
    private val inAppDspEngine = VoiceDspEngine(44100)
    private val inAppAudioLoop = RealtimeAudioLoop(context, inAppDspEngine)

    // Active Navigation Tab: 0 = Studio, 1 = Soundboard, 2 = Floating Gaming
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    // Observable states for Floating Service
    val isFloatingServiceRunning = FloatingVoiceService.isServiceRunningFlow
    val isServiceProcessing = FloatingVoiceService.isProcessingActiveFlow
    val activePreset = FloatingVoiceService.activePresetFlow
    val isMuted = FloatingVoiceService.isMutedFlow

    private val _isInAppTesting = MutableStateFlow(false)
    val isInAppTesting = _isInAppTesting.asStateFlow()

    // Realtime telemetry
    private val _activeRms = MutableStateFlow(0f)
    val activeRms = _activeRms.asStateFlow()

    private val _activeWaveform = MutableStateFlow(FloatArray(32))
    val activeWaveform = _activeWaveform.asStateFlow()

    // Recording Studio States
    val recordingState = recorder.state
    val recordingDurationMs = recorder.durationMs
    val recordingAmplitude = recorder.amplitude
    val recordingWaveform = recorder.waveformPoints

    // Transformed Audio & Player
    private val _lastRecordedRawPcm = MutableStateFlow<ShortArray?>(null)
    val lastRecordedRawPcm = _lastRecordedRawPcm.asStateFlow()

    private val _processedAudioFile = MutableStateFlow<File?>(null)
    val processedAudioFile = _processedAudioFile.asStateFlow()

    private val _isProcessingAudio = MutableStateFlow(false)
    val isProcessingAudio = _isProcessingAudio.asStateFlow()

    private val _processingStatusMessage = MutableStateFlow<String?>(null)
    val processingStatusMessage = _processingStatusMessage.asStateFlow()

    // Soundboard clips
    private val _soundboardClips = MutableStateFlow<List<SoundboardClip>>(emptyList())
    val soundboardClips = _soundboardClips.asStateFlow()

    private val _activeSoundboardClipId = MutableStateFlow<String?>(null)
    val activeSoundboardClipId = _activeSoundboardClipId.asStateFlow()

    // 9 Manual DSP Sliders
    val currentPitch = MutableStateFlow(1.35f)
    val currentSpeed = MutableStateFlow(1.0f)
    val currentBass = MutableStateFlow(0f)
    val currentTreble = MutableStateFlow(0f)
    val currentEcho = MutableStateFlow(0.08f)
    val currentReverb = MutableStateFlow(0f)
    val currentDistortion = MutableStateFlow(0.0f)
    val currentNoiseReduction = MutableStateFlow(0.008f)
    val currentGain = MutableStateFlow(1.25f)

    // DataStore settings
    val userSettings = repository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.example.data.UserVoiceSettings()
    )

    init {
        // Load initial soundboard clips
        refreshSoundboardClips()

        // Sync DataStore settings
        viewModelScope.launch {
            userSettings.collect { settings ->
                currentPitch.value = settings.pitchFactor
                currentSpeed.value = settings.speedFactor
                currentBass.value = settings.bassDb
                currentTreble.value = settings.trebleDb
                currentEcho.value = settings.echoFeedback
                currentReverb.value = settings.reverbLevel
                currentDistortion.value = settings.distortion
                currentNoiseReduction.value = settings.noiseGate
                currentGain.value = settings.masterGain

                val preset = VoicePreset.getPreset(settings.selectedPreset)
                inAppDspEngine.applyPreset(preset)
                inAppDspEngine.pitchFactor = settings.pitchFactor
                inAppDspEngine.speedFactor = settings.speedFactor
                inAppDspEngine.bassBoostDb = settings.bassDb
                inAppDspEngine.trebleBoostDb = settings.trebleDb
                inAppDspEngine.echoFeedback = settings.echoFeedback
                inAppDspEngine.reverbLevel = settings.reverbLevel
                inAppDspEngine.distortion = settings.distortion
                inAppDspEngine.noiseReductionThreshold = settings.noiseGate
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
                } else if (recorder.state.value == RecordingState.RECORDING) {
                    _activeRms.value = recorder.amplitude.value
                    _activeWaveform.value = recorder.waveformPoints.value
                } else {
                    _activeRms.value = 0f
                }
            }
        }
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    // ==========================================
    // Recording & Audio Processing CUJ
    // ==========================================

    fun startRecording(): Boolean {
        player.stop()
        val started = recorder.start()
        if (started) {
            _processingStatusMessage.value = "جاري التسجيل... تحدّث بوضوح في الميكروفون"
        } else {
            _processingStatusMessage.value = "فشل بدء التسجيل، تأكد من صلاحية الميكروفون"
        }
        return started
    }

    fun pauseRecording() {
        recorder.pause()
        _processingStatusMessage.value = "تم إيقاف التسجيل مؤقتاً"
    }

    fun resumeRecording() {
        recorder.resume()
        _processingStatusMessage.value = "جاري متابعة التسجيل..."
    }

    fun stopRecordingAndProcess() {
        val pcm = recorder.stop()
        if (pcm.isEmpty() || pcm.size < 4410) { // Less than ~100ms
            _processingStatusMessage.value = "تنبيه: التسجيل قصير جداً أو فارغ!"
            Toast.makeText(context, "لم يتم التقاط أي صوت، يرجى التحدث في الميكروفون!", Toast.LENGTH_LONG).show()
            return
        }

        val hasSound = recorder.hasAudibleSound(pcm)
        if (!hasSound) {
            _processingStatusMessage.value = "تنبيه: التسجيل صامت تماماً! تأكد من الميكروفون."
            Toast.makeText(context, "الملف المسجل صامت تماماً! يرجى رفع مستوى صوتك", Toast.LENGTH_LONG).show()
        }

        _lastRecordedRawPcm.value = pcm
        processCurrentAudio()
    }

    fun processCurrentAudio() {
        val rawPcm = _lastRecordedRawPcm.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _isProcessingAudio.value = true
            _processingStatusMessage.value = "جاري تطبيق التأثير الصوتي وترميز AAC M4A..."

            try {
                // 1. Setup DSP engine with current parameters
                val dsp = VoiceDspEngine(44100).apply {
                    val p = VoicePreset.getPreset(activePreset.value)
                    applyPreset(p)
                    pitchFactor = currentPitch.value
                    speedFactor = currentSpeed.value
                    bassBoostDb = currentBass.value
                    trebleBoostDb = currentTreble.value
                    echoFeedback = currentEcho.value
                    reverbLevel = currentReverb.value
                    distortion = currentDistortion.value
                    noiseReductionThreshold = currentNoiseReduction.value
                    masterGain = currentGain.value
                }

                // 2. Process whole buffer with DSP
                val processedPcm = dsp.processEntireBuffer(rawPcm)

                // 3. Encode to AAC M4A
                val outputDir = File(context.cacheDir, "audio_exports").apply {
                    if (!exists()) mkdirs()
                }
                val outputFile = File(outputDir, "salah_voice_${System.currentTimeMillis()}.m4a")
                val success = AudioEncoder.encodePcmToM4a(
                    pcmData = processedPcm,
                    outputFile = outputFile,
                    sampleRate = 44100,
                    channels = 1,
                    bitRate = 128000
                )

                withContext(Dispatchers.Main) {
                    _isProcessingAudio.value = false
                    if (success && outputFile.exists() && outputFile.length() > 0) {
                        _processedAudioFile.value = outputFile
                        _processingStatusMessage.value = "تمت معالجة الصوت بنجاح! جاهز للاستماع والإرسال."
                        // Auto-load in player for immediate preview
                        player.play(outputFile)
                    } else {
                        // Fallback to WAV if M4A encoder failed
                        val wavFile = File(outputDir, "salah_voice_${System.currentTimeMillis()}.wav")
                        AudioEncoder.savePcmToWav(processedPcm, wavFile, 44100, 1)
                        if (wavFile.exists() && wavFile.length() > 0) {
                            _processedAudioFile.value = wavFile
                            _processingStatusMessage.value = "تم إنشاء الملف بصيغة WAV جاهز للمشاركة."
                            player.play(wavFile)
                        } else {
                            _processingStatusMessage.value = "فشل ترميز الملف الصوتي."
                            Toast.makeText(context, "فشل إنشاء الملف الصوتي!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in processCurrentAudio", e)
                withContext(Dispatchers.Main) {
                    _isProcessingAudio.value = false
                    _processingStatusMessage.value = "حدث خطأ أثناء معالجة الصوت: ${e.message}"
                }
            }
        }
    }

    fun playProcessedAudio() {
        val file = _processedAudioFile.value ?: return
        player.play(file)
    }

    fun pauseProcessedAudio() {
        player.pause()
    }

    fun seekAudio(progressFraction: Float) {
        val total = player.totalDurationMs.value
        if (total > 0) {
            val targetMs = (progressFraction * total).toLong()
            player.seekTo(targetMs)
        }
    }

    // ==========================================
    // Sharing & Saving Actions
    // ==========================================

    fun shareCurrentToWhatsApp() {
        val file = _processedAudioFile.value
        if (file == null) {
            Toast.makeText(context, "يرجى تسجيل الصوت وتحويله أولاً!", Toast.LENGTH_SHORT).show()
            return
        }
        AudioShareHelper.shareAudio(context, file, AudioShareHelper.TargetApp.WHATSAPP, "رسالة صوتية معدلة")
    }

    fun shareCurrentToTelegram() {
        val file = _processedAudioFile.value
        if (file == null) {
            Toast.makeText(context, "يرجى تسجيل الصوت وتحويله أولاً!", Toast.LENGTH_SHORT).show()
            return
        }
        AudioShareHelper.shareAudio(context, file, AudioShareHelper.TargetApp.TELEGRAM, "رسالة صوتية معدلة")
    }

    fun shareCurrentToDiscord() {
        val file = _processedAudioFile.value
        if (file == null) {
            Toast.makeText(context, "يرجى تسجيل الصوت وتحويله أولاً!", Toast.LENGTH_SHORT).show()
            return
        }
        AudioShareHelper.shareAudio(context, file, AudioShareHelper.TargetApp.DISCORD, "رسالة صوتية معدلة")
    }

    fun shareCurrentGeneral() {
        val file = _processedAudioFile.value
        if (file == null) {
            Toast.makeText(context, "يرجى تسجيل الصوت وتحويله أولاً!", Toast.LENGTH_SHORT).show()
            return
        }
        AudioShareHelper.shareAudio(context, file, AudioShareHelper.TargetApp.GENERAL, "مشاركة الملف الصوتي")
    }

    fun saveCurrentToPhone() {
        val file = _processedAudioFile.value
        if (file == null) {
            Toast.makeText(context, "يرجى تسجيل الصوت وتحويله أولاً!", Toast.LENGTH_SHORT).show()
            return
        }
        val savedPath = AudioShareHelper.saveToPhoneStorage(context, file, "VoiceAI_${activePreset.value.name}_${System.currentTimeMillis()}")
        if (savedPath != null) {
            Toast.makeText(context, "تم حفظ الملف في هاتفك بنجاح:\n$savedPath", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "فشل حفظ الملف في الهاتف!", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveCurrentToSoundboard(title: String) {
        val file = _processedAudioFile.value
        if (file == null) {
            Toast.makeText(context, "لا يوجد صوت مسجل لحفظه في لوحة الأصوات!", Toast.LENGTH_SHORT).show()
            return
        }
        val p = VoicePreset.getPreset(activePreset.value)
        val duration = player.totalDurationMs.value
        val clip = soundboardRepo.addClip(file, title.ifEmpty { "صوتي - ${p.nameArabic}" }, p.nameArabic, duration)
        if (clip != null) {
            refreshSoundboardClips()
            Toast.makeText(context, "تمت إضافة المقطع إلى لوحة الأصوات!", Toast.LENGTH_SHORT).show()
        }
    }

    // ==========================================
    // Soundboard Management
    // ==========================================

    fun refreshSoundboardClips() {
        _soundboardClips.value = soundboardRepo.getClips()
    }

    fun playSoundboardClip(clip: SoundboardClip) {
        val file = File(clip.filePath)
        if (file.exists()) {
            _activeSoundboardClipId.value = clip.id
            player.play(file) {
                _activeSoundboardClipId.value = null
            }
        } else {
            Toast.makeText(context, "الملف غير موجود!", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteSoundboardClip(id: String) {
        if (soundboardRepo.deleteClip(id)) {
            refreshSoundboardClips()
            Toast.makeText(context, "تم حذف المقطع", Toast.LENGTH_SHORT).show()
        }
    }

    fun renameSoundboardClip(id: String, newTitle: String) {
        if (soundboardRepo.renameClip(id, newTitle)) {
            refreshSoundboardClips()
            Toast.makeText(context, "تم تعديل اسم المقطع", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareSoundboardClip(clip: SoundboardClip, targetApp: AudioShareHelper.TargetApp = AudioShareHelper.TargetApp.GENERAL) {
        val file = File(clip.filePath)
        AudioShareHelper.shareAudio(context, file, targetApp, clip.title)
    }

    // ==========================================
    // Presets & 9 Sliders
    // ==========================================

    fun selectPreset(type: PresetType) {
        FloatingVoiceService.activePresetFlow.value = type
        val p = VoicePreset.getPreset(type)
        currentPitch.value = p.pitchFactor
        currentSpeed.value = p.speedFactor
        currentBass.value = p.bassBoostDb
        currentTreble.value = p.trebleBoostDb
        currentEcho.value = p.echoFeedback
        currentReverb.value = p.reverbLevel
        currentDistortion.value = p.distortion
        currentNoiseReduction.value = p.noiseReduction
        currentGain.value = p.masterGain

        FloatingVoiceService.audioLoopInstance?.dspEngine?.applyPreset(p)
        inAppDspEngine.applyPreset(p)

        viewModelScope.launch {
            repository.savePreset(type)
        }

        // If user already recorded audio, automatically re-process with the newly selected preset!
        if (_lastRecordedRawPcm.value != null) {
            processCurrentAudio()
        }
    }

    fun updatePitch(newPitch: Float) {
        currentPitch.value = newPitch
        FloatingVoiceService.audioLoopInstance?.dspEngine?.pitchFactor = newPitch
        inAppDspEngine.pitchFactor = newPitch
        persistCustomParams()
    }

    fun updateSpeed(newSpeed: Float) {
        currentSpeed.value = newSpeed
        FloatingVoiceService.audioLoopInstance?.dspEngine?.speedFactor = newSpeed
        inAppDspEngine.speedFactor = newSpeed
        persistCustomParams()
    }

    fun updateBass(newBass: Float) {
        currentBass.value = newBass
        FloatingVoiceService.audioLoopInstance?.dspEngine?.bassBoostDb = newBass
        inAppDspEngine.bassBoostDb = newBass
        persistCustomParams()
    }

    fun updateTreble(newTreble: Float) {
        currentTreble.value = newTreble
        FloatingVoiceService.audioLoopInstance?.dspEngine?.trebleBoostDb = newTreble
        inAppDspEngine.trebleBoostDb = newTreble
        persistCustomParams()
    }

    fun updateEcho(newEcho: Float) {
        currentEcho.value = newEcho
        FloatingVoiceService.audioLoopInstance?.dspEngine?.echoFeedback = newEcho
        inAppDspEngine.echoFeedback = newEcho
        persistCustomParams()
    }

    fun updateReverb(newReverb: Float) {
        currentReverb.value = newReverb
        FloatingVoiceService.audioLoopInstance?.dspEngine?.reverbLevel = newReverb
        inAppDspEngine.reverbLevel = newReverb
        persistCustomParams()
    }

    fun updateDistortion(newDist: Float) {
        currentDistortion.value = newDist
        FloatingVoiceService.audioLoopInstance?.dspEngine?.distortion = newDist
        inAppDspEngine.distortion = newDist
        persistCustomParams()
    }

    fun updateNoiseReduction(newReduction: Float) {
        currentNoiseReduction.value = newReduction
        FloatingVoiceService.audioLoopInstance?.dspEngine?.noiseReductionThreshold = newReduction
        inAppDspEngine.noiseReductionThreshold = newReduction
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
                speed = currentSpeed.value,
                bassDb = currentBass.value,
                trebleDb = currentTreble.value,
                robotFreq = 70f,
                robotDepth = 0f,
                echoDelay = 100,
                echoFeedback = currentEcho.value,
                reverb = currentReverb.value,
                distortion = currentDistortion.value,
                noiseGate = currentNoiseReduction.value,
                gain = currentGain.value
            )
        }
    }

    // ==========================================
    // Floating Service & Overlay Controls
    // ==========================================

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

    override fun onCleared() {
        super.onCleared()
        recorder.reset()
        player.release()
        inAppAudioLoop.stop()
    }
}
