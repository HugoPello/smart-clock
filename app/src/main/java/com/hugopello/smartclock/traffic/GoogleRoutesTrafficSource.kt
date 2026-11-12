package com.hugopello.smartclock.traffic

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.time.Duration
import java.time.Instant

/**
 * [TrafficSource] backed by the Routes API with `TRAFFIC_AWARE` routing.
 * Never throws: every failure becomes [TrafficResult.Unavailable], so the caller only has two cases.
 */
class GoogleRoutesTrafficSource(
    private val api: RoutesApi,
    private val apiKey: String,
) : TrafficSource {

    override suspend fun travelTime(origin: String, destination: String, departure: Instant): TrafficResult {
        if (apiKey.isBlank() || origin.isBlank() || destination.isBlank()) {
            return TrafficResult.Unavailable(retry = false)
        }
        val request = RoutesRequest(
            origin = Waypoint(origin),
            destination = Waypoint(destination),
            travelMode = "DRIVE",
            routingPreference = "TRAFFIC_AWARE",
            departureTime = departure.toString(),
        )
        return try {
            val route = api.computeRoutes(apiKey, RoutesApi.FIELD_MASK, request).routes.firstOrNull()
            val seconds = route?.duration?.removeSuffix("s")?.toLongOrNull()
            // No route (unknown address, unreachable destination): asking again gives the same answer.
            if (seconds == null) TrafficResult.Unavailable(retry = false) else TrafficResult.Success(Duration.ofSeconds(seconds))
        } catch (e: CancellationException) {
            throw e // never swallow coroutine cancellation
        } catch (e: HttpException) {
            // 429 (quota) and 5xx (Google's side) are temporary; other 4xx (bad key, bad request) are not.
            TrafficResult.Unavailable(retry = e.code() == 429 || e.code() >= 500)
        } catch (_: IOException) {
            TrafficResult.Unavailable(retry = true) // no network, timeout
        } catch (_: SerializationException) {
            TrafficResult.Unavailable(retry = false) // unexpected JSON
        }
    }
}
