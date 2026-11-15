// Port of cpp/base/src/test/ray_affine_contract_tests.cpp
import { test } from "vitest";
import {
    expectDoubleEq,
    expectEq,
    expectFalse,
    expectGe,
    expectGt,
    expectLe,
    expectLt,
    expectNe,
    expectNear,
    expectThrow,
    expectTrue,
} from "../../../../GTestAssertions.js";
import { AffineEllipsoidDistance } from "vsdk/toolkit/environment/geometry/volume/AffineEllipsoidDistance.js";
import { AffineEllipsoidDistanceBounds } from "vsdk/toolkit/environment/geometry/volume/AffineEllipsoidDistanceBounds.js";
import { AffineEllipsoidMinimumDistance } from "vsdk/toolkit/environment/geometry/volume/AffineEllipsoidMinimumDistance.js";
import { AffinePlacement } from "vsdk/toolkit/environment/scene/AffinePlacement.js";
import { AffinePlacementStatus } from "vsdk/toolkit/environment/scene/AffinePlacementStatus.js";
import { ArrayList } from "java/util/ArrayList.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { Cone } from "vsdk/toolkit/environment/geometry/volume/Cone.js";
import { CsgBodyOperand } from "vsdk/toolkit/environment/scene/csg/CsgBodyOperand.js";
import { CsgOperation } from "vsdk/toolkit/environment/scene/csg/CsgOperation.js";
import { CsgPointClassification } from "vsdk/toolkit/environment/scene/csg/CsgPointClassification.js";
import { CsgTestOperand } from "../../../../CsgTestOperand.js";
import { CsgTree } from "vsdk/toolkit/environment/scene/csg/CsgTree.js";
import { Double } from "java/lang/Double.js";
import { FloatingInterval } from "vsdk/toolkit/common/numerics/FloatingInterval.js";
import { Geometry } from "vsdk/toolkit/environment/geometry/Geometry.js";
import { GeometryBounds } from "vsdk/toolkit/environment/geometry/element/GeometryBounds.js";
import { GeometryContainment } from "vsdk/toolkit/environment/geometry/element/GeometryContainment.js";
import { GeometryDistanceStatus } from "vsdk/toolkit/environment/geometry/element/GeometryDistanceStatus.js";
import { GeometryQueryWorkspace } from "vsdk/toolkit/environment/geometry/element/GeometryQueryWorkspace.js";
import { GeometryWorkspaceTestWorker } from "../../../../GeometryWorkspaceTestWorker.js";
import { IllegalStateException } from "java/lang/IllegalStateException.js";
import { InfinitePlane } from "vsdk/toolkit/environment/geometry/surface/InfinitePlane.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { OrthogonalEllipsoidDistance } from "vsdk/toolkit/environment/geometry/volume/OrthogonalEllipsoidDistance.js";
import { PlanarHalfSpace } from "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.js";
import { QuadricRayExpansion } from "vsdk/toolkit/environment/geometry/volume/QuadricRayExpansion.js";
import { QuadricSurface } from "vsdk/toolkit/environment/geometry/volume/QuadricSurface.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayBoundary } from "vsdk/toolkit/environment/geometry/element/RayBoundary.js";
import { RayCrossing } from "vsdk/toolkit/environment/geometry/element/RayCrossing.js";
import { RayCrossingKind } from "vsdk/toolkit/environment/geometry/element/RayCrossingKind.js";
import { RayCrossingQuery } from "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.js";
import { RayCrossingSupport } from "vsdk/toolkit/environment/geometry/element/RayCrossingSupport.js";
import { RayCrossingTestCollector } from "../../../../RayCrossingTestCollector.js";
import { RayFirstHitStatus } from "vsdk/toolkit/environment/geometry/element/RayFirstHitStatus.js";
import { RayHit } from "vsdk/toolkit/environment/geometry/element/RayHit.js";
import { RayInstanceStep } from "vsdk/toolkit/environment/geometry/element/RayInstanceStep.js";
import { RaySolidBoundary } from "vsdk/toolkit/environment/geometry/element/RaySolidBoundary.js";
import { RaySolidClassification } from "vsdk/toolkit/environment/geometry/element/RaySolidClassification.js";
import { RaySolidQueryStatus } from "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.js";
import { SimpleBody } from "vsdk/toolkit/environment/scene/SimpleBody.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { Torus } from "vsdk/toolkit/environment/geometry/volume/Torus.js";
import { Triangle } from "vsdk/toolkit/environment/geometry/element/Triangle.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
test("FloatingIntervalContract_OutwardRoundingAndFailureCases", () => {
    expectTrue(FloatingInterval.isSupportedEnvironment());
    const sum = FloatingInterval.exact(0.1).add(FloatingInterval.exact(0.2));
    expectTrue(sum.isValid());
    expectLe(sum.getLower(), 0.3);
    expectGe(sum.getUpper(), 0.3);
    const quotient = FloatingInterval.exact(1).divide(FloatingInterval.exact(3));
    expectTrue(quotient.isValid());
    expectLt(quotient.getLower(), 1.0 / 3.0);
    expectGt(quotient.getUpper(), 1.0 / 3.0);
    expectFalse(FloatingInterval.exact(1).divide(FloatingInterval.exact(0)).isValid());
    expectFalse(FloatingInterval.exact(Double.MAX_VALUE).multiply(FloatingInterval.exact(2)).isValid());
    const tiny = FloatingInterval.exact(Double.MIN_VALUE).multiply(FloatingInterval.exact(0.5));
    expectTrue(tiny.isValid());
    expectLe(tiny.getLower(), 0);
    const minValue = Double.MIN_VALUE;
    expectGe(tiny.getUpper(), minValue);
    // Not ported: the cpp test switches the rounding mode with fesetround and
    // expects the certified queries to refuse it; JavaScript arithmetic is always
    // round-to-nearest, so a directed rounding mode cannot be set.
});

test("FloatingIntervalContract_BoundsContainIndependentExactRationalResults", () => {
    expectTrue(FloatingInterval.isSupportedEnvironment());
    // Selected floor and ceiling endpoints from
    // generate_interval_reference.py, using exact rational arithmetic.
    const cases = [
        [0.1, 0.2, 0, 0.3, 0.30000000000000004],
        [0.1, 0.2, 2, 0.02, 0.020000000000000004],
        [-0.1, 0.2, 1, -0.30000000000000004, -0.3],
        [1e-300, 1e-20, 0, 1e-20, 1.0000000000000001e-20],
        [1e-300, 1e-20, 2, 1e-320, 1.0005e-320],
        [1e-300, 1e-20, 3, 1.0000000000000001e-280, 1.0000000000000002e-280],
        [1e150, 1e140, 1, 9.999999998999999e149, 9.999999999e149],
        [1e150, 1e140, 2, 9.999999999999999e289, 1e290],
        [1.0000000000000002, 0.9999999999999999, 1, 3.3306690738754696e-16, 3.3306690738754696e-16],
        [1.0000000000000002, 0.9999999999999999, 3, 1.0000000000000002, 1.0000000000000004],
        [-1e-200, -1e-110, 2, 1e-310, 1.00000000000005e-310],
        [1, 3, 3, 0.3333333333333333, 0.33333333333333337],
        [1e-200, 1e100, 3, 9.999999999999999e-301, 1e-300],
    ];
    for (let i = 0; i < 13; ++i) {
        const a = FloatingInterval.exact(cases[i][0]);
        const b = FloatingInterval.exact(cases[i][1]);
        let result = new FloatingInterval();
        switch (cases[i][2]) {
            case 0:
                result = a.add(b);
                break;
            case 1:
                result = a.subtract(b);
                break;
            case 2:
                result = a.multiply(b);
                break;
            default:
                result = a.divide(b);
                break;
        }
        expectTrue(result.isValid());
        expectLe(result.getLower(), cases[i][3]);
        expectGe(result.getUpper(), cases[i][4]);
    }
});

test("EllipsoidDistanceBoundsContract_AxisAndShearEncloseKnownDistances", () => {
    const lower = [-1];
    const upper = [-1];
    const side = [GeometryContainment.Indeterminate];
    const identity = Matrix4x4d.identityMatrix();
    expectEq(
        GeometryDistanceStatus.Valid,
        AffineEllipsoidDistanceBounds.query(
            new Vector3Dd(3, 0, 0),
            1,
            identity,
            new Vector3Dd(1, 0, 0),
            side,
            lower,
            upper,
        ),
    );
    expectEq(GeometryContainment.Outside, side[0]!);
    expectLe(lower[0]!, 2);
    expectGe(upper[0]!, 2);
    expectNear(2, lower[0]!, 1e-12);
    expectNear(2, upper[0]!, 1e-12);
    expectEq(
        GeometryDistanceStatus.Valid,
        AffineEllipsoidDistanceBounds.query(new Vector3Dd(), 1, identity, new Vector3Dd(0, 1, 0), side, lower, upper),
    );
    expectEq(GeometryContainment.Inside, side[0]!);
    expectGt(lower[0]!, 0);
    expectLe(lower[0]!, 1);
    expectGe(upper[0]!, 1);
    expectNear(1, upper[0]!, 1e-12);
    const shear = new Matrix4x4d()
        .translation(3, -2, 1)
        .multiply(Matrix4x4d.identityMatrix().withVal(0, 0, 2).withVal(0, 1, 0.5));
    const seed = new Vector3Dd(0.6, 0.8, 0);
    const normal = new Vector3Dd(0.3, 0.65, 0);
    const point = shear.multiply(seed).add(normal.multiply(0.2 / normal.length()));
    expectEq(
        GeometryDistanceStatus.Valid,
        AffineEllipsoidDistanceBounds.query(point, 1, shear, seed, side, lower, upper),
    );
    expectEq(GeometryContainment.Outside, side[0]!);
    expectLe(lower[0]!, 0.2 + 1e-12);
    expectGe(upper[0]!, 0.2 - 1e-12);
    expectNear(0.2, upper[0]!, 1e-10);
});

test("EllipsoidDistanceBoundsContract_InvalidUncertainAndRoundingModeKeepOutputs", () => {
    const lower = [17];
    const upper = [23];
    const side = [GeometryContainment.Boundary];
    expectEq(
        GeometryDistanceStatus.InvalidInput,
        AffineEllipsoidDistanceBounds.query(
            new Vector3Dd(),
            1,
            new Matrix4x4d().scale(0, 1, 1),
            new Vector3Dd(1, 0, 0),
            side,
            lower,
            upper,
        ),
    );
    expectEq(
        GeometryDistanceStatus.InvalidInput,
        AffineEllipsoidDistanceBounds.query(new Vector3Dd(), 1, new Matrix4x4d(), new Vector3Dd(), side, lower, upper),
    );
    expectDoubleEq(17, lower[0]!);
    expectDoubleEq(23, upper[0]!);
    expectEq(GeometryContainment.Boundary, side[0]!);
    const ill = Matrix4x4d.identityMatrix().withVal(0, 1, 1).withVal(1, 1, 1e-18);
    const status = AffineEllipsoidDistanceBounds.query(
        new Vector3Dd(2, 0, 0),
        1,
        ill,
        new Vector3Dd(1, 0, 0),
        side,
        lower,
        upper,
    );
    expectTrue(status === GeometryDistanceStatus.Indeterminate || status === GeometryDistanceStatus.Valid);
    if (status === GeometryDistanceStatus.Indeterminate) {
        expectDoubleEq(17, lower[0]!);
        expectDoubleEq(23, upper[0]!);
    }
    if (status === GeometryDistanceStatus.Valid) {
        const distance = 2 - Math.sqrt(2.0);
        expectLe(lower[0]!, distance + 1e-12);
        expectGe(upper[0]!, distance - 1e-12);
    }
    lower[0] = 17;
    upper[0] = 23;
    side[0] = GeometryContainment.Boundary;
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    // Not ported: the cpp test switches the rounding mode with fesetround and
    // expects the certified queries to refuse it; JavaScript arithmetic is always
    // round-to-nearest, so a directed rounding mode cannot be set.
});

test("EllipsoidDistanceBoundsContract_SceneExposesCertifiedEnclosuresAndContainment", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setScale(new Vector3Dd(3, 2, 1));
    const side = [GeometryContainment.Indeterminate];
    const lower = [0];
    const upper = [0];
    expectEq(
        GeometryDistanceStatus.Valid,
        body.encloseBoundaryDistance(new Vector3Dd(3.2, 0, 0), new Vector3Dd(1, 0, 0), side, lower, upper),
    );
    expectEq(GeometryContainment.Outside, side[0]!);
    expectLe(lower[0]!, 0.2 + 1e-14);
    expectGe(upper[0]!, 0.2 - 1e-14);
    expectEq(GeometryContainment.Inside, body.classifyPointCertified(new Vector3Dd(), 0.1));
    // World distance is 0.2; the plain inverse-norm bound is below 0.14.
    expectEq(GeometryContainment.Inside, body.classifyPointCertified(new Vector3Dd(2.8, 0, 0), 0.14));
    expectEq(GeometryContainment.Outside, body.classifyPointCertified(new Vector3Dd(3.2, 0, 0), 0.1));
    expectEq(GeometryContainment.Boundary, body.classifyPointCertified(new Vector3Dd(3.05, 0, 0), 0.1));
    expectEq(GeometryContainment.Indeterminate, body.classifyPointCertified(new Vector3Dd(3.1, 0, 0), 0.1));
    expectEq(GeometryContainment.InvalidInput, body.classifyPointCertified(new Vector3Dd(), -1));
    const box = new Box(1, 1, 1);
    body.setGeometryReference(box);
    expectEq(GeometryContainment.Inside, body.classifyPointCertified(new Vector3Dd(), 0.1));
    const cone = new Cone(1, 1, 1);
    body.setGeometryReference(cone);
    expectEq(GeometryContainment.Unsupported, body.classifyPointCertified(new Vector3Dd(), 0.1));
});

test("EllipsoidDistanceBoundsContract_IndependentReferencesStayInsideCertifiedBounds", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    const rotation = Matrix4x4d.identityMatrix().withVal(0, 0, 0).withVal(0, 1, -1).withVal(1, 0, 1).withVal(1, 1, 0);
    const inverseRotation = Matrix4x4d.identityMatrix()
        .withVal(0, 0, 0)
        .withVal(0, 1, 1)
        .withVal(1, 0, -1)
        .withVal(1, 1, 0);
    const shape = Matrix4x4d.identityMatrix().withVal(0, 0, -2).withVal(0, 1, 0.5).withVal(1, 2, -0.3);
    const inverseShape = Matrix4x4d.identityMatrix()
        .withVal(0, 0, -0.5)
        .withVal(0, 1, 0.25)
        .withVal(0, 2, 0.075)
        .withVal(1, 2, 0.3);
    const samples = [
        [0.12, 0.27, 0.31, 0.484219147169112],
        [0.65, 0.1, -0.2, 0.437976391530554],
        [1.1, 0.2, 0.3, 0.228896833955284],
        [0.1, -1.2, 0.5, 0.341598824284725],
        [-0.9, -0.4, -0.3, 0.0336629761551959],
        [0, 0, 0, 0.84234037196832],
    ];
    const factors = [0.01, 1, 100];
    let definite = 0;
    for (let k = 0; k < 3; ++k) {
        const factor = factors[k];
        const forward = new Matrix4x4d()
            .translation(4, -3, 2)
            .multiply(rotation)
            .multiply(new Matrix4x4d().scale(factor, factor, factor))
            .multiply(shape);
        const inverse = inverseShape
            .multiply(new Matrix4x4d().scale(1 / factor, 1 / factor, 1 / factor))
            .multiply(inverseRotation)
            .multiply(new Matrix4x4d().translation(-4, 3, -2));
        expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward, inverse));
        for (let index = 0; index < 6; ++index) {
            const local = new Vector3Dd(samples[index][0], samples[index][1], samples[index][2]);
            const point = forward.multiply(local);
            const seed = local.dotProduct(local) > 0 ? local : new Vector3Dd(1, 0, 0);
            const side = [GeometryContainment.Indeterminate];
            const lower = [0];
            const upper = [0];
            expectEq(GeometryDistanceStatus.Valid, body.encloseBoundaryDistance(point, seed, side, lower, upper));
            const reference = samples[index][3] * factor;
            expectLe(lower[0]!, reference + 1e-9 * factor);
            expectGe(upper[0]!, reference - 1e-9 * factor);
            const expected = local.dotProduct(local) < 1 ? GeometryContainment.Inside : GeometryContainment.Outside;
            expectTrue(side[0]! === expected || side[0]! === GeometryContainment.Indeterminate);
            const nearTolerance = reference + 1e-4 * factor;
            const near = body.classifyPointCertified(point, nearTolerance);
            expectTrue(near === GeometryContainment.Boundary || near === GeometryContainment.Indeterminate);
            expectEq(near, body.classifyPoint(point, nearTolerance));
            const farTolerance = reference * 0.25;
            const far = body.classifyPointCertified(point, farTolerance);
            expectTrue(far === expected || far === GeometryContainment.Indeterminate);
            expectEq(far, body.classifyPoint(point, farTolerance));
            if (near === GeometryContainment.Boundary) ++definite;
            if (far === expected) ++definite;
        }
    }
    expectEq(36, definite);
});

