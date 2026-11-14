package com.example.olahragakuy.pages

import android.util.Patterns
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
import androidx.compose.ui.graphics.Color
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
import com.example.olahragakuy.ui.components.KuySuccessPopupOverlay
import com.example.olahragakuy.ui.components.KuyTextField
import com.example.olahragakuy.ui.theme.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

/** Figma Login 5:2002. Android supplies its own status and navigation bars. */
@Composable
fun LoginScreen(
    onBack: () -> Unit = {},
    onRegistered: () -> Unit = {},
    onLoggedIn: () -> Unit = {},
) {
    var email by rememberSaveable { mutableStateOf("") }
    // Password stays in memory; never serialize it to saved-instance-state.
    var password by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var showSuccessPopup by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    val emailError = when {
        !attempted -> null
        email.isBlank() -> "Isi email terlebih dahulu."
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
            "Masukkan format email yang valid untuk masuk."
        else -> null
    }
    val passwordError = if (attempted && password.isEmpty()) "Isi kata sandi terlebih dahulu." else null

    val submit = {
        attempted = true
        val trimmedEmail = email.trim()
        val isEmailValid = trimmedEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
        if (isEmailValid && password.isNotEmpty() && !loading) {
            focus.clearFocus()
            loading = true
            error = null

            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(trimmedEmail, password)
                .addOnCompleteListener { task ->
                    loading = false
                    if (task.isSuccessful && task.result?.user != null) {
                        attempted = false
                        showSuccessPopup = true
                    } else {
                        error = when (task.exception) {
                            is FirebaseAuthInvalidUserException ->
                                "Akun tidak terdaftar. Silakan daftar terlebih dahulu."
                            is FirebaseAuthInvalidCredentialsException ->
                                "Email atau kata sandi salah, atau akun belum terdaftar."
                            is FirebaseNetworkException ->
                                "Koneksi internet bermasalah. Periksa jaringan Anda."
                            is FirebaseTooManyRequestsException ->
                                "Terlalu banyak percobaan masuk. Coba lagi nanti."
                            else ->
                                "Gagal masuk. Periksa email dan kata sandi Anda."
                        }
                    }
                }
        }
    }

    Box(
        Modifier.fillMaxSize().background(KuyNavy).testTag("login-screen")
            .safeDrawingPadding().imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        BoxWithConstraints(Modifier.widthIn(max = 440.dp).fillMaxSize()) {
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
                        modifier = Modifier.widthIn(min = 128.dp).heightIn(min = 40.dp).clickable(enabled = !loading, onClick = onBack),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 17.5.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(painterResource(R.drawable.login_back), null, Modifier.size(18.dp))
                            Text("Kembali", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Spacer(Modifier.height((22 * gapScale).dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(23.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(
                            painterResource(R.drawable.login_wordmark), "OlahragaKUY",
                            Modifier.size((379f / 3f).dp, 19.dp), contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text("MASUK", color = KuyOrange, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Selamat kembali\nkami sangat merindukanmu",
                        style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height((43 * gapScale).dp))
                Column(verticalArrangement = Arrangement.spacedBy(21.dp)) {
                    KuyTextField(
                        email,
                        {
                            if (!loading) {
                                email = it
                                error = null
                            }
                        },
                        "Email",
                        modifier = Modifier.testTag("username"),
                        errorMessage = emailError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                    )
                    KuyTextField(
                        password,
                        {
                            if (!loading) {
                                password = it
                                error = null
                            }
                        },
                        "Kata Sandi",
                        modifier = Modifier.testTag("password"),
                        errorMessage = passwordError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                    )
                }
                val validationMessage = emailError ?: passwordError ?: error
                if (validationMessage != null) {
                    Text(
                        validationMessage, color = KuyError, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                Spacer(Modifier.height((139 * gapScale).dp))
                KuyPrimaryButton(
                    if (loading) "Memproses..." else "Masuk",
                    submit,
                    Modifier.testTag("login-submit"),
                    enabled = !loading
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Belum Mempunyai Akun?", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Daftar", color = KuyOrange, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clickable(enabled = !loading, onClick = onRegistered)
                    )
                }
                Spacer(Modifier.height((69 * gapScale).dp))
                Text("Atau Masuk Dengan", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    SocialButton(R.drawable.login_google, "Google", 37, !loading) { notice = "Masuk dengan Google belum tersedia." }
                    SocialButton(R.drawable.login_facebook, "Facebook", 36, !loading) { notice = "Masuk dengan Facebook belum tersedia." }
                    SocialButton(R.drawable.login_instagram, "Instagram", 37, !loading) { notice = "Masuk dengan Instagram belum tersedia." }
                }
                Spacer(Modifier.height((136 * gapScale).dp))
            }
        }
    }
    notice?.let { message ->
        AlertDialog(
            onDismissRequest = { notice = null }, title = { Text("Belum tersedia") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { notice = null }) { Text("Mengerti") } }
        )
    }
    KuySuccessPopupOverlay(
        visible = showSuccessPopup,
        illustrationRes = R.drawable.masuk_amico,
        subtitle = "proses masuk akun berhasil",
        onContinue = {
            password = ""
            onLoggedIn()
        },
    )
}

@Composable
private fun SocialButton(@DrawableRes icon: Int, provider: String, iconSize: Int, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = onClick, enabled = enabled, modifier = Modifier.size(60.dp), shape = RoundedCornerShape(12.dp),
        color = Color.White, shadowElevation = 2.dp
    ) {
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
