# Flat binary tree contract (bounding volume hierarchies)

References:
[WALD2007] Wald, Ingo. "On fast Construction of SAH-based Bounding Volume
Hierarchies". Proceedings of the 2007 IEEE Symposium on Interactive Ray
Tracing, 2007, pp. 33-40. (`doc/references/[WALD2007]_*.pdf`)

This is the format that every binary bounding volume hierarchy of vitral
produces and every traversal consumes, on the CPU
(`environment/geometry/boundingVolumeHierarchy`) and on the GPU (Vulkan 1.4
compute path, `computeAccelerationStructure.glsl`). A builder that writes it
works with all the traversals; a traversal that reads it works with any
builder. It is the boundary between "how the tree is built" (the strategy)
and "how it is walked".

## 1. Arrays

A hierarchy over `N` entries (triangles, meshes, instances...) is two
arrays, both indexed from 0:

| Array | Element | Size |
|---|---|---|
| `nodes` | node record (below) | `1` when `N == 0`, at most `2N - 1` otherwise |
| `order` | entry index (`int`) | `N`, a permutation of `0 .. N - 1` |

Node record (`BinnedSahHierarchyNode` in double precision on the CPU,
`VulkanBvhNode` / GLSL `CasNode` in single precision on the GPU, 32 bytes,
`std430`):

```
struct Node {
    lo[3];          // box minimum
    int leftOrFirst;
    hi[3];          // box maximum
    int count;
};
```

- **Inner node** (`count == 0`): its two children are `nodes[leftOrFirst]`
  and `nodes[leftOrFirst + 1]`. Siblings are always stored next to each
  other ([WALD2007].3.6), so one index is enough.
- **Leaf** (`count > 0`): its entries are
  `order[leftOrFirst] .. order[leftOrFirst + count - 1]`, one contiguous
  block of `order`.

## 2. Invariants

1. The root is `nodes[0]`.
2. Every entry appears in exactly one leaf, so `order` is a permutation.
3. A node box encloses the boxes of its children (inner node) or of its
   entries (leaf). Boxes are closed: `[lo, hi]`.
4. **Empty boxes**: a box with `lo > hi` on any axis is empty. Builders
   start from the "negative box" `[+1e30, -1e30]` ([WALD2007].3.4), and an
   entry may have an empty box (an empty instance or mesh). Every
   traversal must treat an empty box as a miss *before* reading
   `leftOrFirst` / `count`: the hierarchy over zero entries is a single
   node with an empty box, `count == 0` and `leftOrFirst == 0` (it would
   otherwise read as an inner node pointing to itself).
5. **Depth** (a single leaf has depth 1) is at most
   `BinnedSahHierarchy::MAXIMUM_DEPTH` (30). Traversal stacks are sized
   from it: `BinnedSahHierarchy::STACK_SIZE` (64) on the CPU,
   `CAS_STACK_SIZE` (32) in GLSL; a nearest-first traversal never holds
   more than `depth + 1` entries.
6. **Determinism**: the same input gives bit for bit the same arrays (no
   threads, no randomness, stable partitions). Statistics and images can
   be compared between runs and between machines.
7. Node order is depth first, left subtree first. No traversal depends on
   it, but it is part of the determinism above and gives some locality.

Indices are relative to the two arrays. A consumer that concatenates
several hierarchies in one buffer (the GPU bottom level holds every mesh in
one node buffer and one triangle buffer) rebases them: `leftOrFirst +=
nodeOffset` for inner nodes, `leftOrFirst += entryOffset` for leaves, and
leaves nothing to rebase for the single node of an empty hierarchy.

## 3. Boxes and precision

- Builders work on the boxes the caller gives, six doubles per entry
  (`lo x, y, z, hi x, y, z`). The caller pads them so that rounding in the
  exact entry test never produces a hit outside the box
  (`TriangleAccelerationStructure::padBox`; instance world boxes are padded
  the same way by `BinnedSahInstanceHierarchy`).
- The GPU copy is single precision. `VulkanComputeAccelerationStructure`
  rounds every box **outward** when it converts it (`lo` towards -infinity,
  `hi` towards +infinity), so the float box always encloses the double box
  and the conversion never culls a hit. Empty boxes stay empty.
