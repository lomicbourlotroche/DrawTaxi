package com.drawtaxi.app.logic

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.drawtaxi.app.MainActivity
import com.drawtaxi.app.R

object NotificationHelper {
    const val CHANNEL_ID_RIDE_ALERTS = "ride_alerts"
    const val CHANNEL_ID_LOCATION = "location_tracking_channel"
    const val CHANNEL_ID_SMS = "sms_watcher_channel"

    private const val NOTIFICATION_ID_NEW_RIDE = 100
    private const val NOTIFICATION_ID_MODIFY = 101
    private const val NOTIFICATION_ID_DELETE = 102
    private const val NOTIFICATION_ID_LOCATION = 103
    private const val NOTIFICATION_ID_SMS = 104

    private var notificationManager: NotificationManagerCompat? = null

    private fun getManager(context: Context): NotificationManagerCompat {
        return notificationManager ?: NotificationManagerCompat.from(context).also { notificationManager = it }
    }

    fun showNewRideNotification(context: Context, rideId: String, destination: String, time: String) {
        val title = "Nouvelle Course !"
        val body = "Destination : $destination${if (time.isNotBlank()) " à $time" else ""}"
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_pending", true)
            putExtra("ride_id", rideId)
        }

        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_RIDE_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            getManager(context).notify(NOTIFICATION_ID_NEW_RIDE, builder.build())
        } catch (e: SecurityException) {
            Log.e("DrawTaxi", "Permission de notification manquante")
        }
    }

    fun showInfoNotification(
        context: Context,
        title: String,
        body: String,
        rideId: String? = null
    ) {
        showNotification(
            context = context,
            title = title,
            body = body,
            notificationId = NOTIFICATION_ID_MODIFY,
            rideId = rideId
        )
    }

    private fun showNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int,
        rideId: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_pending", true)
            rideId?.let { putExtra("ride_id", it) }
        }

        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_RIDE_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            getManager(context).notify(notificationId.coerceIn(1, 1000), builder.build())
        } catch (e: SecurityException) {
            Log.e("DrawTaxi", "Permission de notification manquante")
        }
    }
}
