import { StringBuilder } from "java/lang/StringBuilder.js";
import { Random } from "java/util/Random.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { VSDK } from "vsdk/toolkit/common/VSDK.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidStructuralCopy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { NonManifoldBooleanResultException } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { CompositeShapeOracle, Shape } from "./CompositeShapeOracle.js";
import { PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";

/** Java enum `BooleanCompositeOracleFuzz.Family`. */
export class Family {
    public static readonly NON_CONVEX_AXIS_ALIGNED = new Family("NON_CONVEX_AXIS_ALIGNED", 0, 12);
    public static readonly NON_CONVEX_ROTATED = new Family("NON_CONVEX_ROTATED", 1, 12);
    public static readonly NON_CONVEX_ROTATED_SCALE_1E_MINUS_3 = new Family(
        "NON_CONVEX_ROTATED_SCALE_1E_MINUS_3",
        2,
        12,
    );
    public static readonly NON_CONVEX_ROTATED_SCALE_1E_PLUS_3 = new Family("NON_CONVEX_ROTATED_SCALE_1E_PLUS_3", 3, 12);
    public static readonly CHAINS = new Family("CHAINS", 4, 12);
    // Appended last: the seed of a family is its ordinal.
    public static readonly NON_CONVEX_COPLANAR_BOTTOMS = new Family("NON_CONVEX_COPLANAR_BOTTOMS", 5, 12);

    private constructor(
        private readonly nameValue: string,
        private readonly ordinalValue: number,
        public readonly cases: number,
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
            Family.NON_CONVEX_AXIS_ALIGNED,
            Family.NON_CONVEX_ROTATED,
            Family.NON_CONVEX_ROTATED_SCALE_1E_MINUS_3,
            Family.NON_CONVEX_ROTATED_SCALE_1E_PLUS_3,
            Family.CHAINS,
            Family.NON_CONVEX_COPLANAR_BOTTOMS,
        ];
    }
}

/** Java enum `BooleanCompositeOracleFuzz.Outcome`. */
export enum Outcome {
    OK = "OK",
    REJECTED_NON_MANIFOLD = "REJECTED_NON_MANIFOLD",
    WRONG_VOLUME = "WRONG_VOLUME",
    STRICT_REJECTED = "STRICT_REJECTED",
    NOT_COMMUTATIVE = "NOT_COMMUTATIVE",
    EXCEPTION = "EXCEPTION",
}

const OUTCOME_VALUES = [
    Outcome.OK,
    Outcome.REJECTED_NON_MANIFOLD,
    Outcome.WRONG_VOLUME,
    Outcome.STRICT_REJECTED,
    Outcome.NOT_COMMUTATIVE,
    Outcome.EXCEPTION,
];

/** Java `BooleanCompositeOracleFuzz.CaseResult`. */
export class CaseResult {
    public family!: Family;
    public index = 0;
    public label = "";
    public outcome: Outcome = Outcome.OK;
    public detail: string | null = null;

    public toString(): string {
        return (
            this.family +
            " #" +
            this.index +
            " " +
            this.label +
            ": " +
            this.outcome +
            (this.detail === null ? "" : " " + this.detail)
        );
    }
}

/** Java `BooleanCompositeOracleFuzz.FamilySummary`. */
export class FamilySummary {
    public total = 0;
    public correct = 0;
    /** Java `LinkedHashMap`: insertion order is kept by `Map`. */
    public readonly byOutcome = new Map<Outcome, number>();
    public readonly failures: CaseResult[] = [];
    public readonly rejected: CaseResult[] = [];
}

/** Java `AbstractMap.toString`: `{k1=v1, k2=v2}`. */
export function javaMapToString<K, V>(map: Map<K, V>): string {
    return (
        "{" +
        Array.from(map.entries())
            .map(([k, v]) => String(k) + "=" + String(v))
            .join(", ") +
        "}"
    );
}

/**
Java `String.format("%.9g", value)`: nine significant digits, decimal
notation for 10^-4 <= |value| < 10^9 after rounding, scientific otherwise
(two-digit exponent at least).
 */
