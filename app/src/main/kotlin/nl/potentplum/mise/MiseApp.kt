package nl.potentplum.mise

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import nl.potentplum.mise.security.ScrubbingDebugTree
import nl.potentplum.mise.security.ScrubbingExceptionHandler
import timber.log.Timber

@HiltAndroidApp
class MiseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(ScrubbingExceptionHandler(Thread.getDefaultUncaughtExceptionHandler()))
        // Release builds plant no tree, so Timber calls are no-ops there (spec §9).
        if (BuildConfig.DEBUG) Timber.plant(ScrubbingDebugTree())
    }
}
