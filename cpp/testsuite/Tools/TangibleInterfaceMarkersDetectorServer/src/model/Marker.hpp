#ifndef __MARKER__
#define __MARKER__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Quaterniond.h"

struct Marker {
    int id = -1;
    Vector3Dd position;
    Quaterniond rotation;
    double physicalSideLength = 0.035;
};

#endif
