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
import com.example.olahragakuy.pages.posts.RealtimeSystemNotifier
import com.example.olahragakuy.pages.steptracker.StepTrackerService
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme

class MainActivity : ComponentActivity() {
    private var openDrinkReminderRequested by mutableStateOf(false)
    private var openStepTrackerRequested by mutableStateOf(false)
    private var openChatRoomIdRequested by mutableStateOf<String?>(null)
    private var openChatListRequested by mutableStateOf(false)
    private var openNotificationsRequested by mutableStateOf(false)
    private var openNotificationPostIdRequested by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        handleDrinkReminderIntent(intent)
        handleStepTrackerIntent(intent)
        handleRealtimeNotificationIntent(intent)

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
                    openStepTracker = openStepTrackerRequested,
                    onConsumeOpenStepTracker = { openStepTrackerRequested = false },
                    openChatFromNotification = openChatListRequested,
                    openChatRoomId = openChatRoomIdRequested,
                    onConsumeOpenChatFromNotification = {
                        openChatListRequested = false
                        openChatRoomIdRequested = null
                    },
                    openNotificationsFromNotification = openNotificationsRequested,
                    openNotificationPostId = openNotificationPostIdRequested,
                    onConsumeOpenNotificationsFromNotification = {
                        openNotificationsRequested = false
                        openNotificationPostIdRequested = null
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDrinkReminderIntent(intent)
        handleStepTrackerIntent(intent)
        handleRealtimeNotificationIntent(intent)
    }

    private fun handleDrinkReminderIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(DrinkReminderScheduler.EXTRA_OPEN_DRINK_REMINDER, false) == true) {
            openDrinkReminderRequested = true
            intent.removeExtra(DrinkReminderScheduler.EXTRA_OPEN_DRINK_REMINDER)
        }
    }

    private fun handleStepTrackerIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(StepTrackerService.EXTRA_OPEN_STEP_TRACKER, false) == true) {
            openStepTrackerRequested = true
            intent.removeExtra(StepTrackerService.EXTRA_OPEN_STEP_TRACKER)
        }
    }

    private fun handleRealtimeNotificationIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(RealtimeSystemNotifier.EXTRA_OPEN_CHAT, false)) {
            openChatListRequested = true
            openChatRoomIdRequested = intent.getStringExtra(RealtimeSystemNotifier.EXTRA_OPEN_CHAT_ROOM_ID)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            intent.removeExtra(RealtimeSystemNotifier.EXTRA_OPEN_CHAT)
            intent.removeExtra(RealtimeSystemNotifier.EXTRA_OPEN_CHAT_ROOM_ID)
        }
        if (intent.getBooleanExtra(RealtimeSystemNotifier.EXTRA_OPEN_NOTIFICATIONS, false)) {
            openNotificationsRequested = true
            openNotificationPostIdRequested = intent.getStringExtra(RealtimeSystemNotifier.EXTRA_OPEN_NOTIFICATION_POST_ID)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            intent.removeExtra(RealtimeSystemNotifier.EXTRA_OPEN_NOTIFICATIONS)
            intent.removeExtra(RealtimeSystemNotifier.EXTRA_OPEN_NOTIFICATION_POST_ID)
        }
    }
}
