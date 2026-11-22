package app.ryadom.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.ryadom.core.data.repository.Session
import app.ryadom.core.designsystem.component.BottomItem
import app.ryadom.core.designsystem.component.RyBottomBar
import app.ryadom.core.designsystem.theme.Ry
import app.ryadom.core.designsystem.theme.RyIcons
import app.ryadom.core.l10n.R
import app.ryadom.feature.family.FamilyNav
import app.ryadom.feature.family.FamilyRoute
import app.ryadom.feature.family.MemberEditRoute
import app.ryadom.feature.family.MemberRoute
import app.ryadom.feature.family.MessagesRoute
import app.ryadom.feature.family.familyGraph
import app.ryadom.feature.history.HistoryDayRoute
import app.ryadom.feature.history.HistoryNav
import app.ryadom.feature.history.HistoryRoute
import app.ryadom.feature.history.StatsRoute
import app.ryadom.feature.history.historyGraph
import app.ryadom.feature.home.HomeNav
import app.ryadom.feature.home.HomeRoute
import app.ryadom.feature.home.HomeRouteScreen
import app.ryadom.feature.medication.CourseCompleteRoute
import app.ryadom.feature.medication.InventoryRoute
import app.ryadom.feature.medication.MedicationDetailRoute
import app.ryadom.feature.medication.MedicationEditRoute
import app.ryadom.feature.medication.MedicationNav
import app.ryadom.feature.medication.medicationGraph
import app.ryadom.feature.onboarding.FamilySetupRoute
import app.ryadom.feature.onboarding.WelcomeRoute
import app.ryadom.feature.onboarding.onboardingGraph
import app.ryadom.feature.schedule.RemindersRoute
import app.ryadom.feature.schedule.ScheduleNav
import app.ryadom.feature.schedule.ScheduleRoute
import app.ryadom.feature.schedule.scheduleGraph
import app.ryadom.feature.settings.AppearanceRoute
import app.ryadom.feature.settings.NotificationSettingsRoute
import app.ryadom.feature.settings.PrivacyRoute
import app.ryadom.feature.settings.SettingsNav
import app.ryadom.feature.settings.SettingsRoute
import app.ryadom.feature.settings.SyncSettingsRoute
import app.ryadom.feature.settings.settingsGraph
import kotlin.reflect.KClass

/** Корень: какой граф показывать, решает сессия. Вход, настройка семьи и основное приложение — разные графы. */
@Composable
fun RyadomRoot(session: Session) {
    Box(Modifier.fillMaxSize().background(Ry.colors.bg)) {
        when (session) {
            Session.Loading -> Unit // держится системная заставка
            Session.SignedOut -> OnboardingHost(WelcomeRoute)
            is Session.NoFamily -> OnboardingHost(FamilySetupRoute)
            is Session.Ready -> MainShell()
        }
    }
}

@Composable
private fun OnboardingHost(start: Any) {
    // key: при смене этапа (вышли → вошли без семьи) граф строится заново со своей стартовой точкой
    androidx.compose.runtime.key(start) {
        val nav = rememberNavController()
        NavHost(nav, startDestination = start, enterTransition = { fadeIn(tween(200)) }, exitTransition = { fadeOut(tween(200)) }) {
            onboardingGraph(nav)
        }
    }
}

private data class Tab(val route: Any, val type: KClass<*>, val icon: Int, val label: Int)

private val tabs = listOf(
    Tab(HomeRoute, HomeRoute::class, RyIcons.Home, R.string.nav_home),
    Tab(ScheduleRoute, ScheduleRoute::class, RyIcons.Calendar, R.string.nav_schedule),
    Tab(FamilyRoute(), FamilyRoute::class, RyIcons.Family, R.string.nav_family),
    Tab(HistoryRoute, HistoryRoute::class, RyIcons.History, R.string.nav_history),
)

private fun NavHostController.switchTab(route: Any) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun MainShell() {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val entry by nav.currentBackStackEntryAsState()
    val dest = entry?.destination
    val selected = tabs.indexOfFirst { t -> dest?.hierarchy?.any { it.hasRoute(t.type) } == true }

    val back: () -> Unit = { nav.popBackStack() }
    val openMedication: (String) -> Unit = { nav.navigate(MedicationDetailRoute(it)) }
    val editMedication: (String?, String?) -> Unit = { id, person -> nav.navigate(MedicationEditRoute(id, person)) }

    Scaffold(
        containerColor = Ry.colors.bg,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(containerColor = Ry.colors.text, contentColor = Ry.colors.bg, shape = Ry.shapes.m) { Text(data.visuals.message, style = Ry.type.bodySmall) }
            }
        },
        bottomBar = {
            if (selected >= 0) RyBottomBar(
                items = tabs.map { BottomItem(it.icon, stringResource(it.label)) },
                selected = selected,
                onSelect = { nav.switchTab(tabs[it].route) },
                addLabel = stringResource(R.string.action_add_medication),
                onAdd = { editMedication(null, null) },
            )
        },
    ) { padding ->
        NavHost(
            nav, startDestination = HomeRoute, modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(200)) + slideInVertically(tween(200)) { 24 } },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(150)) },
        ) {
            composable<HomeRoute> {
                HomeRouteScreen(
                    HomeNav(
                        openMedication = openMedication,
                        addMedication = { editMedication(null, null) },
                        openMessages = { nav.navigate(MessagesRoute) },
                        openPerson = { nav.navigate(MemberRoute(it)) },
                        openSettings = { nav.navigate(SettingsRoute) },
                        openInventory = { nav.navigate(InventoryRoute) },
                        openCourseComplete = { nav.navigate(CourseCompleteRoute(it)) },
                        openInvite = { nav.switchTab(FamilyRoute(invite = true)) },
                    ),
                    snackbar,
                )
            }
            scheduleGraph(ScheduleNav(back, openMedication, { nav.navigate(RemindersRoute) }, { nav.navigate(NotificationSettingsRoute) }))
            familyGraph(
                FamilyNav(
                    back = back,
                    openMember = { nav.navigate(MemberRoute(it)) },
                    addMember = { child -> nav.navigate(MemberEditRoute(null, child)) },
                    editMember = { nav.navigate(MemberEditRoute(it)) },
                    openMessages = { nav.navigate(MessagesRoute) },
                    openMedication = openMedication,
                    addMedicationFor = { editMedication(null, it) },
                    openNotificationSettings = { nav.navigate(NotificationSettingsRoute) },
                ),
                snackbar,
            )
            historyGraph(HistoryNav(back, openMedication, { nav.navigate(StatsRoute) }, { nav.navigate(HistoryDayRoute(it.toString())) }))
            medicationGraph(MedicationNav(back, editMedication, openMedication) { nav.navigate(CourseCompleteRoute(it)) })
            settingsGraph(
                SettingsNav(
                    back = back,
                    editProfile = { nav.navigate(MemberEditRoute(it)) },
                    openFamily = { nav.switchTab(FamilyRoute()) },
                    openNotifications = { nav.navigate(NotificationSettingsRoute) },
                    openSync = { nav.navigate(SyncSettingsRoute) },
                    openAppearance = { nav.navigate(AppearanceRoute) },
                    openPrivacy = { nav.navigate(PrivacyRoute) },
                    openReminders = { nav.navigate(RemindersRoute) },
                    openInventory = { nav.navigate(InventoryRoute) },
                ),
                snackbar,
            )
        }
    }
}
