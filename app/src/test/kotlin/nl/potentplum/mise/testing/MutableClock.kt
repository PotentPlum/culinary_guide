package nl.potentplum.mise.testing

import nl.potentplum.mise.core.util.MiseClock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** A clock tests can move, e.g. across midnight in Amsterdam. */
class MutableClock(
    var instant: Instant,
) : Clock() {
    override fun getZone(): ZoneId = MiseClock.ZONE

    override fun withZone(zone: ZoneId): Clock = if (zone == MiseClock.ZONE) this else Clock.fixed(instant, zone)

    override fun instant(): Instant = instant

    fun advance(duration: Duration) {
        instant += duration
    }

    companion object {
        /** A local time in Amsterdam. */
        fun at(dateTime: LocalDateTime): MutableClock = MutableClock(dateTime.atZone(MiseClock.ZONE).toInstant())
    }
}