test("EllipsoidDistanceBoundsContract_DiverseAffineFixturesEncloseIndependentDistances", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    const matrices = [
        Matrix4x4d.identityMatrix().withVal(0, 1, 0.9).withVal(0, 2, 0.2).withVal(1, 2, 0.6),
        Matrix4x4d.identityMatrix().withVal(0, 0, 0.2).withVal(1, 1, 3).withVal(1, 2, 0.4),
        Matrix4x4d.identityMatrix()
            .withVal(0, 1, 0.8)
            .withVal(0, 2, 0.4)
            .withVal(1, 0, 0.3)
            .withVal(1, 2, -0.2)
            .withVal(2, 0, 0.1)
            .withVal(2, 1, 0.5)
            .withVal(2, 2, 1.2),
    ];
    const points = [
        new Vector3Dd(),
        new Vector3Dd(0.1, 0.3, -0.2),
        new Vector3Dd(0.6, -0.2, 0.1),
        new Vector3Dd(0.3, 0.6, 0.2),
        new Vector3Dd(1.2, 0.3, -0.1),
        new Vector3Dd(-0.4, -1.1, 0.6),
        new Vector3Dd(0.2, 0.1, 1.4),
        new Vector3Dd(-0.9, 0.3, -0.4),
    ];
    // Generated independently by generate_certified_bounds_reference.py.
    const expected = [
        [
            0.578406438716408, 0.436864249515077, 0.21684579558424, 0.342292911715945, 0.222512940034161,
            0.318320672587438, 0.453659506464624, 0.0181298663812327,
        ],
        [
            0.2, 0.166279867729029, 0.0749031414139726, 0.0947459615938375, 0.0502305824969421, 0.439324293232699,
            0.413291190037398, 0.00675839616866524,
        ],
        [
            0.526534747238471, 0.427659995509968, 0.195778365340438, 0.309070492537248, 0.199911925624104,
            0.356476528572591, 0.522022480951273, 0.0172612127525319,
        ],
    ];
    let verifiedSides = 0;
    let definite = 0;
    for (let matrix = 0; matrix < 3; ++matrix) {
        expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(matrices[matrix], matrices[matrix].inverse()));
        for (let index = 0; index < 8; ++index) {
            const point = matrices[matrix].multiply(points[index]);
            const seed = points[index].dotProduct(points[index]) > 0 ? points[index] : new Vector3Dd(1, 0, 0);
            const expectedSide =
                points[index].dotProduct(points[index]) < 1 ? GeometryContainment.Inside : GeometryContainment.Outside;
            const side = [GeometryContainment.Indeterminate];
            const lower = [0];
            const upper = [0];
            expectEq(
                GeometryDistanceStatus.Valid,
                AffineEllipsoidDistanceBounds.query(point, 1, matrices[matrix], seed, side, lower, upper),
            );
            expectLe(lower[0]!, expected[matrix][index] + 1e-8);
            expectGe(upper[0]!, expected[matrix][index] - 1e-8);
            expectTrue(side[0]! === expectedSide || side[0]! === GeometryContainment.Indeterminate);
            if (side[0]! === expectedSide) ++verifiedSides;
            const near = body.classifyPointCertified(point, expected[matrix][index] + 1e-4);
            expectTrue(near === GeometryContainment.Boundary || near === GeometryContainment.Indeterminate);
            if (near === GeometryContainment.Boundary) ++definite;
            const far = body.classifyPointCertified(point, expected[matrix][index] * 0.25);
            expectTrue(far === expectedSide || far === GeometryContainment.Indeterminate);
            if (far === expectedSide) ++definite;
            const minimumSide = [GeometryContainment.Indeterminate];
            const minimumLower = [0];
            const minimumUpper = [0];
            expectEq(
                GeometryDistanceStatus.Valid,
                body.encloseMinimumBoundaryDistance(point, 1e-5, minimumSide, minimumLower, minimumUpper),
            );
            expectLe(minimumLower[0]!, expected[matrix][index] + 1e-8);
            expectGe(minimumUpper[0]!, expected[matrix][index] - 1e-8);
            expectLe(minimumUpper[0]! - minimumLower[0]!, 1e-5);
        }
    }
    expectGe(verifiedSides, 20);
    expectEq(48, definite);
});

test("EllipsoidDistanceBoundsContract_NonUnitRadiusAndShearUseWorldTolerance", () => {
    const sphere = new Sphere(2);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    const forward = new Matrix4x4d()
        .translation(3, -2, 1)
        .multiply(Matrix4x4d.identityMatrix().withVal(0, 0, 2).withVal(0, 1, 0.5));
    const inverse = Matrix4x4d.identityMatrix()
        .withVal(0, 0, 0.5)
        .withVal(0, 1, -0.25)
        .multiply(new Matrix4x4d().translation(-3, 2, -1));
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward, inverse));
    const source = new Vector3Dd(0.6, 0.8, 0);
    const normal = new Vector3Dd(0.3, 0.65, 0);
    const point = forward.multiply(source.multiply(2)).add(normal.multiply(0.2 / normal.length()));
    const side = [GeometryContainment.Indeterminate];
    const lower = [0];
    const upper = [0];
    expectEq(GeometryDistanceStatus.Valid, body.encloseBoundaryDistance(point, source, side, lower, upper));
    expectEq(GeometryContainment.Outside, side[0]!);
    expectLe(lower[0]!, 0.2 + 1e-12);
    expectGe(upper[0]!, 0.2 - 1e-12);
    expectEq(GeometryContainment.Outside, body.classifyPointCertified(point, 0.1));
    expectEq(GeometryContainment.Boundary, body.classifyPointCertified(point, 0.3));
    expectEq(GeometryContainment.Inside, body.classifyPointCertified(new Vector3Dd(3, -2, 1), 0.1));
});

test("EllipsoidMinimumDistanceContract_SceneProducesCertifiedTightDistances", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setScale(new Vector3Dd(3, 2, 1));
    const points = [new Vector3Dd(3.2, 0, 0), new Vector3Dd(), new Vector3Dd(1, 0, 0), new Vector3Dd(2.8, 0, 0)];
    const expected = [0.2, 1, Math.sqrt(0.875), 0.2];
    const sides = [
        GeometryContainment.Outside,
        GeometryContainment.Inside,
        GeometryContainment.Inside,
        GeometryContainment.Inside,
    ];
    for (let i = 0; i < 4; ++i) {
        const side = [GeometryContainment.Indeterminate];
        const lower = [0];
        const upper = [0];
        expectEq(
            GeometryDistanceStatus.Valid,
            body.encloseMinimumBoundaryDistance(points[i], 1e-6, side, lower, upper),
        );
        expectEq(sides[i], side[0]!);
        expectLe(lower[0]!, expected[i] + 1e-12);
        expectGe(upper[0]!, expected[i] - 1e-12);
        expectLe(upper[0]! - lower[0]!, 1e-6);
    }
    // The closest point to (1,0,0) is off axis; the radial direction alone
    // cannot produce a witness inside this narrow tolerance band.
    expectEq(
        GeometryContainment.Boundary,
        body.classifyPointCertified(new Vector3Dd(1, 0, 0), Math.sqrt(0.875) + 1e-7),
    );
    const side = [GeometryContainment.Boundary];
    const lower = [17];
    const upper = [23];
    expectEq(
        GeometryDistanceStatus.InvalidInput,
        body.encloseMinimumBoundaryDistance(new Vector3Dd(3.2, 0, 0), 0, side, lower, upper),
    );
    expectEq(
        GeometryDistanceStatus.Indeterminate,
        body.encloseMinimumBoundaryDistance(new Vector3Dd(3.2, 0, 0), 1e-30, side, lower, upper),
    );
    expectEq(GeometryContainment.Boundary, side[0]!);
    expectDoubleEq(17, lower[0]!);
    expectDoubleEq(23, upper[0]!);
});

test("EllipsoidMinimumDistanceContract_ShearedSceneMatchesIndependentReferences", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    const forward = Matrix4x4d.identityMatrix().withVal(0, 0, -2).withVal(0, 1, 0.5).withVal(1, 2, -0.3);
    const inverse = Matrix4x4d.identityMatrix()
        .withVal(0, 0, -0.5)
        .withVal(0, 1, 0.25)
        .withVal(0, 2, 0.075)
        .withVal(1, 2, 0.3);
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward, inverse));
    const samples = [
        [0.12, 0.27, 0.31, 0.484219147169112],
        [0.65, 0.1, -0.2, 0.437976391530554],
        [1.1, 0.2, 0.3, 0.228896833955284],
        [0.1, -1.2, 0.5, 0.341598824284725],
        [0.01, 0.01, 0.01, 0.828855732290258],
        [0, 0, 0, 0.84234037196832],
    ];
    for (let i = 0; i < 6; ++i) {
        const local = new Vector3Dd(samples[i][0], samples[i][1], samples[i][2]);
        const side = [GeometryContainment.Indeterminate];
        const lower = [0];
        const upper = [0];
        expectEq(
            GeometryDistanceStatus.Valid,
            body.encloseMinimumBoundaryDistance(forward.multiply(local), 1e-5, side, lower, upper),
        );
        expectLe(lower[0]!, samples[i][3] + 1e-9);
        expectGe(upper[0]!, samples[i][3] - 1e-9);
        expectLe(upper[0]! - lower[0]!, 1e-5);
    }
});

test("EllipsoidMinimumDistanceContract_CertifiedBoundsSurvivePlacementAndRadius", () => {
    const sphere = new Sphere(2);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    const rotation = Matrix4x4d.identityMatrix().withVal(0, 0, 0).withVal(0, 1, -1).withVal(1, 0, 1).withVal(1, 1, 0);
    const inverseRotation = Matrix4x4d.identityMatrix()
        .withVal(0, 0, 0)
        .withVal(0, 1, 1)
        .withVal(1, 0, -1)
        .withVal(1, 1, 0);
    const shape = Matrix4x4d.identityMatrix().withVal(0, 0, -2).withVal(0, 1, 0.5).withVal(1, 2, -0.3);
    const inverseShape = Matrix4x4d.identityMatrix()
        .withVal(0, 0, -0.5)
        .withVal(0, 1, 0.25)
        .withVal(0, 2, 0.075)
        .withVal(1, 2, 0.3);
    const samples = [
        [0.12, 0.27, 0.31, 0.484219147169112],
        [0.1, -1.2, 0.5, 0.341598824284725],
        [0, 0, 0, 0.84234037196832],
    ];
    const factors = [0.01, 1, 100];
    for (let k = 0; k < 3; ++k) {
        const factor = factors[k];
        const forward = new Matrix4x4d()
            .translation(4, -3, 2)
            .multiply(rotation)
            .multiply(new Matrix4x4d().scale(factor, factor, factor))
            .multiply(shape);
        const inverse = inverseShape
            .multiply(new Matrix4x4d().scale(1 / factor, 1 / factor, 1 / factor))
            .multiply(inverseRotation)
            .multiply(new Matrix4x4d().translation(-4, 3, -2));
        expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward, inverse));
        for (let i = 0; i < 3; ++i) {
            const local = new Vector3Dd(samples[i][0] * 2, samples[i][1] * 2, samples[i][2] * 2);
            const side = [GeometryContainment.Indeterminate];
            const lower = [0];
            const upper = [0];
            expectEq(
                GeometryDistanceStatus.Valid,
                body.encloseMinimumBoundaryDistance(forward.multiply(local), 1e-5 * factor, side, lower, upper),
            );
            const expected = 2 * factor * samples[i][3];
            expectLe(lower[0]!, expected + 1e-9 * factor);
            expectGe(upper[0]!, expected - 1e-9 * factor);
            expectLe(upper[0]! - lower[0]!, 1e-5 * factor);
        }
    }
});

test("EllipsoidMinimumDistanceContract_FailureAndBoundaryDoNotInventResults", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setScale(new Vector3Dd(3, 2, 1));
    const side = [GeometryContainment.Inside];
    const lower = [17];
    const upper = [23];
    expectEq(
        GeometryDistanceStatus.InvalidInput,
        body.encloseMinimumBoundaryDistance(new Vector3Dd(), Double.NaN, side, lower, upper),
    );
    expectEq(GeometryContainment.Inside, side[0]!);
    expectDoubleEq(17, lower[0]!);
    expectDoubleEq(23, upper[0]!);
    expectEq(
        GeometryDistanceStatus.Valid,
        body.encloseMinimumBoundaryDistance(new Vector3Dd(3, 0, 0), 1e-8, side, lower, upper),
    );
    expectLe(lower[0]!, 0);
    expectGe(upper[0]!, 0);
    expectLe(upper[0]!, 1e-8);
    expectEq(GeometryContainment.Indeterminate, side[0]!);
    side[0] = GeometryContainment.Boundary;
    lower[0] = 17;
    upper[0] = 23;
    // Not ported: the cpp test switches the rounding mode with fesetround and
    // expects the certified queries to refuse it; JavaScript arithmetic is always
    // round-to-nearest, so a directed rounding mode cannot be set.
    body.setScale(new Vector3Dd(0, 1, 1));
    expectEq(
        GeometryDistanceStatus.InvalidInput,
        body.encloseMinimumBoundaryDistance(new Vector3Dd(), 1e-5, side, lower, upper),
    );
    expectEq(GeometryContainment.Boundary, side[0]!);
    expectDoubleEq(17, lower[0]!);
    expectDoubleEq(23, upper[0]!);
});

test("EllipsoidMinimumDistanceContract_WrongInverseHintCannotFalsifyBounds", () => {
    const side = [GeometryContainment.Indeterminate];
    const lower = [-1];
    const upper = [-1];
    // The inverse hint is deliberately wrong. It may worsen the witness, but
    // the returned enclosure is calculated from the forward matrix itself.
    expectEq(
        GeometryDistanceStatus.Valid,
        AffineEllipsoidMinimumDistance.query(
            new Vector3Dd(3, 0, 0),
            1,
            Matrix4x4d.identityMatrix(),
            new Matrix4x4d().scale(-1, 1, 1),
            10,
            side,
            lower,
            upper,
        ),
    );
    expectEq(GeometryContainment.Outside, side[0]!);
    expectLe(lower[0]!, 2);
    expectGe(upper[0]!, 2);
    expectLe(upper[0]! - lower[0]!, 10);
    side[0] = GeometryContainment.Boundary;
    lower[0] = 17;
    upper[0] = 23;
    expectEq(
        GeometryDistanceStatus.Indeterminate,
        AffineEllipsoidMinimumDistance.query(
            new Vector3Dd(3, 0, 0),
            1,
            Matrix4x4d.identityMatrix(),
            new Matrix4x4d().scale(-1, 1, 1),
            1e-6,
            side,
            lower,
            upper,
        ),
    );
    expectEq(GeometryContainment.Boundary, side[0]!);
    expectDoubleEq(17, lower[0]!);
    expectDoubleEq(23, upper[0]!);
});

test("FloatingIntervalContract_IntrinsicSphereTolerancePreservesUncertainty", () => {
    const sphere = new Sphere(3);
    expectEq(GeometryContainment.Inside, sphere.classifyPoint(new Vector3Dd(2.8, 0, 0), 0.1));
    expectEq(GeometryContainment.Boundary, sphere.classifyPoint(new Vector3Dd(3.05, 0, 0), 0.1));
    expectEq(GeometryContainment.Outside, sphere.classifyPoint(new Vector3Dd(3.2, 0, 0), 0.1));
    expectEq(GeometryContainment.Indeterminate, sphere.classifyPoint(new Vector3Dd(3.1, 0, 0), 0.1));
    expectThrow(IllegalStateException, () => sphere.doContainmentTest(new Vector3Dd(3.1, 0, 0), 0.1));
    expectEq(GeometryContainment.Boundary, sphere.classifyPoint(new Vector3Dd(3, 0, 0), 0));
});

test("EllipsoidDistanceContract_OrthogonalAxesCoverExteriorInteriorAndHardCase", () => {
    const forward = new Matrix4x4d().translation(5, -2, 3).multiply(new Matrix4x4d().scale(-3, 2, 1));
    const distance = [-1];
    const error = [-1];
    expectEq(
        GeometryDistanceStatus.Valid,
        OrthogonalEllipsoidDistance.query(new Vector3Dd(5, -2, 3), 1, forward, distance, error),
    );
    expectNear(1, distance[0]!, 1e-10);
    expectEq(
        GeometryDistanceStatus.Valid,
        OrthogonalEllipsoidDistance.query(new Vector3Dd(1.8, -2, 3), 1, forward, distance, error),
    );
    expectNear(0.2, distance[0]!, 1e-10);
    expectEq(
        GeometryDistanceStatus.Valid,
        OrthogonalEllipsoidDistance.query(new Vector3Dd(4, -2, 3), 1, forward, distance, error),
    );
    expectNear(Math.sqrt(0.875), distance[0]!, 1e-10);
    expectGt(error[0]!, 0);
    const shear = Matrix4x4d.identityMatrix().withVal(0, 1, 0.5);
    expectEq(
        GeometryDistanceStatus.Unsupported,
        OrthogonalEllipsoidDistance.query(new Vector3Dd(), 1, shear, distance, error),
    );
    expectEq(
        GeometryDistanceStatus.InvalidInput,
        OrthogonalEllipsoidDistance.query(new Vector3Dd(), 0, forward, distance, error),
    );
    expectEq(
        GeometryDistanceStatus.Indeterminate,
        OrthogonalEllipsoidDistance.query(new Vector3Dd(), 1, new Matrix4x4d().scale(1e-9, 1, 1), distance, error),
    );
    // A nonzero column whose squared norm underflows is ill-conditioned,
    // not a proven singular input.
    distance[0] = 17;
    error[0] = 23;
    expectEq(
        GeometryDistanceStatus.Indeterminate,
        OrthogonalEllipsoidDistance.query(new Vector3Dd(), 1, new Matrix4x4d().scale(1e-300, 1, 1), distance, error),
    );
    expectDoubleEq(17, distance[0]!);
    expectDoubleEq(23, error[0]!);
});

