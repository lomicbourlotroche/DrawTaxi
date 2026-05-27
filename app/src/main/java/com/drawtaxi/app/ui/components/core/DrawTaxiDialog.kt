package com.drawtaxi.app.ui.components.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.drawtaxi.app.ui.theme.DrawTaxiTheme

@Composable
fun DrawTaxiDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        DrawTaxiCard(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            elevation = 8.dp,
            onClick = null
        ) {
            Column(modifier = Modifier.padding(24.dp).heightIn(max = 560.dp)) {
                if (icon != null) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        icon()
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (title != null) {
                    Box(modifier = Modifier.fillMaxWidth()) { title() }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    if (text != null) {
                        text()
                    }
                }
                if (text != null) {
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
fun DrawTaxiDialogPreview() {
    DrawTaxiTheme {
        DrawTaxiDialog(
            onDismissRequest = {},
            title = { Text("Titre du dialogue", fontWeight = FontWeight.Bold) },
            text = { Text("Message informatif ou de confirmation.") },
            confirmButton = {
                DrawTaxiButton(onClick = {}) { Text("OK") }
            },
            dismissButton = {
                DrawTaxiOutlinedButton(onClick = {}) { Text("Annuler") }
            }
        )
    }
}
