package fr.vinetos.tranquille.presentation.block

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.vinetos.tranquille.CallLogRow
import fr.vinetos.tranquille.data.CallLogItem
import fr.vinetos.tranquille.data.NumberInfo
import fr.vinetos.tranquille.ui.theme.TranquilleTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.ConscryptMode

@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(AndroidJUnit4::class)
class RecentCallPickerTest {

    @get:Rule
    val compose = createComposeRule()

    private fun row(key: String, number: String?, label: String = number ?: "Private", description: String? = null) =
        CallLogRow(
            key = key, label = label, ratingIcon = null, ratingColor = null,
            types = listOf(CallLogItem.Type.INCOMING), count = 1, description = description,
            duration = null, timestamp = 0L,
            numberInfo = NumberInfo().apply { this.number = number; noNumber = number == null },
        )

    @Test
    fun hiddenAndRepeatedNumbersAreLeftOut() {
        val rows = listOf(
            row("1", "+48000000101", description = "Telemarketer"),
            row("2", null),
            row("3", "+48000000101"),
            row("4", "+48000000230", label = "Bank"),
        )
        assertEquals(listOf("1", "4"), blockableCalls(rows).map { it.key })
    }

    @Test
    fun pickingACallOrEnteringManually() {
        val picked = mutableListOf<String>()
        compose.setContent {
            TranquilleTheme {
                RecentCallPicker(
                    rows = listOf(row("1", "+48000000101", description = "Telemarketer"), row("4", "+48000000230", label = "Bank")),
                    onPick = { picked += it.key },
                    onEnterManually = { picked += "manual" },
                    onDismiss = { picked += "dismiss" },
                )
            }
        }
        compose.onNodeWithText("Block a number").assertIsDisplayed()
        compose.onNodeWithText("Bank").performClick()
        compose.onNodeWithText("Enter a number").performClick()
        assertEquals(listOf("4", "manual"), picked)
    }

    @Test
    fun emptyLogStillOffersManualEntry() {
        var manual = false
        compose.setContent {
            TranquilleTheme { RecentCallPicker(emptyList(), {}, { manual = true }, {}) }
        }
        compose.onNodeWithText("No recent calls").assertIsDisplayed()
        compose.onNodeWithText("Enter a number").performClick()
        assertEquals(true, manual)
    }
}
