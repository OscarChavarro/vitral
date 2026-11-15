#ifndef __CSG_TEST_OPERAND__
#define __CSG_TEST_OPERAND__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/scene/csg/CsgOperandView.h"
class CsgTestOperand : public CsgOperandView {
private:
    GeometryContainment::Value result;
    GeometryContainment::Value smallerResult;
    double switchLimit;
    BOOLEAN malformedRay;
public:
    explicit CsgTestOperand(GeometryContainment::Value value);
    CsgTestOperand(GeometryContainment::Value value, GeometryContainment::Value smaller, double limit);
    void setMalformedRay(BOOLEAN value);
    virtual RaySolidQueryStatus::Value classifyRay(const Ray& ray, const RayCrossingQuery& query,
        RaySolidClassification& result) const;
    virtual const Geometry* getGeometry() const;
    virtual Matrix4x4d getAffinePlacement() const;
    virtual GeometryContainment::Value classifyPointCertified(const Vector3Dd&, double) const;
};
#endif
