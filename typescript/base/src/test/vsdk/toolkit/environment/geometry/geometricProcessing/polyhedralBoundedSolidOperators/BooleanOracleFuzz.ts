import { StringBuilder } from "java/lang/StringBuilder.js";
import { Random } from "java/util/Random.js";
import type { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { VSDK } from "vsdk/toolkit/common/VSDK.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { NonManifoldBooleanResultException } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { OrientedBox, PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";

const rotation = PolyhedralBoundedSolidVolumeOracle.rotation;
const translation = PolyhedralBoundedSolidVolumeOracle.translation;

/** Java enum `BooleanOracleFuzz.Family`. */
export class Family {
    public static readonly AXIS_ALIGNED_GENERAL_POSITION = new Family("AXIS_ALIGNED_GENERAL_POSITION", 0, 12);
    public static readonly B_ROTATED_ABOUT_Z = new Family("B_ROTATED_ABOUT_Z", 1, 12);
    public static readonly B_ROTATED_ABOUT_RANDOM_AXIS = new Family("B_ROTATED_ABOUT_RANDOM_AXIS", 2, 12);
    public static readonly RANDOM_AXIS_TRANSLATED_TO_1000 = new Family("RANDOM_AXIS_TRANSLATED_TO_1000", 3, 12);
    public static readonly INTEGER_GRID = new Family("INTEGER_GRID", 4, 80);
    public static readonly COPLANAR_TOP_BOTTOM_ROTATED_45 = new Family("COPLANAR_TOP_BOTTOM_ROTATED_45", 5, 12);

    private constructor(
        private readonly nameValue: string,
        private readonly ordinalValue: number,
        public readonly pairs: number,
    ) {}

    public name(): string {
        return this.nameValue;
    }

    public ordinal(): number {
        return this.ordinalValue;
    }

    public toString(): string {
        return this.nameValue;
    }

    public static values(): Family[] {
        return [
            Family.AXIS_ALIGNED_GENERAL_POSITION,
            Family.B_ROTATED_ABOUT_Z,
            Family.B_ROTATED_ABOUT_RANDOM_AXIS,
            Family.RANDOM_AXIS_TRANSLATED_TO_1000,
            Family.INTEGER_GRID,
            Family.COPLANAR_TOP_BOTTOM_ROTATED_45,
        ];
    }
}

/** Java enum `BooleanOracleFuzz.Outcome`. */
export enum Outcome {
    OK = "OK",
    /** Union of solids touching only along edges or at vertices,
    rejected by design as non-manifold (B-04): a correct answer. */
    REJECTED_NON_MANIFOLD = "REJECTED_NON_MANIFOLD",
    WRONG_VOLUME = "WRONG_VOLUME",
    STRICT_REJECTED = "STRICT_REJECTED",
    EXCEPTION = "EXCEPTION",
}

const OUTCOME_VALUES = [
    Outcome.OK,
    Outcome.REJECTED_NON_MANIFOLD,
    Outcome.WRONG_VOLUME,
    Outcome.STRICT_REJECTED,
    Outcome.EXCEPTION,
];

/** Java `BooleanOracleFuzz.CaseResult`. */
export class CaseResult {
    public constructor(
        public readonly family: Family,
        public readonly pairIndex: number,
        public readonly operation: number,
        public readonly outcome: Outcome,
        public readonly path: string | null,
        public readonly detail: string,
    ) {}

    public toString(): string {
        return (
            this.family +
            "#" +
            this.pairIndex +
            " " +
            BooleanOracleFuzz.opName(this.operation) +
            " " +
            this.outcome +
            " path=" +
            this.path +
            " " +
            this.detail
        );
    }
}

/** Java `BooleanOracleFuzz.FamilySummary`. */
export class FamilySummary {
    public total = 0;
    public correct = 0;
    /** Included in `correct`. */
    public okByRejection = 0;
    /** Java `LinkedHashMap`: insertion order is kept by `Map`. */
    public readonly byOutcome = new Map<Outcome, number>();
    public readonly failures: CaseResult[] = [];
}

/**
Seeded box-pair fuzz over the six families of Fable5.2Analysis.md Section 1,
judged by `PolyhedralBoundedSolidVolumeOracle`: a result counts as
correct only when setOp returns without exception, the result passes
validateStrict and its divergence-theorem volume matches the exact
inclusion-exclusion volume. Each pair runs A u B, A n B, A \ B and B \ A.

<p>The same generator is used by the slow gate test and can be run as a
program to print the per-family table (`BooleanOracleFuzz.main`).</p>

<p>Java nests `Family`, `Outcome`, `CaseResult` and `FamilySummary`; here
they are sibling exports.</p>
 */
export class BooleanOracleFuzz {
    public static readonly B_MINUS_A = 4;
    public static readonly OPERATIONS = [
        PolyhedralBoundedSolidModeler.UNION,
        PolyhedralBoundedSolidModeler.INTERSECTION,
        PolyhedralBoundedSolidModeler.SUBTRACT,
        BooleanOracleFuzz.B_MINUS_A,
    ];

    private constructor() {}

    public static opName(op: number): string {
        if (op === PolyhedralBoundedSolidModeler.UNION) {
            return "UNION";
        }
        if (op === PolyhedralBoundedSolidModeler.INTERSECTION) {
            return "INTERSECTION";
        }
        if (op === PolyhedralBoundedSolidModeler.SUBTRACT) {
            return "A-B";
        }
        return "B-A";
    }

    /** Fixed-seed pairs of one family; the seed depends only on the family. */
    public static pairs(family: Family): OrientedBox[][] {
        // The translated family replays the random-axis family's seed, so
        // its cases are exactly those pairs moved to (1000, 1000, 1000).
        const seedFamily =
            family === Family.RANDOM_AXIS_TRANSLATED_TO_1000 ? Family.B_ROTATED_ABOUT_RANDOM_AXIS : family;
        const random = new Random(20261114 + seedFamily.ordinal() * 7919);
        const result: OrientedBox[][] = [];
        let i: number;

        for (i = 0; i < family.pairs; i++) {
            result.push(BooleanOracleFuzz.pair(family, random));
        }
        return result;
    }

    private static uniform(random: Random, lo: number, hi: number): number {
        return lo + (hi - lo) * random.nextDouble();
    }

    private static randomAxisRotation(random: Random): Matrix4x4d {
        let x: number;
        let y: number;
        let z: number;
        let length: number;

        do {
            x = BooleanOracleFuzz.uniform(random, -1, 1);
            y = BooleanOracleFuzz.uniform(random, -1, 1);
            z = BooleanOracleFuzz.uniform(random, -1, 1);
            length = Math.sqrt(x * x + y * y + z * z);
        } while (length < 0.2 || length > 1.0);
        return rotation(BooleanOracleFuzz.uniform(random, 0.2, 2.9), x / length, y / length, z / length);
    }

    private static pair(family: Family, random: Random): OrientedBox[] {
        const uniform = BooleanOracleFuzz.uniform;
        let a: OrientedBox;
        let b: OrientedBox;

        switch (family) {
            case Family.AXIS_ALIGNED_GENERAL_POSITION:
                a = new OrientedBox(
                    uniform(random, 0.8, 2.0),
                    uniform(random, 0.8, 2.0),
                    uniform(random, 0.8, 2.0),
                    translation(0, 0, 0),
                );
                b = new OrientedBox(
                    uniform(random, 0.5, 1.8),
                    uniform(random, 0.5, 1.8),
                    uniform(random, 0.5, 1.8),
                    translation(uniform(random, -0.7, 0.7), uniform(random, -0.7, 0.7), uniform(random, -0.7, 0.7)),
                );
                return [a, b];
            case Family.B_ROTATED_ABOUT_Z:
                a = new OrientedBox(
                    uniform(random, 0.8, 2.0),
                    uniform(random, 0.8, 2.0),
                    uniform(random, 0.8, 2.0),
                    translation(0, 0, 0),
                );
                b = new OrientedBox(
                    uniform(random, 0.5, 1.8),
                    uniform(random, 0.5, 1.8),
                    uniform(random, 0.5, 1.8),
                    translation(
                        uniform(random, -0.7, 0.7),
                        uniform(random, -0.7, 0.7),
                        uniform(random, -0.7, 0.7),
                    ).multiply(rotation(uniform(random, 0.2, 1.4), 0, 0, 1)),
                );
                return [a, b];
            case Family.B_ROTATED_ABOUT_RANDOM_AXIS:
            case Family.RANDOM_AXIS_TRANSLATED_TO_1000: {
                const offset = family === Family.RANDOM_AXIS_TRANSLATED_TO_1000 ? 1000 : 0;
                a = new OrientedBox(
                    uniform(random, 0.8, 2.0),
                    uniform(random, 0.8, 2.0),
                    uniform(random, 0.8, 2.0),
                    translation(offset, offset, offset),
                );
                // Java evaluates the receiver (translation) before the
                // argument (randomAxisRotation): keep that order of draws.
                const sx = uniform(random, 0.5, 1.8);
                const sy = uniform(random, 0.5, 1.8);
                const sz = uniform(random, 0.5, 1.8);
                const place = translation(
                    offset + uniform(random, -0.7, 0.7),
                    offset + uniform(random, -0.7, 0.7),
                    offset + uniform(random, -0.7, 0.7),
                );
                b = new OrientedBox(sx, sy, sz, place.multiply(BooleanOracleFuzz.randomAxisRotation(random)));
                return [a, b];
            }
            case Family.INTEGER_GRID:
                a = BooleanOracleFuzz.gridBox(random);
                b = BooleanOracleFuzz.gridBox(random);
                return [a, b];
            case Family.COPLANAR_TOP_BOTTOM_ROTATED_45:
            default: {
                a = new OrientedBox(2, 2, 1, translation(0, 0, 0));
                const side = uniform(random, 0.4, 1.2);
                const place = translation(uniform(random, -1.0, 1.0), uniform(random, -1.0, 1.0), 0).multiply(
                    rotation(Math.PI / 4.0, 0, 0, 1),
                );
                b = new OrientedBox(side, side, 1, place);
                return [a, b];
            }
        }
    }

    /** Box with integer corners in [0, 3]^3: coplanar faces, shared edges. */
    private static gridBox(random: Random): OrientedBox {
        const lo = [0, 0, 0];
        const size = [0, 0, 0];
        let k: number;

        for (k = 0; k < 3; k++) {
            size[k] = 1 + random.nextInt(2);
            lo[k] = random.nextInt(4 - size[k]!);
        }
        return new OrientedBox(
            size[0]!,
            size[1]!,
            size[2]!,
            translation(lo[0]! + size[0]! / 2.0, lo[1]! + size[1]! / 2.0, lo[2]! + size[2]! / 2.0),
        );
    }

    public static run(family: Family, pairIndex: number, pair: OrientedBox[], op: number): CaseResult {
        const first = op === BooleanOracleFuzz.B_MINUS_A ? pair[1]! : pair[0]!;
        const second = op === BooleanOracleFuzz.B_MINUS_A ? pair[0]! : pair[1]!;
        const kernelOp = op === BooleanOracleFuzz.B_MINUS_A ? PolyhedralBoundedSolidModeler.SUBTRACT : op;
        const expected = PolyhedralBoundedSolidVolumeOracle.expectedVolume(first, second, kernelOp);
        const tolerance = 1.0e-6 * Math.max(1.0, pair[0]!.volume() + pair[1]!.volume());
        let result: PolyhedralBoundedSolid;

        try {
            result = PolyhedralBoundedSolidModeler.setOp(
                first.toSolid(),
                second.toSolid(),
                kernelOp,
                false,
                true,
                false,
            );
        } catch (t) {
            if (t instanceof NonManifoldBooleanResultException) {
                const overlap = PolyhedralBoundedSolidVolumeOracle.expectedVolume(
                    first,
                    second,
                    PolyhedralBoundedSolidModeler.INTERSECTION,
                );
                const outcome =
                    kernelOp === PolyhedralBoundedSolidModeler.UNION && overlap <= tolerance
                        ? Outcome.REJECTED_NON_MANIFOLD
                        : Outcome.EXCEPTION;
                return new CaseResult(
                    family,
                    pairIndex,
                    op,
                    outcome,
                    PolyhedralBoundedSolidModeler.getLastSetOpResultPath(),
                    "NonManifoldBooleanResultException, overlap=" + overlap,
                );
            }
            const error = t as Error;
            return new CaseResult(
                family,
                pairIndex,
                op,
                Outcome.EXCEPTION,
                PolyhedralBoundedSolidModeler.getLastSetOpResultPath(),
                error.constructor.name + ": " + BooleanOracleFuzz.firstLine(error.message),
            );
        }
        const path = PolyhedralBoundedSolidModeler.getLastSetOpResultPath();
        const strict = new StringBuilder();
        let strictValid: boolean;
        try {
            strictValid = PolyhedralBoundedSolidValidationEngine.validateStrict(result, strict);
        } catch (t) {
            return new CaseResult(
                family,
                pairIndex,
                op,
                Outcome.EXCEPTION,
                path,
                "validateStrict: " + (t as Error).constructor.name,
            );
        }
        const volume = PolyhedralBoundedSolidVolumeOracle.solidVolume(result);
        const detail = "volume=" + volume.toFixed(6) + " expected=" + expected.toFixed(6);
        if (Math.abs(volume - expected) > tolerance) {
            return new CaseResult(
                family,
                pairIndex,
                op,
                Outcome.WRONG_VOLUME,
                path,
                detail + (strictValid ? " (silent)" : " (strict fail)"),
            );
        }
        if (!strictValid) {
            return new CaseResult(
                family,
                pairIndex,
                op,
                Outcome.STRICT_REJECTED,
                path,
                detail + " " + BooleanOracleFuzz.firstLine(strict.toString()),
            );
        }
        return new CaseResult(family, pairIndex, op, Outcome.OK, path, detail);
    }

    private static firstLine(s: string | null | undefined): string {
        if (s === null || s === undefined) {
            return "";
        }
        const i = s.indexOf("\n");
        return i < 0 ? s : s.substring(0, i);
    }

    public static runFamily(family: Family): FamilySummary {
        const summary = new FamilySummary();
        const pairs = BooleanOracleFuzz.pairs(family);
        let i: number;

        for (const outcome of OUTCOME_VALUES) {
            summary.byOutcome.set(outcome, 0);
        }
        for (i = 0; i < pairs.length; i++) {
            for (const op of BooleanOracleFuzz.OPERATIONS) {
                const r = BooleanOracleFuzz.run(family, i, pairs[i]!, op);
                summary.total++;
                summary.byOutcome.set(r.outcome, summary.byOutcome.get(r.outcome)! + 1);
                if (r.outcome === Outcome.REJECTED_NON_MANIFOLD) {
                    summary.correct++;
                    summary.okByRejection++;
                } else if (r.outcome === Outcome.OK) {
                    summary.correct++;
                } else {
                    summary.failures.push(r);
                }
            }
        }
        return summary;
    }

    /** Java `main(String[])`: prints the per-family table. */
    public static main(args: string[]): void {
        VSDK.setWithSystemExit(false);
        console.log(
            "| Family | Correct (strict-valid or rejected) | " +
                "rejected non-manifold | wrong volume | strict rejected | " +
                "exception |",
        );
        console.log("|---|---:|---:|---:|---:|---:|");
        const failures: CaseResult[] = [];
        for (const family of Family.values()) {
            const s = BooleanOracleFuzz.runFamily(family);
            console.log(
                "| " +
                    family +
                    " | " +
                    s.correct +
                    " / " +
                    s.total +
                    " | " +
                    s.okByRejection +
                    " | " +
                    s.byOutcome.get(Outcome.WRONG_VOLUME) +
                    " | " +
                    s.byOutcome.get(Outcome.STRICT_REJECTED) +
                    " | " +
                    s.byOutcome.get(Outcome.EXCEPTION) +
                    " |",
            );
            failures.push(...s.failures);
        }
        if (args.length > 0 && args[0] === "-v") {
            for (const r of failures) {
                console.log(r.toString());
            }
        }
    }
}
