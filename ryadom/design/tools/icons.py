"""Иконки «Рядом»: сетка 24×24, живая зона 2–22, линия 1.75, скруглённые концы.

Все иконки — только контур (stroke). Точки рисуются коротким отрезком
с круглым концом («h.01»), чтобы не смешивать filled и outline.
"""

ICONS = {
    # --- навигация ---
    "home": '<path d="M4.5 10.2 12 4.5l7.5 5.7V19a1.5 1.5 0 0 1-1.5 1.5H6A1.5 1.5 0 0 1 4.5 19z"/><path d="M10 20.5V17a2 2 0 0 1 4 0v3.5"/>',
    "calendar": '<rect x="4" y="5.5" width="16" height="15" rx="3"/><path d="M4 10h16M8.5 3.5v4M15.5 3.5v4M8.5 14.5h.01M12 14.5h.01M15.5 14.5h.01M8.5 17.5h.01M12 17.5h.01"/>',
    "family": '<circle cx="9" cy="8" r="3.2"/><path d="M3.5 19.5c0-3.2 2.4-5.4 5.5-5.4s5.5 2.2 5.5 5.4"/><circle cx="17.2" cy="10.6" r="2.3"/><path d="M15.6 14.9c.5-.2 1-.3 1.6-.3 2 0 3.3 1.5 3.3 3.9"/>',
    "history": '<path d="M4.6 12.8A7.5 7.5 0 1 0 6.8 6.6"/><path d="M4.5 4.2v3.3h3.3"/><path d="M12 8.3V12l2.6 1.6"/>',
    # --- действия ---
    "plus": '<path d="M12 5.5v13M5.5 12h13"/>',
    "check": '<path d="M5.5 12.5l4 4 9-9"/>',
    "check-circle": '<circle cx="12" cy="12" r="8.5"/><path d="M8.4 12.2l2.5 2.5 4.8-4.9"/>',
    "later": '<path d="M19.4 12.8A7.5 7.5 0 1 1 17.2 6.6"/><path d="M19.5 4.2v3.3h-3.3"/><path d="M12 8.3V12l2.6 1.6"/>',
    "skip": '<circle cx="12" cy="12" r="8.5"/><path d="M8.5 12h7"/>',
    "close": '<path d="M6.5 6.5l11 11M17.5 6.5l-11 11"/>',
    "more": '<path d="M6 12h.01M12 12h.01M18 12h.01" stroke-width="2.6"/>',
    "edit": '<path d="M15.2 5.3a2 2 0 0 1 2.9 0l.6.6a2 2 0 0 1 0 2.9L9 18.5l-4 1 1-4z"/><path d="M13.5 7l3.5 3.5"/>',
    "trash": '<path d="M5 7h14M9.5 7V5.5a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1V7"/><path d="M7 7l.8 11.6a2 2 0 0 0 2 1.9h4.4a2 2 0 0 0 2-1.9L17 7"/><path d="M10.5 11v5.5M13.5 11v5.5"/>',
    "archive": '<rect x="4" y="4.5" width="16" height="4" rx="1.5"/><path d="M5.5 8.5V18a2 2 0 0 0 2 2h9a2 2 0 0 0 2-2V8.5M10 12.5h4"/>',
    "search": '<circle cx="11" cy="11" r="6.5"/><path d="M16 16l3.5 3.5"/>',
    "send": '<path d="M4.8 11.6 19.2 5l-4.3 14.2-3.1-5.9z"/><path d="M11.8 13.3 19.2 5"/>',
    "share": '<path d="M12 4v10.5M7.8 8.2 12 4l4.2 4.2"/><path d="M5 13v5.5A1.5 1.5 0 0 0 6.5 20h11a1.5 1.5 0 0 0 1.5-1.5V13"/>',
    "link": '<path d="M10.2 13.8a3.8 3.8 0 0 0 5.4 0l2.6-2.6a3.8 3.8 0 0 0-5.4-5.4l-1 1"/><path d="M13.8 10.2a3.8 3.8 0 0 0-5.4 0l-2.6 2.6a3.8 3.8 0 0 0 5.4 5.4l1-1"/>',
    "camera": '<path d="M4 8.7a2 2 0 0 1 2-2h2.1l1.5-2.2h4.8l1.5 2.2H18a2 2 0 0 1 2 2V18a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z"/><circle cx="12" cy="13.2" r="3.4"/>',
    "download": '<path d="M12 4v10.5M7.8 10.3 12 14.5l4.2-4.2M5 19.5h14"/>',
    "logout": '<path d="M10 4.5H6.5a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2H10"/><path d="M14.5 8l4 4-4 4M18.5 12h-9"/>',
    "chevron-right": '<path d="M9.5 6l6 6-6 6"/>',
    "chevron-left": '<path d="M14.5 6l-6 6 6 6"/>',
    "chevron-down": '<path d="M6 9.5l6 6 6-6"/>',
    "arrow-left": '<path d="M19 12H5.5M11 6l-6 6 6 6"/>',
    # --- препараты ---
    "capsule": '<rect x="3.6" y="8.5" width="16.8" height="7" rx="3.5" transform="rotate(-45 12 12)"/><path d="M9.5 9.5l5 5"/>',
    "tablet": '<circle cx="12" cy="12" r="7.5"/><path d="M8.2 15.8l7.6-7.6"/>',
    "drop": '<path d="M12 3.8c3 3.5 5.5 6.6 5.5 9.8a5.5 5.5 0 0 1-11 0c0-3.2 2.5-6.3 5.5-9.8z"/>',
    "measure": '<path d="M9.5 3.8h5M10.3 3.8v5.1L5.8 17a2.3 2.3 0 0 0 2 3.4h8.4a2.3 2.3 0 0 0 2-3.4l-4.5-8.1V3.8"/><path d="M7.6 14.5h8.8"/>',
    "sachet": '<path d="M6 6.2l1.5-1.7 1.5 1.7 1.5-1.7 1.5 1.7 1.5-1.7 1.5 1.7 1.5-1.7L18 6.2v12.3a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 6 18.5z"/><path d="M9 12.5h6M9 15.5h4"/>',
    "jar": '<rect x="7" y="3.5" width="10" height="3.5" rx="1"/><path d="M7.5 7h9v11.5a2 2 0 0 1-2 2h-5a2 2 0 0 1-2-2z"/><path d="M7.5 11h9M7.5 16.5h9"/>',
    "glass": '<path d="M6 4.5h12l-1.4 14.2a2 2 0 0 1-2 1.8H9.4a2 2 0 0 1-2-1.8z"/><path d="M6.8 11.5h10.4"/>',
    "box": '<path d="M4 8l8-4 8 4v8.5l-8 4-8-4z"/><path d="M4 8l8 4 8-4M12 12v8.5"/>',
    "bag": '<path d="M5.5 8.5h13l-.9 10.6a1.5 1.5 0 0 1-1.5 1.4H7.9a1.5 1.5 0 0 1-1.5-1.4z"/><path d="M9 10.5V7a3 3 0 0 1 6 0v3.5"/>',
    "first-aid": '<rect x="3.5" y="7" width="17" height="12.5" rx="3"/><path d="M9 7V5.5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1V7M12 10.5v5.5M9.2 13.2h5.6"/>',
    "note": '<path d="M6.5 3.5h7.8l4.2 4.2v10.8a2 2 0 0 1-2 2h-10a2 2 0 0 1-2-2v-13a2 2 0 0 1 2-2z"/><path d="M14 3.5v4.5h4.5M8.5 12.5h7M8.5 16h5"/>',
    "flag": '<path d="M5.5 20.5V4.5"/><path d="M5.5 5h11.5l-2.3 4 2.3 4H5.5"/>',
    # --- время суток ---
    "sunrise": '<path d="M3.5 18h17M7.5 18a4.5 4.5 0 0 1 9 0M12 6.5v2.3M6.2 9.6l1.5 1.5M17.8 9.6l-1.5 1.5M3.8 14.5h1.8M18.4 14.5h1.8"/>',
    "sun": '<circle cx="12" cy="12" r="3.8"/><path d="M12 3.5v1.8M12 18.7v1.8M3.5 12h1.8M18.7 12h1.8M6 6l1.3 1.3M16.7 16.7 18 18M6 18l1.3-1.3M16.7 7.3 18 6"/>',
    "moon": '<path d="M19 14.6A7.5 7.5 0 1 1 9.4 5a6 6 0 0 0 9.6 9.6z"/>',
    "clock": '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 1.9"/>',
    "repeat": '<path d="M4.5 11V9.5A2.5 2.5 0 0 1 7 7h12M16 4l3 3-3 3"/><path d="M19.5 13v1.5A2.5 2.5 0 0 1 17 17H5M8 20l-3-3 3-3"/>',
    # --- люди и семья ---
    "user": '<circle cx="12" cy="8.5" r="3.6"/><path d="M5 20c0-3.6 3.1-6.2 7-6.2s7 2.6 7 6.2"/>',
    "user-plus": '<circle cx="10" cy="8.5" r="3.6"/><path d="M3.5 20c0-3.6 2.9-6.2 6.5-6.2 1.3 0 2.5.3 3.5.9M18 14v6M15 17h6"/>',
    "child": '<circle cx="12" cy="10" r="3"/><path d="M12 7c.2-1.3 1.3-2.1 2.6-1.9"/><path d="M7 20c0-2.9 2.2-5.2 5-5.2s5 2.3 5 5.2"/>',
    "heart": '<path d="M12 19.5s-7.5-4.4-7.5-10A4.2 4.2 0 0 1 12 7a4.2 4.2 0 0 1 7.5 2.5c0 5.6-7.5 10-7.5 10z"/>',
    "message": '<path d="M4.5 7.2a2.7 2.7 0 0 1 2.7-2.7h9.6a2.7 2.7 0 0 1 2.7 2.7v6.6a2.7 2.7 0 0 1-2.7 2.7H11l-4.5 3.5v-3.5h.7a2.7 2.7 0 0 1-2.7-2.7z"/>',
    "shield": '<path d="M12 3.6l7 2.6v5.4c0 4.3-2.9 7.4-7 8.8-4.1-1.4-7-4.5-7-8.8V6.2z"/><path d="M9 11.8l2.1 2.1 3.9-4"/>',
    # --- системные ---
    "bell": '<path d="M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 2h-15z"/><path d="M10 21h4"/>',
    "bell-off": '<path d="M8.2 6.2A6 6 0 0 1 18 11v4M6 11v5.5l-1.5 2h12"/><path d="M10 21h4M4 4l16 16"/>',
    "settings": '<path d="M4 7.5h8.5M17.5 7.5H20M4 16.5h2.5M11.5 16.5H20"/><circle cx="15" cy="7.5" r="2.5"/><circle cx="9" cy="16.5" r="2.5"/>',
    "sync": '<path d="M19 12a7 7 0 0 1-12.4 4.4M5 12a7 7 0 0 1 12.4-4.4"/><path d="M17.8 4v3.6h-3.6M6.2 20v-3.6h3.6"/>',
    "cloud-check": '<path d="M7.3 18.5h9.4a3.8 3.8 0 0 0 .6-7.5 5.5 5.5 0 0 0-10.5-1.2A4.4 4.4 0 0 0 7.3 18.5z"/><path d="M9.6 14.2l1.8 1.8 3.2-3.3"/>',
    "cloud-off": '<path d="M9.3 6.3a5.5 5.5 0 0 1 8 4.7 3.8 3.8 0 0 1 2.4 5.8M16.5 18.5H7.3A4.4 4.4 0 0 1 6.8 9.8"/><path d="M4 4l16 16"/>',
    "info": '<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5M12 8h.01"/>',
    "lock": '<rect x="5.5" y="10.5" width="13" height="10" rx="2.5"/><path d="M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5M12 14.5v2"/>',
    "globe": '<circle cx="12" cy="12" r="8.5"/><path d="M3.5 12h17M12 3.5c2.3 2.4 3.4 5.2 3.4 8.5s-1.1 6.1-3.4 8.5c-2.3-2.4-3.4-5.2-3.4-8.5s1.1-6.1 3.4-8.5z"/>',
    "theme": '<circle cx="12" cy="12" r="8.5"/><path d="M12 3.5v17M12 8h6.6M12 12h8.5M12 16h6.6"/>',
    "volume": '<path d="M4.5 9.5h3l4.5-4v13l-4.5-4h-3z"/><path d="M15.5 9a4 4 0 0 1 0 6M18 6.5a7.5 7.5 0 0 1 0 11"/>',
    "vibrate": '<rect x="8.5" y="4.5" width="7" height="15" rx="1.8"/><path d="M4.8 9v6M19.2 9v6"/>',
    "chart": '<path d="M4.5 20h15M6.5 16.5V12M10.5 16.5V6.5M14.5 16.5v-6M18.5 16.5V9"/>',
    "pause": '<path d="M9.5 6.5v11M14.5 6.5v11"/>',
    "leaf": '<path d="M5.5 18.5C5.5 11 10 5.8 18.8 5.2c-.4 8.6-5.5 13.3-13.3 13.3z"/><path d="M5.5 18.5l7-7"/>',
}

