// Port of java/base/src/test/vsdk/toolkit/environment/geometry/volume/HeightFieldSolidTest.java (reader tests: fs HeightFieldSolidReaders.spec.ts)
import { test } from "vitest";
import {
    expectDoubleEq,
    expectEq,
    expectFalse,
    expectGt,
    expectNear,
    expectTrue,
} from "../../../../../GTestAssertions.js";
import { Double } from "java/lang/Double.js";
import { GeometryBounds } from "vsdk/toolkit/environment/geometry/element/GeometryBounds.js";
import { GeometryContainment } from "vsdk/toolkit/environment/geometry/element/GeometryContainment.js";
import { HeightFieldSolid } from "vsdk/toolkit/environment/geometry/volume/HeightFieldSolid.js";
import { HeightFieldTestFactory } from "../../../../../HeightFieldTestFactory.js";
import { HeightFieldTraceResult } from "vsdk/toolkit/environment/geometry/volume/HeightFieldTraceResult.js";
import { Math as JavaMath } from "java/lang/Math.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayBoundary } from "vsdk/toolkit/environment/geometry/element/RayBoundary.js";
import { RayCrossingQuery } from "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.js";
import { RayFirstHitStatus } from "vsdk/toolkit/environment/geometry/element/RayFirstHitStatus.js";
import { RayHit } from "vsdk/toolkit/environment/geometry/element/RayHit.js";
import { RaySolidClassification } from "vsdk/toolkit/environment/geometry/element/RaySolidClassification.js";
import { RaySolidQueryStatus } from "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";

test("HeightFieldSolidContract_PlaneFieldHasExactCrossingsNormalAndContainment", () => {
    const solid: HeightFieldSolid = HeightFieldTestFactory.createPlane();
    expectTrue(solid.isValid());
    const ray: Ray = Ray.fromAffine(new Vector3Dd(1.3, 10, 2.6), new Vector3Dd(0, -1, 0));
    let classification: RaySolidClassification = new RaySolidClassification();
    expectEq(
        RaySolidQueryStatus.Supported,
        solid.classifyRay(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), classification),
    );
    expectEq(2, classification.boundaries.size());
    expectNear(classification.boundaries.get(0).t, 10 - 2.3, 1e-12);
    expectEq(0, classification.boundaries.get(0).surfaceIndex);
    expectNear(classification.boundaries.get(1).t, 10, 1e-12);
    expectEq(3, classification.boundaries.get(1).surfaceIndex);
    expectEq(3, classification.insideSegments.size());
    expectFalse(classification.insideSegments.get(0));
    expectTrue(classification.insideSegments.get(1));
    expectFalse(classification.insideSegments.get(2));

    let hit: RayHit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
    expectEq(RayFirstHitStatus.Hit, solid.queryFirstHit(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), hit));
    const length: number = JavaMath.sqrt(0.25 + 1 + 0.0625);
    expectNear(hit.normal.x(), -0.5 / length, 1e-12);
    expectNear(hit.normal.y(), 1 / length, 1e-12);
    expectNear(hit.normal.z(), -0.25 / length, 1e-12);

    // The PovRay traversal agrees on a slanted ray.
    const slanted: Ray = Ray.fromAffine(new Vector3Dd(1.3, 10, 2.6), new Vector3Dd(0.1, -1, -0.05));
    expectEq(
        RaySolidQueryStatus.Supported,
        solid.classifyRay(slanted, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), classification),
    );
    expectFalse(classification.boundaries.isEmpty());
    let depth: number[] = [0];
    expectEq(
        HeightFieldTraceResult.Hit,
        solid.traceFirstHit(new Vector3Dd(1.3, 10, 2.6), new Vector3Dd(0.1, -1, -0.05), depth),
    );
    expectNear(depth[0], classification.boundaries.get(0).t, 1e-9);
    expectEq(
        HeightFieldTraceResult.MissedBounds,
        solid.traceFirstHit(new Vector3Dd(1.3, 10, 2.6), new Vector3Dd(0, 1, 0), depth),
    );

    expectEq(GeometryContainment.Inside, solid.classifyPoint(new Vector3Dd(1.3, 1, 2.6), 0));
    expectEq(GeometryContainment.Outside, solid.classifyPoint(new Vector3Dd(1.3, 3, 2.6), 0));
    expectEq(GeometryContainment.Inside, solid.classifyPointCertified(new Vector3Dd(1.3, 1, 2.6), 1e-6));
    expectEq(GeometryContainment.Outside, solid.classifyPointCertified(new Vector3Dd(1.3, 3.9, 2.6), 1e-6));
    expectEq(GeometryContainment.Boundary, solid.classifyPointCertified(new Vector3Dd(1.3, 2.3, 2.6), 1e-6));
    expectEq(GeometryContainment.Outside, solid.classifyPoint(new Vector3Dd(3.5, 1, 2), 0));

    let bounds: GeometryBounds = new GeometryBounds();
    solid.getBounds(bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    expectDoubleEq(bounds.getMaximum().y(), 1 + 1.5 + 0.75);
});

