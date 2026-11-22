package app.ryadom.feature.family

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ryadom.core.designsystem.component.BadgeKind
import app.ryadom.core.designsystem.component.ButtonStyle
import app.ryadom.core.designsystem.component.EmptyState
import app.ryadom.core.designsystem.component.NoticeBanner
import app.ryadom.core.designsystem.component.RyBadge
import app.ryadom.core.designsystem.component.RyButton
import app.ryadom.core.designsystem.component.RyChip
import app.ryadom.core.designsystem.component.RyConfirmDialog
import app.ryadom.core.designsystem.component.RyDivider
import app.ryadom.core.designsystem.component.RyGroup
import app.ryadom.core.designsystem.component.RyIconButton
import app.ryadom.core.designsystem.component.RyListRow
import app.ryadom.core.designsystem.component.RyMeter
import app.ryadom.core.designsystem.component.RySegmented
import app.ryadom.core.designsystem.component.RySheet
import app.ryadom.core.designsystem.component.RyTextField
import app.ryadom.core.designsystem.component.RyTopBar
import app.ryadom.core.designsystem.component.ScreenTitle
import app.ryadom.core.designsystem.component.SectionTitle
import app.ryadom.core.designsystem.component.SkeletonList
import app.ryadom.core.designsystem.theme.Illustration
import app.ryadom.core.designsystem.theme.Ry
import app.ryadom.core.designsystem.theme.RyIcons
import app.ryadom.core.l10n.R
import app.ryadom.core.model.FamilyGroup
import app.ryadom.core.model.FamilyNudgeScope
import app.ryadom.core.model.FamilyRole
import app.ryadom.core.model.Medication
import app.ryadom.core.model.Person
import app.ryadom.core.model.PersonAccent
import app.ryadom.core.model.PersonKind
import app.ryadom.core.model.QuickMessage
import app.ryadom.core.model.ScheduleEngine
import app.ryadom.core.ui.PersonAvatar
import app.ryadom.core.ui.ageText
import app.ryadom.core.ui.colors
import app.ryadom.core.ui.dayMonth
import app.ryadom.core.ui.dayMonthShort
import app.ryadom.core.ui.hhmm
import app.ryadom.core.ui.label
import app.ryadom.core.ui.text
import app.ryadom.core.ui.userMessage
import kotlinx.coroutines.launch
import java.time.ZoneId

data class FamilyNav(
    val back: () -> Unit,
    val openMember: (String) -> Unit,
    val addMember: (child: Boolean) -> Unit,
    val editMember: (String) -> Unit,
    val openMessages: () -> Unit,
    val openMedication: (String) -> Unit,
    val addMedicationFor: (String) -> Unit,
    val openNotificationSettings: () -> Unit,
)

// ---------- Экран «Семья» ----------

