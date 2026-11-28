// Translation of java/base/src/test/vsdk/toolkit/environment/geometry/surface/SubdividedBezierPatchTest.java
// (port of cpp/base/src/test/subdivided_bezier_patch_tests.cpp); the ReaderMitPrimitives_* tests are in
// fs/src/test/vsdk/toolkit/io/geometry/SubdividedBezierPatchReaders.spec.ts, as ReaderMitScene is in fs.
import { test } from "vitest";
import { expectDoubleEq, expectEq, expectGt, expectNear, expectThrow } from "../../../../../GTestAssertions.js";
import { BezierPatchSubdivision } from "vsdk/toolkit/environment/geometry/surface/BezierPatchSubdivision.js";
import { Double } from "java/lang/Double.js";
import { Geometry } from "vsdk/toolkit/environment/geometry/Geometry.js";
import { GeometryBounds } from "vsdk/toolkit/environment/geometry/element/GeometryBounds.js";
import { GeometryContainment } from "vsdk/toolkit/environment/geometry/element/GeometryContainment.js";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { Math as JavaMath } from "java/lang/Math.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayCrossingQuery } from "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.js";
import { RayCrossingSupport } from "vsdk/toolkit/environment/geometry/element/RayCrossingSupport.js";
import { RayCrossingTestCollector } from "../../../../../RayCrossingTestCollector.js";
import { RayFirstHitStatus } from "vsdk/toolkit/environment/geometry/element/RayFirstHitStatus.js";
import { RayHit } from "vsdk/toolkit/environment/geometry/element/RayHit.js";
import { RaySolidClassification } from "vsdk/toolkit/environment/geometry/element/RaySolidClassification.js";
import { RaySolidQueryStatus } from "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.js";
import { SubdividedBezierPatch } from "vsdk/toolkit/environment/geometry/surface/SubdividedBezierPatch.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";

// Control net over x = i, y = j, so the patch's x and y are 3u and 3v.
function curvedNet(net: Vector3Dd[][]): void {
    for (let i = 0; i < 4; ++i) {
        for (let j = 0; j < 4; ++j) net[i][j] = new Vector3Dd(i, j, 0.6 * JavaMath.sin(1.3 * i + 0.8 * j));
    }
}

test("SubdividedBezierPatchContract_EverySubdivisionApproachesTheBezierSurface", () => {
    let net: Vector3Dd[][] = Array.from({ length: 4 }, () => Array.from({ length: 4 }, () => new Vector3Dd()));
    curvedNet(net);
    const kinds: BezierPatchSubdivision[] = [
        BezierPatchSubdivision.UniformGrid,
        BezierPatchSubdivision.PrecomputedGrid,
        BezierPatchSubdivision.Adaptive,
        BezierPatchSubdivision.AdaptiveTree,
        BezierPatchSubdivision.SmoothGrid,
    ];
    const steps: number[] = [16, 16, 4, 4, 16];
    for (let k = 0; k < 5; ++k) {
        const patch: SubdividedBezierPatch = new SubdividedBezierPatch(kinds[k], steps[k], steps[k], 0.001, net);
        for (let s = 0; s < 25; ++s) {
            const x: number = 0.3 + 0.1 * s,
                y: number = 2.7 - 0.09 * s;
            const ray: Ray = Ray.fromAffine(new Vector3Dd(x, y, 5), new Vector3Dd(0, 0, -1));
            let hit: RayHit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
            expectEq(
                RayFirstHitStatus.Hit,
                patch.queryFirstHit(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), hit),
                k + " " + s,
            );
            const exact: number = patch.evaluate(x / 3, y / 3).z();
            expectNear(hit.point.z(), exact, 0.02, k + " " + s);
            // Normals face the u x v side, here +z; the PovRay smooth
            // interpolation can tilt them far from the surface normal.
            expectGt(hit.normal.z(), kinds[k] === BezierPatchSubdivision.SmoothGrid ? 0.0 : 0.5, k + " " + s);
            expectNear(hit.normal.length(), 1, 1e-9);
        }
    }
});

