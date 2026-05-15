package com.drawtaxi.app.data

import com.drawtaxi.app.data.local.AbsenceDao
import com.drawtaxi.app.data.local.AbsenceEntity
import com.drawtaxi.app.data.local.QuoteDao
import com.drawtaxi.app.data.local.QuoteEntity
import com.drawtaxi.app.data.local.RideDao
import com.drawtaxi.app.data.local.SettingsManager
import com.drawtaxi.app.data.local.toDomain
import com.drawtaxi.app.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TaxiRepository(
    private val rideDao: RideDao,
    private val quoteDao: QuoteDao,
    private val absenceDao: AbsenceDao,
    private val settingsManager: SettingsManager
) {
    val validatedRides: Flow<List<RideRequest>> = rideDao.getValidatedRides().map { entities ->
        entities.map { it.toDomain() }
    }

    val pendingRides: Flow<List<RideRequest>> = rideDao.getPendingRides().map { entities ->
        entities.map { it.toDomain() }
    }

    val latestRide: Flow<RideRequest?> = rideDao.getLatestRide().map { it?.toDomain() }

    val settings: Flow<AppSettings> = settingsManager.settingsFlow

    val allQuotes: Flow<List<Quote>> = quoteDao.getAllQuotes().map { entities ->
        entities.map { it.toDomain() }
    }

    val pendingQuotes: Flow<List<Quote>> = quoteDao.getPendingQuotes().map { entities ->
        entities.map { it.toDomain() }
    }

    val allAbsences: Flow<List<Absence>> = absenceDao.getAllAbsences().map { entities ->
        entities.map { entity ->
            Absence(
                id = entity.id,
                startDate = entity.startDate,
                endDate = entity.endDate,
                reason = entity.reason,
                autoSendMessage = entity.autoSendMessage,
                messageSent = entity.messageSent,
                createdAt = entity.createdAt
            )
        }
    }

    suspend fun saveRide(ride: RideRequest) {
        rideDao.insertRide(ride.toEntity())
    }

    suspend fun validateRide(id: String) {
        rideDao.validateRide(id)
    }

    suspend fun updateSettings(settings: AppSettings) {
        settingsManager.updateSettings(settings)
    }

    suspend fun updateRide(ride: RideRequest) {
        rideDao.updateRide(ride.toEntity())
    }

    suspend fun updateRideStatus(rideId: String, status: RideStatus) {
        rideDao.updateRideStatus(rideId, status.name)
    }

    suspend fun deleteRide(ride: RideRequest) {
        rideDao.deleteRide(ride.toEntity())
    }

    suspend fun clearHistory() {
        rideDao.deleteAllRides()
    }

    suspend fun getAllRides(): List<RideRequest> {
        return rideDao.getAllRides().map { it.toDomain() }
    }
    
    suspend fun getPendingRidesList(): List<RideRequest> {
        return rideDao.getPendingRidesList().map { it.toDomain() }
    }

    suspend fun getRidesByStatus(status: RideStatus): List<RideRequest> {
        return rideDao.getRidesByStatus(status.name).map { it.toDomain() }
    }

    fun getRidesByStatusFlow(status: RideStatus): Flow<List<RideRequest>> {
        return rideDao.getRidesByStatusFlow(status.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getRidesInTimeRange(startTime: Long, endTime: Long): List<RideRequest> {
        return rideDao.getRidesInTimeRange(startTime, endTime).map { it.toDomain() }
    }

    suspend fun setQuoteForRide(rideId: String, quoteId: String) {
        rideDao.setQuoteForRide(rideId, quoteId)
    }
    
    suspend fun getSettingsSync(): AppSettings {
        return settings.first()
    }

    suspend fun saveQuote(quote: Quote) {
        quoteDao.insertQuote(quote.toEntity())
    }

    suspend fun updateQuoteStatus(quoteId: String, status: QuoteStatus) {
        quoteDao.updateQuoteStatus(quoteId, status.name)
    }

    suspend fun markQuoteSent(quoteId: String) {
        quoteDao.markQuoteSent(quoteId)
    }

    suspend fun getLatestQuoteForRide(rideId: String): Quote? {
        return quoteDao.getLatestQuoteForRide(rideId)?.toDomain()
    }

    fun getQuotesForRide(rideId: String): Flow<List<Quote>> {
        return quoteDao.getQuotesForRide(rideId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun deleteQuotesForRide(rideId: String) {
        quoteDao.deleteQuotesForRide(rideId)
    }

    suspend fun saveAbsence(absence: Absence) {
        absenceDao.insertAbsence(AbsenceEntity(
            id = absence.id,
            startDate = absence.startDate,
            endDate = absence.endDate,
            reason = absence.reason,
            autoSendMessage = absence.autoSendMessage,
            messageSent = absence.messageSent,
            createdAt = absence.createdAt
        ))
    }

    suspend fun deleteAbsence(absence: Absence) {
        absenceDao.deleteAbsence(AbsenceEntity(
            id = absence.id,
            startDate = absence.startDate,
            endDate = absence.endDate,
            reason = absence.reason,
            autoSendMessage = absence.autoSendMessage,
            messageSent = absence.messageSent,
            createdAt = absence.createdAt
        ))
    }

    suspend fun getCurrentAbsence(): Absence? {
        return absenceDao.getCurrentAbsence()?.let { entity ->
            Absence(
                id = entity.id,
                startDate = entity.startDate,
                endDate = entity.endDate,
                reason = entity.reason,
                autoSendMessage = entity.autoSendMessage,
                messageSent = entity.messageSent,
                createdAt = entity.createdAt
            )
        }
    }

    suspend fun markAbsenceMessageSent(absenceId: String) {
        absenceDao.markMessageSent(absenceId)
    }

    suspend fun getPendingAbsenceMessages(): List<Absence> {
        return absenceDao.getPendingAbsenceMessages().map { entity ->
            Absence(
                id = entity.id,
                startDate = entity.startDate,
                endDate = entity.endDate,
                reason = entity.reason,
                autoSendMessage = entity.autoSendMessage,
                messageSent = entity.messageSent,
                createdAt = entity.createdAt
            )
        }
    }

    suspend fun deletePastAbsences() {
        absenceDao.deletePastAbsences()
    }
}