test("HeightFieldSolidContract_TraversalAndExactClassificationAgreeOnHills", () => {
    const solid: HeightFieldSolid = HeightFieldTestFactory.createHills();
    let compared: number = 0;
    for (let i = 0; i < 400; ++i) {
        const origin: Vector3Dd = new Vector3Dd(-3 + 0.055 * i, 9, 1.013 + 0.037 * i);
        const direction: Vector3Dd = new Vector3Dd(
            JavaMath.cos(0.3 * i),
            -1.1 - 0.4 * JavaMath.sin(0.7 * i),
            JavaMath.sin(0.3 * i),
        );
        const ray: Ray = Ray.fromAffine(origin, direction);
        let classification: RaySolidClassification = new RaySolidClassification();
        expectEq(
            RaySolidQueryStatus.Supported,
            solid.classifyRay(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), classification),
            "" + i,
        );
        // Membership between boundaries matches point containment.
        for (let k = 0; k + 1 < classification.boundaries.size(); ++k) {
            const t: number = 0.5 * (classification.boundaries.get(k).t + classification.boundaries.get(k + 1).t);
            const c: GeometryContainment = solid.classifyPoint(origin.add(direction.multiply(t)), 0);
            expectEq(classification.insideSegments.get(k + 1), c === GeometryContainment.Inside, "" + i);
        }
        let depth: number[] = [0];
        if (solid.traceFirstHit(origin, direction, depth) !== HeightFieldTraceResult.Hit) continue;
        // The PovRay traversal may report hits on cells just outside the box.
        const p: Vector3Dd = origin.add(direction.multiply(depth[0]));
        if (p.x() < 1 || p.x() > 15 || p.z() < 1 || p.z() > 15) continue;
        expectFalse(classification.boundaries.isEmpty(), "" + i);
        const first: RayBoundary = classification.boundaries.get(0);
        if (first.surfaceIndex !== 0) continue;
        expectNear(depth[0], first.t, 1e-6 * (1 + first.t), "" + i);
        ++compared;
    }
    expectGt(compared, 150);
});

test("HeightFieldSolidContract_CertifiedBoxesAndInvalidGrids", () => {
    const solid: HeightFieldSolid = HeightFieldTestFactory.createPlane();
    expectEq(
        GeometryContainment.Inside,
        solid.classifyBoxCertified(new Vector3Dd(2, 0.5, 2), new Vector3Dd(0.4, 0.2, 0.4)),
    );
    expectEq(
        GeometryContainment.Outside,
        solid.classifyBoxCertified(new Vector3Dd(2, 4.6, 2), new Vector3Dd(0.4, 0.2, 0.4)),
    );
    expectEq(
        GeometryContainment.Indeterminate,
        solid.classifyBoxCertified(new Vector3Dd(2, 2.5, 2), new Vector3Dd(0.4, 0.5, 0.4)),
    );
    let outside: HeightFieldSolid = new HeightFieldSolid(3, 3, new Vector3Dd(0, 0, 0), new Vector3Dd(4, 1, 2));
    expectFalse(outside.isValid());
    let classification: RaySolidClassification = new RaySolidClassification();
    expectEq(
        RaySolidQueryStatus.Indeterminate,
        outside.classifyRay(
            Ray.fromAffine(new Vector3Dd(1, 5, 1), new Vector3Dd(0, -1, 0)),
            new RayCrossingQuery(0, Double.POSITIVE_INFINITY),
            classification,
        ),
    );
});
