#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  printf 'usage: %s OUTPUT.png [DEVICE_UDID|booted]\n' "$0" >&2
  exit 64
fi

output_path="$1"
device_ref="${2:-booted}"

if ! command -v xcrun >/dev/null 2>&1; then
  printf 'error: xcrun is required for iOS Simulator capture\n' >&2
  exit 69
fi

mkdir -p "$(dirname -- "$output_path")"
xcrun simctl io "$device_ref" screenshot --type=png "$output_path"
printf 'captured iOS Simulator screenshot: %s\n' "$output_path"
