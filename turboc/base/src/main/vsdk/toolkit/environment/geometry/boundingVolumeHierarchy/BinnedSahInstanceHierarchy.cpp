//= References:                                                             =
//= [WALD2007] Wald, Ingo. "On fast Construction of SAH-based Bounding      =
//=     Volume Hierarchies". Proceedings of the 2007 IEEE Symposium on      =
//=     Interactive Ray Tracing, 2007, pp. 33-40.                           =

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/BinnedSahInstanceHierarchy.h"
#include "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/TriangleAccelerationStructure.h"

BinnedSahInstanceVisitor::~BinnedSahInstanceVisitor()
{
}

/** Leaf test of the inner hierarchy: takes the ray to object space */
class _BinnedSahInstanceAdapter : public BinnedSahLeafVisitor {
  public:
    const BinnedSahInstanceHierarchy &owner;
    const Ray &worldRay;
    BinnedSahInstanceVisitor &visitor;

    _BinnedSahInstanceAdapter(const BinnedSahInstanceHierarchy &inOwner,
        const Ray &inWorldRay, BinnedSahInstanceVisitor &inVisitor)
        : owner(inOwner), worldRay(inWorldRay), visitor(inVisitor) {}

    void visit(int instance, double &inOutBestT)
    {
        visitor.visit(instance, owner.toObjectSpace(instance, worldRay), inOutBestT);
    }
};

BinnedSahInstanceHierarchy::BinnedSahInstanceHierarchy()
{
}

Vector3Dd
BinnedSahInstanceHierarchy::transformPoint(const Matrix4x4d &m, const Vector3Dd &p)
{
    return Vector3Dd(
        m.get(0, 0) * p.x() + m.get(0, 1) * p.y() + m.get(0, 2) * p.z() + m.get(0, 3),
        m.get(1, 0) * p.x() + m.get(1, 1) * p.y() + m.get(1, 2) * p.z() + m.get(1, 3),
        m.get(2, 0) * p.x() + m.get(2, 1) * p.y() + m.get(2, 2) * p.z() + m.get(2, 3));
}

Vector3Dd
BinnedSahInstanceHierarchy::transformVector(const Matrix4x4d &m, const Vector3Dd &v)
{
    return Vector3Dd(
        m.get(0, 0) * v.x() + m.get(0, 1) * v.y() + m.get(0, 2) * v.z(),
        m.get(1, 0) * v.x() + m.get(1, 1) * v.y() + m.get(1, 2) * v.z(),
        m.get(2, 0) * v.x() + m.get(2, 1) * v.y() + m.get(2, 2) * v.z());
}

void
BinnedSahInstanceHierarchy::transformBox(const double *objectBox,
    const Matrix4x4d &m, double *outWorldBox)
{
    int a;
    for (a = 0; a < 3; a++) {
        outWorldBox[a] = 1e30;
        outWorldBox[a + 3] = -1e30;
    }
    if (objectBox[0] > objectBox[3] || objectBox[1] > objectBox[4] ||
        objectBox[2] > objectBox[5]) {
        return;
    }
    for (int corner = 0; corner < 8; corner++) {
        const Vector3Dd p((corner & 1) ? objectBox[3] : objectBox[0],
            (corner & 2) ? objectBox[4] : objectBox[1],
            (corner & 4) ? objectBox[5] : objectBox[2]);
        const Vector3Dd w = transformPoint(m, p);
        // C++ port note: filled one by one, without an aggregate
        // initializer of non constant values
        double c[3];
        c[0] = w.x(); c[1] = w.y(); c[2] = w.z();
        for (a = 0; a < 3; a++) {
            outWorldBox[a] = c[a] < outWorldBox[a] ? c[a] : outWorldBox[a];
            outWorldBox[a + 3] = c[a] > outWorldBox[a + 3] ? c[a] : outWorldBox[a + 3];
        }
    }
}

void
BinnedSahInstanceHierarchy::build(const ArrayList<double> &objectBoxes,
    const ArrayList<Matrix4x4d> &objectToWorld, int maximumLeafSize)
{
    long int count = objectBoxes.size() / 6;
    if (objectToWorld.size() < count) {
        count = objectToWorld.size();
    }
    objectToWorldMatrices.clear();
    worldToObjectMatrices.clear();
    worldBoxes.clear();
    worldBoxes.reserve(6 * (count > 0 ? count : 1));
    for (long int i = 0; i < count; i++) {
        const Matrix4x4d &m = objectToWorld[i];
        objectToWorldMatrices.add(m);
        worldToObjectMatrices.add(m.inverse());
        double world[6];
        transformBox(&objectBoxes.data()[6 * i], m, world);
        if (world[0] <= world[3]) {
            // [WALD2007].3.2: the build only sees these boxes; padded so
            // rounding of the transform never hides a hit.
            TriangleAccelerationStructure::padBox(world, world + 3);
        }
        for (int k = 0; k < 6; k++) {
            worldBoxes.add(world[k]);
        }
    }
    hierarchy.build(worldBoxes, maximumLeafSize);
}

Ray
BinnedSahInstanceHierarchy::toObjectSpace(int instance, const Ray &worldRay) const
{
    const Matrix4x4d &m = worldToObjectMatrices[instance];
    return Ray::fromAffine(transformPoint(m, worldRay.getOrigin()),
        transformVector(m, worldRay.getDirection()));
}

void
BinnedSahInstanceHierarchy::traverseClosest(const Ray &worldRay,
    BinnedSahInstanceVisitor &visitor, double &inOutBestT) const
{
    _BinnedSahInstanceAdapter adapter(*this, worldRay, visitor);
    if (!hierarchy.traverseClosest(worldRay, adapter, inOutBestT)) {
        // Stack overflow: visiting every instance still gives the closest
        // hit (the visitor only lowers inOutBestT).
        for (int i = 0; i < getInstanceCount(); i++) {
            adapter.visit(i, inOutBestT);
        }
    }
}

int
BinnedSahInstanceHierarchy::getInstanceCount() const
{
    return (int)objectToWorldMatrices.size();
}

const BinnedSahHierarchy &
BinnedSahInstanceHierarchy::getHierarchy() const
{
    return hierarchy;
}

const Matrix4x4d &
BinnedSahInstanceHierarchy::getObjectToWorld(int instance) const
{
    return objectToWorldMatrices[instance];
}

const Matrix4x4d &
BinnedSahInstanceHierarchy::getWorldToObject(int instance) const
{
    return worldToObjectMatrices[instance];
}

void
BinnedSahInstanceHierarchy::getWorldBox(int instance, double *outBox) const
{
    for (int k = 0; k < 6; k++) {
        outBox[k] = worldBoxes[6 * instance + k];
    }
}
