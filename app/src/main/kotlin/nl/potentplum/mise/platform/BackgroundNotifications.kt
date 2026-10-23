package nl.potentplum.mise.platform

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import nl.potentplum.mise.R
import nl.potentplum.mise.ShareIntents
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-priority notices on the `background` channel (spec §14). Mise never asks for the notification permission for
 * these; without it they are skipped and the recipe's own state tells the story.
 */
@Singleton
class BackgroundNotifications
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        fun progress(text: String): Notification =
            NotificationCompat
                .Builder(context, NotificationChannels.BACKGROUND)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(text)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()

        fun enrichmentFailed(
            recipeId: String,
            title: String,
        ) {
            val open =
                PendingIntent.getActivity(
                    context,
                    recipeId.hashCode(),
                    ShareIntents.openRecipe(context, recipeId),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            val text = context.getString(R.string.enrich_failed_text, title)
            post(
                ENRICH_FAILED_TAG,
                recipeId.hashCode(),
                NotificationCompat
                    .Builder(context, NotificationChannels.BACKGROUND)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.enrich_failed_title))
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build(),
            )
        }

        private fun post(
            tag: String,
            id: Int,
            notification: Notification,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val manager = NotificationManagerCompat.from(context)
            if (manager.areNotificationsEnabled()) manager.notify(tag, id, notification)
        }

        companion object {
            const val ENRICHING_ID = 1001
            const val ENRICH_FAILED_TAG = "enrich-failed"
        }
    }
