package fr.vinetos.tranquille.presentation.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.vinetos.tranquille.App
import fr.vinetos.tranquille.BuildConfig
import fr.vinetos.tranquille.CallMonitoringService
import fr.vinetos.tranquille.DbManagementActivity
import fr.vinetos.tranquille.NotificationHelper
import fr.vinetos.tranquille.PermissionHelper
import fr.vinetos.tranquille.StartupReceiver
import fr.vinetos.tranquille.data.YacbHolder
import fr.vinetos.tranquille.utils.DbFilteringUtils
import fr.vinetos.tranquille.utils.DebuggingUtils
import fr.vinetos.tranquille.utils.FileUtils
import fr.vinetos.tranquille.utils.PackageManagerUtils
import fr.vinetos.tranquille.utils.SystemUtils
import fr.vinetos.tranquille.work.TaskService
import java.io.File
import java.io.IOException
import org.slf4j.LoggerFactory

/**
 * Side effects of settings changes (permission requests, the call screening role,
 * the monitoring service, the database filter), the same as in the old preference
 * fragments. The hosting activity forwards its lifecycle and result callbacks.
 */
class SettingsController(
    private val activity: AppCompatActivity,
    private val viewModel: SettingsViewModel,
) : SettingsActions {

    private val settings get() = App.getSettings()
    private val environment = SettingsEnvironment(fileBasedEncryption = SystemUtils.isFileBasedEncryptionEnabled())
    private var requestToken: PermissionHelper.RequestToken? = null

    fun onCreate(savedInstanceState: Bundle?) {
        requestToken = PermissionHelper.RequestToken.fromSavedInstanceState(savedInstanceState, STATE_REQUEST_TOKEN)
    }

    fun onStop() = updateNumberFilter()

    fun onSaveInstanceState(outState: Bundle) {
        requestToken?.onSaveInstanceState(outState, STATE_REQUEST_TOKEN)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        if (PermissionHelper.handleCallScreeningResult(activity, requestCode, resultCode, requestToken)) {
            viewModel.reload()
        }
    }

    /**
     * The settings pages. [onNavigateUpFromRoot] is null where settings are a
     * top-level destination; sub-pages always get an up button and handle Back.
     */
    @Composable
    fun Content(onNavigateUpFromRoot: (() -> Unit)?, modifier: Modifier = Modifier) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        var page by rememberSaveable { mutableStateOf(SettingsPage.ROOT) }

        fun up() {
            when (page) {
                SettingsPage.ROOT -> onNavigateUpFromRoot?.invoke()
                SettingsPage.ADVANCED -> page = SettingsPage.ROOT
                SettingsPage.DB_FILTERING -> {
                    updateNumberFilter()
                    page = SettingsPage.ADVANCED
                }
            }
        }
        BackHandler(enabled = page != SettingsPage.ROOT) { up() }
        LaunchedEffect(page) { if (page == SettingsPage.DB_FILTERING) prefillPrefixes() }

        SettingsScreen(
            page = page,
            state = state,
            environment = environment,
            actions = this,
            onNavigate = { page = it },
            onNavigateUp = if (page == SettingsPage.ROOT && onNavigateUpFromRoot == null) null else ::up,
            modifier = modifier,
        )
    }

    // SettingsActions

    override fun change(change: SettingsChange) {
        viewModel.change(change)
        when (change) {
            is SettingsChange.IncomingCallNotifications ->
                if (change.value) PermissionHelper.checkPermissions(activity, true, false, false, true)
            is SettingsChange.BlockNegativeSiaNumbers -> if (change.value) checkBlockingPermissions()
            is SettingsChange.BlockHiddenNumbers -> if (change.value) checkBlockingPermissions()
            is SettingsChange.BlockDenylisted -> if (change.value) checkBlockingPermissions()
            is SettingsChange.BlockFailedVerification -> if (change.value) checkBlockingPermissions()
            is SettingsChange.UseContacts ->
                if (change.value) PermissionHelper.checkPermissions(activity, false, false, true, false)
            is SettingsChange.UseMonitoringService -> {
                PackageManagerUtils.setComponentEnabledOrDefault(activity, StartupReceiver::class.java, change.value)
                if (change.value) CallMonitoringService.start(activity) else CallMonitoringService.stop(activity)
            }
            else -> Unit
        }
    }

    private fun checkBlockingPermissions() = PermissionHelper.checkPermissions(activity, false, true, false, false)

    override fun setCallScreening(enabled: Boolean) {
        if (enabled) {
            requestToken = PermissionHelper.requestCallScreening(activity)
        } else {
            // Shows how to turn it off in system settings; the switch follows the real state.
            PermissionHelper.disableCallScreening(activity)
        }
    }

    override fun openNotificationChannels() {
        NotificationHelper.initNotificationChannels(activity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startActivity(
                Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
            )
        }
    }

    override fun openDbManagement() = activity.startActivity(Intent(activity, DbManagementActivity::class.java))

    override fun exportLogcat() {
        val path = try {
            DebuggingUtils.saveLogcatInCache(activity).also { DebuggingUtils.appendDeviceInfo(it) }
        } catch (e: IOException) {
            LOG.warn("exportLogcat()", e); null
        } catch (e: InterruptedException) {
            LOG.warn("exportLogcat()", e); null
        }
        if (path != null) FileUtils.shareFile(activity, File(path))
    }

    override fun filterDb() {
        updateNumberFilter()
        TaskService.start(activity, TaskService.TASK_FILTER_DB)
    }

    private fun updateNumberFilter() {
        YacbHolder.getDbManager().setNumberFilter(DbFilteringUtils.getNumberFilter(settings))
    }

    /** Fills in the prefixes to keep once, from the detected country, like the old screen. */
    private fun prefillPrefixes() {
        if (settings.isDbFilteringPrefixesPrefilled) return
        settings.isDbFilteringPrefixesPrefilled = true
        if (!settings.dbFilteringPrefixesToKeep.isNullOrEmpty()) return
        Thread {
            val prefixes = DbFilteringUtils.detectPrefixes(activity, settings.cachedAutoDetectedCountryCode)
            if (prefixes.isNotEmpty()) {
                activity.runOnUiThread {
                    if (settings.dbFilteringPrefixesToKeep.isNullOrEmpty()) {
                        change(SettingsChange.DbFilteringPrefixesToKeep(DbFilteringUtils.formatPrefixes(prefixes)))
                    }
                }
            }
        }.start()
    }

    private companion object {
        const val STATE_REQUEST_TOKEN = "STATE_REQUEST_TOKEN"
        val LOG = LoggerFactory.getLogger(SettingsController::class.java)!!
    }
}
