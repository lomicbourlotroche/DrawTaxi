package com.drawtaxi.app.ui.screens.navigation

import android.app.Application
import android.content.Intent
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drawtaxi.app.logic.geocoding.GeocodingService
import com.drawtaxi.app.logic.routing.NavigationEngine
import com.drawtaxi.app.logic.routing.NavigationEngineState
import com.drawtaxi.app.service.tracking.LocationTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NavigationViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = NavigationEngine(application, viewModelScope)

    private val _pickupLocation = MutableStateFlow<Location?>(null)
    val pickupLocation: StateFlow<Location?> = _pickupLocation.asStateFlow()

    private val _destLocation = MutableStateFlow<Location?>(null)
    val destLocation: StateFlow<Location?> = _destLocation.asStateFlow()

    private val _homeLocation = MutableStateFlow<Location?>(null)
    val homeLocation: StateFlow<Location?> = _homeLocation.asStateFlow()

    private val _currentPhase = MutableStateFlow(NavigationPhase.TO_PICKUP)
    val currentPhase: StateFlow<NavigationPhase> = _currentPhase.asStateFlow()

    private val _totalDistanceMeters = MutableStateFlow(0f)
    val totalDistanceMeters: StateFlow<Float> = _totalDistanceMeters.asStateFlow()

    val engineState: StateFlow<NavigationEngineState> = engine.state

    private var lastTrackingLocation: Location? = null
    private var isTrackingDistance = false

    fun geocodeRide(departure: String, arrival: String) {
        viewModelScope.launch {
            if (departure.isNotBlank()) {
                _pickupLocation.value = GeocodingService.geocode(departure, getApplication())
            }
            if (arrival.isNotBlank()) {
                _destLocation.value = GeocodingService.geocode(arrival, getApplication())
            }
        }
    }

    fun geocodeHome(address: String) {
        if (address.isBlank()) return
        viewModelScope.launch {
            _homeLocation.value = GeocodingService.geocode(address, getApplication())
        }
    }

    fun setPhase(phase: NavigationPhase) {
        _currentPhase.value = phase
    }

    fun navigateToPickup() {
        _currentPhase.value = NavigationPhase.TO_DESTINATION
    }

    fun startNavigation(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double) {
        viewModelScope.launch {
            engine.stopNavigation()
            val route = NavigationEngine.fetchRoute(
                fromLat = fromLat, fromLng = fromLng,
                toLat = toLat, toLng = toLng
            )
            if (route != null) {
                engine.startNavigation(route, toLat = toLat, toLng = toLng)
            }
        }
    }

    fun startNavigationToDest(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double) {
        viewModelScope.launch {
            engine.state.value.let { current ->
                if (!current.isNavigating) {
                    val route = NavigationEngine.fetchRoute(
                        fromLat = fromLat, fromLng = fromLng,
                        toLat = toLat, toLng = toLng
                    )
                    if (route != null) {
                        engine.startNavigation(route, toLat = toLat, toLng = toLng)
                    }
                }
            }
        }
    }

    fun stopNavigation() {
        engine.stopNavigation()
    }

    fun startLocationTracking(rideId: String) {
        if (isTrackingDistance) return
        isTrackingDistance = true
        lastTrackingLocation = null
        _totalDistanceMeters.value = 0f

        startDistanceAccumulation()
        startLocationService(rideId)
    }

    fun stopLocationTracking() {
        if (!isTrackingDistance) return
        isTrackingDistance = false
        lastTrackingLocation = null
        stopLocationService()
    }

    fun getTotalDistanceKm(): Float = _totalDistanceMeters.value / 1000f

    private fun startDistanceAccumulation() {
        viewModelScope.launch {
            engine.state.collect { state ->
                val location = state.currentLocation ?: return@collect
                if (isTrackingDistance) {
                    lastTrackingLocation?.let { last ->
                        val distance = last.distanceTo(location)
                        if (distance > 2.0) {
                            _totalDistanceMeters.update { it + distance }
                        }
                    }
                    lastTrackingLocation = location
                }
            }
        }
    }

    private fun startLocationService(rideId: String) {
        try {
            val intent = Intent(getApplication(), LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START
                putExtra(LocationTrackingService.EXTRA_RIDE_ID, rideId)
            }
            ContextCompat.startForegroundService(getApplication(), intent)
            Log.d(TAG, "LocationTrackingService started for ride $rideId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LocationTrackingService", e)
        }
    }

    private fun stopLocationService() {
        try {
            val intent = Intent(getApplication(), LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_STOP
            }
            getApplication<Application>().startService(intent)
            Log.d(TAG, "LocationTrackingService stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop LocationTrackingService", e)
        }
    }

    fun checkArrivedAtPickup(thresholdMeters: Float = 50f) {
        val current = engine.state.value.currentLocation ?: return
        val pickup = _pickupLocation.value ?: return
        if (current.distanceTo(pickup) < thresholdMeters) {
            _currentPhase.value = NavigationPhase.TO_DESTINATION
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationService()
        engine.destroy()
    }

    companion object {
        private const val TAG = "NavigationViewModel"
    }
}
