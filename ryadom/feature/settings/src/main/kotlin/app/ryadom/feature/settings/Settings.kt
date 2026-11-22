package app.ryadom.feature.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.ryadom.core.data.repository.ConnectivityRepository
import app.ryadom.core.data.repository.DataExport
import app.ryadom.core.data.repository.FamilyRepository
import app.ryadom.core.data.repository.Session
import app.ryadom.core.data.repository.SessionRepository
import app.ryadom.core.data.repository.SettingsRepository
import app.ryadom.core.designsystem.component.BadgeKind
import app.ryadom.core.designsystem.component.ButtonStyle
import app.ryadom.core.designsystem.component.NoticeBanner
import app.ryadom.core.designsystem.component.RyBadge
import app.ryadom.core.designsystem.component.RyButton
import app.ryadom.core.designsystem.component.RyConfirmDialog
import app.ryadom.core.designsystem.component.RyDivider
import app.ryadom.core.designsystem.component.RyGroup
import app.ryadom.core.designsystem.component.RyListRow
import app.ryadom.core.designsystem.component.RySwitchRow
import app.ryadom.core.designsystem.component.RyTopBar
import app.ryadom.core.designsystem.component.SectionTitle
import app.ryadom.core.designsystem.theme.Ry
import app.ryadom.core.designsystem.theme.RyIcons
import app.ryadom.core.l10n.R
import app.ryadom.core.model.FamilyNudgeScope
import app.ryadom.core.model.FamilyRole
import app.ryadom.core.model.NotificationSettings
import app.ryadom.core.model.SyncState
import app.ryadom.core.model.ThemeMode
import app.ryadom.core.model.UserSettings
import app.ryadom.core.ui.PersonAvatar
import app.ryadom.core.ui.RyTimePickerDialog
import app.ryadom.core.ui.SyncIndicator
import app.ryadom.core.ui.hhmm
import app.ryadom.core.ui.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.time.LocalTime
import javax.inject.Inject

@Serializable data object SettingsRoute
@Serializable data object NotificationSettingsRoute
@Serializable data object SyncSettingsRoute
@Serializable data object AppearanceRoute
@Serializable data object PrivacyRoute

