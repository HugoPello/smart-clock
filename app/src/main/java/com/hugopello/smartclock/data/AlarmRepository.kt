package com.hugopello.smartclock.data

import com.hugopello.smartclock.domain.AlarmSettings

/**
 * What the rest of the app sees. The ViewModel depends on this interface, not on Room,
 * so tests can pass a fake in-memory implementation.
 */
interface AlarmRepository {
    suspend fun get(): AlarmSettings?
    suspend fun save(settings: AlarmSettings)
}

class RoomAlarmRepository(private val dao: AlarmDao) : AlarmRepository {
    override suspend fun get(): AlarmSettings? = dao.get()?.toDomain()
    override suspend fun save(settings: AlarmSettings) = dao.upsert(settings.toEntity())
}
