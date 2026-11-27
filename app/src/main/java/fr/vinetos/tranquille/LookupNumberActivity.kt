package fr.vinetos.tranquille

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.vinetos.tranquille.data.YacbHolder
import fr.vinetos.tranquille.presentation.lookup.LookupScreen
import fr.vinetos.tranquille.presentation.lookup.LookupViewModel
import fr.vinetos.tranquille.presentation.lookup.NumberLookup
import fr.vinetos.tranquille.ui.components.setTranquilleContent

class LookupNumberActivity : AppCompatActivity() {

    private val viewModel: LookupViewModel by viewModels {
        viewModelFactory {
            initializer {
                LookupViewModel(
                    lookup = NumberLookup { number ->
                        YacbHolder.getCommunityDatabase().getDbItemByNumber(number) to
                            YacbHolder.getFeaturedDatabase().getDbItemByNumber(number)
                    },
                    countryCode = { App.getSettings().cachedAutoDetectedCountryCode },
                    // Like before, a number on the clipboard is looked up right away.
                    initialInput = numberFromClipboard().takeIf { it.any(Char::isDigit) }.orEmpty(),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTranquilleContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            LookupScreen(
                state = state,
                onNavigateUp = ::finish,
                onInputChange = viewModel::onInputChange,
                onSearch = viewModel::search,
                onClear = viewModel::clear,
                onPaste = {
                    val number = numberFromClipboard()
                    if (number.isNotEmpty()) {
                        viewModel.onInputChange(number)
                        viewModel.search()
                    }
                },
                onLoadReviews = {
                    if (viewModel.requireNumber()) ReviewsActivity.startForNumber(this, viewModel.pureNumber())
                },
                onAddWebReview = {
                    if (viewModel.requireNumber()) {
                        val uri = Uri.parse(YacbHolder.getWebService().webReviewsUrlPart + viewModel.pureNumber())
                        IntentHelper.startActivity(this, Intent(Intent.ACTION_VIEW, uri))
                    }
                },
            )
        }
    }

    private fun numberFromClipboard(): String {
        val clipboard = getSystemService(ClipboardManager::class.java) ?: return ""
        val description = clipboard.primaryClipDescription ?: return ""
        if (MIME_TYPES.none(description::hasMimeType)) return ""
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
    }

    private companion object {
        val MIME_TYPES = arrayOf(
            ClipDescription.MIMETYPE_TEXT_PLAIN,
            "vnd.android.cursor.item/phone_v2", // ContactsContract.Phone.CONTENT_ITEM_TYPE
        )
    }
}
