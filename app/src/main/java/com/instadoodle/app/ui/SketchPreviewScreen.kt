package com.instadoodle.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as CanvasStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.instadoodle.app.drawing.CoordinateMapper
import com.instadoodle.app.drawing.Sketch
import com.instadoodle.app.drawing.SketchStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SketchPreviewScreen(
    sketch: Sketch,
    onBack: () -> Unit,
    onSketchSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Replay state
    var isPlaying by remember { mutableStateOf(false) }
    var currentStrokeIndex by remember { mutableIntStateOf(sketch.strokes.size) }
    var currentPointFraction by remember { mutableFloatStateOf(1f) }
    var speedMultiplier by remember { mutableFloatStateOf(2f) }
    var showTravelJumps by remember { mutableStateOf(true) }

    val totalStrokes = sketch.strokes.size
    val totalPoints = sketch.totalPointCount

    // Animation loop
    LaunchedEffect(isPlaying, speedMultiplier, currentStrokeIndex) {
        if (isPlaying && totalStrokes > 0) {
            if (currentStrokeIndex >= totalStrokes) {
                currentStrokeIndex = 0
                currentPointFraction = 0f
            }

            while (isActive && isPlaying && currentStrokeIndex < totalStrokes) {
                val stroke = sketch.strokes[currentStrokeIndex]
                val pointCount = stroke.points.size

                // Animate points within current stroke
                val stepDelay = (16L / speedMultiplier).toLong().coerceAtLeast(2L)
                for (p in 1..pointCount) {
                    currentPointFraction = p.toFloat() / pointCount
                    delay(stepDelay)
                }

                currentStrokeIndex++
                currentPointFraction = 0f
                // Delay between strokes
                delay((30L / speedMultiplier).toLong().coerceAtLeast(4L))
            }

            if (currentStrokeIndex >= totalStrokes) {
                isPlaying = false
                currentStrokeIndex = totalStrokes
                currentPointFraction = 1f
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Sketch Preview & Replay",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalStrokes strokes • $totalPoints points • ${sketch.width}x${sketch.height}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val saved = SketchStorage.saveSketch(context, sketch)
                            scope.launch {
                                if (saved) {
                                    snackbarHostState.showSnackbar("Sketch saved successfully for overlay replay!")
                                    onSketchSaved()
                                } else {
                                    snackbarHostState.showSnackbar("Failed to save sketch file.")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    color = Color(0xFF0A0F1D)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (sketch.width <= 0 || sketch.height <= 0 || sketch.strokes.isEmpty()) {
                            return@Canvas
                        }

                        val mapper = CoordinateMapper(
                            sketchWidth = sketch.width.toFloat(),
                            sketchHeight = sketch.height.toFloat(),
                            targetLeft = 24f,
                            targetTop = 24f,
                            targetWidth = size.width - 48f,
                            targetHeight = size.height - 48f
                        )

                        // Draw subtle grid
                        val gridSpacing = 40f
                        var x = 0f
                        while (x < size.width) {
                            drawLine(
                                color = Color(0xFF1E293B),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f
                            )
                            x += gridSpacing
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = Color(0xFF1E293B),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                            y += gridSpacing
                        }

                        // Determine how many strokes to render based on replay state
                        val strokesToDraw = sketch.strokes.take(currentStrokeIndex)

                        // 1. Draw completed strokes
                        val strokeColor = Color(0xFFE2E8F0)
                        val travelLineColor = Color(0x66F43F5E) // Red/Coral dashed for air travel

                        var lastEndPoint: Offset? = null

                        for (stroke in strokesToDraw) {
                            if (stroke.points.isEmpty()) continue

                            val mappedPoints = stroke.points.map {
                                val p = mapper.mapPoint(it)
                                Offset(p.x, p.y)
                            }

                            // Travel line from previous stroke end to current stroke start
                            if (showTravelJumps && lastEndPoint != null) {
                                drawLine(
                                    color = travelLineColor,
                                    start = lastEndPoint,
                                    end = mappedPoints.first(),
                                    strokeWidth = 2f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                )
                            }

                            val path = Path().apply {
                                moveTo(mappedPoints.first().x, mappedPoints.first().y)
                                for (i in 1 until mappedPoints.size) {
                                    lineTo(mappedPoints[i].x, mappedPoints[i].y)
                                }
                                if (stroke.closed && mappedPoints.size > 2) {
                                    close()
                                }
                            }

                            drawPath(
                                path = path,
                                color = strokeColor,
                                style = CanvasStroke(
                                    width = 3.5f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            lastEndPoint = if (stroke.closed) mappedPoints.first() else mappedPoints.last()
                        }

                        // 2. Draw currently animating stroke
                        var currentPenPos: Offset? = null
                        if (isPlaying && currentStrokeIndex < sketch.strokes.size) {
                            val activeStroke = sketch.strokes[currentStrokeIndex]
                            val pointsCount = (activeStroke.points.size * currentPointFraction).roundToInt().coerceAtLeast(1)
                            val partialPoints = activeStroke.points.take(pointsCount).map {
                                val p = mapper.mapPoint(it)
                                Offset(p.x, p.y)
                            }

                            if (partialPoints.isNotEmpty()) {
                                if (showTravelJumps && lastEndPoint != null) {
                                    drawLine(
                                        color = travelLineColor,
                                        start = lastEndPoint,
                                        end = partialPoints.first(),
                                        strokeWidth = 2f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                    )
                                }

                                val path = Path().apply {
                                    moveTo(partialPoints.first().x, partialPoints.first().y)
                                    for (i in 1 until partialPoints.size) {
                                        lineTo(partialPoints[i].x, partialPoints[i].y)
                                    }
                                }

                                drawPath(
                                    path = path,
                                    color = Color(0xFF67E8F9), // Glowing cyan for active stroke
                                    style = CanvasStroke(
                                        width = 4f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )

                                currentPenPos = partialPoints.last()
                            }
                        } else if (!isPlaying && lastEndPoint != null) {
                            currentPenPos = lastEndPoint
                        }

                        // 3. Draw Stylus Indicator Cursor
                        currentPenPos?.let { pen ->
                            drawCircle(
                                color = Color(0x446366F1),
                                radius = 16f,
                                center = pen
                            )
                            drawCircle(
                                color = Color(0xFF6366F1),
                                radius = 7f,
                                center = pen
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3f,
                                center = pen
                            )
                        }
                    }
                }
            }

            // Bottom Controls Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Progress Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Replay Progress: $currentStrokeIndex / $totalStrokes strokes",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Slider(
                        value = currentStrokeIndex.toFloat(),
                        onValueChange = {
                            isPlaying = false
                            currentStrokeIndex = it.roundToInt()
                            currentPointFraction = 1f
                        },
                        valueRange = 0f..totalStrokes.toFloat(),
                        steps = if (totalStrokes > 1) (totalStrokes - 1).coerceAtMost(100) else 0,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Transport Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    if (isPlaying) {
                                        isPlaying = false
                                    } else {
                                        if (currentStrokeIndex >= totalStrokes) {
                                            currentStrokeIndex = 0
                                        }
                                        isPlaying = true
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier.size(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaying) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            IconButton(
                                onClick = {
                                    isPlaying = false
                                    currentStrokeIndex = 0
                                    currentPointFraction = 0f
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Restart"
                                )
                            }

                            IconButton(
                                onClick = {
                                    isPlaying = false
                                    currentStrokeIndex = totalStrokes
                                    currentPointFraction = 1f
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Show All"
                                )
                            }
                        }

                        // Speed selector chips
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0.5f, 1f, 2f, 5f).forEach { speed ->
                                FilterChip(
                                    selected = speedMultiplier == speed,
                                    onClick = { speedMultiplier = speed },
                                    label = { Text("${speed}x", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Show Travel Jumps Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Show air travel jumps (dashed lines)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = showTravelJumps,
                            onCheckedChange = { showTravelJumps = it }
                        )
                    }
                }
            }
        }
    }
}
