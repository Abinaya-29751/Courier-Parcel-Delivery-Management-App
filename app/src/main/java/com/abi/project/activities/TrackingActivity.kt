package com.abi.project.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.abi.project.database.DatabaseHelper

class TrackingActivity : ComponentActivity() {

    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseHelper = DatabaseHelper(this)

        val courierId = intent.getIntExtra("courierId", -1)
        if (courierId == -1) {
            Toast.makeText(this, "Invalid courier ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch the courier's stored location from the database
        val courierLocation = databaseHelper.getCourierLocation(courierId)
        if (courierLocation.isNullOrEmpty()) {
            Toast.makeText(this, "No location found for this courier", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Open Google Maps with navigation from the courier's location to the constant location (Coimbatore Hopes)
        openGoogleMaps(courierLocation)
    }

    private fun openGoogleMaps(courierLocation: String) {
        val constantDestination = "11.0290,77.0010"

        val gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$courierLocation&destination=$constantDestination")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            // If Google Maps app is not found, open in browser
            val browserIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            startActivity(browserIntent)
        }
    }
}
