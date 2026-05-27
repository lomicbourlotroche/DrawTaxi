package com.drawtaxi.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.drawtaxi.app.ui.theme.*

@Composable
fun RideDetailMapSection(
    departure: String,
    arrival: String,
    brandColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = Slate100,
        shadowElevation = 2.dp
    ) {
        Box {
            RideMap(
                departure = departure,
                arrival = arrival,
                modifier = Modifier.fillMaxSize()
            )
            
            // Glassmorphism-like overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.9f),
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Green500, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "DÉPART",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            departure,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate900,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Slate300,
                        modifier = Modifier.padding(horizontal = 8.dp).size(20.dp)
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(brandColor, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ARRIVÉE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            arrival,
                            style = MaterialTheme.typography.bodySmall,
                            color = brandColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Surface(
                onClick = {
                    val address = arrival.ifBlank { departure }
                    if (address.isNotBlank()) {
                        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        context.startActivity(mapIntent)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = brandColor,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = "GPS",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RideDetailMapSectionPreview() {
    DrawTaxiTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RideDetailMapSection(
                departure = "Paris, Gare du Nord",
                arrival = "Lyon, Part-Dieu",
                brandColor = Indigo500
            )
        }
    }
}
