#!/usr/bin/env python3
"""Tests for pixel_png_generator.

Verifies geometry, that only the two intended colors appear, that each
8x8 block is truly mono-color, randomness, determinism via seed, and that
the PNG decodes back to the expected pixels.
"""

import os
import struct
import tempfile
import zlib

import pixel_png_generator as gen


def decode_png(path):
    """Minimal PNG decoder for our own 8-bit RGB, filter-0 files.

    Returns (width, height, pixels) where pixels[y][x] = (r, g, b).
    """
    with open(path, "rb") as f:
        data = f.read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "bad signature"

    pos = 8
    width = height = None
    idat = bytearray()
    while pos < len(data):
        (length,) = struct.unpack(">I", data[pos:pos + 4])
        tag = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        crc = struct.unpack(">I", data[pos + 8 + length:pos + 12 + length])[0]
        assert crc == (zlib.crc32(tag + chunk) & 0xFFFFFFFF), f"bad CRC for {tag}"
        if tag == b"IHDR":
            width, height, depth, ctype = struct.unpack(">IIBB", chunk[:10])
            assert (depth, ctype) == (8, 2), "expected 8-bit RGB"
        elif tag == b"IDAT":
            idat += chunk
        elif tag == b"IEND":
            break
        pos += 12 + length

    raw = zlib.decompress(bytes(idat))
    stride = width * 3
    pixels = []
    p = 0
    prev = bytearray(stride)
    for _ in range(height):
        ftype = raw[p]
        p += 1
        line = bytearray(raw[p:p + stride])
        p += stride
        # We only ever write filter type 0, but support up/none for safety.
        assert ftype == 0, f"unexpected filter type {ftype}"
        row = [(line[i], line[i + 1], line[i + 2]) for i in range(0, stride, 3)]
        pixels.append(row)
        prev = line
    return width, height, pixels


def test_basic_properties():
    with tempfile.TemporaryDirectory() as d:
        path = os.path.join(d, "out.png")
        gen.generate(path, seed=42)
        w, h, px = decode_png(path)

        assert (w, h) == (gen.WIDTH, gen.HEIGHT), f"wrong dims {(w, h)}"

        # Allowed green range for blue blocks given the +/- jitter.
        g_lo = gen._clamp8(gen.BLUE[1] * (1.0 - gen.GREEN_JITTER))
        g_hi = gen._clamp8(gen.BLUE[1] * (1.0 + gen.GREEN_JITTER))

        saw_blue = saw_brown = False
        green_values = set()
        # Check every block is mono-color and is a valid brown or blue variant.
        for br in range(gen.ROWS):
            for bc in range(gen.COLS):
                colors_in_block = set()
                for dy in range(gen.BLOCK):
                    for dx in range(gen.BLOCK):
                        c = px[br * gen.BLOCK + dy][bc * gen.BLOCK + dx]
                        colors_in_block.add(c)
                assert len(colors_in_block) == 1, f"block {(br, bc)} not mono: {colors_in_block}"
                r, g, b = colors_in_block.pop()
                if (r, g, b) == gen.BROWN:
                    saw_brown = True
                else:
                    # A blue variant: red/blue fixed, green within jitter range.
                    assert (r, b) == (gen.BLUE[0], gen.BLUE[2]), f"unexpected color {(r, g, b)}"
                    assert g_lo <= g <= g_hi, f"green {g} outside [{g_lo}, {g_hi}]"
                    saw_blue = True
                    green_values.add(g)

        assert saw_blue and saw_brown, "both blue and brown should appear"
        assert len(green_values) > 1, "blue green channel should actually vary"
        print("test_basic_properties: OK")


def test_determinism():
    with tempfile.TemporaryDirectory() as d:
        a = os.path.join(d, "a.png")
        b = os.path.join(d, "b.png")
        gen.generate(a, seed=7)
        gen.generate(b, seed=7)
        with open(a, "rb") as fa, open(b, "rb") as fb:
            assert fa.read() == fb.read(), "same seed should produce identical files"
        print("test_determinism: OK")


def test_randomness_differs():
    with tempfile.TemporaryDirectory() as d:
        a = os.path.join(d, "a.png")
        b = os.path.join(d, "b.png")
        gen.generate(a, seed=1)
        gen.generate(b, seed=2)
        with open(a, "rb") as fa, open(b, "rb") as fb:
            assert fa.read() != fb.read(), "different seeds should differ"
        print("test_randomness_differs: OK")


if __name__ == "__main__":
    test_basic_properties()
    test_determinism()
    test_randomness_differs()
    print("\nAll tests passed.")
