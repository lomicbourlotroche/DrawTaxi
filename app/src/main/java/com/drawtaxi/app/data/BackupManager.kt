package com.drawtaxi.app.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.drawtaxi.app.ui.theme.TaxiRed
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context) {

    companion object {
        private const val TAG = "BackupManager"
        const val BACKUP_VERSION = 1
    }

    data class BackupData(
        val version: Int,
        val timestamp: Long,
        val settings: AppSettings,
        val rides: List<RideRequest>
    )

    fun createBackup(settings: AppSettings, rides: List<RideRequest>): String {
        val backup = JSONObject()
        
        backup.put("version", BACKUP_VERSION)
        backup.put("timestamp", System.currentTimeMillis())
        backup.put("appName", "DrawTaxi")
        
        val settingsJson = JSONObject().apply {
            put("companyName", settings.companyName)
            put("name", settings.name)
            put("address", settings.address)
            put("city", settings.city)
            put("siret", settings.siret)
            put("tva", settings.tva)
            put("vehicle", settings.vehicle)
            put("pricePerKm", settings.pricePerKm)
            put("basePrice", settings.basePrice)
            put("brandColor", settings.brandColor.value.toLong())
            put("theme", settings.theme)
            put("showLogo", settings.showLogo)
            put("signature", settings.signature)
            put("missingInfoTemplate", settings.missingInfoTemplate)
            put("arrivalMessageTemplate", settings.arrivalMessageTemplate)
        }
        backup.put("settings", settingsJson)

        val ridesArray = JSONArray()
        rides.forEach { ride ->
            val rideJson = JSONObject().apply {
                put("id", ride.id)
                put("sender", ride.sender)
                put("body", ride.body)
                put("departure", ride.departure)
                put("arrival", ride.arrival)
                put("time", ride.time)
                put("distanceKm", ride.distanceKm)
                put("timestamp", ride.timestamp)
                put("isPending", ride.isPending)
                put("date", ride.date)
                put("price", ride.price)
            }
            ridesArray.put(rideJson)
        }
        backup.put("rides", ridesArray)
        
        backup.put("rideCount", rides.size)

        return backup.toString(2)
    }

    fun parseBackup(jsonString: String): BackupData? {
        return try {
            val json = JSONObject(jsonString)
            
            val version = json.optInt("version", 1)
            val timestamp = json.optLong("timestamp", System.currentTimeMillis())
            
            val settingsJson = json.getJSONObject("settings")
            val settings = AppSettings(
                companyName = settingsJson.optString("companyName", "DrawTaxi"),
                name = settingsJson.optString("name", "Chauffeur"),
                address = settingsJson.optString("address", ""),
                city = settingsJson.optString("city", ""),
                siret = settingsJson.optString("siret", ""),
                tva = settingsJson.optString("tva", ""),
                vehicle = settingsJson.optString("vehicle", ""),
                pricePerKm = settingsJson.optString("pricePerKm", "1.20"),
                basePrice = settingsJson.optString("basePrice", "2.60"),
                brandColor = settingsJson.optLong("brandColor", TaxiRed.value.toLong()).let { Color(it.toULong()) },
                theme = settingsJson.optString("theme", "modern"),
                showLogo = settingsJson.optBoolean("showLogo", true),
                signature = settingsJson.optString("signature", "Fait à [Ville], le [Date]"),
                missingInfoTemplate = settingsJson.optString("missingInfoTemplate", ""),
                arrivalMessageTemplate = settingsJson.optString("arrivalMessageTemplate", ""),
                isFirstLaunch = false
            )

            val ridesJson = json.optJSONArray("rides") ?: JSONArray()
            val rides = mutableListOf<RideRequest>()
            
            for (i in 0 until ridesJson.length()) {
                val rideJson = ridesJson.getJSONObject(i)
                rides.add(
                    RideRequest(
                        id = rideJson.optString("id", UUID.randomUUID().toString()),
                        sender = rideJson.optString("sender", ""),
                        body = rideJson.optString("body", ""),
                        departure = rideJson.optString("departure", ""),
                        arrival = rideJson.optString("arrival", ""),
                        time = rideJson.optString("time", ""),
                        distanceKm = rideJson.optDouble("distanceKm", 0.0),
                        timestamp = rideJson.optLong("timestamp", System.currentTimeMillis()),
                        isPending = rideJson.optBoolean("isPending", true),
                        date = rideJson.optString("date", ""),
                        price = rideJson.optDouble("price", 0.0)
                    )
                )
            }

            BackupData(version, timestamp, settings, rides)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing backup: ${e.message}")
            null
        }
    }

    suspend fun exportToUri(uri: Uri, backupData: String): Result<Unit> {
        return try {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return Result.failure(Exception("Cannot open output stream"))
            outputStream.use {
                OutputStreamWriter(it).use { writer ->
                    writer.write(backupData)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Export error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun importFromUri(uri: Uri): Result<String> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val content = reader.readText()
                    Result.success(content)
                }
            } ?: Result.failure(Exception("Cannot open file"))
        } catch (e: Exception) {
            Log.e(TAG, "Import error: ${e.message}")
            Result.failure(e)
        }
    }

    fun generateBackupFilename(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "drawtaxi_backup_$timestamp.json"
    }

    fun getBackupInfo(jsonString: String): BackupInfo? {
        return try {
            val json = JSONObject(jsonString)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            BackupInfo(
                version = json.optInt("version", 1),
                timestamp = json.optLong("timestamp", 0),
                dateFormatted = dateFormat.format(Date(json.optLong("timestamp", 0))),
                rideCount = json.optJSONArray("rides")?.length() ?: 0,
                appName = json.optString("appName", "DrawTaxi")
            )
        } catch (e: Exception) {
            null
        }
    }

    data class BackupInfo(
        val version: Int,
        val timestamp: Long,
        val dateFormatted: String,
        val rideCount: Int,
        val appName: String
    )
}
