package com.usoy.papiro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Indigo Theme (Default)
private val IndigoLight = lightColorScheme(
    primary = Color(0xFF4F378B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    error = Color(0xFFB3261E)
)

private val IndigoDark = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    error = Color(0xFFF2B8B5)
)

// Emerald Theme
private val EmeraldLight = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF115E59),
    secondary = Color(0xFF0369A1),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    tertiary = Color(0xFF15803D),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF0FDF4),
    onBackground = Color(0xFF14532D),
    surface = Color(0xFFF0FDF4),
    onSurface = Color(0xFF14532D),
    surfaceVariant = Color(0xFFDCFCE7),
    onSurfaceVariant = Color(0xFF166534),
    outline = Color(0xFF15803D),
    error = Color(0xFFB3261E)
)

private val EmeraldDark = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0C4A6E),
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFF4ADE80),
    onTertiary = Color(0xFF052E16),
    background = Color(0xFF022C22),
    onBackground = Color(0xFFCCFBF1),
    surface = Color(0xFF022C22),
    onSurface = Color(0xFFCCFBF1),
    surfaceVariant = Color(0xFF115E59),
    onSurfaceVariant = Color(0xFF99F6E4),
    outline = Color(0xFF5DD4BF),
    error = Color(0xFFF2B8B5)
)

// Amber Theme
private val AmberLight = lightColorScheme(
    primary = Color(0xFFB45309),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFF854D0E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEF9C3),
    onSecondaryContainer = Color(0xFF713F12),
    tertiary = Color(0xFF9A3412),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFBEB),
    onBackground = Color(0xFF451A03),
    surface = Color(0xFFFFFBEB),
    onSurface = Color(0xFF451A03),
    surfaceVariant = Color(0xFFFEF3C7),
    onSurfaceVariant = Color(0xFF78350F),
    outline = Color(0xFFD97706),
    error = Color(0xFFB3261E)
)

private val AmberDark = darkColorScheme(
    primary = Color(0xFFFBBF24),
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = Color(0xFFFACC15),
    onSecondary = Color(0xFF3F2F05),
    secondaryContainer = Color(0xFF713F12),
    onSecondaryContainer = Color(0xFFFEF9C3),
    tertiary = Color(0xFFFB923C),
    onTertiary = Color(0xFF431407),
    background = Color(0xFF271305),
    onBackground = Color(0xFFFDE68A),
    surface = Color(0xFF271305),
    onSurface = Color(0xFFFDE68A),
    surfaceVariant = Color(0xFF451A03),
    onSurfaceVariant = Color(0xFFFBBF24),
    outline = Color(0xFFF59E0B),
    error = Color(0xFFF2B8B5)
)

// Rose Theme
private val RoseLight = lightColorScheme(
    primary = Color(0xFFBE123C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = Color(0xFF4C0519) ,
    secondary = Color(0xFFB45309),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFFA21CAF),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF1F2),
    onBackground = Color(0xFF4C0519),
    surface = Color(0xFFFFF1F2),
    onSurface = Color(0xFF4C0519),
    surfaceVariant = Color(0xFFFFE4E6),
    onSurfaceVariant = Color(0xFF881337),
    outline = Color(0xFFF43F5E),
    error = Color(0xFFB3261E)
)

private val RoseDark = darkColorScheme(
    primary = Color(0xFFFB7185),
    onPrimary = Color(0xFF4C0519),
    primaryContainer = Color(0xFF881337),
    onPrimaryContainer = Color(0xFFFFE4E6),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFFF472B6),
    onTertiary = Color(0xFF4C0519),
    background = Color(0xFF2D0612),
    onBackground = Color(0xFFFECDD3),
    surface = Color(0xFF2D0612),
    onSurface = Color(0xFFFECDD3),
    surfaceVariant = Color(0xFF881337),
    onSurfaceVariant = Color(0xFFFB7185),
    outline = Color(0xFFFDA4AF),
    error = Color(0xFFF2B8B5)
)

// Cosmic Slate Theme
private val CosmicLight = lightColorScheme(
    primary = Color(0xFF1E3A8A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary = Color(0xFF0369A1),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    tertiary = Color(0xFF4338CA),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFF2563EB),
    error = Color(0xFFB3261E)
)

private val CosmicDark = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF1E3A8A),
    primaryContainer = Color(0xFF1D4ED8),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0369A1),
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFF818CF8),
    onTertiary = Color(0xFF312E81),
    background = Color(0xFF0B0F19),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF0B0F19),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF3B82F6),
    error = Color(0xFFF2B8B5)
)

@Composable
fun PapiroTheme(
    themeName: String = "INDIGO",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName.uppercase()) {
        "EMERALD" -> if (darkTheme) EmeraldDark else EmeraldLight
        "AMBER" -> if (darkTheme) AmberDark else AmberLight
        "ROSE" -> if (darkTheme) RoseDark else RoseLight
        "COSMIC" -> if (darkTheme) CosmicDark else CosmicLight
        else -> if (darkTheme) IndigoDark else IndigoLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
