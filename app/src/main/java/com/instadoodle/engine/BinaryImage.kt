package com.instadoodle.engine

/** Row-major binary image; `true` = ink pixel. Pure Kotlin so the engine is JVM-testable. */
class BinaryImage(val width: Int, val height: Int, val pixels: BooleanArray = BooleanArray(width * height)) {
    init {
        require(width > 0 && height > 0) { "Image must not be empty" }
        require(pixels.size == width * height) { "Pixel buffer size mismatch" }
    }

    operator fun get(x: Int, y: Int): Boolean =
        x in 0 until width && y in 0 until height && pixels[y * width + x]

    operator fun set(x: Int, y: Int, value: Boolean) {
        pixels[y * width + x] = value
    }

    fun inkCount(): Int = pixels.count { it }

    fun copy(): BinaryImage = BinaryImage(width, height, pixels.copyOf())

    companion object {
        /** Builds a binary image from 8-bit gray values; pixels darker than [threshold] are ink. */
        fun fromGray(width: Int, height: Int, gray: IntArray, threshold: Int): BinaryImage =
            BinaryImage(width, height, BooleanArray(gray.size) { gray[it] < threshold })
    }
}
