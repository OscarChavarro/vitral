#ifndef PROTOCOL_HPP
#define PROTOCOL_HPP

#include <cstdint>

#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Df.h"
#include "vsdk/toolkit/common/linealAlgebra/Quaternionf.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Quaterniond.h"

struct MarkerPose {
    int markerId = -1;
    Vector3Df position;
    Quaternionf rotation;
    double decisionMargin = 0;
    float viewDot = 1.0f;
};

struct MarkerGroupPose {
    java::String label;
    Vector3Dd position;
    Quaterniond rotation;
};

#endif
