package nl.potentplum.mise.core.util

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Singleton

/**
 * The app's notion of "now". Days and months are always Dutch (spec §6.1): the current month is
 * `LocalDate.now(ZoneId.of("Europe/Amsterdam")).monthValue`. Tests pass a fixed [Clock].
 */
class MiseClock(
    private val clock: Clock = Clock.system(ZONE),
) {
    fun now(): Instant = clock.instant()

    fun nowMillis(): Long = clock.millis()

    fun today(): LocalDate = LocalDate.now(clock.withZone(ZONE))

    fun zonedNow(): ZonedDateTime = ZonedDateTime.now(clock.withZone(ZONE))

    /** 1 = January … 12 = December. */
    fun currentMonth(): Int = today().monthValue

    companion object {
        val ZONE: ZoneId = ZoneId.of("Europe/Amsterdam")

        fun fixed(instant: Instant): MiseClock = MiseClock(Clock.fixed(instant, ZONE))

        /** Noon in Amsterdam on [date]. */
        fun fixed(date: LocalDate): MiseClock = fixed(date.atTime(12, 0).atZone(ZONE).toInstant())
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    @Provides
    @Singleton
    fun clock(): MiseClock = MiseClock()
}
