package Sakuin.Project.data.remote

import java.io.IOException
import java.io.InterruptedIOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.encoding.Base64
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * Reads a receipt photo with the Gemini API (`generateContent` + a JSON response schema).
 * The key comes from `local.properties` through BuildConfig; it is sent as a header, never in the URL.
 */
class GeminiReceiptClient(
    private val apiKey: String,
    private val httpClient: OkHttpClient = defaultHttpClient(),
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    private val model: String = DEFAULT_MODEL,
    /** Waits before retrying a server error; one retry per entry. */
    private val retryDelaysMillis: List<Long> = listOf(1_000, 2_000),
) {
    suspend fun scan(jpeg: ByteArray, categories: List<String>, today: LocalDate): ReceiptDto {
        if (apiKey.isBlank()) throw ScanException(ScanError.NotConfigured, "GEMINI_API_KEY is empty")
        val request = Request.Builder()
            .url("$baseUrl/models/$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(buildRequestBody(jpeg, categories, today).toString().toRequestBody(JsonMediaType))
            .build()
        var attempt = 0
        while (true) {
            try {
                val (code, body) = execute(request)
                return parseResponse(code, body)
            } catch (e: ScanException) {
                if (e.error != ScanError.Server || attempt >= retryDelaysMillis.size) throw e
                delay(retryDelaysMillis[attempt++])
            }
        }
    }

    /** Runs the call off the main thread and cancels it when the coroutine is cancelled. */
    private suspend fun execute(request: Request): Pair<Int, String> = suspendCancellableCoroutine { continuation ->
        val call = httpClient.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e.toScanException())
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val body = response.use { it.body.string() }
                        continuation.resume(response.code to body)
                    } catch (e: IOException) {
                        continuation.resumeWithException(e.toScanException())
                    }
                }
            },
        )
    }

    companion object {
        /** Current stable Flash model (checked against the Gemini API docs, Oct 2026). */
        const val DEFAULT_MODEL = "gemini-3.8-flash"

        private val JsonMediaType = "application/json; charset=utf-8".toMediaType()

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}

private val ResponseJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    // A null where the DTO has a default (e.g. "items": null) falls back to that default.
    coerceInputValues = true
}

private fun IOException.toScanException(): ScanException = ScanException(
    // OkHttp reports both socket and whole-call timeouts as InterruptedIOException.
    if (this is InterruptedIOException) ScanError.Timeout else ScanError.Network,
    message,
    this,
)

internal fun buildPrompt(categories: List<String>, today: LocalDate): String = """
    You read shopping receipts from Indonesia for a money-tracking app. Fill in the JSON fields from the photo.
    - isReceipt: false when the photo is not a receipt, invoice or bill; then leave the other fields null or empty.
    - total: the final amount paid (TOTAL, Grand Total, Jumlah, Total Bayar, Tagihan) in whole rupiah. Receipts use "." or "," between thousands, so "Rp 43.000" is 43000. Never use the cash handed over (Tunai, Cash, Bayar) or the change (Kembali, Kembalian).
    - merchant: the store or restaurant name as printed.
    - date and time: when the purchase happened, as yyyy-MM-dd and HH:mm (24-hour); null when not printed. Today is $today and receipts are never dated in the future.
    - category: the best fit among: ${categories.joinToString(", ")}.
    - suggestedName: a short name for this expense, at most 40 characters, in the receipt's language: the item itself when there is one main item, otherwise the merchant name.
    - items: every purchased line with its name, quantity and line total in rupiah. Include printed tax or service lines. Skip payment and change lines.
""".trimIndent()

/** The `generateContent` request: prompt + photo, answered as JSON that matches [ReceiptDto]. */
internal fun buildRequestBody(jpeg: ByteArray, categories: List<String>, today: LocalDate): JsonObject = buildJsonObject {
    putJsonArray("contents") {
        addJsonObject {
            put("role", "user")
            putJsonArray("parts") {
                addJsonObject { put("text", buildPrompt(categories, today)) }
                addJsonObject {
                    putJsonObject("inlineData") {
                        put("mimeType", "image/jpeg")
                        put("data", Base64.encode(jpeg))
                    }
                }
            }
        }
    }
    putJsonObject("generationConfig") {
        put("responseMimeType", "application/json")
        putJsonObject("responseSchema") { receiptSchema(categories) }
        // Reading a receipt needs little reasoning; a low level keeps the scan quick.
        putJsonObject("thinkingConfig") { put("thinkingLevel", "LOW") }
    }
}

