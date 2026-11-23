"""Иллюстрации «Рядом» — единая авторская коллекция.

Правила стиля (их же проверяет генератор):
  * холст 240×180, всё в одной системе координат;
  * контур — одна толщина 2 (детали 1.5), цвет «ink», круглые концы;
  * заливка — пастельное пятно, сдвинутое от контура на (4, 3):
    эффект «немного смещённой печати», который делает набор узнаваемым;
  * люди — «арки и точки»: тело-арка, голова-круг, без лиц и мелких деталей.
    Тот же язык, что и в логотипе (две арки и точка);
  * палитра — только токены ILLO_* (ink, paper, cream, sage, leaf, sand, clay, bark).

Каждая функция возвращает SVG-фрагмент, где цвет задан классом:
  l   — контур 2px;  lt — контур 1.5px;  ld — пунктир;
  f-* — заливка токеном.
Генератор превращает классы в атрибуты (для файлов и Android) или
оставляет классы (для HTML-витрины с переключением темы).
"""

DX, DY = 4, 3  # смещение «печати»


def off(shape, fill, dx=DX, dy=DY):
    """Пятно заливки, смещённое от контура."""
    return f'<g transform="translate({dx} {dy})">{shape.replace("CLS", "f-" + fill)}</g>'


def line(shape, cls="l"):
    return shape.replace("CLS", cls)


def obj(shape, fill, cls="l", dx=DX, dy=DY):
    """Объект целиком: смещённая заливка + контур."""
    return off(shape, fill, dx, dy) + line(shape, cls)


def solid(shape, fill):
    return shape.replace("CLS", "f-" + fill)


# ---------- общие элементы ----------

def ground(cx=120, cy=146, rx=96, ry=22):
    return (f'<path class="f-cream" d="M{cx-rx} {cy}c0-{ry*0.8:.1f} {rx*0.35:.1f}-{ry} {rx} -{ry}'
            f's{rx} {ry*0.25:.1f} {rx} {ry}-{rx*0.4:.1f} {ry*0.9:.1f}-{rx} {ry*0.9:.1f}'
            f'-{rx}-{ry*0.15:.1f}-{rx}-{ry*0.9:.1f}z"/>')


def base(x1=56, x2=184, y=146):
    return f'<path class="l" d="M{x1} {y}H{x2}"/>'


def dots(points, fill="sand", r=2.4):
    return "".join(f'<circle class="f-{fill}" cx="{x}" cy="{y}" r="{r}"/>' for x, y in points)


def sprig(x, y, flip=False, fill="sage", h=34):
    """Веточка: стебель + три листа. h — высота стебля."""
    sx = -1 if flip else 1
    leaf = 'M0 0C5 -4 11 -4 15 0C11 4 5 4 0 0z'
    parts = [f'<path class="l" d="M0 0C2 -{h*0.35:.1f} 1 -{h*0.7:.1f} -1 -{h}"/>']
    for i, (ly, ang, side) in enumerate([(h * 0.3, -35, 1), (h * 0.58, -145, -1), (h * 0.85, -55, 1)]):
        g = f'<g transform="translate({0.5 if side > 0 else 1.2} -{ly:.1f}) rotate({ang})">'
        parts.append(g + obj(f'<path class="CLS" d="{leaf}"/>', fill, dx=2.5, dy=2) + '</g>')
    return f'<g transform="translate({x} {y}) scale({sx} 1)">' + "".join(parts) + '</g>'


def arch(x, bottom, w, h):
    """Арка: тело человека или окно."""
    top = bottom - h
    return f'<path class="CLS" d="M{x-w} {bottom}V{top+w}A{w} {w} 0 0 1 {x+w} {top+w}V{bottom}"/>'