function formatG9(value: number): string {
    if (!Number.isFinite(value)) {
        return Number.isNaN(value) ? "NaN" : value > 0 ? "Infinity" : "-Infinity";
    }
    if (value === 0) {
        return (Object.is(value, -0) ? "-" : "") + "0.00000000";
    }
    const rounded = Number(value.toPrecision(9));
    const magnitude = Math.abs(rounded);
    if (magnitude >= 1.0e-4 && magnitude < 1.0e9) {
        const exponent = Math.floor(Math.log10(magnitude));
        return rounded.toFixed(Math.max(0, 8 - exponent));
    }
    const [mantissa, exp] = value.toExponential(8).split("e");
    const e = Number(exp);
    return mantissa + "e" + (e < 0 ? "-" : "+") + String(Math.abs(e)).padStart(2, "0");
}

type Expression = (operands: PolyhedralBoundedSolid[]) => PolyhedralBoundedSolid;

/**
Seeded fuzz of booleans on non-convex operands and on chains of booleans,
checked against `CompositeShapeOracle` (Fable5.2Analysis.md Section 8,
"further tests worth writing"):
<ul>
<li>non-convex operands (L, U, stepped block, frame with a through hole),
axis-aligned in general position and rigidly rotated;</li>
<li>chains, where the result of one boolean is an operand of the next:
`(A ∪ B) \ C`, `(A ∩ B) ∪ C`, `A \ (B ∪ C)`;</li>
<li>scale: the rotated non-convex family at 10⁻³ and at 10³;</li>
<li>commutativity of ∪ and ∩: same volume and same topology summary.</li>
</ul>

<p>A case is correct when every result is strict-valid and has the oracle
volume. A non-manifold rejection is correct only in the axis-aligned family:
after a random rigid motion the operands are in general position, where a
touching contact has probability zero, so a rejection there means the
kernel built a broken result. The scaled families replay the cases of the
rotated family. `BooleanCompositeOracleFuzzGateTest` is the ratchet;
`main` prints every family (`-v` also lists the rejections).</p>

<p>Java nests `Family`, `Outcome`, `CaseResult`, `FamilySummary` and
`Expression`; here they are sibling exports (or a type alias).</p>
 */
export class BooleanCompositeOracleFuzz {
    private static readonly EXPRESSIONS = ["(A u B) - C", "(A n B) u C", "A - (B u C)"];

    private constructor() {}

    public static runFamily(family: Family): FamilySummary {
        const summary = new FamilySummary();
        const random = new Random(BooleanCompositeOracleFuzz.seedOf(family));
        let i: number;

        for (const o of OUTCOME_VALUES) {
            summary.byOutcome.set(o, 0);
        }
        for (i = 0; i < family.cases; i++) {
            const results =
                family === Family.CHAINS
                    ? BooleanCompositeOracleFuzz.runChains(family, i, random)
                    : BooleanCompositeOracleFuzz.runPair(family, i, random);
            for (const r of results) {
                summary.total++;
                summary.byOutcome.set(r.outcome, (summary.byOutcome.get(r.outcome) ?? 0) + 1);
                // Random rigid motions put the operands in general
                // position, where touching contacts have probability zero:
                // a non-manifold rejection there hides a broken result.
                // Only the axis-aligned family can really touch.
                const touchingPossible = family === Family.NON_CONVEX_AXIS_ALIGNED;
                if (r.outcome === Outcome.OK || (r.outcome === Outcome.REJECTED_NON_MANIFOLD && touchingPossible)) {
                    summary.correct++;
                    if (r.outcome === Outcome.REJECTED_NON_MANIFOLD) {
                        summary.rejected.push(r);
                    }
                } else {
                    summary.failures.push(r);
                }
            }
        }
        return summary;
    }

    //= Shapes ==============================================================

