package fr.vinetos.tranquille.presentation.settings

import android.content.Context
import fr.vinetos.tranquille.App
import fr.vinetos.tranquille.PermissionHelper
import fr.vinetos.tranquille.Settings
import fr.vinetos.tranquille.presentation.settings.SettingsChange.*
import fr.vinetos.tranquille.utils.DbFilteringUtils
import fr.vinetos.tranquille.work.UpdateScheduler

/** Persists settings through the typed Settings API, like the old preference screens. */
class AppSettingsStore(context: Context) : SettingsStore {

    private val context = context.applicationContext
    private val settings: Settings get() = App.getSettings()
    private val updateScheduler get() = UpdateScheduler.get(context)

    override fun load() = SettingsUiState(
        incomingCallNotifications = settings.incomingCallNotifications,
        incomingCallAlerts = settings.incomingCallAlerts,
        incomingCallActions = settings.incomingCallActions,
        autoUpdates = updateScheduler.isAutoUpdateScheduled,
        useContacts = settings.useContacts,
        uiMode = settings.uiMode,
        callLogGrouping = settings.callLogGrouping,
        blockNegativeSiaNumbers = settings.blockNegativeSiaNumbers,
        blockHiddenNumbers = settings.blockHiddenNumbers,
        blockDenylisted = settings.blockDenylisted,
        callScreeningHeld = PermissionHelper.isCallScreeningHeld(context),
        blockFailedVerification = settings.blockFailedVerificationEnabled,
        useMonitoringService = settings.useMonitoringService,
        notificationsKnown = settings.notificationsForKnownCallers,
        notificationsUnknown = settings.notificationsForUnknownCallers,
        notificationsBlocked = settings.notificationsForBlockedCalls,
        blockInLimitedMode = settings.blockInLimitedMode,
        countryCodeOverride = settings.countryCodeOverride.orEmpty(),
        countryCodeForReviewsOverride = settings.countryCodeForReviewsOverride.orEmpty(),
        detectedCountryCode = settings.cachedAutoDetectedCountryCode,
        databaseDownloadUrl = settings.databaseDownloadUrl.orEmpty(),
        saveCrashesToExternalStorage = settings.saveCrashesToExternalStorage,
        saveLogcatOnCrash = settings.saveLogcatOnCrash,
        dbFilteringEnabled = settings.isDbFilteringEnabled,
        dbFilteringPrefixesToKeep = settings.dbFilteringPrefixesToKeep.orEmpty(),
        dbFilteringThorough = settings.isDbFilteringThorough,
        dbFilteringKeepShortNumbers = settings.dbFilteringKeepShortNumbers,
        dbFilteringKeepShortNumbersMaxLength = settings.dbFilteringKeepShortNumbersMaxLength,
        reviewsForBlockedCalls = settings.reviewsForBlockedCalls,
    )

    override fun write(change: SettingsChange) {
        when (change) {
            is IncomingCallNotifications -> settings.incomingCallNotifications = change.value
            is IncomingCallAlerts -> settings.incomingCallAlerts = change.value
            is IncomingCallActions -> settings.incomingCallActions = change.value
            is AutoUpdates ->
                if (change.value) updateScheduler.scheduleAutoUpdates() else updateScheduler.cancelAutoUpdateWorker()
            is UseContacts -> settings.useContacts = change.value
            is UiMode -> {
                settings.uiMode = change.value
                App.setUiMode(change.value)
            }
            is CallLogGrouping -> settings.callLogGrouping = change.value
            is BlockNegativeSiaNumbers -> settings.blockNegativeSiaNumbers = change.value
            is BlockHiddenNumbers -> settings.blockHiddenNumbers = change.value
            is BlockDenylisted -> settings.blockDenylisted = change.value
            is BlockFailedVerification -> settings.blockFailedVerificationEnabled = change.value
            is UseMonitoringService -> settings.useMonitoringService = change.value
            is NotificationsKnown -> settings.notificationsForKnownCallers = change.value
            is NotificationsUnknown -> settings.notificationsForUnknownCallers = change.value
            is NotificationsBlocked -> settings.notificationsForBlockedCalls = change.value
            is BlockInLimitedMode -> settings.blockInLimitedMode = change.value
            is CountryCodeOverride -> settings.countryCodeOverride = change.value
            is CountryCodeForReviewsOverride -> settings.countryCodeForReviewsOverride = change.value
            is DatabaseDownloadUrl -> settings.databaseDownloadUrl = change.value
            is SaveCrashesToExternalStorage -> settings.saveCrashesToExternalStorage = change.value
            is SaveLogcatOnCrash -> settings.saveLogcatOnCrash = change.value
            is DbFilteringEnabled -> settings.isDbFilteringEnabled = change.value
            // Normalized like the old preference did.
            is DbFilteringPrefixesToKeep -> settings.dbFilteringPrefixesToKeep =
                DbFilteringUtils.formatPrefixes(DbFilteringUtils.parsePrefixes(change.value))
            is DbFilteringThorough -> settings.isDbFilteringThorough = change.value
            is DbFilteringKeepShortNumbers -> settings.dbFilteringKeepShortNumbers = change.value
            is DbFilteringKeepShortNumbersMaxLength -> settings.dbFilteringKeepShortNumbersMaxLength = change.value
            is ReviewsForBlockedCalls -> settings.reviewsForBlockedCalls = change.value
        }
    }
}
