package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCardBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class RoutingModeInfo(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val color: Color,
    val steps: List<String>
)

@Composable
fun RoutingRealitySection(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val modes = listOf(
        RoutingModeInfo(
            titleRes = R.string.mode_hardware_title,
            descRes = R.string.mode_hardware_desc,
            icon = Icons.Default.Cable,
            color = NeonCyan,
            steps = listOf(
                "استخدم كابل Y-Splitter صوتي (4-pole TRRS) أو كارت صوت USB OTG.",
                "وصل مخرج سماعة الهاتف (Audio Out) بمدخل ميكروفون كابل الـ Splitter.",
                "الصوت المعدّل يخرج من تطبيقنا ويدخل كصوت ميكروفون أصلي ومباشر للعبة PUBG Mobile!",
                "هذه الطريقة القياسية 100% بدون أي روت وتعمل على جميع أجهزة أندرويد."
            )
        ),
        RoutingModeInfo(
            titleRes = R.string.mode_soundboard_title,
            descRes = R.string.mode_soundboard_desc,
            icon = Icons.Default.Send,
            color = NeonPink,
            steps = listOf(
                "افتح الأيقونة العائمة فوق أي تطبيق (واتساب، ديسكورد، تيليجرام).",
                "اضغط زر (تسجيل بصمة صوتية) وتحدث بصوتك الطبيعي.",
                "يقوم المعالج بتحويل صوتك فورياً للشخصية المختارة وحفظه.",
                "اضغط إرسال لمشاركة البصمة الصوتية كرسالة صوتية رسمية بنقرة واحدة."
            )
        ),
        RoutingModeInfo(
            titleRes = R.string.mode_monitor_title,
            descRes = R.string.mode_monitor_desc,
            icon = Icons.Default.Headphones,
            color = NeonPurple,
            steps = listOf(
                "قم بارتداء سماعة الرأس (سلكية أو بلوتوث).",
                "فعل المعالجة الحية من الزر الرئيسي أو الأيقونة العائمة.",
                "استمع إلى صوتك المعدل فورياً مع زمن استجابة منخفض جداً (أقل من 35ms).",
                "اضبط الـ Pitch والـ Echo حتى تصل للنبرة المثالية قبل بدء الجولة."
            )
        ),
        RoutingModeInfo(
            titleRes = R.string.mode_speaker_title,
            descRes = R.string.mode_speaker_desc,
            icon = Icons.Default.VolumeUp,
            color = NeonGreen,
            steps = listOf(
                "قم بتشغيل مكبر الصوت الخارجي للهاتف أو سبيكر قريب.",
                "ضع ميكروفون سماعة الألعاب بالقرب من مكبر الصوت الخارجي.",
                "يقوم ميكروفون لعبة PUBG بالتقاط الصوت المعدل من السماعة وبثه للفريق.",
                "حل سريع وفعال دون الحاجة لأي كابلات إضافية."
            )
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, NeonCardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        // Reality Badge Alert
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NeonAmber.copy(alpha = 0.12f))
                .border(1.dp, NeonAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NeonAmber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.routing_reality_title),
                        color = NeonAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.routing_reality_desc),
                        color = TextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mode Selector Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            modes.forEachIndexed { index, mode ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) mode.color.copy(alpha = 0.25f) else DarkSurfaceElevated)
                        .border(
                            1.dp,
                            if (isSelected) mode.color else NeonCardBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = null,
                            tint = if (isSelected) mode.color else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (index) {
                                0 -> "كابل PUBG"
                                1 -> "بصمة واتساب"
                                2 -> "سماعة الرأس"
                                else -> "مكبر الصوت"
                            },
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Mode Details
        val currentMode = modes[selectedTab]
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, currentMode.color.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(currentMode.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = currentMode.icon,
                            contentDescription = null,
                            tint = currentMode.color,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(currentMode.titleRes),
                        color = currentMode.color,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(currentMode.descRes),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Steps list
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    currentMode.steps.forEachIndexed { sIndex, stepText ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "• ",
                                color = currentMode.color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stepText,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
