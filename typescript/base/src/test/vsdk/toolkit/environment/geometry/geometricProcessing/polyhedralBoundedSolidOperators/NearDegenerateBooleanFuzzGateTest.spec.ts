import { describe, expect, it } from "vitest";

import { Contact, NearDegenerateBooleanFuzz } from "./NearDegenerateBooleanFuzz.js";

/**
Gate over `NearDegenerateBooleanFuzz` (plan-stage11 Phase B): box
pairs in degenerate contact, displaced by -100 to 100 tolerances, as built
and rotated; 36 operations per row.

<p>Two conditions per row:</p>
<ul>
<li>No wrong solid, ever. An operation either returns a strict-valid solid
    with the right volume or throws a typed exception. Before this gate,
    FLUSH_FACE_CROSSING rotated at 0.1 tolerances returned B - A with one
    of its two pieces missing: connect had left loose null edges on an open
    intersection curve and finish went on. That is now rejected.</li>
<li>Ratchet on the count of answered operations: raise it when a fix
    lands, never lower it. Every row is at its total except EDGE_CONTACT,
    whose unions near contact are rejected by decision.</li>
</ul>
 */
describe("NearDegenerateBooleanFuzzGateTest", () => {
    /**
    First measurement (2026-11-16): 363 of 432 answered, 69 rejected. With
    the operands aligned inside the tolerance band: 422 answered. The 10
    left are the unions of EDGE_CONTACT at 0, 0.1 and 1 tolerances, which
    are edge contacts and are rejected by decision (B-04).
    */
    const rows: [Contact, boolean, number][] = [
        [Contact.STACKED_PARTIAL, false, 36],
        [Contact.STACKED_PARTIAL, true, 36],
        [Contact.STACKED_SAME_FOOTPRINT, false, 36],
        [Contact.STACKED_SAME_FOOTPRINT, true, 36],
        [Contact.SIDE_BY_SIDE, false, 36],
        [Contact.SIDE_BY_SIDE, true, 36],
        [Contact.FLUSH_FACE_CROSSING, false, 36],
        [Contact.FLUSH_FACE_CROSSING, true, 36],
        [Contact.INNER_TANGENT, false, 36],
        [Contact.INNER_TANGENT, true, 36],
        [Contact.EDGE_CONTACT, false, 31],
        [Contact.EDGE_CONTACT, true, 31],
    ];

    it.each(rows)(
        "given_contactDisplacedByTolerances_when_fourOperations_then_neverAWrongSolid: %s rotated=%s",
        (contact: Contact, rotated: boolean, minimumAnswered: number) => {
            const summary = NearDegenerateBooleanFuzz.run(contact, rotated);
            console.log(
                contact +
                    (rotated ? " rotated" : "") +
                    ": ok " +
                    summary.answered +
                    " rejected " +
                    summary.rejected +
                    " wrong " +
                    summary.wrong +
                    " of " +
                    summary.total,
            );
            summary.details.forEach((d) => console.log("  " + d));
            expect(summary.wrong, "wrong solids returned without an error: [" + summary.details.join(", ") + "]").toBe(
                0,
            );
            expect(summary.answered, "answered operations").toBeGreaterThanOrEqual(minimumAnswered);
        },
    );
});
