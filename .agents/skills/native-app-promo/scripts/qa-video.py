#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import struct
import subprocess
import sys
import tempfile
import zlib
from fractions import Fraction
from pathlib import Path
from typing import Any


FFMPEG = os.environ.get("NATIVE_APP_PROMO_FFMPEG", "ffmpeg")
FFPROBE = os.environ.get("NATIVE_APP_PROMO_FFPROBE", "ffprobe")
FFMPEG_LIBDIR = os.environ.get("NATIVE_APP_PROMO_FFMPEG_LIBDIR")


def media_env() -> dict[str, str]:
    environment = os.environ.copy()
    if FFMPEG_LIBDIR:
        existing = environment.get("DYLD_LIBRARY_PATH")
        environment["DYLD_LIBRARY_PATH"] = (
            f"{FFMPEG_LIBDIR}:{existing}" if existing else FFMPEG_LIBDIR
        )
    return environment


def run(command: list[str]) -> subprocess.CompletedProcess[bytes]:
    return subprocess.run(
        command,
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        env=media_env(),
    )


def fraction(value: str | None) -> float | None:
    if not value or value in {"0/0", "N/A"}:
        return None
    return float(Fraction(value))


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def probe(path: Path) -> dict[str, Any]:
    result = run(
        [
            FFPROBE,
            "-v",
            "error",
            "-count_frames",
            "-show_entries",
            "format=duration:stream=index,codec_name,codec_type,width,height,pix_fmt,r_frame_rate,avg_frame_rate,nb_frames,nb_read_frames,color_range,color_space,sample_rate,channels",
            "-of",
            "json",
            str(path),
        ]
    )
    payload = json.loads(result.stdout)
    video = next(stream for stream in payload["streams"] if stream["codec_type"] == "video")
    audio = next((stream for stream in payload["streams"] if stream["codec_type"] == "audio"), None)
    fps = fraction(video.get("avg_frame_rate")) or fraction(video.get("r_frame_rate"))
    frame_count = video.get("nb_read_frames") or video.get("nb_frames")
    return {
        "duration_seconds": float(payload["format"]["duration"]),
        "width": int(video["width"]),
        "height": int(video["height"]),
        "fps": fps,
        "frame_count": int(frame_count) if frame_count and frame_count != "N/A" else None,
        "video_codec": video.get("codec_name"),
        "pixel_format": video.get("pix_fmt"),
        "color_range": video.get("color_range"),
        "color_space": video.get("color_space"),
        "audio_codec": audio.get("codec_name") if audio else None,
        "audio_sample_rate": int(audio["sample_rate"]) if audio and audio.get("sample_rate") else None,
        "audio_channels": int(audio["channels"]) if audio and audio.get("channels") else None,
    }


def paeth(a: int, b: int, c: int) -> int:
    estimate = a + b - c
    distance_a = abs(estimate - a)
    distance_b = abs(estimate - b)
    distance_c = abs(estimate - c)
    if distance_a <= distance_b and distance_a <= distance_c:
        return a
    return b if distance_b <= distance_c else c


def read_grayscale_png(path: Path) -> bytes:
    payload = path.read_bytes()
    if not payload.startswith(b"\x89PNG\r\n\x1a\n"):
        raise RuntimeError(f"not a PNG: {path}")
    cursor = 8
    compressed = bytearray()
    width = height = 0
    while cursor < len(payload):
        length = struct.unpack(">I", payload[cursor : cursor + 4])[0]
        chunk_type = payload[cursor + 4 : cursor + 8]
        chunk_data = payload[cursor + 8 : cursor + 8 + length]
        cursor += 12 + length
        if chunk_type == b"IHDR":
            width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(
                ">IIBBBBB", chunk_data
            )
            if (bit_depth, color_type, compression, filtering, interlace) != (8, 0, 0, 0, 0):
                raise RuntimeError(f"unsupported PNG format in {path}")
        elif chunk_type == b"IDAT":
            compressed.extend(chunk_data)
        elif chunk_type == b"IEND":
            break
    raw = zlib.decompress(bytes(compressed))
    row_size = width
    previous = bytearray(row_size)
    output = bytearray()
    offset = 0
    for _ in range(height):
        filter_type = raw[offset]
        offset += 1
        source = raw[offset : offset + row_size]
        offset += row_size
        row = bytearray(row_size)
        for index, value in enumerate(source):
            left = row[index - 1] if index else 0
            up = previous[index]
            upper_left = previous[index - 1] if index else 0
            if filter_type == 0:
                predictor = 0
            elif filter_type == 1:
                predictor = left
            elif filter_type == 2:
                predictor = up
            elif filter_type == 3:
                predictor = (left + up) // 2
            elif filter_type == 4:
                predictor = paeth(left, up, upper_left)
            else:
                raise RuntimeError(f"unsupported PNG filter {filter_type} in {path}")
            row[index] = (value + predictor) & 0xFF
        output.extend(row)
        previous = row
    return bytes(output)


