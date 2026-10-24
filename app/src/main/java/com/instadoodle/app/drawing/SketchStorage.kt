package com.instadoodle.app.drawing

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists and retrieves [Sketch] instances using JSON serialization in app-private storage.
 */
object SketchStorage {

    const val DEFAULT_FILE_NAME = "current_sketch.json"

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Saves the given sketch to internal storage as JSON.
     */
    fun saveSketch(
        context: Context,
        sketch: Sketch,
        fileName: String = DEFAULT_FILE_NAME
    ): Boolean {
        return try {
            val file = File(context.filesDir, fileName)
            val jsonString = json.encodeToString(sketch)
            file.writeText(jsonString, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Loads the stored sketch from internal storage.
     */
    fun loadSketch(
        context: Context,
        fileName: String = DEFAULT_FILE_NAME
    ): Sketch? {
        return try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return null
            val jsonString = file.readText(Charsets.UTF_8)
            json.decodeFromString<Sketch>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Checks if a saved sketch file exists.
     */
    fun hasSavedSketch(
        context: Context,
        fileName: String = DEFAULT_FILE_NAME
    ): Boolean {
        val file = File(context.filesDir, fileName)
        return file.exists() && file.length() > 0
    }

    /**
     * Deletes the stored sketch file.
     */
    fun deleteSketch(
        context: Context,
        fileName: String = DEFAULT_FILE_NAME
    ): Boolean {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) file.delete() else true
    }
}
