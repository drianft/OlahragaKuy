package com.example.olahragakuy.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.R
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.google.firebase.auth.FirebaseAuth

private val HomeNavy = Color(0xFF0A1A2C)
private val HomeBlue = Color(0xFF203F60)
private val HomeOrange = Color(0xFFFF6B00)
private val HomeText = Color(0xFFE9EDF5)

@Composable
fun HomeScreen() {
    var draft by rememberSaveable { mutableStateOf("") }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    val currentUser = remember {
        try { FirebaseAuth.getInstance().currentUser }
        catch (_: Exception) { null }
    }
    val userDisplayName = currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser?.email?.substringBefore("@")
        ?: "Teman"

    Scaffold(
        containerColor = HomeNavy,
        contentColor = HomeText,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painterResource(R.drawable.login_wordmark), "OlahragaKUY",
                    Modifier.weight(1f).height(28.dp),
                    alignment = Alignment.CenterStart,
                )
                HomeLogoButton("Cari") { notice = "Fitur pencarian belum tersedia." }
                HomeLogoButton("Notifikasi") { notice = "Belum ada notifikasi." }
                HomeLogoButton("Chat") { notice = "Fitur chat belum tersedia." }
            }
        },
        bottomBar = {
            Surface(color = HomeNavy, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    listOf("Komunitas", "Lokasi", "Latihan", "Profil").forEachIndexed { index, label ->
                        TextButton(
                            onClick = {
                                if (index == 3 && currentUser != null) {
                                    notice = "Masuk sebagai ${currentUser.email ?: userDisplayName}"
                                } else if (index != 0) {
                                    notice = "Halaman $label belum tersedia."
                                }
                            },
                            modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 64.dp)
                                .background(if (index == 0) Color(0xFFBB4D00) else Color.Transparent, CircleShape),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (index == 0) HomeText else HomeOrange,
                            ),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Isi logo", fontSize = 10.sp)
                                Text(label, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        },
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                StreakBanner(onClick = { notice = "Fitur streak belum tersedia." })
            }
            item {
                Row(
                    Modifier.fillMaxWidth().background(HomeBlue).padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HomePlaceholder("Isi avatar\ndi sini", Modifier.size(40.dp), CircleShape)
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text("Apa yang anda pikirkan, $userDisplayName?", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HomeOrange,
                            unfocusedBorderColor = HomeOrange,
                            focusedTextColor = HomeText,
                            unfocusedTextColor = HomeText,
                            unfocusedPlaceholderColor = HomeText.copy(alpha = 0.75f),
                        ),
                    )
                    HomeLogoButton("Foto +") { notice = "Unggah foto belum tersedia." }
                }
            }
            item {
                HomePost(
                    name = "Pecinta Jogging", time = "2 hari lalu",
                    caption = "Ayoo jooging guysss", initialLikes = 57,
                    onAction = { notice = it },
                )
            }
            item {
                HomePost(
                    name = "Monster basket", time = "3 hari lalu",
                    caption = "Yuk olahraga bareng!", initialLikes = 32,
                    onAction = { notice = it },
                )
            }
        }
    }

    notice?.let { message ->
        AlertDialog(
            onDismissRequest = { notice = null },
            title = { Text("Informasi") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { notice = null }) { Text("Mengerti") } },
        )
    }
}

@Composable
private fun StreakBanner(onClick: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            .fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFA183), HomeOrange)))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Pertahankan Streak anda!",
                color = Color(0xFF713000), fontWeight = FontWeight.Bold, fontSize = 16.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔥", fontSize = 30.sp)
                Text("30", color = HomeText, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF8C3C00)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFE9E9E9), contentColor = Color(0xFF8C3C00),
                ),
            ) { Text("Pergi") }
        }
        Image(
            painterResource(R.drawable.login_wordmark), "Ilustrasi olahraga bersama",
            Modifier.width(100.dp).height(120.dp),
        )
    }
}

@Composable
private fun HomePost(
    name: String,
    time: String,
    caption: String,
    initialLikes: Int,
    onAction: (String) -> Unit,
) {
    var liked by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().background(HomeBlue).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomePlaceholder("Isi avatar\ndi sini", Modifier.size(54.dp), CircleShape)
            Column(Modifier.weight(1f)) {
                Text(name, color = HomeText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(time, color = HomeOrange, fontSize = 12.sp)
            }
            TextButton(onClick = { onAction("Menu postingan belum tersedia.") }) {
                Text("⋮", color = HomeText, fontSize = 28.sp)
            }
        }
        Text(caption, color = HomeText, fontSize = 14.sp)
        // Ganti placeholder ini dengan Image(painterResource(R.drawable.nama_foto), ...).
        HomePlaceholder(
            "Isi foto postingan di sini",
            Modifier.fillMaxWidth().aspectRatio(1.28f),
            RoundedCornerShape(24.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { liked = !liked }) {
                Text(
                    "${if (liked) "♥" else "♡"}  ${initialLikes + if (liked) 1 else 0}",
                    color = if (liked) HomeOrange else HomeText, fontSize = 20.sp,
                )
            }
            TextButton(onClick = { onAction("Fitur komentar belum tersedia.") }) {
                Text("Komentar  4", color = HomeText, fontSize = 12.sp)
            }
            TextButton(onClick = { onAction("Fitur bagikan belum tersedia.") }) {
                Text("Bagikan  8", color = HomeText, fontSize = 12.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomePlaceholder("Logo", Modifier.size(24.dp), CircleShape)
            Text("disukai oleh ", color = HomeText, fontSize = 11.sp)
            Text("Monster Basket", color = HomeText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HomeLogoButton(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(2.dp),
        modifier = Modifier.width(48.dp).heightIn(min = 48.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Isi logo", color = Color(0xFF5094D5), fontSize = 9.sp)
            Text(label, color = Color(0xFF5094D5), fontSize = 9.sp)
        }
    }
}

@Composable
private fun HomePlaceholder(
    label: String,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
) {
    Box(
        modifier.clip(shape).background(Color(0xFF315575))
            .border(1.dp, Color(0xFF6587A4), shape).padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = HomeText.copy(alpha = 0.8f), fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeScreenPreview() {
    OlahragaKuyTheme { HomeScreen() }
}
