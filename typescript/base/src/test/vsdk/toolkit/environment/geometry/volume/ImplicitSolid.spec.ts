// Port of java/base/src/test/vsdk/toolkit/environment/geometry/volume/ImplicitSolidTest.java
import { test } from "vitest";
import {
    expectEq,
    expectFalse,
    expectGe,
    expectLe,
    expectNear,
    expectThrow,
    expectTrue,
} from "../../../../../GTestAssertions.js";
import { BlobComponent } from "vsdk/toolkit/environment/geometry/volume/BlobComponent.js";
import { BlobSolid } from "vsdk/toolkit/environment/geometry/volume/BlobSolid.js";
import { Double } from "java/lang/Double.js";
import { GeometryBounds } from "vsdk/toolkit/environment/geometry/element/GeometryBounds.js";
import { GeometryContainment } from "vsdk/toolkit/environment/geometry/element/GeometryContainment.js";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { ImplicitSolidTestFactory } from "../../../../../ImplicitSolidTestFactory.js";
import { Math as JavaMath } from "java/lang/Math.js";
import { PolynomialSurface } from "vsdk/toolkit/environment/geometry/volume/PolynomialSurface.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayBoundary } from "vsdk/toolkit/environment/geometry/element/RayBoundary.js";
import { RayCrossingKind } from "vsdk/toolkit/environment/geometry/element/RayCrossingKind.js";
import { RayCrossingQuery } from "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.js";
import { RayCrossingSupport } from "vsdk/toolkit/environment/geometry/element/RayCrossingSupport.js";
import { RayCrossingTestCollector } from "../../../../../RayCrossingTestCollector.js";
import { RayFirstHitStatus } from "vsdk/toolkit/environment/geometry/element/RayFirstHitStatus.js";
import { RayHit } from "vsdk/toolkit/environment/geometry/element/RayHit.js";
import { RaySolidClassification } from "vsdk/toolkit/environment/geometry/element/RaySolidClassification.js";
import { RaySolidQueryStatus } from "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";

test("PolynomialSurfaceContract_TermIndicesFollowPovRayOrderAndRoundTrip", () => {
    expectEq(0, PolynomialSurface.getTermIndex(4, 4, 0, 0));
    expectEq(10, PolynomialSurface.getTermIndex(4, 1, 3, 0));
    expectEq(34, PolynomialSurface.getTermIndex(4, 0, 0, 0));
    expectEq(-1, PolynomialSurface.getTermIndex(2, 1, 1, 1));
    expectEq(-1, PolynomialSurface.getTermIndex(3, -1, 0, 0));
    for (let order = PolynomialSurface.MINIMUM_ORDER; order <= PolynomialSurface.MAXIMUM_ORDER; ++order) {
        for (let i = 0; i < PolynomialSurface.getTermCount(order); ++i) {
            let x: number[] = [0],
                y: number[] = [0],
                z: number[] = [0],
                w: number[] = [0];
            PolynomialSurface.getTermPowers(order, i, x, y, z, w);
            expectEq(order, x[0] + y[0] + z[0] + w[0]);
            expectEq(i, PolynomialSurface.getTermIndex(order, x[0], y[0], z[0]));
        }
    }
    expectThrow(IllegalArgumentException, () => new PolynomialSurface(1, false));
    expectThrow(IllegalArgumentException, () => new PolynomialSurface(8, false));
});

test("PolynomialSurfaceContract_RayPolynomialsReproduceTheFieldAlongTheRay", () => {
    let quartic: PolynomialSurface = new PolynomialSurface(4, false);
    for (let i = 0; i < 35; ++i) quartic.getCoefficients()[i] = 0.25 * ((i * 7) % 11) - 1.0;
    const origin: Vector3Dd = new Vector3Dd(0.3, -0.7, 1.1),
        direction: Vector3Dd = new Vector3Dd(0.48, 0.6, 0.64);
    let special: number[] = new Array<number>(5).fill(0),
        general: number[] = new Array<number>(5).fill(0);
    quartic.getQuarticRayPolynomial(origin, direction, special);
    quartic.getRayPolynomial(origin, direction, general, 0.0);
    for (let t = -2; t <= 2; t += 0.5) {
        const expected: number = quartic.evaluate(origin.add(direction.multiply(t)));
        expectNear(
            ImplicitSolidTestFactory.evaluateUnivariate(special, 4, t),
            expected,
            1e-9 * (1 + JavaMath.abs(expected)),
        );
        expectNear(
            ImplicitSolidTestFactory.evaluateUnivariate(general, 4, t),
            expected,
            1e-9 * (1 + JavaMath.abs(expected)),
        );
    }
});

