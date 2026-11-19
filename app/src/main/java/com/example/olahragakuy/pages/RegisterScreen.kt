package com.example.olahragakuy.pages

import android.util.Patterns
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
import androidx.compose.ui.autofill.ContentType
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.R
import com.example.olahragakuy.ui.components.KuyPasswordVisibilityToggle
import com.example.olahragakuy.ui.components.KuySuccessPopupOverlay
import com.example.olahragakuy.ui.components.KuyTextField
import com.example.olahragakuy.ui.theme.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.example.olahragakuy.pages.profile.user.ensureUserProfile

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    onLogin: () -> Unit = onBack,
    onRegisteredWithEmail: (String) -> Unit = {},
) {
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var registeredEmail by rememberSaveable { mutableStateOf("") }
    // Passwords deliberately stay out of saved instance state.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmationVisible by remember { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var serverEmailError by remember { mutableStateOf<String?>(null) }
    var serverPasswordError by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var showSuccessPopup by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    val usernameRegex = remember { Regex("^[a-zA-Z0-9._]+$") }
    val usernameError = when {
        !attempted -> null
        username.isBlank() -> "Isi nama pengguna terlebih dahulu."
        username.trim().length < 3 -> "Nama pengguna minimal 3 karakter."
        !usernameRegex.matches(username.trim()) ->
            "Nama pengguna hanya boleh huruf, angka, titik, atau underscore (_)."
        else -> null
    }
    val emailError = when {
        serverEmailError != null -> serverEmailError
        !attempted -> null
        email.isBlank() -> "Isi email terlebih dahulu."
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
            "Masukkan format email yang valid untuk mendaftar."
        else -> null
    }
    val passwordError = when {
        serverPasswordError != null -> serverPasswordError
        !attempted -> null
        password.isEmpty() -> "Isi kata sandi terlebih dahulu."
        password.length < 8 || !password.any { it.isLetter() } || !password.any { it.isDigit() } ->
            "Kata sandi minimal 8 karakter dengan kombinasi huruf dan angka."
        else -> null
    }
    val confirmationError = when {
        !attempted -> null
        confirmation.isEmpty() -> "Isi konfirmasi kata sandi terlebih dahulu."
        password != confirmation -> "Kedua kata sandi harus sama."
        else -> null
    }

    val submit: () -> Unit = {
        attempted = true
        serverEmailError = null
        serverPasswordError = null
        error = null

        val trimmedUsername = username.trim()
        val trimmedEmail = email.trim()
        val isUsernameValid = trimmedUsername.length >= 3 && usernameRegex.matches(trimmedUsername)
        val isEmailValid = trimmedEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
        val isPasswordValid = password.length >= 8 &&
            password.any { it.isLetter() } &&
            password.any { it.isDigit() }
        val isFormValid = isUsernameValid &&
            isEmailValid &&
            isPasswordValid &&
            password == confirmation

        if (isFormValid && !loading) {
            focus.clearFocus()
            passwordVisible = false
            confirmationVisible = false
            loading = true
            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(trimmedEmail, password)
                .addOnCompleteListener { result ->
                    if (result.isSuccessful) {
                        attempted = false
                        password = ""
                        confirmation = ""
                        registeredEmail = trimmedEmail
                        val user = result.result.user
                        if (user != null) {
                            user.updateProfile(
                                UserProfileChangeRequest.Builder().setDisplayName(trimmedUsername).build(),
                            ).addOnCompleteListener { profileResult ->
                                ensureUserProfile(user, trimmedUsername)
                                    .addOnCompleteListener { firestoreResult ->
                                        user.sendEmailVerification().addOnCompleteListener { verificationResult ->
                                            FirebaseAuth.getInstance().signOut()
                                            loading = false
                                            if (profileResult.isSuccessful && firestoreResult.isSuccessful &&
                                                verificationResult.isSuccessful
                                            ) {
                                                showSuccessPopup = true
                                            } else {
                                                notice = buildString {
                                                    append("Akun sudah dibuat. Jangan daftar ulang. ")
                                                    if (!firestoreResult.isSuccessful) {
                                                        append("Profil belum tersimpan di Firestore; coba masuk kembali untuk mengulang penyimpanan. ")
                                                    }
                                                    if (!profileResult.isSuccessful) {
                                                        append("Nama akun belum tersimpan di Authentication. ")
                                                    }
                                                    if (!verificationResult.isSuccessful) {
                                                        append("Email verifikasi belum terkirim; coba masuk untuk mengirim ulang.")
                                                    } else {
                                                        append("Periksa email untuk verifikasi akun.")
                                                    }
                                                }
                                            }
                                        }
                                    }
                            }
                        } else {
                            FirebaseAuth.getInstance().signOut()
                            loading = false
                            showSuccessPopup = true
                        }
                    } else {
                        loading = false
                        when (result.exception) {
                            is FirebaseAuthUserCollisionException ->
                                serverEmailError = "Email sudah terdaftar. Silakan masuk atau gunakan email lain."
                            is FirebaseAuthWeakPasswordException ->
                                serverPasswordError = "Kata sandi terlalu lemah. Gunakan minimal 8 karakter dengan kombinasi huruf dan angka."
                            is FirebaseAuthInvalidCredentialsException ->
                                serverEmailError = "Masukkan format email yang valid untuk mendaftar."
                            is FirebaseNetworkException ->
                                error = "Koneksi internet bermasalah. Periksa jaringan Anda."
                            is FirebaseTooManyRequestsException ->
                                error = "Terlalu banyak percobaan pendaftaran. Coba lagi nanti."
                            else ->
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
        BoxWithConstraints(Modifier.widthIn(max = 440.dp).fillMaxSize()) {
            val gapScale = ((maxHeight.value - 560f) / 396f).coerceIn(0.45f, 1.3f)
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height((10 * gapScale).dp))
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
                Spacer(Modifier.height((25 * gapScale).dp))
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
                Spacer(Modifier.height((45 * gapScale).dp))
                Column(verticalArrangement = Arrangement.spacedBy(21.dp)) {
                    Column(Modifier.fillMaxWidth()) {
                        KuyTextField(
                            username,
                            {
                                if (!loading) {
                                    username = it.take(24)
                                    error = null
                                }
                            },
                            "Nama Pengguna",
                            modifier = Modifier.testTag("register-username"),
                            errorMessage = usernameError,
                            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                            contentType = ContentType.Username,
                        )
                        if (usernameError != null) {
                            Text(
                                usernameError,
                                color = KuyError,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        } else {
                            Text(
                                "3–24 karakter (huruf, angka, titik, atau underscore)",
                                color = KuyText.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
                            )
                        }
                    }
                    Column(Modifier.fillMaxWidth()) {
                        KuyTextField(
                            email,
                            {
                                if (!loading) {
                                    email = it.take(254)
                                    serverEmailError = null
                                    error = null
                                }
                            },
                            "Email",
                            modifier = Modifier.testTag("register-email"),
                            errorMessage = emailError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email, autoCorrectEnabled = false, imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                            contentType = ContentType.EmailAddress,
                        )
                        emailError?.let { errMsg ->
                            Text(
                                errMsg,
                                color = KuyError,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                    }
                    Column(Modifier.fillMaxWidth()) {
                        KuyTextField(
                            password,
                            {
                                if (!loading) {
                                    password = it.take(128)
                                    serverPasswordError = null
                                    error = null
                                }
                            },
                            "Kata Sandi",
                            modifier = Modifier.testTag("register-password"),
                            errorMessage = passwordError,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                            contentType = ContentType.NewPassword,
                            trailingContent = {
                                KuyPasswordVisibilityToggle(
                                    visible = passwordVisible,
                                    onToggle = { if (!loading) passwordVisible = !passwordVisible },
                                    enabled = !loading,
                                    modifier = Modifier.testTag("register-password-toggle"),
                                )
                            },
                        )
                        if (passwordError != null) {
                            Text(
                                passwordError,
                                color = KuyError,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        } else {
                            Text(
                                "Min. 8 karakter dengan kombinasi huruf dan angka",
                                color = KuyText.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
                            )
                        }
                    }
                    Column(Modifier.fillMaxWidth()) {
                        KuyTextField(
                            confirmation,
                            {
                                if (!loading) {
                                    confirmation = it.take(128)
                                    error = null
                                }
                            },
                            "Konfirmasi Kata Sandi",
                            modifier = Modifier.testTag("register-confirmation"),
                            errorMessage = confirmationError,
                            visualTransformation = if (confirmationVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                            contentType = ContentType.NewPassword,
                            trailingContent = {
                                KuyPasswordVisibilityToggle(
                                    visible = confirmationVisible,
                                    onToggle = { if (!loading) confirmationVisible = !confirmationVisible },
                                    enabled = !loading,
                                    modifier = Modifier.testTag("register-confirmation-toggle"),
                                )
                            },
                        )
                        confirmationError?.let { errMsg ->
                            Text(
                                errMsg,
                                color = KuyError,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                    }
                }
                error?.let { errMsg ->
                    Text(
                        errMsg,
                        color = KuyError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Spacer(Modifier.height((58 * gapScale).dp))
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
                Spacer(Modifier.height((45 * gapScale).dp))
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
                Spacer(Modifier.height((100 * gapScale).dp))
            }
        }
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
        subtitle = "akun berhasil didaftarkan, cek email anda untuk verifikasi",
        onContinue = {
            onRegisteredWithEmail(registeredEmail)
            onRegistered()
        },
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
