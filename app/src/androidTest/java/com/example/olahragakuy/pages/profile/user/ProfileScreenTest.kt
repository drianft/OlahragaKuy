package com.example.olahragakuy.pages.profile.user

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.example.olahragakuy.ui.components.HomeNavTab
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun editDraftSurvivesRestorationAndCancelDiscardsIt() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { ProfileScreen() }
        compose.onNodeWithText("Edit Profil").performClick()
        compose.onNodeWithText("Nama Pengguna").performTextReplacement("Draft")
        compose.onNodeWithText("Bio profil").performTextReplacement("Draft bio")
        closeSoftKeyboard()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Draft").assertExists()
        compose.onNodeWithText("Draft bio").assertExists()
        compose.onNodeWithText("Kembali").performClick()
        compose.onNodeWithText("@BibiOlahraga").assertExists()
        compose.onNodeWithText("Edit Profil").performClick()
        compose.onNodeWithText("BibiOlahraga").assertExists()
        compose.onNodeWithText("You Can Do it").assertExists()
        compose.onNodeWithText("Draft").assertDoesNotExist()
    }

    @Test
    fun savedProfileSurvivesRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { ProfileScreen() }
        compose.onNodeWithText("Edit Profil").performClick()
        compose.onNodeWithText("Nama Pengguna").performTextReplacement("NamaBaru")
        compose.onNodeWithText("Bio profil").performTextReplacement("Bio baru")
        closeSoftKeyboard()
        compose.onNodeWithText("Simpan Perubahan").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("@NamaBaru").assertExists()
        compose.onNodeWithText("Bio baru").assertExists()
    }

    @Test
    fun logoutDialogSurvivesRestorationAndOnlyConfirmCallsLogout() {
        val restoration = StateRestorationTester(compose)
        var logoutCount = 0
        restoration.setContent { ProfileScreen(onLogout = { logoutCount++ }) }
        compose.onNodeWithContentDescription("Menu").performClick()
        compose.onNodeWithText("Keluar Akun").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Keluar?").assertExists()
        compose.onAllNodesWithText("Kembali").filter(hasAnyAncestor(isDialog())).onFirst().performClick()
        compose.onNodeWithText("Keluar?").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, logoutCount) }
        compose.onNodeWithText("Keluar Akun").performClick()
        compose.onNodeWithText("Yakin").performClick()
        compose.runOnIdle { assertEquals(1, logoutCount) }
        compose.onNodeWithText("@BibiOlahraga").assertExists()
    }

    @Test
    fun gridClickCallsPostCallbackAndSwitchesToFeed() {
        var clicked: UserPost? = null
        val post = UserPost(5, "Grid post", "Waktu post")
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ProfileScreen(initialTab = 1, gridPosts = listOf(post), feedPosts = listOf(post),
                onPostClick = { clicked = it })
        }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Grid post").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(post, clicked) }
        compose.onNodeWithText("Waktu post").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun taggedClickKeepsGridAndBottomNavigationCallsBothCallbacks() {
        val post = UserPost(5, "Tagged post", "Waktu post")
        var clicked: UserPost? = null
        var tab: HomeNavTab? = null
        var homeCount = 0
        compose.setContent {
            ProfileScreen(initialTab = 2, taggedPosts = listOf(post),
                onPostClick = { clicked = it },
                onTabSelected = { tab = it },
                onHomeClick = { homeCount++ })
        }
        compose.onNodeWithContentDescription("Tagged post").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Tagged post").assertExists()
        compose.runOnIdle { assertEquals(post, clicked) }
        compose.onNodeWithContentDescription("Komunitas").performClick()
        compose.runOnIdle {
            assertEquals(HomeNavTab.KOMUNITAS, tab)
            assertEquals(1, homeCount)
        }
    }
}
