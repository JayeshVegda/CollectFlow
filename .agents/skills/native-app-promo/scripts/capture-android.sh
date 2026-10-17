#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  printf 'usage: %s OUTPUT.png [ADB_SERIAL]\n' "$0" >&2
  exit 64
fi

output_path="$1"
serial="${2:-}"

if ! command -v adb >/dev/null 2>&1; then
  printf 'error: adb is required for Android capture\n' >&2
  exit 69
fi

mkdir -p "$(dirname -- "$output_path")"

if [[ -n "$serial" ]]; then
  adb -s "$serial" exec-out screencap -p >"$output_path"
else
  adb exec-out screencap -p >"$output_path"
fi

printf 'captured Android screenshot: %s\n' "$output_path"
