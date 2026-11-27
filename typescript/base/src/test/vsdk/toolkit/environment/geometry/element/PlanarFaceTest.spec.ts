// Port of cpp/base/src/test/planar_face_tests.cpp
// PlanarFace is exercised here with no material, solver element or
// application module: a face is built from positions only
import { describe, it } from "vitest";
import { Vector2Dd } from "vsdk/toolkit/common/linealAlgebra/Vector2Dd.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PlanarFace } from "vsdk/toolkit/environment/geometry/element/PlanarFace.js";
import { expectEq, expectFalse, expectFloatEq, expectNear, expectTrue } from "../../../../../GTestAssertions.js";

function triangle(): PlanarFace {
    const p = [new Vector3Dd(0, 0, 0), new Vector3Dd(2, 0, 0), new Vector3Dd(0, 2, 0)];
    return new PlanarFace(3, p);
}

function square(): PlanarFace {
    const p = [new Vector3Dd(0, 0, 1), new Vector3Dd(1, 0, 1), new Vector3Dd(1, 1, 1), new Vector3Dd(0, 1, 1)];
    return new PlanarFace(4, p);
}

// A trapezoid: an irregular quadrilateral, whose Jacobian is not constant
function trapezoid(): PlanarFace {
    const p = [new Vector3Dd(0, 0, 0), new Vector3Dd(4, 0, 0), new Vector3Dd(3, 2, 0), new Vector3Dd(1, 2, 0)];
    return new PlanarFace(4, p);
}

function expectPoint(x: number, y: number, z: number, p: Vector3Dd, tolerance: number): void {
    expectNear(x, p.x(), tolerance);
    expectNear(y, p.y(), tolerance);
    expectNear(z, p.z(), tolerance);
}