test("EllipsoidDistanceContract_ObliqueNormalOffsetsGiveWorldDistance", () => {
    const forward = new Matrix4x4d().scale(3, 2, 1);
    const surface = new Vector3Dd(1.8, 1.6, 0);
    const normal = new Vector3Dd(1, 2, 0).multiply(1 / Math.sqrt(5.0));
    const distance = [0];
    const error = [0];
    expectEq(
        GeometryDistanceStatus.Valid,
        OrthogonalEllipsoidDistance.query(surface.add(normal.multiply(0.2)), 1, forward, distance, error),
    );
    expectNear(0.2, distance[0]!, 1e-9);
    expectEq(
        GeometryDistanceStatus.Valid,
        OrthogonalEllipsoidDistance.query(surface.subtract(normal.multiply(0.05)), 1, forward, distance, error),
    );
    expectNear(0.05, distance[0]!, 1e-9);
});

test("EllipsoidDistanceContract_DistancesAgreeWithIndependentSurfaceSampling", () => {
    const rotation = Matrix4x4d.identityMatrix().withVal(0, 0, 0).withVal(0, 1, -1).withVal(1, 0, 1).withVal(1, 1, 0);
    const forward = new Matrix4x4d()
        .translation(4, -3, 2)
        .multiply(rotation)
        .multiply(new Matrix4x4d().scale(-3, 2, 1));
    const localPoints = [
        new Vector3Dd(),
        new Vector3Dd(0.1, 0.3, 0.2),
        new Vector3Dd(-0.7, 0.1, 0),
        new Vector3Dd(0.6, 0.7, 0.1),
        new Vector3Dd(1.4, 0.3, 0.5),
        new Vector3Dd(-0.5, -1.2, 0.3),
    ];
    for (let index = 0; index < 6; ++index) {
        const point = forward.multiply(localPoints[index]);
        let sampled = Double.MAX_VALUE;
        for (let latitude = 0; latitude <= 180; ++latitude) {
            const theta = (Math.PI * latitude) / 180;
            for (let longitude = 0; longitude < 360; ++longitude) {
                const phi = (2 * Math.PI * longitude) / 360;
                const unit = new Vector3Dd(
                    Math.sin(theta) * Math.cos(phi),
                    Math.sin(theta) * Math.sin(phi),
                    Math.cos(theta),
                );
                const candidate = forward.multiply(unit).subtract(point).length();
                if (candidate < sampled) sampled = candidate;
            }
        }
        const distance = [0];
        const error = [0];
        expectEq(GeometryDistanceStatus.Valid, OrthogonalEllipsoidDistance.query(point, 1, forward, distance, error));
        // The Frobenius norm is below 3.75; the two angular half-steps
        // sum to pi/180, so 0.07 bounds the surface sampling error[0]!.
        expectLe(distance[0]!, sampled + error[0]!);
        expectGe(distance[0]!, sampled - 0.07 - error[0]!);
    }
});

test("EllipsoidDistanceContract_RepeatedAxesRotationRadiusAndFailureAreExplicit", () => {
    const rotation = Matrix4x4d.identityMatrix().withVal(0, 0, 0).withVal(0, 1, -1).withVal(1, 0, 1).withVal(1, 1, 0);
    const forward = new Matrix4x4d()
        .translation(4, 2, -3)
        .multiply(rotation)
        .multiply(new Matrix4x4d().scale(-2, 2, 1));
    const distance = [-1];
    const error = [-1];
    expectEq(
        GeometryDistanceStatus.Valid,
        OrthogonalEllipsoidDistance.query(new Vector3Dd(4, 2, -3), 2, forward, distance, error),
    );
    expectNear(2, distance[0]!, 1e-10);
    const endpoint = forward.multiply(new Vector3Dd(2, 0, 0));
    expectEq(GeometryDistanceStatus.Valid, OrthogonalEllipsoidDistance.query(endpoint, 2, forward, distance, error));
    expectNear(0, distance[0]!, 1e-10);
    distance[0] = 17;
    error[0] = 23;
    expectEq(
        GeometryDistanceStatus.Unsupported,
        OrthogonalEllipsoidDistance.query(
            new Vector3Dd(),
            2,
            Matrix4x4d.identityMatrix().withVal(0, 1, 0.3),
            distance,
            error,
        ),
    );
    expectDoubleEq(17, distance[0]!);
    expectDoubleEq(23, error[0]!);
    expectEq(
        GeometryDistanceStatus.InvalidInput,
        OrthogonalEllipsoidDistance.query(new Vector3Dd(Double.NaN, 0, 0), 2, forward, distance, error),
    );
    expectEq(
        GeometryDistanceStatus.InvalidInput,
        OrthogonalEllipsoidDistance.query(new Vector3Dd(), 2, new Matrix4x4d().scale(0, 1, 1), distance, error),
    );
    expectDoubleEq(17, distance[0]!);
    expectDoubleEq(23, error[0]!);
});

test("EllipsoidDistanceContract_ShearSupportsExactNormalOffsetsAndCenter", () => {
    const forward = new Matrix4x4d()
        .translation(3, -2, 1)
        .multiply(Matrix4x4d.identityMatrix().withVal(0, 0, 2).withVal(0, 1, 0.5));
    const surface = forward.multiply(new Vector3Dd(0.6, 0.8, 0));
    const n = new Vector3Dd(0.3, 0.65, 0);
    const normal = n.multiply(1 / n.length());
    const distance = [0];
    const error = [0];
    expectEq(
        GeometryDistanceStatus.Valid,
        AffineEllipsoidDistance.query(surface.add(normal.multiply(0.2)), 1, forward, distance, error),
    );
    expectNear(0.2, distance[0]!, 1e-9);
    expectEq(
        GeometryDistanceStatus.Valid,
        AffineEllipsoidDistance.query(surface.subtract(normal.multiply(0.05)), 1, forward, distance, error),
    );
    expectNear(0.05, distance[0]!, 1e-9);
    expectEq(
        GeometryDistanceStatus.Valid,
        AffineEllipsoidDistance.query(new Vector3Dd(3, -2, 1), 1, forward, distance, error),
    );
    expectNear(Math.sqrt((5.25 - Math.sqrt(11.5625)) / 2), distance[0]!, 1e-9);
    const witness = [new Vector3Dd()];
    expectEq(
        GeometryDistanceStatus.Valid,
        AffineEllipsoidDistance.queryWithWitness(
            surface.add(normal.multiply(0.2)),
            1,
            forward,
            distance,
            error,
            witness,
        ),
    );
    expectNear(0, witness[0]!.subtract(surface).length(), 1e-9);
});

test("EllipsoidDistanceContract_ShearedNearSingularityDoesNotInventDistance", () => {
    const forward = Matrix4x4d.identityMatrix().withVal(0, 1, 1).withVal(1, 1, 1e-8);
    const distance = [17];
    const error = [23];
    expectEq(
        GeometryDistanceStatus.Indeterminate,
        AffineEllipsoidDistance.query(new Vector3Dd(), 1, forward, distance, error),
    );
    expectDoubleEq(17, distance[0]!);
    expectDoubleEq(23, error[0]!);
});

test("EllipsoidDistanceContract_SceneBodyExposesAffineDistanceAndFailures", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    const forward = new Matrix4x4d()
        .translation(3, -2, 1)
        .multiply(Matrix4x4d.identityMatrix().withVal(0, 0, 2).withVal(0, 1, 0.5));
    const inverse = Matrix4x4d.identityMatrix()
        .withVal(0, 0, 0.5)
        .withVal(0, 1, -0.25)
        .multiply(new Matrix4x4d().translation(-3, 2, -1));
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward, inverse));
    const n = new Vector3Dd(0.3, 0.65, 0);
    const point = forward.multiply(new Vector3Dd(0.6, 0.8, 0)).add(n.multiply(0.2 / n.length()));
    const distance = [0];
    const error = [0];
    expectEq(GeometryDistanceStatus.Valid, body.estimateBoundaryDistance(point, distance, error));
    expectNear(0.2, distance[0]!, 1e-9);
    expectGt(error[0]!, 0);
    const box = new Box(1, 1, 1);
    body.setGeometryReference(box);
    distance[0] = 17;
    error[0] = 23;
    expectEq(GeometryDistanceStatus.Unsupported, body.estimateBoundaryDistance(point, distance, error));
    expectDoubleEq(17, distance[0]!);
    expectDoubleEq(23, error[0]!);
    expectEq(
        GeometryDistanceStatus.InvalidInput,
        body.estimateBoundaryDistance(new Vector3Dd(Double.NaN, 0, 0), distance, error),
    );
    body.setGeometryReference(sphere);
    body.resetAffinePlacement();
    body.setScale(new Vector3Dd(0, 1, 1));
    expectEq(GeometryDistanceStatus.InvalidInput, body.estimateBoundaryDistance(point, distance, error));
    body.setScale(new Vector3Dd(1e-12, 1, 1));
    expectEq(GeometryDistanceStatus.Indeterminate, body.estimateBoundaryDistance(point, distance, error));
    expectDoubleEq(17, distance[0]!);
    expectDoubleEq(23, error[0]!);
});

test("EllipsoidDistanceContract_ShearedDistanceAgreesWithIndependentSurfaceSampling", () => {
    const forward = new Matrix4x4d()
        .translation(1, -3, 2)
        .multiply(Matrix4x4d.identityMatrix().withVal(0, 0, -2).withVal(0, 1, 0.5).withVal(1, 2, -0.3));
    const samples = [
        new Vector3Dd(),
        new Vector3Dd(0.1, 0.3, 0.2),
        new Vector3Dd(-0.7, 0.1, 0),
        new Vector3Dd(0.6, 0.7, 0.1),
        new Vector3Dd(1.4, 0.3, 0.5),
        new Vector3Dd(-0.5, -1.2, 0.3),
    ];
    for (let index = 0; index < 6; ++index) {
        const point = forward.multiply(samples[index]);
        let sampled = Double.MAX_VALUE;
        for (let latitude = 0; latitude <= 180; ++latitude) {
            const theta = (Math.PI * latitude) / 180;
            for (let longitude = 0; longitude < 360; ++longitude) {
                const phi = (2 * Math.PI * longitude) / 360;
                const unit = new Vector3Dd(
                    Math.sin(theta) * Math.cos(phi),
                    Math.sin(theta) * Math.sin(phi),
                    Math.cos(theta),
                );
                const candidate = forward.multiply(unit).subtract(point).length();
                if (candidate < sampled) sampled = candidate;
            }
        }
        const distance = [0];
        const error = [0];
        expectEq(GeometryDistanceStatus.Valid, AffineEllipsoidDistance.query(point, 1, forward, distance, error));
        // ||A||_F < 2.6 and the sum of angular half-steps is pi/180.
        expectLe(distance[0]!, sampled + error[0]!);
        expectGe(distance[0]!, sampled - 0.05 - error[0]!);
    }
});

test("EllipsoidDistanceContract_ShearedDistanceMatchesIndependentConstrainedMinimization", () => {
    const forward = Matrix4x4d.identityMatrix().withVal(0, 0, -2).withVal(0, 1, 0.5).withVal(1, 2, -0.3);
    // Generated by generate_ellipsoid_distance_reference.py: independent
    // constrained SLSQP with eight surface seeds, keeping the smallest value.
    const samples = [
        [0.12, 0.27, 0.31, 0.484219147169112],
        [0.65, 0.1, -0.2, 0.437976391530554],
        [0.21, -0.72, 0.35, 0.200937270429387],
        [0.35, 0.55, 0.15, 0.293307568130137],
        [1.1, 0.2, 0.3, 0.228896833955284],
        [-1.1, 0.8, 0.4, 0.717026711154501],
        [0.1, -1.2, 0.5, 0.341598824284725],
        [0.3, 0.2, 1.5, 0.529526975338513],
        [-0.9, -0.4, -0.3, 0.0336629761551959],
        [0.01, 0.01, 0.01, 0.828855732290258],
        [0, 0, 0, 0.84234037196832],
        [1.5, -1.4, 0.9, 2.19701576003708],
    ];
    for (let i = 0; i < 12; ++i) {
        const point = forward.multiply(new Vector3Dd(samples[i][0], samples[i][1], samples[i][2]));
        const distance = [0];
        const error = [0];
        expectEq(GeometryDistanceStatus.Valid, AffineEllipsoidDistance.query(point, 1, forward, distance, error));
        expectNear(samples[i][3], distance[0]!, 5e-7);
        expectLe(Math.abs(samples[i][3] - distance[0]!), error[0]! + 5e-12);
    }
});

test("EllipsoidDistanceContract_NumericalReferenceSurvivesRotationTranslationAndScale", () => {
    const rotation = Matrix4x4d.identityMatrix().withVal(0, 0, 0).withVal(0, 1, -1).withVal(1, 0, 1).withVal(1, 1, 0);
    const shape = Matrix4x4d.identityMatrix().withVal(0, 0, -2).withVal(0, 1, 0.5).withVal(1, 2, -0.3);
    const samples = [
        [0.12, 0.27, 0.31, 0.484219147169112],
        [0.1, -1.2, 0.5, 0.341598824284725],
        [0.01, 0.01, 0.01, 0.828855732290258],
        [-0.9, -0.4, -0.3, 0.0336629761551959],
    ];
    const factors = [0.01, 1, 100];
    for (let k = 0; k < 3; ++k) {
        const factor = factors[k];
        const forward = new Matrix4x4d()
            .translation(4, -3, 2)
            .multiply(rotation)
            .multiply(new Matrix4x4d().scale(factor, factor, factor))
            .multiply(shape);
        for (let i = 0; i < 4; ++i) {
            const point = forward.multiply(new Vector3Dd(samples[i][0], samples[i][1], samples[i][2]));
            const distance = [0];
            const error = [0];
            expectEq(GeometryDistanceStatus.Valid, AffineEllipsoidDistance.query(point, 1, forward, distance, error));
            expectNear(samples[i][3] * factor, distance[0]!, 5e-7 * factor);
        }
    }
});

test("RayAffineContract_CopiesPreserveUnchangedComponents", () => {
    const original = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(2, 0, 0), 1);
    const atExit = original.withT(2);
    expectDoubleEq(2, atExit.getDirection().x());
    expectDoubleEq(1, atExit.getOrigin().add(atExit.getDirection().multiply(atExit.getT())).x());
    expectDoubleEq(1, original.getT());
    const shifted = original.withOrigin(new Vector3Dd(-5, 0, 0));
    expectDoubleEq(2, shifted.getDirection().x());
    expectDoubleEq(1, shifted.getT());
    const redirected = original.withDirection(new Vector3Dd(4, 1, 0));
    expectDoubleEq(4, redirected.getDirection().x());
    expectDoubleEq(1, redirected.getDirection().y());
    expectDoubleEq(-3, redirected.getOrigin().x());
    expectDoubleEq(1, redirected.getT());
    const copied = Ray.copyOf(original);
    expectTrue(copied.equals(original));
    expectEq(0, Double.compare(original.withT(-0.0).getT(), -0.0));
});

test("RayAffineContract_ExplicitAffineConstructionPreservesLegacyConstructor", () => {
    const legacy = new Ray(new Vector3Dd(), new Vector3Dd(2, 0, 0));
    expectDoubleEq(1, legacy.getDirection().x());
    const affine = Ray.fromAffine(new Vector3Dd(), new Vector3Dd(2, 0, 0));
    expectDoubleEq(2, affine.getDirection().x());
    expectDoubleEq(0, affine.getT());
});

test("TriangleContract_SharedKernelPreservesAffineRayAndBarycentrics", () => {
    const a = new Vector3Dd(0, 0, 0);
    const b = new Vector3Dd(2, 0, 0);
    const c = new Vector3Dd(0, 2, 0);
    const ray = Ray.fromAffine(new Vector3Dd(0.5, 0.5, 4), new Vector3Dd(0, 0, -2));
    const t = [0];
    const u = [0];
    const v = [0];
    expectTrue(Triangle.intersectRay(ray, a, b, c, t, u, v));
    expectDoubleEq(2, t[0]!);
    expectDoubleEq(0.25, u[0]!);
    expectDoubleEq(0.25, v[0]!);
    const hit = Triangle.doIntersectionWithTriangle(ray, a, b, c);
    expectNe(null, hit);
    expectDoubleEq(t[0]!, hit!.getT());
    expectTrue(Triangle.intersectRay(ray, a, c, b, t, u, v));
    expectFalse(
        Triangle.intersectRay(Ray.fromAffine(new Vector3Dd(3, 3, 1), new Vector3Dd(0, 0, -1)), a, b, c, t, u, v),
    );
    expectFalse(
        Triangle.intersectRay(Ray.fromAffine(new Vector3Dd(0.5, 0.5, 1), new Vector3Dd(1, 0, 0)), a, b, c, t, u, v),
    );
    expectFalse(Triangle.intersectRay(ray, a, a, c, t, u, v));
});

