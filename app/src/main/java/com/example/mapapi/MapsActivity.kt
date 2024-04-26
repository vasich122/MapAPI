package com.example.mapapi

import android.location.Address
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import com.example.mapapi.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.CameraPosition
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    // API ключ для проверки качества воздуха
    private val apiKey = "399161e1dbb1ab97bd00240c864bf5cbac035c2d"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Получение данных широты и долготы
        val lan = intent.getDoubleExtra("Latitude", 52.28)
        val lon = intent.getDoubleExtra("Longitude", 104.3)

        // Определение точки на карте
        val latLng = LatLng(lan, lon)
        // Добавление маркера и камеры для удобства карт, иначе сразу будет вся карта, а не мини кусок
        mMap.addMarker(MarkerOptions().position(latLng).title("I'm here!"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))

        val airQualityLocation = LatLng(52.28, 104.3) // Вы можете настроить это под ваши нужды
        addAirQualityMarker(airQualityLocation)
        // Приближение карты (здесь наша позиция, масштаб и 2 наклона)
        val cameraPosition = CameraPosition(latLng, 15f, 23f, 10f)
        // Применяем CP
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        // Обработчики нажатий на карте
        mMap.setOnMapClickListener {
            val geocoder = Geocoder(applicationContext, Locale.getDefault())
            // Получение адреса
            var address: String = ""
            // Получение всех ближайших адресов, it-точка касания
            val addresses: MutableList<Address>? = geocoder.getFromLocation(it.latitude, it.longitude, 1)
            // Проверка на то, получили ли мы какие-то адреса или нет
            if (addresses != null) {
                for (adr in addresses) {
                    address += adr.getAddressLine(addresses.indexOf(adr))
                }
            }
            // Ставим маркер в той же точке, где и нажал пользователь с подписью адреса
            mMap.addMarker(MarkerOptions().position(it).title(address))
        }
    }

    private fun addAirQualityMarker(location: LatLng) {
        val client = OkHttpClient()
        val url = "https://api.waqi.info/feed/geo:${location.latitude};${location.longitude}/?token=$apiKey"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                responseBody?.let {
                    val json = JSONObject(it)
                    val status = json.getString("status")
                    if (status == "ok") {
                        val data = json.getJSONObject("data")
                        val aqi = data.getInt("aqi")

                        runOnUiThread {
                            mMap.addMarker(
                                MarkerOptions()
                                    .position(location)
                                    .title("Air Quality: $aqi")
                            )
                        }
                    } else {
                        runOnUiThread {
                            // Handle error when the status is not "ok"
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    // Handle failure
                }
            }
        })
    }

}
