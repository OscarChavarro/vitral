package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;

/**
The fast [ROTH1982] section 3.3 ray classification over the caller's event
type: the state at the start of the ray and the sorted events where it
alternates. A leaf classification is built from one containment sample and
the leaf's sorted crossings (`append` toggles the state); `merge` combines two
classifications by `CsgMembershipRule.combine` (Table 3), keeping only the
events where the combined state changes. Ties take the left event first.
No tolerance, certificate or provenance is involved: renderers use it when
their own crossings and containment are trusted; certified ray
classification is `CsgRothRayClassifier`.
*/
public class CsgSegmentClassification<Payload> {
    private final ArrayList<CsgSegmentEvent<Payload>> events;
    private final boolean initialInside;
    private boolean currentInside;

    // An empty classification starting inside or outside, with room for
    // expectedEvents events.
    public CsgSegmentClassification(boolean initialInside, long expectedEvents)
    {
        events = new ArrayList<>((int)expectedEvents);
        this.initialInside = initialInside;
        currentInside = initialInside;
    }

    // Appends the next sorted event of a leaf, toggling the state.
    public void append(double t, Payload payload)
    {
        currentInside = !currentInside;
        events.add(new CsgSegmentEvent<Payload>(t, currentInside, payload));
    }

    public ArrayList<CsgSegmentEvent<Payload>> getEvents()
    {
        return events;
    }

    public boolean isInitialInside()
    {
        return initialInside;
    }

    public static <Payload> CsgSegmentClassification<Payload> merge(
        CsgSegmentClassification<Payload> left,
        CsgSegmentClassification<Payload> right,
        CsgOperation operation)
    {
        final ArrayList<CsgSegmentEvent<Payload>> leftEvents = left.events;
        final ArrayList<CsgSegmentEvent<Payload>> rightEvents = right.events;
        boolean insideLeft = left.initialInside;
        boolean insideRight = right.initialInside;
        final boolean initialCombined = CsgMembershipRule.combine(operation, insideLeft, insideRight);
        CsgSegmentClassification<Payload> result = new CsgSegmentClassification<Payload>(initialCombined, leftEvents.size() + rightEvents.size());

        boolean previousCombined = initialCombined;
        int i = 0;
        int j = 0;
        while (i < leftEvents.size() || j < rightEvents.size()) {
            boolean takeLeft;
            if (j >= rightEvents.size()) {
                takeLeft = true;
            } else if (i >= leftEvents.size()) {
                takeLeft = false;
            } else {
                takeLeft = leftEvents.get(i).getT() <= rightEvents.get(j).getT();
            }
            final CsgSegmentEvent<Payload> event = takeLeft ? leftEvents.get(i) : rightEvents.get(j);
            if (takeLeft) {
                insideLeft = event.isEntering();
                i++;
            } else {
                insideRight = event.isEntering();
                j++;
            }
            final boolean newCombined = CsgMembershipRule.combine(operation, insideLeft, insideRight);
            if (newCombined != previousCombined) {
                result.events.add(new CsgSegmentEvent<Payload>(event.getT(), newCombined, event.getPayload()));
                previousCombined = newCombined;
            }
        }
        result.currentInside = previousCombined;
        return result;
    }
}
