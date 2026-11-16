#ifndef __RIGID_MOTION_INVARIANCE_GATE__
#define __RIGID_MOTION_INVARIANCE_GATE__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

/**
Java's `RigidMotionInvarianceGateTest.GENERIC_ROTATION`, shared with
`SetOpContractTest`. The gate itself is slow and lives in
`vitral_base_slow_tests`, so the constant is kept here for both
executables.
*/
inline Matrix4x4d rigidMotionGenericRotation()
{
    return Matrix4x4d().axisRotation(0.61, 0.3, 1.0, 0.2);
}

#endif
