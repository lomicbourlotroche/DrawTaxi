package com.drawtaxi.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.ui.theme.*

data class NavigationItem(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun BottomNavigationBar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    brandColor: Color,
    hasPending: Boolean = false,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavigationItem("home", "Accueil", Icons.Filled.Home, Icons.Outlined.Home),
        NavigationItem("message", "Messages", Icons.Filled.Email, Icons.Outlined.Email),
        NavigationItem("dashboard", "Dashboard", Icons.Filled.BarChart, Icons.Outlined.BarChart),
        NavigationItem("settings", "Paramètres", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = activeTab == item.id
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) brandColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "iconColor"
                )

                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        onClick = { onTabSelected(item.id) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp)),
                        color = if (isSelected) brandColor.copy(alpha = 0.1f) else Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp),
                                    tint = iconColor
                                )
                                
                                if (item.id == "message" && hasPending) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-4).dp)
                                            .size(8.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        color = Red500
                                    ) {}
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = iconColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Deprecated("Use BottomNavigationBar instead")
@Composable
fun BottomNavigation(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    brandColor: Color,
    hasPending: Boolean
) {
    BottomNavigationBar(activeTab, onTabSelected, brandColor, hasPending)
}
