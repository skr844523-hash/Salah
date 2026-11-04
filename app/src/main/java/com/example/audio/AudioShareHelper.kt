package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object AudioShareHelper {
    private const val TAG = "AudioShareHelper"

    enum class TargetApp {
        WHATSAPP,
        TELEGRAM,
        DISCORD,
        GENERAL
    }

    /**
     * Shares an audio file to a specific target app or general system chooser.
     */
    fun shareAudio(
        context: Context,
        file: File,
        targetApp: TargetApp = TargetApp.GENERAL,
        title: String = "مشاركة الصوت المعدل"
    ): Boolean {
        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(context, "خطأ: الملف الصوتي غير موجود أو فارغ!", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val mimeType = if (file.name.endsWith(".m4a", ignoreCase = true)) {
                "audio/mp4"
            } else {
                "audio/wav"
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            when (targetApp) {
                TargetApp.WHATSAPP -> {
                    intent.setPackage("com.whatsapp")
                }
                TargetApp.TELEGRAM -> {
                    intent.setPackage("org.telegram.messenger")
                }
                TargetApp.DISCORD -> {
                    intent.setPackage("com.discord")
                }
                TargetApp.GENERAL -> {
                    // Open system chooser
                }
            }

            return try {
                if (targetApp == TargetApp.GENERAL) {
                    val chooser = Intent.createChooser(intent, title).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                } else {
                    context.startActivity(intent)
                }
                true
            } catch (appNotFound: Exception) {
                // If specific app is not installed, open fallback system chooser
                Log.w(TAG, "Target app $targetApp not installed, falling back to chooser")
                intent.setPackage(null)
                val chooser = Intent.createChooser(intent, title).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share audio file", e)
            Toast.makeText(context, "فشل إرسال الملف الصوتي: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    /**
     * Exports and saves the audio file directly into the user's phone storage (Music/Downloads).
     */
    fun saveToPhoneStorage(
        context: Context,
        sourceFile: File,
        displayName: String = "VoiceChanger_${System.currentTimeMillis()}"
    ): String? {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return null

        val isM4a = sourceFile.name.endsWith(".m4a", ignoreCase = true)
        val ext = if (isM4a) "m4a" else "wav"
        val mimeType = if (isM4a) "audio/mp4" else "audio/wav"
        val fileName = "$displayName.$ext"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/VoiceChangerAI")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null

                resolver.openOutputStream(uri)?.use { os ->
                    FileInputStream(sourceFile).use { `is` ->
                        `is`.copyTo(os)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                "Music/VoiceChangerAI/$fileName"
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "VoiceChangerAI")
                if (!dir.exists()) dir.mkdirs()
                val destFile = File(dir, fileName)
                FileInputStream(sourceFile).use { `is` ->
                    FileOutputStream(destFile).use { os ->
                        `is`.copyTo(os)
                    }
                }
                destFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving audio to phone storage", e)
            null
        }
    }
}
