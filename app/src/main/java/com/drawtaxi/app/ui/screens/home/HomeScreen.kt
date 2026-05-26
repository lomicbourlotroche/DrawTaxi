package com.drawtaxi.app.ui.screens.home



import androidx.compose.foundation.background

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*
import com.drawtaxi.app.ui.components.core.*

import androidx.compose.runtime.Composable

import androidx.compose.runtime.remember

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp

import androidx.compose.ui.tooling.preview.Preview

import com.drawtaxi.app.data.RideRequest

import com.drawtaxi.app.util.PermissionHelper

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

    val actualIsIgnoring = isIgnoringBatteryOptimization ?: remember { PermissionHelper.isIgnoringBatteryOptimizations(context) }



    val now = Calendar.getInstance()

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val todayDateStr = dateFormat.format(now.time)



    // Uniquement les courses terminées

    val completedRides = validatedRides.filter { it.isPending == false }.sortedWith(

        compareByDescending<RideRequest> { it.date.ifBlank { "01/01/1970" } }

            .thenByDescending { it.time.ifBlank { "00:00" } }

    )



    // Dernières courses terminées (max 5)

    val recentCompletedRides = completedRides.take(5)



    val todayCompletedRides = completedRides.filter { ride ->

        val rideDate = ride.date.ifBlank { todayDateStr }

        rideDate == todayDateStr

    }



    val todayRevenue = todayCompletedRides.sumOf { it.price }

    val totalKmToday = todayCompletedRides.sumOf { it.distanceKm }

    val todayCoutDeplacement = todayCompletedRides.sumOf { it.fuelCost.takeIf { c -> c > 0 } ?: (it.distanceKm * 0.3 * 0.15) }
    val todayNet = todayRevenue - todayCoutDeplacement


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(bottom = 100.dp) // Espace pour la bottom bar
    ) {

        item {

            Box(

                modifier = Modifier

                    .fillMaxWidth()

                    .background(

                        Brush.linearGradient(

                            colors = listOf(brandColor, brandColor.copy(alpha = 0.85f))

                        )

                    )

                    .padding(horizontal = 20.dp, vertical = 24.dp)

            ) {

                Column {

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Column {

                            Text(

                                text = "Bonjour",

                                style = drawTaxiType().bodyLarge,

                                color = Color.White.copy(alpha = 0.8f)

                            )

                            Text(

                                text = settings.name.ifBlank { "Chauffeur" },

                                style = drawTaxiType().headlineMedium,

                                fontWeight = FontWeight.Bold,

                                color = Color.White

                            )

                        }

                        

                        DrawTaxiIconButton(
                            onClick = onCreateRide,
                            modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Nouvelle course", tint = Color.White, modifier = Modifier.size(24.dp))
                        }

                    }



                    Spacer(modifier = Modifier.height(20.dp))



                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                        HomeStatCard(value = String.format("%.2f €", todayRevenue), label = "Revenus", modifier = Modifier.weight(1f))

                        HomeStatCard(value = "${todayCompletedRides.size}", label = "Courses", modifier = Modifier.weight(1f))

                        HomeStatCard(value = String.format("%.0f km", totalKmToday), label = "Km", modifier = Modifier.weight(1f))

                    }



                    if (todayCompletedRides.isNotEmpty()) {

                        Spacer(modifier = Modifier.height(12.dp))

                        DrawTaxiSurface(

                            shape = RoundedCornerShape(12.dp),

                            color = Color.White.copy(alpha = 0.15f)

                        ) {

                            Row(

                                modifier = Modifier.fillMaxWidth().padding(12.dp),

                                horizontalArrangement = Arrangement.SpaceBetween

                            ) {

                                Column {

                                    Text("Bénéfice net", style = drawTaxiType().labelSmall, color = Color.White.copy(alpha = 0.8f))

                                    Text(String.format("%.2f €", todayNet), style = drawTaxiType().titleMedium, fontWeight = FontWeight.Bold, color = Color.White)

                                }

                                val profitPercent = if (todayRevenue > 0) (todayNet / todayRevenue) * 100.0 else 0.0

                                DrawTaxiSurface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.2f)) {

                                    Text(

                                        text = "${String.format("%.0f", profitPercent)}%",

                                        style = drawTaxiType().labelMedium,

                                        fontWeight = FontWeight.Bold,

                                        color = Color.White,

                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)

                                    )

                                }

                            }

                        }

                    }

                }

            }

        }



        if (!actualIsIgnoring && settings.monitorSms) {

            item {

                Spacer(modifier = Modifier.height(12.dp))

                BatteryWarningBanner(onFixClick = { PermissionHelper.requestIgnoreBatteryOptimization(context) })

            }

        }



        // Section Courses Confirmées

        if (confirmedRides.isNotEmpty()) {

            item {

                Spacer(modifier = Modifier.height(20.dp))

                Text(

                    text = "Courses confirmées",

                    style = drawTaxiType().titleLarge,

                    fontWeight = FontWeight.Bold,

                    color = Emerald500,

                    modifier = Modifier.padding(horizontal = 20.dp)

                )

            }



            items(confirmedRides) { ride ->

                ConfirmedRideCard(

                    ride = ride,

                    brandColor = brandColor,

                    onClick = { onRideClick(ride) }

                )

            }

        }



        // Section Dernières courses terminées

        if (recentCompletedRides.isNotEmpty()) {

            item {

                Spacer(modifier = Modifier.height(20.dp))

                Text(

                    text = "Dernières courses terminées",

                    style = drawTaxiType().titleLarge,

                    fontWeight = FontWeight.Bold,

                    color = Slate800,

                    modifier = Modifier.padding(horizontal = 20.dp)

                )

            }



            items(recentCompletedRides) { ride ->

                RideHistoryCard(

                    ride = ride,

                    brandColor = brandColor,

                    onClick = { onRideClick(ride) }

                )

            }

        }



        if (confirmedRides.isEmpty() && completedRides.isEmpty()) {

            item {

                Spacer(modifier = Modifier.height(40.dp))

                Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                    DrawTaxiSurface(shape = RoundedCornerShape(24.dp), color = Slate100, modifier = Modifier.size(80.dp)) {

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {

                            Icon(Icons.Default.LocalTaxi, contentDescription = null, tint = Slate400, modifier = Modifier.size(40.dp))

                        }

                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Aucune course", style = drawTaxiType().titleMedium, color = Slate500)

                    Text(text = "Créez une course ou attendez les SMS", style = drawTaxiType().bodyMedium, color = Slate400)

                    Spacer(modifier = Modifier.height(20.dp))

                    DrawTaxiSolidButton(
                        onClick = onCreateRide,
                        shape = RoundedCornerShape(14.dp),
                        containerColor = brandColor
                    ) {

                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))

                        Spacer(modifier = Modifier.width(6.dp))

                        Text("Nouvelle course", fontWeight = FontWeight.Bold)

                    }

                }

            }

        }



        item {

            Spacer(modifier = Modifier.height(100.dp))

        }

    }

}



