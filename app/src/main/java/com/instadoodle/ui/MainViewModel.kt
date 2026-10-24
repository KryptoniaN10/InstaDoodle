package com.instadoodle.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.instadoodle.data.DrawSettings
import com.instadoodle.data.SettingsStore
import com.instadoodle.data.Sketch
import com.instadoodle.data.SketchRepository
import com.instadoodle.engine.DemoShapes
import com.instadoodle.engine.EngineConfig
import com.instadoodle.engine.ImageLoader
import com.instadoodle.engine.SketchEngine
import com.instadoodle.engine.SketchException
import com.instadoodle.engine.SketchMode
import com.instadoodle.engine.Stroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class Screen { ONBOARDING, HOME, PREVIEW, LIBRARY, SETTINGS, TEST_CANVAS }

data class UiState(
    val screen: Screen = Screen.HOME,
    val sourceName: String? = null,
    val sourceBitmap: Bitmap? = null,
    val mode: SketchMode = SketchMode.PHOTO,
    val detail: Float = 0.5f,
    val maxStrokes: Int = 250,
    val processing: Boolean = false,
    val error: String? = null,
    val sketchName: String = "",
    val strokes: List<Stroke> = emptyList(),
    val library: List<Sketch> = emptyList(),
    val message: String? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = SketchRepository(app)
    private val settingsStore = SettingsStore.get(app)
    val settings: StateFlow<DrawSettings> = settingsStore.settings

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun navigate(screen: Screen) {
        _state.update { it.copy(screen = screen, error = null) }
        if (screen == Screen.LIBRARY) refreshLibrary()
    }

    fun pickImage(uri: Uri) = loadSource("Picked image") { ImageLoader.fromUri(getApplication(), uri) }

    fun useSample(fileName: String, mode: SketchMode) {
        _state.update { it.copy(mode = mode) }
        loadSource(fileName.substringBefore('.')) { ImageLoader.fromAsset(getApplication(), "samples/$fileName") }
    }

    fun setMode(mode: SketchMode) = _state.update { it.copy(mode = mode) }
    fun setDetail(detail: Float) = _state.update { it.copy(detail = detail) }
    fun setMaxStrokes(max: Int) = _state.update { it.copy(maxStrokes = max) }
    fun clearMessage() = _state.update { it.copy(message = null) }
    fun showMessage(text: String) = _state.update { it.copy(message = text) }

    fun generate() {
        val s = _state.value
        val bitmap = s.sourceBitmap ?: return _state.update { it.copy(error = "Pick an image first.") }
        _state.update { it.copy(processing = true, error = null) }
        viewModelScope.launch {
            val config = EngineConfig(
                mode = s.mode,
                detail = s.detail,
                maxStrokes = s.maxStrokes,
                minStrokeLengthPx = 4f + (1f - s.detail) * 16f,
            )
            try {
                val strokes = SketchEngine.createSketch(bitmap, config)
                _state.update {
                    it.copy(processing = false, strokes = strokes, sketchName = "${s.sourceName ?: "Sketch"} (${s.mode.name.lowercase()})", screen = Screen.PREVIEW)
                }
            } catch (e: SketchException) {
                _state.update { it.copy(processing = false, error = e.message) }
            } catch (e: OutOfMemoryError) {
                _state.update { it.copy(processing = false, error = "Not enough memory for this image.") }
            }
        }
    }

    fun openDemoShapes() = _state.update {
        it.copy(strokes = DemoShapes.demo(), sketchName = "Demo: square + spiral", screen = Screen.PREVIEW)
    }

    fun saveCurrent() {
        val s = _state.value
        if (s.strokes.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.save(Sketch(UUID.randomUUID().toString(), s.sketchName, System.currentTimeMillis(), s.strokes))
            }
            _state.update { it.copy(message = "Saved to Library") }
        }
    }

    fun openSketch(sketch: Sketch) = _state.update {
        it.copy(strokes = sketch.strokes, sketchName = sketch.name, screen = Screen.PREVIEW)
    }

    fun deleteSketch(sketch: Sketch) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.delete(sketch.id) }
            refreshLibrary()
        }
    }

    fun updateSettings(transform: (DrawSettings) -> DrawSettings) = settingsStore.update(transform)

    private fun refreshLibrary() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { repository.list() }
            _state.update { it.copy(library = list) }
        }
    }

    private fun loadSource(name: String, load: () -> Bitmap) {
        _state.update { it.copy(processing = true, error = null) }
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) { load() }
                _state.update { it.copy(processing = false, sourceBitmap = bitmap, sourceName = name) }
            } catch (e: SketchException) {
                _state.update { it.copy(processing = false, error = e.message) }
            }
        }
    }
}
