package com.example.olahragakuy.pages.profile.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.pages.profile.user.LogoutConfirmationDialog
import com.example.olahragakuy.ui.components.KuyBackButton
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.example.olahragakuy.ui.theme.Poppins

private val AdminSettingsCardStart = Color(0xFF244872)
private val AdminSettingsCardEnd = Color(0xFF142B45)
private val AdminLogoutRed = Color(0xFFE53935)

/**
 * Halaman Pengaturan / Edit Akun khusus Admin (`Admin - ProfileEdit`).
 * Serupa dengan halaman pengaturan akun pengguna biasa, dengan tambahan menu
 * **History Action** yang menampilkan riwayat aksi moderasi seluruh akun admin.
 */
@Composable
fun AdminProfileEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onChangeEmail: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onHistoryAction: (() -> Unit)? = null,
    onLogout: () -> Unit = {},
) {
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showLocalHistory by rememberSaveable { mutableStateOf(false) }

    if (showLocalHistory && onHistoryAction == null) {
        AdminHistoryActionScreen(
            onBack = { showLocalHistory = false },
            modifier = modifier,
        )
        return
    }

    BackHandler {
        if (showLogoutDialog) {
            showLogoutDialog = false
        } else {
            onBack()
        }
    }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = HomeNavy,
        contentColor = HomeText,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KuyBackButton(onClick = onBack)
            }

            Spacer(Modifier.height(28.dp))

            AdminSettingsGradientCard(
                icon = Icons.Default.Email,
                title = "Ganti Email",
                onClick = onChangeEmail,
            )

            Spacer(Modifier.height(20.dp))

            AdminSettingsGradientCard(
                icon = Icons.Default.Lock,
                title = "Ganti Password",
                onClick = onChangePassword,
            )

            Spacer(Modifier.height(20.dp))

            AdminSettingsGradientCard(
                icon = Icons.Default.History,
                title = "History Action",
                onClick = {
                    if (onHistoryAction != null) {
                        onHistoryAction()
                    } else {
                        showLocalHistory = true
                    }
                },
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminLogoutRed),
            ) {
                Text(
                    text = "Keluar Akun",
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AdminSettingsGradientCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AdminSettingsCardStart, AdminSettingsCardEnd),
                ),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.width(20.dp))
            Text(
                text = title,
                color = HomeText,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}

@Preview(name = "Admin - ProfileEdit", showBackground = true, showSystemUi = true)
@Composable
private fun AdminProfileEditPreview() {
    OlahragaKuyTheme {
        AdminProfileEditScreen(onBack = {})
    }
}
