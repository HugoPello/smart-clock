package com.hugopello.smartclock.traffic

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * Real HTTP against a local fake server: checks the request we build and how we read the answer,
 * without a key, network or Google bill.
 */
class GoogleRoutesTrafficSourceTest {

    private val server = MockWebServer()
    private lateinit var source: GoogleRoutesTrafficSource

    private val departure = Instant.parse("2026-11-13T06:30:00Z")

    @Before
    fun setUp() {
        server.start()
        source = GoogleRoutesTrafficSource(RoutesApi.create(server.url("/").toString()), apiKey = "test-key")
    }

    @After
    fun tearDown() = server.close()

    private fun respond(code: Int, body: String = "{}") =
        server.enqueue(MockResponse.Builder().code(code).body(body).build())

    private suspend fun ask() = source.travelTime("Home", "Office", departure)

    @Test
    fun `parses traffic duration`() = runTest {
        respond(200, """{"routes":[{"duration":"1830s","staticDuration":"1500s"}]}""")

        assertEquals(TrafficResult.Success(Duration.ofSeconds(1830)), ask())
    }

    @Test
    fun `sends key, field mask and a traffic-aware request`() = runTest {
        respond(200, """{"routes":[{"duration":"60s"}]}""")

        ask()

        val request = server.takeRequest()
        assertEquals("/directions/v2:computeRoutes", request.url.encodedPath)
        assertEquals("test-key", request.headers["X-Goog-Api-Key"])
        assertEquals(RoutesApi.FIELD_MASK, request.headers["X-Goog-FieldMask"])
        val body = request.body?.utf8().orEmpty()
        assertTrue(body, body.contains(""""routingPreference":"TRAFFIC_AWARE""""))
        assertTrue(body, body.contains(""""origin":{"address":"Home"}"""))
        assertTrue(body, body.contains(""""departureTime":"2026-11-13T06:30:00Z""""))
    }

    @Test
    fun `no route found is not retried`() = runTest {
        respond(200, "{}")

        assertEquals(TrafficResult.Unavailable(retry = false), ask())
    }

    @Test
    fun `server error is retried`() = runTest {
        respond(503)

        assertEquals(TrafficResult.Unavailable(retry = true), ask())
    }

    @Test
    fun `rate limit is retried`() = runTest {
        respond(429)

        assertEquals(TrafficResult.Unavailable(retry = true), ask())
    }

    @Test
    fun `bad key is not retried`() = runTest {
        respond(403)

        assertEquals(TrafficResult.Unavailable(retry = false), ask())
    }

    @Test
    fun `no API key - no request at all`() = runTest {
        val noKey = GoogleRoutesTrafficSource(RoutesApi.create(server.url("/").toString()), apiKey = "")

        assertEquals(TrafficResult.Unavailable(retry = false), noKey.travelTime("Home", "Office", departure))
        assertEquals(0, server.requestCount)
    }
}
