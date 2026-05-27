package com.drawtaxi.app.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
 import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import com.drawtaxi.app.ui.components.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.RideStatus
import com.drawtaxi.app.util.PermissionHelper
import com.drawtaxi.app.util.isIgnoringBatteryOptimizations
import com.drawtaxi.app.ui.components.*
import com.drawtaxi.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    validatedRides: List<RideRequest>,
    confirmedRides: List<RideRequest>,
    brandColor: Color,
    onRideClick: (RideRequest) -> Unit,
    onCreateRide: () -> Unit,
    settings: com.drawtaxi.app.data.AppSettings,
    isIgnoringBatteryOptimization: Boolean? = null
) {
    val context = LocalContext.current
    val colors = drawTaxiColors()
    val actualIsIgnoring = isIgnoringBatteryOptimization ?: remember { context.isIgnoringBatteryOptimizations }

    val now = Calendar.getInstance()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val todayDateStr = dateFormat.format(now.time)

    val completedRides = validatedRides.filter { it.isPending == false }.sortedWith(
        compareByDescending<RideRequest> { it.date.ifBlank { "01/01/1970" } }
            .thenByDescending { it.time.ifBlank { "00:00" } }
    )
    val recentCompletedRides = completedRides.take(5)

    val todayCompletedRides = completedRides.filter { ride ->
        ride.date.ifBlank { todayDateStr } == todayDateStr
    }
    val todayRevenue = todayCompletedRides.sumOf { it.price }
    val totalKmToday = todayCompletedRides.sumOf { it.distanceKm }
    val todayCoutDeplacement = todayCompletedRides.sumOf { it.fuelCost.takeIf { c -> c > 0 } ?: (it.distanceKm * 0.3 * settings.coutParKmDeplacement) }
    val todayNet = todayRevenue - todayCoutDeplacement

    // Course EN COURS active
    val rideInProgress = confirmedRides.firstOrNull { it.status == RideStatus.IN_PROGRESS }

    // Prochaine course confirmée (dans l'avenir)
    val nextRide = confirmedRides
        .filter { it.status == RideStatus.CONFIRMED && it.time.isNotBlank() }
        .sortedWith(compareBy({ it.date }, { it.time }))
        .firstOrNull()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(bottom = 100.dp)
        ) {
            // ── HEADER HERO ──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    brandColor,
                                    brandColor.copy(alpha = 0.85f),
                                    brandColor.copy(alpha = 0.6f)
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                ) {
                    Column {
                        // Salutation + bouton +
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                androidx.compose.material3.Text(
                                    text = greetingForHour(),
                                    style = drawTaxiType().bodyLarge,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                                androidx.compose.material3.Text(
                                    text = settings.name.ifBlank { "Chauffeur" },
                                    style = drawTaxiType().headlineLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            // Bouton + inline
                            DrawTaxiIconButton(
                                onClick = onCreateRide,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                    .size(52.dp)
                            ) {
                                DrawTaxiIcon(
                                    Icons.Default.Add,
                                    contentDescription = "Nouvelle course",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        // Stats du jour — Modern Tiles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GlassStatCard(
                                value = String.format(Locale.getDefault(), "%.0f€", todayRevenue),
                                label = "Revenus",
                                icon = Icons.Default.EuroSymbol,
                                modifier = Modifier.weight(1f)
                            )
                            GlassStatCard(
                                value = "${todayCompletedRides.size}",
                                label = "Courses",
                                icon = Icons.Default.LocalTaxi,
                                modifier = Modifier.weight(1f)
                            )
                            GlassStatCard(
                                value = String.format(Locale.getDefault(), "%.0f", totalKmToday),
                                label = "km",
                                icon = Icons.Default.Speed,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Bénéfice net (si courses du jour)
                        if (todayCompletedRides.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(18.dp))
                            DrawTaxiSurface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                borderWidth = 1.dp,
                                borderColor = Color.White.copy(alpha = 0.08f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(Color.White.copy(alpha = 0.1f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            DrawTaxiIcon(
                                                Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = if (todayNet >= 0) Emerald500 else Rose500,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            androidx.compose.material3.Text(
                                                "Bénéfice net estimé",
                                                style = drawTaxiType().labelSmall,
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontWeight = FontWeight.Medium
                                            )
                                            androidx.compose.material3.Text(
                                                String.format(Locale.getDefault(), "%.2f €", todayNet),
                                                style = drawTaxiType().titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    val profitPercent = if (todayRevenue > 0) (todayNet / todayRevenue) * 100.0 else 0.0
                                    val pillColor = when {
                                        profitPercent >= 60 -> Emerald500
                                        profitPercent >= 40 -> Amber500
                                        else -> Rose500
                                    }
                                    DrawTaxiSurface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = pillColor.copy(alpha = 0.25f)
                                    ) {
                                        androidx.compose.material3.Text(
                                            text = "${String.format(Locale.getDefault(), "%.0f", profitPercent)}%",
                                            style = drawTaxiType().labelMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── BANNIÈRE BATTERIE ──
            if (!actualIsIgnoring && settings.monitorSms) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    BatteryWarningBanner(onFixClick = { PermissionHelper.requestIgnoreBatteryOptimization(context) })
                }
            }

            // ── COURSE EN COURS (pulsante) ──
            if (rideInProgress != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    InProgressRideBanner(
                        ride = rideInProgress,
                        brandColor = brandColor,
                        onClick = { onRideClick(rideInProgress) }
                    )
                }
            }

            // ── WIDGET PROCHAINE COURSE ──
            if (nextRide != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    NextRideCard(
                        ride = nextRide,
                        brandColor = brandColor,
                        onClick = { onRideClick(nextRide) }
                    )
                }
            }

            // ── COURSES CONFIRMÉES ──
            if (confirmedRides.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(22.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp, 24.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Emerald500)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        androidx.compose.material3.Text(
                            text = "Courses confirmées",
                            style = drawTaxiType().titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.onBackground
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        DrawTaxiSurface(
                            shape = CircleShape,
                            color = Emerald500.copy(alpha = 0.1f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                androidx.compose.material3.Text(
                                    text = "${confirmedRides.size}",
                                    style = drawTaxiType().labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald600
                                )
                            }
                        }
                    }
                }
                items(confirmedRides) { ride ->
                    ConfirmedRideCard(ride = ride, brandColor = brandColor, onClick = { onRideClick(ride) })
                }
            }

            // ── DERNIÈRES COURSES ──
            if (recentCompletedRides.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(26.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp, 24.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(colors.onSurfaceVariant.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        androidx.compose.material3.Text(
                            text = "Dernières courses",
                            style = drawTaxiType().titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.onBackground
                        )
                    }
                }
                 items(recentCompletedRides) { ride ->
                     // TODO: Replace with actual card component
                     // RideHistoryCard(ride = ride, brandColor = brandColor, onClick = { onRideClick(ride) })
                 }
            }

            // ── EMPTY STATE ──
            if (confirmedRides.isEmpty() && completedRides.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(60.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    Brush.radialGradient(listOf(brandColor.copy(alpha = 0.15f), Color.Transparent)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            DrawTaxiSurface(
                                shape = CircleShape,
                                color = brandColor.copy(alpha = 0.1f),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    DrawTaxiIcon(
                                        Icons.Default.LocalTaxi,
                                        contentDescription = null,
                                        tint = brandColor,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        androidx.compose.material3.Text(
                            "Prêt pour la route ?",
                            style = drawTaxiType().headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = colors.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            "Créez une course ou attendez vos SMS clients pour commencer.",
                            style = drawTaxiType().bodyMedium,
                            color = colors.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        DrawTaxiSolidButton(
                            onClick = onCreateRide,
                            shape = RoundedCornerShape(20.dp),
                            containerColor = brandColor,
                            modifier = Modifier.width(200.dp)
                        ) {
                            DrawTaxiIcon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.material3.Text("Nouvelle course", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        // ── FAB PULSANT ──
        PulsingFAB(
            onClick = onCreateRide,
            brandColor = brandColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp)
        )
    }
}

// ── Carte glassmorphism dans le header ──
@Composable
private fun GlassStatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    DrawTaxiSurface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.12f),
        borderWidth = 1.dp,
        borderColor = Color.White.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                DrawTaxiIcon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            androidx.compose.material3.Text(
                text = value,
                style = drawTaxiType().titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
            androidx.compose.material3.Text(
                text = label,
                style = drawTaxiType().labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Bannière course EN COURS (pulsante) ──
@Composable
fun InProgressRideBanner(
    ride: RideRequest,
    brandColor: Color,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "inProgress")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulseScale"
    )

    DrawTaxiSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .scale(pulseScale),
        shape = RoundedCornerShape(20.dp),
        color = Emerald500.copy(alpha = 0.08f),
        borderWidth = 1.dp,
        borderColor = Emerald500.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Emerald500.copy(alpha = pulseAlpha * 0.2f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Emerald500, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    DrawTaxiIcon(
                        Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(
                    "COURSE EN COURS",
                    style = drawTaxiType().labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Emerald600,
                    letterSpacing = 1.sp
                )
                androidx.compose.material3.Text(
                    text = "${ride.departure.ifBlank { "—" }} → ${ride.arrival.ifBlank { "—" }}",
                    style = drawTaxiType().bodyMedium,
                    color = drawTaxiColors().onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DrawTaxiIcon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Emerald600,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Widget prochaine course confirmée ──
@Composable
fun NextRideCard(
    ride: RideRequest,
    brandColor: Color,
    onClick: () -> Unit
) {
    val colors = drawTaxiColors()
    DrawTaxiSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.clickable { onClick() }) {
            // Accent gradient header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(listOf(brandColor, brandColor.copy(alpha = 0.5f)))
                    )
            )
            
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Badge
                Column(
                    modifier = Modifier
                        .size(64.dp)
                        .background(brandColor.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.Text(
                        text = ride.time.take(2),
                        style = drawTaxiType().headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = brandColor
                    )
                    androidx.compose.material3.Text(
                        text = ride.time.takeLast(2),
                        style = drawTaxiType().labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = brandColor.copy(alpha = 0.6f)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DrawTaxiIcon(Icons.Default.FlashOn, contentDescription = null, tint = Amber500, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        androidx.compose.material3.Text(
                            "PROCHAINE COURSE",
                            style = drawTaxiType().labelSmall,
                            color = brandColor,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    androidx.compose.material3.Text(
                        text = "${ride.departure.ifBlank { "—" }} → ${ride.arrival.ifBlank { "—" }}",
                        style = drawTaxiType().titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    androidx.compose.material3.Text(
                        text = ride.date.ifBlank { "Aujourd'hui" },
                        style = drawTaxiType().bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    if (ride.price > 0) {
                        androidx.compose.material3.Text(
                            text = String.format(Locale.getDefault(), "%.0f€", ride.price),
                            style = drawTaxiType().titleLarge,
                            fontWeight = FontWeight.Black,
                            color = colors.onSurface
                        )
                    }
                    DrawTaxiIcon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant.copy(alpha = 0.3f))
                }
            }
        }
    }
}

// ── FAB pulsant ──
@Composable
fun PulsingFAB(
    onClick: () -> Unit,
    brandColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "fabPulse"
    )

    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(80),
        label = "fabPress"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Halo pulsant en arrière
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(pulseScale)
                .background(brandColor.copy(alpha = 0.18f), CircleShape)
        )
        // Bouton FAB principal
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(pressScale)
                .shadow(8.dp, CircleShape, spotColor = brandColor.copy(alpha = 0.4f))
                .background(brandColor, CircleShape)
                .clip(CircleShape)
                .pointerInput(onClick) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            pressed = event.changes.any { it.pressed }
                            if (event.changes.any { it.pressed.not() && it.previousPressed }) {
                                onClick()
                                pressed = false
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            DrawTaxiIcon(
                Icons.Default.Add,
                contentDescription = "Nouvelle course",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ── Carte course confirmée ──
@Composable
private fun ConfirmedRideCard(
    ride: RideRequest,
    brandColor: Color,
    onClick: () -> Unit
) {
    val colors = drawTaxiColors()
    DrawTaxiSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(Emerald500)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(
                    text = ride.arrival.ifBlank { "Destination" },
                    style = drawTaxiType().titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DrawTaxiIcon(Icons.Default.Place, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    androidx.compose.material3.Text(
                        text = ride.departure.ifBlank { "Lieu de départ" },
                        style = drawTaxiType().bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DrawTaxiIcon(Icons.Default.Schedule, contentDescription = null, tint = brandColor, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    androidx.compose.material3.Text(
                        text = "${ride.date} • ${ride.time}",
                        style = drawTaxiType().bodySmall,
                        color = brandColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                androidx.compose.material3.Text(
                    text = String.format(Locale.getDefault(), "%.0f€", ride.price),
                    style = drawTaxiType().titleMedium,
                    fontWeight = FontWeight.Black,
                    color = colors.onSurface
                )
                DrawTaxiSurface(
                    shape = RoundedCornerShape(8.dp),
                    color = Emerald500.copy(alpha = 0.1f)
                ) {
                    androidx.compose.material3.Text(
                        "CONFIRMÉE",
                        style = drawTaxiType().labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Emerald600,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.material3.Text(text = value, style = drawTaxiType().titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        androidx.compose.material3.Text(text = label, style = drawTaxiType().labelSmall, color = Color.White.copy(alpha = 0.75f))
    }
}

@Composable
fun BatteryWarningBanner(onFixClick: () -> Unit) {
    val colors = drawTaxiColors()
    DrawTaxiCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = Rose500.copy(alpha = 0.12f),
        elevation = 0.dp
    ) {
        Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).background(Rose500.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                DrawTaxiIcon(Icons.Default.Warning, contentDescription = null, tint = Rose500, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(
                    text = "Économie batterie active",
                    style = drawTaxiType().bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Rose500
                )
                androidx.compose.material3.Text(
                    text = "La détection SMS peut être affectée",
                    style = drawTaxiType().bodySmall,
                    color = Rose500.copy(alpha = 0.8f)
                )
            }
            DrawTaxiSolidButton(
                onClick = onFixClick,
                containerColor = Rose500,
                shape = RoundedCornerShape(10.dp)
            ) {
                androidx.compose.material3.Text("Corriger", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun greetingForHour(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Bonjour 🌤"
        in 12..16 -> "Bon après-midi ☀️"
        in 17..21 -> "Bonne soirée 🌆"
        else -> "Bonne nuit 🌙"
    }
}

// ── PREVIEWS ──

@Preview(showBackground = true, name = "Home — Avec courses (Light)")
@Composable
fun HomeScreenPreview() {
    val sampleSettings = com.drawtaxi.app.data.AppSettings(name = "Jean Chauffeur", brandColor = Indigo500)
    val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    val sampleValidated = listOf(
        RideRequest(id = "1", sender = "0601020304", body = "Course 1", departure = "Gare de Lyon", arrival = "Orly", time = "10:00", date = today, price = 45.0, distanceKm = 18.5, isPending = false),
        RideRequest(id = "2", sender = "0601020304", body = "Course 2", departure = "Eiffel Tower", arrival = "Louvre", time = "14:30", date = today, price = 25.0, distanceKm = 5.2, isPending = false)
    )
    val sampleConfirmed = listOf(
        RideRequest(id = "3", sender = "0605060708", body = "Course 3", departure = "Hotel Ritz", arrival = "CDG Airport", time = "18:00", date = today, price = 60.0, distanceKm = 30.0, isPending = true, status = RideStatus.CONFIRMED)
    )
    DrawTaxiTheme(brandColor = sampleSettings.brandColor, darkTheme = false) {
        HomeScreen(validatedRides = sampleValidated, confirmedRides = sampleConfirmed, brandColor = sampleSettings.brandColor, onRideClick = {}, onCreateRide = {}, settings = sampleSettings, isIgnoringBatteryOptimization = true)
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Home — Dark Mode")
@Composable
fun HomeScreenDarkPreview() {
    val sampleSettings = com.drawtaxi.app.data.AppSettings(name = "Jean Chauffeur", brandColor = Indigo500)
    val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    val sampleValidated = listOf(
        RideRequest(id = "1", sender = "0601020304", body = "Course 1", departure = "Gare de Lyon", arrival = "Orly", time = "10:00", date = today, price = 45.0, distanceKm = 18.5, isPending = false)
    )
    val sampleConfirmed = listOf(
        RideRequest(id = "3", sender = "0605060708", body = "Course 3", departure = "Hotel Ritz", arrival = "CDG Airport", time = "18:00", date = today, price = 60.0, distanceKm = 30.0, isPending = true, status = RideStatus.CONFIRMED)
    )
    DrawTaxiTheme(brandColor = sampleSettings.brandColor, darkTheme = true) {
        HomeScreen(validatedRides = sampleValidated, confirmedRides = sampleConfirmed, brandColor = sampleSettings.brandColor, onRideClick = {}, onCreateRide = {}, settings = sampleSettings, isIgnoringBatteryOptimization = true)
    }
}

@Preview(showBackground = true, name = "Home — Empty State")
@Composable
fun HomeScreenEmptyPreview() {
    val sampleSettings = com.drawtaxi.app.data.AppSettings(name = "Jean Chauffeur", brandColor = Indigo500)
    DrawTaxiTheme(brandColor = sampleSettings.brandColor) {
        HomeScreen(validatedRides = emptyList(), confirmedRides = emptyList(), brandColor = sampleSettings.brandColor, onRideClick = {}, onCreateRide = {}, settings = sampleSettings, isIgnoringBatteryOptimization = true)
    }
}

@Preview(showBackground = true, name = "Home — EN COURS")
@Composable
fun HomeScreenInProgressPreview() {
    val sampleSettings = com.drawtaxi.app.data.AppSettings(name = "Jean Chauffeur", brandColor = Indigo500)
    val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    val inProgressRide = RideRequest(id = "99", sender = "0611223344", body = "Course en cours", departure = "Paris Centre", arrival = "Aéroport CDG", time = "20:00", date = today, price = 55.0, distanceKm = 28.0, isPending = true, status = RideStatus.IN_PROGRESS)
    DrawTaxiTheme(brandColor = sampleSettings.brandColor) {
        HomeScreen(validatedRides = emptyList(), confirmedRides = listOf(inProgressRide), brandColor = sampleSettings.brandColor, onRideClick = {}, onCreateRide = {}, settings = sampleSettings, isIgnoringBatteryOptimization = true)
    }
}

@Preview(showBackground = true, name = "Battery Warning")
@Composable
fun HomeScreenBatteryWarningPreview() {
    val sampleSettings = com.drawtaxi.app.data.AppSettings(name = "Jean Chauffeur", brandColor = Indigo500, monitorSms = true)
    DrawTaxiTheme(brandColor = sampleSettings.brandColor) {
        HomeScreen(validatedRides = emptyList(), confirmedRides = emptyList(), brandColor = sampleSettings.brandColor, onRideClick = {}, onCreateRide = {}, settings = sampleSettings, isIgnoringBatteryOptimization = false)
    }
}
