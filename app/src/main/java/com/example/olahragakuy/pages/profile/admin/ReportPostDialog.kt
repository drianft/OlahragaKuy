package com.example.olahragakuy.pages.profile.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.Poppins

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
