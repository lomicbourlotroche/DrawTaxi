package com.drawtaxi.app.ui.screens.onboarding

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
import com.drawtaxi.app.ui.components.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
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
import com.drawtaxi.app.logic.ai.LlamaModelManager
import com.drawtaxi.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    var downloadSpeed by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableStateOf("") }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "downloadProgress"
    )

    val modelSize = LlamaModelManager.getDownloadedSize(context)
    val expectedSizeText = "~2,0 Go"

    // Live updates for speed and ETA during download
    LaunchedEffect(isDownloading) {
        if (isDownloading) {
            while (true) {
                downloadSpeed = LlamaModelManager.getDownloadSpeed(context)
                timeRemaining = LlamaModelManager.estimateTimeRemaining(context, progress)
                delay(2000)
            }
        }
    }

    DrawTaxiSurface(
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

            DrawTaxiSurface(
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
                style = drawTaxiType().headlineMedium,
                fontWeight = FontWeight.Black,
                color = Slate900,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isComplete)
                    "Llama 3.2 3B est installé. Le parsing SMS sera plus précis."
                else
                    "Téléchargez le modèle Llama 3.2 pour un parsing SMS intelligent hors ligne.",
                style = drawTaxiType().bodyMedium,
                color = Slate400,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

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

            AnimatedVisibility(
                visible = isDownloading || isComplete,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                            style = drawTaxiType().labelMedium,
                            color = Slate500
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = drawTaxiType().labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = brandColor
                        )
                    }

                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = downloadSpeed,
                                style = drawTaxiType().labelSmall,
                                color = Slate400
                            )
                            Text(
                                text = timeRemaining,
                                style = drawTaxiType().labelSmall,
                                color = Slate400
                            )
                        }
                    }

                    val downloadedMb = LlamaModelManager.getDownloadedSize(context) / 1_000_000
                    if (downloadedMb > 0 && !isComplete) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${downloadedMb} Mo / 2 000 Mo",
                            style = drawTaxiType().labelSmall,
                            color = Slate400
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (!isComplete) {
                val connectionType = remember { LlamaModelManager.getConnectionType(context) }
                if (connectionType != "WiFi") {
                    DrawTaxiSurface(
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
                                style = drawTaxiType().labelSmall,
                                color = Amber700
                            )
                        }
                    }
                }

                var retryCount by remember { mutableStateOf(0) }
                var lastError by remember { mutableStateOf("") }

                DrawTaxiSolidButton(
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
                                        prog < 0.05f -> "Préparation..."
                                        prog < 0.3f -> "Téléchargement ${(prog * 100).toInt()}%"
                                        prog < 0.7f -> "Téléchargement ${(prog * 100).toInt()}%"
                                        prog < 0.95f -> "Finalisation..."
                                        else -> "Vérification..."
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
                    containerColor = brandColor,
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
                    DrawTaxiSurface(
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
                                style = drawTaxiType().labelSmall,
                                color = Rose700,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Conseils : Vérifiez votre connexion WiFi, désactivez le VPN, ou réessayez plus tard.",
                                style = drawTaxiType().labelSmall,
                                color = Rose600,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DrawTaxiSolidButton(
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
                DrawTaxiSolidButton(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Emerald500
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
        DrawTaxiSurface(
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

@Preview(showBackground = true)
@Composable
fun AiModelDownloadScreenPreview() {
    DrawTaxiTheme {
        AiModelDownloadScreen(
            brandColor = Color(0xFF6366F1),
            onSkip = {},
            onComplete = {}
        )
    }
}


