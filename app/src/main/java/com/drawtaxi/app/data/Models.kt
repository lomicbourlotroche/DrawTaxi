package com.drawtaxi.app.data

import androidx.compose.ui.graphics.Color
import com.drawtaxi.app.ui.theme.TaxiRed

enum class RideStatus(val label: String, val colorHex: String) {
    DRAFT("Brouillon", "#F59E0B"),
    QUOTED("Devis envoyé", "#3B82F6"),
    CONFIRMED("Confirmée", "#10B981"),
    IN_PROGRESS("En cours", "#8B5CF6"),
    COMPLETED("Terminée", "#6366F1"),
    CANCELLED("Annulée", "#F43F5E"),
    ABSENT("Absent", "#6B7280")
}

enum class MessageChannel {
    SMS,
    WHATSAPP,
    EMAIL,
    WEB_FORM
}

enum class QuoteStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

data class AppSettings(
    val companyName: String = "DrawTaxi",
    val name: String = "Jean Chauffeur",
    val address: String = "123 Avenue des Champs-Élysées",
    val city: String = "75008 Paris",
    val siret: String = "123 456 789 00012",
    val tva: String = "FR 12 123456789",
    val vehicle: String = "Tesla Model 3",
    val pricePerKm: String = "1.20",
    val basePrice: String = "2.60",
    val brandColor: Color = TaxiRed,
    val theme: String = "modern",
    val darkMode: Boolean = false,
    val showLogo: Boolean = true,
    val monitorSms: Boolean = true,
    val enableNotifications: Boolean = true,
    val trackLocation: Boolean = false,
    val signature: String = "Fait à [Ville], le [Date]",
    val missingInfoTemplate: String = "Bonjour, nous avons bien reçu votre demande. Il nous manque [FIELDS] pour confirmer votre course. Merci de compléter ces informations.",
    val arrivalMessageTemplate: String = "Bonjour, votre chauffeur est arrivé au point de rendez-vous.",
    val isFirstLaunch: Boolean = true,
    val autoBackupEnabled: Boolean = false,
    val autoBackupInterval: String = "daily",
    val lastBackupDate: Long = 0L,
    val backupReminderShown: Boolean = false,
    val messageTemplates: List<String> = defaultMessageTemplates,
    val clientEmail: String = "",
    val driverEmail: String = "",
    val absenceMessageTemplate: String = "Bonjour, je suis actuellement en congé du [DATE_DEBUT] au [DATE_FIN]. Je serai de retour le [DATE_RETOUR]. Merci de votre compréhension.",
    val quoteTemplate: String = "Bonjour, voici le devis pour votre course :\n\nTrajet : [DEPART] → [ARRIVEE]\nDistance : [DISTANCE] km\nPrix : [PRIX] €\n\nMerci de confirmer votre acceptation en répondant OUI.",
    val rejectionTemplate: String = "Bonjour, votre demande de course a été refusée. N'hésitez pas à nous recontacter.",
    val invoiceTemplate: String = "Bonjour, veuillez trouver ci-joint la facture pour votre course.\n\nCordialement,\n[CHAUFFEUR]",
    val autoGenerateStatsReport: Boolean = true,
    val statsReportTime: String = "23:59",
    val fuelCostPerKm: Double = 0.12,
    val operatingCostPerHour: Double = 15.0,
    val nightSurchargePercent: Double = 0.15,
    val sundaySurchargePercent: Double = 0.10,
    val holidaySurchargePercent: Double = 0.15,
    val euroPerMinute: Double = 1.0,
    val nightStartHour: Int = 20,
    val nightEndHour: Int = 7,
    val tvaTransportRate: Double = 0.10,
    val tvaWaitTimeRate: Double = 0.20,
    val homeAddress: String = "",
    val smsScanIntervalMinutes: Int = 60,
    val aiEnabled: Boolean = true,
    
    // Configuration OVH SMTP (envoi mails)
    val ovhSmtpEnabled: Boolean = false,
    val ovhSmtpServer: String = "ssl0.ovh.net",
    val ovhSmtpPort: Int = 587,
    val ovhSmtpUsername: String = "",
    val ovhSmtpPassword: String = "",
    val ovhSmtpUseSsl: Boolean = true,
    val ovhFromEmail: String = "",
    val ovhFromName: String = "DrawTaxi",
    
    // Configuration OVH IMAP (réception mails)
    val ovhImapEnabled: Boolean = false,
    val ovhImapServer: String = "ssl0.ovh.net",
    val ovhImapPort: Int = 993,
    val ovhImapCheckInterval: Int = 5, // minutes
    val ovhImapFolder: String = "INBOX"
) {
    companion object {
        val defaultMessageTemplates = listOf(
            "Bonjour, je suis en retard de quelques minutes.",
            "Bonjour, j'arrive !",
            "Bonjour, où êtes-vous exactement ?",
            "Bonjour, je vous attends devant."
        )
    }
}

data class RideRequest(
    val id: String,
    val sender: String,
    val body: String,
    val departure: String = "",
    val arrival: String = "",
    val time: String = "",
    val distanceKm: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val isPending: Boolean = true,
    val date: String = "",
    val price: Double = 0.0,
    val invoiceNumber: String = "",
    val notes: String = "",
    val clientName: String = "",
    val status: RideStatus = RideStatus.DRAFT,
    val clientEmail: String = "",
    val messageChannel: MessageChannel = MessageChannel.SMS,
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
) {
    companion object {
        fun createStableId(sender: String, body: String, timestamp: Long): String {
            val raw = "$sender|$body|$timestamp"
            return java.util.UUID.nameUUIDFromBytes(raw.toByteArray()).toString()
        }

        fun calculateProfitability(price: Double, fuelCost: Double, operatingCost: Double): Double {
            val totalCost = fuelCost + operatingCost
            if (totalCost == 0.0 || price == 0.0) return 0.0
            return ((price - totalCost) / price) * 100.0
        }
    }
}

data class Quote(
    val id: String,
    val rideId: String,
    val departure: String,
    val arrival: String,
    val distanceKm: Double,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val status: QuoteStatus = QuoteStatus.PENDING,
    val sentAt: Long = 0L,
    val respondedAt: Long = 0L,
    val messageChannel: MessageChannel = MessageChannel.SMS
) {
    companion object {
        fun createId(rideId: String): String {
            return "quote_${rideId}_${System.currentTimeMillis()}"
        }
    }
}

data class Absence(
    val id: String,
    val startDate: Long,
    val endDate: Long,
    val reason: String = "",
    val autoSendMessage: Boolean = true,
    val messageSent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun createId(): String {
            return "absence_${System.currentTimeMillis()}"
        }
    }
}

data class Client(
    val id: String,
    val name: String,
    val phone: String,
    val email: String = "",
    val rideCount: Int = 0,
    val totalAmount: Double = 0.0,
    val lastRideDate: Long = 0L,
    val notes: String = ""
) {
    companion object {
        fun createStableId(phone: String): String {
            return java.util.UUID.nameUUIDFromBytes(phone.toByteArray()).toString()
        }
    }
}

data class StatsReport(
    val id: String,
    val type: String,
    val startDate: Long,
    val endDate: Long,
    val totalRides: Int,
    val totalRevenue: Double,
    val totalKm: Double,
    val averagePrice: Double,
    val averageDistance: Double,
    val timestamp: Long = System.currentTimeMillis()
)