test("TriangleContract_ProjectedContainmentAndWindingWorkOnEveryAxis", () => {
    const first = new Vector3Dd(0, 0, 0);
    const second = [new Vector3Dd(0, 2, 0), new Vector3Dd(2, 0, 0), new Vector3Dd(2, 0, 0)];
    const third = [new Vector3Dd(0, 0, 2), new Vector3Dd(0, 0, 2), new Vector3Dd(0, 2, 0)];
    const inside = [new Vector3Dd(0, 0.5, 0.5), new Vector3Dd(0.5, 0, 0.5), new Vector3Dd(0.5, 0.5, 0)];
    const outside = [new Vector3Dd(0, 2, 2), new Vector3Dd(2, 0, 2), new Vector3Dd(2, 2, 0)];
    for (let i = 0; i < 3; ++i) {
        for (let reversed = 0; reversed < 2; ++reversed) {
            let a = first;
            let b = reversed ? third[i] : second[i];
            const c = reversed ? second[i] : third[i];
            const normal = [new Vector3Dd()];
            const distance = [0];
            expectTrue(Triangle.supportingPlane(a, b, c, 1e-9, normal, distance));
            const axis = Triangle.dominantProjection(normal[0]!);
            expectEq(i, axis);
            if (Triangle.requiresProjectionSwap(a, b, c, axis)) {
                const temporary = a;
                a = b;
                b = temporary;
            }
            expectTrue(Triangle.containsProjected(a, b, c, inside[i], axis));
            expectFalse(Triangle.containsProjected(a, b, c, outside[i], axis));
            expectTrue(Triangle.containsProjected(a, b, c, a, axis));
        }
    }
    const normal = [new Vector3Dd()];
    const distance = [0];
    expectFalse(Triangle.supportingPlane(first, first, first, 1e-9, normal, distance));
    expectFalse(Triangle.supportingPlane(first, first, first, 0, normal, distance));
});

test("SphereFirstHitContract_FirstHitCrossingsAndDetailUseSameParameter", () => {
    const sphere = new Sphere(1);
    const ray = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(2, 0, 0));
    const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
    expectTrue(sphere.doIntersectionFirstHit(ray, hit));
    expectDoubleEq(1, hit.getHitDistance());
    expectDoubleEq(-1, hit.point.x());
    expectDoubleEq(-1, hit.normal.x());
    expectNe(null, hit.getRay());
    expectDoubleEq(2, hit.getRay()!.getDirection().x());
    const crossings = new RayCrossingTestCollector();
    sphere.emitRayCrossings(ray, new RayCrossingQuery(0, 10), crossings);
    expectEq(2, crossings.roots.size());
    expectDoubleEq(crossings.roots.get(0), hit.getHitDistance());
    const pointerHit = sphere.doIntersectionFirstHit(ray);
    expectNe(null, pointerHit);
    expectDoubleEq(hit.getHitDistance(), pointerHit!.getT());
    expectDoubleEq(2, pointerHit!.getDirection().x());
});

test("SphereFirstHitContract_InteriorExitAndClippedFarRoot", () => {
    const sphere = new Sphere(1);
    let ray = Ray.fromAffine(new Vector3Dd(), new Vector3Dd(2, 0, 0));
    const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
    expectTrue(sphere.doIntersectionFirstHit(ray, hit));
    expectDoubleEq(0.5, hit.getHitDistance());
    expectDoubleEq(1, hit.point.x());
    expectDoubleEq(1, hit.normal.x());
    ray = ray.withOrigin(new Vector3Dd(-3, 0, 0));
    expectTrue(sphere.doIntersectionFirstHit(ray, new RayCrossingQuery(1.25, 2), hit));
    expectDoubleEq(2, hit.getHitDistance());
    expectDoubleEq(1, hit.point.x());
    expectFalse(sphere.doIntersectionFirstHit(ray, new RayCrossingQuery(1.25, 1.75), hit));
    expectTrue(sphere.doIntersectionFirstHit(ray, new RayCrossingQuery(2, 2), null));
    expectFalse(sphere.doIntersectionFirstHit(ray, new RayCrossingQuery(3, 2), hit));
});

test("SphereFirstHitContract_TangencyAndDistanceOnly", () => {
    const sphere = new Sphere(1);
    let ray = Ray.fromAffine(new Vector3Dd(-2, 1, 0), new Vector3Dd(1, 0, 0));
    const hit = new RayHit(RayHit.DETAIL_NONE, false);
    expectTrue(sphere.doIntersectionFirstHit(ray, hit));
    expectDoubleEq(2, hit.getHitDistance());
    expectEq(null, hit.getRay());
    ray = ray.withDirection(new Vector3Dd());
    expectFalse(sphere.doIntersectionFirstHit(ray, hit));
    const oblique = new Sphere(Math.sqrt(5.0));
    ray = Ray.fromAffine(new Vector3Dd(-6, -3, 0), new Vector3Dd(2, 1, 0));
    expectTrue(oblique.doIntersectionFirstHit(ray, hit));
    expectNear(2, hit.getHitDistance(), 1e-12);
});

test("SphereFirstHitContract_SceneFastPathAndDetailAgreeForAffineRay", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setPosition(new Vector3Dd(10, 0, 0));
    let ray = Ray.fromAffine(new Vector3Dd(7, 0, 0), new Vector3Dd(2, 0, 0));
    const distance = new RayHit(RayHit.DETAIL_NONE, false);
    const detail = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
    expectTrue(body.doIntersectionFirstHit(ray, distance));
    expectTrue(body.doIntersectionFirstHit(ray, detail));
    expectDoubleEq(1, distance.getHitDistance());
    expectDoubleEq(distance.getHitDistance(), detail.getHitDistance());
    expectDoubleEq(9, detail.point.x());
    expectDoubleEq(-1, detail.normal.x());
    expectNe(null, detail.getRay());
    expectDoubleEq(2, detail.getRay()!.getDirection().x());
    ray = ray.withOrigin(new Vector3Dd(10, 0, 0));
    expectTrue(body.doIntersectionFirstHit(ray, distance));
    expectTrue(body.doIntersectionFirstHit(ray, detail));
    expectDoubleEq(0.5, distance.getHitDistance());
    expectDoubleEq(distance.getHitDistance(), detail.getHitDistance());
    expectDoubleEq(11, detail.point.x());
});

test("SphereFirstHitContract_NegativeNonuniformScalePreservesWorldParameter", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setPosition(new Vector3Dd(10, 0, 0));
    body.setScale(new Vector3Dd(-2, 3, 1));
    let ray = Ray.fromAffine(new Vector3Dd(4, 0, 0), new Vector3Dd(2, 0, 0));
    const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
    expectTrue(body.doIntersectionFirstHit(ray, hit));
    expectDoubleEq(2, hit.getHitDistance());
    expectDoubleEq(8, hit.point.x());
    expectDoubleEq(-1, hit.normal.x());
    expectNe(null, hit.getRay());
    expectDoubleEq(2, hit.getRay()!.getDirection().x());
    ray = ray.withOrigin(new Vector3Dd(10, 0, 0));
    expectTrue(body.doIntersectionFirstHit(ray, hit));
    expectDoubleEq(1, hit.getHitDistance());
    expectDoubleEq(12, hit.point.x());
    expectDoubleEq(1, hit.normal.x());
});

test("SphereFirstHitContract_InwardShadowBiasFindsTheAnalyticExit", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    const surfacePoint = new Vector3Dd(1, 0, 0);
    const lightDirection = new Vector3Dd(-0.1, 1, 0).normalized();
    const bias = 1e-6;
    const shadowRay = Ray.fromAffine(surfacePoint.add(lightDirection.multiply(bias)), lightDirection);
    expectEq(Geometry.INSIDE, sphere.doContainmentTest(shadowRay.getOrigin(), 0));
    const hit = new RayHit(RayHit.DETAIL_NONE, false);
    expectTrue(body.doIntersectionFirstHit(shadowRay, hit));
    // On the unit sphere, the other chord endpoint lies at -2*(p dot d).
    const expectedExit = -2 * surfacePoint.dotProduct(lightDirection) - bias;
    expectNear(expectedExit, hit.getHitDistance(), 1e-12);
    expectGt(hit.getHitDistance(), bias);
});

test("SphereDetailContract_RequestedAndAvailableAreDistinctAndResettable", () => {
    const sphere = new Sphere(1);
    let ray = Ray.fromAffine(new Vector3Dd(0, 0, 2), new Vector3Dd(0, 0, -1));
    const hit = new RayHit(RayHit.DETAIL_ALL);
    expectFalse(hit.isDetailAvailabilityKnown());
    expectTrue(sphere.doIntersectionFirstHit(ray, hit));
    expectTrue(hit.isDetailAvailabilityKnown());
    expectEq(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, hit.getAvailableDetailMask());
    expectEq(RayHit.DETAIL_ALL, hit.getRequiredDetailMask());
    const copied = new RayHit();
    copied.clone(hit);
    expectTrue(copied.isDetailAvailabilityKnown());
    expectEq(hit.getAvailableDetailMask(), copied.getAvailableDetailMask());
    hit.resetForDistanceOnly();
    expectFalse(hit.isDetailAvailabilityKnown());
    expectEq(RayHit.DETAIL_NONE, hit.getAvailableDetailMask());
    expectTrue(copied.isDetailAvailabilityKnown());
});

test("GeometryQueryContract_DistinguishesUnsupportedMissAndInvalidInput", () => {
    const sphere = new Sphere(1);
    const geometry = sphere;
    let ray = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(2, 0, 0));
    const workspace = new GeometryQueryWorkspace();
    const hit = workspace.hitResult();
    hit.reset(RayHit.DETAIL_POINT);
    const firstBoundary = new RayBoundary();
    expectEq(RayFirstHitStatus.Hit, geometry.queryFirstHit(ray, new RayCrossingQuery(1.25, 2), hit, firstBoundary));
    expectEq(sphere, firstBoundary.geometry);
    expectDoubleEq(2, firstBoundary.t);
    expectFalse(firstBoundary.normalReversed);
    expectDoubleEq(2, hit.getHitDistance());
    expectDoubleEq(1, hit.point.x());
    expectTrue(hit.isDetailAvailabilityKnown());
    expectEq(RayHit.DETAIL_POINT, hit.getAvailableDetailMask());
    expectEq(RayFirstHitStatus.Miss, geometry.queryFirstHit(ray, new RayCrossingQuery(1.25, 1.75), hit, firstBoundary));
    expectEq(null, firstBoundary.geometry);
    expectFalse(hit.hasHitDistance());
    expectEq(RayFirstHitStatus.InvalidInput, geometry.queryFirstHit(ray, new RayCrossingQuery(2, 1), hit));
    ray = ray.withDirection(new Vector3Dd(0, 0, 0));
    expectEq(RayFirstHitStatus.InvalidInput, geometry.queryFirstHit(ray, new RayCrossingQuery(0, 3), hit));
    ray = ray.withDirection(new Vector3Dd(1e300, 0, 0));
    expectEq(RayFirstHitStatus.Indeterminate, geometry.queryFirstHit(ray, new RayCrossingQuery(0, 3), hit));
    const unsupported = new Torus(2, 1);
    expectEq(RayFirstHitStatus.Unsupported, unsupported.queryFirstHit(ray, new RayCrossingQuery(0, 3), hit));
});

test("GeometryQueryContract_WorkspaceAndExtensibleProvenanceAreIndependent", () => {
    const sphere = new Sphere(1);
    const instance = new SimpleBody();
    instance.setGeometryReference(sphere);
    const workspace = new GeometryQueryWorkspace();
    const ray = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(2, 0, 0));
    expectEq(
        RaySolidQueryStatus.Supported,
        sphere.classifyRay(ray, new RayCrossingQuery(0, 3), workspace.solidResult()),
    );
    expectEq(2, workspace.solidResult().boundaries.size());
    const boundary = workspace.solidResult().boundaries.get(0);
    expectEq(sphere, boundary.geometry);
    expectEq(-1, boundary.getProvenance().getSurfaceIndex());
    expectTrue(boundary.getProvenance().setSurfaceIndex(7));
    expectFalse(boundary.getProvenance().setSurfaceIndex(-2));
    boundary.normalReversed = true;
    const forward = new Matrix4x4d().translation(1, 2, 3);
    const inverse = new Matrix4x4d().translation(-1, -2, -3);
    const step = new RayInstanceStep(instance, forward, inverse);
    for (let i = 0; i < 32; i++) expectTrue(boundary.appendInstance(step));
    expectFalse(boundary.appendInstance(new RayInstanceStep()));
    expectEq(32, boundary.getInstanceCount());
    const copied = new RaySolidBoundary(boundary);
    boundary.clearInstances();
    expectEq(7, boundary.getProvenance().getSurfaceIndex());
    expectEq(32, copied.getInstanceCount());
    expectEq(7, copied.getProvenance().getSurfaceIndex());
    expectTrue(copied.normalReversed);
    const recovered = new RayInstanceStep();
    expectTrue(copied.getInstance(31, recovered));
    expectEq(instance, recovered.instance);
    expectDoubleEq(1, recovered.childToParent.multiply(new Vector3Dd()).x());
    expectDoubleEq(0, recovered.parentToChild.multiply(recovered.childToParent.multiply(new Vector3Dd())).x());
    expectFalse(copied.getInstance(32, recovered));
    workspace.clear();
    expectTrue(workspace.solidResult().boundaries.isEmpty());
    expectEq(GeometryBounds.Kind.Unsupported, workspace.boundsResult().getKind());
    expectFalse(workspace.hitResult().hasHitDistance());
    expectEq(32, copied.getInstanceCount());
});

test("GeometryQueryContract_SceneInstancePreservesAffineParameterAndProvenance", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setPosition(new Vector3Dd(10, 0, 0));
    body.setScale(new Vector3Dd(-2, 3, 1));
    const ray = Ray.fromAffine(new Vector3Dd(4, 0, 0), new Vector3Dd(2, 0, 0));
    const workspace = new GeometryQueryWorkspace();
    expectEq(RaySolidQueryStatus.Supported, body.classifyRay(ray, new RayCrossingQuery(0, 5), workspace.solidResult()));
    expectEq(2, workspace.solidResult().boundaries.size());
    expectDoubleEq(2, workspace.solidResult().boundaries.get(0)!.t);
    expectDoubleEq(4, workspace.solidResult().boundaries.get(1)!.t);
    expectEq(sphere, workspace.solidResult().boundaries.get(0)!.geometry);
    const step = new RayInstanceStep();
    expectTrue(workspace.solidResult().boundaries.get(0)!.getInstance(0, step));
    expectEq(body, step.instance);
    const worldPoint = ray.getOrigin().add(ray.getDirection().multiply(2));
    const localPoint = step.parentToChild.multiply(worldPoint);
    expectDoubleEq(1, localPoint.x());
    expectDoubleEq(worldPoint.x(), step.childToParent.multiply(localPoint).x());
    body.setScale(new Vector3Dd(0, 3, 1));
    expectEq(
        RaySolidQueryStatus.InvalidInput,
        body.classifyRay(ray, new RayCrossingQuery(0, 5), workspace.solidResult()),
    );
    expectTrue(workspace.solidResult().boundaries.isEmpty());
});

test("BoxContract_InstancePreservesAffineParameterAndProvenance", () => {
    const box = new Box(2, 2, 2);
    const body = new SimpleBody();
    body.setGeometryReference(box);
    body.setPosition(new Vector3Dd(10, 0, 0));
    body.setScale(new Vector3Dd(-2, 3, 1));
    const ray = Ray.fromAffine(new Vector3Dd(4, 0, 0), new Vector3Dd(2, 0, 0));
    const workspace = new GeometryQueryWorkspace();
    expectEq(RaySolidQueryStatus.Supported, body.classifyRay(ray, new RayCrossingQuery(0, 5), workspace.solidResult()));
    expectEq(2, workspace.solidResult().boundaries.size());
    expectDoubleEq(2, workspace.solidResult().boundaries.get(0)!.t);
    expectDoubleEq(4, workspace.solidResult().boundaries.get(1)!.t);
    expectEq(box, workspace.solidResult().boundaries.get(0)!.geometry);
    const step = new RayInstanceStep();
    expectTrue(workspace.solidResult().boundaries.get(0)!.getInstance(0, step));
    expectEq(body, step.instance);
    // Certified under the reflected nonuniform placement: world x in [8,12],
    // y in [-3,3], z in [-1,1].
    expectEq(GeometryContainment.Inside, body.classifyPoint(new Vector3Dd(10, 0, 0), 0.1));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(12.05, 0, 0), 0.1));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(10, 2.95, 0), 0.1));
    expectEq(GeometryContainment.Outside, body.classifyPoint(new Vector3Dd(7.8, 0, 0), 0.1));
});

test("PlanarHalfSpaceContract_ReflectedInstanceKeepsBoundaryAndInterior", () => {
    const half = new PlanarHalfSpace(1, 0, 0, -1);
    const body = new SimpleBody();
    body.setGeometryReference(half);
    body.setScale(new Vector3Dd(-2, 3, 1));
    const ray = Ray.fromAffine(new Vector3Dd(-4, 0, 0), new Vector3Dd(1, 0, 0));
    const workspace = new GeometryQueryWorkspace();
    expectEq(RaySolidQueryStatus.Supported, body.classifyRay(ray, new RayCrossingQuery(0, 5), workspace.solidResult()));
    expectEq(1, workspace.solidResult().boundaries.size());
    expectDoubleEq(2, workspace.solidResult().boundaries.get(0)!.t);
    expectEq(half, workspace.solidResult().boundaries.get(0)!.geometry);
    expectEq(2, workspace.solidResult().insideSegments.size());
    expectFalse(workspace.solidResult().insideSegments.get(0));
    expectTrue(workspace.solidResult().insideSegments.get(1));
    const step = new RayInstanceStep();
    expectTrue(workspace.solidResult().boundaries.get(0)!.getInstance(0, step));
    expectEq(body, step.instance);
});

