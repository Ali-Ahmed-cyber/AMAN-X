package com.amanx.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amanx.app.domain.model.AppLanguage
import com.amanx.app.domain.model.RiskLevel
import com.amanx.app.domain.model.ThemeMode

object AmanXColors {
    val LuxuryBlack = Color(0xFF07090D)
    val ObsidianSurface = Color(0xFF101520)
    val ObsidianElevated = Color(0xFF172030)
    val RoyalGold = Color(0xFFD4AF37)
    val SoftGold = Color(0xFFF3D575)
    val TechBlue = Color(0xFF1E6BFF)
    val CyanGlow = Color(0xFF58A6FF)
    val PureWhite = Color(0xFFF8FAFC)
    val LightSurface = Color(0xFFFFFFFF)
    val LightMuted = Color(0xFFF1F5F9)
    val DarkText = Color(0xFF0F172A)
    val MutedText = Color(0xFF94A3B8)

    val SafeGreen = Color(0xFF10B981)
    val MediumAmber = Color(0xFFF59E0B)
    val HighOrange = Color(0xFFF97316)
    val CriticalRed = Color(0xFFEF4444)

    // Sudanese Flag authentic palette for the cinematic opening only
    val SudanRed = Color(0xFFD21034)
    val SudanWhite = Color(0xFFFFFFFF)
    val SudanBlack = Color(0xFF000000)
    val SudanGreen = Color(0xFF007229)
}

private val DarkLuxuryScheme = darkColorScheme(
    primary = AmanXColors.RoyalGold,
    onPrimary = AmanXColors.LuxuryBlack,
    primaryContainer = AmanXColors.ObsidianElevated,
    onPrimaryContainer = AmanXColors.SoftGold,
    secondary = AmanXColors.TechBlue,
    onSecondary = AmanXColors.PureWhite,
    secondaryContainer = Color(0xFF0F2447),
    onSecondaryContainer = AmanXColors.CyanGlow,
    background = AmanXColors.LuxuryBlack,
    onBackground = AmanXColors.PureWhite,
    surface = AmanXColors.ObsidianSurface,
    onSurface = AmanXColors.PureWhite,
    surfaceVariant = AmanXColors.ObsidianElevated,
    onSurfaceVariant = AmanXColors.MutedText,
    outline = Color(0xFF2B364A),
    error = AmanXColors.CriticalRed
)

private val HighContrastDarkScheme = DarkLuxuryScheme.copy(
    background = Color.Black,
    surface = Color(0xFF0B0E14),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFE2E8F0),
    outline = AmanXColors.RoyalGold
)

private val CleanLightScheme = lightColorScheme(
    primary = Color(0xFF9C7A14),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBF3D5),
    onPrimaryContainer = Color(0xFF3E2E00),
    secondary = AmanXColors.TechBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EEFF),
    onSecondaryContainer = Color(0xFF0B2E6B),
    background = AmanXColors.LightMuted,
    onBackground = AmanXColors.DarkText,
    surface = AmanXColors.LightSurface,
    onSurface = AmanXColors.DarkText,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626)
)

private val HighContrastLightScheme = CleanLightScheme.copy(
    background = Color.White,
    surface = Color.White,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF1E293B),
    outline = Color.Black
)

private val AmanXTypography = Typography(
    headlineLarge = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
)

@Composable
fun AmanXTheme(
    themeMode: ThemeMode,
    language: AppLanguage,
    highContrast: Boolean = false,
    fontScaleMultiplier: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        useDark && highContrast -> HighContrastDarkScheme
        useDark -> DarkLuxuryScheme
        highContrast -> HighContrastLightScheme
        else -> CleanLightScheme
    }

    val layoutDirection = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr
    val currentDensity = LocalDensity.current

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * fontScaleMultiplier.coerceIn(0.85f, 1.35f)
        )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AmanXTypography,
            content = content
        )
    }
}

@Composable
fun AmanXCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun RiskLevelBadge(level: RiskLevel, language: AppLanguage) {
    val (color, label) = when (level) {
        RiskLevel.LOW -> AmanXColors.SafeGreen to (if (language == AppLanguage.AR) "منخفض" else "LOW")
        RiskLevel.MEDIUM -> AmanXColors.MediumAmber to (if (language == AppLanguage.AR) "متوسط" else "MEDIUM")
        RiskLevel.HIGH -> AmanXColors.HighOrange to (if (language == AppLanguage.AR) "مرتفع" else "HIGH")
        RiskLevel.CRITICAL -> AmanXColors.CriticalRed to (if (language == AppLanguage.AR) "حرج" else "CRITICAL")
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector = Icons.Outlined.Info,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    AmanXCard(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