def head(x, cy, r, hair, skin="clay", hairc="bark"):
    out = obj(f'<circle class="CLS" cx="{x}" cy="{cy}" r="{r}"/>', skin)
    k = r / 12.5
    if hair == "short":
        d = (f'M{x-r} {cy-1*k:.1f}A{r} {r} 0 0 1 {x+r} {cy-1*k:.1f}'
             f'C{x+7*k:.1f} {cy-6*k:.1f} {x-3*k:.1f} {cy-8.5*k:.1f} {x-r} {cy-1*k:.1f}z')
        out += obj(f'<path class="CLS" d="{d}"/>', hairc, dx=2, dy=1.5)
    elif hair in ("long", "bun"):
        R = r + 0.8
        if hair == "long":
            d = (f'M{x-R:.1f} {cy+14*k:.1f}V{cy}A{R:.1f} {R:.1f} 0 0 1 {x+R:.1f} {cy}V{cy+14*k:.1f}'
                 f'C{x+10*k:.1f} {cy+15*k:.1f} {x+8.5*k:.1f} {cy+9*k:.1f} {x+8.5*k:.1f} {cy+2*k:.1f}'
                 f'C{x+4*k:.1f} {cy+1*k:.1f} {x-4*k:.1f} {cy-2*k:.1f} {x-8.5*k:.1f} {cy-3*k:.1f}'
                 f'V{cy+10*k:.1f}C{x-8.5*k:.1f} {cy+13*k:.1f} {x-10.5*k:.1f} {cy+15*k:.1f} {x-R:.1f} {cy+14*k:.1f}z')
            out += obj(f'<path class="CLS" d="{d}"/>', hairc, dx=2, dy=1.5)
        else:
            out = obj(f'<circle class="CLS" cx="{x+7*k:.1f}" cy="{cy-11*k:.1f}" r="{5.5*k:.1f}"/>', hairc, dx=2, dy=1.5) + out
            d = (f'M{x-r} {cy}A{r} {r} 0 0 1 {x+r} {cy}'
                 f'C{x+6*k:.1f} {cy-3*k:.1f} {x-2*k:.1f} {cy-7*k:.1f} {x-r} {cy}z')
            out += obj(f'<path class="CLS" d="{d}"/>', hairc, dx=2, dy=1.5)
    elif hair == "tuft":
        out += line(f'<path class="CLS" d="M{x-1} {cy-r}C{x} {cy-r-5} {x+5} {cy-r-6} {x+7} {cy-r-3}"/>')
    return out


def adult(x, bottom=146, h=66, w=22, top="sage", hair="short", skin="clay"):
    body = obj(arch(x, bottom, w, h), top)
    hd = head(x, bottom - h - 15, 12.5, hair, skin)
    return body + hd


def child(x, bottom=146, h=40, w=14, top="clay", hair="tuft", skin="sand"):
    body = obj(arch(x, bottom, w, h), top)
    hd = head(x, bottom - h - 11, 9, hair, skin)
    return body + hd


def capsule(cx, cy, length, width, angle, fill_a="sand", fill_b="paper"):
    r = width / 2
    x0, y0 = cx - length / 2, cy - r
    half = f'<path class="CLS" d="M{cx} {y0}H{x0+r}a{r} {r} 0 0 0 0 {width}H{cx}z"/>'
    whole = f'<rect class="CLS" x="{x0}" y="{y0}" width="{length}" height="{width}" rx="{r}"/>'
    g = f'<g transform="rotate({angle} {cx} {cy})">'
    g += off(whole, fill_b) + solid(f'<g transform="translate({DX} {DY})">' + half + '</g>', fill_a)
    g += line(whole) + line(f'<path class="CLS" d="M{cx} {y0}V{y0+width}"/>')
    return g + '</g>'


def tablet(cx, cy, r, fill="clay", angle=-30):
    return (obj(f'<circle class="CLS" cx="{cx}" cy="{cy}" r="{r}"/>', fill)
            + f'<g transform="rotate({angle} {cx} {cy})">'
            + line(f'<path class="CLS" d="M{cx-r*0.55:.1f} {cy}H{cx+r*0.55:.1f}"/>', "lt") + '</g>')