test("SubdividedBezierPatchContract_OpenSurfaceCrossingsBoundsAndBudget", () => {
    let net: Vector3Dd[][] = Array.from({ length: 4 }, () => Array.from({ length: 4 }, () => new Vector3Dd()));
    curvedNet(net);
    const patch: SubdividedBezierPatch = new SubdividedBezierPatch(
        BezierPatchSubdivision.PrecomputedGrid,
        8,
        8,
        0.1,
        net,
    );
    const ray: Ray = Ray.fromAffine(new Vector3Dd(1.4, 1.1, 5), new Vector3Dd(0, 0, -1));
    let crossings: RayCrossingTestCollector = new RayCrossingTestCollector();
    expectEq(
        RayCrossingSupport.Supported,
        patch.emitRayCrossings(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), crossings),
    );
    expectEq(1, crossings.roots.size());
    let classification: RaySolidClassification = new RaySolidClassification();
    expectEq(
        RaySolidQueryStatus.Unsupported,
        patch.classifyRay(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), classification),
    );
    expectEq(GeometryContainment.Unsupported, patch.classifyPoint(new Vector3Dd(1, 1, 0), 0));
    let hit: RayHit = new RayHit(RayHit.DETAIL_POINT, false);
    expectEq(
        RayFirstHitStatus.Unsupported,
        patch.queryFirstHit(ray, new RayCrossingQuery(-1, Double.POSITIVE_INFINITY), hit),
    );
    expectEq(RayFirstHitStatus.Miss, patch.queryFirstHit(ray, new RayCrossingQuery(0, 1), hit));

    let bounds: GeometryBounds = new GeometryBounds();
    patch.getBounds(bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    expectDoubleEq(bounds.getMaximum().x(), 3);
    expectDoubleEq(bounds.getMinimum().y(), 0);

    // A spent budget stops the search.
    let depths: number[] = new Array<number>(SubdividedBezierPatch.MAXIMUM_HITS).fill(0);
    let points: Vector3Dd[] = Array.from({ length: SubdividedBezierPatch.MAXIMUM_HITS }, () => new Vector3Dd());
    let normals: Vector3Dd[] = Array.from({ length: SubdividedBezierPatch.MAXIMUM_HITS }, () => new Vector3Dd());
    for (let i = 0; i < SubdividedBezierPatch.MAXIMUM_HITS; i++) {
        points[i] = new Vector3Dd();
        normals[i] = new Vector3Dd();
    }
    expectEq(
        1,
        patch.intersect(new Vector3Dd(1.4, 1.1, 5), new Vector3Dd(0, 0, -1), 0.001, 0, 32, depths, points, normals),
    );
    expectEq(
        0,
        patch.intersect(new Vector3Dd(1.4, 1.1, 5), new Vector3Dd(0, 0, -1), 0.001, 32, 32, depths, points, normals),
    );

    let copy: Geometry = patch.copyGeometry();
    let copied: RayHit = new RayHit(RayHit.DETAIL_POINT, false);
    let original: RayHit = new RayHit(RayHit.DETAIL_POINT, false);
    expectEq(RayFirstHitStatus.Hit, copy.queryFirstHit(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), copied));
    expectEq(
        RayFirstHitStatus.Hit,
        patch.queryFirstHit(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), original),
    );
    expectEq(original.getHitDistance(), copied.getHitDistance());

    expectThrow(
        IllegalArgumentException,
        () => new SubdividedBezierPatch(BezierPatchSubdivision.UniformGrid, 0, 4, 0.1, net),
    );
    net[2][1] = new Vector3Dd(Double.NaN, 0, 0);
    expectThrow(
        IllegalArgumentException,
        () => new SubdividedBezierPatch(BezierPatchSubdivision.Adaptive, 2, 2, 0.1, net),
    );
});
