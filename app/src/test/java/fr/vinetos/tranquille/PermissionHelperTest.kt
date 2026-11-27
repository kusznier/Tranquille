package fr.vinetos.tranquille

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.ConscryptMode

@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(AndroidJUnit4::class)
class PermissionHelperTest {

    /** Regression: the Compose screens call this without a fragment; it crashed with an NPE. */
    @Test
    fun requestCallScreeningFromActivityWithoutFragment() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        PermissionHelper.requestCallScreening(activity)

        val started = shadowOf(activity).nextStartedActivityForResult
        assertNotNull("the role request should be started from the activity", started)
        assertEquals("android.app.role.action.REQUEST_ROLE", started.intent.action)
    }
}
