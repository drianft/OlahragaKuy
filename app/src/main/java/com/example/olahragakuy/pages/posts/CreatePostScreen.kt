package com.example.olahragakuy.pages.posts

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.olahragakuy.pages.HomeBlue
import com.example.olahragakuy.pages.HomeLogoButton
import com.example.olahragakuy.pages.HomeNavy
import com.example.olahragakuy.pages.HomeOrange
import com.example.olahragakuy.pages.HomePlaceholder
import com.example.olahragakuy.pages.HomeText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(onBack: () -> Unit, onPosted: () -> Unit) {
    var showPhotoSheet by remember { mutableStateOf(false) }
    var selectedPhoto by rememberSaveable { mutableStateOf<Uri?>(null) }
    var draft by rememberSaveable { mutableStateOf("") }
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current.applicationContext
    var menyimpan by remember { mutableStateOf(false) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    val userDisplayName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: auth.currentUser?.email?.substringBefore("@")
        ?: "Teman"

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) selectedPhoto = uri
    }

    androidx.activity.compose.BackHandler(enabled = menyimpan) { }
    Scaffold(
        containerColor = HomeNavy,
        contentColor = HomeText,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onBack,
                    enabled = !menyimpan,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF488ACC), contentColor = HomeText),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text("↶", fontSize = 20.sp)
                    Text(" Postingan", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF54CC0C),
                        contentColor = HomeText,
                        disabledContainerColor = Color(0xFF54CC0C).copy(alpha = 0.55f),
                        disabledContentColor = HomeText.copy(alpha = 0.7f),
                    ),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(24.dp),
                    enabled = !menyimpan &&
                            draft.trim().length in 1..2000,
                    onClick = {
                        val caption = draft.trim()
                        menyimpan = true
                        notice = null

                        val photoToUpload = selectedPhoto

                        fun simpanPostingan(uid: String) {
                            val postRef = db.collection("posts").document()

                            fun simpanDokumen(imageUrl: String? = null) {
                                val post = hashMapOf<String, Any>(
                                    "authorId" to uid,
                                    "authorName" to userDisplayName,
                                    "caption" to caption,
                                    "createdAt" to FieldValue.serverTimestamp()
                                )
                                if (imageUrl != null) post["imageUrl"] = imageUrl

                                postRef.set(post)
                                    .addOnSuccessListener {
                                        draft = ""
                                        selectedPhoto = null
                                        menyimpan = false
                                        onPosted()
                                    }
                                    .addOnFailureListener { error ->
                                        menyimpan = false
                                        notice = "Gagal menyimpan postingan: ${error.localizedMessage}"
                                    }
                            }

                            if (photoToUpload == null) {
                                simpanDokumen()
                                return
                            }

                            PostPhotoUploader.upload(
                                context = context,
                                uri = photoToUpload,
                                onSuccess = { url ->
                                    notice = null
                                    simpanDokumen(url)
                                },
                                onError = { message ->
                                    menyimpan = false
                                    notice = message
                                },
                                onWaiting = {
                                    notice = "Upload tertunda. Akan dicoba lagi saat koneksi tersedia."
                                },
                            )
                        }

                        val pengguna = auth.currentUser


                        if (pengguna != null) {
                            simpanPostingan(pengguna.uid)
                        } else {
                            auth.signInAnonymously()
                                .addOnSuccessListener { hasil ->
                                    val uid = hasil.user?.uid

                                    if (uid != null) {
                                        simpanPostingan(uid)
                                    } else {
                                        menyimpan = false
                                        notice = "ID pengguna tidak ditemukan."
                                    }
                                }
                                .addOnFailureListener { error ->
                                    menyimpan = false
                                    notice = "Login gagal: ${error.message}"
                                }
                        }
                    }
                ) {
                    Text(if (menyimpan) "Menyimpan..." else "Posting")
                }
            }
        },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.size(48.dp).background(Color(0xFFFFE35B), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            userDisplayName.take(1).uppercase(),
                            color = HomeNavy,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(userDisplayName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    enabled = !menyimpan,
                    placeholder = {
                        Text("Apa yang anda pikirkan", fontSize = 11.sp)
                    },
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = HomeText),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HomeOrange,
                        unfocusedBorderColor = HomeOrange,
                        focusedTextColor = HomeText,
                        unfocusedTextColor = HomeText,
                        focusedPlaceholderColor = HomeText.copy(alpha = 0.75f),
                        unfocusedPlaceholderColor = HomeText.copy(alpha = 0.75f),
                        cursorColor = HomeOrange,
                    ),
                )
                selectedPhoto?.let { uri ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Foto yang dipilih",
                            color = HomeText,
                            style = MaterialTheme.typography.titleSmall,
                        )

                        AsyncImage(
                            model = uri,
                            contentDescription = "Preview foto postingan",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(HomeBlue),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                enabled = !menyimpan,
                                onClick = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                            ) {
                                Text("Ganti Foto", color = HomeText)
                            }

                            TextButton(
                                enabled = !menyimpan,
                                onClick = { selectedPhoto = null },
                            ) {
                                Text("Hapus", color = HomeOrange)
                            }
                        }

                        Text(
                            "Foto akan diunggah saat kamu menekan Posting.",
                            color = HomeText.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PostActionCard(
                        label = "Tambah\nFoto",
                        photo = true,
                        enabled = !menyimpan,
                        modifier = Modifier.weight(1f),
                    ) {
                        photoPicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                    PostActionCard(
                        label = "Tandai Teman",
                        photo = false,
                        enabled = !menyimpan,
                        modifier = Modifier.weight(1f),
                    ) { notice = "Fitur tandai teman belum tersedia." }
                }
                notice?.let {
                    Text(it, color = HomeText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (showPhotoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoSheet = false },
            containerColor = HomeBlue,
            contentColor = HomeText,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Tambah Foto",
                    style = MaterialTheme.typography.titleLarge,
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        showPhotoSheet = false
                        photoPicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                ) {
                    Text("Pilih dari Galeri")
                }

                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showPhotoSheet = false },
                ) {
                    Text("Batal", color = HomeText)
                }
            }
        }
    }
}

