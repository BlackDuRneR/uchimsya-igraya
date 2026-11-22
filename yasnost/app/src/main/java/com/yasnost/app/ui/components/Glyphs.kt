package com.yasnost.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yasnost.app.R
import com.yasnost.app.ui.theme.Accent
import com.yasnost.app.ui.theme.Y

/**
 * Иконографика «Ясности»: вместо системных эмодзи — векторные глифы
 * (Material Symbols Rounded + собственные рисунки), поданные в фирменном стиле.
 * Ключом служит прежний эмодзи — так данные в домене не меняются.
 */
object Glyphs {
    /** Собственные векторные рисунки приложения. */
    private val custom = mapOf(
        "🩸" to R.drawable.ic_drop, "💧" to R.drawable.ic_drop, "🍷" to R.drawable.ic_wine,
        "🚬" to R.drawable.ic_cigarette, "🏅" to R.drawable.ic_medal, "✦" to R.drawable.ic_spark, "✨" to R.drawable.ic_spark,
    )

    private val vectors: Map<String, ImageVector> = mapOf(
        // цикл и здоровье
        "🌸" to Icons.Rounded.LocalFlorist, "🌱" to Icons.Rounded.Eco, "🌙" to Icons.Rounded.Bedtime,
        "🌧" to Icons.Rounded.Cloud, "⏳" to Icons.Rounded.HourglassBottom, "🥚" to Icons.Rounded.Adjust,
        "📅" to Icons.Rounded.CalendarMonth, "⚠" to Icons.Rounded.WarningAmber, "↪" to Icons.Rounded.PlayCircle,
        "✅" to Icons.Rounded.CheckCircle, "😖" to Icons.Rounded.Healing, "💊" to Icons.Rounded.Medication,
        "🩺" to Icons.Rounded.MedicalServices, "🙂" to Icons.Rounded.SentimentSatisfied, "⚖" to Icons.Rounded.MonitorWeight,
        "🌡" to Icons.Rounded.Thermostat, "🥛" to Icons.Rounded.LocalDrink, "📝" to Icons.Rounded.EditNote,
        "✍" to Icons.Rounded.EditNote, "💡" to Icons.Rounded.Lightbulb, "🔬" to Icons.Rounded.Science,
        "🔥" to Icons.Rounded.LocalFireDepartment, "🏃" to Icons.AutoMirrored.Rounded.DirectionsRun, "🌿" to Icons.Rounded.Spa,
        // близость и пара
        "❤" to Icons.Rounded.Favorite, "💞" to Icons.Rounded.Favorite, "🤍" to Icons.Rounded.FavoriteBorder,
        "✊" to Icons.Rounded.SportsMma, "🫂" to Icons.Rounded.PeopleAlt, "💬" to Icons.Rounded.Forum,
        "🚫" to Icons.Rounded.Block, "🙏" to Icons.Rounded.VolunteerActivism, "🧭" to Icons.Rounded.Explore,
        "🗣" to Icons.Rounded.RecordVoiceOver, "♂" to Icons.Rounded.Male, "♀" to Icons.Rounded.Female,
        "✉" to Icons.Rounded.MailOutline, "🍺" to Icons.Rounded.SportsBar, "💪" to Icons.Rounded.FitnessCenter,
        // деньги и мечты
        "🎯" to Icons.Rounded.TrackChanges, "🎉" to Icons.Rounded.Celebration, "💰" to Icons.Rounded.Savings,
        "☕" to Icons.Rounded.LocalCafe, "📚" to Icons.AutoMirrored.Rounded.MenuBook, "🎬" to Icons.Rounded.Movie,
        "💆" to Icons.Rounded.Spa, "⛽" to Icons.Rounded.LocalGasStation, "🏋" to Icons.Rounded.FitnessCenter,
        "🛒" to Icons.Rounded.ShoppingCart, "🍽" to Icons.Rounded.Restaurant, "👟" to Icons.AutoMirrored.Rounded.DirectionsRun,
        "🎧" to Icons.Rounded.Headphones, "🏞" to Icons.Rounded.Landscape, "⌚" to Icons.Rounded.Watch,
        "✈" to Icons.Rounded.Flight, "💻" to Icons.Rounded.Laptop, "🏖" to Icons.Rounded.BeachAccess,
        "📱" to Icons.Rounded.Smartphone, "🚗" to Icons.Rounded.DirectionsCar,
        // награды
        "💵" to Icons.Rounded.Payments, "🐷" to Icons.Rounded.Savings, "💎" to Icons.Rounded.Diamond,
        "🏦" to Icons.Rounded.AccountBalance, "👑" to Icons.Rounded.WorkspacePremium, "🚀" to Icons.Rounded.RocketLaunch,
        "🌊" to Icons.Rounded.Waves, "🐦" to Icons.Rounded.Whatshot, "🌬" to Icons.Rounded.Air,
        "🏔" to Icons.Rounded.Terrain, "⚡" to Icons.Rounded.Bolt, "🌗" to Icons.Rounded.Contrast,
        "🌕" to Icons.Rounded.LightMode, "🌌" to Icons.Rounded.NightsStay, "🧘" to Icons.Rounded.SelfImprovement,
        "🛡" to Icons.Rounded.Shield, "🔮" to Icons.Rounded.Insights, "🗓" to Icons.AutoMirrored.Rounded.EventNote,
        "🎁" to Icons.Rounded.Redeem, "🌠" to Icons.Rounded.Stars,
    )

