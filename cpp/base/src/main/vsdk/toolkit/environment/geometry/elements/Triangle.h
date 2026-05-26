#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_ELEMENTS_TRIANGLE_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_ELEMENTS_TRIANGLE_H__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/elements/Intersection.h"
#include "java/lang/String.h"
#include "java/lang/String.h"
#include "java/lang/String.h"

class Ray;

class Triangle
{
public:
    int p0;
    int p1;
    int p2;

    Vector3Dd normal;

    Triangle();
    Triangle(int p0, int p1, int p2);

    int getPoint0() const;
    int getPoint1() const;
    int getPoint2() const;

    void setPoint0(int p0);
    void setPoint1(int p1);
    void setPoint2(int p2);

    static Intersection* doIntersectionWithTriangle(
        const Ray& ray,
        const Vector3Dd& v0,
        const Vector3Dd& v1,
        const Vector3Dd& v2);
    static int containmentTest(
        const Vector3Dd& p0,
        const Vector3Dd& p1,
        const Vector3Dd& p2,
        const Vector3Dd& p,
        double distanceTolerance);
    static void minMax(
        const Vector3Dd& p0,
        const Vector3Dd& p1,
        const Vector3Dd& p2,
        double mm[6]);

    /**
    Provides an object to text report convertion, optimized for human
    readability and debugging. Do not use this method for serialization
    or persistence purposes.
    @return human readable representation of current triangle
    */
    java::String toString() const;
};

#endif
