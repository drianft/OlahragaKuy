package com.example.olahragakuy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.olahragakuy.pages.drinkreminder.DrinkReminderScheduler
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme

class MainActivity : ComponentActivity() {
    private var openDrinkReminderRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        handleDrinkReminderIntent(intent)

        // Remove the system splash overlay immediately on the first frame without scaling
        // because OpeningScreen's initial frame is pixel-aligned with the system splash icon.
        splashScreen.setOnExitAnimationListener { splashView ->
            splashView.remove()
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            OlahragaKuyTheme {
                AuthNavigation(
                    onExit = { finish() },
                    openDrinkReminder = openDrinkReminderRequested,
                    onConsumeOpenDrinkReminder = { openDrinkReminderRequested = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDrinkReminderIntent(intent)
    }

    private fun handleDrinkReminderIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(DrinkReminderScheduler.EXTRA_OPEN_DRINK_REMINDER, false) == true) {
            openDrinkReminderRequested = true
            intent.removeExtra(DrinkReminderScheduler.EXTRA_OPEN_DRINK_REMINDER)
        }
    }
}
