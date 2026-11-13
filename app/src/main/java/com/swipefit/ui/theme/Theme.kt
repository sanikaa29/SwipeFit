package com.swipefit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- Brand ----------
val HotPink = Color(0xFFFF3D6E)
val Coral = Color(0xFFFF8A4C)
val NotGray = Color(0xFF8A8A8E)

/** The signature SwipeFit gradient, used for primary actions and hero surfaces. */
val HotGradient = Brush.linearGradient(listOf(HotPink, Coral))

// ---------- Medals ----------
val Gold = Color(0xFFF5B301)
val Silver = Color(0xFFAEB4BF)
val Bronze = Color(0xFFCD7F32)

private val LightScheme = lightColorScheme(
    primary = HotPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE3EA),
    onPrimaryContainer = Color(0xFF5C0020),
    secondary = Coral,
    onSecondary = Color.White,
    background = Color(0xFFF7F6F9),
    onBackground = Color(0xFF14121A),
    surface = Color.White,
    onSurface = Color(0xFF14121A),
    surfaceVariant = Color(0xFFF0EEF3),
    onSurfaceVariant = Color(0xFF77747F),
    outline = Color(0xFFC9C5CF),
    outlineVariant = Color(0xFFE6E3EA)
)

private val DarkScheme = darkColorScheme(
    primary = HotPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A1426),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Coral,
    onSecondary = Color.White,
    background = Color(0xFF0F0E13),
    onBackground = Color(0xFFF2F0F5),
    surface = Color(0xFF1A1820),
    onSurface = Color(0xFFF2F0F5),
    surfaceVariant = Color(0xFF26232E),
    onSurfaceVariant = Color(0xFFA6A3AE),
    outline = Color(0xFF4A4653),
    outlineVariant = Color(0xFF2F2C37)
)

private val AppTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = t.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** Follows the system light/dark setting. */
@Composable
fun SwipeFitTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
