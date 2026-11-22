package app.ryadom.feature.schedule

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.ryadom.core.data.repository.PlanRepository
import app.ryadom.core.data.util.Clock
import app.ryadom.core.designsystem.component.BadgeKind
import app.ryadom.core.designsystem.component.ButtonStyle
import app.ryadom.core.designsystem.component.EmptyState
import app.ryadom.core.designsystem.component.NoticeBanner
import app.ryadom.core.designsystem.component.RyBadge
import app.ryadom.core.designsystem.component.RyButton
import app.ryadom.core.designsystem.component.RyDivider
import app.ryadom.core.designsystem.component.RyGroup
import app.ryadom.core.designsystem.component.RyIcon
import app.ryadom.core.designsystem.component.RyTopBar
import app.ryadom.core.designsystem.component.SectionTitle
import app.ryadom.core.designsystem.theme.Illustration
import app.ryadom.core.designsystem.theme.Ry
import app.ryadom.core.designsystem.theme.RyIcons
import app.ryadom.core.l10n.R
import app.ryadom.core.model.Intake
import app.ryadom.core.ui.PersonDot
import app.ryadom.core.ui.hhmm
import app.ryadom.core.ui.relativeTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import java.time.LocalDate
import javax.inject.Inject

@Serializable data object RemindersRoute

data class RemindersState(val today: LocalDate = LocalDate.now(), val upcoming: List<Intake> = emptyList(), val loading: Boolean = true)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RemindersViewModel @Inject constructor(plan: PlanRepository, clock: Clock) : ViewModel() {
    val state: StateFlow<RemindersState> = plan.today().flatMapLatest { t ->
        combine(plan.day(t), plan.day(t.plusDays(1))) { a, b ->
            val horizon = a.now.plusHours(24)
            RemindersState(t, (a.intakes + b.intakes).filter {
                it.isOpen && it.medication.reminder.enabled && !it.effectiveAt.isAfter(horizon)
            }.sortedBy { it.effectiveAt }, false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersState(today = clock.today()))
}

/** Разрешения, от которых зависит надёжность напоминаний. Проверяются заново при каждом возврате на экран. */
private data class Permissions(val notifications: Boolean, val exactAlarms: Boolean, val unrestrictedBattery: Boolean)

private fun Context.permissions(): Permissions {
    val notif = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val exact = Build.VERSION.SDK_INT < 31 || getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    val battery = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
    return Permissions(notif, exact, battery)
}

@Composable
fun RemindersScreen(onBack: () -> Unit, onSettings: () -> Unit, vm: RemindersViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
    val perms = remember(tick) { ctx.permissions() }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    Column(Modifier.fillMaxSize().background(Ry.colors.bg)) {
        RyTopBar(stringResource(R.string.reminders_title), navLabel = stringResource(R.string.common_back), onNav = onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 640.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    NoticeBanner(stringResource(R.string.reminders_stage_note), RyIcons.Info)
                }
                item { SectionTitle(stringResource(R.string.reminders_reliability)) }
                item {
                    RyGroup {
                        PermissionRow(
                            RyIcons.Bell, stringResource(R.string.perm_notifications), stringResource(R.string.perm_notifications_hint), perms.notifications,
                        ) {
                            if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        RyDivider(start = 52.dp)
                        PermissionRow(
                            RyIcons.Clock, stringResource(R.string.perm_exact), stringResource(R.string.perm_exact_hint), perms.exactAlarms,
                        ) {
                            if (Build.VERSION.SDK_INT >= 31) ctx.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")),
                            )
                        }
                        RyDivider(start = 52.dp)
                        PermissionRow(
                            RyIcons.Shield, stringResource(R.string.perm_battery), stringResource(R.string.perm_battery_hint), perms.unrestrictedBattery,
                        ) { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                    }
                }
                item {
                    Text(stringResource(R.string.reminders_os_note), style = Ry.type.caption, color = Ry.colors.textMuted)
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle(stringResource(R.string.reminders_next_24h), Modifier.weight(1f))
                        RyButton(stringResource(R.string.settings_notifications), onSettings, style = ButtonStyle.Text, small = true)
                    }
                }
                if (!st.loading && st.upcoming.isEmpty()) {
                    item { EmptyState(Illustration.Reminder, stringResource(R.string.reminders_empty_title), stringResource(R.string.reminders_empty_body)) }
                } else item {
                    RyGroup {
                        st.upcoming.forEachIndexed { i, intake ->
                            if (i > 0) RyDivider(start = 16.dp)
                            UpcomingRow(intake, st.today)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(icon: Int, title: String, hint: String, ok: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RyIcon(icon, null, tint = Ry.colors.textMuted)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = Ry.type.subtitle, color = Ry.colors.text)
            Text(hint, style = Ry.type.bodySmall, color = Ry.colors.textMuted)
            if (ok) RyBadge(stringResource(R.string.perm_ok), BadgeKind.Ok, icon = RyIcons.Check)
            else RyButton(stringResource(R.string.perm_fix), onFix, small = true, style = ButtonStyle.Tonal)
        }
    }
}

@Composable
private fun UpcomingRow(intake: Intake, today: LocalDate) {
    val r = intake.medication.reminder
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(intake.effectiveAt.toLocalTime().hhmm(), style = Ry.type.number, color = Ry.colors.text)
            if (intake.scheduledAt.toLocalDate() != today) Text(intake.scheduledAt.toLocalDate().relativeTitle(today), style = Ry.type.caption, color = Ry.colors.textMuted)
            Text(intake.medication.name, style = Ry.type.subtitle, color = Ry.colors.text, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PersonDot(intake.person)
            Text(
                buildList {
                    add(intake.person.name)
                    add(if (r.advanceMinutes == 0) stringResource(R.string.med_remind_on_time) else stringResource(R.string.med_remind_before, r.advanceMinutes))
                    r.repeatAfterMinutes?.let { add(stringResource(R.string.med_remind_repeat_after, it)) }
                    r.familyNudgeAfterMinutes?.let { add(stringResource(R.string.med_remind_family_after, it)) }
                }.joinToString(" · "),
                style = Ry.type.bodySmall, color = Ry.colors.textMuted,
            )
        }
    }
}
