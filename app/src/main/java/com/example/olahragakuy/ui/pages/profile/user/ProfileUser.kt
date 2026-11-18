package com.example.olahragakuy.ui.pages.profile.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.olahragakuy.R

private val NavyDark = Color(0xFF0A1828)
private val NavyPost = Color(0xFF1E3D63)
private val OrangeDark = Color(0xFFC44D00)
private val OrangeBright = Color(0xFFFF6A00)
private val BlueTab = Color(0xFF4A86C8)
private val BlueCardStart = Color(0xFF244872)
private val BlueCardEnd = Color(0xFF142B45)
private val GrayLight = Color(0xFFE6E4E6)
private val TextWhite = Color(0xFFEDEDED)
private val RedLogout = Color(0xFFE53935)
private val Poppins = FontFamily.Default

data class UserPost(
    val id: Int,
    val caption: String,
    val time: String
)

@Composable
fun ProfileScreen(
    initialUsername: String = "BibiOlahraga",
    initialDisplayName: String = "Bibi Olahraga",
    followers: Int = 4,
    following: Int = 5,
    initialBio: String = "You Can Do it",
    initialTab: Int = 0, // 0 = Feed, 1 = Grid, 2 = Ditandai
    feedPosts: List<UserPost> = listOf(
        UserPost(1, "Always on Proces", "2 jam lalu")
    ),
    gridPosts: List<UserPost> = listOf(
        UserPost(1, "Always on Proces", "2 jam lalu")
    ),
    taggedPosts: List<UserPost> = emptyList(),
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onPostClick: (UserPost) -> Unit = {}
) {
    var username by remember { mutableStateOf(initialUsername) }
    var displayName by remember { mutableStateOf(initialDisplayName) }
    var bio by remember { mutableStateOf(initialBio) }

    // Screen State: "profile", "edit_profile", "account_settings"
    var currentSubScreen by remember { mutableStateOf("profile") }
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var selectedNav by remember { mutableIntStateOf(3) }

    val postCount = maxOf(feedPosts.size, gridPosts.size)

    when (currentSubScreen) {
        "edit_profile" -> {
            EditProfileScreen(
                currentUsername = username,
                currentBio = bio,
                onBack = { currentSubScreen = "profile" },
                onSave = { newUsername, newBio ->
                    username = newUsername
                    bio = newBio
                    currentSubScreen = "profile"
                }
            )
        }
        "account_settings" -> {
            AccountSettingsScreen(
                onBack = { currentSubScreen = "profile" },
                onChangeEmail = { /* Aksi ganti email */ },
                onChangePassword = { /* Aksi ganti password */ },
                onLogout = {
                    currentSubScreen = "profile"
                    onLogout()
                }
            )
        }
        else -> {
            Scaffold(
                containerColor = NavyDark,
                bottomBar = { BottomNavBar(selectedNav) { selectedNav = it } }
            ) { padding ->
                when (selectedTab) {
                    // TAB 0: FEED (Postingan Lengkap)
                    0 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) {
                            item {
                                ProfileHeaderSection(
                                    username = username,
                                    displayName = displayName,
                                    postsCount = postCount,
                                    followers = followers,
                                    following = following,
                                    bio = bio,
                                    selectedTab = selectedTab,
                                    onTabSelected = { selectedTab = it },
                                    onBack = onBack,
                                    onMenu = { currentSubScreen = "account_settings" },
                                    onEditProfile = { currentSubScreen = "edit_profile" },
                                    onShareProfile = {}
                                )
                            }

                            items(feedPosts, key = { it.id }) { post ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(NavyPost)
                                        .padding(horizontal = 20.dp, vertical = 16.dp)
                                ) {
                                    PostItem(
                                        username = username,
                                        time = post.time,
                                        caption = post.caption,
                                        onClick = { onPostClick(post) }
                                    )
                                }
                            }
                        }
                    }

                    // TAB 1: GRID (Kotak 3 Kolom)
                    1 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ProfileHeaderSection(
                                    username = username,
                                    displayName = displayName,
                                    postsCount = postCount,
                                    followers = followers,
                                    following = following,
                                    bio = bio,
                                    selectedTab = selectedTab,
                                    onTabSelected = { selectedTab = it },
                                    onBack = onBack,
                                    onMenu = { currentSubScreen = "account_settings" },
                                    onEditProfile = { currentSubScreen = "edit_profile" },
                                    onShareProfile = {}
                                )
                            }

                            items(gridPosts, key = { it.id }) { post ->
                                val colIndex = gridPosts.indexOf(post) % 3
                                Box(
                                    modifier = Modifier
                                        .padding(
                                            start = if (colIndex == 0) 20.dp else 0.dp,
                                            end = if (colIndex == 2) 20.dp else 0.dp
                                        )
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(NavyPost)
                                        .clickable {
                                            onPostClick(post)
                                            selectedTab = 0
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = post.caption,
                                        tint = OrangeBright,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }
                    }

                    // TAB 2: DITANDAI
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ProfileHeaderSection(
                                    username = username,
                                    displayName = displayName,
                                    postsCount = postCount,
                                    followers = followers,
                                    following = following,
                                    bio = bio,
                                    selectedTab = selectedTab,
                                    onTabSelected = { selectedTab = it },
                                    onBack = onBack,
                                    onMenu = { currentSubScreen = "account_settings" },
                                    onEditProfile = { currentSubScreen = "edit_profile" },
                                    onShareProfile = {}
                                )
                            }

                            if (taggedPosts.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Belum ada postingan yang ditandai",
                                            color = TextWhite.copy(alpha = 0.6f),
                                            fontFamily = Poppins,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            } else {
                                items(taggedPosts, key = { it.id }) { post ->
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(NavyPost)
                                        .clickable { onPostClick(post) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FitnessCenter,
                                            contentDescription = post.caption,
                                            tint = OrangeBright,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Halaman 1: Edit Foto, Nama Pengguna, dan Bio profil
 * Sesuai gambar referensi sisi kiri
 */
@Composable
fun EditProfileScreen(
    currentUsername: String,
    currentBio: String,
    onBack: () -> Unit,
    onSave: (username: String, bio: String) -> Unit
) {
    var usernameInput by remember { mutableStateOf(currentUsername) }
    var bioInput by remember { mutableStateOf(currentBio) }

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

/**
 * Halaman 2: Ganti Email, Ganti Password, dan Keluar Akun
 * Sesuai gambar referensi sisi kanan
 */
@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit,
    onChangeEmail: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

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

/**
 * Pop-up Konfirmasi Keluar Akun
 * Sesuai gambar dialog referensi
 */
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

@Composable
private fun BackButtonPill(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(BlueTab)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = "Kembali",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Kembali",
                color = Color.White,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
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

@Composable
private fun ProfileHeaderSection(
    username: String,
    displayName: String,
    postsCount: Int,
    followers: Int,
    following: Int,
    bio: String,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    onEditProfile: () -> Unit,
    onShareProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyDark)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(displayName, onBack, onMenu)
        Spacer(Modifier.height(16.dp))

        ProfileAvatar(size = 140.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "@$username",
            color = TextWhite,
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp
        )

        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(postsCount.toString(), "Postingan")
            StatItem(followers.toString(), "Pengikut")
            StatItem(following.toString(), "Mengikuti")
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Button(
                onClick = onEditProfile,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeDark)
            ) {
                Text(
                    text = "Edit Profil",
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
            Button(
                onClick = onShareProfile,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .border(2.dp, OrangeBright, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GrayLight)
            ) {
                Text(
                    text = "Bagikan Profil",
                    color = OrangeBright,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = bio,
            color = TextWhite,
            fontFamily = Poppins,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        // Tab Bar (Feed | Grid | Ditandai)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProfileTab(
                icon = Icons.Default.Image,
                selected = selectedTab == 0,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(0) }
            )
            ProfileTab(
                icon = Icons.Default.GridView,
                selected = selectedTab == 1,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(1) }
            )
            ProfileTab(
                icon = Icons.Default.PersonPin,
                selected = selectedTab == 2,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(2) }
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit, onMenu: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = "Kembali",
            tint = TextWhite,
            modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onBack)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = TextWhite,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Menu",
            tint = TextWhite,
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onMenu)
        )
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = OrangeBright,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 44.sp
        )
        Text(
            text = label,
            color = TextWhite,
            fontFamily = Poppins,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun ProfileTab(
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(if (selected) BlueTab else GrayLight)
            .then(if (selected) Modifier.border(2.dp, GrayLight, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) GrayLight else BlueTab,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun ProfileAvatar(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(BlueTab),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Foto profil",
            tint = TextWhite,
            modifier = Modifier.size(size * 0.58f)
        )
    }
}

@Composable
private fun PostItem(
    username: String,
    time: String,
    caption: String,
    onClick: () -> Unit = {}
) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(size = 64.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    color = TextWhite,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = time,
                    color = OrangeBright,
                    fontFamily = Poppins,
                    fontSize = 15.sp
                )
            }
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opsi",
                tint = TextWhite,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = caption,
            color = TextWhite,
            fontFamily = Poppins,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(NavyDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = OrangeBright,
                modifier = Modifier.size(72.dp)
            )
        }
    }
}

