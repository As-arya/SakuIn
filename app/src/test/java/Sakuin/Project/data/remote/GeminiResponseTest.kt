package Sakuin.Project.data.remote

import java.time.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GeminiResponseTest {
    private fun answer(text: String, thought: String? = null): String {
        val thoughtPart = thought?.let { """{"text": ${JsonPrimitive(it)}, "thought": true},""" }.orEmpty()
        return """
            {"candidates": [{"content": {"role": "model", "parts": [$thoughtPart {"text": ${JsonPrimitive(text)}}]},
              "finishReason": "STOP"}], "usageMetadata": {"totalTokenCount": 812}}
        """.trimIndent()
    }

    private fun errorOf(block: () -> Unit): ScanError {
        try {
            block()
        } catch (e: ScanException) {
            return e.error
        }
        fail("Expected a ScanException")
        throw AssertionError()
    }

    @Test
    fun parsesTheReceiptJsonAndSkipsThoughts() {
        val receipt = parseResponse(
            200,
            answer(
                """{"isReceipt": true, "merchant": "Indomaret", "total": 27500, "date": "2026-10-22", "time": null,
                   "category": "Shopping", "suggestedName": "Belanja Indomaret", "items": null, "extra": 1}""",
                thought = "Looking at the total line…",
            ),
        )
        assertEquals("Indomaret", receipt.merchant)
        assertEquals(27_500L, receipt.total)
        assertEquals("Shopping", receipt.category)
        assertEquals(emptyList<ReceiptItemDto>(), receipt.items)
    }

    @Test
    fun notAReceipt() {
        assertEquals(ScanError.NotReceipt, errorOf { parseResponse(200, answer("""{"isReceipt": false, "items": []}""")) })
    }

    @Test
    fun emptyBlockedOrMalformedAnswersAreUnreadable() {
        assertEquals(ScanError.Unreadable, errorOf { parseResponse(200, """{"candidates": []}""") })
        assertEquals(ScanError.Unreadable, errorOf { parseResponse(200, """{"promptFeedback": {"blockReason": "OTHER"}}""") })
        assertEquals(ScanError.Unreadable, errorOf { parseResponse(200, """{"candidates": [{"finishReason": "SAFETY"}]}""") })
        assertEquals(ScanError.Unreadable, errorOf { parseResponse(200, answer("Sorry, I can't read this.")) })
        assertEquals(ScanError.Unreadable, errorOf { parseResponse(200, "<html>") })
    }

    @Test
    fun mapsHttpErrors() {
        val badKey = """{"error": {"code": 400, "message": "API key not valid.", "status": "INVALID_ARGUMENT",
            "details": [{"reason": "API_KEY_INVALID"}]}}"""
        assertEquals(ScanError.NotConfigured, errorOf { parseResponse(400, badKey) })
        assertEquals(ScanError.NotConfigured, errorOf { parseResponse(403, "{}") })
        assertEquals(ScanError.NotConfigured, errorOf { parseResponse(404, "{}") })
        assertEquals(ScanError.Unreadable, errorOf { parseResponse(400, """{"error": {"status": "INVALID_ARGUMENT"}}""") })
        assertEquals(ScanError.Busy, errorOf { parseResponse(429, """{"error": {"status": "RESOURCE_EXHAUSTED"}}""") })
        assertEquals(ScanError.Timeout, errorOf { parseResponse(504, "") })
        assertEquals(ScanError.Server, errorOf { parseResponse(503, """{"error": {"status": "UNAVAILABLE"}}""") })
    }

    @Test
    fun requestCarriesThePhotoSchemaAndCategories() {
        val body = buildRequestBody(byteArrayOf(1, 2, 3), listOf("Food", "Others"), LocalDate.of(2026, 10, 22))
        val parts = body["contents"]!!.jsonArray[0].jsonObject["parts"]!!.jsonArray
        assertTrue(parts[0].jsonObject["text"]!!.jsonPrimitive.content.contains("Today is 2026-10-22"))
        val image = parts[1].jsonObject["inlineData"]!!.jsonObject
        assertEquals("image/jpeg", image["mimeType"]!!.jsonPrimitive.content)
        assertEquals("AQID", image["data"]!!.jsonPrimitive.content)

        val config = body["generationConfig"]!!.jsonObject
        assertEquals("application/json", config["responseMimeType"]!!.jsonPrimitive.content)
        val properties = config["responseSchema"]!!.jsonObject["properties"]!!.jsonObject
        assertEquals(JsonArray(listOf(JsonPrimitive("Food"), JsonPrimitive("Others"))), properties["category"]!!.jsonObject["enum"])
        assertEquals("LOW", config["thinkingConfig"]!!.jsonObject["thinkingLevel"]!!.jsonPrimitive.content)
    }

    @Test
    fun schemaHasNoEnumWithoutCategories() {
        val body = buildRequestBody(byteArrayOf(), emptyList(), LocalDate.of(2026, 10, 22))
        val category = body["generationConfig"]!!.jsonObject["responseSchema"]!!.jsonObject["properties"]!!
            .jsonObject["category"] as JsonObject
        assertFalse("enum" in category)
    }
}
