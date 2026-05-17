#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_ELEMENTS_TRIANGLE_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_ELEMENTS_TRIANGLE_H__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include <string>

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

    /**
    Provides an object to text report convertion, optimized for human
    readability and debugging. Do not use this method for serialization
    or persistence purposes.
    @return human readable representation of current triangle
    */
    std::string toString() const;
};

#endif
