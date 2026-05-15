package com.drawtaxi.app.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class TaxiCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return HostValidator.Builder(applicationContext)
            .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
            .build()
    }

    override fun onCreateSession(): Session {
        return TaxiCarSession()
    }
}

class TaxiCarSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        return RideListScreen(carContext)
    }
}
