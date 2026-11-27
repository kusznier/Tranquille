package fr.vinetos.tranquille

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.vinetos.tranquille.data.YacbHolder
import fr.vinetos.tranquille.event.MainDbDownloadFinishedEvent
import fr.vinetos.tranquille.event.SecondaryDbUpdateFinished
import fr.vinetos.tranquille.event.SecondaryDbUpdatingEvent
import fr.vinetos.tranquille.presentation.about.AboutScreen
import fr.vinetos.tranquille.presentation.about.AboutViewModel
import fr.vinetos.tranquille.presentation.about.DbStatusSource
import fr.vinetos.tranquille.ui.components.setTranquilleContent
import fr.vinetos.tranquille.work.TaskService
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class AboutActivity : AppCompatActivity() {

    private val viewModel: AboutViewModel by viewModels {
        viewModelFactory {
            initializer { AboutViewModel(BuildConfig.VERSION_NAME, AppDbStatusSource) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTranquilleContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            AboutScreen(
                state = state,
                onNavigateUp = ::finish,
                onCheckForUpdates = {
                    viewModel.onUpdateStarted()
                    TaskService.start(this, TaskService.TASK_UPDATE_SECONDARY_DB)
                },
                onOpenDbManagement = {
                    startActivity(Intent(this, DbManagementActivity::class.java))
                },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        EventUtils.register(this)
        viewModel.refresh()
    }

    override fun onStop() {
        EventUtils.unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onMainDbDownloadFinished(event: MainDbDownloadFinishedEvent) {
        viewModel.refresh()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onSecondaryDbUpdating(event: SecondaryDbUpdatingEvent) {
        viewModel.onUpdateStarted()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onSecondaryDbUpdateFinished(event: SecondaryDbUpdateFinished) {
        viewModel.onUpdateFinished()
    }

    /** Reads the database state from the existing holders, unchanged. */
    private object AppDbStatusSource : DbStatusSource {
        override fun dbVersion(): Int? {
            val db = YacbHolder.getCommunityDatabase()
            return if (db.isOperational) db.effectiveDbVersion else null
        }

        override fun lastUpdateCheckTime(): Long = App.getSettings().lastUpdateCheckTime

        override fun isUpdating(): Boolean =
            EventUtils.bus().getStickyEvent(SecondaryDbUpdatingEvent::class.java) != null
    }
}
