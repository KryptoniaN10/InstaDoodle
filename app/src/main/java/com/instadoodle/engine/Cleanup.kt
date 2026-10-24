package com.instadoodle.engine

/** Noise removal on binary images and skeletons, plus stroke smoothing. */
object Cleanup {

    private val DX = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
    private val DY = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)

    /** Clears 8-connected ink blobs smaller than [minPixels] (speckles that would become junk strokes). */
    fun removeSmallComponents(img: BinaryImage, minPixels: Int): BinaryImage {
        if (minPixels <= 1) return img
        val out = img.copy()
        val w = img.width
        val seen = BooleanArray(w * img.height)
        val stack = IntArray(w * img.height)
        val component = ArrayList<Int>()
        for (start in seen.indices) {
            if (!img.pixels[start] || seen[start]) continue
            component.clear()
            var top = 0
            stack[top++] = start
            seen[start] = true
            while (top > 0) {
                val i = stack[--top]
                component += i
                val x = i % w
                val y = i / w
                for (d in 0 until 8) {
                    val nx = x + DX[d]
                    val ny = y + DY[d]
                    if (!img[nx, ny]) continue
                    val j = ny * w + nx
                    if (!seen[j]) {
                        seen[j] = true
                        stack[top++] = j
                    }
                }
            }
            if (component.size < minPixels) component.forEach { out.pixels[it] = false }
        }
        return out
    }

    /**
     * Removes skeleton spurs: branches shorter than [maxLength] px that run from a free
     * endpoint into a junction. Thinning leaves these at corners and on bumpy edges.
     */
    fun pruneSpurs(skeleton: BinaryImage, maxLength: Int): BinaryImage {
        if (maxLength <= 0) return skeleton
        val out = skeleton.copy()
        fun neighbours(x: Int, y: Int) = (0 until 8).count { out[x + DX[it], y + DY[it]] }
        val branch = ArrayList<Int>()
        for (y in 0 until out.height) for (x in 0 until out.width) {
            if (!out[x, y] || neighbours(x, y) != 1) continue
            branch.clear()
            var cx = x
            var cy = y
            var px = -1
            var py = -1
            var reachedJunction = false
            while (branch.size <= maxLength) {
                val n = neighbours(cx, cy)
                if (n >= 3 && branch.isNotEmpty()) {
                    reachedJunction = true
                    break
                }
                branch += cy * out.width + cx
                val next = (0 until 8).firstOrNull { d ->
                    val nx = cx + DX[d]
                    val ny = cy + DY[d]
                    out[nx, ny] && !(nx == px && ny == py) && (ny * out.width + nx) !in branch
                } ?: break
                px = cx
                py = cy
                cx += DX[next]
                cy += DY[next]
            }
            if (reachedJunction && branch.size <= maxLength) branch.forEach { out.pixels[it] = false }
        }
        return out
    }

    /** Moving-average smoothing that removes pixel staircase jitter; endpoints stay fixed. */
    fun smooth(stroke: Stroke, iterations: Int): Stroke {
        var pts = stroke.points
        if (pts.size < 3) return stroke
        repeat(iterations) {
            pts = pts.mapIndexed { i, p ->
                if (i == 0 || i == pts.lastIndex) {
                    p
                } else {
                    val a = pts[i - 1]
                    val b = pts[i + 1]
                    Point((a.x + 2 * p.x + b.x) / 4f, (a.y + 2 * p.y + b.y) / 4f)
                }
            }
        }
        return Stroke(pts)
    }
}
