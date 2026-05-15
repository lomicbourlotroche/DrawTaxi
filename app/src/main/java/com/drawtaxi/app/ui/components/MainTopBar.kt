package com.drawtaxi.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.drawtaxi.app.data.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    activeTab: String,
    settingsView: String,
    settings: AppSettings
) {
    TopAppBar(
        title = {
            val titleText = when {
                activeTab == "settings" -> when (settingsView) {
                    "proInfo" -> "Infos Pro"
                    "branding" -> "Visuel"
                    else -> "Paramètres"
                }
                activeTab == "message" -> "Centre de Contrôle"
                else -> settings.companyName
            }
            Text(
                text = titleText.uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic,
                    color = settings.brandColor
                )
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@Preview
@Composable
fun MainTopBarHomePreview() {
    MainTopBar(
        activeTab = "home",
        settingsView = "main",
        settings = AppSettings(companyName = "DrawTaxi")
    )
}

@Preview
@Composable
fun MainTopBarControlCenterPreview() {
    MainTopBar(
        activeTab = "message",
        settingsView = "main",
        settings = AppSettings(companyName = "DrawTaxi")
    )
}