def scan_frames(
    path: Path, fps: float, max_still_seconds: float, near_still_threshold: float
) -> dict[str, Any]:
    # Keep enough spatial detail for small but meaningful HTML motion. At 160x90,
    # area downsampling can erase fine movement and over-report stillness.
    width, height = 320, 180
    frame_size = width * height
    previous: bytes | None = None
    frame_count = 0
    black_frames = 0
    longest_near_still = 0
    current_near_still = 0
    longest_exact = 0
    current_exact = 0
    minimum_luma = 255.0
    maximum_still_frames = round(max_still_seconds * fps)

    with tempfile.TemporaryDirectory(prefix="native-app-promo-frames-") as temporary:
        pattern = str(Path(temporary) / "f-%08d.png")
        run(
            [
                FFMPEG,
                "-v",
                "error",
                "-i",
                str(path),
                "-map",
                "0:v:0",
                "-vf",
                f"scale={width}:{height}:flags=area,format=gray",
                "-c:v",
                "png",
                pattern,
            ]
        )
        for frame_path in sorted(Path(temporary).glob("f-*.png")):
            current = read_grayscale_png(frame_path)
            if len(current) != frame_size:
                raise RuntimeError(f"unexpected decoded frame size: {frame_path}")
            frame_count += 1
            mean_luma = sum(current) / frame_size
            minimum_luma = min(minimum_luma, mean_luma)
            black_frames += int(mean_luma < 8.0)
            if previous is not None:
                mean_abs_diff = sum(abs(a - b) for a, b in zip(current, previous)) / frame_size
                current_exact = current_exact + 1 if mean_abs_diff <= 0.001 else 0
                current_near_still = (
                    current_near_still + 1 if mean_abs_diff <= near_still_threshold else 0
                )
                longest_exact = max(longest_exact, current_exact)
                longest_near_still = max(longest_near_still, current_near_still)
            previous = current

    return {
        "decoded_frames": frame_count,
        "minimum_mean_luma": round(minimum_luma, 4),
        "black_frames": black_frames,
        "longest_exact_repeat_seconds": round(longest_exact / fps, 4),
        "longest_near_still_seconds": round(longest_near_still / fps, 4),
        "near_still_limit_seconds": max_still_seconds,
        "near_still_limit_frames": maximum_still_frames,
        "near_still_mean_abs_diff_threshold": near_still_threshold,
        "near_still_pass": longest_near_still <= maximum_still_frames,
    }


def audio_peak(path: Path) -> float | None:
    result = subprocess.run(
        [FFMPEG, "-hide_banner", "-i", str(path), "-map", "0:a:0", "-af", "volumedetect", "-f", "null", "-"],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.PIPE,
        env=media_env(),
    )
    match = re.search(rb"max_volume:\s*(-?[0-9.]+) dB", result.stderr)
    return float(match.group(1)) if match else None


def main() -> int:
    parser = argparse.ArgumentParser(description="Deterministic media QA for native-app promos")
    parser.add_argument("video", type=Path)
    parser.add_argument("--json", type=Path, dest="json_output")
    parser.add_argument("--expect-width", type=int)
    parser.add_argument("--expect-height", type=int)
    parser.add_argument("--expect-fps", type=float)
    parser.add_argument("--max-still-seconds", type=float, default=5.0)
    parser.add_argument("--near-still-threshold", type=float, default=0.10)
    args = parser.parse_args()

    if not args.video.is_file():
        parser.error(f"video does not exist: {args.video}")

    metadata = probe(args.video)
    if not metadata["fps"]:
        raise RuntimeError("could not determine video frame rate")
    frames = scan_frames(
        args.video,
        metadata["fps"],
        args.max_still_seconds,
        args.near_still_threshold,
    )
    report = {
        "file": str(args.video.resolve()),
        "sha256": sha256(args.video),
        "metadata": metadata,
        "decoded_frame_scan": frames,
        "audio_peak_dbfs": audio_peak(args.video),
        "failures": [],
    }
    failures: list[str] = report["failures"]

    if metadata["frame_count"] and metadata["frame_count"] != frames["decoded_frames"]:
        failures.append("reported and decoded frame counts differ")
    if frames["black_frames"]:
        failures.append("black frames detected")
    if not frames["near_still_pass"]:
        failures.append("near-still run exceeds limit")
    if args.expect_width and metadata["width"] != args.expect_width:
        failures.append("unexpected width")
    if args.expect_height and metadata["height"] != args.expect_height:
        failures.append("unexpected height")
    if args.expect_fps and abs(metadata["fps"] - args.expect_fps) > 0.001:
        failures.append("unexpected frame rate")
    if metadata["audio_sample_rate"] and metadata["audio_sample_rate"] != 48000:
        failures.append("audio is not 48 kHz")
    if metadata["audio_channels"] and metadata["audio_channels"] != 2:
        failures.append("audio is not stereo")
    if report["audio_peak_dbfs"] is not None and report["audio_peak_dbfs"] >= 0.0:
        failures.append("audio peak reaches clipping threshold")

    serialized = json.dumps(report, ensure_ascii=False, indent=2)
    print(serialized)
    if args.json_output:
        args.json_output.parent.mkdir(parents=True, exist_ok=True)
        args.json_output.write_text(serialized + "\n", encoding="utf-8")
    return 1 if failures else 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, RuntimeError, subprocess.CalledProcessError) as error:
        print(f"qa-video: {error}", file=sys.stderr)
        raise SystemExit(2)
