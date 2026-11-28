package fr.vinetos.tranquille.presentation.block

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dummydomain.yetanothercallblocker.sia.model.NumberCategory
import dummydomain.yetanothercallblocker.sia.model.database.CommunityDatabaseItem
import fr.vinetos.tranquille.data.DenylistItem
import fr.vinetos.tranquille.data.NumberInfo
import fr.vinetos.tranquille.presentation.denylist.FakeDenylistRepository
import fr.vinetos.tranquille.ui.theme.TranquilleTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.ConscryptMode
import java.util.Date

@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(AndroidJUnit4::class)
class BlockNumberTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun spammer() = NumberInfo().apply {
        number = "+48000000101"
        rating = NumberInfo.Rating.NEGATIVE
        communityDatabaseItem = CommunityDatabaseItem().apply {
            negativeRatingsCount = 5
            positiveRatingsCount = 1
            category = NumberCategory.TELEMARKETER.id
        }
    }

    @Test
    fun candidateSuggestsTheCategory() {
        val candidate = BlockCandidate.from(context, spammer())
        assertEquals("+48000000101", candidate.number)
        assertNull(candidate.label)
        assertEquals("Telemarketer", candidate.suggestedDescription)
        assertEquals(true, candidate.summary!!.startsWith("Telemarketer"))
    }

    @Test
    fun blockerAddsTheExactNumberOnce() {
        val repo = FakeDenylistRepository()
        val blocker = NumberBlocker(repo)
        assertEquals(BlockResult.ADDED, blocker.block("+48 000-000-101", "  Telemarketer "))
        assertEquals("+48000000101", repo.items.single().pattern)
        assertEquals("Telemarketer", repo.items.single().name)
        assertEquals(BlockResult.ALREADY_ON_DENYLIST, blocker.block("+48000000101", "again"))
        assertEquals(1, repo.items.size)
        assertEquals(BlockResult.INVALID_NUMBER, blocker.block("private", ""))
    }

    @Test
    fun existingPatternCountsAsBlocked() {
        val repo = FakeDenylistRepository(
            listOf(DenylistItem(1, "x", "+48000000101", Date(), false, 0, null))
        )
        assertEquals(BlockResult.ALREADY_ON_DENYLIST, NumberBlocker(repo).block("+48000000101", ""))
    }

    @Test
    fun dialogConfirmsWithTheEditedDescription() {
        var confirmed: String? = null
        var dismissed = false
        compose.setContent {
            TranquilleTheme {
                BlockNumberDialog(
                    BlockCandidate.from(context, spammer()),
                    onConfirm = { confirmed = it },
                    onDismiss = { dismissed = true },
                )
            }
        }
        compose.onNodeWithText("Block this number?").assertIsDisplayed()
        compose.onNodeWithText("+48000000101").assertIsDisplayed()
        compose.onNodeWithText("Telemarketer").performTextReplacement("Fake energy offers")
        compose.onNodeWithText("Block").performClick()
        assertEquals("Fake energy offers", confirmed)
        assertEquals(false, dismissed)
    }
}
