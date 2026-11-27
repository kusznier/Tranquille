package fr.vinetos.tranquille.presentation.denylist

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.vinetos.tranquille.EventUtils
import fr.vinetos.tranquille.R
import fr.vinetos.tranquille.event.DenylistChangedEvent
import fr.vinetos.tranquille.event.DenylistItemChangedEvent
import fr.vinetos.tranquille.ui.components.setTranquilleContent
import fr.vinetos.tranquille.utils.FileUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

class DenylistActivity : AppCompatActivity() {

    private val viewModel: DenylistViewModel by viewModels {
        viewModelFactory { initializer { DenylistViewModel(AppDenylistRepository) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTranquilleContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) viewModel.import { contentResolver.openInputStream(uri) }
            }

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        is DenylistEvent.Deleted -> {
                            val result = snackbarHostState.showSnackbar(
                                message = resources.getQuantityString(
                                    R.plurals.denylist_deleted_count, event.items.size, event.items.size
                                ),
                                actionLabel = getString(R.string.denylist_undo),
                                duration = SnackbarDuration.Long,
                            )
                            if (result == SnackbarResult.ActionPerformed) viewModel.restore(event.items)
                        }
                        is DenylistEvent.Imported -> snackbarHostState.showSnackbar(
                            getString(
                                R.string.denylist_import_result,
                                event.result.added, event.result.updated, event.result.skipped,
                            ),
                            duration = SnackbarDuration.Long,
                        )
                        DenylistEvent.ImportFailed -> snackbarHostState.showSnackbar(
                            getString(R.string.denylist_import_failed), duration = SnackbarDuration.Long,
                        )
                        is DenylistEvent.Exported -> FileUtils.shareFile(this@DenylistActivity, event.file)
                        DenylistEvent.ExportFailed -> snackbarHostState.showSnackbar(
                            getString(R.string.denylist_export_failed),
                        )
                    }
                }
            }

            DenylistScreen(
                state = state,
                snackbarHostState = snackbarHostState,
                onNavigateUp = ::finish,
                onQueryChange = viewModel::onQueryChange,
                onBlockDenylistedChange = viewModel::setBlockDenylisted,
                onItemClick = { startActivity(EditDenylistItemActivity.getIntent(this, it.id)) },
                onToggleSelection = viewModel::toggleSelection,
                onSelectAll = viewModel::selectAllVisible,
                onClearSelection = viewModel::clearSelection,
                onDeleteSelected = viewModel::deleteSelected,
                onAdd = { startActivity(EditDenylistItemActivity.getIntent(this, null, null)) },
                onImport = { importLauncher.launch("*/*") },
                onExport = { viewModel.export(File(cacheDir, "Tranquille_backup.csv")) },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        EventUtils.register(this)
        viewModel.reload()
    }

    override fun onStop() {
        EventUtils.unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onDenylistChanged(event: DenylistChangedEvent) {
        viewModel.reload()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onDenylistItemChanged(event: DenylistItemChangedEvent) {
        viewModel.reload()
    }
}
