package com.example.olahragakuy.pages.profile.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun EditProfileScreen(
    currentUsername: String,
    currentBio: String,
    onBack: () -> Unit,
    onSave: (username: String, bio: String) -> Unit
) {
    var usernameInput by rememberSaveable { mutableStateOf(currentUsername) }
    var bioInput by rememberSaveable { mutableStateOf(currentBio) }

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

            Spacer(Modifier.height(18.dp))

            // Avatar & Edit Foto
            Box(
                modifier = Modifier
                    .size(125.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFDF80)), // Latar kuning kartun/avatar
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Avatar",
                    tint = OrangeDark,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { /* Ubah Foto */ }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Edit Foto",
                    color = TextWhite,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            // Input Nama Pengguna
            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text("Nama Pengguna", color = TextWhite.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeBright,
                    unfocusedBorderColor = OrangeBright.copy(alpha = 0.6f),
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = NavyDark,
                    unfocusedContainerColor = NavyDark
                ),
                singleLine = true
            )

            Spacer(Modifier.height(20.dp))

            // Input Bio profil
            OutlinedTextField(
                value = bioInput,
                onValueChange = { bioInput = it },
                label = { Text("Bio profil", color = TextWhite.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeBright,
                    unfocusedBorderColor = OrangeBright.copy(alpha = 0.6f),
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = NavyDark,
                    unfocusedContainerColor = NavyDark
                ),
                maxLines = 5
            )

            Spacer(Modifier.weight(1f))

            // Tombol Simpan Perubahan
            Button(
                onClick = { onSave(usernameInput, bioInput) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeDark)
            ) {
                Text(
                    text = "Simpan Perubahan",
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

@Preview(name = "Edit profil", showBackground = true)
@Composable
private fun EditProfileScreenPreview() {
    EditProfileScreen(
        currentUsername = "BibiOlahraga",
        currentBio = "You Can Do it",
        onBack = {},
        onSave = { _, _ -> }
    )
}
