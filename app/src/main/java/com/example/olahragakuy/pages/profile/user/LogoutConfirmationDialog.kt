package com.example.olahragakuy.pages.profile.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.olahragakuy.R

@Composable
fun LogoutConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = NavyPost,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_logout_illustration),
                        contentDescription = "Ilustrasi Keluar",
                        modifier = Modifier
                            .size(130.dp)
                    )

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Keluar?",
                        color = Color.White,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "apakah anda yakin ingin keluar dari akun?",
                        color = TextWhite,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(26.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Tombol Kembali
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .border(2.dp, OrangeDark, RoundedCornerShape(25.dp)),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GrayLight),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Kembali",
                                color = Color(0xFF8B3A00),
                                fontFamily = Poppins,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // Tombol Yakin
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeDark),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Yakin",
                                color = Color.White,
                                fontFamily = Poppins,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Konfirmasi logout", showBackground = true)
@Composable
private fun LogoutConfirmationDialogPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark),
        contentAlignment = Alignment.Center
    ) {
        LogoutConfirmationDialog(
            onDismiss = {},
            onConfirm = {}
        )
    }
}
