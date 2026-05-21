package com.drawtaxi.app.car

import android.text.SpannableString
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.local.AppDatabase
import com.drawtaxi.app.data.local.SettingsManager
import com.drawtaxi.app.data.TaxiRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class EditRideCarScreen(carContext: CarContext, private val rideId: String) : Screen(carContext) {

    companion object {
        private const val TAG = "EditRideCarScreen"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var ride: RideRequest? = null
    private var settings: AppSettings? = null
    private var repository: TaxiRepository? = null
    private var distanceKm: Double = 0.0
    private var price: Double = 0.0
    private var isValidated = false
    private var isLoading = true
    private var validationError: String? = null
    private var loadJob: Job? = null

    init {
        loadRideAndSettings()
    }

    override fun onGetTemplate(): Template {
        val rideData = ride

        if (isLoading) {
            return PaneTemplate.Builder(
                Pane.Builder()
                    .addRow(
                        Row.Builder()
                            .setTitle("Chargement...")
                            .build()
                    )
                    .build()
            )
                .setTitle("Terminer la course")
                .setHeaderAction(Action.BACK)
                .build()
        }

        if (rideData == null) {
            return PaneTemplate.Builder(
                Pane.Builder()
                    .addRow(
                        Row.Builder()
                            .setTitle("Course introuvable")
                            .build()
                    )
                    .build()
            )
                .setTitle("Terminer la course")
                .setHeaderAction(Action.BACK)
                .build()
        }

        val paneBuilder = Pane.Builder()

        if (isValidated) {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("Course terminée avec succès !")
                    .addText(SpannableString(
                        buildString {
                            append("Distance: ${String.format("%.1f km", distanceKm)}")
                            append("\nPrix: ${String.format("%.2f €", price)}")
                        }
                    ))
                    .build()
            )

            paneBuilder.addAction(
                Action.Builder()
                    .setTitle("Retour")
                    .setBackgroundColor(CarColor.DEFAULT)
                    .setOnClickListener {
                        screenManager.pop()
                    }
                    .build()
            )
        } else {
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

            val fuelCostPerKm = settings?.fuelCostPerKm ?: 0.12
            val operatingCostPerHour = settings?.operatingCostPerHour ?: 15.0
            val durationMinutes = rideData.durationMinutes
            val fuelCost = distanceKm * fuelCostPerKm
            val operatingCost = if (durationMinutes > 0) (durationMinutes / 60.0) * operatingCostPerHour else 0.0
            val totalCost = fuelCost + operatingCost
            val profitabilityPercent = RideRequest.calculateProfitability(price, fuelCost, operatingCost)

            if (distanceKm > 0 && price > 0) {
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("Rentabilité")
                        .addText(SpannableString(String.format("%.1f%%", profitabilityPercent)))
                        .build()
                )

                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("Coût total")
                        .addText(SpannableString(String.format("%.2f € (carb: %.2f + op: %.2f)", totalCost, fuelCost, operatingCost)))
                        .build()
                )
            }

            validationError?.let { error ->
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle(error)
                        .build()
                )
            }

            val canValidate = distanceKm > 0.0 && price > 0.0

            paneBuilder.addAction(
                Action.Builder()
                    .setTitle(if (canValidate) "Valider la course" else "Données incomplètes")
                    .setBackgroundColor(if (canValidate) CarColor.GREEN else CarColor.RED)
                    .setOnClickListener {
                        if (canValidate) {
                            completeRide()
                        } else {
                            validationError = "Veuillez définir une distance et un prix valides"
                            invalidate()
                        }
                    }
                    .build()
            )

            paneBuilder.addAction(
                Action.Builder()
                    .setTitle("Annuler")
                    .setBackgroundColor(CarColor.DEFAULT)
                    .setOnClickListener {
                        screenManager.pop()
                    }
                    .build()
            )
        }

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Terminer la course")
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun completeRide() {
        scope.launch {
            try {
                repository?.validateRide(rideId)
                withContext(Dispatchers.Main) {
                    isValidated = true
                    validationError = null
                    invalidate()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to complete ride", e)
                withContext(Dispatchers.Main) {
                    validationError = "Erreur lors de la validation"
                    invalidate()
                }
            }
        }
    }

    private fun loadRideAndSettings() {
        loadJob = scope.launch {
            try {
                val context = carContext
                val database = AppDatabase.getDatabase(context)
                val settingsManager = SettingsManager(context)
                repository = TaxiRepository(database.rideDao(), database.quoteDao(), database.absenceDao(), settingsManager)

                settings = settingsManager.settingsFlow.first()

                repository?.let { repo ->
                    ride = repo.getRideById(rideId)
                    ride?.let {
                        distanceKm = it.distanceKm
                        price = it.price
                    }
                }

                isLoading = false
                withContext(Dispatchers.Main) {
                    invalidate()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load ride and settings", e)
                isLoading = false
                withContext(Dispatchers.Main) {
                    invalidate()
                }
            }
        }
    }


}
