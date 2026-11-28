package fr.vinetos.tranquille

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.vinetos.tranquille.data.ContactItem
import fr.vinetos.tranquille.data.NumberInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.ConscryptMode

@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(AndroidJUnit4::class)
class IncomingCallNotificationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Before
    fun resetChannels() {
        // Channels are created once per process; each test has a fresh NotificationManager.
        NotificationHelper::class.java.getDeclaredField("notificationChannelsInitialized")
            .apply { isAccessible = true }.setBoolean(null, false)
    }

    private fun show(
        rating: NumberInfo.Rating,
        contact: Boolean = false,
        alerts: Boolean = true,
        actions: Boolean = true,
    ) =
        NumberInfo().apply {
            number = "+48000000101"
            this.rating = rating
            if (contact) contactItem = ContactItem(1, "Mom")
        }.also { NotificationHelper.showIncomingCallNotification(context, it, alerts, actions) }
            .let { shadowOf(manager).allNotifications.single() }

    @Test
    fun negativeCallerPopsUpWithThumbDownAndActions() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.ANSWER_PHONE_CALLS)
        val notification = show(NumberInfo.Rating.NEGATIVE)
        val channel = manager.getNotificationChannel(notification.channelId)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertNull(channel.sound)
        assertEquals(R.drawable.ic_thumb_down_24dp, notification.smallIcon.resId)
        assertEquals(
            listOf("Reject", "Block", "Online reviews"),
            notification.actions.map { it.title.toString() },
        )
    }

    @Test
    fun unknownCallerPopsUpButWithoutRejectWhenCallsCannotBeEnded() {
        val notification = show(NumberInfo.Rating.UNKNOWN)
        assertEquals(
            NotificationManager.IMPORTANCE_HIGH,
            manager.getNotificationChannel(notification.channelId).importance,
        )
        assertEquals(R.drawable.ic_thumb_down_24dp, notification.smallIcon.resId)
        assertEquals(listOf("Block", "Online reviews"), notification.actions.map { it.title.toString() })
    }

    @Test
    fun positiveCallersAndContactsStayQuiet() {
        val positive = show(NumberInfo.Rating.POSITIVE)
        assertTrue(manager.getNotificationChannel(positive.channelId).importance <= NotificationManager.IMPORTANCE_LOW)
        assertTrue(positive.actions.orEmpty().none { it.title.toString() == "Block" })
        manager.cancelAll()
        val contact = show(NumberInfo.Rating.UNKNOWN, contact = true)
        assertTrue(manager.getNotificationChannel(contact.channelId).importance <= NotificationManager.IMPORTANCE_LOW)
    }

    @Test
    fun withPopUpsOffNegativeCallsUseTheQuietChannel() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.ANSWER_PHONE_CALLS)
        val notification = show(NumberInfo.Rating.NEGATIVE, alerts = false)
        assertEquals("negative_calls", notification.channelId)
        assertEquals(
            NotificationManager.IMPORTANCE_LOW,
            manager.getNotificationChannel(notification.channelId).importance,
        )
        // Buttons are a separate option.
        assertEquals(listOf("Reject", "Block", "Online reviews"), notification.actions.map { it.title.toString() })
    }

    @Test
    fun withButtonsOffOnlyReviewsRemain() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.ANSWER_PHONE_CALLS)
        val notification = show(NumberInfo.Rating.NEGATIVE, actions = false)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, manager.getNotificationChannel(notification.channelId).importance)
        assertEquals(listOf("Online reviews"), notification.actions.map { it.title.toString() })
    }
}