@Composable
fun FamilyScreen(nav: FamilyNav, snackbar: SnackbarHostState, openInviteInitially: Boolean = false, vm: FamilyViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    var invite by rememberSaveable { mutableStateOf(openInviteInitially) }
    var rename by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.errorEvents.collect { snackbar.showSnackbar(ctx.getString(R.string.err_generic)) } }
    val f = st.family
    val me = st.me
    Box(Modifier.fillMaxSize().background(Ry.colors.bg), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 640.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (f == null || me == null) { item { SkeletonList() }; return@LazyColumn }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenTitle(f.name, Modifier.weight(1f), subtitle = stringResource(R.string.family_members_count, f.members.size))
                    if (me.canManageFamily) RyIconButton(RyIcons.Edit, stringResource(R.string.family_rename), { rename = true })
                }
            }
            item {
                RyGroup {
                    f.members.forEachIndexed { i, p ->
                        if (i > 0) RyDivider(start = 68.dp)
                        RyListRow(
                            p.name + if (p.id == me.id) " · " + stringResource(R.string.family_you) else "",
                            subtitle = memberSubtitle(p, f),
                            leading = { PersonAvatar(p, size = 40.dp) },
                            onClick = { nav.openMember(p.id) },
                        )
                    }
                }
            }
            if (me.canManageFamily) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RyButton(stringResource(R.string.family_add_child), { nav.addMember(true) }, Modifier.weight(1f), style = ButtonStyle.Tonal, icon = RyIcons.Child)
                    RyButton(stringResource(R.string.family_invite), { invite = true }, Modifier.weight(1f), style = ButtonStyle.Tonal, icon = RyIcons.UserPlus)
                }
            }
            item {
                RyGroup {
                    RyListRow(
                        stringResource(R.string.messages_title),
                        subtitle = if (st.unread > 0) stringResource(R.string.messages_unread, st.unread) else stringResource(R.string.messages_hint),
                        icon = RyIcons.Message, onClick = nav.openMessages,
                    )
                    RyDivider(start = 52.dp)
                    RyListRow(stringResource(R.string.family_care_rules), subtitle = stringResource(R.string.family_care_rules_hint),
                        icon = RyIcons.Heart, onClick = nav.openNotificationSettings)
                    if (me.canManageFamily) {
                        RyDivider(start = 52.dp)
                        RyListRow(stringResource(R.string.family_add_adult), subtitle = stringResource(R.string.family_add_adult_hint),
                            icon = RyIcons.User, onClick = { nav.addMember(false) })
                    }
                }
            }
            item { Text(stringResource(R.string.family_privacy), style = Ry.type.caption, color = Ry.colors.textMuted) }
        }
    }
    if (invite && f != null) InviteSheet(f, st.now, vm, onDismiss = { invite = false })
    if (rename && f != null) {
        var name by remember { mutableStateOf(f.name) }
        RySheet(onDismiss = { rename = false }, title = stringResource(R.string.family_rename)) {
            RyTextField(name, { name = it.take(40) }, stringResource(R.string.field_family_name))
            RyButton(stringResource(R.string.common_save), { vm.rename(name); rename = false }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun memberSubtitle(p: Person, f: FamilyGroup): String {
    val parts = mutableListOf(p.role.label())
    if (p.kind == PersonKind.CHILD && p.birthDate != null) parts += ageText(p.birthDate!!, java.time.LocalDate.now())
    if (p.kind == PersonKind.ADULT && p.userId == null) parts += stringResource(R.string.family_no_account)
    return parts.joinToString(" · ")
}

@Composable
private fun InviteSheet(f: FamilyGroup, now: java.time.Instant, vm: FamilyViewModel, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val inv = f.invitation?.takeIf { it.isActive(now) }
    RySheet(onDismiss, title = stringResource(R.string.invite_title)) {
        Text(stringResource(R.string.invite_body), style = Ry.type.body, color = Ry.colors.textMuted)
        if (inv == null) {
            RyButton(stringResource(R.string.invite_create), vm::createInvite, Modifier.fillMaxWidth(), icon = RyIcons.Link)
        } else {
            Box(
                Modifier.fillMaxWidth().clip(Ry.shapes.l).background(Ry.colors.primaryContainer).padding(vertical = 18.dp)
                    .semantics { contentDescription = inv.code.toList().joinToString(" ") },
                contentAlignment = Alignment.Center,
            ) {
                Text(inv.code.chunked(3).joinToString(" "), style = Ry.type.display.copy(letterSpacing = Ry.type.display.letterSpacing * -8), color = Ry.colors.primaryDark)
            }
            Text(stringResource(R.string.invite_valid_until, inv.expiresAt.atZone(ZoneId.systemDefault()).toLocalDate().dayMonth()),
                style = Ry.type.caption, color = Ry.colors.textMuted)
            val shareText = stringResource(R.string.invite_share_text, inv.code)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RyButton(stringResource(R.string.common_share), {
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText), null))
                }, Modifier.weight(1f), icon = RyIcons.Share)
                RyButton(stringResource(R.string.common_copy), {
                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("code", inv.code))) }
                }, Modifier.weight(1f), style = ButtonStyle.Ghost)
            }
            RyButton(stringResource(R.string.invite_revoke), vm::revokeInvite, Modifier.fillMaxWidth(), style = ButtonStyle.Text)
        }
        NoticeBanner(stringResource(R.string.invite_cloud_note), RyIcons.Info)
    }
}

// ---------- Профиль ----------

