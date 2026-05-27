package com.drawtaxi.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drawtaxi.app.ui.components.core.DrawTaxiIcon
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
        NavigationItem("dashboard", "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
        NavigationItem("settings", "Réglages", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = brandColor.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(drawTaxiColors().surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                NavItem(
                    item = item,
                    isSelected = activeTab == item.id,
                    brandColor = brandColor,
                    hasBadge = item.id == "message" && hasPending,
                    onClick = { onTabSelected(item.id) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    item: NavigationItem,
    isSelected: Boolean,
    brandColor: Color,
    hasBadge: Boolean,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) brandColor else drawTaxiColors().onSurfaceVariant.copy(alpha = 0.6f),
        animationSpec = tween(250),
        label = "navIconColor"
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(250),
        label = "pillAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "navScale"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                Modifier.pointerInput(onClick) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed.not() && it.previousPressed }) onClick()
                        }
                    }
                }
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(brandColor.copy(alpha = 0.12f * pillAlpha))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box {
                DrawTaxiIcon(
                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    modifier = Modifier.size(22.dp),
                    tint = iconColor
                )
                if (hasBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 5.dp, y = (-3).dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Rose500)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = item.label,
            style = drawTaxiType().labelSmall,
            color = iconColor
        )
    }
}

@Deprecated("Use BottomNavigationBar instead")
@Composable
fun BottomNavigation(
    activeTab: String, onTabSelected: (String) -> Unit, brandColor: Color, hasPending: Boolean
) {
    BottomNavigationBar(activeTab, onTabSelected, brandColor, hasPending)
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationBarPreview() {
    DrawTaxiTheme {
        BottomNavigationBar(
            activeTab = "home",
            onTabSelected = {},
            brandColor = Color(0xFF6366F1),
            hasPending = true
        )
    }
}

@Preview(showBackground = true, name = "NavItem Selected")
@Composable
fun NavItemSelectedPreview() {
    DrawTaxiTheme {
        NavItem(
            item = NavigationItem("home", "Accueil", Icons.Filled.Home, Icons.Outlined.Home),
            isSelected = true,
            brandColor = Indigo500,
            hasBadge = false,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, name = "NavItem Unselected with Badge")
@Composable
fun NavItemUnselectedBadgePreview() {
    DrawTaxiTheme {
        NavItem(
            item = NavigationItem("message", "Messages", Icons.Filled.Email, Icons.Outlined.Email),
            isSelected = false,
            brandColor = Indigo500,
            hasBadge = true,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationPreview() {
    DrawTaxiTheme {
        BottomNavigation(
            activeTab = "home",
            onTabSelected = {},
            brandColor = Color(0xFF6366F1),
            hasPending = true
        )
    }
}
