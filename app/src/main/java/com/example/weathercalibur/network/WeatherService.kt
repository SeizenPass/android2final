package com.example.weathercalibur.network

import com.example.weathercalibur.models.BigResponse
import com.example.weathercalibur.models.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("2.5/weather")
    fun getWeather(
            @Query("lat") lat: Double,
            @Query("lon") lon: Double,
            @Query("units") units: String?,
            @Query("appid") appid: String
    ) : Call<WeatherResponse>
    @GET("2.5/weather")
    fun getWeather2(
            @Query("q") q: String,
            @Query("units") units: String?,
            @Query("appid") appid: String
    ) : Call<WeatherResponse>
    @GET("2.5/onecall")
    fun oneCall(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("exclude") exclude: String,
        @Query("units") units: String?,
        @Query("appid") appid: String
    ) : Call<BigResponse>
}