    /** Убираем вариационные селекторы и ZWJ, чтобы ключ совпал. */
    private fun norm(e: String) = e.filter { it != '️' && it != '‍' }.let { s ->
        // составные (🐦‍🔥) — берём первый символ
        val cp = s.codePointAt(0); String(Character.toChars(cp))
    }

    fun vector(e: String): ImageVector? = vectors[norm(e)]
    fun drawable(e: String): Int? = custom[norm(e)]
}

/** Глиф без подложки. */
@Composable
fun Glyph(key: String, tint: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    val res = Glyphs.drawable(key)
    if (res != null) Icon(painterResource(res), null, tint = tint, modifier = modifier.size(size))
    else Icon(Glyphs.vector(key) ?: Icons.Rounded.AutoAwesome, null, tint = tint, modifier = modifier.size(size))
}

/** Глиф на градиентной плитке — основной «дорогой» вид иконок. */
@Composable
fun GlyphTile(key: String, accent: Accent, size: Dp = 40.dp, dim: Boolean = false, round: Boolean = false, modifier: Modifier = Modifier) {
    val shape = if (round) CircleShape else RoundedCornerShape(size * 0.32f)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .then(
                if (dim) Modifier.background(Color(0x12FFFFFF)).border(1.dp, Y.Stroke, shape)
                else Modifier.background(accent.brush).background(Brush.radialGradient(listOf(Color(0x55FFFFFF), Color.Transparent), radius = size.value * 1.6f))
            ),
        contentAlignment = Alignment.Center,
    ) {
        Glyph(key, if (dim) Y.Text3 else Color(0xFF0B0C14), size * 0.54f)
    }
}

/** Настроение 1..5: «лицо» в цвете от кораллового до мятного. */
val MOOD_COLORS = listOf(Y.Coral, Y.Amber, Y.Sky, Y.Teal, Y.Mint)
private val MOOD_ICONS = listOf(
    Icons.Rounded.SentimentVeryDissatisfied, Icons.Rounded.SentimentDissatisfied, Icons.Rounded.SentimentNeutral,
    Icons.Rounded.SentimentSatisfied, Icons.Rounded.SentimentVerySatisfied,
)

@Composable
fun MoodIcon(mood: Int, size: Dp = 26.dp, modifier: Modifier = Modifier) {
    val i = (mood - 1).coerceIn(0, 4)
    Icon(MOOD_ICONS[i], null, tint = MOOD_COLORS[i], modifier = modifier.size(size))
}

/** Кружок-выбор настроения для диалогов. */
@Composable
fun MoodChoice(mood: Int, selected: Boolean, size: Dp = 46.dp, onClick: () -> Unit) {
    val i = (mood - 1).coerceIn(0, 4)
    val c = MOOD_COLORS[i]
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (selected) c.copy(alpha = 0.22f) else Color(0x0FFFFFFF))
            .border(if (selected) 1.5.dp else 0.dp, if (selected) c else Color.Transparent, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(MOOD_ICONS[i], null, tint = if (selected) c else Y.Text3, modifier = Modifier.size(size * 0.62f)) }
}

