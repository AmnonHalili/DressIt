package com.example.dressit.data.repository

import com.example.dressit.data.api.WeatherService
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class WeatherRepository {
    private val weatherService: WeatherService

    init {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        weatherService = retrofit.create(WeatherService::class.java)
    }

    suspend fun getWeather(latitude: Double, longitude: Double): String {
        val apiKey = "YOUR_API_KEY" // Replace with your OpenWeatherMap API key
        val response = weatherService.getWeather(latitude, longitude, apiKey)
        
        return buildString {
            append(response.name)
            append(" - ")
            append(response.weather.firstOrNull()?.description ?: "Unknown")
            append(", ")
            append(response.main.temp.toInt())
            append("°C")
        }
    }
} 