data class SettingsState(
    val session: Session = Session.Loading,
    val prefs: UserSettings = UserSettings(),
    val sync: SyncState = SyncState.LOCAL_ONLY,
    val online: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val settings: SettingsRepository,
    private val family: FamilyRepository,
    connectivity: ConnectivityRepository,
    private val export: DataExport,
) : ViewModel() {
    private val errors = Channel<Throwable>(Channel.BUFFERED)
    val errorEvents = errors.receiveAsFlow()

    val state: StateFlow<SettingsState> = combine(sessionRepo.session, settings.settings, connectivity.sync, connectivity.online) { s, p, sy, on ->
        SettingsState(s, p, sy, on)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    private fun run(block: suspend () -> Unit) = viewModelScope.launch { runCatching { block() }.onFailure { errors.send(it) } }

    fun setTheme(t: ThemeMode) = run { settings.setTheme(t) }
    fun setLanguage(tag: String?) = run {
        settings.setLanguage(tag)
        AppCompatDelegate.setApplicationLocales(if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
    }
    fun notifications(block: (NotificationSettings) -> NotificationSettings) = run { settings.updateNotifications(block) }
    fun signOut() = run { sessionRepo.signOut() }
    fun deleteAccount() = run { sessionRepo.deleteAccount() }
    fun leaveFamily() = run { family.leaveFamily() }
    fun deleteFamily() = run { family.deleteFamily() }

    suspend fun exportJson(): String = withContext(Dispatchers.Default) { export.exportJson() }
}

data class SettingsNav(
    val back: () -> Unit,
    val editProfile: (String) -> Unit,
    val openFamily: () -> Unit,
    val openNotifications: () -> Unit,
    val openSync: () -> Unit,
    val openAppearance: () -> Unit,
    val openPrivacy: () -> Unit,
    val openReminders: () -> Unit,
    val openInventory: () -> Unit,
)

@Composable
private fun SettingsScaffold(title: String, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Ry.colors.bg)) {
        RyTopBar(title, navLabel = stringResource(R.string.common_back), onNav = onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 640.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
}

@Composable
fun SettingsScreen(nav: SettingsNav, snackbar: SnackbarHostState, vm: SettingsViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.errorEvents.collect { snackbar.showSnackbar(ctx.getString(R.string.err_generic)) } }
    val ready = st.session as? Session.Ready
    SettingsScaffold(stringResource(R.string.settings_title), nav.back) {
        if (ready != null) item {
            RyGroup {
                RyListRow(ready.me.name, subtitle = ready.user.email, leading = { PersonAvatar(ready.me, size = 44.dp) }, onClick = { nav.editProfile(ready.me.id) })
            }
        }
        item {
            RyGroup {
                RyListRow(stringResource(R.string.settings_family), subtitle = ready?.family?.name, icon = RyIcons.Family, onClick = nav.openFamily)
                RyDivider(start = 52.dp)
                RyListRow(stringResource(R.string.settings_notifications), icon = RyIcons.Bell, onClick = nav.openNotifications)
                RyDivider(start = 52.dp)
                RyListRow(stringResource(R.string.reminders_title), subtitle = stringResource(R.string.settings_reminders_hint), icon = RyIcons.Clock, onClick = nav.openReminders)
                RyDivider(start = 52.dp)
                RyListRow(stringResource(R.string.inventory_title), icon = RyIcons.Box, onClick = nav.openInventory)
            }
        }
        item {
            RyGroup {
                RyListRow(stringResource(R.string.settings_sync), icon = RyIcons.Sync, onClick = nav.openSync,
                    trailing = { SyncIndicator(st.sync) })
                RyDivider(start = 52.dp)
                RyListRow(stringResource(R.string.settings_appearance), subtitle = themeName(st.prefs.theme), icon = RyIcons.Theme, onClick = nav.openAppearance)
                RyDivider(start = 52.dp)
                RyListRow(stringResource(R.string.settings_privacy), icon = RyIcons.Lock, onClick = nav.openPrivacy)
            }
        }
        item {
            RyGroup {
                RyListRow(stringResource(R.string.settings_sign_out), icon = RyIcons.Logout, trailing = null, onClick = { confirmSignOut = true })
            }
        }
        item {
            Text(stringResource(R.string.settings_version, appVersion(ctx)), style = Ry.type.caption, color = Ry.colors.textMuted,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
    if (confirmSignOut) RyConfirmDialog(
        stringResource(R.string.settings_sign_out), stringResource(R.string.settings_sign_out_body),
        stringResource(R.string.settings_sign_out), stringResource(R.string.common_cancel),
        onConfirm = { confirmSignOut = false; vm.signOut() }, onDismiss = { confirmSignOut = false }, destructive = true,
    )
}

private fun appVersion(ctx: android.content.Context): String =
    runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "—"

@Composable
private fun themeName(t: ThemeMode) = stringResource(
    when (t) { ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.DARK -> R.string.theme_dark },
)

// ---------- Уведомления ----------

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    val n = st.prefs.notifications
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
    val granted = remember(tick) {
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }
    SettingsScaffold(stringResource(R.string.settings_notifications), onBack) {
        if (!granted) item {
            NoticeBanner(stringResource(R.string.notif_permission_off), RyIcons.BellOff, warn = true,
                action = stringResource(R.string.perm_fix) to { if (Build.VERSION.SDK_INT >= 33) ask.launch(Manifest.permission.POST_NOTIFICATIONS) })
        }
        item {
            RyGroup {
                RySwitchRow(stringResource(R.string.notif_sound), n.sound, { v -> vm.notifications { it.copy(sound = v) } }, icon = RyIcons.Volume)
                RyDivider(start = 52.dp)
                RySwitchRow(stringResource(R.string.notif_vibration), n.vibration, { v -> vm.notifications { it.copy(vibration = v) } }, icon = RyIcons.Vibrate)
                RyDivider(start = 52.dp)
                RyListRow(stringResource(R.string.notif_system_settings), icon = RyIcons.Settings, onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                })
            }
        }
        item { SectionTitle(stringResource(R.string.notif_quiet)) }
        item {
            RyGroup {
                RySwitchRow(stringResource(R.string.notif_quiet_on), n.quietFrom != null, { on ->
                    vm.notifications { it.copy(quietFrom = if (on) LocalTime.of(22, 30) else null, quietTo = if (on) LocalTime.of(7, 0) else null) }
                }, subtitle = stringResource(R.string.notif_quiet_hint), icon = RyIcons.Moon)
                if (n.quietFrom != null && n.quietTo != null) {
                    RyDivider(start = 16.dp)
                    RyListRow(stringResource(R.string.notif_quiet_from), subtitle = n.quietFrom!!.hhmm(), onClick = { pickFrom = true })
                    RyDivider(start = 16.dp)
                    RyListRow(stringResource(R.string.notif_quiet_to), subtitle = n.quietTo!!.hhmm(), onClick = { pickTo = true })
                }
            }
        }
        item { SectionTitle(stringResource(R.string.notif_family)) }
        item { Text(stringResource(R.string.notif_family_hint), style = Ry.type.bodySmall, color = Ry.colors.textMuted) }
        item {
            RyGroup {
                listOf(
                    FamilyNudgeScope.CHILDREN_ONLY to R.string.scope_children,
                    FamilyNudgeScope.IMPORTANT_ONLY to R.string.scope_important,
                    FamilyNudgeScope.ALL to R.string.scope_all,
                    FamilyNudgeScope.NONE to R.string.scope_none,
                ).forEachIndexed { i, (scope, label) ->
                    if (i > 0) RyDivider(start = 56.dp)
                    RadioRow(stringResource(label), n.familyScope == scope) { vm.notifications { it.copy(familyScope = scope) } }
                }
            }
        }
        item { SectionTitle(stringResource(R.string.notif_other)) }
        item {
            RyGroup {
                RySwitchRow(stringResource(R.string.notif_restock), n.restockReminders, { v -> vm.notifications { it.copy(restockReminders = v) } }, icon = RyIcons.Bag)
                RyDivider(start = 52.dp)
                RySwitchRow(stringResource(R.string.notif_course_end), n.courseEndReminders, { v -> vm.notifications { it.copy(courseEndReminders = v) } }, icon = RyIcons.Flag)
                RyDivider(start = 52.dp)
                RySwitchRow(stringResource(R.string.notif_day_complete), n.dayCompleteScreen, { v -> vm.notifications { it.copy(dayCompleteScreen = v) } }, icon = RyIcons.CheckCircle)
            }
        }
    }
    if (pickFrom) RyTimePickerDialog(n.quietFrom ?: LocalTime.of(22, 30), { pickFrom = false }) { t -> pickFrom = false; vm.notifications { it.copy(quietFrom = t) } }
    if (pickTo) RyTimePickerDialog(n.quietTo ?: LocalTime.of(7, 0), { pickTo = false }) { t -> pickTo = false; vm.notifications { it.copy(quietTo = t) } }
}

@Composable
private fun RadioRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onClick).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, null, Modifier.padding(12.dp), colors = RadioButtonDefaults.colors(selectedColor = Ry.colors.primaryDark, unselectedColor = Ry.colors.textSecondary))
        Text(title, style = Ry.type.body, color = Ry.colors.text, modifier = Modifier.weight(1f).padding(end = 12.dp))
    }
}

