package com.instadoodle.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.instadoodle.data.DrawSettings
import com.instadoodle.engine.SketchMode
import com.instadoodle.engine.travelDistance
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun AppContent(state: UiState, settings: DrawSettings, serviceRunning: Boolean, vm: MainViewModel, actions: Actions) {
    Surface(Modifier.fillMaxSize().systemBarsPadding(), color = MaterialTheme.colorScheme.background) {
        when (state.screen) {
            Screen.ONBOARDING -> OnboardingScreen(serviceRunning, actions) { vm.navigate(Screen.HOME) }
            Screen.HOME -> HomeScreen(state, serviceRunning, vm)
            Screen.PREVIEW -> PreviewScreen(state, vm, actions)
            Screen.LIBRARY -> LibraryScreen(state, vm)
            Screen.SETTINGS -> SettingsScreen(settings, vm)
            Screen.TEST_CANVAS -> TestCanvasScreen { vm.navigate(Screen.HOME) }
        }
    }
}

@Composable
private fun ScreenColumn(title: String, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) TextButton(onClick = onBack) { Text("‹ Back") }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
fun OnboardingScreen(serviceRunning: Boolean, actions: Actions, onDone: () -> Unit) {
    ScreenColumn("Welcome to InstaDoodle") {
        Text("InstaDoodle turns an image into line strokes and replays them as touch gestures inside a rectangle you place on screen, e.g. over Instagram's DM Draw canvas.")
        Step(1, "Open Accessibility settings, find \"InstaDoodle drawing overlay\" under Downloaded / Installed apps and turn it on.")
        Step(2, "Android 13+ and the switch is greyed out (\"Restricted setting\")? Sideloaded apps need one extra step: open App info, tap the ⋮ menu (top right), choose \"Allow restricted settings\", confirm, then go back to step 1.")
        Step(3, "The service only dispatches gestures. It reads nothing from other apps and InstaDoodle has no internet permission.")
        Button(onClick = actions.openAccessibilitySettings, Modifier.fillMaxWidth()) { Text("Open Accessibility settings") }
        OutlinedButton(onClick = actions.openAppInfo, Modifier.fillMaxWidth()) { Text("Open App info (Allow restricted settings)") }
        ServiceStatus(serviceRunning)
        Button(onClick = onDone, Modifier.fillMaxWidth()) { Text(if (serviceRunning) "Continue" else "Skip for now") }
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$n.", fontWeight = FontWeight.Bold)
        Text(text)
    }
}

@Composable
private fun ServiceStatus(running: Boolean) {
    val (color, text) = if (running) Color(0xFF2E7D32) to "Accessibility service: ON" else Color(0xFFC62828) to "Accessibility service: OFF"
    Text(text, color = color, fontWeight = FontWeight.SemiBold)
}

