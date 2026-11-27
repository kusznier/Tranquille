package fr.vinetos.tranquille.presentation.about

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.vinetos.tranquille.ui.theme.TranquilleTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.ConscryptMode

@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(AndroidJUnit4::class)
class AboutScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsDatabaseVersionAndChecksForUpdates() {
        var checks = 0
        compose.setContent {
            TranquilleTheme {
                AboutScreen(
                    state = AboutUiState(versionName = "1.2.3", dbVersion = 4213),
                    onNavigateUp = {},
                    onCheckForUpdates = { checks++ },
                    onOpenDbManagement = {},
                )
            }
        }

        compose.onNodeWithText("Database version: 4213").performScrollTo()
        compose.onNodeWithText("v1.2.3").performScrollTo()
        compose.onNodeWithText("Check for updates").performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, checks)
    }

    @Test
    fun disablesUpdateWhileChecking() {
        compose.setContent {
            TranquilleTheme {
                AboutScreen(
                    state = AboutUiState(versionName = "1", dbVersion = 1, checkingForUpdates = true),
                    onNavigateUp = {},
                    onCheckForUpdates = {},
                    onOpenDbManagement = {},
                )
            }
        }

        compose.onNodeWithText("Last update check: checking…").performScrollTo()
        compose.onNodeWithText("Check for updates").performScrollTo().assertIsNotEnabled()
    }
}
