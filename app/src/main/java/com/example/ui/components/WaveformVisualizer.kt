package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCardBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    waveform: FloatArray,
    rmsLevel: Float,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurface)
            .border(1.dp, NeonCardBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        // Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isProcessing) NeonGreen else NeonRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isProcessing) "راسم الإشارة الحي (Oscilloscope)" else "المعالج في وضع الاستعداد",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "${(rmsLevel * 100).toInt()}% dB",
                color = if (rmsLevel > 0.8f) NeonRed else NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Oscilloscope Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkBackground)
                .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(12.dp))
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            // Draw grid lines
            drawLine(
                color = DarkSurfaceVariant,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 1f
            )
            val numVLines = 6
            for (v in 1 until numVLines) {
                val x = (width / numVLines) * v
                drawLine(
                    color = DarkSurfaceVariant.copy(alpha = 0.5f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
            }

            val path = Path()
            val pointsCount = waveform.size.coerceAtLeast(2)
            val dx = width / (pointsCount - 1)

            for (i in 0 until pointsCount) {
                val rawVal = if (isProcessing) {
                    waveform.getOrElse(i) { 0f }
                } else {
                    // Gentle idle synthetic wave
                    (sin(pulsePhase + i * 0.4f) * 0.05f)
                }
                val y = centerY - (rawVal * (height * 0.45f))
                val x = i * dx

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    val prevX = (i - 1) * dx
                    val prevRaw = if (isProcessing) {
                        waveform.getOrElse(i - 1) { 0f }
                    } else {
                        (sin(pulsePhase + (i - 1) * 0.4f) * 0.05f)
                    }
                    val prevY = centerY - (prevRaw * (height * 0.45f))
                    val cx = (prevX + x) / 2f
                    path.quadraticTo(prevX, prevY, cx, (prevY + y) / 2f)
                }
            }

            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        NeonCyan,
                        NeonPurple,
                        NeonPink
                    )
                ),
                style = Stroke(
                    width = if (isProcessing) 3.5f else 2f,
                    cap = StrokeCap.Round
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // RMS VU Meter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مستوى الإشارة:",
                color = TextMuted,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DarkBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(rmsLevel.coerceIn(0.02f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(NeonGreen, NeonCyan, NeonPurple, NeonRed)
                            )
                        )
                )
            }
        }
    }
}
