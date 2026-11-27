package fr.vinetos.tranquille

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.vinetos.tranquille.data.YacbHolder
import fr.vinetos.tranquille.presentation.reviews.ReviewsLoader
import fr.vinetos.tranquille.presentation.reviews.ReviewsScreen
import fr.vinetos.tranquille.presentation.reviews.ReviewsViewModel
import fr.vinetos.tranquille.ui.components.setTranquilleContent

/** Online reviews for a number. Opening this screen sends the number to the reviews service. */
class ReviewsActivity : AppCompatActivity() {

    private val viewModel: ReviewsViewModel by viewModels {
        viewModelFactory {
            initializer {
                ReviewsViewModel(ReviewsLoader { number ->
                    YacbHolder.getCommunityReviewsLoader()
                        .loadReviews(number, App.getSettings().countryCodeForReviews)
                })
            }
        }
    }

    private var number by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setTranquilleContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            ReviewsScreen(number = number, state = state, onNavigateUp = ::finish)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        number = intent.getStringExtra(PARAM_NUMBER).orEmpty()
        if (number.isNotEmpty()) viewModel.load(number)
    }

    companion object {
        private const val PARAM_NUMBER = "param_number"

        @JvmStatic
        fun getNumberIntent(context: Context, number: String?): Intent =
            Intent(context, ReviewsActivity::class.java)
                .putExtra(PARAM_NUMBER, number)
                .setData(IntentHelper.getUriForPhoneNumber(number))

        @JvmStatic
        fun startForNumber(context: Context, number: String?) {
            context.startActivity(IntentHelper.clearTop(getNumberIntent(context, number)))
        }
    }
}
