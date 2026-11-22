package app.ryadom.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.Typography as M3Typography
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class RyadomSpace(
    val xxs: Dp = 4.dp, val xs: Dp = 8.dp, val s: Dp = 12.dp, val m: Dp = 16.dp,
    val l: Dp = 20.dp, val xl: Dp = 24.dp, val xxl: Dp = 32.dp, val xxxl: Dp = 40.dp,
    /** Боковое поле экрана. */
    val gutter: Dp = 16.dp,
    /** Минимальная зона касания. */
    val touch: Dp = 48.dp,
)

/** Три радиуса и «пилюля». Других скруглений в приложении нет. */
@Immutable
data class RyadomShapes(
    val s: RoundedCornerShape = RoundedCornerShape(10.dp),
    val m: RoundedCornerShape = RoundedCornerShape(16.dp),
    val l: RoundedCornerShape = RoundedCornerShape(24.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(percent = 50),
)

val LocalRyadomColors = staticCompositionLocalOf { LightColors }
val LocalRyadomType = staticCompositionLocalOf { Typography }
val LocalRyadomSpace = staticCompositionLocalOf { RyadomSpace() }
val LocalRyadomShapes = staticCompositionLocalOf { RyadomShapes() }

/** Короткий доступ к теме: Ry.colors.primary, Ry.type.title … */
object Ry {
    val colors: RyadomColors @Composable @ReadOnlyComposable get() = LocalRyadomColors.current
    val type: RyadomType @Composable @ReadOnlyComposable get() = LocalRyadomType.current
    val space: RyadomSpace @Composable @ReadOnlyComposable get() = LocalRyadomSpace.current
    val shapes: RyadomShapes @Composable @ReadOnlyComposable get() = LocalRyadomShapes.current
}

@Composable
fun RyadomTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (darkTheme) DarkColors else LightColors
    // Material 3 — только техническая основа: его роли раскрашены нашими токенами,
    // чтобы системные элементы (поля ввода, пикеры, диалоги) не выбивались из стиля.
    val scheme = if (darkTheme) darkColorScheme(
        primary = c.primaryDark, onPrimary = c.onPrimary, primaryContainer = c.primaryContainer, onPrimaryContainer = c.text,
        secondary = c.primary, onSecondary = c.onPrimary, secondaryContainer = c.primaryContainer, onSecondaryContainer = c.text,
        tertiary = c.warm, background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text,
        surfaceVariant = c.surfaceMuted, onSurfaceVariant = c.textMuted, outline = c.border, outlineVariant = c.border,
        error = c.error, onError = c.onPrimary, errorContainer = c.errorContainer, onErrorContainer = c.errorText,
        surfaceContainer = c.surface, surfaceContainerHigh = c.surface, surfaceContainerHighest = c.surfaceMuted,
        surfaceContainerLow = c.surface, surfaceContainerLowest = c.bg, scrim = c.scrim,
    ) else lightColorScheme(
        primary = c.primaryDark, onPrimary = c.onPrimary, primaryContainer = c.primaryContainer, onPrimaryContainer = c.text,
        secondary = c.primary, onSecondary = c.onPrimary, secondaryContainer = c.primaryContainer, onSecondaryContainer = c.text,
        tertiary = c.warm, background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text,
        surfaceVariant = c.surfaceMuted, onSurfaceVariant = c.textMuted, outline = c.border, outlineVariant = c.border,
        error = c.error, onError = c.onPrimary, errorContainer = c.errorContainer, onErrorContainer = c.errorText,
        surfaceContainer = c.surface, surfaceContainerHigh = c.surface, surfaceContainerHighest = c.surfaceMuted,
        surfaceContainerLow = c.surface, surfaceContainerLowest = c.bg, scrim = c.scrim,
    )
    val t = Typography
    val m3Type = M3Typography(
        displaySmall = t.display, headlineMedium = t.headline, headlineSmall = t.headline, titleLarge = t.title,
        titleMedium = t.subtitle, titleSmall = t.label, bodyLarge = t.body, bodyMedium = t.bodySmall,
        bodySmall = t.caption, labelLarge = t.label, labelMedium = t.caption, labelSmall = t.caption,
    )
    val shapes = RyadomShapes()
    MaterialTheme(
        colorScheme = scheme,
        typography = m3Type,
        shapes = Shapes(extraSmall = shapes.s, small = shapes.s, medium = shapes.m, large = shapes.l, extraLarge = shapes.l),
    ) {
        CompositionLocalProvider(
            LocalRyadomColors provides c,
            LocalRyadomType provides t,
            LocalRyadomShapes provides shapes,
            LocalIndication provides ripple(color = c.primaryDark),
            content = content,
        )
    }
}