    public static randomShape(random: Random, scale: number): Shape {
        let w = 0.3 + 0.2 * random.nextDouble();
        const h = 0.3 + 0.3 * random.nextDouble();
        const kind = random.nextInt(5);
        const positive: number[][] = [];
        const negative: number[][] = [];
        let name: string;

        switch (kind) {
            case 0:
                name = "L";
                positive.push([0, 0, 0, 1, w, h]);
                positive.push([0, 0, 0, w, 1, h]);
                break;
            case 1:
                name = "U";
                w = w / 1.5;
                positive.push([0, 0, 0, w, 1, h]);
                positive.push([1 - w, 0, 0, 1, 1, h]);
                positive.push([0, 0, 0, 1, w, h]);
                break;
            case 2:
                name = "STEP";
                positive.push([0, 0, 0, 1, 1, h]);
                positive.push([0, 0, 0, w, 1, 2 * h]);
                break;
            case 3:
                name = "FRAME";
                w = w / 1.5;
                positive.push([0, 0, 0, 1, 1, h]);
                negative.push([w, w, -0.25, 1 - w, 1 - w, h + 0.25]);
                break;
            default:
                name = "BOX";
                positive.push([0, 0, 0, 1, 0.5 + w, h]);
                break;
        }
        BooleanCompositeOracleFuzz.scaleBoxes(positive, scale);
        BooleanCompositeOracleFuzz.scaleBoxes(negative, scale);
        return new Shape(
            name,
            positive,
            negative,
            PolyhedralBoundedSolidVolumeOracle.translation(-0.5 * scale, -0.5 * scale, -0.5 * h * scale),
        );
    }

    private static scaleBoxes(boxes: number[][], scale: number): void {
        for (const b of boxes) {
            let k: number;
            for (k = 0; k < 6; k++) {
                b[k] = b[k]! * scale;
            }
        }
    }

    public static randomRotation(random: Random): Matrix4x4d {
        return PolyhedralBoundedSolidVolumeOracle.rotation(
            0.2 + 2.5 * random.nextDouble(),
            random.nextGaussian(),
            random.nextGaussian(),
            random.nextGaussian(),
        );
    }

    public static randomOffset(random: Random, scale: number): Matrix4x4d {
        return PolyhedralBoundedSolidVolumeOracle.translation(
            scale * (0.8 * random.nextDouble() - 0.4),
            scale * (0.8 * random.nextDouble() - 0.4),
            scale * (0.6 * random.nextDouble() - 0.3),
        );
    }

    /**
    The scaled families replay the cases of NON_CONVEX_ROTATED: the same
    configurations at another size must give the same answers.
    */
    private static seedOf(family: Family): number {
        if (
            family === Family.NON_CONVEX_ROTATED_SCALE_1E_MINUS_3 ||
            family === Family.NON_CONVEX_ROTATED_SCALE_1E_PLUS_3
        ) {
            return 1000 + Family.NON_CONVEX_ROTATED.ordinal();
        }
        return 1000 + family.ordinal();
    }

    private static scaleOf(family: Family): number {
        if (family === Family.NON_CONVEX_ROTATED_SCALE_1E_MINUS_3) {
            return 1.0e-3;
        }
        if (family === Family.NON_CONVEX_ROTATED_SCALE_1E_PLUS_3) {
            return 1.0e3;
        }
        return 1.0;
    }

    //= Pairs ===============================================================

    /**
    The operands of case `index` of `family`, as the fuzz
    generates them (for reproducing one case in isolation).
    */
    public static shapesOf(family: Family, index: number): Shape[] | null {
        const random = new Random(BooleanCompositeOracleFuzz.seedOf(family));
        let shapes: Shape[] | null = null;
        let i: number;

        for (i = 0; i <= index; i++) {
            shapes =
                family === Family.CHAINS
                    ? BooleanCompositeOracleFuzz.chainShapes(random)
                    : BooleanCompositeOracleFuzz.pairShapes(family, random);
        }
        return shapes;
    }

    private static pairShapes(family: Family, random: Random): Shape[] {
        const scale = BooleanCompositeOracleFuzz.scaleOf(family);
        const rotated = family !== Family.NON_CONVEX_AXIS_ALIGNED;
        const a = BooleanCompositeOracleFuzz.randomShape(random, scale);
        const b = BooleanCompositeOracleFuzz.randomShape(random, scale);

        if (family === Family.NON_CONVEX_COPLANAR_BOTTOMS) {
            return BooleanCompositeOracleFuzz.coplanarPair(a, b, random);
        }
        const motionA = rotated ? BooleanCompositeOracleFuzz.randomRotation(random) : new Matrix4x4d();
        const motionB = BooleanCompositeOracleFuzz.randomOffset(random, scale).multiply(
            rotated ? BooleanCompositeOracleFuzz.randomRotation(random) : new Matrix4x4d(),
        );
        return [a.moved(motionA), b.moved(motionA.multiply(motionB))];
    }

