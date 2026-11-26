package com.amanx.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amanx.app.domain.model.AppLanguage
import com.amanx.app.ui.theme.AmanXColors
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Luxury cinematic opening sequence:
 * Deep black canvas -> flowing Sudanese flag -> golden & tech-blue energy merge -> AMAN X shield emblem.
 * Respects reduceMotion / power saver settings. Flag appears only in this opening sequence.
 */
@Composable
fun CinematicIntroScreen(
    language: AppLanguage,
    reduceMotion: Boolean,
    onIntroFinished: () -> Unit
) {
    var phase by remember { mutableStateOf(0) }

    val progress by animateFloatAsState(
        targetValue = when (phase) {
            0 -> 0.1f
            1 -> 0.55f
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = if (reduceMotion) 300 else 1200,
            easing = FastOutSlowInEasing
        ),
        label = "intro_progress"
    )

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            phase = 2
            delay(850)
            onIntroFinished()
        } else {
            delay(250)
            phase = 1
            delay(1300)
            phase = 2
            delay(1300)
            onIntroFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmanXColors.LuxuryBlack),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(300.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // Tech blue & royal gold ambient energy halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AmanXColors.TechBlue.copy(alpha = 0.28f * progress),
                        AmanXColors.RoyalGold.copy(alpha = 0.16f * progress),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = w * 0.48f
                ),
                radius = w * 0.48f,
                center = Offset(cx, cy)
            )

            // Phase 1: Flowing Sudanese flag ribbon merging inward into the AMAN X shield
            val flagAlpha = (1f - ((progress - 0.5f).coerceAtLeast(0f) * 1.4f)).coerceIn(0.18f, 0.95f)
            val flagW = w * 0.62f
            val flagH = h * 0.36f
            val left = cx - flagW / 2f
            val top = cy - flagH / 2f - (1f - progress) * 32f
            val stripeH = flagH / 3f
            val waveOffset = if (reduceMotion) 0f else sin(progress * 6.28f) * 8f

            // Red, White, Black stripes
            drawRect(
                color = AmanXColors.SudanRed.copy(alpha = flagAlpha),
                topLeft = Offset(left, top + waveOffset * 0.3f),
                size = Size(flagW, stripeH)
            )
            drawRect(
                color = AmanXColors.SudanWhite.copy(alpha = flagAlpha),
                topLeft = Offset(left, top + stripeH + waveOffset * 0.2f),
                size = Size(flagW, stripeH)
            )
            drawRect(
                color = AmanXColors.SudanBlack.copy(alpha = flagAlpha),
                topLeft = Offset(left, top + stripeH * 2 + waveOffset * 0.1f),
                size = Size(flagW, stripeH)
            )

            // Green hoist triangle
            val triPath = Path().apply {
                moveTo(left, top)
                lineTo(left + flagW * 0.36f, top + flagH / 2f)
                lineTo(left, top + flagH)
                close()
            }
            drawPath(path = triPath, color = AmanXColors.SudanGreen.copy(alpha = flagAlpha))

            // Phase 2: Golden & Blue Shield Emblem forming cleanly
            val shieldAlpha = ((progress - 0.25f) / 0.75f).coerceIn(0f, 1f)
            val shieldPath = Path().apply {
                moveTo(cx, cy - h * 0.30f)
                lineTo(cx + w * 0.24f, cy - h * 0.19f)
                lineTo(cx + w * 0.24f, cy + h * 0.04f)
                cubicTo(cx + w * 0.24f, cy + h * 0.20f, cx + w * 0.10f, cy + h * 0.29f, cx, cy + h * 0.34f)
                cubicTo(cx - w * 0.10f, cy + h * 0.29f, cx - w * 0.24f, cy + h * 0.20f, cx - w * 0.24f, cy + h * 0.04f)
                lineTo(cx - w * 0.24f, cy - h * 0.19f)
                close()
            }

            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        AmanXColors.RoyalGold.copy(alpha = shieldAlpha),
                        AmanXColors.TechBlue.copy(alpha = shieldAlpha)
                    )
                ),
                style = Stroke(width = 7f, cap = StrokeCap.Round)
            )

            // Inner "X" security vectors
            val xSize = w * 0.095f
            drawLine(
                color = AmanXColors.SoftGold.copy(alpha = shieldAlpha),
                start = Offset(cx - xSize, cy - xSize),
                end = Offset(cx + xSize, cy + xSize),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = AmanXColors.CyanGlow.copy(alpha = shieldAlpha),
                start = Offset(cx + xSize, cy - xSize),
                end = Offset(cx - xSize, cy + xSize),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "AMAN X",
                color = AmanXColors.SoftGold,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
            Text(
                text = if (language == AppLanguage.AR) "حمايتك الرقمية — بهوية سودانية متفردة" else "Your Digital Safety, Reimagined",
                color = AmanXColors.PureWhite.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onIntroFinished) {
                Text(
                    text = if (language == AppLanguage.AR) "تخطي" else "Skip",
                    color = AmanXColors.CyanGlow
                )
            }
        }
    }
}
