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
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.drawtaxi.app.MainActivity
import com.drawtaxi.app.logic.messaging.NotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.maplibre.navigation.core.location.engine.LocationEngine
import org.maplibre.navigation.core.location.engine.LocationEngineProvider
import org.maplibre.navigation.core.location.toAndroidLocation
import java.util.Locale

/**
 * Service de suivi GPS en arrière-plan pour calculer la distance d'une course.
 * Utilise MapLibre LocationEngine pour une meilleure précision et cohérence avec la navigation.
 */
class LocationTrackingService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private lateinit var locationEngine: LocationEngine
    private var locationJob: Job? = null

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _locationHistory = MutableStateFlow<List<Location>>(emptyList())
    val locationHistory: StateFlow<List<Location>> = _locationHistory.asStateFlow()

    private val _totalDistanceMeters = MutableStateFlow(0f)
    val totalDistanceMeters: StateFlow<Float> = _totalDistanceMeters.asStateFlow()

    private var isTracking = false
    private var currentRideId: String? = null

    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        createNotificationChannel()
        Log.d(TAG, "Service de suivi créé")
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
        serviceScope.cancel()
        Log.d(TAG, "Service de suivi détruit")
    }

    private fun createNotificationChannel() {
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

    private fun createNotification(distanceMeters: Float = 0f): Notification {
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

        val distanceText = if (distanceMeters > 0) {
            val km = distanceMeters / 1000f
            String.format(Locale.getDefault(), " - %.2f km", km)
        } else ""

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID_LOCATION)
            .setContentTitle("Course en cours")
            .setContentText("Suivi GPS actif$distanceText")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Arrêter la course",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun startTracking() {
        if (isTracking) return

        // Vérification des permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission ACCESS_FINE_LOCATION non accordée")
            stopSelf()
            return
        }

        isTracking = true
        _locationHistory.value = emptyList()
        _totalDistanceMeters.value = 0f

        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur startForeground: ${e.message}")
            // Sur Android 14+, peut échouer si démarré depuis l'arrière-plan sans privilèges
        }

        locationJob?.cancel()
        locationJob = serviceScope.launch {
            locationEngine.listenToLocation(
                LocationEngine.Request(
                    minIntervalMilliseconds = 3000,
                    maxIntervalMilliseconds = 8000,
                    accuracy = LocationEngine.Request.Accuracy.HIGH
                )
            ).collect { navLocation ->
                handleNewLocation(navLocation.toAndroidLocation())
            }
        }
        
        Log.d(TAG, "Suivi GPS démarré")
    }

    private fun handleNewLocation(location: Location) {
        // Filtrage de base pour éviter le "jitter" GPS
        if (location.accuracy > 30) return // On ignore les points trop imprécis

        val lastLocation = _currentLocation.value
        
        if (lastLocation != null) {
            val distance = lastLocation.distanceTo(location)
            
            // On ignore les micro-mouvements (souvent du bruit GPS à l'arrêt)
            if (distance < 2.0) return 
            
            _totalDistanceMeters.update { it + distance }
        }

        _currentLocation.value = location
        _locationHistory.update { it + location }
        
        // Mise à jour de la notification avec la distance actuelle
        updateNotification(_totalDistanceMeters.value)
        
        Log.d(TAG, "Location: ${location.latitude}, ${location.longitude} - Distance Totale: ${_totalDistanceMeters.value}m")
    }

    private fun updateNotification(distanceMeters: Float) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification(distanceMeters))
    }

    private fun stopTracking() {
        if (!isTracking) return
        isTracking = false

        locationJob?.cancel()
        locationJob = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "Suivi GPS arrêté")
    }

    companion object {
        private const val TAG = "LocationService"
        private const val NOTIFICATION_ID = 1004
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_RIDE_ID = "EXTRA_RIDE_ID"
    }
}
