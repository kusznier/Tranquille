package fr.vinetos.tranquille

import dummydomain.yetanothercallblocker.sia.model.database.CommunityDatabaseItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberInfoUtilsTest {

    private fun item(negative: Int, positive: Int, neutral: Int) = CommunityDatabaseItem().apply {
        negativeRatingsCount = negative
        positiveRatingsCount = positive
        neutralRatingsCount = neutral
    }

    @Test
    fun noItemOrNoRatings() {
        assertFalse(NumberInfoUtils.hasAnyRatings(null))
        assertFalse(NumberInfoUtils.hasAnyRatings(item(0, 0, 0)))
    }

    @Test
    fun countsEveryRatingType() {
        assertTrue(NumberInfoUtils.hasAnyRatings(item(5, 0, 0)))
        assertTrue(NumberInfoUtils.hasAnyRatings(item(0, 1, 0)))
        // the library's hasRatings() returns false here
        assertTrue(NumberInfoUtils.hasAnyRatings(item(0, 0, 3)))
    }
}
