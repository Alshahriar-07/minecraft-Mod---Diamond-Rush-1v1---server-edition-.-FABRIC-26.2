#!/usr/bin/env python3
"""Generates Diamond Rush's original SVG artwork.

The design language is deliberate: dark Minecraft-like panels, diamond cyan
accents, green READY states and gold champion effects. All lettering is drawn
from a hand-written 5x7 pixel font, so no external fonts or assets are needed.
"""
from __future__ import annotations

import os

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "svg")

FONT = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    "C": ["01111", "10000", "10000", "10000", "10000", "10000", "01111"],
    "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
    "G": ["01111", "10000", "10000", "10111", "10001", "10001", "01111"],
    "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
    "J": ["00111", "00010", "00010", "00010", "10010", "10010", "01100"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "Q": ["01110", "10001", "10001", "10001", "10101", "10010", "01101"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    "V": ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
    "W": ["10001", "10001", "10001", "10101", "10101", "11011", "10001"],
    "X": ["10001", "10001", "01010", "00100", "01010", "10001", "10001"],
    "Y": ["10001", "10001", "01010", "00100", "00100", "00100", "00100"],
    "Z": ["11111", "00001", "00010", "00100", "01000", "10000", "11111"],
    "0": ["01110", "10001", "10011", "10101", "11001", "10001", "01110"],
    "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
    "2": ["01110", "10001", "00001", "00010", "00100", "01000", "11111"],
    "3": ["11111", "00010", "00100", "00010", "00001", "10001", "01110"],
    "4": ["00010", "00110", "01010", "10010", "11111", "00010", "00010"],
    "5": ["11111", "10000", "11110", "00001", "00001", "10001", "01110"],
    "6": ["00110", "01000", "10000", "11110", "10001", "10001", "01110"],
    "7": ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
    "8": ["01110", "10001", "10001", "01110", "10001", "10001", "01110"],
    "9": ["01110", "10001", "10001", "01111", "00001", "00010", "01100"],
    " ": ["00000"] * 7,
    "-": ["00000", "00000", "00000", "11111", "00000", "00000", "00000"],
    "!": ["00100", "00100", "00100", "00100", "00100", "00000", "00100"],
    ".": ["00000", "00000", "00000", "00000", "00000", "00000", "00100"],
}

CYAN = "#3FE0F5"
CYAN_DARK = "#1E93A8"
GREEN = "#57E86B"
GREEN_DARK = "#1F7A34"
GOLD = "#FFC542"
GOLD_DARK = "#8A5A0F"
WHITE = "#EDF6FA"
GREY = "#8FA3B0"
RED = "#FF6B6B"


def text_width(text: str, scale: int) -> int:
    return max(0, len(text) * 6 - 1) * scale


def pixel_text(text: str, x: float, y: float, scale: int, color: str, opacity: float = 1.0) -> str:
    """Renders text with a 5x7 pixel font, merging horizontal runs into rects."""
    parts = []
    cursor = 0
    for char in text.upper():
        glyph = FONT.get(char)
        if glyph is None:
            cursor += 6
            continue
        for row_index, row in enumerate(glyph):
            run_start = None
            for column in range(6):
                filled = column < 5 and row[column] == "1"
                if filled and run_start is None:
                    run_start = column
                elif not filled and run_start is not None:
                    length = column - run_start
                    parts.append(
                        f'<rect x="{x + (cursor + run_start) * scale:.1f}" '
                        f'y="{y + row_index * scale:.1f}" '
                        f'width="{length * scale}" height="{scale}" fill="{color}" opacity="{opacity}"/>'
                    )
                    run_start = None
        cursor += 6
    return "".join(parts)


def header(width: int, height: int, defs: str) -> str:
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
            f'viewBox="0 0 {width} {height}">\n<defs>{defs}</defs>\n')


