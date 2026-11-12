package com.hugopello.smartclock.data

import com.hugopello.smartclock.domain.AlarmSettings
import com.hugopello.smartclock.domain.TrafficAdjustment

/**
 * What the rest of the app sees. The ViewModel, AlarmSync and TrafficCheck depend on this
 * interface, not on Room, so tests can pass a fake in-memory implementation.
 */
interface AlarmRepository {
    suspend fun get(): AlarmSettings?

    /** Also discards the last traffic adjustment: it was computed for the old settings. */
    suspend fun save(settings: AlarmSettings)

    suspend fun getAdjustment(): TrafficAdjustment?
    suspend fun saveAdjustment(adjustment: TrafficAdjustment)
}

class RoomAlarmRepository(private val dao: AlarmDao) : AlarmRepository {
    override suspend fun get(): AlarmSettings? = dao.get()?.toDomain()
    override suspend fun save(settings: AlarmSettings) = dao.saveSettings(settings.toEntity())
    override suspend fun getAdjustment(): TrafficAdjustment? = dao.getAdjustment()?.toDomain()
    override suspend fun saveAdjustment(adjustment: TrafficAdjustment) = dao.upsertAdjustment(adjustment.toEntity())
}
