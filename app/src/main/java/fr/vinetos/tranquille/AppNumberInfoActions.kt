package fr.vinetos.tranquille

import android.content.Context
import android.content.Intent
import android.net.Uri
import fr.vinetos.tranquille.data.NumberInfo
import fr.vinetos.tranquille.data.YacbHolder
import fr.vinetos.tranquille.presentation.denylist.EditDenylistItemActivity
import fr.vinetos.tranquille.presentation.info.NumberInfoActions

/** The same three actions the old InfoDialogHelper offered. */
class AppNumberInfoActions(private val context: Context) : NumberInfoActions {

    override fun addToDenylist(info: NumberInfo) {
        context.startActivity(
            EditDenylistItemActivity.getIntent(context, info.featuredDatabaseItem?.name, info.number)
        )
    }

    override fun openOnlineReviews(info: NumberInfo) {
        ReviewsActivity.startForNumber(context, info.number)
    }

    override fun openWebReview(info: NumberInfo) {
        val uri = Uri.parse(YacbHolder.getWebService().webReviewsUrlPart + info.number)
        IntentHelper.startActivity(context, Intent(Intent.ACTION_VIEW, uri))
    }
}
