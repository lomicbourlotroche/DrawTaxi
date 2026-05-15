package com.drawtaxi.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.BackupManager
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.ui.components.TaxiCard
import com.drawtaxi.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    settings: AppSettings,
    allRides: List<RideRequest>,
    onUpdateSettings: (AppSettings) -> Unit,
    onRestore: (AppSettings?, List<RideRequest>?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context) }
    
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreContent by remember { mutableStateOf<String?>(null) }
    var restoreInfo by remember { mutableStateOf<BackupManager.BackupInfo?>(null) }
    
    var autoBackupEnabled by remember { mutableStateOf(settings.autoBackupEnabled) }
    var selectedInterval by remember { mutableStateOf(settings.autoBackupInterval) }
    
    val lastBackupDate = remember(settings.lastBackupDate) {
        if (settings.lastBackupDate > 0) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            dateFormat.format(Date(settings.lastBackupDate))
        } else {
            "Jamais"
        }
    }
    
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                isExporting = true
                try {
                    val backupJson = backupManager.createBackup(settings, allRides)
                    val result = backupManager.exportToUri(uri, backupJson)
                    isExporting = false
                    if (result.isSuccess) {
                        Toast.makeText(context, "Sauvegarde exportée avec succès", Toast.LENGTH_SHORT).show()
                        onUpdateSettings(settings.copy(lastBackupDate = System.currentTimeMillis()))
                    } else {
                        Toast.makeText(context, "Erreur lors de l'export", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    isExporting = false
                    Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                isImporting = true
                val result = backupManager.importFromUri(uri)
                isImporting = false
                result.onSuccess { content ->
                    val info = backupManager.getBackupInfo(content)
                    if (info != null) {
                        restoreInfo = info
                        restoreContent = content
                        showRestoreDialog = true
                    } else {
                        Toast.makeText(context, "Fichier invalide", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure {
                    Toast.makeText(context, "Erreur de lecture", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("Sauvegarde & Restauration") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )

        TaxiCard(title = "Sauvegarde manuelle") {
            Column {
                SettingsMenuItem(
                    title = "Exporter les données",
                    icon = Icons.Default.Upload,
                    onClick = {
                        val filename = backupManager.generateBackupFilename()
                        createDocumentLauncher.launch(filename)
                    },
                    enabled = !isExporting
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsMenuItem(
                    title = "Importer des données",
                    icon = Icons.Default.Download,
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    enabled = !isImporting
                )
                
                if (isExporting || isImporting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isExporting) "Export en cours..." else "Import en cours...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate600
                        )
                    }
                }
            }
        }

        TaxiCard(title = "Dernière sauvegarde") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = lastBackupDate,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Slate800
                    )
                    if (settings.lastBackupDate > 0) {
                        val daysSince = ((System.currentTimeMillis() - settings.lastBackupDate) / (1000 * 60 * 60 * 24)).toInt()
                        Text(
                            text = if (daysSince == 0) "Aujourd'hui" else "Il y a $daysSince jour${if (daysSince > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                }
                Icon(
                    imageVector = if (settings.lastBackupDate > 0) Icons.Default.CheckCircle else Icons.Default.History,
                    contentDescription = null,
                    tint = if (settings.lastBackupDate > 0) Green500 else Slate400,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        TaxiCard(title = "Sauvegarde automatique") {
            Column {
                TaxiToggleRow(
                    title = "Activer la sauvegarde auto",
                    subtitle = "Sauvegarder automatiquement",
                    checked = autoBackupEnabled,
                    onCheckedChange = { enabled ->
                        autoBackupEnabled = enabled
                        onUpdateSettings(settings.copy(autoBackupEnabled = enabled))
                    },
                    icon = Icons.Default.Backup
                )
                
                if (autoBackupEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Fréquence",
                        style = MaterialTheme.typography.labelMedium,
                        color = Slate500,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    IntervalSelector(
                        selectedInterval = selectedInterval,
                        onIntervalSelected = { interval ->
                            selectedInterval = interval
                            onUpdateSettings(settings.copy(autoBackupInterval = interval))
                        }
                    )
                }
            }
        }

        TaxiCard(title = "Restauration") {
            Text(
                text = "La restauration remplacera toutes vos données actuelles (courses et paramètres) par le contenu de la sauvegarde.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate600
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Assurez-vous d'exporter vos données actuelles avant d'importer une sauvegarde.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showRestoreDialog && restoreInfo != null && restoreContent != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            icon = { Icon(Icons.Default.Restore, contentDescription = null) },
            title = { Text("Restaurer la sauvegarde") },
            text = {
                Column {
                    Text("Voulez-vous restaurer cette sauvegarde ?")
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate50)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Date: ${restoreInfo?.dateFormatted}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Courses: ${restoreInfo?.rideCount}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Version: ${restoreInfo?.version}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Attention: Toutes vos données actuelles seront remplacées.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaxiRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDialog = false
                        isImporting = true
                        try {
                            val backupData = backupManager.parseBackup(restoreContent!!)
                            isImporting = false
                            if (backupData != null) {
                                onRestore(backupData.settings, backupData.rides)
                                Toast.makeText(context, "Restauration réussie !", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Erreur lors de la restauration", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            isImporting = false
                            Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        restoreContent = null
                        restoreInfo = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TaxiRed)
                ) {
                    Text("Restaurer")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreDialog = false
                    restoreContent = null
                    restoreInfo = null
                }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun IntervalSelector(
    selectedInterval: String,
    onIntervalSelected: (String) -> Unit
) {
    val intervals = listOf(
        "daily" to "Quotidienne",
        "weekly" to "Hebdomadaire",
        "monthly" to "Mensuelle"
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        intervals.forEach { (value, label) ->
            FilterChip(
                selected = selectedInterval == value,
                onClick = { onIntervalSelected(value) },
                label = { Text(label, fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SettingsMenuItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) Slate50 else Slate50.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) TaxiRed else Slate400,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (enabled) Slate700 else Slate400
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


