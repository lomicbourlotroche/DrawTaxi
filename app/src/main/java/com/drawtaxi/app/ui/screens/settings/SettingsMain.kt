package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.ui.components.core.*
import com.drawtaxi.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SettingsMain(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit,
    onNavigate: (String) -> Unit,
    onRequestSmsPermission: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onRequestLocationPermission: () -> Unit = {}
) {
    var coutParKmDeplacement by remember { mutableStateOf(settings.coutParKmDeplacement.toString()) }
    var smsScanInterval by remember { mutableStateOf(settings.smsScanIntervalMinutes.toString()) }

    LaunchedEffect(coutParKmDeplacement) {
        delay(500)
        val c = coutParKmDeplacement.toDoubleOrNull() ?: settings.coutParKmDeplacement
        if (c != settings.coutParKmDeplacement) onUpdate(settings.copy(coutParKmDeplacement = c))
    }

    LaunchedEffect(smsScanInterval) {
        delay(500)
        val interval = smsScanInterval.toIntOrNull()?.coerceIn(15, 1440) ?: settings.smsScanIntervalMinutes
        if (interval != settings.smsScanIntervalMinutes) onUpdate(settings.copy(smsScanIntervalMinutes = interval))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(drawTaxiColors().background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ── PROFIL ──
        SettingsSection(title = "Profil & Entreprise") {
            SettingsMenuItem(
                title = "Infos Professionnelles",
                subtitle = "Nom, SIRET, adresse",
                icon = Icons.Default.BusinessCenter,
                iconTint = Indigo500,
                onClick = { onNavigate("proInfo") }
            )
        }

        // ── TARIFS ──
        SettingsSection(title = "Tarifs & Coûts") {
            SettingsMenuItem(
                title = "Configuration tarifaire",
                subtitle = "Prix/km, prise en charge, majorations",
                icon = Icons.Default.Payments,
                iconTint = Emerald500,
                onClick = { onNavigate("pricing") }
            )
            DrawTaxiDivider(color = drawTaxiColors().outlineVariant, modifier = Modifier.padding(vertical = 4.dp))
            // Coût déplacement inline
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Amber500.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    DrawTaxiIcon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        tint = Amber500,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Coût déplacement",
                        style = drawTaxiType().bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = drawTaxiColors().onSurface
                    )
                    Text(
                        "€/km domicile → client",
                        style = drawTaxiType().bodySmall,
                        color = drawTaxiColors().onSurfaceVariant
                    )
                }
                DrawTaxiTextField(
                    value = coutParKmDeplacement,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) coutParKmDeplacement = it },
                    modifier = Modifier.width(80.dp),
                    singleLine = true
                )
            }
        }

        // ── COMMUNICATION ──
        SettingsSection(title = "Communication") {
            SettingsMenuItem(
                title = "Templates de Messages",
                subtitle = "Personnaliser les réponses automatiques",
                icon = Icons.Default.Sms,
                iconTint = Blue500,
                onClick = { onNavigate("messageTemplates") }
            )
            DrawTaxiDivider(color = drawTaxiColors().outlineVariant, modifier = Modifier.padding(vertical = 4.dp))
            SettingsMenuItem(
                title = "Email OVH",
                subtitle = "Configuration SMTP",
                icon = Icons.Default.Email,
                iconTint = Violet500,
                onClick = { onNavigate("ovhMail") }
            )
        }

        // ── IA ──
        SettingsSection(title = "Intelligence Artificielle") {
            TaxiToggleRow(
                title = "Analyse IA des SMS",
                subtitle = "Qwen3 4B — fallback regex automatique",
                checked = settings.aiEnabled,
                onCheckedChange = { onUpdate(settings.copy(aiEnabled = it)) },
                icon = Icons.Default.SmartToy,
                iconTint = Violet500
            )

            val context = androidx.compose.ui.platform.LocalContext.current
            val isAiAvailable = remember(settings.aiEnabled) {
                if (settings.aiEnabled) com.drawtaxi.app.logic.ai.LlamaModelManager.isModelAvailable(context)
                else false
            }

            if (settings.aiEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                val (statusColor, statusText, statusIcon) = when {
                    isAiAvailable -> Triple(Emerald500, "IA opérationnelle", Icons.Default.CheckCircle)
                    else -> Triple(Amber500, "Modèle non téléchargé", Icons.Default.Warning)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawTaxiIcon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(statusText, style = drawTaxiType().bodySmall, color = statusColor, fontWeight = FontWeight.Medium)
                }
                if (!isAiAvailable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DrawTaxiOutlinedButton(
                        onClick = { onNavigate("aiDownload") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        DrawTaxiIcon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Télécharger le modèle")
                    }
                }
            }
        }

        // ── SURVEILLANCE ──
        SettingsSection(title = "Surveillance & Permissions") {
            TaxiToggleRow(
                title = "Surveiller les SMS",
                subtitle = "Détection des commandes en temps réel",
                checked = settings.monitorSms,
                onCheckedChange = { enabled ->
                    if (enabled) onRequestSmsPermission() else onUpdate(settings.copy(monitorSms = false))
                },
                icon = Icons.Default.Sms,
                iconTint = Emerald500
            )
            DrawTaxiDivider(color = drawTaxiColors().outlineVariant, modifier = Modifier.padding(vertical = 4.dp))
            // Intervalle scan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(drawTaxiColors().primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    DrawTaxiIcon(Icons.Default.Schedule, contentDescription = null, tint = drawTaxiColors().primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Scan périodique", style = drawTaxiType().bodyMedium, fontWeight = FontWeight.SemiBold, color = drawTaxiColors().onSurface)
                    Text("Intervalle en minutes (min. 15)", style = drawTaxiType().bodySmall, color = drawTaxiColors().onSurfaceVariant)
                }
                DrawTaxiTextField(
                    value = smsScanInterval,
                    onValueChange = { if (it.all { c -> c.isDigit() }) smsScanInterval = it },
                    modifier = Modifier.width(80.dp),
                    singleLine = true
                )
            }
            DrawTaxiDivider(color = drawTaxiColors().outlineVariant, modifier = Modifier.padding(vertical = 4.dp))
            TaxiToggleRow(
                title = "Notifications",
                subtitle = "Alertes visuelles et sonores",
                checked = settings.enableNotifications,
                onCheckedChange = { enabled ->
                    if (enabled) onRequestNotificationPermission() else onUpdate(settings.copy(enableNotifications = false))
                },
                icon = Icons.Default.Notifications,
                iconTint = Amber500
            )
            DrawTaxiDivider(color = drawTaxiColors().outlineVariant, modifier = Modifier.padding(vertical = 4.dp))
            TaxiToggleRow(
                title = "Suivi de Position",
                subtitle = "Partager la position GPS",
                checked = settings.trackLocation,
                onCheckedChange = { enabled ->
                    if (enabled) onRequestLocationPermission() else onUpdate(settings.copy(trackLocation = false))
                },
                icon = Icons.Default.LocationOn,
                iconTint = Rose500
            )
        }

        // ── DONNÉES ──
        SettingsSection(title = "Données") {
            SettingsMenuItem(
                title = "Export CSV / Excel",
                subtitle = "Télécharger l'historique des courses",
                icon = Icons.Default.Download,
                iconTint = Emerald500,
                onClick = { onNavigate("export") }
            )
            DrawTaxiDivider(color = drawTaxiColors().outlineVariant, modifier = Modifier.padding(vertical = 4.dp))
            SettingsMenuItem(
                title = "Sauvegarde & Restauration",
                subtitle = "Sauvegarder ou restaurer vos données",
                icon = Icons.Default.Backup,
                iconTint = Blue500,
                onClick = { onNavigate("backup") }
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = title.uppercase(),
            style = drawTaxiType().labelSmall,
            fontWeight = FontWeight.Bold,
            color = drawTaxiColors().onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        DrawTaxiCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = drawTaxiColors().surface,
            elevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsMainPreview() {
    DrawTaxiTheme {
        SettingsMain(
            settings = AppSettings(),
            onUpdate = {},
            onNavigate = {}
        )
    }
}
