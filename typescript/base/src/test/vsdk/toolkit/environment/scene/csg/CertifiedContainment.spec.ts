// Port of cpp/base/src/test/certified_containment_tests.cpp
import { test } from "vitest";
import {
    expectEq,
    expectFalse,
    expectGe,
    expectGt,
    expectLe,
    expectLt,
    expectNe,
    expectTrue,
} from "../../../../../GTestAssertions.js";
import { AffinePlacementStatus } from "vsdk/toolkit/environment/scene/AffinePlacementStatus.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { Cone } from "vsdk/toolkit/environment/geometry/volume/Cone.js";
import { CsgBodyOperand } from "vsdk/toolkit/environment/scene/csg/CsgBodyOperand.js";
import { CsgOperation } from "vsdk/toolkit/environment/scene/csg/CsgOperation.js";
import { CsgPlacedExpression } from "vsdk/toolkit/environment/scene/csg/CsgPlacedExpression.js";
import { CsgPointClassification } from "vsdk/toolkit/environment/scene/csg/CsgPointClassification.js";
import { CsgPointWitness } from "vsdk/toolkit/environment/scene/csg/CsgPointWitness.js";
import { CsgSolidBounds } from "vsdk/toolkit/environment/scene/csg/CsgSolidBounds.js";
import { CsgTree } from "vsdk/toolkit/environment/scene/csg/CsgTree.js";
import { Geometry } from "vsdk/toolkit/environment/geometry/Geometry.js";
import { GeometryBounds } from "vsdk/toolkit/environment/geometry/element/GeometryBounds.js";
import { GeometryContainment } from "vsdk/toolkit/environment/geometry/element/GeometryContainment.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { PlanarHalfSpace } from "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.js";
import { QuadricSurface } from "vsdk/toolkit/environment/geometry/volume/QuadricSurface.js";
import { SimpleBody } from "vsdk/toolkit/environment/scene/SimpleBody.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { TriangleMesh } from "vsdk/toolkit/environment/geometry/surface/TriangleMesh.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
/*
Certified containment for the basic solid families (stage F-1). Expected
results come from independent closed-form distances; placed cases are also
checked for soundness by sampling the world tolerance ball in long double.
*/

test("CertifiedContainmentContract_BoxUsesEuclideanDistanceToItsBoundary", () => {
    const box = new Box(new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1));
    expectEq(GeometryContainment.Inside, box.classifyPointCertified(new Vector3Dd(0, 0, 0), 0.5));
    expectEq(GeometryContainment.Boundary, box.classifyPointCertified(new Vector3Dd(0.95, 0, 0), 0.1));
    expectEq(GeometryContainment.Boundary, box.classifyPointCertified(new Vector3Dd(1.05, 0, 0), 0.1));
    expectEq(GeometryContainment.Outside, box.classifyPointCertified(new Vector3Dd(1.2, 0, 0), 0.1));
    // Corner region: distance sqrt(2)*0.06 = 0.0849 and sqrt(2)*0.08 = 0.1131.
    expectEq(GeometryContainment.Boundary, box.classifyPointCertified(new Vector3Dd(1.06, 1.06, 0), 0.1));
    expectEq(GeometryContainment.Outside, box.classifyPointCertified(new Vector3Dd(1.08, 1.08, 0), 0.1));
    // A point exactly on a face is a boundary at zero tolerance.
    expectEq(GeometryContainment.Boundary, box.classifyPointCertified(new Vector3Dd(1, 0.5, 0), 0));
    expectEq(GeometryContainment.Inside, box.classifyPointCertified(new Vector3Dd(0.5, 0.5, 0), 0));
    expectEq(GeometryContainment.InvalidInput, box.classifyPointCertified(new Vector3Dd(0, 0, 0), -1));
    // The plain query delegates to the same certificate for positive tolerance.
    expectEq(GeometryContainment.Outside, box.classifyPoint(new Vector3Dd(1.08, 1.08, 0), 0.1));
});

