package com.example.olahragakuy.ui.login

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.olahragakuy.R
import com.example.olahragakuy.ui.components.KuyPrimaryButton
import com.example.olahragakuy.ui.components.KuyTextField
import com.example.olahragakuy.ui.theme.*

/** Figma Login 5:2002. Android supplies its own status and navigation bars. */
@Composable
fun LoginScreen(onBack: () -> Unit = {}) {
    var username by rememberSaveable { mutableStateOf("") }
    // Password stays in memory; never serialize it to saved-instance-state.
    var password by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val usernameError = if (attempted && username.isBlank()) "Isi nama pengguna terlebih dahulu." else null
    val passwordError = if (attempted && password.isEmpty()) "Isi kata sandi terlebih dahulu." else null
    val submit = {
        attempted = true
        if (username.isNotBlank() && password.isNotEmpty()) {
            focus.clearFocus()
            notice = "Fitur masuk akun sedang disiapkan. Belum ada akun yang masuk."
        }
    }
    Box(
        Modifier.fillMaxSize().background(KuyNavy).testTag("login-screen")
            .safeDrawingPadding().imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        BoxWithConstraints(Modifier.widthIn(max = 440.dp).fillMaxSize()) {
            // Scale whitespace, not type or controls. Small windows scroll naturally.
            val gapScale = ((maxHeight.value - 487f) / 419f).coerceIn(0.45f, 1.5f)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height((10 * gapScale).dp))
                Box(Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = KuyBlue, contentColor = KuyText, shadowElevation = 2.dp,
                        modifier = Modifier.widthIn(min = 128.dp).heightIn(min = 40.dp).clickable(onClick = onBack),
                    ) {
                        Row(Modifier.padding(horizontal = 17.5.dp), horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.login_back), null, Modifier.size(18.dp))
                            Text("Kembali", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Spacer(Modifier.height((22 * gapScale).dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(23.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        // Figma PNG export is cropped to the painted wordmark bounds (379 x 57 at 3x).
                        Image(painterResource(R.drawable.login_wordmark), "OlahragaKUY",
                            Modifier.size((379f / 3f).dp, 19.dp), contentScale = ContentScale.Fit)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text("MASUK", color = KuyOrange, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(2.dp))
                    Text("Selamat kembali\nkami sangat merindukanmu",
                        style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height((43 * gapScale).dp))
                Column(verticalArrangement = Arrangement.spacedBy(21.dp)) {
                    KuyTextField(username, { username = it }, "Nama Pengguna",
                        modifier = Modifier.testTag("username"), errorMessage = usernameError,
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }))
                    KuyTextField(password, { password = it }, "Kata Sandi",
                        modifier = Modifier.testTag("password"), errorMessage = passwordError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }))
                }
                val validationMessage = usernameError ?: passwordError
                if (validationMessage != null) {
                    Text(validationMessage, color = KuyError, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite })
                }
                Spacer(Modifier.height((139 * gapScale).dp))
                KuyPrimaryButton("Masuk", submit, Modifier.testTag("login-submit"))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Belum Mempunyai Akun?", style = MaterialTheme.typography.bodySmall)
                    Text("Daftar", color = KuyOrange, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clickable { notice = "Pendaftaran akun akan tersedia pada tahap berikutnya." })
                }
                Spacer(Modifier.height((69 * gapScale).dp))
                Text("Atau Masuk Dengan", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    SocialButton(R.drawable.login_google, "Google", 37) { notice = "Masuk dengan Google belum tersedia." }
                    SocialButton(R.drawable.login_facebook, "Facebook", 36) { notice = "Masuk dengan Facebook belum tersedia." }
                    SocialButton(R.drawable.login_instagram, "Instagram", 37) { notice = "Masuk dengan Instagram belum tersedia." }
                }
                Spacer(Modifier.height((136 * gapScale).dp))
            }
        }
    }
    notice?.let { message ->
        AlertDialog(onDismissRequest = { notice = null }, title = { Text("Belum tersedia") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { notice = null }) { Text("Mengerti") } })
    }
}

@Composable
private fun SocialButton(@DrawableRes icon: Int, provider: String, iconSize: Int, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(60.dp), shape = RoundedCornerShape(12.dp),
        color = androidx.compose.ui.graphics.Color.White, shadowElevation = 2.dp) {
        Box(contentAlignment = Alignment.Center) {
            Image(painterResource(icon), "Masuk dengan $provider", Modifier.size(iconSize.dp))
        }
    }
}

@Preview(name = "Login reference", widthDp = 440, heightDp = 906, showBackground = true)
@Preview(name = "Login compact", widthDp = 360, heightDp = 740, showBackground = true)
@Preview(name = "Login large text", widthDp = 393, heightDp = 820, fontScale = 1.5f)
@Composable
private fun LoginPreview() { OlahragaKuyTheme { LoginScreen() } }
