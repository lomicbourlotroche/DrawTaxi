package com.drawtaxi.app.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.ui.components.core.DrawTaxiCard
import com.drawtaxi.app.ui.components.core.DrawTaxiSolidButton
import com.drawtaxi.app.ui.components.core.DrawTaxiSurface
import com.drawtaxi.app.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

private data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private val steps = listOf(
    OnboardingStep(
        title = "Votre Identité",
        subtitle = "Comment souhaitez-vous apparaître auprès de vos clients ?",
        icon = Icons.Default.Person
    ),
    OnboardingStep(
        title = "Autorisations",
        subtitle = "Quelques accès indispensables pour que DrawTaxi fonctionne.",
        icon = Icons.Default.Shield
    ),
    OnboardingStep(
        title = "Prêt !",
        subtitle = "Vous pouvez commencer à utiliser DrawTaxi.",
        icon = Icons.Default.Star
    )
)

@Composable
fun OnboardingScreen(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit,
    onRequestSms: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestLocation: () -> Unit,
    onComplete: () -> Unit
) {
    var companyName by remember { mutableStateOf(settings.companyName) }
    var driverName by remember { mutableStateOf(settings.name) }
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    val brandColor = settings.brandColor

    val animatedProgress by animateFloatAsState(
        targetValue = (pagerState.currentPage + 1).toFloat() / steps.size,
        animationSpec = tween(400),
        label = "progress"
    )

    DrawTaxiSurface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Slate100)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(brandColor, brandColor.copy(alpha = 0.6f))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Page content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        slideInHorizontally(
                            animationSpec = tween(350, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> direction * fullWidth / 3 }
                        ) + fadeIn(animationSpec = tween(250)) togetherWith
                                slideOutHorizontally(
                                    animationSpec = tween(250),
                                    targetOffsetX = { fullWidth -> -direction * fullWidth / 3 }
                                ) + fadeOut(animationSpec = tween(150))
                    },
                    label = "pageTransition"
                ) { currentPage ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (currentPage) {
                            0 -> IdentityStep(
                                companyName = companyName,
                                driverName = driverName,
                                brandColor = brandColor,
                                onCompanyNameChange = { companyName = it },
                                onDriverNameChange = { driverName = it }
                            )
                            1 -> PermissionsStep(
                                settings = settings,
                                brandColor = brandColor,
                                onRequestSms = onRequestSms,
                                onRequestNotification = onRequestNotification,
                                onRequestLocation = onRequestLocation
                            )
                            2 -> CompletionStep(
                                driverName = driverName,
                                brandColor = brandColor
                            )
                        }
                    }
                }
            }

            // Bottom section
            Column(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, _ ->
                        val isActive = index == pagerState.currentPage
                        val dotScale by animateFloatAsState(
                            targetValue = if (isActive) 1f else 0.7f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "dotScale"
                        )
                        Box(
                            modifier = Modifier
                                .size(if (isActive) 10.dp else 8.dp)
                                .scale(dotScale)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) brandColor
                                    else if (index < pagerState.currentPage) brandColor.copy(alpha = 0.4f)
                                    else Slate200
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                DrawTaxiSolidButton(
                    onClick = {
                        if (pagerState.currentPage < steps.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onUpdateSettings(settings.copy(
                                companyName = companyName,
                                name = driverName
                            ))
                            onComplete()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = brandColor,
                    enabled = when (pagerState.currentPage) {
                        0 -> companyName.isNotBlank() && driverName.isNotBlank()
                        else -> true
                    }
                ) {
                    Text(
                        text = if (pagerState.currentPage < steps.size - 1) "SUIVANT" else "COMMENCER",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (pagerState.currentPage < steps.size - 1) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    } else {
                        Icon(Icons.Default.ThumbUp, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    brandColor: Color
) {
    Column(
        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DrawTaxiSurface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(20.dp),
            color = brandColor.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = brandColor
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = drawTaxiType().headlineMedium,
            fontWeight = FontWeight.Black,
            color = Slate900,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = drawTaxiType().bodyMedium,
            color = Slate400,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun IdentityStep(
    companyName: String,
    driverName: String,
    brandColor: Color,
    onCompanyNameChange: (String) -> Unit,
    onDriverNameChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StepHeader(
            icon = Icons.Default.Person,
            title = "Votre Identité",
            subtitle = "Comment souhaitez-vous apparaître auprès de vos clients ?",
            brandColor = brandColor
        )

        DrawTaxiCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color.White,
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "NOM DE LA COMPAGNIE",
                    style = drawTaxiType().labelSmall,
                    color = Slate400,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = companyName,
                    onValueChange = onCompanyNameChange,
                    placeholder = { Text("Ex: Taxis Parisiens") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandColor,
                        focusedContainerColor = brandColor.copy(alpha = 0.04f),
                        unfocusedContainerColor = Slate50,
                        unfocusedBorderColor = Slate100
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "VOTRE NOM",
                    style = drawTaxiType().labelSmall,
                    color = Slate400,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = driverName,
                    onValueChange = onDriverNameChange,
                    placeholder = { Text("Ex: Jean Dupont") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandColor,
                        focusedContainerColor = brandColor.copy(alpha = 0.04f),
                        unfocusedContainerColor = Slate50,
                        unfocusedBorderColor = Slate100
                    )
                )
            }
        }
    }
}

@Composable
private fun PermissionsStep(
    settings: AppSettings,
    brandColor: Color,
    onRequestSms: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestLocation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StepHeader(
            icon = Icons.Default.Shield,
            title = "Autorisations",
            subtitle = "Quelques accès indispensables pour que DrawTaxi fonctionne.",
            brandColor = brandColor
        )

        PermissionCard(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = "SMS",
            description = "Détecter les réservations et répondre aux clients.",
            isGranted = settings.monitorSms,
            brandColor = brandColor,
            onClick = onRequestSms,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        PermissionCard(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            description = "Être alerté dès qu'une nouvelle course arrive.",
            isGranted = settings.enableNotifications,
            brandColor = brandColor,
            onClick = onRequestNotification,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        PermissionCard(
            icon = Icons.Default.MyLocation,
            title = "Localisation",
            description = "Calculer les distances et les tarifs.",
            isGranted = settings.trackLocation,
            brandColor = brandColor,
            onClick = onRequestLocation
        )
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    brandColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isGranted) Color(0xFFF0FDF4) else Color.White,
        animationSpec = tween(300),
        label = "permBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isGranted) Color(0xFFBBF7D0) else Slate100,
        animationSpec = tween(300),
        label = "permBorder"
    )

    DrawTaxiCard(
        modifier = modifier.fillMaxWidth(),
        onClick = if (!isGranted) onClick else null,
        backgroundColor = bgColor,
        elevation = if (isGranted) 0.dp else 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawTaxiSurface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isGranted) Color(0xFFDCFCE7) else brandColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (isGranted) Color(0xFF15803D) else brandColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) Color(0xFF15803D) else Slate900,
                    fontSize = 15.sp
                )
                Text(
                    text = description,
                    color = if (isGranted) Color(0xFF166534).copy(alpha = 0.6f) else Slate400,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Autorisé",
                    tint = Color(0xFF15803D),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                DrawTaxiSurface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !isGranted, onClick = onClick),
                    color = brandColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "ACTIVER",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = brandColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionStep(
    driverName: String,
    brandColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StepHeader(
            icon = Icons.Default.Star,
            title = "Prêt !",
            subtitle = "Vous pouvez commencer à utiliser DrawTaxi.",
            brandColor = brandColor
        )

        DrawTaxiCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color.White,
            elevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DrawTaxiSurface(
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    color = brandColor.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = brandColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Ravi de vous accueillir${if (driverName.isNotBlank()) ", $driverName" else ""}",
                    style = drawTaxiType().titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "DrawTaxi est configuré et prêt à vous accompagner au quotidien.",
                    style = drawTaxiType().bodyMedium,
                    color = Slate400,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    val sampleSettings = AppSettings()
    DrawTaxiTheme {
        OnboardingScreen(
            settings = sampleSettings,
            onUpdateSettings = {},
            onRequestSms = {},
            onRequestNotification = {},
            onRequestLocation = {},
            onComplete = {}
        )
    }
}
