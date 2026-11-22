package app.ryadom.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.os.ConfigurationCompat
import app.ryadom.core.designsystem.theme.AccentPair
import app.ryadom.core.designsystem.theme.Ry
import app.ryadom.core.designsystem.theme.RyIcons
import app.ryadom.core.l10n.R
import app.ryadom.core.model.DayPart
import app.ryadom.core.model.DaysRule
import app.ryadom.core.model.Dosage
import app.ryadom.core.model.DoseUnit
import app.ryadom.core.model.FamilyRole
import app.ryadom.core.model.MedicationCategory
import app.ryadom.core.model.Person
import app.ryadom.core.model.PersonAccent
import app.ryadom.core.model.QuickMessage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
fun LocalTime.hhmm(): String = format(HHMM)

/** «Суббота, 22 ноября» */
@Composable
fun LocalDate.longDate(): String =
    format(DateTimeFormatter.ofPattern("EEEE, d MMMM", currentLocale())).replaceFirstChar { it.titlecase(currentLocale()) }

/** «22 ноября» */
@Composable
fun LocalDate.dayMonth(): String = format(DateTimeFormatter.ofPattern("d MMMM", currentLocale()))

/** «22 ноя» */
@Composable
fun LocalDate.dayMonthShort(): String = format(DateTimeFormatter.ofPattern("d MMM", currentLocale())).trimEnd('.')

@Composable
fun DayOfWeek.short(): String = getDisplayName(TextStyle.SHORT_STANDALONE, currentLocale()).trimEnd('.').lowercase(currentLocale())

@Composable
fun LocalDate.relativeTitle(today: LocalDate): String = when (this) {
    today -> stringResource(R.string.date_today)
    today.minusDays(1) -> stringResource(R.string.date_yesterday)
    today.plusDays(1) -> stringResource(R.string.date_tomorrow)
    else -> longDate()
}

fun Double.clean(): String = if (this % 1.0 == 0.0) toInt().toString() else ((this * 10).roundToInt() / 10.0).toString()

/** «2 капли», «1 таблетка», «5 мл». Дозировку вводит человек — приложение её только показывает. */
@Composable
fun Dosage.text(): String {
    val n = amount
    val q = if (n % 1.0 == 0.0) n.toInt() else 2 // дробные — форма «other»
    return when (unit) {
        DoseUnit.TABLET -> pluralStringResource(R.plurals.unit_tablet, q, n.clean())
        DoseUnit.CAPSULE -> pluralStringResource(R.plurals.unit_capsule, q, n.clean())
        DoseUnit.DROP -> pluralStringResource(R.plurals.unit_drop, q, n.clean())
        DoseUnit.SACHET -> pluralStringResource(R.plurals.unit_sachet, q, n.clean())
        DoseUnit.ML -> stringResource(R.string.unit_ml_amount, n.clean())
        DoseUnit.OTHER -> "${n.clean()} ${customUnit.orEmpty()}".trim()
    }
}

@Composable
fun DoseUnit.label(): String = stringResource(
    when (this) {
        DoseUnit.TABLET -> R.string.unit_tablet
        DoseUnit.CAPSULE -> R.string.unit_capsule
        DoseUnit.ML -> R.string.unit_ml
        DoseUnit.DROP -> R.string.unit_drop
        DoseUnit.SACHET -> R.string.unit_sachet
        DoseUnit.OTHER -> R.string.unit_other
    },
)

@get:DrawableRes
val DoseUnit.icon: Int
    get() = when (this) {
        DoseUnit.TABLET -> RyIcons.Tablet
        DoseUnit.CAPSULE -> RyIcons.Capsule
        DoseUnit.ML -> RyIcons.Measure
        DoseUnit.DROP -> RyIcons.Drop
        DoseUnit.SACHET -> RyIcons.Sachet
        DoseUnit.OTHER -> RyIcons.Jar
    }

