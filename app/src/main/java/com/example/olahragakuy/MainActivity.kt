package com.example.olahragakuy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Remove the system splash overlay immediately on the first frame without scaling
        // because OpeningScreen's initial frame is pixel-aligned with the system splash icon.
        splashScreen.setOnExitAnimationListener { splashView ->
            splashView.remove()
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent { OlahragaKuyTheme { AuthNavigation(onExit = { finish() }) } }
    }
}
