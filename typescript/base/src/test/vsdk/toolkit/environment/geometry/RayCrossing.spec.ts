// Port of cpp/base/src/test/ray_crossing_tests.cpp
import { describe, expect, it } from "vitest";
import { Double } from "java/lang/Double.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { GeometryBounds } from "vsdk/toolkit/environment/geometry/element/GeometryBounds.js";
import { GeometryContainment } from "vsdk/toolkit/environment/geometry/element/GeometryContainment.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayBoundary } from "vsdk/toolkit/environment/geometry/element/RayBoundary.js";
import { RayCrossingKind } from "vsdk/toolkit/environment/geometry/element/RayCrossingKind.js";
import { RayCrossingQuery } from "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.js";
import { RayCrossingSupport } from "vsdk/toolkit/environment/geometry/element/RayCrossingSupport.js";
import { RayFirstHitStatus } from "vsdk/toolkit/environment/geometry/element/RayFirstHitStatus.js";
import { RayHit } from "vsdk/toolkit/environment/geometry/element/RayHit.js";
import { RaySolidClassification } from "vsdk/toolkit/environment/geometry/element/RaySolidClassification.js";
import { RaySolidQueryStatus } from "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.js";
import { InfinitePlane } from "vsdk/toolkit/environment/geometry/surface/InfinitePlane.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { PlanarHalfSpace } from "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { Torus } from "vsdk/toolkit/environment/geometry/volume/Torus.js";
import { RayCrossingTestCollector } from "../../../../RayCrossingTestCollector.js";

const DETAIL_POINT_NORMAL = RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL;

