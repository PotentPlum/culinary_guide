package nl.potentplum.mise.testing

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import nl.potentplum.mise.core.util.ClockModule
import nl.potentplum.mise.core.util.MiseClock
import java.time.LocalDateTime
import javax.inject.Singleton

/**
 * UI tests run on 22 October 2026 at noon in Amsterdam, the same day they pass to `MiseAppShell`, so seasonal
 * expectations (pumpkin at peak, "Deze maand: oktober") don't depend on when the suite runs.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ClockModule::class])
object FakeClockModule {
    val TODAY: LocalDateTime = LocalDateTime.of(2026, 10, 22, 12, 0)

    @Provides
    @Singleton
    fun clock(): MiseClock = MiseClock(MutableClock.at(TODAY))
}
