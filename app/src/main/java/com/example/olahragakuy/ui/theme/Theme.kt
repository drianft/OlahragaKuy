package com.example.olahragakuy.ui.theme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
private val KuyColors = darkColorScheme(primary = KuyOrange, onPrimary = KuyNavy,
    secondary = KuyBlue, background = KuyNavy, surface = KuyNavy,
    onBackground = KuyText, onSurface = KuyText, error = KuyError)
@Composable
fun OlahragaKuyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KuyColors, typography = Typography) {
        CompositionLocalProvider(LocalContentColor provides KuyText, content = content)
    }
}
