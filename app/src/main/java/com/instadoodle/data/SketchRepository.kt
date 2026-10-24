package com.instadoodle.data

import android.content.Context
import android.util.Log
import java.io.File

/** Stores sketches as JSON files in app-private storage (filesDir/sketches). */
class SketchRepository(context: Context) {

    private val dir = File(context.filesDir, "sketches").apply { mkdirs() }

    fun save(sketch: Sketch) {
        File(dir, "${sketch.id}.json").writeText(SketchJson.encode(sketch))
    }

    fun list(): List<Sketch> = dir.listFiles { f -> f.extension == "json" }.orEmpty()
        .mapNotNull { file ->
            runCatching { SketchJson.decode(file.readText()) }
                .onFailure { Log.w("SketchRepository", "Skipping unreadable ${file.name}", it) }
                .getOrNull()
        }
        .sortedByDescending { it.createdAt }

    fun delete(id: String) {
        File(dir, "$id.json").delete()
    }
}