def jar(x, bottom, w=34, h=50, body_fill="paper", lid_fill="leaf", label_fill="sand", level=None):
    """Баночка витаминов. level — доля заполнения (для «мало осталось»)."""
    top = bottom - h
    lid = f'<rect class="CLS" x="{x-w/2+3}" y="{top-11}" width="{w-6}" height="11" rx="3"/>'
    bod = (f'<path class="CLS" d="M{x-w/2} {top+6}a6 6 0 0 1 6-6h{w-12}a6 6 0 0 1 6 6V{bottom-6}'
           f'a6 6 0 0 1-6 6h-{w-12}a6 6 0 0 1-6-6z"/>')
    out = obj(bod, body_fill) + obj(lid, lid_fill)
    if level is not None:
        ly = bottom - (h - 6) * level
        out += solid(f'<path class="CLS" d="M{x-w/2+2} {ly}H{x+w/2-2}V{bottom-5}a5 5 0 0 1-5 5h-{w-14}a5 5 0 0 1-5-5z"/>', "sage")
        out += line(bod)
        out += line(f'<path class="CLS" d="M{x-w/2+5} {ly}H{x+w/2-5}"/>', "ld")
    else:
        lb = f'<rect class="CLS" x="{x-w/2}" y="{top+h*0.32:.1f}" width="{w}" height="{h*0.36:.1f}"/>'
        out += solid(lb, label_fill) + line(bod)
        out += line(f'<path class="CLS" d="M{x-w/2+7} {top+h*0.5:.1f}H{x+w/2-9}"/>', "lt")
    return out


def glass(x, bottom, w=40, h=62, water=0.6):
    t = bottom - h
    shape = (f'<path class="CLS" d="M{x-w/2} {t}H{x+w/2}L{x+w/2-5} {bottom-5}'
             f'a5 5 0 0 1-5 4.6H{x-w/2+10}a5 5 0 0 1-5-4.6z"/>')
    wy = bottom - h * water
    k = (bottom - wy) / h * 5
    water_d = (f'<path class="CLS" d="M{x-w/2+5-k:.1f} {wy}C{x-6} {wy-3} {x+6} {wy+3} {x+w/2-5+k:.1f} {wy}'
               f'L{x+w/2-5} {bottom-5}a5 5 0 0 1-5 4.6H{x-w/2+10}a5 5 0 0 1-5-4.6z"/>')
    return (off(water_d, "sage") + line(shape)
            + line(f'<path class="CLS" d="M{x-w/2+5-k:.1f} {wy}C{x-6} {wy-3} {x+6} {wy+3} {x+w/2-5+k:.1f} {wy}"/>', "lt"))


def logo_embrace(cx, cy, r, left="sage", right="clay", dot="leaf", band=14):
    """Знак «Рядом» на языке иллюстраций: две полудуги-ленты с контуром и точка."""
    import math
    off = r * 0.19
    t = math.degrees(math.atan2(-math.sqrt(r * r - off * off), off)) + 15
    ro, ri, h = r + band / 2, r - band / 2, band / 2

    def ribbon(c, ccy, a0, a1):
        P = lambda rad, a: (c + rad * math.cos(math.radians(a)), ccy + rad * math.sin(math.radians(a)))
        o0, o1, i0, i1 = P(ro, a0), P(ro, a1), P(ri, a0), P(ri, a1)
        return (f'<path class="CLS" d="M{o0[0]:.1f} {o0[1]:.1f}A{ro} {ro} 0 1 0 {o1[0]:.1f} {o1[1]:.1f}'
                f'A{h} {h} 0 0 0 {i1[0]:.1f} {i1[1]:.1f}A{ri} {ri} 0 1 1 {i0[0]:.1f} {i0[1]:.1f}'
                f'A{h} {h} 0 0 0 {o0[0]:.1f} {o0[1]:.1f}z"/>')
    R = ribbon(cx + off, cy, 180 + t, 180 - t)
    L = ribbon(cx - off, cy, t, -t)
    return (off_fill(R, right) + line(R) + off_fill(L, left) + line(L.replace('class="CLS"', 'class="CLS"'))
            + obj(f'<circle class="CLS" cx="{cx}" cy="{cy}" r="{r*0.24:.1f}"/>', dot))


def off_fill(shape, fill):
    """Заливка со сдвигом + непрозрачная «бумага» под контуром, чтобы нахлёст читался."""
    return shape.replace("CLS", "f-paper") + off(shape, fill)


# ---------- 20 иллюстраций ----------

def il_symbol():
    return (ground(rx=100) + logo_embrace(120, 92, 44) + base(40, 200)
            + sprig(52, 146) + sprig(190, 146, flip=True, fill="sand", h=28)
            + dots([(62, 56), (178, 40), (190, 72)]))


def il_vitamins():
    return (ground() + base()
            + capsule(102, 112, 74, 26, -28, "sand")
            + capsule(152, 128, 44, 16, 18, "sage")
            + tablet(166, 96, 13, "clay")
            + tablet(72, 136, 8, "sage", 20)
            + dots([(132, 64), (190, 116), (60, 96)]))


