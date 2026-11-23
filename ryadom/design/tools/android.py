"""SVG → Android VectorDrawable.

Поддерживает ровно то подмножество SVG, которое выдаёт наш генератор:
<svg>, <g transform>, <path>, <circle>, <rect rx>, наследуемые fill/stroke,
transform: translate/rotate/scale, opacity, stroke-dasharray (точечный пунктир).

VectorDrawable не умеет пунктир, поэтому точечные линии раскладываются
в ряд кружков вдоль пути (svgpathtools).
"""
import re
import xml.etree.ElementTree as ET

INHERIT = ("fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin", "stroke-dasharray", "opacity")


def _num(v):
    s = f"{float(v):.3f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def _color(c):
    if c is None or c == "none":
        return None
    c = c.strip()
    if c.startswith("#") and len(c) == 7:
        return "#FF" + c[1:].upper()
    if c.startswith("#") and len(c) == 9:
        return c.upper()
    if c == "currentColor":
        return "#FF000000"
    raise ValueError(f"цвет {c}")


def _rect_d(x, y, w, h, rx):
    if rx <= 0:
        return f"M{_num(x)},{_num(y)}h{_num(w)}v{_num(h)}h{_num(-w)}z"
    rx = min(rx, w / 2, h / 2)
    return (f"M{_num(x + rx)},{_num(y)}H{_num(x + w - rx)}A{_num(rx)},{_num(rx)} 0 0 1 {_num(x + w)},{_num(y + rx)}"
            f"V{_num(y + h - rx)}A{_num(rx)},{_num(rx)} 0 0 1 {_num(x + w - rx)},{_num(y + h)}"
            f"H{_num(x + rx)}A{_num(rx)},{_num(rx)} 0 0 1 {_num(x)},{_num(y + h - rx)}"
            f"V{_num(y + rx)}A{_num(rx)},{_num(rx)} 0 0 1 {_num(x + rx)},{_num(y)}z")


def _circle_d(cx, cy, r):
    return (f"M{_num(cx - r)},{_num(cy)}A{_num(r)},{_num(r)} 0 1 0 {_num(cx + r)},{_num(cy)}"
            f"A{_num(r)},{_num(r)} 0 1 0 {_num(cx - r)},{_num(cy)}z")


def _dots_d(d, step, r):
    from svgpathtools import parse_path
    path = parse_path(d)
    out = []
    for seg in path.continuous_subpaths():
        length = seg.length()
        n = max(1, int(length // step))
        for i in range(n + 1):
            t = seg.ilength(min(length, i * step)) if length > 0 else 0
            p = seg.point(t)
            out.append(_circle_d(p.real, p.imag, r))
    return "".join(out)


def _transforms(t):
    groups = []
    for name, args in re.findall(r"(\w+)\(([^)]*)\)", t or ""):
        a = [float(x) for x in re.split(r"[\s,]+", args.strip()) if x]
        if name == "translate":
            groups.append({"translateX": a[0], "translateY": a[1] if len(a) > 1 else 0})
        elif name == "rotate":
            g = {"rotation": a[0]}
            if len(a) == 3:
                g.update(pivotX=a[1], pivotY=a[2])
            groups.append(g)
        elif name == "scale":
            groups.append({"scaleX": a[0], "scaleY": a[1] if len(a) > 1 else a[0]})
        else:
            raise ValueError(f"transform {name}")
    return groups


def _path_xml(d, st, ind):
    fill = _color(st.get("fill", "#000000"))
    stroke = _color(st.get("stroke"))
    alpha = float(st.get("opacity", 1))
    dash = st.get("stroke-dasharray")
    sw = float(st.get("stroke-width", 1))
    if dash and stroke:
        # точечный пунктир → кружки цвета линии
        step = sum(float(x) for x in re.split(r"[\s,]+", dash.strip()))
        d = _dots_d(d, step, sw / 2)
        fill, stroke = stroke, None
    attrs = [f'android:pathData="{d}"']
    if fill:
        attrs.append(f'android:fillColor="{fill}"')
        if alpha < 1:
            attrs.append(f'android:fillAlpha="{_num(alpha)}"')
    if stroke:
        attrs.append(f'android:strokeColor="{stroke}"')
        attrs.append(f'android:strokeWidth="{_num(sw)}"')
        cap = st.get("stroke-linecap")
        join = st.get("stroke-linejoin")
        if cap:
            attrs.append(f'android:strokeLineCap="{cap}"')
        if join:
            attrs.append(f'android:strokeLineJoin="{join}"')
        if alpha < 1:
            attrs.append(f'android:strokeAlpha="{_num(alpha)}"')
    sep = "\n" + ind + "    "
    return f"{ind}<path{sep}{sep.join(attrs)} />"


def _walk(el, st, ind):
    st = dict(st)
    for k in INHERIT:
        if k in el.attrib:
            st[k] = el.attrib[k]
    tag = el.tag.split("}")[-1]
    out = []
    groups = _transforms(el.attrib.get("transform"))
    inner = ind + "    " * len(groups)
    for i, g in enumerate(groups):
        a = " ".join(f'android:{k}="{_num(v)}"' for k, v in g.items())
        out.append(f"{ind}{'    ' * i}<group {a}>")
    if tag == "path":
        out.append(_path_xml(el.attrib["d"], st, inner))
    elif tag == "circle":
        out.append(_path_xml(_circle_d(*(float(el.attrib[k]) for k in ("cx", "cy", "r"))), st, inner))
    elif tag == "rect":
        x, y = float(el.attrib.get("x", 0)), float(el.attrib.get("y", 0))
        w, h = float(el.attrib["width"]), float(el.attrib["height"])
        rx = float(el.attrib.get("rx", 0))
        out.append(_path_xml(_rect_d(x, y, w, h, rx), st, inner))
    elif tag in ("g", "svg"):
        for ch in el:
            out.append(_walk(ch, st, inner))
    else:
        raise ValueError(f"тег {tag}")
    for i in reversed(range(len(groups))):
        out.append(f"{ind}{'    ' * i}</group>")
    return "\n".join(x for x in out if x)


def to_vector(svg_text, width_dp, height_dp, comment=None):
    root = ET.fromstring(svg_text)
    vb = [float(x) for x in root.attrib["viewBox"].split()]
    body = "\n".join(_walk(ch, {k: root.attrib[k] for k in INHERIT if k in root.attrib}, "    ") for ch in root)
    head = f"<!-- {comment} -->\n" if comment else ""
    return (f'<?xml version="1.0" encoding="utf-8"?>\n{head}'
            f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{_num(width_dp)}dp"\n    android:height="{_num(height_dp)}dp"\n'
            f'    android:viewportWidth="{_num(vb[2])}"\n    android:viewportHeight="{_num(vb[3])}">\n'
            f'{body}\n</vector>\n')
