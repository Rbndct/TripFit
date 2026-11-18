"""Batch-capture every TripFit screen (light / dark / 1.3x font + uiautomator dump).

Usage: python3 capture.py [slug ...]      (no args = all screens)
Assumes one booted emulator/device and the debug APK installed.
"""
import subprocess, sys, time, os

SDK = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
PKG = "com.itismob.s01.group01.tripfit"
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../.."))
OUT = os.path.join(ROOT, ".impeccable", "review")
SETTLE_S = 4.0

# slug -> (activity, extra adb am-start args)
SCREENS = {
    "login": ("LoginActivity", []),
    "home": ("HomeActivity", []),
    "new-trip": ("NewTripActivity", []),
    "itinerary": ("MainActivity", []),
    "add-destination": ("AddDestinationActivity", []),
    "map-picker": ("MapPickerActivity", ["--ed", "extra_city_lat", "35.6762", "--ed", "extra_city_lng", "139.6503"]),
    "stop-detail": ("StopDetailActivity", ["--es", "extra_item_id", "capture", "--es", "extra_name", "Senso-ji Temple",
                                            "--ei", "extra_day", "1", "--ei", "extra_duration", "45", "--ei", "extra_max_day", "3"]),
    "optimize": ("OptimizeActivity", []),
    "discover": ("DiscoverActivity", []),
    "suggestions": ("SuggestionsActivity", []),
    "budget": ("BudgetActivity", []),
    "budget-setup": ("BudgetSetupActivity", []),
    "calendar-sync": ("CalendarSyncActivity", []),
    "summary": ("SummaryActivity", []),
    "group": ("GroupActivity", []),
    "journal": ("JournalActivity", []),
    "profile": ("ProfileActivity", []),
}


def adb(*args, timeout=30, binary=False):
    try:
        r = subprocess.run([SDK, *args], capture_output=True, timeout=timeout)
        return r.stdout if binary else r.stdout.decode(errors="replace")
    except subprocess.TimeoutExpired:
        print(f"  ! adb {' '.join(args[:3])} timed out after {timeout}s")
        return b"" if binary else ""


def launch(activity, extras):
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-n", f"{PKG}/.DebugLauncherActivity", "--es", "screen", activity, *extras)
    time.sleep(SETTLE_S)


ANR_MARKER = "isn't responding"


def anr_visible():
    """True when a 'System UI / app isn't responding' dialog is covering the screen."""
    xml = adb("exec-out", "uiautomator", "dump", "/dev/tty", timeout=40)
    return ANR_MARKER in xml


def shot(path):
    data = adb("exec-out", "screencap", "-p", binary=True, timeout=30)
    if not data:
        return False
    with open(path, "wb") as f:
        f.write(data)
    if anr_visible():
        print(f"  ! ANR dialog on screen, discarding {os.path.basename(path)}")
        return False
    return True


def dump(path):
    adb("shell", "uiautomator", "dump", "/sdcard/tf_ui.xml", timeout=40)
    xml = adb("exec-out", "cat", "/sdcard/tf_ui.xml", timeout=20)
    if ANR_MARKER in xml:
        print(f"  ! ANR dialog in dump, discarding {os.path.basename(path)}")
        return False
    if "<hierarchy" in xml:
        with open(path, "w") as f:
            f.write(xml[xml.index("<"):])
        return True
    return False


def capture(slug):
    activity, extras = SCREENS[slug]
    base = os.path.join(OUT, slug)
    print(f"[{slug}]")
    launch(activity, extras)
    ok = [shot(base + "-light.png"), dump(base + ".xml")]
    adb("shell", "cmd", "uimode", "night", "yes"); launch(activity, extras)
    ok.append(shot(base + "-dark.png"))
    adb("shell", "cmd", "uimode", "night", "no")
    adb("shell", "settings", "put", "system", "font_scale", "1.3"); launch(activity, extras)
    ok.append(shot(base + "-font.png"))
    adb("shell", "settings", "put", "system", "font_scale", "1.0")
    print("  light/dump/dark/font:", ok)
    return all(ok)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for key in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
        adb("shell", "settings", "put", "global", key, "0")
    slugs = sys.argv[1:] or list(SCREENS)
    failed = [s for s in slugs if not capture(s)]
    print("FAILED:", failed or "none")