test("GeometryQueryContract_PointContainmentSeparatesBoundaryAndUnsupported", () => {
    const sphere = new Sphere(1);
    const geometry = sphere;
    expectEq(GeometryContainment.Inside, geometry.classifyPoint(new Vector3Dd(0, 0, 0), 0));
    expectEq(GeometryContainment.Boundary, geometry.classifyPoint(new Vector3Dd(1, 0, 0), 0));
    expectEq(GeometryContainment.Boundary, geometry.classifyPoint(new Vector3Dd(1.01, 0, 0), 0.02));
    expectEq(GeometryContainment.Outside, geometry.classifyPoint(new Vector3Dd(2, 0, 0), 0));
    expectEq(GeometryContainment.InvalidInput, geometry.classifyPoint(new Vector3Dd(), -1));
    expectEq(GeometryContainment.InvalidInput, geometry.classifyPoint(new Vector3Dd(), Double.NaN));
    expectEq(GeometryContainment.Indeterminate, geometry.classifyPoint(new Vector3Dd(1e300, 0, 0), 0));
    const tiny = new Sphere(1e-300);
    expectEq(GeometryContainment.Indeterminate, tiny.classifyPoint(new Vector3Dd(2e-300, 0, 0), 0));
    const unsupported = new Torus(2, 1);
    expectEq(GeometryContainment.Unsupported, unsupported.classifyPoint(new Vector3Dd(), 0));
});

test("GeometryQueryContract_CrossingFailuresAreNotReportedAsMisses", () => {
    const sphere = new Sphere(1);
    const crossings = new RayCrossingTestCollector();
    let ray = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(0, 0, 0));
    expectEq(RayCrossingSupport.InvalidInput, sphere.emitRayCrossings(ray, new RayCrossingQuery(0, 3), crossings));
    ray = ray.withDirection(new Vector3Dd(1e300, 0, 0));
    expectEq(RayCrossingSupport.Indeterminate, sphere.emitRayCrossings(ray, new RayCrossingQuery(0, 3), crossings));
    expectTrue(crossings.roots.isEmpty());
});

test("GeometryQueryContract_ShearRoundTripPreservesTheRayParameter", () => {
    const sphere = new Sphere(1);
    const shear = Matrix4x4d.identityMatrix().withVal(0, 1, 0.5);
    const inverse = Matrix4x4d.identityMatrix().withVal(0, 1, -0.5);
    const local = Ray.fromAffine(new Vector3Dd(-3, 0.5, 0), new Vector3Dd(2, 0, 0));
    const world = Ray.fromAffine(
        shear.multiply(local.getOrigin()),
        shear.withoutTranslation().multiply(local.getDirection()),
    );
    const recovered = Ray.fromAffine(
        inverse.multiply(world.getOrigin()),
        inverse.withoutTranslation().multiply(world.getDirection()),
    );
    const localHit = new RayHit(RayHit.DETAIL_POINT, false);
    const recoveredHit = new RayHit(RayHit.DETAIL_POINT, false);
    expectEq(RayFirstHitStatus.Hit, sphere.queryFirstHit(local, new RayCrossingQuery(0, 4), localHit));
    expectEq(RayFirstHitStatus.Hit, sphere.queryFirstHit(recovered, new RayCrossingQuery(0, 4), recoveredHit));
    expectDoubleEq(localHit.getHitDistance(), recoveredHit.getHitDistance());
    const expected = shear.multiply(localHit.point);
    const worldAtT = world.getOrigin().add(world.getDirection().multiply(recoveredHit.getHitDistance()));
    expectDoubleEq(expected.x(), worldAtT.x());
    expectDoubleEq(expected.y(), worldAtT.y());
});

test("GeometryQueryContract_SurfaceEventsUseTheSameExtensibleProvenanceModel", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    const event = new RayCrossing(1, sphere, RayCrossingKind.Entering);
    const step = new RayInstanceStep(body, new Matrix4x4d(), new Matrix4x4d());
    expectTrue(event.getProvenance().setSurfaceIndex(3));
    expectTrue(event.getProvenance().appendInstance(step));
    event.normalReversed = true;
    const copy = new RayCrossing(event);
    expectTrue(copy.normalReversed);
    event.getProvenance().clear();
    expectEq(-1, event.getProvenance().getSurfaceIndex());
    expectEq(3, copy.getProvenance().getSurfaceIndex());
    expectEq(1, copy.getProvenance().getInstanceCount());
    const restored = new RayInstanceStep();
    expectTrue(copy.getProvenance().getInstance(0, restored));
    expectEq(body, restored.instance);
});

test("GeometryQueryContract_SeparateWorkerWorkspacesCanRunConcurrently", () => {
    const sphere = new Sphere(1);
    const first = new GeometryWorkspaceTestWorker(sphere);
    const second = new GeometryWorkspaceTestWorker(sphere);
    // JavaScript has no shared-memory threads for this object graph; the two
    // workers run one after the other, each with its own workspace.
    first.run();
    second.run();
    expectTrue(first.passed());
    expectTrue(second.passed());
    expectNe(null, first.boundaryStorage());
    expectNe(null, second.boundaryStorage());
    expectNe(first.boundaryStorage(), second.boundaryStorage());
});

test("GeometryQueryContract_NestedWorkspaceScratchIsStableAndRequiresLifoRelease", () => {
    const workspace = new GeometryQueryWorkspace();
    const acquired = new ArrayList<RaySolidClassification>();
    for (let i = 0; i < 32; i++) {
        const slot = workspace.acquireSolidScratch();
        expectNe(null, slot);
        slot!.insideSegments.add(true);
        acquired.add(slot!);
    }
    expectFalse(workspace.releaseSolidScratch(acquired.get(0)!));
    expectTrue(acquired.get(0)!.insideSegments.get(0));
    for (let i = 31; i >= 0; i--) expectTrue(workspace.releaseSolidScratch(acquired.get(i)!));
    const reused = workspace.acquireSolidScratch();
    expectEq(acquired.get(0)!, reused);
    expectTrue(reused!.insideSegments.isEmpty());
    workspace.clear();
    expectFalse(workspace.releaseSolidScratch(reused));
    expectEq(reused, workspace.acquireSolidScratch());
});

test("GeometryQueryContract_SceneContainmentHasExplicitWorldTolerancePolicy", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setPosition(new Vector3Dd(10, 0, 0));
    body.setScale(new Vector3Dd(-2, 3, 1));
    expectEq(GeometryContainment.Inside, body.classifyPoint(new Vector3Dd(10, 0, 0), 0));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(8, 0, 0), 0));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(8.05, 0, 0), 0.1));
    body.setScale(new Vector3Dd(-2, -2, -2));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(7.95, 0, 0), 0.1));
    expectEq(GeometryContainment.InvalidInput, body.classifyPoint(new Vector3Dd(), -1));
    body.setScale(new Vector3Dd(0, 2, 2));
    expectEq(GeometryContainment.InvalidInput, body.classifyPoint(new Vector3Dd(), 0));
    body.setScale(new Vector3Dd(1e-12, 2, 2));
    expectEq(GeometryContainment.Indeterminate, body.classifyPoint(new Vector3Dd(), 0));
    const workspace = new GeometryQueryWorkspace();
    const ray = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0));
    expectEq(
        RaySolidQueryStatus.Indeterminate,
        body.classifyRay(ray, new RayCrossingQuery(0, 5), workspace.solidResult()),
    );
    body.setScale(new Vector3Dd(Double.POSITIVE_INFINITY, 2, 2));
    expectEq(GeometryContainment.InvalidInput, body.classifyPoint(new Vector3Dd(), 0));
    expectEq(
        RaySolidQueryStatus.InvalidInput,
        body.classifyRay(ray, new RayCrossingQuery(0, 5), workspace.solidResult()),
    );
    body.setScale(new Vector3Dd(1, 1, 1));
    body.setRotation(Matrix4x4d.identityMatrix().withVal(0, 0, 0));
    expectEq(GeometryContainment.Indeterminate, body.classifyPoint(new Vector3Dd(), 0));
    expectEq(
        RaySolidQueryStatus.Indeterminate,
        body.classifyRay(ray, new RayCrossingQuery(0, 5), workspace.solidResult()),
    );
    body.setRotation(Matrix4x4d.identityMatrix().withVal(0, 1, 0.5));
    expectEq(GeometryContainment.Indeterminate, body.classifyPoint(new Vector3Dd(), 0));
    expectEq(
        RaySolidQueryStatus.Indeterminate,
        body.classifyRay(ray, new RayCrossingQuery(0, 5), workspace.solidResult()),
    );
    body.setRotation(new Matrix4x4d());
    body.setPosition(new Vector3Dd(Double.POSITIVE_INFINITY, 0, 0));
    expectEq(GeometryContainment.InvalidInput, body.classifyPoint(new Vector3Dd(), 0));
    expectEq(
        RaySolidQueryStatus.InvalidInput,
        body.classifyRay(ray, new RayCrossingQuery(0, 5), workspace.solidResult()),
    );
    body.setPosition(new Vector3Dd(1e308, 0, 0));
    body.setScale(new Vector3Dd(1e-5, 1, 1));
    expectEq(GeometryContainment.Indeterminate, body.classifyPoint(new Vector3Dd(), 0));
    expectEq(
        RaySolidQueryStatus.Indeterminate,
        body.classifyRay(ray, new RayCrossingQuery(0, 5), workspace.solidResult()),
    );
});

test("GeometryQueryContract_EllipsoidUsesWorldSpaceToleranceOnMultipleAxes", () => {
    const sphere = new Sphere(1);
    const ellipsoid = new SimpleBody();
    ellipsoid.setGeometryReference(sphere);
    ellipsoid.setPosition(new Vector3Dd(10, 0, 0));
    ellipsoid.setScale(new Vector3Dd(3, 2, 1));
    const tolerance = 0.1;
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPoint(new Vector3Dd(10, 0, 0), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(12.95, 0, 0), tolerance));
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPoint(new Vector3Dd(12.8, 0, 0), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(13.05, 0, 0), tolerance));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPoint(new Vector3Dd(13.2, 0, 0), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(10, 0, 0.95), tolerance));
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPoint(new Vector3Dd(10, 0, 0.8), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(10, 0, 1.05), tolerance));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPoint(new Vector3Dd(10, 0, 1.2), tolerance));
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPointCertified(new Vector3Dd(10, 0, 0), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPointCertified(new Vector3Dd(12.95, 0, 0), tolerance));
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPointCertified(new Vector3Dd(12.8, 0, 0), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPointCertified(new Vector3Dd(13.05, 0, 0), tolerance));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPointCertified(new Vector3Dd(13.2, 0, 0), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPointCertified(new Vector3Dd(10, 0, 0.95), tolerance));
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPointCertified(new Vector3Dd(10, 0, 0.8), tolerance));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPointCertified(new Vector3Dd(10, 0, 1.05), tolerance));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPointCertified(new Vector3Dd(10, 0, 1.2), tolerance));
    // Exactly on the tolerance threshold, floating point comparisons must not
    // silently force either boundary or outside when the certificate is unsure.
    expectEq(GeometryContainment.Indeterminate, ellipsoid.classifyPoint(new Vector3Dd(13.1, 0, 0), tolerance));
    ellipsoid.setScale(new Vector3Dd(-3, 2, 1));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(13.05, 0, 0), tolerance));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPoint(new Vector3Dd(13.2, 0, 0), tolerance));
    ellipsoid.setScale(new Vector3Dd(0, 2, 1));
    expectEq(GeometryContainment.InvalidInput, ellipsoid.classifyPoint(new Vector3Dd(10, 0, 0), tolerance));
    ellipsoid.setScale(new Vector3Dd(1e-12, 2, 1));
    expectEq(GeometryContainment.Indeterminate, ellipsoid.classifyPoint(new Vector3Dd(10, 0, 0), tolerance));
});

test("GeometryQueryContract_ShearedEllipsoidUsesWorldSpaceNormalDistance", () => {
    const sphere = new Sphere(1);
    const ellipsoid = new SimpleBody();
    ellipsoid.setGeometryReference(sphere);
    const forward = new Matrix4x4d()
        .translation(3, 0, 0)
        .multiply(Matrix4x4d.identityMatrix().withVal(0, 1, 0.5))
        .multiply(new Matrix4x4d().scale(2, 1, 1));
    const inverse = new Matrix4x4d()
        .scale(0.5, 1, 1)
        .multiply(Matrix4x4d.identityMatrix().withVal(0, 1, -0.5))
        .multiply(new Matrix4x4d().translation(-3, 0, 0));
    expectEq(AffinePlacementStatus.Valid, ellipsoid.setAffinePlacement(forward, inverse));
    // At the transformed local point (0, 1, 0), the world normal is (0, 1, 0).
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(3.5, 1.05, 0), 0.1));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPoint(new Vector3Dd(3.5, 1.2, 0), 0.1));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(3.5, 0.95, 0), 0.1));
});

test("GeometryQueryContract_EllipsoidObliqueNormalUsesAWorldSpaceWitness", () => {
    const sphere = new Sphere(1);
    const ellipsoid = new SimpleBody();
    ellipsoid.setGeometryReference(sphere);
    ellipsoid.setScale(new Vector3Dd(3, 2, 1));
    // (1.8, 1.6, 0) lies on the surface; (1, 2, 0)/sqrt(5) is its
    // outward unit normal. Both offsets have an explicit surface witness.
    const nx = 1.0 / Math.sqrt(5.0);
    const ny = 2.0 * nx;
    expectEq(
        GeometryContainment.Boundary,
        ellipsoid.classifyPoint(new Vector3Dd(1.8 + 0.095 * nx, 1.6 + 0.095 * ny, 0), 0.1),
    );
    expectEq(
        GeometryContainment.Boundary,
        ellipsoid.classifyPoint(new Vector3Dd(1.8 - 0.095 * nx, 1.6 - 0.095 * ny, 0), 0.1),
    );
    expectEq(
        GeometryContainment.Outside,
        ellipsoid.classifyPoint(new Vector3Dd(1.8 + 0.101 * nx, 1.6 + 0.101 * ny, 0), 0.1),
    );
});

test("GeometryQueryContract_EllipsoidInteriorNearToleranceIsNotBoundary", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setScale(new Vector3Dd(3, 2, 1));
    // For x > 3 - 1/3 the nearest surface point lies at (3,0,0),
    // so this interior point is 0.2 world units from the surface.
    expectEq(GeometryContainment.Inside, body.classifyPointCertified(new Vector3Dd(2.8, 0, 0), 0.19));
    expectEq(GeometryContainment.Inside, body.classifyPoint(new Vector3Dd(2.8, 0, 0), 0.19));
});

test("GeometryQueryContract_EllipsoidCenterCanReachBoundaryWithPositiveTolerance", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    body.setScale(new Vector3Dd(3, 2, 1));
    expectEq(GeometryContainment.Inside, body.classifyPoint(new Vector3Dd(), 0.9));
    expectEq(GeometryContainment.Boundary, body.classifyPointCertified(new Vector3Dd(), 1.1));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(), 1.1));
    expectEq(GeometryContainment.Indeterminate, body.classifyPointCertified(new Vector3Dd(), 1.0));
    expectEq(GeometryContainment.Indeterminate, body.classifyPoint(new Vector3Dd(), 1.0));
});

test("GeometryQueryContract_ExtremeAffineEllipsoidCertifiesBoundaryWhenPossible", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    const forward = new Matrix4x4d().scale(1e-12, 1, 1);
    const inverse = new Matrix4x4d().scale(1e12, 1, 1);
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward, inverse));
    expectEq(GeometryContainment.Inside, body.classifyPoint(new Vector3Dd(), 1e-15));
    expectEq(GeometryContainment.Boundary, body.classifyPointCertified(new Vector3Dd(1.05e-12, 0, 0), 1e-13));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(1.05e-12, 0, 0), 1e-13));
    expectEq(GeometryContainment.Inside, body.classifyPoint(new Vector3Dd(), 0));
});

test("GeometryQueryContract_AffineEllipsoidAgreesWithIndependentSurfaceGrid", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    const forward = Matrix4x4d.identityMatrix().withVal(0, 0, 2).withVal(0, 1, 0.5);
    const inverse = Matrix4x4d.identityMatrix().withVal(0, 0, 0.5).withVal(0, 1, -0.25);
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward, inverse));
    const points = [
        new Vector3Dd(0, 0, 0),
        new Vector3Dd(1.1, 0.2, 0.1),
        new Vector3Dd(2.4, 0.3, 0.2),
        new Vector3Dd(-1.8, 0.9, 0.4),
        new Vector3Dd(0.5, -0.3, 1.4),
    ];
    let definite = 0;
    for (let index = 0; index < 5; ++index) {
        let sampled = Double.MAX_VALUE;
        for (let latitude = 0; latitude <= 180; ++latitude) {
            const theta = (Math.PI * latitude) / 180;
            for (let longitude = 0; longitude < 360; ++longitude) {
                const phi = (2 * Math.PI * longitude) / 360;
                const u = new Vector3Dd(
                    Math.sin(theta) * Math.cos(phi),
                    Math.sin(theta) * Math.sin(phi),
                    Math.cos(theta),
                );
                const distance = forward.multiply(u).subtract(points[index]).length();
                if (distance < sampled) sampled = distance;
            }
        }
        // Every surface point is within 0.05 world units of a grid point:
        // ||A||_F < 2.6, and the sum of half angular steps is pi/180.
        const errorBound = 0.05;
        const nearResult = body.classifyPoint(points[index], sampled + errorBound);
        expectTrue(nearResult === GeometryContainment.Boundary || nearResult === GeometryContainment.Indeterminate);
        if (nearResult === GeometryContainment.Boundary) ++definite;
        if (sampled > 2 * errorBound) {
            const side = body.classifyPoint(points[index], 0);
            const farResult = body.classifyPoint(points[index], sampled - 2 * errorBound);
            expectTrue(farResult === side || farResult === GeometryContainment.Indeterminate);
            if (farResult === side) ++definite;
        }
    }
    expectGe(definite, 5);
});

