package app.ryadom.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.ryadom.core.data.repository.ConnectivityRepository
import app.ryadom.core.data.repository.DefaultConnectivityRepository
import app.ryadom.core.data.repository.FamilyRepository
import app.ryadom.core.data.repository.IntakeRepository
import app.ryadom.core.data.repository.LocalFamilyRepository
import app.ryadom.core.data.repository.LocalIntakeRepository
import app.ryadom.core.data.repository.LocalMedicationRepository
import app.ryadom.core.data.repository.LocalMessageRepository
import app.ryadom.core.data.repository.LocalPlanRepository
import app.ryadom.core.data.repository.LocalSessionRepository
import app.ryadom.core.data.repository.MedicationRepository
import app.ryadom.core.data.repository.MessageRepository
import app.ryadom.core.data.repository.PlanRepository
import app.ryadom.core.data.repository.SessionRepository
import app.ryadom.core.data.repository.SettingsRepository
import app.ryadom.core.data.repository.DataStoreSettingsRepository
import app.ryadom.core.data.util.Clock
import app.ryadom.core.data.util.SystemClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Область корутин приложения: живёт столько же, сколько процесс. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppScope

@Module
@InstallIn(SingletonComponent::class)
internal object DataProvidesModule {
    @Provides @Singleton @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton
    fun preferences(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("ryadom-settings") }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataBindsModule {
    @Binds abstract fun clock(impl: SystemClock): Clock
    @Binds abstract fun session(impl: LocalSessionRepository): SessionRepository
    @Binds abstract fun family(impl: LocalFamilyRepository): FamilyRepository
    @Binds abstract fun medications(impl: LocalMedicationRepository): MedicationRepository
    @Binds abstract fun intakes(impl: LocalIntakeRepository): IntakeRepository
    @Binds abstract fun messages(impl: LocalMessageRepository): MessageRepository
    @Binds abstract fun plan(impl: LocalPlanRepository): PlanRepository
    @Binds abstract fun settings(impl: DataStoreSettingsRepository): SettingsRepository
    @Binds abstract fun connectivity(impl: DefaultConnectivityRepository): ConnectivityRepository
}
