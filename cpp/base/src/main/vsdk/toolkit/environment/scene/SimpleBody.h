#ifndef __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLEBODY_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_SCENE_SIMPLEBODY_H__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Geometry;
class Ray;

class SimpleBody {
private:
    Vector3Dd position;
    Geometry* geometry;

public:
    SimpleBody();
    void setPosition(const Vector3Dd& p);
    void setGeometry(Geometry* g);
    Ray* doIntersection(const Ray& inRay) const;
};

#endif
