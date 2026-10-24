package com.instadoodle.data

import com.instadoodle.engine.DemoShapes
import com.instadoodle.engine.Stroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The sketch currently loaded into the overlay (app and service share one process). */
object SketchSession {

    data class Active(val name: String, val strokes: List<Stroke>)

    private val _active = MutableStateFlow(Active("Demo: square + spiral", DemoShapes.demo()))
    val active: StateFlow<Active> = _active.asStateFlow()

    fun load(name: String, strokes: List<Stroke>) {
        _active.value = Active(name, strokes)
    }
}