    /**
    Both bottoms on one plane, B turned about the plane normal and moved
    along the plane, then a common random rotation: coplanar non-convex
    faces whose edges cross at general angles ([MANT1988] 15.1, the case
    the book leaves to a two-dimensional set operation).
    */
    private static coplanarPair(a: Shape, b: Shape, random: Random): Shape[] {
        const common = BooleanCompositeOracleFuzz.randomRotation(random);
        const align = a.frame.get(2, 3) - b.frame.get(2, 3);
        const inPlane = PolyhedralBoundedSolidVolumeOracle.translation(
            0.8 * random.nextDouble() - 0.4,
            0.8 * random.nextDouble() - 0.4,
            align,
        ).multiply(PolyhedralBoundedSolidVolumeOracle.rotation(0.2 + 2.5 * random.nextDouble(), 0, 0, 1));
        return [a.moved(common), b.moved(common.multiply(inPlane))];
    }

    private static chainShapes(random: Random): Shape[] {
        const common = BooleanCompositeOracleFuzz.randomRotation(random);
        const a = BooleanCompositeOracleFuzz.randomShape(random, 1.0).moved(common);
        const b = BooleanCompositeOracleFuzz.randomShape(random, 1.0).moved(
            common.multiply(
                BooleanCompositeOracleFuzz.randomOffset(random, 1.0).multiply(
                    BooleanCompositeOracleFuzz.randomRotation(random),
                ),
            ),
        );
        const c = BooleanCompositeOracleFuzz.randomShape(random, 1.0).moved(
            common.multiply(
                BooleanCompositeOracleFuzz.randomOffset(random, 1.0).multiply(
                    BooleanCompositeOracleFuzz.randomRotation(random),
                ),
            ),
        );
        return [a, b, c];
    }

    private static runPair(family: Family, index: number, random: Random): CaseResult[] {
        const setOp = BooleanCompositeOracleFuzz.setOp;
        const evaluate = BooleanCompositeOracleFuzz.evaluate;
        const scale = BooleanCompositeOracleFuzz.scaleOf(family);
        const shapes = BooleanCompositeOracleFuzz.pairShapes(family, random);
        const a = shapes[0]!;
        const b = shapes[1]!;
        const results: CaseResult[] = [];
        const pairLabel = a + "/" + b;
        const operandProblem = BooleanCompositeOracleFuzz.checkOperands(shapes);
        const tolerance = 1.0e-6 * scale * scale * scale;

        results.push(
            evaluate(
                family,
                index,
                pairLabel + " A u B",
                operandProblem,
                shapes,
                (m) => m[0]! || m[1]!,
                tolerance,
                (ops) => setOp(ops[0]!, ops[1]!, PolyhedralBoundedSolidModeler.UNION),
            ),
        );
        results.push(
            evaluate(
                family,
                index,
                pairLabel + " A n B",
                operandProblem,
                shapes,
                (m) => m[0]! && m[1]!,
                tolerance,
                (ops) => setOp(ops[0]!, ops[1]!, PolyhedralBoundedSolidModeler.INTERSECTION),
            ),
        );
        results.push(
            evaluate(
                family,
                index,
                pairLabel + " A - B",
                operandProblem,
                shapes,
                (m) => m[0]! && !m[1]!,
                tolerance,
                (ops) => setOp(ops[0]!, ops[1]!, PolyhedralBoundedSolidModeler.SUBTRACT),
            ),
        );
        results.push(
            evaluate(
                family,
                index,
                pairLabel + " B - A",
                operandProblem,
                shapes,
                (m) => m[1]! && !m[0]!,
                tolerance,
                (ops) => setOp(ops[1]!, ops[0]!, PolyhedralBoundedSolidModeler.SUBTRACT),
            ),
        );
        results.push(
            BooleanCompositeOracleFuzz.commutativity(
                family,
                index,
                pairLabel + " u comm",
                operandProblem,
                shapes,
                PolyhedralBoundedSolidModeler.UNION,
            ),
        );
        results.push(
            BooleanCompositeOracleFuzz.commutativity(
                family,
                index,
                pairLabel + " n comm",
                operandProblem,
                shapes,
                PolyhedralBoundedSolidModeler.INTERSECTION,
            ),
        );
        return results;
    }

    //= Chains ==============================================================

