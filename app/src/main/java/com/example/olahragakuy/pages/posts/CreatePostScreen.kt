package com.example.olahragakuy.pages.posts

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.R
import com.example.olahragakuy.ui.components.KuySuccessPopupOverlay
import com.example.olahragakuy.ui.components.KuyUserAvatar
import com.example.olahragakuy.ui.components.PostActionCard
import com.example.olahragakuy.ui.components.PostCategoryInputSection
import com.example.olahragakuy.ui.components.SelectedPhotoSection
import com.example.olahragakuy.ui.components.TaggedFriendsRow
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// TODO: ganti ke R.drawable.post_amico setelah amico1.svg diimpor via Vector Asset Studio.
private val PostSuccessIllustration = R.drawable.amico_home

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(onBack: () -> Unit, onPosted: () -> Unit) {
    var showPhotoSheet by remember { mutableStateOf(false) }
    var selectedPhoto by rememberSaveable { mutableStateOf<Uri?>(null) }
    var draft by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current.applicationContext
    var menyimpan by remember { mutableStateOf(false) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    var showTagFriends by rememberSaveable { mutableStateOf(false) }
    var taggedFriends by rememberSaveable(stateSaver = TaggedFriendsSaver) {
        mutableStateOf(emptyList<FollowingFriend>())
    }
    var showSuccessPopup by rememberSaveable { mutableStateOf(false) }
    val userDisplayName = remember(auth.currentUser) { auth.currentUser.resolveDisplayName() }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) selectedPhoto = uri
    }
    val launchPhotoPicker = {
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    BackHandler(enabled = menyimpan) { }
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = HomeNavy,
            contentColor = HomeText,
            topBar = {
                CreatePostTopBar(
                    isSaving = menyimpan,
                    canPost = !menyimpan && draft.trim().length in 1..2000,
                    onBack = onBack,
                    onPostClick = {
                        menyimpan = true
                        notice = null
                        submitNewPost(
                            context = context,
                            auth = auth,
                            db = db,
                            authorName = userDisplayName,
                            caption = draft.trim(),
                            category = category,
                            photoUri = selectedPhoto,
                            taggedFriends = taggedFriends,
                            onWaitingUpload = {
                                notice = "Upload tertunda. Akan dicoba lagi saat koneksi tersedia."
                            },
                            onSuccess = {
                                draft = ""
                                category = ""
                                selectedPhoto = null
                                taggedFriends = emptyList()
                                menyimpan = false
                                showSuccessPopup = true
                            },
                            onError = { message ->
                                menyimpan = false
                                notice = message
                            },
                        )
                    },
                )
            },
        ) { insets ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(insets)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        KuyUserAvatar(name = userDisplayName)
                        Text(userDisplayName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        enabled = !menyimpan,
                        placeholder = {
                            Text("Apa yang anda pikirkan", fontSize = 11.sp)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
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
                    PostCategoryInputSection(
                        category = category,
                        enabled = !menyimpan,
                        onCategoryChange = { category = it },
                        onToggleQuickTag = { tag -> category = toggleCategoryTag(category, tag) },
                    )
                    selectedPhoto?.let { uri ->
                        SelectedPhotoSection(
                            uri = uri,
                            enabled = !menyimpan,
                            onChangePhoto = launchPhotoPicker,
                            onRemovePhoto = { selectedPhoto = null },
                        )
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
                            onClick = launchPhotoPicker,
                        )
                        PostActionCard(
                            label = "Tandai Teman",
                            photo = false,
                            enabled = !menyimpan,
                            modifier = Modifier.weight(1f),
                            onClick = { showTagFriends = true },
                        )
                    }
                    if (taggedFriends.isNotEmpty()) {
                        TaggedFriendsRow(
                            friends = taggedFriends,
                            enabled = !menyimpan,
                            onRemove = { taggedFriends = toggleTaggedFriend(taggedFriends, it) },
                        )
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
                            launchPhotoPicker()
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

        if (showTagFriends) {
            TagFriendsScreen(
                selected = taggedFriends,
                onSelectedChange = { taggedFriends = it },
                onBack = { showTagFriends = false },
            )
        }

        KuySuccessPopupOverlay(
            visible = showSuccessPopup,
            illustrationRes = PostSuccessIllustration,
            subtitle = "Postingan anda berhasil dibuat",
            onContinue = {
                showSuccessPopup = false
                onPosted()
            },
        )
    }
}

@Composable
private fun CreatePostTopBar(
    isSaving: Boolean,
    canPost: Boolean,
    onBack: () -> Unit,
    onPostClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onBack,
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF488ACC),
                contentColor = HomeText,
            ),
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
            enabled = canPost,
            onClick = onPostClick,
        ) {
            Text(if (isSaving) "Menyimpan..." else "Posting")
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun BerhasilPostingPreview() {
    OlahragaKuyTheme {
        Box(Modifier.fillMaxSize().background(HomeNavy)) {
            KuySuccessPopupOverlay(
                visible = true,
                illustrationRes = PostSuccessIllustration,
                subtitle = "Postingan anda berhasil dibuat",
                onContinue = {},
            )
        }
    }
}
