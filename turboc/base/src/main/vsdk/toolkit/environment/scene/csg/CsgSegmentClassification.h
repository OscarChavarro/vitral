#ifndef __CSG_SEGMENT_CLASSIFICATION__
#define __CSG_SEGMENT_CLASSIFICATION__

#include "vsdk/toolkit/common/VSDK.h"

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/scene/csg/CsgOperation.h"
#include "vsdk/toolkit/environment/scene/csg/CsgSegmentEvent.h"

/**
The fast [ROTH1982] section 3.3 ray classification over the caller's event
type: the state at the start of the ray and the sorted events where it
alternates. A leaf classification is built from one containment sample and
the leaf's sorted crossings (`append` toggles the state); `merge` combines two
classifications by `CsgMembershipRule::combine` (Table 3), keeping only the
events where the combined state changes. Ties take the left event first.
No tolerance, certificate or provenance is involved: renderers use it when
their own crossings and containment are trusted; certified ray
classification is `CsgRothRayClassifier`.
*/
template <class Payload>
class CsgSegmentClassification {
private:
    ArrayList<CsgSegmentEvent<Payload> > events;
    BOOLEAN initialInside;
    BOOLEAN currentInside;
public:
    // An empty classification starting inside or outside, with room for
    // expectedEvents events.
    CsgSegmentClassification(BOOLEAN initialInside, long int expectedEvents);
    // Appends the next sorted event of a leaf, toggling the state.
    void append(double t, const Payload& payload);
    const ArrayList<CsgSegmentEvent<Payload> >& getEvents() const;
    BOOLEAN isInitialInside() const;
    static CsgSegmentClassification<Payload> merge(
        const CsgSegmentClassification<Payload>& left,
        const CsgSegmentClassification<Payload>& right,
        CsgOperation::Value operation);
};

#endif