def diamond(cx: float, cy: float, size: float, fill: str, highlight: str = "#BFF8FF",
            stroke: str = "#0A2A33") -> str:
    """An isometric Minecraft-style gem."""
    half = size / 2
    return (
        f'<g stroke="{stroke}" stroke-width="{max(1.0, size / 14):.1f}" stroke-linejoin="round">'
        f'<polygon points="{cx},{cy - half} {cx + half},{cy - half * 0.15} {cx},{cy + half} '
        f'{cx - half},{cy - half * 0.15}" fill="{fill}"/>'
        f'<polygon points="{cx},{cy - half} {cx + half},{cy - half * 0.15} {cx},{cy - half * 0.1}" '
        f'fill="{highlight}" opacity="0.55" stroke="none"/>'
        f'<polygon points="{cx - half},{cy - half * 0.15} {cx + half},{cy - half * 0.15} {cx},{cy + half}" '
        f'fill="none" opacity="0.35"/>'
        f'</g>'
    )


def button(name: str, label: str, base: str, light: str, dark: str, text_color: str,
           hover: bool = False) -> None:
    width, height = 220, 28
    glow = (f'<path d="M2,2 H218 L218,26 H2 Z" fill="none" stroke="{light}" stroke-width="4" '
            f'opacity="0.35"/>') if hover else ""
    defs = (
        f'<linearGradient id="fill" x1="0" y1="0" x2="0" y2="1">'
        f'<stop offset="0" stop-color="{light if hover else base}"/>'
        f'<stop offset="1" stop-color="{base if hover else dark}"/></linearGradient>'
    )
    body = (
        header(width, height, defs)
        + f'<path d="M4,1 H216 L219,4 V24 L216,27 H4 L1,24 V4 Z" fill="url(#fill)" '
          f'stroke="{dark}" stroke-width="1.5"/>'
        # blocky bevel
        + f'<path d="M5,3 H215 L216,4" fill="none" stroke="{light}" stroke-width="1.5" opacity="0.9"/>'
        + f'<path d="M4,24 H216" fill="none" stroke="{dark}" stroke-width="2" opacity="0.9"/>'
        # corner studs
        + f'<rect x="3" y="3" width="3" height="3" fill="{light}" opacity="0.9"/>'
        + f'<rect x="214" y="3" width="3" height="3" fill="{light}" opacity="0.7"/>'
        + glow
        # Labels are rendered live by the GUI so button states can change text;
        # the PNG only supplies the frame and fill.
        + '</svg>\n'
    )
    write(name, body)


def write(name: str, content: str) -> None:
    path = os.path.join(OUT_DIR, name + ".svg")
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(content)
    print("wrote", path)


def build_panel() -> None:
    width, height = 380, 280
    defs = (
        '<linearGradient id="bg" x1="0" y1="0" x2="0" y2="1">'
        '<stop offset="0" stop-color="#0E1C26"/><stop offset="0.55" stop-color="#091118"/>'
        '<stop offset="1" stop-color="#050A0F"/></linearGradient>'
        '<linearGradient id="edge" x1="0" y1="0" x2="1" y2="1">'
        f'<stop offset="0" stop-color="{CYAN}"/><stop offset="1" stop-color="#135264"/></linearGradient>'
        '<pattern id="scan" width="6" height="6" patternUnits="userSpaceOnUse">'
        '<rect width="6" height="1" fill="#3FE0F5" opacity="0.035"/></pattern>'
    )
    content = header(width, height, defs)
    content += f'<path d="M8,0 H372 L380,8 V272 L372,280 H8 L0,272 V8 Z" fill="url(#bg)"/>'
    content += f'<path d="M8,2 H372 L378,8 V272 L372,278 H8 L2,272 V8 Z" fill="none" stroke="url(#edge)" stroke-width="2"/>'
    content += f'<rect x="8" y="8" width="364" height="264" fill="none" stroke="#122F3C" stroke-width="1"/>'
    content += '<rect x="9" y="9" width="362" height="262" fill="url(#scan)"/>'
    # corner diamonds
    for cx, cy in ((16, 16), (364, 16), (16, 264), (364, 264)):
        content += f'<rect x="{cx - 3}" y="{cy - 3}" width="6" height="6" fill="{CYAN}" opacity="0.55" transform="rotate(45 {cx} {cy})"/>'
    # inner top highlight
    content += f'<rect x="14" y="12" width="352" height="1" fill="{CYAN}" opacity="0.35"/>'
    content += '</svg>\n'
    write("panel", content)


