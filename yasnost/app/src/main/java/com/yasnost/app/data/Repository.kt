package com.yasnost.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yasnost.app.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.store by preferencesDataStore(name = "yasnost")
private val KEY = stringPreferencesKey("state_json")

val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

class Repository private constructor(private val ctx: Context) {

    val state: Flow<AppState> = ctx.store.data.map { prefs ->
        prefs[KEY]?.let { runCatching { json.decodeFromString<AppState>(it) }.getOrNull() } ?: AppState()
    }

    suspend fun current(): AppState = state.first()

    suspend fun update(refreshWidgets: Boolean = true, transform: (AppState) -> AppState) {
        ctx.store.edit { prefs ->
            val old = prefs[KEY]?.let { runCatching { json.decodeFromString<AppState>(it) }.getOrNull() } ?: AppState()
            prefs[KEY] = json.encodeToString(AppState.serializer(), transform(old))
        }
        if (refreshWidgets) WidgetUpdater.updateAll(ctx)
    }

    suspend fun upsertHabit(h: Habit) = update { s ->
        val exists = s.habits.any { it.id == h.id }
        s.copy(habits = if (exists) s.habits.map { if (it.id == h.id) h else it } else s.habits + h)
    }

    suspend fun deleteHabit(id: String) = update { s ->
        s.copy(
            habits = s.habits.filterNot { it.id == id },
            relapses = s.relapses.filterNot { it.habitId == id },
            notified = s.notified - id,
        )
    }

    suspend fun saveIntimacy(e: IntimacyEntry) = update(refreshWidgets = false) { s ->
        s.copy(intimacy = s.intimacy.filterNot { it.day == e.day } + e)
    }

    suspend fun deleteIntimacy(day: Long) = update(refreshWidgets = false) { s ->
        s.copy(intimacy = s.intimacy.filterNot { it.day == day })
    }

    suspend fun relapse(id: String, note: String, triggers: List<String> = emptyList()) = update { s ->
        val now = System.currentTimeMillis()
        val h = s.habit(id) ?: return@update s
        s.copy(
            habits = s.habits.map { if (it.id == id) it.copy(startMillis = now) else it },
            relapses = s.relapses + Relapse(id, now, now - h.startMillis, note, triggers),
            notified = s.notified - id,
        )
    }

    suspend fun saveCheckIn(c: CheckIn) = update(refreshWidgets = false) { s ->
        s.copy(checkIns = s.checkIns.filterNot { it.day == c.day } + c)
    }

    companion object {
        @Volatile private var instance: Repository? = null
        fun get(ctx: Context): Repository =
            instance ?: synchronized(this) { instance ?: Repository(ctx.applicationContext).also { instance = it } }

        fun newId(): String = UUID.randomUUID().toString().take(8)
    }
}
