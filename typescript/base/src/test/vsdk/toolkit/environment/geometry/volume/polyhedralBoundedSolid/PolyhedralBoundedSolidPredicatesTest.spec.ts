import { describe, expect, it } from "vitest";

import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolidPredicates } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidPredicates.js";
import { Geometry } from "vsdk/toolkit/environment/geometry/Geometry.js";
import { PolyhedralBoundedSolidTestFixtures } from "./PolyhedralBoundedSolidTestFixtures.js";
import { SimpleTestGeometryLibrary } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.js";

/**
Harness accommodation: JUnit has no per-test time budget, while vitest
defaults to 5 s. Building APPE1967_3 needs several boolean unions and the
ground-truth sweep visits 21^3 sample points.
*/
const APPE1967_TIMEOUT_MS = 900_000;

describe("PolyhedralBoundedSolidPredicatesTest", () => {
    const BOX = [
        [0.5, 0.1, 0.1, 0.5, 0.1, 0.1],
        [0.5, 0.9, 0.1, 0.5, 0.1, 0.1],
        [0.1, 0.5, 0.1, 0.1, 0.5, 0.1],
        [0.9, 0.5, 0.1, 0.1, 0.5, 0.1],
        [0.1, 0.5, 0.1, 0.1, 0.5, 0.1],
        [0.1, 0.5, 0.9, 0.1, 0.5, 0.1],
        [0.1, 0.1, 0.5, 0.1, 0.1, 0.5],
        [0.1, 0.9, 0.5, 0.1, 0.1, 0.5],
        [0.3, 0.5, 0.5, 0.3, 0.1, 0.1],
        [0.5, 0.5, 0.5, 0.1, 0.1, 0.5],
        [0.7, 0.5, 0.9, 0.3, 0.1, 0.1],
        [0.9, 0.5, 0.9, 0.1, 0.5, 0.1],
    ];

    function boxInside(p: Vector3Dd): boolean {
        for (const b of BOX) {
            if (
                Math.abs(p.x() - b[0]!) <= b[3]! - 1e-7 &&
                Math.abs(p.y() - b[1]!) <= b[4]! - 1e-7 &&
                Math.abs(p.z() - b[2]!) <= b[5]! - 1e-7
            )
                return true;
        }
        return false;
    }

    // Java also declares `march(Matrix4x4d, Vector3Dd, Vector3Dd)`; it is
    // unused by the single test below, so it is kept only as this reference
    // note (unused private helpers are a lint error here).
    void Matrix4x4d;

    it(
        "isPointInside_matches12BoxGroundTruth",
        () => {
            const solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
            let bad = 0,
                tested = 0;
            for (let ix = 0; ix <= 20; ix++)
                for (let iy = 0; iy <= 20; iy++)
                    for (let iz = 0; iz <= 20; iz++) {
                        const x = ix / 20.0,
                            y = iy / 20.0,
                            z = iz / 20.0;
                        const p = new Vector3Dd(x, y, z);
                        // skip points near any box boundary (genuinely ambiguous surface)
                        let nearBoundary = false;
                        for (const b of BOX) {
                            for (let a = 0; a < 3; a++) {
                                const c = a === 0 ? p.x() : a === 1 ? p.y() : p.z();
                                const cc = b[a]!,
                                    h = b[3 + a]!;
                                if (Math.abs(Math.abs(c - cc) - h) < 0.01) nearBoundary = true;
                            }
                        }
                        if (nearBoundary) continue;
                        tested++;
                        const gt = boxInside(p);
                        const got = PolyhedralBoundedSolidPredicates.isPointInside(solid, p);
                        if (gt !== got) {
                            bad++;
                            if (bad <= 10)
                                console.log(
                                    `  inside mismatch p=(${x.toFixed(2)},${y.toFixed(2)},${z.toFixed(2)}) gt=${gt} got=${got}`,
                                );
                        }
                    }
            console.log("isPointInside tested=" + tested + " bad=" + bad);
            expect(bad).toBe(0);
        },
        APPE1967_TIMEOUT_MS,
    );
    it("given_boxSolid_when_pointsClassified_then_doContainmentTestDistinguishesFaces", () => {
        // Arrange: unit box centered at (2, 0, 0)
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1, 1, 1, 2, 0, 0);
        const tolerance = 0.01;

        // Act & Assert
        expect(solid.doContainmentTest(new Vector3Dd(2, 0, 0), tolerance)).toBe(Geometry.INSIDE);
        expect(solid.doContainmentTest(new Vector3Dd(2.5, 0.1, -0.2), tolerance)).toBe(Geometry.LIMIT);
        expect(solid.doContainmentTest(new Vector3Dd(2, 0, 0.505), tolerance)).toBe(Geometry.LIMIT);
        expect(solid.doContainmentTest(new Vector3Dd(2, 0, 0.6), tolerance)).toBe(Geometry.OUTSIDE);
        expect(solid.doContainmentTest(new Vector3Dd(0, 0, 0), tolerance)).toBe(Geometry.OUTSIDE);
    });

    it("given_nonConvexSolid_when_pointInTheNotchClassified_then_itIsOutside", () => {
        // Arrange: profile of [MANT1986].4 in the XZ plane, extruded along Y
        // from y = 0.4 to y = 0; its top has a notch between x = 0.37 and
        // x = 0.60 down to z = 0.30
        const solid = PolyhedralBoundedSolidTestFixtures.createMant1986_1Solid();
        const tolerance = 0.001;

        // Act & Assert
        expect(solid.doContainmentTest(new Vector3Dd(0.48, 0.2, 0.4), tolerance)).toBe(Geometry.OUTSIDE);
        expect(solid.doContainmentTest(new Vector3Dd(0.48, 0.2, 0.2), tolerance)).toBe(Geometry.INSIDE);
        expect(solid.doContainmentTest(new Vector3Dd(0.9, 0.2, 0.4), tolerance)).toBe(Geometry.INSIDE);
        expect(solid.doContainmentTest(new Vector3Dd(0.48, 0.2, 0.3), tolerance)).toBe(Geometry.LIMIT);
    });
});