def build_logo() -> None:
    width, height = 320, 56
    defs = (
        '<linearGradient id="rush" x1="0" y1="0" x2="1" y2="0">'
        f'<stop offset="0" stop-color="{CYAN}"/><stop offset="1" stop-color="#9FF3FF"/></linearGradient>'
        f'<linearGradient id="gem" x1="0" y1="0" x2="1" y2="1">'
        f'<stop offset="0" stop-color="#BFF8FF"/><stop offset="0.5" stop-color="{CYAN}"/>'
        f'<stop offset="1" stop-color="#0F7C93"/></linearGradient>'
    )
    text_w = text_width("DIAMOND", 3)
    badge_w = 48
    content_w = 40 + 12 + text_w + 10 + badge_w
    start = (width - content_w) / 2
    gem_cx = start + 20
    text_x = start + 40 + 12
    badge_x = text_x + text_w + 10
    content = header(width, height, defs)
    content += diamond(gem_cx, 28, 40, "url(#gem)")
    content += pixel_text("DIAMOND", text_x, 6, 3, WHITE)
    content += pixel_text("RUSH", text_x, 30, 3, "url(#rush)")
    content += f'<rect x="{badge_x}" y="30" width="{badge_w}" height="22" fill="#140F02" stroke="{GOLD}" stroke-width="1.5"/>'
    content += pixel_text("1V1", badge_x + 6, 34, 2, GOLD)
    content += f'<rect x="{text_x}" y="50" width="{text_w}" height="2" fill="{GOLD}" opacity="0.75"/>'
    content += '</svg>\n'
    write("logo", content)


def build_champion() -> None:
    width = height = 84
    defs = (
        f'<radialGradient id="glow"><stop offset="0" stop-color="{GOLD}" stop-opacity="0.55"/>'
        '<stop offset="1" stop-color="#FFC542" stop-opacity="0"/></radialGradient>'
        f'<linearGradient id="crown" x1="0" y1="0" x2="0" y2="1">'
        f'<stop offset="0" stop-color="#FFE9A3"/><stop offset="0.5" stop-color="{GOLD}"/>'
        f'<stop offset="1" stop-color="#B9791A"/></linearGradient>'
    )
    content = header(width, height, defs)
    content += '<circle cx="42" cy="42" r="40" fill="url(#glow)"/>'
    content += (f'<polygon points="14,60 18,24 34,42 42,16 50,42 66,24 70,60" fill="url(#crown)" '
                f'stroke="#6E4508" stroke-width="1.5" stroke-linejoin="round"/>')
    content += f'<rect x="12" y="60" width="60" height="10" fill="url(#crown)" stroke="#6E4508" stroke-width="1.5"/>'
    for cx, cy, color in ((42, 34, CYAN), (24, 48, CYAN), (60, 48, CYAN)):
        content += (f'<polygon points="{cx},{cy - 5} {cx + 5},{cy} {cx},{cy + 5} {cx - 5},{cy}" '
                    f'fill="{color}" stroke="#0A2A33" stroke-width="1"/>')
    content += f'<rect x="18" y="63" width="48" height="2" fill="{GOLD}" opacity="0.8"/>'
    content += '</svg>\n'
    write("champion", content)


def build_banner(name: str, label: str, edge: str, text_color: str) -> None:
    width, height = 300, 40
    defs = (f'<linearGradient id="plate" x1="0" y1="0" x2="1" y2="0">'
            '<stop offset="0" stop-color="#05090D" stop-opacity="0.2"/>'
            '<stop offset="0.2" stop-color="#0B1620"/>'
            '<stop offset="0.8" stop-color="#0B1620"/>'
            '<stop offset="1" stop-color="#05090D" stop-opacity="0.2"/></linearGradient>'
            '<linearGradient id="gem" x1="0" y1="0" x2="1" y2="1">'
            f'<stop offset="0" stop-color="#BFF8FF"/><stop offset="0.5" stop-color="{CYAN}"/>'
            '<stop offset="1" stop-color="#0F7C93"/></linearGradient>')
    content = header(width, height, defs)
    content += '<rect x="0" y="2" width="300" height="36" fill="url(#plate)"/>'
    content += f'<rect x="0" y="2" width="300" height="1" fill="{edge}" opacity="0.85"/>'
    content += f'<rect x="0" y="37" width="300" height="1" fill="{edge}" opacity="0.5"/>'
    content += diamond(26, 20, 18, "url(#gem)")
    content += diamond(274, 20, 18, "url(#gem)")
    content += pixel_text(label, (width - text_width(label, 3)) / 2, 8, 3, text_color)
    content += '</svg>\n'
    write(name, content)