    private static runChains(family: Family, index: number, random: Random): CaseResult[] {
        const setOp = BooleanCompositeOracleFuzz.setOp;
        const evaluate = BooleanCompositeOracleFuzz.evaluate;
        const EXPRESSIONS = BooleanCompositeOracleFuzz.EXPRESSIONS;
        const shapes = BooleanCompositeOracleFuzz.chainShapes(random);
        const a = shapes[0]!;
        const b = shapes[1]!;
        const c = shapes[2]!;
        const label = a + "/" + b + "/" + c + " ";
        const operandProblem = BooleanCompositeOracleFuzz.checkOperands(shapes);
        const results: CaseResult[] = [];
        const u = PolyhedralBoundedSolidModeler.UNION;
        const n = PolyhedralBoundedSolidModeler.INTERSECTION;
        const d = PolyhedralBoundedSolidModeler.SUBTRACT;

        results.push(
            evaluate(
                family,
                index,
                label + EXPRESSIONS[0],
                operandProblem,
                shapes,
                (m) => (m[0]! || m[1]!) && !m[2]!,
                1.0e-6,
                (ops) => setOp(setOp(ops[0]!, ops[1]!, u), ops[2]!, d),
            ),
        );
        results.push(
            evaluate(
                family,
                index,
                label + EXPRESSIONS[1],
                operandProblem,
                shapes,
                (m) => (m[0]! && m[1]!) || m[2]!,
                1.0e-6,
                (ops) => setOp(setOp(ops[0]!, ops[1]!, n), ops[2]!, u),
            ),
        );
        results.push(
            evaluate(
                family,
                index,
                label + EXPRESSIONS[2],
                operandProblem,
                shapes,
                (m) => m[0]! && !(m[1]! || m[2]!),
                1.0e-6,
                (ops) => setOp(ops[0]!, setOp(ops[1]!, ops[2]!, u), d),
            ),
        );
        return results;
    }

    //= Evaluation ==========================================================

    /**
    Strict setOp. A chain whose first step yields the empty solid passes it
    on as an operand: the empty set is a valid operand of the next step.
    */
    public static setOp(a: PolyhedralBoundedSolid, b: PolyhedralBoundedSolid, op: number): PolyhedralBoundedSolid {
        if (PolyhedralBoundedSolidModeler.isEmptyResult(a)) {
            return op === PolyhedralBoundedSolidModeler.UNION
                ? BooleanCompositeOracleFuzz.copy(b)
                : new PolyhedralBoundedSolid();
        }
        if (PolyhedralBoundedSolidModeler.isEmptyResult(b)) {
            return op === PolyhedralBoundedSolidModeler.INTERSECTION
                ? new PolyhedralBoundedSolid()
                : BooleanCompositeOracleFuzz.copy(a);
        }
        return PolyhedralBoundedSolidModeler.setOp(a, b, op);
    }

    private static copy(s: PolyhedralBoundedSolid): PolyhedralBoundedSolid {
        return PolyhedralBoundedSolidStructuralCopy.copy(s);
    }

    private static checkOperands(shapes: Shape[]): string | null {
        for (const s of shapes) {
            try {
                const solid = s.toSolid();
                const expected = s.volume();
                const actual = PolyhedralBoundedSolidVolumeOracle.solidVolume(solid);
                if (Math.abs(actual - expected) > 1.0e-6 * Math.max(1.0e-9, expected)) {
                    return "operand " + s + " volume " + actual + " expected " + expected;
                }
            } catch (e) {
                return "operand " + s + " " + (e as Error).constructor.name;
            }
        }
        return null;
    }

    private static build(shapes: Shape[]): PolyhedralBoundedSolid[] {
        const solids: PolyhedralBoundedSolid[] = [];
        let i: number;
        for (i = 0; i < shapes.length; i++) {
            solids.push(shapes[i]!.toSolid());
        }
        return solids;
    }

