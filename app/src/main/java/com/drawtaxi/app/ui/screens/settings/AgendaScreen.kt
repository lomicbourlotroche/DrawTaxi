package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.drawtaxi.app.ui.components.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.Absence
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.ui.components.TaxiCard
import com.drawtaxi.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    absences: List<Absence>,
    settings: AppSettings,
    onAddAbsence: (Absence) -> Unit,
    onDeleteAbsence: (Absence) -> Unit,
    onSendMessage: (Absence) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var endDate by remember { mutableStateOf(System.currentTimeMillis() + 86400000L) }
    var reason by remember { mutableStateOf("") }
    var autoSendMessage by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isPickingStartDate by remember { mutableStateOf(true) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        DrawTaxiTopBar(
            title = {
                Column {
                    Text("Agenda - Absences")
                    Text(
                        text = "Gérez vos jours d'absence",
                        style = drawTaxiType().bodySmall,
                        color = Slate500
                    )
                }
            },
            navigationIcon = {
                DrawTaxiIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            actions = {
                DrawTaxiIconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter absence")
                }
            },
            backgroundColor = Color.Transparent
        )

        if (absences.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Slate200,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Aucune absence planifiée",
                        style = drawTaxiType().titleMedium,
                        color = Slate400,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ajoutez vos jours d'absence pour envoyer automatiquement un message aux clients",
                        style = drawTaxiType().bodyMedium,
                        color = Slate500,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    DrawTaxiSolidButton(
                        onClick = { showAddDialog = true },
                        containerColor = settings.brandColor
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ajouter une absence")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Absences enregistrées",
                        style = drawTaxiType().titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                }

                items(absences) { absence ->
                    AbsenceCard(
                        absence = absence,
                        dateFormat = dateFormat,
                        onSendMessage = { onSendMessage(absence) },
                        onDelete = { onDeleteAbsence(absence) },
                        brandColor = settings.brandColor
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    TaxiCard(title = "Message d'absence") {
                        Text(
                            text = settings.absenceMessageTemplate,
                            style = drawTaxiType().bodyMedium,
                            color = Slate600
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ce message sera envoyé automatiquement aux clients pendant vos absences.",
                            style = drawTaxiType().bodySmall,
                            color = Slate400
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Ajouter une absence") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DrawTaxiOutlinedButton(
                        onClick = {
                            isPickingStartDate = true
                            showDatePicker = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Date début: ${dateFormat.format(Date(startDate))}")
                    }

                    DrawTaxiOutlinedButton(
                        onClick = {
                            isPickingStartDate = false
                            showDatePicker = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Date fin: ${dateFormat.format(Date(endDate))}")
                    }

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Raison (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrawTaxiSwitch(
                            checked = autoSendMessage,
                            onCheckedChange = { autoSendMessage = it },
                            checkedTrackColor = settings.brandColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Envoyer message auto",
                            style = drawTaxiType().bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                DrawTaxiSolidButton(
                    onClick = {
                        if (endDate >= startDate) {
                            onAddAbsence(
                                Absence(
                                    id = Absence.createId(),
                                    startDate = startDate,
                                    endDate = endDate,
                                    reason = reason,
                                    autoSendMessage = autoSendMessage
                                )
                            )
                            showAddDialog = false
                            reason = ""
                            autoSendMessage = true
                        }
                    },
                    containerColor = settings.brandColor
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                DrawTaxiSolidButton(onClick = { showAddDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (isPickingStartDate) startDate else endDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                DrawTaxiSolidButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        if (isPickingStartDate) {
                            startDate = millis
                            if (endDate < startDate) {
                                endDate = startDate + 86400000L
                            }
                        } else {
                            endDate = millis
                            if (endDate < startDate) {
                                startDate = endDate - 86400000L
                            }
                        }
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                DrawTaxiSolidButton(onClick = { showDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun AbsenceCard(
    absence: Absence,
    dateFormat: SimpleDateFormat,
    onSendMessage: () -> Unit,
    onDelete: () -> Unit,
    brandColor: Color
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val isCurrent = now in absence.startDate..absence.endDate
    val isFuture = now < absence.startDate
    val isPast = now > absence.endDate

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = brandColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${dateFormat.format(Date(absence.startDate))} → ${dateFormat.format(Date(absence.endDate))}",
                            style = drawTaxiType().titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (absence.reason.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = absence.reason,
                            style = drawTaxiType().bodyMedium,
                            color = Slate600
                        )
                    }
                }

                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = when {
                                isCurrent -> "En cours"
                                isFuture -> "À venir"
                                isPast -> "Passée"
                                else -> ""
                            },
                            style = drawTaxiType().labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when {
                            isCurrent -> Yellow100
                            isFuture -> Blue100
                            isPast -> Slate100
                            else -> Slate100
                        },
                        labelColor = when {
                            isCurrent -> Yellow800
                            isFuture -> Blue800
                            isPast -> Slate600
                            else -> Slate600
                        }
                    ),
                    border = null
                )
            }

            if (absence.autoSendMessage) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (absence.messageSent) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (absence.messageSent) Green500 else Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (absence.messageSent) "Message envoyé" else "Message en attente",
                        style = drawTaxiType().bodySmall,
                        color = if (absence.messageSent) Green600 else Slate500
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            DrawTaxiDivider(color = Slate100)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!absence.messageSent && absence.autoSendMessage) {
                    DrawTaxiOutlinedButton(
                        onClick = onSendMessage,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Envoyer", style = drawTaxiType().labelMedium)
                    }
                }

                DrawTaxiOutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentColor = Red500
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Supprimer", style = drawTaxiType().labelMedium)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer cette absence ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                DrawTaxiSolidButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    containerColor = Red500
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                DrawTaxiSolidButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AgendaScreenPreview() {
    val sampleSettings = AppSettings()
    DrawTaxiTheme {
        AgendaScreen(
            absences = emptyList(),
            settings = sampleSettings,
            onAddAbsence = {},
            onDeleteAbsence = {},
            onSendMessage = {},
            onBack = {}
        )
    }
}




