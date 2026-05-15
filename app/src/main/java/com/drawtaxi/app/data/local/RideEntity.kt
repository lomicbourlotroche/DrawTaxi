package com.drawtaxi.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.drawtaxi.app.data.MessageChannel
import com.drawtaxi.app.data.Quote
import com.drawtaxi.app.data.QuoteStatus
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.RideStatus

@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val body: String,
    val departure: String,
    val arrival: String,
    val time: String,
    val distanceKm: Double,
    val timestamp: Long,
    val isPending: Boolean,
    val date: String = "",
    val price: Double = 0.0,
    val invoiceNumber: String = "",
    val notes: String = "",
    val clientName: String = "",
    val status: String = "DRAFT",
    val clientEmail: String = "",
    val messageChannel: String = "SMS",
    val quoteId: String = "",
    val absenceMessageSent: Boolean = false,
    val fuelCost: Double = 0.0,
    val operatingCost: Double = 0.0,
    val durationMinutes: Int = 0,
    val profitabilityPercent: Double = 0.0,
    val homeAddress: String = "",
    val distanceReelleKm: Double = 0.0,
    val waitMinutes: Int = 0,
    val priceBreakdown: String = "",
    val latitudeDepart: Double = 0.0,
    val longitudeDepart: Double = 0.0,
    val latitudeDestination: Double = 0.0,
    val longitudeDestination: Double = 0.0,
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
    val isTracking: Boolean = false,
    val lastLatitude: Double = 0.0,
    val lastLongitude: Double = 0.0,
    val destinationModifiee: String = "",
    val clientFirstName: String = "",
    val clientLastName: String = "",
    val clientPhone: String = "",
    val hasMissingInfo: Boolean = false,
    val missingFieldsList: String = ""
)

fun RideEntity.toDomain() = RideRequest(
    id = id,
    sender = sender,
    body = body,
    departure = departure,
    arrival = arrival,
    time = time,
    distanceKm = distanceKm,
    timestamp = timestamp,
    isPending = isPending,
    date = date,
    price = price,
    invoiceNumber = invoiceNumber,
    notes = notes,
    clientName = clientName,
    status = try { RideStatus.valueOf(status) } catch (e: Exception) { RideStatus.DRAFT },
    clientEmail = clientEmail,
    messageChannel = try { MessageChannel.valueOf(messageChannel) } catch (e: Exception) { MessageChannel.SMS },
    quoteId = quoteId,
    absenceMessageSent = absenceMessageSent,
    fuelCost = fuelCost,
    operatingCost = operatingCost,
    durationMinutes = durationMinutes,
    profitabilityPercent = profitabilityPercent,
    homeAddress = homeAddress,
    distanceReelleKm = distanceReelleKm,
    waitMinutes = waitMinutes,
    priceBreakdown = priceBreakdown,
    latitudeDepart = latitudeDepart,
    longitudeDepart = longitudeDepart,
    latitudeDestination = latitudeDestination,
    longitudeDestination = longitudeDestination,
    startedAt = startedAt,
    endedAt = endedAt,
    isTracking = isTracking,
    lastLatitude = lastLatitude,
    lastLongitude = lastLongitude,
    destinationModifiee = destinationModifiee,
    clientFirstName = clientFirstName,
    clientLastName = clientLastName,
    clientPhone = clientPhone,
    hasMissingInfo = hasMissingInfo,
    missingFieldsList = missingFieldsList
)

fun RideRequest.toEntity() = RideEntity(
    id = id,
    sender = sender,
    body = body,
    departure = departure,
    arrival = arrival,
    time = time,
    distanceKm = distanceKm,
    timestamp = timestamp,
    isPending = isPending,
    date = date,
    price = price,
    invoiceNumber = invoiceNumber,
    notes = notes,
    clientName = clientName,
    status = status.name,
    clientEmail = clientEmail,
    messageChannel = messageChannel.name,
    quoteId = quoteId,
    absenceMessageSent = absenceMessageSent,
    fuelCost = fuelCost,
    operatingCost = operatingCost,
    durationMinutes = durationMinutes,
    profitabilityPercent = profitabilityPercent,
    homeAddress = homeAddress,
    distanceReelleKm = distanceReelleKm,
    waitMinutes = waitMinutes,
    priceBreakdown = priceBreakdown,
    latitudeDepart = latitudeDepart,
    longitudeDepart = longitudeDepart,
    latitudeDestination = latitudeDestination,
    longitudeDestination = longitudeDestination,
    startedAt = startedAt,
    endedAt = endedAt,
    isTracking = isTracking,
    lastLatitude = lastLatitude,
    lastLongitude = lastLongitude,
    destinationModifiee = destinationModifiee,
    clientFirstName = clientFirstName,
    clientLastName = clientLastName,
    clientPhone = clientPhone,
    hasMissingInfo = hasMissingInfo,
    missingFieldsList = missingFieldsList
)

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey val id: String,
    val rideId: String,
    val departure: String,
    val arrival: String,
    val distanceKm: Double,
    val price: Double,
    val timestamp: Long,
    val status: String = "PENDING",
    val sentAt: Long = 0L,
    val respondedAt: Long = 0L,
    val messageChannel: String = "SMS"
)

fun QuoteEntity.toDomain() = Quote(
    id = id,
    rideId = rideId,
    departure = departure,
    arrival = arrival,
    distanceKm = distanceKm,
    price = price,
    timestamp = timestamp,
    status = try { QuoteStatus.valueOf(status) } catch (e: Exception) { QuoteStatus.PENDING },
    sentAt = sentAt,
    respondedAt = respondedAt,
    messageChannel = try { MessageChannel.valueOf(messageChannel) } catch (e: Exception) { MessageChannel.SMS }
)

fun Quote.toEntity() = QuoteEntity(
    id = id,
    rideId = rideId,
    departure = departure,
    arrival = arrival,
    distanceKm = distanceKm,
    price = price,
    timestamp = timestamp,
    status = status.name,
    sentAt = sentAt,
    respondedAt = respondedAt,
    messageChannel = messageChannel.name
)

@Entity(tableName = "absences")
data class AbsenceEntity(
    @PrimaryKey val id: String,
    val startDate: Long,
    val endDate: Long,
    val reason: String = "",
    val autoSendMessage: Boolean = true,
    val messageSent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
