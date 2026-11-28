package com.example.olahragakuy.pages.profile.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.example.olahragakuy.ui.theme.Poppins

private val ReportSuccessGreen = Color(0xFF4CAF50)
private val ReportSuccessGreenLight = Color(0xFF81C784)

/** Data ringkasan laporan yang baru dikirim untuk ditampilkan pada [ReportSubmittedDialog]. */
data class ReportSubmittedFeedback(
    val reportedUsername: String,
    val reportCategory: String,
)

/**
 * Dialog untuk pengguna melaporkan sebuah postingan ke Firestore (`reports`),
 * sehingga laporan muncul secara real-time di Dashboard Report Admin.
 */
@Composable
fun ReportPostDialog(
    reportedUsername: String,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (category: String, description: String) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf(DefaultAdminReportCategories.first()) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        containerColor = HomeNavy,
        titleContentColor = Color.White,
        textContentColor = HomeText,
        title = {
            Text(
                text = "Laporkan Postingan",
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Pilih alasan pelaporan untuk postingan @$reportedUsername:",
                    color = HomeText.copy(alpha = 0.85f),
                    fontFamily = Poppins,
                    fontSize = 12.5.sp,
                )
                Spacer(Modifier.height(4.dp))

                DefaultAdminReportCategories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !submitting) { selectedCategory = category }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedCategory == category,
                            onClick = { if (!submitting) selectedCategory = category },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = HomeOrange,
                                unselectedColor = HomeText.copy(alpha = 0.7f),
                            ),
                        )
                        Text(
                            text = category,
                            color = HomeText,
                            fontFamily = Poppins,
                            fontSize = 12.5.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 500) description = it },
                    enabled = !submitting,
                    label = {
                        Text(
                            text = "Deskripsi tambahan (opsional)",
                            fontFamily = Poppins,
                            fontSize = 12.sp,
                        )
                    },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HomeOrange,
                        unfocusedBorderColor = HomeBlue,
                        focusedTextColor = HomeText,
                        unfocusedTextColor = HomeText,
                        focusedLabelColor = HomeOrange,
                        unfocusedLabelColor = HomeText.copy(alpha = 0.7f),
                        cursorColor = HomeOrange,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !submitting,
                onClick = { onSubmit(selectedCategory, description.trim()) },
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        color = HomeOrange,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(
                        text = "Kirim Laporan",
                        color = HomeOrange,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !submitting,
                onClick = onDismiss,
            ) {
                Text(
                    text = "Batal",
                    color = HomeText.copy(alpha = 0.8f),
                    fontFamily = Poppins,
                )
            }
        },
    )
}

/**
 * Dialog umpan balik setelah laporan pengguna berhasil dikirim ke Firestore,
 * memberi tahu pelapor bahwa laporannya telah diterima dan akan segera ditindaklanjuti Admin.
 */
@Composable
fun ReportSubmittedDialog(
    reportedUsername: String,
    reportCategory: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            ReportSubmittedCard(
                reportedUsername = reportedUsername,
                reportCategory = reportCategory,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun ReportSubmittedCard(
    reportedUsername: String,
    reportCategory: String,
    onDismiss: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HomeNavy,
        border = BorderStroke(1.dp, HomeBlue),
        shadowElevation = 14.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ReportSuccessGreen.copy(alpha = 0.18f))
                    .border(1.5.dp, ReportSuccessGreen, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Laporan terkirim",
                    tint = ReportSuccessGreenLight,
                    modifier = Modifier.size(34.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Laporan Berhasil Dikirim",
                color = Color.White,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Terima kasih telah membantu menjaga komunitas tetap aman dan suportif. Laporanmu terhadap postingan @$reportedUsername telah diterima dan akan segera ditinjau serta ditindaklanjuti oleh Admin.",
                color = HomeText.copy(alpha = 0.88f),
                fontFamily = Poppins,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
            )

            if (reportCategory.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HomeBlue.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Kategori: $reportCategory",
                        color = HomeText,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeOrange,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
            ) {
                Text(
                    text = "Mengerti",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Preview(name = "Feedback Laporan Terkirim", showBackground = true)
@Composable
private fun ReportSubmittedDialogPreview() {
    OlahragaKuyTheme {
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            ReportSubmittedCard(
                reportedUsername = "BibiOlahraga",
                reportCategory = "Penipuan, penggelapan atau spam",
                onDismiss = {},
            )
        }
    }
}
