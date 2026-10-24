package com.instadoodle.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * All replay timing and placement values. Every timing knob is a setting so
 * tuning for Instagram never needs a rebuild.
 * Rect values < 0 mean "use the default placement".
 */
data class DrawSettings(
    val speedPxPerSec: Int = 1200,
    val minStrokeMs: Int = 60,
    val maxStrokeMs: Int = 4000,
    val interStrokeDelayMs: Int = 150,
    val dotMs: Int = MIN_DOT_MS,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val rectLeft: Int = -1,
    val rectTop: Int = -1,
    val rectWidth: Int = -1,
    val rectHeight: Int = -1,
) {
    companion object {
        const val MIN_DOT_MS = 40
    }
}

/** SharedPreferences-backed settings shared by the UI and the accessibility service. */
class SettingsStore private constructor(private val prefs: SharedPreferences) {

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<DrawSettings> = _settings.asStateFlow()

    fun update(transform: (DrawSettings) -> DrawSettings) {
        val next = transform(_settings.value).let { it.copy(dotMs = it.dotMs.coerceAtLeast(DrawSettings.MIN_DOT_MS)) }
        prefs.edit()
            .putInt(K_SPEED, next.speedPxPerSec)
            .putInt(K_MIN_STROKE, next.minStrokeMs)
            .putInt(K_MAX_STROKE, next.maxStrokeMs)
            .putInt(K_DELAY, next.interStrokeDelayMs)
            .putInt(K_DOT, next.dotMs)
            .putInt(K_OFFSET_X, next.offsetX)
            .putInt(K_OFFSET_Y, next.offsetY)
            .putInt(K_RECT_L, next.rectLeft)
            .putInt(K_RECT_T, next.rectTop)
            .putInt(K_RECT_W, next.rectWidth)
            .putInt(K_RECT_H, next.rectHeight)
            .apply()
        _settings.value = next
    }

    private fun read(): DrawSettings {
        val d = DrawSettings()
        return DrawSettings(
            speedPxPerSec = prefs.getInt(K_SPEED, d.speedPxPerSec),
            minStrokeMs = prefs.getInt(K_MIN_STROKE, d.minStrokeMs),
            maxStrokeMs = prefs.getInt(K_MAX_STROKE, d.maxStrokeMs),
            interStrokeDelayMs = prefs.getInt(K_DELAY, d.interStrokeDelayMs),
            dotMs = prefs.getInt(K_DOT, d.dotMs),
            offsetX = prefs.getInt(K_OFFSET_X, d.offsetX),
            offsetY = prefs.getInt(K_OFFSET_Y, d.offsetY),
            rectLeft = prefs.getInt(K_RECT_L, d.rectLeft),
            rectTop = prefs.getInt(K_RECT_T, d.rectTop),
            rectWidth = prefs.getInt(K_RECT_W, d.rectWidth),
            rectHeight = prefs.getInt(K_RECT_H, d.rectHeight),
        )
    }

    companion object {
        private const val K_SPEED = "speed_px_per_sec"
        private const val K_MIN_STROKE = "min_stroke_ms"
        private const val K_MAX_STROKE = "max_stroke_ms"
        private const val K_DELAY = "inter_stroke_delay_ms"
        private const val K_DOT = "dot_ms"
        private const val K_OFFSET_X = "offset_x"
        private const val K_OFFSET_Y = "offset_y"
        private const val K_RECT_L = "rect_left"
        private const val K_RECT_T = "rect_top"
        private const val K_RECT_W = "rect_width"
        private const val K_RECT_H = "rect_height"

        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore = instance ?: synchronized(this) {
            instance ?: SettingsStore(
                context.applicationContext.getSharedPreferences("draw_settings", Context.MODE_PRIVATE),
            ).also { instance = it }
        }
    }
}
