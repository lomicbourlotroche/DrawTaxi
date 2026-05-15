package com.drawtaxi.app.car

import android.content.Intent
import android.text.SpannableString
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.local.AppDatabase
import com.drawtaxi.app.data.local.SettingsManager
import com.drawtaxi.app.data.TaxiRepository
import com.drawtaxi.app.logic.ShareUtils
import kotlinx.coroutines.*

class EditRideCarScreen(carContext: CarContext, private val rideId: String) : Screen(carContext) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var ride: RideRequest? = null
    private var settings: AppSettings? = null
    private var repository: TaxiRepository? = null
    private var distanceKm: Double = 0.0
    private var price: Double = 0.0
    private var isValidated = false
    private var isLoading = true

    init {
        loadRideAndSettings()
    }

    override fun onGetTemplate(): Template {
        val rideData = ride

        if (isLoading || rideData == null) {
            return PaneTemplate.Builder(
                Pane.Builder()
                    .addRow(
                        Row.Builder()
                            .setTitle("Chargement...")
                            .build()
                    )
                    .build()
            )
                .setTitle("Modifier Course")
                .build()
        }

        val paneBuilder = Pane.Builder()

        if (!isValidated) {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("Distance")
                    .addText(SpannableString(String.format("%.1f km", distanceKm)))
                    .build()
            )

            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("Prix")
                    .addText(SpannableString(String.format("%.2f €", price)))
                    .build()
            )

            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("Appuyez sur Valider pour terminer la course")
                    .build()
            )

            paneBuilder.addAction(
                Action.Builder()
                    .setTitle("Valider la course")
                    .setBackgroundColor(CarColor.GREEN)
                    .setOnClickListener {
                        scope.launch {
                            repository?.validateRide(rideId)
                            withContext(Dispatchers.Main) {
                                isValidated = true
                                invalidate()
                            }
                        }
                    }
                    .build()
            )
        } else {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("Course validée")
                    .addText(SpannableString("Distance: ${String.format("%.1f km", distanceKm)}"))
                    .build()
            )

            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("Prix")
                    .addText(SpannableString(String.format("%.2f €", price)))
                    .build()
            )

            paneBuilder.addAction(
                Action.Builder()
                    .setTitle("Envoyer reçu")
                    .setBackgroundColor(CarColor.BLUE)
                    .setOnClickListener {
                        scope.launch {
                            shareReceipt()
                        }
                    }
                    .build()
            )
        }

        paneBuilder.addAction(
            Action.Builder()
                .setTitle("Annuler")
                .setBackgroundColor(CarColor.DEFAULT)
                .setOnClickListener {
                    screenManager.pop()
                }
                .build()
        )

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Modifier Course")
            .build()
    }

    private suspend fun shareReceipt() {
        val currentRide = ride
        val currentSettings = settings
        if (currentRide != null && currentSettings != null) {
            val completedRide = currentRide.copy(
                distanceKm = distanceKm,
                price = price,
                isPending = false
            )
            repository?.updateRide(completedRide)
            withContext(Dispatchers.Main) {
                ShareUtils.shareReceipt(carContext, completedRide, currentSettings)
                screenManager.popToRoot()
            }
        }
    }

    private fun loadRideAndSettings() {
        scope.launch {
            try {
                val context = carContext
                val database = AppDatabase.getDatabase(context)
                val settingsManager = SettingsManager(context)
                repository = TaxiRepository(database.rideDao(), database.quoteDao(), database.absenceDao(), settingsManager)

                settingsManager.settingsFlow.collect { loadedSettings ->
                    settings = loadedSettings
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        scope.launch {
            try {
                val context = carContext
                val database = AppDatabase.getDatabase(context)
                val settingsManager = SettingsManager(context)
                repository = TaxiRepository(database.rideDao(), database.quoteDao(), database.absenceDao(), settingsManager)

                val allRides = repository!!.getAllRides()
                ride = allRides.find { it.id == rideId }
                ride?.let {
                    distanceKm = it.distanceKm
                    price = it.price
                }

                isLoading = false
                withContext(Dispatchers.Main) {
                    invalidate()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
            }
        }
    }
}