@Composable
fun HomeScreen(state: UiState, serviceRunning: Boolean, vm: MainViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(vm::pickImage) }
    ScreenColumn("InstaDoodle") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ServiceStatus(serviceRunning)
            Spacer(Modifier.weight(1f))
            if (!serviceRunning) TextButton(onClick = { vm.navigate(Screen.ONBOARDING) }) { Text("Set up") }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Choose an image", fontWeight = FontWeight.SemiBold)
                Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text("Pick from gallery")
                }
                Text("or try a sample:", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.useSample("logo.png", SketchMode.LINE_ART) }) { Text("Logo") }
                    OutlinedButton(onClick = { vm.useSample("portrait.jpg", SketchMode.PHOTO) }) { Text("Portrait") }
                    OutlinedButton(onClick = { vm.useSample("handwriting.png", SketchMode.LINE_ART) }) { Text("Writing") }
                }
                state.sourceBitmap?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(it.asImageBitmap(), contentDescription = "Selected image", Modifier.size(96.dp))
                        Text(state.sourceName ?: "")
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("2. Tune", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SketchMode.entries.forEach { mode ->
                        FilterChip(selected = state.mode == mode, onClick = { vm.setMode(mode) }, label = { Text(mode.name) })
                    }
                }
                LabeledSlider("Detail level", state.detail, 0f..1f, "${(state.detail * 100).roundToInt()}%") { vm.setDetail(it) }
                LabeledSlider("Max strokes", state.maxStrokes.toFloat(), 20f..500f, "${state.maxStrokes}") { vm.setMaxStrokes(it.roundToInt()) }
            }
        }
        Button(onClick = vm::generate, enabled = !state.processing && state.sourceBitmap != null, modifier = Modifier.fillMaxWidth()) {
            if (state.processing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            else Text("3. Generate sketch")
        }
        state.error?.let { ErrorCard(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.navigate(Screen.LIBRARY) }) { Text("Library") }
            OutlinedButton(onClick = { vm.navigate(Screen.SETTINGS) }) { Text("Settings") }
            OutlinedButton(onClick = vm::openDemoShapes) { Text("Demo shapes") }
        }
        OutlinedButton(onClick = { vm.navigate(Screen.TEST_CANVAS) }) { Text("Open test canvas") }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(message, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, valueText: String, onChange: (Float) -> Unit) {
    Column {
        Row {
            Text(label)
            Spacer(Modifier.weight(1f))
            Text(valueText, fontWeight = FontWeight.SemiBold)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
fun PreviewScreen(state: UiState, vm: MainViewModel, actions: Actions) {
    val progress = remember { Animatable(0f) }
    var replayKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.strokes, replayKey) {
        progress.snapTo(0f)
        val durationMs = (state.strokes.size * 25).coerceIn(1200, 8000)
        progress.animateTo(1f, tween(durationMs, easing = LinearEasing))
    }
    ScreenColumn("Preview", onBack = { vm.navigate(Screen.HOME) }) {
        Text(state.sketchName, fontWeight = FontWeight.SemiBold)
        Text("${state.strokes.size} strokes · pen-up travel ${"%.2f".format(travelDistance(state.strokes))} (normalized)", style = MaterialTheme.typography.bodySmall)
        StrokeCanvas(state.strokes, Modifier.fillMaxWidth(), progress.value)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { replayKey++ }) { Text("Replay") }
            OutlinedButton(onClick = vm::saveCurrent) { Text("Save") }
            OutlinedButton(onClick = actions.drawOnTestCanvas) { Text("Test canvas") }
        }
        Button(onClick = actions.sendToInstagram, Modifier.fillMaxWidth()) { Text("Send to Instagram") }
        Text(
            "Opens the overlay with this sketch loaded. In Instagram, open a DM → Draw, place the rectangle over the canvas, then tap Start.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun LibraryScreen(state: UiState, vm: MainViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.navigate(Screen.HOME) }) { Text("‹ Back") }
            Text("Library", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        if (state.library.isEmpty()) Text("No saved sketches yet. Generate one and tap Save.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.library, key = { it.id }) { sketch ->
                Card(Modifier.fillMaxWidth().clickable { vm.openSketch(sketch) }) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StrokeCanvas(sketch.strokes, Modifier.width(72.dp), strokeWidth = 1.5f)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(sketch.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${sketch.strokes.size} strokes · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(sketch.createdAt))}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        IconButton(onClick = { vm.deleteSketch(sketch) }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(settings: DrawSettings, vm: MainViewModel) {
    ScreenColumn("Settings", onBack = { vm.navigate(Screen.HOME) }) {
        Text("Replay timing (applies the next time you tap Start)", fontWeight = FontWeight.SemiBold)
        LabeledSlider("Drawing speed", settings.speedPxPerSec.toFloat(), 200f..5000f, "${settings.speedPxPerSec} px/s") { v ->
            vm.updateSettings { it.copy(speedPxPerSec = v.roundToInt()) }
        }
        LabeledSlider("Min stroke duration", settings.minStrokeMs.toFloat(), 20f..1000f, "${settings.minStrokeMs} ms") { v ->
            vm.updateSettings { it.copy(minStrokeMs = v.roundToInt()) }
        }
        LabeledSlider("Max stroke duration", settings.maxStrokeMs.toFloat(), 500f..10000f, "${settings.maxStrokeMs} ms") { v ->
            vm.updateSettings { it.copy(maxStrokeMs = v.roundToInt()) }
        }
        LabeledSlider("Delay between strokes", settings.interStrokeDelayMs.toFloat(), 0f..1500f, "${settings.interStrokeDelayMs} ms") { v ->
            vm.updateSettings { it.copy(interStrokeDelayMs = v.roundToInt()) }
        }
        LabeledSlider("Dot (tap) duration", settings.dotMs.toFloat(), DrawSettings.MIN_DOT_MS.toFloat()..300f, "${settings.dotMs} ms") { v ->
            vm.updateSettings { it.copy(dotMs = v.roundToInt()) }
        }
        Text("Calibration offset", fontWeight = FontWeight.SemiBold)
        LabeledSlider("X offset", settings.offsetX.toFloat(), -100f..100f, "${settings.offsetX} px") { v ->
            vm.updateSettings { it.copy(offsetX = v.roundToInt()) }
        }
        LabeledSlider("Y offset", settings.offsetY.toFloat(), -100f..100f, "${settings.offsetY} px") { v ->
            vm.updateSettings { it.copy(offsetY = v.roundToInt()) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.updateSettings { it.copy(rectLeft = -1, rectTop = -1, rectWidth = -1, rectHeight = -1) } }) {
                Text("Reset rectangle")
            }
            OutlinedButton(onClick = { vm.updateSettings { DrawSettings() } }) { Text("Reset all") }
        }
        Box(Modifier.height(8.dp))
    }
}
