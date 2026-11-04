package com.example.olahragakuy

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.olahragakuy.ui.login.LoginScreen
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme

class LoginScreenshotTest {
    @PreviewTest
    @Preview(name = "Figma content", widthDp = 440, heightDp = 906)
    @Preview(name = "Compact", widthDp = 360, heightDp = 740)
    @Preview(name = "Large text", widthDp = 393, heightDp = 820, fontScale = 1.5f)
    @Composable
    fun login() { OlahragaKuyTheme { LoginScreen() } }
}
