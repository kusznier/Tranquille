package fr.vinetos.tranquille.presentation.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.vinetos.tranquille.CallLogRow
import fr.vinetos.tranquille.ui.theme.TranquilleTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.ConscryptMode

@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private class RecordingActions : MainActions {
        val calls = mutableListOf<String>()
        override fun lookupNumber() { calls += "lookup" }
        override fun openSettings() { calls += "settings" }
        override fun openAbout() { calls += "about" }
        override fun blockNumber() { calls += "block" }
        override fun openCall(row: CallLogRow) { calls += "call" }
        override fun downloadDatabase() { calls += "download" }
        override fun requestCallScreening() { calls += "screening" }
        override fun openNotificationSettings() { calls += "notifications" }
        override fun requestCallLogPermission() { calls += "callLog" }
        override fun setBlockByRating(block: Boolean) { calls += "rating=$block" }
    }

    private fun show(status: ProtectionStatus): RecordingActions {
        val actions = RecordingActions()
        compose.setContent {
            TranquilleTheme { MainScreen(MainUiState(loading = false, status = status), actions) }
        }
        return actions
    }

    @Test
    fun protectionOnShowsSwitchThatTogglesRatingBlocking() {
        val actions = show(ProtectionStatus(dbVersion = 4213, denylistCount = 2))
        compose.onNodeWithText("Protection is on").assertIsDisplayed()
        compose.onNodeWithText("2 numbers on the denylist").assertIsDisplayed()
        compose.onNodeWithText("No recent calls").assertIsDisplayed()
        compose.onNodeWithText("Block based on rating").performClick()
        assertEquals(listOf("rating=true"), actions.calls)
    }

    @Test
    fun missingDatabaseOffersDownload() {
        val actions = show(ProtectionStatus(dbVersion = null))
        compose.onNodeWithText("Download database").performClick()
        assertEquals(listOf("download"), actions.calls)
    }

    @Test
    fun missingRoleOffersToTurnItOn() {
        val actions = show(ProtectionStatus(dbVersion = 1, callScreeningHeld = false))
        compose.onNodeWithText("Advanced blocking is off").assertIsDisplayed()
        compose.onNodeWithText("Turn on").performClick()
        assertEquals(listOf("screening"), actions.calls)
    }

    @Test
    fun missingCallLogPermissionOffersToAllow() {
        val actions = show(ProtectionStatus(dbVersion = 1, hasCallLogPermission = false))
        compose.onNodeWithText("Allow").performClick()
        assertEquals(listOf("callLog"), actions.calls)
    }
}
