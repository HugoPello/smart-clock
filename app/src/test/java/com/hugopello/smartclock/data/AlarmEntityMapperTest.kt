package com.hugopello.smartclock.data

import com.hugopello.smartclock.domain.AlarmSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalTime

class AlarmEntityMapperTest {

    @Test
    fun `domain to entity and back is lossless`() {
        val settings = AlarmSettings(
            arrival = LocalTime.of(23, 45),
            preparation = Duration.ofMinutes(50),
            usualTravel = Duration.ofMinutes(35),
            destination = "Office",
        )

        assertEquals(settings, settings.toEntity().toDomain())
    }

    @Test
    fun `arrival is stored as minute of day`() {
        val entity = AlarmSettings.DEFAULT.copy(arrival = LocalTime.of(9, 15)).toEntity()

        assertEquals(9 * 60 + 15, entity.arrivalMinuteOfDay)
    }
}
