package com.drawtaxi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.drawtaxi.app.ui.theme.Slate400
import com.drawtaxi.app.ui.theme.Slate700

@Composable
fun ProfileScreen(
    validatedCount: Int,
    pendingCount: Int,
    onClearHistory: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Historique complet", color = Slate700, fontWeight = FontWeight.Bold)
            Text("${validatedCount + pendingCount} courses enregistrées", color = Slate400, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClearHistory,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(0.1f), 
                    contentColor = Color.Red
                )
            ) {
                Text("Effacer tout l'historique")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(
        validatedCount = 10,
        pendingCount = 2,
        onClearHistory = {}
    )
}
