package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var templates by remember { mutableStateOf(settings.messageTemplates) }
    var newTemplate by remember { mutableStateOf("") }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var editingText by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = drawTaxiColors().background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DrawTaxiTopBar(
                title = { DrawTaxiTopBarTitle("Templates de Messages") },
                navigationIcon = {
                    DrawTaxiIconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = drawTaxiColors().onSurface
                        )
                    }
                },
                backgroundColor = Color.Transparent
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    TaxiCard(
                        title = "Nouveau template",
                        titleIcon = Icons.Default.AddComment,
                        brandColor = drawTaxiColors().primary
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            OutlinedTextField(
                                value = newTemplate,
                                onValueChange = { newTemplate = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        "Tapez votre message ici...",
                                        style = drawTaxiType().bodyMedium,
                                        color = drawTaxiColors().secondary
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = drawTaxiColors().primary,
                                    unfocusedBorderColor = drawTaxiColors().outline,
                                    unfocusedContainerColor = drawTaxiColors().surfaceVariant.copy(alpha = 0.3f),
                                    focusedContainerColor = Color.White
                                ),
                                textStyle = drawTaxiType().bodyMedium,
                                maxLines = 4
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            DrawTaxiIconButton(
                                onClick = {
                                    if (newTemplate.isNotBlank()) {
                                        val newList = templates + newTemplate
                                        templates = newList
                                        onUpdateSettings(settings.copy(messageTemplates = newList))
                                        newTemplate = ""
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.size(52.dp),
                                enabled = newTemplate.isNotBlank()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (newTemplate.isNotBlank()) drawTaxiColors().primary else drawTaxiColors().outline,
                                            RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (newTemplate.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Add,
                                        contentDescription = "Ajouter",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Mes Templates",
                            style = drawTaxiType().titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = drawTaxiColors().onSurface
                        )
                        if (templates.isNotEmpty()) {
                            Surface(
                                color = drawTaxiColors().primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${templates.size}",
                                    style = drawTaxiType().labelMedium,
                                    color = drawTaxiColors().primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (templates.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(drawTaxiColors().surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = drawTaxiColors().secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Aucun template enregistré",
                                style = drawTaxiType().titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = drawTaxiColors().onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ajoutez des réponses rapides pour gagner du temps avec vos clients.",
                                style = drawTaxiType().bodySmall,
                                color = drawTaxiColors().secondary,
                                textAlign = TextAlign.Center
                            )
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
                            val newList = templates.filterIndexed { i, _ -> i != index }
                            templates = newList
                            onUpdateSettings(settings.copy(messageTemplates = newList))
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        color = drawTaxiColors().primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, drawTaxiColors().primary.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = drawTaxiColors().primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Astuce : Ces templates sont accessibles directement depuis l'écran de course pour des réponses instantanées.",
                                style = drawTaxiType().bodySmall,
                                color = drawTaxiColors().onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingIndex >= 0) {
        AlertDialog(
            onDismissRequest = {
                editingIndex = -1
                editingText = ""
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = drawTaxiColors().surface,
            title = {
                Text(
                    "Modifier le template",
                    style = drawTaxiType().titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = drawTaxiColors().primary,
                        unfocusedBorderColor = drawTaxiColors().outline,
                        unfocusedContainerColor = drawTaxiColors().surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = drawTaxiColors().surface
                    ),
                    textStyle = drawTaxiType().bodyMedium,
                    maxLines = 5
                )
            },
            confirmButton = {
                DrawTaxiSolidButton(
                    onClick = {
                        val newList = templates.toMutableList().also {
                            it[editingIndex] = editingText
                        }
                        templates = newList
                        onUpdateSettings(settings.copy(messageTemplates = newList))
                        editingIndex = -1
                        editingText = ""
                    },
                    modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingIndex = -1
                        editingText = ""
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text("Annuler", color = drawTaxiColors().secondary)
                }
            }
        )
    }
}

@Composable
private fun TemplateItem(
    template: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    TaxiCard(
        modifier = Modifier.fillMaxWidth(),
        brandColor = drawTaxiColors().primary
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = template,
                style = drawTaxiType().bodyLarge,
                color = drawTaxiColors().onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                DrawTaxiIconButton(
                    onClick = onEdit,
                    size = 40.dp
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Modifier",
                        modifier = Modifier.size(18.dp),
                        tint = drawTaxiColors().primary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                DrawTaxiIconButton(
                    onClick = { showDeleteDialog = true },
                    size = 40.dp
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        modifier = Modifier.size(18.dp),
                        tint = Red500.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = drawTaxiColors().surface,
            title = {
                Text(
                    "Supprimer ce template ?",
                    style = drawTaxiType().titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text("Cette action est irréversible.", style = drawTaxiType().bodyMedium) },
            confirmButton = {
                DrawTaxiSolidButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    containerColor = Red500,
                    modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text("Annuler", color = drawTaxiColors().secondary)
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MessageTemplatesScreenPreview() {
    val sampleSettings = AppSettings(
        messageTemplates = listOf(
            "Bonjour, je suis en retard de quelques minutes.",
            "Bonjour, j'arrive !",
            "Bonjour, où êtes-vous exactement ?"
        )
    )
    DrawTaxiTheme {
        MessageTemplatesScreen(
            settings = sampleSettings,
            onUpdateSettings = {},
            onBack = {}
        )
    }
}
