package com.drawtaxi.app.ui.components.core

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.random.Random

data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val speedY: Float,
    val speedX: Float,
    val size: Size,
    val color: Color,
    val rotation: Float,
    val rotationSpeed: Float,
    val type: ParticleType
)

enum class ParticleType {
    RECTANGLE, CIRCLE, TRIANGLE
}

@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    durationMs: Long = 3000,
    onFinished: () -> Unit = {}
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidth = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { config.screenHeightDp.dp.toPx() }

    val colors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFFF43F5E), // Rose
        Color(0xFF3B82F6), // Blue
        Color(0xFF8B5CF6)  // Violet
    )

    // Generate initial particles
    val particles = remember {
        mutableStateListOf<ConfettiParticle>().apply {
            repeat(85) {
                add(
                    ConfettiParticle(
                        x = Random.nextFloat() * screenWidth,
                        y = -Random.nextFloat() * 400f - 20f,
                        speedY = Random.nextFloat() * 400f + 250f,
                        speedX = (Random.nextFloat() - 0.5f) * 150f,
                        size = Size(Random.nextFloat() * 12f + 8f, Random.nextFloat() * 20f + 10f),
                        color = colors[Random.nextInt(colors.size)],
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                        type = ParticleType.entries[Random.nextInt(ParticleType.entries.size)]
                    )
                )
            }
        }
    }

    var elapsed by remember { mutableStateOf(0L) }
    val infiniteTransition = rememberInfiniteTransition(label = "confettiTransition")
    
    // Smooth tick rate using frame state
    val timeState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    var lastTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < durationMs) {
            val currTime = System.currentTimeMillis()
            val dt = (currTime - lastTime) / 1000f
            lastTime = currTime
            
            // Update particles
            for (i in particles.indices) {
                val p = particles[i]
                var nextY = p.y + p.speedY * dt
                var nextX = p.x + p.speedX * dt
                
                // Wind effect
                val wind = Math.sin(currTime / 500.0).toFloat() * 20f * dt
                nextX += wind
                
                // If it goes below screen, reset it to top if we're still in early phases, or let it fall off
                if (nextY > screenHeight) {
                    if (System.currentTimeMillis() - startTime < durationMs - 1000) {
                        nextY = -Random.nextFloat() * 100f - 10f
                        nextX = Random.nextFloat() * screenWidth
                    }
                }
                
                particles[i] = p.copy(
                    x = nextX,
                    y = nextY,
                    rotation = p.rotation + p.rotationSpeed * dt
                )
            }
            kotlinx.coroutines.delay(16)
        }
        onFinished()
    }

    // Force recomposition on animation state change
    val animTime = timeState.value

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            if (p.y in -50f..screenHeight + 50f && p.x in -50f..screenWidth + 50f) {
                withTransform({
                    translate(p.x, p.y)
                    rotate(p.rotation)
                }) {
                    when (p.type) {
                        ParticleType.RECTANGLE -> {
                            drawRect(
                                color = p.color,
                                size = p.size
                            )
                        }
                        ParticleType.CIRCLE -> {
                            drawCircle(
                                color = p.color,
                                radius = p.size.width / 2
                            )
                        }
                        ParticleType.TRIANGLE -> {
                            val path = Path().apply {
                                moveTo(0f, -p.size.height / 2)
                                lineTo(p.size.width / 2, p.size.height / 2)
                                lineTo(-p.size.width / 2, p.size.height / 2)
                                close()
                            }
                            drawPath(
                                path = path,
                                color = p.color
                            )
                        }
                    }
                }
            }
        }
    }
}
