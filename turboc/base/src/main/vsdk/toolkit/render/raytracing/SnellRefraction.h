#ifndef __SNELL_REFRACTION__
#define __SNELL_REFRACTION__

#include "vsdk/toolkit/common/VSDK.h"

class Vector3Dd;

/**
Direction of a ray crossing the interface between two media, by Snell's
law n1 sin(theta1) = n2 sin(theta2), in the vector form used by Whitted
[WHIT1980] and in GLSL `refract`.  Both input vectors are unit vectors on
the same side of the interface: `towardViewer` points back along the
incoming ray and `normal` points into the medium the ray comes from.
*/
class SnellRefraction {
public:
    /**
    Writes the transmitted direction for relative index eta = n1 / n2 and
    returns true, or returns false when the angle exceeds the critical
    angle (total internal reflection) and leaves `outDirection` untouched.
    */
    static BOOLEAN refract(const Vector3Dd& towardViewer, const Vector3Dd& normal,
                        double eta, Vector3Dd* outDirection);

    /** Mirror direction of `towardViewer` about `normal`. */
    static Vector3Dd reflect(const Vector3Dd& towardViewer, const Vector3Dd& normal);
};

#endif
