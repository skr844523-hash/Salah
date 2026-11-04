package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.audio.PresetType
import com.example.audio.RealtimeAudioLoop
import com.example.audio.VoiceDspEngine
import com.example.audio.VoicePreset
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCardBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

class FloatingVoiceService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        const val CHANNEL_ID = "voice_changer_gaming_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.voicechanger.START"
        const val ACTION_STOP = "com.example.voicechanger.STOP"
        const val ACTION_TOGGLE_PROCESSING = "com.example.voicechanger.TOGGLE_PROCESSING"
        const val ACTION_TOGGLE_MUTE = "com.example.voicechanger.TOGGLE_MUTE"

        val isServiceRunningFlow = MutableStateFlow(false)
        val activePresetFlow = MutableStateFlow(PresetType.GIRL)
        val isProcessingActiveFlow = MutableStateFlow(false)
        val isMutedFlow = MutableStateFlow(false)

        var audioLoopInstance: RealtimeAudioLoop? = null
            private set
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var bubbleComposeView: ComposeView? = null
    private var hudComposeView: ComposeView? = null

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var hudParams: WindowManager.LayoutParams? = null

    private val isHudOpenFlow = MutableStateFlow(false)

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var audioLoop: RealtimeAudioLoop

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()

        val dsp = VoiceDspEngine(44100)
        dsp.applyPreset(VoicePreset.getPreset(activePresetFlow.value))
        audioLoop = RealtimeAudioLoop(this, dsp)
        audioLoopInstance = audioLoop

        isServiceRunningFlow.value = true
        isMutedFlow.value = false

        initBubbleWindow()
        initHudWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_PROCESSING -> {
                toggleProcessing()
            }
            ACTION_TOGGLE_MUTE -> {
                toggleMute()
            }
            ACTION_START -> {
                // start foreground
                startForegroundNotification()
                // auto start processing on bubble launch
                if (!audioLoop.isRunning()) {
                    startAudioProcessing()
                }
            }
        }

        return START_STICKY
    }

    private fun startForegroundNotification() {
        val stopIntent = Intent(this, FloatingVoiceService::class.java).apply {
            this.action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, FloatingVoiceService::class.java).apply {
            this.action = ACTION_TOGGLE_PROCESSING
        }
        val togglePendingIntent = PendingIntent.getService(
            this, 2, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val preset = VoicePreset.getPreset(activePresetFlow.value)
        val presetName = getString(preset.titleRes)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("الأيقونة العائمة نشطة | $presetName")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_launcher_foreground, "تبديل المعالجة", togglePendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "إيقاف", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "خدمة تغيير الصوت للألعاب",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار تشغيل الأيقونة العائمة ومعالجة الصوت الحية"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startAudioProcessing() {
        val success = audioLoop.start()
        isProcessingActiveFlow.value = success
    }

    private fun stopAudioProcessing() {
        audioLoop.stop()
        isProcessingActiveFlow.value = false
    }

    private fun toggleProcessing() {
        if (audioLoop.isRunning()) {
            stopAudioProcessing()
        } else {
            startAudioProcessing()
        }
        startForegroundNotification()
    }

    private fun toggleMute() {
        val newMute = !isMutedFlow.value
        isMutedFlow.value = newMute
        audioLoop.dspEngine.isMuted = newMute
    }

    private fun initBubbleWindow() {
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - 180
            y = screenHeight / 3
        }

        bubbleComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingVoiceService)
            setViewTreeSavedStateRegistryOwner(this@FloatingVoiceService)
            setContent {
                MyApplicationTheme {
                    val isProcessing by isProcessingActiveFlow.collectAsState()
                    val isMuted by isMutedFlow.collectAsState()
                    val activePreset by activePresetFlow.collectAsState()
                    val isHudOpen by isHudOpenFlow.collectAsState()

                    if (!isHudOpen) {
                        FloatingBubbleUi(
                            isProcessing = isProcessing,
                            isMuted = isMuted,
                            preset = activePreset,
                            onBubbleClick = {
                                openHud()
                            },
                            onDragDelta = { dx, dy ->
                                bubbleParams?.let { p ->
                                    p.x += dx.toInt()
                                    p.y += dy.toInt()
                                    windowManager?.updateViewLayout(bubbleComposeView, p)
                                }
                            },
                            onDragEnd = {
                                snapBubbleToEdge()
                            }
                        )
                    }
                }
            }
        }

        windowManager?.addView(bubbleComposeView, bubbleParams)
    }

    private fun snapBubbleToEdge() {
        val params = bubbleParams ?: return
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val targetX = if (params.x < screenWidth / 2) 20 else (screenWidth - 180)
        params.x = targetX
        windowManager?.updateViewLayout(bubbleComposeView, params)
    }

    private fun initHudWindow() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        hudParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        hudComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingVoiceService)
            setViewTreeSavedStateRegistryOwner(this@FloatingVoiceService)
            setContent {
                MyApplicationTheme {
                    val isHudOpen by isHudOpenFlow.collectAsState()
                    val isProcessing by isProcessingActiveFlow.collectAsState()
                    val isMuted by isMutedFlow.collectAsState()
                    val activePreset by activePresetFlow.collectAsState()
                    val rmsLevel by audioLoop.rmsLevelFlow.collectAsState()
                    val isRecordingClip by audioLoop.isRecordingClipFlow.collectAsState()

                    AnimatedVisibility(visible = isHudOpen) {
                        FloatingHudUi(
                            isProcessing = isProcessing,
                            isMuted = isMuted,
                            activePreset = activePreset,
                            rmsLevel = rmsLevel,
                            isRecordingClip = isRecordingClip,
                            dspEngine = audioLoop.dspEngine,
                            onToggleProcessing = { toggleProcessing() },
                            onToggleMute = { toggleMute() },
                            onSelectPreset = { preset ->
                                activePresetFlow.value = preset
                                audioLoop.dspEngine.applyPreset(VoicePreset.getPreset(preset))
                                startForegroundNotification()
                            },
                            onRecordQuickClip = {
                                if (isRecordingClip) {
                                    val cacheFile = File(cacheDir, "gaming_clip_${System.currentTimeMillis()}.wav")
                                    val saved = audioLoop.stopAndSaveClip(cacheFile)
                                    if (saved) {
                                        shareAudioClip(cacheFile)
                                    }
                                } else {
                                    audioLoop.startRecordingClip()
                                }
                            },
                            onCloseHud = { closeHud() },
                            onStopService = { stopSelf() }
                        )
                    }
                }
            }
        }
    }

    private fun openHud() {
        isHudOpenFlow.value = true
        if (hudComposeView?.parent == null) {
            windowManager?.addView(hudComposeView, hudParams)
        }
    }

    private fun closeHud() {
        isHudOpenFlow.value = false
        if (hudComposeView?.parent != null) {
            windowManager?.removeView(hudComposeView)
        }
    }

    private fun shareAudioClip(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "إرسال البصمة الصوتية المعدلة").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(chooser)
        } catch (e: Exception) {
            // fallback generic share
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(Intent.createChooser(intent, "مشاركة الصوت").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        stopAudioProcessing()
        audioLoopInstance = null

        isServiceRunningFlow.value = false
        serviceJob.cancel()

        if (bubbleComposeView != null && bubbleComposeView?.parent != null) {
            windowManager?.removeView(bubbleComposeView)
        }
        if (hudComposeView != null && hudComposeView?.parent != null) {
            windowManager?.removeView(hudComposeView)
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun FloatingBubbleUi(
    isProcessing: Boolean,
    isMuted: Boolean,
    preset: PresetType,
    onBubbleClick: () -> Unit,
    onDragDelta: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val glowColor = when {
        isMuted -> NeonRed
        isProcessing -> NeonCyan
        else -> NeonPurple
    }

    Box(
        modifier = Modifier
            .size(68.dp)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.9f),
                        DarkSurface
                    )
                )
            )
            .border(2.5.dp, glowColor, CircleShape)
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        if (totalDrag < 15f) {
                            onBubbleClick()
                        }
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += kotlin.math.hypot(dragAmount.x, dragAmount.y)
                        onDragDelta(dragAmount.x, dragAmount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Voice Bubble",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = when (preset) {
                    PresetType.GIRL -> "فتاة"
                    PresetType.ROBOT -> "روبوت"
                    PresetType.DEEP -> "عميق"
                    PresetType.CHILD -> "طفل"
                    PresetType.MONSTER -> "وحش"
                    PresetType.FUNNY -> "مضحك"
                    PresetType.CUSTOM -> "مخصص"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun FloatingHudUi(
    isProcessing: Boolean,
    isMuted: Boolean,
    activePreset: PresetType,
    rmsLevel: Float,
    isRecordingClip: Boolean,
    dspEngine: VoiceDspEngine,
    onToggleProcessing: () -> Unit,
    onToggleMute: () -> Unit,
    onSelectPreset: (PresetType) -> Unit,
    onRecordQuickClip: () -> Unit,
    onCloseHud: () -> Unit,
    onStopService: () -> Unit
) {
    var pitchSlider by remember { mutableFloatStateOf(dspEngine.pitchFactor) }
    var echoSlider by remember { mutableFloatStateOf(dspEngine.echoFeedback) }
    var robotSlider by remember { mutableFloatStateOf(dspEngine.robotDepth) }
    var gainSlider by remember { mutableFloatStateOf(dspEngine.masterGain) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkBackground.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonPurple),
        shadowElevation = 24.dp,
        modifier = Modifier
            .width(330.dp)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "VOICE GAMING HUD",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )

                Row {
                    IconButton(
                        onClick = onStopService,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Stop Service",
                            tint = NeonRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onCloseHud,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close HUD",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Master controls row: Power & Mute
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onToggleProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isProcessing) NeonGreen else DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isProcessing) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isProcessing) Color.Black else Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isProcessing) "معالجة نشطة" else "تشغيل الصوت",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isProcessing) Color.Black else Color.White
                    )
                }

                Button(
                    onClick = onToggleMute,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMuted) NeonRed else DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMuted) "مكتوم" else "كتم المايك",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Presets row
            Text(
                text = "اختر الشخصية الصوتية",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PresetType.values().filter { it != PresetType.CUSTOM }.forEach { type ->
                    val isSelected = (type == activePreset)
                    val presetName = when (type) {
                        PresetType.GIRL -> "فتاة"
                        PresetType.ROBOT -> "روبوت"
                        PresetType.DEEP -> "عميق"
                        PresetType.CHILD -> "طفل"
                        PresetType.MONSTER -> "وحش"
                        PresetType.FUNNY -> "مضحك"
                        else -> ""
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonPurple else DarkSurfaceElevated)
                            .border(
                                1.dp,
                                if (isSelected) NeonCyan else NeonCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onSelectPreset(type)
                                pitchSlider = dspEngine.pitchFactor
                                echoSlider = dspEngine.echoFeedback
                                robotSlider = dspEngine.robotDepth
                                gainSlider = dspEngine.masterGain
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = presetName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sliders: Pitch, Echo, Robot
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("طبقة الصوت (Pitch)", fontSize = 11.sp, color = TextSecondary)
                    Text(String.format("%.2fx", pitchSlider), fontSize = 11.sp, color = NeonCyan)
                }
                Slider(
                    value = pitchSlider,
                    onValueChange = {
                        pitchSlider = it
                        dspEngine.pitchFactor = it
                    },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonPurple
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الصدى (Echo)", fontSize = 11.sp, color = TextSecondary)
                    Text("${(echoSlider * 100).toInt()}%", fontSize = 11.sp, color = NeonCyan)
                }
                Slider(
                    value = echoSlider,
                    onValueChange = {
                        echoSlider = it
                        dspEngine.echoFeedback = it
                    },
                    valueRange = 0f..0.7f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonPurple
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("مستوى المضخم (Gain)", fontSize = 11.sp, color = TextSecondary)
                    Text(String.format("%.1fx", gainSlider), fontSize = 11.sp, color = NeonCyan)
                }
                Slider(
                    value = gainSlider,
                    onValueChange = {
                        gainSlider = it
                        dspEngine.masterGain = it
                    },
                    valueRange = 0.5f..2.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonPurple
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Soundboard Voice Note Button (for WhatsApp / Discord sharing)
            Button(
                onClick = onRecordQuickClip,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecordingClip) NeonRed else NeonPink
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (isRecordingClip) Icons.Default.Stop else Icons.Default.Share,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRecordingClip) "جارِ التسجيل... اضغط للمشاركة" else "تسجيل بصمة صوتية ومشاركتها",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
