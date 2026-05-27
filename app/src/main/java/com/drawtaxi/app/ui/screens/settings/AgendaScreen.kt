package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.Absence
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.ui.components.TaxiCard
import com.drawtaxi.app.ui.components.core.*
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(drawTaxiColors().background)
    ) {
        DrawTaxiTopBar(
            title = {
                DrawTaxiTopBarTitle(
                    text = "Agenda - Absences",
                    subtitle = "Gérez vos jours d'absence"
                )
            },
            navigationIcon = {
                DrawTaxiIconButton(onClick = onBack) {
                    DrawTaxiIcon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Slate700)
                }
            },
            actions = {
                DrawTaxiIconButton(onClick = { showAddDialog = true }) {
                    DrawTaxiIcon(Icons.Default.Add, contentDescription = "Ajouter absence", tint = Slate700)
                }
            },
            backgroundColor = Color.Transparent
        )

        if (absences.isEmpty()) {
            EmptyAbsenceState(
                brandColor = settings.brandColor,
                onAddClick = { showAddDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text(
                        text = "Absences enregistrées",
                        style = drawTaxiType().titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
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
                    Spacer(modifier = Modifier.height(24.dp))
                    TaxiCard(
                        title = "Message d'absence",
                        titleIcon = Icons.Default.Sms,
                        brandColor = settings.brandColor
                    ) {
                        Text(
                            text = settings.absenceMessageTemplate,
                            style = drawTaxiType().bodyMedium,
                            color = Slate600
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DrawTaxiDivider(color = Slate100)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DrawTaxiIcon(Icons.Default.Info, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ce message sera envoyé automatiquement aux clients pendant vos absences.",
                                style = drawTaxiType().bodySmall,
                                color = Slate400
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAbsenceDialog(
            startDate = startDate,
            endDate = endDate,
            reason = reason,
            autoSendMessage = autoSendMessage,
            dateFormat = dateFormat,
            brandColor = settings.brandColor,
            onReasonChange = { reason = it },
            onAutoSendMessageChange = { autoSendMessage = it },
            onPickStartDate = {
                isPickingStartDate = true
                showDatePicker = true
            },
            onPickEndDate = {
                isPickingStartDate = false
                showDatePicker = true
            },
            onDismiss = { showAddDialog = false },
            onConfirm = {
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
private fun EmptyAbsenceState(
    brandColor: Color,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(brandColor.copy(alpha = 0.05f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(brandColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    DrawTaxiIcon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = brandColor,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Aucune absence planifiée",
                style = drawTaxiType().titleLarge,
                color = Slate800,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Ajoutez vos jours d'absence pour envoyer automatiquement un message aux clients lors de leurs réservations.",
                style = drawTaxiType().bodyMedium,
                color = Slate500,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(40.dp))
            DrawTaxiSolidButton(
                onClick = onAddClick,
                containerColor = brandColor,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                DrawTaxiIcon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajouter une absence")
            }
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

    DrawTaxiCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        backgroundColor = Color.White
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(brandColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        DrawTaxiIcon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = brandColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${dateFormat.format(Date(absence.startDate))} → ${dateFormat.format(Date(absence.endDate))}",
                        style = drawTaxiType().titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                }

                if (absence.reason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = absence.reason,
                        style = drawTaxiType().bodyMedium,
                        color = Slate600
                    )
                }
            }

            Surface(
                color = when {
                    isCurrent -> Yellow100
                    isFuture -> Blue100
                    isPast -> Slate100
                    else -> Slate100
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = when {
                        isCurrent -> "En cours"
                        isFuture -> "À venir"
                        isPast -> "Passée"
                        else -> ""
                    },
                    style = drawTaxiType().labelSmall,
                    color = when {
                        isCurrent -> Yellow800
                        isFuture -> Blue800
                        isPast -> Slate600
                        else -> Slate600
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (absence.autoSendMessage) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(if (absence.messageSent) drawTaxiColors().statusValidatedBg.copy(alpha = 0.5f) else drawTaxiColors().surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                DrawTaxiIcon(
                    if (absence.messageSent) Icons.Default.CheckCircle else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (absence.messageSent) Green600 else Slate400,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (absence.messageSent) "Message envoyé aux clients" else "Message auto activé",
                    style = drawTaxiType().bodySmall,
                    color = if (absence.messageSent) Green600 else Slate500,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        DrawTaxiDivider(color = Slate100)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!absence.messageSent && absence.autoSendMessage) {
                DrawTaxiOutlinedButton(
                    onClick = onSendMessage,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    minHeight = 40.dp
                ) {
                    DrawTaxiIcon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = brandColor)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Informer", style = drawTaxiType().labelMedium, color = brandColor)
                }
            }

            DrawTaxiOutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentColor = Red500,
                minHeight = 40.dp
            ) {
                DrawTaxiIcon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Red500)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Supprimer", style = drawTaxiType().labelMedium)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer l'absence ?", fontWeight = FontWeight.Bold) },
            text = { Text("Cette absence sera retirée de votre agenda.") },
            confirmButton = {
                DrawTaxiSolidButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    containerColor = Red500,
                    minHeight = 44.dp
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                DrawTaxiOutlinedButton(onClick = { showDeleteDialog = false }, minHeight = 44.dp) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun AddAbsenceDialog(
    startDate: Long,
    endDate: Long,
    reason: String,
    autoSendMessage: Boolean,
    dateFormat: SimpleDateFormat,
    brandColor: Color,
    onReasonChange: (String) -> Unit,
    onAutoSendMessageChange: (Boolean) -> Unit,
    onPickStartDate: () -> Unit,
    onPickEndDate: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Planifier une absence", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Début", style = drawTaxiType().labelMedium, color = Slate500)
                        Spacer(modifier = Modifier.height(4.dp))
                        DrawTaxiOutlinedButton(
                            onClick = onPickStartDate,
                            modifier = Modifier.fillMaxWidth(),
                            minHeight = 44.dp,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(dateFormat.format(Date(startDate)), style = drawTaxiType().bodyMedium)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fin", style = drawTaxiType().labelMedium, color = Slate500)
                        Spacer(modifier = Modifier.height(4.dp))
                        DrawTaxiOutlinedButton(
                            onClick = onPickEndDate,
                            modifier = Modifier.fillMaxWidth(),
                            minHeight = 44.dp,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(dateFormat.format(Date(endDate)), style = drawTaxiType().bodyMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Raison / Libellé") },
                    placeholder = { Text("Ex: Congés, RDV...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandColor,
                        unfocusedBorderColor = Slate200
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(drawTaxiColors().surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Message automatique",
                            style = drawTaxiType().bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Informer les clients par SMS",
                            style = drawTaxiType().bodySmall,
                            color = Slate500
                        )
                    }
                    DrawTaxiSwitch(
                        checked = autoSendMessage,
                        onCheckedChange = onAutoSendMessageChange,
                        checkedTrackColor = brandColor
                    )
                }
            }
        },
        confirmButton = {
            DrawTaxiSolidButton(
                onClick = onConfirm,
                containerColor = brandColor,
                minHeight = 48.dp
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            DrawTaxiOutlinedButton(onClick = onDismiss, minHeight = 48.dp) {
                Text("Annuler")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AgendaScreenEmptyPreview() {
    val sampleSettings = AppSettings(brandColor = Indigo500)
    DrawTaxiTheme(brandColor = Indigo500) {
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

@Preview(showBackground = true)
@Composable
fun AgendaScreenWithContentPreview() {
    val sampleSettings = AppSettings(brandColor = Indigo500)
    val now = System.currentTimeMillis()
    val absences = listOf(
        Absence(
            id = "1",
            startDate = now - 172800000L,
            endDate = now - 86400000L,
            reason = "Vacances d'été",
            autoSendMessage = true,
            messageSent = true
        ),
        Absence(
            id = "2",
            startDate = now,
            endDate = now + 172800000L,
            reason = "Formation",
            autoSendMessage = true,
            messageSent = false
        ),
        Absence(
            id = "3",
            startDate = now + 345600000L,
            endDate = now + 518400000L,
            reason = "Repos hebdomadaire",
            autoSendMessage = false
        )
    )
    DrawTaxiTheme(brandColor = Indigo500) {
        AgendaScreen(
            absences = absences,
            settings = sampleSettings,
            onAddAbsence = {},
            onDeleteAbsence = {},
            onSendMessage = {},
            onBack = {}
        )
    }
}
