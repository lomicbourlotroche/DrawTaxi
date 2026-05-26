package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.ui.components.TaxiCard
import com.drawtaxi.app.ui.components.core.*
import com.drawtaxi.app.ui.theme.*

@Composable
fun MessageTemplatesScreen(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    var templates by remember { mutableStateOf(settings.messageTemplates.toMutableList()) }
    var newTemplate by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf(-1) }
    var editingText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        DrawTaxiTopBar(
            title = { Text("Templates de Messages") },
            navigationIcon = {
                DrawTaxiIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            backgroundColor = Color.Transparent
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TaxiCard(title = "Nouveau template") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTemplate,
                            onValueChange = { newTemplate = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Votre message...") },
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (newTemplate.isNotBlank()) {
                                    templates = (templates + newTemplate).toMutableList()
                                    newTemplate = ""
                                    onUpdateSettings(settings.copy(messageTemplates = templates))
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Green500,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Ajouter")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Templates enregistrés",
                    style = drawTaxiType().titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (templates.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate100)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucun template. Ajoutez-en un !",
                                color = Slate500
                            )
                        }
                    }
                }
            }

            itemsIndexed(templates) { index, template ->
                TemplateItem(
                    template = template,
                    onEdit = {
                        editingIndex = index
                        editingText = template
                    },
                    onDelete = {
                        templates = templates.toMutableList().also { it.removeAt(index) }
                        onUpdateSettings(settings.copy(messageTemplates = templates))
                    },
                    onSend = { /* Sera implémenté plus tard */ }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Ces templates seront disponibles sur l'écran de détail de course pour envoyer rapidement des messages au client.",
                    style = drawTaxiType().bodySmall,
                    color = Slate500
                )
            }
        }
    }

    if (editingIndex >= 0) {
        AlertDialog(
            onDismissRequest = {
                editingIndex = -1
                editingText = ""
            },
            title = { Text("Modifier le template") },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )
            },
            confirmButton = {
                DrawTaxiSolidButton(
                    onClick = {
                        templates = templates.toMutableList().also {
                            it[editingIndex] = editingText
                        }
                        onUpdateSettings(settings.copy(messageTemplates = templates))
                        editingIndex = -1
                        editingText = ""
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                DrawTaxiSolidButton(onClick = {
                    editingIndex = -1
                    editingText = ""
                }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun TemplateItem(
    template: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSend: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = template,
                style = drawTaxiType().bodyMedium,
                color = Slate700
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                DrawTaxiSolidButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modifier")
                }
                TextButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Red500)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Supprimer")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer ce template ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                DrawTaxiSolidButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    containerColor = Red500
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                DrawTaxiSolidButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MessageTemplatesScreenPreview() {
    val sampleSettings = AppSettings()
    DrawTaxiTheme {
        MessageTemplatesScreen(
            settings = sampleSettings,
            onUpdateSettings = {},
            onBack = {}
        )
    }
}


