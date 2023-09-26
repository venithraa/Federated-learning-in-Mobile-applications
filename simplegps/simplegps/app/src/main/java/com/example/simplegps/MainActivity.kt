@file:Suppress("DEPRECATION")

package com.example.simplegps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.simplegps.DataExporter.exportDataToJsonAndTFLite
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity(), AppLifecycleTracker.Listener {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var startStopButton: Button
    private var isTracking: Boolean = false
    private lateinit var appLifecycleTracker: AppLifecycleTracker
    private var isAppInForeground: Boolean = false
    private lateinit var telephonyManager: TelephonyManager
    private var signalStrengthListener: PhoneStateListener? = null
    private var gsmSignalStrength: String? = null

    private val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
    private val locationPermissionRequestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startStopButton = findViewById(R.id.startStopButton)

        appLifecycleTracker = AppLifecycleTracker(this)
        application.registerActivityLifecycleCallbacks(appLifecycleTracker)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { updateLocation(it) }
            }
        }

        startStopButton.setOnClickListener {
            if (isTracking) {
                stopTracking()
            } else {
                startTracking()
            }
        }
        // Initialize TelephonyManager and add SignalStrengthListener
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        signalStrengthListener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                super.onSignalStrengthsChanged(signalStrength)
                // Get the GSM signal strength
                gsmSignalStrength = signalStrength.gsmSignalStrength.toString()
                // You can now use the gsmSignalStrength value as needed
            }
        }
        // Register the signalStrengthListener
        telephonyManager.listen(signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
    }

    private fun startTracking() {
        if (ContextCompat.checkSelfPermission(
                this,
                locationPermission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            isTracking = true
            startStopButton.text = "Stop Recording"
            startLocationTrackingService()

            val intervalEditText = findViewById<EditText>(R.id.intervalEditText)
            val intervalInSeconds = intervalEditText.text.toString().toIntOrNull() ?: 10
            val intervalInMillis = intervalInSeconds * 1000L

            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = intervalInMillis
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        } else {
            requestLocationPermission()
        }
    }

    private fun stopTracking() {
        isTracking = false
        startStopButton.text = "Start Recording"
        stopLocationTrackingService()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        checkDataInDatabase()
    }

    private fun updateLocation(location: Location) {
        val currentTime = System.currentTimeMillis()
        val timestamp = Date(currentTime).toString()
        val isForeground = isAppInForeground
        val gpsDataEntity = GPSData(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            timestamp = timestamp,
            isForeground = isForeground,
            signalStrength = gsmSignalStrength
        )
        insertGPSData(gpsDataEntity)
    }

    private fun insertGPSData(gpsDataEntity: GPSData) {
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "gps-database"
        ).fallbackToDestructiveMigration().build()

        lifecycleScope.launch {
            database.gpsDataDao().insert(gpsDataEntity)
        }
    }

    private fun checkDataInDatabase() {
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "gps-database"
        ).fallbackToDestructiveMigration().build()

        lifecycleScope.launch {
            val gpsDataList = database.gpsDataDao().getAllGPSData()
            // Get the TensorFlow Lite model bytes
            val tfliteModelBytes = readTFLiteModelBytes()
            if (gpsDataList.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    exportDataToJsonAndTFLite(applicationContext, gpsDataList, tfliteModelBytes)
                }

                for (gpsData in gpsDataList) {
                    val foregroundState = if (gpsData.isForeground) "Foreground" else "Background"
                    println("Latitude: ${gpsData.latitude}, Longitude: ${gpsData.longitude}, Altitude:${gpsData.altitude},  Timestamp: ${gpsData.timestamp}, State: $foregroundState, SignalStrength: ${gpsData.signalStrength}" )
                }
            } else {
                println("No data found in the database")
            }

            val file = File(applicationContext.filesDir, "gps-data.json")
            val filePath = file.absolutePath
            println("Exported JSON file path: $filePath")
            database.gpsDataDao().deleteAllGPSData()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(locationPermission),
            locationPermissionRequestCode
        )
    }

    private fun readTFLiteModelBytes(): ByteArray {
        val tfliteModelPath = "mean_model.tflite" // Replace with the path
        val assetManager = assets
        return assetManager.open(tfliteModelPath).readBytes()
    }

    private fun startLocationTrackingService() {
        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopLocationTrackingService() {
        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        stopService(serviceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        application.unregisterActivityLifecycleCallbacks(appLifecycleTracker)
        signalStrengthListener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
        }
    }

    override fun onAppForeground() {
        isAppInForeground = true
    }

    override fun onAppBackground() {
        isAppInForeground = false
    }
}
