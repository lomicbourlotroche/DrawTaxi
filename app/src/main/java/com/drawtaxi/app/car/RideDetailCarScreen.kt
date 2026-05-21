package com.drawtaxi.app.car

import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.local.AppDatabase
import com.drawtaxi.app.data.local.SettingsManager
import com.drawtaxi.app.data.TaxiRepository
import kotlinx.coroutines.*

class RideDetailCarScreen(carContext: CarContext, private val rideId: String) : Screen(carContext) {

    companion object {
        private const val TAG = "RideDetailCarScreen"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var ride: RideRequest? = null
    private var loadJob: Job? = null

    init {
        loadRide()
    }

    override fun onGetTemplate(): Template {
        val rideData = ride

        if (rideData == null) {
            return PaneTemplate.Builder(
                Pane.Builder()
                    .addRow(
                        Row.Builder()
                            .setTitle("Chargement...")
                            .build()
                    )
                    .build()
            )
                .setTitle("Détails course")
                .setHeaderAction(Action.BACK)
                .build()
        }

        val paneBuilder = Pane.Builder()

        val clientName = rideData.clientName.ifBlank {
            rideData.clientFirstName.ifBlank {
                rideData.sender.ifBlank { "Inconnu" }
            }
        }

        paneBuilder.addRow(
            Row.Builder()
                .setTitle(clientName)
                .addText(SpannableString("Client"))
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, android.R.drawable.ic_menu_call)
                    ).build()
                )
                .build()
        )

        if (rideData.departure.isNotBlank()) {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(rideData.departure)
                    .addText(SpannableString("Départ"))
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, android.R.drawable.ic_menu_mylocation)
                        ).build()
                    )
                    .build()
            )
        }

        if (rideData.arrival.isNotBlank()) {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(rideData.arrival)
                    .addText(SpannableString("Arrivée"))
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, android.R.drawable.ic_menu_directions)
                        ).build()
                    )
                    .build()
            )
        }

        if (rideData.time.isNotBlank()) {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(rideData.time)
                    .addText(SpannableString("Heure"))
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, android.R.drawable.ic_menu_recent_history)
                        ).build()
                    )
                    .build()
            )
        }

        if (rideData.distanceKm > 0) {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(String.format("%.1f km", rideData.distanceKm))
                    .addText(SpannableString("Distance"))
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, android.R.drawable.ic_menu_mapmode)
                        ).build()
                    )
                    .build()
            )
        }

        if (rideData.price > 0) {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(String.format("%.2f €", rideData.price))
                    .addText(SpannableString("Prix"))
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, android.R.drawable.ic_menu_manage)
                        ).build()
                    )
                    .build()
            )
        }

        val phoneNumber = rideData.clientPhone.ifBlank { rideData.sender }

        if (phoneNumber.isNotBlank()) {
            paneBuilder.addAction(
                Action.Builder()
                    .setTitle("Appeler")
                    .setBackgroundColor(CarColor.BLUE)
                    .setOnClickListener {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phoneNumber")
                        }
                        try {
                            carContext.startCarApp(intent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start dial intent", e)
                        }
                    }
                    .build()
            )
        }

        if (phoneNumber.isNotBlank()) {
            paneBuilder.addAction(
                Action.Builder()
                    .setTitle("WhatsApp")
                    .setBackgroundColor(CarColor.GREEN)
                    .setOnClickListener {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/${phoneNumber.replace("+", "").replace(" ", "")}")
                            }
                            carContext.startCarApp(intent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start WhatsApp intent", e)
                        }
                    }
                    .build()
            )
        }

        if (rideData.isPending) {
            paneBuilder.addAction(
                Action.Builder()
                    .setTitle("Course terminée")
                    .setBackgroundColor(CarColor.GREEN)
                    .setOnClickListener {
                        screenManager.push(EditRideCarScreen(carContext, rideData.id))
                    }
                    .build()
            )
        }

        if (rideData.arrival.isNotBlank()) {
            paneBuilder.addAction(
                Action.Builder()
                    .setTitle("Naviguer")
                    .setBackgroundColor(CarColor.BLUE)
                    .setOnClickListener {
                        val destination = rideData.arrival
                        if (destination.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
                                setPackage("com.google.android.apps.maps")
                            }
                            try {
                                carContext.startCarApp(intent)
                            } catch (e: Exception) {
                                Log.e(TAG, "Google Maps not available, using web fallback", e)
                                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}")
                                }
                                carContext.startCarApp(webIntent)
                            }
                        }
                    }
                    .build()
            )
        }

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Détails course")
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun loadRide() {
        loadJob = scope.launch {
            try {
                val context = carContext
                val database = AppDatabase.getDatabase(context)
                val settingsManager = SettingsManager(context)
                val repo = TaxiRepository(database.rideDao(), database.quoteDao(), database.absenceDao(), settingsManager)

                ride = repo.getRideById(rideId)

                withContext(Dispatchers.Main) {
                    invalidate()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load ride $rideId", e)
            }
        }
    }


}
