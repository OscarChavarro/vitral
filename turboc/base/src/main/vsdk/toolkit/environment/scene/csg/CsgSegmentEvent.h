#ifndef __CSG_SEGMENT_EVENT__
#define __CSG_SEGMENT_EVENT__

#include "vsdk/toolkit/common/VSDK.h"

/**
One boundary event of a ray's alternating in/out classification: the ray
parameter, whether the ray enters the solid there, and the caller's event
(a renderer's hit record, a crossing reference). [ROTH1982] section 2.2, the
t[i]/S[i] pairs of RAYCAST. The payload is copied, never interpreted.
*/
template <class Payload>
class CsgSegmentEvent {
private:
    double t;
    BOOLEAN entering;
    Payload payload;
public:
    CsgSegmentEvent() : t(0.0), entering(FALSE), payload(vsdkDefaultValue((Payload*)0)) {}
    CsgSegmentEvent(double t, BOOLEAN entering, const Payload& payload) :
        t(t), entering(entering), payload(payload) {}
    double getT() const { return t; }
    BOOLEAN isEntering() const { return entering; }
    const Payload& getPayload() const { return payload; }
};

#endif
