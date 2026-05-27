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
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(28.dp),
                color = if (isComplete) Emerald500.copy(0.1f) else brandColor.copy(0.1f),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = if (isComplete)
                                    listOf(Emerald500.copy(0.05f), Emerald500.copy(0.15f))
                                else
                                    listOf(brandColor.copy(0.05f), brandColor.copy(0.15f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.Memory,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (isComplete) Emerald500 else brandColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (isComplete) "Modèle prêt !" else "Intelligence Artificielle",
                style = drawTaxiType().displaySmall,
                fontWeight = FontWeight.Black,
                color = Slate900,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isComplete)
                    "Le modèle Qwen3 4B est installé avec succès."
                else
                    "Améliorez la précision du parsing SMS avec le modèle Qwen3 4B.",
                style = drawTaxiType().bodyMedium,
                color = Slate500,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureItem(
                    icon = Icons.Default.Shield,
                    title = "100% hors ligne",
                    description = "Vos données ne quittent jamais l'appareil",
                    brandColor = brandColor
                )
                FeatureItem(
                    icon = Icons.Default.Speed,
                    title = "Parsing Précis",
                    description = "Compréhension avancée des messages",
                    brandColor = brandColor
                )
                FeatureItem(
                    icon = Icons.Default.Storage,
                    title = "Optimisé",
                    description = "Modèle compressé de $expectedSizeText",
                    brandColor = brandColor
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

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
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Slate100)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(brandColor, brandColor.copy(0.8f))
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

                    val downloadedMb = LlamaModelManager.getDownloadedSize(context).toFloat() / 1_000_000
                    if (downloadedMb > 0 && !isComplete) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%.1f Mo / 2 000 Mo".format(downloadedMb),
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
                        shape = RoundedCornerShape(12.dp),
                        color = Amber50,
                        modifier = Modifier.padding(bottom = 20.dp),
                        borderWidth = 1.dp,
                        borderColor = Amber100
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = Amber600,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Connexion $connectionType - WiFi recommandé (~2 Go)",
                                style = drawTaxiType().labelMedium,
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

                DrawTaxiOutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth(),
                    contentColor = Slate500,
                    borderColor = Slate200
                ) {
                    Text(
                        text = "Passer pour l'instant (parsing basique)",
                        style = drawTaxiType().labelLarge,
                        fontWeight = FontWeight.Medium
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
    description: String,
    brandColor: Color
) {
    DrawTaxiSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        borderWidth = 1.dp,
        borderColor = Slate100
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawTaxiSurface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = brandColor.copy(0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = brandColor
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = drawTaxiType().titleSmall,
                    color = Slate900,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = drawTaxiType().bodySmall,
                    color = Slate500
                )
            }
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