test("GeometryQueryContract_SurfaceGridChecksReflectionsShearAndScaleRanges", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    const rotation = Matrix4x4d.identityMatrix().withVal(0, 0, 0).withVal(0, 1, -1).withVal(1, 0, 1).withVal(1, 1, 0);
    const inverseRotation = Matrix4x4d.identityMatrix()
        .withVal(0, 0, 0)
        .withVal(0, 1, 1)
        .withVal(1, 0, -1)
        .withVal(1, 1, 0);
    const shear = Matrix4x4d.identityMatrix().withVal(0, 1, 0.5).withVal(1, 2, -0.3);
    const inverseShear = Matrix4x4d.identityMatrix().withVal(0, 1, -0.5).withVal(1, 2, 0.3).withVal(0, 2, -0.15);
    const localPoints = [
        new Vector3Dd(),
        new Vector3Dd(0.6, 0.2, 0.1),
        new Vector3Dd(1.2, 0.4, 0.3),
        new Vector3Dd(-0.8, 0.9, 0.4),
        new Vector3Dd(0.2, -0.4, 1.4),
    ];
    const scales = [0.01, 1, 100];
    let definite = 0;
    for (let mode = 0; mode < 3; ++mode) {
        const factor = scales[mode];
        const forward = new Matrix4x4d()
            .translation(4, -3, 2)
            .multiply(rotation)
            .multiply(shear)
            .multiply(new Matrix4x4d().scale(-2 * factor, factor, 0.7 * factor));
        const inverse = new Matrix4x4d()
            .scale(-0.5 / factor, 1 / factor, 1 / (0.7 * factor))
            .multiply(inverseShear)
            .multiply(inverseRotation)
            .multiply(new Matrix4x4d().translation(-4, 3, -2));
        expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward, inverse));
        // ||A||_F < 3*factor; angular grid half-step sum is pi/180.
        const errorBound = 0.055 * factor;
        for (let index = 0; index < 5; ++index) {
            const point = forward.multiply(localPoints[index]);
            let sampled = Double.MAX_VALUE;
            for (let latitude = 0; latitude <= 180; ++latitude) {
                const theta = (Math.PI * latitude) / 180;
                for (let longitude = 0; longitude < 360; ++longitude) {
                    const phi = (2 * Math.PI * longitude) / 360;
                    const unit = new Vector3Dd(
                        Math.sin(theta) * Math.cos(phi),
                        Math.sin(theta) * Math.sin(phi),
                        Math.cos(theta),
                    );
                    const distance = forward.multiply(unit).subtract(point).length();
                    if (distance < sampled) sampled = distance;
                }
            }
            const nearResult = body.classifyPoint(point, sampled + errorBound);
            expectTrue(nearResult === GeometryContainment.Boundary || nearResult === GeometryContainment.Indeterminate);
            if (nearResult === GeometryContainment.Boundary) ++definite;
            if (sampled > 2 * errorBound) {
                const side =
                    localPoints[index].dotProduct(localPoints[index]) < 1
                        ? GeometryContainment.Inside
                        : GeometryContainment.Outside;
                const farResult = body.classifyPoint(point, sampled - 2 * errorBound);
                expectTrue(farResult === side || farResult === GeometryContainment.Indeterminate);
                if (farResult === side) ++definite;
            }
        }
    }
    expectGe(definite, 15);
});

test("GeometryQueryContract_ShearedEllipsoidObliqueNormalHasWorldDistance", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    const forward = Matrix4x4d.identityMatrix().withVal(0, 0, 2).withVal(0, 1, 0.5);
    const inverse = Matrix4x4d.identityMatrix().withVal(0, 0, 0.5).withVal(0, 1, -0.25);
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward, inverse));
    // The transformed point of (0.6, 0.8, 0) has normal proportional to (0.3, 0.65, 0).
    const length = Math.sqrt(0.3 * 0.3 + 0.65 * 0.65);
    const nx = 0.3 / length;
    const ny = 0.65 / length;
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(1.6 + 0.09 * nx, 0.8 + 0.09 * ny, 0), 0.1));
    expectEq(GeometryContainment.Boundary, body.classifyPoint(new Vector3Dd(1.6 - 0.09 * nx, 0.8 - 0.09 * ny, 0), 0.1));
    expectEq(GeometryContainment.Outside, body.classifyPoint(new Vector3Dd(1.6 + 0.11 * nx, 0.8 + 0.11 * ny, 0), 0.1));
});

test("GeometryQueryContract_ExteriorNearBandUsesImprovedSupportingPlane", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    const forward = Matrix4x4d.identityMatrix().withVal(0, 0, 3).withVal(0, 1, 0.8);
    const inverse = Matrix4x4d.identityMatrix()
        .withVal(0, 0, 1.0 / 3)
        .withVal(0, 1, -0.8 / 3);
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward, inverse));
    // Transform the unit-sphere point (0.6, 0.8, 0). Convexity makes the
    // outward normal offset its exact distance to the transformed surface.
    const surface = forward.multiply(new Vector3Dd(0.6, 0.8, 0));
    const normal = new Vector3Dd(0.2, 0.64, 0);
    const unit = normal.multiply(1.0 / normal.length());
    expectEq(GeometryContainment.Outside, body.classifyPointCertified(surface.add(unit.multiply(0.1001)), 0.1));
    expectEq(GeometryContainment.Outside, body.classifyPoint(surface.add(unit.multiply(0.1001)), 0.1));
});

test("GeometryQueryContract_ShearedEllipsoidNormalOffsetsBracketTolerance", () => {
    const sphere = new Sphere(1);
    const body = new SimpleBody();
    body.setGeometryReference(sphere);
    const forward = Matrix4x4d.identityMatrix().withVal(0, 0, 3).withVal(0, 1, 0.8);
    const inverse = Matrix4x4d.identityMatrix()
        .withVal(0, 0, 1.0 / 3)
        .withVal(0, 1, -0.8 / 3);
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward, inverse));
    const source = [
        new Vector3Dd(0.6, 0.8, 0),
        new Vector3Dd(-0.8, 0.6, 0),
        new Vector3Dd(0, 0.6, 0.8),
        new Vector3Dd(0.6, 0, 0.8),
        new Vector3Dd(-0.6, -0.8, 0),
    ];
    let definite = 0;
    for (let i = 0; i < 5; ++i) {
        const p = forward.multiply(source[i]);
        const n = new Vector3Dd(source[i].x() / 3, source[i].y() - (0.8 * source[i].x()) / 3, source[i].z());
        const u = n.multiply(1.0 / n.length());
        // Every outward normal ray of a convex ellipsoid has an exact
        // distance equal to its offset from the chosen surface point.
        const outside = body.classifyPoint(p.add(u.multiply(0.1001)), 0.1);
        expectTrue(outside === GeometryContainment.Outside || outside === GeometryContainment.Indeterminate);
        if (outside === GeometryContainment.Outside) ++definite;
        const near = body.classifyPoint(p.add(u.multiply(0.0999)), 0.1);
        expectTrue(near === GeometryContainment.Boundary || near === GeometryContainment.Indeterminate);
        if (near === GeometryContainment.Boundary) ++definite;
    }
    expectGe(definite, 5);
});

test("GeometryQueryContract_RotatedReflectedEllipsoidUsesWorldTolerance", () => {
    const sphere = new Sphere(1);
    const ellipsoid = new SimpleBody();
    ellipsoid.setGeometryReference(sphere);
    const rotation = Matrix4x4d.identityMatrix().withVal(0, 0, 0).withVal(0, 1, -1).withVal(1, 0, 1).withVal(1, 1, 0);
    const inverseRotation = Matrix4x4d.identityMatrix()
        .withVal(0, 0, 0)
        .withVal(0, 1, 1)
        .withVal(1, 0, -1)
        .withVal(1, 1, 0);
    const forward = rotation.multiply(new Matrix4x4d().scale(-3, 2, 1));
    const inverse = new Matrix4x4d().scale(-1.0 / 3, 0.5, 1).multiply(inverseRotation);
    expectEq(AffinePlacementStatus.Valid, ellipsoid.setAffinePlacement(forward, inverse));
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPoint(new Vector3Dd(), 0.1));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPoint(new Vector3Dd(0, 3.05, 0), 0.1));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPoint(new Vector3Dd(0, 3.2, 0), 0.1));
});

test("GeometryQueryContract_OpenSurfaceDoesNotAcquireFictitiousSolidInterior", () => {
    const plane = new InfinitePlane(1, 0, 0, 0);
    const ray = Ray.fromAffine(new Vector3Dd(-2, 0, 0), new Vector3Dd(1, 0, 0));
    const legacy = new RayHit(RayHit.DETAIL_NONE, false);
    expectTrue(plane.doIntersectionFirstHit(ray, legacy));
    const workspace = new GeometryQueryWorkspace();
    expectEq(RayFirstHitStatus.Hit, plane.queryFirstHit(ray, new RayCrossingQuery(0, 4), workspace.hitResult()));
    expectEq(
        RaySolidQueryStatus.Unsupported,
        plane.classifyRay(ray, new RayCrossingQuery(0, 4), workspace.solidResult()),
    );
    expectTrue(workspace.solidResult().insideSegments.isEmpty());
});

test("GeometryQueryContract_SmallSphereNormalIsUnitAndZeroNormalUnavailable", () => {
    const sphere = new Sphere(1e-4);
    const ray = Ray.fromAffine(new Vector3Dd(0, 0, 3e-4), new Vector3Dd(0, 0, -1));
    const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
    expectEq(RayFirstHitStatus.Hit, sphere.queryFirstHit(ray, new RayCrossingQuery(0, 1), hit));
    expectNear(1, hit.normal.z(), 1e-12);
    expectEq(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, hit.getAvailableDetailMask());
    const unit = new Sphere(1);
    const center = Ray.fromAffine(new Vector3Dd(), new Vector3Dd(1, 0, 0));
    hit.reset(RayHit.DETAIL_NORMAL);
    unit.doExtraInformation(center, 0, hit);
    expectTrue(hit.isDetailAvailabilityKnown());
    expectEq(0, hit.getAvailableDetailMask() & RayHit.DETAIL_NORMAL);
});

test("GeometryBoundsContract_TransformsAllCornersAndReportsInvalidPlacement", () => {
    const local = new GeometryBounds();
    expectTrue(local.setFinite(new Vector3Dd(1, 2, 3), new Vector3Dd(3, 4, 5)));
    const placement = Matrix4x4d.identityMatrix().withVal(0, 0, -1).withVal(0, 1, 2).withVal(0, 3, 10);
    const world = local.transformed(placement);
    expectEq(GeometryBounds.Kind.Finite, world.getKind());
    expectDoubleEq(11, world.getMinimum().x());
    expectDoubleEq(17, world.getMaximum().x());
    expectDoubleEq(2, world.getMinimum().y());
    expectDoubleEq(4, world.getMaximum().y());
    expectDoubleEq(3, world.getMinimum().z());
    expectDoubleEq(5, world.getMaximum().z());
    expectEq(GeometryBounds.Kind.Indeterminate, local.transformed(placement.withVal(3, 0, 1)).getKind());
    expectEq(
        GeometryBounds.Kind.Indeterminate,
        local.transformed(placement.withVal(0, 0, Double.POSITIVE_INFINITY)).getKind(),
    );
    const empty = new GeometryBounds();
    empty.setEmpty();
    expectEq(GeometryBounds.Kind.Empty, empty.transformed(placement).getKind());
    const unbounded = new GeometryBounds();
    unbounded.setUnbounded();
    expectEq(GeometryBounds.Kind.Unbounded, unbounded.transformed(placement).getKind());
});

test("GeometryBoundsContract_BodyUsesItsPlacementAndGeometry", () => {
    const body = new SimpleBody();
    body.setGeometry(new Sphere(2));
    body.setPosition(new Vector3Dd(5, 0, 0));
    body.setScale(new Vector3Dd(-2, 1, 1));
    const bounds = new GeometryBounds();
    body.getWorldBounds(bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    expectDoubleEq(1, bounds.getMinimum().x());
    expectDoubleEq(9, bounds.getMaximum().x());
    expectDoubleEq(-2, bounds.getMinimum().y());
    expectDoubleEq(2, bounds.getMaximum().y());
    const empty = new SimpleBody();
    empty.getWorldBounds(bounds);
    expectEq(GeometryBounds.Kind.Empty, bounds.getKind());
});

test("GeometryBoundsContract_EnclosingPreservesUnknownAndUnboundedStates", () => {
    const first = new GeometryBounds();
    const second = new GeometryBounds();
    const empty = new GeometryBounds();
    const unknown = new GeometryBounds();
    const indeterminate = new GeometryBounds();
    const unbounded = new GeometryBounds();
    expectTrue(first.setFinite(new Vector3Dd(-2, 1, 3), new Vector3Dd(1, 4, 5)));
    expectTrue(second.setFinite(new Vector3Dd(0, -3, 4), new Vector3Dd(5, 2, 7)));
    empty.setEmpty();
    indeterminate.setIndeterminate();
    unbounded.setUnbounded();
    const combined = first.enclosing(second);
    expectEq(GeometryBounds.Kind.Finite, combined.getKind());
    expectDoubleEq(-2, combined.getMinimum().x());
    expectDoubleEq(-3, combined.getMinimum().y());
    expectDoubleEq(7, combined.getMaximum().z());
    expectEq(GeometryBounds.Kind.Finite, empty.enclosing(first).getKind());
    expectEq(GeometryBounds.Kind.Unbounded, first.enclosing(unbounded).getKind());
    expectEq(GeometryBounds.Kind.Unsupported, unbounded.enclosing(unknown).getKind());
    expectEq(GeometryBounds.Kind.Indeterminate, unknown.enclosing(indeterminate).getKind());
});

test("QuadricSurfaceContract_EllipsoidAndParaboloidUseAffineRoots", () => {
    const ellipsoid = new QuadricSurface(new Vector3Dd(1, 4, 9), new Vector3Dd(), new Vector3Dd(), -1);
    const ray = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(2, 0, 0));
    const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
    const boundary = new RayBoundary();
    expectEq(RayFirstHitStatus.Hit, ellipsoid.queryFirstHit(ray, new RayCrossingQuery(0, 10), hit, boundary));
    expectDoubleEq(1, boundary.t);
    expectEq(ellipsoid, boundary.geometry);
    expectDoubleEq(-1, hit.point.x());
    expectDoubleEq(-1, hit.normal.x());
    const deferred = new RayHit(RayHit.DETAIL_NORMAL, false);
    boundary.normalReversed = true;
    expectEq(RayFirstHitStatus.Hit, ellipsoid.querySurfaceDetail(ray, boundary, deferred));
    expectDoubleEq(1, deferred.normal.x());
    const events = new RayCrossingTestCollector();
    expectEq(RayCrossingSupport.Supported, ellipsoid.emitRayCrossings(ray, new RayCrossingQuery(0, 10), events));
    expectEq(2, events.roots.size());
    expectDoubleEq(1, events.roots.get(0));
    expectDoubleEq(2, events.roots.get(1));
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPoint(new Vector3Dd(), 0));
    const paraboloid = new QuadricSurface(new Vector3Dd(1, 1, 0), new Vector3Dd(), new Vector3Dd(0, 0, -1), 0);
    const vertical = Ray.fromAffine(new Vector3Dd(1, 0, 3), new Vector3Dd(0, 0, -2));
    expectEq(RayFirstHitStatus.Hit, paraboloid.queryFirstHit(vertical, new RayCrossingQuery(0, 10), hit, boundary));
    expectDoubleEq(1, boundary.t);
    const verticalEvents = new RayCrossingTestCollector();
    expectEq(
        RayCrossingSupport.Supported,
        paraboloid.emitRayCrossings(vertical, new RayCrossingQuery(0, 10), verticalEvents),
    );
    expectEq(1, verticalEvents.roots.size());
    expectEq(RayCrossingKind.Leaving, verticalEvents.kinds.get(0));
    const plane = new QuadricSurface(new Vector3Dd(), new Vector3Dd(), new Vector3Dd(0, 0, 1), 0);
    expectEq(
        RayFirstHitStatus.Indeterminate,
        plane.queryFirstHit(
            Ray.fromAffine(new Vector3Dd(), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, 10),
            hit,
            boundary,
        ),
    );
});