test("CertifiedContainmentContract_HalfSpaceNormalizesItsCoefficients", () => {
    const half = new PlanarHalfSpace(2, 0, 0, -2); // x <= 1
    expectEq(GeometryContainment.Inside, half.classifyPointCertified(new Vector3Dd(0.8, 5, 5), 0.1));
    expectEq(GeometryContainment.Boundary, half.classifyPointCertified(new Vector3Dd(1.05, 0, 0), 0.1));
    expectEq(GeometryContainment.Boundary, half.classifyPointCertified(new Vector3Dd(0.95, 0, 0), 0.1));
    expectEq(GeometryContainment.Outside, half.classifyPointCertified(new Vector3Dd(1.2, 0, 0), 0.1));
    const oblique = new PlanarHalfSpace(1, 1, 0, 0); // distance (x+y)/sqrt(2)
    expectEq(GeometryContainment.Boundary, oblique.classifyPointCertified(new Vector3Dd(0.1, 0, 0), 0.1));
    expectEq(GeometryContainment.Outside, oblique.classifyPointCertified(new Vector3Dd(0.1, 0.1, 0), 0.1));
    expectEq(
        GeometryContainment.InvalidInput,
        new PlanarHalfSpace(0, 0, 0, 1).classifyPointCertified(new Vector3Dd(), 0.1),
    );
});

test("CertifiedContainmentContract_QuadricProvesSidesAndSignChangesOnly", () => {
    // x^2/4 + y^2 + z^2 <= 1: semi-axes 2, 1, 1.
    const ellipsoid = new QuadricSurface(new Vector3Dd(0.25, 1, 1), new Vector3Dd(), new Vector3Dd(), -1);
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPointCertified(new Vector3Dd(), 0.5));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPointCertified(new Vector3Dd(2.05, 0, 0), 0.1));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPointCertified(new Vector3Dd(0, 0.95, 0), 0.1));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPointCertified(new Vector3Dd(2.3, 0, 0), 0.1));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPointCertified(new Vector3Dd(0, 1.5, 0), 0.1));
    // Zero tolerance: sides are proven by the sign of F; the surface itself is not.
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPointCertified(new Vector3Dd(1.9, 0, 0), 0));
    expectEq(GeometryContainment.Indeterminate, ellipsoid.classifyPointCertified(new Vector3Dd(2, 0, 0), 0));
    // A saddle with a mixed term: xy <= 1.
    const saddle = new QuadricSurface(new Vector3Dd(), new Vector3Dd(1, 0, 0), new Vector3Dd(), -1);
    expectEq(GeometryContainment.Boundary, saddle.classifyPointCertified(new Vector3Dd(1, 1, 0), 0.01));
    expectEq(GeometryContainment.Inside, saddle.classifyPointCertified(new Vector3Dd(-1, 1, 0), 0.1));
    // A tangent-only contact (x^2 <= 0) has no sign change and is not claimed.
    const slab = new QuadricSurface(new Vector3Dd(1, 0, 0), new Vector3Dd(), new Vector3Dd(), 0);
    expectEq(GeometryContainment.Indeterminate, slab.classifyPointCertified(new Vector3Dd(0.01, 0, 0), 0.1));
    expectEq(GeometryContainment.Outside, slab.classifyPointCertified(new Vector3Dd(0.5, 0, 0), 0.1));
});

// The C++ sampler evaluates in long double; binary64 is used here.
class CertifiedContainmentSampler {
    // Signs of the local implicit description at ball samples: -1 inside, +1 outside.
    public static boxValue(p: number[]): number {
        let value = -1e300;
        for (let i = 0; i < 3; ++i) {
            const d = Math.abs(p[i]!) - 1.0;
            if (d > value) value = d;
        }
        return value;
    }
    public static halfSpaceValue(p: number[]): number {
        return p[0]! + 2.0 * p[1]! - p[2]! - 0.5;
    }
    public static ellipsoidValue(p: number[]): number {
        return 0.25 * p[0]! * p[0]! + p[1]! * p[1]! + p[2]! * p[2]! + 0.5 * p[0]! * p[1]! - 1.0;
    }
    public static value(family: number, p: number[]): number {
        return family === 0
            ? CertifiedContainmentSampler.boxValue(p)
            : family === 1
              ? CertifiedContainmentSampler.halfSpaceValue(p)
              : CertifiedContainmentSampler.ellipsoidValue(p);
    }
    public static local(inverse: Matrix4x4d, world: Vector3Dd, out: number[]): void {
        for (let i = 0; i < 3; ++i) {
            out[i] = inverse.get(i, 3);
            out[i] += inverse.get(i, 0) * world.x() + inverse.get(i, 1) * world.y() + inverse.get(i, 2) * world.z();
        }
    }
    // Returns -1/+1 when every sample has that sign, 0 when both appear.
    public static sampleSign(family: number, inverse: Matrix4x4d, centre: Vector3Dd, radius: number): number {
        let negative = false;
        let positive = false;
        const count = 96;
        for (let shell = 0; shell <= 2; ++shell) {
            const r = (radius * shell) / 2.0;
            for (let k = 0; k < count; ++k) {
                const z = 1.0 - (2.0 * k + 1.0) / count;
                const ring = Math.sqrt(1.0 - z * z);
                const angle = 2.399963229728653 * k;
                const q = centre.add(new Vector3Dd(ring * Math.cos(angle), ring * Math.sin(angle), z).multiply(r));
                const p: number[] = [0, 0, 0];
                CertifiedContainmentSampler.local(inverse, q, p);
                const v = CertifiedContainmentSampler.value(family, p);
                if (v < 0) negative = true;
                if (v > 0) positive = true;
                if (shell === 0) break;
            }
        }
        return negative && positive ? 0 : negative ? -1 : 1;
    }
}

