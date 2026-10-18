package com.example.bingdailywallpapers.worker

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.bingdailywallpapers.MainActivity
import com.example.bingdailywallpapers.data.repository.BingWallpaperRepository
import com.example.bingdailywallpapers.util.WallpaperHelper
import com.example.bingdailywallpapers.util.WallpaperPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DailyWallpaperWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                if (!WallpaperPreferences.isAutoWallpaperEnabled(applicationContext)) {
                    return@withContext Result.success()
                }

                val repository = BingWallpaperRepository()
                val market = WallpaperPreferences.getBingMarket(applicationContext)
                val result = repository.fetchWallpapers(idx = 0, n = 1, mkt = market)
                val images = result.getOrNull()
                val latestImage = images?.firstOrNull()

                if (latestImage != null && !latestImage.portraitUrl.isNullOrEmpty()) {
                    val wallpaperResult = WallpaperHelper.setWallpaperFromUrl(
                        applicationContext,
                        latestImage.portraitUrl,
                        WallpaperHelper.WallpaperTarget.BOTH
                    )
                    if (wallpaperResult.isSuccess) {
                        WallpaperPreferences.setLastSyncTimestamp(applicationContext, System.currentTimeMillis())
                        sendNotification(applicationContext, latestImage.title ?: "New Bing Wallpaper")
                        Result.success()
                    } else {
                        Result.retry()
                    }
                } else {
                    Result.retry()
                }
            } catch (e: Exception) {
                Result.failure()
            }
        }
    }

    private fun sendNotification(context: Context, title: String) {
        try {
            val channelId = "bing_wallpaper_channel"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Daily Wallpapers",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for automatic daily Bing wallpapers"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_menu_gallery)
                .setContentTitle("New Bing Wallpaper Set!")
                .setContentText(title)
                .setStyle(NotificationCompat.BigTextStyle().bigText("Successfully applied daily wallpaper: $title"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(2026, notification)
        } catch (_: Exception) {}
    }

    companion object {
        private const val WORK_NAME = "DailyWallpaperPeriodicWork"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<DailyWallpaperWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun triggerTest(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val testWorkRequest = OneTimeWorkRequestBuilder<DailyWallpaperWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(testWorkRequest)
        }
    }
}
