# BVH for triangle meshes (CPU)

References:
[WALD2007] Wald, Ingo. "On fast Construction of SAH-based Bounding Volume
Hierarchies". Proceedings of the 2007 IEEE Symposium on Interactive Ray
Tracing, 2007, pp. 33-40. (`doc/references/[WALD2007]_*.pdf`)

## Starting point (2026-11-15)

- `TriangleMesh::doIntersectionFirstHit` tested every triangle (brute force);
  the only filter was the bounding box of the whole mesh, recomputed from
  every vertex on every ray (`SurfaceRayIntersection` → `getMinMax()`).
- `TriangleMeshGroup` (one per OBJ file, one mesh per `g` / `usemtl`) looped
  over all its meshes for every ray.
- `AabbBinHierarchy` existed in `boundingVolumeHierarchy` but nothing used
  it.

Baseline, `RaytracingOfflineExample --parallel` on
`etc/geometry/mitscenes/balls_cowobj.ray` (1280x720, `cow.obj`: 2903
vertices, 3263 faces, about 5800 triangles, in 172 meshes), Intel Xeon Gold 6154, 72 threads,
Release build:

| run | render time |
|---|---|
| 1 | 10.116 s |
| 2 | 10.115 s |
| 3 | 10.115 s |

The same scene without the cow renders in 0.104 s: the mesh was the whole
cost.

## Design

All CPU, single threaded build, no STL, no GPU: usable on every vitral
platform (minimal kernel, Turbo C port).

- `TriangleMeshAccelerationMethod` (enum): `BRUTE_FORCE`,
  `WALD2007_BINNED_SAH`. Selected by
  `RendererConfiguration::setMeshAccelerationMethod` (default
  `WALD2007_BINNED_SAH`); `RaytracingOfflineExample -m bruteforce|wald2007`.
- `TriangleAccelerationStructure`: abstract mesh level structure. A new
  scheme is a new enum constant, a subclass and a case in
  `TriangleAccelerationStructure::create`.
- `BinnedSahHierarchy`: generic binned SAH BVH over boxes, the double
  precision port of `VulkanBvhBuilder` (Vulkan 1.4 raytracer, compute path)
  with the same parameters (12 bins, leaves of at most 4, depth at most 30)
  and the traversal of `casTraverseBlas` (nearest child first). Deviations
  from [WALD2007] are listed in its header.
- Two levels, as the Vulkan BLAS / TLAS:
  - `BinnedSahTriangleHierarchy`: triangles of one `TriangleMesh`.
  - `TriangleMeshGroup`: a `BinnedSahHierarchy` over the boxes of its
    meshes.
- `SimpleRaytracer::prepareAccelerationStructures` builds them before
  tracing (`SimpleRaytracer::execute`, and `ParallelRaytracer` before
  starting its workers); later calls only read.
- The cached vertex bounds of the structures replace the per ray
  `getMinMax()` of the bounds pre-test.

Exactness: every method gives the brute force hit bit for bit (same
`Triangle::intersectRay` on the same values, padded boxes, nodes skipped
only when strictly beyond the closest hit, ties to the lowest triangle /
mesh index).

## Results (2026-11-16)

`balls_cowobj.ray`, same machine, `--parallel`, 72 threads:

| method | render time (3 runs) | speed-up | image |
|---|---|---|---|
| baseline (before changes) | 10.115 s | 1x | reference |
| `bruteforce` (after changes) | 7.66 - 7.71 s | 1.3x | identical |
| `wald2007`, mesh level only | 6.31 - 6.41 s | 1.6x | identical |
| `wald2007`, + cached bounds | 2.36 - 2.41 s | 4.3x | identical |
| `wald2007`, + mesh group level | 0.454 - 0.455 s | 22x | identical |

- Build time of all structures: 0.002 s.
- `wald2007` serial (1 thread): 3.544 s, faster than brute force with 72
  threads.
- `bruteforce` gained from skipping redundant writes of the legacy last hit
  indexes (`TriangleMeshIntersectionIndex`, `TriangleMeshGroup`), shared by
  all the threads.
- Identical images (`cmp`) between both methods for all 30 scenes of
  `etc/geometry/mitscenes`; `cow.ray`: 0.70 s → 0.10 s.
- Tests: `triangle_acceleration_tests.cpp` (exact match against brute force
  on soups, shared edge grids and a 60 mesh group; determinism; depth
  bound; copies and invalidation). `vitral_base_tests`: 354 passed.

## Pending

- `TriangleMesh::queryFirstHit` / `emitRayCrossings` (CSG and typed
  queries) still use the linear loop: their contract reports
  `Indeterminate` triangles, so they need their own traversal.
- Scene level: `SimpleRaytracer` still loops over all bodies
  (`selectNearestThingInRayDirection`, shadow rays).
- Java and Turbo C ports of the new classes.
- `balls_featured_object.ray` takes 4 s with either method: not mesh bound,
  to be profiled.
