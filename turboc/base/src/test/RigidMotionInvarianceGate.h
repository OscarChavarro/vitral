#ifndef __RIGID_MOTION_GATE__
#define __RIGID_MOTION_GATE__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

/**
Java's `RigidMotionInvarianceGateTest.GENERIC_ROTATION`, shared by the set
operation tests: `new Matrix4x4d().axisRotation(0.61, 0.3, 1.0, 0.2)`.
C++ port note: a function, as the test programs of the port are separate
executables and a static object of a header would be built in each.
*/
inline Matrix4x4d
rigidMotionGenericRotation()
{
    return Matrix4x4d().axisRotation(0.61, 0.3, 1.0, 0.2);
}

#endif
