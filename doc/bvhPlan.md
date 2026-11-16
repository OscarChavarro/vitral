# BVH for triangle meshes (CPU)

References:
[WALD2007] Wald, Ingo. "On fast Construction of SAH-based Bounding Volume
Hierarchies". Proceedings of the 2007 IEEE Symposium on Interactive Ray
Tracing, 2007, pp. 33-40. (`doc/references/[WALD2007]_*.pdf`)

## Design

All CPU, single threaded build, no STL, no GPU: usable on every vitral
platform (minimal kernel, Turbo C port). Package
`environment/geometry/boundingVolumeHierarchy`.

- `TriangleMeshAccelerationMethod` (enum): `BRUTE_FORCE`,
  `WALD2007_BINNED_SAH`. Selected by
  `RendererConfiguration::setMeshAccelerationMethod` (default
  `WALD2007_BINNED_SAH`); `RaytracingOfflineExample -m bruteforce|wald2007`.
- `TriangleAccelerationStructure`: abstract mesh level structure. A new
  scheme is a new enum constant, a subclass and a case in
  `TriangleAccelerationStructure::create`.
- `BinnedSahHierarchy`: the single BVH builder of vitral, binned SAH over
  boxes (12 bins, leaves of `maximumLeafSize` entries, 4 by default, depth
  at most 30), traversed nearest child first. Deviations from [WALD2007] are
  listed in its header. Output format, shared with the GPU:
  `doc/flatBinaryTreeContract.md`; the Vulkan 1.4 raytracer (compute path)
  uploads it.
- Levels, as the Vulkan BLAS / TLAS:
  - `BinnedSahTriangleHierarchy`: triangles of one `TriangleMesh`.
  - `TriangleMeshGroup`: a `BinnedSahHierarchy` over the boxes of its
    meshes.
  - `BinnedSahInstanceHierarchy`: transformed instances of bottom levels
    (object box + object to world matrix, leaves of 1), the CPU form of a
    Vulkan TLAS; the Vulkan compute path builds its top level with it.
- `SimpleRaytracer::prepareAccelerationStructures` builds them before
  tracing (`SimpleRaytracer::execute`, and `ParallelRaytracer` before
  starting its workers); tracing only reads them.
- The structures cache the vertex bounds of their mesh, used by the bounds
  pre-test of every ray instead of walking the vertices.

Exactness: every method gives the brute force hit bit for bit (same
`Triangle::intersectRay` on the same values, padded boxes, nodes skipped
only when strictly beyond the closest hit, ties to the lowest triangle /
mesh index). Images are identical (`cmp`) between both methods for the 30
scenes of `etc/geometry/mitscenes`.

## Counters

`RaytraceStatistics` counts traversals, node visits and entry / primitive
tests per level (`doc/flatBinaryTreeContract.md` section 7), enabled with
`RaytracingOfflineExample --stats`; the image is the same with and without
it.

## Measurements

`RaytracingOfflineExample --parallel` on
`etc/geometry/mitscenes/balls_cowobj.ray` (1280x720, `cow.obj`: 2903
vertices, 3263 faces, about 5800 triangles in 172 meshes), Intel Xeon Gold
6154, 72 threads, Release build:

| method | render time | speed-up |
|---|---|---|
| `bruteforce` | 7.66 - 7.71 s | 1x |
| `wald2007` | 0.454 - 0.455 s | 17x |

- Build time of all structures: 0.002 s.
- `wald2007` serial (1 thread): 3.544 s, faster than `bruteforce` with 72
  threads.
- `wald2007` work: 9.77 M primitive level traversals, 27.1 M node visits,
  10.8 M triangle tests.
- Without the cow the scene renders in 0.104 s.

Tests: `triangle_acceleration_tests.cpp` (exact match against brute force
on soups, shared edge grids and a 60 mesh group; transformed instances
against a linear loop; determinism; depth and leaf size bounds; copies and
invalidation; counters).

## Pending

- `TriangleMesh::queryFirstHit` / `emitRayCrossings` (CSG and typed
  queries) use a linear loop: their contract reports `Indeterminate`
  triangles, so they need their own traversal.
- Scene level: `SimpleRaytracer` loops over all bodies
  (`selectNearestThingInRayDirection`, shadow rays);
  `BinnedSahInstanceHierarchy` is the structure for it (one instance per
  `SimpleBody`, its placement as the matrix).
- Java, TypeScript and Turbo C ports of `BinnedSahInstanceHierarchy`, the
  `maximumLeafSize` parameter and the counters.
- `balls_featured_object.ray` takes 4 s with either method: not mesh bound,
  to be profiled.
