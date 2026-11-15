#ifndef __HEIGHT_FIELD_TRACE_RESULT__
#define __HEIGHT_FIELD_TRACE_RESULT__

// Outcome of HeightFieldSolid::traceFirstHit: the ray missed the bounding
// box, crossed it without hitting the surface, or hit the surface.
enum class HeightFieldTraceResult { MissedBounds, MissedSurface, Hit };
#endif
