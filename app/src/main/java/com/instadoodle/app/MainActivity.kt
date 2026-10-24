package com.instadoodle.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.instadoodle.app.drawing.Sketch
import com.instadoodle.app.image.ImageProcessor
import com.instadoodle.app.ui.ImagePickerScreen
import com.instadoodle.app.ui.SketchPreviewScreen
import com.instadoodle.app.ui.theme.InstaDoodleTheme

sealed interface Screen {
    data object ImagePicker : Screen
    data class SketchPreview(val sketch: Sketch) : Screen
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pre-initialize OpenCV asynchronously
        ImageProcessor.isAvailable()

        setContent {
            InstaDoodleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.ImagePicker) }

                    when (val screen = currentScreen) {
                        is Screen.ImagePicker -> {
                            ImagePickerScreen(
                                onNavigateToPreview = { sketch ->
                                    currentScreen = Screen.SketchPreview(sketch)
                                }
                            )
                        }

                        is Screen.SketchPreview -> {
                            SketchPreviewScreen(
                                sketch = screen.sketch,
                                onBack = {
                                    currentScreen = Screen.ImagePicker
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
