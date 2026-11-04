package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.PresetType
import com.example.audio.VoicePreset
import com.example.ui.components.AudioParametersSection
import com.example.ui.components.PresetCard
import com.example.ui.components.RoutingRealitySection
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MyApplicationTheme
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

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    GamingVoiceChangerApp(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamingVoiceChangerApp(viewModel: MainViewModel) {
    val context = LocalContext.current

    // State collections
    val isServiceRunning by viewModel.isFloatingServiceRunning.collectAsState()
    val isServiceProcessing by viewModel.isServiceProcessing.collectAsState()
    val isInAppTesting by viewModel.isInAppTesting.collectAsState()
    val activePreset by viewModel.activePreset.collectAsState()

    val currentRms by viewModel.activeRms.collectAsState()
    val currentWaveform by viewModel.activeWaveform.collectAsState()

    val pitch by viewModel.currentPitch.collectAsState()
    val echo by viewModel.currentEcho.collectAsState()
    val distortion by viewModel.currentDistortion.collectAsState()
    val robot by viewModel.currentRobot.collectAsState()
    val noiseGate by viewModel.currentNoiseGate.collectAsState()
    val gain by viewModel.currentGain.collectAsState()

    // Permission tracking
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasOverlayPermission by remember {
        mutableStateOf(viewModel.hasOverlayPermission())
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            Toast.makeText(context, "تم تفعيل إذن الميكروفون بنجاح", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "إذن الميكروفون ضروري لمعالجة الصوت", Toast.LENGTH_LONG).show()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Check overlay on resume
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        hasOverlayPermission = viewModel.hasOverlayPermission()
        hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        onPauseOrDispose { }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(NeonCyan, NeonPurple)))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "VOICE CHANGER AI",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = "GAMING & ESPORTS EDITION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }
                },
                actions = {
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isServiceRunning) NeonGreen.copy(alpha = 0.2f) else DarkSurfaceElevated)
                            .border(
                                1.dp,
                                if (isServiceRunning) NeonGreen else NeonCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isServiceRunning) "الفقاعة نشطة" else "غير نشط",
                            color = if (isServiceRunning) NeonGreen else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.5.dp, NeonPurple, RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.hero_voice_gaming),
                        contentDescription = "Gaming Voice Changer Header",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        DarkBackground.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .padding(14.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Text(
                                text = "غير صوتك مباشرة أثناء اللعب في PUBG Mobile",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "أيقونة عائمة ذكية، زمن استجابة فائق السرعة، ومعالجة DSP متقدمة",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Permissions Alert if needed
            if (!hasMicPermission || !hasOverlayPermission) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NeonAmber.copy(alpha = 0.12f))
                            .border(1.dp, NeonAmber, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = NeonAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تنبيه الأذونات الضرورية لتشغيل التطبيق",
                                color = NeonAmber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!hasMicPermission) {
                            Button(
                                onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("منح إذن الميكروفون (ضروري للمعالجة)", fontSize = 12.sp, color = Color.White)
                            }
                        }

                        if (!hasOverlayPermission) {
                            Button(
                                onClick = {
                                    context.startActivity(viewModel.getOverlaySettingsIntent())
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "منح إذن الظهور فوق التطبيقات (للأيقونة العائمة)",
                                    fontSize = 12.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Main Primary Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Floating Bubble Toggle Button
                    Button(
                        onClick = {
                            if (!hasOverlayPermission) {
                                context.startActivity(viewModel.getOverlaySettingsIntent())
                                return@Button
                            }
                            if (!hasMicPermission) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@Button
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }

                            if (isServiceRunning) {
                                viewModel.stopFloatingService()
                                Toast.makeText(context, "تم إيقاف الأيقونة العائمة", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.startFloatingService()
                                Toast.makeText(context, "تم تشغيل الأيقونة العائمة فوق الشاشة!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServiceRunning) NeonRed else NeonPurple
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(
                                1.5.dp,
                                if (isServiceRunning) NeonRed else NeonCyan,
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.Layers,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isServiceRunning) "إيقاف الأيقونة العائمة (Stop Floating)" else "تفعيل الأيقونة العائمة للألعاب (Floating Bubble)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    // In-App Instant Test Button
                    Button(
                        onClick = {
                            if (!hasMicPermission) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@Button
                            }
                            val active = viewModel.toggleInAppTest()
                            Toast.makeText(
                                context,
                                if (active) "بدأ اختبار الصوت! تحدث الآن في الميكروفون واسمع صوتك المعدل" else "تم إيقاف الاختبار",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isInAppTesting) NeonGreen else DarkSurfaceElevated
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(1.dp, if (isInAppTesting) NeonGreen else NeonCardBorder, RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            imageVector = if (isInAppTesting) Icons.Default.Stop else Icons.Default.Headphones,
                            contentDescription = null,
                            tint = if (isInAppTesting) Color.Black else NeonCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isInAppTesting) "إيقاف اختبار الصوت الحي" else "اختبار الصوت المباشر (تحدث واسمع نفسك فورياً)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isInAppTesting) Color.Black else Color.White
                        )
                    }
                }
            }

            // Real-Time Waveform & Oscilloscope
            item {
                WaveformVisualizer(
                    waveform = currentWaveform,
                    rmsLevel = currentRms,
                    isProcessing = isServiceProcessing || isInAppTesting
                )
            }

            // Preset Characters Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الشخصيات الصوتية الستة (Voice Presets)",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "اختر لتطبيق المعالجة الحية",
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }
            }

            // Preset Cards
            items(VoicePreset.ALL_PRESETS) { preset ->
                PresetCard(
                    preset = preset,
                    isSelected = (preset.id == activePreset),
                    onSelect = {
                        viewModel.selectPreset(preset.id)
                        Toast.makeText(
                            context,
                            "تم تفعيل ${context.getString(preset.titleRes)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            // DSP Parameter Tuning Section
            item {
                AudioParametersSection(
                    pitch = pitch,
                    onPitchChange = { viewModel.updatePitch(it) },
                    robotDepth = robot,
                    onRobotDepthChange = { viewModel.updateRobot(it) },
                    echoFeedback = echo,
                    onEchoFeedbackChange = { viewModel.updateEcho(it) },
                    distortion = distortion,
                    onDistortionChange = { viewModel.updateDistortion(it) },
                    noiseGate = noiseGate,
                    onNoiseGateChange = { viewModel.updateNoiseGate(it) },
                    masterGain = gain,
                    onMasterGainChange = { viewModel.updateMasterGain(it) },
                    onResetDefaults = { viewModel.resetToDefaults() }
                )
            }

            // Android Audio Reality & Gaming Routing Guide
            item {
                RoutingRealitySection()
            }

            // Footer info
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Voice Changer Gaming AI • Built for PUBG Mobile, Discord & WhatsApp",
                        fontSize = 10.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
