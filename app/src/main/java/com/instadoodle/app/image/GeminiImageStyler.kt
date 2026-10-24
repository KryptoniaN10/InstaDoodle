package com.instadoodle.app.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends an image to the Gemini multimodal image-generation model and returns a stylized
 * version suitable for clean contour extraction (line art, pencil sketch, cartoon, etc.).
 *
 * This is an optional first step in the InstaDoodle processing pipeline — transforming a
 * complex real-world photo into a simplified, high-contrast outline image that the OpenCV
 * stage can extract clean strokes from.
 */
object GeminiImageStyler {

    private const val TAG = "GeminiImageStyler"

    /**
     * The Gemini model capable of generating image output from an image+text prompt.
     */
    private const val MODEL_ID = "gemini-2.0-flash-preview-image-generation"
    private const val BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_ID:generateContent"

    /** Maximum pixel dimension of the image sent to the API (reduces token cost & latency). */
    private const val MAX_INPUT_DIM = 1024

    // ──────────────────────────────────────────────────────────────────────────
    // Style definitions
    // ──────────────────────────────────────────────────────────────────────────

    enum class Style(val label: String, val prompt: String) {
        LINE_ART(
            label = "Line Art",
            prompt = """
                Transform this image into clean, minimalist black-and-white line art.
                Use only crisp, thin outlines to define the main shapes and silhouettes.
                Remove all texture, colour gradients, shading, and background complexity.
                The result must be a simple coloring-book-style outline drawing on a pure
                white background, with clear, continuous black lines and nothing else.
                It should look easy to trace with a pen plotter.
            """.trimIndent()
        ),
        PENCIL_SKETCH(
            label = "Pencil Sketch",
            prompt = """
                Convert this image into a hand-drawn pencil sketch.
                Use visible pencil-stroke textures, light hatching only for major shadows,
                and clear outline strokes for all edges. Keep the sketch simple with
                well-defined lines on a white background.
                Avoid heavy shading — prioritise clean, traceable outlines.
            """.trimIndent()
        ),
        CARTOON(
            label = "Cartoon",
            prompt = """
                Simplify this image into a clean cartoon illustration.
                Use bold black outlines around every shape, simplified geometric forms,
                and minimal interior detail lines.
                White or very light background. Clear separation between foreground objects.
                Style: flat 2D cartoon suitable for vectorization and pen-plotting.
            """.trimIndent()
        ),
        FLAT_ILLUSTRATION(
            label = "Flat",
            prompt = """
                Convert this image into a modern flat vector illustration.
                Reduce all detail to simple geometric shapes with clean, bold black edges.
                White background, minimal interior lines, no gradients or textures.
                Optimised for clean contour extraction by an edge-detection algorithm.
            """.trimIndent()
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    data class StylerResult(
        val styledBitmap: Bitmap,
        val style: Style,
        /** Optional description text returned alongside the image by the model. */
        val modelNote: String
    )

    /**
     * Sends [inputBitmap] to the Gemini API and returns a stylized [StylerResult].
     *
     * @throws IllegalArgumentException if [apiKey] is blank.
     * @throws Exception on network or API errors (caller should handle and fallback).
     */
    suspend fun stylize(
        inputBitmap: Bitmap,
        style: Style,
        apiKey: String
    ): StylerResult = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) {
            "GEMINI_API_KEY is not configured — add it to local.properties"
        }

        Log.d(TAG, "Stylizing with style=${style.name} …")

        // 1. Scale input bitmap down to reduce upload size
        val scaledBitmap = scaleBitmap(inputBitmap, MAX_INPUT_DIM)

        // 2. Encode as JPEG base64 (NO_WRAP = no newlines in the string)
        val base64Image = bitmapToBase64(scaledBitmap)

        // 3. Build the JSON request body
        val requestBody = buildRequestJson(base64Image, style.prompt)

        // 4. Call the API
        val responseJson = postJson(
            url = "$BASE_URL?key=$apiKey",
            body = requestBody
        )

        // 5. Parse the response to extract the generated image
        val (bitmap, note) = parseResponse(responseJson)

        Log.d(TAG, "Stylization complete. modelNote='${note.take(80)}'")
        StylerResult(styledBitmap = bitmap, style = style, modelNote = note)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun buildRequestJson(base64Image: String, prompt: String): String {
        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                        put(
                            JSONObject().put(
                                "inline_data",
                                JSONObject().apply {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64Image)
                                }
                            )
                        )
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().apply {
                    put("IMAGE")
                    put("TEXT")
                })
            })
        }.toString()
    }

    /**
     * Parses the Gemini response JSON and returns the (image, text) pair.
     * Throws with a descriptive message if no image part is found.
     */
    private fun parseResponse(responseJson: String): Pair<Bitmap, String> {
        val root = JSONObject(responseJson)

        // Surface API-level errors early
        root.optJSONObject("error")?.let { err ->
            val code = err.optInt("code", -1)
            val msg  = err.optString("message", "Unknown Gemini API error")
            error("Gemini API error $code: $msg")
        }

        val candidates = root.optJSONArray("candidates")
            ?: error("Gemini response has no 'candidates'.\nBody: ${responseJson.take(400)}")

        val content = candidates
            .getJSONObject(0)
            .optJSONObject("content")
            ?: error("Gemini candidate has no 'content'")

        val parts = content.optJSONArray("parts")
            ?: error("Gemini content has no 'parts'")

        var imageBitmap: Bitmap? = null
        var textNote = ""

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            when {
                part.has("inline_data") -> {
                    val inlineData = part.getJSONObject("inline_data")
                    val bytes = Base64.decode(inlineData.getString("data"), Base64.DEFAULT)
                    imageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: error("Failed to decode Gemini image bytes")
                }
                part.has("text") -> {
                    textNote = part.getString("text")
                }
            }
        }

        return Pair(
            imageBitmap ?: error(
                "Gemini response contained no image part.\n" +
                "Check that the model supports image output for this key/region.\n" +
                "Parts received: $parts"
            ),
            textNote
        )
    }

    /**
     * Makes a blocking POST request (must be called on IO dispatcher) and returns the
     * response body as a string.
     */
    private fun postJson(url: String, body: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            doOutput = true
            connectTimeout = 30_000
            readTimeout   = 120_000  // image generation can take up to ~60 s
        }
        return try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { w ->
                w.write(body)
                w.flush()
            }

            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.readText()
                    ?: "No error body"
                error("HTTP $code from Gemini API: $errorBody")
            }

            connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } finally {
            connection.disconnect()
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDim) return bitmap
        val scale = maxDim.toFloat() / maxSide
        val w = (bitmap.width  * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }
}
