import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerConstructions } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerConstructions.js";
import { PolyhedralBoundedSolidEulerOperators } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";

/**
Prisms built with the Euler operators and the translational sweep, for the
tests that need operands other than boxes: regular prisms of N sides (the
polyhedral stand-in for a cylinder) and star prisms (non-convex, with N
reflex edges).
 */
export class PrismFixtures {
    private constructor() {}

    /** Prism over the polygon `xy` (counterclockwise), from z=0 to z=height. */
    public static prism(xy: number[][], height: number): PolyhedralBoundedSolid {
        const solid = new PolyhedralBoundedSolid();
        let i: number;

        PolyhedralBoundedSolidEulerOperators.mvfs(solid, new Vector3Dd(xy[0]![0]!, xy[0]![1]!, 0.0), 1, 1);
        for (i = 1; i < xy.length; i++) {
            PolyhedralBoundedSolidEulerOperators.smev(solid, 1, i, i + 1, new Vector3Dd(xy[i]![0]!, xy[i]![1]!, 0.0));
        }
        PolyhedralBoundedSolidEulerOperators.smef(solid, 1, xy.length, 1, 2);
        let t = new Matrix4x4d();
        t = t.translation(0.0, 0.0, height);
        PolyhedralBoundedSolidEulerConstructions.translationalSweepExtrudeFacePlanar(solid, solid.findFace(1)!, t);
        return solid;
    }

    /** Regular polygon of `sides` sides inscribed in a circle. */
    public static regularPolygon(sides: number, radius: number): number[][] {
        const xy: number[][] = [];
        let i: number;

        for (i = 0; i < sides; i++) {
            const a = (2.0 * Math.PI * i) / sides;
            xy.push([radius * Math.cos(a), radius * Math.sin(a)]);
        }
        return xy;
    }

    /** Star of `points` points: vertices alternate between two radii. */
    public static starPolygon(points: number, outerRadius: number, innerRadius: number): number[][] {
        const xy: number[][] = [];
        let i: number;

        for (i = 0; i < 2 * points; i++) {
            const a = (Math.PI * i) / points;
            const r = i % 2 === 0 ? outerRadius : innerRadius;
            xy.push([r * Math.cos(a), r * Math.sin(a)]);
        }
        return xy;
    }

    /** Area of a simple polygon (positive when counterclockwise). */
    public static polygonArea(xy: number[][]): number {
        let area = 0.0;
        let i: number;

        for (i = 0; i < xy.length; i++) {
            const p = xy[i]!;
            const q = xy[(i + 1) % xy.length]!;
            area += p[0]! * q[1]! - q[0]! * p[1]!;
        }
        return area / 2.0;
    }
}
