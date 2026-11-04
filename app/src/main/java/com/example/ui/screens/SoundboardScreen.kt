package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.audio.AudioShareHelper
import com.example.data.SoundboardClip
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
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun SoundboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val clips by viewModel.soundboardClips.collectAsState()
    val isPlaying by viewModel.player.isPlaying.collectAsState()
    val activeClipId by viewModel.activeSoundboardClipId.collectAsState()

    var clipToRename by remember { mutableStateOf<SoundboardClip?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var clipToDelete by remember { mutableStateOf<SoundboardClip?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header info
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "لوحة الأصوات والتأثيرات (Soundboard)",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تشغيل فوري بلمسة واحدة وإرسال مباشر لواتساب وديسكورد",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${clips.size} مقاطع",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (clips.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurface)
                        .border(1.dp, NeonCardBorder, RoundedCornerShape(20.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد تسجيلات محفوظة بعد",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "سجّل صوتك في الاستوديو واحفظه هنا لتشغيله بنقرة واحدة أثناء الألعاب والمحادثات!",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(clips, key = { it.id }) { clip ->
                val isThisClipPlaying = isPlaying && activeClipId == clip.id

                SoundboardClipCard(
                    clip = clip,
                    isPlaying = isThisClipPlaying,
                    onPlayPause = {
                        if (isThisClipPlaying) {
                            viewModel.player.pause()
                        } else {
                            viewModel.playSoundboardClip(clip)
                        }
                    },
                    onShare = { targetApp ->
                        viewModel.shareSoundboardClip(clip, targetApp)
                    },
                    onRename = {
                        clipToRename = clip
                        renameInput = clip.title
                    },
                    onDelete = {
                        clipToDelete = clip
                    }
                )
            }
        }
    }

    // Rename Dialog
    if (clipToRename != null) {
        AlertDialog(
            onDismissRequest = { clipToRename = null },
            containerColor = DarkSurface,
            title = { Text("تعديل اسم المقطع", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val c = clipToRename ?: return@Button
                        viewModel.renameSoundboardClip(c.id, renameInput.trim())
                        clipToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("حفظ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { clipToRename = null }) {
                    Text("إلغاء", color = TextMuted)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (clipToDelete != null) {
        AlertDialog(
            onDismissRequest = { clipToDelete = null },
            containerColor = DarkSurface,
            title = { Text("حذف المقطع الصوتي؟", color = NeonRed, fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من حذف \"${clipToDelete?.title}\" نهائياً؟", color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipToDelete?.let { viewModel.deleteSoundboardClip(it.id) }
                        clipToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                ) {
                    Text("نعم، احذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { clipToDelete = null }) {
                    Text("إلغاء", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun SoundboardClipCard(
    clip: SoundboardClip,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onShare: (AudioShareHelper.TargetApp) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var shareMenuExpanded by remember { mutableStateOf(false) }

    val durationSec = (clip.durationMs / 1000).coerceAtLeast(1)
    val sizeKb = (clip.fileSizeBytes / 1024).coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(
                1.dp,
                if (isPlaying) NeonGreen else NeonCardBorder,
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play / Pause Circle Button
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (isPlaying) NeonGreen else DarkSurfaceElevated)
                .clickable { onPlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                tint = if (isPlaying) Color.Black else NeonCyan,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Clip Metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = clip.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonPurple.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = clip.presetName,
                        color = NeonPurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format(Locale.US, "%02d:%02d • %d KB", durationSec / 60, durationSec % 60, sizeKb),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Action Buttons: Share, Rename, Delete
        Box {
            IconButton(onClick = { shareMenuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "مشاركة المقطع",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = shareMenuExpanded,
                onDismissRequest = { shareMenuExpanded = false },
                modifier = Modifier.background(DarkSurfaceElevated)
            ) {
                DropdownMenuItem(
                    text = { Text("إرسال إلى واتساب", color = Color(0xFF25D366)) },
                    onClick = {
                        shareMenuExpanded = false
                        onShare(AudioShareHelper.TargetApp.WHATSAPP)
                    }
                )
                DropdownMenuItem(
                    text = { Text("إرسال إلى تيليجرام", color = Color(0xFF0088CC)) },
                    onClick = {
                        shareMenuExpanded = false
                        onShare(AudioShareHelper.TargetApp.TELEGRAM)
                    }
                )
                DropdownMenuItem(
                    text = { Text("إرسال إلى ديسكورد", color = Color(0xFF5865F2)) },
                    onClick = {
                        shareMenuExpanded = false
                        onShare(AudioShareHelper.TargetApp.DISCORD)
                    }
                )
                DropdownMenuItem(
                    text = { Text("مشاركة عامة", color = TextPrimary) },
                    onClick = {
                        shareMenuExpanded = false
                        onShare(AudioShareHelper.TargetApp.GENERAL)
                    }
                )
            }
        }

        IconButton(onClick = onRename) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "تعديل الاسم",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "حذف المقطع",
                tint = NeonRed.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
