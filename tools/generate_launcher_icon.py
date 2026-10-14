from pathlib import Path
from urllib.request import Request, urlopen
from PIL import Image

ROOT = Path("app/src/main/res")
SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
LOGO_URL = "https://animejs.com/assets/images/favicon.png"
TEMP_LOGO = Path("/tmp/animejs-favicon.png")

# Fetch the site's actual Anime.js logo mark rather than drawing a substitute.
request = Request(LOGO_URL, headers={"User-Agent": "Mozilla/5.0"})
with urlopen(request, timeout=30) as response:
    TEMP_LOGO.write_bytes(response.read())

source = Image.open(TEMP_LOGO).convert("RGBA")
side = max(source.width, source.height)
canvas = Image.new("RGBA", (side, side), (255, 255, 255, 255))
source.thumbnail((int(side * 0.84), int(side * 0.84)), Image.Resampling.LANCZOS)
canvas.alpha_composite(source, ((side - source.width) // 2, (side - source.height) // 2))

# Density-specific launcher icons.
for folder_name, size in SIZES.items():
    folder = ROOT / folder_name
    folder.mkdir(parents=True, exist_ok=True)
    icon = canvas.resize((size, size), Image.Resampling.LANCZOS)
    icon.save(folder / "ic_launcher.webp", "WEBP", quality=96, method=6)
    icon.save(folder / "ic_launcher_round.webp", "WEBP", quality=96, method=6)

# Adaptive icon foreground keeps the official mark centered within Android's safe zone.
foreground_dir = ROOT / "drawable"
foreground_dir.mkdir(parents=True, exist_ok=True)
fg = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
mark = canvas.resize((360, 360), Image.Resampling.LANCZOS)
fg.alpha_composite(mark, ((432 - 360) // 2, (432 - 360) // 2))
fg.save(foreground_dir / "animejs_icon_foreground.png", optimize=True)

# Monochrome version for Android themed icons.
mono = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
mark_l = mark.convert("L")
white_mark = Image.new("RGBA", mark.size, (255, 255, 255, 0))
white_mark.putalpha(mark_l)
mono.alpha_composite(white_mark, ((432 - 360) // 2, (432 - 360) // 2))
mono.save(foreground_dir / "animejs_icon_monochrome.png", optimize=True)

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

values_dir = ROOT / "values"
values_dir.mkdir(parents=True, exist_ok=True)
(values_dir / "animejs_icon_background.xml").write_text(
    '''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="animejs_icon_background">#FFFFFF</color>
</resources>
''',
    encoding="utf-8",
)
print(f"Generated Anime.js launcher icons from {LOGO_URL}")
