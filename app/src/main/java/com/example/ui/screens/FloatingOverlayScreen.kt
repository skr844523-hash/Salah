package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.audio.VoicePreset
import com.example.ui.components.RoutingRealitySection
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

@Composable
fun FloatingOverlayScreen(
    viewModel: MainViewModel,
    hasMicPermission: Boolean,
    hasOverlayPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isServiceRunning by viewModel.isFloatingServiceRunning.collectAsState()
    val isProcessing by viewModel.isServiceProcessing.collectAsState()
    val isInAppTesting by viewModel.isInAppTesting.collectAsState()
    val activePreset by viewModel.activePreset.collectAsState()
    val rmsLevel by viewModel.activeRms.collectAsState()
    val waveform by viewModel.activeWaveform.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Overlay & Permissions Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurface)
                    .border(
                        1.dp,
                        if (isServiceRunning) NeonGreen else NeonPurple.copy(alpha = 0.5f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isServiceRunning) NeonGreen else NeonAmber)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isServiceRunning) "الأيقونة العائمة نشطة فوق التطبيقات" else "الأيقونة العائمة متوقفة",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = VoicePreset.getPreset(activePreset).nameArabic,
                            color = NeonPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Permissions status indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PermissionStatusBadge(
                        title = "الظهور فوق التطبيقات",
                        isGranted = hasOverlayPermission,
                        modifier = Modifier.weight(1f)
                    )
                    PermissionStatusBadge(
                        title = "إذن الميكروفون",
                        isGranted = hasMicPermission,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Toggle Button for Service
                if (!hasOverlayPermission) {
                    Button(
                        onClick = onRequestOverlayPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("منح إذن الظهور فوق التطبيقات", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (isServiceRunning) {
                                viewModel.stopFloatingService()
                            } else {
                                if (!hasMicPermission) {
                                    onRequestMicPermission()
                                } else {
                                    viewModel.startFloatingService()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServiceRunning) NeonRed else NeonPurple
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isServiceRunning) "إيقاف الأيقونة العائمة والخدمة" else "تشغيل الأيقونة العائمة للألعاب",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live Audio Monitor Loop Button
                Button(
                    onClick = {
                        if (!hasMicPermission) {
                            onRequestMicPermission()
                        } else {
                            val started = viewModel.toggleInAppTest()
                            if (started) {
                                Toast.makeText(context, "بدأ اختبار الصوت الحي! استخدم سماعة رأس لتجنب الصدى", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInAppTesting) NeonCyan else DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isInAppTesting) Icons.Default.Stop else Icons.Default.Headphones,
                        contentDescription = null,
                        tint = if (isInAppTesting) Color.Black else NeonCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isInAppTesting) "إيقاف الاستماع الحي" else "اختبار الاستماع الحي في السماعة (Live Monitor)",
                        color = if (isInAppTesting) Color.Black else NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 2. Oscilloscope / Live Waveform Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurface)
                    .border(1.dp, NeonCardBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "محلل الترددات والموجات الصوتية (Oscilloscope)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isServiceRunning || isInAppTesting) "إشارة حية" else "في وضع الاستعداد",
                        color = if (isServiceRunning || isInAppTesting) NeonGreen else TextMuted,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                WaveformVisualizer(
                    waveform = waveform,
                    rmsLevel = rmsLevel,
                    isProcessing = isServiceRunning || isInAppTesting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                )
            }
        }

        // 3. Complete PUBG & Discord Reality Guide
        item {
            RoutingRealitySection()
        }
    }
}

@Composable
private fun PermissionStatusBadge(
    title: String,
    isGranted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceElevated)
            .border(
                1.dp,
                if (isGranted) NeonGreen.copy(alpha = 0.5f) else NeonAmber.copy(alpha = 0.5f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isGranted) NeonGreen else NeonAmber,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = if (isGranted) TextPrimary else NeonAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
