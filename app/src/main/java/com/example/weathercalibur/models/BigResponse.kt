package com.example.weathercalibur.models

import java.io.Serializable

data class BigResponse(
        val lat: Double,
        val lon: Double,
        val daily: List<Day>
) : Serializable