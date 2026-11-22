"""Логотип «Рядом».

Основной знак «Ладони»: две полудуги бережно обнимают точку.
Полудуги — двое взрослых (два цвета: шалфей и тёплая глина),
точка в центре — тот, о ком заботятся: ребёнок, близкий, «общее важное».
Дуги чуть заходят друг за друга — люди держатся вместе, а не просто стоят рядом.

Знак читается при 24 px и в монохроме (иконка уведомления — один цвет).
Альтернативы (на выбор заказчика) — alt_roof() и alt_hands().
"""

SW = 8.5  # толщина линии знака в сетке 100×100

LEFT = "M57.93 21.19A32 32 0 1 0 57.93 78.81"
RIGHT = "M42.07 78.81A32 32 0 1 0 42.07 21.19"
DOT = (50, 50, 8)


def mark_inner(left, right, dot, gap, sw=SW):
    """Знак без обёртки <svg>. gap — цвет фона для прорези в месте нахлёста (None — без прорези)."""
    gap_path = f'<path d="{LEFT}" stroke="{gap}" stroke-width="{sw + 6}"/>' if gap else ""
    return (f'<g fill="none" stroke-linecap="round" stroke-linejoin="round">'
            f'<path d="{RIGHT}" stroke="{right}" stroke-width="{sw}"/>{gap_path}'
            f'<path d="{LEFT}" stroke="{left}" stroke-width="{sw}"/>'
            f'<circle cx="{DOT[0]}" cy="{DOT[1]}" r="{DOT[2]}" fill="{dot}" stroke="none"/></g>')


def mark(left="#536C5B", right="#C29A86", dot="#829987", gap="#F6F5F0", size=None, mono=None):
    if mono:
        left = right = dot = mono
    s = f' width="{size}" height="{size}"' if size else ""
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"{s}>'
            f'{mark_inner(left, right, dot, gap)}</svg>')


# --- альтернативы для выбора на этапе концепции ---

def alt_roof(left="#536C5B", right="#C29A86", dot="#829987"):
    """«Под одной крышей»: две половины арки сходятся наверху, внутри — точка."""
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" fill="none" '
            f'stroke-linecap="round" stroke-width="{SW}">'
            f'<path d="M22 82V54C22 36 34 24 50 22" stroke="{left}"/>'
            f'<path d="M78 82V54C78 36 66 24 50 22" stroke="{right}"/>'
            f'<circle cx="50" cy="62" r="8" fill="{dot}" stroke="none"/></svg>')


def alt_hands(left="#536C5B", right="#C29A86", dot="#829987"):
    """«Рука об руку»: две фигуры-арки, соединённые дугой."""
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" fill="none" '
            f'stroke-linecap="round" stroke-width="{SW}">'
            f'<path d="M16 86V70A14 14 0 0 1 44 70V86" stroke="{left}"/>'
            f'<path d="M56 86V70A14 14 0 0 1 84 70V86" stroke="{right}"/>'
            f'<circle cx="30" cy="40" r="8" fill="{left}" stroke="none"/>'
            f'<circle cx="70" cy="40" r="8" fill="{right}" stroke="none"/>'
            f'<path d="M44 74C46 60 54 60 56 74" stroke="{dot}" stroke-width="6"/></svg>')


# --- иконка приложения ---
# Из присланного эскиза взята идея тёмного фона и тёплого «металла»,
# но в палитре бренда и без медицинских символов.
ICON_BG = "#3E5245"
ICON_LEFT = "#F1ECE1"
ICON_RIGHT = "#D9B9A5"
ICON_DOT = "#B7C8B8"


def app_icon(size=512, rounded=True):
    r = size * 0.225 if rounded else 0
    k = size / 108  # adaptive icon: холст 108dp, безопасная зона 66dp
    scale = 58 / 100 * k
    t = (size - 100 * scale) / 2
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {size} {size}">'
            f'<rect width="{size}" height="{size}" rx="{r:.1f}" fill="{ICON_BG}"/>'
            f'<g transform="translate({t:.2f} {t:.2f}) scale({scale:.4f})">'
            f'{mark_inner(ICON_LEFT, ICON_RIGHT, ICON_DOT, ICON_BG)}</g></svg>')


def _layer(inner):
    scale = 0.58
    t = (108 - 100 * scale) / 2
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">'
            f'<g transform="translate({t} {t}) scale({scale})">{inner}</g></svg>')


def adaptive_foreground():
    """Передний слой adaptive icon 108×108: знак в безопасной зоне 66dp."""
    return _layer(mark_inner(ICON_LEFT, ICON_RIGHT, ICON_DOT, ICON_BG))


def adaptive_background():
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">'
            f'<rect width="108" height="108" fill="{ICON_BG}"/></svg>')


def adaptive_monochrome():
    """Android 13+ themed icon: один цвет, система сама перекрашивает."""
    return _layer(mark_inner("#000000", "#000000", "#000000", None))


def notification_icon():
    """Иконка уведомления 24×24: белый силуэт, линия чуть толще для мелкого размера."""
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="24" height="24">'
            f'{mark_inner("#FFFFFF", "#FFFFFF", "#FFFFFF", None, sw=11)}</svg>')
