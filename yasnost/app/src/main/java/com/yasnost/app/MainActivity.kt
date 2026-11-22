package com.yasnost.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yasnost.app.data.AppState
import com.yasnost.app.data.Repository
import com.yasnost.app.ui.components.AuroraBackground
import com.yasnost.app.ui.screens.AwardsScreen
import com.yasnost.app.ui.screens.CalendarScreen
import com.yasnost.app.ui.screens.EditorScreen
import com.yasnost.app.ui.screens.HabitScreen
import com.yasnost.app.ui.screens.HomeScreen
import com.yasnost.app.ui.screens.SettingsScreen
import com.yasnost.app.ui.screens.SosScreen
import com.yasnost.app.ui.theme.Y
import com.yasnost.app.ui.theme.YType
import com.yasnost.app.ui.theme.YasnostTheme
import com.yasnost.app.ui.theme.accent
import com.yasnost.app.work.Ticker
import kotlinx.coroutines.CoroutineScope

sealed interface Route {
    data object Home : Route
    data object Calendar : Route
    data object Sos : Route
    data object Settings : Route
    data object Awards : Route
    data object Money : Route
    data class Habit(val id: String) : Route
    data class Editor(val id: String?, val preset: String? = null) : Route
}

class Nav(val stack: MutableList<Route>) {
    val top get() = stack.last()
    fun go(r: Route) { stack.add(r) }
    fun tab(r: Route) { stack.clear(); stack.add(r) }
    fun back(): Boolean = if (stack.size > 1) { stack.removeAt(stack.lastIndex); true } else false
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        Ticker.schedule(this)
        val start = intent?.getStringExtra(EXTRA_HABIT)
        val sos = intent?.getBooleanExtra(EXTRA_SOS, false) == true
        setContent {
            YasnostTheme { App(start, sos) }
        }
    }

    companion object {
        const val EXTRA_HABIT = "habit"
        const val EXTRA_SOS = "sos"
    }
}

@Composable
private fun App(startHabit: String?, startSos: Boolean) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val state by repo.state.collectAsStateWithLifecycle(initialValue = null)
    val stack = remember {
        mutableStateListOf<Route>(Route.Home).apply {
            if (startHabit != null) add(Route.Habit(startHabit))
            if (startSos) add(Route.Sos)
        }
    }
    val nav = remember { Nav(stack) }
    val scope = rememberCoroutineScope()

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    BackHandler(enabled = stack.size > 1) { nav.back() }

    val s = state ?: run { Box(Modifier.fillMaxSize().background(Y.Bg)); return }
    val activity = ctx as? android.app.Activity
    LaunchedEffect(s.settings.secureScreen) {
        if (s.settings.secureScreen) activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
    }
    val top = nav.top
    val accentType = (top as? Route.Habit)?.let { s.habit(it.id)?.type }
    val acc = accentType?.accent()

    if (!s.profile.setupDone || !s.settings.modulesChosen) {
        AuroraBackground { com.yasnost.app.ui.screens.WelcomeScreen(s, repo, scope) }
        return
    }

    androidx.compose.runtime.CompositionLocalProvider(com.yasnost.app.ui.components.LocalFemale provides s.profile.female) {
    AuroraBackground(acc?.a ?: Y.Violet, acc?.b ?: Y.Teal) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                AnimatedContent(top, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "nav") { r ->
                    Screen(r, s, nav, repo, scope)
                }
            }
            if (top is Route.Home || top is Route.Calendar || top is Route.Settings || top is Route.Awards) {
                BottomBar(top, nav, s.settings.anyHabits)
            }
        }
    }
    }
}

@Composable
private fun Screen(r: Route, s: AppState, nav: Nav, repo: Repository, scope: CoroutineScope) {
    when (r) {
        Route.Home -> HomeScreen(s, nav, repo, scope)
        Route.Calendar -> CalendarScreen(s, repo, scope)
        Route.Sos -> SosScreen(s, nav, repo, scope)
        Route.Settings -> SettingsScreen(s, nav, repo, scope)
        Route.Awards -> AwardsScreen(s, nav)
        Route.Money -> com.yasnost.app.ui.screens.MoneyScreen(s, nav, repo, scope)
        is Route.Habit -> HabitScreen(r.id, s, nav, repo, scope)
        is Route.Editor -> EditorScreen(r.id, r.preset, s, nav, repo, scope)
    }
}

@Composable
private fun BottomBar(top: Route, nav: Nav, sos: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xE6121420))
                .border(1.dp, Y.Stroke, RoundedCornerShape(32.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Filled.Home, "Главная", top is Route.Home) { nav.tab(Route.Home) }
            NavItem(Icons.Filled.DateRange, "Отметки", top is Route.Calendar) { nav.tab(Route.Calendar) }
            if (sos) Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Y.Coral, Y.Magenta)))
                    .clickable { nav.go(Route.Sos) },
                contentAlignment = Alignment.Center,
            ) {
                Text("SOS", color = Color(0xFF0B0C14), style = YType.Label.copy(fontSize = 13.sp, letterSpacing = 0.5.sp))
            }
            NavItem(painter = R.drawable.ic_medal, label = "Награды", selected = top is Route.Awards) { nav.tab(Route.Awards) }
            NavItem(Icons.Filled.Settings, "Ещё", top is Route.Settings) { nav.tab(Route.Settings) }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector? = null, label: String, selected: Boolean, painter: Int? = null, onClick: () -> Unit) {
    val c = if (selected) Y.Text else Y.Text3
    Column(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) Icon(icon, null, tint = c, modifier = Modifier.size(24.dp))
        else if (painter != null) Icon(painterResource(painter), null, tint = c, modifier = Modifier.size(24.dp))
        Text(label, color = c, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
    }
}
