package com.drawtaxi.app.ui.components.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.drawtaxi.app.ui.theme.DrawTaxiTheme

@Composable
fun DrawTaxiAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(28.dp)
) {
    Dialog(onDismissRequest = onDismissRequest) {
        DrawTaxiCard(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            elevation = 8.dp,
            onClick = null
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                if (title != null) {
                    Box(modifier = Modifier.fillMaxWidth()) { title() }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (text != null) {
                    text()
                    Spacer(modifier = Modifier.height(24.dp))
                }
                if (confirmButton != null || dismissButton != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                        if (dismissButton != null) {
                            dismissButton()
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (confirmButton != null) {
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DrawTaxiAlertDialogPreview() {
    DrawTaxiTheme {
        DrawTaxiAlertDialog(
            onDismissRequest = {},
            title = { Text("Confirmer la suppression", fontWeight = FontWeight.Bold) },
            text = { Text("Cette action est irréversible. Voulez-vous continuer ?") },
            confirmButton = {
                DrawTaxiButton(onClick = {}) { Text("Confirmer") }
            },
            dismissButton = {
                DrawTaxiOutlinedButton(onClick = {}) { Text("Annuler") }
            }
        )
    }
}
