package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AudioParametersSection(
    pitch: Float,
    onPitchChange: (Float) -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    bassDb: Float,
    onBassDbChange: (Float) -> Unit,
    trebleDb: Float,
    onTrebleDbChange: (Float) -> Unit,
    echoFeedback: Float,
    onEchoFeedbackChange: (Float) -> Unit,
    reverbLevel: Float,
    onReverbLevelChange: (Float) -> Unit,
    distortion: Float,
    onDistortionChange: (Float) -> Unit,
    noiseReduction: Float,
    onNoiseReductionChange: (Float) -> Unit,
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
                    text = "التحكم في معالج الصوت (DSP Studio)",
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

        // 1. Pitch
        ParameterSlider(
            label = "طبقة الصوت (Pitch)",
            value = pitch,
            onValueChange = onPitchChange,
            valueRange = 0.5f..2.0f,
            displayValue = String.format("%.2fx", pitch),
            accentColor = NeonCyan
        )

        // 2. Speed
        ParameterSlider(
            label = "سرعة الصوت (Speed / Rate)",
            value = speed,
            onValueChange = onSpeedChange,
            valueRange = 0.7f..1.5f,
            displayValue = String.format("%.2fx", speed),
            accentColor = NeonGreen
        )

        // 3. Bass EQ
        ParameterSlider(
            label = "تضخيم البيز (Bass Boost)",
            value = bassDb,
            onValueChange = onBassDbChange,
            valueRange = -10f..12f,
            displayValue = "${bassDb.toInt()} dB",
            accentColor = NeonPurple
        )

        // 4. Treble EQ
        ParameterSlider(
            label = "حدة النبرة (Treble Clarity)",
            value = trebleDb,
            onValueChange = onTrebleDbChange,
            valueRange = -10f..12f,
            displayValue = "${trebleDb.toInt()} dB",
            accentColor = NeonCyan
        )

        // 5. Echo Delay
        ParameterSlider(
            label = "الصدى والتكرار (Echo)",
            value = echoFeedback,
            onValueChange = onEchoFeedbackChange,
            valueRange = 0f..0.7f,
            displayValue = "${(echoFeedback * 100).toInt()}%",
            accentColor = NeonPink
        )

        // 6. Reverb
        ParameterSlider(
            label = "تردد الغرفة (Reverb Ambience)",
            value = reverbLevel,
            onValueChange = onReverbLevelChange,
            valueRange = 0f..0.8f,
            displayValue = "${(reverbLevel * 100).toInt()}%",
            accentColor = NeonPurple
        )

        // 7. Distortion
        ParameterSlider(
            label = "التشبع والتشويه (Overdrive)",
            value = distortion,
            onValueChange = onDistortionChange,
            valueRange = 0f..0.9f,
            displayValue = "${(distortion * 100).toInt()}%",
            accentColor = NeonPink
        )

        // 8. Noise Reduction
        ParameterSlider(
            label = "عزل الضوضاء (Noise Reduction)",
            value = noiseReduction,
            onValueChange = onNoiseReductionChange,
            valueRange = 0.001f..0.04f,
            displayValue = String.format("%.3f", noiseReduction),
            accentColor = NeonCyan
        )

        // 9. Volume
        ParameterSlider(
            label = "مستوى التضخيم (Volume / Gain)",
            value = masterGain,
            onValueChange = onMasterGainChange,
            valueRange = 0.5f..2.5f,
            displayValue = String.format("%.1fx", masterGain),
            accentColor = NeonGreen
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
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
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
                activeTrackColor = accentColor.copy(alpha = 0.85f),
                inactiveTrackColor = DarkSurfaceElevated
            )
        )
    }
}