/** Остаток в единицах: «36 капель», «5 капсул». */
@Composable
fun unitCount(unit: DoseUnit, amount: Double, custom: String? = null): String = Dosage(amount, unit, custom).text()

@Composable
fun MedicationCategory.label(): String = stringResource(
    when (this) {
        MedicationCategory.VITAMIN -> R.string.cat_vitamin
        MedicationCategory.MINERAL -> R.string.cat_mineral
        MedicationCategory.MEDICINE -> R.string.cat_medicine
        MedicationCategory.SUPPLEMENT -> R.string.cat_supplement
        MedicationCategory.OTHER -> R.string.cat_other
    },
)

@get:DrawableRes
val MedicationCategory.icon: Int
    get() = when (this) {
        MedicationCategory.VITAMIN -> RyIcons.Capsule
        MedicationCategory.MINERAL -> RyIcons.Tablet
        MedicationCategory.MEDICINE -> RyIcons.FirstAid
        MedicationCategory.SUPPLEMENT -> RyIcons.Jar
        MedicationCategory.OTHER -> RyIcons.Leaf
    }

@Composable
fun DayPart.label(): String = stringResource(
    when (this) {
        DayPart.MORNING -> R.string.daypart_morning
        DayPart.DAY -> R.string.daypart_day
        DayPart.EVENING -> R.string.daypart_evening
    },
)

@get:DrawableRes
val DayPart.icon: Int
    get() = when (this) {
        DayPart.MORNING -> RyIcons.Sunrise
        DayPart.DAY -> RyIcons.Sun
        DayPart.EVENING -> RyIcons.Moon
    }

@Composable
fun FamilyRole.label(): String = stringResource(
    when (this) {
        FamilyRole.OWNER -> R.string.role_owner
        FamilyRole.ADMIN -> R.string.role_admin
        FamilyRole.MEMBER -> R.string.role_member
        FamilyRole.CHILD_PROFILE -> R.string.role_child
    },
)

@Composable
fun QuickMessage.text(): String = stringResource(
    when (this) {
        QuickMessage.GAVE_IT -> R.string.quick_gave_it
        QuickMessage.ALREADY_TAKEN -> R.string.quick_already_taken
        QuickMessage.PLEASE_REMIND -> R.string.quick_please_remind
        QuickMessage.DONT_FORGET_EVENING -> R.string.quick_dont_forget_evening
        QuickMessage.BUY_PACKAGE -> R.string.quick_buy_package
        QuickMessage.MY_TURN_TODAY -> R.string.quick_my_turn
        QuickMessage.THANKS -> R.string.quick_thanks
    },
)

@Composable
fun DaysRule.text(): String = when (this) {
    DaysRule.Daily -> stringResource(R.string.days_daily)
    is DaysRule.Weekdays -> days.sorted().map { it.short() }.joinToString(", ")
    is DaysRule.Interval -> pluralStringResource(R.plurals.days_every_n, everyDays, everyDays)
}

/** Возраст ребёнка: «4 года». */
@Composable
fun ageText(birth: LocalDate, today: LocalDate): String {
    val years = java.time.Period.between(birth, today).years
    return if (years >= 1) pluralStringResource(R.plurals.age_years, years, years)
    else {
        val months = java.time.Period.between(birth, today).toTotalMonths().toInt().coerceAtLeast(0)
        pluralStringResource(R.plurals.age_months, months, months)
    }
}

@Composable
@ReadOnlyComposable
fun PersonAccent.colors(): AccentPair = Ry.colors.person(ordinal)

@Composable
@ReadOnlyComposable
fun Person.colors(): AccentPair = accent.colors()

@Composable
fun greeting(hour: Int): String = stringResource(
    when (hour) {
        in 5..11 -> R.string.home_greeting_morning
        in 12..17 -> R.string.home_greeting_day
        in 18..22 -> R.string.home_greeting_evening
        else -> R.string.home_greeting_night
    },
)
