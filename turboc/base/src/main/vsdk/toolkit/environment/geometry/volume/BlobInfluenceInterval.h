#ifndef __BLOB_INFLUENCE_INTERVAL__
#define __BLOB_INFLUENCE_INTERVAL__

#include "vsdk/toolkit/common/VSDK.h"

// Start or end, along a ray, of the influence sphere of one blob component.
class BlobInfluenceInterval {
private:
    BOOLEAN starting;
    long int component;
    double bound;
public:
    BlobInfluenceInterval() : starting(TRUE), component(0), bound(0) {}
    BlobInfluenceInterval(BOOLEAN isStart, long int componentIndex, double distance) :
        starting(isStart), component(componentIndex), bound(distance) {}
    BOOLEAN isStart() const { return starting; }
    long int getComponent() const { return component; }
    double getBound() const { return bound; }
};
#endif
