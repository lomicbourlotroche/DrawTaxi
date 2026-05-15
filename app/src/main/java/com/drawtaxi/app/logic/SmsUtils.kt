package com.drawtaxi.app.logic

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat

object SmsUtils {
    private const val TAG = "SmsUtils"

    /**
     * Sends an SMS directly if permission is granted, otherwise falls back to opening the SMS app via Intent.
     */
    fun sendSms(context: Context, phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(android.telephony.SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                
                Toast.makeText(context, "SMS envoyé directement", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "SMS sent directly to $phoneNumber")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send direct SMS, falling back to Intent", e)
                sendSmsViaIntent(context, phoneNumber, message)
            }
        } else {
            Log.d(TAG, "SEND_SMS permission not granted, falling back to Intent")
            sendSmsViaIntent(context, phoneNumber, message)
        }
    }

    private fun sendSmsViaIntent(context: Context, phoneNumber: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS via Intent", e)
            Toast.makeText(context, "Erreur : impossible d'ouvrir les SMS", Toast.LENGTH_SHORT).show()
        }
    }
}