@Composable

private fun ConfirmedRideCard(

    ride: RideRequest,

    brandColor: Color,

    onClick: () -> Unit

) {

    Card(

        modifier = Modifier

            .fillMaxWidth()

            .padding(horizontal = 16.dp, vertical = 6.dp),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(containerColor = Color.White),

        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),

        onClick = onClick

    ) {

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .padding(16.dp),

            verticalAlignment = Alignment.CenterVertically

        ) {

            Box(

                modifier = Modifier

                    .size(12.dp)

                    .clip(RoundedCornerShape(6.dp))

                    .background(Emerald500)

            )

            Spacer(modifier = Modifier.width(12.dp))



            Column(modifier = Modifier.weight(1f)) {

                Text(

                    text = ride.arrival.ifBlank { "Destination" },

                    style = drawTaxiType().titleMedium,

                    fontWeight = FontWeight.Bold,

                    maxLines = 1

                )

                if (ride.departure.isNotBlank()) {

                    Text(

                        text = "Depuis ${ride.departure}",

                        style = drawTaxiType().bodySmall,

                        color = Slate500,

                        maxLines = 1

                    )

                }

                Text(

                    text = "${ride.date} à ${ride.time}",

                    style = drawTaxiType().bodySmall,

                    color = brandColor

                )

            }



            if (ride.price > 0) {

                Text(

                    text = String.format("%.2f €", ride.price),

                    style = drawTaxiType().titleMedium,

                    fontWeight = FontWeight.Bold,

                    color = brandColor

                )

            }

        }

    }

}



