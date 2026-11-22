package app.ryadom.core.data.repository

import app.ryadom.core.data.store.LocalStore
import app.ryadom.core.data.store.StoreState
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** Экспорт всех данных семьи этого телефона в JSON (раздел 28 ТЗ: экспорт и резервная копия). */
class DataExport @Inject constructor(private val store: LocalStore) {
    private val json = Json { prettyPrint = true; encodeDefaults = true }
    fun exportJson(): String = json.encodeToString(StoreState.serializer(), store.state.value)
}