describe("RayCrossings", () => {
    it("SpherePreservesAffineParameterAndOrdersRoots", () => {
        const sphere = new Sphere(1);
        const ray = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(2, 0, 0));
        const hits = new RayCrossingTestCollector();
        expect(sphere.emitRayCrossings(ray, new RayCrossingQuery(0, 10), hits)).toBe(RayCrossingSupport.Supported);
        expect(hits.roots.size()).toBe(2);
        expect(hits.roots.get(0)).toBe(1);
        expect(hits.roots.get(1)).toBe(2);
        expect(hits.geometries.get(0)).toBe(sphere);
        expect(hits.kinds.get(0)).toBe(RayCrossingKind.Entering);
        expect(hits.kinds.get(1)).toBe(RayCrossingKind.Leaving);
    });

    it("InteriorTangentAndClippedIntervals", () => {
        const sphere = new Sphere(1);
        const inside = new RayCrossingTestCollector(),
            tangent = new RayCrossingTestCollector(),
            clipped = new RayCrossingTestCollector(),
            zero = new RayCrossingTestCollector();
        sphere.emitRayCrossings(
            new Ray(new Vector3Dd(0, 0, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, 10),
            inside,
        );
        expect(inside.roots.size()).toBe(1);
        expect(inside.roots.get(0)).toBe(1);
        sphere.emitRayCrossings(
            new Ray(new Vector3Dd(-2, 1, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, 10),
            tangent,
        );
        expect(tangent.roots.size()).toBe(1);
        expect(tangent.roots.get(0)).toBe(2);
        expect(tangent.kinds.get(0)).toBe(RayCrossingKind.Tangent);
        sphere.emitRayCrossings(
            new Ray(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(2.5, 3.5),
            clipped,
        );
        expect(clipped.roots.isEmpty()).toBe(true);
        const invalid = new Ray().withDirection(new Vector3Dd(0, 0, 0));
        sphere.emitRayCrossings(invalid, new RayCrossingQuery(0, 10), zero);
        expect(zero.roots.isEmpty()).toBe(true);
    });

    it("UnsupportedIsDifferentFromMiss", () => {
        const unsupported = new Torus(2, 1);
        const sphere = new Sphere(1);
        const ray = new Ray(new Vector3Dd(3, 0, 0), new Vector3Dd(1, 0, 0));
        const hits = new RayCrossingTestCollector();
        expect(unsupported.emitRayCrossings(ray, new RayCrossingQuery(0, 10), hits)).toBe(
            RayCrossingSupport.Unsupported,
        );
        expect(sphere.emitRayCrossings(ray, new RayCrossingQuery(0, 10), hits)).toBe(RayCrossingSupport.Supported);
        expect(hits.roots.isEmpty()).toBe(true);
    });
});

describe("BoxContract", () => {
    it("FirstHitCrossingsAndClippedSolidShareAffineParameters", () => {
        const box = new Box(2, 2, 2);
        const ray = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(2, 0, 0));
        const hits = new RayCrossingTestCollector();
        expect(box.emitRayCrossings(ray, new RayCrossingQuery(0, 4), hits)).toBe(RayCrossingSupport.Supported);
        expect(hits.roots.size()).toBe(2);
        expect(hits.roots.get(0)).toBe(1);
        expect(hits.roots.get(1)).toBe(2);
        expect(hits.kinds.get(0)).toBe(RayCrossingKind.Entering);
        expect(hits.kinds.get(1)).toBe(RayCrossingKind.Leaving);
        const hit = new RayHit(DETAIL_POINT_NORMAL, false);
        const boundary = new RayBoundary();
        expect(box.queryFirstHit(ray, new RayCrossingQuery(0, 4), hit, boundary)).toBe(RayFirstHitStatus.Hit);
        expect(hit.getHitDistance()).toBe(1);
        expect(hit.point.x()).toBe(-1);
        expect(hit.normal.x()).toBe(-1);
        expect(boundary.geometry).toBe(box);
        expect(hit.isDetailAvailabilityKnown()).toBe(true);
        expect(hit.getAvailableDetailMask()).toBe(DETAIL_POINT_NORMAL);
        expect(box.queryFirstHit(ray, new RayCrossingQuery(1.1, 1.9), hit)).toBe(RayFirstHitStatus.Miss);
        const solid = new RaySolidClassification();
        expect(box.classifyRay(ray, new RayCrossingQuery(1.1, 1.9), solid)).toBe(RaySolidQueryStatus.Supported);
        expect(solid.insideSegments.size()).toBe(1);
        expect(solid.insideSegments.get(0)).toBe(true);
        expect(solid.boundaries.isEmpty()).toBe(true);
        expect(box.classifyRay(ray, new RayCrossingQuery(1, 2), solid)).toBe(RaySolidQueryStatus.Supported);
        expect(solid.boundaries.size()).toBe(2);
        expect(solid.insideSegments.size()).toBe(3);
        expect(solid.insideSegments.get(0)).toBe(false);
        expect(solid.insideSegments.get(1)).toBe(true);
        expect(solid.insideSegments.get(2)).toBe(false);
    });

    it("TangencyOverlapAndNumericalFailuresAreExplicit", () => {
        const box = new Box(2, 2, 2);
        const tangent = new RayCrossingTestCollector();
        const corner = Ray.fromAffine(new Vector3Dd(-2, 0, 0), new Vector3Dd(1, 1, 0));
        expect(box.emitRayCrossings(corner, new RayCrossingQuery(0, 4), tangent)).toBe(RayCrossingSupport.Supported);
        expect(tangent.roots.size()).toBe(1);
        expect(tangent.roots.get(0)).toBe(1);
        expect(tangent.kinds.get(0)).toBe(RayCrossingKind.Tangent);
        const solid = new RaySolidClassification();
        expect(box.classifyRay(corner, new RayCrossingQuery(0, 4), solid)).toBe(RaySolidQueryStatus.Supported);
        expect(solid.insideSegments.size()).toBe(1);
        expect(solid.insideSegments.get(0)).toBe(false);
        const alongFace = Ray.fromAffine(new Vector3Dd(-2, 1, 0), new Vector3Dd(1, 0, 0));
        const overlap = new RayCrossingTestCollector();
        expect(box.emitRayCrossings(alongFace, new RayCrossingQuery(0, 4), overlap)).toBe(
            RayCrossingSupport.Indeterminate,
        );
        expect(box.classifyRay(alongFace, new RayCrossingQuery(0, 4), solid)).toBe(RaySolidQueryStatus.Indeterminate);
        expect(solid.insideSegments.isEmpty()).toBe(true);
        expect(
            box.queryFirstHit(Ray.fromAffine(new Vector3Dd(), new Vector3Dd()), new RayCrossingQuery(0, 4), null),
        ).toBe(RayFirstHitStatus.InvalidInput);
        expect(box.queryFirstHit(corner, new RayCrossingQuery(0, 4, 0.001), null)).toBe(RayFirstHitStatus.Unsupported);
        expect(
            box.queryFirstHit(
                Ray.fromAffine(new Vector3Dd(-1e300, 0, 0), new Vector3Dd(1, 0, 0)),
                new RayCrossingQuery(0, Double.POSITIVE_INFINITY),
                null,
            ),
        ).toBe(RayFirstHitStatus.Indeterminate);
        const parallelMiss = new RayCrossingTestCollector();
        expect(
            box.emitRayCrossings(
                Ray.fromAffine(new Vector3Dd(0, 2, 0), new Vector3Dd(1, 0, 0)),
                new RayCrossingQuery(0, 4),
                parallelMiss,
            ),
        ).toBe(RayCrossingSupport.Supported);
        expect(parallelMiss.roots.isEmpty()).toBe(true);
        const invalid = new Box(2, -2, 2);
        expect(invalid.queryFirstHit(corner, new RayCrossingQuery(0, 4), null)).toBe(RayFirstHitStatus.InvalidInput);
    });

    it("EuclideanContainmentBoundsAndIndependentCopy", () => {
        const box = new Box(2, 4, 6);
        expect(box.classifyPoint(new Vector3Dd(0, 0, 0), 0)).toBe(GeometryContainment.Inside);
        expect(box.classifyPoint(new Vector3Dd(1, 0, 0), 0)).toBe(GeometryContainment.Boundary);
        expect(box.classifyPoint(new Vector3Dd(1.05, 2.05, 0), 0.1)).toBe(GeometryContainment.Boundary);
        expect(box.classifyPoint(new Vector3Dd(1.08, 2.08, 0), 0.1)).toBe(GeometryContainment.Outside);
        expect(box.classifyPoint(new Vector3Dd(), -1)).toBe(GeometryContainment.InvalidInput);
        const bounds = new GeometryBounds();
        box.getBounds(bounds);
        expect(bounds.getKind()).toBe(GeometryBounds.Kind.Finite);
        expect(bounds.getMinimum().x()).toBe(-1);
        expect(bounds.getMaximum().z()).toBe(3);
        const copy = box.copyGeometry();
        expect(copy).not.toBeNull();
        box.setSize(4, 4, 4);
        expect(copy!.classifyPoint(new Vector3Dd(1.5, 0, 0), 0)).toBe(GeometryContainment.Outside);
    });

    it("ArbitraryBoundsAndParallelPolicy", () => {
        const minimum = new Vector3Dd(-2, -1, -3),
            maximum = new Vector3Dd(4, 3, 1);
        const entry = [Double.NEGATIVE_INFINITY],
            exit = [Double.POSITIVE_INFINITY];
        const overlap = [false];
        expect(
            Box.intersectBounds(
                new Vector3Dd(-4, 0, 0),
                new Vector3Dd(1, 0, 0),
                minimum,
                maximum,
                0,
                true,
                entry,
                exit,
                overlap,
            ),
        ).toBe(RayFirstHitStatus.Hit);
        expect(entry[0]).toBe(2);
        expect(exit[0]).toBe(8);
        expect(overlap[0]).toBe(false);
        entry[0] = 0;
        exit[0] = Double.POSITIVE_INFINITY;
        expect(
            Box.intersectBounds(
                new Vector3Dd(5, 0, 0),
                new Vector3Dd(-1e-8, 1, 0),
                minimum,
                maximum,
                1e-7,
                false,
                entry,
                exit,
                overlap,
            ),
        ).toBe(RayFirstHitStatus.Miss);
        entry[0] = Double.NEGATIVE_INFINITY;
        exit[0] = Double.POSITIVE_INFINITY;
        expect(
            Box.intersectBounds(
                new Vector3Dd(-4, 3, 0),
                new Vector3Dd(1, 0, 0),
                minimum,
                maximum,
                0,
                true,
                entry,
                exit,
                overlap,
            ),
        ).toBe(RayFirstHitStatus.Hit);
        expect(overlap[0]).toBe(true);
    });
});

describe("InfinitePlaneContract", () => {
    it("SurfaceQueryPreservesAffineParameterAndDetail", () => {
        const plane = new InfinitePlane(2, 0, 0, -2);
        const ray = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(2, 0, 0));
        const crossings = new RayCrossingTestCollector();
        expect(plane.emitRayCrossings(ray, new RayCrossingQuery(0, 4), crossings)).toBe(RayCrossingSupport.Supported);
        expect(crossings.roots.size()).toBe(1);
        expect(crossings.roots.get(0)).toBe(2);
        expect(crossings.kinds.get(0)).toBe(RayCrossingKind.Unclassified);
        expect(crossings.geometries.get(0)).toBe(plane);
        const hit = new RayHit(DETAIL_POINT_NORMAL, false);
        const boundary = new RayBoundary();
        expect(plane.queryFirstHit(ray, new RayCrossingQuery(2, 2), hit, boundary)).toBe(RayFirstHitStatus.Hit);
        expect(hit.getHitDistance()).toBe(2);
        expect(hit.point.x()).toBe(1);
        expect(hit.normal.x()).toBe(1);
        expect(boundary.geometry).toBe(plane);
        expect(hit.getAvailableDetailMask()).toBe(DETAIL_POINT_NORMAL);
        expect(plane.queryFirstHit(ray, new RayCrossingQuery(0, 1), hit)).toBe(RayFirstHitStatus.Miss);
        expect(hit.hasHitDistance()).toBe(false);
        const solid = new RaySolidClassification();
        expect(plane.classifyRay(ray, new RayCrossingQuery(0, 4), solid)).toBe(RaySolidQueryStatus.Unsupported);
        expect(plane.classifyPoint(new Vector3Dd(), 0)).toBe(GeometryContainment.Unsupported);
    });

    it("ParallelCoincidentDegenerateAndBoundsAreExplicit", () => {
        const plane = new InfinitePlane(1, 0, 0, 0);
        const events = new RayCrossingTestCollector();
        const parallel = Ray.fromAffine(new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0));
        const coincident = Ray.fromAffine(new Vector3Dd(), new Vector3Dd(0, 1, 0));
        expect(plane.emitRayCrossings(parallel, new RayCrossingQuery(0, 4), events)).toBe(RayCrossingSupport.Supported);
        expect(events.roots.isEmpty()).toBe(true);
        expect(plane.emitRayCrossings(coincident, new RayCrossingQuery(0, 4), events)).toBe(
            RayCrossingSupport.Indeterminate,
        );
        const invalid = new InfinitePlane(0, 0, 0, 1);
        expect(invalid.queryFirstHit(parallel, new RayCrossingQuery(0, 4), null)).toBe(RayFirstHitStatus.InvalidInput);
        const tiny = new InfinitePlane(1e-200, 0, 0, 0);
        expect(tiny.queryFirstHit(parallel, new RayCrossingQuery(0, 4), null)).toBe(RayFirstHitStatus.Indeterminate);
        const shallow = new InfinitePlane(1e-100, 0, 0, 1);
        expect(
            shallow.queryFirstHit(
                Ray.fromAffine(new Vector3Dd(), new Vector3Dd(1e-250, 1, 0)),
                new RayCrossingQuery(0, 4),
                null,
            ),
        ).toBe(RayFirstHitStatus.Indeterminate);
        const bounds = new GeometryBounds();
        plane.getBounds(bounds);
        expect(bounds.getKind()).toBe(GeometryBounds.Kind.Unbounded);
        invalid.getBounds(bounds);
        expect(bounds.getKind()).toBe(GeometryBounds.Kind.Indeterminate);
        const copy = plane.copyGeometry();
        expect(copy).not.toBeNull();
        plane.setD(-5);
        expect(
            copy!.queryFirstHit(
                Ray.fromAffine(new Vector3Dd(-2, 0, 0), new Vector3Dd(1, 0, 0)),
                new RayCrossingQuery(2, 2),
                null,
            ),
        ).toBe(RayFirstHitStatus.Hit);
        const depth = [0];
        expect(InfinitePlane.intersectionDepth(0.001, 0.0005, 0.001, 0, 10, false, depth)).toBe(RayFirstHitStatus.Miss);
        expect(InfinitePlane.intersectionDepth(0.001, 0.001, 0.001, 0, 10, false, depth)).toBe(RayFirstHitStatus.Hit);
        expect(depth[0]).toBe(1);
    });
});