    private static evaluate(
        family: Family,
        index: number,
        label: string,
        operandProblem: string | null,
        shapes: Shape[],
        membership: (member: boolean[]) => boolean,
        tolerance: number,
        expression: Expression,
    ): CaseResult {
        const r = BooleanCompositeOracleFuzz.result(family, index, label);

        if (operandProblem !== null) {
            r.outcome = Outcome.EXCEPTION;
            r.detail = operandProblem;
            return r;
        }
        const expected = CompositeShapeOracle.volume(shapes, membership);
        try {
            const out = expression(BooleanCompositeOracleFuzz.build(shapes));
            const actual = PolyhedralBoundedSolidVolumeOracle.solidVolume(out);
            const report = new StringBuilder();
            if (
                out.getPolygonsList().size() > 0 &&
                !PolyhedralBoundedSolidValidationEngine.validateStrict(out, report)
            ) {
                r.outcome = Outcome.STRICT_REJECTED;
                r.detail = BooleanCompositeOracleFuzz.firstLine(report.toString());
            } else if (Math.abs(actual - expected) > Math.max(tolerance, 1.0e-6 * expected)) {
                r.outcome = Outcome.WRONG_VOLUME;
                r.detail = "volume " + actual + " expected " + expected;
            } else {
                r.outcome = Outcome.OK;
            }
        } catch (e) {
            if (e instanceof NonManifoldBooleanResultException) {
                r.outcome = Outcome.REJECTED_NON_MANIFOLD;
                r.detail = BooleanCompositeOracleFuzz.firstLine(e.message);
            } else {
                const error = e as Error;
                r.outcome = Outcome.EXCEPTION;
                r.detail = error.constructor.name + ": " + BooleanCompositeOracleFuzz.firstLine(error.message);
            }
        }
        return r;
    }

    private static commutativity(
        family: Family,
        index: number,
        label: string,
        operandProblem: string | null,
        shapes: Shape[],
        op: number,
    ): CaseResult {
        const r = BooleanCompositeOracleFuzz.result(family, index, label);

        if (operandProblem !== null) {
            r.outcome = Outcome.EXCEPTION;
            r.detail = operandProblem;
            return r;
        }
        try {
            const ab = BooleanCompositeOracleFuzz.build(shapes);
            const ba = BooleanCompositeOracleFuzz.build(shapes);
            const first = BooleanCompositeOracleFuzz.describe(BooleanCompositeOracleFuzz.setOp(ab[0]!, ab[1]!, op));
            const second = BooleanCompositeOracleFuzz.describe(BooleanCompositeOracleFuzz.setOp(ba[1]!, ba[0]!, op));
            r.outcome = first === second ? Outcome.OK : Outcome.NOT_COMMUTATIVE;
            if (r.outcome !== Outcome.OK) {
                r.detail = first + " vs " + second;
            }
        } catch (e) {
            if (e instanceof NonManifoldBooleanResultException) {
                r.outcome = Outcome.REJECTED_NON_MANIFOLD;
            } else {
                const error = e as Error;
                r.outcome = Outcome.EXCEPTION;
                r.detail = error.constructor.name + ": " + BooleanCompositeOracleFuzz.firstLine(error.message);
            }
        }
        return r;
    }

    private static describe(solid: PolyhedralBoundedSolid): string {
        const t = PolyhedralBoundedSolidTopologySummary.from(solid);
        return (
            "F=" +
            t.getFaceCount() +
            " E=" +
            t.getEdgeCount() +
            " V=" +
            t.getVertexCount() +
            " S=" +
            t.getShellCount() +
            " vol=" +
            formatG9(PolyhedralBoundedSolidVolumeOracle.solidVolume(solid))
        );
    }

    private static result(family: Family, index: number, label: string): CaseResult {
        const r = new CaseResult();
        r.family = family;
        r.index = index;
        r.label = label;
        return r;
    }

    private static firstLine(text: string | null | undefined): string {
        if (text === null || text === undefined) {
            return "";
        }
        const cut = text.indexOf("\n");
        const line = cut < 0 ? text : text.substring(0, cut);
        return line.length > 200 ? line.substring(0, 200) : line;
    }

    /** Java `main(String[])`: prints every family. */
    public static main(args: string[]): void {
        VSDK.setWithSystemExit(false);
        for (const family of Family.values()) {
            const t0 = performance.now();
            const s = BooleanCompositeOracleFuzz.runFamily(family);
            console.log(
                family +
                    ": " +
                    s.correct +
                    " / " +
                    s.total +
                    " " +
                    javaMapToString(s.byOutcome) +
                    " " +
                    Math.trunc(performance.now() - t0) +
                    " ms",
            );
            s.failures.forEach((f) => console.log("  " + f));
            if (args.length > 0 && args[0] === "-v") {
                s.rejected.forEach((f) => console.log("  rejected " + f));
            }
        }
    }
}
