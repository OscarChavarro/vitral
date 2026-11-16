import { ArrayList } from "java/util/ArrayList.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";

/** One oriented box: half-space set plus the recipe to build its B-Rep. */
export class OrientedBox {
    public readonly size: number[];
    public readonly transform: Matrix4x4d;

    public constructor(sx: number, sy: number, sz: number, transform: Matrix4x4d) {
        this.size = [sx, sy, sz];
        this.transform = transform;
    }

    public toSolid(): PolyhedralBoundedSolid {
        const solid = new Box(
            new Vector3Dd(this.size[0]!, this.size[1]!, this.size[2]!),
        ).exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(solid, this.transform);
        return solid;
    }

    /** Planes as {nx, ny, nz, d} meaning n.x <= d, n unit outward. */
    public halfSpaces(): ArrayList<number[]> {
        const planes = new ArrayList<number[]>();
        const origin = this.transform.multiply(new Vector3Dd(0, 0, 0));
        const axes = [
            this.transform.multiply(new Vector3Dd(1, 0, 0)).subtract(origin),
            this.transform.multiply(new Vector3Dd(0, 1, 0)).subtract(origin),
            this.transform.multiply(new Vector3Dd(0, 0, 1)).subtract(origin),
        ];
        let i: number;

        for (i = 0; i < 3; i++) {
            const stretch = axes[i]!.length();
            const n = axes[i]!.normalized();
            const center = n.dotProduct(origin);
            const half = (stretch * this.size[i]!) / 2.0;
            planes.add([n.x(), n.y(), n.z(), center + half]);
            planes.add([-n.x(), -n.y(), -n.z(), -center + half]);
        }
        return planes;
    }

    public volume(): number {
        return PolyhedralBoundedSolidVolumeOracle.convexVolume(this.halfSpaces());
    }
}

/**
Independent test oracle for boolean and split results: it knows nothing
about the half-edge pipeline and only uses (a) the divergence-theorem volume
of a B-Rep and (b) the exact volume of an intersection of convex half-space
sets.

<p>For two convex operands A and B with intersection volume I:
vol(A u B) = vol(A) + vol(B) - I, vol(A n B) = I, vol(A \ B) = vol(A) - I.
This turns every box/box (or convex/convex) boolean into a test with a
known numeric answer, independent of topology counts, which is the missing
complement of the fixture-snapshot tests (faces/edges/vertices/bbox).</p>

<p>Traceability: the B-Rep volume is the integral property of [MANT1988]
Sec. 13.3 (Program 13.9 avolume), generalized to faces with rings.</p>

<p>Java nests `OrientedBox` as `PolyhedralBoundedSolidVolumeOracle.OrientedBox`;
here it is a sibling export.</p>
 */
export class PolyhedralBoundedSolidVolumeOracle {
    private constructor() {}

    public static translation(x: number, y: number, z: number): Matrix4x4d {
        return new Matrix4x4d().translation(x, y, z);
    }

    public static rotation(angle: number, x: number, y: number, z: number): Matrix4x4d {
        return new Matrix4x4d().axisRotation(angle, x, y, z);
    }

    public static intersectionVolume(a: OrientedBox, b: OrientedBox): number {
        const all = new ArrayList<number[]>(a.halfSpaces());
        all.addAll(b.halfSpaces());
        return PolyhedralBoundedSolidVolumeOracle.convexVolume(all);
    }

    /** Expected volume for op in {UNION, INTERSECTION, SUBTRACT} (A op B). */
    public static expectedVolume(a: OrientedBox, b: OrientedBox, op: number): number {
        const i = PolyhedralBoundedSolidVolumeOracle.intersectionVolume(a, b);

        if (op === PolyhedralBoundedSolidModeler.UNION) {
            return a.volume() + b.volume() - i;
        }
        if (op === PolyhedralBoundedSolidModeler.INTERSECTION) {
            return i;
        }
        return a.volume() - i;
    }

