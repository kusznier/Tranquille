package fr.vinetos.tranquille

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.vinetos.tranquille.presentation.settings.AppSettingsStore
import fr.vinetos.tranquille.presentation.settings.SettingsController
import fr.vinetos.tranquille.presentation.settings.SettingsViewModel
import fr.vinetos.tranquille.ui.components.setTranquilleContent

/** Settings in Compose; the side effects of changes live in [SettingsController]. */
class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels {
        viewModelFactory { initializer { SettingsViewModel(AppSettingsStore(applicationContext)) } }
    }

    private val settings get() = App.getSettings()
    private val controller by lazy { SettingsController(this, viewModel) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller.onCreate(savedInstanceState)
        setTranquilleContent { controller.Content(onNavigateUpFromRoot = ::finish) }
    }

    override fun onStart() {
        super.onStart()
        // The call screening role and update schedule can change outside this screen.
        viewModel.reload()
    }

    override fun onStop() {
        controller.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        controller.onSaveInstanceState(outState)
    }

    @Deprecated("Results of PermissionHelper and role requests arrive through the Activity API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        controller.onActivityResult(requestCode, resultCode)
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
}
