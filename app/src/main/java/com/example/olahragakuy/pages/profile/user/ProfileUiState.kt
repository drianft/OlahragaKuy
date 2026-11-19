package com.example.olahragakuy.pages.profile.user

import androidx.compose.runtime.saveable.listSaver

internal enum class ProfileContentTab {
    FEED, GRID, TAGGED;

    companion object {
        fun fromIndex(index: Int): ProfileContentTab = when (index) {
            0 -> FEED
            1 -> GRID
            else -> TAGGED
        }
    }
}

internal enum class ProfileSubScreen { PROFILE, EDIT_PROFILE, ACCOUNT_SETTINGS }

internal data class ProfileUiState(
    val username: String,
    val bio: String,
    val selectedTab: ProfileContentTab,
    val subScreen: ProfileSubScreen = ProfileSubScreen.PROFILE,
    private val sourceUsername: String = username,
    private val sourceBio: String = bio,
) {
    fun syncInputs(username: String, bio: String): ProfileUiState = copy(
        username = if (username != sourceUsername) username else this.username,
        bio = if (bio != sourceBio) bio else this.bio,
        sourceUsername = username,
        sourceBio = bio,
    )

    fun selectTab(tab: ProfileContentTab) = copy(selectedTab = tab)
    fun openEdit() = copy(subScreen = ProfileSubScreen.EDIT_PROFILE)
    fun openSettings() = copy(subScreen = ProfileSubScreen.ACCOUNT_SETTINGS)
    fun backToProfile() = copy(subScreen = ProfileSubScreen.PROFILE)
    fun saveProfile(username: String, bio: String) = copy(
        username = username,
        bio = bio,
        subScreen = ProfileSubScreen.PROFILE,
    )

    companion object {
        val Saver = listSaver<ProfileUiState, String>(
            save = {
                listOf(it.username, it.bio, it.selectedTab.name, it.subScreen.name,
                    it.sourceUsername, it.sourceBio)
            },
            restore = {
                ProfileUiState(
                    username = it[0],
                    bio = it[1],
                    selectedTab = ProfileContentTab.valueOf(it[2]),
                    subScreen = ProfileSubScreen.valueOf(it[3]),
                    sourceUsername = it[4],
                    sourceBio = it[5],
                )
            }
        )
    }
}
