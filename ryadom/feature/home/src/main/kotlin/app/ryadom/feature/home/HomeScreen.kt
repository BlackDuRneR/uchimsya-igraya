package app.ryadom.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ryadom.core.data.repository.DayPlan
import app.ryadom.core.designsystem.component.ButtonStyle
import app.ryadom.core.designsystem.component.EmptyState
import app.ryadom.core.designsystem.component.GroupHeader
import app.ryadom.core.designsystem.component.NoticeBanner
import app.ryadom.core.designsystem.component.RyButton
import app.ryadom.core.designsystem.component.RyChip
import app.ryadom.core.designsystem.component.RyDivider
import app.ryadom.core.designsystem.component.RyGroup
import app.ryadom.core.designsystem.component.RyIcon
import app.ryadom.core.designsystem.component.RyIllustration
import app.ryadom.core.designsystem.component.SegmentedProgress
import app.ryadom.core.designsystem.component.SkeletonList
import app.ryadom.core.designsystem.component.rememberHaptics
import app.ryadom.core.designsystem.theme.Illustration
import app.ryadom.core.designsystem.theme.Ry
import app.ryadom.core.designsystem.theme.RyIcons
import app.ryadom.core.l10n.R
import app.ryadom.core.model.FamilyGroup
import app.ryadom.core.model.FamilyMessage
import app.ryadom.core.model.Intake
import app.ryadom.core.model.IntakeStatus
import app.ryadom.core.model.ScheduleEngine
import app.ryadom.core.model.SyncState
import app.ryadom.core.ui.IntakeActionsSheet
import app.ryadom.core.ui.IntakeRow
import app.ryadom.core.ui.PersonAvatar
import app.ryadom.core.ui.SyncIndicator
import app.ryadom.core.ui.greeting
import app.ryadom.core.ui.hhmm
import app.ryadom.core.ui.icon
import app.ryadom.core.ui.label
import app.ryadom.core.ui.longDate
import app.ryadom.core.ui.text
import app.ryadom.core.ui.userMessage
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute

data class HomeNav(
    val openMedication: (String) -> Unit,
    val addMedication: () -> Unit,
    val openMessages: () -> Unit,
    val openPerson: (String) -> Unit,
    val openSettings: () -> Unit,
    val openInventory: () -> Unit,
    val openCourseComplete: (String) -> Unit,
    val openInvite: () -> Unit,
)

@Composable
fun HomeRouteScreen(nav: HomeNav, snackbar: SnackbarHostState, vm: HomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        vm.uiEvents.collect { e ->
            when (e) {
                is HomeEvent.OpenMedication -> nav.openMedication(e.id)
                is HomeEvent.Error -> snackbar.showSnackbar(ctx.getString(R.string.err_generic))
            }
        }
    }
    HomeScreen(state, vm, nav)
}

@Composable
fun HomeScreen(state: HomeState, vm: HomeViewModel?, nav: HomeNav) {
    var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }
    val plan = state.plan
    val family = state.family
    Box(Modifier.fillMaxSize().background(Ry.colors.bg), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 640.dp).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") { Header(state, nav) }
            if (state.loading || plan == null || family == null) {
                item(key = "skeleton") { SkeletonList() }
                return@LazyColumn
            }
            if (state.sync == SyncState.OFFLINE) item(key = "offline") {
                NoticeBanner(stringResource(R.string.offline_banner), RyIcons.CloudOff, warn = true)
            }
            if (state.isDemo) item(key = "demo") {
                NoticeBanner(stringResource(R.string.demo_banner), RyIcons.Info,
                    action = stringResource(R.string.demo_leave) to { vm?.leaveDemo() })
            }
            item(key = "progress") { Progress(plan, nav) }

            if (plan.intakes.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        Illustration.EmptySchedule,
                        stringResource(R.string.home_empty_title),
                        stringResource(R.string.home_empty_body),
                        primary = stringResource(R.string.action_add_medication) to nav.addMedication,
                        primaryIcon = RyIcons.Plus,
                        secondary = if (family.adults.size < 2) stringResource(R.string.action_invite_partner) to nav.openInvite else null,
                    )
                }
            } else {
                val current = plan.current
                if (current != null) item(key = "now") {
                    NowCard(current, plan, onMark = { vm?.taken(current) }, onLater = { sheetFor = current.id })
                } else if (plan.allDone && state.showDayComplete) item(key = "done") { DayCompleteCard() }
            }

            state.message?.let { msg -> item(key = "msg") { MessageStrip(msg, family, onOpen = { vm?.markMessagesRead(); nav.openMessages() }, onThanks = { vm?.replyThanks(msg) }) } }

            state.courseEnding.forEach { m ->
                item(key = "course-${m.id}") {
                    NoticeBanner(stringResource(R.string.home_course_ends_today, m.name), RyIcons.Flag,
                        action = stringResource(R.string.action_finish) to { nav.openCourseComplete(m.id) })
                }
            }
            if (state.lowStock.isNotEmpty()) item(key = "stock") {
                val m = state.lowStock.first()
                val days = ScheduleEngine.daysLeft(m) ?: 0
                val more = state.lowStock.size - 1
                NoticeBanner(
                    if (more > 0) stringResource(R.string.home_low_stock_many, m.name, days, more) else stringResource(R.string.home_low_stock, m.name, days),
                    RyIcons.Bag, warn = true, action = stringResource(R.string.inventory_title) to nav.openInventory,
                )
            }

            plan.byPart.forEach { (part, list) ->
                item(key = "part-$part") {
                    RyGroup(
                        Modifier.animateContentSize(),
                        header = {
                            GroupHeader(part.icon, part.label(), stringResource(R.string.count_of, list.count { it.isDone }, list.count { it.status != IntakeStatus.CANCELLED }))
                        },
                    ) {
                        list.forEachIndexed { i, intake ->
                            if (i > 0) RyDivider(start = 66.dp)
                            IntakeRow(intake, family, onCheck = { vm?.toggle(intake) }, onOpen = { sheetFor = intake.id })
                        }
                    }
                }
            }
        }
    }
    val sheetIntake = plan?.intakes?.firstOrNull { it.id == sheetFor }
    if (sheetIntake != null && family != null && vm != null) {
        IntakeActionsSheet(sheetIntake, family, state.me?.id, plan.now, vm, onDismiss = { sheetFor = null })
    }
}