describe("PlanarHalfSpaceContract", () => {
    it("ContainmentBoundsAndIndependentCopy", () => {
        const half = new PlanarHalfSpace(2, 0, 0, -2);
        expect(half.classifyPoint(new Vector3Dd(0, 0, 0), 0)).toBe(GeometryContainment.Inside);
        expect(half.classifyPoint(new Vector3Dd(1, 0, 0), 0)).toBe(GeometryContainment.Boundary);
        expect(half.classifyPoint(new Vector3Dd(1.05, 0, 0), 0.1)).toBe(GeometryContainment.Boundary);
        expect(half.classifyPoint(new Vector3Dd(1.2, 0, 0), 0.1)).toBe(GeometryContainment.Outside);
        expect(half.classifyPoint(new Vector3Dd(), -1)).toBe(GeometryContainment.InvalidInput);
        const invalid = new PlanarHalfSpace(0, 0, 0, 1);
        expect(invalid.classifyPoint(new Vector3Dd(), 0)).toBe(GeometryContainment.InvalidInput);
        const tiny = new PlanarHalfSpace(1e-200, 0, 0, 0);
        expect(tiny.classifyPoint(new Vector3Dd(), 0)).toBe(GeometryContainment.Indeterminate);
        const shallow = new PlanarHalfSpace(1e-100, 0, 0, 0);
        expect(shallow.classifyPoint(new Vector3Dd(1e-250, 0, 0), 0)).toBe(GeometryContainment.Indeterminate);
        const bounds = new GeometryBounds();
        half.getBounds(bounds);
        expect(bounds.getKind()).toBe(GeometryBounds.Kind.Unbounded);
        const copy = half.copyGeometry();
        expect(copy).not.toBeNull();
        expect(copy!.classifyPoint(new Vector3Dd(), 0)).toBe(GeometryContainment.Inside);
    });

    it("OrderedCrossingAndEndpointStates", () => {
        const half = new PlanarHalfSpace(2, 0, 0, -2);
        const leaving = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(2, 0, 0));
        const crossings = new RayCrossingTestCollector();
        expect(half.emitRayCrossings(leaving, new RayCrossingQuery(0, 4), crossings)).toBe(
            RayCrossingSupport.Supported,
        );
        expect(crossings.roots.size()).toBe(1);
        expect(crossings.roots.get(0)).toBe(2);
        expect(crossings.kinds.get(0)).toBe(RayCrossingKind.Leaving);
        expect(crossings.geometries.get(0)).toBe(half);
        const hit = new RayHit(DETAIL_POINT_NORMAL, false);
        const boundary = new RayBoundary();
        expect(half.queryFirstHit(leaving, new RayCrossingQuery(2, 2), hit, boundary)).toBe(RayFirstHitStatus.Hit);
        expect(boundary.geometry).toBe(half);
        expect(hit.point.x()).toBe(1);
        expect(hit.normal.x()).toBe(1);
        const solid = new RaySolidClassification();
        expect(half.classifyRay(leaving, new RayCrossingQuery(2, 4), solid)).toBe(RaySolidQueryStatus.Supported);
        expect(solid.boundaries.size()).toBe(1);
        expect(solid.insideSegments.size()).toBe(2);
        expect(solid.insideSegments.get(0)).toBe(true);
        expect(solid.insideSegments.get(1)).toBe(false);
        const entering = Ray.fromAffine(new Vector3Dd(3, 0, 0), new Vector3Dd(-2, 0, 0));
        const opposite = new RayCrossingTestCollector();
        expect(half.emitRayCrossings(entering, new RayCrossingQuery(0, 4), opposite)).toBe(
            RayCrossingSupport.Supported,
        );
        expect(opposite.roots.size()).toBe(1);
        expect(opposite.roots.get(0)).toBe(1);
        expect(opposite.kinds.get(0)).toBe(RayCrossingKind.Entering);
        expect(half.classifyRay(entering, new RayCrossingQuery(1, 4), solid)).toBe(RaySolidQueryStatus.Supported);
        expect(solid.insideSegments.size()).toBe(2);
        expect(solid.insideSegments.get(0)).toBe(false);
        expect(solid.insideSegments.get(1)).toBe(true);
    });

    it("ParallelAndClippedIntervalsRemainDistinct", () => {
        const half = new PlanarHalfSpace(1, 0, 0, 0);
        const solid = new RaySolidClassification();
        expect(
            half.classifyRay(
                Ray.fromAffine(new Vector3Dd(-1, 0, 0), new Vector3Dd(0, 1, 0)),
                new RayCrossingQuery(0, 4),
                solid,
            ),
        ).toBe(RaySolidQueryStatus.Supported);
        expect(solid.insideSegments.size()).toBe(1);
        expect(solid.insideSegments.get(0)).toBe(true);
        expect(
            half.classifyRay(
                Ray.fromAffine(new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0)),
                new RayCrossingQuery(0, 4),
                solid,
            ),
        ).toBe(RaySolidQueryStatus.Supported);
        expect(solid.insideSegments.size()).toBe(1);
        expect(solid.insideSegments.get(0)).toBe(false);
        expect(
            half.classifyRay(
                Ray.fromAffine(new Vector3Dd(), new Vector3Dd(0, 1, 0)),
                new RayCrossingQuery(0, 4),
                solid,
            ),
        ).toBe(RaySolidQueryStatus.Indeterminate);
        expect(solid.insideSegments.isEmpty()).toBe(true);
        expect(
            half.classifyRay(
                Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0)),
                new RayCrossingQuery(0, 2),
                solid,
            ),
        ).toBe(RaySolidQueryStatus.Supported);
        expect(solid.insideSegments.size()).toBe(1);
        expect(solid.insideSegments.get(0)).toBe(true);
        expect(solid.boundaries.isEmpty()).toBe(true);
    });
});

