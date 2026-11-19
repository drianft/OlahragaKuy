package com.example.olahragakuy.pages.posts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.R
import com.example.olahragakuy.ui.components.KuyBackButton
import com.example.olahragakuy.ui.components.KuyPostSearchBar
import com.example.olahragakuy.ui.components.KuyUserAvatar
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyButton
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ChatTab(val label: String) {
    ORANG("Orang"),
    KOMUNITAS("Komunitas"),
}

data class VenueInviteUiModel(
    val title: String,
    val sport: String,
    val description: String,
)

data class ChatMessageUiModel(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timeText: String,
    val isFromMe: Boolean,
    val venueInvite: VenueInviteUiModel? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

data class ChatRoomUiModel(
    val id: String,
    val name: String,
    val isGroup: Boolean,
    val statusOrMembers: String,
    val lastMessage: String,
    val dateLabel: String,
    val avatarColorHex: Long = 0xFF5B7CFAL,
    val participantIds: List<String> = emptyList(),
    val participantNames: List<String> = emptyList(),
)

private val IncomingBubbleBlue = Color(0xFF4C8EDA)
private val OutgoingBubbleOrange = Color(0xFFC44D00)
private val UnselectedTabBg = Color(0xFFEAEAEA)
private val InputBarNavy = Color(0xFF0B1F35)

private val DefaultChatRooms = listOf(
    ChatRoomUiModel(
        id = "dm_kenangan",
        name = "KenanganTerindah",
        isGroup = false,
        statusOrMembers = "aktif 1 jam lalu",
        lastMessage = "Ayok olahraga bareng temannn",
        dateLabel = "4/11",
        avatarColorHex = 0xFF6B8FF7L,
        participantNames = listOf("KenanganTerindah"),
    ),
    ChatRoomUiModel(
        id = "dm_monster",
        name = "Monster Basket",
        isGroup = false,
        statusOrMembers = "aktif 2 jam lalu",
        lastMessage = "Anda mengirim ajakan olahraga bersama",
        dateLabel = "4/11",
        avatarColorHex = 0xFF59C39CL,
        participantNames = listOf("Monster Basket"),
    ),
    ChatRoomUiModel(
        id = "group_gymclub",
        name = "GymClub",
        isGroup = true,
        statusOrMembers = "Anda,Monster Basket,KenanganTerindah",
        lastMessage = "Monster Basket: Bosann",
        dateLabel = "4/11",
        avatarColorHex = 0xFFE05A47L,
        participantNames = listOf("Anda", "Monster Basket", "KenanganTerindah"),
    ),
)

private val DefaultRoomMessages: Map<String, List<ChatMessageUiModel>> = mapOf(
    "dm_monster" to listOf(
        ChatMessageUiModel(
            id = "m1",
            senderId = "u_monster",
            senderName = "Monster Basket",
            text = "Bro nanti inali olahraga bareng dong , kepengen banget main basket gue",
            timeText = "11:20",
            isFromMe = false,
            createdAtMillis = 1_000L,
        ),
        ChatMessageUiModel(
            id = "m2",
            senderId = "me",
            senderName = "Anda",
            text = "Kapan kapan gas bro",
            timeText = "11:21",
            isFromMe = true,
            createdAtMillis = 2_000L,
        ),
        ChatMessageUiModel(
            id = "m3",
            senderId = "u_monster",
            senderName = "Monster Basket",
            text = "Oke broo",
            timeText = "11:22",
            isFromMe = false,
            createdAtMillis = 3_000L,
        ),
        ChatMessageUiModel(
            id = "m4",
            senderId = "me",
            senderName = "Anda",
            text = "Ayo bro",
            timeText = "11:23",
            isFromMe = true,
            createdAtMillis = 4_000L,
        ),
        ChatMessageUiModel(
            id = "m5",
            senderId = "me",
            senderName = "Anda",
            text = "",
            timeText = "11:24",
            isFromMe = true,
            venueInvite = VenueInviteUiModel(
                title = "GOR Cikal USU",
                sport = "Basket",
                description = "Lapangan Basket mahasiswa\nUniversitas Sumatera Utara",
            ),
            createdAtMillis = 5_000L,
        ),
    ),
    "dm_kenangan" to listOf(
        ChatMessageUiModel(
            id = "k1",
            senderId = "u_kenangan",
            senderName = "KenanganTerindah",
            text = "Temannn",
            timeText = "11:20",
            isFromMe = false,
            createdAtMillis = 1_000L,
        ),
        ChatMessageUiModel(
            id = "k2",
            senderId = "me",
            senderName = "Anda",
            text = "Kenapa bro kuuuuuu",
            timeText = "11:21",
            isFromMe = true,
            createdAtMillis = 2_000L,
        ),
        ChatMessageUiModel(
            id = "k3",
            senderId = "u_kenangan",
            senderName = "KenanganTerindah",
            text = "Ayok olahraga bareng temannn",
            timeText = "11:22",
            isFromMe = false,
            createdAtMillis = 3_000L,
        ),
    ),
    "group_gymclub" to listOf(
        ChatMessageUiModel(
            id = "g1",
            senderId = "u_monster",
            senderName = "Monster Basket",
            text = "Siapa mau gym soree",
            timeText = "11:20",
            isFromMe = false,
            createdAtMillis = 1_000L,
        ),
        ChatMessageUiModel(
            id = "g2",
            senderId = "me",
            senderName = "Anda",
            text = "Aku Baju Gym",
            timeText = "11:21",
            isFromMe = true,
            createdAtMillis = 2_000L,
        ),
        ChatMessageUiModel(
            id = "g3",
            senderId = "u_monster",
            senderName = "Monster Basket",
            text = "Ayok la gym bareng plizzzzzzz",
            timeText = "11:22",
            isFromMe = false,
            createdAtMillis = 3_000L,
        ),
        ChatMessageUiModel(
            id = "g4",
            senderId = "u_kenangan",
            senderName = "KenanganTerindah",
            text = "Baskett ajaa la ayokkkk",
            timeText = "11:23",
            isFromMe = false,
            createdAtMillis = 4_000L,
        ),
        ChatMessageUiModel(
            id = "g5",
            senderId = "u_monster",
            senderName = "Monster Basket",
            text = "Bosann",
            timeText = "11:24",
            isFromMe = false,
            createdAtMillis = 5_000L,
        ),
    ),
)

/** Menyimpan ruang obrolan dan pesan sesi aktif (kosong saat runtime, hanya diisi otomatis oleh data Firebase / aksi pengguna). */
val LocalChatRooms = mutableStateListOf<ChatRoomUiModel>()
val LocalChatMessages = mutableStateMapOf<String, List<ChatMessageUiModel>>()

/** Membentuk ID ruang obrolan personal (DM) deterministik antara dua akun Firebase. */
fun directChatRoomId(uidA: String, uidB: String): String {
    val cleanA = uidA.trim().ifEmpty { "anon" }
    val cleanB = uidB.trim().ifEmpty { "friend" }
    val (first, second) = if (cleanA <= cleanB) cleanA to cleanB else cleanB to cleanA
    return "dm_${first}_$second"
}

/** Menyiapkan ruang DM antara pengguna aktif dan target agar langsung terbuka dan masuk ke log chat. */
fun ensureDirectChatRoom(
    currentUserId: String,
    currentUserName: String,
    targetUid: String,
    targetName: String,
): ChatRoomUiModel {
    val cleanTargetName = targetName.trim().ifEmpty { "Teman" }
    val dmId = directChatRoomId(currentUserId, targetUid)
    val existing = LocalChatRooms.firstOrNull { !it.isGroup && it.id == dmId }
    if (existing != null) return existing
    val created = ChatRoomUiModel(
        id = dmId,
        name = cleanTargetName,
        isGroup = false,
        statusOrMembers = "Aktif",
        lastMessage = "Ketuk untuk mulai chat dengan $cleanTargetName",
        dateLabel = "",
        avatarColorHex = 0xFF5B7CFAL,
        participantIds = listOf(currentUserId, targetUid).filter { it.isNotBlank() }.distinct(),
        participantNames = listOf(currentUserName, cleanTargetName).filter { it.isNotBlank() }.distinct(),
    )
    LocalChatRooms.add(0, created)
    return created
}

/** Menentukan judul ruang obrolan dari sudut pandang akun yang sedang login. */
fun resolveChatRoomDisplayName(
    fallbackName: String,
    isGroup: Boolean,
    participantNames: List<String>,
    currentUserName: String,
): String {
    if (isGroup) return fallbackName
    return participantNames.firstOrNull { name ->
        name.isNotBlank() &&
            !name.equals(currentUserName, ignoreCase = true) &&
            !name.equals("Anda", ignoreCase = true)
    } ?: fallbackName
}

/** Menggabungkan daftar ruang obrolan dengan daftar akun Firebase yang di-follow agar setiap akun bisa langsung di-chat. */
fun mergeChatRoomsWithFollowing(
    existingRooms: List<ChatRoomUiModel>,
    followingFriends: List<FollowingFriend>,
    currentUserId: String = "me",
    currentUserName: String = "Anda",
): List<ChatRoomUiModel> {
    val result = existingRooms.toMutableList()
    val palette = listOf(0xFF5B7CFAL, 0xFF4FC3A1L, 0xFFF2B84BL, 0xFF7FA7E8L)
    followingFriends.forEachIndexed { index, friend ->
        val dmId = directChatRoomId(currentUserId, friend.uid)
        val alreadyExists = result.any { room ->
            !room.isGroup && (
                room.id == dmId ||
                    room.name.equals(friend.displayName, ignoreCase = true) ||
                    friend.uid in room.participantIds
            )
        }
        if (!alreadyExists) {
            result.add(
                ChatRoomUiModel(
                    id = dmId,
                    name = friend.displayName,
                    isGroup = false,
                    statusOrMembers = "Akun diikuti",
                    lastMessage = "Ketuk untuk mulai chat dengan ${friend.displayName}",
                    dateLabel = "",
                    avatarColorHex = palette[index % palette.size],
                    participantIds = listOf(currentUserId, friend.uid).distinct(),
                    participantNames = listOf(currentUserName, friend.displayName).distinct(),
                ),
            )
        }
    }
    return result
}

/** Memfilter daftar ruang obrolan berdasarkan tab (`Orang` vs `Komunitas`) dan kata kunci pencarian. */
fun filterChatRooms(
    rooms: List<ChatRoomUiModel>,
    tab: ChatTab,
    query: String,
): List<ChatRoomUiModel> {
    val q = query.trim().lowercase()
    return rooms.filter { room ->
        val matchesTab = if (tab == ChatTab.KOMUNITAS) room.isGroup else !room.isGroup
        val matchesQuery = q.isEmpty() ||
            room.name.lowercase().contains(q) ||
            room.lastMessage.lowercase().contains(q) ||
            room.statusOrMembers.lowercase().contains(q)
        matchesTab && matchesQuery
    }
}

/** Membuat objek grup komunitas baru bersama teman-teman Firebase yang dipilih. */
fun createGroupChatRoom(
    groupName: String,
    selectedFriends: List<FollowingFriend>,
    creatorId: String = "me",
    creatorName: String = "Anda",
    dateLabel: String = "Baru",
): ChatRoomUiModel {
    val cleanName = groupName.trim().ifEmpty { "Grup Olahraga" }
    val displayMembers = listOf("Anda") + selectedFriends.map { it.displayName }
    val storedMemberNames = (listOf(creatorName) + selectedFriends.map { it.displayName }).distinct()
    val storedMemberIds = (listOf(creatorId) + selectedFriends.map { it.uid }).filter { it.isNotBlank() }.distinct()
    val roomId = "group_${cleanName.lowercase().replace(Regex("[^a-z0-9]+"), "_")}_${System.currentTimeMillis()}"
    return ChatRoomUiModel(
        id = roomId,
        name = cleanName,
        isGroup = true,
        statusOrMembers = displayMembers.joinToString(","),
        lastMessage = "$creatorName membuat grup $cleanName",
        dateLabel = dateLabel,
        avatarColorHex = 0xFFE05A47L,
        participantIds = storedMemberIds,
        participantNames = storedMemberNames,
    )
}

/**
 * Halaman Pesan & Detail Pesan (`PagePesan`, `PagePesan2`, & `PageDetailPesan`):
 * - Tab `Orang`: menampilkan percakapan personal dengan akun-akun nyata di Firebase (`users/{uid}/following`, `users`, `posts`).
 * - Tab `Komunitas`: menampilkan grup komunitas olahraga serta tombol pembuatan grup baru dengan akun teman di Firebase.
 * - `PageDetailPesan`: ruang obrolan personal maupun grup secara *real-time* di Firestore.
 */
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: ChatTab = ChatTab.ORANG,
    initialSelectedRoomId: String? = null,
    initialDirectRoom: ChatRoomUiModel? = null,
    exitOnDetailBack: Boolean = false,
) {
    val isPreview = LocalInspectionMode.current
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedRoomId by rememberSaveable { mutableStateOf(initialSelectedRoomId ?: initialDirectRoom?.id) }
    var showCreateGroupDialog by rememberSaveable { mutableStateOf(false) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }

    val previewFollowing = remember {
        listOf(
            FollowingFriend("u_kenangan", "KenanganTerindah"),
            FollowingFriend("u_monster", "Monster Basket"),
        )
    }
    var followingFriends by remember {
        mutableStateOf(if (isPreview) previewFollowing else emptyList())
    }
    var remoteRooms by remember {
        mutableStateOf(if (isPreview) DefaultChatRooms else emptyList())
    }

    val currentUser = remember { currentUserOrNull() }
    val currentUserId = currentUser?.uid ?: "me"
    val currentUserName = remember(currentUser) { currentUser.resolveDisplayName("Anda") }

    if (!isPreview) {
        LaunchedEffect(currentUser?.uid) {
            syncCurrentUserToFirestore()
            loadFirebaseFriends(
                uid = currentUser?.uid,
                currentUserName = currentUserName,
                onResult = { loaded ->
                    followingFriends = loaded
                },
            )
        }

        // Dengarkan koleksi chats di Firestore untuk akun yang sedang login
        DisposableEffect(currentUser?.uid, currentUserName) {
            var registration: ListenerRegistration? = null
            try {
                registration = FirebaseFirestore.getInstance()
                    .collection("chats")
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .limit(50)
                    .addSnapshotListener { snap, error ->
                        if (error == null && snap != null) {
                            remoteRooms = snap.documents.mapNotNull { doc ->
                                val data = doc.data.orEmpty()
                                val rawName = (data["name"] as? String)?.takeIf { it.isNotBlank() }
                                    ?: return@mapNotNull null
                                val isGroup = (data["isGroup"] as? Boolean) ?: false
                                val memberIds = (data["participantIds"] as? List<*>)
                                    .orEmpty()
                                    .mapNotNull { it as? String }
                                val memberNames = (data["participantNames"] as? List<*>)
                                    .orEmpty()
                                    .mapNotNull { it as? String }

                                // Tampilkan room jika pengguna termasuk partisipan atau jika room publik/komunitas
                                val isParticipant = memberIds.isEmpty() ||
                                    currentUserId in memberIds ||
                                    memberNames.any { it.equals(currentUserName, ignoreCase = true) }
                                if (!isParticipant) return@mapNotNull null

                                val displayName = resolveChatRoomDisplayName(
                                    fallbackName = rawName,
                                    isGroup = isGroup,
                                    participantNames = memberNames,
                                    currentUserName = currentUserName,
                                )
                                val statusOrMembers = if (isGroup) {
                                    val others = memberNames.filterNot {
                                        it.equals(currentUserName, ignoreCase = true) ||
                                            it.equals("Anda", ignoreCase = true)
                                    }
                                    (listOf("Anda") + others).joinToString(",")
                                } else {
                                    (data["statusOrMembers"] as? String) ?: "Akun diikuti"
                                }
                                val lastMsg = (data["lastMessage"] as? String).orEmpty()
                                val dateStr = (data["dateLabel"] as? String).orEmpty()
                                ChatRoomUiModel(
                                    id = doc.id,
                                    name = displayName,
                                    isGroup = isGroup,
                                    statusOrMembers = statusOrMembers,
                                    lastMessage = lastMsg,
                                    dateLabel = dateStr,
                                    avatarColorHex = if (isGroup) 0xFFE05A47L else 0xFF5B7CFAL,
                                    participantIds = memberIds,
                                    participantNames = memberNames,
                                )
                            }
                        }
                    }
            } catch (_: Exception) {
            }
            onDispose { registration?.remove() }
        }
    }

    val allRooms = remember(isPreview, LocalChatRooms.toList(), remoteRooms, followingFriends, currentUserId, currentUserName, initialDirectRoom) {
        if (isPreview) {
            DefaultChatRooms
        } else {
            val combined = (remoteRooms + LocalChatRooms.toList() + listOfNotNull(initialDirectRoom)).distinctBy { it.id }
            mergeChatRoomsWithFollowing(
                existingRooms = combined,
                followingFriends = followingFriends,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
            )
        }
    }

    val activeRoom = remember(allRooms, selectedRoomId, initialDirectRoom) {
        allRooms.firstOrNull { it.id == selectedRoomId }
            ?: initialDirectRoom?.takeIf { it.id == selectedRoomId }
    }

    val handleDetailBack: () -> Unit = {
        if (exitOnDetailBack) onBack() else selectedRoomId = null
    }

    BackHandler(enabled = selectedRoomId != null && !exitOnDetailBack) {
        selectedRoomId = null
    }

    if (activeRoom != null) {
        ChatDetailContent(
            room = activeRoom,
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            onBack = handleDetailBack,
            onCallClick = { notice = "Fitur panggilan suara segera hadir." },
            onVenueDetailClick = { venueTitle ->
                notice = "Membuka detail lokasi $venueTitle."
            },
            modifier = modifier,
        )
    } else {
        ChatListContent(
            rooms = filterChatRooms(allRooms, selectedTab, searchQuery),
            followingFriends = followingFriends,
            selectedTab = selectedTab,
            onTabChange = { selectedTab = it },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onRoomClick = { room -> selectedRoomId = room.id },
            onStartChatWithFriend = { friend ->
                val dmId = directChatRoomId(currentUserId, friend.uid)
                val existing = allRooms.firstOrNull {
                    !it.isGroup && (it.id == dmId || it.name.equals(friend.displayName, ignoreCase = true))
                }
                if (existing != null) {
                    selectedRoomId = existing.id
                } else {
                    val newRoom = ChatRoomUiModel(
                        id = dmId,
                        name = friend.displayName,
                        isGroup = false,
                        statusOrMembers = "Akun diikuti",
                        lastMessage = "Ketuk untuk mulai chat dengan ${friend.displayName}",
                        dateLabel = "",
                        participantIds = listOf(currentUserId, friend.uid).distinct(),
                        participantNames = listOf(currentUserName, friend.displayName).distinct(),
                    )
                    LocalChatRooms.add(0, newRoom)
                    selectedRoomId = newRoom.id
                }
            },
            onCreateGroupClick = { showCreateGroupDialog = true },
            onBack = onBack,
            modifier = modifier,
        )
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            friends = followingFriends,
            onDismiss = { showCreateGroupDialog = false },
            onCreateGroup = { groupName, chosenFriends ->
                val todayLabel = SimpleDateFormat("d/M", Locale.getDefault()).format(Date())
                val newGroup = createGroupChatRoom(
                    groupName = groupName,
                    selectedFriends = chosenFriends,
                    creatorId = currentUserId,
                    creatorName = currentUserName,
                    dateLabel = todayLabel,
                )
                LocalChatRooms.add(0, newGroup)
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("chats").document(newGroup.id).set(
                        mapOf(
                            "name" to newGroup.name,
                            "isGroup" to true,
                            "statusOrMembers" to newGroup.statusOrMembers,
                            "lastMessage" to newGroup.lastMessage,
                            "dateLabel" to newGroup.dateLabel,
                            "participantIds" to newGroup.participantIds,
                            "participantNames" to newGroup.participantNames,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    )
                } catch (_: Exception) {
                }
                showCreateGroupDialog = false
                selectedTab = ChatTab.KOMUNITAS
                selectedRoomId = newGroup.id
            },
        )
    }

    notice?.let { msg ->
        AlertDialog(
            onDismissRequest = { notice = null },
            title = { Text("Informasi") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { notice = null }) { Text("Mengerti") }
            },
        )
    }
}

