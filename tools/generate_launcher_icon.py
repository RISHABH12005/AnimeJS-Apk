from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path("app/src/main/res")
SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
BG = (17, 18, 22, 255)
FG = (248, 247, 246, 255)
ACCENT = (244, 79, 101, 255)

def make_icon(size: int) -> Image.Image:
    scale = size / 432
    image = Image.new("RGBA", (size, size), BG)
    draw = ImageDraw.Draw(image)
    # Strong, simple lowercase "a" mark with Anime.js's coral accent.
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf",
    ]
    font_path = next((p for p in candidates if Path(p).exists()), None)
    font = ImageFont.truetype(font_path, max(1, int(270 * scale))) if font_path else ImageFont.load_default()
    bbox = draw.textbbox((0, 0), "a", font=font)
    text_w, text_h = bbox[2] - bbox[0], bbox[3] - bbox[1]
    x = (size - text_w) / 2 - bbox[0] - 5 * scale
    y = (size - text_h) / 2 - bbox[1] - 7 * scale
    draw.text((x, y), "a", font=font, fill=FG, stroke_width=0)
    # Small coral square echoes the site's red accent and remains visible at launcher size.
    accent = max(2, int(42 * scale))
    left = int(size * 0.67)
    top = int(size * 0.18)
    draw.rounded_rectangle(
        (left, top, left + accent, top + accent),
        radius=max(1, int(4 * scale)),
        fill=ACCENT
    )
    return image

for folder_name, size in SIZES.items():
    folder = ROOT / folder_name
    folder.mkdir(parents=True, exist_ok=True)
    icon = make_icon(size)
    icon.save(folder / "ic_launcher.webp", "WEBP", quality=96, method=6)
    icon.save(folder / "ic_launcher_round.webp", "WEBP", quality=96, method=6)

# Transparent foreground asset for Android adaptive icons.
foreground_dir = ROOT / "drawable"
foreground_dir.mkdir(parents=True, exist_ok=True)
size = 432
fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
draw = ImageDraw.Draw(fg)
candidates = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf",
]
font_path = next((p for p in candidates if Path(p).exists()), None)
font = ImageFont.truetype(font_path, 270) if font_path else ImageFont.load_default()
bbox = draw.textbbox((0, 0), "a", font=font)
tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
draw.text(((size - tw) / 2 - bbox[0] - 5, (size - th) / 2 - bbox[1] - 7), "a", font=font, fill=FG)
draw.rounded_rectangle((290, 76, 332, 118), radius=4, fill=ACCENT)
fg.save(foreground_dir / "animejs_icon_foreground.png", optimize=True)

adaptive_dir = ROOT / "mipmap-anydpi-v26"
adaptive_dir.mkdir(parents=True, exist_ok=True)
xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/animejs_icon_background" />
    <foreground android:drawable="@drawable/animejs_icon_foreground" />
    <monochrome android:drawable="@drawable/animejs_icon_monochrome" />
</adaptive-icon>
'''
(adaptive_dir / "ic_launcher.xml").write_text(xml, encoding="utf-8")
(adaptive_dir / "ic_launcher_round.xml").write_text(xml, encoding="utf-8")
# Monochrome adaptive icon: same mark in white without the coral accent.
mono = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
mono_draw = ImageDraw.Draw(mono)
mono_bbox = mono_draw.textbbox((0, 0), "a", font=font)
mtw, mth = mono_bbox[2] - mono_bbox[0], mono_bbox[3] - mono_bbox[1]
mono_draw.text(((432 - mtw) / 2 - mono_bbox[0] - 5, (432 - mth) / 2 - mono_bbox[1] - 7), "a", font=font, fill=(255, 255, 255, 255))
mono.save(foreground_dir / "animejs_icon_monochrome.png", optimize=True)
values_dir = ROOT / "values"
values_dir.mkdir(parents=True, exist_ok=True)
(values_dir / "animejs_icon_background.xml").write_text('''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="animejs_icon_background">#111216</color>
</resources>
''', encoding="utf-8")
