package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import com.drawtaxi.app.ui.components.core.*
import com.drawtaxi.app.ui.theme.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.drawtaxi.app.data.AppSettings
import kotlinx.coroutines.delay

@Composable
fun PricingSettingsScreen(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    val brandColor = settings.brandColor
    
    // Tarifs
    var pricePerKm by remember { mutableStateOf(settings.pricePerKm) }
    var basePrice by remember { mutableStateOf(settings.basePrice) }
    var minDistanceKm by remember { mutableStateOf(settings.minDistanceKm) }
    var nightSurcharge by remember { mutableStateOf(settings.nightSurchargePercent.toString()) }
    var sundaySurcharge by remember { mutableStateOf(settings.sundaySurchargePercent.toString()) }
    var holidaySurcharge by remember { mutableStateOf(settings.holidaySurchargePercent.toString()) }
    var nightStartHour by remember { mutableStateOf(settings.nightStartHour.toString()) }
    var nightEndHour by remember { mutableStateOf(settings.nightEndHour.toString()) }
    var euroPerMinute by remember { mutableStateOf(settings.euroPerMinute.toString()) }
    
    // Coûts
    var coutParKmDeplacement by remember { mutableStateOf(settings.coutParKmDeplacement.toString()) }
    
    // TVA
    var tvaTransport by remember { mutableStateOf(settings.tvaTransportRate.toString()) }
    var tvaWait by remember { mutableStateOf(settings.tvaWaitTimeRate.toString()) }
    
    // Sauvegarde automatique
    LaunchedEffect(
        pricePerKm, basePrice, minDistanceKm, nightSurcharge, sundaySurcharge, holidaySurcharge,
        nightStartHour, nightEndHour, euroPerMinute,
        coutParKmDeplacement,
        tvaTransport, tvaWait
    ) {
        delay(500)
        onUpdate(
            settings.copy(
                pricePerKm = pricePerKm,
                basePrice = basePrice,
                minDistanceKm = minDistanceKm,
                nightSurchargePercent = nightSurcharge.toDoubleOrNull() ?: settings.nightSurchargePercent,
                sundaySurchargePercent = sundaySurcharge.toDoubleOrNull() ?: settings.sundaySurchargePercent,
                holidaySurchargePercent = holidaySurcharge.toDoubleOrNull() ?: settings.holidaySurchargePercent,
                nightStartHour = nightStartHour.toIntOrNull() ?: settings.nightStartHour,
                nightEndHour = nightEndHour.toIntOrNull() ?: settings.nightEndHour,
                euroPerMinute = euroPerMinute.toDoubleOrNull() ?: settings.euroPerMinute,
                coutParKmDeplacement = coutParKmDeplacement.toDoubleOrNull() ?: settings.coutParKmDeplacement,
                tvaTransportRate = tvaTransport.toDoubleOrNull() ?: settings.tvaTransportRate,
                tvaWaitTimeRate = tvaWait.toDoubleOrNull() ?: settings.tvaWaitTimeRate
            )
        )
    }

    DrawTaxiScaffold(
        topBar = {
            DrawTaxiTopBar(
                title = { Text("Tarifs & Coûts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    DrawTaxiIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                backgroundColor = Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Slate50)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            PricingCard(
                title = "Tarifs de base",
                icon = Icons.Default.Payments,
                brandColor = brandColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PricingInput(
                        label = "Prix au kilomètre (€)",
                        value = pricePerKm,
                        onValueChange = { pricePerKm = it },
                        brandColor = brandColor
                    )
                    PricingInput(
                        label = "Prise en charge (€)",
                        value = basePrice,
                        onValueChange = { basePrice = it },
                        brandColor = brandColor
                    )
                    PricingInput(
                        label = "Distance minimale incluse (km)",
                        value = minDistanceKm,
                        onValueChange = { minDistanceKm = it },
                        brandColor = brandColor
                    )
                    PricingInput(
                        label = "Tarif attente (€/min)",
                        value = euroPerMinute,
                        onValueChange = { euroPerMinute = it },
                        brandColor = brandColor
                    )
                }
            }

            // Section Majorations
            PricingCard(
                title = "Majorations horaires",
                icon = Icons.Default.NightsStay,
                brandColor = brandColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PricingInput(
                            label = "Nuit (%)",
                            value = nightSurcharge,
                            onValueChange = { nightSurcharge = it },
                            modifier = Modifier.weight(1f),
                            brandColor = brandColor
                        )
                        PricingInput(
                            label = "Dimanche (%)",
                            value = sundaySurcharge,
                            onValueChange = { sundaySurcharge = it },
                            modifier = Modifier.weight(1f),
                            brandColor = brandColor
                        )
                        PricingInput(
                            label = "Férié (%)",
                            value = holidaySurcharge,
                            onValueChange = { holidaySurcharge = it },
                            modifier = Modifier.weight(1f),
                            brandColor = brandColor
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PricingInput(
                            label = "Début nuit (h)",
                            value = nightStartHour,
                            onValueChange = { nightStartHour = it },
                            modifier = Modifier.weight(1f),
                            brandColor = brandColor
                        )
                        PricingInput(
                            label = "Fin nuit (h)",
                            value = nightEndHour,
                            onValueChange = { nightEndHour = it },
                            modifier = Modifier.weight(1f),
                            brandColor = brandColor
                        )
                    }
                }
            }

            // Section Coûts
            PricingCard(
                title = "Coût déplacement domicile → client",
                icon = Icons.Default.Route,
                brandColor = brandColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PricingInput(
                        label = "Coût par km de déplacement (€)",
                        value = coutParKmDeplacement,
                        onValueChange = { coutParKmDeplacement = it },
                        brandColor = brandColor
                    )
                    Text(
                        "Utilisé pour calculer la rentabilité : distance(domicile → départ) × coût/km",
                        style = drawTaxiType().bodySmall,
                        color = Slate500
                    )
                }
            }

            // Section TVA
            PricingCard(
                title = "TVA",
                icon = Icons.Default.AccountBalance,
                brandColor = brandColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PricingInput(
                        label = "TVA transport (%)",
                        value = tvaTransport,
                        onValueChange = { tvaTransport = it },
                        brandColor = brandColor
                    )
                    PricingInput(
                        label = "TVA attente (%)",
                        value = tvaWait,
                        onValueChange = { tvaWait = it },
                        brandColor = brandColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PricingCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    brandColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = brandColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = drawTaxiType().titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
private fun PricingInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    brandColor: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = { 
            // N'accepter que les chiffres et le point
            if (it.isEmpty() || it.matches(Regex("^[0-9]*\\.?[0-9]*$"))) {
                onValueChange(it)
            }
        },
        label = { 
            Text(
                text = label,
                style = drawTaxiType().bodySmall,
                color = Slate500
            ) 
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = brandColor,
            focusedLabelColor = brandColor,
            unfocusedBorderColor = Slate200,
            unfocusedLabelColor = Slate500
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PricingSettingsScreenPreview() {
    val sampleSettings = AppSettings()
    DrawTaxiTheme {
        PricingSettingsScreen(
            settings = sampleSettings,
            onUpdate = {},
            onBack = {}
        )
    }
}


