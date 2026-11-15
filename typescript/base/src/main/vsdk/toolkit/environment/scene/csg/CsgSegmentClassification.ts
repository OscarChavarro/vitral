import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { CsgMembershipRule } from "./CsgMembershipRule.js";
import type { CsgOperation } from "./CsgOperation.js";
import { CsgSegmentEvent } from "./CsgSegmentEvent.js";

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
export class CsgSegmentClassification<Payload> {
    private readonly events: ArrayList<CsgSegmentEvent<Payload>>;
    private readonly initialInside: boolean;
    private currentInside: boolean;

    // An empty classification starting inside or outside, with room for
    // expectedEvents events.
    public constructor(initialInside: boolean, expectedEvents: number) {
        this.events = new ArrayList<CsgSegmentEvent<Payload>>(expectedEvents);
        this.initialInside = initialInside;
        this.currentInside = initialInside;
    }

    // Appends the next sorted event of a leaf, toggling the state.
    public append(t: number, payload: Payload): void {
        this.currentInside = !this.currentInside;
        this.events.add(new CsgSegmentEvent<Payload>(t, this.currentInside, payload));
    }

    public getEvents(): ArrayList<CsgSegmentEvent<Payload>> {
        return this.events;
    }

    public isInitialInside(): boolean {
        return this.initialInside;
    }

    public static merge<Payload>(
        left: CsgSegmentClassification<Payload>,
        right: CsgSegmentClassification<Payload>,
        operation: CsgOperation,
    ): CsgSegmentClassification<Payload> {
        const leftEvents = left.events;
        const rightEvents = right.events;
        let insideLeft = left.initialInside;
        let insideRight = right.initialInside;
        const initialCombined = CsgMembershipRule.combine(operation, insideLeft, insideRight);
        const result = new CsgSegmentClassification<Payload>(initialCombined, leftEvents.size() + rightEvents.size());
        let previousCombined = initialCombined;
        let i = 0;
        let j = 0;
        while (i < leftEvents.size() || j < rightEvents.size()) {
            let takeLeft: boolean;
            if (j >= rightEvents.size()) {
                takeLeft = true;
            } else if (i >= leftEvents.size()) {
                takeLeft = false;
            } else {
                takeLeft = leftEvents.get(i).getT() <= rightEvents.get(j).getT();
            }
            const event = takeLeft ? leftEvents.get(i) : rightEvents.get(j);
            if (takeLeft) {
                insideLeft = event.isEntering();
                i++;
            } else {
                insideRight = event.isEntering();
                j++;
            }
            const newCombined = CsgMembershipRule.combine(operation, insideLeft, insideRight);
            if (newCombined !== previousCombined) {
                result.events.add(new CsgSegmentEvent<Payload>(event.getT(), newCombined, event.getPayload()));
                previousCombined = newCombined;
            }
        }
        result.currentInside = previousCombined;
        return result;
    }
}
