// Port of cpp/base/src/test/prism_tests.cpp
import { describe, expect, it } from "vitest";
import { expectNear } from "../../../../../GTestAssertions.js";

import { ArrayList } from "java/util/ArrayList.js";
import { StringBuilder } from "java/lang/StringBuilder.js";
import type { Entity } from "vsdk/toolkit/common/Entity.js";
import { Vector2Dd } from "vsdk/toolkit/common/linealAlgebra/Vector2Dd.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Geometry } from "vsdk/toolkit/environment/geometry/Geometry.js";
import { GeometryBounds } from "vsdk/toolkit/environment/geometry/element/GeometryBounds.js";
import { GeometryContainment } from "vsdk/toolkit/environment/geometry/element/GeometryContainment.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayCrossingQuery } from "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.js";
import { RayFirstHitStatus } from "vsdk/toolkit/environment/geometry/element/RayFirstHitStatus.js";
import { RayHit } from "vsdk/toolkit/environment/geometry/element/RayHit.js";
import { RaySolidClassification } from "vsdk/toolkit/environment/geometry/element/RaySolidClassification.js";
import { RaySolidQueryStatus } from "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { Prism } from "vsdk/toolkit/environment/geometry/volume/Prism.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { SimpleBody } from "vsdk/toolkit/environment/scene/SimpleBody.js";
import { CsgBody } from "vsdk/toolkit/environment/scene/csg/CsgBody.js";
import { CsgBodyEvaluator } from "vsdk/toolkit/environment/scene/csg/CsgBodyEvaluator.js";
import { CsgOperation } from "vsdk/toolkit/environment/scene/csg/CsgOperation.js";
import { ControlSpecification } from "vsdk/toolkit/gui/editor/ControlSpecification.js";
import { GenericEditor } from "vsdk/toolkit/gui/editor/GenericEditor.js";

