package com.hugopello.smartclock.traffic

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hugopello.smartclock.SmartClockApp
import java.time.Duration

/**
 * Runs [TrafficCheck] under WorkManager, which adds what a bare receiver can't:
 * waits for network, retries with backoff, and survives the process being killed.
 * It's *started* by an exact alarm because WorkManager alone doesn't run at an exact time.
 */
class TrafficCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val check = (applicationContext as SmartClockApp).trafficCheck
        return when (check.run(isLastAttempt = runAttemptCount >= MAX_ATTEMPTS - 1)) {
            CheckOutcome.RETRY -> Result.retry()
            CheckOutcome.DONE, CheckOutcome.SKIPPED -> Result.success()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "traffic-check"
        private const val MAX_ATTEMPTS = 3

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<TrafficCheckWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(30))
                .build()
            // KEEP: if a check is already waiting (e.g. for network), don't start a second one.
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