describe("RaySolidClassification", () => {
    it("NonunitRayAndClippedInterior", () => {
        const sphere = new Sphere(1);
        const ray = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(2, 0, 0));
        const result = new RaySolidClassification();
        expect(sphere.classifyRay(ray, new RayCrossingQuery(0, 4), result)).toBe(RaySolidQueryStatus.Supported);
        expect(result.boundaries.size()).toBe(2);
        expect(result.boundaries.get(0).t).toBe(1);
        expect(result.boundaries.get(1).t).toBe(2);
        expect(result.boundaries.get(0).geometry).toBe(sphere);
        expect(result.boundaries.get(0).normalReversed).toBe(false);
        expect(result.insideSegments.size()).toBe(3);
        expect(result.insideSegments.get(0)).toBe(false);
        expect(result.insideSegments.get(1)).toBe(true);
        expect(result.insideSegments.get(2)).toBe(false);
        expect(sphere.classifyRay(ray, new RayCrossingQuery(1.25, 1.75), result)).toBe(RaySolidQueryStatus.Supported);
        expect(result.boundaries.isEmpty()).toBe(true);
        expect(result.insideSegments.size()).toBe(1);
        expect(result.insideSegments.get(0)).toBe(true);
    });

    it("InteriorStartAndEndpointLimitingStates", () => {
        const sphere = new Sphere(1);
        let ray = Ray.fromAffine(new Vector3Dd(0, 0, 0), new Vector3Dd(2, 0, 0));
        const result = new RaySolidClassification();
        sphere.classifyRay(ray, new RayCrossingQuery(0, 2), result);
        expect(result.boundaries.size()).toBe(1);
        expect(result.boundaries.get(0).t).toBe(0.5);
        expect(result.insideSegments.size()).toBe(2);
        expect(result.insideSegments.get(0)).toBe(true);
        expect(result.insideSegments.get(1)).toBe(false);
        ray = ray.withOrigin(new Vector3Dd(-3, 0, 0));
        sphere.classifyRay(ray, new RayCrossingQuery(1, 2), result);
        expect(result.boundaries.size()).toBe(2);
        expect(result.insideSegments.size()).toBe(3);
        expect(result.insideSegments.get(0)).toBe(false);
        expect(result.insideSegments.get(1)).toBe(true);
        expect(result.insideSegments.get(2)).toBe(false);
        sphere.classifyRay(ray, new RayCrossingQuery(2, 3), result);
        expect(result.boundaries.size()).toBe(1);
        expect(result.boundaries.get(0).t).toBe(2);
        expect(result.insideSegments.size()).toBe(2);
        expect(result.insideSegments.get(0)).toBe(true);
        expect(result.insideSegments.get(1)).toBe(false);
    });

    it("TangencyIsASurfaceEventButNotAnInterior", () => {
        const sphere = new Sphere(1);
        const ray = new Ray(new Vector3Dd(-2, 1, 0), new Vector3Dd(1, 0, 0));
        const crossings = new RayCrossingTestCollector();
        sphere.emitRayCrossings(ray, new RayCrossingQuery(0, 4), crossings);
        expect(crossings.roots.size()).toBe(1);
        expect(crossings.kinds.get(0)).toBe(RayCrossingKind.Tangent);
        const result = new RaySolidClassification();
        expect(sphere.classifyRay(ray, new RayCrossingQuery(0, 4), result)).toBe(RaySolidQueryStatus.Supported);
        expect(result.boundaries.isEmpty()).toBe(true);
        expect(result.insideSegments.size()).toBe(1);
        expect(result.insideSegments.get(0)).toBe(false);
    });

    it("MissAndUnboundedInterval", () => {
        const sphere = new Sphere(1);
        const result = new RaySolidClassification();
        let ray = new Ray(new Vector3Dd(-3, 2, 0), new Vector3Dd(1, 0, 0));
        sphere.classifyRay(ray, new RayCrossingQuery(0, 4), result);
        expect(result.boundaries.isEmpty()).toBe(true);
        expect(result.insideSegments.size()).toBe(1);
        expect(result.insideSegments.get(0)).toBe(false);
        ray = ray.withOrigin(new Vector3Dd(-3, 0, 0));
        const inf = Double.POSITIVE_INFINITY;
        sphere.classifyRay(ray, new RayCrossingQuery(-inf, inf), result);
        expect(result.boundaries.size()).toBe(2);
        expect(result.boundaries.get(0).t).toBe(2);
        expect(result.boundaries.get(1).t).toBe(4);
        expect(result.insideSegments.size()).toBe(3);
        expect(result.insideSegments.get(0)).toBe(false);
        expect(result.insideSegments.get(1)).toBe(true);
        expect(result.insideSegments.get(2)).toBe(false);
    });

    it("InvalidUnsupportedAndIndeterminateClearReusedResult", () => {
        const sphere = new Sphere(1);
        const unsupported = new Torus(2, 1);
        let ray = new Ray(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0));
        const result = new RaySolidClassification();
        sphere.classifyRay(ray, new RayCrossingQuery(0, 4), result);
        expect(unsupported.classifyRay(ray, new RayCrossingQuery(0, 4), result)).toBe(RaySolidQueryStatus.Unsupported);
        expect(result.boundaries.isEmpty()).toBe(true);
        expect(result.insideSegments.isEmpty()).toBe(true);
        const invalidQueries = [
            new RayCrossingQuery(4, 0),
            new RayCrossingQuery(1, 1),
            new RayCrossingQuery(0, 4, 0.001),
            new RayCrossingQuery(0, 4, 0, 0.001),
            new RayCrossingQuery(Double.NaN, 4),
        ];
        for (const query of invalidQueries) {
            sphere.classifyRay(ray, new RayCrossingQuery(0, 4), result);
            expect(sphere.classifyRay(ray, query, result)).toBe(RaySolidQueryStatus.InvalidInput);
            expect(result.boundaries.isEmpty()).toBe(true);
            expect(result.insideSegments.isEmpty()).toBe(true);
        }
        ray = ray.withDirection(new Vector3Dd(0, 0, 0));
        expect(sphere.classifyRay(ray, new RayCrossingQuery(0, 4), result)).toBe(RaySolidQueryStatus.InvalidInput);
        ray = ray.withDirection(new Vector3Dd(1e300, 0, 0));
        expect(sphere.classifyRay(ray, new RayCrossingQuery(0, 4), result)).toBe(RaySolidQueryStatus.Indeterminate);
        expect(result.insideSegments.isEmpty()).toBe(true);
    });

    it("UnrepresentableRadiusIsNotReportedAsEmpty", () => {
        const tiny = new Sphere(1e-300);
        const ray = new Ray(new Vector3Dd(0, 0, 0), new Vector3Dd(1, 0, 0));
        const result = new RaySolidClassification();
        expect(tiny.classifyRay(ray, new RayCrossingQuery(-1e-301, 1e-301), result)).toBe(
            RaySolidQueryStatus.Indeterminate,
        );
        expect(result.boundaries.isEmpty()).toBe(true);
        expect(result.insideSegments.isEmpty()).toBe(true);
    });

    // The C++ storage-pointer checks (data() identity after clear) have no
    // TypeScript counterpart; the list identity is checked instead.
    it("NativeListsRetainCapacityAndCopiesAreIndependent", () => {
        const sphere = new Sphere(1);
        const ray = new Ray(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0));
        const result = new RaySolidClassification();
        sphere.classifyRay(ray, new RayCrossingQuery(0, 5), result);
        expect(result.boundaries.size()).toBe(2);
        const boundaryStorage = result.boundaries;
        const segmentStorage = result.insideSegments;
        const copied = new RaySolidClassification(result);
        const assigned = new RaySolidClassification();
        assigned.assign(result);
        sphere.classifyRay(ray, new RayCrossingQuery(2.5, 3.5), result);
        expect(result.boundaries.isEmpty()).toBe(true);
        expect(result.boundaries).toBe(boundaryStorage);
        expect(result.insideSegments).toBe(segmentStorage);
        expect(copied.boundaries.size()).toBe(2);
        expect(assigned.boundaries.size()).toBe(2);
        copied.boundaries.get(0).t = 99;
        expect(assigned.boundaries.get(0).t).toBe(2);
        expect(copied.insideSegments.size()).toBe(3);
        expect(result.insideSegments.size()).toBe(1);
        expect(result.insideSegments.get(0)).toBe(true);
    });

    it("JavaDoubleClassifiesSpecialValues", () => {
        expect(Double.isNaN(Double.NaN)).toBe(true);
        expect(Double.isFinite(Double.NaN)).toBe(false);
        expect(Double.isFinite(Double.POSITIVE_INFINITY)).toBe(false);
        expect(Double.isFinite(Double.NEGATIVE_INFINITY)).toBe(false);
        expect(Double.isFinite(Double.MAX_VALUE)).toBe(true);
        expect(Double.isFinite(Double.MIN_VALUE)).toBe(true);
        expect(Double.isNaN(0)).toBe(false);
    });
});

