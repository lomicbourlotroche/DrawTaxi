package com.drawtaxi.app.logic

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.drawtaxi.app.data.local.AppDatabase
import com.drawtaxi.app.data.local.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "Boot/PackageReplaced - vérification de la surveillance SMS")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settingsManager = SettingsManager(context)
                    val settings = settingsManager.settingsFlow.first()

                    if (!settings.monitorSms) {
                        Log.d(TAG, "Surveillance SMS désactivée dans les paramètres")
                        return@launch
                    }

                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
                        != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Permission READ_SMS non accordée")
                        return@launch
                    }

                    if (SmsForegroundService.isRunning) {
                        Log.d(TAG, "Service déjà en cours d'exécution")
                        return@launch
                    }

                    Log.d(TAG, "Démarrage du SmsForegroundService après boot")
                    val serviceIntent = Intent(context, SmsForegroundService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur au démarrage du service: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
