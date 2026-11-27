package fr.vinetos.tranquille.presentation.info

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.vinetos.tranquille.data.ContactItem
import fr.vinetos.tranquille.data.NumberInfo
import fr.vinetos.tranquille.ui.theme.TranquilleTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.ConscryptMode

@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(AndroidJUnit4::class)
class NumberInfoDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private class Recorder : NumberInfoActions {
        val calls = mutableListOf<String>()
        override fun addToDenylist(info: NumberInfo) { calls += "denylist" }
        override fun openOnlineReviews(info: NumberInfo) { calls += "reviews" }
        override fun openWebReview(info: NumberInfo) { calls += "web" }
    }

    private fun show(info: NumberInfo): Pair<Recorder, () -> Int> {
        val recorder = Recorder()
        var dismissed = 0
        compose.setContent { TranquilleTheme { NumberInfoDialog(info, recorder) { dismissed++ } } }
        return recorder to { dismissed }
    }

    private fun number(contact: Boolean) = NumberInfo().apply {
        number = "+99900000101"
        if (contact) contactItem = ContactItem(1, "Mom")
    }

    @Test
    fun unknownNumberOpensReviewsDirectly() {
        val (recorder, dismissed) = show(number(contact = false))
        compose.onNodeWithText("+99900000101").assertExists()
        compose.onNodeWithText("Online reviews").performClick()
        assertEquals(listOf("reviews"), recorder.calls)
        assertEquals(1, dismissed())
    }

    @Test
    fun contactNeedsConfirmationBeforeTheNumberIsSent() {
        val (recorder, _) = show(number(contact = true))
        compose.onNodeWithText("Online reviews").performClick()
        assertTrue(recorder.calls.isEmpty())

        compose.onNodeWithText("No").performClick()
        assertTrue(recorder.calls.isEmpty())

        compose.onNodeWithText("Add review (web)").performClick()
        compose.onNodeWithText("Yes").performClick()
        assertEquals(listOf("web"), recorder.calls)
    }

    @Test
    fun addToDenylist() {
        val (recorder, _) = show(number(contact = false))
        compose.onNodeWithText("Add to denylist").performClick()
        assertEquals(listOf("denylist"), recorder.calls)
    }
}
