@file:UseSerializers(InstantSerializer::class)

package app.ryadom.core.data.store

import android.content.Context
import android.util.Log
import app.ryadom.core.data.di.AppScope
import app.ryadom.core.model.FamilyGroup
import app.ryadom.core.model.FamilyMessage
import app.ryadom.core.model.Habit
import app.ryadom.core.model.HabitCompletion
import app.ryadom.core.model.InstantSerializer
import app.ryadom.core.model.IntakeEvent
import app.ryadom.core.model.Medication
import app.ryadom.core.model.RestockEntry
import app.ryadom.core.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Всё, что приложение знает о семье, в одном снимке. */
@Serializable
data class StoreState(
    val user: User? = null,
    /** Профиль текущего пользователя внутри семьи. */
    val meId: String? = null,
    val family: FamilyGroup? = null,
    val medications: List<Medication> = emptyList(),
    val events: Map<String, IntakeEvent> = emptyMap(),
    val messages: List<FamilyMessage> = emptyList(),
    val restocks: List<RestockEntry> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitCompletions: List<HabitCompletion> = emptyList(),
    /** Пример семьи, загруженный с экрана приветствия. */
    val isDemo: Boolean = false,
)

/**
 * Локальное хранилище этапа 2: состояние в памяти + атомарная запись в файл.
 * На этапе 3 его заменит Room с тем же набором репозиториев сверху,
 * поэтому интерфейс репозиториев не изменится.
 */
@OptIn(FlowPreview::class)
@Singleton
class LocalStore @Inject constructor(
    @ApplicationContext context: Context,
    @AppScope private val scope: CoroutineScope,
) {
    private val file = File(context.filesDir, "ryadom-store.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val mutex = Mutex()
    private val _state = MutableStateFlow(StoreState())
    private val _ready = MutableStateFlow(false)

    val state: StateFlow<StoreState> = _state.asStateFlow()

    /** true, когда данные прочитаны с диска (держим заставку до этого момента). */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        scope.launch {
            val loaded = withContext(Dispatchers.IO) { read() }
            if (loaded != null) _state.value = loaded
            _ready.value = true
            _state.drop(1).debounce(250).onEach { persist(it) }.launchIn(scope)
        }
    }

    suspend fun update(transform: (StoreState) -> StoreState) {
        // Запись до чтения файла была бы затёрта загрузкой — ждём, пока данные прочитаны.
        _ready.first { it }
        mutex.withLock { _state.value = transform(_state.value) }
    }

    suspend fun reset() = update { StoreState() }

    private fun read(): StoreState? = try {
        if (file.exists()) json.decodeFromString(StoreState.serializer(), file.readText()) else null
    } catch (e: Exception) {
        // повреждённый файл не должен ронять приложение: сохраняем копию и стартуем с пустого состояния
        Log.e(TAG, "store read failed", e)
        file.copyTo(File(file.parentFile, "ryadom-store.broken.json"), overwrite = true)
        null
    }

    private suspend fun persist(s: StoreState) = withContext(Dispatchers.IO) {
        try {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(json.encodeToString(StoreState.serializer(), s))
            if (!tmp.renameTo(file)) {
                file.delete(); tmp.renameTo(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "store write failed", e)
        }
    }

    private companion object {
        const val TAG = "LocalStore"
    }
}