private fun JsonObjectBuilder.receiptSchema(categories: List<String>) {
    put("type", "OBJECT")
    putJsonObject("properties") {
        putJsonObject("isReceipt") { put("type", "BOOLEAN") }
        putJsonObject("merchant") { field("STRING", nullable = true) }
        putJsonObject("total") { field("INTEGER", nullable = true) }
        putJsonObject("date") { field("STRING", nullable = true, description = "yyyy-MM-dd") }
        putJsonObject("time") { field("STRING", nullable = true, description = "HH:mm, 24-hour") }
        putJsonObject("category") {
            field("STRING", nullable = true)
            if (categories.isNotEmpty()) putJsonArray("enum") { categories.forEach { add(it) } }
        }
        putJsonObject("suggestedName") { field("STRING", nullable = true) }
        putJsonObject("items") {
            put("type", "ARRAY")
            putJsonObject("items") {
                put("type", "OBJECT")
                putJsonObject("properties") {
                    putJsonObject("name") { put("type", "STRING") }
                    putJsonObject("quantity") { field("INTEGER", nullable = true) }
                    putJsonObject("price") { field("INTEGER", nullable = true, description = "Line total in rupiah") }
                }
                putJsonArray("required") { add("name") }
            }
        }
    }
    val order = listOf("isReceipt", "merchant", "total", "date", "time", "category", "suggestedName", "items")
    putJsonArray("required") { order.forEach { add(it) } }
    putJsonArray("propertyOrdering") { order.forEach { add(it) } }
}

private fun JsonObjectBuilder.field(type: String, nullable: Boolean = false, description: String? = null) {
    put("type", type)
    if (nullable) put("nullable", true)
    if (description != null) put("description", description)
}

@Serializable
private data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList(),
    val promptFeedback: PromptFeedback? = null,
)

@Serializable
private data class Candidate(val content: Content? = null, val finishReason: String? = null)

@Serializable
private data class Content(val parts: List<Part> = emptyList())

@Serializable
private data class Part(val text: String? = null, val thought: Boolean = false)

@Serializable
private data class PromptFeedback(val blockReason: String? = null)

@Serializable
private data class ErrorResponse(val error: ErrorBody? = null)

@Serializable
private data class ErrorBody(val code: Int? = null, val message: String? = null, val status: String? = null)

/** Maps an HTTP status and body to a [ReceiptDto], or throws a [ScanException] saying what went wrong. */
internal fun parseResponse(code: Int, body: String): ReceiptDto {
    if (code !in 200..299) throw ScanException(errorFor(code, body), "HTTP $code: ${errorMessage(body)}")
    val response = try {
        ResponseJson.decodeFromString<GenerateContentResponse>(body)
    } catch (e: SerializationException) {
        throw ScanException(ScanError.Unreadable, "Unexpected response", e)
    }
    response.promptFeedback?.blockReason?.let { throw ScanException(ScanError.Unreadable, "Blocked: $it") }
    val candidate = response.candidates.firstOrNull() ?: throw ScanException(ScanError.Unreadable, "No candidates")
    val text = candidate.content?.parts.orEmpty().filterNot { it.thought }.mapNotNull { it.text }.joinToString("")
    if (text.isBlank()) throw ScanException(ScanError.Unreadable, "Empty answer (${candidate.finishReason})")
    val receipt = try {
        ResponseJson.decodeFromString<ReceiptDto>(text)
    } catch (e: SerializationException) {
        throw ScanException(ScanError.Unreadable, "Answer is not receipt JSON", e)
    } catch (e: IllegalArgumentException) {
        throw ScanException(ScanError.Unreadable, "Answer is not receipt JSON", e)
    }
    if (!receipt.isReceipt) throw ScanException(ScanError.NotReceipt)
    return receipt
}

private fun errorFor(code: Int, body: String): ScanError = when (code) {
    401, 403, 404 -> ScanError.NotConfigured
    // Bad or missing key, or the free tier is unavailable for the key's project.
    400 -> if ("API_KEY" in body || "FAILED_PRECONDITION" in body) ScanError.NotConfigured else ScanError.Unreadable
    402, 429 -> ScanError.Busy
    408, 504 -> ScanError.Timeout
    in 500..599 -> ScanError.Server
    else -> ScanError.Unreadable
}

private fun errorMessage(body: String): String =
    runCatching { ResponseJson.decodeFromString<ErrorResponse>(body).error?.message }.getOrNull() ?: body.take(200)
