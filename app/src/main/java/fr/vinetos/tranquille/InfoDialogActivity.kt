package fr.vinetos.tranquille

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import fr.vinetos.tranquille.data.YacbHolder
import fr.vinetos.tranquille.presentation.info.NumberInfoDialog
import fr.vinetos.tranquille.ui.theme.TranquilleTheme

/** Transparent activity that shows the number info dialog, opened from call notifications. */
class InfoDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val numberInfo = YacbHolder.getNumberInfo(
            intent.getStringExtra(PARAM_NUMBER),
            App.getSettings().cachedAutoDetectedCountryCode,
        )
        setContent {
            TranquilleTheme {
                NumberInfoDialog(numberInfo, AppNumberInfoActions(this), onDismiss = ::finish)
            }
        }
    }

    companion object {
        const val PARAM_NUMBER = "number"

        @JvmStatic
        fun getIntent(context: Context, number: String?): Intent =
            Intent(context, InfoDialogActivity::class.java)
                .putExtra(PARAM_NUMBER, number)
                .setData(IntentHelper.getUriForPhoneNumber(number))
    }
}
