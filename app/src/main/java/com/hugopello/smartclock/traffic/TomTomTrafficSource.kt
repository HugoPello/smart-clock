package com.hugopello.smartclock.traffic

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * [TrafficSource] backed by TomTom: geocode both addresses, then ask for a car route with traffic.
 * Three requests per check, far below the free tier (20K/month per API).
 * Never throws: every failure becomes [TrafficResult.Unavailable], so the caller only has two cases.
 */
class TomTomTrafficSource(
    private val api: TomTomApi,
    private val apiKey: String,
) : TrafficSource {

    override suspend fun travelTime(origin: String, destination: String, departure: Instant): TrafficResult {
        if (apiKey.isBlank() || origin.isBlank() || destination.isBlank()) {
            return TrafficResult.Unavailable(retry = false)
        }
        return try {
            // Unknown address: asking again gives the same answer.
            val from = locate(origin) ?: return TrafficResult.Unavailable(retry = false)
            val to = locate(destination) ?: return TrafficResult.Unavailable(retry = false)
            val route = api.calculateRoute(
                locations = "${from.asParam()}:${to.asParam()}",
                apiKey = apiKey,
                departAt = DEPART_AT_FORMAT.format(departure.atOffset(ZoneOffset.UTC)),
            ).routes.firstOrNull()
            if (route == null) {
                TrafficResult.Unavailable(retry = false)
            } else {
                TrafficResult.Success(Duration.ofSeconds(route.summary.travelTimeInSeconds))
            }
        } catch (e: CancellationException) {
            throw e // never swallow coroutine cancellation
        } catch (e: HttpException) {
            // 429 (too many requests) and 5xx (TomTom's side) are temporary. 400 (e.g. no road between
            // the points) and 403 (bad or inactive key) won't fix themselves.
            TrafficResult.Unavailable(retry = e.code() == 429 || e.code() >= 500)
        } catch (_: IOException) {
            TrafficResult.Unavailable(retry = true) // no network, timeout
        } catch (_: SerializationException) {
            TrafficResult.Unavailable(retry = false) // unexpected JSON
        }
    }

    private suspend fun locate(address: String): Position? =
        api.geocode(address, apiKey).results.firstOrNull()?.position

    private fun Position.asParam() = String.format(Locale.ROOT, "%.6f,%.6f", lat, lon)

    private companion object {
        /** "2026-11-13T06:30:00+00:00": RFC 3339 with an explicit offset, as in TomTom's examples. */
        val DEPART_AT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")
    }
}
