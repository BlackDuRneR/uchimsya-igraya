#!/usr/bin/env python3
"""Сборка дизайн-ассетов «Рядом».

    python3 design/tools/build.py

Выход:
  design/assets/icons/*.svg                      — 64 иконки (stroke #2E3531)
  design/assets/illustrations/{light,dark}/*.svg — 20 иллюстраций × 2 темы
  design/assets/logo/*.svg                        — знак, иконка приложения, слои adaptive icon
  design/tokens/tokens.json                       — дизайн-токены
  design/concept.html                             — страница концепции (из concept.src.html)
"""
import json
import pathlib
import re
import sys

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import icons  # noqa: E402
import illustrations as il  # noqa: E402
import logo  # noqa: E402
from palette import ILLO_DARK, ILLO_LIGHT, UI_DARK, UI_LIGHT  # noqa: E402

DESIGN = HERE.parent
ASSETS = DESIGN / "assets"

CLASS_ATTRS = {
    "l": lambda p: f'fill="none" stroke="{p["ink"]}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"',
    "lt": lambda p: f'fill="none" stroke="{p["ink"]}" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"',
    "ld": lambda p: f'fill="none" stroke="{p["ink"]}" stroke-width="1.75" stroke-linecap="round" stroke-dasharray="0.1 6"',
}


def resolve(fragment, pal):
    """Классы → атрибуты (для файлов и импорта в Android Studio)."""
    def sub(m):
        cls = m.group(1)
        if cls in CLASS_ATTRS:
            return CLASS_ATTRS[cls](pal)
        if cls.startswith("f-"):
            return f'fill="{pal[cls[2:]]}"'
        if cls.startswith("s-"):
            return f'fill="none" stroke="{pal[cls[2:]]}" stroke-linecap="round"'
        raise ValueError(f"неизвестный класс {cls}")
    return re.sub(r'class="([\w-]+)"', sub, fragment)


def illo_svg(fragment, pal):
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 180" width="240" height="180">'
            + resolve(fragment, pal) + '</svg>')


def illo_inline(fragment, label):
    return (f'<svg class="illo" viewBox="0 0 240 180" role="img" aria-label="{label}">' + fragment + '</svg>')


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def tokens():
    return {
        "$description": "Дизайн-токены «Рядом». Источник правды для Compose-темы и графики.",
        "color": {"light": UI_LIGHT, "dark": UI_DARK},
        "illustration": {"light": ILLO_LIGHT, "dark": ILLO_DARK},
        "font": {
            "display": {"family": "Source Serif 4", "use": "приветствие, крупные числа, заголовки экранов"},
            "text": {"family": "Onest", "use": "весь интерфейс"},
            "license": "SIL Open Font License, шрифты вшиваются в APK (res/font)",
        },
        "type": {
            "display": {"font": "display", "size": 32, "line": 38, "weight": 400, "tracking": -0.4},
            "headline": {"font": "display", "size": 24, "line": 30, "weight": 500, "tracking": -0.2},
            "title": {"font": "text", "size": 18, "line": 24, "weight": 600},
            "subtitle": {"font": "text", "size": 16, "line": 22, "weight": 600},
            "body": {"font": "text", "size": 16, "line": 24, "weight": 400},
            "body-s": {"font": "text", "size": 14, "line": 20, "weight": 400},
            "label": {"font": "text", "size": 14, "line": 18, "weight": 500, "tracking": 0.1},
            "caption": {"font": "text", "size": 13, "line": 16, "weight": 500, "tracking": 0.2},
            "numeric": {"font": "text", "feature": "tnum", "note": "время и количество — моноширинные цифры"},
        },
        "space": [4, 8, 12, 16, 20, 24, 32, 40, 56],
        "radius": {"s": 10, "m": 16, "l": 24, "pill": 999,
                   "note": "s — чипы и поля, m — строки и плитки, l — листы и крупные блоки"},
        "stroke": {"hairline": 1, "icon": 1.75, "illustration": 2},
        "elevation": {"flat": "border 1dp", "float": "0 8 24 rgba(46,53,49,.08) — только FAB, листы, снекбар"},
        "touch": {"min": 48},
        "motion": {
            "press": {"ms": 120, "easing": "standard"},
            "state": {"ms": 200, "easing": "standard"},
            "enter": {"ms": 280, "easing": "emphasized-decelerate"},
            "progress": {"ms": 450, "easing": "standard"},
            "easing": {"standard": [0.2, 0, 0, 1], "emphasized-decelerate": [0.05, 0.7, 0.1, 1]},
        },
        "icon": {"grid": 24, "live": "2–22", "stroke": 1.75, "cap": "round", "join": "round", "style": "outline"},
    }


