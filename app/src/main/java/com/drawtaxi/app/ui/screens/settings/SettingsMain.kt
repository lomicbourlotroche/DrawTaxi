package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.ui.components.TaxiCard
import com.drawtaxi.app.ui.components.TaxiInputField
import com.drawtaxi.app.ui.theme.Slate500
import com.drawtaxi.app.ui.theme.Slate700
import kotlinx.coroutines.delay

import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SettingsMain(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit,
    onNavigate: (String) -> Unit,
    onRequestSmsPermission: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onRequestLocationPermission: () -> Unit = {}
) {
    var pricePerKm by remember { mutableStateOf(settings.pricePerKm) }
    var basePrice by remember { mutableStateOf(settings.basePrice) }
    var fuelCostPerKm by remember { mutableStateOf(settings.fuelCostPerKm.toString()) }
    var operatingCostPerHour by remember { mutableStateOf(settings.operatingCostPerHour.toString()) }
    var smsScanInterval by remember { mutableStateOf(settings.smsScanIntervalMinutes.toString()) }

    LaunchedEffect(pricePerKm, basePrice) {
        delay(500)
        if (pricePerKm != settings.pricePerKm || basePrice != settings.basePrice) {
            onUpdate(settings.copy(pricePerKm = pricePerKm, basePrice = basePrice))
        }
    }

    LaunchedEffect(fuelCostPerKm, operatingCostPerHour) {
        delay(500)
        val fuel = fuelCostPerKm.toDoubleOrNull() ?: settings.fuelCostPerKm
        val op = operatingCostPerHour.toDoubleOrNull() ?: settings.operatingCostPerHour
        if (fuel != settings.fuelCostPerKm || op != settings.operatingCostPerHour) {
            onUpdate(settings.copy(fuelCostPerKm = fuel, operatingCostPerHour = op))
        }
    }

    LaunchedEffect(smsScanInterval) {
        delay(500)
        val interval = smsScanInterval.toIntOrNull()?.coerceIn(15, 1440) ?: settings.smsScanIntervalMinutes
        if (interval != settings.smsScanIntervalMinutes) {
            onUpdate(settings.copy(smsScanIntervalMinutes = interval))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        TaxiCard(title = "Profil & Entreprise") {
            SettingsMenuItem(title = "Infos Professionnelles", icon = Icons.Default.BusinessCenter, onClick = { onNavigate("proInfo") })
        }

        TaxiCard(title = "Tarifs & Coûts") {
            SettingsMenuItem(title = "Configuration tarifaire", icon = Icons.Default.Payments, onClick = { onNavigate("pricing") })
        }

        TaxiCard(title = "Communication") {
            SettingsMenuItem(title = "Templates de Messages", icon = Icons.Default.Sms, onClick = { onNavigate("messageTemplates") })
            Spacer(modifier = Modifier.height(12.dp))
            SettingsMenuItem(
                title = "Email OVH",
                icon = Icons.Default.Email,
                onClick = { onNavigate("ovhMail") }
            )
        }

        TaxiCard(title = "Intelligence Artificielle") {
            TaxiToggleRow(
                title = "Activer l'analyse IA",
                subtitle = "Utiliser l'IA pour analyser les SMS automatiquement",
                checked = settings.aiEnabled,
                onCheckedChange = { onUpdate(settings.copy(aiEnabled = it)) },
                icon = Icons.Default.SmartToy
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Indicateur de statut IA
            val context = androidx.compose.ui.platform.LocalContext.current
            val isAiAvailable = remember(settings.aiEnabled) {
                if (settings.aiEnabled) {
                    com.drawtaxi.app.logic.ai.LlamaModelManager.isModelAvailable(context)
                } else {
                    false
                }
            }
            
            val (statusColor, statusText, statusIcon) = when {
                !settings.aiEnabled -> Triple(androidx.compose.ui.graphics.Color.Gray, "IA désactivée", Icons.Default.Block)
                isAiAvailable -> Triple(androidx.compose.ui.graphics.Color(0xFF10B981), "IA opérationnelle", Icons.Default.CheckCircle)
                else -> Triple(androidx.compose.ui.graphics.Color(0xFFF59E0B), "IA indisponible - Mode regex", Icons.Default.Warning)
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Modèle: Phi-3 Mini. Fallback automatique vers regex si indisponible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
            
            // Bouton téléchargement modèle si non disponible
            if (settings.aiEnabled && !isAiAvailable) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onNavigate("aiDownload") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Télécharger le modèle IA")
                }
            }
        }

        TaxiCard(title = "Surveillance & Permissions") {
            TaxiToggleRow(
                title = "Surveiller les SMS",
                subtitle = "Détecter les commandes entrantes en temps réel",
                checked = settings.monitorSms,
                onCheckedChange = { enabled ->
                    if (enabled) onRequestSmsPermission()
                    else onUpdate(settings.copy(monitorSms = false))
                },
                icon = Icons.Default.Sms
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = settings.brandColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Scan SMS périodique", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Slate700)
                    Text("Intervalle entre chaque scan automatique (minutes)", style = MaterialTheme.typography.bodySmall, color = Slate500)
                }
                OutlinedTextField(
                    value = smsScanInterval,
                    onValueChange = { if (it.all { c -> c.isDigit() }) smsScanInterval = it },
                    modifier = Modifier.width(80.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            TaxiToggleRow(
                title = "Notifications",
                subtitle = "Alertes visuelles et sonores",
                checked = settings.enableNotifications,
                onCheckedChange = { enabled ->
                    if (enabled) onRequestNotificationPermission()
                    else onUpdate(settings.copy(enableNotifications = false))
                },
                icon = Icons.Default.Notifications
            )
            Spacer(modifier = Modifier.height(12.dp))
            TaxiToggleRow(
                title = "Suivi de Position",
                subtitle = "Partager la position du taxi",
                checked = settings.trackLocation,
                onCheckedChange = { enabled ->
                    if (enabled) onRequestLocationPermission()
                    else onUpdate(settings.copy(trackLocation = false))
                },
                icon = Icons.Default.LocationOn
            )
        }

        TaxiCard(title = "Données") {
            SettingsMenuItem(title = "Export CSV/Excel", icon = Icons.Default.Download, onClick = { onNavigate("export") })
            Spacer(modifier = Modifier.height(12.dp))
            SettingsMenuItem(title = "Sauvegarde & Restauration", icon = Icons.Default.Backup, onClick = { onNavigate("backup") })
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
