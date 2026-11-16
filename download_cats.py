"""Decode the embedded cat meme images (base64 JPEGs in cats.json) and save
them as JPEGs under app/src/main/res/drawable-nodpi/.
Run by CI before the Gradle build (the images are not committed as binaries).
Requires: pip install pillow
"""
import base64
import json
import os

from PIL import Image

OUT = os.path.join("app", "src", "main", "res", "drawable-nodpi")


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


if __name__ == "__main__":
    main()