@Composable
private fun Header(state: HomeState, nav: HomeNav) {
    val plan = state.plan
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            val hour = plan?.now?.hour ?: java.time.LocalTime.now().hour
            val name = state.me?.name
            Text(
                if (name != null) stringResource(R.string.home_greeting_named, greeting(hour), name) else greeting(hour),
                style = Ry.type.display, color = Ry.colors.text, modifier = Modifier.semantics { heading() },
            )
            Text(
                (plan?.date ?: java.time.LocalDate.now()).longDate(),
                style = Ry.type.caption, color = Ry.colors.textMuted, modifier = Modifier.padding(top = 4.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.me?.let { me ->
                Box(
                    Modifier.size(48.dp).clip(Ry.shapes.pill).clickable(role = Role.Button, onClickLabel = stringResource(R.string.settings_title), onClick = nav.openSettings),
                    contentAlignment = Alignment.Center,
                ) { PersonAvatar(me, size = 40.dp) }
            }
            SyncIndicator(state.sync)
        }
    }
}

@Composable
private fun Progress(plan: DayPlan, nav: HomeNav) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.count_of, plan.done, plan.total), style = Ry.type.headline, color = Ry.colors.text)
            Text(stringResource(R.string.home_progress_caption), style = Ry.type.caption, color = Ry.colors.textMuted, modifier = Modifier.padding(bottom = 3.dp))
        }
        SegmentedProgress(plan.total, plan.done, highlightNext = plan.current != null)
        if (plan.people.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(plan.people, key = { it.person.id }) { p ->
                    RyChip(
                        stringResource(R.string.count_slash, p.done, p.total), false, { nav.openPerson(p.person.id) },
                        leading = { PersonAvatar(p.person, size = 24.dp) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NowCard(intake: Intake, plan: DayPlan, onMark: () -> Unit, onLater: () -> Unit) {
    val c = Ry.colors
    val haptics = rememberHaptics()
    Column(
        Modifier.fillMaxWidth().clip(Ry.shapes.l).background(c.surface).border(1.dp, c.border, Ry.shapes.l).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(Ry.shapes.m).background(c.warmContainer), contentAlignment = Alignment.Center) {
                RyIcon(intake.medication.dosage.unit.icon, null)
            }
            Column(Modifier.weight(1f)) {
                val due = intake.status == IntakeStatus.DUE
                Text(
                    stringResource(if (due) R.string.home_now else R.string.home_soon, intake.effectiveAt.toLocalTime().hhmm()).uppercase(),
                    style = Ry.type.overline, color = if (due) c.primaryDark else c.textMuted,
                )
                Text(intake.medication.name, style = Ry.type.title, color = c.text)
                Text("${intake.person.name} · ${intake.medication.dosage.text()}", style = Ry.type.bodySmall, color = c.textMuted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RyButton(stringResource(R.string.intake_mark), { haptics.confirm(); onMark() }, Modifier.weight(1f), icon = RyIcons.Check)
            RyButton(stringResource(R.string.intake_later), onLater, Modifier.weight(1f), style = ButtonStyle.Ghost, icon = RyIcons.Later)
        }
    }
}

@Composable
private fun DayCompleteCard() {
    Column(
        Modifier.fillMaxWidth().clip(Ry.shapes.l).background(Ry.colors.surface).border(1.dp, Ry.colors.border, Ry.shapes.l).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RyIllustration(Illustration.DayComplete, Modifier.widthIn(max = 220.dp).fillMaxWidth())
        Text(stringResource(R.string.home_day_complete_title), style = Ry.type.headline, color = Ry.colors.text)
        Text(stringResource(R.string.home_day_complete_body), style = Ry.type.bodySmall, color = Ry.colors.textMuted)
    }
}

@Composable
private fun MessageStrip(msg: FamilyMessage, family: FamilyGroup, onOpen: () -> Unit, onThanks: () -> Unit) {
    val from = family.person(msg.fromId) ?: return
    Row(
        Modifier.fillMaxWidth().clip(Ry.shapes.m).background(Ry.colors.surfaceMuted).clickable(onClick = onOpen).padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PersonAvatar(from)
        Text(
            "${from.name}: ${msg.text ?: msg.quick?.text().orEmpty()}",
            style = Ry.type.bodySmall, color = Ry.colors.text, modifier = Modifier.weight(1f), maxLines = 2,
        )
        if (msg.quick != app.ryadom.core.model.QuickMessage.THANKS) {
            RyChip(stringResource(R.string.quick_thanks_short), false, onThanks)
        }
    }
}
