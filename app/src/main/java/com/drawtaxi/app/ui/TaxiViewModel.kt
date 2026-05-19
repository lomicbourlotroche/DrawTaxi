package com.drawtaxi.app.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drawtaxi.app.data.Absence
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.MessageChannel
import com.drawtaxi.app.data.Quote
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.RideStatus
import com.drawtaxi.app.data.StatsReport
import com.drawtaxi.app.data.QuoteStatus
import com.drawtaxi.app.data.TaxiRepository
import com.drawtaxi.app.logic.messaging.NotificationHelper
import com.drawtaxi.app.logic.sms.parseSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TaxiViewModel(private val repository: TaxiRepository) : ViewModel() {

    val validatedRides: StateFlow<List<RideRequest>> = repository.validatedRides
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRides: StateFlow<List<RideRequest>> = repository.pendingRides
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestRide: StateFlow<RideRequest?> = repository.latestRide
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val allQuotes: StateFlow<List<Quote>> = repository.allQuotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingQuotes: StateFlow<List<Quote>> = repository.pendingQuotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAbsences: StateFlow<List<Absence>> = repository.allAbsences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun validateRide(id: String) {
        viewModelScope.launch {
            repository.validateRide(id)
        }
    }

    fun deleteRide(ride: RideRequest) {
        viewModelScope.launch {
            repository.deleteRide(ride)
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
        }
    }

    fun addRide(ride: RideRequest) {
        viewModelScope.launch {
            repository.saveRide(ride)
        }
    }

    fun updateRide(ride: RideRequest) {
        viewModelScope.launch {
            repository.updateRide(ride)
        }
    }

    fun updateRideStatus(rideId: String, status: RideStatus) {
        viewModelScope.launch {
            repository.updateRideStatus(rideId, status)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun testNotification(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            NotificationHelper.showNewRideNotification(
                context,
                "test_id",
                "Test Destination",
                "12:00"
            )
        }
    }

    fun refreshRides(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val scannedRides = com.drawtaxi.app.logic.sms.SmsScanner.scanLastHourSms(context)
            scannedRides.forEach { ride ->
                repository.saveRide(ride)
            }
        }
    }

    fun scanSmsNow(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.drawtaxi.app.service.foreground.SmsForegroundService.triggerScan()

                val scannedRides = com.drawtaxi.app.logic.sms.SmsScanner.scanLastHourSms(context)
                scannedRides.forEach { ride ->
                    repository.saveRide(ride)
                }
                Log.d("TaxiViewModel", "Scan manuel terminé: ${scannedRides.size} courses trouvées")
            } catch (e: Exception) {
                Log.e("TaxiViewModel", "Erreur scan manuel: ${e.message}")
            }
        }
    }

    fun parseSmsWithAI(context: Context, sender: String, body: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings = repository.getSettingsSync()
                val ride = com.drawtaxi.app.logic.sms.SmsScanner.parseSmsWithAI(context, sender, body, timestamp, settings.aiEnabled)
                ride?.let {
                    repository.saveRide(it)
                    Log.d("TaxiViewModel", "Course AI ajoutée: ${it.departure} → ${it.arrival}")
                }
            } catch (e: Exception) {
                Log.e("TaxiViewModel", "Erreur parsing AI: ${e.message}")
                val fallbackRide = parseSms(sender, body, timestamp)
                fallbackRide?.let { repository.saveRide(it) }
            }
        }
    }

    fun createQuoteForRide(ride: RideRequest, distanceKm: Double, price: Double, channel: MessageChannel = MessageChannel.SMS) {
        viewModelScope.launch {
            val quote = Quote(
                id = Quote.createId(ride.id),
                rideId = ride.id,
                departure = ride.departure,
                arrival = ride.arrival,
                distanceKm = distanceKm,
                price = price,
                messageChannel = channel
            )
            repository.saveQuote(quote)
            repository.setQuoteForRide(ride.id, quote.id)
            repository.updateRideStatus(ride.id, RideStatus.QUOTED)
            Log.d("TaxiViewModel", "Devis créé pour la course ${ride.id}")
        }
    }

    fun sendQuote(ride: RideRequest, quote: Quote, context: Context) {
        viewModelScope.launch {
            repository.saveQuote(quote)
            repository.setQuoteForRide(ride.id, quote.id)
            repository.updateRideStatus(ride.id, RideStatus.QUOTED)

            val quoteMessage = repository.getSettingsSync().quoteTemplate
                .replace("[DEPART]", quote.departure)
                .replace("[ARRIVEE]", quote.arrival)
                .replace("[DISTANCE]", String.format("%.1f", quote.distanceKm))
                .replace("[PRIX]", String.format("%.2f", quote.price))

            com.drawtaxi.app.logic.messaging.MessageSender.sendMessage(
                context = context,
                channel = quote.messageChannel,
                phone = ride.sender,
                email = ride.clientEmail,
                message = quoteMessage
            )
            repository.markQuoteSent(quote.id)
            Log.d("TaxiViewModel", "Devis envoyé pour la course ${ride.id}")
        }
    }

    fun acceptQuote(quote: Quote) {
        viewModelScope.launch {
            repository.updateQuoteStatus(quote.id, QuoteStatus.ACCEPTED)
            repository.updateRideStatus(quote.rideId, RideStatus.CONFIRMED)
            val ride = repository.getPendingRidesList().find { it.id == quote.rideId }
            ride?.let {
                val updatedRide = it.copy(
                    price = quote.price,
                    distanceKm = quote.distanceKm,
                    status = RideStatus.CONFIRMED
                )
                repository.updateRide(updatedRide)
            }
            Log.d("TaxiViewModel", "Devis accepté: ${quote.id}")
        }
    }

    fun rejectQuote(quote: Quote) {
        viewModelScope.launch {
            repository.updateQuoteStatus(quote.id, QuoteStatus.REJECTED)
            repository.updateRideStatus(quote.rideId, RideStatus.CANCELLED)
            val ride = repository.getPendingRidesList().find { it.id == quote.rideId }
            ride?.let {
                repository.deleteRide(it)
            }
            Log.d("TaxiViewModel", "Devis rejeté: ${quote.id}")
        }
    }

    fun markQuoteSent(quoteId: String) {
        viewModelScope.launch {
            repository.markQuoteSent(quoteId)
        }
    }

    fun deleteRideWithMessage(ride: RideRequest, message: String, context: Context) {
        viewModelScope.launch {
            repository.updateRideStatus(ride.id, RideStatus.CANCELLED)
            repository.deleteRide(ride)
            com.drawtaxi.app.logic.messaging.MessageSender.sendMessage(
                context = context,
                channel = ride.messageChannel,
                phone = ride.sender,
                email = ride.clientEmail,
                message = message
            )
            Log.d("TaxiViewModel", "Course supprimée avec message: ${ride.id}")
        }
    }

    fun addAbsence(absence: Absence) {
        viewModelScope.launch {
            repository.saveAbsence(absence)
        }
    }

    fun deleteAbsence(absence: Absence) {
        viewModelScope.launch {
            repository.deleteAbsence(absence)
        }
    }

    fun markAbsenceMessageSent(absenceId: String) {
        viewModelScope.launch {
            repository.markAbsenceMessageSent(absenceId)
        }
    }

    fun completeRide(ride: RideRequest, context: Context) {
        viewModelScope.launch {
            repository.validateRide(ride.id)
            repository.updateRide(ride)
            Log.d("TaxiViewModel", "Course terminée: ${ride.id}")
        }
    }

    fun generateWeeklyStatsReport(): StatsReport? {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val weekAgo = calendar.timeInMillis

        val weekRides = validatedRides.value.filter { it.timestamp >= weekAgo }
        if (weekRides.isEmpty()) return null

        return StatsReport(
            id = "weekly_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}",
            type = "weekly",
            startDate = weekAgo,
            endDate = now,
            totalRides = weekRides.size,
            totalRevenue = weekRides.sumOf { it.price },
            totalKm = weekRides.sumOf { it.distanceKm },
            averagePrice = weekRides.map { it.price }.average(),
            averageDistance = weekRides.map { it.distanceKm }.average()
        )
    }

    fun generateMonthlyStatsReport(): StatsReport? {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.add(Calendar.MONTH, -1)
        val monthAgo = calendar.timeInMillis

        val monthRides = validatedRides.value.filter { it.timestamp >= monthAgo }
        if (monthRides.isEmpty()) return null

        return StatsReport(
            id = "monthly_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}",
            type = "monthly",
            startDate = monthAgo,
            endDate = now,
            totalRides = monthRides.size,
            totalRevenue = monthRides.sumOf { it.price },
            totalKm = monthRides.sumOf { it.distanceKm },
            averagePrice = monthRides.map { it.price }.average(),
            averageDistance = monthRides.map { it.distanceKm }.average()
        )
    }
}

class TaxiViewModelFactory(private val repository: TaxiRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaxiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaxiViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
