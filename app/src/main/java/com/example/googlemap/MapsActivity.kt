package com.example.googlemap

import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.getUiSettings().setZoomControlsEnabled(true)
        map.setOnMarkerClickListener(this)
        setUpMap()
    }

    override fun onMarkerClick(p0: Marker?) = false

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        map.isMyLocationEnabled = true

        map.mapType = GoogleMap.MAP_TYPE_TERRAIN

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
                val destination = LatLng(18.4537, 73.8586)

                val url: String = getDirectionsUrl(currentLatLng, destination)

                val downloadTask = DownloadTask()
                downloadTask.execute(url)

            }
        }
    }

    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {

        val str_origin = "origin=" + origin.latitude + "," + origin.longitude
        val str_dest = "destination=" + dest.latitude + "," + dest.longitude
        val sensor = "sensor=false"
        val parameters = "$str_origin&$str_dest&$sensor"
        val output = "json"
        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters"
    }

    private class DownloadTask : AsyncTask<String?, Void?, String>() {
        override fun doInBackground(vararg params: String?): String? {
            var data = ""
            try {
                data = downloadUrl(url[0])
            } catch (e: Exception) {
                Log.d("Background Task", e.toString())
            }
            return data
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            val parserTask = ParserTask()

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result)
        }
    }

    private class ParserTask :
        AsyncTask<String?, Int?, List<List<HashMap<String, String>>>?>() {
        // Parsing the data in non-ui thread
        override fun doInBackground(vararg params: String?): List<List<HashMap<String, String>>>? {
            val jObject: JSONObject
            var routes: List<List<HashMap<String, String>>>? = null
            try {
                jObject = JSONObject(jsonData[0])
                val parser = DirectionsJSONParser()

                // Starts parsing data
                routes = parser.parse(jObject)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return routes
        }

        // Executes in UI thread, after the parsing process
        override fun onPostExecute(result: List<List<HashMap<String, String>>>?) {
            var points: ArrayList<LatLng?>? = null
            var lineOptions: PolylineOptions? = null
            val markerOptions = MarkerOptions()

            // Traversing through all the routes
            for (i in 0 until result!!.size()) {
                points = ArrayList()
                lineOptions = PolylineOptions()

                // Fetching i-th route
                val path = result!![i]

                // Fetching all the points in i-th route
                for (j in 0 until path.size()) {
                    val point = path[j]
                    val lat = point["lat"]!!.toDouble()
                    val lng = point["lng"]!!.toDouble()
                    val position = LatLng(lat, lng)
                    points.add(position)
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points)
                lineOptions.width(2)
                lineOptions.color(Color.RED)
            }

            // Drawing polyline in the Google Map for the i-th route
            map.addPolyline(lineOptions)
        }
    }

    private fun downloadUrl(strUrl: String): String? {
        var data = ""
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(strUrl)

            // Creating an http connection to communicate with url
            urlConnection = url.openConnection() as HttpURLConnection

            // Connecting to url
            urlConnection.connect()

            // Reading data from url
            iStream = urlConnection.getInputStream()
            val br = BufferedReader(InputStreamReader(iStream))
            val sb = StringBuffer()
            var line: String? = ""
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
            }
            data = sb.toString()
            br.close()
        } catch (e: java.lang.Exception) {
            Log.d("Exception downloading", e.toString())
        } finally {
            if (iStream != null) {
                iStream.close()
            }
            if (urlConnection != null) {
                urlConnection.disconnect()
            }
        }
        return data
    }


    private fun placeMarkerOnMap(location: LatLng) {
        val markerOptions = MarkerOptions().position(location)
        map.addMarker(markerOptions)
    }



    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }


}