- Instance level boxes are world boxes: the eight corners of the object box
  of the instance, transformed by its object to world matrix.

## 4. Levels

Two level structures, as Vulkan BLAS / TLAS:

| Level | Entries | CPU class | GPU buffers |
|---|---|---|---|
| primitive (bottom) | triangles of one mesh | `BinnedSahTriangleHierarchy` | `casBlasNodes`, `casTriangles` |
| instance (top) | meshes of a `TriangleMeshGroup` | `TriangleMeshGroup` (its `BinnedSahHierarchy`) | — |
| instance (top) | transformed instances of bottom levels | `BinnedSahInstanceHierarchy` | `casTlasNodes`, `casInstances` |

For the instance level with transforms, the entry test takes the world ray
to object space with the world to object matrix of the instance, without
normalizing the direction, so a parameter `t` keeps its meaning across
instances (`BinnedSahInstanceVisitor`, `casTraverse`).

Leaf sizes: bottom levels use `BinnedSahHierarchy::DEFAULT_MAXIMUM_LEAF_SIZE`
(4); instance levels use 1 (`BinnedSahInstanceHierarchy::
DEFAULT_MAXIMUM_LEAF_SIZE`), as a hardware TLAS. `TriangleMeshGroup` keeps
the default of 4.

## 5. Producers and consumers

There is a single builder, `BinnedSahHierarchy::build` (binned SAH of
[WALD2007].3, deviations listed in its header). Everything else consumes
its output:

| Consumer | Where | Order |
|---|---|---|
| `BinnedSahHierarchy::traverseClosest` | CPU, generic (visitor per entry) | nearest child first |
| `BinnedSahTriangleHierarchy::firstHit` | CPU, triangles | nearest child first |
| `BinnedSahInstanceHierarchy::traverseClosest` | CPU, instances | nearest child first |
| `casTraverseBlas` | GLSL, bottom level | nearest child first |
| `casTraverse` | GLSL, top level | fixed (left first), box tested when popped |
| `VulkanComputeAccelerationStructure` | flattens to `VulkanBvhNode` (float, outward rounding), rebases, uploads | — |

## 6. Strategies

A different construction strategy is a different producer of the same two
arrays. All the traversals above, CPU and GPU, keep working unchanged as
long as it respects section 2. Examples that fit the contract: object
median or spatial median splits, binned SAH with other bin counts or only
the widest axis, full sweep SAH, LBVH (Morton codes), agglomerative
clustering, refitting after deformation (same topology, new boxes), and
SBVH with an entry repeated in several leaves (it relaxes invariant 2 to
"at least once"; closest hit traversals already tolerate it).

Strategies that change the layout need a new contract and new traversals
on both sides: wide trees (BVH4 / BVH8), compressed or quantized nodes,
stackless traversal with skip pointers (ropes).

## 7. Traversal counters

Both sides count the same quantities per level, so strategies can be
compared by work and not only by time:

| Quantity | Meaning |
|---|---|
| traversals | queries of a hierarchy (one closest or any hit query) |
| node visits | nodes taken from the stack and not skipped (box entered before the closest hit) |
| entry tests | leaf entries handed to their test: instances / meshes at the instance level, triangles at the primitive level |

- CPU: `RaytraceStatistics` (`recordInstanceLevelTraversal`,
  `recordPrimitiveLevelTraversal`, getters, `printSummary`), enabled with
  `setEnabled(true)` or the system property `vsdk.raytrace.stats`;
  `RaytracingOfflineExample --stats`. The brute force method records one
  primitive level traversal with zero node visits and every triangle
  tested.
- GPU: `VulkanComputeTraversalCounters`, a storage buffer of 64 bit
  counters (two 32 bit words with carry, no 64 bit integers needed)
  written by `computeAccelerationStructure.glsl` when the including shader
  defines `CAS_BINDING_COUNTERS` and enables `CAS_COUNTERS_ENABLED`;
  simpleRaytracer `--stats`.

The counts are not expected to match between CPU and GPU (single vs double
precision, different scenes and shading), only to mean the same thing.
