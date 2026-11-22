package app.ryadom.feature.schedule

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

data class ScheduleNav(
    val back: () -> Unit,
    val openMedication: (String) -> Unit,
    val openReminders: () -> Unit,
    val openNotificationSettings: () -> Unit,
)

fun NavGraphBuilder.scheduleGraph(nav: ScheduleNav) {
    composable<ScheduleRoute> { ScheduleScreen(onOpenMedication = nav.openMedication, onReminders = nav.openReminders) }
    composable<RemindersRoute> { RemindersScreen(onBack = nav.back, onSettings = nav.openNotificationSettings) }
}
