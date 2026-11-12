package com.hugopello.smartclock.traffic

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * The two TomTom endpoints we use. Retrofit generates the HTTP code from this interface.
 * Docs: https://docs.tomtom.com/geocoding-api/documentation/geocode
 *       https://docs.tomtom.com/routing-api/documentation/tomtom-maps/calculate-route
 */
interface TomTomApi {

    /** Free-text address → coordinates. `@Path` URL-encodes the address (spaces, commas, accents...). */
    @GET("search/2/geocode/{query}.json")
    suspend fun geocode(
        @Path("query") address: String,
        @Query("key") apiKey: String,
        @Query("limit") limit: Int = 1,
    ): GeocodeResponse

    /**
     * Routing only accepts coordinates: "lat,lon:lat,lon".
     * `encoded = true` so the `,` and `:` separators are sent as they are, not percent-encoded.
     */
    @GET("routing/1/calculateRoute/{locations}/json")
    suspend fun calculateRoute(
        @Path("locations", encoded = true) locations: String,
        @Query("key") apiKey: String,
        /** RFC 3339 with offset. In the future → *predicted* traffic for that moment. */
        @Query("departAt") departAt: String,
        @Query("traffic") traffic: Boolean = true,
        @Query("travelMode") travelMode: String = "car",
        /** We only need the summary; skips the (large) list of route points. */
        @Query("routeRepresentation") routeRepresentation: String = "summaryOnly",
    ): RouteResponse

    companion object {
        const val BASE_URL = "https://api.tomtom.com/"

        private val json = Json { ignoreUnknownKeys = true }

        fun create(baseUrl: String = BASE_URL): TomTomApi {
            val client = OkHttpClient.Builder()
                // Must finish well before the alarm; WorkManager retries if it times out.
                .callTimeout(20, TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TomTomApi::class.java)
        }
    }
}

/* JSON shapes: only the fields we read. kotlinx.serialization generates the parsers at compile time. */

@Serializable
data class GeocodeResponse(val results: List<GeocodeResult> = emptyList())

@Serializable
data class GeocodeResult(val position: Position)

@Serializable
data class Position(val lat: Double, val lon: Double)

@Serializable
data class RouteResponse(val routes: List<TomTomRoute> = emptyList())

@Serializable
data class TomTomRoute(val summary: RouteSummary)

/** `travelTimeInSeconds` already includes traffic delay (`trafficDelayInSeconds` is the extra part). */
@Serializable
data class RouteSummary(val travelTimeInSeconds: Long, val trafficDelayInSeconds: Long = 0)
