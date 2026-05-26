package com.drawtaxi.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.drawtaxi.app.ui.components.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.ui.components.TaxiInputField
import com.drawtaxi.app.ui.theme.*

@Composable
fun OnboardingScreen(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit,
    onRequestSms: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestLocation: () -> Unit,
    onComplete: () -> Unit
) {
    var companyName by remember { mutableStateOf(settings.companyName) }
    var driverName by remember { mutableStateOf(settings.name) }

    DrawTaxiSurface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Logo / Icon
            DrawTaxiSurface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = settings.brandColor.copy(0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LocalTaxi,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = settings.brandColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Bienvenue sur DrawTaxi",
                style = drawTaxiType().headlineMedium,
                fontWeight = FontWeight.Black,
                color = Slate900,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Configurons votre outil de travail en quelques secondes.",
                style = drawTaxiType().bodyMedium,
                color = Slate400,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Step 1: Identity
            OnboardingSection(title = "1. Votre Identité") {
                TaxiInputField(
                    label = "Nom de votre compagnie",
                    value = companyName,
                    onValueChange = { companyName = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
                TaxiInputField(
                    label = "Votre nom de chauffeur",
                    value = driverName,
                    onValueChange = { driverName = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step 2: Permissions
            OnboardingSection(title = "2. Autorisations nécessaires") {
                PermissionItem(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = "Lecture & Envoi SMS",
                    description = "Pour détecter les réservations et répondre à vos clients.",
                    isGranted = settings.monitorSms,
                    onClick = onRequestSms
                )
                
                PermissionItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    description = "Pour vous alerter dès qu'une nouvelle course est identifiée.",
                    isGranted = settings.enableNotifications,
                    onClick = onRequestNotification
                )

                PermissionItem(
                    icon = Icons.Default.MyLocation,
                    title = "Localisation",
                    description = "Pour le calcul des distances et des tarifs au kilomètre.",
                    isGranted = settings.trackLocation,
                    onClick = onRequestLocation
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            DrawTaxiSolidButton(
                onClick = {
                    onUpdateSettings(settings.copy(
                        companyName = companyName,
                        name = driverName
                    ))
                    onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = settings.brandColor,
                enabled = companyName.isNotBlank() && driverName.isNotBlank()
            ) {
                Text("SUIVANT", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun OnboardingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = drawTaxiType().labelLarge,
            color = Slate400,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        content()
    }
}

@Composable
fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = if (!isGranted) onClick else ({}),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isGranted) Color(0xFFF0FDF4) else Slate50,
        border = if (isGranted) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF15803D) else Slate400,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) Color(0xFF15803D) else Slate900,
                    fontSize = 14.sp
                )
                Text(
                    text = description,
                    color = if (isGranted) Color(0xFF166534).copy(0.7f) else Slate400,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            if (isGranted) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D))
            } else {
                Text(
                    "AUTORISER",
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    color = Color.Blue,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    val sampleSettings = AppSettings()
    DrawTaxiTheme {
        OnboardingScreen(
            settings = sampleSettings,
            onUpdateSettings = {},
            onRequestSms = {},
            onRequestNotification = {},
            onRequestLocation = {},
            onComplete = {}
        )
    }
}


