## Upstream items (executed in the VITRAL repository, listed for symmetry)

1. `getMinMax()`: adopt povCpp's `AxisAlignedBoundingBox` return type in
   VITRAL, replacing the raw `double*`.
2. The all-crossings contract and both CSG strategies (step 6), gated on the
   hit-record taxonomy decision.
3. The statistics taxonomy: reconcile the extracted `GeometryStatistics`
   (step 1) with VITRAL's static `RaytraceStatistics` — per-primitive
   granularity as tags/parameters rather than one method per primitive, and
   povCpp's parts-summing-constructor ownership model as the shared answer for
   multi-threaded aggregation. `SolidTextureStatistics` stays the shared
   sub-model as-is.
