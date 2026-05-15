package com.drawtaxi.app.car

import android.content.Intent
import android.net.Uri
import android.text.SpannableString
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

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var ride: RideRequest? = null

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
                .setTitle("Course")
                .build()
        }

        val paneBuilder = Pane.Builder()

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Client")
                .addText(SpannableString(rideData.sender.ifBlank { "Inconnu" }))
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
                    .setTitle("Départ")
                    .addText(SpannableString(rideData.departure))
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
                    .setTitle("Arrivée")
                    .addText(SpannableString(rideData.arrival))
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
                    .setTitle("Heure")
                    .addText(SpannableString(rideData.time))
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
                    .setTitle("Distance")
                    .addText(SpannableString(String.format("%.1f km", rideData.distanceKm)))
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
                    .setTitle("Prix")
                    .addText(SpannableString(String.format("%.2f €", rideData.price)))
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, android.R.drawable.ic_menu_manage)
                        ).build()
                    )
                    .build()
            )
        }

        paneBuilder.addAction(
            Action.Builder()
                .setTitle("Appeler")
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener {
                    val phoneNumber = rideData.sender
                    if (phoneNumber.isNotBlank()) {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phoneNumber")
                        }
                        carContext.startCarApp(intent)
                    }
                }
                .build()
        )

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
                            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}")
                            }
                            carContext.startCarApp(webIntent)
                        }
                    }
                }
                .build()
        )

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Course")
            .build()
    }

    private fun loadRide() {
        scope.launch {
            try {
                val context = carContext
                val database = AppDatabase.getDatabase(context)
                val settingsManager = SettingsManager(context)
                val repository = TaxiRepository(database.rideDao(), database.quoteDao(), database.absenceDao(), settingsManager)

                val allRides = repository.getAllRides()
                ride = allRides.find { it.id == rideId }

                withContext(Dispatchers.Main) {
                    invalidate()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
