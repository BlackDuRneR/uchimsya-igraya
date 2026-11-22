package com.yasnost.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.yasnost.app.R
import com.yasnost.app.data.HabitType

/** Дизайн-код «Ясность»: глубокая ночь + северное сияние. */
object Y {
    val Bg = Color(0xFF06070C)
    val Surface = Color(0xFF10121C)
    val Surface2 = Color(0xFF171A28)
    val Stroke = Color(0x1FFFFFFF)
    val Text = Color(0xFFF2F4FF)
    val Text2 = Color(0xFF9AA0BC)
    val Text3 = Color(0xFF5E6380)

    val Violet = Color(0xFF8B7CFF)
    val Magenta = Color(0xFFE07CFF)
    val Teal = Color(0xFF2DE3C5)
    val Mint = Color(0xFF8CF5A8)
    val Amber = Color(0xFFFFB547)
    val Coral = Color(0xFFFF6B6B)
    val Sky = Color(0xFF5CB8FF)
    val Danger = Color(0xFFFF5C7A)
}

data class Accent(val a: Color, val b: Color) {
    val brush get() = Brush.linearGradient(listOf(a, b))
    val soft get() = a.copy(alpha = 0.14f)
}

fun HabitType.accent(): Accent = when (this) {
    HabitType.ALCOHOL -> Accent(Y.Violet, Y.Magenta)
    HabitType.SMOKING -> Accent(Y.Teal, Y.Mint)
    HabitType.CUSTOM -> Accent(Y.Amber, Y.Coral)
}

fun HabitType.icon(): Int = when (this) {
    HabitType.ALCOHOL -> R.drawable.ic_wine
    HabitType.SMOKING -> R.drawable.ic_cigarette
    HabitType.CUSTOM -> R.drawable.ic_spark
}

object YType {
    val Hero = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp, fontFeatureSettings = "tnum")
    val Big = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, fontFeatureSettings = "tnum")
    val Title = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
    val Sub = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val Body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 21.sp)
    val Label = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
    val Mono = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum", letterSpacing = 1.sp)
}

@Composable
fun YasnostTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Y.Violet,
            onPrimary = Color.White,
            secondary = Y.Teal,
            background = Y.Bg,
            surface = Y.Surface,
            surfaceContainerHigh = Y.Surface2,
            surfaceContainer = Y.Surface,
            onSurface = Y.Text,
            onSurfaceVariant = Y.Text2,
            outline = Y.Stroke,
            error = Y.Danger,
        ),
        content = content,
    )
}
