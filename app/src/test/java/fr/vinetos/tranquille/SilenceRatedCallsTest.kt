package fr.vinetos.tranquille

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.vinetos.tranquille.data.DenylistItem
import fr.vinetos.tranquille.data.NumberInfo
import fr.vinetos.tranquille.data.NumberInfoService
import fr.vinetos.tranquille.domain.service.DenylistService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.ConscryptMode
import java.util.Date

@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(AndroidJUnit4::class)
class SilenceRatedCallsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var settings: Settings
    private val denylisted = mutableSetOf<String>()

    private val denylistService = object : DenylistService({}, null) {
        override fun getDenylistItemForNumber(number: String?): DenylistItem? =
            if (number in denylisted) DenylistItem(1, "x", number!!, Date(), false, 0, null) else null
    }

    private lateinit var service: NumberInfoService

    @Before
    fun setUp() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        settings = Settings(context)
        service = NumberInfoService(settings, null, null, null, null, null, denylistService)
    }

    private fun call(reason: NumberInfo.BlockingReason?) = NumberInfo().apply {
        number = "+48000000101"
        blockingReason = reason
    }

    @Test
    fun offByDefault() {
        assertFalse(service.shouldSilenceInstead(call(NumberInfo.BlockingReason.SIA_RATING), true))
    }

    @Test
    fun silencesOnlyRatingBasedBlockingWhenPossible() {
        settings.silenceRatedCalls = true
        assertTrue(service.shouldSilenceInstead(call(NumberInfo.BlockingReason.SIA_RATING), true))
        // Without the call screening role nothing can silence the call: it is rejected.
        assertFalse(service.shouldSilenceInstead(call(NumberInfo.BlockingReason.SIA_RATING), false))
        assertFalse(service.shouldSilenceInstead(call(NumberInfo.BlockingReason.DENYLISTED), true))
        assertFalse(service.shouldSilenceInstead(call(NumberInfo.BlockingReason.HIDDEN_NUMBER), true))
        assertFalse(service.shouldSilenceInstead(call(NumberInfo.BlockingReason.FAILED_VERIFICATION), true))
        assertFalse(service.shouldSilenceInstead(call(null), true))
    }

    @Test
    fun denylistedNumbersWithABadRatingAreStillRejected() {
        settings.silenceRatedCalls = true
        denylisted += "+48000000101"
        assertFalse(service.shouldSilenceInstead(call(NumberInfo.BlockingReason.SIA_RATING), true))
        // ...unless blocking denylisted numbers is off.
        settings.blockDenylisted = false
        assertTrue(service.shouldSilenceInstead(call(NumberInfo.BlockingReason.SIA_RATING), true))
    }
}
