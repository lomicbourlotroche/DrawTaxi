package com.drawtaxi.app.ui.components

import androidx.compose.ui.tooling.preview.Preview
import com.drawtaxi.app.ui.theme.DrawTaxiTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideCreateForm(
    sender: String,
    onSenderChange: (String) -> Unit,
    date: String,
    onDateChange: (String) -> Unit,
    time: String,
    onTimeChange: (String) -> Unit,
    departure: String,
    onDepartureChange: (String) -> Unit,
    arrival: String,
    onArrivalChange: (String) -> Unit,
    price: String,
    onPriceChange: (String) -> Unit,
    brandColor: Color
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = sender,
            onValueChange = onSenderChange,
            label = { Text("Passager") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = date,
                onValueChange = {},
                label = { Text("Date") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                modifier = Modifier
                    .weight(1f)
                    .clickable { showDatePicker = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                readOnly = true,
                singleLine = true
            )
            OutlinedTextField(
                value = time,
                onValueChange = {},
                label = { Text("Heure") },
                leadingIcon = { Icon(Icons.Default.Schedule, null) },
                modifier = Modifier
                    .weight(1f)
                    .clickable { showTimePicker = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                readOnly = true,
                singleLine = true
            )
        }

        OutlinedTextField(
            value = departure,
            onValueChange = onDepartureChange,
            label = { Text("Départ") },
            leadingIcon = { Icon(Icons.Default.Place, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = arrival,
            onValueChange = onArrivalChange,
            label = { Text("Arrivée") },
            leadingIcon = { Icon(Icons.Default.Place, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = if (price.isEmpty()) "0.00" else price,
            onValueChange = onPriceChange,
            label = { Text("Total Estimé (Auto)") },
            leadingIcon = { Icon(Icons.Default.Payments, null, tint = brandColor) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            suffix = { Text("€") },
            singleLine = true
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        onDateChange(sdf.format(Date(millis)))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val hours = timePickerState.hour.toString().padStart(2, '0')
                    val minutes = timePickerState.minute.toString().padStart(2, '0')
                    onTimeChange("${hours}h${minutes}")
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Annuler")
                }
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RideCreateFormPreview() {
    DrawTaxiTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RideCreateForm(
                sender = "John Doe",
                onSenderChange = {},
                date = "26/05/2026",
                onDateChange = {},
                time = "14h30",
                onTimeChange = {},
                departure = "Gare de Lyon, Paris",
                onDepartureChange = {},
                arrival = "Aéroport CDG, Roissy",
                onArrivalChange = {},
                price = "85.50",
                onPriceChange = {},
                brandColor = Color(0xFF6366F1)
            )
        }
    }
}
