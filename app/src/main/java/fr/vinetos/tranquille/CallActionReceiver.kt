package fr.vinetos.tranquille

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import fr.vinetos.tranquille.utils.PhoneUtils

/** Handles the "Reject" action of the incoming call notification. */
class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REJECT) {
            PhoneUtils.endCall(context, false)
        }
    }

    companion object {
        private const val ACTION_REJECT = "fr.vinetos.tranquille.action.REJECT_CALL"

        @JvmStatic
        fun rejectIntent(context: Context): PendingIntent {
            val intent = Intent(context, CallActionReceiver::class.java).setAction(ACTION_REJECT)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            return PendingIntent.getBroadcast(context, 0, intent, flags)
        }
    }
}
