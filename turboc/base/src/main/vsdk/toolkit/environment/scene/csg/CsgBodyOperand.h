#ifndef __CSG_BODY_OPERAND__
#define __CSG_BODY_OPERAND__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Class.h"

#include "vsdk/toolkit/environment/scene/csg/CsgOperandView.h"
class SceneSolid;

// Borrowed scene body (any SceneSolid); retains its existing geometry and affine placement.
class CsgBodyOperand : public CsgOperandView {
private:
    const SceneSolid& body;
public:
    static const Class CLASS;
    virtual const Class *getClass() const;
    explicit CsgBodyOperand(const SceneSolid& body);
    virtual GeometryContainment::Value classifyPointCertified(const Vector3Dd& point, double tolerance) const;
    virtual void getSolidBounds(GeometryBounds& result) const;
    virtual RaySolidQueryStatus::Value classifyRay(const Ray& ray, const RayCrossingQuery& query,
        RaySolidClassification& result) const;
    virtual BOOLEAN isSameSolid(const CsgOperandView& other) const;
    // A body whose geometry is a CSG expression is traversed structurally only
    // when the body denotes exactly that placed geometry.
    virtual BOOLEAN isStructuralTraversalSafe() const;
    virtual BOOLEAN queryModificationVersion(long long& version) const;
    virtual const CsgBodyOperand* getBodyOperand() const;
    virtual const Geometry* getGeometry() const;
    virtual Matrix4x4d getAffinePlacement() const;
    virtual AffinePlacementStatus::Value queryAffinePlacement(Matrix4x4d& forward, Matrix4x4d& inverse) const;
};
#endif