test("CertifiedContainmentContract_PlacedFamiliesAreSoundUnderShearReflectionAndRotation", () => {
    const box = new Box(new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1));
    const half = new PlanarHalfSpace(1, 2, -1, -0.5);
    const ellipsoid = new QuadricSurface(new Vector3Dd(0.25, 1, 1), new Vector3Dd(0.5, 0, 0), new Vector3Dd(), -1);
    const geometries: Geometry[] = [box, half, ellipsoid];
    const placements = [
        new Matrix4x4d().translation(1, -2, 0.5).multiply(new Matrix4x4d().scale(3, 0.5, 1.5)),
        new Matrix4x4d().translation(-1, 0, 2).multiply(
            Matrix4x4d.identityMatrix()
                .withVal(0, 1, 0.75)
                .multiply(new Matrix4x4d().scale(-1, 2, 1)),
        ),
        new Matrix4x4d().axisRotation(0.7, 1, 2, 3).multiply(new Matrix4x4d().scale(2, 1, 0.25)),
    ];
    let definite = 0;
    let total = 0;
    for (let family = 0; family < 3; ++family) {
        for (let placement = 0; placement < 3; ++placement) {
            const body = new SimpleBody();
            body.setGeometryReference(geometries[family]!);
            const inverse = placements[placement].inverse();
            expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(placements[placement], inverse));
            for (let step = -40; step <= 40; ++step) {
                const local = new Vector3Dd(0.05 * step, 0.3, -0.2);
                const world = placements[placement].multiply(local);
                for (let t = 0; t < 2; ++t) {
                    const tolerance = t === 0 ? 0.05 : 0.2;
                    const result = body.classifyPointCertified(world, tolerance);
                    ++total;
                    if (result === GeometryContainment.Indeterminate) continue;
                    expectNe(GeometryContainment.Unsupported, result);
                    expectNe(GeometryContainment.InvalidInput, result);
                    ++definite;
                    const sign = CertifiedContainmentSampler.sampleSign(family, inverse, world, tolerance);
                    if (result === GeometryContainment.Inside) expectEq(-1, sign);
                    if (result === GeometryContainment.Outside) expectEq(1, sign);
                }
            }
        }
    }
    // The certificates must decide most of the sweep, not merely stay sound.
    expectGt(definite, Math.trunc((total * 3) / 4));
});

test("CertifiedContainmentContract_AnisotropicBoxUsesTheTightAxisEnclosure", () => {
    const box = new Box(2, 2, 2); // [-1,1]^3
    const body = new SimpleBody();
    body.setGeometryReference(box);
    body.setPosition(new Vector3Dd(10, 0, 0));
    body.setScale(new Vector3Dd(-2, 3, 1)); // world x in [8,12], y in [-3,3]
    // World distance 0.15 from the x face. The enclosing local ball would have
    // radius about 0.117 against a local distance 0.075; the axis box is exact.
    expectEq(GeometryContainment.Outside, body.classifyPointCertified(new Vector3Dd(7.85, 0, 0), 0.1));
    expectEq(GeometryContainment.Inside, body.classifyPointCertified(new Vector3Dd(8.15, 0, 0), 0.1));
    expectEq(GeometryContainment.Boundary, body.classifyPointCertified(new Vector3Dd(8.05, 0, 0), 0.1));
    // The inner local ball has radius tolerance/3 = 0.02 against a local face
    // distance 0.025; two certified world samples still prove the boundary.
    expectEq(GeometryContainment.Boundary, body.classifyPointCertified(new Vector3Dd(12.05, 0, 0), 0.06));
    expectEq(GeometryContainment.Outside, body.classifyPointCertified(new Vector3Dd(12.07, 0, 0), 0.06));
});

