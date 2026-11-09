#ifndef VSDK_RAYCROSSINGSINK_H
#define VSDK_RAYCROSSINGSINK_H
#include "vsdk/toolkit/environment/geometry/element/RayCrossing.h"
class RayCrossingSink { public: virtual ~RayCrossingSink() {} virtual void crossing(const RayCrossing& event) = 0; };
#endif
