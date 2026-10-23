package nl.potentplum.mise

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import nl.potentplum.mise.platform.NotificationChannels
import nl.potentplum.mise.security.ScrubbingDebugTree
import nl.potentplum.mise.security.ScrubbingExceptionHandler
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MiseApp :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // WorkManager starts on demand with Hilt's factory; the default initializer is removed in the manifest.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(ScrubbingExceptionHandler(Thread.getDefaultUncaughtExceptionHandler()))
        // Release builds plant no tree, so Timber calls are no-ops there (spec §9).
        if (BuildConfig.DEBUG) Timber.plant(ScrubbingDebugTree())
        NotificationChannels.create(this)
    }
}
