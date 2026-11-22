package app.ryadom.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Цветовые токены «Рядом». Значения совпадают с design/tokens/tokens.json.
 * Компоненты берут цвет только отсюда, по роли, а не по значению.
 */
@Immutable
data class RyadomColors(
    val bg: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val primary: Color,
    val primaryDark: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val accent: Color,
    val warm: Color,
    val warmContainer: Color,
    val text: Color,
    /** #818780 из ТЗ: иконки и крупный текст. */
    val textSecondary: Color,
    /** Подписи до 18 sp: контраст 4,9:1. */
    val textMuted: Color,
    val border: Color,
    val success: Color,
    val warning: Color,
    val warningContainer: Color,
    val warningText: Color,
    val error: Color,
    val errorContainer: Color,
    val errorText: Color,
    val onPrimary: Color,
    val scrim: Color,
    /** Пары «тон / подложка» для цветовых акцентов людей. */
    val people: List<AccentPair>,
    val isDark: Boolean,
) {
    fun person(index: Int): AccentPair = people[index.mod(people.size)]
}

@Immutable
data class AccentPair(val tone: Color, val container: Color)

val LightColors = RyadomColors(
    bg = Color(0xFFF6F5F0),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFEFEEE7),
    primary = Color(0xFF829987),
    primaryDark = Color(0xFF536C5B),
    primaryContainer = Color(0xFFE4EBE3),
    secondary = Color(0xFFD9CBB8),
    accent = Color(0xFFB7C8B8),
    warm = Color(0xFFD8B9A8),
    warmContainer = Color(0xFFF4EAE3),
    text = Color(0xFF2E3531),
    textSecondary = Color(0xFF818780),
    textMuted = Color(0xFF666C65),
    border = Color(0xFFE7E8E2),
    success = Color(0xFF7E9B83),
    warning = Color(0xFFD2B17D),
    warningContainer = Color(0xFFF6EEDF),
    warningText = Color(0xFF7A5C25),
    error = Color(0xFFC8877C),
    errorContainer = Color(0xFFF5E6E2),
    errorText = Color(0xFF8D544B),
    onPrimary = Color(0xFFFFFFFF),
    scrim = Color(0x662E3531),
    people = listOf(
        AccentPair(Color(0xFF9A6B57), Color(0xFFF1E2D9)), // глина
        AccentPair(Color(0xFF4F6B57), Color(0xFFDDE7DE)), // мох
        AccentPair(Color(0xFF85683A), Color(0xFFF1E7D3)), // песок
        AccentPair(Color(0xFF4D6B5E), Color(0xFFDCE8E2)), // шалфей
        AccentPair(Color(0xFF95596A), Color(0xFFF2E1E5)), // роза
        AccentPair(Color(0xFF4F6170), Color(0xFFDFE5EA)), // сланец
    ),
    isDark = false,
)

val DarkColors = RyadomColors(
    bg = Color(0xFF141816),
    surface = Color(0xFF1C211E),
    surfaceMuted = Color(0xFF232925),
    primary = Color(0xFF9DB5A2),
    primaryDark = Color(0xFFB4CAB8),
    primaryContainer = Color(0xFF2A372E),
    secondary = Color(0xFF6E6455),
    accent = Color(0xFF3E5044),
    warm = Color(0xFFA88878),
    warmContainer = Color(0xFF33291F),
    text = Color(0xFFECEAE3),
    textSecondary = Color(0xFFA2A79F),
    textMuted = Color(0xFFB9BDB4),
    border = Color(0xFF2C332F),
    success = Color(0xFF93B098),
    warning = Color(0xFFDDBF8E),
    warningContainer = Color(0xFF352E22),
    warningText = Color(0xFFE5C995),
    error = Color(0xFFDB9C91),
    errorContainer = Color(0xFF3A2925),
    errorText = Color(0xFFE8B2A8),
    onPrimary = Color(0xFF16201A),
    scrim = Color(0x99000000),
    people = listOf(
        AccentPair(Color(0xFFE2BBA8), Color(0xFF43332B)),
        AccentPair(Color(0xFFAFC9B4), Color(0xFF2B3A2F)),
        AccentPair(Color(0xFFE0C896), Color(0xFF3F3524)),
        AccentPair(Color(0xFFA9C8BA), Color(0xFF283A32)),
        AccentPair(Color(0xFFE2B4C0), Color(0xFF412D33)),
        AccentPair(Color(0xFFB3C3D0), Color(0xFF2B333A)),
    ),
    isDark = true,
)