@Composable
private fun PostActionCard(
    label: String,
    photo: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(106.dp),
        shape = RoundedCornerShape(24.dp),
        color = HomeBlue,
        contentColor = HomeText,
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Canvas(Modifier.size(34.dp)) {
                val tint = Color(0xFFB5CFF3)
                if (photo) {
                    drawRoundRect(
                        Color(0xFF85B9CF),
                        topLeft = Offset(size.width * 0.1f, size.height * 0.08f),
                        size = Size(size.width * 0.75f, size.height * 0.66f),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                    drawCircle(tint, size.width * 0.08f, Offset(size.width * 0.64f, size.height * 0.25f))
                    val mountain = Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.62f)
                        lineTo(size.width * 0.4f, size.height * 0.35f)
                        lineTo(size.width * 0.65f, size.height * 0.62f)
                        close()
                    }
                    drawPath(mountain, tint)
                    val center = Offset(size.width * 0.77f, size.height * 0.72f)
                    drawCircle(Color(0xFF55BD4D), size.width * 0.18f, center)
                    drawLine(Color.White, center - Offset(4.dp.toPx(), 0f), center + Offset(4.dp.toPx(), 0f), 2.dp.toPx())
                    drawLine(Color.White, center - Offset(0f, 4.dp.toPx()), center + Offset(0f, 4.dp.toPx()), 2.dp.toPx())
                } else {
                    drawCircle(tint, size.width * 0.2f, Offset(size.width * 0.38f, size.height * 0.24f))
                    drawRoundRect(
                        tint,
                        topLeft = Offset(size.width * 0.05f, size.height * 0.5f),
                        size = Size(size.width * 0.65f, size.height * 0.35f),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                    )
                    val tag = Path().apply {
                        moveTo(size.width * 0.6f, size.height * 0.48f)
                        lineTo(size.width * 0.77f, size.height * 0.48f)
                        lineTo(size.width * 0.98f, size.height * 0.7f)
                        lineTo(size.width * 0.8f, size.height * 0.88f)
                        lineTo(size.width * 0.58f, size.height * 0.66f)
                        close()
                    }
                    drawPath(tag, HomeBlue, style = Stroke(4.dp.toPx()))
                    drawPath(tag, tint)
                    drawCircle(HomeBlue, 1.5.dp.toPx(), Offset(size.width * 0.68f, size.height * 0.57f))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