test("CertifiedContainmentContract_CsgLeavesOfEveryBasicFamilyAreCertified", () => {
    const block = new SimpleBody();
    const cut = new SimpleBody();
    const rounded = new SimpleBody();
    block.setGeometry(new Box(new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1)));
    cut.setGeometry(new PlanarHalfSpace(0, 0, -1, 0)); // z >= 0
    rounded.setGeometry(new QuadricSurface(new Vector3Dd(1, 1, 1), new Vector3Dd(), new Vector3Dd(), -2.25));
    rounded.setScale(new Vector3Dd(1, 1, 0.5));
    const a = new CsgBodyOperand(block);
    const b = new CsgBodyOperand(cut);
    const c = new CsgBodyOperand(rounded);
    const root = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(root.addOperand(a));
    expectTrue(root.addOperand(b));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(root, new Vector3Dd(0, 0, -0.5), 0.1));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(root, new Vector3Dd(0, 0, 0.5), 0.1));
    const witness = new CsgPointWitness();
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithWitness(root, new Vector3Dd(0, 0, 0.02), 0.05, witness),
    );
    expectEq(1, witness.getLength());
    expectEq(b, witness.getOperand(0));
    expectTrue(witness.isReversed());
    const both = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(both.addOperand(a));
    expectTrue(both.addOperand(c));
    // The scaled quadric reaches z = 0.75 on the axis; the box reaches z = 1.
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classify(both, new Vector3Dd(0, 0, 0.76), 0.05));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(both, new Vector3Dd(0, 0, 0.9), 0.05));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(both, new Vector3Dd(0, 0, 0.5), 0.05));
});

test("ConservativeBoundsContract_PlacedBoundsEncloseExactCornersAndStayExactWhenRepresentable", () => {
    const local = new GeometryBounds();
    expectTrue(local.setFinite(new Vector3Dd(-0.1, -0.3, 0.7), new Vector3Dd(0.2, 0.9, 1.3)));
    const placement = new Matrix4x4d()
        .translation(0.1, -0.7, 0.3)
        .multiply(Matrix4x4d.identityMatrix().withVal(0, 1, 0.3).withVal(1, 2, -0.7).withVal(2, 0, 0.1));
    const placed = local.transformed(placement);
    expectEq(GeometryBounds.Kind.Finite, placed.getKind());
    const lo = [placed.getMinimum().x(), placed.getMinimum().y(), placed.getMinimum().z()];
    const hi = [placed.getMaximum().x(), placed.getMaximum().y(), placed.getMaximum().z()];
    for (let corner = 0; corner < 8; ++corner) {
        const p = [corner & 1 ? 0.2 : -0.1, corner & 2 ? 0.9 : -0.3, corner & 4 ? 1.3 : 0.7];
        for (let row = 0; row < 3; ++row) {
            // The C++ test accumulates in long double; binary64 is used here.
            let value = placement.get(row, 3);
            for (let column = 0; column < 3; ++column) value += placement.get(row, column) * p[column]!;
            expectLe(lo[row]!, value);
            expectGe(hi[row]!, value);
        }
    }
    const unit = new GeometryBounds();
    expectTrue(unit.setFinite(new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1)));
    const shifted = unit.transformed(new Matrix4x4d().translation(10, 0, 0).multiply(new Matrix4x4d().scale(2, 3, 1)));
    expectEq(8, shifted.getMinimum().x());
    expectEq(12, shifted.getMaximum().x());
    expectEq(-3, shifted.getMinimum().y());
    expectEq(3, shifted.getMaximum().y());
});

test("ConservativeBoundsContract_IntersectionInflationAndBallExclusionStayConservative", () => {
    const a = new GeometryBounds();
    const b = new GeometryBounds();
    const unknown = new GeometryBounds();
    const unbounded = new GeometryBounds();
    const empty = new GeometryBounds();
    expectTrue(a.setFinite(new Vector3Dd(0, 0, 0), new Vector3Dd(2, 2, 2)));
    expectTrue(b.setFinite(new Vector3Dd(1, 1, 1), new Vector3Dd(3, 3, 3)));
    unbounded.setUnbounded();
    empty.setEmpty();
    const both = a.intersecting(b);
    expectEq(1, both.getMinimum().x());
    expectEq(2, both.getMaximum().x());
    expectEq(GeometryBounds.Kind.Finite, a.intersecting(unknown).getKind());
    expectEq(GeometryBounds.Kind.Finite, unbounded.intersecting(a).getKind());
    expectEq(GeometryBounds.Kind.Unsupported, unknown.intersecting(unknown).getKind());
    const far = new GeometryBounds();
    expectTrue(far.setFinite(new Vector3Dd(5, 5, 5), new Vector3Dd(6, 6, 6)));
    expectEq(GeometryBounds.Kind.Empty, a.intersecting(far).getKind());
    const grown = a.inflated(0.1);
    expectLe(grown.getMinimum().x(), -0.1);
    expectGe(grown.getMaximum().x(), 2.1);
    expectTrue(a.excludesBall(new Vector3Dd(2.2, 1, 1), 0.1));
    expectFalse(a.excludesBall(new Vector3Dd(2.05, 1, 1), 0.1));
    expectFalse(a.excludesBall(new Vector3Dd(1, 1, 1), 0.1));
    // Corner distance sqrt(2)*0.08 = 0.113 exceeds 0.1; per-axis checks would not.
    expectTrue(a.excludesBall(new Vector3Dd(2.08, 2.08, 1), 0.1));
    expectTrue(empty.excludesBall(new Vector3Dd(), 1));
    expectFalse(unbounded.excludesBall(new Vector3Dd(100, 0, 0), 0.1));
    expectFalse(unknown.excludesBall(new Vector3Dd(100, 0, 0), 0.1));
});

