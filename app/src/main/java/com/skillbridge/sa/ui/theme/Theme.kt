package com.skillbridge.sa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val SkillBridgeColorScheme = lightColorScheme(
    primary = Green,
    onPrimary = SbWhite,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,
    secondary = Navy,
    onSecondary = SbWhite,
    tertiary = Gold,
    background = SbBackground,
    onBackground = TextPrimary,
    surface = SbWhite,
    onSurface = TextPrimary,
    error = SbError
)

val SkillBridgeTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
)

@Composable
fun SkillBridgeSATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SkillBridgeColorScheme,
        typography = SkillBridgeTypography,
        content = content
    )
}
