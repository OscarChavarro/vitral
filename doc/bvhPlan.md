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
  `WALD2007_BINNED_SAH`, `VOXEL_GRID` (C++ only, see the redesign note at
  the end). Selected by
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

## Redesign note: space subdivision, object subdivision, BLAS and TLAS

Recorded 2026-11-21, while RenderPark's voxel grid moved into VITRAL
(RenderPark `doc/vitralAlignmentPlan.md`, steps 4 to 6). Not scheduled: a
reminder for a later redesign.

Two families of ray acceleration structures now live in VITRAL:

| | Space subdivision | Object subdivision |
|---|---|---|
| Examples | uniform grid [SNYD1987], octree, kd-tree, BSP | BVH ([WALD2007]) |
| What is split | the volume, in regions that do not overlap | the object list, in groups whose boxes may overlap |
| An object lies in | every cell its box touches | exactly one leaf |
| Traversal | cells in ray order; stops at the first cell beyond the closest hit | nodes, pruned by the closest hit so far |
| Specific care | a hit found in one cell may lie in a later one ([SNYD1987] Fig. 11) | none |
| Weak case | objects of very different sizes ("teapot in a stadium") | build cost |

[SNYD1987] Snyder, John M. and Barr, Alan H. "Ray Tracing Complex Models
Containing Surface Tessellations". SIGGRAPH '87, Computer Graphics 21(4),
1987, pp. 119-128 (`doc/references/[SNYD1987]_*.pdf`). Its nested grids carry boxes and are inserted as objects
in other grids, so it is a hierarchy of space subdivisions (a hybrid), not
a BVH.

Where they are today (C++):

- `environment/geometry/spatialIndex`: the generic service. `SpatialIndex`
  answers closest-hit and any-hit ray queries over primitives given by
  boxes, with the caller's exact test (`PrimitiveRayTest`). Implementations:
  `BinnedSahSpatialIndex` ([WALD2007]), `GridSpatialIndex` ([SNYD1987], with
  `SpatialGroupTree` for the nested grids, ported from RenderPark's
  `VoxelGrid`) and `LinearSpatialIndex` (brute force). `GridSpatialIndex`
  also offers `queryWith<Test>`, the same query templated on the test type
  (`GridSpatialIndex.txx`), so a final test class is inlined instead of a
  virtual call per candidate.
- `environment/geometry/boundingVolumeHierarchy`: the mesh level.
  `TriangleAccelerationStructure` with `BinnedSahTriangleHierarchy` and
  `GridTriangleHierarchy` (a `GridSpatialIndex` over one mesh, method
  `voxel`, results identical to brute force), plus `TriangleMeshGroup` and
  `BinnedSahInstanceHierarchy` above it.

Questions for the redesign:

1. **Two parallel abstractions.** `SpatialIndex` (generic, primitives by
   box) and `TriangleAccelerationStructure` (triangles of a mesh) solve the
   same problem. The mesh classes could become thin adapters over a
   `SpatialIndex` with a triangle test, as `GridTriangleHierarchy` already
   is.
2. **Names and packages.** `boundingVolumeHierarchy` now holds a grid, and
   "Hierarchy" in `GridTriangleHierarchy` is misleading. A neutral package
   (for example `accelerationStructure`) with the BVH and the grid as two
   implementations would state the two families.
3. **BLAS vs TLAS.** Make the levels explicit, as Vulkan does: a bottom
   level over the primitives of one shape (triangles of a mesh, faces of a
   RenderPark geometry) and a top level over transformed instances of
   bottom levels (`BinnedSahInstanceHierarchy`; RenderPark's scene-wide
   index over all faces is a flattened single level today). Decide per
   level which family is allowed: a grid is natural as a bottom level of a
   well tessellated surface; [SNYD1987] itself nests grids as instances,
   which is a grid TLAS.
4. **One query contract for both levels.** Closest-hit with the brute
   force tie rule, any-hit with any valid blocker, workspace for per-query
   scratch (mailbox), counters; the top level calls the bottom level as its
   exact test.
5. **Ports.** `GridSpatialIndex`, `SpatialGroupTree`, `GridTriangleHierarchy`
   and `queryWith` are C++ only; Java, TypeScript and Turbo C ports follow
   the redesign, not the current layout.

Measured so far (RenderPark corridor, Galerkin without shaft culling, 5
runs, median CPU): `BinnedSahSpatialIndex` 209 s, RenderPark's original
grid 309 s, `GridSpatialIndex` 367 s before `queryWith`; identical images.
On the VITRAL mesh scenes the grid and the BVH are within 15 %. The BVH
stays the default at both levels; the grid is kept as an alternative and
as the [SNYD1987] reference implementation.

