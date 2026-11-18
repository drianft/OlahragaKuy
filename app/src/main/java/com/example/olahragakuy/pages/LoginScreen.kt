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
import androidx.compose.ui.autofill.ContentType
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.olahragakuy.R
import com.example.olahragakuy.ui.components.KuyPasswordVisibilityToggle
import com.example.olahragakuy.ui.components.KuyPrimaryButton
import com.example.olahragakuy.ui.components.KuySuccessPopupOverlay
import com.example.olahragakuy.ui.components.KuyTextField
import com.example.olahragakuy.ui.theme.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/** Figma Login 5:2002. Android supplies its own status and navigation bars. */
@Composable
fun LoginScreen(
    onBack: () -> Unit = {},
    onRegistered: () -> Unit = {},
    onLoggedIn: (String) -> Unit = {},
    initialEmail: String = "",
) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    LaunchedEffect(initialEmail) {
        if (initialEmail.isNotBlank() && email.isBlank()) {
            email = initialEmail
        }
    }
    // Password stays in memory; never serialize it to saved-instance-state.
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var credentialsError by remember { mutableStateOf(false) }
    var serverEmailError by remember { mutableStateOf<String?>(null) }
    var serverPasswordError by remember { mutableStateOf<String?>(null) }
    var noticeTitle by remember { mutableStateOf("Belum tersedia") }
    var notice by remember { mutableStateOf<String?>(null) }
    var showSuccessPopup by rememberSaveable { mutableStateOf(false) }
    var loggedInRole by rememberSaveable { mutableStateOf("member") }

    var showForgotPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var resetEmail by rememberSaveable { mutableStateOf("") }
    var resetLoading by remember { mutableStateOf(false) }
    var resetEmailError by remember { mutableStateOf<String?>(null) }

    val focus = LocalFocusManager.current

    val emailError = when {
        serverEmailError != null -> serverEmailError
        !attempted -> null
        email.isBlank() -> "Isi email terlebih dahulu."
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
            "Masukkan format email yang valid untuk masuk."
        else -> null
    }
    val passwordError = when {
        serverPasswordError != null -> serverPasswordError
        attempted && password.isEmpty() -> "Isi kata sandi terlebih dahulu."
        else -> null
    }

    val submit = {
        attempted = true
        serverEmailError = null
        serverPasswordError = null
        credentialsError = false
        error = null
        val trimmedEmail = email.trim()
        val isEmailValid = trimmedEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
        if (isEmailValid && password.isNotEmpty() && !loading) {
            focus.clearFocus()
            passwordVisible = false
            loading = true

            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(trimmedEmail, password)
                .addOnCompleteListener { task ->
                    val user = if (task.isSuccessful) task.result?.user else null
                    if (user != null) {
                        val userDocRef = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.uid)
                        userDocRef.get()
                            .addOnSuccessListener { doc ->
                                val role = doc.getString("role") ?: "member"
                                if (role != "admin" && !user.isEmailVerified) {
                                    user.sendEmailVerification().addOnCompleteListener {
                                        FirebaseAuth.getInstance().signOut()
                                        loading = false
                                        noticeTitle = "Verifikasi Email Diperlukan"
                                        notice = "Email Anda belum diverifikasi. Tautan verifikasi baru telah dikirim ke kotak masuk atau folder spam email Anda. Silakan klik tautan tersebut terlebih dahulu sebelum masuk."
                                    }
                                } else {
                                    if (!doc.exists()) {
                                        val defaultUsername = user.displayName
                                            ?.takeIf { it.isNotBlank() }
                                            ?: (user.email ?: trimmedEmail).substringBefore("@")
                                        userDocRef.set(
                                            mapOf(
                                                "uid" to user.uid,
                                                "username" to defaultUsername,
                                                "email" to (user.email ?: trimmedEmail),
                                                "role" to role,
                                                "createdAt" to FieldValue.serverTimestamp(),
                                            ),
                                        )
                                    }
                                    loading = false
                                    attempted = false
                                    loggedInRole = role
                                    showSuccessPopup = true
                                }
                            }
                            .addOnFailureListener {
                                if (!user.isEmailVerified) {
                                    user.sendEmailVerification().addOnCompleteListener {
                                        FirebaseAuth.getInstance().signOut()
                                        loading = false
                                        noticeTitle = "Verifikasi Email Diperlukan"
                                        notice = "Email Anda belum diverifikasi. Tautan verifikasi baru telah dikirim ke kotak masuk atau folder spam email Anda. Silakan klik tautan tersebut terlebih dahulu sebelum masuk."
                                    }
                                } else {
                                    loading = false
                                    attempted = false
                                    loggedInRole = "member"
                                    showSuccessPopup = true
                                }
                            }
                    } else {
                        loading = false
                        when (val exception = task.exception) {
                            is FirebaseAuthInvalidUserException -> {
                                if (exception.errorCode == "ERROR_USER_DISABLED") {
                                    serverEmailError = "Akun dengan email ini telah dinonaktifkan."
                                } else {
                                    credentialsError = true
                                    error = "Email atau kata sandi yang Anda masukkan salah."
                                }
                            }
                            is FirebaseAuthInvalidCredentialsException -> {
                                when (exception.errorCode) {
                                    "ERROR_INVALID_EMAIL" ->
                                        serverEmailError = "Masukkan format email yang valid untuk masuk."
                                    else -> {
                                        credentialsError = true
                                        error = "Email atau kata sandi yang Anda masukkan salah."
                                    }
                                }
                            }
                            is FirebaseNetworkException ->
                                error = "Koneksi internet bermasalah. Periksa jaringan Anda."
                            is FirebaseTooManyRequestsException ->
                                error = "Terlalu banyak percobaan masuk. Coba lagi nanti."
                            else ->
                                error = "Gagal masuk. Periksa email dan kata sandi Anda."
                        }
                    }
                }
        }
    }

    val submitResetPassword = {
        val trimmedResetEmail = resetEmail.trim()
        resetEmailError = when {
            trimmedResetEmail.isEmpty() -> "Isi email terlebih dahulu."
            !Patterns.EMAIL_ADDRESS.matcher(trimmedResetEmail).matches() ->
                "Masukkan format email yang valid."
            else -> null
        }
        if (resetEmailError == null && !resetLoading) {
            focus.clearFocus()
            resetLoading = true
            FirebaseAuth.getInstance()
                .sendPasswordResetEmail(trimmedResetEmail)
                .addOnCompleteListener { task ->
                    resetLoading = false
                    val exception = task.exception
                    if (task.isSuccessful || (exception is FirebaseAuthInvalidUserException && exception.errorCode != "ERROR_USER_DISABLED")) {
                        email = trimmedResetEmail
                        showForgotPasswordDialog = false
                        noticeTitle = "Email Terkirim"
                        notice = "Jika email terdaftar, tautan pemulihan kata sandi telah dikirim ke $trimmedResetEmail. Silakan periksa kotak masuk atau folder spam Anda."
                    } else {
                        resetEmailError = when (exception) {
                            is FirebaseAuthInvalidUserException ->
                                "Akun dengan email ini telah dinonaktifkan."
                            is FirebaseAuthInvalidCredentialsException ->
                                "Masukkan format email yang valid."
                            is FirebaseNetworkException ->
                                "Koneksi internet bermasalah. Periksa jaringan Anda."
                            is FirebaseTooManyRequestsException ->
                                "Terlalu banyak permintaan. Coba lagi nanti."
                            else ->
                                "Gagal mengirim email pemulihan. Periksa kembali email Anda."
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
                                email = it.take(254)
                                serverEmailError = null
                                credentialsError = false
                                error = null
                            }
                        },
                        "Email",
                        modifier = Modifier.testTag("username"),
                        errorMessage = emailError ?: if (credentialsError) "" else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                        contentType = ContentType.EmailAddress,
                    )
                    KuyTextField(
                        password,
                        {
                            if (!loading) {
                                password = it.take(128)
                                serverPasswordError = null
                                credentialsError = false
                                error = null
                            }
                        },
                        "Kata Sandi",
                        modifier = Modifier.testTag("password"),
                        errorMessage = passwordError ?: if (credentialsError) "" else null,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        contentType = ContentType.Password,
                        trailingContent = {
                            KuyPasswordVisibilityToggle(
                                visible = passwordVisible,
                                onToggle = { if (!loading) passwordVisible = !passwordVisible },
                                enabled = !loading,
                                modifier = Modifier.testTag("password-toggle"),
                            )
                        },
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        text = "Lupa Kata Sandi?",
                        color = KuyOrange,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .testTag("forgot-password")
                            .clickable(enabled = !loading) {
                                resetEmail = email.trim()
                                resetEmailError = null
                                showForgotPasswordDialog = true
                            },
                    )
                }
                val validationMessage = emailError ?: passwordError ?: error
                if (validationMessage != null) {
                    Text(
                        validationMessage, color = KuyError, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                Spacer(Modifier.height((115 * gapScale).dp))
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
                    SocialButton(R.drawable.login_google, "Google", 37, !loading) {
                        noticeTitle = "Belum tersedia"
                        notice = "Masuk dengan Google belum tersedia."
                    }
                    SocialButton(R.drawable.login_facebook, "Facebook", 36, !loading) {
                        noticeTitle = "Belum tersedia"
                        notice = "Masuk dengan Facebook belum tersedia."
                    }
                    SocialButton(R.drawable.login_instagram, "Instagram", 37, !loading) {
                        noticeTitle = "Belum tersedia"
                        notice = "Masuk dengan Instagram belum tersedia."
                    }
                }
                Spacer(Modifier.height((136 * gapScale).dp))
            }
        }
    }
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!resetLoading) {
                    showForgotPasswordDialog = false
                    resetEmailError = null
                }
            },
            containerColor = KuyNavy,
            titleContentColor = Color.White,
            textContentColor = KuyText,
            title = {
                Text(
                    text = "Lupa Kata Sandi",
                    color = KuyOrange,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Masukkan email akun Anda untuk menerima kode/tautan pemulihan kata sandi.",
                        color = KuyText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    KuyTextField(
                        value = resetEmail,
                        onValueChange = {
                            if (!resetLoading) {
                                resetEmail = it.take(254)
                                resetEmailError = null
                            }
                        },
                        label = "Email",
                        modifier = Modifier.testTag("reset-email"),
                        errorMessage = resetEmailError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitResetPassword() }),
                        contentType = ContentType.EmailAddress,
                    )
                    resetEmailError?.let { errMsg ->
                        Text(
                            text = errMsg,
                            color = KuyError,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = submitResetPassword,
                    enabled = !resetLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KuyButton,
                        contentColor = KuyText,
                    ),
                    modifier = Modifier.testTag("reset-submit"),
                ) {
                    Text(if (resetLoading) "Mengirim..." else "Kirim")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotPasswordDialog = false
                        resetEmailError = null
                    },
                    enabled = !resetLoading,
                    colors = ButtonDefaults.textButtonColors(contentColor = KuyText),
                ) {
                    Text("Batal")
                }
            },
        )
    }
    notice?.let { message ->
        AlertDialog(
            onDismissRequest = { notice = null },
            containerColor = KuyNavy,
            titleContentColor = KuyOrange,
            textContentColor = KuyText,
            title = { Text(noticeTitle, color = KuyOrange, style = MaterialTheme.typography.titleLarge) },
            text = { Text(message, color = KuyText, style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                TextButton(
                    onClick = { notice = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = KuyOrange),
                ) { Text("Mengerti") }
            },
        )
    }
    KuySuccessPopupOverlay(
        visible = showSuccessPopup,
        illustrationRes = R.drawable.masuk_amico,
        subtitle = "proses masuk akun berhasil",
        onContinue = {
            password = ""
            onLoggedIn(loggedInRole)
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
