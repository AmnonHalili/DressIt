package com.example.dressit.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("weather")
    suspend fun getWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "weather") val weather: List<Weather>,
    @Json(name = "main") val main: Main,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class Weather(
    @Json(name = "main") val main: String,
    @Json(name = "description") val description: String,
    @Json(name = "icon") val icon: String
)

@JsonClass(generateAdapter = true)
data class Main(
    @Json(name = "temp") val temp: Double,
    @Json(name = "feels_like") val feels_like: Double,
    @Json(name = "humidity") val humidity: Int
) 