package com.instadoodle.engine

/**
 * Converts a 1px skeleton into polylines (in pixel coordinates) by walking the
 * 8-connected pixel graph. Walks start at endpoints and junctions first so open
 * lines become single strokes; remaining pixels belong to closed loops.
 */
object SkeletonTracer {

    private val DX = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
    private val DY = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)

    fun trace(skeleton: BinaryImage): List<Stroke> {
        val w = skeleton.width
        val visited = BooleanArray(w * skeleton.height)
        val strokes = ArrayList<Stroke>()

        fun neighbourCount(x: Int, y: Int) = (0 until 8).count { skeleton[x + DX[it], y + DY[it]] }

        // Pass 1: start from endpoints / junctions. Pass 2: leftover pixels (loops, isolated dots).
        for (pass in 0..1) {
            for (y in 0 until skeleton.height) {
                for (x in 0 until w) {
                    if (!skeleton[x, y] || visited[y * w + x]) continue
                    val n = neighbourCount(x, y)
                    if (pass == 0 && n == 2) continue
                    strokes += walkFrom(skeleton, visited, x, y)
                }
            }
        }
        return strokes
    }

    /** Follows unvisited neighbours greedily, preferring straight continuation. */
    private fun walkFrom(img: BinaryImage, visited: BooleanArray, sx: Int, sy: Int): Stroke {
        val w = img.width
        val pts = ArrayList<Point>()
        var x = sx
        var y = sy
        var lastDir = -1
        while (true) {
            visited[y * w + x] = true
            pts += Point(x.toFloat(), y.toFloat())
            val dir = nextDirection(img, visited, x, y, lastDir) ?: break
            x += DX[dir]
            y += DY[dir]
            lastDir = dir
        }
        return Stroke(pts)
    }

    private fun nextDirection(img: BinaryImage, visited: BooleanArray, x: Int, y: Int, lastDir: Int): Int? {
        var best: Int? = null
        var bestScore = Int.MAX_VALUE
        for (d in 0 until 8) {
            val nx = x + DX[d]
            val ny = y + DY[d]
            if (!img[nx, ny] || visited[ny * img.width + nx]) continue
            // Prefer the smallest turn; prefer 4-connected steps on ties.
            val turn = if (lastDir < 0) 0 else minOf((d - lastDir + 8) % 8, (lastDir - d + 8) % 8)
            val score = turn * 2 + (d % 2)
            if (score < bestScore) {
                bestScore = score
                best = d
            }
        }
        return best
    }
}