// ---------- Синхронизация ----------

@Composable
fun SyncSettingsScreen(onBack: () -> Unit, onPrivacy: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    SettingsScaffold(stringResource(R.string.settings_sync), onBack) {
        item {
            RyGroup {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SyncIndicator(st.sync)
                    Text(
                        stringResource(
                            when (st.sync) {
                                SyncState.OFFLINE -> R.string.sync_offline_body
                                SyncState.LOCAL_ONLY -> R.string.sync_local_body
                                SyncState.SYNCED -> R.string.sync_synced_body
                                SyncState.SYNCING -> R.string.sync_syncing_body
                                SyncState.ERROR -> R.string.sync_error_body
                            },
                        ),
                        style = Ry.type.body, color = Ry.colors.text,
                    )
                    RyBadge(stringResource(if (st.online) R.string.sync_network_on else R.string.sync_network_off),
                        if (st.online) BadgeKind.Ok else BadgeKind.Warn, icon = if (st.online) RyIcons.CloudCheck else RyIcons.CloudOff)
                }
            }
        }
        item { SectionTitle(stringResource(R.string.sync_how_title)) }
        item { Text(stringResource(R.string.sync_how_body), style = Ry.type.bodySmall, color = Ry.colors.textMuted) }
        item { RyGroup { RyListRow(stringResource(R.string.privacy_export), subtitle = stringResource(R.string.privacy_export_hint), icon = RyIcons.Download, onClick = onPrivacy) } }
    }
}

// ---------- Внешний вид ----------

@Composable
fun AppearanceScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    SettingsScaffold(stringResource(R.string.settings_appearance), onBack) {
        item { SectionTitle(stringResource(R.string.appearance_theme)) }
        item {
            RyGroup {
                ThemeMode.entries.forEachIndexed { i, t ->
                    if (i > 0) RyDivider(start = 56.dp)
                    RadioRow(themeName(t), st.prefs.theme == t) { vm.setTheme(t) }
                }
            }
        }
        item { SectionTitle(stringResource(R.string.appearance_language)) }
        item {
            RyGroup {
                listOf(null to stringResource(R.string.lang_system), "ru" to "Русский", "kk" to "Қазақша", "en" to "English").forEachIndexed { i, (tag, name) ->
                    if (i > 0) RyDivider(start = 56.dp)
                    RadioRow(name, st.prefs.language == tag) { vm.setLanguage(tag) }
                }
            }
        }
        item { Text(stringResource(R.string.appearance_kk_note), style = Ry.type.caption, color = Ry.colors.textMuted) }
    }
}

