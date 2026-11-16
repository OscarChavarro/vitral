import { Random } from "java/util/Random.js";
import type { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { VSDK } from "vsdk/toolkit/common/VSDK.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { PolyhedralBoundedSolidOperationException } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidOperationException.js";
import { javaMapToString } from "./BooleanCompositeOracleFuzz.js";
import { PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";
import { PrismFixtures } from "./PrismFixtures.js";

/** Java enum `BooleanVolumeIdentityFuzz.Family`. */
export class Family {
    /** Two regular prisms; 5 to 24 sides. */
    public static readonly CONVEX_PRISMS = new Family("CONVEX_PRISMS", 0);
    /** A star prism and a regular prism. */
    public static readonly STAR_AND_CONVEX = new Family("STAR_AND_CONVEX", 1);
    /** Two star prisms. */
    public static readonly STAR_PRISMS = new Family("STAR_PRISMS", 2);

    private constructor(
        private readonly nameValue: string,
        private readonly ordinalValue: number,
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
        return [Family.CONVEX_PRISMS, Family.STAR_AND_CONVEX, Family.STAR_PRISMS];
    }
}

/** Java `BooleanVolumeIdentityFuzz.Summary`. */
export class Summary {
    public total = 0;
    public correct = 0;
    /** Java `TreeMap`: kept sorted by key on every insertion. */
    public byOutcome = new Map<string, number>();
    public readonly failures: string[] = [];

    public merge(key: string): void {
        this.byOutcome.set(key, (this.byOutcome.get(key) ?? 0) + 1);
        this.byOutcome = new Map(Array.from(this.byOutcome.entries()).sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0)));
    }
}

/**
Seeded fuzz that needs no oracle: for any two solids the four results must
satisfy
<pre>
|A u B| = |A| + |B| - |A n B|
|A - B| = |A| - |A n B|
|B - A| = |B| - |A n B|
</pre>
with 0 &lt;= |A n B| &lt;= min(|A|, |B|). The operands are prisms: regular ones
of N sides (polyhedral cylinders) and star prisms (non-convex), B rotated
about a random axis so that no face of one is parallel to a face of the
other. The volume of each operand is also checked against the area of its
base polygon, so the prisms themselves are known to be right.

<p>A case is correct when the four operations return strict-valid solids
and the three identities hold.</p>

<p>Java nests `Family` and `Summary`; here they are sibling exports.</p>
 */
export class BooleanVolumeIdentityFuzz {
    public static readonly TOLERANCE = 1.0e-9;
    public static readonly CASES_PER_FAMILY = 16;

    private constructor() {}

    public static runFamily(family: Family): Summary {
        const summary = new Summary();
        const random = new Random(4201 + family.ordinal());
        let i: number;

        for (i = 0; i < BooleanVolumeIdentityFuzz.CASES_PER_FAMILY; i++) {
            const baseA = BooleanVolumeIdentityFuzz.base(family, 0, random);
            const baseB = BooleanVolumeIdentityFuzz.base(family, 1, random);
            const heightA = 0.8 + 0.6 * random.nextDouble();
            const heightB = 0.8 + 0.6 * random.nextDouble();
            const placement = BooleanVolumeIdentityFuzz.placement(random);
            const outcome = BooleanVolumeIdentityFuzz.check(baseA, heightA, baseB, heightB, placement);
            summary.total++;
            summary.merge(outcome.split(" ")[0]!);
            if (outcome === "OK") {
                summary.correct++;
            } else {
                summary.failures.push(family + " #" + i + " " + outcome);
            }
        }
        return summary;
    }

    private static base(family: Family, which: number, random: Random): number[][] {
        const star = family === Family.STAR_PRISMS || (family === Family.STAR_AND_CONVEX && which === 0);
        if (star) {
            const points = 4 + random.nextInt(4);
            return PrismFixtures.starPolygon(
                points,
                0.9 + 0.3 * random.nextDouble(),
                0.35 + 0.15 * random.nextDouble(),
            );
        }
        return PrismFixtures.regularPolygon(5 + random.nextInt(20), 0.7 + 0.4 * random.nextDouble());
    }

    /** B is rotated about a random axis and moved less than its size. */
    private static placement(random: Random): Matrix4x4d {
        const x = random.nextGaussian();
        const y = random.nextGaussian();
        const z = random.nextGaussian();
        const angle = 0.2 + 1.2 * random.nextDouble();
        return PolyhedralBoundedSolidVolumeOracle.translation(
            0.6 * (random.nextDouble() - 0.5),
            0.6 * (random.nextDouble() - 0.5),
            0.5 * (random.nextDouble() - 0.5) + 0.3,
        ).multiply(PolyhedralBoundedSolidVolumeOracle.rotation(angle, x, y, z));
    }

