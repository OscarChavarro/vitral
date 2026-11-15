#ifndef __CSG_OPERAND_VIEW__
#define __CSG_OPERAND_VIEW__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Class.h"

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/scene/AffinePlacementStatus.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryContainment.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.h"
class Geometry;
class GeometryBounds;
class CsgExpression;
class CsgBodyOperand;
class Ray;
class RayCrossingQuery;
class RaySolidClassification;

// A borrowed operand view; its geometry, child expression and placement live
// at least as long as the expression that references this view.
class CsgOperandView {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;
    virtual ~CsgOperandView();
    virtual const Geometry* getGeometry() const = 0;
    virtual Matrix4x4d getAffinePlacement() const = 0;
    // Output matrices are usable only when the returned status is Valid.
    virtual AffinePlacementStatus::Value queryAffinePlacement(Matrix4x4d& forward, Matrix4x4d& inverse) const;
    virtual const CsgExpression* getChildExpression() const;
    // Opt in only when child and placement fully describe this view's solid.
    virtual BOOLEAN isStructuralTraversalSafe() const;
    // True only when both views denote the very same placed solid.
    virtual BOOLEAN isSameSolid(const CsgOperandView& other) const;
    // World-space certified query; unknown operand implementations stay explicit.
    virtual GeometryContainment::Value classifyPointCertified(const Vector3Dd& point, double tolerance) const;
    // Conservative parent-space bounds of a leaf's solid; Unsupported by default.
    virtual void getSolidBounds(GeometryBounds& result) const;
    // Solid segments of a leaf in this operand's parent coordinates.
    virtual RaySolidQueryStatus::Value classifyRay(const Ray& ray, const RayCrossingQuery& query,
        RaySolidClassification& result) const;
    // Counter that changes whenever this view's solid or placement is edited
    // through its API; false when the view does not track its edits.
    virtual BOOLEAN queryModificationVersion(long long& version) const;
    // C++ port note: replaces the dynamic_cast to CsgBodyOperand of the cpp
    // version (no RTTI on legacy compilers); NULL unless this is one.
    virtual const CsgBodyOperand* getBodyOperand() const;
};
#endif
