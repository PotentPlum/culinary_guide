package nl.potentplum.mise.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/** Timers live in the phone's Clock app (spec §6.10); in-app timers are P3. */
interface ClockTimers {
    /** Whether an app handles `ACTION_SET_TIMER`; without one, cook mode hides its timer chips. */
    val supported: Boolean

    /** Starts a timer without leaving Mise; false when no app took it. */
    fun start(
        seconds: Int,
        message: String,
    ): Boolean

    /** Opens the Clock app's list of running timers. */
    fun showTimers(): Boolean
}

@Singleton
class ClockTimerLauncher
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : ClockTimers {
        override val supported: Boolean
            get() = Intent(AlarmClock.ACTION_SET_TIMER).resolveActivity(context.packageManager) != null

        override fun start(
            seconds: Int,
            message: String,
        ): Boolean =
            launch(
                Intent(AlarmClock.ACTION_SET_TIMER)
                    .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, message)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, true),
            )

        override fun showTimers(): Boolean = launch(Intent(AlarmClock.ACTION_SHOW_TIMERS))

        private fun launch(intent: Intent): Boolean =
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (_: ActivityNotFoundException) {
                false
            } catch (_: SecurityException) {
                false
            }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class ClockTimersModule {
    @Binds
    abstract fun clockTimers(launcher: ClockTimerLauncher): ClockTimers
}
