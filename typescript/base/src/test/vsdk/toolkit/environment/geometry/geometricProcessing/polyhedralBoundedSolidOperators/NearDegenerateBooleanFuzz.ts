import { StringBuilder } from "java/lang/StringBuilder.js";
import { VSDK } from "vsdk/toolkit/common/VSDK.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidNumericPolicy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { PolyhedralBoundedSolidOperationException } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidOperationException.js";
import { BooleanOracleFuzz } from "./BooleanOracleFuzz.js";
import { javaMapToString } from "./BooleanCompositeOracleFuzz.js";
import { OrientedBox, PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";

/** Java enum `NearDegenerateBooleanFuzz.Contact`. */
export enum Contact {
    /** B on top of A, footprints partly overlapping. */
    STACKED_PARTIAL = "STACKED_PARTIAL",
    /** B on top of A, same footprint. */
    STACKED_SAME_FOOTPRINT = "STACKED_SAME_FOOTPRINT",
    /** B beside A with coplanar top and bottom, touching side faces. */
    SIDE_BY_SIDE = "SIDE_BY_SIDE",
    /** B crossing A with one face coplanar with a face of A. */
    FLUSH_FACE_CROSSING = "FLUSH_FACE_CROSSING",
    /** B inside A touching one face of A from inside. */
    INNER_TANGENT = "INNER_TANGENT",
    /** B touching A along one edge only. */
    EDGE_CONTACT = "EDGE_CONTACT",
}

/** Java `NearDegenerateBooleanFuzz.Summary`. */
export class Summary {
    public total = 0;
    public answered = 0;
    public rejected = 0;
    public wrong = 0;
    /** Java `TreeMap`: kept sorted by key on every insertion. */
    public byOutcome = new Map<string, number>();
    public readonly details: string[] = [];

    public merge(key: string): void {
        this.byOutcome.set(key, (this.byOutcome.get(key) ?? 0) + 1);
        this.byOutcome = new Map(Array.from(this.byOutcome.entries()).sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0)));
    }
}

/** Java `Double.toString` for the step values printed in labels. */
function javaDouble(value: number): string {
    return Number.isInteger(value) ? value.toFixed(1) : String(value);
}

/**
Box pairs in degenerate contact, displaced by a multiple of the kernel
tolerance (plan-stage11 Phase B). Each configuration starts from an exact
contact and moves B along the contact normal by d = k x bigEpsilon, with k
in {-100, -10, -1, -0.1, 0, 0.1, 1, 10, 100}: from a clear overlap, through
the band where the kernel must decide whether two faces coincide, to a
clear gap. The whole pair is also tested rotated, so that the decision does
not rest on axis-aligned coordinates.

<p>Outcomes of one operation:</p>
<ul>
<li>OK: a strict-valid solid whose volume is the exact one of the displaced
    boxes, within what a displacement of one tolerance can change;</li>
<li>REJECTED: a typed exception (the operation refused to answer);</li>
<li>WRONG: a returned solid with another volume, or not strict-valid. This
    is the outcome that must never happen.</li>
</ul>

<p>Java nests `Contact` and `Summary`; here they are sibling exports.</p>
 */
export class NearDegenerateBooleanFuzz {
    public static readonly STEPS = [-100, -10, -1, -0.1, 0, 0.1, 1, 10, 100];

    private constructor() {}

    /** A and B for a contact, B moved by d along the contact normal. */
    public static pair(contact: Contact, d: number, rotated: boolean): OrientedBox[] {
        const box = NearDegenerateBooleanFuzz.box;
        let a: OrientedBox;
        let b: OrientedBox;

        switch (contact) {
            case Contact.STACKED_PARTIAL:
                a = box(0, 0, 0, 1, 1, 1);
                b = box(0.4, 0.3, 1 + d, 1.4, 1.3, 1.6 + d);
                break;
            case Contact.STACKED_SAME_FOOTPRINT:
                a = box(0, 0, 0, 1, 1, 1);
                b = box(0, 0, 1 + d, 1, 1, 1.7 + d);
                break;
            case Contact.SIDE_BY_SIDE:
                a = box(0, 0, 0, 1, 1, 1);
                b = box(1 + d, 0.2, 0, 1.8 + d, 0.9, 1);
                break;
            case Contact.FLUSH_FACE_CROSSING:
                a = box(0, 0, 0, 1, 1, 1);
                b = box(0.3, -0.4, 0.5 + d, 0.8, 1.5, 1 + d);
                break;
            case Contact.INNER_TANGENT:
                a = box(0, 0, 0, 1, 1, 1);
                b = box(0.2, 0.3, 0.6 + d, 0.7, 0.8, 1 + d);
                break;
            default:
                a = box(0, 0, 0, 1, 1, 1);
                b = box(1 + d, 0.1, 1 + d, 1.9 + d, 0.9, 1.8 + d);
                break;
        }
        if (rotated) {
            const motion = PolyhedralBoundedSolidVolumeOracle.translation(0.3, -0.2, 0.5).multiply(
                PolyhedralBoundedSolidVolumeOracle.rotation(0.83, 1, 2, -1.5),
            );
            a = new OrientedBox(a.size[0]!, a.size[1]!, a.size[2]!, motion.multiply(a.transform));
            b = new OrientedBox(b.size[0]!, b.size[1]!, b.size[2]!, motion.multiply(b.transform));
        }
        return [a, b];
    }