test("PolynomialSurfaceContract_TorusCrossingsClassificationAndDetail", () => {
    for (let sturm = 0; sturm < 2; ++sturm) {
        const solid: PolynomialSurface = ImplicitSolidTestFactory.createTorus(sturm !== 0);
        const ray: Ray = Ray.fromAffine(new Vector3Dd(-5, 0, 0), new Vector3Dd(2, 0, 0));
        const query: RayCrossingQuery = new RayCrossingQuery(0, Double.POSITIVE_INFINITY);
        let crossings: RayCrossingTestCollector = new RayCrossingTestCollector();
        expectEq(RayCrossingSupport.Supported, solid.emitRayCrossings(ray, query, crossings));
        expectEq(4, crossings.roots.size());
        const expected: number[] = [1.25, 1.75, 3.25, 3.75];
        for (let i = 0; i < 4; ++i) {
            expectNear(crossings.roots.get(i), expected[i], 1e-9);
            expectEq(i % 2 === 0 ? RayCrossingKind.Entering : RayCrossingKind.Leaving, crossings.kinds.get(i));
        }
        let classification: RaySolidClassification = new RaySolidClassification();
        expectEq(RaySolidQueryStatus.Supported, solid.classifyRay(ray, query, classification));
        expectEq(4, classification.boundaries.size());
        expectEq(5, classification.insideSegments.size());
        for (let i = 0; i < 5; ++i) expectEq(i % 2 === 1, classification.insideSegments.get(i));

        let hit: RayHit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        let boundary: RayBoundary = new RayBoundary();
        expectEq(RayFirstHitStatus.Hit, solid.queryFirstHit(ray, query, hit, boundary));
        expectNear(boundary.t, 1.25, 1e-9);
        expectNear(hit.point.x(), -2.5, 1e-9);
        expectNear(hit.normal.x(), -1, 1e-9);
        expectEq(
            RayFirstHitStatus.Miss,
            solid.queryFirstHit(new Ray(new Vector3Dd(0, 0, 5), new Vector3Dd(0, 0, -1)), query, null),
        );
    }
});

test("PolynomialSurfaceContract_CertifiedContainmentAndUnboundedBounds", () => {
    const solid: PolynomialSurface = ImplicitSolidTestFactory.createTorus(false);
    expectEq(GeometryContainment.Inside, solid.classifyPointCertified(new Vector3Dd(2, 0, 0), 0.1));
    expectEq(GeometryContainment.Outside, solid.classifyPointCertified(new Vector3Dd(0, 0, 0), 0.1));
    expectEq(GeometryContainment.Boundary, solid.classifyPointCertified(new Vector3Dd(2.5, 0, 0), 0.01));
    expectEq(GeometryContainment.Inside, solid.classifyPoint(new Vector3Dd(0, 2, 0.2), 0));
    expectEq(
        GeometryContainment.Outside,
        solid.classifyBoxCertified(new Vector3Dd(0, 0, 3), new Vector3Dd(0.5, 0.5, 0.5)),
    );
    let bounds: GeometryBounds = new GeometryBounds();
    solid.getBounds(bounds);
    expectEq(GeometryBounds.Kind.Unbounded, bounds.getKind());
});