def il_water():
    drop = 'M0 -16C6 -8 10 -2 10 4a10 10 0 0 1-20 0c0-6 4-12 10-20z'
    return (ground() + base()
            + glass(112, 146, 46, 76, 0.62)
            + f'<g transform="translate(160 104)">' + obj(f'<path class="CLS" d="{drop}"/>', "leaf") + '</g>'
            + capsule(156, 138, 34, 12, -8, "clay")
            + sprig(66, 146, fill="sage", h=30)
            + dots([(176, 66), (90, 48)]))


def il_family_care():
    return (ground(rx=104) + base(36, 204)
            + line('<path class="CLS" d="M58 66C78 26 162 26 182 66"/>', "ld")
            + adult(98, top="clay", hair="long") + adult(144, top="sage", hair="short", h=72)
            + dots([(120, 30)], "leaf", 4) + dots([(56, 78), (186, 80)]))


def il_mother_child():
    return (ground() + base(40, 200)
            + adult(104, top="sand", hair="long")
            + line('<path class="CLS" d="M124 108C134 110 140 114 144 120"/>')
            + child(152, top="sage")
            + sprig(190, 146, flip=True, h=26)
            + dots([(66, 64), (178, 74)]))


def il_father_child():
    return (ground() + base(40, 200)
            + child(84, top="clay", hair="bun")
            + line('<path class="CLS" d="M117 104C108 106 100 110 96 118"/>')
            + adult(138, top="sage", hair="short", h=72)
            + sprig(50, 146, h=26)
            + dots([(176, 56), (66, 80)]))


def il_family_scene():
    window = arch(120, 146, 64, 128)
    return (off(window, "cream", 0, 0)
            + line(window, "lt")
            + adult(80, top="clay", hair="long")
            + child(120, top="sand", h=36)
            + adult(160, top="sage", hair="short", h=72)
            + base(28, 212))


def il_morning():
    sun = '<path class="CLS" d="M74 146a46 46 0 0 1 92 0z"/>'
    rays = "".join(
        f'<path class="lt" d="M{120+58*c:.1f} {146-58*s:.1f}L{120+68*c:.1f} {146-68*s:.1f}"/>'
        for c, s in [(-0.94, 0.34), (-0.64, 0.77), (0, 1), (0.64, 0.77), (0.94, 0.34)])
    return (ground() + off(sun, "sand", 0, 0) + line(sun, "lt") + rays
            + glass(98, 146, 30, 50, 0.55)
            + jar(140, 146, 30, 40, lid_fill="leaf")
            + base(40, 200))


def il_evening():
    moon = 'M0 -22a22 22 0 1 0 20 30a17 17 0 0 1-20-30z'
    mug = ('<path class="CLS" d="M92 104h40v32a10 10 0 0 1-10 10h-20a10 10 0 0 1-10-10z"/>')
    handle = '<path class="l" d="M132 112h4a8 8 0 0 1 0 16h-4"/>'
    stars = "".join(f'<path class="lt" d="M{x-4} {y}h8M{x} {y-4}v8"/>' for x, y in [(74, 50), (98, 30), (196, 92)])
    return (ground() + base()
            + f'<g transform="translate(168 52)">' + obj(f'<path class="CLS" d="{moon}"/>', "sand") + '</g>'
            + stars + obj(mug, "sage") + handle
            + line('<path class="CLS" d="M104 94c-3-5 3-8 0-13M116 94c-3-5 3-8 0-13"/>', "lt")
            + capsule(160, 140, 30, 11, -12, "clay")
            + dots([(66, 110)]))


def il_package():
    box = '<path class="CLS" d="M64 84h56v62H64z"/>'
    return (ground() + base()
            + obj(box, "clay")
            + line('<path class="CLS" d="M64 98H120M74 112h24M74 120h16"/>', "lt")
            + jar(150, 146, 44, 66, lid_fill="leaf", label_fill="sand")
            + sprig(190, 146, flip=True, h=24)
            + dots([(92, 62), (186, 76)]))