    private static box(x0: number, y0: number, z0: number, x1: number, y1: number, z1: number): OrientedBox {
        return new OrientedBox(
            x1 - x0,
            y1 - y0,
            z1 - z0,
            PolyhedralBoundedSolidVolumeOracle.translation((x0 + x1) / 2.0, (y0 + y1) / 2.0, (z0 + z1) / 2.0),
        );
    }

    public static run(contact: Contact, rotated: boolean): Summary {
        const summary = new Summary();
        const bigEpsilon = PolyhedralBoundedSolidNumericPolicy.forSolid(
            NearDegenerateBooleanFuzz.pair(contact, 0, rotated)[0]!.toSolid(),
        ).bigEpsilon();

        for (const step of NearDegenerateBooleanFuzz.STEPS) {
            for (const op of BooleanOracleFuzz.OPERATIONS) {
                const outcome = NearDegenerateBooleanFuzz.runOne(contact, step * bigEpsilon, rotated, op, bigEpsilon);
                const label =
                    contact +
                    (rotated ? " rotated" : "") +
                    " k=" +
                    javaDouble(step) +
                    " " +
                    BooleanOracleFuzz.opName(op);
                summary.total++;
                summary.merge(outcome.split(" ")[0]!);
                if (outcome.startsWith("OK")) {
                    summary.answered++;
                } else if (outcome.startsWith("REJECTED")) {
                    summary.rejected++;
                    summary.details.push(label + " " + outcome);
                } else {
                    summary.wrong++;
                    summary.details.push(label + " " + outcome);
                }
            }
        }
        return summary;
    }

    private static runOne(contact: Contact, d: number, rotated: boolean, op: number, bigEpsilon: number): string {
        const pair = NearDegenerateBooleanFuzz.pair(contact, d, rotated);
        const first = op === BooleanOracleFuzz.B_MINUS_A ? pair[1]! : pair[0]!;
        const second = op === BooleanOracleFuzz.B_MINUS_A ? pair[0]! : pair[1]!;
        const kernelOp = op === BooleanOracleFuzz.B_MINUS_A ? PolyhedralBoundedSolidModeler.SUBTRACT : op;
        // The expected volume comes from the pair before the rotation: a
        // rigid motion keeps volumes, and the half-space oracle is exact
        // for axis-aligned boxes even when they only touch.
        const aligned = NearDegenerateBooleanFuzz.pair(contact, d, false);
        const expected = PolyhedralBoundedSolidVolumeOracle.expectedVolume(
            op === BooleanOracleFuzz.B_MINUS_A ? aligned[1]! : aligned[0]!,
            op === BooleanOracleFuzz.B_MINUS_A ? aligned[0]! : aligned[1]!,
            kernelOp,
        );
        // Faces closer than the tolerance are one face for the kernel: the
        // volume may differ by the contact area (under 2 here) times the
        // displacement, when the displacement is inside the band.
        const tolerance = 1.0e-9 + 2.0 * Math.min(Math.abs(d), 2.0 * bigEpsilon);
        let result: PolyhedralBoundedSolid;

        try {
            result = PolyhedralBoundedSolidModeler.setOp(first.toSolid(), second.toSolid(), kernelOp);
        } catch (e) {
            if (e instanceof PolyhedralBoundedSolidOperationException) {
                const message = String(e.message).split("\n")[0]!;
                return (
                    "REJECTED " +
                    e.constructor.name +
                    ": " +
                    (message.length > 150 ? message.substring(0, 150) : message)
                );
            }
            throw e;
        }
        const volume = PolyhedralBoundedSolidVolumeOracle.solidVolume(result);
        if (Math.abs(volume - expected) > tolerance) {
            return "WRONG volume=" + volume + " expected=" + expected;
        }
        const message = new StringBuilder();
        if (
            result.getPolygonsList().size() > 0 &&
            !PolyhedralBoundedSolidValidationEngine.validateStrict(result, message)
        ) {
            return "WRONG not strict-valid: " + message.toString().split("\n")[0];
        }
        return "OK";
    }

    /** Java `main(String[])`: prints every outcome that is not OK. */
    public static main(): void {
        VSDK.setWithSystemExit(false);
        for (const contact of Object.values(Contact)) {
            for (const rotated of [false, true]) {
                const s = NearDegenerateBooleanFuzz.run(contact, rotated);
                console.log(
                    contact +
                        (rotated ? " rotated" : "") +
                        ": ok " +
                        s.answered +
                        " rejected " +
                        s.rejected +
                        " wrong " +
                        s.wrong +
                        " of " +
                        s.total +
                        " " +
                        javaMapToString(s.byOutcome),
                );
                s.details.forEach((x) => console.log("  " + x));
            }
        }
    }
}
