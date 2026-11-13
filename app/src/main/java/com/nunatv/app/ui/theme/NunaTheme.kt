package com.nunatv.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunatv.app.data.preferences.AppColorPalette
import com.nunatv.app.data.preferences.AppThemeMode

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val ExpressiveTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.25).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
)

fun buildNunaColorScheme(
    palette: AppColorPalette,
    themeMode: AppThemeMode
): ColorScheme {
    val isDark = themeMode == AppThemeMode.DARK
    return when (palette) {
        AppColorPalette.RED -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFE51926),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF5C1117),
                onPrimaryContainer = Color(0xFFFFDAD9),
                secondary = Color(0xFFFFB3B5),
                onSecondary = Color(0xFF5F1218),
                secondaryContainer = Color(0xFF3A1A1D),
                onSecondaryContainer = Color(0xFFFFDAD9),
                background = Color(0xFF100E0F),
                onBackground = Color(0xFFF5EFF0),
                surface = Color(0xFF171314),
                onSurface = Color(0xFFF5EFF0),
                surfaceVariant = Color(0xFF241E20),
                onSurfaceVariant = Color(0xFFBDB3B5),
                surfaceContainerLowest = Color(0xFF0B090A),
                surfaceContainerLow = Color(0xFF151112),
                surfaceContainer = Color(0xFF1C1719),
                surfaceContainerHigh = Color(0xFF272123),
                outline = Color(0xFF4F4346),
                outlineVariant = Color(0xFF332B2D)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFD31926),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFDAD9),
                onPrimaryContainer = Color(0xFF410006),
                secondary = Color(0xFF775657),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFFECEC),
                onSecondaryContainer = Color(0xFF2C1516),
                background = Color(0xFFFFF8F8),
                onBackground = Color(0xFF201A1A),
                surface = Color(0xFFFFF8F8),
                onSurface = Color(0xFF201A1A),
                surfaceVariant = Color(0xFFF4E6E7),
                onSurfaceVariant = Color(0xFF5C4E50),
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = Color(0xFFFDF2F2),
                surfaceContainer = Color(0xFFF7E9EA),
                surfaceContainerHigh = Color(0xFFF0DFE0),
                outline = Color(0xFF917F81),
                outlineVariant = Color(0xFFE0CECF)
            )
        }

        AppColorPalette.PURPLE -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFF7E57FF),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF3E2478),
                onPrimaryContainer = Color(0xFFEDE7F6),
                secondary = Color(0xFFD0BCFF),
                onSecondary = Color(0xFF381E72),
                secondaryContainer = Color(0xFF2D2246),
                onSecondaryContainer = Color(0xFFEADDFF),
                background = Color(0xFF100E16),
                onBackground = Color(0xFFF3EFF9),
                surface = Color(0xFF16131F),
                onSurface = Color(0xFFF3EFF9),
                surfaceVariant = Color(0xFF231E30),
                onSurfaceVariant = Color(0xFFBEB7CE),
                surfaceContainerLowest = Color(0xFF0B0910),
                surfaceContainerLow = Color(0xFF14111C),
                surfaceContainer = Color(0xFF1C1726),
                surfaceContainerHigh = Color(0xFF272134),
                outline = Color(0xFF4C445E),
                outlineVariant = Color(0xFF322C40)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF6B3FD9),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFEADDFF),
                onPrimaryContainer = Color(0xFF21005D),
                secondary = Color(0xFF625B71),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFF0E8FF),
                onSecondaryContainer = Color(0xFF1D192B),
                background = Color(0xFFFCF9FF),
                onBackground = Color(0xFF1D1B22),
                surface = Color(0xFFFCF9FF),
                onSurface = Color(0xFF1D1B22),
                surfaceVariant = Color(0xFFECE6F5),
                onSurfaceVariant = Color(0xFF534D60),
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = Color(0xFFF6F2FC),
                surfaceContainer = Color(0xFFEEE8F7),
                surfaceContainerHigh = Color(0xFFE6DFF0),
                outline = Color(0xFF837B92),
                outlineVariant = Color(0xFFD5CEE0)
            )
        }

        AppColorPalette.BLUE -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFF1E88E5),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF0F4C81),
                onPrimaryContainer = Color(0xFFD6E9FF),
                secondary = Color(0xFFA8C7FA),
                onSecondary = Color(0xFF062E6F),
                secondaryContainer = Color(0xFF1A293D),
                onSecondaryContainer = Color(0xFFD3E3FD),
                background = Color(0xFF0C1118),
                onBackground = Color(0xFFEEF3FA),
                surface = Color(0xFF121822),
                onSurface = Color(0xFFEEF3FA),
                surfaceVariant = Color(0xFF1C2432),
                onSurfaceVariant = Color(0xFFB0BDD0),
                surfaceContainerLowest = Color(0xFF080B10),
                surfaceContainerLow = Color(0xFF101620),
                surfaceContainer = Color(0xFF161E2B),
                surfaceContainerHigh = Color(0xFF202A3A),
                outline = Color(0xFF414E63),
                outlineVariant = Color(0xFF2B3545)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF1565C0),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD6E4FF),
                onPrimaryContainer = Color(0xFF001B3E),
                secondary = Color(0xFF535F70),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFE6EEFA),
                onSecondaryContainer = Color(0xFF101C2B),
                background = Color(0xFFF8FAFF),
                onBackground = Color(0xFF191C22),
                surface = Color(0xFFF8FAFF),
                onSurface = Color(0xFF191C22),
                surfaceVariant = Color(0xFFE2E9F5),
                onSurfaceVariant = Color(0xFF495364),
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = Color(0xFFF1F5FC),
                surfaceContainer = Color(0xFFE8EEF9),
                surfaceContainerHigh = Color(0xFFDFE6F3),
                outline = Color(0xFF798496),
                outlineVariant = Color(0xFFC9D2E2)
            )
        }

        AppColorPalette.BROWN -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFF8D5B4C),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF5D3522),
                onPrimaryContainer = Color(0xFFFFDFCE),
                secondary = Color(0xFFE0C1B1),
                onSecondary = Color(0xFF3A271E),
                secondaryContainer = Color(0xFF2F231E),
                onSecondaryContainer = Color(0xFFF5DFD3),
                background = Color(0xFF120F0D),
                onBackground = Color(0xFFF5EFEA),
                surface = Color(0xFF191411),
                onSurface = Color(0xFFF5EFEA),
                surfaceVariant = Color(0xFF261F1B),
                onSurfaceVariant = Color(0xFFC5B6AD),
                surfaceContainerLowest = Color(0xFF0C0A08),
                surfaceContainerLow = Color(0xFF171210),
                surfaceContainer = Color(0xFF1F1916),
                surfaceContainerHigh = Color(0xFF2B231F),
                outline = Color(0xFF54463F),
                outlineVariant = Color(0xFF382E29)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF7D4B32),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFDBC9),
                onPrimaryContainer = Color(0xFF2E1204),
                secondary = Color(0xFF70594E),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFF6EBE5),
                onSecondaryContainer = Color(0xFF271810),
                background = Color(0xFFFFFBF8),
                onBackground = Color(0xFF211A16),
                surface = Color(0xFFFFFBF8),
                onSurface = Color(0xFF211A16),
                surfaceVariant = Color(0xFFF2E6DF),
                onSurfaceVariant = Color(0xFF5A4C45),
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = Color(0xFFFAF3EE),
                surfaceContainer = Color(0xFFF4EAE4),
                surfaceContainerHigh = Color(0xFFECE0D9),
                outline = Color(0xFF8C7B72),
                outlineVariant = Color(0xFFDCD0C8)
            )
        }
    }
}

@Composable
fun NunaTheme(
    palette: AppColorPalette = AppColorPalette.RED,
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = buildNunaColorScheme(palette, themeMode),
        shapes = ExpressiveShapes,
        typography = ExpressiveTypography,
        content = content
    )
}
