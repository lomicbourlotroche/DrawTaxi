package com.drawtaxi.app.logic.sms

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.drawtaxi.app.data.RideRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsScanner {
    private const val TAG = "SmsScanner"

    fun scanLastHourSms(context: Context): List<RideRequest> {
        val rides = mutableListOf<RideRequest>()

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Permission READ_SMS non accordée")
            return rides
        }

        val oneHourAgo = System.currentTimeMillis() - 3600000

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = "${Telephony.Sms.DATE} > ?"
        val selectionArgs = arrayOf(oneHourAgo.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                var scanned = 0
                var matched = 0

                while (it.moveToNext()) {
                    scanned++
                    val address = it.getString(addressIndex) ?: ""
                    val body = it.getString(bodyIndex) ?: ""
                    val date = it.getLong(dateIndex)

                    if (address.isBlank() || body.isBlank()) continue

                    if (!isTaxiRelated(body) && !isCancellationMessage(body) &&
                        !isModificationMessage(body) && !isConfirmationMessage(body)) {
                        continue
                    }

                    val ride = parseSms(address, body, date)
                    if (ride != null) {
                        matched++
                        rides.add(ride)
                    }
                }
                Log.d(TAG, "Scan terminé: $scanned SMS analysés, $matched courses trouvées")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException lecture SMS: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning SMS: ${e.message}", e)
        }

        return rides
    }

    suspend fun parseSmsWithAI(context: Context, sender: String, body: String, timestamp: Long = System.currentTimeMillis(), aiEnabled: Boolean = true): RideRequest? {
        return withContext(Dispatchers.IO) {
            try {
                val aiResult = AiSmsParser.parseWithAI(context, body, aiEnabled)
                val ride = aiResult.toRideRequest(sender, timestamp)
                if (ride != null) {
                    Log.d(TAG, "AI parsed SMS: ${ride.departure} → ${ride.arrival} (confidence: ${aiResult.confidence})")
                }
                ride
            } catch (e: Exception) {
                Log.e(TAG, "AI parsing failed, falling back to regex: ${e.message}")
                parseSms(sender, body, timestamp)
            }
        }
    }
}
