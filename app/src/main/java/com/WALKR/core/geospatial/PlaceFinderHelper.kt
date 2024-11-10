package com.WALKR.core.geospatial

import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class PlaceFinderHelper(private val apiKey: String) {

    private val client = OkHttpClient()

    // Define callback interface
    interface PlaceFinderCallback {
        fun onResult(address: String?, coordinates: Map<String, Double>?)
    }

    // Method to fetch address and coordinates based on location name
    @OptIn(DelicateCoroutinesApi::class)
    fun getPlaceAddress(locationName: String, callback: PlaceFinderCallback) {
        GlobalScope.launch(Dispatchers.IO) {
            val roughLatitude = 42.7284  // Replace with your specific latitude
            val roughLongitude = -73.6918  // Replace with your specific longitude
            val searchRadius = 20000  // Radius in meters, e.g., 50km

            val url = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json" +
                    "?input=$locationName&inputtype=textquery&fields=formatted_address,geometry" +
                    "&locationbias=circle:$searchRadius@$roughLatitude,$roughLongitude&key=$apiKey"

            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("PlaceFinderHelper", "Failed request: ${response.message}")
                    callback.onResult(null, null)
                    return@use
                }

                // Process response JSON data
                val responseData = response.body?.string()
                responseData?.let {
                    val json = JSONObject(it)
                    val candidates = json.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val place = candidates.getJSONObject(0)
                        val address = place.optString("formatted_address")
                        val location = place.getJSONObject("geometry").getJSONObject("location")
                        val coordinates = mapOf(
                            "lat" to location.getDouble("lat"),
                            "lng" to location.getDouble("lng")
                        )
                        Log.d("PlaceFinderHelper", "Address: $address, Coordinates: $coordinates")
                        callback.onResult(address, coordinates)
                        return@use
                    }
                }
                // If no results are found, return nulls
                callback.onResult(null, null)
            }
        }
    }
}
