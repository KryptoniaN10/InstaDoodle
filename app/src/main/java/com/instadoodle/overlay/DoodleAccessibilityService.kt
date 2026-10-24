package com.instadoodle.overlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.instadoodle.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dispatches drawing gestures and hosts the overlay. It subscribes to no accessibility
 * events and cannot retrieve window content (see res/xml/accessibility_service_config.xml).
 */
class DoodleAccessibilityService : AccessibilityService(), GestureDispatcher {

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: OverlayController? = null
    lateinit var player: GesturePlayer
        private set

    override fun onServiceConnected() {
        super.onServiceConnected()
        player = GesturePlayer(this, HandlerScheduler(handler))
        overlay = OverlayController(this, SettingsStore.get(this), player)
        _instance.value = this
    }

    fun showOverlay() = overlay?.show()

    fun hideOverlay() = overlay?.hide()

    override fun dispatch(stroke: PlannedStroke, onCompleted: () -> Unit, onCancelled: () -> Unit): Boolean {
        val path = Path().apply {
            moveTo(stroke.points.first().x, stroke.points.first().y)
            stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val gesture = try {
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, stroke.durationMs))
                .build()
        } catch (e: IllegalArgumentException) {
            return false
        }
        return dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) = onCompleted()
                override fun onCancelled(gestureDescription: GestureDescription?) = onCancelled()
            },
            handler,
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        if (::player.isInitialized) player.stop()
    }

    override fun onDestroy() {
        if (::player.isInitialized) player.stop()
        overlay?.hide()
        overlay = null
        _instance.value = null
        super.onDestroy()
    }

    private class HandlerScheduler(private val handler: Handler) : Scheduler {
        private val token = Any()
        override fun schedule(delayMs: Long, block: () -> Unit) {
            handler.postAtTime(block, token, android.os.SystemClock.uptimeMillis() + delayMs)
        }
        override fun cancelAll() = handler.removeCallbacksAndMessages(token)
    }

    companion object {
        private val _instance = MutableStateFlow<DoodleAccessibilityService?>(null)

        /** The connected service, or null when it is disabled. */
        val instance: StateFlow<DoodleAccessibilityService?> = _instance.asStateFlow()

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
            val me = ComponentName(context, DoodleAccessibilityService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }

        fun maxGestureDurationMs(): Long = GestureDescription.getMaxGestureDuration()
    }
}
