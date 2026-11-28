package fr.vinetos.tranquille.presentation.settings

import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.vinetos.tranquille.R
import fr.vinetos.tranquille.presentation.settings.SettingsChange.*
import fr.vinetos.tranquille.ui.components.ClickSetting
import fr.vinetos.tranquille.ui.components.ListSetting
import fr.vinetos.tranquille.ui.components.MultiSelectSetting
import fr.vinetos.tranquille.ui.components.SettingsCategory
import fr.vinetos.tranquille.ui.components.SwitchSetting
import fr.vinetos.tranquille.ui.components.TextSetting
import fr.vinetos.tranquille.ui.components.TranquilleTopBar

enum class SettingsPage { ROOT, ADVANCED, DB_FILTERING }

/** Actions that leave the settings list or need the activity. */
interface SettingsActions {
    fun change(change: SettingsChange)
    fun setCallScreening(enabled: Boolean)
    fun openNotificationChannels()
    fun openDbManagement()
    fun exportLogcat()
    fun filterDb()
}

/** Platform facts that decide which settings are shown. */
data class SettingsEnvironment(
    val sdk: Int = Build.VERSION.SDK_INT,
    val fileBasedEncryption: Boolean = false,
)

@Composable
fun SettingsScreen(
    page: SettingsPage,
    state: SettingsUiState,
    environment: SettingsEnvironment,
    actions: SettingsActions,
    onNavigate: (SettingsPage) -> Unit,
    /** Null on the root page where settings are a top-level destination. */
    onNavigateUp: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(
        when (page) {
            SettingsPage.ROOT -> R.string.title_settings_activity
            SettingsPage.ADVANCED -> R.string.settings_screen_advanced
            SettingsPage.DB_FILTERING -> R.string.settings_screen_db_filtering
        }
    )
    Scaffold(modifier = modifier, topBar = { TranquilleTopBar(title, onNavigateUp = onNavigateUp) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            when (page) {
                SettingsPage.ROOT -> rootPage(state, environment, actions, onNavigate)
                SettingsPage.ADVANCED -> advancedPage(state, environment, actions, onNavigate)
                SettingsPage.DB_FILTERING -> dbFilteringPage(state, actions)
            }
        }
    }
}

