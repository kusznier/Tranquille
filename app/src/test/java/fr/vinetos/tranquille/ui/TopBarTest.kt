package fr.vinetos.tranquille.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.vinetos.tranquille.ui.components.TranquilleTopBar
import fr.vinetos.tranquille.ui.theme.TranquilleTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.ConscryptMode

// The app depends on conscrypt-android, whose native library does not load on the JVM.
@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(AndroidJUnit4::class)
class TopBarTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsTitleAndNavigatesUp() {
        var upClicks = 0
        compose.setContent {
            TranquilleTheme {
                TranquilleTopBar(title = "Tranquille", onNavigateUp = { upClicks++ })
            }
        }

        compose.onNodeWithText("Tranquille").assertIsDisplayed()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        assertEquals(1, upClicks)
    }
}
