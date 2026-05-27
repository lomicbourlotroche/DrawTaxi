package com.drawtaxi.app.ui.screens.rides

import java.util.Locale

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.drawtaxi.app.ui.components.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.RideStatus
import com.drawtaxi.app.ui.components.*
import com.drawtaxi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    ride: RideRequest,
    onBack: () -> Unit,
    onDelete: (RideRequest) -> Unit,
    onEdit: (RideRequest) -> Unit = {},
    onStartRide: (RideRequest) -> Unit = {},
    isPending: Boolean = false,
    settings: AppSettings,
    onShareLocation: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val brandColor = settings.brandColor

    // Calcul de rentabilité basé sur le prix réel de la course
    val actualPrice = ride.price.takeIf { it > 0 } ?: run {
        val priceBreakdown = com.drawtaxi.app.logic.pricing.PriceEngine.calculate(
            distanceKm = ride.distanceKm,
            dateTime = java.util.Calendar.getInstance(),
            pricePerKm = settings.pricePerKm.toDoubleOrNull() ?: 2.50,
            baseFare = settings.basePrice.toDoubleOrNull() ?: 9.00,
            minDistanceKm = settings.minDistanceKm.toDoubleOrNull() ?: 3.6,
            nightSurchargePercent = settings.nightSurchargePercent,
            sundaySurchargePercent = settings.sundaySurchargePercent,
            holidaySurchargePercent = settings.holidaySurchargePercent,
            nightStartHour = settings.nightStartHour,
            nightEndHour = settings.nightEndHour,
            tvaTransportRate = settings.tvaTransportRate
        )
        priceBreakdown.totalTTC
    }

    val distanceDomicileKm = if (settings.coutParKmDeplacement > 0 && ride.fuelCost > 0) {
        (ride.fuelCost / settings.coutParKmDeplacement).takeIf { it.isFinite() } ?: (ride.distanceKm * 0.3)
    } else {
        ride.distanceKm * 0.3
    }
    val coutDeplacement = RideRequest.calculateCoutDeplacement(distanceDomicileKm, settings.coutParKmDeplacement)
    
    // For completed rides, sum actual fuelCost and operatingCost. Fallback to estimated empty running cost otherwise.
    val totalCost = if (ride.status == RideStatus.COMPLETED && (ride.fuelCost > 0 || ride.operatingCost > 0)) {
        ride.fuelCost + ride.operatingCost
    } else {
        coutDeplacement.takeIf { it.isFinite() } ?: 0.0
    }
    val netProfit = actualPrice - totalCost
    val profitability = if (actualPrice > 0.001) {
        ((netProfit / actualPrice) * 100).takeIf { it.isFinite() } ?: 0.0
    } else 0.0

    DrawTaxiScaffold(
        topBar = {
            DrawTaxiTopBar(
                title = {
                    DrawTaxiTopBarTitle(
                        text = "Course",
                        subtitle = ride.date.ifBlank { null }
                    )
                },
                navigationIcon = {
                    DrawTaxiIconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = drawTaxiColors().onSurface
                        )
                    }
                },
                actions = {
                    if (isPending) {
                        Surface(
                            color = Green500.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "En attente",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = drawTaxiType().labelSmall,
                                color = Green500,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                backgroundColor = drawTaxiColors().surface
            )
        },
        bottomBar = {
            DrawTaxiSurface(
                modifier = Modifier.fillMaxWidth(),
                color = drawTaxiColors().surface,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawTaxiIconButton(
                        onClick = { onDelete(ride); onBack() },
                        modifier = Modifier.background(Red500.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Red500)
                    }

                    DrawTaxiIconButton(
                        onClick = { onEdit(ride) },
                        modifier = Modifier.background(brandColor.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = brandColor)
                    }

                    if (isPending || ride.status == com.drawtaxi.app.data.RideStatus.CONFIRMED || 
                        ride.status == com.drawtaxi.app.data.RideStatus.IN_PROGRESS) {
                        DrawTaxiSolidButton(
                            onClick = { onStartRide(ride) },
                            modifier = Modifier.weight(1f),
                            containerColor = if (ride.status == com.drawtaxi.app.data.RideStatus.IN_PROGRESS) brandColor else Green500
                        ) {
                            Icon(
                                if (ride.status == com.drawtaxi.app.data.RideStatus.IN_PROGRESS) Icons.Default.Check else Icons.Default.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (ride.status == com.drawtaxi.app.data.RideStatus.IN_PROGRESS) "Terminer" else "Commencer",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(drawTaxiColors().background)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isPreview = LocalInspectionMode.current
            
            // Une seule section de carte qui s'adapte au statut
            if (isPreview) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Slate100, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aperçu de la carte", color = Slate400, style = drawTaxiType().bodyMedium)
                }
            } else {
                if (isPending && ride.departure.isNotBlank()) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        RouteToClientMap(pickupAddress = ride.departure, brandColor = brandColor)
                    }
                } else {
                    RideDetailMapSection(departure = ride.departure, arrival = ride.arrival, brandColor = brandColor)
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RideDetailClientCard(
                    ride = ride,
                    brandColor = brandColor,
                    onCall = {
                        val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${ride.sender}") }
                        context.startActivity(intent)
                    },
                    onMessage = { message ->
                        com.drawtaxi.app.logic.sms.SmsUtils.sendSms(context, ride.sender, message)
                    },
                    onShareLocation = onShareLocation,
                    messageTemplates = settings.messageTemplates
                )

                RideDetailTripCard(ride = ride, brandColor = brandColor)

                RideDetailPriceCard(ride = ride, settings = settings, brandColor = brandColor)

                RideDetailProfitabilityCard(
                    ride = ride,
                    profitability = profitability,
                    netProfit = netProfit,
                    fuelCost = if (ride.fuelCost > 0 || ride.operatingCost > 0) ride.fuelCost else coutDeplacement,
                    operatingCost = ride.operatingCost,
                    hasRealCosts = ride.fuelCost > 0 || ride.operatingCost > 0,
                    totalPrice = actualPrice,
                    brandColor = brandColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun RideDetailProfitabilityCard(
    ride: RideRequest,
    profitability: Double,
    netProfit: Double,
    fuelCost: Double,
    operatingCost: Double,
    hasRealCosts: Boolean,
    totalPrice: Double,
    brandColor: Color
) {
    val profitColor = when {
        profitability >= 50 -> Green500
        profitability >= 30 -> Amber500
        profitability >= 15 -> Color(0xFFFF6B00)
        else -> Red500
    }
    val statusLabel = when {
        profitability >= 50 -> "Excellente"
        profitability >= 30 -> "Correcte"
        profitability >= 15 -> "Faible"
        else -> "Non rentable"
    }
    val totalCostDisplay = fuelCost + operatingCost

    DrawTaxiSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = drawTaxiColors().surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = brandColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = brandColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Rentabilité",
                            style = drawTaxiType().titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = drawTaxiColors().onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                statusLabel,
                                style = drawTaxiType().bodySmall,
                                color = profitColor,
                                fontWeight = FontWeight.Medium
                            )
                            if (!hasRealCosts) {
                                Text(
                                    "• estimée",
                                    style = drawTaxiType().labelSmall,
                                    color = drawTaxiColors().onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                // Big % badge
                Surface(
                    color = profitColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.0f", profitability)}%",
                        style = drawTaxiType().headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = profitColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(profitColor.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((profitability / 100.0).coerceIn(0.0, 1.0).toFloat())
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(profitColor)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cost breakdown block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(drawTaxiColors().surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Revenue row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Prix TTC",
                        style = drawTaxiType().bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = drawTaxiColors().onSurface
                    )
                    Text(
                        String.format(Locale.getDefault(), "+%.2f €", totalPrice),
                        style = drawTaxiType().bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Green500
                    )
                }

                DrawTaxiDivider(color = drawTaxiColors().outline.copy(alpha = 0.4f))

                Text(
                    if (hasRealCosts) "COÛTS RÉELS" else "COÛTS ESTIMÉS",
                    style = drawTaxiType().labelSmall,
                    color = drawTaxiColors().onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Coût kilométrique",
                        style = drawTaxiType().bodySmall,
                        color = drawTaxiColors().onSurfaceVariant
                    )
                    Text(
                        String.format(Locale.getDefault(), "-%.2f €", fuelCost),
                        style = drawTaxiType().bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = drawTaxiColors().onSurface
                    )
                }

                if (operatingCost > 0 || hasRealCosts) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Charges exploitation",
                            style = drawTaxiType().bodySmall,
                            color = drawTaxiColors().onSurfaceVariant
                        )
                        Text(
                            String.format(Locale.getDefault(), "-%.2f €", operatingCost),
                            style = drawTaxiType().bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = drawTaxiColors().onSurface
                        )
                    }
                }

                DrawTaxiDivider(color = drawTaxiColors().outline.copy(alpha = 0.3f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Total coûts",
                        style = drawTaxiType().bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = drawTaxiColors().onSurface
                    )
                    Text(
                        String.format(Locale.getDefault(), "-%.2f €", totalCostDisplay),
                        style = drawTaxiType().bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Red500
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Net profit
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = (if (netProfit > 0) Green500 else Red500).copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (netProfit > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (netProfit > 0) Green500 else Red500,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Bénéfice net",
                            style = drawTaxiType().titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = drawTaxiColors().onSurface
                        )
                    }
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f €", netProfit),
                        style = drawTaxiType().titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (netProfit > 0) Green500 else Red500
                    )
                }
            }

            // Revenue per hour (if duration known)
            if (ride.durationMinutes > 0 && ride.price > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = drawTaxiColors().onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Text("Revenu / heure", style = drawTaxiType().bodySmall, color = drawTaxiColors().onSurfaceVariant)
                    }
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f €/h", (ride.price / ride.durationMinutes) * 60),
                        style = drawTaxiType().bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = brandColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfitabilityStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Slate50,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = value, style = drawTaxiType().bodyMedium, fontWeight = FontWeight.Bold, color = Slate800)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, style = drawTaxiType().labelSmall, color = Slate500, fontSize = 10.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailClientCard(
    ride: RideRequest,
    brandColor: Color,
    onCall: () -> Unit,
    onMessage: (String) -> Unit,
    onShareLocation: (() -> Unit)? = null,
    messageTemplates: List<String> = emptyList()
) {
    var showTemplateSheet by remember { mutableStateOf(false) }
    
    DrawTaxiSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = drawTaxiColors().surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = brandColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (ride.clientName.ifBlank { ride.sender }).take(2).uppercase(),
                                fontWeight = FontWeight.Black,
                                color = brandColor,
                                style = drawTaxiType().titleLarge
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = ride.clientName.ifBlank { "Client" },
                            style = drawTaxiType().titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = ride.sender,
                            style = drawTaxiType().bodyMedium,
                            color = Slate500
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DrawTaxiSolidButton(
                    onClick = onCall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = brandColor
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Appeler", fontWeight = FontWeight.Bold)
                }
                DrawTaxiOutlinedButton(
                    onClick = { showTemplateSheet = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    contentColor = brandColor,
                    borderColor = brandColor.copy(alpha = 0.2f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Message", fontWeight = FontWeight.Bold)
                }
                if (onShareLocation != null) {
                    DrawTaxiIconButton(
                        onClick = onShareLocation,
                        modifier = Modifier.background(brandColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = brandColor)
                    }
                }
            }
        }
    }
    
    if (showTemplateSheet) {
        ModalBottomSheet(onDismissRequest = { showTemplateSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "Envoyer un message", style = drawTaxiType().titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                DrawTaxiOutlinedButton(onClick = { onMessage(""); showTemplateSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Écrire un message...")
                }
                
                if (messageTemplates.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Templates", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    messageTemplates.forEach { template ->
                        Card(
                            onClick = { onMessage(template); showTemplateSheet = false },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = drawTaxiColors().onSurfaceVariant, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = template, style = drawTaxiType().bodyMedium)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun RideDetailTripCard(ride: RideRequest, brandColor: Color) {
    DrawTaxiSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = drawTaxiColors().surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = brandColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = brandColor, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = ride.time.ifBlank { "—" }, style = drawTaxiType().headlineSmall, fontWeight = FontWeight.Bold, color = brandColor)
                }
                if (ride.date.isNotBlank()) {
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = ride.date,
                            style = drawTaxiType().labelSmall,
                            color = Slate600,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp).fillMaxHeight()) {
                    Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = Green500) {}
                    Box(modifier = Modifier.width(2.dp).weight(1f).background(Slate100))
                    Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = brandColor) {}
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    Column {
                        Text(text = "DÉPART", style = drawTaxiType().labelSmall, color = Slate400, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(text = ride.departure.ifBlank { "—" }, style = drawTaxiType().bodyLarge, fontWeight = FontWeight.SemiBold, color = Slate900)
                    }
                    Column {
                        Text(text = "ARRIVÉE", style = drawTaxiType().labelSmall, color = Slate400, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(text = ride.arrival.ifBlank { "—" }, style = drawTaxiType().bodyLarge, fontWeight = FontWeight.SemiBold, color = brandColor)
                    }
                }
            }

            if (ride.distanceKm > 0) {
                Spacer(modifier = Modifier.height(24.dp))
                DrawTaxiDivider(color = drawTaxiColors().outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Route, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${String.format(Locale.getDefault(), "%.1f", ride.distanceKm)} km", style = drawTaxiType().bodyMedium, color = Slate700, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "~${((ride.distanceKm * 2).toInt())} min", style = drawTaxiType().bodyMedium, color = Slate700, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun RideDetailPriceCard(ride: RideRequest, settings: AppSettings, brandColor: Color) {
    val basePrice = settings.basePrice.toDoubleOrNull() ?: 2.60
    val perKm = settings.pricePerKm.toDoubleOrNull() ?: 1.20
    val calculatedPrice = basePrice + (ride.distanceKm * perKm)
    val displayedPrice = if (ride.price > 0) ride.price else calculatedPrice

    DrawTaxiSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = drawTaxiColors().surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = brandColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = brandColor, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Prix", style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = if (displayedPrice > 0) String.format(Locale.getDefault(), "%.2f €", displayedPrice) else "—",
                    style = drawTaxiType().headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = if (displayedPrice > 0) brandColor else Slate400
                )
            }

            if (ride.price == 0.0 && ride.distanceKm > 0) {
                Spacer(modifier = Modifier.height(20.dp))
                DrawTaxiDivider(color = drawTaxiColors().outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                PriceRow("Prise en charge", String.format(Locale.getDefault(), "%.2f €", basePrice))
                PriceRow("${String.format(Locale.getDefault(), "%.1f", ride.distanceKm)} km × ${String.format(Locale.getDefault(), "%.2f", perKm)} €/km", String.format(Locale.getDefault(), "%.2f €", ride.distanceKm * perKm))
                
                Spacer(modifier = Modifier.height(12.dp))
                DrawTaxiDivider(color = drawTaxiColors().outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "TOTAL ESTIMÉ", style = drawTaxiType().labelSmall, fontWeight = FontWeight.Bold, color = Slate500)
                    Text(text = String.format(Locale.getDefault(), "%.2f €", displayedPrice), style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold, color = brandColor)
                }
            }

            if (ride.price > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Green500.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Prix confirmé",
                        style = drawTaxiType().labelSmall,
                        color = Green500,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = drawTaxiType().bodyMedium, color = drawTaxiColors().onSurfaceVariant)
        Text(text = value, style = drawTaxiType().bodyMedium, color = drawTaxiColors().onSurface)
    }
}

@Preview(showBackground = true)
@Composable
fun RideDetailScreenPreview() {
    val sampleRide = RideRequest(
        id = "1",
        sender = "0612345678",
        body = "Taxi depuis Paris vers Lyon à 14h30",
        departure = "Paris",
        arrival = "Lyon",
        time = "14:30",
        price = 45.0,
        distanceKm = 18.5
    )
    DrawTaxiTheme(brandColor = Indigo500) {
        RideDetailScreen(
            ride = sampleRide,
            onBack = {},
            onDelete = {},
            settings = AppSettings(brandColor = Indigo500),
            onShareLocation = {},
            isPending = true
        )
    }
}