@Composable
fun MemberScreen(nav: FamilyNav, snackbar: SnackbarHostState, vm: MemberViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    var confirmTransfer by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.errorEvents.collect { snackbar.showSnackbar(it.userMessageText(ctx)) } }
    LaunchedEffect(st.removed) { if (st.removed) nav.back() }
    val p = st.person
    val me = st.me
    val f = st.family
    Column(Modifier.fillMaxSize().background(Ry.colors.bg)) {
        RyTopBar("", navLabel = stringResource(R.string.common_back), onNav = nav.back, actions = {
            if (p != null && me != null && (me.canManageFamily || p.id == me.id)) RyIconButton(RyIcons.More, stringResource(R.string.common_more), { menu = true })
        })
        if (st.loading || f == null || me == null) { Box(Modifier.padding(16.dp)) { SkeletonList() }; return@Column }
        if (p == null) {
            EmptyState(Illustration.FamilyCare, stringResource(R.string.member_not_found), "", primary = stringResource(R.string.common_back) to nav.back)
            return@Column
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 640.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        PersonAvatar(p, size = 72.dp)
                        Column {
                            Text(p.name, style = Ry.type.display, color = Ry.colors.text)
                            Text(memberSubtitle(p, f), style = Ry.type.caption, color = Ry.colors.textMuted)
                        }
                    }
                }
                item {
                    RySegmented(listOf(stringResource(R.string.member_tab_meds), stringResource(R.string.member_tab_history), stringResource(R.string.member_tab_rules)), tab, { tab = it })
                }
                when (tab) {
                    0 -> {
                        val active = st.meds.filter { !it.archived }
                        val archived = st.meds.filter { it.archived }
                        if (active.isEmpty()) item {
                            EmptyState(
                                if (p.kind == PersonKind.CHILD) Illustration.MotherChild else Illustration.Vitamins,
                                stringResource(R.string.member_no_meds_title), stringResource(R.string.member_no_meds_body, p.name),
                                primary = stringResource(R.string.action_add_medication) to { nav.addMedicationFor(p.id) }, primaryIcon = RyIcons.Plus,
                            )
                        } else {
                            item { MedList(active, st.today, nav.openMedication) }
                            item { RyButton(stringResource(R.string.member_add_med_for, p.name), { nav.addMedicationFor(p.id) }, Modifier.fillMaxWidth(), style = ButtonStyle.Tonal, icon = RyIcons.Plus) }
                        }
                        if (archived.isNotEmpty()) {
                            item { SectionTitle(stringResource(R.string.member_archive)) }
                            item { MedList(archived, st.today, nav.openMedication) }
                        }
                    }
                    1 -> {
                        val total = st.week.sumOf { it.total }
                        val taken = st.week.sumOf { it.taken }
                        item {
                            Text(stringResource(R.string.member_week_summary, taken, total), style = Ry.type.headline, color = Ry.colors.text)
                        }
                        item {
                            RyGroup {
                                st.week.reversed().forEachIndexed { i, d ->
                                    if (i > 0) RyDivider(start = 16.dp)
                                    RyListRow(
                                        d.date.dayMonth(),
                                        subtitle = if (d.total == 0) stringResource(R.string.history_no_intakes) else stringResource(R.string.count_of, d.taken, d.total) +
                                            if (d.skipped > 0) " · " + stringResource(R.string.history_skipped_n, d.skipped) else "",
                                        trailing = null,
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        item {
                            RyGroup {
                                RyListRow(
                                    stringResource(R.string.member_rules_scope),
                                    subtitle = scopeText(st.scope),
                                    icon = RyIcons.Heart, onClick = nav.openNotificationSettings,
                                )
                            }
                        }
                        val withNudge = st.meds.filter { !it.archived && it.reminder.familyNudgeAfterMinutes != null }
                        item {
                            if (withNudge.isEmpty()) Text(stringResource(R.string.member_rules_none), style = Ry.type.bodySmall, color = Ry.colors.textMuted)
                            else RyGroup {
                                withNudge.forEachIndexed { i, m ->
                                    if (i > 0) RyDivider(start = 16.dp)
                                    RyListRow(m.name, subtitle = stringResource(R.string.med_remind_family_after, m.reminder.familyNudgeAfterMinutes!!),
                                        onClick = { nav.openMedication(m.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (menu && p != null && me != null) {
        RySheet(onDismiss = { menu = false }, title = p.name) {
            Column {
                RyListRow(stringResource(R.string.common_edit), icon = RyIcons.Edit, trailing = null, onClick = { menu = false; nav.editMember(p.id) })
                if (me.role == FamilyRole.OWNER && p.kind == PersonKind.ADULT && p.id != me.id) {
                    RyListRow(
                        stringResource(if (p.role == FamilyRole.ADMIN) R.string.member_revoke_admin else R.string.member_make_admin),
                        icon = RyIcons.Shield, trailing = null, onClick = { menu = false; vm.makeAdmin(p.role != FamilyRole.ADMIN) },
                    )
                    if (p.userId != null) RyListRow(stringResource(R.string.member_transfer), icon = RyIcons.Share, trailing = null,
                        onClick = { menu = false; confirmTransfer = true })
                }
                if (me.canManageFamily && p.id != me.id && p.role != FamilyRole.OWNER) {
                    RyListRow(stringResource(R.string.member_remove), icon = RyIcons.Trash, titleColor = Ry.colors.errorText, trailing = null,
                        onClick = { menu = false; confirmRemove = true })
                }
            }
        }
    }
    if (confirmRemove && p != null) RyConfirmDialog(
        stringResource(R.string.member_remove_title, p.name), stringResource(R.string.member_remove_body),
        stringResource(R.string.member_remove), stringResource(R.string.common_cancel),
        onConfirm = { confirmRemove = false; vm.remove() }, onDismiss = { confirmRemove = false }, destructive = true,
    )
    if (confirmTransfer && p != null) RyConfirmDialog(
        stringResource(R.string.member_transfer_title), stringResource(R.string.member_transfer_body, p.name),
        stringResource(R.string.member_transfer), stringResource(R.string.common_cancel),
        onConfirm = { confirmTransfer = false; vm.transferOwnership() }, onDismiss = { confirmTransfer = false },
    )
}

private fun Throwable.userMessageText(ctx: android.content.Context): String {
    val r = (this as? app.ryadom.core.model.RyadomException)?.reason
    return ctx.getString(
        when (r) {
            app.ryadom.core.model.RyadomException.Reason.NOT_ALLOWED -> R.string.err_not_allowed
            app.ryadom.core.model.RyadomException.Reason.LAST_OWNER -> R.string.err_last_owner
            else -> R.string.err_generic
        },
    )
}

@Composable
private fun scopeText(s: FamilyNudgeScope) = stringResource(
    when (s) {
        FamilyNudgeScope.ALL -> R.string.scope_all
        FamilyNudgeScope.CHILDREN_ONLY -> R.string.scope_children
        FamilyNudgeScope.IMPORTANT_ONLY -> R.string.scope_important
        FamilyNudgeScope.NONE -> R.string.scope_none
    },
)

@Composable
private fun MedList(meds: List<Medication>, today: java.time.LocalDate, onOpen: (String) -> Unit) {
    RyGroup {
        meds.forEachIndexed { i, m ->
            if (i > 0) RyDivider(start = 16.dp)
            Column(Modifier.fillMaxWidth().clickable { onOpen(m.id) }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.name, style = Ry.type.subtitle, color = Ry.colors.text, modifier = Modifier.weight(1f))
                    Text(m.schedule.times.joinToString(", ") { it.hhmm() }, style = Ry.type.caption, color = Ry.colors.textMuted)
                }
                val inv = m.inventory
                if (inv != null) RyMeter((inv.remaining / (inv.packageSize ?: inv.remaining.coerceAtLeast(1.0))).toFloat(), ScheduleEngine.isLowStock(m))
                val parts = buildList {
                    add(m.dosage.text())
                    ScheduleEngine.daysLeft(m)?.let { add(stringResource(R.string.stock_days, it)) }
                    ScheduleEngine.courseProgress(m, today)?.takeIf { m.course.finishedAt == null }?.let { add(stringResource(R.string.course_day_of, it.first, it.second)) }
                    if (m.course.finishedAt != null) add(stringResource(R.string.course_finished_on, m.course.finishedAt!!.dayMonthShort()))
                }
                Text(parts.joinToString(" · "), style = Ry.type.caption,
                    color = if (ScheduleEngine.isLowStock(m)) Ry.colors.warningText else Ry.colors.textMuted)
            }
        }
    }
}

// ---------- Добавить / изменить члена семьи ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberEditScreen(onClose: () -> Unit, vm: MemberEditViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    var pickBirth by remember { mutableStateOf(false) }
    LaunchedEffect(st.done) { if (st.done) onClose() }
    Column(Modifier.fillMaxSize().background(Ry.colors.bg).imePadding()) {
        RyTopBar(
            stringResource(if (st.isNew) (if (st.draft.kind == PersonKind.CHILD) R.string.member_new_child else R.string.member_new_adult) else R.string.member_edit),
            navIcon = RyIcons.Close, navLabel = stringResource(R.string.common_close), onNav = onClose,
            actions = { RyButton(stringResource(R.string.common_done), vm::save, style = ButtonStyle.Text) },
        )
        if (st.loading) return@Column
        val d = st.draft
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                RyTextField(d.name, { v -> vm.edit { it.copy(name = v.take(40)) } }, stringResource(R.string.field_name),
                    error = if (st.nameError) stringResource(R.string.err_empty_name) else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                if (st.isNew) {
                    RySegmented(listOf(stringResource(R.string.kind_child), stringResource(R.string.kind_adult)),
                        if (d.kind == PersonKind.CHILD) 0 else 1, { i -> vm.edit { it.copy(kind = if (i == 0) PersonKind.CHILD else PersonKind.ADULT) } })
                }
                if (d.kind == PersonKind.ADULT && st.isNew) NoticeBanner(stringResource(R.string.member_adult_note), RyIcons.Info)
                RyGroup {
                    RyListRow(
                        stringResource(R.string.field_birth_date),
                        subtitle = d.birthDate?.dayMonth()?.let { "$it ${d.birthDate!!.year}" } ?: stringResource(R.string.field_optional),
                        icon = RyIcons.Calendar, onClick = { pickBirth = true },
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.field_color), style = Ry.type.caption, color = Ry.colors.textMuted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PersonAccent.entries.forEach { a ->
                            val pair = a.colors()
                            val on = a == d.accent
                            Box(
                                Modifier.size(48.dp).clip(CircleShape)
                                    .clickable(role = Role.RadioButton) { vm.edit { it.copy(accent = a) } }
                                    .semantics { selected = on; contentDescription = a.name.lowercase() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape).background(pair.container)
                                        .then(if (on) Modifier.border(2.dp, pair.tone, CircleShape) else Modifier),
                                    contentAlignment = Alignment.Center,
                                ) { Text(d.name.firstOrNull()?.uppercase() ?: "", style = Ry.type.label, color = pair.tone) }
                            }
                        }
                    }
                }
                st.error?.let { NoticeBanner(it.userMessage(), RyIcons.Info, warn = true) }
                RyButton(stringResource(R.string.common_save), vm::save, Modifier.fillMaxWidth())
            }
        }
    }
    if (pickBirth) {
        app.ryadom.core.ui.RyDatePicker(st.draft.birthDate ?: java.time.LocalDate.now().minusYears(3), { pickBirth = false }) { v ->
            pickBirth = false; vm.edit { it.copy(birthDate = v) }
        }
    }
}

// ---------- Сообщения ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessagesScreen(onBack: () -> Unit, vm: MessagesViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    var text by rememberSaveable { mutableStateOf("") }
    val f = st.family
    val me = st.me
    val list = rememberLazyListState()
    LaunchedEffect(st.messages.size) { if (st.messages.isNotEmpty()) list.animateScrollToItem(st.messages.size - 1) }
    Column(Modifier.fillMaxSize().background(Ry.colors.bg).imePadding()) {
        RyTopBar(stringResource(R.string.messages_title), navLabel = stringResource(R.string.common_back), onNav = onBack)
        if (f == null || me == null) return@Column
        val partner = f.adults.firstOrNull { it.id != me.id }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            if (st.messages.isEmpty()) {
                EmptyState(Illustration.FamilyCare, stringResource(R.string.messages_empty_title), stringResource(R.string.messages_empty_body))
            } else LazyColumn(
                Modifier.widthIn(max = 640.dp).fillMaxSize(), state = list,
                contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(st.messages, key = { it.id }) { m ->
                    val mine = m.fromId == me.id
                    val from = f.person(m.fromId)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Column(
                            Modifier.widthIn(max = 300.dp).clip(Ry.shapes.l)
                                .background(if (mine) Ry.colors.primaryContainer else Ry.colors.surface)
                                .then(if (mine) Modifier else Modifier.border(1.dp, Ry.colors.border, Ry.shapes.l))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (!mine && from != null) Text(from.name, style = Ry.type.caption, color = from.colors().tone)
                            Text(m.text ?: m.quick?.text().orEmpty(), style = Ry.type.body, color = Ry.colors.text)
                            val about = m.medicationId?.let { st.meds[it]?.name }
                            val time = m.at.atZone(ZoneId.systemDefault()).toLocalTime().hhmm()
                            Text(listOfNotNull(about, time).joinToString(" · "), style = Ry.type.caption, color = Ry.colors.textMuted)
                        }
                    }
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().background(Ry.colors.surface).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickMessage.entries.forEach { q -> RyChip(q.text(), false, { vm.send(q, null, partner?.id) }) }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RyTextField(text, { text = it.take(140) }, stringResource(R.string.messages_own), Modifier.weight(1f),
                    placeholder = partner?.let { stringResource(R.string.messages_to, it.name) })
                RyIconButton(RyIcons.Send, stringResource(R.string.messages_send), {
                    if (text.isNotBlank()) { vm.send(null, text, partner?.id); text = "" }
                }, tint = Ry.colors.primaryDark, enabled = text.isNotBlank())
            }
        }
    }
}
