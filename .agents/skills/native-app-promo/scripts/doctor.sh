#!/usr/bin/env bash
set -euo pipefail

required=(node npm npx python3)
missing=0

for command_name in "${required[@]}"; do
  if command -v "$command_name" >/dev/null 2>&1; then
    version="$($command_name --version 2>&1 | head -n 1)"
    printf 'ok      %-10s %s\n' "$command_name" "$version"
  else
    printf 'missing %-10s required\n' "$command_name"
    missing=1
  fi
done

if command -v ffmpeg >/dev/null 2>&1; then
  printf 'ok      %-10s %s\n' 'ffmpeg' "$(ffmpeg -version 2>&1 | head -n 1)"
elif [[ -x "${NATIVE_APP_PROMO_FFMPEG:-}" ]]; then
  printf 'ok      %-10s explicit Remotion or custom binary\n' 'ffmpeg'
else
  printf 'missing %-10s required; set NATIVE_APP_PROMO_FFMPEG when using Remotion bundled binary\n' 'ffmpeg'
  missing=1
fi

if command -v ffprobe >/dev/null 2>&1; then
  printf 'ok      %-10s %s\n' 'ffprobe' "$(ffprobe -version 2>&1 | head -n 1)"
elif [[ -x "${NATIVE_APP_PROMO_FFPROBE:-}" ]]; then
  printf 'ok      %-10s explicit Remotion or custom binary\n' 'ffprobe'
else
  printf 'missing %-10s required; set NATIVE_APP_PROMO_FFPROBE when using Remotion bundled binary\n' 'ffprobe'
  missing=1
fi

if command -v xcrun >/dev/null 2>&1 && xcrun simctl help >/dev/null 2>&1; then
  printf 'ok      %-10s iOS Simulator capture available\n' 'simctl'
else
  printf 'optional %-10s unavailable\n' 'simctl'
fi

if command -v adb >/dev/null 2>&1; then
  printf 'ok      %-10s Android capture available\n' 'adb'
else
  printf 'optional %-10s unavailable\n' 'adb'
fi

shotcraft_dir="${NATIVE_APP_PROMO_SHOTCRAFT_DIR:-}"

if [[ -z "$shotcraft_dir" && -n "${CODEX_HOME:-}" ]]; then
  shotcraft_dir="${CODEX_HOME}/skills/video-shotcraft"
elif [[ -z "$shotcraft_dir" && -n "${HOME:-}" ]]; then
  shotcraft_dir="${HOME}/.codex/skills/video-shotcraft"
fi

if [[ -f "${shotcraft_dir}/SKILL.md" ]]; then
  printf 'optional %-10s installed at %s\n' 'shotcraft' "$shotcraft_dir"
else
  printf 'optional %-10s not found; install separately or set NATIVE_APP_PROMO_SHOTCRAFT_DIR\n' 'shotcraft'
fi

exit "$missing"
