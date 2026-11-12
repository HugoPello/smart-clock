package com.hugopello.smartclock.data

import com.hugopello.smartclock.domain.AlarmSettings
import com.hugopello.smartclock.domain.DecisionReason
import com.hugopello.smartclock.domain.TrafficAdjustment
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class AlarmEntityMapperTest {

    @Test
    fun `domain to entity and back is lossless`() {
        val settings = AlarmSettings(
            arrival = LocalTime.of(23, 45),
            preparation = Duration.ofMinutes(50),
            usualTravel = Duration.ofMinutes(35),
            destination = "Office",
            enabled = true,
            origin = "Home",
        )

        assertEquals(settings, settings.toEntity().toDomain())
    }

    @Test
    fun `arrival is stored as minute of day`() {
        val entity = AlarmSettings.DEFAULT.copy(arrival = LocalTime.of(9, 15)).toEntity()

        assertEquals(9 * 60 + 15, entity.arrivalMinuteOfDay)
    }

    @Test
    fun `traffic adjustment round trip keeps seconds and missing travel`() {
        val adjustment = TrafficAdjustment(
            occurrenceDate = LocalDate.of(2026, 11, 13),
            shift = Duration.ofSeconds(-1_234),
            todayTravel = null,
            reason = DecisionReason.NO_TRAFFIC_DATA,
            checkedAt = Instant.parse("2026-11-13T05:15:00Z"),
        )

        assertEquals(adjustment, adjustment.toEntity().toDomain())
    }
}
