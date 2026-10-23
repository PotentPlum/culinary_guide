package nl.potentplum.mise.testing

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import nl.potentplum.mise.platform.ClockTimers
import nl.potentplum.mise.platform.ClockTimersModule

/** Records the timers cook mode starts instead of opening a Clock app. */
object FakeClockTimers : ClockTimers {
    override var supported: Boolean = true
    val started = mutableListOf<Pair<Int, String>>()
    var shown = 0

    override fun start(
        seconds: Int,
        message: String,
    ): Boolean {
        if (!supported) return false
        started += seconds to message
        return true
    }

    override fun showTimers(): Boolean {
        shown++
        return supported
    }

    fun reset() {
        supported = true
        started.clear()
        shown = 0
    }
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ClockTimersModule::class])
object FakeClockTimersModule {
    @Provides
    fun clockTimers(): ClockTimers = FakeClockTimers
}
