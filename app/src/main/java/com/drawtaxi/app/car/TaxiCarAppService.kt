package com.drawtaxi.app.car

import android.content.Intent
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class TaxiCarAppService : CarAppService() {

    companion object {
        private const val TAG = "TaxiCarAppService"
    }

    override fun createHostValidator(): HostValidator {
        return try {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create host validator", e)
            HostValidator.Builder(applicationContext).build()
        }
    }

    override fun onCreateSession(): Session {
        Log.d(TAG, "onCreateSession")
        return TaxiCarSession()
    }
}

class TaxiCarSession : Session() {

    companion object {
        private const val TAG = "TaxiCarSession"
    }

    override fun onCreateScreen(intent: Intent): Screen {
        Log.d(TAG, "onCreateScreen: intent=$intent")
        return RideListScreen(carContext)
    }
}
