package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonCardBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AudioParametersSection(
    pitch: Float,
    onPitchChange: (Float) -> Unit,
    robotDepth: Float,
    onRobotDepthChange: (Float) -> Unit,
    echoFeedback: Float,
    onEchoFeedbackChange: (Float) -> Unit,
    distortion: Float,
    onDistortionChange: (Float) -> Unit,
    noiseGate: Float,
    onNoiseGateChange: (Float) -> Unit,
    masterGain: Float,
    onMasterGainChange: (Float) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = NeonPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "الضبط الدقيق للمعالج الصوتي (DSP)",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onResetDefaults,
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("إعادة الضبط", fontSize = 11.sp, color = NeonCyan)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Pitch
        ParameterSlider(
            label = "طبقة الصوت (Pitch Factor)",
            value = pitch,
            onValueChange = onPitchChange,
            valueRange = 0.5f..2.0f,
            displayValue = String.format("%.2fx", pitch),
            accentColor = NeonCyan
        )

        // Echo / Reverb
        ParameterSlider(
            label = "الصدى والتكرار (Echo Feedback)",
            value = echoFeedback,
            onValueChange = onEchoFeedbackChange,
            valueRange = 0f..0.7f,
            displayValue = "${(echoFeedback * 100).toInt()}%",
            accentColor = NeonPurple
        )

        // Robot Modulation
        ParameterSlider(
            label = "التأثير الإلكتروني (Robot Modulation)",
            value = robotDepth,
            onValueChange = onRobotDepthChange,
            valueRange = 0f..1.0f,
            displayValue = "${(robotDepth * 100).toInt()}%",
            accentColor = NeonCyan
        )

        // Distortion
        ParameterSlider(
            label = "التشبع والتشويه (Overdrive Distortion)",
            value = distortion,
            onValueChange = onDistortionChange,
            valueRange = 0f..0.9f,
            displayValue = "${(distortion * 100).toInt()}%",
            accentColor = NeonPink
        )

        // Noise Gate
        ParameterSlider(
            label = "عازل الضوضاء المحيطة (Noise Gate)",
            value = noiseGate,
            onValueChange = onNoiseGateChange,
            valueRange = 0.001f..0.04f,
            displayValue = String.format("%.3f", noiseGate),
            accentColor = NeonCyan
        )

        // Master Gain
        ParameterSlider(
            label = "مستوى التضخيم العام (Master Gain)",
            value = masterGain,
            onValueChange = onMasterGainChange,
            valueRange = 0.5f..2.5f,
            displayValue = String.format("%.1fx", masterGain),
            accentColor = NeonPurple
        )
    }
}

@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    accentColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextSecondary, fontSize = 11.sp)
            Text(displayValue, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor.copy(alpha = 0.8f),
                inactiveTrackColor = DarkSurfaceElevated
            )
        )
    }
}
