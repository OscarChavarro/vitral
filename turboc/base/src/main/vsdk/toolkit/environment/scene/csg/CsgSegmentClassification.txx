#ifndef __CSG_SGMNT_CLSSFCTN_TXX__
#define __CSG_SGMNT_CLSSFCTN_TXX__

#include "vsdk/toolkit/common/VSDK.h"

#include "vsdk/toolkit/environment/scene/csg/CsgSegmentClassification.h"
#include "vsdk/toolkit/environment/scene/csg/CsgMembershipRule.h"
#include "java/util/ArrayList.txx"

template <class Payload>
CsgSegmentClassification<Payload>::CsgSegmentClassification(BOOLEAN initialInside, long int expectedEvents) :
    events(expectedEvents), initialInside(initialInside), currentInside(initialInside)
{
}

template <class Payload>
void CsgSegmentClassification<Payload>::append(double t, const Payload& payload)
{
    currentInside = !currentInside;
    events.add(CsgSegmentEvent<Payload>(t, currentInside, payload));
}

template <class Payload>
const ArrayList<CsgSegmentEvent<Payload> >& CsgSegmentClassification<Payload>::getEvents() const
{
    return events;
}

template <class Payload>
BOOLEAN CsgSegmentClassification<Payload>::isInitialInside() const
{
    return initialInside;
}

template <class Payload>
CsgSegmentClassification<Payload> CsgSegmentClassification<Payload>::merge(
    const CsgSegmentClassification<Payload>& left,
    const CsgSegmentClassification<Payload>& right,
    CsgOperation::Value operation)
{
    const ArrayList<CsgSegmentEvent<Payload> >& leftEvents = left.events;
    const ArrayList<CsgSegmentEvent<Payload> >& rightEvents = right.events;
    BOOLEAN insideLeft = left.initialInside;
    BOOLEAN insideRight = right.initialInside;
    const BOOLEAN initialCombined = CsgMembershipRule::combine(operation, insideLeft, insideRight);
    CsgSegmentClassification<Payload> result(initialCombined, leftEvents.size() + rightEvents.size());

    BOOLEAN previousCombined = initialCombined;
    long int i = 0;
    long int j = 0;
    while (i < leftEvents.size() || j < rightEvents.size()) {
        BOOLEAN takeLeft;
        if (j >= rightEvents.size()) {
            takeLeft = TRUE;
        } else if (i >= leftEvents.size()) {
            takeLeft = FALSE;
        } else {
            takeLeft = leftEvents[i].getT() <= rightEvents[j].getT();
        }
        const CsgSegmentEvent<Payload>& event = takeLeft ? leftEvents[i] : rightEvents[j];
        if (takeLeft) {
            insideLeft = event.isEntering();
            i++;
        } else {
            insideRight = event.isEntering();
            j++;
        }
        const BOOLEAN newCombined = CsgMembershipRule::combine(operation, insideLeft, insideRight);
        if (newCombined != previousCombined) {
            result.events.add(CsgSegmentEvent<Payload>(event.getT(), newCombined, event.getPayload()));
            previousCombined = newCombined;
        }
    }
    result.currentInside = previousCombined;
    return result;
}

#endif