def il_first_aid():
    kit = '<rect class="CLS" x="64" y="82" width="104" height="64" rx="14"/>'
    handle = '<path class="l" d="M100 82V72a6 6 0 0 1 6-6h20a6 6 0 0 1 6 6v10"/>'
    badge = '<circle class="CLS" cx="116" cy="114" r="17"/>'
    return (ground() + base()
            + obj(kit, "sage") + handle
            + obj(badge, "paper", dx=0, dy=0)
            + line('<path class="CLS" d="M116 105v18M107 114h18"/>')
            + jar(186, 146, 22, 30, lid_fill="clay")
            + dots([(60, 64), (160, 58)]))


def il_reminder():
    bell = ('<path class="CLS" d="M96 124V100a24 24 0 0 1 48 0v24l8 10H88z"/>')
    clock = '<circle class="CLS" cx="178" cy="118" r="18"/>'
    return (ground() + base(52, 206)
            + obj(bell, "sand")
            + line('<path class="CLS" d="M112 142a8 8 0 0 0 16 0"/>')
            + line('<path class="CLS" d="M120 76V68"/>')
            + line('<path class="CLS" d="M74 86c-6 6-8 14-6 22M166 86c6 6 8 14 6 22"/>', "lt")
            + obj(clock, "paper", dx=3, dy=2)
            + line('<path class="CLS" d="M178 108v10l6 4"/>', "lt")
            + dots([(66, 56), (176, 62)]))


def il_calendar():
    sheet = '<rect class="CLS" x="66" y="48" width="108" height="98" rx="12"/>'
    band = '<path class="CLS" d="M66 60a12 12 0 0 1 12-12h84a12 12 0 0 1 12 12v12H66z"/>'
    out = ground() + off(sheet, "paper") + off(band, "sage") + line(sheet) + line('<path class="CLS" d="M66 72H174"/>')
    out += line('<path class="CLS" d="M90 40v16M150 40v16"/>')
    states = ["done", "done", "done", "done", "done", "done", "today", "dot", "dot", "dot", "dot", "dot"]
    i = 0
    for r in range(3):
        for c in range(4):
            x, y = 86 + c * 23, 90 + r * 20
            s = states[i]; i += 1
            if s == "done":
                out += f'<circle class="f-leaf" cx="{x}" cy="{y}" r="5"/>'
            elif s == "today":
                out += f'<circle class="l" cx="{x}" cy="{y}" r="7"/>' + f'<circle class="f-clay" cx="{x}" cy="{y}" r="3.5"/>'
            else:
                out += f'<circle class="f-sand" cx="{x}" cy="{y}" r="3"/>'
    return out + sprig(190, 146, flip=True, h=28) + dots([(50, 70)])


def il_intake_done():
    circle = '<circle class="CLS" cx="120" cy="92" r="40"/>'
    return (ground() + base()
            + obj(circle, "sage")
            + line('<path class="CLS" d="M102 92l12 12 24-26"/>')
            + capsule(168, 138, 36, 13, -20, "sand")
            + tablet(72, 136, 9, "clay")
            + line('<path class="CLS" d="M70 54l-6-6M170 54l6-6M120 34v-8"/>', "lt"))


def il_empty_schedule():
    tray = '<rect class="CLS" x="46" y="98" width="112" height="48" rx="14"/>'
    vase = '<path class="CLS" d="M184 146c-8 0-12-6-12-14s4-14 12-14 12 6 12 14-4 14-12 14z"/>'
    return (ground() + off(tray, "cream", 0, 0) + line(tray)
            + line('<path class="CLS" d="M64 116H128M64 130H108"/>', "ld")
            + sprig(186, 122, h=40, fill="sage")
            + obj(vase, "sand")
            + base(40, 200)
            + dots([(84, 70), (124, 56)]))


def il_missed():
    clock = '<circle class="CLS" cx="112" cy="94" r="36"/>'
    return (ground() + base()
            + obj(clock, "paper")
            + solid('<path class="CLS" d="M112 58a36 36 0 0 1 34 24H112z" opacity=".9"/>', "cream")
            + line(clock)
            + line('<path class="CLS" d="M112 72v22l14 9"/>')
            + line('<path class="CLS" d="M160 70a52 52 0 0 1 2 46"/>', "ld")
            + line('<path class="CLS" d="M156 112l6 6 5-7"/>', "lt")
            + capsule(78, 140, 30, 11, 12, "clay")
            + dots([(170, 140), (182, 54)]))


