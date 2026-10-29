"""Regenerate the NotesTaking App launcher and loading artwork from one vector geometry.
Requires Pillow only for asset regeneration; Android builds use committed resources.
"""
from __future__ import annotations

from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app" / "src" / "main" / "res"
NAVY = "#17243B"
PAPER = "#F8F7F3"
FOLD = "#D1DBF0"
RULE = "#BAC5D8"
INK = "#526DDF"

PAGE = "M33,24 H63 L79,40 V79 Q79,84 74,84 H33 Q29,84 29,80 V28 Q29,24 33,24 Z"
FOLD_PATH = "M63,24 V40 H79 Z"
RULES = "M39,47 H64 M39,55 H57"
INK_PATH = "M39,71 C44,62 50,78 57,70 C63,62 67,60 72,63"

SVG = f"""<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" viewBox="0 0 108 108">
  <title>NotesTaking App — unfolding paper and ink trail</title>
  <path d="{PAGE}" fill="{PAPER}"/>
  <path d="{FOLD_PATH}" fill="{FOLD}"/>
  <path d="{RULES}" fill="none" stroke="{RULE}" stroke-width="2.7" stroke-linecap="round"/>
  <path d="{INK_PATH}" fill="none" stroke="{INK}" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
"""

VECTOR_HEAD = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
"""
VECTOR_MARK = (
    VECTOR_HEAD
    + f'    <path android:fillColor="{PAPER}" android:pathData="{PAGE}"/>\n'
    + f'    <path android:fillColor="{FOLD}" android:pathData="{FOLD_PATH}"/>\n'
    + f'    <path android:fillColor="#00000000" android:strokeColor="{RULE}" '
      f'android:strokeWidth="2.7" android:strokeLineCap="round" android:pathData="{RULES}"/>\n'
    + f'    <path android:fillColor="#00000000" android:strokeColor="{INK}" '
      f'android:strokeWidth="5" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{INK_PATH}"/>\n'
    + "</vector>\n"
)
VECTOR_MONO = (
    VECTOR_HEAD
    + f'    <path android:fillColor="#00000000" android:strokeColor="#FFFFFFFF" '
      f'android:strokeWidth="4.5" android:strokeLineJoin="round" android:pathData="{PAGE}"/>\n'
    + f'    <path android:fillColor="#00000000" android:strokeColor="#FFFFFFFF" '
      f'android:strokeWidth="4" android:pathData="M63,24 V40 H79"/>\n'
    + f'    <path android:fillColor="#00000000" android:strokeColor="#FFFFFFFF" '
      f'android:strokeWidth="4.8" android:strokeLineCap="round" android:pathData="{INK_PATH}"/>\n'
    + "</vector>\n"
)


def curve_point(a: tuple[float, float], b: tuple[float, float],
                c: tuple[float, float], d: tuple[float, float], t: float) -> tuple[float, float]:
    m = 1 - t
    return (
        m**3 * a[0] + 3*m*m*t*b[0] + 3*m*t*t*c[0] + t**3*d[0],
        m**3 * a[1] + 3*m*m*t*b[1] + 3*m*t*t*c[1] + t**3*d[1],
    )


def draw_mark(size: int, *, background: bool, progress: float = 1.0) -> Image.Image:
    # Supersampling makes fine pen strokes sharp even on the legacy 48px launcher.
    high = size * (4 if size <= 192 else 2)
    scale = high / 108.0
    image = Image.new("RGBA", (high, high), NAVY if background else (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    def pt(x: float, y: float) -> tuple[int, int]:
        return (round(x * scale), round(y * scale))
    def quadratic(p0, p1, p2):
        for n in range(1, 13):
            t = n / 12
            yield ((1-t)**2*p0[0]+2*(1-t)*t*p1[0]+t*t*p2[0],
                   (1-t)**2*p0[1]+2*(1-t)*t*p1[1]+t*t*p2[1])

    sheet = [(33,24),(63,24),(79,40),(79,79)]
    sheet.extend(quadratic((79,79),(79,84),(74,84)))
    sheet.append((33,84))
    sheet.extend(quadratic((33,84),(29,84),(29,80)))
    sheet.append((29,28))
    sheet.extend(quadratic((29,28),(29,24),(33,24)))
    draw.polygon([pt(*p) for p in sheet], fill=PAPER)
    draw.polygon([pt(63,24), pt(63,40), pt(79,40)], fill=FOLD)
    for x1,y1,x2,y2 in ((39,47,64,47),(39,55,57,55)):
        draw.line((pt(x1,y1),pt(x2,y2)), fill=RULE, width=max(1,round(2.7*scale)), joint="curve")
        r=1.35*scale
        for x in (x1,x2):
            cx,cy=pt(x,y1)
            draw.ellipse((round(cx-r),round(cy-r),round(cx+r),round(cy+r)),fill=RULE)
    progress = min(1.0, max(0.0, progress))
    if progress:
        samples = [
            curve_point((39,71),(44,62),(50,78),(57,70),i/80) for i in range(81)
        ] + [
            curve_point((57,70),(63,62),(67,60),(72,63),i/80) for i in range(1,81)
        ]
        count = max(2, round((len(samples)-1)*progress)+1)
        points = [pt(*p) for p in samples[:count]]
        width = max(1,round(5*scale))
        draw.line(points, fill=INK, width=width, joint="curve")
        r=width/2
        for x,y in (points[0],points[-1]):
            draw.ellipse((round(x-r),round(y-r),round(x+r),round(y+r)),fill=INK)
    return image.resize((size,size),Image.Resampling.LANCZOS)


def save_icon(target: Path, image: Image.Image) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target, optimize=True)
    with Image.open(target) as check:
        assert check.size == image.size and check.mode == "RGBA", target


def main() -> None:
    vector_dir = RES / "drawable"
    (vector_dir / "ic_brand_mark.xml").write_text(VECTOR_MARK, encoding="utf-8")
    (vector_dir / "ic_launcher_monochrome.xml").write_text(VECTOR_MONO, encoding="utf-8")
    brand = ROOT / "design" / "brand"
    brand.mkdir(parents=True, exist_ok=True)
    (brand / "notestaking-symbol.svg").write_text(SVG, encoding="utf-8")

    for density, edge in (("mdpi",48),("hdpi",72),("xhdpi",96),("xxhdpi",144),("xxxhdpi",192)):
        folder = RES / f"mipmap-{density}"
        icon = draw_mark(edge, background=True)
        save_icon(folder / "ic_launcher.png", icon)
        # For pre-adaptive launchers only. Modern devices render the drawable vector directly.
        save_icon(folder / "ic_launcher_foreground.png", draw_mark(round(edge*2.25), background=False))
        round_icon = icon.copy()
        mask = Image.new("L", icon.size, 0)
        m = ImageDraw.Draw(mask)
        m.ellipse((0,0,edge-1,edge-1), fill=255)
        round_icon.putalpha(mask)
        save_icon(folder / "ic_launcher_round.png", round_icon)

    # Keep the app's existing 24-frame loading contract and animate the ink stroke.
    for frame in range(24):
        progress = frame / 23
        save_icon(RES / "drawable-nodpi" / f"notestaking_frame_{frame:02d}.png",
                  draw_mark(540, background=True, progress=progress))
    print("Generated SVG, two Android vectors, 15 launcher PNGs and 24 loader frames")


if __name__ == "__main__":
    main()
