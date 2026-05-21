package com.drawtaxi.app.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.ui.theme.TaxiRed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    private object Keys {
        val COMPANY_NAME = stringPreferencesKey("company_name")
        val NAME = stringPreferencesKey("name")
        val ADDRESS = stringPreferencesKey("address")
        val CITY = stringPreferencesKey("city")
        val SIRET = stringPreferencesKey("siret")
        val TVA = stringPreferencesKey("tva")
        val VEHICLE = stringPreferencesKey("vehicle")
        val PRICE_PER_KM = stringPreferencesKey("price_per_km")
        val BASE_PRICE = stringPreferencesKey("base_price")
        val MIN_DISTANCE_KM = stringPreferencesKey("min_distance_km")
        val BRAND_COLOR = longPreferencesKey("brand_color")
        val THEME = stringPreferencesKey("theme")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val SHOW_LOGO = booleanPreferencesKey("show_logo")
        val MONITOR_SMS = booleanPreferencesKey("monitor_sms")
        val ENABLE_NOTIFICATIONS = booleanPreferencesKey("enable_notifications")
        val TRACK_LOCATION = booleanPreferencesKey("track_location")
        val SIGNATURE = stringPreferencesKey("signature")
        val MISSING_INFO_TEMPLATE = stringPreferencesKey("missing_info_template")
        val ARRIVAL_MESSAGE_TEMPLATE = stringPreferencesKey("arrival_message_template")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_INTERVAL = stringPreferencesKey("auto_backup_interval")
        val LAST_BACKUP_DATE = longPreferencesKey("last_backup_date")
        val BACKUP_REMINDER_SHOWN = booleanPreferencesKey("backup_reminder_shown")
        val MESSAGE_TEMPLATES = stringPreferencesKey("message_templates")
        val REJECTION_TEMPLATE = stringPreferencesKey("rejection_template")
        val INVOICE_TEMPLATE = stringPreferencesKey("invoice_template")
        val QUOTE_TEMPLATE = stringPreferencesKey("quote_template")
        val ABSENCE_MESSAGE_TEMPLATE = stringPreferencesKey("absence_message_template")
        val AUTO_GENERATE_STATS = booleanPreferencesKey("auto_generate_stats")
        val STATS_REPORT_TIME = stringPreferencesKey("stats_report_time")
        val FUEL_COST_PER_KM = floatPreferencesKey("fuel_cost_per_km")
        val OPERATING_COST_PER_HOUR = floatPreferencesKey("operating_cost_per_hour")
        val CLIENT_EMAIL = stringPreferencesKey("client_email")
        val DRIVER_EMAIL = stringPreferencesKey("driver_email")
        val NIGHT_SURCHARGE = floatPreferencesKey("night_surcharge")
        val SUNDAY_SURCHARGE = floatPreferencesKey("sunday_surcharge")
        val HOLIDAY_SURCHARGE = floatPreferencesKey("holiday_surcharge")
        val EURO_PER_MINUTE = floatPreferencesKey("euro_per_minute")
        val NIGHT_START_HOUR = intPreferencesKey("night_start_hour")
        val NIGHT_END_HOUR = intPreferencesKey("night_end_hour")
        val TVA_TRANSPORT = floatPreferencesKey("tva_transport")
        val TVA_WAIT = floatPreferencesKey("tva_wait")
        val HOME_ADDRESS = stringPreferencesKey("home_address")
        val SMS_SCAN_INTERVAL = intPreferencesKey("sms_scan_interval")
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val COUT_PAR_KM_DEPLACEMENT = floatPreferencesKey("cout_par_km_deplacement")
        
        // OVH SMTP
        val OVH_SMTP_ENABLED = booleanPreferencesKey("ovh_smtp_enabled")
        val OVH_SMTP_SERVER = stringPreferencesKey("ovh_smtp_server")
        val OVH_SMTP_PORT = intPreferencesKey("ovh_smtp_port")
        val OVH_SMTP_USERNAME = stringPreferencesKey("ovh_smtp_username")
        val OVH_SMTP_PASSWORD = stringPreferencesKey("ovh_smtp_password")
        val OVH_SMTP_USE_SSL = booleanPreferencesKey("ovh_smtp_use_ssl")
        val OVH_FROM_EMAIL = stringPreferencesKey("ovh_from_email")
        val OVH_FROM_NAME = stringPreferencesKey("ovh_from_name")
        
        // OVH IMAP
        val OVH_IMAP_ENABLED = booleanPreferencesKey("ovh_imap_enabled")
        val OVH_IMAP_SERVER = stringPreferencesKey("ovh_imap_server")
        val OVH_IMAP_PORT = intPreferencesKey("ovh_imap_port")
        val OVH_IMAP_CHECK_INTERVAL = intPreferencesKey("ovh_imap_check_interval")
        val OVH_IMAP_FOLDER = stringPreferencesKey("ovh_imap_folder")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val templatesJson = preferences[Keys.MESSAGE_TEMPLATES]
        val templates = if (templatesJson != null) {
            templatesJson.split("|||").filter { it.isNotBlank() }
        } else {
            AppSettings.defaultMessageTemplates
        }
        AppSettings(
            companyName = preferences[Keys.COMPANY_NAME] ?: "DrawTaxi",
            name = preferences[Keys.NAME] ?: "Jean Chauffeur",
            address = preferences[Keys.ADDRESS] ?: "123 Avenue des Champs-Élysées",
            city = preferences[Keys.CITY] ?: "75008 Paris",
            siret = preferences[Keys.SIRET] ?: "123 456 789 00012",
            tva = preferences[Keys.TVA] ?: "FR 12 123456789",
            vehicle = preferences[Keys.VEHICLE] ?: "Tesla Model 3",
            pricePerKm = preferences[Keys.PRICE_PER_KM] ?: "2.50",
            basePrice = preferences[Keys.BASE_PRICE] ?: "9.00",
            minDistanceKm = preferences[Keys.MIN_DISTANCE_KM] ?: "3.6",
            brandColor = preferences[Keys.BRAND_COLOR]?.let { Color(it.toULong()) } ?: TaxiRed,
            theme = preferences[Keys.THEME] ?: "modern",
            darkMode = preferences[Keys.DARK_MODE] ?: false,
            showLogo = preferences[Keys.SHOW_LOGO] ?: true,
            monitorSms = preferences[Keys.MONITOR_SMS] ?: true,
            enableNotifications = preferences[Keys.ENABLE_NOTIFICATIONS] ?: true,
            trackLocation = preferences[Keys.TRACK_LOCATION] ?: false,
            signature = preferences[Keys.SIGNATURE] ?: "Fait à [Ville], le [Date]",
            missingInfoTemplate = preferences[Keys.MISSING_INFO_TEMPLATE] ?: "DrawTaxi : Bonjour, il nous manque des infos pour votre course : [FIELDS]. Merci de nous les envoyer.",
            arrivalMessageTemplate = preferences[Keys.ARRIVAL_MESSAGE_TEMPLATE] ?: "DrawTaxi : Bonjour, votre chauffeur est arrivé au point de rendez-vous.",
            isFirstLaunch = preferences[Keys.IS_FIRST_LAUNCH] ?: true,
            autoBackupEnabled = preferences[Keys.AUTO_BACKUP_ENABLED] ?: false,
            autoBackupInterval = preferences[Keys.AUTO_BACKUP_INTERVAL] ?: "daily",
            lastBackupDate = preferences[Keys.LAST_BACKUP_DATE] ?: 0L,
            backupReminderShown = preferences[Keys.BACKUP_REMINDER_SHOWN] ?: false,
            messageTemplates = templates,
            clientEmail = preferences[Keys.CLIENT_EMAIL] ?: "",
            driverEmail = preferences[Keys.DRIVER_EMAIL] ?: "",
            absenceMessageTemplate = preferences[Keys.ABSENCE_MESSAGE_TEMPLATE] ?: "Bonjour, je suis actuellement en congé du [DATE_DEBUT] au [DATE_FIN]. Je serai de retour le [DATE_RETOUR]. Merci de votre compréhension.",
            quoteTemplate = preferences[Keys.QUOTE_TEMPLATE] ?: "Bonjour, voici le devis pour votre course :\n\nTrajet : [DEPART] → [ARRIVEE]\nDistance : [DISTANCE] km\nPrix : [PRIX] €\n\nMerci de confirmer votre acceptation en répondant OUI.",
            rejectionTemplate = preferences[Keys.REJECTION_TEMPLATE] ?: "Bonjour, votre demande de course a été refusée. N'hésitez pas à nous recontacter.",
            invoiceTemplate = preferences[Keys.INVOICE_TEMPLATE] ?: "Bonjour, veuillez trouver ci-joint le reçu pour votre course.\n\nCordialement,\n[CHAUFFEUR]",
            autoGenerateStatsReport = preferences[Keys.AUTO_GENERATE_STATS] ?: true,
            statsReportTime = preferences[Keys.STATS_REPORT_TIME] ?: "23:59",
            fuelCostPerKm = preferences[Keys.FUEL_COST_PER_KM]?.toDouble() ?: 0.12,
            operatingCostPerHour = preferences[Keys.OPERATING_COST_PER_HOUR]?.toDouble() ?: 15.0,
            nightSurchargePercent = preferences[Keys.NIGHT_SURCHARGE]?.toDouble() ?: 0.15,
            sundaySurchargePercent = preferences[Keys.SUNDAY_SURCHARGE]?.toDouble() ?: 0.10,
            holidaySurchargePercent = preferences[Keys.HOLIDAY_SURCHARGE]?.toDouble() ?: 0.15,
            euroPerMinute = preferences[Keys.EURO_PER_MINUTE]?.toDouble() ?: 1.0,
            nightStartHour = preferences[Keys.NIGHT_START_HOUR] ?: 20,
            nightEndHour = preferences[Keys.NIGHT_END_HOUR] ?: 7,
            tvaTransportRate = preferences[Keys.TVA_TRANSPORT]?.toDouble() ?: 0.10,
            tvaWaitTimeRate = preferences[Keys.TVA_WAIT]?.toDouble() ?: 0.20,
            homeAddress = preferences[Keys.HOME_ADDRESS] ?: "",
            smsScanIntervalMinutes = preferences[Keys.SMS_SCAN_INTERVAL] ?: 60,
            aiEnabled = preferences[Keys.AI_ENABLED] ?: true,
            coutParKmDeplacement = preferences[Keys.COUT_PAR_KM_DEPLACEMENT]?.toDouble() ?: 0.10,
            
            // OVH SMTP
            ovhSmtpEnabled = preferences[Keys.OVH_SMTP_ENABLED] ?: false,
            ovhSmtpServer = preferences[Keys.OVH_SMTP_SERVER] ?: "ssl0.ovh.net",
            ovhSmtpPort = preferences[Keys.OVH_SMTP_PORT] ?: 587,
            ovhSmtpUsername = preferences[Keys.OVH_SMTP_USERNAME] ?: "",
            ovhSmtpPassword = preferences[Keys.OVH_SMTP_PASSWORD] ?: "",
            ovhSmtpUseSsl = preferences[Keys.OVH_SMTP_USE_SSL] ?: true,
            ovhFromEmail = preferences[Keys.OVH_FROM_EMAIL] ?: "",
            ovhFromName = preferences[Keys.OVH_FROM_NAME] ?: "DrawTaxi",
            
            // OVH IMAP
            ovhImapEnabled = preferences[Keys.OVH_IMAP_ENABLED] ?: false,
            ovhImapServer = preferences[Keys.OVH_IMAP_SERVER] ?: "ssl0.ovh.net",
            ovhImapPort = preferences[Keys.OVH_IMAP_PORT] ?: 993,
            ovhImapCheckInterval = preferences[Keys.OVH_IMAP_CHECK_INTERVAL] ?: 5,
            ovhImapFolder = preferences[Keys.OVH_IMAP_FOLDER] ?: "INBOX"
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[Keys.COMPANY_NAME] = settings.companyName
            preferences[Keys.NAME] = settings.name
            preferences[Keys.ADDRESS] = settings.address
            preferences[Keys.CITY] = settings.city
            preferences[Keys.SIRET] = settings.siret
            preferences[Keys.TVA] = settings.tva
            preferences[Keys.VEHICLE] = settings.vehicle
            preferences[Keys.PRICE_PER_KM] = settings.pricePerKm
            preferences[Keys.BASE_PRICE] = settings.basePrice
            preferences[Keys.MIN_DISTANCE_KM] = settings.minDistanceKm
            preferences[Keys.BRAND_COLOR] = settings.brandColor.value.toLong()
            preferences[Keys.THEME] = settings.theme
            preferences[Keys.DARK_MODE] = settings.darkMode
            preferences[Keys.SHOW_LOGO] = settings.showLogo
            preferences[Keys.MONITOR_SMS] = settings.monitorSms
            preferences[Keys.ENABLE_NOTIFICATIONS] = settings.enableNotifications
            preferences[Keys.TRACK_LOCATION] = settings.trackLocation
            preferences[Keys.SIGNATURE] = settings.signature
            preferences[Keys.MISSING_INFO_TEMPLATE] = settings.missingInfoTemplate
            preferences[Keys.ARRIVAL_MESSAGE_TEMPLATE] = settings.arrivalMessageTemplate
            preferences[Keys.IS_FIRST_LAUNCH] = settings.isFirstLaunch
            preferences[Keys.AUTO_BACKUP_ENABLED] = settings.autoBackupEnabled
            preferences[Keys.AUTO_BACKUP_INTERVAL] = settings.autoBackupInterval
            preferences[Keys.LAST_BACKUP_DATE] = settings.lastBackupDate
            preferences[Keys.BACKUP_REMINDER_SHOWN] = settings.backupReminderShown
            preferences[Keys.MESSAGE_TEMPLATES] = settings.messageTemplates.joinToString("|||")
            preferences[Keys.CLIENT_EMAIL] = settings.clientEmail
            preferences[Keys.DRIVER_EMAIL] = settings.driverEmail
            preferences[Keys.ABSENCE_MESSAGE_TEMPLATE] = settings.absenceMessageTemplate
            preferences[Keys.QUOTE_TEMPLATE] = settings.quoteTemplate
            preferences[Keys.REJECTION_TEMPLATE] = settings.rejectionTemplate
            preferences[Keys.INVOICE_TEMPLATE] = settings.invoiceTemplate
            preferences[Keys.AUTO_GENERATE_STATS] = settings.autoGenerateStatsReport
            preferences[Keys.STATS_REPORT_TIME] = settings.statsReportTime
            preferences[Keys.FUEL_COST_PER_KM] = settings.fuelCostPerKm.toFloat()
            preferences[Keys.OPERATING_COST_PER_HOUR] = settings.operatingCostPerHour.toFloat()
            preferences[Keys.NIGHT_SURCHARGE] = settings.nightSurchargePercent.toFloat()
            preferences[Keys.SUNDAY_SURCHARGE] = settings.sundaySurchargePercent.toFloat()
            preferences[Keys.HOLIDAY_SURCHARGE] = settings.holidaySurchargePercent.toFloat()
            preferences[Keys.EURO_PER_MINUTE] = settings.euroPerMinute.toFloat()
            preferences[Keys.NIGHT_START_HOUR] = settings.nightStartHour
            preferences[Keys.NIGHT_END_HOUR] = settings.nightEndHour
            preferences[Keys.TVA_TRANSPORT] = settings.tvaTransportRate.toFloat()
            preferences[Keys.TVA_WAIT] = settings.tvaWaitTimeRate.toFloat()
            preferences[Keys.HOME_ADDRESS] = settings.homeAddress
            preferences[Keys.SMS_SCAN_INTERVAL] = settings.smsScanIntervalMinutes
            preferences[Keys.AI_ENABLED] = settings.aiEnabled
            preferences[Keys.COUT_PAR_KM_DEPLACEMENT] = settings.coutParKmDeplacement.toFloat()
            
            // OVH SMTP
            preferences[Keys.OVH_SMTP_ENABLED] = settings.ovhSmtpEnabled
            preferences[Keys.OVH_SMTP_SERVER] = settings.ovhSmtpServer
            preferences[Keys.OVH_SMTP_PORT] = settings.ovhSmtpPort
            preferences[Keys.OVH_SMTP_USERNAME] = settings.ovhSmtpUsername
            preferences[Keys.OVH_SMTP_PASSWORD] = settings.ovhSmtpPassword
            preferences[Keys.OVH_SMTP_USE_SSL] = settings.ovhSmtpUseSsl
            preferences[Keys.OVH_FROM_EMAIL] = settings.ovhFromEmail
            preferences[Keys.OVH_FROM_NAME] = settings.ovhFromName
            
            // OVH IMAP
            preferences[Keys.OVH_IMAP_ENABLED] = settings.ovhImapEnabled
            preferences[Keys.OVH_IMAP_SERVER] = settings.ovhImapServer
            preferences[Keys.OVH_IMAP_PORT] = settings.ovhImapPort
            preferences[Keys.OVH_IMAP_CHECK_INTERVAL] = settings.ovhImapCheckInterval
            preferences[Keys.OVH_IMAP_FOLDER] = settings.ovhImapFolder
        }
    }

    suspend fun updateLastBackupDate() {
        context.dataStore.edit { preferences ->
            preferences[Keys.LAST_BACKUP_DATE] = System.currentTimeMillis()
        }
    }

    suspend fun setBackupReminderShown(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.BACKUP_REMINDER_SHOWN] = shown
        }
    }

    suspend fun updateAutoBackupSettings(enabled: Boolean, interval: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AUTO_BACKUP_ENABLED] = enabled
            preferences[Keys.AUTO_BACKUP_INTERVAL] = interval
        }
    }
}