def il_course_complete():
    hill = '<path class="CLS" d="M126 146c4-30 26-46 52-46s40 22 40 46z"/>'
    flag = '<path class="CLS" d="M178 98V48"/>'
    banner = '<path class="CLS" d="M178 50h26l-7 10 7 10h-26z"/>'
    path = '<path class="ld" d="M34 140C60 140 64 112 88 112S116 132 140 118"/>'
    pts = [(34, 140), (62, 128), (88, 112), (116, 124)]
    return (ground(rx=100) + obj(hill, "sage") + path
            + "".join(f'<circle class="f-leaf" cx="{x}" cy="{y}" r="5"/>' for x, y in pts)
            + line(flag) + obj(banner, "clay")
            + base(24, 220)
            + dots([(150, 56), (100, 70)]))


def il_low_stock():
    bag = '<path class="CLS" d="M136 100h46l-4 46h-38z"/>'
    return (ground() + base()
            + jar(98, 146, 46, 70, lid_fill="leaf", level=0.22)
            + capsule(92, 134, 18, 8, 15, "sand")
            + capsule(106, 136, 18, 8, -10, "clay")
            + obj(bag, "sand")
            + line('<path class="CLS" d="M148 100v-6a11 11 0 0 1 22 0v6"/>')
            + dots([(66, 70), (176, 66)]))


def il_history():
    out = ground() + line('<path class="CLS" d="M84 40V146"/>')
    rows = [(52, "leaf", 66, "sand"), (78, "leaf", 50, "clay"), (104, "sage", 74, "sand"), (130, "ring", 40, "cream")]
    for y, kind, w, fill in rows:
        if kind == "ring":
            out += f'<circle class="l" cx="84" cy="{y}" r="7"/>'
            out += f'<circle class="f-paper" cx="84" cy="{y}" r="6"/>' + f'<circle class="l" cx="84" cy="{y}" r="6"/>'
        else:
            out += f'<circle class="f-{kind}" cx="84" cy="{y}" r="7"/>'
        bar = f'<rect class="CLS" x="102" y="{y-8}" width="{w}" height="16" rx="8"/>'
        out += obj(bar, fill, dx=3, dy=2)
    return out + sprig(184, 146, flip=True, h=30) + base(60, 200) + dots([(186, 60)])


def il_day_complete():
    sun = '<path class="CLS" d="M70 146a50 50 0 0 1 100 0z"/>'
    moon = 'M0 -13a13 13 0 1 0 12 18a10 10 0 0 1-12-18z'
    return (ground(rx=104) + off(sun, "sand", 0, 0) + line(sun, "lt")
            + line('<path class="CLS" d="M88 128H152M96 136H144"/>', "lt")
            + '<g transform="translate(196 64)">' + obj(f'<path class="CLS" d="{moon}"/>', "clay", dx=3, dy=2) + '</g>'
            + sprig(48, 146) + sprig(198, 146, flip=True, fill="sand", h=26)
            + line('<path class="CLS" d="M56 56c4 0 6-2 6-6 0 4 2 6 6 6-4 0-6 2-6 6 0-4-2-6-6-6zM176 40c3 0 4-1 4-4 0 3 1 4 4 4-3 0-4 1-4 4 0-3-1-4-4-4z"/>', "lt")
            + base(30, 210))


ILLUSTRATIONS = [
    ("symbol", "Символ «Рядом»", il_symbol),
    ("vitamins", "Витамины и капсулы", il_vitamins),
    ("water", "Стакан воды", il_water),
    ("family_care", "Семейная забота", il_family_care),
    ("mother_child", "Мама и ребёнок", il_mother_child),
    ("father_child", "Папа и ребёнок", il_father_child),
    ("family_scene", "Семейная сцена", il_family_scene),
    ("morning", "Утренний ритуал", il_morning),
    ("evening", "Вечерний ритуал", il_evening),
    ("package", "Упаковка витаминов", il_package),
    ("first_aid", "Аптечка", il_first_aid),
    ("reminder", "Напоминание", il_reminder),
    ("calendar", "Календарь", il_calendar),
    ("intake_done", "Приём завершён", il_intake_done),
    ("empty_schedule", "Пустое расписание", il_empty_schedule),
    ("missed", "Пропущенное напоминание", il_missed),
    ("course_complete", "Курс завершён", il_course_complete),
    ("low_stock", "Мало препарата", il_low_stock),
    ("history", "История приёма", il_history),
    ("day_complete", "День завершён", il_day_complete),
]
