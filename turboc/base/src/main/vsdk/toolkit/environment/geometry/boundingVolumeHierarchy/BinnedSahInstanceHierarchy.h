//= References:                                                             =
//= [WALD2007] Wald, Ingo. "On fast Construction of SAH-based Bounding      =
//=     Volume Hierarchies". Proceedings of the 2007 IEEE Symposium on      =
//=     Interactive Ray Tracing, 2007, pp. 33-40.                           =

#ifndef __BNND_SAH_INSTNC_HRRCHY__
#define __BNND_SAH_INSTNC_HRRCHY__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/BinnedSahHierarchy.h"

/**
Leaf test of `BinnedSahInstanceHierarchy::traverseClosest`: tests the
geometry of one instance with the ray already taken to its object space,
and lowers `inOutBestT` when it finds a closer hit (or applies its own tie
rule, for instance the lowest instance index on equal distances).
*/
class BinnedSahInstanceVisitor {
  public:
    virtual ~BinnedSahInstanceVisitor();
    /**
    @param objectRay the world ray with origin and direction multiplied by
    the world to object matrix of the instance; the direction is not
    normalized, so a parameter `t` along it is the same `t` along the world
    ray, and `inOutBestT` is comparable across instances
    */
    virtual void visit(int instance, const Ray &objectRay, double &inOutBestT) = 0;
};

/**
Top level of a two level acceleration structure: a `BinnedSahHierarchy`
over instances, each one an object space box (the bounds of its bottom
level, for example a `BinnedSahTriangleHierarchy`) placed in the world by
an affine object to world matrix. It is the CPU counterpart of a Vulkan top
level acceleration structure over `VkAccelerationStructureInstanceKHR`, and
the Vulkan 1.4 compute path builds its top level with it.

Build: the world box of an instance encloses the eight transformed corners
of its object box, padded with `TriangleAccelerationStructure::padBox`; an
empty object box gives an empty world box (the instance is never reached).
The world to object matrices are computed once (`Matrix4x4d::inverse`).
Leaves hold `DEFAULT_MAXIMUM_LEAF_SIZE` (1) instance unless told otherwise,
as a hardware TLAS. The tree follows `doc/flatBinaryTreeContract.md`; its
entries are instance indices.

Rebuild it when the transforms change (the cost is that of a build over the
instance count; the bottom levels are kept).

Matrices follow the vitral convention: column vectors, translation in
column 3 (`get(row, 3)`); only the upper 3x4 part is used.
*/
class BinnedSahInstanceHierarchy {
  public:
    enum {
        DEFAULT_MAXIMUM_LEAF_SIZE = 1
    };

  private:
    BinnedSahHierarchy hierarchy;
    ArrayList<Matrix4x4d> objectToWorldMatrices;
    ArrayList<Matrix4x4d> worldToObjectMatrices;
    /** Six values per instance: padded world box */
    ArrayList<double> worldBoxes;

  public:
    BinnedSahInstanceHierarchy();

    /**
    @param objectBoxes six values per instance (lo x, y, z, hi x, y, z) in
    object space; lo > hi on an axis marks an empty instance
    @param objectToWorld one matrix per instance
    @param maximumLeafSize see `BinnedSahHierarchy::build`
    */
    void build(const ArrayList<double> &objectBoxes,
        const ArrayList<Matrix4x4d> &objectToWorld,
        int maximumLeafSize = DEFAULT_MAXIMUM_LEAF_SIZE);

    /**
    Closest hit traversal of the world ray: calls the visitor for every
    instance whose world box the ray enters before `inOutBestT`, nearest
    first, with the ray in object space. Never misses an instance: when the
    traversal stack would overflow (not reachable, see
    `BinnedSahHierarchy::traverseClosest`) every instance is visited.
    */
    void traverseClosest(const Ray &worldRay, BinnedSahInstanceVisitor &visitor,
        double &inOutBestT) const;

    /** The world ray in the object space of an instance (see the visitor) */
    Ray toObjectSpace(int instance, const Ray &worldRay) const;

    int getInstanceCount() const;
    const BinnedSahHierarchy &getHierarchy() const;
    const Matrix4x4d &getObjectToWorld(int instance) const;
    const Matrix4x4d &getWorldToObject(int instance) const;
    /** Padded world box of an instance: six values */
    void getWorldBox(int instance, double *outBox) const;

    /** `m * (p, 1)`, upper 3x4 part */
    static Vector3Dd transformPoint(const Matrix4x4d &m, const Vector3Dd &p);
    /** `m * (v, 0)`, upper 3x3 part */
    static Vector3Dd transformVector(const Matrix4x4d &m, const Vector3Dd &v);
    /** Box enclosing the eight transformed corners (unpadded); an empty box
    stays empty */
    static void transformBox(const double *objectBox, const Matrix4x4d &objectToWorld,
        double *outWorldBox);
};

#endif
