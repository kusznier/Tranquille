package fr.vinetos.tranquille.presentation.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.vinetos.tranquille.presentation.main.OnboardingState
import fr.vinetos.tranquille.presentation.main.OnboardingStep
import fr.vinetos.tranquille.presentation.main.ProtectionStatus
import fr.vinetos.tranquille.ui.theme.TranquilleTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.ConscryptMode

@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private class RecordingActions : OnboardingActions {
        val calls = mutableListOf<String>()
        override fun requestCallScreening() { calls += "screening" }
        override fun requestPermissions() { calls += "permissions" }
        override fun downloadDatabase() { calls += "download" }
        override fun setAutoUpdates(enabled: Boolean) { calls += "autoUpdates=$enabled" }
        override fun next() { calls += "next" }
    }

    private fun show(step: OnboardingStep, status: ProtectionStatus): RecordingActions {
        val actions = RecordingActions()
        val state = OnboardingState(OnboardingStep.entries, OnboardingStep.entries.indexOf(step))
        compose.setContent { TranquilleTheme { OnboardingScreen(state, status, actions) } }
        return actions
    }

    @Test
    fun callScreeningStepExplainsAndAsksForTheRole() {
        val actions = show(OnboardingStep.CALL_SCREENING, ProtectionStatus(callScreeningHeld = false))
        compose.onNodeWithText("Step 1 of 3").assertIsDisplayed()
        compose.onNodeWithText("Block calls before they ring").assertIsDisplayed()
        compose.onNodeWithText("Set as caller ID app").performClick()
        compose.onNodeWithText("Skip").performClick()
        assertEquals(listOf("screening", "next"), actions.calls)
    }

    @Test
    fun grantedPermissionsShowDoneAndNext() {
        val actions = show(OnboardingStep.PERMISSIONS, ProtectionStatus(permissionsMissing = false))
        compose.onNodeWithText("Done").assertIsDisplayed()
        compose.onNodeWithText("Next").performClick()
        assertEquals(listOf("next"), actions.calls)
    }

    @Test
    fun databaseStepDownloadsAndTogglesAutoUpdates() {
        val actions = show(OnboardingStep.DATABASE, ProtectionStatus(dbVersion = null))
        compose.onNodeWithText("Step 3 of 3").assertIsDisplayed()
        compose.onNodeWithText("Download database").performClick()
        compose.onNodeWithText("Auto-update database").performScrollTo().performClick()
        assertEquals(listOf("download", "autoUpdates=true"), actions.calls)
    }

    @Test
    fun lastStepFinishesWhenTheDatabaseIsThere() {
        val actions = show(OnboardingStep.DATABASE, ProtectionStatus(dbVersion = 4213))
        compose.onNodeWithText("Finish").performClick()
        assertEquals(listOf("next"), actions.calls)
    }

    @Test
    fun downloadButtonIsDisabledWhileDownloading() {
        show(OnboardingStep.DATABASE, ProtectionStatus(dbVersion = null, dbDownloading = true))
        compose.onNodeWithText("Downloading DB…").assertIsDisplayed()
    }
}