test("ConservativeBoundsContract_CsgBoundsFollowBooleanOperationsAndPlacement", () => {
    const left = new SimpleBody();
    const right = new SimpleBody();
    const plane = new SimpleBody();
    const mesh = new SimpleBody();
    left.setGeometry(new Sphere(1));
    right.setGeometry(new Sphere(1));
    right.setPosition(new Vector3Dd(1.5, 0, 0));
    plane.setGeometry(new PlanarHalfSpace(1, 0, 0, 0));
    mesh.setGeometry(new TriangleMesh());
    const a = new CsgBodyOperand(left);
    const b = new CsgBodyOperand(right);
    const c = new CsgBodyOperand(plane);
    const m = new CsgBodyOperand(mesh);
    const bounds = new GeometryBounds();
    const join = new CsgTree(CsgOperation.UNION);
    const meet = new CsgTree(CsgOperation.INTERSECTION);
    const cut = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(join.addOperand(a));
    expectTrue(join.addOperand(b));
    expectTrue(meet.addOperand(a));
    expectTrue(meet.addOperand(b));
    expectTrue(meet.addOperand(c));
    expectTrue(cut.addOperand(b));
    expectTrue(cut.addOperand(c));
    CsgSolidBounds.query(join, bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    expectLe(bounds.getMinimum().x(), -1);
    expectGe(bounds.getMaximum().x(), 2.5);
    CsgSolidBounds.query(meet, bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    expectLe(bounds.getMinimum().x(), 0.5);
    expectGe(bounds.getMaximum().x(), 1);
    expectLt(bounds.getMaximum().x(), 1.01);
    CsgSolidBounds.query(cut, bounds);
    expectLe(bounds.getMinimum().x(), 0.5);
    expectGe(bounds.getMaximum().x(), 2.5);
    const placed = new CsgPlacedExpression(join);
    const forward = new Matrix4x4d().translation(0, 10, 0).multiply(new Matrix4x4d().scale(1, 2, 1));
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(forward, forward.inverse()));
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    CsgSolidBounds.query(root, bounds);
    expectLe(bounds.getMinimum().y(), 8);
    expectGe(bounds.getMaximum().y(), 12);
    // A surface has no solid bounds; a union with it stays unknown.
    const withSurface = new CsgTree(CsgOperation.UNION);
    expectTrue(withSurface.addOperand(a));
    expectTrue(withSurface.addOperand(m));
    CsgSolidBounds.query(withSurface, bounds);
    expectEq(GeometryBounds.Kind.Unsupported, bounds.getKind());
});

test("ConservativeBoundsContract_LeafCullingDecidesOnlyProvenDisjointSolids", () => {
    const cone = new SimpleBody();
    const mesh = new SimpleBody();
    cone.setGeometry(new Cone(1, 1, 1));
    mesh.setGeometry(new TriangleMesh());
    const c = new CsgBodyOperand(cone);
    const m = new CsgBodyOperand(mesh);
    const solid = new CsgTree(CsgOperation.UNION);
    const surface = new CsgTree(CsgOperation.UNION);
    expectTrue(solid.addOperand(c));
    expectTrue(surface.addOperand(m));
    // The cone has no certified containment, but its certified bounds prove
    // that a distant ball is outside.
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(solid, new Vector3Dd(10, 0, 0), 0.1));
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(solid, new Vector3Dd(0, 0, 0.5), 0.1));
    // An open surface never acquires an exterior from its bounds.
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(surface, new Vector3Dd(10, 0, 0), 0.1));
});
