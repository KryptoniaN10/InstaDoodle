package com.instadoodle.engine

/**
 * Orders strokes to minimise pen-up travel: greedy nearest neighbour (strokes may be
 * reversed) followed by optional 2-opt passes. Never returns a worse order than the input.
 */
object StrokeOrderer {

    fun order(strokes: List<Stroke>, twoOptPasses: Int = 2): List<Stroke> {
        if (strokes.size < 2) return strokes
        var ordered = greedy(strokes)
        repeat(twoOptPasses) {
            val improved = twoOpt(ordered)
            if (improved === ordered) return@repeat
            ordered = improved
        }
        return if (travelDistance(ordered) <= travelDistance(strokes)) ordered else strokes
    }

    private fun greedy(strokes: List<Stroke>): List<Stroke> {
        val remaining = strokes.toMutableList()
        val result = ArrayList<Stroke>(strokes.size)
        var pen = Point(0f, 0f)
        while (remaining.isNotEmpty()) {
            var bestIndex = 0
            var bestReversed = false
            var bestDist = Float.MAX_VALUE
            remaining.forEachIndexed { i, s ->
                val dStart = pen.distanceTo(s.start)
                val dEnd = pen.distanceTo(s.end)
                if (dStart < bestDist) { bestDist = dStart; bestIndex = i; bestReversed = false }
                if (dEnd < bestDist) { bestDist = dEnd; bestIndex = i; bestReversed = true }
            }
            val next = remaining.removeAt(bestIndex).let { if (bestReversed) it.reversed() else it }
            result += next
            pen = next.end
        }
        return result
    }

    /**
     * One 2-opt pass: reversing the segment i..j (and each stroke inside it) changes only the
     * two connecting travel edges. Returns the same list instance if nothing improved.
     */
    private fun twoOpt(strokes: List<Stroke>): List<Stroke> {
        val s = strokes.toMutableList()
        var improved = false
        for (i in 1 until s.size - 1) {
            for (j in i + 1 until s.size) {
                val before = s[i - 1].end.distanceTo(s[i].start) +
                    if (j + 1 < s.size) s[j].end.distanceTo(s[j + 1].start) else 0f
                val after = s[i - 1].end.distanceTo(s[j].end) +
                    if (j + 1 < s.size) s[i].start.distanceTo(s[j + 1].start) else 0f
                if (after + 1e-4f < before) {
                    val segment = s.subList(i, j + 1).asReversed().map { it.reversed() }
                    for (k in segment.indices) s[i + k] = segment[k]
                    improved = true
                }
            }
        }
        return if (improved) s else strokes
    }
}
