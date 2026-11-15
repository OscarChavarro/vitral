#ifndef __RAY_CROSSING_TEST_COLLECTOR__
#define __RAY_CROSSING_TEST_COLLECTOR__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/element/RayCrossingSink.h"

class RayCrossingTestCollector : public RayCrossingSink {
public:
    ArrayList<double> roots;
    ArrayList<RayCrossingKind::Value> kinds;
    ArrayList<const Geometry*> geometries;
    ArrayList<int> surfaceIndices;
    ArrayList<int> instanceCounts;

    RayCrossingTestCollector();
    virtual ~RayCrossingTestCollector();
    virtual void crossing(const RayCrossing& event);
};

#endif
