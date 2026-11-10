package com.example.olahragakuy.pages

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.example.olahragakuy.ui.components.KuySuccessPopupOverlay
import com.example.olahragakuy.ui.components.KuyTextField
import com.example.olahragakuy.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    onLogin: () -> Unit = onBack,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    // Passwords deliberately stay out of saved instance state.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var profileWarning by remember { mutableStateOf(false) }
    var showSuccessPopup by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    val submit: () -> Unit = {
        if (!loading) {
            error = when {
                username.isBlank() -> "Isi nama pengguna terlebih dahulu."
                email.isBlank() -> "Isi email terlebih dahulu."
                password.length < 6 -> "Kata sandi minimal 6 karakter."
                password != confirmation -> "Kedua kata sandi harus sama."
                else -> null
            }
            if (error == null) {
                focus.clearFocus()
                val displayName = username.trim()
                loading = true
                confirmation = ""
                FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener { result ->
                        if (result.isSuccessful) {
                            password = ""
                            val user = result.result.user
                            if (user != null) {
                                user.updateProfile(
                                    UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
                                ).addOnCompleteListener { profileResult ->
                                    loading = false
                                    if (profileResult.isSuccessful) showSuccessPopup = true
                                    else profileWarning = true
                                }
                            } else {
                                loading = false
                                showSuccessPopup = true
                            }
                        } else {
                            loading = false
                            error = "Pendaftaran gagal. Periksa email dan kata sandi, lalu coba lagi."
                        }
                    }
            }
        }
    }

    Box(
        Modifier.fillMaxSize().background(KuyNavy).testTag("register-screen")
            .safeDrawingPadding().imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 440.dp).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth()) {
                Surface(
                    onClick = onBack, enabled = !loading,
                    modifier = Modifier.widthIn(min = 128.dp).heightIn(min = 40.dp),
                    shape = RoundedCornerShape(24.dp), color = KuyBlue, contentColor = KuyText,
                ) {
                    Row(
                        Modifier.padding(horizontal = 17.5.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(painterResource(R.drawable.login_back), null, Modifier.size(18.dp))
                        Text("Kembali", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Spacer(Modifier.height(25.dp))
            Image(
                painterResource(R.drawable.login_wordmark), "OlahragaKUY",
                Modifier.size((379f / 3f).dp, 19.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text("DAFTAR", color = KuyOrange, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                "Selamat datang\njelajahi dunia olahraga",
                style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(45.dp))
            Column(verticalArrangement = Arrangement.spacedBy(21.dp)) {
                KuyTextField(
                    username, { if (!loading) username = it }, "Nama Pengguna",
                    modifier = Modifier.testTag("register-username"),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                )
                KuyTextField(
                    email, { if (!loading) email = it }, "Email",
                    modifier = Modifier.testTag("register-email"),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email, autoCorrectEnabled = false, imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                )
                KuyTextField(
                    password, { if (!loading) password = it }, "Kata Sandi",
                    modifier = Modifier.testTag("register-password"),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                )
                KuyTextField(
                    confirmation, { if (!loading) confirmation = it }, "Konfirmasi Kata Sandi",
                    modifier = Modifier.testTag("register-confirmation"),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
            }
            error?.let {
                Text(it, color = KuyError, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite })
            }
            Spacer(Modifier.height(58.dp))
            Button(
                onClick = submit, enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(60.dp).testTag("register-submit"),
                shape = RoundedCornerShape(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KuyButton, contentColor = KuyText),
            ) {
                Text(if (loading) "Memproses..." else "Daftar", style = MaterialTheme.typography.labelLarge)
            }
            Row(
                Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Sudah Mempunyai Akun?", style = MaterialTheme.typography.bodySmall)
                TextButton(
                    onClick = onLogin, enabled = !loading, contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = KuyOrange),
                ) { Text("Masuk", style = MaterialTheme.typography.labelLarge) }
            }
            Spacer(Modifier.height(45.dp))
            Text("Atau Daftar Dengan", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                RegisterSocialButton(R.drawable.login_google, "Google", 37, !loading) {
                    notice = "Pendaftaran dengan Google belum tersedia."
                }
                RegisterSocialButton(R.drawable.login_facebook, "Facebook", 36, !loading) {
                    notice = "Pendaftaran dengan Facebook belum tersedia."
                }
                RegisterSocialButton(R.drawable.login_instagram, "Instagram", 37, !loading) {
                    notice = "Pendaftaran dengan Instagram belum tersedia."
                }
            }
            Spacer(Modifier.height(100.dp))
        }
    }
    if (profileWarning) {
        AlertDialog(
            onDismissRequest = { profileWarning = false; showSuccessPopup = true },
            title = { Text("Akun berhasil dibuat") },
            text = { Text("Nama pengguna belum berhasil disimpan. Akun tetap bisa digunakan untuk masuk.") },
            confirmButton = {
                TextButton(onClick = { profileWarning = false; showSuccessPopup = true }) { Text("Lanjutkan") }
            },
        )
    }
    notice?.let { message ->
        AlertDialog(
            onDismissRequest = { notice = null }, title = { Text("Informasi") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { notice = null }) { Text("Mengerti") } },
        )
    }
    KuySuccessPopupOverlay(
        visible = showSuccessPopup,
        illustrationRes = R.drawable.daftar_amico,
        subtitle = "akun anda berhasil didaftarkan",
        onContinue = onRegistered,
    )
}

@Composable
private fun RegisterSocialButton(
    @DrawableRes icon: Int, provider: String, iconSize: Int, enabled: Boolean, onClick: () -> Unit,
) {
    Surface(
        onClick = onClick, enabled = enabled, modifier = Modifier.size(60.dp),
        shape = RoundedCornerShape(12.dp), color = Color.White,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(painterResource(icon), "Daftar dengan $provider", Modifier.size(iconSize.dp))
        }
    }
}

@Preview(name = "Register reference", widthDp = 440, heightDp = 956, showSystemUi = true)
@Preview(name = "Register compact", widthDp = 360, heightDp = 740)
@Composable
private fun RegisterPreview() {
    OlahragaKuyTheme { RegisterScreen(onBack = {}, onRegistered = {}) }
}
