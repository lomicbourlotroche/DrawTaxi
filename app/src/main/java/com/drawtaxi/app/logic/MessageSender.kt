package com.drawtaxi.app.logic

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.widget.Toast
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.MessageChannel
import com.drawtaxi.app.data.RideRequest

object MessageSender {

    fun sendMessage(context: Context, channel: MessageChannel, phone: String, email: String = "", message: String): Boolean {
        return when (channel) {
            MessageChannel.SMS -> sendSms(context, phone, message)
            MessageChannel.WHATSAPP -> sendWhatsApp(context, phone, message)
            MessageChannel.EMAIL -> if (email.isNotBlank()) {
                sendEmail(context, email, "Confirmation de course", message)
            } else {
                sendSms(context, phone, message)
            }
            MessageChannel.WEB_FORM -> sendSms(context, phone, message)
        }
    }

    fun sendSms(context: Context, phone: String, message: String): Boolean {
        return try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
            Toast.makeText(context, "SMS envoyé", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur envoi SMS: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun sendWhatsApp(context: Context, phone: String, message: String): Boolean {
        return try {
            val cleanPhone = phone.replace(Regex("[^+\\d]"), "")
            val uri = Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.whatsapp")
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp non installé", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun sendEmail(context: Context, email: String, subject: String, body: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(intent, "Envoyer par email"))
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur envoi email", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun sendQuoteMessage(context: Context, ride: RideRequest, quote: com.drawtaxi.app.data.Quote, settings: AppSettings) {
        val message = settings.quoteTemplate
            .replace("[DEPART]", quote.departure)
            .replace("[ARRIVEE]", quote.arrival)
            .replace("[DISTANCE]", String.format("%.1f", quote.distanceKm))
            .replace("[PRIX]", String.format("%.2f", quote.price))

        sendMessage(
            context = context,
            channel = quote.messageChannel,
            phone = ride.sender,
            email = ride.clientEmail,
            message = message
        )
    }

    fun sendAbsenceMessage(
        context: Context,
        phone: String,
        email: String,
        absence: com.drawtaxi.app.data.Absence,
        settings: AppSettings,
        channel: MessageChannel = MessageChannel.SMS
    ) {
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val message = settings.absenceMessageTemplate
            .replace("[DATE_DEBUT]", dateFormat.format(java.util.Date(absence.startDate)))
            .replace("[DATE_FIN]", dateFormat.format(java.util.Date(absence.endDate)))
            .replace("[DATE_RETOUR]", dateFormat.format(java.util.Date(absence.endDate + 86400000)))

        sendMessage(
            context = context,
            channel = channel,
            phone = phone,
            email = email,
            message = message
        )
    }
}
