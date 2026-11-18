"""Deterministic Android checks over uiautomator dumps (the lean pipeline's stand-in for
impeccable's detector, which does not scan Android XML).

Usage: python3 native_checks.py <density_dpi> <dump.xml> [...]
Writes <slug>-checks.txt next to each dump and prints a summary.
"""
import re, sys, os, xml.etree.ElementTree as ET

MIN_DP = 48


def bounds(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds", "[0,0][0,0]")))
    return x1, y1, x2, y2


def has_text_descendant(node):
    return any((n.get("text") or n.get("content-desc")) for n in node.iter("node"))


def check(path, dpi):
    px_per_dp = dpi / 160.0
    root = ET.parse(path).getroot()
    nodes = list(root.iter("node"))
    screen_w = max((bounds(n)[2] for n in nodes), default=0)
    issues = []
    for n in nodes:
        if n.get("package", "").startswith("com.android.systemui"):
            continue
        x1, y1, x2, y2 = bounds(n)
        w_dp, h_dp = (x2 - x1) / px_per_dp, (y2 - y1) / px_per_dp
        label = n.get("text") or n.get("content-desc") or n.get("resource-id", "").split("/")[-1] or n.get("class")
        if n.get("clickable") == "true" and (w_dp < MIN_DP or h_dp < MIN_DP) and w_dp > 0:
            issues.append(f"TOUCH_TARGET  {w_dp:.0f}x{h_dp:.0f}dp < {MIN_DP}dp  {label}")
        if n.get("clickable") == "true" and not has_text_descendant(n):
            issues.append(f"NO_LABEL      clickable with no text/content-desc  {label}")
        if x2 > screen_w + 1 or x1 < 0:
            issues.append(f"OFFSCREEN     bounds {n.get('bounds')}  {label}")
    out = path.replace(".xml", "-checks.txt")
    with open(out, "w") as f:
        f.write("\n".join(issues) if issues else "no native-check issues")
    return len(issues)


if __name__ == "__main__":
    dpi = int(sys.argv[1])
    for p in sys.argv[2:]:
        print(f"{os.path.basename(p):28s} {check(p, dpi)} issues")