test("PolynomialSurfaceContract_SexticUsesTheGeneralExpansion", () => {
    // |p|^6 - 1: the unit sphere written as an order 6 polynomial.
    let solid: PolynomialSurface = new PolynomialSurface(6, false);
    let a: number[] = solid.getCoefficients();
    a[PolynomialSurface.getTermIndex(6, 6, 0, 0)] = 1;
    a[PolynomialSurface.getTermIndex(6, 0, 6, 0)] = 1;
    a[PolynomialSurface.getTermIndex(6, 0, 0, 6)] = 1;
    a[PolynomialSurface.getTermIndex(6, 4, 2, 0)] = 3;
    a[PolynomialSurface.getTermIndex(6, 4, 0, 2)] = 3;
    a[PolynomialSurface.getTermIndex(6, 2, 4, 0)] = 3;
    a[PolynomialSurface.getTermIndex(6, 0, 4, 2)] = 3;
    a[PolynomialSurface.getTermIndex(6, 2, 0, 4)] = 3;
    a[PolynomialSurface.getTermIndex(6, 0, 2, 4)] = 3;
    a[PolynomialSurface.getTermIndex(6, 2, 2, 2)] = 6;
    a[PolynomialSurface.getTermIndex(6, 0, 0, 0)] = -1;
    let classification: RaySolidClassification = new RaySolidClassification();
    expectEq(
        RaySolidQueryStatus.Supported,
        solid.classifyRay(
            new Ray(new Vector3Dd(-3, 0.6, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, 10),
            classification,
        ),
    );
    expectEq(2, classification.boundaries.size());
    expectNear(classification.boundaries.get(0).t, 2.2, 1e-8);
    expectNear(classification.boundaries.get(1).t, 3.8, 1e-8);
    expectEq(
        RaySolidQueryStatus.Unsupported,
        solid.classifyRay(
            new Ray(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(Double.NEGATIVE_INFINITY, 10),
            classification,
        ),
    );
});

test("BlobSolidContract_SingleComponentIsTheThresholdSphere", () => {
    // (1 - d^2/4)^2 > 0.5 inside d^2 < 4 (1 - sqrt(0.5)).
    const radius: number = JavaMath.sqrt(4.0 * (1.0 - JavaMath.sqrt(0.5)));
    for (let sturm = 0; sturm < 2; ++sturm) {
        let blob: BlobSolid = new BlobSolid(0.5, sturm !== 0);
        blob.addComponent(new BlobComponent(new Vector3Dd(0, 0, 0), 2.0, 1.0));
        const ray: Ray = Ray.fromAffine(new Vector3Dd(-5, 0, 0), new Vector3Dd(1, 0, 0));
        let classification: RaySolidClassification = new RaySolidClassification();
        expectEq(RaySolidQueryStatus.Supported, blob.classifyRay(ray, new RayCrossingQuery(0, 20), classification));
        expectEq(2, classification.boundaries.size());
        expectNear(classification.boundaries.get(0).t, 5 - radius, 1e-9);
        expectNear(classification.boundaries.get(1).t, 5 + radius, 1e-9);
        expectFalse(classification.insideSegments.get(0));
        expectTrue(classification.insideSegments.get(1));
        let hit: RayHit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        expectTrue(blob.doIntersectionFirstHit(ray, hit));
        expectNear(hit.normal.x(), -1, 1e-12);
        expectEq(GeometryContainment.Inside, blob.classifyPointCertified(new Vector3Dd(0, 0, 0), 0.1));
        expectEq(GeometryContainment.Outside, blob.classifyPointCertified(new Vector3Dd(3, 0, 0), 0.5));
        expectEq(GeometryContainment.Boundary, blob.classifyPointCertified(new Vector3Dd(0, radius, 0), 0.01));
    }
});

test("BlobSolidContract_OverlappingComponentsMergeAndBoundsFollowStrengthSigns", () => {
    let blob: BlobSolid = new BlobSolid(0.6, false);
    blob.addComponent(new BlobComponent(new Vector3Dd(-0.8, 0, 0), 1.5, 1.0));
    blob.addComponent(new BlobComponent(new Vector3Dd(0.8, 0, 0), 1.5, 1.0));
    let crossings: RayCrossingTestCollector = new RayCrossingTestCollector();
    expectEq(
        RayCrossingSupport.Supported,
        blob.emitRayCrossings(
            Ray.fromAffine(new Vector3Dd(-5, 0, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, Double.POSITIVE_INFINITY),
            crossings,
        ),
    );
    expectEq(2, crossings.roots.size());
    expectEq(RayCrossingKind.Entering, crossings.kinds.get(0));
    expectEq(RayCrossingKind.Leaving, crossings.kinds.get(1));
    expectEq(GeometryContainment.Inside, blob.classifyPoint(new Vector3Dd(0, 0, 0), 0));
    let bounds: GeometryBounds = new GeometryBounds();
    blob.getBounds(bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    expectLe(bounds.getMinimum().x(), -2.3);
    expectGe(bounds.getMaximum().x(), 2.3);
    expectLe(bounds.getMaximum().x(), 2.3 + 1e-12);
    blob.addComponent(new BlobComponent(new Vector3Dd(10, 0, 0), 1.0, -1.0));
    blob.getBounds(bounds);
    expectLe(bounds.getMaximum().x(), 2.3 + 1e-12);
    let everywhere: BlobSolid = new BlobSolid(-0.1, false);
    everywhere.addComponent(new BlobComponent(new Vector3Dd(0, 0, 0), 1.0, 1.0));
    everywhere.getBounds(bounds);
    expectEq(GeometryBounds.Kind.Unbounded, bounds.getKind());
    expectTrue(new BlobComponent(new Vector3Dd(0, 0, 0), 1e-6, 1.0).isDegenerate());
});
