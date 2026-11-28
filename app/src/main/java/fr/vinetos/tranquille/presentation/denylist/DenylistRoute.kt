package fr.vinetos.tranquille.presentation.denylist

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.vinetos.tranquille.R
import fr.vinetos.tranquille.utils.FileUtils
import java.io.File

/**
 * The denylist screen together with what used to live in its activity: the import
 * picker, snackbars for delete/import/export results and opening the edit screen.
 * [onNavigateUp] is null where the screen is a top-level destination.
 */
@Composable
fun DenylistRoute(
    viewModel: DenylistViewModel,
    onNavigateUp: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as Activity
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.import { activity.contentResolver.openInputStream(uri) }
    }

    // Back leaves selection mode first, like the close button.
    BackHandler(enabled = state.selectionMode) { viewModel.clearSelection() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DenylistEvent.Deleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = activity.resources.getQuantityString(
                            R.plurals.denylist_deleted_count, event.items.size, event.items.size
                        ),
                        actionLabel = activity.getString(R.string.denylist_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.restore(event.items)
                }
                is DenylistEvent.Imported -> snackbarHostState.showSnackbar(
                    activity.getString(
                        R.string.denylist_import_result,
                        event.result.added, event.result.updated, event.result.skipped,
                    ),
                    duration = SnackbarDuration.Long,
                )
                DenylistEvent.ImportFailed -> snackbarHostState.showSnackbar(
                    activity.getString(R.string.denylist_import_failed), duration = SnackbarDuration.Long,
                )
                is DenylistEvent.Exported -> FileUtils.shareFile(activity, event.file)
                DenylistEvent.ExportFailed -> snackbarHostState.showSnackbar(
                    activity.getString(R.string.denylist_export_failed),
                )
            }
        }
    }

    DenylistScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateUp = onNavigateUp,
        onQueryChange = viewModel::onQueryChange,
        onBlockDenylistedChange = viewModel::setBlockDenylisted,
        onItemClick = { activity.startActivity(EditDenylistItemActivity.getIntent(activity, it.id)) },
        onToggleSelection = viewModel::toggleSelection,
        onSelectAll = viewModel::selectAllVisible,
        onClearSelection = viewModel::clearSelection,
        onDeleteSelected = viewModel::deleteSelected,
        onAdd = { activity.startActivity(EditDenylistItemActivity.getIntent(activity, null, null)) },
        onImport = { importLauncher.launch("*/*") },
        onExport = { viewModel.export(File(activity.cacheDir, "Tranquille_backup.csv")) },
        modifier = modifier,
    )
}
