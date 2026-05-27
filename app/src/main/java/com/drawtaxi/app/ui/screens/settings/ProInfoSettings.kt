package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.drawtaxi.app.ui.components.core.*
import com.drawtaxi.app.ui.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.ui.components.TaxiCard
import com.drawtaxi.app.ui.components.TaxiInputField
import com.drawtaxi.app.ui.theme.Slate400
import kotlinx.coroutines.delay

import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ProInfoSettings(settings: AppSettings, onUpdate: (AppSettings) -> Unit, onBack: () -> Unit) {
    var companyName by remember { mutableStateOf(settings.companyName) }
    var name by remember { mutableStateOf(settings.name) }
    var siret by remember { mutableStateOf(settings.siret) }
    var city by remember { mutableStateOf(settings.city) }
    var vehicle by remember { mutableStateOf(settings.vehicle) }
    var homeAddress by remember { mutableStateOf(settings.homeAddress) }
    var signature by remember { mutableStateOf(settings.signature) }
    var missingInfoTemplate by remember { mutableStateOf(settings.missingInfoTemplate) }
    var arrivalMessageTemplate by remember { mutableStateOf(settings.arrivalMessageTemplate) }

    // Debounced update to avoid DataStore lag
    LaunchedEffect(companyName, name, siret, city, vehicle, homeAddress, signature, missingInfoTemplate, arrivalMessageTemplate) {
        delay(500)
        if (companyName != settings.companyName || name != settings.name || 
            siret != settings.siret || city != settings.city || 
            vehicle != settings.vehicle || homeAddress != settings.homeAddress ||
            signature != settings.signature ||
            missingInfoTemplate != settings.missingInfoTemplate ||
            arrivalMessageTemplate != settings.arrivalMessageTemplate) {
            onUpdate(settings.copy(
                companyName = companyName,
                name = name,
                siret = siret,
                city = city,
                vehicle = vehicle,
                homeAddress = homeAddress,
                signature = signature,
                missingInfoTemplate = missingInfoTemplate,
                arrivalMessageTemplate = arrivalMessageTemplate
            ))
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(drawTaxiColors().background)) {
        DrawTaxiTopBar(
            title = { DrawTaxiTopBarTitle("Configuration Pro", subtitle = "Identité & Modèles") },
            navigationIcon = {
                DrawTaxiIconButton(onClick = onBack) {
                    DrawTaxiIcon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = drawTaxiColors().onSurface
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            TaxiCard(
                title = "Compagnie & Application",
                titleIcon = Icons.Default.Business,
                brandColor = Indigo500
            ) {
                TaxiInputField(label = "Nom de la Compagnie / App", value = companyName, onValueChange = { companyName = it })
            }

            TaxiCard(
                title = "Identité du Chauffeur",
                titleIcon = Icons.Default.Person,
                brandColor = Emerald500
            ) {
                TaxiInputField(label = "Nom du Prestataire", value = name, onValueChange = { name = it })
                TaxiInputField(label = "SIRET", value = siret, onValueChange = { siret = it })
                TaxiInputField(label = "Ville", value = city, onValueChange = { city = it })
                TaxiInputField(label = "Véhicule", value = vehicle, onValueChange = { vehicle = it })
                TaxiInputField(label = "Adresse du domicile", value = homeAddress, onValueChange = { homeAddress = it }, placeholder = "Utilisé pour le GPS retour")
            }

            TaxiCard(
                title = "Signature & Modèles de message",
                titleIcon = Icons.AutoMirrored.Filled.Assignment,
                brandColor = Amber600
            ) {
                TaxiInputField(label = "Mention de signature (ex: Fait à...)", value = signature, onValueChange = { signature = it })
                Spacer(modifier = Modifier.height(8.dp))
                TaxiInputField(
                    label = "Modèle message infos manquantes", 
                    value = missingInfoTemplate, 
                    onValueChange = { missingInfoTemplate = it }
                )
                Text(
                    text = "Utilisez [FIELDS] pour les infos manquantes.",
                    style = drawTaxiType().labelSmall,
                    color = Slate400,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TaxiInputField(
                    label = "Modèle message 'Je suis arrivé'", 
                    value = arrivalMessageTemplate, 
                    onValueChange = { arrivalMessageTemplate = it }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProInfoSettingsPreview() {
    val mockSettings = AppSettings(
        companyName = "DrawTaxi Premium",
        name = "Alice Smith",
        siret = "555 666 777 00088",
        city = "Bordeaux",
        vehicle = "Mercedes EQE",
        signature = "Fait à Bordeaux le ..."
    )
    DrawTaxiTheme {
        ProInfoSettings(settings = mockSettings, onUpdate = {}, onBack = {})
    }
}
