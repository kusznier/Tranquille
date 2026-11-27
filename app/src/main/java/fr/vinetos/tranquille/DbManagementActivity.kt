package fr.vinetos.tranquille

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.vinetos.tranquille.data.YacbHolder
import fr.vinetos.tranquille.event.SecondaryDbUpdateFinished
import fr.vinetos.tranquille.presentation.dbmanagement.DbMaintenance
import fr.vinetos.tranquille.presentation.dbmanagement.DbManagementScreen
import fr.vinetos.tranquille.presentation.dbmanagement.DbManagementViewModel
import fr.vinetos.tranquille.ui.components.setTranquilleContent
import fr.vinetos.tranquille.work.TaskService
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Date

class DbManagementActivity : AppCompatActivity() {

    private val viewModel: DbManagementViewModel by viewModels {
        viewModelFactory { initializer { DbManagementViewModel(AppDbMaintenance()) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTranquilleContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            DbManagementScreen(
                state = state,
                onNavigateUp = ::finish,
                onInfo = viewModel::showInfo,
                onUpdate = viewModel::update,
                onAskReset = viewModel::askReset,
                onConfirmReset = viewModel::confirmReset,
                onCancelReset = viewModel::cancelReset,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        EventUtils.register(this)
    }

    override fun onStop() {
        EventUtils.unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onSecondaryDbUpdateFinished(event: SecondaryDbUpdateFinished) {
        viewModel.onUpdateFinished()
    }

    /** The same operations and diagnostic text as the old screen. */
    private inner class AppDbMaintenance : DbMaintenance {
        override fun info(): String = buildString {
            val siaMetadata = YacbHolder.getSiaMetadata()
            val communityDatabase = YacbHolder.getCommunityDatabase()
            append("DB info:\n")
            append("Operational: ").append(communityDatabase.isOperational).append('\n')
            append("Base version: ").append(communityDatabase.baseDbVersion)
            append(" (SIA: ").append(siaMetadata.siaAppVersion).append(")\n")
            append("Effective version: ").append(communityDatabase.effectiveDbVersion).append('\n')
            append("Last update time: ").append(dateOrNever(App.getSettings().lastUpdateTime)).append('\n')
            append("Last update check time: ").append(dateOrNever(App.getSettings().lastUpdateCheckTime)).append('\n')
            val featuredDatabase = YacbHolder.getFeaturedDatabase()
            append("\nFeatured DB info:\n")
            append("Operational: ").append(featuredDatabase.isOperational).append('\n')
            append("Effective version: ").append(featuredDatabase.baseDbVersion).append('\n')
        }

        private fun dateOrNever(time: Long) = if (time > 0) Date(time).toString() else "never"

        override fun startUpdate() {
            TaskService.start(this@DbManagementActivity, TaskService.TASK_UPDATE_SECONDARY_DB)
        }

        override fun resetAll() {
            YacbHolder.getCommunityDatabase().resetSecondaryDatabase()
            YacbHolder.getDbManager().removeMainDb()
            YacbHolder.getCommunityDatabase().reload()
            YacbHolder.getFeaturedDatabase().reload()
            YacbHolder.getSiaMetadata().reload()
        }

        override fun resetUpdates() {
            YacbHolder.getCommunityDatabase().resetSecondaryDatabase()
        }

        override fun effectiveVersion(): Int = YacbHolder.getCommunityDatabase().effectiveDbVersion
    }
}