def build():
    # иконки
    for name in icons.ICONS:
        write(ASSETS / "icons" / f"{name}.svg", icons.svg(name, color="#2E3531"))
    # иллюстрации
    for key, label, fn in il.ILLUSTRATIONS:
        frag = fn()
        write(ASSETS / "illustrations" / "light" / f"il_{key}.svg", illo_svg(frag, ILLO_LIGHT))
        write(ASSETS / "illustrations" / "dark" / f"il_{key}.svg", illo_svg(frag, ILLO_DARK))
    # логотип
    write(ASSETS / "logo" / "mark.svg", logo.mark())
    write(ASSETS / "logo" / "mark-dark.svg", logo.mark("#B4CAB8", "#C9A693", "#9DB5A2", "#141816"))
    write(ASSETS / "logo" / "mark-mono.svg", logo.mark(mono="#2E3531", gap="#FFFFFF"))
    write(ASSETS / "logo" / "app-icon-512.svg", logo.app_icon(512))
    write(ASSETS / "logo" / "ic_launcher_foreground.svg", logo.adaptive_foreground())
    write(ASSETS / "logo" / "ic_launcher_background.svg", logo.adaptive_background())
    write(ASSETS / "logo" / "ic_launcher_monochrome.svg", logo.adaptive_monochrome())
    write(ASSETS / "logo" / "ic_notification.svg", logo.notification_icon())
    # токены
    write(DESIGN / "tokens" / "tokens.json", json.dumps(tokens(), ensure_ascii=False, indent=2))
    # страница концепции
    src = DESIGN / "concept.src.html"
    if src.exists():
        write(DESIGN / "concept.html", render_page(src.read_text(encoding="utf-8")))
    print("ok:", len(icons.ICONS), "иконок,", len(il.ILLUSTRATIONS), "иллюстраций")


def render_page(tpl):
    """Подстановки в шаблоне: {{icon:name}}, {{illo:key}}, {{icon-grid}}, {{illo-grid}}, {{logo:...}}."""
    illos = {k: (label, fn()) for k, label, fn in il.ILLUSTRATIONS}

    def icon_grid():
        out = []
        for group, names in icons.GROUPS:
            cells = "".join(
                f'<figure class="ic-cell">{icons.svg(n)}<figcaption>{n}</figcaption></figure>' for n in names)
            out.append(f'<div class="ic-group"><h4>{group}</h4><div class="ic-cells">{cells}</div></div>')
        return "".join(out)

    def illo_grid():
        cells = []
        for i, (k, label, _) in enumerate(il.ILLUSTRATIONS, 1):
            cells.append(f'<figure class="il-cell">{illo_inline(illos[k][1], label)}'
                         f'<figcaption><span class="num">{i:02d}</span>{label}</figcaption></figure>')
        return "".join(cells)

    def sub(m):
        kind, _, arg = m.group(1).partition(":")
        if kind == "icon":
            name, _, cls = arg.partition(".")
            return icons.svg(name, cls=f"ic {cls}".strip())
        if kind == "illo":
            return illo_inline(illos[arg][1], illos[arg][0])
        if kind == "icon-grid":
            return icon_grid()
        if kind == "illo-grid":
            return illo_grid()
        if kind == "logo":
            if arg == "mark":
                return logo.mark(left="var(--logo-l)", right="var(--logo-r)", dot="var(--logo-d)", gap="var(--logo-gap)")
            if arg == "app":
                return logo.app_icon(512)
            if arg == "mono":
                return logo.mark(mono="currentColor", gap="var(--logo-gap)")
            if arg == "notif":
                return logo.mark(mono="currentColor", gap=None)
        raise ValueError(m.group(0))

    page = re.sub(r"\{\{([\w:.-]+)\}\}", sub, tpl)

    names = {
        "bg": "Background", "surface": "Surface", "surface-muted": "Surface muted", "primary": "Primary",
        "primary-dark": "Primary Dark", "primary-container": "Primary container", "secondary": "Secondary",
        "accent": "Accent", "warm": "Warm Accent", "warm-container": "Warm container", "text": "Text Primary",
        "text-2": "Text Secondary", "text-2-strong": "Text Secondary · мелкий", "border": "Border",
        "success": "Success", "warning": "Warning", "warning-container": "Warning container",
        "warning-text": "Warning · текст", "error": "Error", "error-container": "Error container",
        "error-text": "Error · текст", "on-primary": "On Primary",
    }

    def swatches(pal):
        return "".join(
            f'<div class="sw"><i style="background:{v}"></i><div><span>{names.get(k, k)}</span><code>{v}</code></div></div>'
            for k, v in pal.items())

    import base64
    ref = DESIGN / "reference" / "user-icon-sketch.jpg"
    ref_uri = "data:image/jpeg;base64," + base64.b64encode(ref.read_bytes()).decode() if ref.exists() else ""
    return (page
            .replace("__SW_LIGHT__", swatches(UI_LIGHT))
            .replace("__SW_DARK__", swatches(UI_DARK))
            .replace("__ALT_ROOF__", logo.alt_roof("var(--logo-l)", "var(--logo-r)", "var(--logo-d)"))
            .replace("__ALT_HANDS__", logo.alt_hands("var(--logo-l)", "var(--logo-r)", "var(--logo-d)"))
            .replace("__REF_IMG__", ref_uri))


if __name__ == "__main__":
    build()
