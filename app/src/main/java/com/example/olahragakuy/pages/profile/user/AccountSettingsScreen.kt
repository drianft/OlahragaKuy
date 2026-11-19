package com.example.olahragakuy.pages.profile.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit,
    onChangeEmail: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            }
        )
    }

    Scaffold(
        containerColor = NavyDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tombol Kembali
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButtonPill(onClick = onBack)
            }

            Spacer(Modifier.height(28.dp))

            // Kartu Ganti Email
            SettingsGradientCard(
                icon = Icons.Default.Email,
                title = "Ganti Email",
                onClick = onChangeEmail
            )

            Spacer(Modifier.height(20.dp))

            // Kartu Ganti Password
            SettingsGradientCard(
                icon = Icons.Default.Lock,
                title = "Ganti Password",
                onClick = onChangePassword
            )

            Spacer(Modifier.weight(1f))

            // Tombol Keluar Akun (Merah)
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedLogout)
            ) {
                Text(
                    text = "Keluar Akun",
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsGradientCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(BlueCardStart, BlueCardEnd)
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.width(20.dp))
            Text(
                text = title,
                color = TextWhite,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Preview(name = "Pengaturan akun", showBackground = true)
@Composable
private fun AccountSettingsScreenPreview() {
    AccountSettingsScreen(
        onBack = {},
        onChangeEmail = {},
        onChangePassword = {},
        onLogout = {}
    )
}
