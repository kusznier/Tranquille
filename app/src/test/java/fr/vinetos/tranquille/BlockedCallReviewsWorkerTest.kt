package fr.vinetos.tranquille

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dummydomain.yetanothercallblocker.sia.model.CommunityReview
import dummydomain.yetanothercallblocker.sia.model.NumberCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.ConscryptMode

@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(AndroidJUnit4::class)
class BlockedCallReviewsWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun review(rating: CommunityReview.Rating, title: String?, category: NumberCategory?) =
        CommunityReview().apply { this.rating = rating; this.title = title; this.category = category }

    @Test
    fun mostCommonTitleOfNegativeReviews() {
        val reviews = listOf(
            review(CommunityReview.Rating.NEGATIVE, "Example Bank", NumberCategory.TELEMARKETER),
            review(CommunityReview.Rating.NEGATIVE, " Example Bank ", NumberCategory.ROBOCALL),
            review(CommunityReview.Rating.NEGATIVE, "Other", NumberCategory.TELEMARKETER),
            review(CommunityReview.Rating.POSITIVE, "Nice people", null),
            review(CommunityReview.Rating.POSITIVE, "Nice people", null),
            review(CommunityReview.Rating.POSITIVE, "Nice people", null),
        )
        assertEquals("Example Bank", BlockedCallReviewsWorker.summarize(context, reviews))
    }

    @Test
    fun fallsBackToCategoryWithoutTitles() {
        val reviews = listOf(
            review(CommunityReview.Rating.NEGATIVE, "", NumberCategory.TELEMARKETER),
            review(CommunityReview.Rating.NEGATIVE, null, NumberCategory.TELEMARKETER),
        )
        assertEquals("Telemarketer", BlockedCallReviewsWorker.summarize(context, reviews))
    }

    @Test
    fun noReviewsNoSummary() {
        assertNull(BlockedCallReviewsWorker.summarize(context, emptyList()))
    }
}
