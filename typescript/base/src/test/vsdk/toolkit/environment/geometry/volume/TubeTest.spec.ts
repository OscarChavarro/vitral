// Port of java/base/src/test/vsdk/toolkit/environment/geometry/volume/TubeTest.java
import { describe, expect, it } from "vitest";
import { expectNear } from "../../../../../GTestAssertions.js";

import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Geometry } from "vsdk/toolkit/environment/geometry/Geometry.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayCrossingQuery } from "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.js";
import { RayHit } from "vsdk/toolkit/environment/geometry/element/RayHit.js";
import { RaySolidClassification } from "vsdk/toolkit/environment/geometry/element/RaySolidClassification.js";
import { RaySolidQueryStatus } from "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.js";
import { Tube } from "vsdk/toolkit/environment/geometry/volume/Tube.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidFace } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import { StringBuilder } from "java/lang/StringBuilder.js";

/**
Exercises the `Tube` (axis along Z, base at z = 0): ray intersection, ray
classification, point containment, and its boundary representation built
with Euler operators, also for the washer (height 0) case.
*/
describe("TubeTest", () => {
    const TOLERANCE = 0.01;
    const EPS = 1e-9;

    function ray(ox: number, oy: number, oz: number, dx: number, dy: number, dz: number): Ray {
        return new Ray(new Vector3Dd(ox, oy, oz), new Vector3Dd(dx, dy, dz));
    }

    function fullHit(): RayHit {
        return new RayHit(RayHit.DETAIL_ALL, true);
    }

    it("given_tube_when_rayFromOutsideHitsExternalWall_then_normalPointsOut", () => {
        const tube = new Tube(1, 2, 3);
        const hit = fullHit();

        expect(tube.doIntersectionFirstHit(ray(-5, 0, 1, 1, 0, 0), hit)).toBe(true);
        expectNear(3, hit.getRay()!.getT(), EPS);
        expectNear(-1, hit.normal.x(), EPS);
    });

    it("given_tube_when_rayStartsInHole_then_hitsInternalWallWithNormalToAxis", () => {
        const tube = new Tube(1, 2, 3);
        const hit = fullHit();

        expect(tube.doIntersectionFirstHit(ray(0, 0, 1, 1, 0, 0), hit)).toBe(true);
        expectNear(1, hit.getRay()!.getT(), EPS);
        expectNear(-1, hit.normal.x(), EPS);
    });

    it("given_tube_when_verticalRayOverWallOrHole_then_capOrNothingIsHit", () => {
        const tube = new Tube(1, 2, 3);
        const hit = fullHit();

        expect(tube.doIntersectionFirstHit(ray(1.5, 0, 10, 0, 0, -1), hit)).toBe(true);
        expectNear(7, hit.getRay()!.getT(), EPS);
        expectNear(1, hit.normal.z(), EPS);
        expect(tube.doIntersectionFirstHit(ray(0.5, 0, 10, 0, 0, -1), fullHit())).toBe(false);
        expect(tube.doIntersectionFirstHit(ray(2.5, 0, 10, 0, 0, -1), fullHit())).toBe(false);
    });

    it("given_solidCylinderTube_when_rayCrossesAxis_then_oneInterval", () => {
        const tube = new Tube(0, 2, 3);
        const c = new RaySolidClassification();

        expect(tube.classifyRay(ray(-5, 0, 1, 1, 0, 0), new RayCrossingQuery(0, 100), c)).toBe(
            RaySolidQueryStatus.Supported,
        );
        expect(c.boundaries.size()).toBe(2);
        expectNear(3, c.boundaries.get(0).t, EPS);
        expectNear(7, c.boundaries.get(1).t, EPS);
    });

    it("given_tube_when_rayCrossesHole_then_twoInteriorIntervals", () => {
        const tube = new Tube(1, 2, 3);
        const c = new RaySolidClassification();

        expect(tube.classifyRay(ray(-5, 0, 1, 1, 0, 0), new RayCrossingQuery(0, 100), c)).toBe(
            RaySolidQueryStatus.Supported,
        );
        expect(c.insideSegments.get(0)).toBe(false);
        expect(c.boundaries.size()).toBe(4);
        const expected = [3, 4, 6, 7];
        for (let i = 0; i < 4; i++) {
            expectNear(expected[i]!, c.boundaries.get(i).t, EPS);
            expect(c.insideSegments.get(i + 1)).toBe(i % 2 === 0);
        }
    });

    it("given_tube_when_queryStartsInsideWall_then_startsInside", () => {
        const tube = new Tube(1, 2, 3);
        const c = new RaySolidClassification();

        tube.classifyRay(ray(-5, 0, 1, 1, 0, 0), new RayCrossingQuery(3.5, 100), c);
        expect(c.insideSegments.get(0)).toBe(true);
        expect(c.boundaries.size()).toBe(3);
        expectNear(4, c.boundaries.get(0).t, EPS);
    });

    it("given_tube_when_pointsClassified_then_wallHoleAndCapsAreRespected", () => {
        const tube = new Tube(1, 2, 3);

        expect(tube.doContainmentTest(new Vector3Dd(1.5, 0, 1), TOLERANCE)).toBe(Geometry.INSIDE);
        expect(tube.doContainmentTest(new Vector3Dd(0, 0.5, 1), TOLERANCE)).toBe(Geometry.OUTSIDE);
        expect(tube.doContainmentTest(new Vector3Dd(0, 1.0, 1), TOLERANCE)).toBe(Geometry.LIMIT);
        expect(tube.doContainmentTest(new Vector3Dd(2.0, 0, 1), TOLERANCE)).toBe(Geometry.LIMIT);
        expect(tube.doContainmentTest(new Vector3Dd(1.5, 0, 3), TOLERANCE)).toBe(Geometry.LIMIT);
        expect(tube.doContainmentTest(new Vector3Dd(2.5, 0, 1), TOLERANCE)).toBe(Geometry.OUTSIDE);
        expect(tube.doContainmentTest(new Vector3Dd(1.5, 0, -0.5), TOLERANCE)).toBe(Geometry.OUTSIDE);
    });

    it("given_washer_when_raysAndPointsTested_then_onlyTheAnnulusIsHit", () => {
        const washer = new Tube(1, 2, 0);
        const hit = fullHit();
        const c = new RaySolidClassification();

        expect(washer.isWasher()).toBe(true);
        expect(washer.doIntersectionFirstHit(ray(1.5, 0, 5, 0, 0, -1), hit)).toBe(true);
        expectNear(5, hit.getRay()!.getT(), EPS);
        expectNear(1, hit.normal.z(), EPS);
        expect(washer.doIntersectionFirstHit(ray(1.5, 0, -5, 0, 0, 1), hit)).toBe(true);
        expectNear(-1, hit.normal.z(), EPS);
        expect(washer.doIntersectionFirstHit(ray(0.5, 0, 5, 0, 0, -1), fullHit())).toBe(false);
        expect(washer.doContainmentTest(new Vector3Dd(1.5, 0, 0), TOLERANCE)).toBe(Geometry.LIMIT);
        expect(washer.doContainmentTest(new Vector3Dd(0.5, 0, 0), TOLERANCE)).toBe(Geometry.OUTSIDE);
        washer.classifyRay(ray(1.5, 0, 5, 0, 0, -1), new RayCrossingQuery(0, 100), c);
        expect(c.boundaries.isEmpty()).toBe(true);
    });

    function assertValidBrep(
        solid: PolyhedralBoundedSolid,
        expectedVertices: number,
        expectedEdges: number,
        expectedFaces: number,
    ): void {
        const topology = PolyhedralBoundedSolidTopologySummary.from(solid);

        expect(topology.getVertexCount()).toBe(expectedVertices);
        expect(topology.getEdgeCount()).toBe(expectedEdges);
        expect(topology.getFaceCount()).toBe(expectedFaces);
        expect(topology.getShellCount()).toBe(1);
        const msg = new StringBuilder();
        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(solid, msg), msg.toString()).toBe(true);
    }

    it("given_tube_when_exportedToBrep_then_isValidSolidOfGenusOne", () => {
        const n = 12;
        const solid = new Tube(1, 2, 3).exportToPolyhedralBoundedSolid(n);

        // Top and bottom annuli (one face each, with a ring) plus n external
        // and n internal side faces; V - E + F - (L - F) = 2 (S - H) with
        // one ring in each cap and genus 1
        assertValidBrep(solid, 4 * n, 6 * n, 2 * n + 2);
        for (let i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i)!;
            const normal = face.getContainingPlane()!.getNormal();
            const p = face.boundariesList.get(0)!.boundaryStartHalfEdge!.startingVertex.position;
            const centroid = faceCentroid(face);
            if (Math.abs(normal.z()) > 0.5) {
                // Caps: outward along Z
                expect(Math.sign(normal.z())).toBe(p.z() > 1.5 ? 1.0 : -1.0);
            } else {
                // Walls: away from the axis outside, towards it inside
                const radial = normal.x() * centroid.x() + normal.y() * centroid.y();
                const rho = Math.hypot(centroid.x(), centroid.y());
                expect(Math.sign(radial)).toBe(rho > 1.5 ? 1.0 : -1.0);
            }
        }
    });

    it("given_washer_when_exportedToBrep_then_isLaminaWithHole", () => {
        const n = 12;
        const solid = new Tube(1, 2, 0).exportToPolyhedralBoundedSolid(n);
        const topology = PolyhedralBoundedSolidTopologySummary.from(solid);

        expect(topology.getVertexCount()).toBe(2 * n);
        expect(topology.getEdgeCount()).toBe(2 * n);
        expect(topology.getFaceCount()).toBe(2);
        for (let i = 0; i < solid.getPolygonsList().size(); i++) {
            expect(solid.getPolygonsList().get(i)!.boundariesList.size()).toBe(2);
        }
    });

    it("given_solidCylinderTube_when_exportedToBrep_then_isValid", () => {
        const n = 12;
        assertValidBrep(new Tube(0, 2, 3).exportToPolyhedralBoundedSolid(n), 2 * n, 3 * n, n + 2);
    });

    function faceCentroid(face: _PolyhedralBoundedSolidFace): Vector3Dd {
        const start = face.boundariesList.get(0)!.boundaryStartHalfEdge!;
        let he = start;
        let sum = new Vector3Dd(0, 0, 0);
        let count = 0;
        do {
            sum = sum.add(he.startingVertex.position);
            count++;
            he = he.next()!;
        } while (he !== start);
        return sum.multiply(1.0 / count);
    }
});
