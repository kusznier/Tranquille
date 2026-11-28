package fr.vinetos.tranquille

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.vinetos.tranquille.data.NumberInfo
import fr.vinetos.tranquille.presentation.info.NumberInfoDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.vinetos.tranquille.event.CallEndedEvent
import fr.vinetos.tranquille.event.MainDbDownloadFinishedEvent
import fr.vinetos.tranquille.event.MainDbDownloadingEvent
import fr.vinetos.tranquille.event.SecondaryDbUpdateFinished
import fr.vinetos.tranquille.presentation.denylist.DenylistActivity
import fr.vinetos.tranquille.presentation.denylist.EditDenylistItemActivity
import fr.vinetos.tranquille.presentation.main.AppMainRepository
import fr.vinetos.tranquille.presentation.main.MainActions
import fr.vinetos.tranquille.presentation.main.MainScreen
import fr.vinetos.tranquille.presentation.main.MainViewModel
import fr.vinetos.tranquille.presentation.onboarding.OnboardingActions
import fr.vinetos.tranquille.presentation.onboarding.OnboardingScreen
import fr.vinetos.tranquille.ui.components.setTranquilleContent
import fr.vinetos.tranquille.work.TaskService
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity(), MainActions {

    private val settings get() = App.getSettings()

    /** Number whose info dialog is open, if any. */
    private var openedNumber by mutableStateOf<NumberInfo?>(null)

    private val viewModel: MainViewModel by viewModels {
        viewModelFactory { initializer { MainViewModel(AppMainRepository(applicationContext)) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTranquilleContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val onboarding = state.onboarding
            when {
                onboarding != null -> {
                    BackHandler(enabled = onboarding.index > 0) { viewModel.previousOnboardingStep() }
                    OnboardingScreen(onboarding, state.status, onboardingActions)
                }
                // Decided right after the first status load; avoids flashing the main screen.
                state.onboardingPending -> Surface(Modifier.fillMaxSize()) {}
                else -> MainScreen(state = state, actions = this)
            }
            openedNumber?.let { info ->
                NumberInfoDialog(info, AppNumberInfoActions(this), onDismiss = { openedNumber = null })
            }
        }
    }

    override fun onStart() {
        super.onStart()
        EventUtils.register(this)
        // On the first run the setup screen asks for permissions with an explanation instead.
        if (settings.onboardingDone) checkPermissions()
        viewModel.refresh()
    }

    private val onboardingActions = object : OnboardingActions {
        override fun requestCallScreening() = this@MainActivity.requestCallScreening()
        override fun requestPermissions() = checkPermissions()
        override fun downloadDatabase() = this@MainActivity.downloadDatabase()
        override fun setAutoUpdates(enabled: Boolean) = viewModel.setAutoUpdates(enabled)
        override fun next() = viewModel.nextOnboardingStep()
    }

    override fun onStop() {
        EventUtils.unregister(this)
        super.onStop()
    }

    @Deprecated("Permission results still arrive through the Activity API used by PermissionHelper")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
    ) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.handlePermissionsResult(
            this, requestCode, permissions, grantResults,
            settings.incomingCallNotifications, settings.callBlockingEnabled,
            settings.useContacts, settings.useNotification,
        )
        viewModel.refresh()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onCallEvent(event: CallEndedEvent) {
        Handler(Looper.getMainLooper()).postDelayed({ viewModel.refresh() }, 1000)
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onMainDbDownloading(event: MainDbDownloadingEvent) {
        viewModel.refreshStatus()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onMainDbDownloadFinished(event: MainDbDownloadFinishedEvent) {
        viewModel.refresh()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onSecondaryDbUpdateFinished(event: SecondaryDbUpdateFinished) {
        if (event.updated) viewModel.refresh() else viewModel.refreshStatus()
    }

    private fun checkPermissions() {
        PermissionHelper.checkPermissions(
            this,
            settings.incomingCallNotifications, settings.callBlockingEnabled,
            settings.useContacts, settings.useNotification,
        )
    }

    // MainActions

    override fun lookupNumber() = startActivity(Intent(this, LookupNumberActivity::class.java))
    override fun openDenylist() = startActivity(Intent(this, DenylistActivity::class.java))
    override fun openSettings() = startActivity(Intent(this, SettingsActivity::class.java))
    override fun openAbout() = startActivity(Intent(this, AboutActivity::class.java))

    override fun blockNumber() =
        startActivity(EditDenylistItemActivity.getIntent(this, null, null))

    override fun openCall(row: CallLogRow) {
        openedNumber = row.numberInfo
    }

    override fun downloadDatabase() {
        TaskService.start(this, TaskService.TASK_DOWNLOAD_MAIN_DB)
    }

    override fun requestCallScreening() {
        PermissionHelper.requestCallScreening(this)
    }

    override fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, packageName)
        } else {
            Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.fromParts("package", packageName, null))
        }
        IntentHelper.startActivity(this, intent)
    }

    override fun requestCallLogPermission() = checkPermissions()

    override fun setBlockByRating(block: Boolean) {
        viewModel.setBlockByRating(block)
        if (block) checkPermissions()
    }
}
