package com.drawtaxi.app.service.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.drawtaxi.app.MainActivity
import com.drawtaxi.app.logic.messaging.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationTrackingService : Service() {

    private val binder = LocalBinder()
    private lateinit var locationManager: LocationManager
    private var locationListener: android.location.LocationListener? = null

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _locationHistory = MutableStateFlow<List<Location>>(emptyList())
    val locationHistory: StateFlow<List<Location>> = _locationHistory

    private var isTracking = false
    private var currentRideId: String? = null

    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
        Log.d(TAG, "Service créé")
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentRideId = intent.getStringExtra(EXTRA_RIDE_ID)
                startTracking()
            }
            ACTION_STOP -> {
                stopTracking()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        Log.d(TAG, "Service détruit")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationHelper.CHANNEL_ID_LOCATION,
                "Suivi GPS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Suivi GPS actif pour la course en cours"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "LocationService"
        private const val NOTIFICATION_ID = 1004
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_RIDE_ID = "EXTRA_RIDE_ID"
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID_LOCATION)
            .setContentTitle("Course en cours")
            .setContentText("Suivi GPS actif")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Arrêter",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startTracking() {
        if (isTracking) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permission POST_NOTIFICATIONS non accordée")
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission ACCESS_FINE_LOCATION non accordée")
            stopSelf()
            return
        }

        isTracking = true
        _locationHistory.value = emptyList()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        locationListener = android.location.LocationListener { location ->
            _currentLocation.value = location
            _locationHistory.value = _locationHistory.value + location
            Log.d(TAG, "Location: ${location.latitude}, ${location.longitude}")
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                0f,
                locationListener!!,
                Looper.getMainLooper()
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000L,
                0f,
                locationListener!!,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Suivi GPS démarré")
        } catch (e: SecurityException) {
            Log.e(TAG, "Erreur permission: ${e.message}")
        }
    }

    private fun stopTracking() {
        if (!isTracking) return
        isTracking = false

        locationListener?.let { listener ->
            locationManager.removeUpdates(listener)
        }
        locationListener = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "Suivi GPS arrêté")
    }

    fun getTotalDistance(): Float {
        val locations = _locationHistory.value
        if (locations.size < 2) return 0f

        var totalDistance = 0f
        for (i in 1 until locations.size) {
            totalDistance += locations[i - 1].distanceTo(locations[i])
        }
        return totalDistance
    }
}