    private static a(base: number[][], height: number): PolyhedralBoundedSolid {
        return PrismFixtures.prism(base, height);
    }

    private static b(base: number[][], height: number, placement: Matrix4x4d): PolyhedralBoundedSolid {
        const solid = PrismFixtures.prism(base, height);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, placement);
        return solid;
    }

    public static check(
        baseA: number[][],
        heightA: number,
        baseB: number[][],
        heightB: number,
        placement: Matrix4x4d,
    ): string {
        const TOLERANCE = BooleanVolumeIdentityFuzz.TOLERANCE;
        const a = BooleanVolumeIdentityFuzz.a;
        const b = BooleanVolumeIdentityFuzz.b;
        const volumeA = PolyhedralBoundedSolidVolumeOracle.solidVolume(a(baseA, heightA));
        const volumeB = PolyhedralBoundedSolidVolumeOracle.solidVolume(b(baseB, heightB, placement));
        const scale = Math.max(1.0, volumeA + volumeB);
        let union: number;
        let intersection: number;
        let aMinusB: number;
        let bMinusA: number;

        if (
            Math.abs(volumeA - PrismFixtures.polygonArea(baseA) * heightA) > TOLERANCE * scale ||
            Math.abs(volumeB - PrismFixtures.polygonArea(baseB) * heightB) > TOLERANCE * scale
        ) {
            return "OPERAND volume differs from base area x height";
        }
        try {
            union = PolyhedralBoundedSolidVolumeOracle.solidVolume(
                PolyhedralBoundedSolidModeler.setOp(
                    a(baseA, heightA),
                    b(baseB, heightB, placement),
                    PolyhedralBoundedSolidModeler.UNION,
                ),
            );
            intersection = PolyhedralBoundedSolidVolumeOracle.solidVolume(
                PolyhedralBoundedSolidModeler.setOp(
                    a(baseA, heightA),
                    b(baseB, heightB, placement),
                    PolyhedralBoundedSolidModeler.INTERSECTION,
                ),
            );
            aMinusB = PolyhedralBoundedSolidVolumeOracle.solidVolume(
                PolyhedralBoundedSolidModeler.setOp(
                    a(baseA, heightA),
                    b(baseB, heightB, placement),
                    PolyhedralBoundedSolidModeler.SUBTRACT,
                ),
            );
            bMinusA = PolyhedralBoundedSolidVolumeOracle.solidVolume(
                PolyhedralBoundedSolidModeler.setOp(
                    b(baseB, heightB, placement),
                    a(baseA, heightA),
                    PolyhedralBoundedSolidModeler.SUBTRACT,
                ),
            );
        } catch (e) {
            if (e instanceof PolyhedralBoundedSolidOperationException) {
                return "EXCEPTION " + e.constructor.name + ": " + BooleanVolumeIdentityFuzz.firstLine(e.message);
            }
            throw e;
        }
        if (intersection < -TOLERANCE * scale || intersection > Math.min(volumeA, volumeB) + TOLERANCE * scale) {
            return "RANGE |AnB|=" + intersection + " |A|=" + volumeA + " |B|=" + volumeB;
        }
        const errorUnion = union - (volumeA + volumeB - intersection);
        const errorAB = aMinusB - (volumeA - intersection);
        const errorBA = bMinusA - (volumeB - intersection);
        if (
            Math.abs(errorUnion) > TOLERANCE * scale ||
            Math.abs(errorAB) > TOLERANCE * scale ||
            Math.abs(errorBA) > TOLERANCE * scale
        ) {
            return (
                "IDENTITY union " +
                errorUnion +
                ", A-B " +
                errorAB +
                ", B-A " +
                errorBA +
                " (|A|=" +
                volumeA +
                " |B|=" +
                volumeB +
                " |AnB|=" +
                intersection +
                ")"
            );
        }
        return "OK";
    }

    private static firstLine(message: string | null | undefined): string {
        if (message === null || message === undefined) {
            return "";
        }
        const line = message.split("\n")[0]!;
        return line.length > 160 ? line.substring(0, 160) : line;
    }

    /** Java `main(String[])`: prints the outcome of every family. */
    public static main(): void {
        VSDK.setWithSystemExit(false);
        for (const family of Family.values()) {
            const s = BooleanVolumeIdentityFuzz.runFamily(family);
            console.log(family + ": " + s.correct + " / " + s.total + " " + javaMapToString(s.byOutcome));
            s.failures.forEach((f) => console.log("  " + f));
        }
    }
}
