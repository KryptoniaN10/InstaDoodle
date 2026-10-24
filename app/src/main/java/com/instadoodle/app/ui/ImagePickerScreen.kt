package com.instadoodle.app.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.instadoodle.app.drawing.PathSimplifier
import com.instadoodle.app.drawing.Sketch
import com.instadoodle.app.drawing.SketchStorage
import com.instadoodle.app.image.ContourExtractor
import com.instadoodle.app.image.GeminiImageStyler
import com.instadoodle.app.image.ImageProcessor
import com.instadoodle.app.image.SampleImages
import com.instadoodle.app.BuildConfig
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerScreen(
    onNavigateToPreview: (Sketch) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Selected image
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var edgeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentSketch by remember { mutableStateOf<Sketch?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }

    // AI Stylization state
    var useAiStylization by remember { mutableStateOf(false) }
    var aiStyle by remember { mutableStateOf(GeminiImageStyler.Style.LINE_ART) }
    var isAiProcessing by remember { mutableStateOf(false) }
    var aiStyledBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Pipeline Sliders & Settings
    var extractionMode by remember { mutableStateOf(ContourExtractor.Mode.PHOTO) }
    var maxDimension by remember { mutableIntStateOf(512) }
    var cannyLow by remember { mutableDoubleStateOf(50.0) }
    var cannyHigh by remember { mutableDoubleStateOf(150.0) }
    var adaptiveBlockSize by remember { mutableIntStateOf(11) }
    var adaptiveC by remember { mutableDoubleStateOf(2.0) }
    var epsilon by remember { mutableFloatStateOf(1.5f) }
    var minPoints by remember { mutableIntStateOf(8) }

    // Helper function to re-run pipeline
    val runPipeline: (Bitmap) -> Unit = { bitmap ->
        scope.launch {
            isProcessing = true
            statusText = "Preprocessing image..."
            try {
                if (!ImageProcessor.isAvailable()) {
                    snackbarHostState.showSnackbar("Initializing OpenCV...")
                }

                // Step AI (optional): Gemini stylization
                val pipelineInput: Bitmap = if (useAiStylization) {
                    statusText = "✨ AI stylizing (${aiStyle.label})..."
                    isAiProcessing = true
                    try {
                        val apiKey = BuildConfig.GEMINI_API_KEY
                        if (apiKey.isBlank() || apiKey == "your_gemini_api_key_here") {
                            snackbarHostState.showSnackbar(
                                "⚠️ Add your GEMINI_API_KEY to local.properties"
                            )
                            bitmap // fall back to original
                        } else {
                            val result = GeminiImageStyler.stylize(
                                inputBitmap = bitmap,
                                style = aiStyle,
                                apiKey = apiKey
                            )
                            aiStyledBitmap = result.styledBitmap
                            result.styledBitmap
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(
                            "AI stylization failed: ${e.message?.take(80)}"
                        )
                        bitmap // fall back to original on error
                    } finally {
                        isAiProcessing = false
                        statusText = "Preprocessing image..."
                    }
                } else {
                    aiStyledBitmap = null
                    bitmap
                }

                // Step A: ImageProcessor
                val preprocessConfig = ImageProcessor.PreprocessConfig(
                    maxDimension = maxDimension
                )
                val preprocessResult = ImageProcessor.preprocess(pipelineInput, preprocessConfig)

                // Step B: ContourExtractor
                statusText = "Extracting contours..."
                val contourConfig = ContourExtractor.Config(
                    mode = extractionMode,
                    cannyThreshold1 = cannyLow,
                    cannyThreshold2 = cannyHigh,
                    adaptiveBlockSize = adaptiveBlockSize,
                    adaptiveC = adaptiveC
                )
                val contourResult = ContourExtractor.extractContours(preprocessResult.enhancedMat, contourConfig)
                edgeBitmap = contourResult.edgeBitmap

                // Step C: PathSimplifier
                statusText = "Simplifying & ordering paths..."
                val simplifierConfig = PathSimplifier.Config(
                    epsilon = epsilon,
                    minPoints = minPoints,
                    minArcLength = 6.0f,
                    optimizeOrder = true
                )
                val finalStrokes = PathSimplifier.process(contourResult.rawStrokes, simplifierConfig)

                currentSketch = Sketch(
                    width = contourResult.width,
                    height = contourResult.height,
                    strokes = finalStrokes
                )

                preprocessResult.close()
            } catch (e: Throwable) {
                e.printStackTrace()
                snackbarHostState.showSnackbar("Error processing image: ${e.message}")
            } finally {
                isProcessing = false
                statusText = ""
            }
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                currentBitmap = bitmap
                runPipeline(bitmap)
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to open image: ${e.message}")
                }
            }
        }
    }

    // Load initial sample if empty
    LaunchedEffect(Unit) {
        if (currentBitmap == null) {
            val sample = SampleImages.createSampleLogo(400)
            currentBitmap = sample
            runPipeline(sample)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "InstaDoodle",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pick image action card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Select Source Image",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Image")
                        }

                        OutlinedButton(
                            onClick = {
                                val star = SampleImages.createSampleStar(400)
                                currentBitmap = star
                                runPipeline(star)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Star")
                        }

                        OutlinedButton(
                            onClick = {
                                val logo = SampleImages.createSampleLogo(400)
                                currentBitmap = logo
                                runPipeline(logo)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Logo")
                        }
                    }
                }
            }

            // ── AI Stylization Card ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (useAiStylization)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header row with toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (useAiStylization)
                                            MaterialTheme.colorScheme.secondary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (useAiStylization)
                                        MaterialTheme.colorScheme.onSecondary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "2. AI Pre-Stylization",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Gemini simplifies the photo before OpenCV",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        androidx.compose.material3.Switch(
                            checked = useAiStylization,
                            onCheckedChange = { enabled ->
                                useAiStylization = enabled
                                if (!enabled) aiStyledBitmap = null
                                currentBitmap?.let { runPipeline(it) }
                            }
                        )
                    }

                    // Style chips — only visible when toggled on
                    AnimatedVisibility(visible = useAiStylization) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Output Style",
                                style = MaterialTheme.typography.labelLarge
                            )
                            // Row 1
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GeminiImageStyler.Style.entries.forEach { style ->
                                    FilterChip(
                                        selected = aiStyle == style,
                                        onClick = {
                                            if (aiStyle != style) {
                                                aiStyle = style
                                                currentBitmap?.let { runPipeline(it) }
                                            }
                                        },
                                        label = { Text(style.label) },
                                        leadingIcon = if (aiStyle == style) {
                                            { Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                }
                            }

                            // AI processing indicator
                            if (isAiProcessing) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Image Preview Tabs (Source / ✨ AI Styled / Edge Map)
            if (currentBitmap != null) {
                val tabCount = if (useAiStylization) 3 else 2
                var selectedPreviewTab by remember { mutableIntStateOf(0) }
                // Keep selected tab in range if AI toggle changes
                if (selectedPreviewTab >= tabCount) selectedPreviewTab = 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        TabRow(selectedTabIndex = selectedPreviewTab) {
                            Tab(
                                selected = selectedPreviewTab == 0,
                                onClick = { selectedPreviewTab = 0 },
                                text = { Text("Source") }
                            )
                            if (useAiStylization) {
                                Tab(
                                    selected = selectedPreviewTab == 1,
                                    onClick = { selectedPreviewTab = 1 },
                                    text = { Text("✨ AI Styled") }
                                )
                            }
                            Tab(
                                selected = selectedPreviewTab == tabCount - 1,
                                onClick = { selectedPreviewTab = tabCount - 1 },
                                text = { Text("Edge Map") }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                selectedPreviewTab == 0 -> {
                                    Image(
                                        bitmap = currentBitmap!!.asImageBitmap(),
                                        contentDescription = "Source Bitmap",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                useAiStylization && selectedPreviewTab == 1 -> {
                                    val styledBm = aiStyledBitmap
                                    if (styledBm != null) {
                                        Image(
                                            bitmap = styledBm.asImageBitmap(),
                                            contentDescription = "AI Styled Bitmap",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = if (isAiProcessing) "Generating…" else "Enable AI stylization and apply",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                selectedPreviewTab == tabCount - 1 && edgeBitmap != null -> {
                                    Image(
                                        bitmap = edgeBitmap!!.asImageBitmap(),
                                        contentDescription = "Edge Detection Bitmap",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            if (isProcessing) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(statusText, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }


            // Live Stroke Statistics Card
            currentSketch?.let { sketch ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${sketch.strokeCount}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Strokes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${sketch.totalPointCount}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Total Points",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${sketch.width}x${sketch.height}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Resolution",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Processing Pipeline Controls Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3. Contour & Path Parameters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { currentBitmap?.let { runPipeline(it) } },
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Apply")
                        }
                    }

                    // Mode switch: Photo vs Logo
                    Text("Detection Mode", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = extractionMode == ContourExtractor.Mode.PHOTO,
                            onClick = {
                                extractionMode = ContourExtractor.Mode.PHOTO
                                currentBitmap?.let { runPipeline(it) }
                            },
                            label = { Text("Photo (Canny)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                        FilterChip(
                            selected = extractionMode == ContourExtractor.Mode.LOGO_LINE_ART,
                            onClick = {
                                extractionMode = ContourExtractor.Mode.LOGO_LINE_ART
                                currentBitmap?.let { runPipeline(it) }
                            },
                            label = { Text("Logo / Line Art (Adaptive)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    // Max size slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Max Dimension", style = MaterialTheme.typography.bodyMedium)
                            Text("${maxDimension}px", fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = maxDimension.toFloat(),
                            onValueChange = { maxDimension = it.roundToInt() },
                            valueRange = 128f..1024f,
                            steps = 6
                        )
                    }

                    // Mode-specific sliders
                    if (extractionMode == ContourExtractor.Mode.PHOTO) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Canny Low Threshold", style = MaterialTheme.typography.bodyMedium)
                                Text("${cannyLow.roundToInt()}", fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = cannyLow.toFloat(),
                                onValueChange = { cannyLow = it.toDouble() },
                                valueRange = 10f..200f
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Canny High Threshold", style = MaterialTheme.typography.bodyMedium)
                                Text("${cannyHigh.roundToInt()}", fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = cannyHigh.toFloat(),
                                onValueChange = { cannyHigh = it.toDouble() },
                                valueRange = 40f..350f
                            )
                        }
                    } else {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Adaptive Block Size", style = MaterialTheme.typography.bodyMedium)
                                Text("${adaptiveBlockSize}", fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = adaptiveBlockSize.toFloat(),
                                onValueChange = {
                                    val rounded = it.roundToInt()
                                    adaptiveBlockSize = if (rounded % 2 == 0) rounded + 1 else rounded
                                },
                                valueRange = 3f..31f
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Adaptive C Offset", style = MaterialTheme.typography.bodyMedium)
                                Text(String.format("%.1f", adaptiveC), fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = adaptiveC.toFloat(),
                                onValueChange = { adaptiveC = it.toDouble() },
                                valueRange = -5f..15f
                            )
                        }
                    }

                    // Simplification epsilon
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Simplification Epsilon (px)", style = MaterialTheme.typography.bodyMedium)
                            Text(String.format("%.2f px", epsilon), fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = epsilon,
                            onValueChange = { epsilon = it },
                            valueRange = 0.5f..8.0f
                        )
                    }

                    // Min stroke length / points
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Min Points Per Stroke", style = MaterialTheme.typography.bodyMedium)
                            Text("$minPoints points", fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = minPoints.toFloat(),
                            onValueChange = { minPoints = it.roundToInt() },
                            valueRange = 3f..30f,
                            steps = 26
                        )
                    }
                }
            }

            // Primary Action Buttons
            currentSketch?.let { sketch ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val saved = SketchStorage.saveSketch(context, sketch)
                            scope.launch {
                                if (saved) {
                                    snackbarHostState.showSnackbar("Sketch saved as current_sketch.json")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to save sketch")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Sketch")
                    }

                    Button(
                        onClick = { onNavigateToPreview(sketch) },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preview & Replay")
                    }
                }
            }
        }
    }
}
