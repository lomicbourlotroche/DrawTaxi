package com.drawtaxi.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
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
        NavigationItem("dashboard", "Dashboard", Icons.Filled.BarChart, Icons.Outlined.BarChart),
        NavigationItem("settings", "Param\u00e8tres", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(drawTaxiColors().surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = activeTab == item.id
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) brandColor else drawTaxiColors().onSurfaceVariant,
                    animationSpec = tween(200), label = "iconColor"
                )
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .then(
                            if (isSelected) Modifier.background(brandColor.copy(alpha = 0.1f))
                            else Modifier
                        )
                        .clickable { onTabSelected(item.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        DrawTaxiIcon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp),
                            tint = iconColor
                        )
                        if (item.id == "message" && hasPending) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-4).dp)
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Red500)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    androidx.compose.material3.Text(
                        text = item.label,
                        style = drawTaxiType().labelSmall,
                        color = iconColor
                    )
                }
            }
        }
    }
}

@Deprecated("Use BottomNavigationBar instead")
@Composable
fun BottomNavigation(
    activeTab: String, onTabSelected: (String) -> Unit, brandColor: Color, hasPending: Boolean
) {
    BottomNavigationBar(activeTab, onTabSelected, brandColor, hasPending)
}

private fun Modifier.clickable(onClick: () -> Unit): Modifier = this.then(
    pointerInput(onClick) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.any { it.pressed.not() && it.previousPressed }) onClick()
            }
        }
    }
)

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