@Composable
private fun ChatListContent(
    rooms: List<ChatRoomUiModel>,
    followingFriends: List<FollowingFriend>,
    selectedTab: ChatTab,
    onTabChange: (ChatTab) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRoomClick: (ChatRoomUiModel) -> Unit,
    onStartChatWithFriend: (FollowingFriend) -> Unit,
    onCreateGroupClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = HomeNavy,
        contentColor = HomeText,
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                KuyBackButton(
                    text = "Pesan",
                    onClick = onBack,
                )
            }
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Bagian atas: Search bar + tombol tab Orang / Komunitas
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HomeBlue)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KuyPostSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onSearch = {},
                    placeholder = "Cari orang / komunitas",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ChatTabButton(
                        text = ChatTab.ORANG.label,
                        selected = selectedTab == ChatTab.ORANG,
                        onClick = { onTabChange(ChatTab.ORANG) },
                        modifier = Modifier.weight(1f),
                    )
                    ChatTabButton(
                        text = ChatTab.KOMUNITAS.label,
                        selected = selectedTab == ChatTab.KOMUNITAS,
                        onClick = { onTabChange(ChatTab.KOMUNITAS) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Bagian bawah: Daftar percakapan / komunitas
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(HomeBlue)
                    .padding(vertical = 8.dp),
            ) {
                if (selectedTab == ChatTab.KOMUNITAS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Grup bersama teman (${followingFriends.size} diikuti)",
                            color = HomeText.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                        )
                        Surface(
                            onClick = onCreateGroupClick,
                            shape = RoundedCornerShape(16.dp),
                            color = KuyButton,
                            contentColor = HomeText,
                        ) {
                            Text(
                                text = "+ Buat Grup",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                if (rooms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (selectedTab == ChatTab.KOMUNITAS) {
                                "Belum ada grup komunitas.\nTekan + Buat Grup untuk membuat grup bersama temanmu."
                            } else {
                                "Belum ada akun teman yang ditemukan di Firebase."
                            },
                            color = HomeText.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(rooms, key = { it.id }) { room ->
                            ChatRoomRow(
                                room = room,
                                onClick = { onRoomClick(room) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) KuyButton else UnselectedTabBg,
        contentColor = if (selected) HomeText else KuyButton,
        border = BorderStroke(1.dp, KuyButton),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ChatRoomRow(
    room: ChatRoomUiModel,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KuyUserAvatar(
            name = room.name,
            size = 46.dp,
            fontSize = 18.sp,
            backgroundColor = Color(room.avatarColorHex),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = room.name,
                color = HomeText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = room.lastMessage,
                color = HomeText.copy(alpha = 0.85f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = room.dateLabel,
            color = HomeText.copy(alpha = 0.85f),
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun ChatDetailContent(
    room: ChatRoomUiModel,
    currentUserId: String,
    currentUserName: String,
    onBack: () -> Unit,
    onCallClick: () -> Unit,
    onVenueDetailClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPreview = LocalInspectionMode.current
    var messageInput by rememberSaveable(room.id) { mutableStateOf("") }
    var remoteMessages by remember(room.id) { mutableStateOf<List<ChatMessageUiModel>>(emptyList()) }

    if (!isPreview) {
        DisposableEffect(room.id) {
            var listener: ListenerRegistration? = null
            try {
                listener = FirebaseFirestore.getInstance()
                    .collection("chats")
                    .document(room.id)
                    .collection("messages")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .limit(100)
                    .addSnapshotListener { snap, error ->
                        if (error == null && snap != null) {
                            val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                            remoteMessages = snap.documents.mapNotNull { doc ->
                                val senderId = doc.getString("senderId").orEmpty()
                                val senderName = doc.getString("senderName") ?: "Teman"
                                val text = doc.getString("text") ?: ""
                                val ts = doc.getTimestamp("createdAt")?.toDate()
                                val timeStr = doc.getString("timeText")
                                    ?: ts?.let { fmt.format(it) }
                                    ?: "Baru"
                                ChatMessageUiModel(
                                    id = doc.id,
                                    senderId = senderId,
                                    senderName = senderName,
                                    text = text,
                                    timeText = timeStr,
                                    isFromMe = senderId == currentUserId || senderName == "Anda",
                                    createdAtMillis = ts?.time ?: System.currentTimeMillis(),
                                )
                            }
                        }
                    }
            } catch (_: Exception) {
            }
            onDispose { listener?.remove() }
        }
    }

    val localMessages = if (isPreview) {
        DefaultRoomMessages[room.id].orEmpty()
    } else {
        LocalChatMessages[room.id].orEmpty()
    }
    val messages = remember(localMessages, remoteMessages) {
        (localMessages + remoteMessages).distinctBy { it.id }
    }

    val sendMessage: () -> Unit = {
        val trimmed = messageInput.trim()
        if (trimmed.isNotEmpty()) {
            val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val dateNow = SimpleDateFormat("d/M", Locale.getDefault()).format(Date())
            val msgId = "msg_${System.currentTimeMillis()}"
            val newMsg = ChatMessageUiModel(
                id = msgId,
                senderId = currentUserId,
                senderName = if (room.isGroup) "Anda" else currentUserName,
                text = trimmed,
                timeText = timeNow,
                isFromMe = true,
                createdAtMillis = System.currentTimeMillis(),
            )
            LocalChatMessages[room.id] = messages + newMsg

            // Perbarui preview pesan terakhir pada daftar ruang obrolan dan naikkan ke urutan teratas
            val roomIdx = LocalChatRooms.indexOfFirst { it.id == room.id }
            val updatedPreview = if (room.isGroup) "$currentUserName: $trimmed" else trimmed
            val updatedRoom = (if (roomIdx >= 0) LocalChatRooms[roomIdx] else room).copy(
                lastMessage = updatedPreview,
                dateLabel = dateNow,
            )
            if (roomIdx >= 0) {
                LocalChatRooms.removeAt(roomIdx)
            }
            LocalChatRooms.add(0, updatedRoom)
            messageInput = ""

            if (!isPreview) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val chatRef = db.collection("chats").document(room.id)
                    val participantIds = (room.participantIds + currentUserId).filter { it.isNotBlank() }.distinct()
                    val participantNames = (room.participantNames + currentUserName + room.name)
                        .filter { it.isNotBlank() && !it.equals("Anda", ignoreCase = true) }
                        .distinct()
                    chatRef.set(
                        mapOf(
                            "name" to room.name,
                            "isGroup" to room.isGroup,
                            "statusOrMembers" to room.statusOrMembers,
                            "lastMessage" to updatedPreview,
                            "dateLabel" to dateNow,
                            "participantIds" to participantIds,
                            "participantNames" to participantNames,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    ).addOnFailureListener { err ->
                        android.util.Log.e("ChatScreen", "Gagal menyimpan chat ke Firestore: ${err.message}", err)
                    }
                    chatRef.collection("messages").document(msgId).set(
                        mapOf(
                            "senderId" to currentUserId,
                            "senderName" to currentUserName,
                            "text" to trimmed,
                            "timeText" to timeNow,
                            "createdAt" to FieldValue.serverTimestamp(),
                        ),
                    ).addOnFailureListener { err ->
                        android.util.Log.e("ChatScreen", "Gagal mengirim pesan ke Firestore: ${err.message}", err)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ChatScreen", "Exception saat kirim pesan: ${e.message}", e)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = HomeNavy,
        contentColor = HomeText,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HomeBlue)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(32.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.login_back),
                        contentDescription = "Kembali",
                        modifier = Modifier.size(18.dp),
                    )
                }
                KuyUserAvatar(
                    name = room.name,
                    size = 42.dp,
                    fontSize = 17.sp,
                    backgroundColor = Color(room.avatarColorHex),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room.name,
                        color = HomeText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = room.statusOrMembers,
                        color = HomeText.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onCallClick) {
                    PhoneCallIcon(modifier = Modifier.size(24.dp))
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InputBarNavy)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BasicTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = HomeText,
                        fontSize = 12.sp,
                    ),
                    cursorBrush = SolidColor(HomeOrange),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    decorationBox = { innerTextField ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HomeNavy,
                            border = BorderStroke(1.dp, KuyButton),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (messageInput.isEmpty()) {
                                    Text(
                                        text = "Apa yang anda pikirkan",
                                        color = HomeText.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                    )
                                }
                                innerTextField()
                            }
                        }
                    },
                )
                IconButton(
                    onClick = sendMessage,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = "Kirim pesan",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        },
    ) { insets ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatMessageBubbleRow(
                    message = msg,
                    isGroup = room.isGroup,
                    onVenueDetailClick = onVenueDetailClick,
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubbleRow(
    message: ChatMessageUiModel,
    isGroup: Boolean,
    onVenueDetailClick: (String) -> Unit,
) {
    val senderPalette = listOf(
        Color(0xFF59C39C),
        Color(0xFF6B8FF7),
        Color(0xFFF2B84B),
    )
    val senderColor = if (message.senderName.contains("Monster", ignoreCase = true)) {
        Color(0xFF59C39C)
    } else if (message.senderName.contains("Kenangan", ignoreCase = true)) {
        Color(0xFF6B8FF7)
    } else {
        senderPalette[message.senderName.hashCode().mod(senderPalette.size)]
    }

    if (message.venueInvite != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top,
        ) {
            VenueInviteCard(
                invite = message.venueInvite,
                onClick = { onVenueDetailClick(message.venueInvite.title) },
            )
            Spacer(Modifier.width(8.dp))
            KuyUserAvatar(
                name = message.senderName,
                size = 26.dp,
                fontSize = 11.sp,
                backgroundColor = Color(0xFFFFE35B),
            )
        }
        return
    }

    if (message.isFromMe) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OutgoingBubbleOrange,
                contentColor = HomeText,
                modifier = Modifier.widthIn(min = 96.dp, max = 250.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    if (isGroup) {
                        Text(
                            text = "Anda",
                            color = HomeText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        )
                    }
                    Text(
                        text = message.text,
                        color = HomeText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.End,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = message.timeText,
                        color = HomeText.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            KuyUserAvatar(
                name = message.senderName,
                size = 26.dp,
                fontSize = 11.sp,
                backgroundColor = Color(0xFFFFE35B),
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top,
        ) {
            KuyUserAvatar(
                name = message.senderName,
                size = 26.dp,
                fontSize = 11.sp,
                backgroundColor = senderColor,
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = IncomingBubbleBlue,
                contentColor = HomeText,
                modifier = Modifier.widthIn(min = 96.dp, max = 250.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    if (isGroup) {
                        Text(
                            text = message.senderName,
                            color = HomeText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        )
                    }
                    Text(
                        text = message.text,
                        color = HomeText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = message.timeText,
                        color = HomeText.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun VenueInviteCard(
    invite: VenueInviteUiModel,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = HomeBlue,
        contentColor = HomeText,
        modifier = Modifier.width(220.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = invite.title,
                color = HomeText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Text(
                text = invite.sport,
                color = HomeText.copy(alpha = 0.8f),
                fontSize = 10.sp,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2D5F8B), Color(0xFF4F87B8)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("🏀  ${invite.title}", color = HomeText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = invite.description,
                color = HomeText,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
            Button(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KuyButton,
                    contentColor = HomeText,
                ),
            ) {
                Text("Lihat", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    friends: List<FollowingFriend>,
    onDismiss: () -> Unit,
    onCreateGroup: (String, List<FollowingFriend>) -> Unit,
) {
    var groupName by rememberSaveable { mutableStateOf("") }
    var selectedFriends by remember { mutableStateOf(friends.take(2)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HomeBlue,
        titleContentColor = HomeText,
        textContentColor = HomeText,
        title = {
            Text("Buat Grup Komunitas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    singleLine = true,
                    placeholder = { Text("Nama Grup (misal: GymClub)", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HomeOrange,
                        unfocusedBorderColor = HomeOrange,
                        focusedTextColor = HomeText,
                        unfocusedTextColor = HomeText,
                        focusedPlaceholderColor = HomeText.copy(alpha = 0.7f),
                        unfocusedPlaceholderColor = HomeText.copy(alpha = 0.7f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Pilih teman yang di-follow:",
                    color = HomeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    friends.forEach { friend ->
                        val isSelected = selectedFriends.any { it.uid == friend.uid }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) HomeNavy else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) HomeOrange else HomeText.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable {
                                    selectedFriends = toggleTaggedFriend(selectedFriends, friend)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(friend.displayName, color = HomeText, fontSize = 12.sp)
                            Text(
                                if (isSelected) "✓ Dipilih" else "+ Pilih",
                                color = if (isSelected) HomeOrange else HomeText.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreateGroup(groupName, selectedFriends) },
                enabled = groupName.isNotBlank() && selectedFriends.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KuyButton,
                    contentColor = HomeText,
                ),
            ) {
                Text("Buat Grup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = HomeText)
            }
        },
    )
}

@Composable
private fun PhoneCallIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.24f, size.height * 0.16f)
            lineTo(size.width * 0.38f, size.height * 0.16f)
            lineTo(size.width * 0.45f, size.height * 0.34f)
            lineTo(size.width * 0.34f, size.height * 0.44f)
            quadraticTo(
                size.width * 0.44f,
                size.height * 0.62f,
                size.width * 0.58f,
                size.height * 0.68f,
            )
            lineTo(size.width * 0.68f, size.height * 0.57f)
            lineTo(size.width * 0.86f, size.height * 0.64f)
            lineTo(size.width * 0.86f, size.height * 0.78f)
            quadraticTo(
                size.width * 0.84f,
                size.height * 0.90f,
                size.width * 0.68f,
                size.height * 0.88f,
            )
            quadraticTo(
                size.width * 0.22f,
                size.height * 0.78f,
                size.width * 0.14f,
                size.height * 0.32f,
            )
            quadraticTo(
                size.width * 0.12f,
                size.height * 0.18f,
                size.width * 0.24f,
                size.height * 0.16f,
            )
            close()
        }
        drawPath(path = path, color = HomeOrange)
        drawArc(
            color = HomeOrange,
            startAngle = -65f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(size.width * 0.35f, size.height * 0.15f),
            size = size * 0.5f,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Preview(name = "PagePesan", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PagePesanPreview() {
    OlahragaKuyTheme {
        ChatScreen(
            onBack = {},
            initialTab = ChatTab.ORANG,
        )
    }
}

@Preview(name = "PagePesan2", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PagePesan2Preview() {
    OlahragaKuyTheme {
        ChatScreen(
            onBack = {},
            initialTab = ChatTab.KOMUNITAS,
        )
    }
}

@Preview(name = "PageDetailPesan Monster", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PageDetailPesanMonsterPreview() {
    OlahragaKuyTheme {
        ChatScreen(
            onBack = {},
            initialSelectedRoomId = "dm_monster",
        )
    }
}

@Preview(name = "PageDetailPesan Kenangan", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PageDetailPesanKenanganPreview() {
    OlahragaKuyTheme {
        ChatScreen(
            onBack = {},
            initialSelectedRoomId = "dm_kenangan",
        )
    }
}

@Preview(name = "PageDetailPesan GymClub", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PageDetailPesanGymClubPreview() {
    OlahragaKuyTheme {
        ChatScreen(
            onBack = {},
            initialSelectedRoomId = "group_gymclub",
        )
    }
}
