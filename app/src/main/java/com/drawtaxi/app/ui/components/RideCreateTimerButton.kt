package com.drawtaxi.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.drawtaxi.app.ui.theme.DrawTaxiTheme
import com.drawtaxi.app.ui.theme.drawTaxiType

@Composable
fun RideCreateTimerButton(
    timeLeft: Int,
    isTimerRunning: Boolean,
    onConfirm: () -> Unit,
    brandColor: Color,
    modifier: Modifier = Modifier,
    totalTime: Int = 30
) {
    val progress by animateFloatAsState(
        targetValue = if (isTimerRunning && timeLeft > 0) timeLeft.toFloat() / totalTime else 1f,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "ButtonProgress"
    )

    Button(
        onClick = onConfirm,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = brandColor.copy(alpha = 0.5f)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = brandColor,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isTimerRunning && timeLeft > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .align(Alignment.CenterStart)
                        .background(Color.White.copy(alpha = 0.15f))
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isTimerRunning && timeLeft > 0) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        trackColor = Color.White.copy(0.3f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Ajout auto (${timeLeft}s)",
                        style = drawTaxiType().titleMedium.copy(color = Color.White),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                            val path = Path().apply {
                                moveTo(size.width * 0.2f, size.height * 0.5f)
                                lineTo(size.width * 0.45f, size.height * 0.75f)
                                lineTo(size.width * 0.8f, size.height * 0.25f)
                            }
                            drawPath(
                                path = path,
                                color = Color.White,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Confirmer la course",
                        style = drawTaxiType().titleMedium.copy(color = Color.White),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun RideCreateTimerButtonRunningPreview() {
    DrawTaxiTheme(brandColor = Color.Blue) {
        Box(modifier = Modifier.padding(16.dp).background(Color.White)) {
            RideCreateTimerButton(
                timeLeft = 7,
                isTimerRunning = true,
                onConfirm = {},
                brandColor = Color.Blue,
                totalTime = 10
            )
        }
    }
}

@Preview
@Composable
fun RideCreateTimerButtonStoppedPreview() {
    DrawTaxiTheme(brandColor = Color.Blue) {
        Box(modifier = Modifier.padding(16.dp).background(Color.White)) {
            RideCreateTimerButton(
                timeLeft = 0,
                isTimerRunning = false,
                onConfirm = {},
                brandColor = Color.Blue
            )
        }
    }
}
