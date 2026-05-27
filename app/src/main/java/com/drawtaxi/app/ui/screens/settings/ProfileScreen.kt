package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.drawtaxi.app.ui.components.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.drawtaxi.app.ui.theme.*

@Composable
fun ProfileScreen(
    validatedCount: Int,
    pendingCount: Int,
    onClearHistory: () -> Unit
) {
    Scaffold(
        topBar = {
            DrawTaxiTopBar(
                title = { DrawTaxiTopBarTitle("Mon Profil") }
            )
        },
        containerColor = drawTaxiColors().background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Profile Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(drawTaxiColors().primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                DrawTaxiIcon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = drawTaxiColors().primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Chauffeur DrawTaxi",
                style = drawTaxiType().bodyLarge,
                fontWeight = FontWeight.Bold,
                color = drawTaxiColors().onBackground
            )
            
            Text(
                "Version 1.0.0",
                style = drawTaxiType().bodySmall,
                color = drawTaxiColors().onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Stats Section
            ProfileSection(title = "Statistiques") {
                SettingsMenuItem(
                    title = "Courses validées",
                    subtitle = "$validatedCount terminées",
                    icon = Icons.Default.CheckCircle,
                    iconTint = Emerald500,
                    onClick = {}
                )
                DrawTaxiDivider(modifier = Modifier.padding(vertical = 4.dp))
                SettingsMenuItem(
                    title = "Courses en attente",
                    subtitle = "$pendingCount à traiter",
                    icon = Icons.Default.Schedule,
                    iconTint = Amber500,
                    onClick = {}
                )
                DrawTaxiDivider(modifier = Modifier.padding(vertical = 4.dp))
                SettingsMenuItem(
                    title = "Total historique",
                    subtitle = "${validatedCount + pendingCount} courses au total",
                    icon = Icons.Default.History,
                    iconTint = Indigo500,
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions Section
            ProfileSection(title = "Actions") {
                SettingsMenuItem(
                    title = "Effacer l'historique",
                    subtitle = "Action irréversible",
                    icon = Icons.Default.DeleteForever,
                    iconTint = Rose500,
                    onClick = onClearHistory
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileSection(
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
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp)) {
                content()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    DrawTaxiTheme {
        ProfileScreen(
            validatedCount = 10,
            pendingCount = 2,
            onClearHistory = {}
        )
    }
}
