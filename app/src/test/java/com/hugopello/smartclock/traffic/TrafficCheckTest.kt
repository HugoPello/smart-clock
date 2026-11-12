package com.hugopello.smartclock.traffic

import com.hugopello.smartclock.FakeAlarmRepository
import com.hugopello.smartclock.FakeAlarmScheduler
import com.hugopello.smartclock.FakeTrafficSource
import com.hugopello.smartclock.alarm.AlarmSync
import com.hugopello.smartclock.domain.AlarmSettings
import com.hugopello.smartclock.domain.DecisionReason
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TrafficCheckTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private fun at(text: String) = ZonedDateTime.of(LocalDateTime.parse(text), zone)

    // 8:00 − 30 − 30 → original alarm 07:00, check at 06:15, normal departure 07:30
    private val settings = AlarmSettings(
        arrival = LocalTime.of(8, 0),
        preparation = Duration.ofMinutes(30),
        usualTravel = Duration.ofMinutes(30),
        destination = "Office",
        enabled = true,
        origin = "Home",
    )

    private val repository = FakeAlarmRepository(settings)
    private val scheduler = FakeAlarmScheduler()

    private fun check(now: String, traffic: TrafficSource): TrafficCheck {
        val clock = { at(now) }
        return TrafficCheck(repository, traffic, AlarmSync(repository, scheduler, now = clock), now = clock)
    }

    private fun success(minutes: Long) = FakeTrafficSource(TrafficResult.Success(Duration.ofMinutes(minutes)))

    @Test
    fun `heavy traffic moves the alarm earlier and reschedules it`() = runTest {
        val traffic = success(45)

        val outcome = check("2026-11-12T06:15", traffic).run(isLastAttempt = false)

        assertEquals(CheckOutcome.DONE, outcome)
        assertEquals(Duration.ofMinutes(-15), repository.adjustment?.shift)
        assertEquals(DecisionReason.ADJUSTED, repository.adjustment?.reason)
        assertEquals(LocalDate.parse("2026-11-12"), repository.adjustment?.occurrenceDate)
        assertEquals(at("2026-11-12T06:45"), scheduler.ringAt)
        assertNull(scheduler.trafficCheckAt)
    }

    @Test
    fun `asks for traffic at the usual departure time`() = runTest {
        val traffic = success(30)

        check("2026-11-12T06:15", traffic).run(isLastAttempt = false)

        assertEquals(at("2026-11-12T07:30").toInstant(), traffic.lastDeparture)
    }

    @Test
    fun `huge delay is capped at 30 min earlier`() = runTest {
        check("2026-11-12T06:15", success(120)).run(isLastAttempt = false)

        assertEquals(at("2026-11-12T06:30"), scheduler.ringAt)
        assertEquals(DecisionReason.CAPPED_EARLIER, repository.adjustment?.reason)
    }

    @Test
    fun `temporary failure asks for a retry and keeps the original alarm`() = runTest {
        val outcome = check("2026-11-12T06:15", FakeTrafficSource(TrafficResult.Unavailable(retry = true)))
            .run(isLastAttempt = false)

        assertEquals(CheckOutcome.RETRY, outcome)
        assertNull(repository.adjustment)
    }

    @Test
    fun `failure on the last attempt keeps the original time and stops checking`() = runTest {
        val outcome = check("2026-11-12T06:15", FakeTrafficSource(TrafficResult.Unavailable(retry = true)))
            .run(isLastAttempt = true)

        assertEquals(CheckOutcome.DONE, outcome)
        assertEquals(Duration.ZERO, repository.adjustment?.shift)
        assertEquals(DecisionReason.NO_TRAFFIC_DATA, repository.adjustment?.reason)
        assertEquals(at("2026-11-12T07:00"), scheduler.ringAt)
        assertNull(scheduler.trafficCheckAt)
    }

    @Test
    fun `late check never schedules a ring in the past`() = runTest {
        // Wanted 06:30 (−30), but the check only ran at 06:50: ring in one minute instead.
        check("2026-11-12T06:50", success(90)).run(isLastAttempt = false)

        assertEquals(at("2026-11-12T06:51"), scheduler.ringAt)
    }

    @Test
    fun `too early for the next check - skipped without calling the API`() = runTest {
        val traffic = success(30)

        val outcome = check("2026-11-12T05:00", traffic).run(isLastAttempt = false)

        assertEquals(CheckOutcome.SKIPPED, outcome)
        assertEquals(0, traffic.calls)
    }

    @Test
    fun `already checked today - skipped`() = runTest {
        check("2026-11-12T06:15", success(40)).run(isLastAttempt = false)
        val second = success(60)

        val outcome = check("2026-11-12T06:20", second).run(isLastAttempt = false)

        assertEquals(CheckOutcome.SKIPPED, outcome)
        assertEquals(0, second.calls)
    }

    @Test
    fun `alarm switched off - skipped`() = runTest {
        repository.stored = settings.copy(enabled = false)

        val outcome = check("2026-11-12T06:15", success(40)).run(isLastAttempt = false)

        assertEquals(CheckOutcome.SKIPPED, outcome)
    }
}
