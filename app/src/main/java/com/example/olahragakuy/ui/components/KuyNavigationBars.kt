package com.example.olahragakuy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.R
import com.example.olahragakuy.ui.theme.KuyBlue
import com.example.olahragakuy.ui.theme.KuyButton
import com.example.olahragakuy.ui.theme.KuyNavy
import com.example.olahragakuy.ui.theme.KuyOrange
import com.example.olahragakuy.ui.theme.KuyText

@Composable
fun KuyBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Kembali",
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.widthIn(min = 128.dp).heightIn(min = 40.dp),
        shape = RoundedCornerShape(24.dp),
        color = KuyBlue,
        contentColor = KuyText,
    ) {
        Row(
            Modifier.padding(horizontal = 17.5.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.login_back), contentDescription = null, Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

enum class HomeNavTab(val label: String, @param:DrawableRes val iconRes: Int) {
    KOMUNITAS("Komunitas", R.drawable.ic_nav_community),
    LOKASI("Lokasi", R.drawable.ic_nav_location),
    LATIHAN("Latihan", R.drawable.ic_nav_workout),
    PROFIL("Profil", R.drawable.ic_nav_profile),
}

@Composable
fun KuyHomeTopBar(
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onChatClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.login_wordmark),
            contentDescription = "OlahragaKUY",
            modifier = Modifier.height(24.dp),
            contentScale = ContentScale.FillHeight,
        )
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HomeLogoButton("Cari", R.drawable.ic_baseline_search, onClick = onSearchClick)
            HomeLogoButton("Notifikasi", R.drawable.ic_mingcute_notification_fill, onClick = onNotificationClick)
            HomeLogoButton("Chat", R.drawable.ic_majesticons_share, onClick = onChatClick)
        }
    }
}

@Composable
fun KuyBottomNavBar(
    onTabSelected: (HomeNavTab) -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: HomeNavTab = HomeNavTab.KOMUNITAS,
) {
    Surface(
        modifier = modifier,
        color = KuyNavy,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeNavTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .size(55.dp)
                        .clip(CircleShape)
                        .background(if (selected) KuyButton else Color.Transparent)
                        .clickable(role = Role.Tab) { onTabSelected(tab) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = tab.label,
                        tint = if (selected) Color.White else KuyOrange,
                        modifier = Modifier.size(35.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun HomeLogoButton(
    label: String,
    @DrawableRes iconRes: Int? = null,
    onClick: () -> Unit,
) {
    if (iconRes != null) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = KuyBlue,
                modifier = Modifier.size(32.dp),
            )
        }
    } else {
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
}
