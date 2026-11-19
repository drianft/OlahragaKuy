package com.example.olahragakuy.pages.profile.user

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProfileUiStateTest {
    private val initial = ProfileUiState("Bibi", "Bio", ProfileContentTab.GRID)

    @Test
    fun initialTabPreservesLegacyMapping() {
        assertEquals(ProfileContentTab.FEED, ProfileContentTab.fromIndex(0))
        assertEquals(ProfileContentTab.GRID, ProfileContentTab.fromIndex(1))
        listOf(-1, 2, 99).forEach {
            assertEquals(ProfileContentTab.TAGGED, ProfileContentTab.fromIndex(it))
        }
    }

    @Test
    fun cancelEditKeepsProfileAndSelectedTab() {
        assertEquals(initial, initial.openEdit().backToProfile())
    }

    @Test
    fun saveEditReturnsToProfileWithoutChangingTab() {
        val saved = initial.openEdit().saveProfile("Nama baru", "Bio baru")
        assertEquals("Nama baru", saved.username)
        assertEquals("Bio baru", saved.bio)
        assertEquals(ProfileSubScreen.PROFILE, saved.subScreen)
        assertEquals(ProfileContentTab.GRID, saved.selectedTab)
    }

    @Test
    fun navigationPreservesProfileValues() {
        val state = initial.selectTab(ProfileContentTab.TAGGED).openSettings()
        assertEquals(ProfileSubScreen.ACCOUNT_SETTINGS, state.subScreen)
        assertEquals(ProfileContentTab.TAGGED, state.backToProfile().selectedTab)
        assertEquals(initial.username, state.username)
        assertEquals(initial.bio, state.bio)
    }

    @Test
    fun unchangedInputsDoNotOverwriteLocalEdits() {
        val saved = initial.saveProfile("Lokal", "Bio lokal")
        assertEquals(saved, saved.syncInputs("Bibi", "Bio"))
    }

    @Test
    fun changedInputsReplaceOnlyTheirOwnFields() {
        val saved = initial.saveProfile("Lokal", "Bio lokal").openSettings()
        val renamed = saved.syncInputs("Server", "Bio")
        assertEquals("Server", renamed.username)
        assertEquals("Bio lokal", renamed.bio)
        assertEquals(ProfileSubScreen.ACCOUNT_SETTINGS, renamed.subScreen)
        val updatedBio = saved.syncInputs("Bibi", "Server bio")
        assertEquals("Lokal", updatedBio.username)
        assertEquals("Server bio", updatedBio.bio)
    }

    @Test
    fun saverRestoresEveryScreenAndPreservesInputBaseline() {
        val scope = SaverScope { true }
        ProfileSubScreen.entries.forEach { screen ->
            val state = initial.saveProfile("Lokal", "Bio lokal").copy(subScreen = screen)
            val saved = with(ProfileUiState.Saver) { scope.save(state) }
            assertNotNull(saved)
            val restored = ProfileUiState.Saver.restore(saved!!)
            assertEquals(state, restored)
            assertEquals(state, restored!!.syncInputs("Bibi", "Bio"))
            assertEquals("Server", restored.syncInputs("Server", "Bio").username)
        }
    }
}
