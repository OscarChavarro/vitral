#ifndef VSDK_RAYCROSSINGQUERY_H
#define VSDK_RAYCROSSINGQUERY_H
#include <math.h>
class RayCrossingQuery { public:
    double minT, maxT, minimumChordSquared, minimumExteriorApproach;
    RayCrossingQuery(double minT, double maxT, double chord = 0.0, double approach = -HUGE_VAL)
      : minT(minT), maxT(maxT), minimumChordSquared(chord), minimumExteriorApproach(approach) {}
};
#endif
