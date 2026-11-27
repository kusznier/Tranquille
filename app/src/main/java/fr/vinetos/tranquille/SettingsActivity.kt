package fr.vinetos.tranquille

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.vinetos.tranquille.data.YacbHolder
import fr.vinetos.tranquille.presentation.settings.AppSettingsStore
import fr.vinetos.tranquille.presentation.settings.SettingsActions
import fr.vinetos.tranquille.presentation.settings.SettingsChange
import fr.vinetos.tranquille.presentation.settings.SettingsEnvironment
import fr.vinetos.tranquille.presentation.settings.SettingsPage
import fr.vinetos.tranquille.presentation.settings.SettingsScreen
import fr.vinetos.tranquille.presentation.settings.SettingsViewModel
import fr.vinetos.tranquille.ui.components.setTranquilleContent
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
 * Settings in Compose. Side effects of a change (permission requests, the call screening role,
 * the monitoring service, the database filter) are the same as in the old preference fragments.
 */
class SettingsActivity : AppCompatActivity(), SettingsActions {

    private val viewModel: SettingsViewModel by viewModels {
        viewModelFactory { initializer { SettingsViewModel(AppSettingsStore(applicationContext)) } }
    }

    private val settings get() = App.getSettings()
    private var requestToken: PermissionHelper.RequestToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestToken = PermissionHelper.RequestToken.fromSavedInstanceState(savedInstanceState, STATE_REQUEST_TOKEN)
        val environment = SettingsEnvironment(fileBasedEncryption = SystemUtils.isFileBasedEncryptionEnabled())

        setTranquilleContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            var page by rememberSaveable { mutableStateOf(SettingsPage.ROOT) }

            fun up() {
                when (page) {
                    SettingsPage.ROOT -> finish()
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
                onNavigateUp = ::up,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        // The call screening role and update schedule can change outside this screen.
        viewModel.reload()
    }

    override fun onStop() {
        updateNumberFilter()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        requestToken?.onSaveInstanceState(outState, STATE_REQUEST_TOKEN)
    }

    @Deprecated("Results of PermissionHelper and role requests arrive through the Activity API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (PermissionHelper.handleCallScreeningResult(this, requestCode, resultCode, requestToken)) {
            viewModel.reload()
        }
    }

    @Deprecated("Permission results still arrive through the Activity API used by PermissionHelper")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.handlePermissionsResult(
            this, requestCode, permissions, grantResults,
            settings.incomingCallNotifications, settings.callBlockingEnabled,
            settings.useContacts, settings.useNotification,
        )
    }

    // SettingsActions

    override fun change(change: SettingsChange) {
        viewModel.change(change)
        when (change) {
            is SettingsChange.IncomingCallNotifications ->
                if (change.value) PermissionHelper.checkPermissions(this, true, false, false, true)
            is SettingsChange.BlockNegativeSiaNumbers -> if (change.value) checkBlockingPermissions()
            is SettingsChange.BlockHiddenNumbers -> if (change.value) checkBlockingPermissions()
            is SettingsChange.BlockDenylisted -> if (change.value) checkBlockingPermissions()
            is SettingsChange.BlockFailedVerification -> if (change.value) checkBlockingPermissions()
            is SettingsChange.UseContacts ->
                if (change.value) PermissionHelper.checkPermissions(this, false, false, true, false)
            is SettingsChange.UseMonitoringService -> {
                PackageManagerUtils.setComponentEnabledOrDefault(this, StartupReceiver::class.java, change.value)
                if (change.value) CallMonitoringService.start(this) else CallMonitoringService.stop(this)
            }
            else -> Unit
        }
    }

    private fun checkBlockingPermissions() = PermissionHelper.checkPermissions(this, false, true, false, false)

    override fun setCallScreening(enabled: Boolean) {
        if (enabled) {
            requestToken = PermissionHelper.requestCallScreening(this)
        } else {
            // Shows how to turn it off in system settings; the switch follows the real state.
            PermissionHelper.disableCallScreening(this)
        }
    }

    override fun openNotificationChannels() {
        NotificationHelper.initNotificationChannels(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startActivity(
                Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
            )
        }
    }

    override fun openDbManagement() = startActivity(Intent(this, DbManagementActivity::class.java))

    override fun exportLogcat() {
        val path = try {
            DebuggingUtils.saveLogcatInCache(this).also { DebuggingUtils.appendDeviceInfo(it) }
        } catch (e: IOException) {
            LOG.warn("exportLogcat()", e); null
        } catch (e: InterruptedException) {
            LOG.warn("exportLogcat()", e); null
        }
        if (path != null) FileUtils.shareFile(this, File(path))
    }

    override fun filterDb() {
        updateNumberFilter()
        TaskService.start(this, TaskService.TASK_FILTER_DB)
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
            val prefixes = DbFilteringUtils.detectPrefixes(this, settings.cachedAutoDetectedCountryCode)
            if (prefixes.isNotEmpty()) {
                runOnUiThread {
                    if (settings.dbFilteringPrefixesToKeep.isNullOrEmpty()) {
                        change(SettingsChange.DbFilteringPrefixesToKeep(DbFilteringUtils.formatPrefixes(prefixes)))
                    }
                }
            }
        }.start()
    }

    private companion object {
        const val STATE_REQUEST_TOKEN = "STATE_REQUEST_TOKEN"
        val LOG = LoggerFactory.getLogger(SettingsActivity::class.java)!!
    }
}
