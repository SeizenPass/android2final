package com.example.weathercalibur.models

import java.io.Serializable

data class Day(
        val dt: Long,
        val sunrise: Long,
        val sunset: Long,
        val moonrise: Long,
        val moonset: Long,
        val moon_phase: Double,
        val temp: Temp,
        val feels_like: FeelsLike,
        val pressure: Int,
        val humidity: Int,
        val dew_point: Double,
        val wind_speed: Double,
        val wind_deg: Int,
        val wind_gust: Double,
        val weather: List<Weather>,
        val clouds: Int,
        val pop: Double,
        val rain: Double,
        val uvi: Double
) : Serializable
