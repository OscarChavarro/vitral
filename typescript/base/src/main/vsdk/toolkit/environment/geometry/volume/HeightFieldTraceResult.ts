// Outcome of HeightFieldSolid::traceFirstHit: the ray missed the bounding
// box, crossed it without hitting the surface, or hit the surface.
export enum HeightFieldTraceResult {
    MissedBounds,
    MissedSurface,
    Hit,
}
