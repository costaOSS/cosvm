package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = CosmicPrimary,
  onPrimary = CosmicOnPrimary,
  secondary = CosmicSecondary,
  onSecondary = CosmicOnSecondary,
  tertiary = CosmicTertiary,
  onTertiary = CosmicOnTertiary,
  background = CosmicBg,
  onBackground = CosmicTextPrimary,
  surface = CosmicSurface,
  onSurface = CosmicTextPrimary,
  surfaceVariant = CosmicSurfaceVariant,
  onSurfaceVariant = CosmicTextSecondary,
  error = CosmicError,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = Color(0xFF007A8A),              // Deep Teal for light mode
  onPrimary = Color.White,
  secondary = Color(0xFF6A1B9A),            // Purple
  onSecondary = Color.White,
  tertiary = Color(0xFFC2185B),             // Pink
  onTertiary = Color.White,
  background = Color(0xFFF4F6FA),
  onBackground = Color(0xFF1C1D21),
  surface = Color.White,
  onSurface = Color(0xFF1C1D21),
  surfaceVariant = Color(0xFFE5E9F0),
  onSurfaceVariant = Color(0xFF4A5568)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Enable dynamic colors on Android 12+ if desired, but default to our custom theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> DarkColorScheme // Force dark by default for CosVM's high-fidelity cyber aesthetic, or follow system theme if requested
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
