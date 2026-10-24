package com.instadoodle.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.instadoodle.data.DrawSettings
import com.instadoodle.engine.SketchMode
import com.instadoodle.engine.Stroke
import com.instadoodle.engine.travelDistance
import com.instadoodle.overlay.GestureMapper
import com.instadoodle.overlay.GestureTiming
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun AppContent(state: UiState, settings: DrawSettings, serviceRunning: Boolean, vm: MainViewModel, actions: Actions) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.systemBarsPadding()) {
            when (state.screen) {
                Screen.ONBOARDING -> OnboardingScreen(serviceRunning, actions) { vm.navigate(Screen.HOME) }
                Screen.HOME -> HomeScreen(state, serviceRunning, vm)
                Screen.PREVIEW -> PreviewScreen(state, settings, vm, actions)
                Screen.LIBRARY -> LibraryScreen(state, vm)
                Screen.SETTINGS -> SettingsScreen(settings, vm)
                Screen.TEST_CANVAS -> TestCanvasScreen { vm.navigate(Screen.HOME) }
            }
        }
    }
}

// ---------- shared building blocks ----------

@Composable
private fun ScreenColumn(title: String, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TopBar(title, onBack)
        content()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TopBar(title: String, onBack: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun GradientButton(text: String, icon: ImageVector, enabled: Boolean = true, loading: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(if (enabled) Brand.gradient else SolidGray)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp, color = Color.White)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Text(text, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private val SolidGray = androidx.compose.ui.graphics.SolidColor(Color(0xFFBDB5BA))

@Composable
private fun StatusPill(running: Boolean, onClick: (() -> Unit)? = null) {
    val (bg, label) = if (running) Color(0xFF2E7D32) to "Overlay ready" else Color(0xFFB3261E) to "Overlay off · Set up"
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(bg.copy(alpha = 0.9f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(if (running) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun StatChip(icon: ImageVector, text: String) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, valueText: String, onChange: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Text(valueText, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

/** Rough replay time for a ~900 px placement rectangle using the current timing settings. */
fun estimateDrawSeconds(strokes: List<Stroke>, settings: DrawSettings, sidePx: Float = 900f): Int {
    val timing = GestureTiming(
        speedPxPerSec = settings.speedPxPerSec.toFloat(),
        minStrokeMs = settings.minStrokeMs.toLong(),
        maxStrokeMs = settings.maxStrokeMs.toLong(),
        dotMs = settings.dotMs.toLong(),
        maxGestureMs = 60_000L,
    )
    val totalMs = strokes.sumOf { s ->
        val draw = if (s.points.size == 1) settings.dotMs.toLong() else GestureMapper.durationFor(s.length * sidePx, timing)
        draw + settings.interStrokeDelayMs
    }
    return (totalMs / 1000).toInt()
}

private fun formatDuration(seconds: Int): String = if (seconds < 60) "${seconds}s" else "${seconds / 60}m ${seconds % 60}s"

// ---------- screens ----------

@Composable
fun OnboardingScreen(serviceRunning: Boolean, actions: Actions, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(Brand.gradient).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Default.Draw, null, tint = Color.White, modifier = Modifier.size(36.dp))
            Text("Welcome to InstaDoodle", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Turn an image into line strokes and replay them as touch gestures inside a rectangle you place on screen, e.g. over Instagram's DM Draw canvas.",
                color = Color.White.copy(alpha = 0.9f),
            )
        }
        SectionCard("One-time setup", Icons.Default.Settings) {
            Step(1, "Open Accessibility settings, find \"InstaDoodle drawing overlay\" under Downloaded / Installed apps and turn it on.")
            Step(2, "Android 13+ and the switch is greyed out (\"Restricted setting\")? Open App info, tap ⋮ (top right), choose \"Allow restricted settings\", confirm, then repeat step 1.")
            Step(3, "The service only dispatches gestures. It reads nothing from other apps and InstaDoodle has no internet permission.")
        }
        Button(onClick = actions.openAccessibilitySettings, Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.Accessibility, null)
            Spacer(Modifier.width(8.dp))
            Text("Open Accessibility settings")
        }
        OutlinedButton(onClick = actions.openAppInfo, Modifier.fillMaxWidth().height(52.dp)) {
            Text("Open App info (Allow restricted settings)")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { StatusPill(serviceRunning) }
        GradientButton(if (serviceRunning) "Continue" else "Skip for now", Icons.AutoMirrored.Filled.ArrowForward, onClick = onDone)
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(Brand.gradient), contentAlignment = Alignment.Center) {
            Text("$n", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text(text, Modifier.weight(1f))
    }
}

@Composable
fun HomeScreen(state: UiState, serviceRunning: Boolean, vm: MainViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(vm::pickImage) }
    val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Hero header
        Column(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(Brand.gradient).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Draw, null, tint = Color.White, modifier = Modifier.size(30.dp))
                Text("InstaDoodle", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            }
            Text("Turn any picture into a doodle", color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(4.dp))
            StatusPill(serviceRunning, onClick = if (serviceRunning) null else ({ vm.navigate(Screen.ONBOARDING) }))
        }

        SectionCard("Choose an image", Icons.Default.Image) {
            val bitmap = state.sourceBitmap
            Box(
                Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), MaterialTheme.shapes.medium)
                    .clickable(onClick = pick),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(bitmap.asImageBitmap(), "Selected image", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Text(
                        state.sourceName ?: "",
                        Modifier.align(Alignment.BottomStart).padding(10.dp).clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White, style = MaterialTheme.typography.labelLarge,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Tap to pick from gallery", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Text("or try a sample", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { vm.useSample("logo.png", SketchMode.LINE_ART) }, label = { Text("Logo") }, leadingIcon = { Icon(Icons.Default.Star, null, Modifier.size(18.dp)) })
                AssistChip(onClick = { vm.useSample("portrait.jpg", SketchMode.PHOTO) }, label = { Text("Portrait") }, leadingIcon = { Icon(Icons.Default.Face, null, Modifier.size(18.dp)) })
                AssistChip(onClick = { vm.useSample("handwriting.png", SketchMode.LINE_ART) }, label = { Text("Writing") }, leadingIcon = { Icon(Icons.Default.Gesture, null, Modifier.size(18.dp)) })
            }
        }

        SectionCard("Style", Icons.Default.Tune) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.mode == SketchMode.PHOTO, onClick = { vm.setMode(SketchMode.PHOTO) },
                    label = { Text("Photo") }, leadingIcon = { Icon(Icons.Default.PhotoCamera, null, Modifier.size(18.dp)) },
                )
                FilterChip(
                    selected = state.mode == SketchMode.LINE_ART, onClick = { vm.setMode(SketchMode.LINE_ART) },
                    label = { Text("Line art") }, leadingIcon = { Icon(Icons.Default.Brush, null, Modifier.size(18.dp)) },
                )
            }
            Text(
                if (state.mode == SketchMode.PHOTO) "Traces edges in photos of people, pets, places." else "Best for logos, drawings and handwriting on a plain background.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LabeledSlider("Detail level", state.detail, 0f..1f, "${(state.detail * 100).roundToInt()}%") { vm.setDetail(it) }
            LabeledSlider("Max strokes", state.maxStrokes.toFloat(), 20f..1000f, "${state.maxStrokes}") { vm.setMaxStrokes(it.roundToInt()) }
        }

        GradientButton(
            if (state.sourceBitmap == null) "Pick an image first" else "Generate sketch",
            Icons.Default.AutoAwesome,
            enabled = state.sourceBitmap != null,
            loading = state.processing,
            onClick = vm::generate,
        )
        state.error?.let { ErrorCard(it) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionTile("Library", "${state.library.size} saved", Icons.Default.Collections, Modifier.weight(1f)) { vm.navigate(Screen.LIBRARY) }
            ActionTile("Settings", "Speed & timing", Icons.Default.Settings, Modifier.weight(1f)) { vm.navigate(Screen.SETTINGS) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionTile("Demo shapes", "Square + spiral", Icons.Default.Category, Modifier.weight(1f), vm::openDemoShapes)
            ActionTile("Test canvas", "Try it safely", Icons.Default.Draw, Modifier.weight(1f)) { vm.navigate(Screen.TEST_CANVAS) }
        }
    }
}

@Composable
private fun ActionTile(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
            }
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PreviewScreen(state: UiState, settings: DrawSettings, vm: MainViewModel, actions: Actions) {
    val progress = remember { Animatable(0f) }
    var replayKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.strokes, replayKey) {
        progress.snapTo(0f)
        val durationMs = (state.strokes.size * 25).coerceIn(1200, 8000)
        progress.animateTo(1f, tween(durationMs, easing = LinearEasing))
    }
    val seconds = remember(state.strokes, settings) { estimateDrawSeconds(state.strokes, settings) }
    ScreenColumn("Preview", onBack = { vm.navigate(Screen.HOME) }) {
        Text(state.sketchName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip(Icons.Default.Gesture, "${state.strokes.size} strokes")
            StatChip(Icons.Default.Timer, "~${formatDuration(seconds)} to draw")
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            StrokeCanvas(state.strokes, Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium), progress.value)
        }
        LinearProgressIndicator(progress = { progress.value }, Modifier.fillMaxWidth().clip(RoundedCornerShape(50)))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { replayKey++ }, Modifier.weight(1f)) {
                Icon(Icons.Default.Replay, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Replay")
            }
            FilledTonalButton(onClick = vm::saveCurrent, Modifier.weight(1f)) {
                Icon(Icons.Default.BookmarkAdd, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Save")
            }
            FilledTonalButton(onClick = actions.drawOnTestCanvas, Modifier.weight(1f)) {
                Icon(Icons.Default.Draw, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Test")
            }
        }
        GradientButton("Send to Instagram", Icons.AutoMirrored.Filled.Send, onClick = actions.sendToInstagram)
        Text(
            "Opens the overlay with this sketch loaded. In Instagram, open a DM → Draw, place the rectangle over the canvas, then tap Start. " +
                "Pen-up travel ${"%.2f".format(travelDistance(state.strokes))}.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun LibraryScreen(state: UiState, vm: MainViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TopBar("Library", onBack = { vm.navigate(Screen.HOME) })
        if (state.library.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Collections, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                Spacer(Modifier.height(8.dp))
                Text("No saved sketches yet", style = MaterialTheme.typography.titleMedium)
                Text("Generate one and tap Save.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.library, key = { it.id }) { sketch ->
                ElevatedCard(onClick = { vm.openSketch(sketch) }) {
                    StrokeCanvas(sketch.strokes, Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)), strokeWidth = 1.5f)
                    Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(sketch.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${sketch.strokes.size} strokes · ${DateFormat.getDateInstance(DateFormat.SHORT).format(Date(sketch.createdAt))}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { vm.deleteSketch(sketch) }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete") }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(settings: DrawSettings, vm: MainViewModel) {
    ScreenColumn("Settings", onBack = { vm.navigate(Screen.HOME) }) {
        SectionCard("Replay timing", Icons.Default.Speed) {
            Text("Applies the next time you tap Start.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        }
        SectionCard("Calibration offset", Icons.Default.CenterFocusStrong) {
            LabeledSlider("X offset", settings.offsetX.toFloat(), -100f..100f, "${settings.offsetX} px") { v ->
                vm.updateSettings { it.copy(offsetX = v.roundToInt()) }
            }
            LabeledSlider("Y offset", settings.offsetY.toFloat(), -100f..100f, "${settings.offsetY} px") { v ->
                vm.updateSettings { it.copy(offsetY = v.roundToInt()) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.updateSettings { it.copy(rectLeft = -1, rectTop = -1, rectWidth = -1, rectHeight = -1) } }, Modifier.weight(1f)) {
                Text("Reset rectangle")
            }
            OutlinedButton(onClick = { vm.updateSettings { DrawSettings() } }, Modifier.weight(1f)) { Text("Reset all") }
        }
    }
}
