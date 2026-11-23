"""Единый источник цветов для генератора ассетов «Рядом».

Те же значения лежат в design/tokens/tokens.json и (на этапе 2) в
ui/theme/Color.kt. Менять цвет — только здесь и в tokens.json.
"""

# Цвета интерфейса (из ТЗ, раздел 5) + производные, которых не хватало
UI_LIGHT = {
    "bg": "#F6F5F0",
    "surface": "#FFFFFF",
    "surface-muted": "#EFEEE7",
    "primary": "#829987",
    "primary-dark": "#536C5B",
    "primary-container": "#E4EBE3",
    "secondary": "#D9CBB8",
    "accent": "#B7C8B8",
    "warm": "#D8B9A8",
    "warm-container": "#F4EAE3",
    "text": "#2E3531",
    "text-2": "#818780",
    "text-2-strong": "#666C65",
    "border": "#E7E8E2",
    "success": "#7E9B83",
    "warning": "#D2B17D",
    "warning-container": "#F6EEDF",
    "error": "#C8877C",
    "error-container": "#F5E6E2",
    "error-text": "#8D544B",
    "warning-text": "#7A5C25",
    "on-primary": "#FFFFFF",
}

UI_DARK = {
    "bg": "#141816",
    "surface": "#1C211E",
    "surface-muted": "#232925",
    "primary": "#9DB5A2",
    "primary-dark": "#B4CAB8",
    "primary-container": "#2A372E",
    "secondary": "#6E6455",
    "accent": "#3E5044",
    "warm": "#A88878",
    "warm-container": "#33291F",
    "text": "#ECEAE3",
    "text-2": "#A2A79F",
    "text-2-strong": "#B9BDB4",
    "border": "#2C332F",
    "success": "#93B098",
    "warning": "#DDBF8E",
    "warning-container": "#352E22",
    "error": "#DB9C91",
    "error-container": "#3A2925",
    "error-text": "#E8B2A8",
    "warning-text": "#E5C995",
    "on-primary": "#16201A",
}

# Палитра иллюстраций: те же оттенки, что и в интерфейсе, плюс «кора» для волос и дерева
ILLO_LIGHT = {
    "ink": "#3A433D",
    "paper": "#FFFFFF",
    "cream": "#ECE7DA",
    "sage": "#B7C8B8",
    "leaf": "#829987",
    "sand": "#D9CBB8",
    "clay": "#D8B9A8",
    "bark": "#A8937E",
}

ILLO_DARK = {
    "ink": "#D9D5CA",
    "paper": "#252B27",
    "cream": "#262C28",
    "sage": "#4A5D4F",
    "leaf": "#7F9A85",
    "sand": "#615749",
    "clay": "#73594D",
    "bark": "#8E7966",
}
