package com.drawtaxi.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun RideCreateTimerButton(
    timeLeft: Int,
    isTimerRunning: Boolean,
    onConfirm: () -> Unit,
    brandColor: Color
) {
    Button(
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = brandColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (isTimerRunning && timeLeft > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    progress = { timeLeft / 10f },
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(0.3f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Ajout auto ($timeLeft s)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text("Confirmer la course", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview
@Composable
fun RideCreateTimerButtonRunningPreview() {
    RideCreateTimerButton(
        timeLeft = 7,
        isTimerRunning = true,
        onConfirm = {},
        brandColor = Color.Blue
    )
}

@Preview
@Composable
fun RideCreateTimerButtonStoppedPreview() {
    RideCreateTimerButton(
        timeLeft = 0,
        isTimerRunning = false,
        onConfirm = {},
        brandColor = Color.Blue
    )
}
