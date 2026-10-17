#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  printf 'usage: %s OUTPUT_DIRECTORY\n' "$0" >&2
  exit 64
fi

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
skill_dir="$(dirname -- "$script_dir")"
starter_dir="$skill_dir/assets/remotion-starter"
output_dir="$1"

if [[ -e "$output_dir" ]]; then
  printf 'error: output already exists: %s\n' "$output_dir" >&2
  exit 73
fi

mkdir -p "$(dirname -- "$output_dir")"
cp -R "$starter_dir" "$output_dir"
printf 'created isolated Remotion project: %s\n' "$output_dir"
