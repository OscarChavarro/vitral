#ifndef __CSG_PLACED_EXPRESSION__
#define __CSG_PLACED_EXPRESSION__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Class.h"
#include "vsdk/toolkit/environment/scene/AffinePlacement.h"
#include "vsdk/toolkit/environment/scene/csg/CsgOperandView.h"
class CsgExpression;
// Borrowed child expression and an independently editable child-to-parent placement.
class CsgPlacedExpression : public CsgOperandView {
private:
    const CsgExpression& child;
    AffinePlacement placement;
    long long modificationVersion;
public:
    static const Class CLASS;
    virtual const Class *getClass() const;
    explicit CsgPlacedExpression(const CsgExpression& child);
    AffinePlacementStatus::Value setAffinePlacement(const Matrix4x4d& forward, const Matrix4x4d& inverse);
    virtual BOOLEAN isSameSolid(const CsgOperandView& other) const;
    virtual const Geometry* getGeometry() const;
    virtual Matrix4x4d getAffinePlacement() const;
    virtual AffinePlacementStatus::Value queryAffinePlacement(Matrix4x4d& forward, Matrix4x4d& inverse) const;
    virtual const CsgExpression* getChildExpression() const;
    virtual BOOLEAN isStructuralTraversalSafe() const;
    virtual BOOLEAN queryModificationVersion(long long& version) const;
};
#endif
