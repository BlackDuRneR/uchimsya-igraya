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

    /** Состояние для экрана и виджетов: привычки отключённых модулей скрыты (но не удалены). */
    val state: Flow<AppState> = ctx.store.data.map { prefs ->
        val raw = prefs[KEY]?.let { runCatching { json.decodeFromString<AppState>(it) }.getOrNull() } ?: AppState()
        raw.copy(habits = raw.habits.filter { raw.settings.tracks(it.type) })
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

    suspend fun saveCycleDay(c: CycleDay) = update(refreshWidgets = false) { s ->
        val empty = c.flow == null && c.pain == 0 && c.symptoms.isEmpty() && c.mood == 0 && c.meds.isEmpty() && c.note.isBlank()
        s.copy(cycle = s.cycle.filterNot { it.day == c.day } + if (empty) emptyList() else listOf(c))
    }

    /** Отметить дни месячных from..to (не затирая уже введённые данные). */
    suspend fun markPeriod(from: Long, to: Long, close: Boolean) = update(refreshWidgets = false) { s ->
        val add = (from..to).filter { d -> s.cycleOn(d)?.flow == null }.map { d ->
            (s.cycleOn(d) ?: CycleDay(d)).copy(flow = if (d - from <= 1) com.yasnost.app.data.Flow.MEDIUM else com.yasnost.app.data.Flow.LIGHT)
        }
        s.copy(
            cycle = s.cycle.filterNot { c -> add.any { it.day == c.day } } + add,
            periodClosedStart = if (close) from else s.periodClosedStart,
        )
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