describe("PlanarFace", () => {
    // IsTriviallyCopyable is not ported: it checks the C++ only type property
    // std::is_trivially_copyable, which has no TypeScript counterpart

    it("TriangleGeometry", () => {
        const face = triangle();
        expectTrue(face.isValid());
        expectEq(3, face.getNumberOfVertices());
        expectFloatEq(2.0, face.getArea());
        expectPoint(0, 0, 1, face.getNormal(), 0.0);
        expectFloatEq(0.0, face.getPlaneConstant());
        expectEq(2, face.getDominantAxis());
        expectFalse(face.hasJacobian());
        expectPoint(2.0 / 3.0, 2.0 / 3.0, 0, face.getMidPoint(), 1e-6);
    });

    it("ParallelogramHasNoJacobian", () => {
        const face = square();
        expectTrue(face.isValid());
        expectFloatEq(1.0, face.getArea());
        expectFalse(face.hasJacobian());
        expectFloatEq(-1.0, face.getPlaneConstant());
        expectFloatEq(0.0, face.planeDistance(new Vector3Dd(0.3, 0.7, 1.0)));
    });

    it("IrregularQuadrilateralAreaAndJacobian", () => {
        const face = trapezoid();
        expectTrue(face.isValid());
        expectFloatEq(6.0, face.getArea());
        expectTrue(face.hasJacobian());
        expectFloatEq(face.getArea(), face.getJacobianA() + 0.5 * (face.getJacobianB() + face.getJacobianC()));
    });

    it("ParametersInvertTheMapping", () => {
        const faces = [triangle(), square(), trapezoid()];
        for (const face of faces) {
            const p = face.pointAt(0.25, 0.375);
            const uv = new Vector2Dd();
            expectTrue(face.parameters(p, uv));
            expectNear(0.25, uv.x, 1e-5);
            expectNear(0.375, uv.y, 1e-5);
        }
    });

    it("PointsOutsideAreReportedOutside", () => {
        const uv = new Vector2Dd();
        expectFalse(triangle().parameters(new Vector3Dd(1.5, 1.5, 0), uv));
        expectFalse(square().parameters(new Vector3Dd(1.5, 0.5, 1), uv));
        expectFalse(trapezoid().parameters(new Vector3Dd(0.2, 1.8, 0), uv));
    });

    it("UniformMappingPreservesArea", () => {
        const face = trapezoid();
        // The line of uniform u = 0.5 splits the area in two equal halves: by
        // symmetry of the trapezoid it is the vertical line x = 2
        const bottom = face.uniformPointAt(0.5, 0.0);
        const top = face.uniformPointAt(0.5, 1.0);
        expectNear(2.0, bottom.x(), 1e-5);
        expectNear(2.0, top.x(), 1e-5);

        // The line of uniform v = 0.5 leaves half of the area (3) under it: the
        // trapezoid of height h under it has area (4 + (4 - h)) h / 2 = 3
        const middle = face.uniformPointAt(0.5, 0.5);
        const h = 4.0 - Math.sqrt(10.0);
        expectNear(h, middle.y(), 1e-5);

        const uv = new Vector2Dd();
        expectTrue(face.uniformParameters(face.uniformPointAt(0.3, 0.6), uv));
        expectNear(0.3, uv.x, 1e-5);
        expectNear(0.6, uv.y, 1e-5);
    });

    it("RayHit", () => {
        const face = square();
        const origin = new Vector3Dd(0.5, 0.25, 3.0);

        const normalDotDirection = [0];
        expectEq(PlanarFace.Side.FRONT, face.sideFacing(new Vector3Dd(0, 0, -1), normalDotDirection));
        const t = face.planeHitParameter(origin, normalDotDirection[0]!);
        expectFloatEq(2.0, t);

        const uv = new Vector2Dd();
        expectTrue(face.parameters(new Vector3Dd(0.5, 0.25, 1.0), uv));
        expectNear(0.5, uv.x, 1e-6);
        expectNear(0.25, uv.y, 1e-6);

        expectEq(PlanarFace.Side.BACK, face.sideFacing(new Vector3Dd(0, 0, 1), normalDotDirection));
        expectEq(PlanarFace.Side.PARALLEL, face.sideFacing(new Vector3Dd(1, 0, 0), normalDotDirection));
    });

    it("RayHitInOneCall", () => {
        const face = square();
        const origin = new Vector3Dd(0.5, 0.25, 3.0);
        const down = new Vector3Dd(0, 0, -1);

        const hit = new PlanarFace.RayHit();
        expectTrue(face.rayHit(origin, down, true, false, 0.0, 10.0, hit));
        expectEq(PlanarFace.Side.FRONT, hit.getSide());
        expectFloatEq(2.0, hit.getDistance());
        expectFloatEq(0.5, hit.getPointX());
        expectFloatEq(0.25, hit.getPointY());
        expectFloatEq(1.0, hit.getPointZ());
        expectNear(0.5, hit.getU(), 1e-6);
        expectNear(0.25, hit.getV(), 1e-6);

        // Same answers as the separate calls
        const normalDotDirection = [0];
        face.sideFacing(down, normalDotDirection);
        expectEq(face.planeHitParameter(origin, normalDotDirection[0]!), hit.getDistance());

        expectFalse(face.rayHit(origin, down, false, true, 0.0, 10.0, hit)); // Front side not accepted
        expectFalse(face.rayHit(origin, down, true, false, 0.0, 1.5, hit)); // Too far
        expectFalse(face.rayHit(origin, down, true, false, 2.5, 10.0, hit)); // Too close
        expectFalse(face.rayHit(origin, new Vector3Dd(1, 0, 0), true, true, 0.0, 10.0, hit)); // Parallel
        expectFalse(face.rayHit(new Vector3Dd(5.0, 0.25, 3.0), down, true, false, 0.0, 10.0, hit)); // Outside

        expectTrue(face.rayHit(new Vector3Dd(0.5, 0.25, -1.0), new Vector3Dd(0, 0, 1), false, true, 0.0, 10.0, hit));
        expectEq(PlanarFace.Side.BACK, hit.getSide());
    });

    it("Facing", () => {
        const floor = [new Vector3Dd(0, 0, 0), new Vector3Dd(1, 0, 0), new Vector3Dd(1, 1, 0), new Vector3Dd(0, 1, 0)];
        const ceiling = [
            new Vector3Dd(0, 0, 1),
            new Vector3Dd(0, 1, 1),
            new Vector3Dd(1, 1, 1),
            new Vector3Dd(1, 0, 1),
        ];
        const above = [new Vector3Dd(0, 0, 2), new Vector3Dd(1, 0, 2), new Vector3Dd(1, 1, 2), new Vector3Dd(0, 1, 2)];
        const floorFace = new PlanarFace(4, floor);
        const ceilingFace = new PlanarFace(4, ceiling);
        const aboveFace = new PlanarFace(4, above);
        expectTrue(floorFace.facing(ceilingFace));
        expectFalse(floorFace.facing(aboveFace)); // Same orientation: one is behind the other
        expectFalse(floorFace.facing(floorFace)); // Coplanar
    });

    it("InvalidFaces", () => {
        const collinear = [new Vector3Dd(0, 0, 0), new Vector3Dd(1, 0, 0), new Vector3Dd(2, 0, 0)];
        expectEq(PlanarFace.BuildStatus.DEGENERATE_NORMAL, new PlanarFace(3, collinear).getStatus());
        const pentagon = [new Vector3Dd(), new Vector3Dd(), new Vector3Dd(), new Vector3Dd(), new Vector3Dd()];
        expectEq(PlanarFace.BuildStatus.UNSUPPORTED_VERTEX_COUNT, new PlanarFace(5, pentagon).getStatus());
        expectFalse(new PlanarFace().isValid());
    });
});