private fun LazyListScope.rootPage(
    state: SettingsUiState,
    env: SettingsEnvironment,
    actions: SettingsActions,
    onNavigate: (SettingsPage) -> Unit,
) {
    item { SettingsCategory(stringResource(R.string.settings_category_main)) }
    item {
        SwitchSetting(
            stringResource(R.string.incoming_call_notifications),
            stringResource(R.string.incoming_call_notifications_summary),
            state.incomingCallNotifications,
            { actions.change(IncomingCallNotifications(it)) },
        )
    }
    item {
        SwitchSetting(
            stringResource(R.string.incoming_call_alerts),
            stringResource(R.string.incoming_call_alerts_summary),
            state.incomingCallAlerts,
            { actions.change(IncomingCallAlerts(it)) },
            enabled = state.incomingCallNotifications,
        )
    }
    item {
        SwitchSetting(
            stringResource(R.string.incoming_call_actions),
            stringResource(R.string.incoming_call_actions_summary),
            state.incomingCallActions,
            { actions.change(IncomingCallActions(it)) },
            enabled = state.incomingCallNotifications,
        )
    }
    item {
        SwitchSetting(
            stringResource(R.string.auto_updates), stringResource(R.string.auto_updates_summary),
            state.autoUpdates, { actions.change(AutoUpdates(it)) },
        )
    }
    item {
        SwitchSetting(
            stringResource(R.string.use_contacts), stringResource(R.string.use_contacts_summary),
            state.useContacts, { actions.change(UseContacts(it)) },
        )
    }
    item {
        val entries = stringArrayResource(R.array.ui_mode_entries).toList()
        val values = stringArrayResource(R.array.ui_mode_entry_values).map { it.toInt() }
        ListSetting(stringResource(R.string.ui_mode), entries, values.indexOf(state.uiMode)) {
            actions.change(UiMode(values[it]))
        }
    }
    item {
        val entries = stringArrayResource(R.array.call_log_grouping_entries).toList()
        val values = stringArrayResource(R.array.call_log_grouping_values).toList()
        ListSetting(stringResource(R.string.call_log_grouping), entries, values.indexOf(state.callLogGrouping)) {
            actions.change(CallLogGrouping(values[it]))
        }
    }

    item { SettingsCategory(stringResource(R.string.settings_category_call_blocking)) }
    item {
        SwitchSetting(
            stringResource(R.string.block_negative_sia), stringResource(R.string.block_negative_sia_numbers_summary),
            state.blockNegativeSiaNumbers, { actions.change(BlockNegativeSiaNumbers(it)) },
        )
    }
    if (env.sdk >= Build.VERSION_CODES.Q) {
        item {
            // Silencing needs the call screening role; without it calls are rejected as before.
            val available = state.callScreeningHeld && state.blockNegativeSiaNumbers
            SwitchSetting(
                stringResource(R.string.silence_rated_calls),
                stringResource(R.string.silence_rated_calls_summary),
                available && state.silenceRatedCalls,
                { actions.change(SilenceRatedCalls(it)) },
                enabled = available,
            )
        }
    }
    item {
        SwitchSetting(
            stringResource(R.string.block_hidden_number), stringResource(R.string.block_hidden_number_summary),
            state.blockHiddenNumbers, { actions.change(BlockHiddenNumbers(it)) },
        )
    }
    item {
        SwitchSetting(
            stringResource(R.string.block_denylisted), stringResource(R.string.block_denylisted_summary),
            state.blockDenylisted, { actions.change(BlockDenylisted(it)) },
        )
    }
    item {
        SwitchSetting(
            stringResource(R.string.use_call_screening_service),
            stringResource(R.string.use_call_screening_service_summary),
            state.callScreeningHeld, actions::setCallScreening,
        )
    }
    if (env.sdk >= Build.VERSION_CODES.R) {
        item {
            SwitchSetting(
                stringResource(R.string.block_failed_verification),
                stringResource(R.string.block_failed_verification_summary),
                state.callScreeningHeld && state.blockFailedVerification,
                { actions.change(BlockFailedVerification(it)) },
                enabled = state.callScreeningHeld,
            )
        }
    }
    item {
        SwitchSetting(
            stringResource(R.string.use_monitoring_service), stringResource(R.string.use_monitoring_service_summary),
            state.useMonitoringService, { actions.change(UseMonitoringService(it)) },
        )
    }

    item { SettingsCategory(stringResource(R.string.notification_settings)) }
    if (env.sdk >= Build.VERSION_CODES.O) {
        item { ClickSetting(stringResource(R.string.notification_settings), null, onClick = actions::openNotificationChannels) }
    } else {
        item {
            SwitchSetting(
                stringResource(R.string.show_notifications_for_known_callers),
                stringResource(R.string.show_notifications_for_known_callers_summary),
                state.notificationsKnown, { actions.change(NotificationsKnown(it)) },
            )
        }
        item {
            SwitchSetting(
                stringResource(R.string.show_notifications_for_unknown_callers),
                stringResource(R.string.show_notifications_for_unknown_callers_summary),
                state.notificationsUnknown, { actions.change(NotificationsUnknown(it)) },
            )
        }
        item { BlockedNotificationsSwitch(state.notificationsBlocked, actions) }
    }
    item {
        SwitchSetting(
            stringResource(R.string.reviews_for_blocked_calls),
            stringResource(R.string.reviews_for_blocked_calls_summary),
            state.reviewsForBlockedCalls, { actions.change(ReviewsForBlockedCalls(it)) },
        )
    }

    item { SettingsCategory(stringResource(R.string.settings_screen_advanced)) }
    item { ClickSetting(stringResource(R.string.settings_screen_advanced), null) { onNavigate(SettingsPage.ADVANCED) } }
}

/** Turning off blocked-call notifications (Android 7 only) asks for confirmation, as before. */
@Composable
private fun BlockedNotificationsSwitch(checked: Boolean, actions: SettingsActions) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    SwitchSetting(
        stringResource(R.string.show_notifications_for_blocked_calls),
        stringResource(R.string.show_notifications_for_blocked_calls_summary),
        checked,
        { if (it) actions.change(NotificationsBlocked(true)) else confirm = true },
    )
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.are_you_sure)) },
            text = { Text(stringResource(R.string.blocked_call_notifications_disable_message)) },
            confirmButton = {
                TextButton(onClick = { confirm = false; actions.change(NotificationsBlocked(false)) }) {
                    Text(stringResource(R.string.blocked_call_notifications_disable_confirmation))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

private fun LazyListScope.advancedPage(
    state: SettingsUiState,
    env: SettingsEnvironment,
    actions: SettingsActions,
    onNavigate: (SettingsPage) -> Unit,
) {
    item { SettingsCategory(stringResource(R.string.settings_category_database)) }
    item { ClickSetting(stringResource(R.string.open_db_management_activity), null, onClick = actions::openDbManagement) }
    item { ClickSetting(stringResource(R.string.settings_screen_db_filtering), null) { onNavigate(SettingsPage.DB_FILTERING) } }

    if (env.fileBasedEncryption) {
        item {
            val entries = stringArrayResource(R.array.block_in_limited_mode_entries).toList()
            val values = stringArrayResource(R.array.block_in_limited_mode_values).toList()
            val selected = values.indices.filterTo(HashSet()) { values[it] in state.blockInLimitedMode }
            val summary = stringResource(R.string.block_in_limited_mode_summary) + ".\n" +
                selected.sorted().joinToString(", ") { entries[it] }
            MultiSelectSetting(stringResource(R.string.block_in_limited_mode), summary, entries, selected) { chosen ->
                actions.change(BlockInLimitedMode(chosen.mapTo(HashSet()) { values[it] }))
            }
        }
    }

    item { SettingsCategory(stringResource(R.string.settings_category_country_codes)) }
    item { CountryCodesInfo(state.detectedCountryCode) }
    item {
        CountryCodeSetting(
            stringResource(R.string.country_code_override), stringResource(R.string.country_code_override_summary),
            state.countryCodeOverride,
        ) { actions.change(CountryCodeOverride(it)) }
    }
    item {
        CountryCodeSetting(
            stringResource(R.string.country_code_for_reviews_override),
            stringResource(R.string.country_code_for_reviews_override_summary),
            state.countryCodeForReviewsOverride,
        ) { actions.change(CountryCodeForReviewsOverride(it)) }
    }
    item {
        TextSetting(
            stringResource(R.string.database_download_url), state.databaseDownloadUrl.ifEmpty { null },
            state.databaseDownloadUrl, { actions.change(DatabaseDownloadUrl(it)) }, KeyboardType.Uri,
        )
    }

    item { SettingsCategory(stringResource(R.string.settings_category_debugging)) }
    item { ClickSetting(stringResource(R.string.export_logcat), stringResource(R.string.export_logcat_summary), onClick = actions::exportLogcat) }
    item {
        SwitchSetting(
            stringResource(R.string.save_crashes_to_external_storage),
            stringResource(R.string.save_crashes_to_external_storage_summary),
            state.saveCrashesToExternalStorage, { actions.change(SaveCrashesToExternalStorage(it)) },
        )
    }
    item {
        SwitchSetting(
            stringResource(R.string.save_logcat_on_crash), stringResource(R.string.save_logcat_on_crash_summary),
            state.saveLogcatOnCrash, { actions.change(SaveLogcatOnCrash(it)) },
        )
    }
}

@Composable
private fun CountryCodesInfo(detected: String?) {
    var open by rememberSaveable { mutableStateOf(false) }
    val text = stringResource(R.string.country_codes_info_summary) + ". " +
        stringResource(R.string.country_codes_info_summary_addition, detected.toString())
    ClickSetting(stringResource(R.string.country_codes_info), text) { open = true }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.settings_category_country_codes)) },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.back)) } },
        )
    }
}

