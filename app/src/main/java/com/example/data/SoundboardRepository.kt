package com.example.data

import android.content.Context
import com.example.audio.AudioEncoder
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

data class SoundboardClip(
    val id: String,
    val title: String,
    val filePath: String,
    val presetName: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val createdAt: Long
)

class SoundboardRepository(private val context: Context) {
    private val baseDir = File(context.filesDir, "soundboard").apply {
        if (!exists()) mkdirs()
    }
    private val indexFile = File(baseDir, "clips_index.json")

    init {
        ensureInitialClips()
    }

    @Synchronized
    fun getClips(): List<SoundboardClip> {
        val list = mutableListOf<SoundboardClip>()
        if (!indexFile.exists()) return list

        try {
            val jsonStr = indexFile.readText()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val path = obj.getString("filePath")
                val file = File(path)
                if (file.exists() && file.length() > 0) {
                    list.add(
                        SoundboardClip(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            filePath = path,
                            presetName = obj.optString("presetName", "مخصص"),
                            durationMs = obj.optLong("durationMs", 0L),
                            fileSizeBytes = file.length(),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @Synchronized
    fun addClip(
        sourceFile: File,
        title: String,
        presetName: String,
        durationMs: Long
    ): SoundboardClip? {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return null

        try {
            val id = "clip_${System.currentTimeMillis()}"
            val ext = sourceFile.extension.ifEmpty { "m4a" }
            val destFile = File(baseDir, "$id.$ext")
            sourceFile.copyTo(destFile, overwrite = true)

            val newClip = SoundboardClip(
                id = id,
                title = title,
                filePath = destFile.absolutePath,
                presetName = presetName,
                durationMs = durationMs,
                fileSizeBytes = destFile.length(),
                createdAt = System.currentTimeMillis()
            )

            val current = getClips().toMutableList()
            current.add(0, newClip)
            saveClipsIndex(current)
            return newClip
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    @Synchronized
    fun renameClip(id: String, newTitle: String): Boolean {
        val current = getClips().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = current[index]
            current[index] = old.copy(title = newTitle)
            saveClipsIndex(current)
            return true
        }
        return false
    }

    @Synchronized
    fun deleteClip(id: String): Boolean {
        val current = getClips().toMutableList()
        val clip = current.find { it.id == id } ?: return false
        try {
            File(clip.filePath).delete()
        } catch (ignored: Exception) {}
        current.removeAll { it.id == id }
        saveClipsIndex(current)
        return true
    }

    private fun saveClipsIndex(clips: List<SoundboardClip>) {
        val array = JSONArray()
        for (c in clips) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("title", c.title)
                put("filePath", c.filePath)
                put("presetName", c.presetName)
                put("durationMs", c.durationMs)
                put("createdAt", c.createdAt)
            }
            array.put(obj)
        }
        indexFile.writeText(array.toString())
    }

    /**
     * Seeds initial fun gamer soundboard clips if the soundboard is empty.
     */
    private fun ensureInitialClips() {
        if (indexFile.exists() && getClips().isNotEmpty()) return

        try {
            val sampleRate = 44100

            // 1. Victory Fanfare Synth
            val victoryPcm = ShortArray(sampleRate * 2) { i ->
                val t = i.toFloat() / sampleRate
                val freq = if (t < 0.3f) 523.25f else if (t < 0.6f) 659.25f else 783.99f
                (sin(2.0 * PI * freq * t) * 16000).toInt().toShort()
            }
            val victoryFile = File(baseDir, "victory_fanfare.wav")
            AudioEncoder.savePcmToWav(victoryPcm, victoryFile, sampleRate, 1)

            // 2. Robot Beep Comms
            val robotPcm = ShortArray(sampleRate) { i ->
                val t = i.toFloat() / sampleRate
                val freq = 880f + sin(2.0 * PI * 15.0 * t).toFloat() * 200f
                (sin(2.0 * PI * freq * t) * 15000).toInt().toShort()
            }
            val robotFile = File(baseDir, "robot_comms.wav")
            AudioEncoder.savePcmToWav(robotPcm, robotFile, sampleRate, 1)

            val initialList = listOf(
                SoundboardClip(
                    id = "preset_victory",
                    title = "نغمة النصر (Victory Gaming)",
                    filePath = victoryFile.absolutePath,
                    presetName = "صوت احترافي للألعاب",
                    durationMs = 2000L,
                    fileSizeBytes = victoryFile.length(),
                    createdAt = System.currentTimeMillis()
                ),
                SoundboardClip(
                    id = "preset_robot",
                    title = "إشارة الروبوت (Robot Beep)",
                    filePath = robotFile.absolutePath,
                    presetName = "صوت روبوت",
                    durationMs = 1000L,
                    fileSizeBytes = robotFile.length(),
                    createdAt = System.currentTimeMillis()
                )
            )
            saveClipsIndex(initialList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
