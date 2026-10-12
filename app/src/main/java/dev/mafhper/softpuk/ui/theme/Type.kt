package dev.mafhper.softpuk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

private val baseTypography = Typography()

private fun mono(style: TextStyle): TextStyle =
    style.copy(fontFamily = FontFamily.Monospace)

val SoftPukTypography = Typography(
    displayLarge = mono(baseTypography.displayLarge),
    displayMedium = mono(baseTypography.displayMedium),
    displaySmall = mono(baseTypography.displaySmall),
    headlineLarge = mono(baseTypography.headlineLarge),
    headlineMedium = mono(baseTypography.headlineMedium),
    headlineSmall = mono(baseTypography.headlineSmall),
    titleLarge = mono(baseTypography.titleLarge),
    titleMedium = mono(baseTypography.titleMedium),
    titleSmall = mono(baseTypography.titleSmall),
    bodyLarge = mono(baseTypography.bodyLarge),
    bodyMedium = mono(baseTypography.bodyMedium),
    bodySmall = mono(baseTypography.bodySmall),
    labelLarge = mono(baseTypography.labelLarge),
    labelMedium = mono(baseTypography.labelMedium),
    labelSmall = mono(baseTypography.labelSmall),
)