@Composable
private fun BottomNavBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Icons.Default.Forum,
        Icons.Default.Place,
        Icons.Default.FitnessCenter,
        Icons.Default.Person
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(NavyDark)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, icon ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .size(if (active) 76.dp else 56.dp)
                    .clip(CircleShape)
                    .background(if (active) OrangeDark else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (active) Color.White else OrangeBright,
                    modifier = Modifier.size(if (active) 38.dp else 34.dp)
                )
            }
        }
    }
}

// ---------------- PREVIEWS ----------------

@Preview(name = "1. Tampilan Feed", showBackground = true, showSystemUi = true)
@Composable
private fun ProfileFeedPreview() {
    ProfileScreen(initialTab = 0)
}

@Preview(name = "2. Tampilan Grid", showBackground = true, showSystemUi = true)
@Composable
private fun ProfileGridPreview() {
    ProfileScreen(initialTab = 1)
}

@Preview(name = "3. Edit Profil (Sisi Kiri Gambar)", showBackground = true, showSystemUi = true)
@Composable
private fun EditProfileScreenPreview() {
    EditProfileScreen(
        currentUsername = "BibiOlahraga",
        currentBio = "You Can Do it",
        onBack = {},
        onSave = { _, _ -> }
    )
}

@Preview(name = "4. Pengaturan Akun (Sisi Kanan Gambar)", showBackground = true, showSystemUi = true)
@Composable
private fun AccountSettingsScreenPreview() {
    AccountSettingsScreen(
        onBack = {},
        onChangeEmail = {},
        onChangePassword = {},
        onLogout = {}
    )
}

@Preview(name = "5. Pop-up Keluar Akun", showBackground = true)
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
