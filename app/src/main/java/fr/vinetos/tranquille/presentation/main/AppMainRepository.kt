package fr.vinetos.tranquille.presentation.main

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import fr.vinetos.tranquille.App
import fr.vinetos.tranquille.CallLogRow
import fr.vinetos.tranquille.CallLogRows
import fr.vinetos.tranquille.EventUtils
import fr.vinetos.tranquille.PermissionHelper
import fr.vinetos.tranquille.RoleManagerHelper
import fr.vinetos.tranquille.Settings
import fr.vinetos.tranquille.data.CallLogHelper
import fr.vinetos.tranquille.data.CallLogItem
import fr.vinetos.tranquille.data.CallLogItemGroup
import fr.vinetos.tranquille.data.NumberInfo
import fr.vinetos.tranquille.data.YacbHolder
import fr.vinetos.tranquille.event.MainDbDownloadingEvent
import fr.vinetos.tranquille.work.UpdateScheduler

/** Reads the same sources the old MainActivity and call log data source used. */
class AppMainRepository(context: Context) : MainRepository {

    private val context = context.applicationContext
    private val settings: Settings get() = App.getSettings()

    override fun loadStatus(): ProtectionStatus {
        val db = YacbHolder.getCommunityDatabase()
        return ProtectionStatus(
            // Before Android 10 calls are blocked without the call screening role.
            callScreeningHeld = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                RoleManagerHelper.hasCallScreeningRole(context),
            blockingEnabled = settings.callBlockingEnabled,
            blockByRating = settings.blockNegativeSiaNumbers,
            dbVersion = if (db.isOperational) db.effectiveDbVersion else null,
            dbDownloading = EventUtils.bus().getStickyEvent(MainDbDownloadingEvent::class.java) != null,
            lastUpdateTime = settings.lastUpdateTime,
            denylistCount = YacbHolder.getDenylistDao().countValid(),
            notificationsBlocked = settings.useNotification &&
                !NotificationManagerCompat.from(context).areNotificationsEnabled(),
            hasCallLogPermission = PermissionHelper.hasCallLogPermission(context),
            permissionsMissing = PermissionHelper.getMissingPermissions(
                context,
                settings.incomingCallNotifications, settings.callBlockingEnabled,
                settings.useContacts, settings.useNotification,
            ).isNotEmpty(),
            screeningRoleSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            autoUpdates = UpdateScheduler.get(context).isAutoUpdateScheduled,
        )
    }

    override fun loadCallLog(): List<CallLogRow> {
        val countryCode = settings.cachedAutoDetectedCountryCode
        val cache = HashMap<String?, NumberInfo>()
        val items = CallLogHelper.loadCalls(context, null, false, CALL_LOG_LIMIT)
        for (item in items) {
            item.numberInfo = cache.getOrPut(item.number) { YacbHolder.getNumberInfo(item.number, countryCode) }
        }
        return group(items).map { CallLogRows.from(context, it) }
    }

    private fun group(items: List<CallLogItem>): List<CallLogItemGroup> = when (settings.callLogGrouping) {
        Settings.PREF_CALL_LOG_GROUPING_NONE -> CallLogItemGroup.noGrouping(items)
        Settings.PREF_CALL_LOG_GROUPING_DAY -> CallLogItemGroup.groupInDay(items)
        else -> CallLogItemGroup.groupConsecutive(items)
    }

    override fun setBlockByRating(block: Boolean) {
        settings.blockNegativeSiaNumbers = block
    }

    override fun isOnboardingDone() = settings.onboardingDone

    override fun setOnboardingDone() {
        settings.onboardingDone = true
    }

    override fun setAutoUpdates(enabled: Boolean) {
        val scheduler = UpdateScheduler.get(context)
        if (enabled) scheduler.scheduleAutoUpdates() else scheduler.cancelAutoUpdateWorker()
    }

    private companion object {
        /** The old screen paged through the whole log; recent calls are what people check. */
        const val CALL_LOG_LIMIT = 200
    }
}
