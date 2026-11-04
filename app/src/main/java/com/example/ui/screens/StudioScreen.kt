package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.audio.PresetType
import com.example.audio.RecordingState
import com.example.audio.VoicePreset
import com.example.ui.components.AudioParametersSection
import com.example.ui.components.PresetCard
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCardBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.util.Locale

@Composable
fun StudioScreen(
    viewModel: MainViewModel,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordingState by viewModel.recordingState.collectAsState()
    val recordingDurationMs by viewModel.recordingDurationMs.collectAsState()
    val recordingWaveform by viewModel.recordingWaveform.collectAsState()
    val recordingAmplitude by viewModel.recordingAmplitude.collectAsState()

    val processedFile by viewModel.processedAudioFile.collectAsState()
    val isProcessingAudio by viewModel.isProcessingAudio.collectAsState()
    val statusMessage by viewModel.processingStatusMessage.collectAsState()

    val isPlaying by viewModel.player.isPlaying.collectAsState()
    val playPositionMs by viewModel.player.currentPositionMs.collectAsState()
    val playDurationMs by viewModel.player.totalDurationMs.collectAsState()

    val activePreset by viewModel.activePreset.collectAsState()

    val pitch by viewModel.currentPitch.collectAsState()
    val speed by viewModel.currentSpeed.collectAsState()
    val bass by viewModel.currentBass.collectAsState()
    val treble by viewModel.currentTreble.collectAsState()
    val echo by viewModel.currentEcho.collectAsState()
    val reverb by viewModel.currentReverb.collectAsState()
    val dist by viewModel.currentDistortion.collectAsState()
    val noise by viewModel.currentNoiseReduction.collectAsState()
    val gain by viewModel.currentGain.collectAsState()

    var showSoundboardDialog by remember { mutableStateOf(false) }
    var soundboardTitleInput by remember { mutableStateOf("") }
    var showSlidersSection by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Microphone Permission Banner (if not granted)
        if (!hasMicPermission) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, NeonRed, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "صلاحية الميكروفون مطلوبة لتسجيل الصوت الحقيقي",
                        color = NeonRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRequestMicPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("منح إذن الميكروفون الآن", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Live Recording Studio Hero Card
        item {
            RecordingHeroCard(
                recordingState = recordingState,
                durationMs = recordingDurationMs,
                amplitude = recordingAmplitude,
                waveform = recordingWaveform,
                onStart = {
                    if (hasMicPermission) {
                        viewModel.startRecording()
                    } else {
                        onRequestMicPermission()
                    }
                },
                onPause = { viewModel.pauseRecording() },
                onResume = { viewModel.resumeRecording() },
                onFinish = { viewModel.stopRecordingAndProcess() }
            )
        }

        // 3. Status Pill / Progress Indicator
        if (isProcessingAudio || statusMessage != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, if (isProcessingAudio) NeonCyan else NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isProcessingAudio) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = statusMessage ?: "جاري معالجة الصوت...",
                        color = if (isProcessingAudio) NeonCyan else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 4. Internal Audio Preview Player (المشغل الداخلي للاستماع قبل الإرسال)
        if (processedFile != null) {
            item {
                AudioPreviewPlayerCard(
                    file = processedFile!!,
                    isPlaying = isPlaying,
                    currentPosMs = playPositionMs,
                    totalDurationMs = playDurationMs,
                    onPlayPause = {
                        if (isPlaying) viewModel.pauseProcessedAudio() else viewModel.playProcessedAudio()
                    },
                    onSeek = { fraction -> viewModel.seekAudio(fraction) },
                    onReapplyPreset = { viewModel.processCurrentAudio() }
                )
            }

            // 5. Professional Sharing Command Center
            item {
                SharingCommandCenter(
                    onShareWhatsApp = { viewModel.shareCurrentToWhatsApp() },
                    onShareTelegram = { viewModel.shareCurrentToTelegram() },
                    onShareDiscord = { viewModel.shareCurrentToDiscord() },
                    onShareGeneral = { viewModel.shareCurrentGeneral() },
                    onSavePhone = { viewModel.saveCurrentToPhone() },
                    onSaveSoundboard = {
                        soundboardTitleInput = "تسجيلي - ${VoicePreset.getPreset(activePreset).nameArabic}"
                        showSoundboardDialog = true
                    }
                )
            }
        }

        // 6. Voice Presets Carousel / Grid (10 Presets)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اختر شخصية الصوت (10 شخصيات)",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = VoicePreset.getPreset(activePreset).nameArabic,
                        color = NeonPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                val allPresets = VoicePreset.allPresets
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (chunk in allPresets.chunked(2)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (p in chunk) {
                                PresetCard(
                                    preset = p,
                                    isSelected = activePreset == p.id,
                                    onSelect = { viewModel.selectPreset(p.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Toggle 9 DSP Manual Sliders
        item {
            Button(
                onClick = { showSlidersSection = !showSlidersSection },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonCardBorder, RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = if (showSlidersSection) "▲ إخفاء التحكم الدقيق (9 مزلقات DSP)" else "▼ التحكم الدقيق والتخصيص (9 مزلقات DSP)",
                    color = NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (showSlidersSection) {
            item {
                AudioParametersSection(
                    pitch = pitch,
                    onPitchChange = { viewModel.updatePitch(it) },
                    speed = speed,
                    onSpeedChange = { viewModel.updateSpeed(it) },
                    bassDb = bass,
                    onBassDbChange = { viewModel.updateBass(it) },
                    trebleDb = treble,
                    onTrebleDbChange = { viewModel.updateTreble(it) },
                    echoFeedback = echo,
                    onEchoFeedbackChange = { viewModel.updateEcho(it) },
                    reverbLevel = reverb,
                    onReverbLevelChange = { viewModel.updateReverb(it) },
                    distortion = dist,
                    onDistortionChange = { viewModel.updateDistortion(it) },
                    noiseReduction = noise,
                    onNoiseReductionChange = { viewModel.updateNoiseReduction(it) },
                    masterGain = gain,
                    onMasterGainChange = { viewModel.updateMasterGain(it) },
                    onResetDefaults = { viewModel.resetToDefaults() }
                )
            }
        }
    }

    // Save to Soundboard Dialog
    if (showSoundboardDialog) {
        AlertDialog(
            onDismissRequest = { showSoundboardDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("حفظ المقطع في لوحة الأصوات", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("أدخل اسماً مميزاً للمقطع لتشغيله أو مشاركته لاحقاً:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = soundboardTitleInput,
                        onValueChange = { soundboardTitleInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentToSoundboard(soundboardTitleInput.trim())
                        showSoundboardDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("حفظ المقطع", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSoundboardDialog = false }) {
                    Text("إلغاء", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun RecordingHeroCard(
    recordingState: RecordingState,
    durationMs: Long,
    amplitude: Float,
    waveform: FloatArray,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit
) {
    val isRecording = recordingState == RecordingState.RECORDING
    val isPaused = recordingState == RecordingState.PAUSED
    val isIdle = recordingState == RecordingState.IDLE || recordingState == RecordingState.STOPPED

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurface)
            .border(
                1.5.dp,
                if (isRecording) NeonPink else NeonPurple.copy(alpha = 0.5f),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Duration Timer Display
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / 60000)
        val millis = (durationMs % 1000) / 100
        val timeString = String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, millis)

        Text(
            text = timeString,
            color = if (isRecording) NeonPink else TextPrimary,
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when {
                isRecording -> "جاري تسجيل صوتك الحقيقي... تكلّم الآن"
                isPaused -> "التسجيل متوقف مؤقتاً"
                else -> "اضغط على زر الميكروفون للبدء"
            },
            color = if (isRecording) NeonPink else TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Waveform Visualizer
        WaveformVisualizer(
            waveform = waveform,
            rmsLevel = amplitude,
            isProcessing = isRecording,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Recording Control Buttons
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isIdle) {
                // Large Start Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(NeonPurple, NeonPink)
                            )
                        )
                        .clickable { onStart() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "بدء التسجيل",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else {
                // Pause / Resume Button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                        .border(1.dp, NeonCyan, CircleShape)
                        .clickable { if (isRecording) onPause() else onResume() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRecording) "إيقاف مؤقت" else "استئناف",
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                // Finish / Stop Button
                Box(
                    modifier = Modifier
                        .scale(if (isRecording) pulseScale else 1f)
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(NeonPink, NeonRed)
                            )
                        )
                        .clickable { onFinish() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "إنهاء التسجيل ومعالجة الصوت",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioPreviewPlayerCard(
    file: File,
    isPlaying: Boolean,
    currentPosMs: Long,
    totalDurationMs: Long,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onReapplyPreset: () -> Unit
) {
    val durationSec = (totalDurationMs / 1000).coerceAtLeast(1)
    val curSec = (currentPosMs / 1000).coerceAtMost(durationSec)
    val progressFraction = if (totalDurationMs > 0) {
        (currentPosMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val fileSizeKb = (file.length() / 1024).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, NeonGreen.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "مشغل المعاينة الداخلي (جاهز للإرسال)",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // File format and size badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceElevated)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${file.extension.uppercase()} • ${fileSizeKb} KB",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Playback slider & progress
        Slider(
            value = progressFraction,
            onValueChange = onSeek,
            colors = SliderDefaults.colors(
                thumbColor = NeonGreen,
                activeTrackColor = NeonGreen,
                inactiveTrackColor = DarkSurfaceElevated
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = String.format(Locale.US, "%02d:%02d", curSec / 60, curSec % 60),
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = String.format(Locale.US, "%02d:%02d", durationSec / 60, durationSec % 60),
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Play/Pause button and Re-Apply Preset button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPlayPause,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPlaying) "إيقاف الاستماع" else "استمع للصوت المعدل",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            IconButton(
                onClick = onReapplyPreset,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "إعادة المعالجة",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SharingCommandCenter(
    onShareWhatsApp: () -> Unit,
    onShareTelegram: () -> Unit,
    onShareDiscord: () -> Unit,
    onShareGeneral: () -> Unit,
    onSavePhone: () -> Unit,
    onSaveSoundboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, NeonCardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "نظام الإرسال والمشاركة الاحترافي (M4A / AAC)",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "يتم إرسال الملف بتنسيق M4A الصوتي المتوافق مباشرة مع مشغلات المحادثات بدون صمت أو تشويه.",
            color = TextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Row 1: WhatsApp, Telegram, Discord
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShareAppButton(
                title = "واتساب",
                color = Color(0xFF25D366),
                onClick = onShareWhatsApp,
                modifier = Modifier.weight(1f)
            )
            ShareAppButton(
                title = "تيليجرام",
                color = Color(0xFF0088CC),
                onClick = onShareTelegram,
                modifier = Modifier.weight(1f)
            )
            ShareAppButton(
                title = "ديسكورد",
                color = Color(0xFF5865F2),
                onClick = onShareDiscord,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: General Share, Save Phone, Save Soundboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShareAppButton(
                title = "مشاركة عامة",
                color = NeonCyan,
                onClick = onShareGeneral,
                modifier = Modifier.weight(1f)
            )
            ShareAppButton(
                title = "حفظ بالهاتف",
                color = NeonAmber,
                onClick = onSavePhone,
                modifier = Modifier.weight(1f)
            )
            ShareAppButton(
                title = "لوحة الأصوات",
                color = NeonPurple,
                onClick = onSaveSoundboard,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ShareAppButton(
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
    ) {
        Text(
            text = title,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}