// ---------- Приватность и данные ----------

@Composable
fun PrivacyScreen(onBack: () -> Unit, snackbar: SnackbarHostState, vm: SettingsViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                val json = vm.exportJson()
                withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } }
            }.isSuccess
            snackbar.showSnackbar(ctx.getString(if (ok) R.string.privacy_export_done else R.string.err_generic))
        }
    }
    LaunchedEffect(Unit) { vm.errorEvents.collect { snackbar.showSnackbar(it.userMessageText(ctx)) } }
    val ready = st.session as? Session.Ready
    SettingsScaffold(stringResource(R.string.settings_privacy), onBack) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.privacy_point_1, R.string.privacy_point_2, R.string.privacy_point_3, R.string.privacy_point_4).forEach {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        app.ryadom.core.designsystem.component.RyIcon(RyIcons.Check, null, tint = Ry.colors.primary, size = 18.dp)
                        Text(stringResource(it), style = Ry.type.bodySmall, color = Ry.colors.text)
                    }
                }
            }
        }
        item {
            RyGroup {
                RyListRow(stringResource(R.string.privacy_export), subtitle = stringResource(R.string.privacy_export_hint), icon = RyIcons.Download,
                    onClick = { exportLauncher.launch("ryadom-${java.time.LocalDate.now()}.json") })
            }
        }
        if (ready != null) item {
            RyGroup {
                RyListRow(stringResource(R.string.privacy_leave_family), subtitle = stringResource(R.string.privacy_leave_family_hint),
                    icon = RyIcons.Logout, trailing = null, onClick = { confirm = "leave" })
                if (ready.me.role == FamilyRole.OWNER) {
                    RyDivider(start = 52.dp)
                    RyListRow(stringResource(R.string.privacy_delete_family), subtitle = stringResource(R.string.privacy_delete_family_hint),
                        icon = RyIcons.Trash, titleColor = Ry.colors.errorText, trailing = null, onClick = { confirm = "family" })
                }
            }
        }
        item {
            RyGroup {
                RyListRow(stringResource(R.string.privacy_delete_account), subtitle = stringResource(R.string.privacy_delete_account_hint),
                    icon = RyIcons.Trash, titleColor = Ry.colors.errorText, trailing = null, onClick = { confirm = "account" })
            }
        }
    }
    when (confirm) {
        "leave" -> RyConfirmDialog(stringResource(R.string.privacy_leave_family), stringResource(R.string.privacy_leave_family_confirm),
            stringResource(R.string.privacy_leave_family), stringResource(R.string.common_cancel),
            onConfirm = { confirm = null; vm.leaveFamily() }, onDismiss = { confirm = null }, destructive = true)
        "family" -> RyConfirmDialog(stringResource(R.string.privacy_delete_family), stringResource(R.string.privacy_delete_family_confirm),
            stringResource(R.string.common_delete), stringResource(R.string.common_cancel),
            onConfirm = { confirm = null; vm.deleteFamily() }, onDismiss = { confirm = null }, destructive = true)
        "account" -> RyConfirmDialog(stringResource(R.string.privacy_delete_account), stringResource(R.string.privacy_delete_account_confirm),
            stringResource(R.string.common_delete), stringResource(R.string.common_cancel),
            onConfirm = { confirm = null; vm.deleteAccount() }, onDismiss = { confirm = null }, destructive = true)
    }
}

private fun Throwable.userMessageText(ctx: android.content.Context): String = ctx.getString(
    when ((this as? app.ryadom.core.model.RyadomException)?.reason) {
        app.ryadom.core.model.RyadomException.Reason.LAST_OWNER -> R.string.err_last_owner
        app.ryadom.core.model.RyadomException.Reason.NOT_ALLOWED -> R.string.err_not_allowed
        else -> R.string.err_generic
    },
)

fun NavGraphBuilder.settingsGraph(nav: SettingsNav, snackbar: SnackbarHostState) {
    composable<SettingsRoute> { SettingsScreen(nav, snackbar) }
    composable<NotificationSettingsRoute> { NotificationSettingsScreen(onBack = nav.back) }
    composable<SyncSettingsRoute> { SyncSettingsScreen(onBack = nav.back, onPrivacy = nav.openPrivacy) }
    composable<AppearanceRoute> { AppearanceScreen(onBack = nav.back) }
    composable<PrivacyRoute> { PrivacyScreen(onBack = nav.back, snackbar = snackbar) }
}
