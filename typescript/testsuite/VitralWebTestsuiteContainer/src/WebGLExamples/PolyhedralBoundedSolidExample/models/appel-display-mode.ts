/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/src/models/AppelDisplayMode.java`.
 *
 * Display states for the Appel hidden-line algorithm, cycled with key [8]:
 * OFF disables it, EDGES_VISIBLE_HIDDEN draws contour, visible and hidden lines,
 * EDGES_VISIBLE draws only contour and visible lines (the hidden ones are
 * suppressed), and EDGES_ONLY draws only the contour (border) lines (both the
 * visible non-contour and the hidden lines are suppressed).
 */
export type AppelDisplayMode = 'OFF' | 'EDGES_VISIBLE_HIDDEN' | 'EDGES_VISIBLE' | 'EDGES_ONLY';

const VALUES: readonly AppelDisplayMode[] = [
  'OFF',
  'EDGES_VISIBLE_HIDDEN',
  'EDGES_VISIBLE',
  'EDGES_ONLY',
];

const LABELS: Readonly<Record<AppelDisplayMode, string>> = {
  OFF: 'OFF',
  EDGES_VISIBLE_HIDDEN: 'edges + visible + hidden',
  EDGES_VISIBLE: 'edges + visible',
  EDGES_ONLY: 'edges only',
};

export function appelDisplayModeLabel(mode: AppelDisplayMode): string {
  return LABELS[mode];
}

export function appelDisplayModeNextCircular(mode: AppelDisplayMode): AppelDisplayMode {
  return VALUES[(VALUES.indexOf(mode) + 1) % VALUES.length]!;
}