@Composable
private fun CountryCodeSetting(title: String, summary: String, value: String, onChange: (String) -> Unit) {
    val error = stringResource(R.string.country_code_incorrect_format)
    TextSetting(
        title, if (value.isEmpty()) summary else "$summary\n$value", value, onChange,
        validate = { if (SettingsViewModel.isValidCountryCode(it)) null else error },
    )
}

private fun LazyListScope.dbFilteringPage(state: SettingsUiState, actions: SettingsActions) {
    item { DbFilteringInfo() }
    item {
        SwitchSetting(
            stringResource(R.string.db_filtering_enabled), stringResource(R.string.db_filtering_enabled_summary),
            state.dbFilteringEnabled, { actions.change(DbFilteringEnabled(it)) },
        )
    }
    item {
        val summary = stringResource(R.string.db_filtering_prefixes_to_keep_summary)
        TextSetting(
            stringResource(R.string.db_filtering_prefixes_to_keep),
            if (state.dbFilteringPrefixesToKeep.isEmpty()) summary else "$summary\n${state.dbFilteringPrefixesToKeep}",
            state.dbFilteringPrefixesToKeep, { actions.change(DbFilteringPrefixesToKeep(it)) },
        )
    }
    item {
        SwitchSetting(
            stringResource(R.string.db_filtering_thorough), stringResource(R.string.db_filtering_thorough_summary),
            state.dbFilteringThorough, { actions.change(DbFilteringThorough(it)) },
        )
    }
    item {
        SwitchSetting(
            stringResource(R.string.db_filtering_keep_short_numbers),
            stringResource(R.string.db_filtering_keep_short_numbers_summary),
            state.dbFilteringKeepShortNumbers, { actions.change(DbFilteringKeepShortNumbers(it)) },
        )
    }
    item {
        val summary = stringResource(R.string.db_filtering_keep_short_numbers_max_length_summary)
        val invalid = stringResource(R.string.number_pattern_incorrect)
        TextSetting(
            stringResource(R.string.db_filtering_keep_short_numbers_max_length),
            "$summary\n${state.dbFilteringKeepShortNumbersMaxLength}",
            state.dbFilteringKeepShortNumbersMaxLength.toString(),
            { actions.change(DbFilteringKeepShortNumbersMaxLength(it.toInt())) },
            KeyboardType.Number,
            validate = { if (it.toIntOrNull()?.let { n -> n >= 0 } == true) null else invalid },
        )
    }
    item {
        ClickSetting(
            stringResource(R.string.db_filtering_filter_db), stringResource(R.string.db_filtering_filter_db_summary),
            onClick = actions::filterDb,
        )
    }
}

@Composable
private fun DbFilteringInfo() {
    var open by rememberSaveable { mutableStateOf(false) }
    val summary = stringResource(R.string.db_filtering_info_summary)
    ClickSetting(stringResource(R.string.db_filtering_info), summary) { open = true }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.settings_screen_db_filtering)) },
            text = { Text(summary) },
            confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.back)) } },
        )
    }
}
