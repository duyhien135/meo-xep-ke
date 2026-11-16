"""Download the 30 cat meme images for Meo Xep Ke and save them as
transparent PNGs under app/src/main/res/drawable-nodpi/.
Run by CI before the Gradle build (the PNGs are not committed).
Requires: pip install pillow requests
"""
import os
import sys
from PIL import Image
import requests

URLS = [
    "https://static.vecteezy.com/system/resources/thumbnails/054/648/920/small/beautiful-cat-holding-a-empty-blank-sign-board-isolated-on-transparent-background-free-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/045/847/336/small/angry-cat-meme-sticker-illustration-free-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/075/521/526/small/expressive-grey-domestic-cat-with-wide-open-mouth-and-excited-expression-isolated-on-transparent-background-png.png",
    "https://images.bestpng.com/library/animals/b7bae92f-b82b-40db-8de5-ef809f614f63.png",
    "https://static.vecteezy.com/system/resources/previews/009/400/932/original/kitty-cat-clipart-design-illustration-free-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/027/187/563/small/sticker-cute-cat-free-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/027/187/650/small/sticker-cute-cat-free-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/027/183/114/small/sticker-cute-cat-free-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/060/468/516/small/funny-face-sticking-tongue-cat-meme-sticker-transparent-t-shirt-cute-illustration-free-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/027/187/635/small/sticker-cute-cat-free-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/045/069/426/small/sticker-of-a-funny-cartoon-cat-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/027/183/111/small/sticker-cute-cat-free-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/044/950/121/small/sticker-of-a-funny-cartoon-cat-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/039/880/700/small/sticker-of-a-cartoon-cat-face-png.png",
    "https://pub-static.fotor.com/assets/stickers/169e0491-e7fa-4021-8dc0-8a245878df86_medium_thumb.jpg",
    "https://pub-static.fotor.com/assets/stickers/4971897a-0639-4f1b-bb21-ef78006b43f6_medium_thumb.jpg",
    "https://www.pngitem.com/pimgs/b/663-6633041_cartoon-hd-png-download.png",
    "https://static.vecteezy.com/system/resources/thumbnails/040/265/963/small/sticker-of-a-cartoon-cat-face-png.png",
    "https://img.pikbest.com/origin/06/48/44/15apIkbEsTfPH.png!f305cw",
    "https://static.vecteezy.com/system/resources/thumbnails/045/648/579/small/crying-cat-meme-sticker-illustration-free-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/048/024/253/small/cat-cry-sticker-illustration-on-transparent-background-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/044/876/797/small/cat-in-jar-sticker-free-png.png",
    "https://fastpng.com/images/file/cat-meme-48xxp9ghjmykl961.png",
    "https://static.vecteezy.com/system/resources/thumbnails/048/024/094/small/cat-angry-sticker-illustration-on-transparent-background-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/048/783/992/small/swag-orange-cat-meme-transparent-background-cute-illustration-png.png",
    "https://images.squarespace-cdn.com/content/v1/5f2256f279dd6b12ba8e25ee/1656013616457-EKNJ1UYXCKXW2UNEYKN2/CatAstro_nobkgrd.png?format=500w",
    "https://tgstick.ru/static/stickers/e12/603/AgADOYAAAhPO0Uo_preview.webp",
    "https://static.vecteezy.com/system/resources/thumbnails/060/468/523/small/funny-face-orange-cat-meme-sticker-transparent-t-shirt-cute-illustration-free-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/055/030/508/small/silly-white-cat-meme-sticker-t-shirt-transparent-cute-illustration-free-png.png",
    "https://static.vecteezy.com/system/resources/thumbnails/044/876/796/small/white-cat-meme-sticker-tshirt-illustration-free-png.png",
]

OUT = os.path.join("app", "src", "main", "res", "drawable-nodpi")
HEADERS = {
    "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36",
    "Referer": "https://www.google.com/",
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for i, url in enumerate(URLS, start=1):
        dest = os.path.join(OUT, "cat_%02d.png" % i)
        print("downloading %s ..." % url[:80])
        r = requests.get(url, headers=HEADERS, timeout=30)
        r.raise_for_status()
        with open(dest + ".tmp", "wb") as f:
            f.write(r.content)
        try:
            with Image.open(dest + ".tmp") as im:
                w, h = im.size
                if min(w, h) < 120:
                    raise ValueError("too small: %sx%s" % (w, h))
                rgba = im.convert("RGBA")
                alpha = rgba.getchannel("A").getdata()
                frac = sum(1 for a in alpha if a == 0) / len(alpha)
                if frac < 0.03 or frac > 0.95:
                    raise ValueError("bad transparency fraction: %.2f" % frac)
                bbox = rgba.getbbox()
                crop = rgba.crop(bbox) if bbox else rgba
                if max(crop.size) > 512:
                    crop.thumbnail((512, 512), Image.LANCZOS)
                crop.save(dest, "PNG")
        finally:
            if os.path.exists(dest + ".tmp"):
                os.remove(dest + ".tmp")
        print("  -> %s OK" % dest)
    n = len([f for f in os.listdir(OUT) if f.endswith(".png")])
    if n != 30:
        raise SystemExit("expected 30 cat PNGs, got %d" % n)
    print("all 30 cat images ready")


if __name__ == "__main__":
    main()
