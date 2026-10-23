package nl.potentplum.mise.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import nl.potentplum.mise.R

/** The two channels of spec §14. Creating them is idempotent; posting needs the permission, asked in context. */
object NotificationChannels {
    const val SEASON = "season"
    const val BACKGROUND = "background"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    SEASON,
                    context.getString(R.string.notification_channel_season),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
                NotificationChannel(
                    BACKGROUND,
                    context.getString(R.string.notification_channel_background),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            ),
        )
    }
}