/**
Exercises the parametric `Prism` (base polygon on z = 0, swept along +Z):
ray intersection and [ROTH1982] ray classification against convex and
concave bases (compared with `Box` for a rectangular base), CSG with the
Roth evaluator, point containment, the boundary representation and the
parameters exposed to generic editors.
*/
describe("PrismTest", () => {
    const EPS = 1e-9;

    function ray(ox: number, oy: number, oz: number, dx: number, dy: number, dz: number): Ray {
        return new Ray(new Vector3Dd(ox, oy, oz), new Vector3Dd(dx, dy, dz));
    }

    function square(half: number): ArrayList<Vector2Dd> {
        const polygon = new ArrayList<Vector2Dd>();
        polygon.add(new Vector2Dd(-half, -half));
        polygon.add(new Vector2Dd(half, -half));
        polygon.add(new Vector2Dd(half, half));
        polygon.add(new Vector2Dd(-half, half));
        return polygon;
    }

    /** "U" shape: x in [0, 3], y in [0, 2], with the notch x in [1, 2], y > 1 */
    function uShape(): ArrayList<Vector2Dd> {
        const polygon = new ArrayList<Vector2Dd>();
        polygon.add(new Vector2Dd(0, 0));
        polygon.add(new Vector2Dd(3, 0));
        polygon.add(new Vector2Dd(3, 2));
        polygon.add(new Vector2Dd(2, 2));
        polygon.add(new Vector2Dd(2, 1));
        polygon.add(new Vector2Dd(1, 1));
        polygon.add(new Vector2Dd(1, 2));
        polygon.add(new Vector2Dd(0, 2));
        return polygon;
    }

    function star(points: number, outer: number, inner: number): ArrayList<Vector2Dd> {
        const polygon = new ArrayList<Vector2Dd>();
        for (let i = 0; i < 2 * points; i++) {
            const a = (Math.PI * i) / points;
            const r = i % 2 === 0 ? outer : inner;
            polygon.add(new Vector2Dd(r * Math.cos(a), r * Math.sin(a)));
        }
        return polygon;
    }

    /**
    Deterministic replacement for the C `std::srand` / `std::rand` pair of
    the C++ test: a 31-bit linear congruential generator. The test only
    compares two geometries over the same rays, so the exact sequence is
    not part of the contract.
    */
    let seed = 12345;
    function random(low: number, high: number): number {
        seed = (Math.imul(seed, 1103515245) + 12345) & 0x7fffffff;
        return low + (high - low) * (seed / 0x7fffffff);
    }

    it("given_squarePrism_when_randomRaysClassified_then_matchesBox", () => {
        const prism = new Prism(square(1.0), 2.0);
        const box = new Box(new Vector3Dd(-1, -1, 0), new Vector3Dd(1, 1, 2));
        seed = 12345;
        let compared = 0;

        for (let i = 0; i < 2000; i++) {
            const origin = new Vector3Dd(random(-4, 4), random(-4, 4), random(-3, 5));
            const direction = new Vector3Dd(random(-1, 1), random(-1, 1), random(-1, 1));
            if (direction.length() < 0.1) {
                continue;
            }
            const r = new Ray(origin, direction);
            const query = new RayCrossingQuery(0.0, 100.0);
            const fromPrism = new RaySolidClassification();
            const fromBox = new RaySolidClassification();
            expect(prism.classifyRay(r, query, fromPrism)).toBe(RaySolidQueryStatus.Supported);
            expect(box.classifyRay(r, query, fromBox)).toBe(RaySolidQueryStatus.Supported);
            expect(fromPrism.boundaries.size()).toBe(fromBox.boundaries.size());
            expect(fromPrism.insideSegments.size()).toBe(fromBox.insideSegments.size());
            for (let j = 0; j < fromBox.boundaries.size(); j++) {
                expectNear(fromPrism.boundaries.get(j).t, fromBox.boundaries.get(j).t, 1e-9);
            }
            for (let j = 0; j < fromBox.insideSegments.size(); j++) {
                expect(fromPrism.insideSegments.get(j)).toBe(fromBox.insideSegments.get(j));
            }

            const prismHit = new RayHit(RayHit.DETAIL_ALL, true);
            const boxHit = new RayHit(RayHit.DETAIL_ALL, true);
            const prismStatus = prism.queryFirstHit(r, query, prismHit);
            const boxStatus = box.queryFirstHit(r, query, boxHit);
            expect(prismStatus).toBe(boxStatus);
            if (prismStatus === RayFirstHitStatus.Hit) {
                expectNear(prismHit.getHitDistance(), boxHit.getHitDistance(), 1e-9);
                expectNear(prismHit.normal.dotProduct(boxHit.normal), 1.0, 1e-9);
                compared++;
            }
        }
        expect(compared).toBeGreaterThan(100);
    });

    it("given_concavePrism_when_rayCrossesNotch_then_twoInteriorIntervals", () => {
        const prism = new Prism(uShape(), 1.0);
        // Along x at y = 1.5, z = 0.5: inside [0,1] and [2,3]
        const result = new RaySolidClassification();
        expect(prism.classifyRay(ray(-1, 1.5, 0.5, 1, 0, 0), new RayCrossingQuery(-10, 10), result)).toBe(
            RaySolidQueryStatus.Supported,
        );
        expect(result.boundaries.size()).toBe(4);
        expectNear(result.boundaries.get(0).t, 1.0, EPS);
        expectNear(result.boundaries.get(1).t, 2.0, EPS);
        expectNear(result.boundaries.get(2).t, 3.0, EPS);
        expectNear(result.boundaries.get(3).t, 4.0, EPS);
        expect(result.insideSegments.get(0)).toBe(false);
        expect(result.insideSegments.get(1)).toBe(true);
        expect(result.insideSegments.get(2)).toBe(false);
        expect(result.insideSegments.get(3)).toBe(true);
        expect(result.insideSegments.get(4)).toBe(false);

        // The first hit from inside the notch is the inner wall of the right arm
        const hit = new RayHit(RayHit.DETAIL_ALL, true);
        expect(prism.queryFirstHit(ray(1.5, 1.5, 0.5, 1, 0, 0), new RayCrossingQuery(0, 10), hit)).toBe(
            RayFirstHitStatus.Hit,
        );
        expectNear(hit.getHitDistance(), 0.5, EPS);
        expectNear(hit.normal.x(), -1.0, EPS);
    });

    it("given_concavePrism_when_rayPassesThroughVertices_then_intervalsAreConsistent", () => {
        const prism = new Prism(uShape(), 1.0);
        const result = new RaySolidClassification();

        // Line x + y = 2: enters at the convex corner (0,2), goes through the
        // reflex corner (1,1) without leaving the base, and leaves at (2,0)
        expect(prism.classifyRay(ray(-1, 3, 0.5, 1, -1, 0), new RayCrossingQuery(-10, 10), result)).toBe(
            RaySolidQueryStatus.Supported,
        );
        expect(result.boundaries.size()).toBe(2);
        // (Ray normalizes its direction: t is a distance)
        expectNear(result.boundaries.get(0).t, 1.0 * Math.sqrt(2.0), EPS);
        expectNear(result.boundaries.get(1).t, 3.0 * Math.sqrt(2.0), EPS);

        // Line y = x: inside from the corner (0,0) to the reflex corner (1,1),
        // where it enters the notch; it touches the corner (2,2) of the right arm
        expect(prism.classifyRay(ray(-1, -1, 0.5, 1, 1, 0), new RayCrossingQuery(-10, 10), result)).toBe(
            RaySolidQueryStatus.Supported,
        );
        expect(result.boundaries.size()).toBe(2);
        expectNear(result.boundaries.get(0).t, 1.0 * Math.sqrt(2.0), EPS);
        expectNear(result.boundaries.get(1).t, 2.0 * Math.sqrt(2.0), EPS);

        // Line touching the convex corner (3,0) only: no interior
        expect(prism.classifyRay(ray(2, -1, 0.5, 1, 1, 0), new RayCrossingQuery(-10, 10), result)).toBe(
            RaySolidQueryStatus.Supported,
        );
        expect(result.boundaries.size()).toBe(0);
        expect(result.insideSegments.size()).toBe(1);
        expect(result.insideSegments.get(0)).toBe(false);
    });

    it("given_prism_when_rayRunsAlongAFace_then_isIndeterminate", () => {
        const prism = new Prism(square(1.0), 2.0);
        const result = new RaySolidClassification();
        // On the wall x = 1
        expect(prism.classifyRay(ray(1, -5, 1, 0, 1, 0), new RayCrossingQuery(0, 10), result)).toBe(
            RaySolidQueryStatus.Indeterminate,
        );
        // On the top cap plane, over the base
        expect(prism.classifyRay(ray(-5, 0, 2, 1, 0, 0), new RayCrossingQuery(0, 10), result)).toBe(
            RaySolidQueryStatus.Indeterminate,
        );
        // On the plane of the top cap, but away from the base: a miss
        expect(prism.classifyRay(ray(-5, 3, 2, 1, 0, 0), new RayCrossingQuery(0, 10), result)).toBe(
            RaySolidQueryStatus.Supported,
        );
        expect(result.boundaries.size()).toBe(0);
    });

    it("given_prism_when_verticalRays_then_capsOrNothingAreHit", () => {
        const prism = new Prism(uShape(), 1.0);
        const hit = new RayHit(RayHit.DETAIL_ALL, true);

        expect(prism.queryFirstHit(ray(0.5, 1.5, 5, 0, 0, -1), new RayCrossingQuery(0, 10), hit)).toBe(
            RayFirstHitStatus.Hit,
        );
        expectNear(hit.getHitDistance(), 4.0, EPS);
        expectNear(hit.normal.z(), 1.0, EPS);
        // Over the notch
        expect(prism.queryFirstHit(ray(1.5, 1.5, 5, 0, 0, -1), new RayCrossingQuery(0, 10), hit)).toBe(
            RayFirstHitStatus.Miss,
        );
        // From below, the bottom cap
        expect(prism.queryFirstHit(ray(2.5, 0.5, -1, 0, 0, 1), new RayCrossingQuery(0, 10), hit)).toBe(
            RayFirstHitStatus.Hit,
        );
        expectNear(hit.getHitDistance(), 1.0, EPS);
        expectNear(hit.normal.z(), -1.0, EPS);
    });

    it("given_clockwiseBase_when_constructed_then_isStoredCounterclockwiseWithOutwardWalls", () => {
        const clockwise = new ArrayList<Vector2Dd>();
        const counterclockwise = square(1.0);
        for (let i = counterclockwise.size() - 1; i >= 0; i--) {
            clockwise.add(counterclockwise.get(i));
        }
        const prism = new Prism(clockwise, 1.0);
        expect(prism.hasValidParameters()).toBe(true);

        const hit = new RayHit(RayHit.DETAIL_ALL, true);
        expect(prism.doIntersectionFirstHit(ray(5, 0, 0.5, -1, 0, 0), hit)).toBe(true);
        expectNear(hit.getHitDistance(), 4.0, EPS);
        expectNear(hit.normal.x(), 1.0, EPS);
    });

    it("given_invalidBases_when_constructed_then_haveNoValidParameters", () => {
        const bowtie = new ArrayList<Vector2Dd>();
        bowtie.add(new Vector2Dd(0, 0));
        bowtie.add(new Vector2Dd(1, 1));
        bowtie.add(new Vector2Dd(1, 0));
        bowtie.add(new Vector2Dd(0, 1));
        expect(new Prism(bowtie, 1.0).hasValidParameters()).toBe(false);

        const collinear = new ArrayList<Vector2Dd>();
        collinear.add(new Vector2Dd(0, 0));
        collinear.add(new Vector2Dd(1, 0));
        collinear.add(new Vector2Dd(2, 0));
        expect(new Prism(collinear, 1.0).hasValidParameters()).toBe(false);

        expect(new Prism(square(1.0), 0.0).hasValidParameters()).toBe(false);
        const hit = new RayHit(RayHit.DETAIL_ALL, true);
        expect(new Prism(bowtie, 1.0).queryFirstHit(ray(-5, 0.5, 0.5, 1, 0, 0), new RayCrossingQuery(0, 10), hit)).toBe(
            RayFirstHitStatus.InvalidInput,
        );
    });

    it("given_concavePrism_when_pointsClassified_then_notchAndCapsAreRespected", () => {
        const prism = new Prism(uShape(), 1.0);
        const tol = 0.01;
        expect(prism.doContainmentTest(new Vector3Dd(0.5, 1.5, 0.5), tol)).toBe(Geometry.INSIDE);
        expect(prism.doContainmentTest(new Vector3Dd(1.5, 1.5, 0.5), tol)).toBe(Geometry.OUTSIDE);
        expect(prism.doContainmentTest(new Vector3Dd(1.0, 1.5, 0.5), tol)).toBe(Geometry.LIMIT);
        expect(prism.doContainmentTest(new Vector3Dd(2.5, 0.5, 1.0), tol)).toBe(Geometry.LIMIT);
        expect(prism.doContainmentTest(new Vector3Dd(2.5, 0.5, 1.5), tol)).toBe(Geometry.OUTSIDE);
        expect(prism.classifyPoint(new Vector3Dd(2.5, 0.5, 0.5), tol)).toBe(GeometryContainment.Inside);

        const bounds = new GeometryBounds();
        prism.getBounds(bounds);
        expect(bounds.getKind()).toBe(GeometryBounds.Kind.Finite);
        expect(bounds.getMinimum().x()).toBe(0.0);
        expect(bounds.getMaximum().x()).toBe(3.0);
        expect(bounds.getMaximum().y()).toBe(2.0);
        expect(bounds.getMaximum().z()).toBe(1.0);
    });

    it("given_prismsAsCsgOperands_when_rothEvaluated_then_differenceHasTheNotch", () => {
        // A square slab minus a smaller square prism through it
        const body = new CsgBody(CsgOperation.DIFFERENCE, CsgBodyEvaluator.ROTH);
        const outer = new SimpleBody();
        outer.setGeometry(new Prism(square(2.0), 1.0));
        const inner = new SimpleBody();
        inner.setGeometry(new Prism(4, 1.0, 3.0));
        inner.setPosition(new Vector3Dd(0, 0, -1));
        expect(body.addLeaf(0, outer)).toBe(true);
        expect(body.addLeaf(0, inner)).toBe(true);

        // Along x at z = 0.5, y = 0: the inner prism is a diamond of
        // circumradius 1, so the solid is [-2, -1] U [1, 2]
        const result = new RaySolidClassification();
        expect(body.classifyRay(ray(-5, 0, 0.5, 1, 0, 0), new RayCrossingQuery(0, 20), result)).toBe(
            RaySolidQueryStatus.Supported,
        );
        expect(result.boundaries.size()).toBe(4);
        expectNear(result.boundaries.get(0).t, 3.0, EPS);
        expectNear(result.boundaries.get(1).t, 4.0, EPS);
        expectNear(result.boundaries.get(2).t, 6.0, EPS);
        expectNear(result.boundaries.get(3).t, 7.0, EPS);

        // From the hole, the first hit is the diamond wall, facing the hole
        const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        expect(body.doIntersectionFirstHit(ray(0, 0, 0.5, 1, 0, 0), hit)).toBe(true);
        expectNear(hit.getHitDistance(), 1.0, EPS);
        expect(hit.normal.x()).toBeLessThan(0.0);
    });

    it("given_starPrism_when_exportedToBrep_then_isValidSolid", () => {
        const points = 5;
        const prism = new Prism(star(points, 2.0, 0.8), 1.5);
        const solid = prism.exportToPolyhedralBoundedSolid();
        const n = 2 * points;

        const topology = PolyhedralBoundedSolidTopologySummary.from(solid);
        expect(topology.getVertexCount()).toBe(2 * n);
        expect(topology.getEdgeCount()).toBe(3 * n);
        expect(topology.getFaceCount()).toBe(n + 2);
        expect(topology.getShellCount()).toBe(1);
        const msg = new StringBuilder();
        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(solid, msg), msg.toString()).toBe(true);
    });

    it("given_concaveBase_when_triangulated_then_trianglesCoverTheBase", () => {
        const prism = new Prism(star(6, 2.0, 0.7), 1.0);
        const triangles = new ArrayList<number>();
        expect(prism.triangulateBase(triangles)).toBe(10);
        expect(triangles.size()).toBe(30);

        let polygonArea = 0;
        const base = prism.getBase();
        for (let i = 0; i < base.size(); i++) {
            const a = base.get(i);
            const b = base.get((i + 1) % base.size());
            polygonArea += 0.5 * (a.x * b.y - b.x * a.y);
        }
        let trianglesArea = 0;
        for (let i = 0; i < triangles.size(); i += 3) {
            const a = base.get(triangles.get(i));
            const b = base.get(triangles.get(i + 1));
            const c = base.get(triangles.get(i + 2));
            const area = 0.5 * ((b.x - a.x) * (c.y - a.y) - (c.x - a.x) * (b.y - a.y));
            expect(area).toBeGreaterThan(0.0);
            trianglesArea += area;
        }
        expectNear(trianglesArea, polygonArea, 1e-12);
    });

    /**
    The C++ test edits the prism through `EntityControlAccessor`; in this
    port the generic editors read and write the attributes named by the
    control specifications through their getters and setters
    (`GenericEditor.readValue` / `updateValue`).
    */
    class AccessorEditor extends GenericEditor {
        protected beginBuild(_title: string): void {}
        protected addControl(_specification: ControlSpecification, _value: string): void {}
        protected endBuild(): void {}
        protected showValidationMessage(_message: string | null): void {}
        protected setControlValue(_specification: ControlSpecification, _value: string): void {}
        protected clearControls(_message: string): void {}
    }

    it("given_prism_when_editedThroughControlAccessors_then_parametersChange", () => {
        const prism = new Prism(6, 1.0, 2.0);
        const specifications = prism.getControlSpecifications();
        expect(specifications.length).toBe(3);

        const editor = new AccessorEditor();
        editor.build(prism as Entity);
        const sides = ControlSpecification.parse(specifications[0]!);
        const radius = ControlSpecification.parse(specifications[1]!);
        const height = ControlSpecification.parse(specifications[2]!);
        expect(sides).not.toBeNull();
        expect(radius).not.toBeNull();
        expect(height).not.toBeNull();
        expect(sides!.getName()).toBe("numberOfSides");
        expect(radius!.getName()).toBe("circumradius");
        expect(height!.getName()).toBe("height");
        expect(sides!.getType()).toBe("int");

        expect(editor.readValue(sides!)).toBe("6");
        expect(editor.updateValue(sides!, "8")).toBe(true);
        expect(prism.getNumberOfSides()).toBe(8);
        expectNear(prism.getCircumradius(), 1.0, EPS);
        expect(editor.updateValue(radius!, "2.5")).toBe(true);
        expectNear(prism.getCircumradius(), 2.5, EPS);
        expect(prism.getNumberOfSides()).toBe(8);
        expect(editor.updateValue(height!, "4")).toBe(true);
        expect(prism.getHeight()).toBe(4.0);
        editor.detach();

        const copy = prism.copyGeometry();
        expect(copy).not.toBeNull();
        expect((copy as Prism).getNumberOfSides()).toBe(8);
    });
});
