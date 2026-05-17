#ifndef __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLEBODYGROUP_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLEBODYGROUP_H__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

#include <string>
#include <vector>

class SimpleBody;
class Ray;

class SimpleBodyGroup {
private:
    std::vector<SimpleBody*> bodies;
    Vector3Dd position;
    Vector3Dd scale;
    Matrix4x4d rotation;
    Matrix4x4d rotation_i;
    std::string name;

public:
    SimpleBodyGroup();
    virtual ~SimpleBodyGroup() {}

    std::vector<SimpleBody*>& getBodies();

    const std::string& getName() const;
    void setName(const std::string& n);

    Matrix4x4d getRotation() const;
    void setRotation(const Matrix4x4d& rotation);

    Matrix4x4d getRotationInverse() const;
    void setRotationInverse(const Matrix4x4d& rotationi);

    Vector3Dd getPosition() const;
    void setPosition(const Vector3Dd& p);

    Vector3Dd getScale() const;
    void setScale(const Vector3Dd& s);

    Matrix4x4d getTransformationMatrix() const;

    Ray* doIntersection(const Ray& inOutRay);
    double* getMinMax();
};

#endif
