package fr.vinetos.tranquille

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import fr.vinetos.tranquille.data.YacbHolder
import fr.vinetos.tranquille.presentation.block.BlockCandidate
import fr.vinetos.tranquille.presentation.block.BlockNumberDialog
import fr.vinetos.tranquille.presentation.block.BlockResult
import fr.vinetos.tranquille.presentation.block.NumberBlocker
import fr.vinetos.tranquille.presentation.denylist.AppDenylistRepository
import fr.vinetos.tranquille.ui.theme.TranquilleTheme
import fr.vinetos.tranquille.utils.PhoneUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Transparent activity with the block confirmation, opened by the "Block" action
 * of the incoming call notification: it rejects the ringing call first, then asks
 * for an optional description and adds the number to the denylist.
 */
class BlockNumberActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val number = intent.getStringExtra(PARAM_NUMBER)
        if (number.isNullOrEmpty()) {
            finish()
            return
        }
        if (savedInstanceState == null && intent.getBooleanExtra(PARAM_END_CALL, false)) {
            PhoneUtils.endCall(this, false)
        }
        val info = YacbHolder.getNumberInfo(number, App.getSettings().cachedAutoDetectedCountryCode)
        val candidate = BlockCandidate.from(this, info)
        setContent {
            TranquilleTheme {
                BlockNumberDialog(candidate, onConfirm = ::block, onDismiss = ::finish)
            }
        }
    }

    private fun block(description: String) {
        val number = intent.getStringExtra(PARAM_NUMBER) ?: return
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                NumberBlocker(AppDenylistRepository).block(number, description)
            }
            Toast.makeText(applicationContext, result.message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        private const val PARAM_NUMBER = "number"
        private const val PARAM_END_CALL = "endCall"

        @JvmStatic
        fun getIntent(context: Context, number: String, endCall: Boolean): Intent =
            Intent(context, BlockNumberActivity::class.java)
                .putExtra(PARAM_NUMBER, number)
                .putExtra(PARAM_END_CALL, endCall)
                .setData(IntentHelper.getUriForPhoneNumber(number))
    }
}

val BlockResult.message: Int
    get() = when (this) {
        BlockResult.ADDED -> R.string.block_number_added
        BlockResult.ALREADY_ON_DENYLIST -> R.string.block_number_already
        BlockResult.INVALID_NUMBER -> R.string.block_number_invalid
    }