@Composable

private fun HomeStatCard(value: String, label: String, modifier: Modifier = Modifier) {

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {

        Text(text = value, style = drawTaxiType().titleLarge, fontWeight = FontWeight.Bold, color = Color.White)

        Text(text = label, style = drawTaxiType().labelSmall, color = Color.White.copy(alpha = 0.75f))

    }

}



@Composable

fun BatteryWarningBanner(onFixClick: () -> Unit) {

    Card(

        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),

        shape = RoundedCornerShape(14.dp),

        colors = CardDefaults.cardColors(containerColor = Rose100)

    ) {

        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {

            Icon(Icons.Default.Warning, contentDescription = null, tint = Rose500, modifier = Modifier.size(22.dp))

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(text = "Économie de batterie actif", style = drawTaxiType().bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF9F1239))

                Text(text = "La détection SMS peut être affectée", style = drawTaxiType().bodySmall, color = Color(0xFF9F1239).copy(alpha = 0.7f))

            }

            DrawTaxiSolidButton(onClick = onFixClick) {

                Text("Corriger", color = Color(0xFF9F1239))

            }

        }

    }

}



@Preview(showBackground = true)

@Composable

fun HomeScreenPreview() {

    val sampleSettings = com.drawtaxi.app.data.AppSettings(

        name = "Jean Chauffeur",

        brandColor = Indigo500

    )

    

    val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

    

    val sampleValidatedRides = listOf(

        RideRequest(

            id = "1",

            sender = "0601020304",

            body = "Course 1",

            departure = "Gare de Lyon",

            arrival = "Orly",

            time = "10:00",

            date = today,

            price = 45.0,

            distanceKm = 18.5,

            isPending = false

        ),

        RideRequest(

            id = "2",

            sender = "0601020304",

            body = "Course 2",

            departure = "Eiffel Tower",

            arrival = "Louvre",

            time = "14:30",

            date = today,

            price = 25.0,

            distanceKm = 5.2,

            isPending = false

        )

    )

    

    val sampleConfirmedRides = listOf(

        RideRequest(

            id = "3",

            sender = "0605060708",

            body = "Course 3",

            departure = "Hotel Ritz",

            arrival = "CDG Airport",

            time = "18:00",

            date = today,

            price = 60.0,

            distanceKm = 30.0,

            isPending = true,

            status = com.drawtaxi.app.data.RideStatus.CONFIRMED

        )

    )



    DrawTaxiTheme(brandColor = sampleSettings.brandColor) {

        HomeScreen(

            validatedRides = sampleValidatedRides,

            confirmedRides = sampleConfirmedRides,

            brandColor = sampleSettings.brandColor,

            onRideClick = {},

            onCreateRide = {},

            settings = sampleSettings,

            isIgnoringBatteryOptimization = true

        )

    }

}



@Preview(showBackground = true, name = "Empty State")

@Composable

fun HomeScreenEmptyPreview() {

    val sampleSettings = com.drawtaxi.app.data.AppSettings(

        name = "Jean Chauffeur",

        brandColor = Indigo500

    )

    

    DrawTaxiTheme(brandColor = sampleSettings.brandColor) {

        HomeScreen(

            validatedRides = emptyList(),

            confirmedRides = emptyList(),

            brandColor = sampleSettings.brandColor,

            onRideClick = {},

            onCreateRide = {},

            settings = sampleSettings,

            isIgnoringBatteryOptimization = true

        )

    }

}



@Preview(showBackground = true, name = "Battery Warning")

@Composable

fun HomeScreenBatteryWarningPreview() {

    val sampleSettings = com.drawtaxi.app.data.AppSettings(

        name = "Jean Chauffeur",

        brandColor = Indigo500,

        monitorSms = true

    )

    

    DrawTaxiTheme(brandColor = sampleSettings.brandColor) {

        HomeScreen(

            validatedRides = emptyList(),

            confirmedRides = emptyList(),

            brandColor = sampleSettings.brandColor,

            onRideClick = {},

            onCreateRide = {},

            settings = sampleSettings,

            isIgnoringBatteryOptimization = false

        )

    }

}



