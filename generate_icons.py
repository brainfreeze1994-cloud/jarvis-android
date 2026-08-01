"""
Generates HENRY gold-triangle PNG icons for all Android mipmap densities.
Runs during GitHub Actions before the Gradle build.
"""
import os, math
from PIL import Image, ImageDraw

SIZES = {
    'mipmap-mdpi':    48,
    'mipmap-hdpi':    72,
    'mipmap-xhdpi':   96,
    'mipmap-xxhdpi':  144,
    'mipmap-xxxhdpi': 192,
}

BG    = (10, 10, 10, 255)        # #0a0a0a
DARK  = (18, 15,  5, 255)        # dark circle fill
GOLD  = (201, 168, 76, 255)      # #c9a84c
GOLDA = (201, 168, 76, 140)      # gold semi-transparent

def draw_icon(size):
    img = Image.new('RGBA', (size, size), BG)
    d = ImageDraw.Draw(img)
    cx = cy = size / 2.0

    r_outer = size * 0.43
    r_inner = size * 0.41
    ring_w  = max(1, int(size / 36))
    bw      = max(1, int(size / 44))   # bracket stroke width

    # Dark filled circle
    d.ellipse([cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer], fill=DARK)

    # Gold outer ring
    d.ellipse(
        [cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer],
        outline=GOLD, width=ring_w
    )

    # Corner brackets (L-shapes at 4 corners of a square frame)
    sq = r_outer * 1.30          # half-side of the bracket square
    bl = sq * 0.30               # length of each bracket arm
    pts = [
        (cx - sq, cy - sq,  bl, 0,  0,  bl),   # top-left
        (cx + sq, cy - sq, -bl, 0,  0,  bl),   # top-right
        (cx - sq, cy + sq,  bl, 0,  0, -bl),   # bottom-left
        (cx + sq, cy + sq, -bl, 0,  0, -bl),   # bottom-right
    ]
    for (ox, oy, dxH, dyH, dxV, dyV) in pts:
        d.line([(ox, oy), (ox + dxH, oy + dyH)], fill=GOLD, width=bw)
        d.line([(ox, oy), (ox + dxV, oy + dyV)], fill=GOLD, width=bw)

    # Gold equilateral triangle pointing UP in center
    tri_h = r_outer * 0.60
    tri_w = tri_h * 1.00
    top   = (cx, cy - tri_h * 0.62)
    bL    = (cx - tri_w * 0.50, cy + tri_h * 0.38)
    bR    = (cx + tri_w * 0.50, cy + tri_h * 0.38)
    d.polygon([top, bL, bR], outline=GOLD, width=bw)

    # Small gold center dot
    dot_r = size * 0.030
    d.ellipse([cx - dot_r, cy - dot_r, cx + dot_r, cy + dot_r], fill=GOLD)

    return img

base = 'app/src/main/res'
for folder, size in SIZES.items():
    path = os.path.join(base, folder)
    os.makedirs(path, exist_ok=True)

    icon = draw_icon(size)
    icon.save(os.path.join(path, 'ic_launcher.png'))

    # Round icon — circle mask
    round_icon = draw_icon(size)
    mask = Image.new('L', (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    round_bg = Image.new('RGBA', (size, size), BG)
    round_bg.paste(round_icon, mask=round_icon.split()[3])
    round_bg.save(os.path.join(path, 'ic_launcher_round.png'))
    print(f'Generated {folder} ({size}x{size})')

print('All icons generated successfully.')
