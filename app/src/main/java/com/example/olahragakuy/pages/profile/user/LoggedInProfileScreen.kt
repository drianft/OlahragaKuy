package com.example.olahragakuy.pages.profile.user

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.olahragakuy.ui.components.HomeNavTab
import com.example.olahragakuy.ui.theme.KuyNavy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoggedInProfileScreen(
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onLogout: () -> Unit,
    onTabSelected: (HomeNavTab) -> Unit = {},
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val currentUser = user
    if (currentUser == null) {
        LaunchedEffect(Unit) { onLogout() }
        return
    }

    key(currentUser.uid) {
        val fallbackName = currentUser.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Pengguna"
        var username by remember { mutableStateOf(fallbackName) }
        var displayName by remember { mutableStateOf(fallbackName) }
        var bio by remember { mutableStateOf("") }
        var followers by remember { mutableIntStateOf(0) }
        var following by remember { mutableIntStateOf(0) }
        var userPosts by remember { mutableStateOf<List<UserPost>>(emptyList()) }
        var taggedPosts by remember { mutableStateOf<List<UserPost>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }
        var showError by remember { mutableStateOf(false) }
        var saveError by remember { mutableStateOf<String?>(null) }
        var retry by remember { mutableIntStateOf(0) }

        LaunchedEffect(currentUser.uid) {
            ensureUserProfile(currentUser)
        }

        DisposableEffect(currentUser.uid, retry) {
            loading = true
            val db = FirebaseFirestore.getInstance()
            val userRegistration = db.collection("users")
                .document(currentUser.uid)
                .addSnapshotListener { document, error ->
                    if (auth.currentUser?.uid == currentUser.uid) {
                        if (error != null) {
                            showError = true
                        } else {
                            val loadedUsername = (document?.get("username") as? String)
                                ?.takeIf { it.isNotBlank() } ?: fallbackName
                            username = loadedUsername
                            displayName = (document?.get("displayName") as? String)
                                ?.takeIf { it.isNotBlank() } ?: loadedUsername
                            bio = (document?.get("bio") as? String) ?: ""
                            followers = (document?.get("followersCount") as? Number)?.toInt() ?: 0
                            following = (document?.get("followingCount") as? Number)?.toInt() ?: 0
                            showError = false
                        }
                        loading = false
                    }
                }

            val postsRegistration = db.collection("posts")
                .whereEqualTo("authorId", currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (auth.currentUser?.uid == currentUser.uid && snapshot != null) {
                        userPosts = snapshot.documents.toSortedUserPosts()
                    }
                }

            val taggedRegistration = db.collection("posts")
                .whereArrayContains("taggedFriendIds", currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (auth.currentUser?.uid == currentUser.uid && snapshot != null) {
                        taggedPosts = snapshot.documents.toSortedUserPosts()
                    }
                }

            val followingRegistration = db.collection("users")
                .document(currentUser.uid)
                .collection("following")
                .addSnapshotListener { snapshot, _ ->
                    if (auth.currentUser?.uid == currentUser.uid && snapshot != null) {
                        val subCount = snapshot.size()
                        if (subCount > following) {
                            following = subCount
                        }
                    }
                }

            onDispose {
                userRegistration.remove()
                postsRegistration.remove()
                taggedRegistration.remove()
                followingRegistration.remove()
            }
        }

        if (loading) {
            Scaffold(containerColor = KuyNavy) { padding ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            ProfileScreen(
                initialUsername = username,
                initialDisplayName = displayName,
                initialBio = bio,
                followers = followers,
                following = following,
                feedPosts = userPosts,
                gridPosts = userPosts,
                taggedPosts = taggedPosts,
                onBack = onBack,
                onHomeClick = onHomeClick,
                onTabSelected = onTabSelected,
                onLogout = onLogout,
                onProfileSaved = { updatedUsername, updatedBio ->
                    username = updatedUsername
                    displayName = updatedUsername
                    bio = updatedBio
                    saveUserProfileToFirestore(
                        user = currentUser,
                        username = updatedUsername,
                        bio = updatedBio,
                        onError = { saveError = it },
                    )
                },
            )
        }

        if (showError) {
            AlertDialog(
                onDismissRequest = { showError = false },
                title = { Text("Profil belum dapat dimuat") },
                text = { Text("Data profil terbaru belum tersedia. Nama akun dari Firebase Authentication digunakan jika data profil belum pernah dimuat.") },
                confirmButton = {
                    TextButton(onClick = {
                        showError = false
                        retry++
                    }) { Text("Coba lagi") }
                },
                dismissButton = {
                    TextButton(onClick = { showError = false }) { Text("Tutup") }
                },
            )
        }

        saveError?.let { errMsg ->
            AlertDialog(
                onDismissRequest = { saveError = null },
                title = { Text("Gagal menyimpan profil") },
                text = { Text(errMsg) },
                confirmButton = {
                    TextButton(onClick = { saveError = null }) { Text("Mengerti") }
                },
            )
        }
    }
}
