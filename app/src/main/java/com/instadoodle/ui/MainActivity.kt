package com.instadoodle.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instadoodle.data.SketchSession
import com.instadoodle.overlay.DoodleAccessibilityService

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && !DoodleAccessibilityService.isEnabled(this)) {
            viewModel.navigate(Screen.ONBOARDING)
        }
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFFC2185B), secondary = Color(0xFF7B1FA2))) {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val settings by viewModel.settings.collectAsStateWithLifecycle()
                val serviceRunning by DoodleAccessibilityService.instance.collectAsStateWithLifecycle()

                LaunchedEffect(state.message) {
                    state.message?.let {
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearMessage()
                    }
                }
                BackHandler(enabled = state.screen != Screen.HOME) { viewModel.navigate(Screen.HOME) }

                val actions = Actions(
                    openAccessibilitySettings = ::openAccessibilitySettings,
                    openAppInfo = ::openAppInfo,
                    drawOnTestCanvas = { startOverlay(openInstagram = false) },
                    sendToInstagram = { startOverlay(openInstagram = true) },
                )
                AppContent(state, settings, serviceRunning != null, viewModel, actions)
            }
        }
    }

    /** Loads the current sketch into the overlay, then opens Instagram or the in-app test canvas. */
    private fun startOverlay(openInstagram: Boolean) {
        val state = viewModel.state.value
        if (!DoodleAccessibilityService.isEnabled(this)) {
            viewModel.showMessage("Enable the InstaDoodle accessibility service first")
            openAccessibilitySettings()
            return
        }
        val service = DoodleAccessibilityService.instance.value
        if (service == null) {
            viewModel.showMessage("Service is starting, try again in a moment")
            return
        }
        if (state.strokes.isNotEmpty()) SketchSession.load(state.sketchName, state.strokes)
        service.showOverlay()
        if (!openInstagram) {
            viewModel.navigate(Screen.TEST_CANVAS)
            return
        }
        val launch = packageManager.getLaunchIntentForPackage(INSTAGRAM_PACKAGE)
        if (launch == null) {
            viewModel.showMessage("Instagram is not installed. The overlay is ready.")
        } else {
            startActivity(launch)
        }
    }

    private fun openAccessibilitySettings() = safeStart(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    private fun openAppInfo() =
        safeStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))

    private fun safeStart(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            viewModel.showMessage("Could not open system settings on this device")
        }
    }

    private companion object {
        const val INSTAGRAM_PACKAGE = "com.instagram.android"
    }
}

/** Activity-level callbacks the screens need. */
data class Actions(
    val openAccessibilitySettings: () -> Unit,
    val openAppInfo: () -> Unit,
    val drawOnTestCanvas: () -> Unit,
    val sendToInstagram: () -> Unit,
)
