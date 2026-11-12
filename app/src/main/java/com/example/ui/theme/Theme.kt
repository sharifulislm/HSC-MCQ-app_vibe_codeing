package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode

private val NocturneDarkColorScheme = darkColorScheme(
    primary = NocturnePrimary,
    onPrimary = NocturneOnPrimary,
    primaryContainer = NocturnePrimaryContainer,
    onPrimaryContainer = NocturneOnPrimaryContainer,
    secondary = NocturneSecondary,
    onSecondary = NocturneOnSecondary,
    secondaryContainer = NocturneSecondaryContainer,
    onSecondaryContainer = NocturneOnSecondaryContainer,
    tertiary = NocturneTertiary,
    onTertiary = NocturneOnTertiary,
    tertiaryContainer = NocturneTertiaryContainer,
    onTertiaryContainer = NocturneOnTertiaryContainer,
    background = NocturneBackground,
    onBackground = NocturneOnBackground,
    surface = NocturneSurface,
    onSurface = NocturneOnSurface,
    surfaceVariant = NocturneSurfaceVariant,
    onSurfaceVariant = NocturneOnSurfaceVariant,
    outline = NocturneOutline
)

private val IvoryLightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = CobaltOnPrimary,
    primaryContainer = CobaltPrimaryContainer,
    onPrimaryContainer = CobaltOnPrimaryContainer,
    secondary = SaffronSecondary,
    onSecondary = SaffronOnSecondary,
    secondaryContainer = SaffronSecondaryContainer,
    onSecondaryContainer = SaffronOnSecondaryContainer,
    tertiary = RoyalTertiary,
    onTertiary = RoyalOnTertiary,
    tertiaryContainer = RoyalTertiaryContainer,
    onTertiaryContainer = RoyalOnTertiaryContainer,
    background = IvoryBackground,
    onBackground = IvoryOnBackground,
    surface = IvorySurface,
    onSurface = IvoryOnSurface,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = IvoryOnSurfaceVariant,
    outline = IvoryOutline
)

val HscShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HscEdgeTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> NocturneDarkColorScheme
        else -> IvoryLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HscTypography,
        shapes = HscShapes,
        content = content
    )
}
