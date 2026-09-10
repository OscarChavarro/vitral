import { Vector3Dd } from "../../../../common/linealAlgebra/Vector3Dd.js";
import { Ray } from "../../element/Ray.js";
import { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidNumericPolicy } from "./PolyhedralBoundedSolidNumericPolicy.js";
/** Robust ray-parity predicates following the general-position approach of [EDEL1990], used by quantitative invisibility [APPE1967]. */
export class PolyhedralBoundedSolidPredicates {
    private static readonly probes = [
        new Vector3Dd(0.3172, 0.549, 0.7725),
        new Vector3Dd(0.803, -0.1399, 0.5793),
        new Vector3Dd(-0.4211, 0.7022, 0.5735),
        new Vector3Dd(0.611, 0.4561, -0.647),
    ].map((x) => x.normalized());
    public static isPointInside(s: PolyhedralBoundedSolid, p: Vector3Dd): boolean {
        const c = PolyhedralBoundedSolidNumericPolicy.forPoints(s.getVerticesList().map((v) => v.position)),
            max = c.modelScale() * 100 + 1;
        let last = 0;
        for (const d of this.probes) {
            const n = this.crossings(s, p, d, max, c.bigEpsilon());
            if (n >= 0) return n % 2 === 1;
            last = -n - 1;
        }
        return last % 2 === 1;
    }
    public static quantitativeInvisibility(s: PolyhedralBoundedSolid, eye: Vector3Dd, point: Vector3Dd): number {
        const d = point.subtract(eye),
            length = d.length();
        if (length === 0) return 0;
        const direction = d.multiply(1 / length),
            reach = length * (1 - 0.001),
            c = PolyhedralBoundedSolidNumericPolicy.forPoints(s.getVerticesList().map((v) => v.position));
        const ts = this.distances(s, eye, direction, reach, c.bigEpsilon()) ?? [];
        let previous = false,
            qi = 0,
            lower = 0;
        for (const upper of [...ts, reach])
            if (upper - lower > c.epsilon()) {
                const midpoint = eye.add(direction.multiply((lower + upper) / 2)),
                    inside = !this.isOnSurface(s, midpoint, c.bigEpsilon()) && this.isPointInside(s, midpoint);
                if (inside && !previous) qi++;
                previous = inside;
                lower = upper;
            }
        return qi;
    }
    /**
     * A line that runs along a face must not manufacture an interior interval.
     * This is the explicit ON_SURFACE state used by the Java Appel predicate.
     */
    private static isOnSurface(s: PolyhedralBoundedSolid, point: Vector3Dd, tolerance: number): boolean {
        return s.getPolygonsList().some((face) => {
            const plane = face.getContainingPlane();
            return (
                plane !== null &&
                Math.abs(plane.pointDistance(point)) <= tolerance &&
                face.testPointInside(point, tolerance, plane) !== 0
            );
        });
    }
    private static crossings(s: PolyhedralBoundedSolid, p: Vector3Dd, d: Vector3Dd, max: number, e: number): number {
        const values = this.distances(s, p, d, max, e, true);
        return values === null ? -1 : values.length;
    }
    private static distances(
        s: PolyhedralBoundedSolid,
        o: Vector3Dd,
        d: Vector3Dd,
        max: number,
        e: number,
        grazing = false,
    ): number[] | null {
        const values: number[] = [];
        for (const face of s.getPolygonsList()) {
            const plane = face.getContainingPlane(),
                ray = new Ray(o, d);
            if (plane === null) continue;
            const hit = plane.intersectRay(ray);
            if (hit === null || hit.getT() <= e || hit.getT() >= max) continue;
            const p = o.add(d.multiply(hit.getT())),
                status = face.testPointInside(p, e, plane);
            if (grazing && status === 2) return null;
            if (status === 0) continue;
            if (!values.some((x) => Math.abs(x - hit.getT()) < e)) values.push(hit.getT());
        }
        return values.sort((a, b) => a - b);
    }
}
