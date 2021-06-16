package com.example.weathercalibur.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.weathercalibur.R
import com.example.weathercalibur.models.BigResponse
import com.example.weathercalibur.models.WeatherResponse
import com.example.weathercalibur.network.WeatherService
import com.example.weathercalibur.utils.Constants
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mSharedPreferences: SharedPreferences
    private var currentDay = 0
    private lateinit var bigResponse: BigResponse
    private var lastlat: Double = 0.0
    private var lastlon: Double = 0.0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
        if (!isLocationEnabled()) {
            Toast.makeText(
                    this,
                    "Your location provider is turned off. Please turn it on",
                    Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        else {
            Toast.makeText(
                    this,
                    "Your location provider is already on.",
                    Toast.LENGTH_SHORT
            ).show()
        }
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ).withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                    this@MainActivity,
                                    "You have denied location permission. Please allow it is mandatory.",
                                    Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
                        showRationalDialogForPermission()
                    }
                }).onSameThread()
                .check()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            val latitude = mLastLocation.latitude
            Log.i("Current Latitude", "$latitude")
            lastlat = latitude
            val longitude = mLastLocation.longitude
            Log.i("Current Longitude", "$longitude")
            lastlon = longitude
            ArrayAdapter.createFromResource(this@MainActivity, R.array.cities_array, android.R.layout.simple_spinner_item)
                    .also {
                        arrayAdapter ->
                        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        city_spinner.adapter = arrayAdapter
                    }
            city_spinner.onItemSelectedListener = this@MainActivity
        }
    }

    private fun showRationalDialogForPermission() {
        AlertDialog.Builder(this)
                .setMessage("It looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
                .setPositiveButton(
                        "GO TO SETTINGS"
                ) { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }.show()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        )
    }

    private fun getLocationWeatherDetails(lat: Double, lon: Double){
        if (Constants.isNetworkAvailable(this@MainActivity)) {
            val retrofit : Retrofit = Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            val service: WeatherService = retrofit.create(WeatherService::class.java)
            val listCall: Call<BigResponse> = service.oneCall(
                    lat, lon, "hourly,minutely", Constants.METRIC_UNIT, Constants.APP_ID
            )
            listCall.enqueue(object : Callback<BigResponse> {
                override fun onFailure(call: Call<BigResponse>, t: Throwable) {
                    Log.e("Fail","FAIL, ${t.message.toString()}")
                }

                override fun onResponse(
                        call: Call<BigResponse>,
                        response: Response<BigResponse>
                ) {
                    if(response.isSuccessful){
                        val weatherList: BigResponse = response.body()!!

                        val weatherResponseJsonString = Gson().toJson(weatherList)
                        val editor = mSharedPreferences.edit()
                        editor.putString(Constants.BIG_RESPONSE_DATA, weatherResponseJsonString)
                        editor.apply()
                        val weatherData: BigResponse? =
                                weatherResponseJsonString?.run {
                                    Gson().fromJson(weatherResponseJsonString, BigResponse::class.java)
                                } ?: null
                        val days = ArrayList<String>()
                        for (day in weatherData!!.daily) {
                            days.add(unixDate(day.dt))
                        }
                        val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, days)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        day_spinner.adapter = adapter
                        day_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                                currentDay = p2
                                setupUI()
                            }

                            override fun onNothingSelected(p0: AdapterView<*>?) {
                                TODO("Not yet implemented")
                            }

                        }
                        setupUI()
                        Log.i("ResponseResult", "$weatherList")
                    }else{
                        val rc = response.code()
                        when(rc){
                            400 -> Log.e("Error 400", "BAD Connection")
                            404 -> Log.e("Error 404", "Not Found")
                            else ->  Log.e("Error $rc", "$rc ERROR")
                        }
                    }
                    //hideProgressBar()
                }
            })
        } else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    @SuppressLint("SetTextI18n")
    private fun setupUI(){
        val bigResponseJsonString = mSharedPreferences.getString(Constants.BIG_RESPONSE_DATA, "")
        val weatherList: BigResponse? =
                bigResponseJsonString?.run {
                    Gson().fromJson(bigResponseJsonString, BigResponse::class.java)
                } ?: null
        val i = currentDay
            Log.i("DT", unixDate(weatherList!!.daily[i].dt))
            Log.i("$i weather Name", weatherList!!.daily[i].toString())
                tv_main.text = weatherList.daily[i].weather[0].main
                tv_main_description.text = weatherList.daily[i].weather[0].description
                tv_pressure.text = weatherList.daily[i].pressure.toString() + "hPa"
                tv_sunrise_time.text = unixTime(weatherList.daily[i].sunrise)
                tv_sunset_time.text = unixTime(weatherList.daily[i].sunset)
                tv_max.text = "Max: " + weatherList.daily[i].temp.max + getUnit("")
                tv_min.text ="Min: " +  weatherList.daily[i].temp.min + getUnit("")
                tv_current.text = "Day: " + weatherList.daily[i].temp.day + getUnit("")
                tv_feels_like.text = "Night: " + weatherList.daily[i].temp.night  + getUnit("")
                tv_speed.text = weatherList.daily[i].wind_speed.toString()
                tv_speed_unit.text ="m/s"
                tv_humidity.text = "${weatherList.daily[i].humidity}%"
                //tv_name.text = weatherList.name
                when(weatherList.daily[i].weather[0].icon){
                    "01d" -> iv_main.setImageResource(R.drawable.sunny)
                    "02d" -> iv_main.setImageResource(R.drawable.cloud)
                    "03d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04d" -> iv_main.setImageResource(R.drawable.cloud)
                    "09d" -> iv_main.setImageResource(R.drawable.rain)
                    "10d" -> iv_main.setImageResource(R.drawable.rain)
                    "11d" -> iv_main.setImageResource(R.drawable.storm)
                    "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                    "01n" -> iv_main.setImageResource(R.drawable.sunny)
                    "02n" -> iv_main.setImageResource(R.drawable.cloud)
                    "03n" -> iv_main.setImageResource(R.drawable.cloud)
                    "04n" -> iv_main.setImageResource(R.drawable.cloud)
                    "09n" -> iv_main.setImageResource(R.drawable.rain)
                    "10n" -> iv_main.setImageResource(R.drawable.rain)
                    "11n" -> iv_main.setImageResource(R.drawable.storm)
                    "13n" -> iv_main.setImageResource(R.drawable.snowflake)
        }
    }
    private fun getUnit(loc: String): String {
        return when(loc){
            "US", "LR", "MM"-> "℉"
            else -> "℃"
        }
    }

    private fun unixDate(timex: Long): String {
        val date = Date(timex * 1000L)
        Log.e("UNIX_DATE", "$date")
        val sdf = SimpleDateFormat("dd MMM", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    private fun unixTime(timex: Long): String{
        val date = Date(timex * 1000L)
        Log.e("date","$date")
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        val city = parent!!.getItemAtPosition(pos).toString();
        currentDay = 0
        day_spinner.setSelection(0)
        if (city == "Nur-Sultan") {
            getLocationWeatherDetails(51.169392, 71.449074)
        } else if (city == "Almaty") {
            getLocationWeatherDetails(43.238949, 76.889709)
        } else if (city == "Current Location") {
            getLocationWeatherDetails(lastlat, lastlon)
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }
}