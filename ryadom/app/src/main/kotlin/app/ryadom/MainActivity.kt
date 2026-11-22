package app.ryadom

import android.app.Application
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.activity.viewModels
import app.ryadom.core.data.repository.Session
import app.ryadom.core.data.repository.SessionRepository
import app.ryadom.core.data.repository.SettingsRepository
import app.ryadom.core.designsystem.theme.RyadomTheme
import app.ryadom.core.model.ThemeMode
import app.ryadom.core.model.UserSettings
import app.ryadom.navigation.RyadomRoot
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltAndroidApp
class RyadomApp : Application()

@HiltViewModel
class MainViewModel @Inject constructor(session: SessionRepository, settings: SettingsRepository) : ViewModel() {
    val session: StateFlow<Session> = session.session.stateIn(viewModelScope, SharingStarted.Eagerly, Session.Loading)
    val settings: StateFlow<UserSettings?> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Заставка держится, пока данные читаются с диска: без мигания пустого экрана.
        installSplashScreen().setKeepOnScreenCondition { vm.session.value is Session.Loading || vm.settings.value == null }
        super.onCreate(savedInstanceState)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val session by vm.session.collectAsStateWithLifecycle()
            val dark = when (settings?.theme ?: ThemeMode.SYSTEM) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                )
                onDispose { }
            }
            RyadomTheme(darkTheme = dark) { RyadomRoot(session) }
        }
    }
}
