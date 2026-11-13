#!/usr/bin/env python3
"""Minimal Windows driver: focus the Minecraft window, type chat commands, screenshot.

Used only for local verification of the Diamond Rush GUI.
"""
import ctypes
import sys
import time

from PIL import ImageGrab

user32 = ctypes.windll.user32
KEYEVENTF_KEYUP = 0x0002


def find_window():
    results = []

    @ctypes.WINFUNCTYPE(ctypes.c_bool, ctypes.c_void_p, ctypes.c_void_p)
    def callback(hwnd, _):
        if user32.IsWindowVisible(hwnd):
            buffer = ctypes.create_unicode_buffer(256)
            user32.GetWindowTextW(hwnd, buffer, 256)
            title = buffer.value
            if "Minecraft" in title:
                results.append((hwnd, title))
        return True

    user32.EnumWindows(callback, 0)
    return results


def focus(hwnd):
    user32.ShowWindow(hwnd, 9)  # SW_RESTORE
    user32.SetForegroundWindow(hwnd)
    time.sleep(0.4)


def tap(vk, shift=False):
    if shift:
        user32.keybd_event(0x10, 0, 0, 0)
    user32.keybd_event(vk, 0, 0, 0)
    time.sleep(0.01)
    user32.keybd_event(vk, 0, KEYEVENTF_KEYUP, 0)
    if shift:
        user32.keybd_event(0x10, 0, KEYEVENTF_KEYUP, 0)
    time.sleep(0.02)


def type_text(text):
    for char in text:
        code = user32.VkKeyScanW(ord(char))
        vk = code & 0xFF
        shift = bool((code >> 8) & 1)
        if vk == 0xFF:
            raise ValueError(f"No virtual key for {char!r}")
        tap(vk, shift)


def main():
    if len(sys.argv) < 2:
        print("usage: drive_client.py windows | focus | type <text> | chat <text> | shot <path>")
        return 1
    command = sys.argv[1]
    windows = find_window()
    if not windows:
        print("NO_MINECRAFT_WINDOW")
        return 2
    hwnd, title = windows[0]
    print("window:", hwnd, title)
    if command == "windows":
        print(len(windows), "window(s)")
        return 0
    focus(hwnd)
    if command == "focus":
        return 0
    if command == "type":
        type_text(" ".join(sys.argv[2:]))
        return 0
    if command == "chat":
        tap(ord("T"))
        time.sleep(0.4)
        type_text(" ".join(sys.argv[2:]))
        time.sleep(0.2)
        tap(0x0D)  # Enter
        time.sleep(0.6)
        return 0
    if command == "key":
        names = {"esc": 0x1B, "enter": 0x0D, "space": 0x20, "tab": 0x09}
        tap(names[sys.argv[2].lower()])
        return 0
    if command == "click":
        x, y = int(sys.argv[2]), int(sys.argv[3])
        user32.SetCursorPos(x, y)
        time.sleep(0.2)
        user32.mouse_event(0x0002, 0, 0, 0, 0)
        time.sleep(0.05)
        user32.mouse_event(0x0004, 0, 0, 0, 0)
        time.sleep(0.3)
        return 0
    if command == "shot":
        path = sys.argv[2]
        time.sleep(0.4)
        ImageGrab.grab().save(path)
        print("saved", path)
        return 0
    print("unknown command")
    return 1


if __name__ == "__main__":
    sys.exit(main())
