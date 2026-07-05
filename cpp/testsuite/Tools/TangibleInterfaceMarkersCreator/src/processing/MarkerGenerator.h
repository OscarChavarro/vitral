#ifndef __MARKERGENERATOR__
#define __MARKERGENERATOR__

#include "model/Marker.h"
#include <apriltag.h>
class MarkerGenerator {
public:
    MarkerGenerator();
    ~MarkerGenerator();

    Marker generate(int id) throw();

    // Largest valid marker id in the tag36h11 family.
    static constexpr int maxId() { return MAX_ID; }

private:
    apriltag_family_t* family;
    static constexpr int MAX_ID = 586;
};

#endif
