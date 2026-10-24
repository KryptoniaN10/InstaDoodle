package com.instadoodle.data

import com.instadoodle.engine.Point
import com.instadoodle.engine.Stroke
import org.json.JSONArray
import org.json.JSONObject

/** A saved set of normalized strokes. */
data class Sketch(
    val id: String,
    val name: String,
    val createdAt: Long,
    val strokes: List<Stroke>,
)

/** Compact JSON: each stroke is a flat [x0, y0, x1, y1, ...] array. */
object SketchJson {

    fun encode(sketch: Sketch): String = JSONObject().apply {
        put("id", sketch.id)
        put("name", sketch.name)
        put("createdAt", sketch.createdAt)
        put("strokes", JSONArray().apply {
            sketch.strokes.forEach { stroke ->
                put(JSONArray().apply {
                    stroke.points.forEach { put(it.x.toDouble()); put(it.y.toDouble()) }
                })
            }
        })
    }.toString()

    fun decode(json: String): Sketch {
        val obj = JSONObject(json)
        val strokesJson = obj.getJSONArray("strokes")
        val strokes = (0 until strokesJson.length()).map { i ->
            val flat = strokesJson.getJSONArray(i)
            Stroke((0 until flat.length() / 2).map { p ->
                Point(flat.getDouble(p * 2).toFloat(), flat.getDouble(p * 2 + 1).toFloat())
            })
        }.filter { it.points.isNotEmpty() }
        return Sketch(obj.getString("id"), obj.getString("name"), obj.getLong("createdAt"), strokes)
    }
}
