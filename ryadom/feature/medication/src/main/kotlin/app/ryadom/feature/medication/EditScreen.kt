package app.ryadom.feature.medication

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ryadom.core.designsystem.component.ButtonStyle
import app.ryadom.core.designsystem.component.NoticeBanner
import app.ryadom.core.designsystem.component.RyButton
import app.ryadom.core.designsystem.component.RyChip
import app.ryadom.core.designsystem.component.RyDivider
import app.ryadom.core.designsystem.component.RyGroup
import app.ryadom.core.designsystem.component.RyIcon
import app.ryadom.core.designsystem.component.RyIconButton
import app.ryadom.core.designsystem.component.RyListRow
import app.ryadom.core.designsystem.component.RySegmented
import app.ryadom.core.designsystem.component.RyStepper
import app.ryadom.core.designsystem.component.RySwitchRow
import app.ryadom.core.designsystem.component.RyTextField
import app.ryadom.core.designsystem.component.RyTopBar
import app.ryadom.core.designsystem.component.SkeletonList
import app.ryadom.core.designsystem.theme.Ry
import app.ryadom.core.designsystem.theme.RyIcons
import app.ryadom.core.l10n.R
import app.ryadom.core.model.DoseUnit
import app.ryadom.core.model.MedicationCategory
import app.ryadom.core.ui.PersonAvatar
import app.ryadom.core.ui.RyTimePickerDialog
import app.ryadom.core.ui.RyDatePicker
import app.ryadom.core.ui.clean
import app.ryadom.core.ui.dayMonth
import app.ryadom.core.ui.hhmm
import app.ryadom.core.ui.icon
import app.ryadom.core.ui.label
import app.ryadom.core.ui.short
import app.ryadom.core.ui.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
fun MedicationEditScreen(onClose: () -> Unit, vm: MedicationEditViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(st.saved) { if (st.saved) onClose() }
    Column(Modifier.fillMaxSize().background(Ry.colors.bg).imePadding()) {
        RyTopBar(
            stringResource(if (st.draft.isNew) R.string.med_new_title else R.string.med_edit_title),
            navIcon = RyIcons.Close, navLabel = stringResource(R.string.common_close), onNav = onClose,
            actions = { RyButton(stringResource(R.string.common_done), vm::save, style = ButtonStyle.Text, enabled = !st.loading) },
        )
        if (st.loading) {
            Box(Modifier.padding(16.dp)) { SkeletonList() }
            return@Column
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            EditForm(st, vm)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditForm(st: EditState, vm: MedicationEditViewModel) {
    val d = st.draft
    val family = st.family ?: return
    var pickTime by remember { mutableStateOf(false) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.edit { it.copy(photoUri = uri.toString()) }
        }
    }
    Column(
        Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Основное
        RyTextField(
            d.name, { v -> vm.edit { it.copy(name = v) } }, stringResource(R.string.med_field_name),
            placeholder = stringResource(R.string.med_field_name_hint),
            error = if (st.nameError) stringResource(R.string.err_empty_name) else null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            trailing = {
                RyIconButton(RyIcons.Camera, stringResource(R.string.med_photo), {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, tint = Ry.colors.textMuted)
            },
        )
        d.photoUri?.let { uri ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UriImage(uri, Modifier.size(72.dp).clip(Ry.shapes.m))
                RyButton(stringResource(R.string.med_photo_remove), { vm.edit { it.copy(photoUri = null) } }, style = ButtonStyle.Text, small = true)
            }
        }
        Field(stringResource(R.string.med_field_category)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MedicationCategory.entries.forEach { c ->
                    RyChip(c.label(), d.category == c, { vm.edit { it.copy(category = c) } }, icon = if (d.category == c) c.icon else null)
                }
            }
        }
        Field(stringResource(R.string.med_field_for_whom)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                family.members.forEach { p ->
                    RyChip(p.name, d.personId == p.id, { vm.edit { it.copy(personId = p.id) } }, leading = { PersonAvatar(p, size = 26.dp) })
                }
            }
        }

        // Дозировка
        Field(stringResource(R.string.med_field_dosage)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RyStepper(
                    d.amount, { v -> vm.edit { it.copy(amount = v) } },
                    stringResource(R.string.a11y_decrease), stringResource(R.string.a11y_increase),
                    step = if (d.unit == DoseUnit.ML) 0.5 else 1.0, min = if (d.unit == DoseUnit.ML) 0.5 else 1.0,
                    format = { it.clean() },
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DoseUnit.entries.forEach { u -> RyChip(u.label(), d.unit == u, { vm.edit { it.copy(unit = u) } }, icon = u.icon) }
            }
            if (d.unit == DoseUnit.OTHER) {
                RyTextField(d.customUnit, { v -> vm.edit { it.copy(customUnit = v.take(16)) } }, stringResource(R.string.med_field_custom_unit))
            }
            Hint(stringResource(R.string.med_dose_disclaimer))
        }

        // Расписание
        Field(stringResource(R.string.med_field_schedule)) {
            RySegmented(
                listOf(stringResource(R.string.days_daily), stringResource(R.string.days_weekdays), stringResource(R.string.days_interval)),
                d.daysMode.ordinal, { i -> vm.edit { it.copy(daysMode = DaysMode.entries[i]) } },
            )
            when (d.daysMode) {
                DaysMode.WEEKDAYS -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        RyChip(day.short(), day in d.weekdays, {
                            vm.edit { it.copy(weekdays = if (day in it.weekdays) it.weekdays - day else it.weekdays + day) }
                        })
                    }
                }
                DaysMode.INTERVAL -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.med_every), style = Ry.type.body, color = Ry.colors.text)
                    RyStepper(d.intervalDays.toDouble(), { v -> vm.edit { it.copy(intervalDays = v.toInt()) } },
                        stringResource(R.string.a11y_decrease), stringResource(R.string.a11y_increase), min = 2.0)
                    Text(stringResource(R.string.med_days_suffix), style = Ry.type.body, color = Ry.colors.text)
                }
                DaysMode.DAILY -> Unit
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                d.times.forEach { t ->
                    RyChip(t.hhmm(), true, { vm.removeTime(t) }, icon = RyIcons.Close)
                }
                RyChip(stringResource(R.string.med_add_time), false, { pickTime = true }, icon = RyIcons.Plus)
            }
            if (st.timesError) Text(stringResource(R.string.med_err_no_times), style = Ry.type.caption, color = Ry.colors.errorText)
        }

        // Курс
        Field(stringResource(R.string.med_field_course)) {
            RyGroup {
                RyListRow(stringResource(R.string.med_course_start), subtitle = d.start.dayMonth(), icon = RyIcons.Calendar, onClick = { pickStart = true })
            }
            RySegmented(
                listOf(stringResource(R.string.med_course_open), stringResource(R.string.med_course_until), stringResource(R.string.med_course_days)),
                d.endMode.ordinal, { i -> vm.edit { it.copy(endMode = EndMode.entries[i]) } },
            )
            when (d.endMode) {
                EndMode.DATE -> RyGroup { RyListRow(stringResource(R.string.med_course_end), subtitle = d.endDate.dayMonth(), icon = RyIcons.Flag, onClick = { pickEnd = true }) }
                EndMode.DAYS -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RyStepper(d.durationDays.toDouble(), { v -> vm.edit { it.copy(durationDays = v.toInt()) } },
                        stringResource(R.string.a11y_decrease), stringResource(R.string.a11y_increase), step = 1.0, min = 1.0)
                    Text(stringResource(R.string.med_days_suffix), style = Ry.type.body, color = Ry.colors.text)
                }
                EndMode.NONE -> Unit
            }
            if (d.endMode != EndMode.NONE) {
                RyGroup { RySwitchRow(stringResource(R.string.med_course_notify), d.notifyOnEnd, { v -> vm.edit { it.copy(notifyOnEnd = v) } }) }
            }
        }

        // Кто следит
        if (family.adults.size > 1) {
            Field(stringResource(R.string.med_field_care)) {
                val partner = family.adults.firstOrNull { it.id != st.me?.id }
                RySegmented(
                    listOf(stringResource(R.string.care_me), partner?.name ?: stringResource(R.string.care_partner),
                        stringResource(R.string.care_both), stringResource(R.string.care_any)),
                    d.care.ordinal, { i -> vm.edit { it.copy(care = CareMode.entries[i]) } },
                )
            }
        }

        // Напоминания
        Field(stringResource(R.string.med_field_reminders)) {
            RyGroup {
                RySwitchRow(stringResource(R.string.med_reminders_on), d.remindersOn, { v -> vm.edit { it.copy(remindersOn = v) } }, icon = RyIcons.Bell)
                if (d.remindersOn) {
                    RyDivider(start = 52.dp)
                    OptionRow(stringResource(R.string.med_remind_advance), listOf(0, 5, 10, 15), d.advanceMinutes,
                        { if (it == 0) stringResource(R.string.med_remind_on_time) else stringResource(R.string.minutes_short, it) }) { v -> vm.edit { it.copy(advanceMinutes = v) } }
                    RyDivider(start = 16.dp)
                    OptionRow(stringResource(R.string.med_remind_repeat), listOf(null, 10, 15, 30), d.repeatAfter,
                        { if (it == null) stringResource(R.string.common_no) else stringResource(R.string.minutes_short, it) }) { v -> vm.edit { it.copy(repeatAfter = v) } }
                    if (family.adults.size > 1) {
                        RyDivider(start = 16.dp)
                        OptionRow(stringResource(R.string.med_remind_family), listOf(null, 15, 30, 60), d.nudgeAfter,
                            { if (it == null) stringResource(R.string.common_no) else stringResource(R.string.minutes_short, it) }) { v -> vm.edit { it.copy(nudgeAfter = v) } }
                    }
                    RyDivider(start = 16.dp)
                    RySwitchRow(stringResource(R.string.med_important), d.important, { v -> vm.edit { it.copy(important = v) } },
                        subtitle = stringResource(R.string.med_important_hint))
                }
            }
        }

        // Остаток
        Field(stringResource(R.string.med_field_stock)) {
            RyGroup { RySwitchRow(stringResource(R.string.med_stock_track), d.trackStock, { v -> vm.edit { it.copy(trackStock = v) } }, icon = RyIcons.Box) }
            if (d.trackStock) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RyTextField(d.remaining, { v -> vm.edit { it.copy(remaining = v.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(7)) } },
                        stringResource(R.string.med_stock_now), Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    RyTextField(d.packageSize, { v -> vm.edit { it.copy(packageSize = v.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(7)) } },
                        stringResource(R.string.med_stock_package), Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                Hint(stringResource(R.string.med_stock_units_hint, d.unit.label()))
            }
        }

        // Заметки
        RyTextField(d.notes, { v -> vm.edit { it.copy(notes = v.take(500)) } }, stringResource(R.string.med_field_notes),
            placeholder = stringResource(R.string.med_field_notes_hint), singleLine = false, minLines = 2)

        st.error?.let { NoticeBanner(it.userMessage(), RyIcons.Info, warn = true) }
        RyButton(stringResource(R.string.common_save), vm::save, Modifier.fillMaxWidth().padding(bottom = 24.dp))
    }

    if (pickTime) RyTimePickerDialog(d.times.lastOrNull()?.plusHours(4) ?: LocalTime.of(9, 0), { pickTime = false }) { pickTime = false; vm.addTime(it) }
    if (pickStart) RyDatePicker(d.start, { pickStart = false }) { v -> pickStart = false; vm.edit { it.copy(start = v) } }
    if (pickEnd) RyDatePicker(d.endDate, { pickEnd = false }) { v -> pickEnd = false; vm.edit { it.copy(endDate = v) } }
}

@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = Ry.type.caption, color = Ry.colors.textMuted)
        content()
    }
}

@Composable
private fun Hint(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RyIcon(RyIcons.Info, null, tint = Ry.colors.textMuted, size = 18.dp)
        Text(text, style = Ry.type.caption, color = Ry.colors.textMuted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> OptionRow(title: String, options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = Ry.type.body, color = Ry.colors.text)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { o -> RyChip(label(o), o == selected, { onSelect(o) }) }
        }
    }
}

/** Миниатюра фото упаковки. Декодирование — в фоне и с уменьшением, чтобы не тратить память. */
@Composable
fun UriImage(uri: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val u = Uri.parse(uri)
                val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.contentResolver.openInputStream(u)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
                var sample = 1
                while (opts.outWidth / sample > 512 || opts.outHeight / sample > 512) sample *= 2
                ctx.contentResolver.openInputStream(u)?.use {
                    android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
                }?.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(modifier.background(Ry.colors.surfaceMuted), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else RyIcon(RyIcons.Camera, null, tint = Ry.colors.textSecondary)
    }
}