def build_diamond_icon() -> None:
    width = height = 22
    defs = (f'<linearGradient id="gem" x1="0" y1="0" x2="1" y2="1">'
            f'<stop offset="0" stop-color="#D6FBFF"/><stop offset="0.45" stop-color="{CYAN}"/>'
            f'<stop offset="1" stop-color="#0E6C82"/></linearGradient>')
    content = header(width, height, defs)
    content += diamond(11, 11, 20, "url(#gem)")
    content += '</svg>\n'
    write("diamond", content)


def build_scoreboard() -> None:
    width, height = 240, 40
    defs = ('<linearGradient id="plate" x1="0" y1="0" x2="0" y2="1">'
            '<stop offset="0" stop-color="#0C1922"/><stop offset="1" stop-color="#061017"/></linearGradient>')
    content = header(width, height, defs)
    content += '<path d="M10,0 H230 L240,10 V30 L230,40 H10 L0,30 V10 Z" fill="url(#plate)" stroke="#123A48" stroke-width="1"/>'
    for x in (0, 230):
        content += f'<rect x="{x}" y="14" width="10" height="12" fill="{CYAN}" opacity="0.35"/>'
    content += f'<rect x="24" y="6" width="1" height="28" fill="{CYAN}" opacity="0.55"/>'
    content += f'<rect x="215" y="6" width="1" height="28" fill="{CYAN}" opacity="0.55"/>'
    content += f'<rect x="107" y="8" width="1" height="24" fill="{CYAN_DARK}" opacity="0.5"/>'
    content += f'<rect x="132" y="8" width="1" height="24" fill="{CYAN_DARK}" opacity="0.5"/>'
    content += '</svg>\n'
    write("scoreboard", content)


def build_icon() -> None:
    width = height = 128
    defs = (
        '<linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">'
        '<stop offset="0" stop-color="#0E1C26"/><stop offset="1" stop-color="#04080C"/></linearGradient>'
        f'<linearGradient id="gem" x1="0" y1="0" x2="1" y2="1">'
        f'<stop offset="0" stop-color="#D6FBFF"/><stop offset="0.45" stop-color="{CYAN}"/>'
        f'<stop offset="1" stop-color="#0E6C82"/></linearGradient>'
    )
    content = header(width, height, defs)
    content += '<rect width="128" height="128" fill="url(#bg)"/>'
    content += f'<rect x="4" y="4" width="120" height="120" fill="none" stroke="{CYAN_DARK}" stroke-width="2"/>'
    content += diamond(64, 48, 64, "url(#gem)")
    content += pixel_text("DR", (width - text_width("DR", 4)) / 2, 84, 4, WHITE)
    content += f'<rect x="28" y="112" width="72" height="3" fill="{GOLD}"/>'
    content += '</svg>\n'
    write("icon", content)


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    build_panel()
    build_logo()
    build_champion()
    build_diamond_icon()
    build_scoreboard()
    build_icon()
    build_banner("victory", "VICTORY", GOLD, "#FFE9A3")
    build_banner("defeat", "DEFEAT", "#7A2B2B", RED)
    button("button_ready", "READY", "#2E9E48", "#7CFF98", "#145C28", WHITE)
    button("button_ready_hover", "READY", "#3FCB5D", "#B6FFC6", "#1B7A35", WHITE, hover=True)
    button("button_play_again", "PLAY AGAIN", "#177F92", "#6FE6F7", "#0A4A57", WHITE)
    button("button_play_again_hover", "PLAY AGAIN", "#1FA0B8", "#A8F4FF", "#0C5E6E", WHITE, hover=True)
    button("button_new_tournament", "NEW TOURNAMENT", "#9A6A12", "#FFDD8A", "#4E3405", "#FFF6D8")
    button("button_new_tournament_hover", "NEW TOURNAMENT", "#C08A1D", "#FFEFB8", "#63430A", "#FFF6D8", hover=True)
    button("button_leave", "LEAVE QUEUE", "#3C4A55", "#8FA3B0", "#1C262D", WHITE)
    button("button_leave_hover", "LEAVE QUEUE", "#556571", "#C6D6E0", "#26323A", WHITE, hover=True)


if __name__ == "__main__":
    main()
