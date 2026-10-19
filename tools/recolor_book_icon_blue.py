"""Recolor the vanilla recipe-book button sprites to a muted blue.

Keeps each pixel's lightness so the bevel shading stays, and lowers saturation
so the button sits on the oak inventory.
"""

from __future__ import annotations

import colorsys
import io
import json
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "common" / "src" / "main" / "resources" / "assets" / "bluerecipebook" / "textures" / "gui"
SPRITES = ("button.png", "button_highlighted.png")
HUE = 212 / 360
SATURATION_SCALE = 0.7
SATURATION_CAP = 0.55


def download_client() -> bytes:
    with urllib.request.urlopen("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json") as response:
        manifest = json.load(response)
    version = next(entry for entry in manifest["versions"] if entry["id"] == "1.21.1")
    with urllib.request.urlopen(version["url"]) as response:
        meta = json.load(response)
    with urllib.request.urlopen(meta["downloads"]["client"]["url"]) as response:
        return response.read()


def recolor(image):
    from PIL import Image

    image = image.convert("RGBA")
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            red, green, blue, alpha = pixels[x, y]
            if alpha == 0:
                continue
            hue, lightness, saturation = colorsys.rgb_to_hls(red / 255, green / 255, blue / 255)
            saturation = min(saturation * SATURATION_SCALE, SATURATION_CAP)
            new_red, new_green, new_blue = colorsys.hls_to_rgb(HUE, lightness, saturation)
            pixels[x, y] = (
                round(new_red * 255),
                round(new_green * 255),
                round(new_blue * 255),
                alpha,
            )
    return image


def main() -> None:
    from PIL import Image

    jar = download_client()
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(io.BytesIO(jar)) as archive:
        names = sorted(
            name for name in archive.namelist() if "textures/gui/sprites/recipe_book/" in name
        )
        print("sprites:")
        for name in names:
            print(" ", name)
        book = "assets/minecraft/textures/gui/recipe_book.png"
        if book in archive.namelist():
            with archive.open(book) as handle:
                book_image = Image.open(handle)
                book_image.load()
            print(f"recipe_book.png {book_image.size}")
        for sprite in SPRITES:
            path = f"assets/minecraft/textures/gui/sprites/recipe_book/{sprite}"
            with archive.open(path) as handle:
                image = Image.open(handle)
                image.load()
            print(f"{sprite} {image.size} mode={image.mode}")
            recolor(image).save(OUT_DIR / f"recipe_book_{sprite}")


if __name__ == "__main__":
    main()
