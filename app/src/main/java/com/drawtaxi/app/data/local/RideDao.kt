package com.drawtaxi.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Query("SELECT * FROM rides WHERE isPending = 0 ORDER BY timestamp DESC")
    fun getValidatedRides(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE isPending = 1 ORDER BY timestamp DESC")
    fun getPendingRides(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides ORDER BY timestamp DESC LIMIT 1")
    fun getLatestRide(): Flow<RideEntity?>

    @Query("SELECT * FROM rides ORDER BY timestamp DESC")
    suspend fun getAllRides(): List<RideEntity>

    @Query("SELECT * FROM rides WHERE isPending = 1 ORDER BY timestamp DESC")
    suspend fun getPendingRidesList(): List<RideEntity>

    @Query("SELECT * FROM rides WHERE id = :rideId LIMIT 1")
    suspend fun getRideById(rideId: String): RideEntity?

    @Query("SELECT * FROM rides WHERE status = :status ORDER BY timestamp DESC")
    suspend fun getRidesByStatus(status: String): List<RideEntity>

    @Query("SELECT * FROM rides WHERE status = :status ORDER BY timestamp DESC")
    fun getRidesByStatusFlow(status: String): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getRidesInTimeRange(startTime: Long, endTime: Long): List<RideEntity>

    @Query("UPDATE rides SET isPending = 0, status = 'COMPLETED' WHERE id = :rideId")
    suspend fun validateRide(rideId: String)

    @Query("UPDATE rides SET status = :status WHERE id = :rideId")
    suspend fun updateRideStatus(rideId: String, status: String)

    @Query("UPDATE rides SET quoteId = :quoteId WHERE id = :rideId")
    suspend fun setQuoteForRide(rideId: String, quoteId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRide(ride: RideEntity)

    @Update
    suspend fun updateRide(ride: RideEntity)

    @Delete
    suspend fun deleteRide(ride: RideEntity)

    @Query("DELETE FROM rides")
    suspend fun deleteAllRides()

    @Query("DELETE FROM rides WHERE status = 'CANCELLED'")
    suspend fun deleteCancelledRides()
}

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes ORDER BY timestamp DESC")
    fun getAllQuotes(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE rideId = :rideId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestQuoteForRide(rideId: String): QuoteEntity?

    @Query("SELECT * FROM quotes WHERE rideId = :rideId ORDER BY timestamp DESC")
    fun getQuotesForRide(rideId: String): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingQuotes(): Flow<List<QuoteEntity>>

    @Query("UPDATE quotes SET status = :status, respondedAt = :respondedAt WHERE id = :quoteId")
    suspend fun updateQuoteStatus(quoteId: String, status: String, respondedAt: Long = System.currentTimeMillis())

    @Query("UPDATE quotes SET sentAt = :sentAt WHERE id = :quoteId")
    suspend fun markQuoteSent(quoteId: String, sentAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: QuoteEntity)

    @Delete
    suspend fun deleteQuote(quote: QuoteEntity)

    @Query("DELETE FROM quotes WHERE rideId = :rideId")
    suspend fun deleteQuotesForRide(rideId: String)
}

@Dao
interface AbsenceDao {
    @Query("SELECT * FROM absences ORDER BY startDate DESC")
    fun getAllAbsences(): Flow<List<AbsenceEntity>>

    @Query("SELECT * FROM absences WHERE startDate <= :date AND endDate >= :date LIMIT 1")
    suspend fun getCurrentAbsence(date: Long = System.currentTimeMillis()): AbsenceEntity?

    @Query("SELECT * FROM absences WHERE startDate <= :endDate AND endDate >= :startDate")
    suspend fun getAbsencesInRange(startDate: Long, endDate: Long): List<AbsenceEntity>

    @Query("SELECT * FROM absences WHERE messageSent = 0 AND autoSendMessage = 1 AND startDate <= :currentTime")
    suspend fun getPendingAbsenceMessages(currentTime: Long = System.currentTimeMillis()): List<AbsenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbsence(absence: AbsenceEntity)

    @Update
    suspend fun updateAbsence(absence: AbsenceEntity)

    @Delete
    suspend fun deleteAbsence(absence: AbsenceEntity)

    @Query("UPDATE absences SET messageSent = 1 WHERE id = :absenceId")
    suspend fun markMessageSent(absenceId: String)

    @Query("DELETE FROM absences WHERE endDate < :date")
    suspend fun deletePastAbsences(date: Long = System.currentTimeMillis())
}
