import {Easing, interpolate} from 'remotion';

export const clamp01 = (value: number) => Math.max(0, Math.min(1, value));

export const progress = (frame: number, start: number, end: number) =>
  interpolate(frame, [start, end], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
    easing: Easing.bezier(0.22, 1, 0.36, 1),
  });

export const lerp = (from: number, to: number, value: number) =>
  from + (to - from) * clamp01(value);
