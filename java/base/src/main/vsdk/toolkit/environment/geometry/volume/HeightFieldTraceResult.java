package vsdk.toolkit.environment.geometry.volume;

/**
Outcome of HeightFieldSolid.traceFirstHit: the ray missed the bounding
box, crossed it without hitting the surface, or hit the surface.
*/
public enum HeightFieldTraceResult { MissedBounds, MissedSurface, Hit }