describe("GeometryBoundsContract", () => {
    it("ExplicitStatesAndSphereBounds", () => {
        const bounds = new GeometryBounds();
        expect(bounds.getKind()).toBe(GeometryBounds.Kind.Unsupported);
        const sphere = new Sphere(2);
        sphere.getBounds(bounds);
        expect(bounds.getKind()).toBe(GeometryBounds.Kind.Finite);
        expect(bounds.getMinimum().x()).toBe(-2);
        expect(bounds.getMaximum().z()).toBe(2);
        const box = new Box(1, 1, 1);
        box.getBounds(bounds);
        expect(bounds.getKind()).toBe(GeometryBounds.Kind.Finite);
        bounds.setEmpty();
        expect(bounds.getKind()).toBe(GeometryBounds.Kind.Empty);
        bounds.setUnbounded();
        expect(bounds.getKind()).toBe(GeometryBounds.Kind.Unbounded);
        expect(bounds.setFinite(new Vector3Dd(2, 0, 0), new Vector3Dd(1, 1, 1))).toBe(false);
        expect(bounds.getKind()).toBe(GeometryBounds.Kind.Indeterminate);
        expect(bounds.setFinite(new Vector3Dd(), new Vector3Dd(Double.POSITIVE_INFINITY, 1, 1))).toBe(false);
        expect(bounds.getKind()).toBe(GeometryBounds.Kind.Indeterminate);
    });
});
