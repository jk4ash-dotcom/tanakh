#!/usr/bin/env python3
"""Tap Compose UI via uiautomator dump. Used only when an emulator/device is present.

Stable handles come from the app sources (contentDescription / visible Text), not coordinates.
"""

from __future__ import annotations

import argparse
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path


def run(argv: list[str], check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(argv, check=check, text=True, capture_output=True)


def dump_ui(serial: str, dest: Path) -> Path:
    remote = "/sdcard/verify-tanakh-dump.xml"
    run(["adb", "-s", serial, "shell", "uiautomator", "dump", remote])
    run(["adb", "-s", serial, "pull", remote, str(dest)])
    return dest


def nodes(dump_path: Path) -> list[ET.Element]:
    tree = ET.parse(dump_path)
    return list(tree.iter("node"))


def find_node(dump_path: Path, *, text: str | None = None, desc: str | None = None) -> ET.Element | None:
    for n in nodes(dump_path):
        if text is not None and n.attrib.get("text") == text:
            return n
        if desc is not None and n.attrib.get("content-desc") == desc:
            return n
    return None


def tap_node(serial: str, node: ET.Element) -> None:
    bounds = node.attrib.get("bounds") or ""
    # bounds="[x1,y1][x2,y2]"
    parts = bounds.replace("][", ",").replace("[", "").replace("]", "").split(",")
    if len(parts) != 4:
        raise SystemExit(f"cannot parse bounds={bounds!r}")
    x1, y1, x2, y2 = map(int, parts)
    x, y = (x1 + x2) // 2, (y1 + y2) // 2
    run(["adb", "-s", serial, "shell", "input", "tap", str(x), str(y)])


def wait_for(
    serial: str,
    dest: Path,
    *,
    text: str | None = None,
    desc: str | None = None,
    timeout: float = 30.0,
) -> ET.Element:
    deadline = time.time() + timeout
    last = None
    while time.time() < deadline:
        dump_ui(serial, dest)
        last = dest.read_text(encoding="utf-8", errors="replace")
        node = find_node(dest, text=text, desc=desc)
        if node is not None:
            return node
        if "Could not load catalog" in last:
            raise SystemExit("UI shows 'Could not load catalog'")
        time.sleep(1.5)
    raise SystemExit(f"timeout waiting for text={text!r} desc={desc!r}")


def recipe_verse_gloss(serial: str, evidence: Path) -> None:
    """Home → Genesis → Genesis 1 → 1:1 → phonetic chip → gloss sheet.

    Handles from HomeScreen / BookScreen / ChapterScreen / VerseScreen / BookTitles.
    """
    step = evidence / "emulator-smoke"
    step.mkdir(parents=True, exist_ok=True)

    dump = step / "01-home.xml"
    wait_for(serial, dump, text="Tanakh Learner")
    wait_for(serial, dump, text="Genesis")
    tap_node(serial, find_node(dump, text="Genesis"))  # type: ignore[arg-type]

    dump = step / "02-book.xml"
    wait_for(serial, dump, text="Genesis 1")
    tap_node(serial, find_node(dump, text="Genesis 1"))  # type: ignore[arg-type]

    dump = step / "03-chapter.xml"
    wait_for(serial, dump, text="1:1")
    tap_node(serial, find_node(dump, text="1:1"))  # type: ignore[arg-type]

    dump = step / "04-verse.xml"
    wait_for(serial, dump, text="Genesis 1:1")
    wait_for(serial, dump, text="Hebrew + phonetic (OSHB order, LTR paired chips)")
    # Phonetic chip for Gen.1.1 first token (PackSanityTest / Sofer SBL-Learner).
    phonetic = find_node(dump, text="bĕrēʾshîth")
    hebrew = find_node(dump, text="בְּרֵאשִׁית")
    if phonetic is None and hebrew is None:
        raise SystemExit(
            "verse dump missing first-token chips bĕrēʾshîth / בְּרֵאשִׁית — Compose may not expose chip text to uiautomator"
        )
    # LTR paired chips: first Hebrew must appear before last token הָאָרֶץ in document order
    texts = [n.attrib.get("text") or "" for n in nodes(dump)]
    if "בְּרֵאשִׁית" in texts and "הָאָרֶץ" in texts:
        if texts.index("בְּרֵאשִׁית") > texts.index("הָאָרֶץ"):
            raise SystemExit("LTR invariant failed: בְּרֵאשִׁית appears after הָאָרֶץ in UI dump")
    tap_node(serial, phonetic or hebrew)  # type: ignore[arg-type]

    dump = step / "05-gloss.xml"
    wait_for(serial, dump, text="Possible sense(s)")
    wait_for(serial, dump, text="Gloss ≠ verse translation")
    blob = dump.read_text(encoding="utf-8", errors="replace").lower()
    for forbidden in ("jehovah", "ye.ho.vah", "yehovah"):
        if forbidden in blob:
            raise SystemExit(f"gloss sheet leaked {forbidden!r}")
    print("PROOF (emulator-smoke): Home→Genesis→Genesis 1→1:1→gloss sheet Possible sense(s)")


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--serial", required=True)
    p.add_argument("--evidence", required=True)
    p.add_argument("--recipe", default="verse-gloss")
    args = p.parse_args()
    evidence = Path(args.evidence)
    if args.recipe == "verse-gloss":
        recipe_verse_gloss(args.serial, evidence)
        return 0
    print(f"unknown recipe {args.recipe}", file=sys.stderr)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
