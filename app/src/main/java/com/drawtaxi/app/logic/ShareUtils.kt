package com.drawtaxi.app.logic

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest

object ShareUtils {

    fun shareReceipt(context: Context, ride: RideRequest, settings: AppSettings) {
        val receiptText = buildReceiptText(ride, settings)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, receiptText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Envoyer le reçu"))
    }

    fun shareReceiptViaEmail(context: Context, ride: RideRequest, settings: AppSettings) {
        if (ride.clientEmail.isBlank()) {
            Toast.makeText(context, "Email client non renseigné", Toast.LENGTH_SHORT).show()
            return
        }
        val receiptText = buildReceiptText(ride, settings)
        MessageSender.sendEmail(
            context = context,
            email = ride.clientEmail,
            subject = "Reçu - Course ${ride.departure} → ${ride.arrival}",
            body = receiptText
        )
    }

    fun shareReceiptViaSms(context: Context, ride: RideRequest, settings: AppSettings) {
        val receiptText = buildReceiptText(ride, settings)
        MessageSender.sendSms(context, ride.sender, receiptText)
    }

    fun shareReceiptViaWhatsApp(context: Context, ride: RideRequest, settings: AppSettings) {
        val receiptText = buildReceiptText(ride, settings)
        MessageSender.sendWhatsApp(context, ride.sender, receiptText)
    }

    fun shareText(context: Context, text: String, title: String = "Partager") {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(shareIntent, title))
    }

    private fun buildReceiptText(ride: RideRequest, settings: AppSettings): String {
        return buildString {
            appendLine("${settings.companyName} - Reçu de course")
            appendLine("────────────────────────")
            appendLine("Date: ${ride.date}")
            appendLine("De: ${ride.departure}")
            appendLine("À: ${ride.arrival}")
            appendLine("Distance: ${String.format("%.1f", ride.distanceKm)} km")
            appendLine("────────────────────────")
            appendLine("TOTAL: ${String.format("%.2f €", ride.price)}")
            appendLine("────────────────────────")
            appendLine("SIRET: ${settings.siret}")
        }
    }
}
