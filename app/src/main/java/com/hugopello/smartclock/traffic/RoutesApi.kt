package com.hugopello.smartclock.traffic

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Google Routes API, `computeRoutes` endpoint. Retrofit generates the HTTP code from this interface.
 * Docs: https://developers.google.com/maps/documentation/routes/compute_route_directions
 */
interface RoutesApi {
    @POST("directions/v2:computeRoutes")
    suspend fun computeRoutes(
        @Header("X-Goog-Api-Key") apiKey: String,
        // Field mask: ask only for what we use. Required by the API, and it decides what you are billed for.
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Body request: RoutesRequest,
    ): RoutesResponse

    companion object {
        const val BASE_URL = "https://routes.googleapis.com/"
        const val FIELD_MASK = "routes.duration,routes.staticDuration"

        private val json = Json { ignoreUnknownKeys = true }

        fun create(baseUrl: String = BASE_URL, vararg interceptors: Interceptor): RoutesApi {
            val client = OkHttpClient.Builder()
                // Must finish well before the alarm; WorkManager retries if it times out.
                .callTimeout(20, TimeUnit.SECONDS)
                .apply { interceptors.forEach(::addInterceptor) }
                .build()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(RoutesApi::class.java)
        }
    }
}

/* JSON shapes (only the fields we send/read). kotlinx.serialization generates the (de)serializers at compile time. */

@Serializable
data class RoutesRequest(
    val origin: Waypoint,
    val destination: Waypoint,
    val travelMode: String,
    val routingPreference: String,
    /** RFC 3339, e.g. "2026-11-13T07:25:00Z". In the future → predicted traffic for that moment. */
    val departureTime: String,
)

/** An address is enough: Google geocodes it. No location permission needed. */
@Serializable
data class Waypoint(val address: String)

@Serializable
data class RoutesResponse(val routes: List<Route> = emptyList())

/** Durations come as strings like "1234s". `duration` includes traffic, `staticDuration` doesn't. */
@Serializable
data class Route(val duration: String? = null, val staticDuration: String? = null)
