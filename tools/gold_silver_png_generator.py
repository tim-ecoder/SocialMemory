#!/usr/bin/env python3
"""Generate 640x480 PNGs made of randomized 3x3 mono-color pixels.

Each "big pixel" is a 3x3 block painted one of two colors: gold or silver.
Gold appears 1.8x as often as silver. No third-party deps — writes PNG by
hand using only the standard library (zlib + struct).
"""

import argparse
import random
import struct
import zlib

# Image geometry
WIDTH = 640
HEIGHT = 480
BLOCK = 3
# Round up so the blocks cover the full image; edge blocks may be clipped
# when WIDTH/HEIGHT are not exact multiples of BLOCK.
COLS = -(-WIDTH // BLOCK)
ROWS = -(-HEIGHT // BLOCK)

# The two colors (R, G, B)
GOLD = (212, 175, 55)     # metallic gold
SILVER = (192, 192, 192)  # metallic silver

# Gold appears 1.8x as often as silver (ratio 1.8 : 1).
GOLD_RATIO = 1.8
P_GOLD = GOLD_RATIO / (GOLD_RATIO + 1.0)


def make_pixel_grid(rng):
    """Return a ROWS x COLS grid, each cell an (r, g, b) tuple.

    Gold blocks occur ~1.8x as often as silver blocks (P_GOLD).
    """
    return [[GOLD if rng.random() < P_GOLD else SILVER
             for _ in range(COLS)] for _ in range(ROWS)]


def grid_to_rgb_rows(grid):
    """Expand the block grid into full-resolution raw RGB scanlines.

    Returns the raw image bytes laid out as PNG filtered scanlines
    (filter type 0 prepended to each row). Edge blocks are clipped so the
    output is exactly WIDTH x HEIGHT even when dimensions aren't multiples
    of BLOCK.
    """
    stride = WIDTH * 3
    raw = bytearray()
    y = 0
    for block_row in grid:
        # Build one full pixel row of bytes, then clip to exactly WIDTH pixels.
        row = bytearray()
        for (r, g, b) in block_row:
            row += bytes((r, g, b)) * BLOCK
        row = row[:stride]
        # Each block row is up to BLOCK scanlines tall; stop at HEIGHT.
        for _ in range(BLOCK):
            if y >= HEIGHT:
                break
            raw.append(0)  # filter type 0 (none)
            raw += row
            y += 1
    return bytes(raw)


def _chunk(tag, data):
    out = struct.pack(">I", len(data)) + tag + data
    crc = zlib.crc32(tag + data) & 0xFFFFFFFF
    return out + struct.pack(">I", crc)


def write_png(path, raw_rgb):
    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", WIDTH, HEIGHT, 8, 2, 0, 0, 0)  # 8-bit, color type 2 (RGB)
    idat = zlib.compress(raw_rgb, 9)
    with open(path, "wb") as f:
        f.write(sig)
        f.write(_chunk(b"IHDR", ihdr))
        f.write(_chunk(b"IDAT", idat))
        f.write(_chunk(b"IEND", b""))


def generate(path, seed=None):
    # With an explicit seed, use a reproducible PRNG. Otherwise draw from the
    # OS entropy source (SystemRandom) for higher-quality, non-deterministic
    # randomization that isn't tied to wall-clock-style seeding.
    rng = random.Random(seed) if seed is not None else random.SystemRandom()
    grid = make_pixel_grid(rng)
    raw = grid_to_rgb_rows(grid)
    write_png(path, raw)
    return path


def main():
    p = argparse.ArgumentParser(description="Generate randomized gold/silver 3x3-pixel PNGs.")
    p.add_argument("-n", "--count", type=int, default=1, help="number of PNGs to generate")
    p.add_argument("-o", "--output", default="goldsilver.png",
                   help="output filename (or prefix when count > 1)")
    p.add_argument("-s", "--seed", type=int, default=None, help="random seed (optional)")
    args = p.parse_args()

    if args.count == 1:
        out = generate(args.output, args.seed)
        print(f"wrote {out}")
    else:
        base = args.output[:-4] if args.output.endswith(".png") else args.output
        for i in range(args.count):
            seed = None if args.seed is None else args.seed + i
            out = generate(f"{base}_{i:03d}.png", seed)
            print(f"wrote {out}")


if __name__ == "__main__":
    main()
