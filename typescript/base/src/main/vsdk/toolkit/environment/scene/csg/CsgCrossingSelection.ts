import { CsgMembershipRule } from "./CsgMembershipRule.js";
import type { CsgOperation } from "./CsgOperation.js";

/**
The fast crossing-membership step of the POV heuristic (§5.1) over the
caller's containment query: a surface event of operand `eventOperand` is
selected when every sibling's containment of the event point agrees with
`CsgMembershipRule.requiresContainment`; union events are always selected.
`inside` is any callable taking a sibling index and returning whether that
sibling contains the event point (the C++ `SiblingContainment` template
parameter). Siblings are queried from the last to the first, after
`firstSibling` when it is a valid other operand, and the query stops at the
first disagreement. No tolerance or certificate is involved; the certified
selection is `CsgCrossingMembershipFilter`.
*/
export class CsgCrossingSelection {
    public static isSelected(
        operation: CsgOperation,
        eventOperand: number,
        operandCount: number,
        firstSibling: number,
        inside: (sibling: number) => boolean,
    ): boolean {
        if (!CsgMembershipRule.filtersEvents(operation)) {
            return true;
        }
        if (
            firstSibling >= 0 &&
            firstSibling < operandCount &&
            firstSibling !== eventOperand &&
            inside(firstSibling) !== CsgMembershipRule.requiresContainment(operation, eventOperand, firstSibling)
        ) {
            return false;
        }
        for (let j = operandCount - 1; j >= 0; j--) {
            if (j === eventOperand || j === firstSibling) {
                continue;
            }
            if (inside(j) !== CsgMembershipRule.requiresContainment(operation, eventOperand, j)) {
                return false;
            }
        }
        return true;
    }
}
