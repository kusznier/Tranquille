package fr.vinetos.tranquille.presentation.settings

import android.os.Build
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.hasSetTextAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.vinetos.tranquille.ui.theme.TranquilleTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.ConscryptMode

@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private class Recorder : SettingsActions {
        val calls = mutableListOf<Any>()
        override fun change(change: SettingsChange) { calls += change }
        override fun setCallScreening(enabled: Boolean) { calls += "screening=$enabled" }
        override fun openNotificationChannels() { calls += "channels" }
        override fun openDbManagement() { calls += "dbManagement" }
        override fun exportLogcat() { calls += "logcat" }
        override fun filterDb() { calls += "filter" }
    }

    private fun show(page: SettingsPage, state: SettingsUiState = SettingsUiState()): Pair<Recorder, () -> SettingsPage?> {
        val recorder = Recorder()
        var navigated: SettingsPage? = null
        compose.setContent {
            TranquilleTheme {
                SettingsScreen(page, state, SettingsEnvironment(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE), recorder, { navigated = it }, {})
            }
        }
        return recorder to { navigated }
    }

    private fun scrollTo(text: String) = compose.onNode(hasScrollAction()).performScrollToNode(hasText(text))

    @Test
    fun switchesWriteChanges() {
        val (recorder, _) = show(SettingsPage.ROOT)
        scrollTo("Block hidden numbers")
        compose.onNodeWithText("Block hidden numbers").performClick()
        assertEquals(listOf<Any>(SettingsChange.BlockHiddenNumbers(true)), recorder.calls)
    }

    @Test
    fun callNotificationOptionsAreOnAndFollowNotifications() {
        val (recorder, _) = show(SettingsPage.ROOT)
        compose.onNodeWithText("Pop up for unknown and negative calls").performClick()
        compose.onNodeWithText("Reject and Block buttons").performClick()
        assertEquals(
            listOf<Any>(SettingsChange.IncomingCallAlerts(false), SettingsChange.IncomingCallActions(false)),
            recorder.calls,
        )
    }

    @Test
    fun callNotificationOptionsNeedNotifications() {
        show(SettingsPage.ROOT, SettingsUiState(incomingCallNotifications = false))
        compose.onNode(hasText("Pop up for unknown and negative calls") and isToggleable()).assertIsNotEnabled()
        compose.onNode(hasText("Reject and Block buttons") and isToggleable()).assertIsNotEnabled()
    }

    @Test
    fun failedVerificationNeedsCallScreening() {
        show(SettingsPage.ROOT, SettingsUiState(callScreeningHeld = false))
        scrollTo("Block failed verification")
        compose.onNode(hasText("Block failed verification") and isToggleable()).assertIsNotEnabled()
    }

    @Test
    fun onlineReviewsSettingIsShownAndNavigatesToAdvanced() {
        val (recorder, navigated) = show(SettingsPage.ROOT)
        scrollTo("Online reviews for blocked calls")
        compose.onNodeWithText("Online reviews for blocked calls").performClick()
        assertEquals(listOf<Any>(SettingsChange.ReviewsForBlockedCalls(true)), recorder.calls)

        scrollTo("Advanced settings")
        compose.onNode(hasText("Advanced settings") and hasClickActionMatcher()).performClick()
        assertEquals(SettingsPage.ADVANCED, navigated())
    }

    @Test
    fun invalidCountryCodeIsRejected() {
        val (recorder, _) = show(SettingsPage.ADVANCED)
        compose.onNodeWithText("Country code").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("POL")
        compose.onNodeWithText("OK").performClick()
        assertTrue(recorder.calls.isEmpty())

        compose.onNode(hasSetTextAction()).performTextReplacement("pl")
        compose.onNodeWithText("OK").performClick()
        assertEquals(listOf<Any>(SettingsChange.CountryCodeOverride("pl")), recorder.calls)
    }

    private fun hasClickActionMatcher() = androidx.compose.ui.test.hasClickAction()
}
