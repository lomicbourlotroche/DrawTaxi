package com.drawtaxi.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.drawtaxi.app.logic.LlamaModelManager
import com.drawtaxi.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiModelDownloadScreen(
    brandColor: Color = Indigo500,
    onSkip: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var isComplete by remember { mutableStateOf(LlamaModelManager.isModelAvailable(context)) }
    var hasError by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "downloadProgress"
    )

    val modelSize = LlamaModelManager.getDownloadedSize(context)
    val expectedSizeText = "~2,0 Go"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isComplete) Emerald50 else Slate50
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Icon with gradient background
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = if (isComplete) Emerald500.copy(0.1f) else brandColor.copy(0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.Memory,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = if (isComplete) Emerald500 else brandColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isComplete) "Modèle prêt !" else "Intelligence Artificielle",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Slate900,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isComplete)
                    "Llama 3.2 3B est installé. Le parsing SMS sera plus précis."
                else
                    "Téléchargez le modèle Llama 3.2 pour un parsing SMS intelligent hors ligne.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Features list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureItem(
                    icon = Icons.Default.Shield,
                    title = "100% hors ligne",
                    description = "Vos SMS ne quittent jamais votre téléphone"
                )
                FeatureItem(
                    icon = Icons.Default.Speed,
                    title = "Plus précis",
                    description = "Comprend les formulations complexes et le contexte"
                )
                FeatureItem(
                    icon = Icons.Default.Storage,
                    title = "Taille du modèle",
                    description = expectedSizeText
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Progress section
            AnimatedVisibility(
                visible = isDownloading || isComplete,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Slate200)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(brandColor, Violet500)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate500
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = brandColor
                        )
                    }

                    if (modelSize > 0 && !isComplete) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${modelSize / 1_000_000} Mo / ${expectedSizeText}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Action buttons
            if (!isComplete) {
                // Afficher le type de connexion
                val connectionType = remember { LlamaModelManager.getConnectionType(context) }
                if (connectionType != "WiFi") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Amber100,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Amber600,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Connexion $connectionType - ~2 Go à télécharger",
                                style = MaterialTheme.typography.labelSmall,
                                color = Amber700
                            )
                        }
                    }
                }

                var retryCount by remember { mutableStateOf(0) }
                var lastError by remember { mutableStateOf("") }

                Button(
                    onClick = {
                        isDownloading = true
                        hasError = false
                        retryCount = 0
                        statusText = "Vérification de la connexion..."
                        scope.launch {
                            val success = LlamaModelManager.downloadModel(
                                context = context,
                                maxRetries = 5,
                                onProgress = { prog ->
                                    progress = prog
                                    statusText = when {
                                        prog < 0.05f -> "Préparation du téléchargement..."
                                        prog < 0.3f -> "Téléchargement en cours (${(prog * 100).toInt()}%)"
                                        prog < 0.7f -> "Téléchargement... ${(prog * 100).toInt()}%"
                                        prog < 0.95f -> "Finalisation..."
                                        else -> "Vérification du fichier..."
                                    }
                                },
                                onRetry = { attempt, error ->
                                    retryCount = attempt
                                    lastError = error
                                    statusText = "Tentative $attempt/5... ($error)"
                                },
                                onStatusChange = { status ->
                                    when (status) {
                                        LlamaModelManager.ModelStatus.DOWNLOADING -> isDownloading = true
                                        LlamaModelManager.ModelStatus.READY -> {
                                            isDownloading = false
                                            isComplete = true
                                            statusText = "Téléchargement terminé !"
                                        }
                                        LlamaModelManager.ModelStatus.ERROR -> {
                                            isDownloading = false
                                            hasError = true
                                        }
                                        else -> {}
                                    }
                                }
                            )
                            isDownloading = false
                            if (success) {
                                isComplete = true
                                statusText = "Téléchargement terminé !"
                            } else {
                                hasError = true
                                statusText = if (retryCount > 0) {
                                    "Échec après 5 tentatives - $lastError"
                                } else {
                                    "Échec du téléchargement - Vérifiez votre connexion"
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                    enabled = !isDownloading
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isDownloading) "Téléchargement..." else if (modelSize > 0) "REPRENDRE LE TÉLÉCHARGEMENT" else "TÉLÉCHARGER LE MODÈLE",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (hasError) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Rose100,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Rose600,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Rose700,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Conseils : Vérifiez votre connexion WiFi, désactivez le VPN, ou réessayez plus tard.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Rose600,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Passer pour l'instant (parsing basique activé)",
                        color = Slate500,
                        fontSize = 13.sp
                    )
                }
            } else {
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Text("CONTINUER", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(10.dp),
            color = Indigo500.copy(0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Indigo500
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                fontSize = 14.sp
            )
            Text(
                text = description,
                color = Slate500,
                fontSize = 12.sp
            )
        }
    }
}