test("QuadricSurfaceContract_SolidIntervalsHandleEllipsoidsComplementsAndClipping", () => {
    const solid = new QuadricSurface(new Vector3Dd(1, 1, 1), new Vector3Dd(), new Vector3Dd(), -1);
    const complement = new QuadricSurface(new Vector3Dd(-1, -1, -1), new Vector3Dd(), new Vector3Dd(), 1);
    const ray = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0));
    const result = new RaySolidClassification();
    expectEq(RaySolidQueryStatus.Supported, solid.classifyRay(ray, new RayCrossingQuery(0, 10), result));
    expectEq(2, result.boundaries.size());
    expectEq(3, result.insideSegments.size());
    expectDoubleEq(2, result.boundaries.get(0)!.t);
    expectDoubleEq(4, result.boundaries.get(1)!.t);
    expectEq(solid, result.boundaries.get(0)!.geometry);
    expectFalse(result.insideSegments.get(0));
    expectTrue(result.insideSegments.get(1));
    expectFalse(result.insideSegments.get(2));
    expectEq(RaySolidQueryStatus.Supported, complement.classifyRay(ray, new RayCrossingQuery(0, 10), result));
    expectEq(3, result.insideSegments.size());
    expectTrue(result.insideSegments.get(0));
    expectFalse(result.insideSegments.get(1));
    expectTrue(result.insideSegments.get(2));
    expectEq(RaySolidQueryStatus.Supported, solid.classifyRay(ray, new RayCrossingQuery(2.5, 3.5), result));
    expectEq(0, result.boundaries.size());
    expectEq(1, result.insideSegments.size());
    expectTrue(result.insideSegments.get(0));
    expectEq(RaySolidQueryStatus.Supported, solid.classifyRay(ray, new RayCrossingQuery(2, 4), result));
    expectEq(3, result.insideSegments.size());
    expectFalse(result.insideSegments.get(0));
    expectTrue(result.insideSegments.get(1));
    expectFalse(result.insideSegments.get(2));
});

test("QuadricSurfaceContract_SolidIntervalsHandleLinearTangencyAndCoincidence", () => {
    const paraboloid = new QuadricSurface(new Vector3Dd(1, 1, 0), new Vector3Dd(), new Vector3Dd(0, 0, -1), 0);
    const result = new RaySolidClassification();
    const vertical = Ray.fromAffine(new Vector3Dd(1, 0, 3), new Vector3Dd(0, 0, -2));
    expectEq(RaySolidQueryStatus.Supported, paraboloid.classifyRay(vertical, new RayCrossingQuery(0, 10), result));
    expectEq(1, result.boundaries.size());
    expectEq(2, result.insideSegments.size());
    expectDoubleEq(1, result.boundaries.get(0)!.t);
    expectTrue(result.insideSegments.get(0));
    expectFalse(result.insideSegments.get(1));
    const sphere = new QuadricSurface(new Vector3Dd(1, 1, 1), new Vector3Dd(), new Vector3Dd(), -1);
    const inverse = new QuadricSurface(new Vector3Dd(-1, -1, -1), new Vector3Dd(), new Vector3Dd(), 1);
    const tangent = Ray.fromAffine(new Vector3Dd(-3, 1, 0), new Vector3Dd(1, 0, 0));
    expectEq(RaySolidQueryStatus.Supported, sphere.classifyRay(tangent, new RayCrossingQuery(0, 10), result));
    expectEq(0, result.boundaries.size());
    expectEq(1, result.insideSegments.size());
    expectFalse(result.insideSegments.get(0));
    expectEq(RaySolidQueryStatus.Supported, inverse.classifyRay(tangent, new RayCrossingQuery(0, 10), result));
    expectEq(1, result.insideSegments.size());
    expectTrue(result.insideSegments.get(0));
    const plane = new QuadricSurface(new Vector3Dd(), new Vector3Dd(), new Vector3Dd(0, 0, 1), 0);
    expectEq(
        RaySolidQueryStatus.Indeterminate,
        plane.classifyRay(Ray.fromAffine(new Vector3Dd(), new Vector3Dd(1, 0, 0)), new RayCrossingQuery(0, 10), result),
    );
    expectEq(0, result.insideSegments.size());
    expectEq(0, result.boundaries.size());
});

test("QuadricSurfaceContract_SolidIntervalsPreserveAffineInstanceProvenance", () => {
    const body = new SimpleBody();
    const quadric = new QuadricSurface(new Vector3Dd(1, 1, 1), new Vector3Dd(), new Vector3Dd(), -1);
    body.setGeometry(quadric);
    body.setPosition(new Vector3Dd(3, 0, 0));
    body.setScale(new Vector3Dd(2, 1, 1));
    const ray = Ray.fromAffine(new Vector3Dd(-1, 0, 0), new Vector3Dd(2, 0, 0));
    const result = new RaySolidClassification();
    expectEq(RaySolidQueryStatus.Supported, body.classifyRay(ray, new RayCrossingQuery(0, 10), result));
    expectEq(2, result.boundaries.size());
    expectDoubleEq(1, result.boundaries.get(0)!.t);
    expectDoubleEq(3, result.boundaries.get(1)!.t);
    expectEq(quadric, result.boundaries.get(0)!.geometry);
    expectEq(1, result.boundaries.get(0)!.getInstanceCount());
});

test("QuadricSurfaceContract_FiniteBoundsEncloseShiftedMixedEllipsoid", () => {
    const quadric = new QuadricSurface(new Vector3Dd(2, 2, 4), new Vector3Dd(2, 0, 0), new Vector3Dd(0, 6, -4), -2);
    const bounds = new GeometryBounds();
    quadric.getBounds(bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    const extent = Math.sqrt(6.0);
    expectNear(1 - extent, bounds.getMinimum().x(), 1e-12);
    expectNear(1 + extent, bounds.getMaximum().x(), 1e-12);
    expectNear(-2 - extent, bounds.getMinimum().y(), 1e-12);
    expectNear(-2 + extent, bounds.getMaximum().y(), 1e-12);
    expectNear(-1, bounds.getMinimum().z(), 1e-12);
    expectNear(2, bounds.getMaximum().z(), 1e-12);
    const legacy = quadric.getMinMax();
    expectDoubleEq(bounds.getMinimum().x(), legacy[0]);
    expectDoubleEq(bounds.getMaximum().z(), legacy[5]);
    const body = new SimpleBody();
    body.setGeometryReference(quadric);
    body.setPosition(new Vector3Dd(4, 0, 0));
    body.setScale(new Vector3Dd(-2, 1, 1));
    body.getWorldBounds(bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    expectNear(2 - 2 * extent, bounds.getMinimum().x(), 1e-11);
    expectNear(2 + 2 * extent, bounds.getMaximum().x(), 1e-11);
});

test("QuadricSurfaceContract_BoundsAreConservativeForUnboundedAndInvalidInputs", () => {
    const bounds = new GeometryBounds();
    const inverse = new QuadricSurface(new Vector3Dd(-1, -1, -1), new Vector3Dd(), new Vector3Dd(), 1);
    inverse.getBounds(bounds);
    expectEq(GeometryBounds.Kind.Unbounded, bounds.getKind());
    const cylinder = new QuadricSurface(new Vector3Dd(1, 1, 0), new Vector3Dd(), new Vector3Dd(), -1);
    cylinder.getBounds(bounds);
    expectEq(GeometryBounds.Kind.Unbounded, bounds.getKind());
    const tiny = new QuadricSurface(new Vector3Dd(1e-15, 1, 1), new Vector3Dd(), new Vector3Dd(), -1);
    tiny.getBounds(bounds);
    expectEq(GeometryBounds.Kind.Unbounded, bounds.getKind());
    const invalid = new QuadricSurface(
        new Vector3Dd(Double.POSITIVE_INFINITY, 1, 1),
        new Vector3Dd(),
        new Vector3Dd(),
        -1,
    );
    invalid.getBounds(bounds);
    expectEq(GeometryBounds.Kind.Indeterminate, bounds.getKind());
    const overflow = new QuadricSurface(new Vector3Dd(1e200, 1e200, 1e200), new Vector3Dd(), new Vector3Dd(), -1);
    overflow.getBounds(bounds);
    expectEq(GeometryBounds.Kind.Indeterminate, bounds.getKind());
});

test("QuadricSurfaceContract_MixedTermSolidIntervalsAgreeWithAnalyticRoots", () => {
    const quadric = new QuadricSurface(new Vector3Dd(2, 2, 4), new Vector3Dd(2, 0, 0), new Vector3Dd(0, 6, -4), -2);
    const ray = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0));
    let result = new RaySolidClassification();
    expectEq(RaySolidQueryStatus.Supported, quadric.classifyRay(ray, new RayCrossingQuery(0, 10), result));
    expectEq(2, result.boundaries.size());
    expectEq(3, result.insideSegments.size());
    expectDoubleEq(2, result.boundaries.get(0)!.t);
    expectDoubleEq(4, result.boundaries.get(1)!.t);
    expectFalse(result.insideSegments.get(0));
    expectTrue(result.insideSegments.get(1));
    expectFalse(result.insideSegments.get(2));
});

test("BoxContract_ArbitraryBoundsSupportSharedSolidQueriesAndDeferredDetail", () => {
    const box = new Box(new Vector3Dd(-2, -1, -3), new Vector3Dd(4, 3, 1));
    const ray = Ray.fromAffine(new Vector3Dd(-4, 0, 0), new Vector3Dd(1, 0, 0));
    const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
    const boundary = new RayBoundary();
    expectEq(RayFirstHitStatus.Hit, box.queryFirstHit(ray, new RayCrossingQuery(0, 10), hit, boundary));
    expectDoubleEq(2, boundary.t);
    expectEq(box, boundary.geometry);
    expectDoubleEq(-2, hit.point.x());
    expectDoubleEq(-1, hit.normal.x());
    boundary.normalReversed = true;
    const deferred = new RayHit(RayHit.DETAIL_NORMAL, false);
    expectEq(RayFirstHitStatus.Hit, box.querySurfaceDetail(ray, boundary, deferred));
    expectDoubleEq(1, deferred.normal.x());
    let result = new RaySolidClassification();
    expectEq(RaySolidQueryStatus.Supported, box.classifyRay(ray, new RayCrossingQuery(0, 10), result));
    expectEq(2, result.boundaries.size());
    expectEq(3, result.insideSegments.size());
    expectDoubleEq(2, result.boundaries.get(0)!.t);
    expectDoubleEq(8, result.boundaries.get(1)!.t);
    expectFalse(result.insideSegments.get(0));
    expectTrue(result.insideSegments.get(1));
    expectFalse(result.insideSegments.get(2));
    expectEq(GeometryContainment.Inside, box.classifyPoint(new Vector3Dd(0, 0, 0), 0));
    expectTrue(box.doIntersectionFirstHit(ray, hit));
    expectDoubleEq(-2, hit.point.x());
    const clone = box.copyGeometry();
    expectNe(null, clone);
    const copied = new GeometryBounds();
    clone!.getBounds(copied);
    expectEq(GeometryBounds.Kind.Finite, copied.getKind());
    expectDoubleEq(-2, copied.getMinimum().x());
    expectDoubleEq(4, copied.getMaximum().x());
    box.getRawBounds()[0] = new Vector3Dd(-1, -1, -3);
    expectEq(RayFirstHitStatus.Hit, box.queryFirstHit(ray, new RayCrossingQuery(0, 10), null, boundary));
    expectDoubleEq(3, boundary.t);
});

test("PlanarHalfSpaceContract_DeferredDetailPreservesLeafAndOrientation", () => {
    const solid = new PlanarHalfSpace(2, 0, 0, -4);
    const ray = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0));
    const boundary = new RayBoundary();
    expectEq(RayFirstHitStatus.Hit, solid.queryFirstHit(ray, new RayCrossingQuery(0, 10), null, boundary));
    expectEq(solid, boundary.geometry);
    expectDoubleEq(5, boundary.t);
    boundary.normalReversed = true;
    const detail = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
    expectEq(RayFirstHitStatus.Hit, solid.querySurfaceDetail(ray, boundary, detail));
    expectDoubleEq(2, detail.point.x());
    expectDoubleEq(-1, detail.normal.x());
});

test("CsgStructureContract_OrderedBorrowedOperandsAndNestedTreesPreserveIdentity", () => {
    const first = new SimpleBody();
    const second = new SimpleBody();
    first.setGeometry(new Sphere(1));
    second.setGeometry(new Sphere(2));
    second.setPosition(new Vector3Dd(4, 0, 0));
    const firstOperand = new CsgBodyOperand(first);
    const secondOperand = new CsgBodyOperand(second);
    const difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(difference.addOperand(firstOperand));
    expectTrue(difference.addOperand(secondOperand));
    expectEq(CsgOperation.DIFFERENCE, difference.getOperation());
    expectEq(2, difference.getOperandCount());
    expectEq(first.getGeometry(), difference.getOperand(0)!.getGeometry());
    expectEq(second.getGeometry(), difference.getOperand(1)!.getGeometry());
    expectEq(null, difference.getOperand(0)!.getChildExpression());
    expectDoubleEq(4, difference.getOperand(1)!.getAffinePlacement().multiply(new Vector3Dd()).x());
    const forward = [new Matrix4x4d()];
    const inverse = [new Matrix4x4d()];
    expectEq(AffinePlacementStatus.Valid, difference.getOperand(1)!.queryAffinePlacement(forward, inverse));
    expectDoubleEq(-4, inverse[0]!.multiply(new Vector3Dd()).x());
    second.setPosition(new Vector3Dd(7, 0, 0));
    expectDoubleEq(7, difference.getOperand(1)!.getAffinePlacement().multiply(new Vector3Dd()).x());
    expectEq(null, difference.getOperand(-1));
    expectEq(null, difference.getOperand(2));
    expectFalse(difference.addOperand(difference));
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(difference));
    expectEq(difference, root.getOperand(0)!.getChildExpression());
    expectEq(null, root.getOperand(0)!.getGeometry());
    expectEq(CsgOperation.DIFFERENCE, root.getOperand(0)!.getChildExpression()!.getOperation());
    expectFalse(difference.addOperand(root));
    expectEq(2, difference.getOperandCount());
});

test("AffinePlacementContract_ValidatesPairsAndRejectsInvalidOrInconsistentMatrices", () => {
    const forward = new Matrix4x4d().translation(3, -2, 1).multiply(Matrix4x4d.identityMatrix().withVal(0, 1, 0.5));
    const inverse = Matrix4x4d.identityMatrix()
        .withVal(0, 1, -0.5)
        .multiply(new Matrix4x4d().translation(-3, 2, -1));
    expectEq(AffinePlacementStatus.Valid, AffinePlacement.validate(forward, inverse));
    expectEq(
        AffinePlacementStatus.Invalid,
        AffinePlacement.validate(new Matrix4x4d().scale(0, 1, 1), new Matrix4x4d()),
    );
    expectEq(AffinePlacementStatus.Indeterminate, AffinePlacement.validate(forward, new Matrix4x4d()));
    expectEq(
        AffinePlacementStatus.Indeterminate,
        AffinePlacement.validate(new Matrix4x4d().scale(1e-200, 1e-200, 1), new Matrix4x4d().scale(1e200, 1e200, 1)),
    );
    expectEq(AffinePlacementStatus.Invalid, AffinePlacement.validate(forward.withVal(3, 0, 1), inverse));
    expectEq(
        AffinePlacementStatus.Invalid,
        AffinePlacement.validate(forward.withVal(0, 0, Double.POSITIVE_INFINITY), inverse),
    );
});

test("AffinePlacementContract_SceneCrossingsKeepParametersAndInstanceProvenance", () => {
    const body = new SimpleBody();
    const sphere = new Sphere(1);
    body.setGeometry(sphere);
    body.setPosition(new Vector3Dd(3, 0, 0));
    body.setScale(new Vector3Dd(-2, 1, 1));
    const ray = Ray.fromAffine(new Vector3Dd(-1, 0, 0), new Vector3Dd(2, 0, 0));
    let result = new RayCrossingTestCollector();
    expectEq(RayCrossingSupport.Supported, body.emitRayCrossings(ray, new RayCrossingQuery(0, 10), result));
    expectEq(2, result.roots.size());
    expectDoubleEq(1, result.roots.get(0));
    expectDoubleEq(3, result.roots.get(1));
    expectEq(sphere, result.geometries.get(0));
    expectEq(1, result.instanceCounts.get(0));
    expectEq(RayCrossingKind.Entering, result.kinds.get(0));
    expectEq(RayCrossingKind.Leaving, result.kinds.get(1));
});

test("AffinePlacementContract_ExplicitShearDrivesQueriesRenderingAndEditing", () => {
    const body = new SimpleBody();
    const sphere = new Sphere(1);
    body.setGeometry(sphere);
    const forward = new Matrix4x4d()
        .translation(3, 0, 0)
        .multiply(Matrix4x4d.identityMatrix().withVal(0, 1, 0.5))
        .multiply(new Matrix4x4d().scale(2, 1, 1));
    const inverse = new Matrix4x4d()
        .scale(0.5, 1, 1)
        .multiply(Matrix4x4d.identityMatrix().withVal(0, 1, -0.5))
        .multiply(new Matrix4x4d().translation(-3, 0, 0));
    expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(forward, inverse));
    expectFalse(body.supportsDecomposedPlacement());
    const ray = Ray.fromAffine(
        forward.multiply(new Vector3Dd(-3, 0, 0)),
        forward.withoutTranslation().multiply(new Vector3Dd(1, 0, 0)),
    );
    const classification = new RaySolidClassification();
    expectEq(RaySolidQueryStatus.Supported, body.classifyRay(ray, new RayCrossingQuery(0, 10), classification));
    expectEq(2, classification.boundaries.size());
    expectNear(2, classification.boundaries.get(0)!.t, 1e-12);
    const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, true);
    expectTrue(body.doIntersectionFirstHit(ray, hit));
    expectNe(null, hit.getRay());
    expectNear(2, hit.getRay()!.getT(), 1e-12);
    expectNear(1, hit.point.x(), 1e-12);
    expectNear(-0.8944271909999159, hit.normal.x(), 1e-12);
    expectNear(0.4472135954999579, hit.normal.y(), 1e-12);
    expectEq(GeometryContainment.Inside, body.classifyPoint(forward.multiply(new Vector3Dd()), 0));
    expectEq(GeometryContainment.Inside, body.classifyPoint(forward.multiply(new Vector3Dd()), 0.1));
    const before = body.getAffinePlacement();
    expectEq(AffinePlacementStatus.Invalid, body.setAffinePlacement(new Matrix4x4d().scale(0, 1, 1), new Matrix4x4d()));
    expectTrue(body.getAffinePlacement().epsilonEquals(before, 0));
    body.setPosition(new Vector3Dd(5, 1, 0));
    const moved = [new Matrix4x4d()];
    const movedInverse = [new Matrix4x4d()];
    expectEq(AffinePlacementStatus.Valid, body.queryAffinePlacement(moved, movedInverse));
    expectNear(5, moved[0]!.multiply(new Vector3Dd()).x(), 1e-12);
    expectNear(0, movedInverse[0]!.multiply(new Vector3Dd(5, 1, 0)).length(), 1e-12);
    body.resetAffinePlacement();
    expectTrue(body.supportsDecomposedPlacement());
    expectTrue(body.getAffinePlacement().epsilonEquals(new Matrix4x4d(), 0));
});

