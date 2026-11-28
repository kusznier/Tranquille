package fr.vinetos.tranquille.presentation.main

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.vinetos.tranquille.ui.theme.TranquilleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.ConscryptMode

@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(AndroidJUnit4::class)
class MainTabsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun bottomBarSwitchesBetweenTabs() {
        compose.setContent {
            TranquilleTheme {
                var tab by remember { mutableStateOf(MainTab.CALLS) }
                MainTabs(selected = tab, onSelect = { tab = it }) { selected, modifier ->
                    Text("content:${selected.name}", modifier)
                }
            }
        }
        compose.onNodeWithText("content:CALLS").assertIsDisplayed()
        compose.onNodeWithText("Calls").assertIsSelected()

        compose.onNodeWithText("Denylist").performClick()
        compose.onNodeWithText("content:DENYLIST").assertIsDisplayed()
        compose.onNodeWithText("Denylist").assertIsSelected()
        compose.onNodeWithText("Calls").assertIsNotSelected()

        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("content:SETTINGS").assertIsDisplayed()
    }
}
