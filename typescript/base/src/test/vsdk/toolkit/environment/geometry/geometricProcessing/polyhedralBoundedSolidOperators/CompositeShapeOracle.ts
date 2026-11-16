import type { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { OrientedBox, PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";

/**
Java `TreeSet<Double>` turned into a list: ascending order by
`Double.compare` (so -0.0 sorts before 0.0 and both are kept), duplicates
removed.
 */
function sortedDistinct(values: number[]): number[] {
    const sorted = values.slice().sort((a, b) => {
        if (a < b) {
            return -1;
        }
        if (a > b) {
            return 1;
        }
        if (Object.is(a, b)) {
            return 0;
        }
        return Object.is(a, -0) ? -1 : 1;
    });
    const out: number[] = [];
    for (const v of sorted) {
        if (out.length === 0 || !Object.is(out[out.length - 1], v)) {
            out.push(v);
        }
    }
    return out;
}

/** Axis-aligned boxes {x0, y0, z0, x1, y1, z1} in the local frame. */
export class Shape {
    public constructor(
        public readonly name: string,
        public readonly positive: number[][],
        public readonly negative: number[][],
        public readonly frame: Matrix4x4d,
    ) {}

    public moved(motion: Matrix4x4d): Shape {
        return new Shape(this.name, this.positive, this.negative, motion.multiply(this.frame));
    }

    /** Disjoint convex cells of the shape, in world coordinates. */
    public cells(): OrientedBox[] {
        const cells: OrientedBox[] = [];
        const xs = this.coordinates(0);
        const ys = this.coordinates(1);
        const zs = this.coordinates(2);
        let i: number;
        let j: number;
        let k: number;

        for (i = 0; i + 1 < xs.length; i++) {
            for (j = 0; j + 1 < ys.length; j++) {
                for (k = 0; k + 1 < zs.length; k++) {
                    const c = [(xs[i]! + xs[i + 1]!) / 2.0, (ys[j]! + ys[j + 1]!) / 2.0, (zs[k]! + zs[k + 1]!) / 2.0];
                    if (!this.insideLocal(c)) {
                        continue;
                    }
                    cells.push(
                        new OrientedBox(
                            xs[i + 1]! - xs[i]!,
                            ys[j + 1]! - ys[j]!,
                            zs[k + 1]! - zs[k]!,
                            this.frame.multiply(PolyhedralBoundedSolidVolumeOracle.translation(c[0]!, c[1]!, c[2]!)),
                        ),
                    );
                }
            }
        }
        return cells;
    }

    private insideLocal(p: number[]): boolean {
        let inside = false;
        for (const b of this.positive) {
            inside = inside || Shape.insideBox(b, p);
        }
        for (const b of this.negative) {
            inside = inside && !Shape.insideBox(b, p);
        }
        return inside;
    }

    private static insideBox(b: number[], p: number[]): boolean {
        return p[0]! > b[0]! && p[0]! < b[3]! && p[1]! > b[1]! && p[1]! < b[4]! && p[2]! > b[2]! && p[2]! < b[5]!;
    }

    private coordinates(axis: number): number[] {
        const values: number[] = [];
        for (const b of this.positive) {
            values.push(b[axis]!);
            values.push(b[axis + 3]!);
        }
        for (const b of this.negative) {
            values.push(b[axis]!);
            values.push(b[axis + 3]!);
        }
        return sortedDistinct(values);
    }

    /**
    Builds the B-Rep with the kernel in the axis-aligned local frame (a
    union of the positive boxes minus the negative ones), then applies
    the rigid motion. Tests compare its volume with `volume()`
    before using it as an operand.
    */
    public toSolid(): PolyhedralBoundedSolid {
        let solid = Shape.box(this.positive[0]!);
        let i: number;

        for (i = 1; i < this.positive.length; i++) {
            solid = PolyhedralBoundedSolidModeler.setOp(
                solid,
                Shape.box(this.positive[i]!),
                PolyhedralBoundedSolidModeler.UNION,
            );
        }
        for (const b of this.negative) {
            solid = PolyhedralBoundedSolidModeler.setOp(solid, Shape.box(b), PolyhedralBoundedSolidModeler.SUBTRACT);
        }
        PolyhedralBoundedSolidModeler.applyTransformation(solid, this.frame);
        return solid;
    }

    public volume(): number {
        return CompositeShapeOracle.volume([this], (m) => m[0]!);
    }

    public toString(): string {
        return this.name;
    }

    private static box(b: number[]): PolyhedralBoundedSolid {
        return new OrientedBox(
            b[3]! - b[0]!,
            b[4]! - b[1]!,
            b[5]! - b[2]!,
            PolyhedralBoundedSolidVolumeOracle.translation(
                (b[0]! + b[3]!) / 2.0,
                (b[1]! + b[4]!) / 2.0,
                (b[2]! + b[5]!) / 2.0,
            ),
        ).toSolid();
    }
}

function bitCount(value: number): number {
    let count = 0;
    let v = value;
    while (v !== 0) {
        count += v & 1;
        v >>>= 1;
    }
    return count;
}

/**
Exact volume oracle for non-convex operands (Fable5.2Analysis.md Section 8,
"further tests worth writing"). A shape is a union of boxes minus a union
of boxes, all axis-aligned in the shape's own frame, followed by a rigid
motion: L, U and stepped blocks, and a frame with a through hole (genus 1).

<p>The oracle never asks the kernel. Each shape is cut into disjoint
convex cells along the coordinates of its boxes; a cell belongs to the
shape when its centre is in some positive box and in no negative box. The
volume of the points that are in exactly the shapes of a set P is
obtained by inclusion-exclusion from the volumes of the intersections of
shapes, and each intersection is a sum over tuples of cells of the volume
of a convex polytope (`PolyhedralBoundedSolidVolumeOracle.convexVolume`).
So the oracle is exact for any boolean expression of up to a few shapes,
chains included.</p>

<p>Traceability: [MANT1988] Sec. 13.3 (volume as integral property),
Appendix B (regularised set operations).</p>

<p>Java nests `Shape` as `CompositeShapeOracle.Shape`; here it is a
sibling export.</p>
 */
export class CompositeShapeOracle {
    private constructor() {}

    /**
    Exact volume of the points whose membership vector (one flag per shape)
    satisfies `expression`. The expression must be false when the
    point is in no shape.
    */
    public static volume(shapes: Shape[], expression: (member: boolean[]) => boolean): number {
        const n = shapes.length;
        const cells: OrientedBox[][] = [];
        const intersection = new Array<number>(1 << n).fill(0.0);
        let total = 0.0;
        let set: number;

        for (const s of shapes) {
            cells.push(s.cells());
        }
        for (set = 1; set < 1 << n; set++) {
            intersection[set] = CompositeShapeOracle.intersectionVolume(cells, set);
        }
        for (set = 1; set < 1 << n; set++) {
            const member: boolean[] = [];
            let i: number;
            for (i = 0; i < n; i++) {
                member.push((set & (1 << i)) !== 0);
            }
            if (!expression(member)) {
                continue;
            }
            // Points in exactly the shapes of `set`: Moebius inversion.
            let superset: number;
            let exact = 0.0;
            for (superset = set; superset < 1 << n; superset++) {
                if ((superset & set) !== set) {
                    continue;
                }
                const extra = bitCount(superset) - bitCount(set);
                exact += (extra % 2 === 0 ? 1.0 : -1.0) * intersection[superset]!;
            }
            total += exact;
        }
        return total;
    }

    private static intersectionVolume(cells: OrientedBox[][], set: number): number {
        const chosen: OrientedBox[][] = [];
        let i: number;
        for (i = 0; i < cells.length; i++) {
            if ((set & (1 << i)) !== 0) {
                chosen.push(cells[i]!);
            }
        }
        return CompositeShapeOracle.sumOverTuples(chosen, 0, []);
    }

    private static sumOverTuples(chosen: OrientedBox[][], depth: number, planes: number[][]): number {
        if (depth === chosen.length) {
            return PolyhedralBoundedSolidVolumeOracle.convexVolume(planes);
        }
        let sum = 0.0;
        for (const cell of chosen[depth]!) {
            const next = planes.slice();
            next.push(...cell.halfSpaces().toArray());
            sum += CompositeShapeOracle.sumOverTuples(chosen, depth + 1, next);
        }
        return sum;
    }
}
