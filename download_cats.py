"""Decode the embedded cat meme images (base64 JPEGs in cats.json) and save
them as JPEGs under app/src/main/res/drawable-nodpi/.
Run by CI before the Gradle build (the images are not committed as binaries).
Requires: pip install pillow
"""
import base64
import io
import json
import os

from PIL import Image

OUT = os.path.join("app", "src", "main", "res", "drawable-nodpi")
RES = os.path.join("app", "src", "main", "res")


def main():
    with open("cats.json") as f:
        payload = json.load(f)
    cats = payload["cats"]
    assert payload["count"] == len(cats)
    os.makedirs(OUT, exist_ok=True)
    for i, b64 in enumerate(cats, start=1):
        dest = os.path.join(OUT, "cat_%02d.jpg" % i)
        data = base64.b64decode(b64)
        with open(dest, "wb") as f:
            f.write(data)
        with Image.open(dest) as im:
            if min(im.size) < 120:
                raise ValueError("%s too small: %s" % (dest, im.size))
        print("  -> %s OK (%d bytes)" % (dest, len(data)))
    n = len([f for f in os.listdir(OUT) if f.endswith(".jpg")])
    if n != len(cats):
        raise SystemExit("expected %d cat images, got %d" % (len(cats), n))
    print("all %d cat images ready" % n)
    if "background" in payload:
        dest = os.path.join(OUT, "bg.jpg")
        with open(dest, "wb") as f:
            f.write(base64.b64decode(payload["background"]))
        with Image.open(dest) as im:
            if min(im.size) < 360:
                raise ValueError("%s too small: %s" % (dest, im.size))
        print("  -> %s OK (background)" % dest)
    if "icon" in payload:
        # Icon launcher: tao 5 ban theo mat do tu anh vuong goc.
        with Image.open(io.BytesIO(base64.b64decode(payload["icon"]))) as icon:
            icon = icon.convert("RGB")
            if icon.size[0] != icon.size[1] or icon.size[0] < 192:
                raise ValueError("icon must be square, >= 192px: %s" % (icon.size,))
            for density, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96),
                                  ("xxhdpi", 144), ("xxxhdpi", 192)]:
                folder = os.path.join(RES, "mipmap-" + density)
                os.makedirs(folder, exist_ok=True)
                icon.resize((size, size), Image.LANCZOS).save(
                    os.path.join(folder, "ic_launcher.png"), "PNG")
        print("  -> launcher icon OK (5 densities)")


if __name__ == "__main__":
    main()
