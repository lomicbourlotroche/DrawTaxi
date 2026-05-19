package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.ui.components.TaxiCard
import com.drawtaxi.app.ui.theme.*

import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BrandingSettings(settings: AppSettings, onUpdate: (AppSettings) -> Unit, onBack: () -> Unit) {
    val colors = listOf(TaxiRed, Color(0xFF7C3AED), Color(0xFFDB2777), Color(0xFF019669), Color(0xFFEA580C), Slate800)
    
    Column(modifier = Modifier.fillMaxSize().background(Slate50).verticalScroll(rememberScrollState())) {
        TextButton(onClick = onBack, modifier = Modifier.padding(bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null)
                Text("Retour", fontWeight = FontWeight.SemiBold)
            }
        }

        TaxiCard(title = "Aperçu", brandColor = settings.brandColor) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp).background(settings.brandColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocalTaxi, contentDescription = null, tint = settings.brandColor, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(settings.companyName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = settings.brandColor)
                    Text(settings.name, style = MaterialTheme.typography.bodyMedium, color = Slate600)
                }
            }
        }

        TaxiCard(title = "Couleur de marque", brandColor = settings.brandColor) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(color, CircleShape)
                            .border(
                                width = if (settings.brandColor == color) 3.dp else 0.dp,
                                color = if (settings.brandColor == color) Slate900 else Color.Transparent,
                                shape = CircleShape
                            )
                            .padding(if (settings.brandColor == color) 4.dp else 0.dp)
                            .clip(CircleShape)
                            .clickable { onUpdate(settings.copy(brandColor = color)) }
                    )
                }
            }
        }

        TaxiCard(title = "Thème", brandColor = settings.brandColor) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeToggleButton(label = "Moderne", isSelected = settings.theme == "modern", onClick = { onUpdate(settings.copy(theme = "modern")) }, modifier = Modifier.weight(1f))
                ThemeToggleButton(label = "Minimal", isSelected = settings.theme == "minimal", onClick = { onUpdate(settings.copy(theme = "minimal")) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1F5F9)
@Composable
fun BrandingSettingsPreview() {
    val mockSettings = AppSettings(
        brandColor = TaxiRed,
        theme = "modern",
        companyName = "DrawTaxi Service"
    )
    Box(modifier = Modifier.padding(16.dp)) {
        BrandingSettings(settings = mockSettings, onUpdate = {}, onBack = {})
    }
}