# Группы — для документации и витрины на странице концепции
GROUPS = [
    ("Навигация", ["home", "calendar", "family", "history"]),
    ("Действия", ["plus", "check", "check-circle", "later", "skip", "close", "more", "edit", "trash", "archive",
                  "search", "send", "share", "link", "camera", "download", "logout",
                  "chevron-right", "chevron-left", "chevron-down", "arrow-left"]),
    ("Препараты", ["capsule", "tablet", "drop", "measure", "sachet", "jar", "glass", "box", "bag", "first-aid", "note", "flag"]),
    ("Время", ["sunrise", "sun", "moon", "clock", "repeat"]),
    ("Люди", ["user", "user-plus", "child", "heart", "message", "shield"]),
    ("Система", ["bell", "bell-off", "settings", "sync", "cloud-check", "cloud-off", "info", "lock", "globe",
                 "theme", "volume", "vibrate", "chart", "pause", "leaf"]),
]

STROKE_WIDTH = 1.75


def svg(name, color="currentColor", size=24, cls=None, extra=""):
    body = ICONS[name]
    c = f' class="{cls}"' if cls else ""
    return (f'<svg{c} xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 24 24" '
            f'fill="none" stroke="{color}" stroke-width="{STROKE_WIDTH}" stroke-linecap="round" '
            f'stroke-linejoin="round" aria-hidden="true"{extra}>{body}</svg>')
