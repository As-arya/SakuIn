package Sakuin.Project.data.remote

import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class GeminiReceiptClientTest {
    private val server = MockWebServer()
    private val today = LocalDate.of(2026, 10, 22)
    private val okAnswer = """{"candidates": [{"content": {"parts": [{"text": "{\"isReceipt\": true, \"total\": 43000}"}]}}]}"""

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun client(
        apiKey: String = "test-key",
        http: OkHttpClient = OkHttpClient(),
        models: List<String> = listOf("primary-model"),
    ) = GeminiReceiptClient(
        apiKey = apiKey,
        httpClient = http,
        baseUrl = server.url("/v1beta").toString().trimEnd('/'),
        models = models,
        retryDelaysMillis = listOf(0, 0),
    )

    private suspend fun scanError(client: GeminiReceiptClient): ScanError {
        try {
            client.scan(byteArrayOf(1), listOf("Food"), today)
        } catch (e: ScanException) {
            return e.error
        }
        fail("Expected a ScanException")
        throw AssertionError()
    }

    @Test
    fun sendsTheKeyAsAHeaderToTheModelEndpoint() = runTest {
        server.enqueue(MockResponse(body = okAnswer))
        val receipt = client().scan(byteArrayOf(1), listOf("Food"), today)

        assertEquals(43_000L, receipt.total)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1beta/models/primary-model:generateContent", request.url.encodedPath)
        assertEquals("test-key", request.headers["x-goog-api-key"])
        assertTrue("key must not be in the URL", request.url.query == null)
    }

    @Test
    fun emptyKeyFailsWithoutCallingTheApi() = runTest {
        assertEquals(ScanError.NotConfigured, scanError(client(apiKey = "")))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun retriesServerErrorsThenSucceeds() = runTest {
        server.enqueue(MockResponse(code = 503, body = """{"error": {"status": "UNAVAILABLE"}}"""))
        server.enqueue(MockResponse(body = okAnswer))
        assertEquals(43_000L, client().scan(byteArrayOf(1), listOf("Food"), today).total)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun givesUpAfterTheLastRetry() = runTest {
        repeat(3) { server.enqueue(MockResponse(code = 500)) }
        assertEquals(ScanError.Server, scanError(client()))
        assertEquals(3, server.requestCount)
    }

    @Test
    fun doesNotRetryRateLimits() = runTest {
        server.enqueue(MockResponse(code = 429, body = """{"error": {"status": "RESOURCE_EXHAUSTED"}}"""))
        assertEquals(ScanError.Busy, scanError(client()))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun retiredModelFallsBackToTheNextOne() = runTest {
        server.enqueue(MockResponse(code = 404, body = """{"error": {"status": "NOT_FOUND"}}"""))
        server.enqueue(MockResponse(body = okAnswer))
        val receipt = client(models = listOf("old-model", "new-model")).scan(byteArrayOf(1), listOf("Food"), today)

        assertEquals(43_000L, receipt.total)
        assertEquals("/v1beta/models/old-model:generateContent", server.takeRequest().url.encodedPath)
        assertEquals("/v1beta/models/new-model:generateContent", server.takeRequest().url.encodedPath)
    }

    @Test
    fun serverErrorsFallBackAfterRetries() = runTest {
        repeat(3) { server.enqueue(MockResponse(code = 503)) }
        server.enqueue(MockResponse(body = okAnswer))
        assertEquals(43_000L, client(models = listOf("busy-model", "new-model")).scan(byteArrayOf(1), listOf("Food"), today).total)
        assertEquals(4, server.requestCount)
    }

    @Test
    fun rateLimitsDoNotFallBack() = runTest {
        server.enqueue(MockResponse(code = 429))
        assertEquals(ScanError.Busy, scanError(client(models = listOf("a", "b"))))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun slowAnswersTimeOut() = runTest {
        server.enqueue(MockResponse.Builder().body(okAnswer).headersDelay(2, TimeUnit.SECONDS).build())
        val http = OkHttpClient.Builder().readTimeout(200, TimeUnit.MILLISECONDS).build()
        assertEquals(ScanError.Timeout, scanError(client(http = http)))
    }

    @Test
    fun unreachableServerIsANetworkError() = runTest {
        val unreachable = GeminiReceiptClient(apiKey = "k", baseUrl = "http://127.0.0.1:1/v1beta", models = listOf("m"), retryDelaysMillis = emptyList())
        assertEquals(ScanError.Network, scanError(unreachable))
    }
}
