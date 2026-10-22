package nl.potentplum.mise

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MiseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Release builds plant no tree, so Timber calls are no-ops there (spec §9).
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
