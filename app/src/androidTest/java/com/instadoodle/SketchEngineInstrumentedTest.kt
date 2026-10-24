package com.instadoodle

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.instadoodle.engine.EngineConfig
import com.instadoodle.engine.ImageLoader
import com.instadoodle.engine.SketchEngine
import com.instadoodle.engine.SketchMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the full OpenCV + Kotlin pipeline on the bundled samples on a device/emulator. */
@RunWith(AndroidJUnit4::class)
class SketchEngineInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun sketch(asset: String, mode: SketchMode, maxStrokes: Int = 250) = runBlocking {
        val bitmap = ImageLoader.fromAsset(context, "samples/$asset")
        SketchEngine.createSketch(bitmap, EngineConfig(mode = mode, maxStrokes = maxStrokes))
    }

    private fun assertValid(asset: String, mode: SketchMode) {
        val strokes = sketch(asset, mode)
        assertTrue("$asset produced no strokes", strokes.isNotEmpty())
        assertTrue("$asset exceeded cap", strokes.size <= 250)
        strokes.flatMap { it.points }.forEach {
            assertTrue("$asset point out of range: $it", it.x in 0f..1f && it.y in 0f..1f)
        }
    }

    @Test fun logoLineArt() = assertValid("logo.png", SketchMode.LINE_ART)

    @Test fun portraitPhoto() = assertValid("portrait.jpg", SketchMode.PHOTO)

    @Test fun handwritingLineArt() = assertValid("handwriting.png", SketchMode.LINE_ART)

    @Test fun respectsStrokeCap() {
        assertTrue(sketch("portrait.jpg", SketchMode.PHOTO, maxStrokes = 20).size <= 20)
    }
}
