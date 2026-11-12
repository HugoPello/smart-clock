package com.hugopello.smartclock.traffic

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * Real HTTP against a local fake server: checks the requests we build and how we read the answers,
 * without a key, network or quota.
 */
class TomTomTrafficSourceTest {

    private val server = MockWebServer()
    private lateinit var source: TomTomTrafficSource

    private val departure = Instant.parse("2026-11-13T06:30:00Z")

    private val home = """{"results":[{"position":{"lat":40.416775,"lon":-3.703790}}]}"""
    private val office = """{"results":[{"position":{"lat":40.450000,"lon":-3.730000}}]}"""
    private val route = """{"routes":[{"summary":{"travelTimeInSeconds":1830,"trafficDelayInSeconds":300}}]}"""

    @Before
    fun setUp() {
        server.start()
        source = TomTomTrafficSource(TomTomApi.create(server.url("/").toString()), apiKey = "test-key")
    }

    @After
    fun tearDown() = server.close()

    private fun respond(code: Int, body: String = "{}") =
        server.enqueue(MockResponse.Builder().code(code).body(body).build())

    private suspend fun ask() = source.travelTime("Puerta del Sol, Madrid", "Ciudad Universitaria", departure)

    @Test
    fun `geocodes both addresses and returns the traffic travel time`() = runTest {
        respond(200, home)
        respond(200, office)
        respond(200, route)

        assertEquals(TrafficResult.Success(Duration.ofSeconds(1830)), ask())
    }

    @Test
    fun `builds the expected requests`() = runTest {
        respond(200, home)
        respond(200, office)
        respond(200, route)

        ask()

        val geocode = server.takeRequest().url
        assertEquals("/search/2/geocode/Puerta%20del%20Sol,%20Madrid.json", geocode.encodedPath)
        assertEquals("test-key", geocode.queryParameter("key"))
        assertEquals("1", geocode.queryParameter("limit"))

        server.takeRequest() // second geocode

        val routing = server.takeRequest().url
        assertEquals(
            "/routing/1/calculateRoute/40.416775,-3.703790:40.450000,-3.730000/json",
            routing.encodedPath,
        )
        assertEquals("2026-11-13T06:30:00+00:00", routing.queryParameter("departAt"))
        assertEquals("true", routing.queryParameter("traffic"))
        assertEquals("summaryOnly", routing.queryParameter("routeRepresentation"))
    }

    @Test
    fun `unknown address is not retried and no route is requested`() = runTest {
        respond(200, """{"results":[]}""")

        assertEquals(TrafficResult.Unavailable(retry = false), ask())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `no road between the points is not retried`() = runTest {
        respond(200, home)
        respond(200, office)
        respond(400)

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
        val noKey = TomTomTrafficSource(TomTomApi.create(server.url("/").toString()), apiKey = "")

        assertEquals(TrafficResult.Unavailable(retry = false), noKey.travelTime("A", "B", departure))
        assertEquals(0, server.requestCount)
    }
}
