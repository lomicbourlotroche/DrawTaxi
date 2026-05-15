package com.drawtaxi.app.car

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

class RideListScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pendingRides = mutableListOf<RideRequest>()
    private var isLoaded = false

    init {
        loadRides()
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        if (pendingRides.isEmpty() && isLoaded) {
            return PaneTemplate.Builder(
                Pane.Builder()
                    .addRow(
                        Row.Builder()
                            .setTitle("Aucune course en attente")
                            .build()
                    )
                    .build()
            )
                .setTitle("DrawTaxi")
                .build()
        }

        pendingRides.take(10).forEach { ride ->
            listBuilder.addItem(createRideListItem(ride))
        }

        return ListTemplate.Builder()
            .setTitle("DrawTaxi")
            .setSingleList(
                listBuilder.build()
            )
            .build()
    }

    private fun createRideListItem(ride: RideRequest): Item {
        val arrival = ride.arrival.ifBlank { "Destination non définie" }
        val time = if (ride.time.isNotBlank()) ride.time else ""
        val sender = ride.sender.ifBlank { "Client" }

        val rowBuilder = Row.Builder()
            .setTitle(arrival)
            .addText(SpannableString("$sender • $time"))
            .setImage(
                CarIcon.Builder(
                    IconCompat.createWithResource(carContext, android.R.drawable.ic_menu_directions)
                ).build()
            )

        rowBuilder.setOnClickListener {
            screenManager.push(RideDetailCarScreen(carContext, ride.id))
        }

        return rowBuilder.build()
    }

    private fun loadRides() {
        scope.launch {
            try {
                val context = carContext
                val database = AppDatabase.getDatabase(context)
                val settingsManager = SettingsManager(context)
                val repository = TaxiRepository(database.rideDao(), database.quoteDao(), database.absenceDao(), settingsManager)

                pendingRides.clear()

                repository.pendingRides.let { flow ->
                    flow.collect { rides ->
                        pendingRides.clear()
                        pendingRides.addAll(rides)
                        isLoaded = true
                        withContext(Dispatchers.Main) {
                            invalidate()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isLoaded = true
            }
        }
    }
}
