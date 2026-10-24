package com.instadoodle.engine

/** Cleans up traced strokes: merge near endpoints, drop tiny strokes, cap the count. */
object StrokeFilter {

    /** Joins strokes whose endpoints are within [maxGap], reversing strokes as needed. */
    fun mergeClose(strokes: List<Stroke>, maxGap: Float): List<Stroke> {
        val pool = strokes.toMutableList()
        val result = ArrayList<Stroke>()
        while (pool.isNotEmpty()) {
            var current = pool.removeAt(pool.lastIndex)
            var merged = true
            while (merged) {
                merged = false
                val iterator = pool.listIterator()
                while (iterator.hasNext()) {
                    val other = iterator.next()
                    val joined = join(current, other, maxGap) ?: continue
                    current = joined
                    iterator.remove()
                    merged = true
                }
            }
            result += current
        }
        return result
    }

    private fun join(a: Stroke, b: Stroke, maxGap: Float): Stroke? = when {
        a.end.distanceTo(b.start) <= maxGap -> Stroke(a.points + b.points)
        a.end.distanceTo(b.end) <= maxGap -> Stroke(a.points + b.points.asReversed())
        a.start.distanceTo(b.end) <= maxGap -> Stroke(b.points + a.points)
        a.start.distanceTo(b.start) <= maxGap -> Stroke(b.points.asReversed() + a.points)
        else -> null
    }

    fun dropShort(strokes: List<Stroke>, minLength: Float): List<Stroke> =
        strokes.filter { it.length >= minLength }

    /** Keeps the [maxStrokes] longest strokes. */
    fun capLongest(strokes: List<Stroke>, maxStrokes: Int): List<Stroke> =
        if (strokes.size <= maxStrokes) strokes else strokes.sortedByDescending { it.length }.take(maxStrokes)
}