test("FloatingIntervalContract_DeterministicStressContainsExactRationalBounds", () => {
    expectTrue(FloatingInterval.isSupportedEnvironment());
    // Generated by generate_interval_stress_reference.py using exact rationals.
    const cases = [
        [7.146546754040844e-69, 7.146546754040845e-69, 0, 1.4293093508081688e-68, 1.429309350808169e-68],
        [-3.5566057525170395e21, -1.4583237856775876e33, 1, 1.4583237856740308e33, 1.458323785674031e33],
        [-8.11391527557009e84, -1.5651942759869695e-82, 2, 1269.985374516554, 1269.9853745165542],
        [-7.005861629145818e116, 2.093032075091621e26, 3, -3.347230896516071e90, -3.3472308965160706e90],
        [1.7111650208095105e-80, 3951736602112120.0, 0, 3951736602112120.0, 3951736602112120.5],
        [-3.899596746493458e-148, -1.4313098573698764e53, 1, 1.4313098573698762e53, 1.4313098573698764e53],
        [-2.9147068561821865e67, 3.2035585822490825e-138, 2, -9.337434163862687e-71, -9.337434163862685e-71],
        [-1.0036314697615076e56, 8.595075937226869e-51, 3, -1.1676819112377978e106, -1.1676819112377976e106],
        [-1.690969726056025e-14, -5.55933477059183e44, 0, -5.559334770591831e44, -5.55933477059183e44],
        [-4.3388294890186107e42, -1.7015852866887574e116, 1, 1.701585286688757e116, 1.7015852866887574e116],
        [-1.894326392189219e53, 2.329431193695396e-17, 2, -4.412702989006026e36, -4.412702989006025e36],
        [-1.0208457775245834e153, -1.0208457775245832e153, 3, 1.0, 1.0000000000000002],
        [-4.562818881199697e85, 2.7172586259056785e-72, 0, -4.562818881199697e85, -4.562818881199696e85],
        [2.960898220504463e-40, 6.649198969387046e-112, 1, 2.9608982205044627e-40, 2.960898220504463e-40],
        [-6646668405780803.0, 2.848094402563231e110, 2, -1.893033908219818e126, -1.8930339082198176e126],
        [8.454772843731548e-50, 1.1089667819206423e73, 3, 7.624009106105553e-123, 7.624009106105555e-123],
        [2.8086253420183986e58, -1.0792115406317105e-124, 0, 2.808625342018398e58, 2.8086253420183986e58],
        [1.5882786408807579e-99, 2.964314447997346e-102, 1, 1.5853143264327605e-99, 1.5853143264327607e-99],
        [-2.680570190843948e104, 6.248135193642002e67, 2, -1.6748564948439732e172, -1.674856494843973e172],
        [-4.931861314370597e-21, -3.391881745352832e-46, 3, 1.4540192390632924e25, 1.4540192390632926e25],
        [1.3741384403734075e70, -2.044099822357928e149, 0, -2.044099822357928e149, -2.0440998223579277e149],
        [7.652799199332246e21, 3.9278002242702226e-89, 1, 7.652799199332245e21, 7.652799199332246e21],
        [3.75199107563365e-22, 3.7519910756336506e-22, 2, 1.4077437031634557e-43, 1.4077437031634559e-43],
        [-9.094398980472972e-147, 1.3287552253160717e-133, 3, -6.844299692826934e-14, -6.844299692826933e-14],
        [1.0265886784318475e108, -1.916315795976818e61, 0, 1.0265886784318473e108, 1.0265886784318475e108],
        [1.1188068606861152e53, 1.2393022779389087e70, 1, -1.2393022779389087e70, -1.2393022779389085e70],
        [4.027621290101606e-63, 7.831154528608348e-129, 2, 3.154092470549859e-191, 3.1540924705498595e-191],
        [1.4998938791684562e66, -1.0134956380621436e-78, 3, -1.4799213956523103e144, -1.4799213956523101e144],
        [-3.291038874024351e20, -4.5813304442681997e-125, 0, -3.2910388740243515e20, -3.291038874024351e20],
        [6.628538733888976e-24, -154744931.69699302, 1, 154744931.69699302, 154744931.69699305],
        [-9.205481630175304e-152, -1.3587824594568078e55, 2, 1.2508246969934063e-96, 1.2508246969934066e-96],
        [-7.960151267585567e-87, -7.998774084540255e-37, 3, 9.951714079499587e-51, 9.951714079499588e-51],
        [-4.630168634212488e158, -4.834134258630453e-18, 0, -4.630168634212489e158, -4.630168634212488e158],
        [3.847223730397442e-117, 3.847223730397443e-117, 1, -7.044203657368268e-133, -7.044203657368268e-133],
        [-6.833021388605202e-77, -2.7067301603686296e107, 2, 1.8495145078981634e31, 1.8495145078981637e31],
        [-2.715791966617085e-97, -1.816861271330557e-46, 3, 1.4947712351357492e-51, 1.4947712351357495e-51],
        [7.971032764418196e55, -7.402207684804431e-112, 0, 7.971032764418195e55, 7.971032764418196e55],
        [-4.882773892133515e24, 4.0718275219942044e-82, 1, -4.882773892133516e24, -4.882773892133515e24],
        [-2.4793688191233346e-64, 2.3867158809198215e-110, 2, -5.917548935259088e-174, -5.917548935259087e-174],
        [4.947951554127676e144, 3.666361594251627e40, 3, 1.3495536179206691e104, 1.3495536179206693e104],
        [-2.3530147634202964e-72, -5.951630108185943e-73, 0, -2.948177774238891e-72, -2.9481777742388903e-72],
        [1.5835292443330722e-25, 5.230194477104014e-79, 1, 1.583529244333072e-25, 1.5835292443330722e-25],
        [-8.439740587736398e-115, 9.917447007016836e-9, 2, -8.370068003184486e-123, -8.370068003184485e-123],
        [2.1679518598992123e-100, 8.955389978715759e-95, 3, 2.4208346761579058e-6, 2.420834676157906e-6],
        [-3.248475655729729e-55, -3.2484756557297285e-55, 0, -6.496951311459458e-55, -6.496951311459457e-55],
        [-2.5154338451103455e-142, 1.2809559014651951e-48, 1, -1.2809559014651952e-48, -1.2809559014651951e-48],
        [-370705540480549.56, -1.6058651255974986e60, 2, 5.953030993234863e74, 5.953030993234864e74],
        [-8.912909900324393e-70, -2.392715657549679e78, 3, 3.725018420889962e-148, 3.725018420889963e-148],
        [7.521179311428108e19, 4.300536432303738e-10, 0, 7.521179311428108e19, 7.52117931142811e19],
        [-4.6044121992001574e-126, -1.321209905758332e67, 1, 1.3212099057583318e67, 1.321209905758332e67],
        [-5.8223902096831565e134, 1.466369558975731e-114, 2, -8.537775763957706e20, -8.537775763957704e20],
        [5.0903072198465135e120, -1.4937149639888107e84, 3, -3.407816981529982e36, -3.407816981529981e36],
        [8.526842899455951e-27, -1.999149353405858e-7, 0, -1.999149353405858e-7, -1.9991493534058578e-7],
        [-4.092137893802181e140, 1.5403752585329928e89, 1, -4.092137893802182e140, -4.092137893802181e140],
        [-1.156487437800255e-147, -1.5947101103409532e-31, 2, 1.8442622095423708e-178, 1.844262209542371e-178],
        [1.696053312351897e104, 1.6960533123518972e104, 3, 0.9999999999999998, 0.9999999999999999],
        [-2.5248682808900746e-8, -4.261666906767745e16, 0, -4.261666906767746e16, -4.261666906767745e16],
        [5.2751611350313e116, 0.001492527481737261, 1, 5.2751611350312994e116, 5.2751611350313e116],
        [-1.2280760209282201e66, -5.6334765238469545e20, 2, 6.918337433398509e86, 6.91833743339851e86],
        [6.357995500901358e-124, -1.317484762865612e-131, 3, -48258588.48699183, -48258588.48699182],
        [4.487976006258921e-134, -8.20423364170612e158, 0, -8.20423364170612e158, -8.204233641706119e158],
        [1.4366870856264788e68, 8.423740531088334e155, 1, -8.423740531088334e155, -8.423740531088333e155],
        [-7.288522672983534e32, -8.27933867935607e71, 2, 6.034414768179625e104, 6.034414768179627e104],
        [-3.2203343329787468e150, -4.3538290944758115e85, 3, 7.396556601325357e64, 7.396556601325358e64],
    ];
    for (let i = 0; i < 64; ++i) {
        const a = FloatingInterval.exact(cases[i][0]);
        const b = FloatingInterval.exact(cases[i][1]);
        let result = new FloatingInterval();
        switch (cases[i][2]) {
            case 0:
                result = a.add(b);
                break;
            case 1:
                result = a.subtract(b);
                break;
            case 2:
                result = a.multiply(b);
                break;
            default:
                result = a.divide(b);
                break;
        }
        expectTrue(result.isValid());
        expectLe(result.getLower(), cases[i][3]);
        expectGe(result.getUpper(), cases[i][4]);
    }
});

test("CsgPointClassificationContract_PlacedSpheresAndNestedBooleanOperations", () => {
    const a = new SimpleBody();
    const b = new SimpleBody();
    a.setGeometry(new Sphere(2));
    b.setGeometry(new Sphere(1));
    b.setPosition(new Vector3Dd(1, 0, 0));
    const aOperand = new CsgBodyOperand(a);
    const bOperand = new CsgBodyOperand(b);
    const united = new CsgTree(CsgOperation.UNION);
    const intersected = new CsgTree(CsgOperation.INTERSECTION);
    const difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(united.addOperand(aOperand));
    expectTrue(united.addOperand(bOperand));
    expectTrue(intersected.addOperand(aOperand));
    expectTrue(intersected.addOperand(bOperand));
    expectTrue(difference.addOperand(aOperand));
    expectTrue(difference.addOperand(bOperand));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(united, new Vector3Dd(-1, 0, 0), 0.1));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(united, new Vector3Dd(4, 0, 0), 0.1));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(intersected, new Vector3Dd(1, 0, 0), 0.1));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(intersected, new Vector3Dd(-1, 0, 0), 0.1));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(difference, new Vector3Dd(-1, 0, 0), 0.1));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(difference, new Vector3Dd(1, 0, 0), 0.1));
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classify(difference, new Vector3Dd(0, 0, 0), 0.1));
    const nested = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(nested.addOperand(united));
    expectTrue(nested.addOperand(aOperand));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(nested, new Vector3Dd(-1, 0, 0), 0.1));
    expectEq(GeometryContainment.InvalidInput, CsgPointClassification.classify(nested, new Vector3Dd(), -1));
    const empty = new CsgTree(CsgOperation.UNION);
    expectEq(GeometryContainment.InvalidInput, CsgPointClassification.classify(empty, new Vector3Dd(), 0));
});

test("CsgPointClassificationContract_PreservesUncertaintyAndProvesMaskedResults", () => {
    const inside = new CsgTestOperand(GeometryContainment.Inside);
    const outside = new CsgTestOperand(GeometryContainment.Outside);
    const boundary = new CsgTestOperand(GeometryContainment.Boundary);
    const unknown = new CsgTestOperand(GeometryContainment.Indeterminate);
    const unsupported = new CsgTestOperand(GeometryContainment.Unsupported);
    const invalid = new CsgTestOperand(GeometryContainment.InvalidInput);
    const hiddenBoundary = new CsgTree(CsgOperation.UNION);
    const exposedBoundary = new CsgTree(CsgOperation.UNION);
    expectTrue(hiddenBoundary.addOperand(inside));
    expectTrue(hiddenBoundary.addOperand(boundary));
    expectTrue(exposedBoundary.addOperand(outside));
    expectTrue(exposedBoundary.addOperand(boundary));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(hiddenBoundary, new Vector3Dd(), 0.1));
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classify(exposedBoundary, new Vector3Dd(), 0.1));
    const unknownUnion = new CsgTree(CsgOperation.UNION);
    const unknownIntersection = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(unknownUnion.addOperand(outside));
    expectTrue(unknownUnion.addOperand(unknown));
    expectTrue(unknownIntersection.addOperand(inside));
    expectTrue(unknownIntersection.addOperand(unsupported));
    expectEq(GeometryContainment.Indeterminate, CsgPointClassification.classify(unknownUnion, new Vector3Dd(), 0.1));
    expectEq(
        GeometryContainment.Unsupported,
        CsgPointClassification.classify(unknownIntersection, new Vector3Dd(), 0.1),
    );
    const decisive = new CsgTree(CsgOperation.INTERSECTION);
    const malformed = new CsgTree(CsgOperation.UNION);
    expectTrue(decisive.addOperand(outside));
    expectTrue(decisive.addOperand(unknown));
    expectTrue(malformed.addOperand(inside));
    expectTrue(malformed.addOperand(invalid));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(decisive, new Vector3Dd(), 0.1));
    expectEq(GeometryContainment.InvalidInput, CsgPointClassification.classify(malformed, new Vector3Dd(), 0.1));
});

test("QuadricRayExpansionContract_ExpansionMatchesQuadricAlongRay", () => {
    const quadric = new QuadricSurface(
        new Vector3Dd(1.5, -0.75, 2.0),
        new Vector3Dd(0.5, -1.25, 0.375),
        new Vector3Dd(-0.25, 1.0, 0.625),
        -3.0,
    );
    // new Q(origin) < 0 and a > 0, so the ray crosses the surface twice.
    const origin = new Vector3Dd(0.25, -0.125, 0.5);
    const direction = new Vector3Dd(1.0, 0.25, 0.5);
    const a = QuadricRayExpansion.squareTerm(
        quadric,
        true,
        direction.multiply(direction),
        QuadricRayExpansion.mixedProducts(direction, direction),
    );
    const b = QuadricRayExpansion.linearTerm(
        quadric,
        origin.multiply(direction),
        direction,
        QuadricRayExpansion.symmetricMixedProducts(origin, direction),
    );
    const c = QuadricRayExpansion.constantTerm(
        quadric,
        QuadricRayExpansion.constantTermWithoutMixed(quadric, origin.multiply(origin), origin),
        QuadricRayExpansion.mixedProducts(origin, origin),
    );
    expectNear(c, quadric.evaluate(origin), 1e-12);
    for (let i = -4; i <= 4; i++) {
        const t = 0.75 * i;
        expectNear((a * t + b) * t + c, quadric.evaluate(origin.add(direction.multiply(t))), 1e-11);
    }
    expectGt(a, 0.0);
    expectLt(c, 0.0);
    const first = [0];
    const second = [0];
    expectTrue(QuadricSurface.solve(a, b, c, first, second));
    expectGt(first[0]!, 0.0);
    expectLt(second[0]!, 0.0);
    expectNear(quadric.evaluate(origin.add(direction.multiply(first[0]!))), 0.0, 1e-9);
    expectNear(quadric.evaluate(origin.add(direction.multiply(second[0]!))), 0.0, 1e-9);
});

test("QuadricRayExpansionContract_QuadricWithoutSquareTermsIsLinearAlongRay", () => {
    const plane = new QuadricSurface(new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 2), -1.0);
    const origin = new Vector3Dd(0.25, 0.5, -1.5);
    const direction = new Vector3Dd(0, 0.5, 1);
    const a = QuadricRayExpansion.squareTerm(
        plane,
        false,
        direction.multiply(direction),
        QuadricRayExpansion.mixedProducts(direction, direction),
    );
    const b = QuadricRayExpansion.linearTerm(
        plane,
        origin.multiply(direction),
        direction,
        QuadricRayExpansion.symmetricMixedProducts(origin, direction),
    );
    const c = QuadricRayExpansion.constantTerm(
        plane,
        QuadricRayExpansion.constantTermWithoutMixed(plane, origin.multiply(origin), origin),
        QuadricRayExpansion.mixedProducts(origin, origin),
    );
    expectEq(a, 0.0);
    const first = [0];
    const second = [0];
    expectTrue(QuadricSurface.solve(a, b, c, first, second));
    expectDoubleEq(first[0]!, 2.0);
    expectDoubleEq(second[0]!, 2.0);
});
