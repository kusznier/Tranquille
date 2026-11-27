package fr.vinetos.tranquille.presentation.denylist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.vinetos.tranquille.App
import fr.vinetos.tranquille.PermissionHelper
import fr.vinetos.tranquille.ui.components.setTranquilleContent

class EditDenylistItemActivity : AppCompatActivity() {

    private val viewModel: EditDenylistItemViewModel by viewModels {
        viewModelFactory {
            initializer {
                val itemId = intent.getLongExtra(PARAM_ITEM_ID, -1L).takeIf { it != -1L }
                EditDenylistItemViewModel(
                    repository = AppDenylistRepository,
                    itemId = itemId,
                    initialName = intent.getStringExtra(PARAM_NAME),
                    initialPattern = intent.getStringExtra(PARAM_NUMBER_PATTERN),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contactsNotice = when {
            !App.getSettings().useContacts -> ContactsNotice.NOT_ENABLED
            !PermissionHelper.hasContactsPermission(this) -> ContactsNotice.NO_PERMISSION
            else -> ContactsNotice.NEVER_BLOCKED
        }

        setTranquilleContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(state.finished, state.missing) {
                if (state.finished || state.missing) finish()
            }

            EditDenylistItemScreen(
                state = state,
                contactsNotice = contactsNotice,
                onNavigateUp = ::finish,
                onNameChange = viewModel::onNameChange,
                onPatternChange = viewModel::onPatternChange,
                onSave = viewModel::save,
                onDelete = viewModel::delete,
            )
        }
    }

    companion object {
        private const val PARAM_ITEM_ID = "itemId"
        private const val PARAM_NAME = "itemName"
        private const val PARAM_NUMBER_PATTERN = "numberPattern"

        fun getIntent(context: Context?, itemId: Long): Intent {
            val intent = Intent(context, EditDenylistItemActivity::class.java)
            intent.putExtra(PARAM_ITEM_ID, itemId)
            return intent
        }

        @JvmStatic
        fun getIntent(context: Context?, name: String?, numberPattern: String?): Intent {
            val intent = Intent(context, EditDenylistItemActivity::class.java)
            intent.putExtra(PARAM_NAME, name)
            intent.putExtra(PARAM_NUMBER_PATTERN, numberPattern)
            return intent
        }
    }
}
