//= References:                                                             =
//= [WALD2007] Wald, Ingo. "On fast Construction of SAH-based Bounding      =
//=     Volume Hierarchies". Proceedings of the 2007 IEEE Symposium on      =
//=     Interactive Ray Tracing, 2007, pp. 33-40.                           =

#ifndef __BINNED_SAH_HIERARCHY__
#define __BINNED_SAH_HIERARCHY__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"

/**
One node of a `BinnedSahHierarchy`, the double precision form of
`VulkanBvhNode` (`CasNode` of `computeAccelerationStructure.glsl`):

- inner node (`count == 0`): children at `leftOrFirst` and `leftOrFirst + 1`;
- leaf (`count > 0`): entries `leftOrFirst` .. `leftOrFirst + count - 1`
  of the primitive order.

As in [WALD2007].3.6: the child pair of an inner node is stored next to
each other, a leaf refers to one contiguous block of the primitive ids.
*/
class BinnedSahHierarchyNode {
  public:
    double lo[3];
    int leftOrFirst;
    double hi[3];
    int count;
};

/**
Leaf test of `BinnedSahHierarchy::traverseClosest`: tests one primitive and
lowers `inOutBestT` when it finds a closer hit (or applies its own tie
rule). The traversal only skips nodes whose entry is beyond `inOutBestT`.
*/
class BinnedSahLeafVisitor {
  public:
    virtual ~BinnedSahLeafVisitor();
    virtual void visit(int primitive, double &inOutBestT) = 0;
};

/**
Binary bounding volume hierarchy over axis aligned primitive boxes, built
top-down with the binned surface area heuristic of [WALD2007].3. It is the
CPU, double precision port of `VulkanBvhBuilder` (Vulkan 1.4 raytracer,
compute path without `VK_KHR_acceleration_structure`), with the same
parameters, and serves both levels as there: the triangles of a mesh
(`BinnedSahTriangleHierarchy`, bottom level) and the meshes of a
`TriangleMeshGroup` (top level). Its traversal is the one of
`casTraverseBlas` in `computeAccelerationStructure.glsl`: a stack, nearest
child first, nodes farther than the closest hit skipped.

Build, following [WALD2007].3: primitives are binned by the centroid of
their box into `BIN_COUNT` bins that uniformly subdivide the centroid bounds
([WALD2007].3.1, .3.3); each bin keeps its count and the full 3D box of its
members, grown from a negative box ([WALD2007].3.4); the cost
`A_L N_L + A_R N_R` of the `BIN_COUNT - 1` partitions is evaluated with one
sweep from each side ([WALD2007].3.1, .3.5); the build rearranges one
primitive id array, with a temporary array ([WALD2007].3.6).

Deviations from [WALD2007], the same of `VulkanBvhBuilder`:
- the three axes are binned, not only the widest one (the alternative
  mentioned in [WALD2007].3.1);
- 12 bins (16 recommended, 4 already reasonable, [WALD2007].3.7);
- bin index clamped to `[0, BIN_COUNT - 1]` instead of the `K(1 - epsilon)`
  factor of [WALD2007].3.3;
- termination ([WALD2007].3.8): a node with all its centroids at one point
  is split in halves instead of becoming a leaf, a split more expensive
  than the leaf is still taken while the node holds more than twice
  `MAXIMUM_LEAF_SIZE` primitives, and the depth is capped by
  `MAXIMUM_DEPTH` (bounded traversal stack);
- single threaded and deterministic: the parallel build of [WALD2007].4 is
  not used, so it runs on every vitral platform.
*/
class BinnedSahHierarchy {
  public:
    enum {
        BIN_COUNT = 12,
        MAXIMUM_DEPTH = 30,
        MAXIMUM_LEAF_SIZE = 4,
        /** Traversal stack; it never holds more than `MAXIMUM_DEPTH + 1`
        entries */
        STACK_SIZE = 64
    };

  private:
    ArrayList<BinnedSahHierarchyNode> nodes;
    ArrayList<int> primitiveOrder;

  public:
    BinnedSahHierarchy();

    /**
    @param boxes six values per primitive: lo x, y, z, hi x, y, z. Callers
    pad the boxes so rounding never hides a hit.
    */
    void build(const ArrayList<double> &boxes);

    /**
    Closest hit traversal: calls the visitor for the primitives of every
    leaf the ray reaches before `inOutBestT`, nearest nodes first. A node
    whose entry equals `inOutBestT` is still visited, so a visitor
    resolving ties by primitive index gets the same answer as a linear
    loop. Returns false when the stack would overflow (not reachable with
    `MAXIMUM_DEPTH < STACK_SIZE`); the caller then uses its linear loop.
    */
    BOOLEAN traverseClosest(const Ray &ray, BinnedSahLeafVisitor &visitor,
        double &inOutBestT) const;

    const ArrayList<BinnedSahHierarchyNode> &getNodes() const;
    /** Primitive ids in leaf order */
    const ArrayList<int> &getPrimitiveOrder() const;
    long int getNodeCount() const;
    /** Depth of the hierarchy (a single leaf has depth 1) */
    int getDepth() const;

    /** Slab entry parameters of a ray, shared by the traversals */
    class RaySlab {
      public:
        double origin[3];
        double inverse[3];
        BOOLEAN parallel[3];
        explicit RaySlab(const Ray &ray);
        /** Entry parameter of the ray in the node box clipped to [0, tMax],
        or -1 on a miss (`casSlab`) */
        double entry(const BinnedSahHierarchyNode &node, double tMax) const;
    };
};

inline BinnedSahHierarchy::RaySlab::RaySlab(const Ray &ray)
{
    const Vector3Dd &o = ray.getOrigin();
    const Vector3Dd &d = ray.getDirection();
    origin[0] = o.x(); origin[1] = o.y(); origin[2] = o.z();
    // C++ port note: filled one by one, without an aggregate initializer of
    // non constant values
    double direction[3];
    direction[0] = d.x(); direction[1] = d.y(); direction[2] = d.z();
    // Only a component exactly zero is parallel: any other one, however
    // small, uses its reciprocal, so no hit is lost to the slab test.
    for (int a = 0; a < 3; a++) {
        parallel[a] = direction[a] == 0.0;
        inverse[a] = parallel[a] ? 0.0 : 1.0 / direction[a];
    }
}

inline double
BinnedSahHierarchy::RaySlab::entry(const BinnedSahHierarchyNode &node, double tMax) const
{
    double tNear = 0.0;
    double tFar = tMax;
    for (int a = 0; a < 3; a++) {
        if (parallel[a]) {
            if (origin[a] < node.lo[a] || origin[a] > node.hi[a]) {
                return -1.0;
            }
            continue;
        }
        double t0 = (node.lo[a] - origin[a]) * inverse[a];
        double t1 = (node.hi[a] - origin[a]) * inverse[a];
        if (t0 > t1) {
            const double swap = t0;
            t0 = t1;
            t1 = swap;
        }
        tNear = t0 > tNear ? t0 : tNear;
        tFar = t1 < tFar ? t1 : tFar;
        if (tNear > tFar) {
            return -1.0;
        }
    }
    return tNear;
}

#endif
