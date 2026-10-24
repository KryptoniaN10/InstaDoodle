package com.instadoodle.app.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Uses the Gemini image-generation model to convert an arbitrary photo into
 * clean black-on-white line art before the OpenCV contour pipeline runs.
 *
 * This dramatically improves doodle quality for complex photos because the AI
 * strips colour/texture/noise and leaves only semantically-meaningful outlines —
 * something Canny edge detection cannot do on its own.
 */
object AiImagePreprocessor {

    private const val TAG = "AiImagePreprocessor"

    /**
     * Gemini model that supports image input + image output.
     * Swap to "gemini-2.0-pro-preview-image-generation" for higher quality at the cost of latency.
     */
    private const val MODEL = "gemini-2.0-flash-preview-image-generation"

    private const val API_BASE =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    /** Prompt tuned to produce output that Canny/adaptive-threshold handles well. */
    private val DOODLE_PROMPT = """
        Convert this image into clean black line art on a pure white background.
        Remove all colour, texture, shading, and gradients.
        Keep only essential outlines and silhouettes.
        Make it look like a simple coloring-book line drawing —
        minimal, high-contrast, easy to trace as a vector sketch.
        Output ONLY the line-art image, no text, no watermarks.
    """.trimIndent().replace('\n', ' ')

    /** Maximum JPEG dimension sent to the API (keeps request size reasonable). */
    private const val MAX_INPUT_SIDE = 1024

    /**
     * Sends [bitmap] to the Gemini API and returns a new [Bitmap] that contains
     * the AI-generated line-art version, ready to be fed into [ImageProcessor].
     *
     * @param bitmap  Source image. Can be any resolution; will be downscaled if needed.
     * @param apiKey  Gemini API key (from https://aistudio.google.com/app/apikey).
     * @return [Result.success] with the processed bitmap, or [Result.failure] with an error.
     */
    suspend fun generateDoodleFriendlyImage(
        bitmap: Bitmap,
        apiKey: String
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val scaled = scaledForApi(bitmap)
            val base64 = bitmapToBase64(scaled)
            val requestBody = buildRequestJson(base64)

            val url = URL("$API_BASE?key=$apiKey")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 30_000
                readTimeout = 90_000   // image gen can take up to ~60 s
            }

            connection.outputStream.bufferedWriter().use { it.write(requestBody) }

            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val err = connection.errorStream?.bufferedReader()?.readText() ?: "(no body)"
                Log.e(TAG, "API returned HTTP $code: $err")
                return@withContext Result.failure(Exception("Gemini API error $code: $err"))
            }

            val responseText = connection.inputStream.bufferedReader().readText()
            val resultBitmap = parseImageFromResponse(responseText)
                ?: return@withContext Result.failure(Exception("No image part in Gemini response"))

            Log.i(TAG, "AI preprocessing succeeded: ${resultBitmap.width}x${resultBitmap.height}")
            Result.success(resultBitmap)

        } catch (e: Exception) {
            Log.e(TAG, "AI preprocessing failed", e)
            Result.failure(e)
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Downscale [src] so neither side exceeds [MAX_INPUT_SIDE], preserving aspect ratio. */
    private fun scaledForApi(src: Bitmap): Bitmap {
        val max = maxOf(src.width, src.height)
        if (max <= MAX_INPUT_SIDE) return src
        val scale = MAX_INPUT_SIDE.toFloat() / max
        val w = (src.width * scale).toInt().coerceAtLeast(1)
        val h = (src.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Builds the JSON request body manually to avoid introducing additional
     * serialization classes; all fields are straightforward ASCII.
     */
    private fun buildRequestJson(base64Image: String): String {
        val escapedPrompt = DOODLE_PROMPT
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        return buildString {
            append("""{"contents":[{"parts":[""")
            append("""{"text":"$escapedPrompt"},""")
            append("""{"inline_data":{"mime_type":"image/jpeg","data":"$base64Image"}}""")
            append("""]}],"generationConfig":{"responseModalities":["IMAGE"]}}""")
        }
    }

    /**
     * Extracts the first inlineData image part from the Gemini response JSON
     * and decodes it into a [Bitmap].
     */
    private fun parseImageFromResponse(responseBody: String): Bitmap? {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(responseBody).jsonObject
            val candidates = root["candidates"]?.jsonArray ?: return null
            val parts = candidates
                .firstOrNull()?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray
                ?: return null

            for (part in parts) {
                val inlineData = part.jsonObject["inlineData"]?.jsonObject ?: continue
                val data = inlineData["data"]?.jsonPrimitive?.content ?: continue
                val bytes = Base64.decode(data, Base64.NO_WRAP)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) return bmp
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Response parse error", e)
            null
        }
    }
}