    /**
    Volume of the convex polytope { x : n_k.x <= d_k }: each plane is
    clipped against all others (Sutherland-Hodgman) and the face areas are
    accumulated with the divergence theorem. Duplicate planes are merged.
    */
    public static convexVolume(input: Iterable<number[]>): number {
        const planes = new ArrayList<number[]>();
        let volume = 0.0;
        let k: number;

        for (const p of input) {
            let duplicate = false;
            for (const q of planes) {
                if (
                    Math.abs(p[0]! - q[0]!) < 1e-12 &&
                    Math.abs(p[1]! - q[1]!) < 1e-12 &&
                    Math.abs(p[2]! - q[2]!) < 1e-12 &&
                    Math.abs(p[3]! - q[3]!) < 1e-12
                ) {
                    duplicate = true;
                }
            }
            if (!duplicate) {
                planes.add(p);
            }
        }

        for (k = 0; k < planes.size(); k++) {
            const p = planes.get(k);
            const n = new Vector3Dd(p[0]!, p[1]!, p[2]!);
            const helper = Math.abs(p[0]!) < 0.9 ? new Vector3Dd(1, 0, 0) : new Vector3Dd(0, 1, 0);
            const u = n.crossProduct(helper).normalized();
            const v = n.crossProduct(u);
            const c = n.multiply(p[3]!);
            const big = 1.0e4;
            let polygon = new ArrayList<Vector3Dd>();
            let j: number;

            polygon.add(c.add(u.multiply(big)).add(v.multiply(big)));
            polygon.add(c.add(u.multiply(-big)).add(v.multiply(big)));
            polygon.add(c.add(u.multiply(-big)).add(v.multiply(-big)));
            polygon.add(c.add(u.multiply(big)).add(v.multiply(-big)));

            for (j = 0; j < planes.size() && !polygon.isEmpty(); j++) {
                if (j === k) {
                    continue;
                }
                const q = planes.get(j);
                const qn = new Vector3Dd(q[0]!, q[1]!, q[2]!);
                const clipped = new ArrayList<Vector3Dd>();
                let i: number;

                for (i = 0; i < polygon.size(); i++) {
                    const a = polygon.get(i);
                    const b = polygon.get((i + 1) % polygon.size());
                    const da = qn.dotProduct(a) - q[3]!;
                    const db = qn.dotProduct(b) - q[3]!;

                    if (da <= 0) {
                        clipped.add(a);
                    }
                    if ((da < 0 && db > 0) || (da > 0 && db < 0)) {
                        clipped.add(a.add(b.subtract(a).multiply(da / (da - db))));
                    }
                }
                polygon = clipped;
            }
            if (polygon.size() < 3) {
                continue;
            }

            let areaVector = new Vector3Dd(0, 0, 0);
            let i: number;
            for (i = 0; i < polygon.size(); i++) {
                areaVector = areaVector.add(
                    polygon
                        .get(i)
                        .subtract(c)
                        .crossProduct(polygon.get((i + 1) % polygon.size()).subtract(c)),
                );
            }
            volume += (p[3]! * Math.abs(areaVector.dotProduct(n))) / 6.0;
        }
        return volume;
    }

    /**
    Unsigned volume of a B-Rep by the divergence theorem. Rings contribute
    with their own (opposite) orientation, so faces with holes need no
    triangulation. Zero-area laminae contribute nothing, which is why a
    volume check alone is not sufficient and must be paired with strict
    validation.
    */
    public static solidVolume(solid: PolyhedralBoundedSolid): number {
        let volume = 0.0;
        let i: number;

        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i)!;
            let areaVector = new Vector3Dd(0, 0, 0);
            let anchor: Vector3Dd | null = null;
            let j: number;

            for (j = 0; j < face.boundariesList.size(); j++) {
                const loop = face.boundariesList.get(j)!;
                const n = loop.halfEdgesList.size();
                let k: number;

                for (k = 0; k < n; k++) {
                    const a = loop.halfEdgesList.get(k)!.startingVertex.position;
                    const b = loop.halfEdgesList.get((k + 1) % n)!.startingVertex.position;
                    if (anchor === null) {
                        anchor = a;
                    }
                    areaVector = areaVector.add(a.crossProduct(b));
                }
            }
            if (anchor !== null) {
                volume += anchor.dotProduct(areaVector) / 6.0;
            }
        }
        return Math.abs(volume);
    }
}
