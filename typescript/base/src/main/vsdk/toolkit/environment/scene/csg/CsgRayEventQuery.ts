import { Double } from "../../../../../java/lang/Double.js";
import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { RaySolidQueryStatus } from "../../geometry/element/RaySolidQueryStatus.js";
import { CsgRayEventBatch } from "./CsgRayEventBatch.js";
import { CsgRayEventCursor } from "./CsgRayEventCursor.js";
import { CsgRayEventGroup } from "./CsgRayEventGroup.js";
import { CsgRayEventReference } from "./CsgRayEventReference.js";
import type { CsgRayLeafBatch } from "./CsgRayLeafBatch.js";

// Stable exact-t grouping; nearby values are not asserted coincident. References
// are valid only with the unchanged leaf batch used for this query.
export class CsgRayEventQuery {
    // Streaming form over caller storage. validate checks every leaf and
    // resets one cursor per leaf; nextGroup then fills group with the next
    // exactly equal parameter's references in leaf order, or returns false
    // when every boundary has been consumed.
    public static validate(leaves: CsgRayLeafBatch, cursors: ArrayList<CsgRayEventCursor>): RaySolidQueryStatus {
        cursors.clear();
        for (let i = 0; i < leaves.size(); ++i) {
            const c = leaves.get(i)!.getClassification();
            if (c.insideSegments.size() !== c.boundaries.size() + 1) return RaySolidQueryStatus.Indeterminate;
            for (let j = 0; j < c.boundaries.size(); ++j) {
                if (
                    c.boundaries.get(j).geometry === null ||
                    !Double.isFinite(c.boundaries.get(j).t) ||
                    (j > 0 && c.boundaries.get(j).t < c.boundaries.get(j - 1).t) ||
                    c.insideSegments.get(j) === c.insideSegments.get(j + 1)
                )
                    return RaySolidQueryStatus.Indeterminate;
            }
            if (!cursors.add(new CsgRayEventCursor())) return RaySolidQueryStatus.Indeterminate;
        }
        return RaySolidQueryStatus.Supported;
    }

    public static nextGroup(
        leaves: CsgRayLeafBatch,
        cursors: ArrayList<CsgRayEventCursor>,
        t: number[],
        group: ArrayList<CsgRayEventReference>,
    ): boolean {
        group.clear();
        let found = false;
        for (let i = 0; i < leaves.size() && i < cursors.size(); ++i) {
            const c = leaves.get(i)!.getClassification();
            if (
                cursors.get(i).index < c.boundaries.size() &&
                (!found || c.boundaries.get(cursors.get(i).index).t < t[0]!)
            ) {
                t[0] = c.boundaries.get(cursors.get(i).index).t;
                found = true;
            }
        }
        if (!found) return false;
        for (let i = 0; i < leaves.size() && i < cursors.size(); ++i) {
            const c = leaves.get(i)!.getClassification();
            while (cursors.get(i).index < c.boundaries.size() && c.boundaries.get(cursors.get(i).index).t === t[0]) {
                if (!group.add(new CsgRayEventReference(i, cursors.get(i).index))) {
                    group.clear();
                    return false;
                }
                ++cursors.get(i).index;
            }
        }
        return true;
    }

    public static group(leaves: CsgRayLeafBatch, result: CsgRayEventBatch): RaySolidQueryStatus {
        result.clear();
        const cursors = new ArrayList<CsgRayEventCursor>(leaves.size() > 0 ? leaves.size() : 1);
        const status = CsgRayEventQuery.validate(leaves, cursors);
        if (status !== RaySolidQueryStatus.Supported) return status;
        const candidate = new CsgRayEventBatch();
        const references = new ArrayList<CsgRayEventReference>(8);
        const t: number[] = [0];
        while (CsgRayEventQuery.nextGroup(leaves, cursors, t, references)) {
            const group = new CsgRayEventGroup(t[0]!);
            for (let k = 0; k < references.size(); ++k)
                if (!group.add(references.get(k))) return RaySolidQueryStatus.Indeterminate;
            if (!candidate.add(group)) return RaySolidQueryStatus.Indeterminate;
        }
        result.assign(candidate);
        return RaySolidQueryStatus.Supported;
    }
}
