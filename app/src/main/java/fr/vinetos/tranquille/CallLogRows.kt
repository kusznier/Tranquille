package fr.vinetos.tranquille

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import fr.vinetos.tranquille.data.CallLogItem
import fr.vinetos.tranquille.data.CallLogItemGroup
import fr.vinetos.tranquille.data.NumberInfo

/** One row of the call log as the Compose main screen shows it. */
data class CallLogRow(
    val key: String,
    val label: String,
    @DrawableRes val ratingIcon: Int?,
    @ColorRes val ratingColor: Int?,
    val types: List<CallLogItem.Type>,
    val count: Int,
    val description: String?,
    val duration: String?,
    val timestamp: Long,
    val numberInfo: NumberInfo,
)

/** Same labels, icons and texts as the old RecyclerView adapter, as plain data. */
object CallLogRows {

    fun from(context: Context, group: CallLogItemGroup): CallLogRow {
        val items = group.items
        val item = items[0]
        val info = item.numberInfo
        val iconAndColor = IconAndColor.forNumberRating(info.rating, info.contactItem != null)
        val showDuration = !(item.duration == 0L && item.type == CallLogItem.Type.MISSED ||
            item.type == CallLogItem.Type.REJECTED)
        return CallLogRow(
            key = items.joinToString(",") { it.id.toString() },
            label = label(context, item),
            ratingIcon = if (iconAndColor.noInfo) null else iconAndColor.iconResId,
            ratingColor = if (iconAndColor.noInfo) null else iconAndColor.colorResId,
            types = items.take(3).map { it.type },
            count = items.size,
            description = NumberInfoUtils.getShortDescription(context, info),
            duration = if (showDuration) duration(context, item.duration) else null,
            timestamp = item.timestamp,
            numberInfo = info,
        )
    }

    private fun label(context: Context, item: CallLogItem): String {
        val info = item.numberInfo
        return when {
            info.noNumber -> context.getString(R.string.no_number)
            info.name != null -> info.name
            !info.denylistItem?.name.isNullOrEmpty() -> info.denylistItem!!.name!!
            else -> item.number
        }
    }

    private fun duration(context: Context, duration: Long): String {
        val seconds = duration % 60
        var minutes = duration / 60
        val hours = minutes / 60
        minutes -= hours * 60
        return when {
            hours != 0L -> context.getString(R.string.duration_h_m_s, hours, minutes, seconds)
            minutes != 0L -> context.getString(R.string.duration_m_s, minutes, seconds)
            else -> context.getString(R.string.duration_s, seconds)
        }
    }
}
