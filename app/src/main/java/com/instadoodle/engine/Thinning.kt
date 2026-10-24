package com.instadoodle.engine

/**
 * Zhang-Suen thinning: reduces ink regions to 1px-wide skeletons so each line
 * becomes a single stroke instead of a doubled outline.
 */
object Thinning {

    fun thin(input: BinaryImage): BinaryImage {
        val img = input.copy()
        val toClear = ArrayList<Int>()
        var changed = true
        while (changed) {
            changed = false
            for (step in 0..1) {
                toClear.clear()
                for (y in 0 until img.height) {
                    for (x in 0 until img.width) {
                        if (img[x, y] && shouldRemove(img, x, y, step)) toClear.add(y * img.width + x)
                    }
                }
                if (toClear.isNotEmpty()) {
                    changed = true
                    toClear.forEach { img.pixels[it] = false }
                }
            }
        }
        return img
    }

    private fun shouldRemove(img: BinaryImage, x: Int, y: Int, step: Int): Boolean {
        // Neighbours P2..P9 clockwise starting at north.
        val p = booleanArrayOf(
            img[x, y - 1], img[x + 1, y - 1], img[x + 1, y], img[x + 1, y + 1],
            img[x, y + 1], img[x - 1, y + 1], img[x - 1, y], img[x - 1, y - 1],
        )
        val neighbours = p.count { it }
        if (neighbours < 2 || neighbours > 6) return false
        var transitions = 0
        for (i in 0 until 8) if (!p[i] && p[(i + 1) % 8]) transitions++
        if (transitions != 1) return false
        val (p2, p4, p6, p8) = listOf(p[0], p[2], p[4], p[6])
        return if (step == 0) {
            !(p2 && p4 && p6) && !(p4 && p6 && p8)
        } else {
            !(p2 && p4 && p8) && !(p2 && p6 && p8)
        }
    }
}
