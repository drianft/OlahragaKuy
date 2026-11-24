package com.example.olahragakuy.pages.profile.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.R

@Composable
internal fun ProfileHeaderSection(
    username: String,
    displayName: String,
    postsCount: Int,
    followers: Int,
    following: Int,
    bio: String,
    selectedTab: ProfileContentTab,
    onTabSelected: (ProfileContentTab) -> Unit,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    onEditProfile: () -> Unit,
    onShareProfile: () -> Unit,
    photoUrl: String? = null,
    showMenu: Boolean = true,
    showAdminDashboard: Boolean = false,
    onAdminDashboardClick: () -> Unit = {},
    primaryActionLabel: String = "Edit Profil",
    secondaryActionLabel: String = "Bagikan Profil",
    primaryActionSelected: Boolean = false,
    primaryActionEnabled: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyDark)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopBar(
            title = displayName,
            onBack = onBack,
            onMenu = onMenu,
            showMenu = showMenu,
            showAdminDashboard = showAdminDashboard,
            onAdminDashboardClick = onAdminDashboardClick,
        )
        Spacer(Modifier.height(16.dp))

        ProfileAvatar(size = 140.dp, photoUrl = photoUrl)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "@$username",
            color = TextWhite,
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
        )

        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatItem(postsCount.toString(), "Postingan")
            StatItem(followers.toString(), "Pengikut")
            StatItem(following.toString(), "Mengikuti")
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val primaryShape = RoundedCornerShape(18.dp)
            Button(
                onClick = onEditProfile,
                enabled = primaryActionEnabled,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .then(
                        if (primaryActionSelected) {
                            Modifier.border(2.dp, OrangeBright, primaryShape)
                        } else {
                            Modifier
                        },
                    ),
                shape = primaryShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (primaryActionSelected) BlueTab else OrangeDark,
                    disabledContainerColor = OrangeDark.copy(alpha = 0.6f),
                ),
            ) {
                Text(
                    text = primaryActionLabel,
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }
            Button(
                onClick = onShareProfile,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .border(2.dp, OrangeBright, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GrayLight),
            ) {
                Text(
                    text = secondaryActionLabel,
                    color = OrangeBright,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        if (bio.isNotBlank()) {
            Text(
                text = bio,
                color = TextWhite,
                fontFamily = Poppins,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        }

        // Tab Bar (Feed | Grid | Ditandai)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProfileTab(
                icon = Icons.Default.Image,
                selected = selectedTab == ProfileContentTab.FEED,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(ProfileContentTab.FEED) },
            )
            ProfileTab(
                icon = Icons.Default.GridView,
                selected = selectedTab == ProfileContentTab.GRID,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(ProfileContentTab.GRID) },
            )
            ProfileTab(
                icon = Icons.Default.PersonPin,
                selected = selectedTab == ProfileContentTab.TAGGED,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(ProfileContentTab.TAGGED) },
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun TopBar(
    title: String,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    showMenu: Boolean = true,
    showAdminDashboard: Boolean = false,
    onAdminDashboardClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = "Kembali",
            tint = TextWhite,
            modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onBack),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = TextWhite,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.weight(1f),
        )
        if (showAdminDashboard) {
            Icon(
                painter = painterResource(id = R.drawable.ic_admin_dashboard_report),
                contentDescription = "Dashboard Laporan",
                tint = TextWhite,
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onAdminDashboardClick),
            )
            if (showMenu) {
                Spacer(Modifier.width(14.dp))
            }
        }
        if (showMenu) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = TextWhite,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onMenu),
            )
        }
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
            fontSize = 44.sp,
        )
        Text(
            text = label,
            color = TextWhite,
            fontFamily = Poppins,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun ProfileTab(
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(if (selected) BlueTab else GrayLight)
            .then(if (selected) Modifier.border(2.dp, GrayLight, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) GrayLight else BlueTab,
            modifier = Modifier.size(26.dp),
        )
    }
}
