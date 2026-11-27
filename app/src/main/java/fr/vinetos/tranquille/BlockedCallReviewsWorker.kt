package fr.vinetos.tranquille

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import dummydomain.yetanothercallblocker.sia.model.CommunityReview
import fr.vinetos.tranquille.data.SiaNumberCategoryUtils
import fr.vinetos.tranquille.data.YacbHolder

/**
 * Opt-in (Settings: "Online reviews for blocked calls", off by default): after a call was
 * blocked, loads the number's online reviews with the existing reviews loader and adds the most
 * common reason to the blocked-call notification. This sends the blocked number to the reviews
 * service; numbers of contacts are never sent.
 */
class BlockedCallReviewsWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val number = inputData.getString(KEY_NUMBER) ?: return Result.failure()
        val settings = App.getSettings()
        if (!settings.reviewsForBlockedCalls) return Result.success()

        val reviews = YacbHolder.getCommunityReviewsLoader()
            .loadReviews(number, settings.countryCodeForReviews)
            ?: return Result.success()

        val summary = summarize(applicationContext, reviews) ?: return Result.success()
        val numberInfo = YacbHolder.getNumberInfo(number, settings.cachedAutoDetectedCountryCode)
        NotificationHelper.showBlockedCallNotification(applicationContext, numberInfo, summary)
        return Result.success()
    }

    companion object {
        private const val KEY_NUMBER = "number"

        @JvmStatic
        fun enqueue(context: Context, number: String) {
            val request = OneTimeWorkRequest.Builder(BlockedCallReviewsWorker::class.java)
                .setInputData(Data.Builder().putString(KEY_NUMBER, number).build())
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("blocked-call-reviews-$number", ExistingWorkPolicy.KEEP, request)
        }

        /**
         * The most common review title (often the company name), or else the most common
         * category, among negative reviews if there are any, otherwise among all reviews.
         */
        fun summarize(context: Context, reviews: List<CommunityReview>): String? {
            if (reviews.isEmpty()) return null
            val negative = reviews.filter { it.rating == CommunityReview.Rating.NEGATIVE }
            val pool = negative.ifEmpty { reviews }
            pool.mapNotNull { it.title?.trim()?.takeIf(String::isNotEmpty) }
                .mostCommon()?.let { return it }
            return pool.mapNotNull { it.category }
                .mostCommon()
                ?.let { runCatching { SiaNumberCategoryUtils.getName(context, it) }.getOrNull() }
        }

        private fun <T> List<T>.mostCommon(): T? = groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    }
}
