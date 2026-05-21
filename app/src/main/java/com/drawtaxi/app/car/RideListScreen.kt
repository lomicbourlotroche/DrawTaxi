package com.drawtaxi.app.car

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
import java.text.SimpleDateFormat
import java.util.*

class RideListScreen(carContext: CarContext) : Screen(carContext) {

    companion object {
        private const val TAG = "RideListScreen"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pendingRides = listOf<RideRequest>()
    private var isLoaded = false
    private var repository: TaxiRepository? = null
    private var loadJob: Job? = null

    init {
        loadRides()
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        if (pendingRides.isEmpty() && isLoaded) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Aucune course en attente")
                    .addText(SpannableString("Les nouvelles courses apparaitront ici"))
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, android.R.drawable.ic_menu_info_details)
                        ).build()
                    )
                    .build()
            )
        }

        val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())

        pendingRides.take(20).forEach { ride ->
            listBuilder.addItem(createRideListItem(ride, dateFormat))
        }

        return ListTemplate.Builder()
            .setTitle("DrawTaxi - Courses en attente")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(
                listBuilder.build()
            )
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Actualiser")
                            .setIcon(
                                CarIcon.Builder(
                                    IconCompat.createWithResource(carContext, android.R.drawable.ic_menu_rotate)
                                ).build()
                            )
                            .setOnClickListener { refreshRides() }
                            .build()
                    )
                    .addAction(
                        Action.Builder()
                            .setTitle("Menu")
                            .setIcon(
                                CarIcon.Builder(
                                    IconCompat.createWithResource(carContext, android.R.drawable.ic_menu_more)
                                ).build()
                            )
                            .setOnClickListener { 
                                screenManager.push(CarMenuScreen(carContext))
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun createRideListItem(ride: RideRequest, dateFormat: SimpleDateFormat): Item {
        val arrival = ride.arrival.ifBlank { "Destination non définie" }
        val departure = ride.departure.ifBlank { "" }
        val sender = ride.sender.ifBlank { "Client" }
        val timeStr = if (ride.time.isNotBlank()) ride.time else ""
        val dateStr = if (ride.timestamp > 0L) dateFormat.format(Date(ride.timestamp)) else ""
        val displayTime = if (timeStr.isNotBlank()) timeStr else dateStr

        val subtitle = buildString {
            append(sender)
            if (displayTime.isNotBlank()) {
                append(" • $displayTime")
            }
            if (departure.isNotBlank()) {
                append(" • $departure")
            }
        }

        val rowBuilder = Row.Builder()
            .setTitle(arrival)
            .addText(SpannableString(subtitle))
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

    private fun refreshRides() {
        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                val currentRides = repository?.getPendingRidesList() ?: emptyList()
                pendingRides = currentRides
                isLoaded = true
                withContext(Dispatchers.Main) {
                    invalidate()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Refresh failed", e)
            }
        }
    }

    private fun loadRides() {
        loadJob = scope.launch {
            try {
                val context = carContext
                val database = AppDatabase.getDatabase(context)
                val settingsManager = SettingsManager(context)
                repository = TaxiRepository(database.rideDao(), database.quoteDao(), database.absenceDao(), settingsManager)

                repository?.let { repo ->
                    repo.pendingRides.collect { rides ->
                        if (!isActive) return@collect
                        pendingRides = rides
                        isLoaded = true
                        withContext(Dispatchers.Main) {
                            invalidate()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load rides", e)
                isLoaded = true
                withContext(Dispatchers.Main) {
                    invalidate()
                }
            }
        }
    }


}
