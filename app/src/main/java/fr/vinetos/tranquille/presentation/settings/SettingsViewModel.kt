package fr.vinetos.tranquille.presentation.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Every value the settings screens show, read through [SettingsStore]. */
data class SettingsUiState(
    // Main
    val incomingCallNotifications: Boolean = true,
    val autoUpdates: Boolean = false,
    val useContacts: Boolean = false,
    val uiMode: Int = -1,
    val callLogGrouping: String = "consecutive",
    // Call blocking
    val blockNegativeSiaNumbers: Boolean = false,
    val blockHiddenNumbers: Boolean = false,
    val blockDenylisted: Boolean = true,
    val callScreeningHeld: Boolean = false,
    val blockFailedVerification: Boolean = false,
    val useMonitoringService: Boolean = false,
    // Notifications before Android 8 (no channels)
    val notificationsKnown: Boolean = false,
    val notificationsUnknown: Boolean = false,
    val notificationsBlocked: Boolean = true,
    // Advanced
    val blockInLimitedMode: Set<String> = emptySet(),
    val countryCodeOverride: String = "",
    val countryCodeForReviewsOverride: String = "",
    val detectedCountryCode: String? = null,
    val databaseDownloadUrl: String = "",
    val saveCrashesToExternalStorage: Boolean = false,
    val saveLogcatOnCrash: Boolean = false,
    // DB filtering
    val dbFilteringEnabled: Boolean = false,
    val dbFilteringPrefixesToKeep: String = "",
    val dbFilteringThorough: Boolean = true,
    val dbFilteringKeepShortNumbers: Boolean = true,
    val dbFilteringKeepShortNumbersMaxLength: Int = 5,
    // Online reviews for blocked calls (U1b), off by default
    val reviewsForBlockedCalls: Boolean = false,
)

/** Reads and writes the values through the existing Settings class and system services. */
interface SettingsStore {
    fun load(): SettingsUiState
    fun write(change: SettingsChange)
}

/** One user change; the store persists it the same way the old preference screens did. */
sealed interface SettingsChange {
    data class IncomingCallNotifications(val value: Boolean) : SettingsChange
    data class AutoUpdates(val value: Boolean) : SettingsChange
    data class UseContacts(val value: Boolean) : SettingsChange
    data class UiMode(val value: Int) : SettingsChange
    data class CallLogGrouping(val value: String) : SettingsChange
    data class BlockNegativeSiaNumbers(val value: Boolean) : SettingsChange
    data class BlockHiddenNumbers(val value: Boolean) : SettingsChange
    data class BlockDenylisted(val value: Boolean) : SettingsChange
    data class BlockFailedVerification(val value: Boolean) : SettingsChange
    data class UseMonitoringService(val value: Boolean) : SettingsChange
    data class NotificationsKnown(val value: Boolean) : SettingsChange
    data class NotificationsUnknown(val value: Boolean) : SettingsChange
    data class NotificationsBlocked(val value: Boolean) : SettingsChange
    data class BlockInLimitedMode(val value: Set<String>) : SettingsChange
    data class CountryCodeOverride(val value: String) : SettingsChange
    data class CountryCodeForReviewsOverride(val value: String) : SettingsChange
    data class DatabaseDownloadUrl(val value: String) : SettingsChange
    data class SaveCrashesToExternalStorage(val value: Boolean) : SettingsChange
    data class SaveLogcatOnCrash(val value: Boolean) : SettingsChange
    data class DbFilteringEnabled(val value: Boolean) : SettingsChange
    data class DbFilteringPrefixesToKeep(val value: String) : SettingsChange
    data class DbFilteringThorough(val value: Boolean) : SettingsChange
    data class DbFilteringKeepShortNumbers(val value: Boolean) : SettingsChange
    data class DbFilteringKeepShortNumbersMaxLength(val value: Int) : SettingsChange
    data class ReviewsForBlockedCalls(val value: Boolean) : SettingsChange
}

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    private val _state = MutableStateFlow(store.load())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    /** Values can change outside the screen (role granted, update scheduled), so reload on start. */
    fun reload() {
        _state.value = store.load()
    }

    fun change(change: SettingsChange) {
        store.write(change)
        reload()
    }

    companion object {
        private val COUNTRY_CODE = Regex("^[a-zA-Z]{2}$")

        /** Empty (automatic) or a two-letter country code, as before. */
        fun isValidCountryCode(value: String) = value.isEmpty() || COUNTRY_CODE.matches(value)
    }
}
