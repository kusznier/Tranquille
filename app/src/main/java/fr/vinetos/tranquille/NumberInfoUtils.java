package fr.vinetos.tranquille;

import android.content.Context;

import fr.vinetos.tranquille.data.NumberInfo;
import fr.vinetos.tranquille.data.SiaNumberCategoryUtils;
import dummydomain.yetanothercallblocker.sia.model.NumberCategory;
import dummydomain.yetanothercallblocker.sia.model.database.CommunityDatabaseItem;

public class NumberInfoUtils {

    public static String getShortDescription(Context context, NumberInfo numberInfo) {
        if (numberInfo.isFailedVerification) {
            return context.getString(R.string.info_failed_verification);
        }

        if (numberInfo.communityDatabaseItem != null) {
            NumberCategory category = NumberCategory.getById(
                    numberInfo.communityDatabaseItem.getCategory());

            if (category != null && category != NumberCategory.NONE) {
                return SiaNumberCategoryUtils.getName(context, category);
            }
        }

        if (numberInfo.denylistItem != null && numberInfo.contactItem == null) {
            return context.getString(R.string.info_in_denylist);
        }

        return null;
    }

    /**
     * Whether the item has any ratings. Used for display only: the library's
     * CommunityDatabaseItem.hasRatings() counts negative ratings twice and ignores
     * neutral ones, so a number with only neutral ratings would show no summary.
     */
    public static boolean hasAnyRatings(CommunityDatabaseItem item) {
        return item != null && item.getPositiveRatingsCount() + item.getNegativeRatingsCount()
                + item.getNeutralRatingsCount() > 0;
    }

    /**
     * A short ratings summary for a notification title, e.g. "👎 5 · 👍 1",
     * or null when there are no ratings. Uses only the offline database.
     */
    public static String getShortRatings(Context context, NumberInfo numberInfo) {
        CommunityDatabaseItem item = numberInfo.communityDatabaseItem;
        if (!hasAnyRatings(item)) return null;
        return context.getString(R.string.notification_ratings_short,
                item.getNegativeRatingsCount(), item.getPositiveRatingsCount());
    }

}
