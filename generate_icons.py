import os, math
from PIL import Image, ImageDraw

SIZES = {
    'mipmap-mdpi':    48,
    'mipmap-hdpi':    72,
    'mipmap-xhdpi':   96,
    'mipmap-xxhdpi':  144,
    'mipmap-xxxhdpi': 192,
}

BG    = (7,  13, 26,  255)
BLUE1 = (29, 78, 216, 255)
BLUE2 = (59, 130, 246, 200)
BLUE3 = (147, 197, 253, 180)
WHITE = (191, 219, 254, 230)
RING1 = (30, 58, 95, 255)

def draw_icon(size):
    img = Image.new('RGBA', (size, size), BG)
    d = ImageDraw.Draw(img)
    cx = cy = size / 2
    scale = size / 108

    def circle(r, fill=None, outline=None, width=1):
        x0, y0 = cx - r, cy - r
        x1, y1 = cx + r, cy + r
        d.ellipse([x0, y0, x1, y1], fill=fill, outline=outline, width=max(1, int(width * scale)))

    circle(44 * scale, outline=RING1, width=2)
    circle(36 * scale, outline=(*BLUE2[:3], 150), width=1)
    circle(22 * scale, fill=BLUE1)
    circle(15 * scale, outline=(*BLUE3[:3], 180), width=1)
    circle(5  * scale, fill=WHITE)

    for angle_deg in [90, 210, 330]:
        rad = math.radians(angle_deg)
        x_end = cx + math.cos(rad) * 44 * scale
        y_end = cy - math.sin(rad) * 44 * scale
        d.line([(cx, cy), (x_end, y_end)], fill=(*BLUE2[:3], 80), width=max(1, int(scale)))

    return img

base = 'app/src/main/res'
for folder, size in SIZES.items():
    path = os.path.join(base, folder)
    os.makedirs(path, exist_ok=True)
    icon = draw_icon(size)
    icon.save(os.path.join(path, 'ic_launcher.png'))
    round_icon = draw_icon(size)
    mask = Image.new('L', (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size-1, size-1], fill=255)
    round_icon.putalpha(mask)
    round_bg = Image.new('RGBA', (size, size), BG)
    round_bg.paste(round_icon, mask=round_icon.split()[3])
    round_bg.save(os.path.join(path, 'ic_launcher_round.png'))
    print(f'Generated {folder} ({size}x{size})')

print('All icons generated.')
