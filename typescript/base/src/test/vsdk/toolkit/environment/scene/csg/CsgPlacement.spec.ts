// Port of cpp/base/src/test/csg_placement_tests.cpp
import { test } from "vitest";
import {
    expectDoubleEq,
    expectEq,
    expectFalse,
    expectGt,
    expectLe,
    expectNe,
    expectNear,
    expectTrue,
} from "../../../../../GTestAssertions.js";
import { AffinePlacementStatus } from "vsdk/toolkit/environment/scene/AffinePlacementStatus.js";
import { AffineToleranceMap } from "vsdk/toolkit/environment/scene/AffineToleranceMap.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { CsgBodyOperand } from "vsdk/toolkit/environment/scene/csg/CsgBodyOperand.js";
import { CsgCycleTestOperand } from "../../../../../CsgCycleTestOperand.js";
import { CsgDyadic } from "vsdk/toolkit/environment/scene/csg/CsgDyadic.js";
import { CsgExactArithmetic } from "vsdk/toolkit/environment/scene/csg/CsgExactArithmetic.js";
import { CsgExactQuadric } from "vsdk/toolkit/environment/scene/csg/CsgExactQuadric.js";
import { CsgOpaqueTestExpression } from "../../../../../CsgOpaqueTestExpression.js";
import { CsgOperation } from "vsdk/toolkit/environment/scene/csg/CsgOperation.js";
import { CsgPlacedExpression } from "vsdk/toolkit/environment/scene/csg/CsgPlacedExpression.js";
import { CsgPointAttribution } from "vsdk/toolkit/environment/scene/csg/CsgPointAttribution.js";
import { CsgPointClassification } from "vsdk/toolkit/environment/scene/csg/CsgPointClassification.js";
import { CsgPointProof } from "vsdk/toolkit/environment/scene/csg/CsgPointProof.js";
import { CsgPointWitness } from "vsdk/toolkit/environment/scene/csg/CsgPointWitness.js";
import { CsgPolicyTestOperand } from "../../../../../CsgPolicyTestOperand.js";
import { CsgPolicyTestTree } from "../../../../../CsgPolicyTestTree.js";
import { CsgRayEventBatch } from "vsdk/toolkit/environment/scene/csg/CsgRayEventBatch.js";
import { CsgRayEventQuery } from "vsdk/toolkit/environment/scene/csg/CsgRayEventQuery.js";
import { CsgRayLeafBatch } from "vsdk/toolkit/environment/scene/csg/CsgRayLeafBatch.js";
import { CsgRayLeafQuery } from "vsdk/toolkit/environment/scene/csg/CsgRayLeafQuery.js";
import { CsgTestOperand } from "../../../../../CsgTestOperand.js";
import { CsgTree } from "vsdk/toolkit/environment/scene/csg/CsgTree.js";
import { Double } from "java/lang/Double.js";
import { GeometryContainment } from "vsdk/toolkit/environment/geometry/element/GeometryContainment.js";
import { Math as JavaMath } from "java/lang/Math.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { PlanarHalfSpace } from "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.js";
import { QuadricSurface } from "vsdk/toolkit/environment/geometry/volume/QuadricSurface.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayCrossingQuery } from "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.js";
import { RaySolidQueryStatus } from "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.js";
import { SimpleBody } from "vsdk/toolkit/environment/scene/SimpleBody.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { UncertifiedTestBody } from "../../../../../UncertifiedTestBody.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
test("CsgPlacedComposition_PreservesWorldToleranceUnderShearAndReflection", () => {
    const sphere = new SimpleBody();
    sphere.setGeometry(new Sphere(1));
    const leaf = new CsgBodyOperand(sphere);
    const child = new CsgTree(CsgOperation.UNION);
    expectTrue(child.addOperand(leaf));
    const placed = new CsgPlacedExpression(child);
    const shear = Matrix4x4d.identityMatrix().withVal(0, 1, 0.5);
    const unshear = Matrix4x4d.identityMatrix().withVal(0, 1, -0.5);
    const forward = new Matrix4x4d()
        .translation(4, -1, 2)
        .multiply(shear)
        .multiply(new Matrix4x4d().scale(-2, 1, 0.5));
    const inverse = new Matrix4x4d()
        .scale(-0.5, 1, 2)
        .multiply(unshear)
        .multiply(new Matrix4x4d().translation(-4, 1, -2));
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(forward, inverse));
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    expectEq(
        GeometryContainment.Inside,
        CsgPointClassification.classify(root, forward.multiply(new Vector3Dd()), 0.05),
    );
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(root, forward.multiply(new Vector3Dd()), 0));
    expectEq(
        GeometryContainment.Outside,
        CsgPointClassification.classify(root, forward.multiply(new Vector3Dd(2, 0, 0)), 0.05),
    );
    expectEq(
        GeometryContainment.Outside,
        CsgPointClassification.classify(root, forward.multiply(new Vector3Dd(2, 0, 0)), 0),
    );
    expectEq(
        GeometryContainment.Indeterminate,
        CsgPointClassification.classify(root, forward.multiply(new Vector3Dd(1, 0, 0)), 0),
    );
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classify(root, forward.multiply(new Vector3Dd(1.01, 0, 0)), 0.1),
    );
    expectEq(
        AffinePlacementStatus.Invalid,
        placed.setAffinePlacement(new Matrix4x4d().scale(0, 1, 1), new Matrix4x4d()),
    );
    expectEq(
        GeometryContainment.Inside,
        CsgPointClassification.classify(root, forward.multiply(new Vector3Dd()), 0.05),
    );
    expectFalse(child.addOperand(placed));
});

test("CsgPlacedComposition_NestedDifferenceUsesOrderedOrientation", () => {
    const big = new SimpleBody();
    const small = new SimpleBody();
    big.setGeometry(new Sphere(2));
    small.setGeometry(new Sphere(1));
    const a = new CsgBodyOperand(big);
    const b = new CsgBodyOperand(small);
    const difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(difference.addOperand(a));
    expectTrue(difference.addOperand(b));
    const placed = new CsgPlacedExpression(difference);
    const forward = new Matrix4x4d().translation(10, 0, 0).multiply(new Matrix4x4d().scale(2, 1, 1));
    const inverse = new Matrix4x4d().scale(0.5, 1, 1).multiply(new Matrix4x4d().translation(-10, 0, 0));
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(forward, inverse));
    const root = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(root.addOperand(placed));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(root, new Vector3Dd(10, 0, 0), 0.05));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(root, new Vector3Dd(13, 0, 0), 0.05));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(root, new Vector3Dd(15, 0, 0), 0.05));
});

test("AffineToleranceMap_EnclosesInverseImagesAndDoesNotMutateOnFailure", () => {
    const forward = new Matrix4x4d()
        .translation(4, -1, 2)
        .multiply(Matrix4x4d.identityMatrix().withVal(0, 1, 0.5))
        .multiply(new Matrix4x4d().scale(-2, 1, 0.5));
    const inverse = new Matrix4x4d()
        .scale(-0.5, 1, 2)
        .multiply(Matrix4x4d.identityMatrix().withVal(0, 1, -0.5))
        .multiply(new Matrix4x4d().translation(-4, 1, -2));
    const point = new Vector3Dd(4.3, -0.7, 2.1);
    const center: Vector3Dd[] = [new Vector3Dd()];
    const bound: number[] = [0];
    const inner: number[] = [-1];
    expectEq(AffinePlacementStatus.Valid, AffineToleranceMap.map(forward, point, 0.4, center, bound, inner));
    expectGt(inner[0]!, 0);
    for (let x = -1; x <= 1; ++x)
        for (let y = -1; y <= 1; ++y)
            for (let z = -1; z <= 1; ++z) {
                const length = Math.sqrt(x * x + y * y + z * z);
                if (length === 0) continue;
                const sample = point.add(new Vector3Dd(x, y, z).multiply(0.4 / length));
                expectLe(inverse.multiply(sample).subtract(center[0]!).length(), bound[0]!);
                const childSample = center[0]!.add(new Vector3Dd(x, y, z).multiply(inner[0]! / length));
                expectLe(forward.multiply(childSample).subtract(point).length(), 0.4);
            }
    center[0] = new Vector3Dd(7, 8, 9);
    bound[0] = 42;
    expectEq(
        AffinePlacementStatus.Indeterminate,
        AffineToleranceMap.map(new Matrix4x4d().scale(0, 1, 1), point, 0.4, center, bound),
    );
    expectDoubleEq(7, center[0]!.x());
    expectDoubleEq(42, bound[0]!);
    expectEq(AffinePlacementStatus.Invalid, AffineToleranceMap.map(forward, point, -1, center, bound));
    expectDoubleEq(7, center[0]!.x());
    expectDoubleEq(42, bound[0]!);
});

test("CsgPointWitnessContract_RecordsDecisivePathAndDifferenceOrientation", () => {
    const outer = new SimpleBody();
    const inner = new SimpleBody();
    outer.setGeometry(new Sphere(2));
    inner.setGeometry(new Sphere(1));
    const a = new CsgBodyOperand(outer);
    const b = new CsgBodyOperand(inner);
    const difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(difference.addOperand(a));
    expectTrue(difference.addOperand(b));
    const placed = new CsgPlacedExpression(difference);
    const forward = new Matrix4x4d().translation(3, 0, 0);
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(forward, new Matrix4x4d().translation(-3, 0, 0)));
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    const witness = new CsgPointWitness();
    expectEq(
        GeometryContainment.Outside,
        CsgPointClassification.classifyWithWitness(root, new Vector3Dd(3, 0, 0), 0.1, witness),
    );
    expectEq(2, witness.getLength());
    expectEq(placed, witness.getOperand(0));
    expectEq(b, witness.getOperand(1));
    expectTrue(witness.isReversed());
    expectEq(null, witness.getOperand(-1));
    expectEq(null, witness.getOperand(2));
    // Inside a difference requires both operands, so a single-leaf proof is unavailable.
    expectEq(
        GeometryContainment.Inside,
        CsgPointClassification.classifyWithWitness(root, new Vector3Dd(4.5, 0, 0), 0.05, witness),
    );
    expectFalse(witness.isAvailable());
    expectFalse(witness.isReversed());
    expectEq(
        GeometryContainment.InvalidInput,
        CsgPointClassification.classifyWithWitness(root, new Vector3Dd(), -1, witness),
    );
    expectFalse(witness.isAvailable());
});

test("CsgPointWitnessContract_CertifiesOnlyExposedBoundaries", () => {
    const outer = new SimpleBody();
    const inner = new SimpleBody();
    const distant = new SimpleBody();
    outer.setGeometry(new Sphere(2));
    inner.setGeometry(new Sphere(1));
    distant.setGeometry(new Sphere(1));
    distant.setPosition(new Vector3Dd(10, 0, 0));
    const a = new CsgBodyOperand(outer);
    const b = new CsgBodyOperand(inner);
    const c = new CsgBodyOperand(distant);
    const united = new CsgTree(CsgOperation.UNION);
    const intersected = new CsgTree(CsgOperation.INTERSECTION);
    const difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(united.addOperand(a));
    expectTrue(united.addOperand(c));
    expectTrue(intersected.addOperand(a));
    expectTrue(intersected.addOperand(b));
    expectTrue(difference.addOperand(a));
    expectTrue(difference.addOperand(b));
    const witness = new CsgPointWitness();
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithWitness(united, new Vector3Dd(2, 0, 0), 0.01, witness),
    );
    expectEq(1, witness.getLength());
    expectEq(a, witness.getOperand(0));
    expectFalse(witness.isReversed());
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithWitness(intersected, new Vector3Dd(1, 0, 0), 0.01, witness),
    );
    expectEq(1, witness.getLength());
    expectEq(b, witness.getOperand(0));
    expectFalse(witness.isReversed());
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithWitness(difference, new Vector3Dd(1, 0, 0), 0.01, witness),
    );
    expectEq(1, witness.getLength());
    expectEq(b, witness.getOperand(0));
    expectTrue(witness.isReversed());
    const repeatedInner = new CsgBodyOperand(inner);
    expectTrue(difference.addOperand(repeatedInner));
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithWitness(difference, new Vector3Dd(1, 0, 0), 0.01, witness),
    );
    expectEq(1, witness.getLength());
    expectEq(b, witness.getOperand(0));
    expectTrue(witness.isReversed());
    const coincident = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(coincident.addOperand(b));
    expectTrue(coincident.addOperand(b));
    expectEq(
        GeometryContainment.Outside,
        CsgPointClassification.classifyWithWitness(coincident, new Vector3Dd(1, 0, 0), 0.01, witness),
    );
    expectFalse(witness.isAvailable());
});

test("CsgPlacedComposition_TraversesMultiplePlacementsAndRetainsWitnessPath", () => {
    const sphere = new SimpleBody();
    sphere.setGeometry(new Sphere(1));
    const leaf = new CsgBodyOperand(sphere);
    const inner = new CsgTree(CsgOperation.UNION);
    expectTrue(inner.addOperand(leaf));
    const first = new CsgPlacedExpression(inner);
    expectEq(
        AffinePlacementStatus.Valid,
        first.setAffinePlacement(new Matrix4x4d().translation(2, 0, 0), new Matrix4x4d().translation(-2, 0, 0)),
    );
    const middle = new CsgTree(CsgOperation.UNION);
    expectTrue(middle.addOperand(first));
    const second = new CsgPlacedExpression(middle);
    expectEq(
        AffinePlacementStatus.Valid,
        second.setAffinePlacement(new Matrix4x4d().scale(-2, 1, 1), new Matrix4x4d().scale(-0.5, 1, 1)),
    );
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(second));
    const witness = new CsgPointWitness();
    expectEq(
        GeometryContainment.Inside,
        CsgPointClassification.classifyWithWitness(root, new Vector3Dd(-4, 0, 0), 0.1, witness),
    );
    expectEq(3, witness.getLength());
    expectEq(second, witness.getOperand(0));
    expectEq(first, witness.getOperand(1));
    expectEq(leaf, witness.getOperand(2));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(root, new Vector3Dd(2, 0, 0), 0.1));
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classify(root, new Vector3Dd(-6.01, 0, 0), 0.1));
});

test("AffineToleranceMap_NearSingularMappingPreservesFailureOutputs", () => {
    // The exact determinant of these binary64 entries is zero, but the rounded
    // products cannot decide its sign.
    const nearlySingular = Matrix4x4d.identityMatrix()
        .withVal(0, 0, 0.1)
        .withVal(0, 1, 0.3)
        .withVal(1, 0, 0.2)
        .withVal(1, 1, 0.6);
    const point: Vector3Dd[] = [new Vector3Dd(7, 8, 9)];
    const tolerance: number[] = [42];
    expectEq(
        AffinePlacementStatus.Indeterminate,
        AffineToleranceMap.map(nearlySingular, new Vector3Dd(), 0.1, point, tolerance),
    );
    expectDoubleEq(7, point[0]!.x());
    expectDoubleEq(42, tolerance[0]!);
    // An exactly representable determinant 2^-52 is decided, however small.
    const tiny = Matrix4x4d.identityMatrix()
        .withVal(0, 0, 1)
        .withVal(0, 1, 1)
        .withVal(1, 0, 1)
        .withVal(1, 1, JavaMath.nextAfter(1, 2));
    expectEq(AffinePlacementStatus.Valid, AffineToleranceMap.map(tiny, new Vector3Dd(), 0.1, point, tolerance));
});

test("AffineToleranceMap_EnclosesSamplesAcrossScalesAndShears", () => {
    const scales = [0.01, 0.5, 2, 100];
    for (let i = 0; i < 4; ++i)
        for (let j = 0; j < 4; ++j) {
            const sx = scales[i];
            const sy = scales[j];
            const sz = i % 2 ? -1.5 : 1.5;
            const forward = new Matrix4x4d()
                .translation(5, -3, 2)
                .multiply(Matrix4x4d.identityMatrix().withVal(0, 1, 0.75).withVal(1, 2, -0.25))
                .multiply(new Matrix4x4d().scale(sx, sy, sz));
            const inverse = new Matrix4x4d()
                .scale(1 / sx, 1 / sy, 1 / sz)
                .multiply(Matrix4x4d.identityMatrix().withVal(0, 1, -0.75).withVal(1, 2, 0.25).withVal(0, 2, -0.1875))
                .multiply(new Matrix4x4d().translation(-5, 3, -2));
            const point = new Vector3Dd(5.25, -3.5, 2.125);
            const center: Vector3Dd[] = [new Vector3Dd()];
            const outer: number[] = [0];
            const inner: number[] = [-1];
            expectEq(AffinePlacementStatus.Valid, AffineToleranceMap.map(forward, point, 0.25, center, outer, inner));
            expectGt(inner[0]!, 0);
            for (let x = -1; x <= 1; ++x)
                for (let y = -1; y <= 1; ++y)
                    for (let z = -1; z <= 1; ++z) {
                        const length = Math.sqrt(x * x + y * y + z * z);
                        if (length === 0) continue;
                        const direction = new Vector3Dd(x, y, z).multiply(1 / length);
                        const parentSample = point.add(direction.multiply(0.25));
                        expectLe(inverse.multiply(parentSample).subtract(center[0]!).length(), outer[0]!);
                        const childSample = center[0]!.add(direction.multiply(inner[0]!));
                        expectLe(forward.multiply(childSample).subtract(point).length(), 0.25);
                    }
        }
});

test("CsgPlacedComposition_RotatedBoundaryKeepsItsWitness", () => {
    const sphere = new SimpleBody();
    sphere.setGeometry(new Sphere(1));
    const leaf = new CsgBodyOperand(sphere);
    const child = new CsgTree(CsgOperation.UNION);
    expectTrue(child.addOperand(leaf));
    const placed = new CsgPlacedExpression(child);
    const rotation = Matrix4x4d.identityMatrix().withVal(0, 0, 0).withVal(0, 1, -1).withVal(1, 0, 1).withVal(1, 1, 0);
    const inverseRotation = rotation.transpose();
    const forward = new Matrix4x4d()
        .translation(2, 3, 0)
        .multiply(rotation)
        .multiply(new Matrix4x4d().scale(2, 1, 1));
    const inverse = new Matrix4x4d()
        .scale(0.5, 1, 1)
        .multiply(inverseRotation)
        .multiply(new Matrix4x4d().translation(-2, -3, 0));
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(forward, inverse));
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    const witness = new CsgPointWitness();
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithWitness(root, forward.multiply(new Vector3Dd(1, 0, 0)), 0.1, witness),
    );
    expectEq(2, witness.getLength());
    expectEq(placed, witness.getOperand(0));
    expectEq(leaf, witness.getOperand(1));
    expectFalse(witness.isReversed());
});

test("CsgPlacedComposition_InnerProbeDoesNotHideErrors", () => {
    const invalid = new CsgTestOperand(GeometryContainment.Indeterminate, GeometryContainment.InvalidInput, 0.08);
    const unsupported = new CsgTestOperand(GeometryContainment.Indeterminate, GeometryContainment.Unsupported, 0.08);
    const invalidChild = new CsgTree(CsgOperation.UNION);
    const unsupportedChild = new CsgTree(CsgOperation.UNION);
    expectTrue(invalidChild.addOperand(invalid));
    expectTrue(unsupportedChild.addOperand(unsupported));
    const placedInvalid = new CsgPlacedExpression(invalidChild);
    const placedUnsupported = new CsgPlacedExpression(unsupportedChild);
    const forward = new Matrix4x4d().scale(2, 1, 1);
    const inverse = new Matrix4x4d().scale(0.5, 1, 1);
    expectEq(AffinePlacementStatus.Valid, placedInvalid.setAffinePlacement(forward, inverse));
    expectEq(AffinePlacementStatus.Valid, placedUnsupported.setAffinePlacement(forward, inverse));
    const rootInvalid = new CsgTree(CsgOperation.UNION);
    const rootUnsupported = new CsgTree(CsgOperation.UNION);
    expectTrue(rootInvalid.addOperand(placedInvalid));
    expectTrue(rootUnsupported.addOperand(placedUnsupported));
    expectEq(GeometryContainment.InvalidInput, CsgPointClassification.classify(rootInvalid, new Vector3Dd(), 0.1));
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(rootUnsupported, new Vector3Dd(), 0.1));
});

test("CsgPointProofContract_RecordsConjunctiveLeafFactsAndBoundaryOrientation", () => {
    const outer = new SimpleBody();
    const inner = new SimpleBody();
    const distant = new SimpleBody();
    outer.setGeometry(new Sphere(2));
    inner.setGeometry(new Sphere(1));
    distant.setGeometry(new Sphere(1));
    distant.setPosition(new Vector3Dd(10, 0, 0));
    const a = new CsgBodyOperand(outer);
    const b = new CsgBodyOperand(inner);
    const c = new CsgBodyOperand(distant);
    const united = new CsgTree(CsgOperation.UNION);
    const intersection = new CsgTree(CsgOperation.INTERSECTION);
    const difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(united.addOperand(a));
    expectTrue(united.addOperand(c));
    expectTrue(intersection.addOperand(a));
    expectTrue(intersection.addOperand(b));
    expectTrue(difference.addOperand(a));
    expectTrue(difference.addOperand(b));
    const proof = new CsgPointProof();
    expectEq(
        GeometryContainment.Outside,
        CsgPointClassification.classifyWithProof(united, new Vector3Dd(5, 0, 0), 0.1, proof),
    );
    expectTrue(proof.isAvailable());
    expectEq(2, proof.getCount());
    expectEq(GeometryContainment.Outside, proof.getEvidence(0)!.getClassification());
    expectEq(GeometryContainment.Outside, proof.getEvidence(1)!.getClassification());
    expectEq(a, proof.getEvidence(0)!.getPath().getOperand(0));
    expectEq(c, proof.getEvidence(1)!.getPath().getOperand(0));
    expectDoubleEq(5, proof.getEvidence(0)!.getPoint().x());
    expectDoubleEq(0.1, proof.getEvidence(0)!.getTolerance());
    expectEq(
        GeometryContainment.Inside,
        CsgPointClassification.classifyWithProof(intersection, new Vector3Dd(), 0.1, proof),
    );
    expectEq(2, proof.getCount());
    expectEq(GeometryContainment.Inside, proof.getEvidence(0)!.getClassification());
    expectEq(GeometryContainment.Inside, proof.getEvidence(1)!.getClassification());
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithProof(difference, new Vector3Dd(1, 0, 0), 0.01, proof),
    );
    expectEq(2, proof.getCount());
    expectEq(GeometryContainment.Boundary, proof.getEvidence(1)!.getClassification());
    expectTrue(proof.getEvidence(1)!.getPath().isReversed());
    expectEq(null, proof.getEvidence(2));
    expectEq(
        GeometryContainment.InvalidInput,
        CsgPointClassification.classifyWithProof(difference, new Vector3Dd(), -1, proof),
    );
    expectFalse(proof.isAvailable());
});
test("CsgPointProofContract_SimplifiesEqualSpheresButNotMerelyNearbySurfaces", () => {
    const solid = new SimpleBody();
    const equalButDistinct = new SimpleBody();
    const almostEqual = new SimpleBody();
    solid.setGeometry(new Sphere(1));
    equalButDistinct.setGeometry(new Sphere(1));
    almostEqual.setGeometry(new Sphere(1));
    almostEqual.setPosition(new Vector3Dd(0.0001, 0, 0));
    const a = new CsgBodyOperand(solid);
    const alias = new CsgBodyOperand(solid);
    const other = new CsgBodyOperand(equalButDistinct);
    const nearOther = new CsgBodyOperand(almostEqual);
    const united = new CsgTree(CsgOperation.UNION);
    const intersection = new CsgTree(CsgOperation.INTERSECTION);
    const difference = new CsgTree(CsgOperation.DIFFERENCE);
    const unknown = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(united.addOperand(a));
    expectTrue(united.addOperand(alias));
    expectTrue(intersection.addOperand(a));
    expectTrue(intersection.addOperand(alias));
    expectTrue(difference.addOperand(a));
    expectTrue(difference.addOperand(alias));
    expectTrue(unknown.addOperand(a));
    expectTrue(unknown.addOperand(nearOther));
    const proof = new CsgPointProof();
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithProof(united, new Vector3Dd(1, 0, 0), 0.01, proof),
    );
    expectEq(2, proof.getCount());
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classify(intersection, new Vector3Dd(1, 0, 0), 0.01));
    expectEq(
        GeometryContainment.Outside,
        CsgPointClassification.classifyWithProof(difference, new Vector3Dd(1, 0, 0), 0.01, proof),
    );
    expectEq(2, proof.getCount());
    expectTrue(proof.getEvidence(1)!.getPath().isReversed());
    const equalGeometry = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(equalGeometry.addOperand(a));
    expectTrue(equalGeometry.addOperand(other));
    expectEq(
        GeometryContainment.Outside,
        CsgPointClassification.classifyWithProof(equalGeometry, new Vector3Dd(1, 0, 0), 0.01, proof),
    );
    expectEq(2, proof.getCount());
    expectEq(
        GeometryContainment.Indeterminate,
        CsgPointClassification.classifyWithProof(unknown, new Vector3Dd(1, 0, 0), 0.01, proof),
    );
    expectFalse(proof.isAvailable());
    const uncertain = new CsgTestOperand(GeometryContainment.Indeterminate);
    const selfWithUnknown = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(selfWithUnknown.addOperand(a));
    expectTrue(selfWithUnknown.addOperand(alias));
    expectTrue(selfWithUnknown.addOperand(uncertain));
    expectEq(
        GeometryContainment.Outside,
        CsgPointClassification.classifyWithProof(selfWithUnknown, new Vector3Dd(1, 0, 0), 0.01, proof),
    );
    expectEq(2, proof.getCount());
});

test("CsgRayLeafQueryContract_CollectsPlacedDifferenceAndPreservesOrientation", () => {
    const outer = new SimpleBody();
    const inner = new SimpleBody();
    outer.setGeometry(new Sphere(2));
    inner.setGeometry(new Sphere(1));
    const a = new CsgBodyOperand(outer);
    const b = new CsgBodyOperand(inner);
    const difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(difference.addOperand(a));
    expectTrue(difference.addOperand(b));
    const placed = new CsgPlacedExpression(difference);
    const forward = new Matrix4x4d().translation(3, 0, 0).multiply(new Matrix4x4d().scale(2, 1, 1));
    const inverse = new Matrix4x4d().scale(0.5, 1, 1).multiply(new Matrix4x4d().translation(-3, 0, 0));
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(forward, inverse));
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    const batch = new CsgRayLeafBatch();
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRayLeafQuery.query(
            root,
            Ray.fromAffine(new Vector3Dd(-5, 0, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, 20),
            batch,
        ),
    );
    expectEq(2, batch.size());
    const first = batch.get(0)!;
    const second = batch.get(1)!;
    expectEq(2, first.getPath().getLength());
    expectEq(2, second.getPath().getLength());
    expectEq(placed, first.getPath().getOperand(0));
    expectEq(a, first.getPath().getOperand(1));
    expectEq(b, second.getPath().getOperand(1));
    expectFalse(first.getPath().isReversed());
    expectTrue(second.getPath().isReversed());
    const out = first.getClassification();
    const inside = second.getClassification();
    expectEq(2, out.boundaries.size());
    expectEq(2, inside.boundaries.size());
    expectEq(3, out.insideSegments.size());
    expectEq(3, inside.insideSegments.size());
    expectDoubleEq(4, out.boundaries.get(0)!.t);
    expectDoubleEq(12, out.boundaries.get(1)!.t);
    expectDoubleEq(6, inside.boundaries.get(0)!.t);
    expectDoubleEq(10, inside.boundaries.get(1)!.t);
    expectFalse(out.insideSegments.get(0));
    expectTrue(out.insideSegments.get(1));
    expectFalse(out.insideSegments.get(2));
    expectFalse(inside.insideSegments.get(0));
    expectTrue(inside.insideSegments.get(1));
    expectFalse(inside.insideSegments.get(2));
    expectFalse(out.boundaries.get(0)!.normalReversed);
    expectTrue(inside.boundaries.get(0)!.normalReversed);
    expectEq(1, out.boundaries.get(0)!.getInstanceCount());
    expectEq(1, first.getPlacementCount());
    const parent = first.getPlacement(0);
    expectNe(null, parent);
    expectEq(placed, parent!.operand);
    expectDoubleEq(3, parent!.childToParent.multiply(new Vector3Dd()).x());
});
test("CsgRayLeafQueryContract_KeepsCoincidentLeavesAndClearsResultsOnFailure", () => {
    const sphere = new SimpleBody();
    sphere.setGeometry(new Sphere(1));
    const a = new CsgBodyOperand(sphere);
    const alias = new CsgBodyOperand(sphere);
    const equal = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(equal.addOperand(a));
    expectTrue(equal.addOperand(alias));
    const batch = new CsgRayLeafBatch();
    const ray = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0));
    expectEq(RaySolidQueryStatus.Supported, CsgRayLeafQuery.query(equal, ray, new RayCrossingQuery(0, 10), batch));
    expectEq(2, batch.size());
    expectDoubleEq(
        batch.get(0)!.getClassification().boundaries.get(0)!.t,
        batch.get(1)!.getClassification().boundaries.get(0)!.t,
    );
    expectFalse(batch.get(0)!.getPath().isReversed());
    expectTrue(batch.get(1)!.getPath().isReversed());
    const unsupported = new CsgTestOperand(GeometryContainment.Indeterminate);
    const partial = new CsgTree(CsgOperation.UNION);
    expectTrue(partial.addOperand(a));
    expectTrue(partial.addOperand(unsupported));
    expectEq(RaySolidQueryStatus.Unsupported, CsgRayLeafQuery.query(partial, ray, new RayCrossingQuery(0, 10), batch));
    expectEq(0, batch.size());
    unsupported.setMalformedRay(true);
    expectEq(
        RaySolidQueryStatus.Indeterminate,
        CsgRayLeafQuery.query(partial, ray, new RayCrossingQuery(0, 10), batch),
    );
    expectEq(0, batch.size());
    expectEq(RaySolidQueryStatus.InvalidInput, CsgRayLeafQuery.query(equal, ray, new RayCrossingQuery(1, 1), batch));
    expectEq(0, batch.size());
});

test("CsgRayLeafQueryContract_NestedDifferencesPreserveParityAndLeafParameter", () => {
    const outer = new SimpleBody();
    const middle = new SimpleBody();
    const inner = new SimpleBody();
    outer.setGeometry(new Sphere(3));
    middle.setGeometry(new Sphere(2));
    inner.setGeometry(new Sphere(1));
    const a = new CsgBodyOperand(outer);
    const b = new CsgBodyOperand(middle);
    const c = new CsgBodyOperand(inner);
    const nested = new CsgTree(CsgOperation.DIFFERENCE);
    const root = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(nested.addOperand(b));
    expectTrue(nested.addOperand(c));
    expectTrue(root.addOperand(a));
    expectTrue(root.addOperand(nested));
    const batch = new CsgRayLeafBatch();
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRayLeafQuery.query(
            root,
            Ray.fromAffine(new Vector3Dd(-5, 0, 0), new Vector3Dd(2, 0, 0)),
            new RayCrossingQuery(0, 10),
            batch,
        ),
    );
    expectEq(3, batch.size());
    expectFalse(batch.get(0)!.getPath().isReversed());
    expectTrue(batch.get(1)!.getPath().isReversed());
    expectFalse(batch.get(2)!.getPath().isReversed());
    expectTrue(batch.get(1)!.getClassification().boundaries.get(0)!.normalReversed);
    expectFalse(batch.get(2)!.getClassification().boundaries.get(0)!.normalReversed);
    expectDoubleEq(1, batch.get(0)!.getClassification().boundaries.get(0)!.t);
    expectDoubleEq(1.5, batch.get(1)!.getClassification().boundaries.get(0)!.t);
    expectDoubleEq(2, batch.get(2)!.getClassification().boundaries.get(0)!.t);
});

test("CsgRayLeafQueryContract_CollectsBasicSolidFamiliesAndEndpointEvents", () => {
    const box = new SimpleBody();
    const halfspace = new SimpleBody();
    const quadric = new SimpleBody();
    box.setGeometry(new Box(2, 2, 2));
    halfspace.setGeometry(new PlanarHalfSpace(1, 0, 0, 0));
    quadric.setGeometry(new QuadricSurface(new Vector3Dd(1, 1, 1), new Vector3Dd(), new Vector3Dd(), -1));
    const a = new CsgBodyOperand(box);
    const b = new CsgBodyOperand(halfspace);
    const c = new CsgBodyOperand(quadric);
    const root = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(root.addOperand(a));
    expectTrue(root.addOperand(b));
    expectTrue(root.addOperand(c));
    const batch = new CsgRayLeafBatch();
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRayLeafQuery.query(
            root,
            Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(2, 4),
            batch,
        ),
    );
    expectEq(3, batch.size());
    expectEq(2, batch.get(0)!.getClassification().boundaries.size());
    expectEq(1, batch.get(1)!.getClassification().boundaries.size());
    expectEq(2, batch.get(2)!.getClassification().boundaries.size());
    expectDoubleEq(2, batch.get(0)!.getClassification().boundaries.get(0)!.t);
    expectDoubleEq(4, batch.get(0)!.getClassification().boundaries.get(1)!.t);
    expectDoubleEq(3, batch.get(1)!.getClassification().boundaries.get(0)!.t);
    expectDoubleEq(2, batch.get(2)!.getClassification().boundaries.get(0)!.t);
    expectDoubleEq(4, batch.get(2)!.getClassification().boundaries.get(1)!.t);
});

test("CsgRayEventQueryContract_GroupsExactTiesWithoutMergingNearbyEvents", () => {
    const same = new SimpleBody();
    const shifted = new SimpleBody();
    same.setGeometry(new Sphere(1));
    shifted.setGeometry(new Sphere(1));
    shifted.setPosition(new Vector3Dd(0.000001, 0, 0));
    const a = new CsgBodyOperand(same);
    const alias = new CsgBodyOperand(same);
    const b = new CsgBodyOperand(shifted);
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(a));
    expectTrue(root.addOperand(alias));
    expectTrue(root.addOperand(b));
    const leaves = new CsgRayLeafBatch();
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRayLeafQuery.query(
            root,
            Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, 10),
            leaves,
        ),
    );
    const groups = new CsgRayEventBatch();
    expectEq(RaySolidQueryStatus.Supported, CsgRayEventQuery.group(leaves, groups));
    expectEq(4, groups.size());
    expectDoubleEq(2, groups.get(0)!.getT());
    expectDoubleEq(4, groups.get(2)!.getT());
    expectEq(2, groups.get(0)!.size());
    expectEq(2, groups.get(2)!.size());
    expectEq(0, groups.get(0)!.get(0)!.leafIndex);
    expectEq(0, groups.get(0)!.get(0)!.boundaryIndex);
    expectEq(1, groups.get(0)!.get(1)!.leafIndex);
    expectEq(0, groups.get(0)!.get(1)!.boundaryIndex);
    expectEq(2, groups.get(1)!.get(0)!.leafIndex);
    expectGt(groups.get(1)!.getT(), groups.get(0)!.getT());
    expectGt(groups.get(3)!.getT(), groups.get(2)!.getT());
    leaves.get(2)!.getClassification().insideSegments.clear();
    expectEq(RaySolidQueryStatus.Indeterminate, CsgRayEventQuery.group(leaves, groups));
    expectEq(0, groups.size());
});
test("CsgRayEventQueryContract_EmptyLeafBoundariesProduceEmptyEventBatch", () => {
    const solid = new SimpleBody();
    solid.setGeometry(new Sphere(1));
    const operand = new CsgBodyOperand(solid);
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(operand));
    const leaves = new CsgRayLeafBatch();
    const groups = new CsgRayEventBatch();
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRayLeafQuery.query(
            root,
            Ray.fromAffine(new Vector3Dd(0, 0, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, 0.5),
            leaves,
        ),
    );
    expectEq(1, leaves.size());
    expectTrue(leaves.get(0)!.getClassification().insideSegments.get(0));
    expectEq(RaySolidQueryStatus.Supported, CsgRayEventQuery.group(leaves, groups));
    expectEq(0, groups.size());
});

test("CsgPointProofContract_CarriesAllLeafFactsThroughPlacedNestedExpression", () => {
    const left = new SimpleBody();
    const right = new SimpleBody();
    left.setGeometry(new Sphere(1));
    right.setGeometry(new Sphere(1));
    right.setPosition(new Vector3Dd(10, 0, 0));
    const a = new CsgBodyOperand(left);
    const b = new CsgBodyOperand(right);
    const child = new CsgTree(CsgOperation.UNION);
    expectTrue(child.addOperand(a));
    expectTrue(child.addOperand(b));
    const placed = new CsgPlacedExpression(child);
    const forward = new Matrix4x4d().translation(3, 0, 0).multiply(new Matrix4x4d().scale(2, 1, 1));
    const inverse = new Matrix4x4d().scale(0.5, 1, 1).multiply(new Matrix4x4d().translation(-3, 0, 0));
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(forward, inverse));
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    const proof = new CsgPointProof();
    expectEq(
        GeometryContainment.Outside,
        CsgPointClassification.classifyWithProof(root, new Vector3Dd(13, 0, 0), 0.1, proof),
    );
    expectEq(2, proof.getCount());
    expectEq(placed, proof.getEvidence(0)!.getPath().getOperand(0));
    expectEq(a, proof.getEvidence(0)!.getPath().getOperand(1));
    expectEq(placed, proof.getEvidence(1)!.getPath().getOperand(0));
    expectEq(b, proof.getEvidence(1)!.getPath().getOperand(1));
    expectNear(5, proof.getEvidence(0)!.getPoint().x(), 1e-12);
    expectGt(proof.getEvidence(0)!.getTolerance(), 0.05);
});

test("CsgExactArithmeticContract_ComparesDyadicProductsAndAffineEndpointsExactly", () => {
    expectTrue(CsgExactArithmetic.equalProducts(0.5, 6, 3, 1));
    expectFalse(CsgExactArithmetic.equalProducts(0.1, 0.1, 0.01, 1));
    expectTrue(CsgExactArithmetic.equalProducts(Double.MIN_VALUE, 2, JavaMath.scalb(Double.MIN_VALUE, 1), 1));
    expectFalse(CsgExactArithmetic.equalProducts(Double.MIN_VALUE, 0.5, 0, 1));
    expectTrue(CsgExactArithmetic.equalProducts(Double.MAX_VALUE, 2, 2, Double.MAX_VALUE));
    expectFalse(CsgExactArithmetic.equalProducts(Double.MAX_VALUE, 2, Double.MAX_VALUE, 1));
    expectTrue(CsgExactArithmetic.equalAffine(2, 3, -5, 1, 1, 0));
    expectFalse(CsgExactArithmetic.equalAffine(1, Double.MAX_VALUE, 1, 1, Double.MAX_VALUE, 0));
    expectFalse(CsgExactArithmetic.equalProducts(Double.NaN, 1, 1, 1));
});

test("CsgSolidEquivalenceContract_SpheresSupportPermutationsReflectionsAndRadiusScale", () => {
    const a = new SimpleBody();
    const b = new SimpleBody();
    const nearby = new SimpleBody();
    a.setGeometry(new Sphere(2));
    b.setGeometry(new Sphere(1));
    nearby.setGeometry(new Sphere(1));
    const rotation = Matrix4x4d.identityMatrix().withVal(0, 0, 0).withVal(0, 1, -1).withVal(1, 0, 1).withVal(1, 1, 0);
    const placement = new Matrix4x4d()
        .translation(3, 0, 0)
        .multiply(rotation)
        .multiply(new Matrix4x4d().scale(2, 2, 2));
    const inverse = new Matrix4x4d()
        .scale(0.5, 0.5, 0.5)
        .multiply(rotation.transpose())
        .multiply(new Matrix4x4d().translation(-3, 0, 0));
    a.setPosition(new Vector3Dd(3, 0, 0));
    expectEq(AffinePlacementStatus.Valid, b.setAffinePlacement(placement, inverse));
    nearby.setPosition(new Vector3Dd(3.00000001, 0, 0));
    nearby.setScale(new Vector3Dd(2, 2, 2));
    const aa = new CsgBodyOperand(a);
    const bb = new CsgBodyOperand(b);
    const cc = new CsgBodyOperand(nearby);
    expectTrue(aa.isSameSolid(bb));
    expectTrue(bb.isSameSolid(aa));
    expectFalse(aa.isSameSolid(cc));
    const difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(difference.addOperand(aa));
    expectTrue(difference.addOperand(bb));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(difference, new Vector3Dd(5, 0, 0), 0.1));
});

test("CsgSolidEquivalenceContract_BoxesSupportTranslatedBoundsAndAffineEdgeReparameterization", () => {
    const a = new SimpleBody();
    const b = new SimpleBody();
    const c = new SimpleBody();
    const shearA = new SimpleBody();
    const shearB = new SimpleBody();
    a.setGeometry(new Box(new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1)));
    b.setGeometry(new Box(new Vector3Dd(0, 0, 0), new Vector3Dd(4, 4, 4)));
    b.setPosition(new Vector3Dd(-1, -1, -1));
    b.setScale(new Vector3Dd(0.5, 0.5, 0.5));
    c.setGeometry(new Box(new Vector3Dd(0, 0, 0), new Vector3Dd(4, 4, 4)));
    c.setPosition(new Vector3Dd(-1 + 1e-12, -1, -1));
    c.setScale(new Vector3Dd(0.5, 0.5, 0.5));
    const aa = new CsgBodyOperand(a);
    const bb = new CsgBodyOperand(b);
    const cc = new CsgBodyOperand(c);
    expectTrue(aa.isSameSolid(bb));
    expectTrue(bb.isSameSolid(aa));
    expectFalse(aa.isSameSolid(cc));
    shearA.setGeometry(new Box(new Vector3Dd(-1, -2, -3), new Vector3Dd(1, 2, 3)));
    shearB.setGeometry(new Box(new Vector3Dd(-0.5, -2, -3), new Vector3Dd(0.5, 2, 3)));
    const shear = Matrix4x4d.identityMatrix().withVal(0, 1, 0.5);
    const unshear = Matrix4x4d.identityMatrix().withVal(0, 1, -0.5);
    expectEq(AffinePlacementStatus.Valid, shearA.setAffinePlacement(shear, unshear));
    expectEq(
        AffinePlacementStatus.Valid,
        shearB.setAffinePlacement(
            shear.multiply(new Matrix4x4d().scale(2, 1, 1)),
            new Matrix4x4d().scale(0.5, 1, 1).multiply(unshear),
        ),
    );
    const sa = new CsgBodyOperand(shearA);
    const sb = new CsgBodyOperand(shearB);
    expectTrue(sa.isSameSolid(sb));
    expectTrue(sb.isSameSolid(sa));
});

test("CsgSolidEquivalenceContract_HalfSpacesAndQuadricsPreserveSignAndAxisMappings", () => {
    const ha = new SimpleBody();
    const hb = new SimpleBody();
    const hc = new SimpleBody();
    const qa = new SimpleBody();
    const qb = new SimpleBody();
    const qc = new SimpleBody();
    ha.setGeometry(new PlanarHalfSpace(1, 2, 0, -3));
    hb.setGeometry(new PlanarHalfSpace(2, -1, 0, -3));
    hc.setGeometry(new PlanarHalfSpace(-1, -2, 0, 3));
    const rotation = Matrix4x4d.identityMatrix().withVal(0, 0, 0).withVal(0, 1, -1).withVal(1, 0, 1).withVal(1, 1, 0);
    expectEq(AffinePlacementStatus.Valid, hb.setAffinePlacement(rotation, rotation.transpose()));
    const a = new CsgBodyOperand(ha);
    const b = new CsgBodyOperand(hb);
    const c = new CsgBodyOperand(hc);
    expectTrue(a.isSameSolid(b));
    expectTrue(b.isSameSolid(a));
    expectFalse(a.isSameSolid(c));
    qa.setGeometry(new QuadricSurface(new Vector3Dd(1, 2, 3), new Vector3Dd(), new Vector3Dd(), -1));
    qb.setGeometry(new QuadricSurface(new Vector3Dd(6, 3, 9), new Vector3Dd(), new Vector3Dd(), -3));
    qc.setGeometry(new QuadricSurface(new Vector3Dd(-1, -2, -3), new Vector3Dd(), new Vector3Dd(), 1));
    expectEq(AffinePlacementStatus.Valid, qb.setAffinePlacement(rotation, rotation.transpose()));
    const x = new CsgBodyOperand(qa);
    const y = new CsgBodyOperand(qb);
    const z = new CsgBodyOperand(qc);
    expectTrue(x.isSameSolid(y));
    expectTrue(y.isSameSolid(x));
    expectFalse(x.isSameSolid(z));
});

test("CsgSolidEquivalenceContract_PlacedViewsOfSameExpressionMustHaveSamePlacement", () => {
    const a = new SimpleBody();
    a.setGeometry(new Sphere(1));
    const leaf = new CsgBodyOperand(a);
    const child = new CsgTree(CsgOperation.UNION);
    expectTrue(child.addOperand(leaf));
    const first = new CsgPlacedExpression(child);
    const second = new CsgPlacedExpression(child);
    const shifted = new CsgPlacedExpression(child);
    expectEq(
        AffinePlacementStatus.Valid,
        first.setAffinePlacement(new Matrix4x4d().translation(3, 0, 0), new Matrix4x4d().translation(-3, 0, 0)),
    );
    expectEq(
        AffinePlacementStatus.Valid,
        second.setAffinePlacement(new Matrix4x4d().translation(3, 0, 0), new Matrix4x4d().translation(-3, 0, 0)),
    );
    expectEq(
        AffinePlacementStatus.Valid,
        shifted.setAffinePlacement(new Matrix4x4d().translation(4, 0, 0), new Matrix4x4d().translation(-4, 0, 0)),
    );
    expectTrue(first.isSameSolid(second));
    expectTrue(second.isSameSolid(first));
    expectFalse(first.isSameSolid(shifted));
    const difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(difference.addOperand(first));
    expectTrue(difference.addOperand(second));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(difference, new Vector3Dd(4, 0, 0), 0.1));
});

test("CsgExactArithmeticContract_IndependentExactRationalFixtures", () => {
    // Generated by generate_csg_exact_reference.py; include inside a test.
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.3481291096894082e218,
            1.508602034143908e16,
            1.508602034143908e16,
            1.3481291096894082e218,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.3481291096894082e218, 1.508602034143908e16, 2.033790317166057e234, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.3481291096894082e218,
            1.508602034143908e16,
            4.438613657350534e-116,
            -1.2245195957036193e81,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            3.589038430970725e-219,
            4.158862259124421e232,
            4.158862259124421e232,
            3.589038430970725e-219,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(3.589038430970725e-219, 4.158862259124421e232, 149263164771112.75, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            3.589038430970725e-219,
            4.158862259124421e232,
            5.153079963745761e-51,
            1.1058976235358466e-74,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.5468208164672275e-291,
            2.8466966728028586e271,
            2.8466966728028586e271,
            1.5468208164672275e-291,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.5468208164672275e-291, 2.8466966728028586e271, 4.403329671659458e-20, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.5468208164672275e-291,
            2.8466966728028586e271,
            6.1798534378290304e35,
            8.928074420568578e-248,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.4487626954062786e-158,
            -4.662247180320476e-225,
            -4.662247180320476e-225,
            1.4487626954062786e-158,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(1.4487626954062786e-158, -4.662247180320476e-225, -0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.4487626954062786e-158,
            -4.662247180320476e-225,
            8.031968851680031e-184,
            1.8789710001984733e203,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.8607729869049007e-303,
            -3.438200668160537e118,
            -3.438200668160537e118,
            -2.8607729869049007e-303,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-2.8607729869049007e-303, -3.438200668160537e118, 9.835911595032045e-185, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.8607729869049007e-303,
            -3.438200668160537e118,
            -9.967083420641857e294,
            -6.94648487336797e297,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.4363557675522107e-212,
            -2.461764621971174e91,
            -2.461764621971174e91,
            1.4363557675522107e-212,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.4363557675522107e-212, -2.461764621971174e91, -3.5359698131242835e-121, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.4363557675522107e-212,
            -2.461764621971174e91,
            -1.4829372502924315e-171,
            2.497933014500966e-207,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.4039400012702702e-147,
            -4.5969116532802337e-104,
            -4.5969116532802337e-104,
            1.4039400012702702e-147,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.4039400012702702e-147,
            -4.5969116532802337e-104,
            -6.453788152345571e-251,
            1.0,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.4039400012702702e-147,
            -4.5969116532802337e-104,
            1.0239288700919138e20,
            -7.887617572248624e74,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            3.2763948378187095e-96,
            -1.1833848705410401e294,
            -1.1833848705410401e294,
            3.2763948378187095e-96,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(3.2763948378187095e-96, -1.1833848705410401e294, -3.877236080993426e198, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            3.2763948378187095e-96,
            -1.1833848705410401e294,
            4.6281473586391736e-297,
            9.315058967147795e98,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.7572044299816007e-115,
            -1.4795060259586057e307,
            -1.4795060259586057e307,
            1.7572044299816007e-115,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.7572044299816007e-115, -1.4795060259586057e307, -2.599794542998935e192, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.7572044299816007e-115,
            -1.4795060259586057e307,
            -1.5690430914159116e-49,
            -8.901185484835217e40,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            4.0012841610577694e76,
            -6.409740127629624e173,
            -6.409740127629624e173,
            4.0012841610577694e76,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(4.0012841610577694e76, -6.409740127629624e173, -2.5647191649180818e250, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            4.0012841610577694e76,
            -6.409740127629624e173,
            -2.496166796116452e-79,
            1.0009474926133902e-259,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            6.37295608392316e-213,
            4.1542913352853495e-194,
            4.1542913352853495e-194,
            6.37295608392316e-213,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(6.37295608392316e-213, 4.1542913352853495e-194, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            6.37295608392316e-213,
            4.1542913352853495e-194,
            1.0822523954569696e-127,
            -3.0918258122205746e294,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            3.1399387225200916e-132,
            6.312340885489272e53,
            6.312340885489272e53,
            3.1399387225200916e-132,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(3.1399387225200916e-132, 6.312340885489272e53, 1.9820363576094529e-78, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            3.1399387225200916e-132,
            6.312340885489272e53,
            1.0596740184695348e111,
            3.4937682013650087e-35,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            7.416435561212425e-288,
            -2.2932730236464724e244,
            -2.2932730236464724e244,
            7.416435561212425e-288,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(7.416435561212425e-288, -2.2932730236464724e244, -1.700791160414084e-43, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            7.416435561212425e-288,
            -2.2932730236464724e244,
            -2.2214175483835312e-65,
            -1.6111275585641478e169,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -7.98045555068581e147,
            -8.887471462372605e68,
            -8.887471462372605e68,
            -7.98045555068581e147,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-7.98045555068581e147, -8.887471462372605e68, 7.092607096345319e216, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -7.98045555068581e147,
            -8.887471462372605e68,
            3.073437407397713e192,
            8.775654129639383e180,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            9.984510029473208e244,
            -8.62904467523619e-103,
            -8.62904467523619e-103,
            9.984510029473208e244,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(9.984510029473208e244, -8.62904467523619e-103, -8.615678310466812e142, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            9.984510029473208e244,
            -8.62904467523619e-103,
            -3.7285728678151266e-17,
            3.2301761099053165e200,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -8.059512201133745e158,
            -7.96693273503773e-30,
            -7.96693273503773e-30,
            -8.059512201133745e158,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-8.059512201133745e158, -7.96693273503773e-30, 6.420959158364842e129, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -8.059512201133745e158,
            -7.96693273503773e-30,
            -5.356993591877341e29,
            1.463804363778437e304,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -5.724421408569658e259,
            -1.2547701643053196e99,
            -1.2547701643053196e99,
            -5.724421408569658e259,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -5.724421408569658e259,
            -1.2547701643053196e99,
            8.474517145361876e-12,
            -4.392467938543961e154,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            2.64584643043174e-27,
            1.0897294658529334e-10,
            1.0897294658529334e-10,
            2.64584643043174e-27,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(2.64584643043174e-27, 1.0897294658529334e-10, 2.8832568173632705e-37, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            2.64584643043174e-27,
            1.0897294658529334e-10,
            3.512300843353455e-286,
            5.968360949832252e-260,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -5.988074315605563e-236,
            -1.1018467908627255e23,
            -1.1018467908627255e23,
            -5.988074315605563e-236,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-5.988074315605563e-236, -1.1018467908627255e23, 6.597940468097501e-213, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -5.988074315605563e-236,
            -1.1018467908627255e23,
            6.5309583489039e131,
            -1.2569475242898597e283,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.48583326714882e-104,
            -2.741846236156757e-275,
            -2.741846236156757e-275,
            -1.48583326714882e-104,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(-1.48583326714882e-104, -2.741846236156757e-275, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.48583326714882e-104,
            -2.741846236156757e-275,
            3.0997592435857054e-71,
            1.4449455964255898e210,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.7195361304078723e35,
            -1.0889012627139563e-277,
            -1.0889012627139563e-277,
            1.7195361304078723e35,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.7195361304078723e35,
            -1.0889012627139563e-277,
            -1.8724050636834024e-242,
            1.0,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.7195361304078723e35,
            -1.0889012627139563e-277,
            -1.8228265725950638e-204,
            -3.5448760117522405e75,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            3.1023219688719697e-212,
            -2.174364238215849e-164,
            -2.174364238215849e-164,
            3.1023219688719697e-212,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(3.1023219688719697e-212, -2.174364238215849e-164, -0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            3.1023219688719697e-212,
            -2.174364238215849e-164,
            8.90889771196218e296,
            3.5786999818105915e206,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            9.108979167555573e57,
            -2.435586850806786e-197,
            -2.435586850806786e-197,
            9.108979167555573e57,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(9.108979167555573e57, -2.435586850806786e-197, -2.2185709884771296e-139, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            9.108979167555573e57,
            -2.435586850806786e-197,
            2.3868115174986217e35,
            -2.8791857925654606e-6,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.6484474090158737e41,
            1.364334685896825e-203,
            1.364334685896825e-203,
            1.6484474090158737e41,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.6484474090158737e41, 1.364334685896825e-203, 2.249033977997107e-162, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.6484474090158737e41,
            1.364334685896825e-203,
            -2.1505956418115575e-101,
            -4.675421085484511e-275,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.5815763614297798e-139,
            -7.35844247510422e89,
            -7.35844247510422e89,
            1.5815763614297798e-139,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.5815763614297798e-139, -7.35844247510422e89, -1.1637938675565674e-49, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.5815763614297798e-139,
            -7.35844247510422e89,
            -3.831093431255948e-34,
            6.349473410089681e-98,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -5.052620076366961e22,
            -9.221097203531264e-185,
            -9.221097203531264e-185,
            -5.052620076366961e22,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-5.052620076366961e22, -9.221097203531264e-185, 4.65907008566933e-162, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -5.052620076366961e22,
            -9.221097203531264e-185,
            2.496092087021999e-284,
            -1.9260181345591108e-219,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.7271727774973107e-212,
            8.378809750228319e217,
            8.378809750228319e217,
            -1.7271727774973107e-212,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.7271727774973107e-212, 8.378809750228319e217, -1447165.2108423393, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.7271727774973107e-212,
            8.378809750228319e217,
            -2.599333233349599e-23,
            4.057693315242782e241,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            3.9994259530468145e-137,
            -4.025676549898497e169,
            -4.025676549898497e169,
            3.9994259530468145e-137,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(3.9994259530468145e-137, -4.025676549898497e169, -1.6100395272236008e33, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            3.9994259530468145e-137,
            -4.025676549898497e169,
            -1.2972487135025036e41,
            -3.966010748334712e-191,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.0452768880288252e224,
            5.111058712413606e-292,
            5.111058712413606e-292,
            1.0452768880288252e224,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.0452768880288252e224, 5.111058712413606e-292, 5.342471545444309e-68, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.0452768880288252e224,
            5.111058712413606e-292,
            6.039039889994851e-237,
            7.093755738351112e-155,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            9.381577053547151e253,
            -2.522175620003527e204,
            -2.522175620003527e204,
            9.381577053547151e253,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            9.381577053547151e253,
            -2.522175620003527e204,
            -6.803169196076615e-111,
            -6.424553291203255e-203,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.3797126672109038e-288,
            -1.7061018570074718e-42,
            -1.7061018570074718e-42,
            -1.3797126672109038e-288,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(-1.3797126672109038e-288, -1.7061018570074718e-42, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.3797126672109038e-288,
            -1.7061018570074718e-42,
            -1.7431156673215193e-107,
            -7.106486510496925e171,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.2718681308877603e214,
            -2.917740771366676e-89,
            -2.917740771366676e-89,
            1.2718681308877603e214,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.2718681308877603e214, -2.917740771366676e-89, -3.7109815012931464e125, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.2718681308877603e214,
            -2.917740771366676e-89,
            1.182230227513639e-70,
            1.5688843580839795e-64,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            6.231571359973529e261,
            -5.481665691953133e-302,
            -5.481665691953133e-302,
            6.231571359973529e261,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(6.231571359973529e261, -5.481665691953133e-302, -3.4159390930924623e-40, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            6.231571359973529e261,
            -5.481665691953133e-302,
            2.593657006353751e-39,
            2.877557062174041e-44,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -5.527921536042896e89,
            -7.928529588042178e287,
            -7.928529588042178e287,
            -5.527921536042896e89,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -5.527921536042896e89,
            -7.928529588042178e287,
            -1.9949818342898656e220,
            2.973314465142009e120,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.4639542886967337e-248,
            -5.047408806137786e292,
            -5.047408806137786e292,
            1.4639542886967337e-248,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.4639542886967337e-248, -5.047408806137786e292, -7.389175768551073e44, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.4639542886967337e-248,
            -5.047408806137786e292,
            -9.988752614528658e-203,
            -7.694759478775393e231,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.526241517520765e-66,
            -1.523725392899945e281,
            -1.523725392899945e281,
            -1.526241517520765e-66,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.526241517520765e-66, -1.523725392899945e281, 2.325572955944536e215, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.526241517520765e-66,
            -1.523725392899945e281,
            5.914506075170534e82,
            2.548921573007938e-113,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.397514685957105e-27,
            -1.2507389795286155e-283,
            -1.2507389795286155e-283,
            -2.397514685957105e-27,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-2.397514685957105e-27, -1.2507389795286155e-283, 2.9986650717189e-310, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.397514685957105e-27,
            -1.2507389795286155e-283,
            -2.6066336297495882e-55,
            -3.5250874261103594e-26,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.4086762287265513e72,
            -1.2813411613404658e162,
            -1.2813411613404658e162,
            -2.4086762287265513e72,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-2.4086762287265513e72, -1.2813411613404658e162, 3.0863359962096525e234, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.4086762287265513e72,
            -1.2813411613404658e162,
            -1.0899027668154274e49,
            -5.7536034689064826e-242,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.6919381816073623e-277,
            -3.2670511726870365e251,
            -3.2670511726870365e251,
            -1.6919381816073623e-277,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.6919381816073623e-277, -3.2670511726870365e251, 5.527648620334305e-26, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.6919381816073623e-277,
            -3.2670511726870365e251,
            40.26718680697478,
            7.161508564093279e-223,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.6108115722690846e-129,
            2.7699170120361115e35,
            2.7699170120361115e35,
            -1.6108115722690846e-129,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.6108115722690846e-129, 2.7699170120361115e35, -4.461814377212774e-94, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.6108115722690846e-129,
            2.7699170120361115e35,
            8.748181696511017e-37,
            9.960880076534891e42,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -3.5332060740096926e71,
            -3.1809706864089923e-221,
            -3.1809706864089923e-221,
            -3.5332060740096926e71,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.5332060740096926e71,
            -3.1809706864089923e-221,
            1.1239024950467033e-149,
            1.0,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.5332060740096926e71,
            -3.1809706864089923e-221,
            4.3537217381664956e217,
            -1702770729811.5579,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.4288497097750977e-111,
            -1.8050310726609112e-137,
            -1.8050310726609112e-137,
            1.4288497097750977e-111,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.4288497097750977e-111,
            -1.8050310726609112e-137,
            -2.5791181243065763e-248,
            1.0,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.4288497097750977e-111,
            -1.8050310726609112e-137,
            9.207240231643856e-144,
            5.512711667949457e40,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            3.623267980555273e-245,
            6.181704619120986e-215,
            6.181704619120986e-215,
            3.623267980555273e-245,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(3.623267980555273e-245, 6.181704619120986e-215, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            3.623267980555273e-245,
            6.181704619120986e-215,
            -1.3943982020851042e-139,
            1.5238640169856065e154,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            2.9397889780587074e-120,
            1.7989108859724693e-86,
            1.7989108859724693e-86,
            2.9397889780587074e-120,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(2.9397889780587074e-120, 1.7989108859724693e-86, 5.28841839509169e-206, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            2.9397889780587074e-120,
            1.7989108859724693e-86,
            -6.185360160542837e68,
            1.5109726202572663e286,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -3.4598303954423543e109,
            -980067142266183.4,
            -980067142266183.4,
            -3.4598303954423543e109,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-3.4598303954423543e109, -980067142266183.4, 3.3908660883868675e124, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.4598303954423543e109,
            -980067142266183.4,
            1.1384733110192156e-302,
            1.5552108952063733e-285,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            7.604725200088188e-171,
            3.984721977304193e40,
            3.984721977304193e40,
            7.604725200088188e-171,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(7.604725200088188e-171, 3.984721977304193e40, 3.030271563615043e-130, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            7.604725200088188e-171,
            3.984721977304193e40,
            -6.19876382097866e-172,
            -1.6470020864439731e190,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.1845877692777048e77,
            -2.3101018570515598e-290,
            -2.3101018570515598e-290,
            -1.1845877692777048e77,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.1845877692777048e77,
            -2.3101018570515598e-290,
            2.7365184056489907e-213,
            1.0,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.1845877692777048e77,
            -2.3101018570515598e-290,
            -3.886654268990096e67,
            3.429455097211195e-38,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.3723223515296032e250,
            -5.17822881967953e-62,
            -5.17822881967953e-62,
            -1.3723223515296032e250,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.3723223515296032e250, -5.17822881967953e-62, 7.106199150580975e188, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.3723223515296032e250,
            -5.17822881967953e-62,
            97410632301808.06,
            3.2208265389001054e-71,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -3.5372486056341842e-239,
            1.9334814633115787e151,
            1.9334814633115787e151,
            -3.5372486056341842e-239,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-3.5372486056341842e-239, 1.9334814633115787e151, -6.839204610118424e-88, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.5372486056341842e-239,
            1.9334814633115787e151,
            -3.5678353861257974e198,
            3.402264633415554e173,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -6.200919891947601e-265,
            -6.630929217321835e-296,
            -6.630929217321835e-296,
            -6.200919891947601e-265,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(-6.200919891947601e-265, -6.630929217321835e-296, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -6.200919891947601e-265,
            -6.630929217321835e-296,
            1.1495357508037499e-250,
            -620499968.382895,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.5673436388352688e227,
            -6.299035274708433e-141,
            -6.299035274708433e-141,
            -1.5673436388352688e227,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.5673436388352688e227, -6.299035274708433e-141, 9.872752868613233e86, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.5673436388352688e227,
            -6.299035274708433e-141,
            -9.784739103832505e-13,
            -3.343971125674349e109,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.7563759449550938e221,
            0.00016749351778580583,
            0.00016749351778580583,
            1.7563759449550938e221,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.7563759449550938e221, 0.00016749351778580583, 2.941815855748975e217, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.7563759449550938e221,
            0.00016749351778580583,
            1.7574015881467568e-217,
            4.667664617427529e95,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.8988565804899733e289,
            -3.3204331610325534e-158,
            -3.3204331610325534e-158,
            1.8988565804899733e289,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.8988565804899733e289, -3.3204331610325534e-158, -6.305026357903787e131, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.8988565804899733e289,
            -3.3204331610325534e-158,
            -6.504526690584936e-46,
            5.983276388121338e170,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.189094145974023e225,
            1.1920906683630745e-72,
            1.1920906683630745e-72,
            -1.189094145974023e225,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.189094145974023e225, 1.1920906683630745e-72, -1.4175080352207924e153, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.189094145974023e225,
            1.1920906683630745e-72,
            -3.7230953617759116e-46,
            6.225513323869337e101,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -4.8001041235186393e253,
            -4.650291819978472e-99,
            -4.650291819978472e-99,
            -4.8001041235186393e253,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-4.8001041235186393e253, -4.650291819978472e-99, 2.232188494064366e155, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -4.8001041235186393e253,
            -4.650291819978472e-99,
            -1.2313694527326595e-174,
            -1.767038546773903e90,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            6.260445322233003e257,
            -2.2586532636559984e-168,
            -2.2586532636559984e-168,
            6.260445322233003e257,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(6.260445322233003e257, -2.2586532636559984e-168, -1.41401752590015e90, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            6.260445322233003e257,
            -2.2586532636559984e-168,
            2.480772242589418e-246,
            -8.616033182184031e-190,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            3.890403770620353e219,
            -2.269314169667365e130,
            -2.269314169667365e130,
            3.890403770620353e219,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            3.890403770620353e219,
            -2.269314169667365e130,
            -2.0041448954916108e201,
            4.392577339139362e-32,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            4.4106505561662335e244,
            7.244886218822815e-302,
            7.244886218822815e-302,
            4.4106505561662335e244,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(4.4106505561662335e244, 7.244886218822815e-302, 3.195466143041193e-57, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            4.4106505561662335e244,
            7.244886218822815e-302,
            3.132295544812294e231,
            -6.187292863502991e-209,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.3519470107104943e304,
            -3.449784305514324e-289,
            -3.449784305514324e-289,
            1.3519470107104943e304,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.3519470107104943e304, -3.449784305514324e-289, -4663925579436069.0, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.3519470107104943e304,
            -3.449784305514324e-289,
            3.954092843373093e-32,
            2.461275355427021e-221,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.7646975380621556e-133,
            -8.166400510207901e138,
            -8.166400510207901e138,
            1.7646975380621556e-133,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.7646975380621556e-133, -8.166400510207901e138, -1441122.6875193415, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.7646975380621556e-133,
            -8.166400510207901e138,
            5.520059087965036e-123,
            6.081635427130257e203,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.8445193297049218e257,
            -3.9938535564389966e19,
            -3.9938535564389966e19,
            1.8445193297049218e257,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.8445193297049218e257, -3.9938535564389966e19, -7.366740084862476e276, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.8445193297049218e257,
            -3.9938535564389966e19,
            -1.0388953889534243e186,
            -1.952447742038904e109,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            9.629736511719228e223,
            7.599905943608676,
            7.599905943608676,
            9.629736511719228e223,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(9.629736511719228e223, 7.599905943608676, 7.318509175080044e224, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            9.629736511719228e223,
            7.599905943608676,
            -2.1581231077732505e196,
            -1.2228413752921724e-90,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.4979095084072548e153,
            1.8758305628976851e273,
            1.8758305628976851e273,
            1.4979095084072548e153,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.4979095084072548e153,
            1.8758305628976851e273,
            -5.43689818088408e-243,
            -4.916193689762869e-53,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -3.0380662008238732e200,
            8.233654468425607e-255,
            8.233654468425607e-255,
            -3.0380662008238732e200,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-3.0380662008238732e200, 8.233654468425607e-255, -2.501438734978629e-54, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.0380662008238732e200,
            8.233654468425607e-255,
            3.9709165230667806e-212,
            7.058594253984529e-131,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            46341.102957181385,
            1.010988555450726e-169,
            1.010988555450726e-169,
            46341.102957181385,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(46341.102957181385, 1.010988555450726e-169, 4.685032473667418e-165, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            46341.102957181385,
            1.010988555450726e-169,
            4.673526780941689e-243,
            2.8265924359971563e-207,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.970621501961703e124,
            3.810969096319969e-304,
            3.810969096319969e-304,
            -2.970621501961703e124,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-2.970621501961703e124, 3.810969096319969e-304, -1.132094674083966e-179, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.970621501961703e124,
            3.810969096319969e-304,
            8.74778707205269e-188,
            3.841726552248192e229,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            9.28711493698474e-270,
            1.34535188252746e-192,
            1.34535188252746e-192,
            9.28711493698474e-270,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(9.28711493698474e-270, 1.34535188252746e-192, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            9.28711493698474e-270,
            1.34535188252746e-192,
            1.161454949303382e-162,
            -1.079621757910798e22,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            2.5799357918253835e210,
            9.256386176939586e175,
            9.256386176939586e175,
            2.5799357918253835e210,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            2.5799357918253835e210,
            9.256386176939586e175,
            -2.374128586936532e-293,
            0.0001022945850451412,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.277634898473764e-154,
            -1.1179889670157253e-29,
            -1.1179889670157253e-29,
            -2.277634898473764e-154,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.277634898473764e-154,
            -1.1179889670157253e-29,
            2.5463706873836496e-183,
            1.0,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.277634898473764e-154,
            -1.1179889670157253e-29,
            -1.6592740873519087e41,
            -1.4614390811371496e51,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.856078401302311e71,
            -9.360851272442084e97,
            -9.360851272442084e97,
            1.856078401302311e71,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.856078401302311e71, -9.360851272442084e97, -1.7374473864583006e169, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.856078401302311e71,
            -9.360851272442084e97,
            1.75006020751505e163,
            6.20613694558444e-137,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            3.6690899430568613e-237,
            5.5963889531913316e277,
            5.5963889531913316e277,
            3.6690899430568613e-237,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(3.6690899430568613e-237, 5.5963889531913316e277, 2.053365442558883e41, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            3.6690899430568613e-237,
            5.5963889531913316e277,
            -1.1057384678448939e-48,
            -2.620515361267024e-204,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.4549981199586967e303,
            -1.6410368606861317e261,
            -1.6410368606861317e261,
            -2.4549981199586967e303,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.4549981199586967e303,
            -1.6410368606861317e261,
            -3.544361252235825e279,
            -1.5233105969010818e-74,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.4381348761579695e183,
            -1.1799224319013672e-233,
            -1.1799224319013672e-233,
            -1.4381348761579695e183,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.4381348761579695e183,
            -1.1799224319013672e-233,
            1.6968876004784828e-50,
            1.0,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.4381348761579695e183,
            -1.1799224319013672e-233,
            -2.9944615920250707e-206,
            -2.6098476318435196e16,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            2.8177170779942707e-214,
            3.440175881072782e181,
            3.440175881072782e181,
            2.8177170779942707e-214,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(2.8177170779942707e-214, 3.440175881072782e181, 9.693442331402764e-33, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            2.8177170779942707e-214,
            3.440175881072782e181,
            4.45310315639011e95,
            -9.734704243797436e245,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.0599685000536876e105,
            -9.652878022953404e22,
            -9.652878022953404e22,
            1.0599685000536876e105,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.0599685000536876e105, -9.652878022953404e22, -1.0231746639191125e128, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.0599685000536876e105,
            -9.652878022953404e22,
            5.6938436806964695e-124,
            -9.030183355238974e259,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.411249462614591e-231,
            -8.14458415976459e146,
            -8.14458415976459e146,
            1.411249462614591e-231,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.411249462614591e-231, -8.14458415976459e146, -1.1494040018687087e-84, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.411249462614591e-231,
            -8.14458415976459e146,
            -1.9222523638336213e101,
            -6.377038375960367e84,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.2157543149147464e151,
            3.548782168876793e65,
            3.548782168876793e65,
            1.2157543149147464e151,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.2157543149147464e151, 3.548782168876793e65, 4.3144472345044736e216, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.2157543149147464e151,
            3.548782168876793e65,
            9.191248878491496e-286,
            -1.5143768931183932e116,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.6951644783245833e-5,
            4.4212676316054325e-102,
            4.4212676316054325e-102,
            1.6951644783245833e-5,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.6951644783245833e-5, 4.4212676316054325e-102, 7.494775838263788e-107, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.6951644783245833e-5,
            4.4212676316054325e-102,
            -1.4514290497947335e228,
            -9.494241352862808e-114,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            7.852790326229692e-112,
            -2.3619356326819148e-92,
            -2.3619356326819148e-92,
            7.852790326229692e-112,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(7.852790326229692e-112, -2.3619356326819148e-92, -1.854778528750175e-203, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            7.852790326229692e-112,
            -2.3619356326819148e-92,
            4.5548058351095904e-94,
            5.246705077351025e-279,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.1754744925266528e175,
            1.0912631503305121e280,
            1.0912631503305121e280,
            1.1754744925266528e175,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.1754744925266528e175,
            1.0912631503305121e280,
            2.743876894749261e18,
            -4.515460817084539e103,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            4.224370240814935e26,
            -1.0924275053252555e200,
            -1.0924275053252555e200,
            4.224370240814935e26,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(4.224370240814935e26, -1.0924275053252555e200, -4.6148182437437083e226, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            4.224370240814935e26,
            -1.0924275053252555e200,
            -1.2720376921761665e173,
            -1.383972772261922e-29,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            0.6924927710421925,
            6.991391826775196e-119,
            6.991391826775196e-119,
            0.6924927710421925,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(0.6924927710421925, 6.991391826775196e-119, 4.841488299565292e-119, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            0.6924927710421925,
            6.991391826775196e-119,
            1.3163177506413418e69,
            -5.638219957614892e-299,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.00712611804706e270,
            -8.27709276246663e151,
            -8.27709276246663e151,
            -1.00712611804706e270,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.00712611804706e270,
            -8.27709276246663e151,
            -1.447950514300179e242,
            -3.490701328984201e-166,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.5763050904536142e-201,
            -5.8168696596487e-271,
            -5.8168696596487e-271,
            1.5763050904536142e-201,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(1.5763050904536142e-201, -5.8168696596487e-271, -0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.5763050904536142e-201,
            -5.8168696596487e-271,
            -624865799097.6042,
            -3.284305086849272e-57,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            5.6315322411720045e-214,
            -5.6613741335762425e-216,
            -5.6613741335762425e-216,
            5.6315322411720045e-214,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(5.6315322411720045e-214, -5.6613741335762425e-216, -0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            5.6315322411720045e-214,
            -5.6613741335762425e-216,
            1.9611560168507456e75,
            -1.0454509262387639e20,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -4.316062049858167e-138,
            -2.9234343703580386e-29,
            -2.9234343703580386e-29,
            -4.316062049858167e-138,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -4.316062049858167e-138,
            -2.9234343703580386e-29,
            1.2617724141153336e-166,
            1.0,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -4.316062049858167e-138,
            -2.9234343703580386e-29,
            1.5531675614113892e-127,
            4.881797702404058e-117,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -9.56533438670839e-99,
            3.761851075351606e-206,
            3.761851075351606e-206,
            -9.56533438670839e-99,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-9.56533438670839e-99, 3.761851075351606e-206, -3.598336344873665e-304, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -9.56533438670839e-99,
            3.761851075351606e-206,
            -9.823551234569894e95,
            9.188253019249041e167,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.0061285445980484e-45,
            -9541.91553157694,
            -9541.91553157694,
            -1.0061285445980484e-45,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.0061285445980484e-45, -9541.91553157694, 9.60039358646302e-42, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.0061285445980484e-45,
            -9541.91553157694,
            5.915489824782749e-202,
            2.360583593306639e-272,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -6.7899871006292424e16,
            -5.368490120831118e96,
            -5.368490120831118e96,
            -6.7899871006292424e16,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-6.7899871006292424e16, -5.368490120831118e96, 3.645197867029882e113, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -6.7899871006292424e16,
            -5.368490120831118e96,
            -5.956301583225814e73,
            -3.1303430844383633e-218,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.020811779203553e-303,
            9.550227884514601e266,
            9.550227884514601e266,
            -2.020811779203553e-303,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-2.020811779203553e-303, 9.550227884514601e266, -1.9299213003105335e-36, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.020811779203553e-303,
            9.550227884514601e266,
            -1.1065355863287734e-145,
            7.64896434193754e-36,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -3.5487770041565944e-246,
            -6.874929127943936e-126,
            -6.874929127943936e-126,
            -3.5487770041565944e-246,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(-3.5487770041565944e-246, -6.874929127943936e-126, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.5487770041565944e-246,
            -6.874929127943936e-126,
            -1.577715529825601e-230,
            -2.377562286904181e252,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -5.75990957705361e178,
            -5.0135019428531146e36,
            -5.0135019428531146e36,
            -5.75990957705361e178,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-5.75990957705361e178, -5.0135019428531146e36, 2.8877317855216533e215, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -5.75990957705361e178,
            -5.0135019428531146e36,
            6.690825537319465e-89,
            -5.479586758054085e237,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            5.435543755138374e-171,
            -8.498697195121521e-30,
            -8.498697195121521e-30,
            5.435543755138374e-171,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(5.435543755138374e-171, -8.498697195121521e-30, -4.61950404657548e-200, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            5.435543755138374e-171,
            -8.498697195121521e-30,
            -4.3806111444418435e114,
            2.6387051503521827e-192,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.821072329394243e89,
            8.511934212472077e137,
            8.511934212472077e137,
            1.821072329394243e89,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.821072329394243e89, 8.511934212472077e137, 1.5500847863957076e227, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.821072329394243e89,
            8.511934212472077e137,
            -1.0507483712893123e102,
            1.745591306227915e-277,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.5146566850867942e148,
            1.0532217318288913e-70,
            1.0532217318288913e-70,
            -2.5146566850867942e148,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-2.5146566850867942e148, 1.0532217318288913e-70, -2.6484910688222126e78, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.5146566850867942e148,
            1.0532217318288913e-70,
            -1.1005491458768313e-258,
            7.2942742682961334e-99,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.8139952147147598e-299,
            2.1457848170444374e-202,
            2.1457848170444374e-202,
            1.8139952147147598e-299,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(1.8139952147147598e-299, 2.1457848170444374e-202, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.8139952147147598e-299,
            2.1457848170444374e-202,
            1.4111640539580831e-174,
            -1.0259470956273248e136,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -5.38767155138819e224,
            3.620085601786565e36,
            3.620085601786565e36,
            -5.38767155138819e224,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-5.38767155138819e224, 3.620085601786565e36, -1.9503832210335472e261, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -5.38767155138819e224,
            3.620085601786565e36,
            4.421087815166281e299,
            3.7253517639601015e135,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.663442575748588e60,
            -4.4265471685667903e-156,
            -4.4265471685667903e-156,
            -1.663442575748588e60,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.663442575748588e60, -4.4265471685667903e-156, 7.36330702375336e-96, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.663442575748588e60,
            -4.4265471685667903e-156,
            -1.0569018429513061e39,
            4.88542483903448e-168,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -3.11598784332239e-76,
            3.66181376656477e-52,
            3.66181376656477e-52,
            -3.11598784332239e-76,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-3.11598784332239e-76, 3.66181376656477e-52, -1.1410167181126395e-127, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.11598784332239e-76,
            3.66181376656477e-52,
            -7.655605145748932e148,
            2.3470034245878135e-216,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.238557941121838e257,
            5.3409818423319555e-20,
            5.3409818423319555e-20,
            -1.238557941121838e257,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.238557941121838e257, 5.3409818423319555e-20, -6.615115474207788e237, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.238557941121838e257,
            5.3409818423319555e-20,
            2.7652523817681966e-77,
            -3.6915572969849904e269,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1011521477.6723564,
            -1.0379984748143093e-225,
            -1.0379984748143093e-225,
            -1011521477.6723564,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1011521477.6723564, -1.0379984748143093e-225, 1.0499577510658222e-216, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1011521477.6723564,
            -1.0379984748143093e-225,
            -1.2194843476340386e-95,
            -5.085543267098591e90,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -3.0451780839405765e19,
            3.5305930773961826e-15,
            3.5305930773961826e-15,
            -3.0451780839405765e19,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-3.0451780839405765e19, 3.5305930773961826e-15, -107512.8466259917, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.0451780839405765e19,
            3.5305930773961826e-15,
            -8.927190136379526e-72,
            0.003321788469213375,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.4626416207935197e-252,
            1.0489702940275709e229,
            1.0489702940275709e229,
            -1.4626416207935197e-252,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.4626416207935197e-252,
            1.0489702940275709e229,
            -1.5342676110207414e-23,
            1.0,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.4626416207935197e-252,
            1.0489702940275709e229,
            -4.2995401496500314e67,
            2.4510591324680501e145,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.4620318052804983e146,
            1.8164277763805538e203,
            1.8164277763805538e203,
            -1.4620318052804983e146,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.4620318052804983e146,
            1.8164277763805538e203,
            -1.2873208332307673e-177,
            -8.138633856138024e-285,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -3.3849863468212495e-176,
            1.2652108581333348e157,
            1.2652108581333348e157,
            -3.3849863468212495e-176,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-3.3849863468212495e-176, 1.2652108581333348e157, -4.282721480631335e-19, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.3849863468212495e-176,
            1.2652108581333348e157,
            -6.71104899347814e236,
            8.111324520030616e-37,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            8.336241638927608e-274,
            6.815628016556961e-89,
            6.815628016556961e-89,
            8.336241638927608e-274,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(8.336241638927608e-274, 6.815628016556961e-89, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            8.336241638927608e-274,
            6.815628016556961e-89,
            2.8589669696208454e-221,
            -1.373693682883778e-153,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            3.916185026646287e143,
            -2.3513986179059544e254,
            -2.3513986179059544e254,
            3.916185026646287e143,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            3.916185026646287e143,
            -2.3513986179059544e254,
            4.3448571677241576e55,
            -2.820479899142744e-57,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -4.633420135261714e282,
            -2.963549380991019e184,
            -2.963549380991019e184,
            -4.633420135261714e282,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -4.633420135261714e282,
            -2.963549380991019e184,
            2.7928533283508145e-245,
            2.596970901926454e-299,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            4657.102674771628,
            8.988145600876433e-15,
            8.988145600876433e-15,
            4657.102674771628,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(4657.102674771628, 8.988145600876433e-15, 4.185871691907848e-11, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            4657.102674771628,
            8.988145600876433e-15,
            -7.348308029142198e-145,
            -1.879845768259463e65,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -3.1117268579687203e-40,
            -5.319283356518194e-101,
            -5.319283356518194e-101,
            -3.1117268579687203e-40,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.1117268579687203e-40,
            -5.319283356518194e-101,
            1.6552156885623667e-140,
            1.0,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.1117268579687203e-40,
            -5.319283356518194e-101,
            -1.973482757843715e210,
            4.716747849033835e-179,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.6349823780784314e-241,
            3.722076800343649e104,
            3.722076800343649e104,
            -1.6349823780784314e-241,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.6349823780784314e-241, 3.722076800343649e104, -6.085529978416418e-137, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.6349823780784314e-241,
            3.722076800343649e104,
            -9.71469960009188e41,
            -7771504.042768875,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.8456001732293934e75,
            5.069434762446191e282,
            5.069434762446191e282,
            -1.8456001732293934e75,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.8456001732293934e75,
            5.069434762446191e282,
            -7.64005145355022e126,
            4.685469306003076e78,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            2.0660010423608814e238,
            5.0403356367474556e-46,
            5.0403356367474556e-46,
            2.0660010423608814e238,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(2.0660010423608814e238, 5.0403356367474556e-46, 1.041333867936894e193, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            2.0660010423608814e238,
            5.0403356367474556e-46,
            1.985179459232912e-305,
            -6.7918508988365856e-68,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.5088617746237474e141,
            -6.169874160148204e-31,
            -6.169874160148204e-31,
            1.5088617746237474e141,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.5088617746237474e141, -6.169874160148204e-31, -9.309487274486423e110, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.5088617746237474e141,
            -6.169874160148204e-31,
            -3.353103916092255e64,
            1.4171003874864644e-134,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -3.2138862747432323e-27,
            6.64750670297002e189,
            6.64750670297002e189,
            -3.2138862747432323e-27,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-3.2138862747432323e-27, 6.64750670297002e189, -2.1364330553938982e163, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.2138862747432323e-27,
            6.64750670297002e189,
            2.688758304882041e29,
            -1.3811855408944995e50,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -8.857529553030596e-119,
            3.4247942376050026e-62,
            3.4247942376050026e-62,
            -8.857529553030596e-119,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-8.857529553030596e-119, 3.4247942376050026e-62, -3.03352161726352e-180, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -8.857529553030596e-119,
            3.4247942376050026e-62,
            -9.268569037549652e-107,
            8.434399910411526e-122,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            5.077381442515587e295,
            -5.828068938418767e-248,
            -5.828068938418767e-248,
            5.077381442515587e295,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(5.077381442515587e295, -5.828068938418767e-248, -2.9591329073628963e48, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            5.077381442515587e295,
            -5.828068938418767e-248,
            1.1499997501525451e107,
            -2.2844626238954576e142,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            3.7190515768108727e28,
            6.34332636457128e-257,
            6.34332636457128e-257,
            3.7190515768108727e28,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(3.7190515768108727e28, 6.34332636457128e-257, 2.3591157918384798e-228, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            3.7190515768108727e28,
            6.34332636457128e-257,
            -8.315471668253908e250,
            -4.8576967027585924e-189,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -5.109504715362428e-40,
            -1.76037711366255e-123,
            -1.76037711366255e-123,
            -5.109504715362428e-40,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-5.109504715362428e-40, -1.76037711366255e-123, 8.9946551630749e-163, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -5.109504715362428e-40,
            -1.76037711366255e-123,
            4.154958664367053e-131,
            5.775261421890512e55,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            2.2501367973727098e200,
            -1.3889732802694822e-12,
            -1.3889732802694822e-12,
            2.2501367973727098e200,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(2.2501367973727098e200, -1.3889732802694822e-12, -3.12537988850184e188, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            2.2501367973727098e200,
            -1.3889732802694822e-12,
            -1274.6632626511766,
            -1.649337909415689e45,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            6.15356665515974e-121,
            9.5412317465442e-243,
            9.5412317465442e-243,
            6.15356665515974e-121,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(6.15356665515974e-121, 9.5412317465442e-243, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            6.15356665515974e-121,
            9.5412317465442e-243,
            -7.305267545129371e251,
            -1.9280100931114417e203,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -4.714831832528434e-253,
            -4.286863116887758e183,
            -4.286863116887758e183,
            -4.714831832528434e-253,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-4.714831832528434e-253, -4.286863116887758e183, 2.021183868519446e-69, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -4.714831832528434e-253,
            -4.286863116887758e183,
            4.29849582221959e-292,
            1.88676478909594e203,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            7.691275699077847e-15,
            7.040077902008626e-97,
            7.040077902008626e-97,
            7.691275699077847e-15,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(7.691275699077847e-15, 7.040077902008626e-97, 5.41471800873339e-111, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            7.691275699077847e-15,
            7.040077902008626e-97,
            1.8155145862322748e207,
            9.69627641569957e50,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -7.034372792840793e-137,
            1.4540162093179195e215,
            1.4540162093179195e215,
            -7.034372792840793e-137,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-7.034372792840793e-137, 1.4540162093179195e215, -1.0228092063175476e79, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -7.034372792840793e-137,
            1.4540162093179195e215,
            3.3923000488104177e-75,
            0.10184068490313201,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.7299136245552465e143,
            6.106867156740294e-178,
            6.106867156740294e-178,
            -2.7299136245552465e143,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-2.7299136245552465e143, 6.106867156740294e-178, -1.6671219854534288e-34, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.7299136245552465e143,
            6.106867156740294e-178,
            8.163410990319235e-246,
            3.4098328545066746e100,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.2708346289372747e-245,
            1.1512733037686907e226,
            1.1512733037686907e226,
            1.2708346289372747e-245,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.2708346289372747e-245, 1.1512733037686907e226, 1.4630779818002743e-19, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.2708346289372747e-245,
            1.1512733037686907e226,
            -1.9116486031847097e199,
            8.677597757600896e293,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -8.267186487149332e-229,
            5.691522941291178e-152,
            5.691522941291178e-152,
            -8.267186487149332e-229,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(-8.267186487149332e-229, 5.691522941291178e-152, -0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -8.267186487149332e-229,
            5.691522941291178e-152,
            1.6192408072567078e227,
            5.8969229917875325e208,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.4334168085760887e-286,
            9.582756421424647e95,
            9.582756421424647e95,
            -2.4334168085760887e-286,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-2.4334168085760887e-286, 9.582756421424647e95, -2.3318840548385185e-190, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.4334168085760887e-286,
            9.582756421424647e95,
            -1.0673543491605709e222,
            1.3983827494945156e-6,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            3.003374357553899e-252,
            -4.6678199531995595e-188,
            -4.6678199531995595e-188,
            3.003374357553899e-252,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(3.003374357553899e-252, -4.6678199531995595e-188, -0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            3.003374357553899e-252,
            -4.6678199531995595e-188,
            1.903442015035145e74,
            -3.288951701188521e32,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -3.9771938058663866e205,
            -1.3247829282412793e296,
            -1.3247829282412793e296,
            -3.9771938058663866e205,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.9771938058663866e205,
            -1.3247829282412793e296,
            4.3573647311136333e-110,
            -2.519197772334797e-144,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -3.6780602309358916e-214,
            2.867022978766188e-288,
            2.867022978766188e-288,
            -3.6780602309358916e-214,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(-3.6780602309358916e-214, 2.867022978766188e-288, -0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.6780602309358916e-214,
            2.867022978766188e-288,
            5.997279192931372e211,
            -9.027928439088896e275,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -7.313916148589004e-196,
            -3.7956675703518706e-131,
            -3.7956675703518706e-131,
            -7.313916148589004e-196,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(-7.313916148589004e-196, -3.7956675703518706e-131, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -7.313916148589004e-196,
            -3.7956675703518706e-131,
            1.9767689662776846e-75,
            -1.7834411369082922e158,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.2322659580398857e250,
            -1.5204624368051676e-65,
            -1.5204624368051676e-65,
            -2.2322659580398857e250,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-2.2322659580398857e250, -1.5204624368051676e-65, 3.394076538158547e185, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.2322659580398857e250,
            -1.5204624368051676e-65,
            3.215836248377261e-264,
            -9.295954360152798e-166,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            24521469570126.26,
            -1.9162696604231024e-253,
            -1.9162696604231024e-253,
            24521469570126.26,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(24521469570126.26, -1.9162696604231024e-253, -4.6989748166221293e-240, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            24521469570126.26,
            -1.9162696604231024e-253,
            -2.2423176929730798e-76,
            -3.5058377895316308e289,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.3192253486134147e-88,
            1.4236826287935882e232,
            1.4236826287935882e232,
            -2.3192253486134147e-88,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-2.3192253486134147e-88, 1.4236826287935882e232, -3.3018408410786725e144, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.3192253486134147e-88,
            1.4236826287935882e232,
            1.4533719047685567e-298,
            7.157042701151312e108,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            2.0858122556891997e40,
            -9.351633360748837e-141,
            -9.351633360748837e-141,
            2.0858122556891997e40,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(2.0858122556891997e40, -9.351633360748837e-141, -1.9505751474561904e-100, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            2.0858122556891997e40,
            -9.351633360748837e-141,
            5.238774701627453e179,
            -3.7141087121338987e-147,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            3.263621135354911e-238,
            1.5267108393020445e-251,
            1.5267108393020445e-251,
            3.263621135354911e-238,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(3.263621135354911e-238, 1.5267108393020445e-251, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            3.263621135354911e-238,
            1.5267108393020445e-251,
            -7.997925021771701e-137,
            -4.1326005432065466e-263,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -6.193855403867345e-190,
            7.201708321308416e138,
            7.201708321308416e138,
            -6.193855403867345e-190,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-6.193855403867345e-190, 7.201708321308416e138, -4.4606340003012566e-51, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -6.193855403867345e-190,
            7.201708321308416e138,
            1.5840963001372958e63,
            8.113108728281571e242,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -3.0335277245999337e53,
            1.2846435098765779e-210,
            1.2846435098765779e-210,
            -3.0335277245999337e53,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-3.0335277245999337e53, 1.2846435098765779e-210, -3.897001703437968e-157, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.0335277245999337e53,
            1.2846435098765779e-210,
            3.3794150299975055e-31,
            4.0055580301986525e-177,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.740543350156291e-283,
            1.6892509117073526e-120,
            1.6892509117073526e-120,
            1.740543350156291e-283,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(1.740543350156291e-283, 1.6892509117073526e-120, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.740543350156291e-283,
            1.6892509117073526e-120,
            -4.811504855980822e46,
            9804246744.578419,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -5.091511624784875e-286,
            -1.1750719960920854e-159,
            -1.1750719960920854e-159,
            -5.091511624784875e-286,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(-5.091511624784875e-286, -1.1750719960920854e-159, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -5.091511624784875e-286,
            -1.1750719960920854e-159,
            1.0804793016904688e-62,
            6.515580577560533e-103,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.5061046420169346e125,
            2.6532691346617316e-21,
            2.6532691346617316e-21,
            -1.5061046420169346e125,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.5061046420169346e125, 2.6532691346617316e-21, -3.996100960234289e104, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.5061046420169346e125,
            2.6532691346617316e-21,
            5.908497541858193e248,
            9.177891424651213e-215,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.4598232992849926e193,
            0.01924836083038734,
            0.01924836083038734,
            1.4598232992849926e193,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.4598232992849926e193, 0.01924836083038734, 2.8099205613244067e191, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.4598232992849926e193,
            0.01924836083038734,
            -2.6685321908312568e-76,
            -4.485888412637209e-115,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.0634185981343783e223,
            2.4771793946985214e-32,
            2.4771793946985214e-32,
            -2.0634185981343783e223,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-2.0634185981343783e223, 2.4771793946985214e-32, -5.111458033936191e191, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.0634185981343783e223,
            2.4771793946985214e-32,
            -6.03983423115818e110,
            7.916689328222127e-160,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.7920890883716662e-298,
            5.310244043008381e224,
            5.310244043008381e224,
            1.7920890883716662e-298,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.7920890883716662e-298, 5.310244043008381e224, 9.51643040606596e-74, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.7920890883716662e-298,
            5.310244043008381e224,
            -5.505007945212789e219,
            -1.0765141258531045e304,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            5.3465017574214304e119,
            -1.3742676991050766e210,
            -1.3742676991050766e210,
            5.3465017574214304e119,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            5.3465017574214304e119,
            -1.3742676991050766e210,
            -5.767331561154495e-146,
            -8.922300058534749e-235,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.4013540531041567e37,
            -9.182138171681831e191,
            -9.182138171681831e191,
            1.4013540531041567e37,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.4013540531041567e37, -9.182138171681831e191, -1.2867426543048725e229, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.4013540531041567e37,
            -9.182138171681831e191,
            -5.291021786295807e-77,
            -3.1609393335119757e-305,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.0677046035232714e-26,
            1.7685529621128689e-74,
            1.7685529621128689e-74,
            -1.0677046035232714e-26,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.0677046035232714e-26,
            1.7685529621128689e-74,
            -1.8882921392226279e-100,
            1.0,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.0677046035232714e-26,
            1.7685529621128689e-74,
            1.7938137312976804e122,
            7.760361294466137e-122,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -0.00011778630384882728,
            -2.6206974962739593e-98,
            -2.6206974962739593e-98,
            -0.00011778630384882728,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-0.00011778630384882728, -2.6206974962739593e-98, 3.086822715919855e-102, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -0.00011778630384882728,
            -2.6206974962739593e-98,
            -230012032730619.94,
            3080.95677223102,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.1574409010081442e-291,
            -7.70631785207281e-106,
            -7.70631785207281e-106,
            1.1574409010081442e-291,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(1.1574409010081442e-291, -7.70631785207281e-106, -0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.1574409010081442e-291,
            -7.70631785207281e-106,
            402.13929637193115,
            5.992651157748554e175,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.9332446036709815e105,
            -2.3967037908440095e-83,
            -2.3967037908440095e-83,
            1.9332446036709815e105,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.9332446036709815e105, -2.3967037908440095e-83, -4.633414670246966e22, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.9332446036709815e105,
            -2.3967037908440095e-83,
            -9.397964706754374e-85,
            2.2438436371960408e251,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            6.2516366700386194e-276,
            1.243585489725413e-202,
            1.243585489725413e-202,
            6.2516366700386194e-276,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(6.2516366700386194e-276, 1.243585489725413e-202, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            6.2516366700386194e-276,
            1.243585489725413e-202,
            -69890842007552.234,
            -2.5658361992269577e-79,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            2.3142970262170712e207,
            -3.973867258716786e121,
            -3.973867258716786e121,
            2.3142970262170712e207,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            2.3142970262170712e207,
            -3.973867258716786e121,
            -3.411784005706972e21,
            6.7062403482230354e-15,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            7.127799374214608e47,
            -5.153920475646856e125,
            -5.153920475646856e125,
            7.127799374214608e47,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(7.127799374214608e47, -5.153920475646856e125, -3.6736111141067514e173, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            7.127799374214608e47,
            -5.153920475646856e125,
            2.9657035600916204e-159,
            7.356246799580295e236,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.9625390286286277e-297,
            -2.234867812458018e251,
            -2.234867812458018e251,
            -1.9625390286286277e-297,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.9625390286286277e-297, -2.234867812458018e251, 4.3860153057747444e-46, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.9625390286286277e-297,
            -2.234867812458018e251,
            -1.4449750347901711e-145,
            9.172137680199804e-125,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.157425682544381e138,
            -1.9666305717926292e90,
            -1.9666305717926292e90,
            -1.157425682544381e138,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.157425682544381e138, -1.9666305717926292e90, 2.2762287318697302e228, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.157425682544381e138,
            -1.9666305717926292e90,
            -7.722993188229041,
            -1.6257090850407435e-91,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -4793951010.5682125,
            -7.687746019503874e242,
            -7.687746019503874e242,
            -4793951010.5682125,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-4793951010.5682125, -7.687746019503874e242, 3.685467779919235e252, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -4793951010.5682125,
            -7.687746019503874e242,
            9.61635587197443e42,
            2.441253183106398e-50,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -6.723928989271653e298,
            -3.426842751631114e26,
            -3.426842751631114e26,
            -6.723928989271653e298,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -6.723928989271653e298,
            -3.426842751631114e26,
            -4.306724602591309e-96,
            1.9725735109613348e282,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.3157240303236476e-54,
            -3.9402227551838966e248,
            -3.9402227551838966e248,
            1.3157240303236476e-54,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.3157240303236476e-54, -3.9402227551838966e248, -5.1842457638235035e194, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.3157240303236476e-54,
            -3.9402227551838966e248,
            -1.3965434581430099e253,
            -9.371701049875337e-248,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.9745918785746644e-137,
            -3.4485986799789936e-84,
            -3.4485986799789936e-84,
            -1.9745918785746644e-137,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.9745918785746644e-137,
            -3.4485986799789936e-84,
            6.809574945949828e-221,
            1.0,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.9745918785746644e-137,
            -3.4485986799789936e-84,
            -4.840036759303419e-187,
            1.6396701021124205e-280,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            6.145235406065758e-179,
            7.813826285198259e208,
            7.813826285198259e208,
            6.145235406065758e-179,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(6.145235406065758e-179, 7.813826285198259e208, 4.8017801944647616e30, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            6.145235406065758e-179,
            7.813826285198259e208,
            -3.7478652387436376e212,
            -8.627598670245776e237,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            4.466011717731527e120,
            6.183046852809938e-124,
            6.183046852809938e-124,
            4.466011717731527e120,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(4.466011717731527e120, 6.183046852809938e-124, 0.002761355969593222, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            4.466011717731527e120,
            6.183046852809938e-124,
            -1.3242789801739648e-282,
            1.6861485026652423e-245,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -4.535745189646393e-291,
            -7.515630547332078e-250,
            -7.515630547332078e-250,
            -4.535745189646393e-291,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(-4.535745189646393e-291, -7.515630547332078e-250, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -4.535745189646393e-291,
            -7.515630547332078e-250,
            3.9955106004513845e-122,
            -1.5646937467277966e-257,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            4.764265027122243e-8,
            -8.024785414372178e275,
            -8.024785414372178e275,
            4.764265027122243e-8,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(4.764265027122243e-8, -8.024785414372178e275, -3.823220449985405e268, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            4.764265027122243e-8,
            -8.024785414372178e275,
            1.0568952993049645e136,
            -9.973354354257388e144,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -6.55808178090837e225,
            -1.5430782976710524e114,
            -1.5430782976710524e114,
            -6.55808178090837e225,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -6.55808178090837e225,
            -1.5430782976710524e114,
            1.565598922536672e43,
            5.290070126988986e219,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -7.1610227804747545e118,
            -2.530059989718295e129,
            -2.530059989718295e129,
            -7.1610227804747545e118,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-7.1610227804747545e118, -2.530059989718295e129, 1.8117817222340434e248, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -7.1610227804747545e118,
            -2.530059989718295e129,
            -2.5087447268797412e54,
            2.54809421918617e129,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.5694069528575815e-189,
            -2.0851165486638392e-24,
            -2.0851165486638392e-24,
            1.5694069528575815e-189,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.5694069528575815e-189,
            -2.0851165486638392e-24,
            -3.2723964089914326e-213,
            1.0,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.5694069528575815e-189,
            -2.0851165486638392e-24,
            -1.0709758160931946e-166,
            -1.0178938185377036e75,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            2.544214100689489e-127,
            1.302723726978651e-84,
            1.302723726978651e-84,
            2.544214100689489e-127,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(2.544214100689489e-127, 1.302723726978651e-84, 3.314408075481848e-211, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            2.544214100689489e-127,
            1.302723726978651e-84,
            1.868416934704971e-42,
            3.699047803429566e-44,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.5387143060926643e203,
            -2.892235488941781e-230,
            -2.892235488941781e-230,
            -1.5387143060926643e203,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.5387143060926643e203, -2.892235488941781e-230, 4.45032412342363e-27, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.5387143060926643e203,
            -2.892235488941781e-230,
            -3.1531941219638543e286,
            1.3903833963902518e264,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -4.1659272893340025e218,
            6.284824191823329e66,
            6.284824191823329e66,
            -4.1659272893340025e218,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-4.1659272893340025e218, 6.284824191823329e66, -2.6182120609383327e285, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -4.1659272893340025e218,
            6.284824191823329e66,
            1.386134556429591e-109,
            4.8334425643755604e122,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.0556572379034005e-141,
            2.2054096555225835e30,
            2.2054096555225835e30,
            -1.0556572379034005e-141,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.0556572379034005e-141,
            2.2054096555225835e30,
            -2.3281566653944603e-111,
            1.0,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.0556572379034005e-141,
            2.2054096555225835e30,
            2.8196491392773013e221,
            2.539608831827345e-139,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.1973878899212357e-173,
            -1.1123721722614638e226,
            -1.1123721722614638e226,
            -2.1973878899212357e-173,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-2.1973878899212357e-173, -1.1123721722614638e226, 2.444313140412719e53, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.1973878899212357e-173,
            -1.1123721722614638e226,
            -2.105788941810196e251,
            -4.298794673968506e112,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            8.022010841445684e-134,
            -1.6801064950215397e-128,
            -1.6801064950215397e-128,
            8.022010841445684e-134,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(8.022010841445684e-134, -1.6801064950215397e-128, -1.34778325178461e-261, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            8.022010841445684e-134,
            -1.6801064950215397e-128,
            -2.5578852415133235e-83,
            -2.4751255832465527e-245,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.2694980182022733e-273,
            -2.983041464881081e285,
            -2.983041464881081e285,
            -2.2694980182022733e-273,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-2.2694980182022733e-273, -2.983041464881081e285, 6770006692762.82, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.2694980182022733e-273,
            -2.983041464881081e285,
            2.5429991286299653e-284,
            -1.5895547451901909e69,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            6.076210201094809e-66,
            3.268734408822325e-130,
            3.268734408822325e-130,
            6.076210201094809e-66,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(6.076210201094809e-66, 3.268734408822325e-130, 1.986151735955582e-195, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            6.076210201094809e-66,
            3.268734408822325e-130,
            3.648768673900375e-228,
            8.964196280756791e154,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -9.031119870584213e-49,
            -7.110331796086534e-151,
            -7.110331796086534e-151,
            -9.031119870584213e-49,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-9.031119870584213e-49, -7.110331796086534e-151, 6.421425877008383e-199, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -9.031119870584213e-49,
            -7.110331796086534e-151,
            -4.4487331704636503e263,
            7.2558367841552315e-99,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            2.2980423371627012e-164,
            1.9295085845415668e-172,
            1.9295085845415668e-172,
            2.2980423371627012e-164,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(2.2980423371627012e-164, 1.9295085845415668e-172, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            2.2980423371627012e-164,
            1.9295085845415668e-172,
            3.387763277451419e156,
            1.5602374048829332e109,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.5144530892373823e-244,
            1.2860541501261636e-177,
            1.2860541501261636e-177,
            -2.5144530892373823e-244,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(-2.5144530892373823e-244, 1.2860541501261636e-177, -0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.5144530892373823e-244,
            1.2860541501261636e-177,
            -2.3177022953151006e-222,
            4.977165749313355e40,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.174163400369047e-213,
            2.769784263758964e-75,
            2.769784263758964e-75,
            1.174163400369047e-213,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.174163400369047e-213, 2.769784263758964e-75, 3.2521793094239024e-288, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.174163400369047e-213,
            2.769784263758964e-75,
            5.318828556566654e-106,
            -2.56432015192006e-199,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            7.374742582160069e28,
            2.4503189061572906e-165,
            2.4503189061572906e-165,
            7.374742582160069e28,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(7.374742582160069e28, 2.4503189061572906e-165, 1.8070471177110053e-136, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            7.374742582160069e28,
            2.4503189061572906e-165,
            1.2539800109517433e-289,
            7.421230408187482e214,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            2.8172326429046027e-275,
            2.5589356358125138e219,
            2.5589356358125138e219,
            2.8172326429046027e-275,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(2.8172326429046027e-275, 2.5589356358125138e219, 7.209117004302858e-56, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            2.8172326429046027e-275,
            2.5589356358125138e219,
            -2.34382568427959e-6,
            3.238411641569298e242,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.285217102441179e308,
            4.514779957217082e-102,
            4.514779957217082e-102,
            -1.285217102441179e308,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.285217102441179e308, 4.514779957217082e-102, -5.802472414774048e206, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.285217102441179e308,
            4.514779957217082e-102,
            2.7508111318139533e149,
            7.418529902488585e301,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            4.402718501997988e139,
            -2.5799297045683e27,
            -2.5799297045683e27,
            4.402718501997988e139,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(4.402718501997988e139, -2.5799297045683e27, -1.1358704244157059e167, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            4.402718501997988e139,
            -2.5799297045683e27,
            -3.374913202612439e281,
            9.855482300833697e-48,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.2689965703297633e179,
            1.2705519244836777e306,
            1.2705519244836777e306,
            1.2689965703297633e179,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.2689965703297633e179,
            1.2705519244836777e306,
            -8.237616056018728e258,
            5.1261040932818936e-304,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -7.461339734398645e-294,
            -1.5056963651459054e-286,
            -1.5056963651459054e-286,
            -7.461339734398645e-294,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(-7.461339734398645e-294, -1.5056963651459054e-286, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -7.461339734398645e-294,
            -1.5056963651459054e-286,
            -7.15400049820133e283,
            -4.1254237645016093e-302,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -2.7276989334397786e100,
            2.7283779683053693e141,
            2.7283779683053693e141,
            -2.7276989334397786e100,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-2.7276989334397786e100, 2.7283779683053693e141, -7.442193674167146e241, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -2.7276989334397786e100,
            2.7283779683053693e141,
            102733.24597269979,
            -2.41326198862499e-225,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.9140921741276689e25,
            1.8341593652485173e-25,
            1.8341593652485173e-25,
            -1.9140921741276689e25,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.9140921741276689e25, 1.8341593652485173e-25, -3.51075008712516, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.9140921741276689e25,
            1.8341593652485173e-25,
            -2.1920758171084378e-286,
            -37213914412.696625,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.1187631418630266e16,
            2.2467790572313842e238,
            2.2467790572313842e238,
            1.1187631418630266e16,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.1187631418630266e16, 2.2467790572313842e238, 2.513613597140232e254, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.1187631418630266e16,
            2.2467790572313842e238,
            1.4102229574916728e276,
            3.7155104095706254e62,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            4.6019089281824834e-301,
            -1.7072924053552805e120,
            -1.7072924053552805e120,
            4.6019089281824834e-301,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            4.6019089281824834e-301,
            -1.7072924053552805e120,
            -7.856804163222613e-181,
            1.0,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            4.6019089281824834e-301,
            -1.7072924053552805e120,
            5.697837699532154e-238,
            -2.461810870099801e165,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -7.449781522752757e-6,
            2.0955405922977956e-80,
            2.0955405922977956e-80,
            -7.449781522752757e-6,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-7.449781522752757e-6, 2.0955405922977956e-80, -1.5611319584678487e-85, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -7.449781522752757e-6,
            2.0955405922977956e-80,
            -2.0756169772824216e56,
            -2.725289462578534e-157,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.87683445673648e-291,
            1.2267554554297112e257,
            1.2267554554297112e257,
            -1.87683445673648e-291,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.87683445673648e-291, 1.2267554554297112e257, -2.302416908739935e-34, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.87683445673648e-291,
            1.2267554554297112e257,
            -7.597668271699612e-167,
            -1.4874649135412562e36,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            2.2045721025247304e285,
            5.3858193168403965e177,
            5.3858193168403965e177,
            2.2045721025247304e285,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            2.2045721025247304e285,
            5.3858193168403965e177,
            -6.5714152550036405e-304,
            4.145558767440944e44,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -8.794565915348913e80,
            1.2685492297482976e-250,
            1.2685492297482976e-250,
            -8.794565915348913e80,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-8.794565915348913e80, 1.2685492297482976e-250, -1.1156339817886495e-169, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -8.794565915348913e80,
            1.2685492297482976e-250,
            0.15815511531152476,
            -9.371972008439491e44,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            4.652379797784354e-17,
            1.2676220932025595e-229,
            1.2676220932025595e-229,
            4.652379797784354e-17,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(4.652379797784354e-17, 1.2676220932025595e-229, 5.897459417640703e-246, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            4.652379797784354e-17,
            1.2676220932025595e-229,
            5.0506465540041414e-248,
            -3.1080977717977513e-8,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -5.621725969466011e198,
            1.887123004777289e255,
            1.887123004777289e255,
            -5.621725969466011e198,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -5.621725969466011e198,
            1.887123004777289e255,
            -1.7791624802691846e220,
            -2.2924425590557177e-204,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.1455800581929369e-302,
            5.393441781116169e-175,
            5.393441781116169e-175,
            1.1455800581929369e-302,
        ),
    );
    expectEq(false, CsgExactArithmetic.equalProducts(1.1455800581929369e-302, 5.393441781116169e-175, 0.0, 1.0));
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.1455800581929369e-302,
            5.393441781116169e-175,
            -9.27269808254716e-298,
            -2.2646254868568374e-80,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -3.558737199168415e-232,
            3.9383353147182956e297,
            3.9383353147182956e297,
            -3.558737199168415e-232,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-3.558737199168415e-232, 3.9383353147182956e297, -1.4015500387286645e66, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -3.558737199168415e-232,
            3.9383353147182956e297,
            1.936513377875338e207,
            2.6986156039966364e-124,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            1.5941734199880962e-155,
            2.4340460559282574e-20,
            2.4340460559282574e-20,
            1.5941734199880962e-155,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(1.5941734199880962e-155, 2.4340460559282574e-20, 3.880291525387687e-175, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            1.5941734199880962e-155,
            2.4340460559282574e-20,
            2.0310244371892213e-85,
            -3.068887186144737e-265,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -1.3176979994700154e-39,
            -9.454132166427323e272,
            -9.454132166427323e272,
            -1.3176979994700154e-39,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-1.3176979994700154e-39, -9.454132166427323e272, 1.2457691042426407e234, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -1.3176979994700154e-39,
            -9.454132166427323e272,
            -1.3131524606153e61,
            4.8452869290038407e55,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalProducts(
            -7.909085353644467e-101,
            -9.878182488648635e172,
            -9.878182488648635e172,
            -7.909085353644467e-101,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(-7.909085353644467e-101, -9.878182488648635e172, 7.812738844159818e72, 1.0),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalProducts(
            -7.909085353644467e-101,
            -9.878182488648635e172,
            4.855084724160233e-23,
            5.085990882907142e-22,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.2267179672387133e275,
            -8.069805151255082e-277,
            -5.423608739208936e-31,
            -8.069805151255082e-277,
            -1.2267179672387133e275,
            -5.423608739208936e-31,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.2267179672387133e275,
            -8.069805151255082e-277,
            -5.423608739208936e-31,
            -1.2267179672387133e275,
            -8.069805151255082e-277,
            -5.423608739208935e-31,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.2267179672387133e275,
            -8.069805151255082e-277,
            -5.423608739208936e-31,
            -3.8901218616724496e-301,
            7.420922789102481e-45,
            5.617895360944203e248,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.8266648783362077e-105,
            7.158816622723409e-95,
            2.624190583622378e-173,
            7.158816622723409e-95,
            -1.8266648783362077e-105,
            2.624190583622378e-173,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.8266648783362077e-105,
            7.158816622723409e-95,
            2.624190583622378e-173,
            -1.8266648783362077e-105,
            7.158816622723409e-95,
            2.6241905836223785e-173,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.8266648783362077e-105,
            7.158816622723409e-95,
            2.624190583622378e-173,
            6.34985002123085e252,
            -5.021572375702363e248,
            -2.7316799786428377e209,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            4.0058918865991687e-85,
            -1.0209641781326612e-196,
            1.1131852717441254e-178,
            -1.0209641781326612e-196,
            4.0058918865991687e-85,
            1.1131852717441254e-178,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.0058918865991687e-85,
            -1.0209641781326612e-196,
            1.1131852717441254e-178,
            4.0058918865991687e-85,
            -1.0209641781326612e-196,
            1.1131852717441255e-178,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.0058918865991687e-85,
            -1.0209641781326612e-196,
            1.1131852717441254e-178,
            -1.2660945280046128e236,
            4.893299571936474e180,
            2.2555865228909418e30,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.914360744221567e41,
            5.67834188195538e-192,
            1.3703726178520644e-232,
            5.67834188195538e-192,
            -2.914360744221567e41,
            1.3703726178520644e-232,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.914360744221567e41,
            5.67834188195538e-192,
            1.3703726178520644e-232,
            -2.914360744221567e41,
            5.67834188195538e-192,
            1.3703726178520646e-232,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.914360744221567e41,
            5.67834188195538e-192,
            1.3703726178520644e-232,
            -1.8850577305779804e-259,
            -1.1097858688657052e-214,
            -5.049798523595264e-241,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            3.22526547692798e-210,
            2.367837389841796e-35,
            -1.4706544086711624e166,
            2.367837389841796e-35,
            3.22526547692798e-210,
            -1.4706544086711624e166,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.22526547692798e-210,
            2.367837389841796e-35,
            -1.4706544086711624e166,
            3.22526547692798e-210,
            2.367837389841796e-35,
            -1.4706544086711623e166,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.22526547692798e-210,
            2.367837389841796e-35,
            -1.4706544086711624e166,
            8.095858324215864e245,
            1.6482209650872074e-45,
            6.308589892231096e-263,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.611620321935601e-89,
            -1.0100322025026361e142,
            -536135.3498122204,
            -1.0100322025026361e142,
            -2.611620321935601e-89,
            -536135.3498122204,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.611620321935601e-89,
            -1.0100322025026361e142,
            -536135.3498122204,
            -2.611620321935601e-89,
            -1.0100322025026361e142,
            -536135.3498122203,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.611620321935601e-89,
            -1.0100322025026361e142,
            -536135.3498122204,
            -6.136886370972491e-141,
            -5.844081070140621e-47,
            -1.091723986764871e-121,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            4.070708250154831e-19,
            3.0245495887285514e237,
            -4.402877003043162e-80,
            3.0245495887285514e237,
            4.070708250154831e-19,
            -4.402877003043162e-80,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.070708250154831e-19,
            3.0245495887285514e237,
            -4.402877003043162e-80,
            4.070708250154831e-19,
            3.0245495887285514e237,
            -4.4028770030431615e-80,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.070708250154831e-19,
            3.0245495887285514e237,
            -4.402877003043162e-80,
            -1.2236812964976699e42,
            4.925499363867994e203,
            2.386482542943705e249,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.1897933181497146e247,
            -2.5604985037553315e-170,
            -1.7201049260153442e33,
            -2.5604985037553315e-170,
            -1.1897933181497146e247,
            -1.7201049260153442e33,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.1897933181497146e247,
            -2.5604985037553315e-170,
            -1.7201049260153442e33,
            -1.1897933181497146e247,
            -2.5604985037553315e-170,
            -1.720104926015344e33,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.1897933181497146e247,
            -2.5604985037553315e-170,
            -1.7201049260153442e33,
            8.605064156741485e-269,
            1.954234137096891e299,
            -1.4248872792451159e208,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -223.82392618162524,
            -10819743.55272981,
            -7.305098176656374e177,
            -10819743.55272981,
            -223.82392618162524,
            -7.305098176656374e177,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -223.82392618162524,
            -10819743.55272981,
            -7.305098176656374e177,
            -223.82392618162524,
            -10819743.55272981,
            -7.305098176656373e177,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -223.82392618162524,
            -10819743.55272981,
            -7.305098176656374e177,
            6.992822952372267e-284,
            -6.694164069849535e302,
            -1.3789404380988692e-249,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.2693409456527845e185,
            -4.628546847179848e149,
            -2.10157355457235e80,
            -4.628546847179848e149,
            -2.2693409456527845e185,
            -2.10157355457235e80,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.2693409456527845e185,
            -4.628546847179848e149,
            -2.10157355457235e80,
            -2.2693409456527845e185,
            -4.628546847179848e149,
            -2.1015735545723498e80,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.2693409456527845e185,
            -4.628546847179848e149,
            -2.10157355457235e80,
            2.4023871834570075e-13,
            2.1787244035007323e197,
            -3.5426680020795334e286,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.0392722992967585e-5,
            7.304474006196308e-152,
            1.585353642614242e221,
            7.304474006196308e-152,
            2.0392722992967585e-5,
            1.585353642614242e221,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.0392722992967585e-5,
            7.304474006196308e-152,
            1.585353642614242e221,
            2.0392722992967585e-5,
            7.304474006196308e-152,
            1.5853536426142422e221,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.0392722992967585e-5,
            7.304474006196308e-152,
            1.585353642614242e221,
            -3.154149149824443e-42,
            -5.622635916612863e209,
            1.984073906617701e174,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.6419017158590967e218,
            -3.9645568272458196e-33,
            1.8786091453020026e123,
            -3.9645568272458196e-33,
            2.6419017158590967e218,
            1.8786091453020026e123,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.6419017158590967e218,
            -3.9645568272458196e-33,
            1.8786091453020026e123,
            2.6419017158590967e218,
            -3.9645568272458196e-33,
            1.878609145302003e123,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.6419017158590967e218,
            -3.9645568272458196e-33,
            1.8786091453020026e123,
            -1.131879393927308e156,
            -2.812795948747655e-68,
            9.665536746693267e-65,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.3697879869044537e-269,
            1.2945442461676222e76,
            8.506573348721635e-24,
            1.2945442461676222e76,
            -1.3697879869044537e-269,
            8.506573348721635e-24,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.3697879869044537e-269,
            1.2945442461676222e76,
            8.506573348721635e-24,
            -1.3697879869044537e-269,
            1.2945442461676222e76,
            8.506573348721636e-24,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.3697879869044537e-269,
            1.2945442461676222e76,
            8.506573348721635e-24,
            -7.07047292379874e-172,
            -3.843596768581038e107,
            1.6843634013754272e-166,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.0735447067787838e304,
            5.870954639096086e82,
            -3.383935492286231e154,
            5.870954639096086e82,
            -1.0735447067787838e304,
            -3.383935492286231e154,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.0735447067787838e304,
            5.870954639096086e82,
            -3.383935492286231e154,
            -1.0735447067787838e304,
            5.870954639096086e82,
            -3.38393549228623e154,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.0735447067787838e304,
            5.870954639096086e82,
            -3.383935492286231e154,
            -1.5866203990100068e294,
            2.7198075204288737e-257,
            -2.3571901460526595e-161,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.9154766939034856e128,
            -2.041894056070378e-214,
            5.197404489078339e-55,
            -2.041894056070378e-214,
            -1.9154766939034856e128,
            5.197404489078339e-55,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.9154766939034856e128,
            -2.041894056070378e-214,
            5.197404489078339e-55,
            -1.9154766939034856e128,
            -2.041894056070378e-214,
            5.19740448907834e-55,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.9154766939034856e128,
            -2.041894056070378e-214,
            5.197404489078339e-55,
            -3.6323438623270235e-65,
            -8.623925623631059e-282,
            3.0490096589559794e137,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.369315305542141e83,
            1.301088512526853e-153,
            1.09253850767121e-138,
            1.301088512526853e-153,
            2.369315305542141e83,
            1.09253850767121e-138,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.369315305542141e83,
            1.301088512526853e-153,
            1.09253850767121e-138,
            2.369315305542141e83,
            1.301088512526853e-153,
            1.0925385076712101e-138,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.369315305542141e83,
            1.301088512526853e-153,
            1.09253850767121e-138,
            -2.148462540724428e-128,
            -4.550517587482632e-137,
            -1.0604755974107604e-109,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            5.2519518311414225e135,
            -9.523804910536171e-262,
            1.748858174420493e145,
            -9.523804910536171e-262,
            5.2519518311414225e135,
            1.748858174420493e145,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            5.2519518311414225e135,
            -9.523804910536171e-262,
            1.748858174420493e145,
            5.2519518311414225e135,
            -9.523804910536171e-262,
            1.7488581744204933e145,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            5.2519518311414225e135,
            -9.523804910536171e-262,
            1.748858174420493e145,
            1.1058619618712686e84,
            3.949959243675927e50,
            6.00084816921276e-199,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            7.110643064220375e135,
            9.995308286854402e-193,
            1.3334714842783373e187,
            9.995308286854402e-193,
            7.110643064220375e135,
            1.3334714842783373e187,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            7.110643064220375e135,
            9.995308286854402e-193,
            1.3334714842783373e187,
            7.110643064220375e135,
            9.995308286854402e-193,
            1.3334714842783374e187,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            7.110643064220375e135,
            9.995308286854402e-193,
            1.3334714842783373e187,
            -1.8459529342377285e86,
            -4.6441405134328484e-241,
            5.053151769662349e205,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.2712942781955835e287,
            -3.3847620659598737e-274,
            4.889653058159848e-16,
            -3.3847620659598737e-274,
            -2.2712942781955835e287,
            4.889653058159848e-16,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.2712942781955835e287,
            -3.3847620659598737e-274,
            4.889653058159848e-16,
            -2.2712942781955835e287,
            -3.3847620659598737e-274,
            4.889653058159849e-16,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.2712942781955835e287,
            -3.3847620659598737e-274,
            4.889653058159848e-16,
            7.485231471140764e-203,
            -6.15031892660427e-245,
            -1.9661176205582303e266,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.032817591209111e281,
            -3.166743771601185e40,
            7.328227408595228e157,
            -3.166743771601185e40,
            1.032817591209111e281,
            7.328227408595228e157,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.032817591209111e281,
            -3.166743771601185e40,
            7.328227408595228e157,
            1.032817591209111e281,
            -3.166743771601185e40,
            7.32822740859523e157,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.032817591209111e281,
            -3.166743771601185e40,
            7.328227408595228e157,
            2.988754317411153e234,
            6.299852758475382e244,
            9.085196033432809e163,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            9.711151741400262e174,
            3.9370507666754533e108,
            -5.712747971121085e182,
            3.9370507666754533e108,
            9.711151741400262e174,
            -5.712747971121085e182,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            9.711151741400262e174,
            3.9370507666754533e108,
            -5.712747971121085e182,
            9.711151741400262e174,
            3.9370507666754533e108,
            -5.712747971121084e182,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            9.711151741400262e174,
            3.9370507666754533e108,
            -5.712747971121085e182,
            -3.3071762926886873e-90,
            -2.1637048408580049e-302,
            -1.5166149722483087e115,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            9.957286774717794e113,
            8.460865513204359e-141,
            -5.325739199723164e-61,
            8.460865513204359e-141,
            9.957286774717794e113,
            -5.325739199723164e-61,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            9.957286774717794e113,
            8.460865513204359e-141,
            -5.325739199723164e-61,
            9.957286774717794e113,
            8.460865513204359e-141,
            -5.325739199723163e-61,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            9.957286774717794e113,
            8.460865513204359e-141,
            -5.325739199723164e-61,
            2.565125681374196e196,
            -1.4392676125657211e296,
            -1.5344249199638732e126,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            4.1052315609294255e-110,
            6.218313349100793e-289,
            1.8984857364210202e-296,
            6.218313349100793e-289,
            4.1052315609294255e-110,
            1.8984857364210202e-296,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.1052315609294255e-110,
            6.218313349100793e-289,
            1.8984857364210202e-296,
            4.1052315609294255e-110,
            6.218313349100793e-289,
            1.8984857364210205e-296,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.1052315609294255e-110,
            6.218313349100793e-289,
            1.8984857364210202e-296,
            6.838355018167461e-61,
            1.554384527109461e-92,
            1.48981936192023e211,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.593366044545772e75,
            -2.4009833630596432e199,
            2.6563283652900983e-213,
            -2.4009833630596432e199,
            1.593366044545772e75,
            2.6563283652900983e-213,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.593366044545772e75,
            -2.4009833630596432e199,
            2.6563283652900983e-213,
            1.593366044545772e75,
            -2.4009833630596432e199,
            2.6563283652900987e-213,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.593366044545772e75,
            -2.4009833630596432e199,
            2.6563283652900983e-213,
            -9.24763460685412e-225,
            -7.188937500488974e-61,
            -1.9716480437437766e-267,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.6224545314156195e24,
            -5.542011752636355e-152,
            -3.829910845084557e-297,
            -5.542011752636355e-152,
            -2.6224545314156195e24,
            -3.829910845084557e-297,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.6224545314156195e24,
            -5.542011752636355e-152,
            -3.829910845084557e-297,
            -2.6224545314156195e24,
            -5.542011752636355e-152,
            -3.829910845084556e-297,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.6224545314156195e24,
            -5.542011752636355e-152,
            -3.829910845084557e-297,
            1.6073502465352606e126,
            -4.4749551483966703e238,
            -1.9419533901954654e24,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -8.4453939360605e173,
            6.104456151974506e-18,
            -91436.11150098052,
            6.104456151974506e-18,
            -8.4453939360605e173,
            -91436.11150098052,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -8.4453939360605e173,
            6.104456151974506e-18,
            -91436.11150098052,
            -8.4453939360605e173,
            6.104456151974506e-18,
            -91436.1115009805,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -8.4453939360605e173,
            6.104456151974506e-18,
            -91436.11150098052,
            -2.898322692366407e107,
            1.725348204293674e-40,
            4.730738563115299e189,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.5318052877995813e280,
            1.2357921099127642e-44,
            1.2058644359995673e-283,
            1.2357921099127642e-44,
            -1.5318052877995813e280,
            1.2058644359995673e-283,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.5318052877995813e280,
            1.2357921099127642e-44,
            1.2058644359995673e-283,
            -1.5318052877995813e280,
            1.2357921099127642e-44,
            1.2058644359995675e-283,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.5318052877995813e280,
            1.2357921099127642e-44,
            1.2058644359995673e-283,
            6.5138088764833004e296,
            1.2420700725445753e-58,
            3.491470035112778e-75,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            8.113249362590482e-277,
            2.8611359306647133e-261,
            8.79016648233894e-256,
            2.8611359306647133e-261,
            8.113249362590482e-277,
            8.79016648233894e-256,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            8.113249362590482e-277,
            2.8611359306647133e-261,
            8.79016648233894e-256,
            8.113249362590482e-277,
            2.8611359306647133e-261,
            8.790166482338941e-256,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            8.113249362590482e-277,
            2.8611359306647133e-261,
            8.79016648233894e-256,
            8.922102291294371e147,
            1.1192728556451472e266,
            2.2403472338762817e-115,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.028116727006527e125,
            1.36707493996039e-195,
            -5249117682.938091,
            1.36707493996039e-195,
            1.028116727006527e125,
            -5249117682.938091,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.028116727006527e125,
            1.36707493996039e-195,
            -5249117682.938091,
            1.028116727006527e125,
            1.36707493996039e-195,
            -5249117682.93809,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.028116727006527e125,
            1.36707493996039e-195,
            -5249117682.938091,
            4.518291729264486e-71,
            -8.833438749995521e84,
            -1.8830204504846136e-97,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.8677839001037726e-5,
            2.5559280886814654e270,
            -4.4924277744524784e163,
            2.5559280886814654e270,
            2.8677839001037726e-5,
            -4.4924277744524784e163,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.8677839001037726e-5,
            2.5559280886814654e270,
            -4.4924277744524784e163,
            2.8677839001037726e-5,
            2.5559280886814654e270,
            -4.492427774452478e163,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.8677839001037726e-5,
            2.5559280886814654e270,
            -4.4924277744524784e163,
            -1.0235115878777002e-26,
            -1.9588113179054898e183,
            -3.483708960698774e-186,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.700320026618988e-187,
            -7.769469074005825e23,
            6.259848721306274e-130,
            -7.769469074005825e23,
            -2.700320026618988e-187,
            6.259848721306274e-130,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.700320026618988e-187,
            -7.769469074005825e23,
            6.259848721306274e-130,
            -2.700320026618988e-187,
            -7.769469074005825e23,
            6.2598487213062745e-130,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.700320026618988e-187,
            -7.769469074005825e23,
            6.259848721306274e-130,
            -3.285229960053892e-166,
            -3.6927888161995747e202,
            1.9036748253000696e174,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -4.618083605869897e-192,
            8.445613093206941e39,
            30964082.363294963,
            8.445613093206941e39,
            -4.618083605869897e-192,
            30964082.363294963,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.618083605869897e-192,
            8.445613093206941e39,
            30964082.363294963,
            -4.618083605869897e-192,
            8.445613093206941e39,
            30964082.363294967,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.618083605869897e-192,
            8.445613093206941e39,
            30964082.363294963,
            -1.3913753371554912e-188,
            2.3611074990327324e154,
            -1.8577776579863838e-277,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -7.540306792550561e-127,
            5.632414079051136e157,
            7.19953704494381e47,
            5.632414079051136e157,
            -7.540306792550561e-127,
            7.19953704494381e47,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -7.540306792550561e-127,
            5.632414079051136e157,
            7.19953704494381e47,
            -7.540306792550561e-127,
            5.632414079051136e157,
            7.199537044943811e47,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -7.540306792550561e-127,
            5.632414079051136e157,
            7.19953704494381e47,
            -8.585405212789652e-153,
            -1.6803155448990658e-144,
            1.7345104199029595e172,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.920119185588147e-21,
            -1.2224196373534328e229,
            -8.53169682285933e30,
            -1.2224196373534328e229,
            2.920119185588147e-21,
            -8.53169682285933e30,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.920119185588147e-21,
            -1.2224196373534328e229,
            -8.53169682285933e30,
            2.920119185588147e-21,
            -1.2224196373534328e229,
            -8.531696822859329e30,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.920119185588147e-21,
            -1.2224196373534328e229,
            -8.53169682285933e30,
            2.702653007599359e275,
            3.0689031659314033e-105,
            7.828549462039224e-265,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.196668403562649e-167,
            4.769714273536195e-142,
            -6.033118124949737e-285,
            4.769714273536195e-142,
            -1.196668403562649e-167,
            -6.033118124949737e-285,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.196668403562649e-167,
            4.769714273536195e-142,
            -6.033118124949737e-285,
            -1.196668403562649e-167,
            4.769714273536195e-142,
            -6.0331181249497365e-285,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.196668403562649e-167,
            4.769714273536195e-142,
            -6.033118124949737e-285,
            -7.395340247492872e62,
            -1.055280378522195e68,
            -8.950197932647121e154,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -5.9530290627741504e-307,
            -6.2055527078001835e-96,
            -1.7678660289296065e277,
            -6.2055527078001835e-96,
            -5.9530290627741504e-307,
            -1.7678660289296065e277,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.9530290627741504e-307,
            -6.2055527078001835e-96,
            -1.7678660289296065e277,
            -5.9530290627741504e-307,
            -6.2055527078001835e-96,
            -1.7678660289296063e277,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.9530290627741504e-307,
            -6.2055527078001835e-96,
            -1.7678660289296065e277,
            8.045261522196495e-151,
            1.3854717141899371e-86,
            -3.338167771711886e178,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -7.683628576875931e209,
            -1.3450170539092137e124,
            2.676524954102102e-66,
            -1.3450170539092137e124,
            -7.683628576875931e209,
            2.676524954102102e-66,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -7.683628576875931e209,
            -1.3450170539092137e124,
            2.676524954102102e-66,
            -7.683628576875931e209,
            -1.3450170539092137e124,
            2.6765249541021027e-66,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -7.683628576875931e209,
            -1.3450170539092137e124,
            2.676524954102102e-66,
            1.1793480414975705e-153,
            -4.4340789964183e167,
            1.4709114580965052e39,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -8.790516440502866e-41,
            6.456404789049343e220,
            -1.2634665730382186e-173,
            6.456404789049343e220,
            -8.790516440502866e-41,
            -1.2634665730382186e-173,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -8.790516440502866e-41,
            6.456404789049343e220,
            -1.2634665730382186e-173,
            -8.790516440502866e-41,
            6.456404789049343e220,
            -1.2634665730382184e-173,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -8.790516440502866e-41,
            6.456404789049343e220,
            -1.2634665730382186e-173,
            -4.675477461789432e190,
            -1.9518733985354812e187,
            -2.0002562976783008e19,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            9.317745946579933e167,
            -1.4267585328273097e139,
            6.623487847654484e-159,
            -1.4267585328273097e139,
            9.317745946579933e167,
            6.623487847654484e-159,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            9.317745946579933e167,
            -1.4267585328273097e139,
            6.623487847654484e-159,
            9.317745946579933e167,
            -1.4267585328273097e139,
            6.623487847654485e-159,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            9.317745946579933e167,
            -1.4267585328273097e139,
            6.623487847654484e-159,
            5.534849203773181e-245,
            2.5553165821983163e-272,
            -6.178719087528672e-273,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.1846678998728034e171,
            3.089880004439692e263,
            3.19631619553476e39,
            3.089880004439692e263,
            1.1846678998728034e171,
            3.19631619553476e39,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.1846678998728034e171,
            3.089880004439692e263,
            3.19631619553476e39,
            1.1846678998728034e171,
            3.089880004439692e263,
            3.196316195534761e39,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.1846678998728034e171,
            3.089880004439692e263,
            3.19631619553476e39,
            -7.0598667243309085e217,
            -4.531861168651926e-102,
            7.451185831672626e152,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -3.877583038363232e224,
            -4.6590254573039926e302,
            -3.5454701444952863e103,
            -4.6590254573039926e302,
            -3.877583038363232e224,
            -3.5454701444952863e103,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.877583038363232e224,
            -4.6590254573039926e302,
            -3.5454701444952863e103,
            -3.877583038363232e224,
            -4.6590254573039926e302,
            -3.545470144495286e103,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.877583038363232e224,
            -4.6590254573039926e302,
            -3.5454701444952863e103,
            3.078980331165725e-293,
            -6.725659044384825e284,
            -5.072716281503803e-182,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            6.055169250166733e-34,
            -6.308903009057975e29,
            -8.28549278019065e-78,
            -6.308903009057975e29,
            6.055169250166733e-34,
            -8.28549278019065e-78,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            6.055169250166733e-34,
            -6.308903009057975e29,
            -8.28549278019065e-78,
            6.055169250166733e-34,
            -6.308903009057975e29,
            -8.285492780190649e-78,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            6.055169250166733e-34,
            -6.308903009057975e29,
            -8.28549278019065e-78,
            -2.3251011407615257e-161,
            -2.0933681001186925e-128,
            6.925040823021012e-177,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.0250996330914544e195,
            3.95649775548807e186,
            -3.060341331613473e-219,
            3.95649775548807e186,
            1.0250996330914544e195,
            -3.060341331613473e-219,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.0250996330914544e195,
            3.95649775548807e186,
            -3.060341331613473e-219,
            1.0250996330914544e195,
            3.95649775548807e186,
            -3.0603413316134725e-219,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.0250996330914544e195,
            3.95649775548807e186,
            -3.060341331613473e-219,
            -1.6732027504721948e-146,
            1.475610156228193e168,
            6.74724900638552e88,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.3135297917668673e-104,
            2.2141122665084157e-246,
            1.8808019539329742e154,
            2.2141122665084157e-246,
            2.3135297917668673e-104,
            1.8808019539329742e154,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.3135297917668673e-104,
            2.2141122665084157e-246,
            1.8808019539329742e154,
            2.3135297917668673e-104,
            2.2141122665084157e-246,
            1.8808019539329745e154,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.3135297917668673e-104,
            2.2141122665084157e-246,
            1.8808019539329742e154,
            -806105698.3049163,
            -2.3092715063099882e147,
            -6.234052466977662e176,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.3702383433793013e-201,
            6.254925195053221e-207,
            3.026279224128639e-92,
            6.254925195053221e-207,
            1.3702383433793013e-201,
            3.026279224128639e-92,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.3702383433793013e-201,
            6.254925195053221e-207,
            3.026279224128639e-92,
            1.3702383433793013e-201,
            6.254925195053221e-207,
            3.0262792241286392e-92,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.3702383433793013e-201,
            6.254925195053221e-207,
            3.026279224128639e-92,
            -4.674086307825919e59,
            1.0729251967637693e83,
            -22578847275443.58,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -7.575165997975436e-79,
            1.3826820747621644e73,
            -1.0618812363685226e166,
            1.3826820747621644e73,
            -7.575165997975436e-79,
            -1.0618812363685226e166,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -7.575165997975436e-79,
            1.3826820747621644e73,
            -1.0618812363685226e166,
            -7.575165997975436e-79,
            1.3826820747621644e73,
            -1.0618812363685224e166,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -7.575165997975436e-79,
            1.3826820747621644e73,
            -1.0618812363685226e166,
            2.9251235966769305e-204,
            -8.357820674301766e-170,
            -4.022089239348647e180,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.8795691013641251e-50,
            7.790317700729721e-306,
            -6.521571396542372e103,
            7.790317700729721e-306,
            -1.8795691013641251e-50,
            -6.521571396542372e103,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.8795691013641251e-50,
            7.790317700729721e-306,
            -6.521571396542372e103,
            -1.8795691013641251e-50,
            7.790317700729721e-306,
            -6.521571396542371e103,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.8795691013641251e-50,
            7.790317700729721e-306,
            -6.521571396542372e103,
            -1.2051004514681466e-27,
            -1.110461016213752e271,
            2.4353104939113564e253,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            9.39970922166975e-129,
            -8.041283133807937e-213,
            -1.8748741394651962e-243,
            -8.041283133807937e-213,
            9.39970922166975e-129,
            -1.8748741394651962e-243,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            9.39970922166975e-129,
            -8.041283133807937e-213,
            -1.8748741394651962e-243,
            9.39970922166975e-129,
            -8.041283133807937e-213,
            -1.874874139465196e-243,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            9.39970922166975e-129,
            -8.041283133807937e-213,
            -1.8748741394651962e-243,
            -7.504644185928366e-191,
            -5.555043278463416e53,
            9.450384159270897e-194,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.4170889631461035e149,
            -1.9097081340442046e-128,
            1.1469802676033218e-128,
            -1.9097081340442046e-128,
            -2.4170889631461035e149,
            1.1469802676033218e-128,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.4170889631461035e149,
            -1.9097081340442046e-128,
            1.1469802676033218e-128,
            -2.4170889631461035e149,
            -1.9097081340442046e-128,
            1.146980267603322e-128,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.4170889631461035e149,
            -1.9097081340442046e-128,
            1.1469802676033218e-128,
            -5.2921446580517576e-30,
            4.0740310631327736e189,
            1.914047348364675e148,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            9.014334902344102e290,
            5.590054619367038e-6,
            -5.8524099901233425e-30,
            5.590054619367038e-6,
            9.014334902344102e290,
            -5.8524099901233425e-30,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            9.014334902344102e290,
            5.590054619367038e-6,
            -5.8524099901233425e-30,
            9.014334902344102e290,
            5.590054619367038e-6,
            -5.852409990123342e-30,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            9.014334902344102e290,
            5.590054619367038e-6,
            -5.8524099901233425e-30,
            -7.934511174237544e-224,
            -5.41074114154092e71,
            4.1234907212554575e262,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            6.90484704055892e-101,
            7.987601391123801e-152,
            4.643703952625533e100,
            7.987601391123801e-152,
            6.90484704055892e-101,
            4.643703952625533e100,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            6.90484704055892e-101,
            7.987601391123801e-152,
            4.643703952625533e100,
            6.90484704055892e-101,
            7.987601391123801e-152,
            4.643703952625534e100,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            6.90484704055892e-101,
            7.987601391123801e-152,
            4.643703952625533e100,
            -5.008080592383958e49,
            2.9829047245305747e-95,
            -2.9637178194991043e288,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.671101374303602e103,
            -1.3450769076012782e289,
            9.988019825089622e-26,
            -1.3450769076012782e289,
            2.671101374303602e103,
            9.988019825089622e-26,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.671101374303602e103,
            -1.3450769076012782e289,
            9.988019825089622e-26,
            2.671101374303602e103,
            -1.3450769076012782e289,
            9.988019825089623e-26,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.671101374303602e103,
            -1.3450769076012782e289,
            9.988019825089622e-26,
            -7.827942770990756e283,
            -5.633974140947059e-84,
            -2.7986910578652503e295,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.1928182917561038e155,
            1.503124770728699e146,
            2.2425717216060802e-227,
            1.503124770728699e146,
            -1.1928182917561038e155,
            2.2425717216060802e-227,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.1928182917561038e155,
            1.503124770728699e146,
            2.2425717216060802e-227,
            -1.1928182917561038e155,
            1.503124770728699e146,
            2.2425717216060807e-227,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.1928182917561038e155,
            1.503124770728699e146,
            2.2425717216060802e-227,
            1.031822425752281e-218,
            -1.1401731171045622e-272,
            1.5170979265486963e212,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            8.042583468684657e-304,
            6.93680927222386e116,
            1.8007441016695182e-23,
            6.93680927222386e116,
            8.042583468684657e-304,
            1.8007441016695182e-23,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            8.042583468684657e-304,
            6.93680927222386e116,
            1.8007441016695182e-23,
            8.042583468684657e-304,
            6.93680927222386e116,
            1.8007441016695185e-23,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            8.042583468684657e-304,
            6.93680927222386e116,
            1.8007441016695182e-23,
            5.429830108286974e-186,
            -1.869988305021154e249,
            -1.408246530126333e25,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -3.015484451088033e-106,
            -1.7835467549787093e162,
            -8.881671093736535e-160,
            -1.7835467549787093e162,
            -3.015484451088033e-106,
            -8.881671093736535e-160,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.015484451088033e-106,
            -1.7835467549787093e162,
            -8.881671093736535e-160,
            -3.015484451088033e-106,
            -1.7835467549787093e162,
            -8.881671093736534e-160,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.015484451088033e-106,
            -1.7835467549787093e162,
            -8.881671093736535e-160,
            -2.0486181638679673e-23,
            2.775895266410559e-196,
            6.490858640167191e216,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -3.7555916928348965e-103,
            2.0173452070687425e304,
            1.8952896976162237e-294,
            2.0173452070687425e304,
            -3.7555916928348965e-103,
            1.8952896976162237e-294,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.7555916928348965e-103,
            2.0173452070687425e304,
            1.8952896976162237e-294,
            -3.7555916928348965e-103,
            2.0173452070687425e304,
            1.895289697616224e-294,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.7555916928348965e-103,
            2.0173452070687425e304,
            1.8952896976162237e-294,
            2.9118588620352036e-201,
            8.050767159421067e69,
            -2.633450925195127e75,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.0139286170875936e-22,
            -1.3233239208286584e-182,
            1.747458469825929e-126,
            -1.3233239208286584e-182,
            1.0139286170875936e-22,
            1.747458469825929e-126,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.0139286170875936e-22,
            -1.3233239208286584e-182,
            1.747458469825929e-126,
            1.0139286170875936e-22,
            -1.3233239208286584e-182,
            1.7474584698259295e-126,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.0139286170875936e-22,
            -1.3233239208286584e-182,
            1.747458469825929e-126,
            7.574860765682867e-209,
            56481551111438.17,
            206.48219506891937,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -9.116950365692782e-163,
            -3.9967938869277725e-272,
            4.9082265592531e-26,
            -3.9967938869277725e-272,
            -9.116950365692782e-163,
            4.9082265592531e-26,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -9.116950365692782e-163,
            -3.9967938869277725e-272,
            4.9082265592531e-26,
            -9.116950365692782e-163,
            -3.9967938869277725e-272,
            4.9082265592531003e-26,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -9.116950365692782e-163,
            -3.9967938869277725e-272,
            4.9082265592531e-26,
            5.21684017355212e205,
            1.487480988394845e39,
            1.8984722429809815e-205,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -5.972860218746337e-69,
            1.3870978469744932e297,
            1.1250194390456323e105,
            1.3870978469744932e297,
            -5.972860218746337e-69,
            1.1250194390456323e105,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.972860218746337e-69,
            1.3870978469744932e297,
            1.1250194390456323e105,
            -5.972860218746337e-69,
            1.3870978469744932e297,
            1.1250194390456324e105,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.972860218746337e-69,
            1.3870978469744932e297,
            1.1250194390456323e105,
            1.0409428225122381e-268,
            -4.515704506376415e-150,
            4.7534870684544247e188,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            3.290739003609309e44,
            9.997513102748044e216,
            -7.43115268178874e-176,
            9.997513102748044e216,
            3.290739003609309e44,
            -7.43115268178874e-176,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.290739003609309e44,
            9.997513102748044e216,
            -7.43115268178874e-176,
            3.290739003609309e44,
            9.997513102748044e216,
            -7.431152681788738e-176,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.290739003609309e44,
            9.997513102748044e216,
            -7.43115268178874e-176,
            4.986918760706941e-229,
            4.0456190997946335e-16,
            1.5414181346926668e-31,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.659200268352166e-47,
            2.5275223258491476e-116,
            -2.720523023427353e133,
            2.5275223258491476e-116,
            2.659200268352166e-47,
            -2.720523023427353e133,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.659200268352166e-47,
            2.5275223258491476e-116,
            -2.720523023427353e133,
            2.659200268352166e-47,
            2.5275223258491476e-116,
            -2.7205230234273524e133,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.659200268352166e-47,
            2.5275223258491476e-116,
            -2.720523023427353e133,
            3.334230454316357e-281,
            -7.932181645425356e151,
            -3.038360810968509e120,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.3152122113850742e219,
            7.018889852116343e275,
            -3.307109770168659e269,
            7.018889852116343e275,
            1.3152122113850742e219,
            -3.307109770168659e269,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.3152122113850742e219,
            7.018889852116343e275,
            -3.307109770168659e269,
            1.3152122113850742e219,
            7.018889852116343e275,
            -3.307109770168658e269,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.3152122113850742e219,
            7.018889852116343e275,
            -3.307109770168659e269,
            -5.786903615523437e298,
            2.9043984784307468e57,
            -2.283839954498926e-69,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            7.077969411752192e-128,
            -8.811582557517315e-235,
            -2.5931550843000125e226,
            -8.811582557517315e-235,
            7.077969411752192e-128,
            -2.5931550843000125e226,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            7.077969411752192e-128,
            -8.811582557517315e-235,
            -2.5931550843000125e226,
            7.077969411752192e-128,
            -8.811582557517315e-235,
            -2.593155084300012e226,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            7.077969411752192e-128,
            -8.811582557517315e-235,
            -2.5931550843000125e226,
            2.3326814492367773e70,
            -2.57977866427243e138,
            1.3149838255190293e-84,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.3195345738525887e-202,
            2.0809998067255264e-282,
            -7.739305746801032e-262,
            2.0809998067255264e-282,
            1.3195345738525887e-202,
            -7.739305746801032e-262,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.3195345738525887e-202,
            2.0809998067255264e-282,
            -7.739305746801032e-262,
            1.3195345738525887e-202,
            2.0809998067255264e-282,
            -7.739305746801031e-262,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.3195345738525887e-202,
            2.0809998067255264e-282,
            -7.739305746801032e-262,
            -1.8200343146838542e255,
            5.6413841971125144e103,
            -9.865434697311022e-284,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            8.465791712020063e-242,
            2.978126710553754e254,
            -5.8630958389289195e-276,
            2.978126710553754e254,
            8.465791712020063e-242,
            -5.8630958389289195e-276,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            8.465791712020063e-242,
            2.978126710553754e254,
            -5.8630958389289195e-276,
            8.465791712020063e-242,
            2.978126710553754e254,
            -5.863095838928919e-276,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            8.465791712020063e-242,
            2.978126710553754e254,
            -5.8630958389289195e-276,
            4.876443243911336e-97,
            1.3370713895091254e-95,
            -1.8227432752858595e25,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.7014819266508327e-139,
            7.60122448066727e115,
            -7.444460171811843e-48,
            7.60122448066727e115,
            2.7014819266508327e-139,
            -7.444460171811843e-48,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.7014819266508327e-139,
            7.60122448066727e115,
            -7.444460171811843e-48,
            2.7014819266508327e-139,
            7.60122448066727e115,
            -7.444460171811842e-48,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.7014819266508327e-139,
            7.60122448066727e115,
            -7.444460171811843e-48,
            1.8568349997421233e-96,
            -3.726695662958892e-187,
            -8.682096518764652e69,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            8.78182169943225e-270,
            6.1056762664473e134,
            1.065795098817174e284,
            6.1056762664473e134,
            8.78182169943225e-270,
            1.065795098817174e284,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            8.78182169943225e-270,
            6.1056762664473e134,
            1.065795098817174e284,
            8.78182169943225e-270,
            6.1056762664473e134,
            1.0657950988171742e284,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            8.78182169943225e-270,
            6.1056762664473e134,
            1.065795098817174e284,
            2.4933040117533456e-245,
            8.400587445126342e206,
            5.890841994591357e-41,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            5.2115588990598465e-227,
            1.4257615944990996e62,
            5.695081609990034e-251,
            1.4257615944990996e62,
            5.2115588990598465e-227,
            5.695081609990034e-251,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            5.2115588990598465e-227,
            1.4257615944990996e62,
            5.695081609990034e-251,
            5.2115588990598465e-227,
            1.4257615944990996e62,
            5.695081609990035e-251,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            5.2115588990598465e-227,
            1.4257615944990996e62,
            5.695081609990034e-251,
            6.464100197135071e241,
            5.268503536849995e-8,
            -2.3086315948367057e296,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.0650949771130934e-281,
            -2.5243721546507093e35,
            -2.881200113433366e-238,
            -2.5243721546507093e35,
            -1.0650949771130934e-281,
            -2.881200113433366e-238,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.0650949771130934e-281,
            -2.5243721546507093e35,
            -2.881200113433366e-238,
            -1.0650949771130934e-281,
            -2.5243721546507093e35,
            -2.8812001134333655e-238,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.0650949771130934e-281,
            -2.5243721546507093e35,
            -2.881200113433366e-238,
            -5.3286067398085126e85,
            -4.8956867503612264e160,
            8.158616433039422e-145,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -4.745510239624022e226,
            -1.6305599116366694e-193,
            2.0281081558114796e-231,
            -1.6305599116366694e-193,
            -4.745510239624022e226,
            2.0281081558114796e-231,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.745510239624022e226,
            -1.6305599116366694e-193,
            2.0281081558114796e-231,
            -4.745510239624022e226,
            -1.6305599116366694e-193,
            2.02810815581148e-231,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.745510239624022e226,
            -1.6305599116366694e-193,
            2.0281081558114796e-231,
            9.207567614432474e-307,
            -1.2950377308183e159,
            4.7155453543152716e300,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -8.312872646523928e68,
            -1.6161188709834637e62,
            -1.9453787565840173e231,
            -1.6161188709834637e62,
            -8.312872646523928e68,
            -1.9453787565840173e231,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -8.312872646523928e68,
            -1.6161188709834637e62,
            -1.9453787565840173e231,
            -8.312872646523928e68,
            -1.6161188709834637e62,
            -1.945378756584017e231,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -8.312872646523928e68,
            -1.6161188709834637e62,
            -1.9453787565840173e231,
            -4.509979313885096e-28,
            -2.7769556146026295e52,
            2.4994847525948392e-167,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.3555136288048412e196,
            1.2169247408137366e-307,
            -1.2228293705741458e-63,
            1.2169247408137366e-307,
            -2.3555136288048412e196,
            -1.2228293705741458e-63,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.3555136288048412e196,
            1.2169247408137366e-307,
            -1.2228293705741458e-63,
            -2.3555136288048412e196,
            1.2169247408137366e-307,
            -1.2228293705741455e-63,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.3555136288048412e196,
            1.2169247408137366e-307,
            -1.2228293705741458e-63,
            -2.0042665636678498e-302,
            2.1351982572630997e87,
            -3.9690112422444723e211,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -3.905147880490215e259,
            -1.2457147093392239e-6,
            -6.341578766673344e-212,
            -1.2457147093392239e-6,
            -3.905147880490215e259,
            -6.341578766673344e-212,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.905147880490215e259,
            -1.2457147093392239e-6,
            -6.341578766673344e-212,
            -3.905147880490215e259,
            -1.2457147093392239e-6,
            -6.341578766673343e-212,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.905147880490215e259,
            -1.2457147093392239e-6,
            -6.341578766673344e-212,
            -2.804795598191584e-228,
            -2.2568809406776494e-233,
            1.7497490464609567e-124,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.545566830389426e-280,
            -2.8847874091326847e-304,
            -3.305399247381801e-188,
            -2.8847874091326847e-304,
            -2.545566830389426e-280,
            -3.305399247381801e-188,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.545566830389426e-280,
            -2.8847874091326847e-304,
            -3.305399247381801e-188,
            -2.545566830389426e-280,
            -2.8847874091326847e-304,
            -3.3053992473818007e-188,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.545566830389426e-280,
            -2.8847874091326847e-304,
            -3.305399247381801e-188,
            -6.918755702888512e282,
            -5.113591275289108e281,
            -2.844310226256781e-196,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -4.668192133700708e-196,
            4.5230056780533945e51,
            3.701994479015593e307,
            4.5230056780533945e51,
            -4.668192133700708e-196,
            3.701994479015593e307,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.668192133700708e-196,
            4.5230056780533945e51,
            3.701994479015593e307,
            -4.668192133700708e-196,
            4.5230056780533945e51,
            3.7019944790155937e307,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.668192133700708e-196,
            4.5230056780533945e51,
            3.701994479015593e307,
            3.284016140296798e287,
            -9.967736156970791e76,
            2.6384366273418484e-12,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -4.723037460263346e-197,
            -2.3891698813920154e17,
            3.837965971203585e44,
            -2.3891698813920154e17,
            -4.723037460263346e-197,
            3.837965971203585e44,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.723037460263346e-197,
            -2.3891698813920154e17,
            3.837965971203585e44,
            -4.723037460263346e-197,
            -2.3891698813920154e17,
            3.837965971203586e44,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.723037460263346e-197,
            -2.3891698813920154e17,
            3.837965971203585e44,
            -2.268833845344765e-145,
            -8.111521867386179e65,
            -6.27471789920541e-93,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.1181948331666136e-266,
            -1.2431855671465035e191,
            -3.3731586734122645e-120,
            -1.2431855671465035e191,
            2.1181948331666136e-266,
            -3.3731586734122645e-120,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.1181948331666136e-266,
            -1.2431855671465035e191,
            -3.3731586734122645e-120,
            2.1181948331666136e-266,
            -1.2431855671465035e191,
            -3.373158673412264e-120,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.1181948331666136e-266,
            -1.2431855671465035e191,
            -3.3731586734122645e-120,
            -2.70857720805329e163,
            4.519460057765979e244,
            -3.771063546751823e-97,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -3.7222049110520354e226,
            1.4230760027394052e45,
            1.417159847213267e218,
            1.4230760027394052e45,
            -3.7222049110520354e226,
            1.417159847213267e218,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.7222049110520354e226,
            1.4230760027394052e45,
            1.417159847213267e218,
            -3.7222049110520354e226,
            1.4230760027394052e45,
            1.4171598472132672e218,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.7222049110520354e226,
            1.4230760027394052e45,
            1.417159847213267e218,
            -1.3075542827597374e-89,
            -0.006289569953217738,
            3.867422662168544e249,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            3.3202874232044724e260,
            -2.137354858894597e-145,
            -2.7487296310314277e166,
            -2.137354858894597e-145,
            3.3202874232044724e260,
            -2.7487296310314277e166,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.3202874232044724e260,
            -2.137354858894597e-145,
            -2.7487296310314277e166,
            3.3202874232044724e260,
            -2.137354858894597e-145,
            -2.7487296310314274e166,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.3202874232044724e260,
            -2.137354858894597e-145,
            -2.7487296310314277e166,
            4.1192947962866636e211,
            -7.495592533461601e-22,
            1.1916306899931935e-125,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.276760439602136e-161,
            -17103247191.190645,
            -1.1051528014102167e162,
            -17103247191.190645,
            1.276760439602136e-161,
            -1.1051528014102167e162,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.276760439602136e-161,
            -17103247191.190645,
            -1.1051528014102167e162,
            1.276760439602136e-161,
            -17103247191.190645,
            -1.1051528014102165e162,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.276760439602136e-161,
            -17103247191.190645,
            -1.1051528014102167e162,
            2.3004332632152956e-50,
            1.2139354448476596e221,
            -6.927483380962327e190,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -4.2368156315322574e254,
            -9.89028903381481e61,
            -2.049066604903586e19,
            -9.89028903381481e61,
            -4.2368156315322574e254,
            -2.049066604903586e19,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.2368156315322574e254,
            -9.89028903381481e61,
            -2.049066604903586e19,
            -4.2368156315322574e254,
            -9.89028903381481e61,
            -2.0490666049035858e19,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.2368156315322574e254,
            -9.89028903381481e61,
            -2.049066604903586e19,
            -3.1946682066286106e153,
            0.10890007556591913,
            -1.2316302706874663e-10,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            9.60832587845573e206,
            -9.098861572476524e177,
            1.360526645712692e297,
            -9.098861572476524e177,
            9.60832587845573e206,
            1.360526645712692e297,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            9.60832587845573e206,
            -9.098861572476524e177,
            1.360526645712692e297,
            9.60832587845573e206,
            -9.098861572476524e177,
            1.3605266457126923e297,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            9.60832587845573e206,
            -9.098861572476524e177,
            1.360526645712692e297,
            1.9128768261829484e-84,
            -4.016859094727923e-44,
            -1.1192768722203897e104,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.995158227471702e286,
            -1.1811577769604044e-49,
            1.477487627777101e296,
            -1.1811577769604044e-49,
            1.995158227471702e286,
            1.477487627777101e296,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.995158227471702e286,
            -1.1811577769604044e-49,
            1.477487627777101e296,
            1.995158227471702e286,
            -1.1811577769604044e-49,
            1.4774876277771012e296,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.995158227471702e286,
            -1.1811577769604044e-49,
            1.477487627777101e296,
            -2.6528726469336733e249,
            -1.6687381283486097e197,
            -1.3525278231387578e-118,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -5.740904347399357e-102,
            1.3335688940234817e-209,
            -2.163334153277252e63,
            1.3335688940234817e-209,
            -5.740904347399357e-102,
            -2.163334153277252e63,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.740904347399357e-102,
            1.3335688940234817e-209,
            -2.163334153277252e63,
            -5.740904347399357e-102,
            1.3335688940234817e-209,
            -2.1633341532772517e63,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.740904347399357e-102,
            1.3335688940234817e-209,
            -2.163334153277252e63,
            -8.430781935630133e53,
            6.533453907442751e-18,
            -2.7007009647596288e-191,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -9.218226714280297e-126,
            -2.9433527297146365e-59,
            -2.9873396678472646e17,
            -2.9433527297146365e-59,
            -9.218226714280297e-126,
            -2.9873396678472646e17,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -9.218226714280297e-126,
            -2.9433527297146365e-59,
            -2.9873396678472646e17,
            -9.218226714280297e-126,
            -2.9433527297146365e-59,
            -2.987339667847264e17,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -9.218226714280297e-126,
            -2.9433527297146365e-59,
            -2.9873396678472646e17,
            -7.585323201423443e-222,
            -5.044170904793721e185,
            -7.1923334535119395e-174,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            5.172922199233939e133,
            -6.95195453145264e-222,
            -2.5904444946400522e-132,
            -6.95195453145264e-222,
            5.172922199233939e133,
            -2.5904444946400522e-132,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            5.172922199233939e133,
            -6.95195453145264e-222,
            -2.5904444946400522e-132,
            5.172922199233939e133,
            -6.95195453145264e-222,
            -2.590444494640052e-132,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            5.172922199233939e133,
            -6.95195453145264e-222,
            -2.5904444946400522e-132,
            5.636233313770184e-258,
            -4.651467077113574e66,
            -2.3221032855611395e-252,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            8.180931253674436e-110,
            -1.1067950353833952e-84,
            -8.648505714267587e-261,
            -1.1067950353833952e-84,
            8.180931253674436e-110,
            -8.648505714267587e-261,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            8.180931253674436e-110,
            -1.1067950353833952e-84,
            -8.648505714267587e-261,
            8.180931253674436e-110,
            -1.1067950353833952e-84,
            -8.648505714267586e-261,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            8.180931253674436e-110,
            -1.1067950353833952e-84,
            -8.648505714267587e-261,
            1.8901281312016707e260,
            5.584387246721977e-87,
            4.1835211222108395e189,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -3.575033481507262e56,
            7.728943770793632e280,
            -3.0545971402836486e-292,
            7.728943770793632e280,
            -3.575033481507262e56,
            -3.0545971402836486e-292,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.575033481507262e56,
            7.728943770793632e280,
            -3.0545971402836486e-292,
            -3.575033481507262e56,
            7.728943770793632e280,
            -3.054597140283648e-292,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.575033481507262e56,
            7.728943770793632e280,
            -3.0545971402836486e-292,
            4.80384424181484e275,
            6.9514823500263645e62,
            -1.3736960190625664e-11,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.2349635399030374e-229,
            2.928208094196702e26,
            6.748058469883029e89,
            2.928208094196702e26,
            -2.2349635399030374e-229,
            6.748058469883029e89,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.2349635399030374e-229,
            2.928208094196702e26,
            6.748058469883029e89,
            -2.2349635399030374e-229,
            2.928208094196702e26,
            6.74805846988303e89,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.2349635399030374e-229,
            2.928208094196702e26,
            6.748058469883029e89,
            -1.3622188809348105e-240,
            -4.723848952204956e83,
            -1.1989659265338889e41,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            7.502533694140184e-219,
            -4.746432830021807e-285,
            0.021234965648421495,
            -4.746432830021807e-285,
            7.502533694140184e-219,
            0.021234965648421495,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            7.502533694140184e-219,
            -4.746432830021807e-285,
            0.021234965648421495,
            7.502533694140184e-219,
            -4.746432830021807e-285,
            0.0212349656484215,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            7.502533694140184e-219,
            -4.746432830021807e-285,
            0.021234965648421495,
            4.433446010222447e248,
            7.296226435714614e70,
            -6.235681113620067e-165,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            4.0070347736387268e-112,
            1.5786329874335168e295,
            2.4726833584947463e-18,
            1.5786329874335168e295,
            4.0070347736387268e-112,
            2.4726833584947463e-18,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.0070347736387268e-112,
            1.5786329874335168e295,
            2.4726833584947463e-18,
            4.0070347736387268e-112,
            1.5786329874335168e295,
            2.4726833584947466e-18,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.0070347736387268e-112,
            1.5786329874335168e295,
            2.4726833584947463e-18,
            -2.881195520517952e-30,
            7.402082443450691e-86,
            -1.0194721006146956e-235,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            4.032677161334532e-133,
            -2.0442138433224203e297,
            -3.600494368813949e-219,
            -2.0442138433224203e297,
            4.032677161334532e-133,
            -3.600494368813949e-219,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.032677161334532e-133,
            -2.0442138433224203e297,
            -3.600494368813949e-219,
            4.032677161334532e-133,
            -2.0442138433224203e297,
            -3.6004943688139486e-219,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.032677161334532e-133,
            -2.0442138433224203e297,
            -3.600494368813949e-219,
            -5.2569481276551065e-188,
            -2.429811279289198e-33,
            -5.21897167344613e33,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.2369387755667837e-23,
            -1.0749219502919108e-214,
            -1.1961687180368602e115,
            -1.0749219502919108e-214,
            1.2369387755667837e-23,
            -1.1961687180368602e115,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.2369387755667837e-23,
            -1.0749219502919108e-214,
            -1.1961687180368602e115,
            1.2369387755667837e-23,
            -1.0749219502919108e-214,
            -1.19616871803686e115,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.2369387755667837e-23,
            -1.0749219502919108e-214,
            -1.1961687180368602e115,
            -4.515476271153835e102,
            4.1199173133967016e-188,
            -3.9922208278451e-291,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.3982924269275674e-79,
            -1.4168493838185497e-206,
            -3.788536157901379e-252,
            -1.4168493838185497e-206,
            -1.3982924269275674e-79,
            -3.788536157901379e-252,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.3982924269275674e-79,
            -1.4168493838185497e-206,
            -3.788536157901379e-252,
            -1.3982924269275674e-79,
            -1.4168493838185497e-206,
            -3.7885361579013787e-252,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.3982924269275674e-79,
            -1.4168493838185497e-206,
            -3.788536157901379e-252,
            -1.5302133594642448e60,
            -4.92750622419518e-288,
            9.725240147708163e296,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.486775064047052e-124,
            -1.1447433143080799e272,
            5.29848945205633e-237,
            -1.1447433143080799e272,
            -1.486775064047052e-124,
            5.29848945205633e-237,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.486775064047052e-124,
            -1.1447433143080799e272,
            5.29848945205633e-237,
            -1.486775064047052e-124,
            -1.1447433143080799e272,
            5.298489452056331e-237,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.486775064047052e-124,
            -1.1447433143080799e272,
            5.29848945205633e-237,
            3.430853218596205e193,
            -1.0314783112767795e191,
            -8.462062241704857e221,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.5871015501187066e-303,
            -1.431323925292803e293,
            1.1474189482869174e203,
            -1.431323925292803e293,
            1.5871015501187066e-303,
            1.1474189482869174e203,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.5871015501187066e-303,
            -1.431323925292803e293,
            1.1474189482869174e203,
            1.5871015501187066e-303,
            -1.431323925292803e293,
            1.1474189482869176e203,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.5871015501187066e-303,
            -1.431323925292803e293,
            1.1474189482869174e203,
            7014944690275419.0,
            -2.3288552428955504e201,
            -3.825213978976907e123,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            5.863021842026156e-264,
            9.370589468366017e-12,
            6.661452695332031e105,
            9.370589468366017e-12,
            5.863021842026156e-264,
            6.661452695332031e105,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            5.863021842026156e-264,
            9.370589468366017e-12,
            6.661452695332031e105,
            5.863021842026156e-264,
            9.370589468366017e-12,
            6.661452695332032e105,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            5.863021842026156e-264,
            9.370589468366017e-12,
            6.661452695332031e105,
            2.9017312406584316e124,
            7.327530498976963e-180,
            -7.430748771167766e199,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.6074300835860002e-179,
            3.2405358355574113e189,
            -1.6164353127122336e-118,
            3.2405358355574113e189,
            1.6074300835860002e-179,
            -1.6164353127122336e-118,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.6074300835860002e-179,
            3.2405358355574113e189,
            -1.6164353127122336e-118,
            1.6074300835860002e-179,
            3.2405358355574113e189,
            -1.6164353127122333e-118,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.6074300835860002e-179,
            3.2405358355574113e189,
            -1.6164353127122336e-118,
            1.5195231955700175e-217,
            2.612076603022614e218,
            -5.7025606065249975e189,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.4003018727546066e-244,
            -7.528625046984423e27,
            -1.6141378495375604e-90,
            -7.528625046984423e27,
            1.4003018727546066e-244,
            -1.6141378495375604e-90,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.4003018727546066e-244,
            -7.528625046984423e27,
            -1.6141378495375604e-90,
            1.4003018727546066e-244,
            -7.528625046984423e27,
            -1.6141378495375602e-90,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.4003018727546066e-244,
            -7.528625046984423e27,
            -1.6141378495375604e-90,
            5.1374910887669405e-294,
            2.0255202316105795e-72,
            -2.853035264623543e125,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.4569863166052534e216,
            1.106952710279773e207,
            -173448606149736.94,
            1.106952710279773e207,
            2.4569863166052534e216,
            -173448606149736.94,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.4569863166052534e216,
            1.106952710279773e207,
            -173448606149736.94,
            2.4569863166052534e216,
            1.106952710279773e207,
            -173448606149736.9,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.4569863166052534e216,
            1.106952710279773e207,
            -173448606149736.94,
            2.949933298853009e-271,
            4.8188628618341914e17,
            6.907961965327869e-295,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -5.424862194232616e-292,
            -6.100238652516135e131,
            -1.9800215683368243e-15,
            -6.100238652516135e131,
            -5.424862194232616e-292,
            -1.9800215683368243e-15,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.424862194232616e-292,
            -6.100238652516135e131,
            -1.9800215683368243e-15,
            -5.424862194232616e-292,
            -6.100238652516135e131,
            -1.980021568336824e-15,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.424862194232616e-292,
            -6.100238652516135e131,
            -1.9800215683368243e-15,
            1.2747317173027747e-249,
            2.372490452342956e-212,
            1.1991345303792797e257,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -8.177751059302314e-69,
            -2.946959999561113e-135,
            1.1140879446363027e-219,
            -2.946959999561113e-135,
            -8.177751059302314e-69,
            1.1140879446363027e-219,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -8.177751059302314e-69,
            -2.946959999561113e-135,
            1.1140879446363027e-219,
            -8.177751059302314e-69,
            -2.946959999561113e-135,
            1.1140879446363028e-219,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -8.177751059302314e-69,
            -2.946959999561113e-135,
            1.1140879446363027e-219,
            -3.762491405845269e240,
            4.1778832390259246e-256,
            6.015083356298315e-267,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -7.728282842871731e-18,
            -3.392582668737837e-176,
            -4.32209949656112e166,
            -3.392582668737837e-176,
            -7.728282842871731e-18,
            -4.32209949656112e166,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -7.728282842871731e-18,
            -3.392582668737837e-176,
            -4.32209949656112e166,
            -7.728282842871731e-18,
            -3.392582668737837e-176,
            -4.322099496561119e166,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -7.728282842871731e-18,
            -3.392582668737837e-176,
            -4.32209949656112e166,
            -6.410492996162867e-110,
            1.6829719626710983e141,
            -1.227848178621817e-197,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -9.241331612145138e-72,
            2.728203737199611e-116,
            6.585495714637631e79,
            2.728203737199611e-116,
            -9.241331612145138e-72,
            6.585495714637631e79,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -9.241331612145138e-72,
            2.728203737199611e-116,
            6.585495714637631e79,
            -9.241331612145138e-72,
            2.728203737199611e-116,
            6.585495714637632e79,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -9.241331612145138e-72,
            2.728203737199611e-116,
            6.585495714637631e79,
            5.972180479822925e-178,
            1.059772134660744e-85,
            -4.260457445075002e-213,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            333028.96039217274,
            2.8537509616196655e-148,
            2.1441921514562938e-252,
            2.8537509616196655e-148,
            333028.96039217274,
            2.1441921514562938e-252,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            333028.96039217274,
            2.8537509616196655e-148,
            2.1441921514562938e-252,
            333028.96039217274,
            2.8537509616196655e-148,
            2.144192151456294e-252,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            333028.96039217274,
            2.8537509616196655e-148,
            2.1441921514562938e-252,
            -5.917864965500634e237,
            -2.5318829053148113e-84,
            -7.333338075910045e22,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.1026072694089555e74,
            7.531021643171891e56,
            -9.435618050227257e41,
            7.531021643171891e56,
            1.1026072694089555e74,
            -9.435618050227257e41,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.1026072694089555e74,
            7.531021643171891e56,
            -9.435618050227257e41,
            1.1026072694089555e74,
            7.531021643171891e56,
            -9.435618050227255e41,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.1026072694089555e74,
            7.531021643171891e56,
            -9.435618050227257e41,
            -2.8711982465332185e-249,
            1.8049296218112136e-127,
            1.6926991487846068e158,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.0197771142258952e-177,
            6.2334555038967195e34,
            -1.7344202379400041e-100,
            6.2334555038967195e34,
            1.0197771142258952e-177,
            -1.7344202379400041e-100,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.0197771142258952e-177,
            6.2334555038967195e34,
            -1.7344202379400041e-100,
            1.0197771142258952e-177,
            6.2334555038967195e34,
            -1.734420237940004e-100,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.0197771142258952e-177,
            6.2334555038967195e34,
            -1.7344202379400041e-100,
            4.356187873824416e-45,
            3.8406318900986395e110,
            -1.8620829077908406e180,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.2302222959878149e-15,
            -6.882039879112713e-296,
            9.356666375590293e-225,
            -6.882039879112713e-296,
            -1.2302222959878149e-15,
            9.356666375590293e-225,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.2302222959878149e-15,
            -6.882039879112713e-296,
            9.356666375590293e-225,
            -1.2302222959878149e-15,
            -6.882039879112713e-296,
            9.356666375590295e-225,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.2302222959878149e-15,
            -6.882039879112713e-296,
            9.356666375590293e-225,
            1.532428641837249e-252,
            -5.885286863464065e-251,
            -5.867133027552897e-253,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -3.857917708309726e-156,
            8.119505718168367e169,
            3.3907395099457756e285,
            8.119505718168367e169,
            -3.857917708309726e-156,
            3.3907395099457756e285,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.857917708309726e-156,
            8.119505718168367e169,
            3.3907395099457756e285,
            -3.857917708309726e-156,
            8.119505718168367e169,
            3.390739509945776e285,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.857917708309726e-156,
            8.119505718168367e169,
            3.3907395099457756e285,
            5.151169875220322e275,
            -1.6163531751877233e-268,
            9.073634251730674e227,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.3942652650843544e226,
            5.0755348499684685e252,
            -2.757222979955941e-27,
            5.0755348499684685e252,
            1.3942652650843544e226,
            -2.757222979955941e-27,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.3942652650843544e226,
            5.0755348499684685e252,
            -2.757222979955941e-27,
            1.3942652650843544e226,
            5.0755348499684685e252,
            -2.7572229799559405e-27,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.3942652650843544e226,
            5.0755348499684685e252,
            -2.757222979955941e-27,
            1.2269240605947203e-61,
            2.497687322620002e264,
            6.113813500643253e172,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.9470819609250423e212,
            5.053935410726332e-129,
            -9.162797231083447e215,
            5.053935410726332e-129,
            -2.9470819609250423e212,
            -9.162797231083447e215,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.9470819609250423e212,
            5.053935410726332e-129,
            -9.162797231083447e215,
            -2.9470819609250423e212,
            5.053935410726332e-129,
            -9.162797231083446e215,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.9470819609250423e212,
            5.053935410726332e-129,
            -9.162797231083447e215,
            5.547745279324474e267,
            9.01171746575721e-84,
            -7.908900940531014e-263,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.6159724919427717e277,
            4.884875777042647e-62,
            5.437743330543872e-108,
            4.884875777042647e-62,
            -1.6159724919427717e277,
            5.437743330543872e-108,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.6159724919427717e277,
            4.884875777042647e-62,
            5.437743330543872e-108,
            -1.6159724919427717e277,
            4.884875777042647e-62,
            5.437743330543873e-108,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.6159724919427717e277,
            4.884875777042647e-62,
            5.437743330543872e-108,
            3.859630945515211e-303,
            -2.1044967377938075e-66,
            -4.241569071377456e-180,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            6.163529083694098e268,
            1.804898643355962e-182,
            2.206290335890217e-175,
            1.804898643355962e-182,
            6.163529083694098e268,
            2.206290335890217e-175,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            6.163529083694098e268,
            1.804898643355962e-182,
            2.206290335890217e-175,
            6.163529083694098e268,
            1.804898643355962e-182,
            2.2062903358902173e-175,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            6.163529083694098e268,
            1.804898643355962e-182,
            2.206290335890217e-175,
            2.0728828655948095e-38,
            2.3732494825536838e-181,
            -2.751149755437825e-225,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.5837645974581904e-273,
            -2.0217009504819562e108,
            1.1329218507682021e125,
            -2.0217009504819562e108,
            1.5837645974581904e-273,
            1.1329218507682021e125,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.5837645974581904e-273,
            -2.0217009504819562e108,
            1.1329218507682021e125,
            1.5837645974581904e-273,
            -2.0217009504819562e108,
            1.1329218507682023e125,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.5837645974581904e-273,
            -2.0217009504819562e108,
            1.1329218507682021e125,
            -7.729861887645892e174,
            -1.845719890347489e151,
            -1.3997525396730795e-174,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.423570004944958e-119,
            6.902672878192029e-240,
            1.3841108873782473e-106,
            6.902672878192029e-240,
            -2.423570004944958e-119,
            1.3841108873782473e-106,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.423570004944958e-119,
            6.902672878192029e-240,
            1.3841108873782473e-106,
            -2.423570004944958e-119,
            6.902672878192029e-240,
            1.3841108873782475e-106,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.423570004944958e-119,
            6.902672878192029e-240,
            1.3841108873782473e-106,
            -4.46630675905761e153,
            5.263232638493932e-266,
            1.0546788485716151e77,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            4.001174608749752e107,
            8.473227930993724e-302,
            -5.140103470518936e66,
            8.473227930993724e-302,
            4.001174608749752e107,
            -5.140103470518936e66,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.001174608749752e107,
            8.473227930993724e-302,
            -5.140103470518936e66,
            4.001174608749752e107,
            8.473227930993724e-302,
            -5.140103470518935e66,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.001174608749752e107,
            8.473227930993724e-302,
            -5.140103470518936e66,
            1.4040749623967037e253,
            5.28265125138389e159,
            -9.721053465810611e-296,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.573321045728246e179,
            5.635756948198503e127,
            -7.936934561960748e-124,
            5.635756948198503e127,
            -1.573321045728246e179,
            -7.936934561960748e-124,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.573321045728246e179,
            5.635756948198503e127,
            -7.936934561960748e-124,
            -1.573321045728246e179,
            5.635756948198503e127,
            -7.936934561960747e-124,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.573321045728246e179,
            5.635756948198503e127,
            -7.936934561960748e-124,
            7.993856855226715e-277,
            2.144658146565871e159,
            -2.803307319573604e-42,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -3.623287345861386e132,
            7.758543903520837e105,
            -1.5532134563757565e66,
            7.758543903520837e105,
            -3.623287345861386e132,
            -1.5532134563757565e66,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.623287345861386e132,
            7.758543903520837e105,
            -1.5532134563757565e66,
            -3.623287345861386e132,
            7.758543903520837e105,
            -1.5532134563757563e66,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.623287345861386e132,
            7.758543903520837e105,
            -1.5532134563757565e66,
            1.5612754137562281e22,
            -6.947080709141274e195,
            -1.3235303827414387e-80,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.7774835987037616e-49,
            5.443822361362893e31,
            7.675976492362184e-23,
            5.443822361362893e31,
            -1.7774835987037616e-49,
            7.675976492362184e-23,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.7774835987037616e-49,
            5.443822361362893e31,
            7.675976492362184e-23,
            -1.7774835987037616e-49,
            5.443822361362893e31,
            7.675976492362185e-23,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.7774835987037616e-49,
            5.443822361362893e31,
            7.675976492362184e-23,
            -8.751042091183915e161,
            2.978784600414793e111,
            1.7393476229891364e-81,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -6.5223813093884036e63,
            2.466953560197636e-289,
            5.848889468871502e298,
            2.466953560197636e-289,
            -6.5223813093884036e63,
            5.848889468871502e298,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -6.5223813093884036e63,
            2.466953560197636e-289,
            5.848889468871502e298,
            -6.5223813093884036e63,
            2.466953560197636e-289,
            5.848889468871503e298,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -6.5223813093884036e63,
            2.466953560197636e-289,
            5.848889468871502e298,
            -3.6234894777490253e-143,
            -1.621537715148551e-89,
            -5.389975814060857e174,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -7.63109723221242e133,
            -3.984008338774073e-143,
            1.4074542640905873e20,
            -3.984008338774073e-143,
            -7.63109723221242e133,
            1.4074542640905873e20,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -7.63109723221242e133,
            -3.984008338774073e-143,
            1.4074542640905873e20,
            -7.63109723221242e133,
            -3.984008338774073e-143,
            1.4074542640905875e20,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -7.63109723221242e133,
            -3.984008338774073e-143,
            1.4074542640905873e20,
            -2.415606490545924e67,
            -7.364620125500195e165,
            1.2758317729968826e-225,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -5.326614032652678e-48,
            3.5948921607167453e301,
            3.178833190665299e283,
            3.5948921607167453e301,
            -5.326614032652678e-48,
            3.178833190665299e283,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.326614032652678e-48,
            3.5948921607167453e301,
            3.178833190665299e283,
            -5.326614032652678e-48,
            3.5948921607167453e301,
            3.1788331906652996e283,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.326614032652678e-48,
            3.5948921607167453e301,
            3.178833190665299e283,
            1.1179549354686235e67,
            6.239618313137613e-81,
            5.827362530235128e-268,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.5674135179972632e235,
            7.908728189072395e-265,
            -2.444633997553429e154,
            7.908728189072395e-265,
            -1.5674135179972632e235,
            -2.444633997553429e154,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.5674135179972632e235,
            7.908728189072395e-265,
            -2.444633997553429e154,
            -1.5674135179972632e235,
            7.908728189072395e-265,
            -2.4446339975534288e154,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.5674135179972632e235,
            7.908728189072395e-265,
            -2.444633997553429e154,
            -3.0042175029416744e292,
            5.873108817663645e-28,
            1.6684952539102054e233,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -3.834278215137693e89,
            -7.249771730195817e87,
            -1.8707447404863804e-130,
            -7.249771730195817e87,
            -3.834278215137693e89,
            -1.8707447404863804e-130,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.834278215137693e89,
            -7.249771730195817e87,
            -1.8707447404863804e-130,
            -3.834278215137693e89,
            -7.249771730195817e87,
            -1.87074474048638e-130,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.834278215137693e89,
            -7.249771730195817e87,
            -1.8707447404863804e-130,
            -9.557185417297139e275,
            -1.476886584837285e-62,
            1.710104863490732e145,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.397711898027009e171,
            -2.6032080006066955e22,
            1.3081126552615148e-119,
            -2.6032080006066955e22,
            1.397711898027009e171,
            1.3081126552615148e-119,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.397711898027009e171,
            -2.6032080006066955e22,
            1.3081126552615148e-119,
            1.397711898027009e171,
            -2.6032080006066955e22,
            1.308112655261515e-119,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.397711898027009e171,
            -2.6032080006066955e22,
            1.3081126552615148e-119,
            -5.1266262404667515e-121,
            -3.090000802453968e180,
            2.3544145165881358e266,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.2618215291895671e135,
            -2.2647451686033643e-138,
            -1.1658317670865835e-300,
            -2.2647451686033643e-138,
            -1.2618215291895671e135,
            -1.1658317670865835e-300,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.2618215291895671e135,
            -2.2647451686033643e-138,
            -1.1658317670865835e-300,
            -1.2618215291895671e135,
            -2.2647451686033643e-138,
            -1.1658317670865834e-300,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.2618215291895671e135,
            -2.2647451686033643e-138,
            -1.1658317670865835e-300,
            3.590336954104694e-299,
            -8.378285133406185e36,
            -5.3784767778434714e-132,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.6844474178423862e-35,
            -5.67287206185664e202,
            -7.650809816680889e125,
            -5.67287206185664e202,
            -1.6844474178423862e-35,
            -7.650809816680889e125,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.6844474178423862e-35,
            -5.67287206185664e202,
            -7.650809816680889e125,
            -1.6844474178423862e-35,
            -5.67287206185664e202,
            -7.650809816680888e125,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.6844474178423862e-35,
            -5.67287206185664e202,
            -7.650809816680889e125,
            -3.370555236734386e246,
            -2.0891416186790516e-303,
            9.850035893511628e21,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.2805191653220934e-182,
            -6.80122803962382e-293,
            -3.038027431361132e-179,
            -6.80122803962382e-293,
            1.2805191653220934e-182,
            -3.038027431361132e-179,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.2805191653220934e-182,
            -6.80122803962382e-293,
            -3.038027431361132e-179,
            1.2805191653220934e-182,
            -6.80122803962382e-293,
            -3.0380274313611317e-179,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.2805191653220934e-182,
            -6.80122803962382e-293,
            -3.038027431361132e-179,
            3.0735684339978496e-31,
            6.955531467582423e218,
            -4.2722199788512154e302,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.7325776933170836e-28,
            -4.389133760616678e-130,
            1.9483221110337238e-147,
            -4.389133760616678e-130,
            1.7325776933170836e-28,
            1.9483221110337238e-147,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.7325776933170836e-28,
            -4.389133760616678e-130,
            1.9483221110337238e-147,
            1.7325776933170836e-28,
            -4.389133760616678e-130,
            1.948322111033724e-147,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.7325776933170836e-28,
            -4.389133760616678e-130,
            1.9483221110337238e-147,
            -9.228547671509776e-90,
            -2.6943059750754026e-294,
            -7.83481686735075e164,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.8014807107172198e-180,
            2.0821017648676092e237,
            -9.66444868236126e160,
            2.0821017648676092e237,
            1.8014807107172198e-180,
            -9.66444868236126e160,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.8014807107172198e-180,
            2.0821017648676092e237,
            -9.66444868236126e160,
            1.8014807107172198e-180,
            2.0821017648676092e237,
            -9.664448682361259e160,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.8014807107172198e-180,
            2.0821017648676092e237,
            -9.66444868236126e160,
            -2.930698682043132e268,
            2.678216317333592e112,
            -1.4856707982032055e-282,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.8433401193406037e-291,
            1.4608634119571723e49,
            1.740435601157065e257,
            1.4608634119571723e49,
            2.8433401193406037e-291,
            1.740435601157065e257,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.8433401193406037e-291,
            1.4608634119571723e49,
            1.740435601157065e257,
            2.8433401193406037e-291,
            1.4608634119571723e49,
            1.7404356011570653e257,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.8433401193406037e-291,
            1.4608634119571723e49,
            1.740435601157065e257,
            -3.1940267428121417e270,
            -3.6649819197075917e87,
            -2.2303107823104826e-268,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -3.306124773247596e271,
            -3.396352575684101e-292,
            -1.138670197035278e63,
            -3.396352575684101e-292,
            -3.306124773247596e271,
            -1.138670197035278e63,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.306124773247596e271,
            -3.396352575684101e-292,
            -1.138670197035278e63,
            -3.306124773247596e271,
            -3.396352575684101e-292,
            -1.1386701970352778e63,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.306124773247596e271,
            -3.396352575684101e-292,
            -1.138670197035278e63,
            -1.2886686204031858e281,
            -1.1554238018307372e149,
            1.7576915530723287e-307,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -5.107423265232136e20,
            1.667179450243983e-195,
            -3.8364691223685383e99,
            1.667179450243983e-195,
            -5.107423265232136e20,
            -3.8364691223685383e99,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.107423265232136e20,
            1.667179450243983e-195,
            -3.8364691223685383e99,
            -5.107423265232136e20,
            1.667179450243983e-195,
            -3.836469122368538e99,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.107423265232136e20,
            1.667179450243983e-195,
            -3.8364691223685383e99,
            1.7113481448919384e-303,
            1.7473353400534596e28,
            2.3437756350729527e-213,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -4.617237694425093e286,
            1.3686927615048185e-264,
            -1.6065720078743852e182,
            1.3686927615048185e-264,
            -4.617237694425093e286,
            -1.6065720078743852e182,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.617237694425093e286,
            1.3686927615048185e-264,
            -1.6065720078743852e182,
            -4.617237694425093e286,
            1.3686927615048185e-264,
            -1.606572007874385e182,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.617237694425093e286,
            1.3686927615048185e-264,
            -1.6065720078743852e182,
            -6.0346746721346286e-136,
            46216.62527462489,
            -1.5539605133504104e-203,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.3126014835405753e289,
            6.463669966348103e-56,
            5.784320785951127e266,
            6.463669966348103e-56,
            2.3126014835405753e289,
            5.784320785951127e266,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.3126014835405753e289,
            6.463669966348103e-56,
            5.784320785951127e266,
            2.3126014835405753e289,
            6.463669966348103e-56,
            5.784320785951128e266,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.3126014835405753e289,
            6.463669966348103e-56,
            5.784320785951127e266,
            -2.647814876348271e62,
            1.7333593062622812e235,
            1.0061867108208591e294,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -4.4340128040037395e-204,
            3.182298901487361e-198,
            -1.2700300339478583e-241,
            3.182298901487361e-198,
            -4.4340128040037395e-204,
            -1.2700300339478583e-241,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.4340128040037395e-204,
            3.182298901487361e-198,
            -1.2700300339478583e-241,
            -4.4340128040037395e-204,
            3.182298901487361e-198,
            -1.2700300339478582e-241,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.4340128040037395e-204,
            3.182298901487361e-198,
            -1.2700300339478583e-241,
            8.724837713060613e-106,
            3.636988840442564e172,
            3.254281410073579e169,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.456740235154418e192,
            -3.986471189968515e-5,
            1.6791869760481472e248,
            -3.986471189968515e-5,
            -1.456740235154418e192,
            1.6791869760481472e248,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.456740235154418e192,
            -3.986471189968515e-5,
            1.6791869760481472e248,
            -1.456740235154418e192,
            -3.986471189968515e-5,
            1.6791869760481475e248,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.456740235154418e192,
            -3.986471189968515e-5,
            1.6791869760481472e248,
            4.7333719078471206e250,
            1.35734005762794e69,
            -1.7412090994920946e139,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.8176333831776416e-219,
            -1.648987083999003e106,
            2.9585367164394054e91,
            -1.648987083999003e106,
            -2.8176333831776416e-219,
            2.9585367164394054e91,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.8176333831776416e-219,
            -1.648987083999003e106,
            2.9585367164394054e91,
            -2.8176333831776416e-219,
            -1.648987083999003e106,
            2.9585367164394058e91,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.8176333831776416e-219,
            -1.648987083999003e106,
            2.9585367164394054e91,
            -25577993077991.074,
            -1.5263914318921887e-157,
            9.531800049237552e235,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.7165101861049343e-72,
            -1.2973577573400837e-222,
            3.959034651350486e170,
            -1.2973577573400837e-222,
            -2.7165101861049343e-72,
            3.959034651350486e170,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.7165101861049343e-72,
            -1.2973577573400837e-222,
            3.959034651350486e170,
            -2.7165101861049343e-72,
            -1.2973577573400837e-222,
            3.9590346513504866e170,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.7165101861049343e-72,
            -1.2973577573400837e-222,
            3.959034651350486e170,
            1.3072181940169464e-188,
            -4.969479465633927e-157,
            -1.5555316049879355e189,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.546487873608125e-75,
            -2.553708552793546e104,
            1.63238377986769e-45,
            -2.553708552793546e104,
            -1.546487873608125e-75,
            1.63238377986769e-45,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.546487873608125e-75,
            -2.553708552793546e104,
            1.63238377986769e-45,
            -1.546487873608125e-75,
            -2.553708552793546e104,
            1.6323837798676904e-45,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.546487873608125e-75,
            -2.553708552793546e104,
            1.63238377986769e-45,
            -1.3331729266941353e271,
            -8.647711693028033e136,
            -2.045699399176374e242,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.778417778983852e93,
            -6.127782340519228e213,
            -4.488128982964823e-144,
            -6.127782340519228e213,
            2.778417778983852e93,
            -4.488128982964823e-144,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.778417778983852e93,
            -6.127782340519228e213,
            -4.488128982964823e-144,
            2.778417778983852e93,
            -6.127782340519228e213,
            -4.4881289829648223e-144,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.778417778983852e93,
            -6.127782340519228e213,
            -4.488128982964823e-144,
            5.857187958079377e-293,
            -4.586978198512997e58,
            -1.154603768515595e-194,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.2314629428564741e81,
            -8.779061507185595e66,
            -1.6610988720491794e122,
            -8.779061507185595e66,
            -1.2314629428564741e81,
            -1.6610988720491794e122,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.2314629428564741e81,
            -8.779061507185595e66,
            -1.6610988720491794e122,
            -1.2314629428564741e81,
            -8.779061507185595e66,
            -1.661098872049179e122,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.2314629428564741e81,
            -8.779061507185595e66,
            -1.6610988720491794e122,
            -4.7210367869475804e-307,
            -1.1726239814842844e-135,
            -1.2044286382304751e-64,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -4.2763786867436054e54,
            -1.6154548539539912e274,
            4.2676792260875915e-55,
            -1.6154548539539912e274,
            -4.2763786867436054e54,
            4.2676792260875915e-55,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.2763786867436054e54,
            -1.6154548539539912e274,
            4.2676792260875915e-55,
            -4.2763786867436054e54,
            -1.6154548539539912e274,
            4.267679226087592e-55,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.2763786867436054e54,
            -1.6154548539539912e274,
            4.2676792260875915e-55,
            2.2882150891321121e77,
            1.961807331219798e-308,
            7.410399926333948e-98,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            305330805.3305048,
            -5.075373068033703e-11,
            3.186018634947921e61,
            -5.075373068033703e-11,
            305330805.3305048,
            3.186018634947921e61,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            305330805.3305048,
            -5.075373068033703e-11,
            3.186018634947921e61,
            305330805.3305048,
            -5.075373068033703e-11,
            3.186018634947922e61,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            305330805.3305048,
            -5.075373068033703e-11,
            3.186018634947921e61,
            -237974.8778595236,
            7.171858816095017e-269,
            -5.28399878967786e-252,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -3.615181147349277e-28,
            6.526651227481272e43,
            7.780216448230682e113,
            6.526651227481272e43,
            -3.615181147349277e-28,
            7.780216448230682e113,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.615181147349277e-28,
            6.526651227481272e43,
            7.780216448230682e113,
            -3.615181147349277e-28,
            6.526651227481272e43,
            7.780216448230683e113,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.615181147349277e-28,
            6.526651227481272e43,
            7.780216448230682e113,
            2.976759273544892e37,
            -1.96218155245683e-286,
            3.6978834783896787e189,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -8.873142557538622e-27,
            4.909921341102075e69,
            -5.04301435506816e51,
            4.909921341102075e69,
            -8.873142557538622e-27,
            -5.04301435506816e51,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -8.873142557538622e-27,
            4.909921341102075e69,
            -5.04301435506816e51,
            -8.873142557538622e-27,
            4.909921341102075e69,
            -5.043014355068159e51,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -8.873142557538622e-27,
            4.909921341102075e69,
            -5.04301435506816e51,
            -3.987119745577769e113,
            4.222135741312957e203,
            -7.729252544887505e-168,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.9967454008824414e-66,
            1.6279290348907835e-232,
            6.913432764953629e192,
            1.6279290348907835e-232,
            -1.9967454008824414e-66,
            6.913432764953629e192,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.9967454008824414e-66,
            1.6279290348907835e-232,
            6.913432764953629e192,
            -1.9967454008824414e-66,
            1.6279290348907835e-232,
            6.91343276495363e192,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.9967454008824414e-66,
            1.6279290348907835e-232,
            6.913432764953629e192,
            4.767340971844925e125,
            -1.3634132596500567e-95,
            1.968005848290659e222,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.852420840290531e-60,
            6.997940054866342e287,
            -2.2070664032363228e76,
            6.997940054866342e287,
            -2.852420840290531e-60,
            -2.2070664032363228e76,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.852420840290531e-60,
            6.997940054866342e287,
            -2.2070664032363228e76,
            -2.852420840290531e-60,
            6.997940054866342e287,
            -2.2070664032363225e76,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.852420840290531e-60,
            6.997940054866342e287,
            -2.2070664032363228e76,
            1.0234918072098078e-159,
            1.8059363683138564e175,
            1.0592133505641125e-169,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.6420149389692207e26,
            1.639428320889148e-202,
            3.0719521838223715e-81,
            1.639428320889148e-202,
            -1.6420149389692207e26,
            3.0719521838223715e-81,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.6420149389692207e26,
            1.639428320889148e-202,
            3.0719521838223715e-81,
            -1.6420149389692207e26,
            1.639428320889148e-202,
            3.071952183822372e-81,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.6420149389692207e26,
            1.639428320889148e-202,
            3.0719521838223715e-81,
            7.43683342428796e-37,
            -3.1993831904451707e-168,
            2.034867261147439e144,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -6.194713041833605e17,
            1.4039735255508039e-248,
            2.8523121279555656e-124,
            1.4039735255508039e-248,
            -6.194713041833605e17,
            2.8523121279555656e-124,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -6.194713041833605e17,
            1.4039735255508039e-248,
            2.8523121279555656e-124,
            -6.194713041833605e17,
            1.4039735255508039e-248,
            2.852312127955566e-124,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -6.194713041833605e17,
            1.4039735255508039e-248,
            2.8523121279555656e-124,
            -4.02944818410285e-133,
            -1.6183150437207763e-196,
            3.328668087446233e-181,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.364655336613882e-68,
            -8.44711552124499e62,
            1.6730943996906662e-91,
            -8.44711552124499e62,
            -1.364655336613882e-68,
            1.6730943996906662e-91,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.364655336613882e-68,
            -8.44711552124499e62,
            1.6730943996906662e-91,
            -1.364655336613882e-68,
            -8.44711552124499e62,
            1.6730943996906665e-91,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.364655336613882e-68,
            -8.44711552124499e62,
            1.6730943996906662e-91,
            -3.686730343186454e-98,
            9.062837631743353e-46,
            -4.1928323653320345e46,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.0364792702577475e-76,
            -2.3008930072680738e95,
            -6.7379122672341234e-49,
            -2.3008930072680738e95,
            2.0364792702577475e-76,
            -6.7379122672341234e-49,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.0364792702577475e-76,
            -2.3008930072680738e95,
            -6.7379122672341234e-49,
            2.0364792702577475e-76,
            -2.3008930072680738e95,
            -6.737912267234123e-49,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.0364792702577475e-76,
            -2.3008930072680738e95,
            -6.7379122672341234e-49,
            -6.728232129805242e261,
            -2.7171612436785945e42,
            -1.1695690980852669e-168,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            0.0007522769003004687,
            5.8635417661092144e168,
            -3.183789339394287e219,
            5.8635417661092144e168,
            0.0007522769003004687,
            -3.183789339394287e219,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            0.0007522769003004687,
            5.8635417661092144e168,
            -3.183789339394287e219,
            0.0007522769003004687,
            5.8635417661092144e168,
            -3.1837893393942866e219,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            0.0007522769003004687,
            5.8635417661092144e168,
            -3.183789339394287e219,
            2.0383273331353444e-40,
            -6.632740983760172e-77,
            -2.35160985642946e-197,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            3.1824498176147806e-304,
            -4.5776185223878606e57,
            -6.307168080958104e-224,
            -4.5776185223878606e57,
            3.1824498176147806e-304,
            -6.307168080958104e-224,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.1824498176147806e-304,
            -4.5776185223878606e57,
            -6.307168080958104e-224,
            3.1824498176147806e-304,
            -4.5776185223878606e57,
            -6.307168080958103e-224,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.1824498176147806e-304,
            -4.5776185223878606e57,
            -6.307168080958104e-224,
            1.311903605298269e152,
            3.599875162608375e-233,
            -9.134694880200722e-89,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.0510418120031552e-67,
            -3.0243475469089006e-79,
            -9.015612522116908e197,
            -3.0243475469089006e-79,
            1.0510418120031552e-67,
            -9.015612522116908e197,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.0510418120031552e-67,
            -3.0243475469089006e-79,
            -9.015612522116908e197,
            1.0510418120031552e-67,
            -3.0243475469089006e-79,
            -9.015612522116907e197,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.0510418120031552e-67,
            -3.0243475469089006e-79,
            -9.015612522116908e197,
            -8.231756123191366e214,
            3.165629662973141e-31,
            2.923376976076939e-88,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            3.454877827370139e276,
            -1.9436787379261107e147,
            -7336974324651.777,
            -1.9436787379261107e147,
            3.454877827370139e276,
            -7336974324651.777,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.454877827370139e276,
            -1.9436787379261107e147,
            -7336974324651.777,
            3.454877827370139e276,
            -1.9436787379261107e147,
            -7336974324651.776,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.454877827370139e276,
            -1.9436787379261107e147,
            -7336974324651.777,
            1.8738192227645508e-41,
            3.714783306662137e236,
            1.445788199635681e-251,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            4.187747988786607e-291,
            1.3594763018577451e-129,
            -2.440202862597658e78,
            1.3594763018577451e-129,
            4.187747988786607e-291,
            -2.440202862597658e78,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.187747988786607e-291,
            1.3594763018577451e-129,
            -2.440202862597658e78,
            4.187747988786607e-291,
            1.3594763018577451e-129,
            -2.4402028625976575e78,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.187747988786607e-291,
            1.3594763018577451e-129,
            -2.440202862597658e78,
            -4.340145490417329e294,
            1.955053899079928e166,
            -2.532063238600049e-42,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            3.181156276080502e118,
            2.1492864875065766e283,
            -4.251131840664631e231,
            2.1492864875065766e283,
            3.181156276080502e118,
            -4.251131840664631e231,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.181156276080502e118,
            2.1492864875065766e283,
            -4.251131840664631e231,
            3.181156276080502e118,
            2.1492864875065766e283,
            -4.2511318406646304e231,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.181156276080502e118,
            2.1492864875065766e283,
            -4.251131840664631e231,
            6.734825009406913e-27,
            5.755184176720194e-28,
            7.91421911984612e-244,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            3.4442552232271654e-290,
            1.770581202616982e173,
            -0.160750973177122,
            1.770581202616982e173,
            3.4442552232271654e-290,
            -0.160750973177122,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.4442552232271654e-290,
            1.770581202616982e173,
            -0.160750973177122,
            3.4442552232271654e-290,
            1.770581202616982e173,
            -0.16075097317712198,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.4442552232271654e-290,
            1.770581202616982e173,
            -0.160750973177122,
            -5.370597854741404e-52,
            -3.562934468759406e-166,
            3.112151714368905e92,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.2535869456028183e-46,
            1.115107797112862e76,
            -1.287986566743264e-170,
            1.115107797112862e76,
            2.2535869456028183e-46,
            -1.287986566743264e-170,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.2535869456028183e-46,
            1.115107797112862e76,
            -1.287986566743264e-170,
            2.2535869456028183e-46,
            1.115107797112862e76,
            -1.2879865667432638e-170,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.2535869456028183e-46,
            1.115107797112862e76,
            -1.287986566743264e-170,
            -1.5290994467999359e62,
            3.312334271018507e280,
            2.0992064460483347e195,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -5.0604436309718664e-191,
            5594891.7197901625,
            4.546468294596088e-287,
            5594891.7197901625,
            -5.0604436309718664e-191,
            4.546468294596088e-287,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.0604436309718664e-191,
            5594891.7197901625,
            4.546468294596088e-287,
            -5.0604436309718664e-191,
            5594891.7197901625,
            4.5464682945960884e-287,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.0604436309718664e-191,
            5594891.7197901625,
            4.546468294596088e-287,
            4.5010455150802414e-60,
            -7.586635196003136e20,
            1.7625996403806013e278,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.725220779624576e-166,
            6.818879205281082e297,
            7.378422516276971e-37,
            6.818879205281082e297,
            -1.725220779624576e-166,
            7.378422516276971e-37,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.725220779624576e-166,
            6.818879205281082e297,
            7.378422516276971e-37,
            -1.725220779624576e-166,
            6.818879205281082e297,
            7.378422516276972e-37,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.725220779624576e-166,
            6.818879205281082e297,
            7.378422516276971e-37,
            1.2623964171787087e28,
            9.601326090726818e-102,
            -1.6385844601839784e-109,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.8576230510357708e185,
            -1.997964608775496e-117,
            -2.326312290039391e-160,
            -1.997964608775496e-117,
            -1.8576230510357708e185,
            -2.326312290039391e-160,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.8576230510357708e185,
            -1.997964608775496e-117,
            -2.326312290039391e-160,
            -1.8576230510357708e185,
            -1.997964608775496e-117,
            -2.3263122900393907e-160,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.8576230510357708e185,
            -1.997964608775496e-117,
            -2.326312290039391e-160,
            -3.1789385945524386e-263,
            1.4423311155303365e-268,
            2.298026409308761e-268,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.008383915138228e-268,
            -9.361888094384058e-266,
            -1.83004276282363e19,
            -9.361888094384058e-266,
            -2.008383915138228e-268,
            -1.83004276282363e19,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.008383915138228e-268,
            -9.361888094384058e-266,
            -1.83004276282363e19,
            -2.008383915138228e-268,
            -9.361888094384058e-266,
            -1.8300427628236298e19,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.008383915138228e-268,
            -9.361888094384058e-266,
            -1.83004276282363e19,
            -1.2735933066135678e196,
            -1.4297929157392367e66,
            2.6205372292525274e-113,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.4066490042399428e-202,
            -3.0764597008766758e-49,
            4.6823235766804995e-232,
            -3.0764597008766758e-49,
            -1.4066490042399428e-202,
            4.6823235766804995e-232,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.4066490042399428e-202,
            -3.0764597008766758e-49,
            4.6823235766804995e-232,
            -1.4066490042399428e-202,
            -3.0764597008766758e-49,
            4.6823235766805e-232,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.4066490042399428e-202,
            -3.0764597008766758e-49,
            4.6823235766804995e-232,
            5.604628947420864e42,
            -3.7025850538276913e266,
            -1.8518943724051407e169,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            5.29551351924928e29,
            -1.8619280542874047e187,
            4.3545202411879655e-101,
            -1.8619280542874047e187,
            5.29551351924928e29,
            4.3545202411879655e-101,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            5.29551351924928e29,
            -1.8619280542874047e187,
            4.3545202411879655e-101,
            5.29551351924928e29,
            -1.8619280542874047e187,
            4.354520241187966e-101,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            5.29551351924928e29,
            -1.8619280542874047e187,
            4.3545202411879655e-101,
            7.175389004229475e-222,
            3.962008092291188e180,
            8.417352647837213e-116,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.2236895336375103e212,
            -6.266494740309905e-52,
            -2.1734535460658997e-214,
            -6.266494740309905e-52,
            1.2236895336375103e212,
            -2.1734535460658997e-214,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.2236895336375103e212,
            -6.266494740309905e-52,
            -2.1734535460658997e-214,
            1.2236895336375103e212,
            -6.266494740309905e-52,
            -2.1734535460658993e-214,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.2236895336375103e212,
            -6.266494740309905e-52,
            -2.1734535460658997e-214,
            -1.1438717752062861e136,
            -4.06621929251252e-70,
            2.7588800927232496e-126,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.2155785216646279e-272,
            1.4544555781812994e20,
            7.552638867667254e-140,
            1.4544555781812994e20,
            1.2155785216646279e-272,
            7.552638867667254e-140,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.2155785216646279e-272,
            1.4544555781812994e20,
            7.552638867667254e-140,
            1.2155785216646279e-272,
            1.4544555781812994e20,
            7.552638867667255e-140,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.2155785216646279e-272,
            1.4544555781812994e20,
            7.552638867667254e-140,
            -1.1380640713038349e150,
            -3.2125423487815156e108,
            -2.3590702092209736e-210,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.4522703727314336e164,
            4.297049362125286e103,
            -3.564488654780068e-227,
            4.297049362125286e103,
            2.4522703727314336e164,
            -3.564488654780068e-227,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.4522703727314336e164,
            4.297049362125286e103,
            -3.564488654780068e-227,
            2.4522703727314336e164,
            4.297049362125286e103,
            -3.5644886547800677e-227,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.4522703727314336e164,
            4.297049362125286e103,
            -3.564488654780068e-227,
            3.0354844983322637e70,
            -5.558154297734884e306,
            3.5523524484883693e291,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.5550542329226444e-5,
            1.9777602701946183e164,
            -4.910265470721912e241,
            1.9777602701946183e164,
            -1.5550542329226444e-5,
            -4.910265470721912e241,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.5550542329226444e-5,
            1.9777602701946183e164,
            -4.910265470721912e241,
            -1.5550542329226444e-5,
            1.9777602701946183e164,
            -4.910265470721911e241,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.5550542329226444e-5,
            1.9777602701946183e164,
            -4.910265470721912e241,
            -9.391845303274917e267,
            -1.1858108545514307e-121,
            -4.2577891473462086e17,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.1449129242548562e127,
            -1.7503293544923396e234,
            1.368876936475753e-249,
            -1.7503293544923396e234,
            -2.1449129242548562e127,
            1.368876936475753e-249,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.1449129242548562e127,
            -1.7503293544923396e234,
            1.368876936475753e-249,
            -2.1449129242548562e127,
            -1.7503293544923396e234,
            1.3688769364757533e-249,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.1449129242548562e127,
            -1.7503293544923396e234,
            1.368876936475753e-249,
            3.392025417473269e-248,
            2.2625766650124026e273,
            -7.003906221552952e77,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.4023223719263348e-12,
            1.221992415964643e298,
            -4.975528712891854e-45,
            1.221992415964643e298,
            1.4023223719263348e-12,
            -4.975528712891854e-45,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.4023223719263348e-12,
            1.221992415964643e298,
            -4.975528712891854e-45,
            1.4023223719263348e-12,
            1.221992415964643e298,
            -4.9755287128918534e-45,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.4023223719263348e-12,
            1.221992415964643e298,
            -4.975528712891854e-45,
            5.131360919905619e-32,
            1.2102054315704735e-158,
            -2.117550853394448e181,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            8.804897071505094e65,
            -2.0346518731625605e-194,
            -6.347439647403439e214,
            -2.0346518731625605e-194,
            8.804897071505094e65,
            -6.347439647403439e214,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            8.804897071505094e65,
            -2.0346518731625605e-194,
            -6.347439647403439e214,
            8.804897071505094e65,
            -2.0346518731625605e-194,
            -6.347439647403438e214,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            8.804897071505094e65,
            -2.0346518731625605e-194,
            -6.347439647403439e214,
            -5.417616972451397e165,
            -1.6228596480665336e101,
            -2.541866669032582e-305,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.5574012050215925e-108,
            1.8030447890447267e137,
            -2.3959905581466376e51,
            1.8030447890447267e137,
            -2.5574012050215925e-108,
            -2.3959905581466376e51,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.5574012050215925e-108,
            1.8030447890447267e137,
            -2.3959905581466376e51,
            -2.5574012050215925e-108,
            1.8030447890447267e137,
            -2.3959905581466373e51,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.5574012050215925e-108,
            1.8030447890447267e137,
            -2.3959905581466376e51,
            4.50083137643429e208,
            -6.803228519494031e-296,
            3.629046888076691e39,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.717627264832759e-277,
            1.532292866386779e-236,
            2.428880249546452e298,
            1.532292866386779e-236,
            2.717627264832759e-277,
            2.428880249546452e298,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.717627264832759e-277,
            1.532292866386779e-236,
            2.428880249546452e298,
            2.717627264832759e-277,
            1.532292866386779e-236,
            2.4288802495464524e298,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.717627264832759e-277,
            1.532292866386779e-236,
            2.428880249546452e298,
            -2.3126040374230292e44,
            -2.0049509190923273e-186,
            8.996091194292953e-289,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            3.5196124312355037,
            4.136247513507615e-258,
            3.911282533865215e272,
            4.136247513507615e-258,
            3.5196124312355037,
            3.911282533865215e272,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.5196124312355037,
            4.136247513507615e-258,
            3.911282533865215e272,
            3.5196124312355037,
            4.136247513507615e-258,
            3.9112825338652156e272,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.5196124312355037,
            4.136247513507615e-258,
            3.911282533865215e272,
            1.5116456412741278e128,
            -1.745888690337514e280,
            4.0451535790191214e-195,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            7.523882125475249e78,
            7.723456413116011e-104,
            -1.9735935472959893e-277,
            7.723456413116011e-104,
            7.523882125475249e78,
            -1.9735935472959893e-277,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            7.523882125475249e78,
            7.723456413116011e-104,
            -1.9735935472959893e-277,
            7.523882125475249e78,
            7.723456413116011e-104,
            -1.973593547295989e-277,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            7.523882125475249e78,
            7.723456413116011e-104,
            -1.9735935472959893e-277,
            3.2540366653479136e216,
            -2.416088033015579e199,
            2.2377394128566688e-146,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            7.871137286961827e177,
            -2.1147049174387933e-300,
            -1.8690538999501524e-167,
            -2.1147049174387933e-300,
            7.871137286961827e177,
            -1.8690538999501524e-167,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            7.871137286961827e177,
            -2.1147049174387933e-300,
            -1.8690538999501524e-167,
            7.871137286961827e177,
            -2.1147049174387933e-300,
            -1.869053899950152e-167,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            7.871137286961827e177,
            -2.1147049174387933e-300,
            -1.8690538999501524e-167,
            -1.1235377443817564e-256,
            3.5122255235311993e-134,
            5.245680429080786e-225,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -1.659650476631694e89,
            6.606445798933959e-275,
            2.6015992829123124e113,
            6.606445798933959e-275,
            -1.659650476631694e89,
            2.6015992829123124e113,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.659650476631694e89,
            6.606445798933959e-275,
            2.6015992829123124e113,
            -1.659650476631694e89,
            6.606445798933959e-275,
            2.6015992829123128e113,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -1.659650476631694e89,
            6.606445798933959e-275,
            2.6015992829123124e113,
            -2.1404063133625775e-191,
            -1.612012994057336e261,
            -1.1300834655118761e178,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            2.6648123556093843e-120,
            4.567241781433288e-102,
            -7.701027453646016e77,
            4.567241781433288e-102,
            2.6648123556093843e-120,
            -7.701027453646016e77,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.6648123556093843e-120,
            4.567241781433288e-102,
            -7.701027453646016e77,
            2.6648123556093843e-120,
            4.567241781433288e-102,
            -7.701027453646015e77,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            2.6648123556093843e-120,
            4.567241781433288e-102,
            -7.701027453646016e77,
            -7.956936630785565e201,
            -2.976063638279301e-176,
            -4.364743231907437e-71,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.2481833538017014e-201,
            -6.334162777189719e55,
            2.2227698791965274e297,
            -6.334162777189719e55,
            1.2481833538017014e-201,
            2.2227698791965274e297,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.2481833538017014e-201,
            -6.334162777189719e55,
            2.2227698791965274e297,
            1.2481833538017014e-201,
            -6.334162777189719e55,
            2.2227698791965277e297,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.2481833538017014e-201,
            -6.334162777189719e55,
            2.2227698791965274e297,
            -2.89888499609466e51,
            -3.1930846497766047e-79,
            -2.999244731926118e186,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            3.4739630215718197e151,
            8.176996488717955e271,
            7.870406385410414e259,
            8.176996488717955e271,
            3.4739630215718197e151,
            7.870406385410414e259,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.4739630215718197e151,
            8.176996488717955e271,
            7.870406385410414e259,
            3.4739630215718197e151,
            8.176996488717955e271,
            7.870406385410416e259,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            3.4739630215718197e151,
            8.176996488717955e271,
            7.870406385410414e259,
            8.041360898627709e-155,
            -3.1760243060588205e111,
            -6.331872466636195e219,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -4.0297649110606253e257,
            -2.5203920301672486e282,
            7.525393236291551e-247,
            -2.5203920301672486e282,
            -4.0297649110606253e257,
            7.525393236291551e-247,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.0297649110606253e257,
            -2.5203920301672486e282,
            7.525393236291551e-247,
            -4.0297649110606253e257,
            -2.5203920301672486e282,
            7.525393236291552e-247,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.0297649110606253e257,
            -2.5203920301672486e282,
            7.525393236291551e-247,
            -1.0802941852517273e24,
            1.6260309775468048e137,
            9.288847910212047e278,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.4242110912058782e97,
            -2.9931458178909806e292,
            8.688327094956684e-202,
            -2.9931458178909806e292,
            -2.4242110912058782e97,
            8.688327094956684e-202,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.4242110912058782e97,
            -2.9931458178909806e292,
            8.688327094956684e-202,
            -2.4242110912058782e97,
            -2.9931458178909806e292,
            8.688327094956686e-202,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.4242110912058782e97,
            -2.9931458178909806e292,
            8.688327094956684e-202,
            -1.9307370454669174e-101,
            -5.096670845570017e-169,
            1.5331176328260163e-23,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -2.867512838941491e-266,
            2.815468714240397e-142,
            -1.6302601838148925e-227,
            2.815468714240397e-142,
            -2.867512838941491e-266,
            -1.6302601838148925e-227,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.867512838941491e-266,
            2.815468714240397e-142,
            -1.6302601838148925e-227,
            -2.867512838941491e-266,
            2.815468714240397e-142,
            -1.6302601838148922e-227,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -2.867512838941491e-266,
            2.815468714240397e-142,
            -1.6302601838148925e-227,
            -2.221912112549445e-150,
            3.7577136803169303e-303,
            1.1719271539039202e-139,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -4.408894571560278e-208,
            -1.8076048632948263e-136,
            2.9857309985024723e195,
            -1.8076048632948263e-136,
            -4.408894571560278e-208,
            2.9857309985024723e195,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.408894571560278e-208,
            -1.8076048632948263e-136,
            2.9857309985024723e195,
            -4.408894571560278e-208,
            -1.8076048632948263e-136,
            2.985730998502473e195,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -4.408894571560278e-208,
            -1.8076048632948263e-136,
            2.9857309985024723e195,
            1.8643596392483064e166,
            -6.4909279917683155e-221,
            1.5956413947977282e112,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            8.737513675320832e73,
            -2.808240903240821e66,
            -8.625760715133042e39,
            -2.808240903240821e66,
            8.737513675320832e73,
            -8.625760715133042e39,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            8.737513675320832e73,
            -2.808240903240821e66,
            -8.625760715133042e39,
            8.737513675320832e73,
            -2.808240903240821e66,
            -8.625760715133041e39,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            8.737513675320832e73,
            -2.808240903240821e66,
            -8.625760715133042e39,
            -1.7727414854972606e124,
            -5.339467867582044e-157,
            3885674.9654391557,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.9740144573539402e179,
            4.497690465473286e-303,
            -2.326071549920756e-65,
            4.497690465473286e-303,
            1.9740144573539402e179,
            -2.326071549920756e-65,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.9740144573539402e179,
            4.497690465473286e-303,
            -2.326071549920756e-65,
            1.9740144573539402e179,
            4.497690465473286e-303,
            -2.3260715499207557e-65,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.9740144573539402e179,
            4.497690465473286e-303,
            -2.326071549920756e-65,
            3.5995869424829998e84,
            -1.2530405724665672e33,
            2.2765097353207365e-291,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -3.5348908547127697e174,
            -8.177954827048964e-95,
            6.428849315248486e-292,
            -8.177954827048964e-95,
            -3.5348908547127697e174,
            6.428849315248486e-292,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.5348908547127697e174,
            -8.177954827048964e-95,
            6.428849315248486e-292,
            -3.5348908547127697e174,
            -8.177954827048964e-95,
            6.428849315248487e-292,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.5348908547127697e174,
            -8.177954827048964e-95,
            6.428849315248486e-292,
            -4.357372811475448e-129,
            -1.4400324128674765e256,
            -1.2060925491209205e-242,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.9297024862703127e159,
            2.1429665647738345e-258,
            -7.680970094582255e-51,
            2.1429665647738345e-258,
            1.9297024862703127e159,
            -7.680970094582255e-51,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.9297024862703127e159,
            2.1429665647738345e-258,
            -7.680970094582255e-51,
            1.9297024862703127e159,
            2.1429665647738345e-258,
            -7.680970094582254e-51,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.9297024862703127e159,
            2.1429665647738345e-258,
            -7.680970094582255e-51,
            -1.0327277250629479e213,
            3.2941523640673343e-194,
            2.3179323583664194e-239,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -5.8519716824307166e-211,
            -9.258618667967062e183,
            -3.7984180916211256,
            -9.258618667967062e183,
            -5.8519716824307166e-211,
            -3.7984180916211256,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.8519716824307166e-211,
            -9.258618667967062e183,
            -3.7984180916211256,
            -5.8519716824307166e-211,
            -9.258618667967062e183,
            -3.798418091621125,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -5.8519716824307166e-211,
            -9.258618667967062e183,
            -3.7984180916211256,
            -4.902537955350203e-93,
            -3.502711588645113e179,
            -4.812032406073248e293,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -8.681969116255738e32,
            8.416603548375196e-116,
            -9.538340970244034e207,
            8.416603548375196e-116,
            -8.681969116255738e32,
            -9.538340970244034e207,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -8.681969116255738e32,
            8.416603548375196e-116,
            -9.538340970244034e207,
            -8.681969116255738e32,
            8.416603548375196e-116,
            -9.538340970244033e207,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -8.681969116255738e32,
            8.416603548375196e-116,
            -9.538340970244034e207,
            -1.1348437698689779e105,
            -9.542675122637106e-44,
            1.2862497173162548e119,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.560270090872926e126,
            -2.3737259029701604e44,
            5.173007713244174e-30,
            -2.3737259029701604e44,
            1.560270090872926e126,
            5.173007713244174e-30,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.560270090872926e126,
            -2.3737259029701604e44,
            5.173007713244174e-30,
            1.560270090872926e126,
            -2.3737259029701604e44,
            5.1730077132441746e-30,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.560270090872926e126,
            -2.3737259029701604e44,
            5.173007713244174e-30,
            2.010660616363746e197,
            4.321688713483237e192,
            -1.4417671417358825e169,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            4.0544340774534354e-194,
            -1.359538242290476e29,
            -3.78544849132887e-295,
            -1.359538242290476e29,
            4.0544340774534354e-194,
            -3.78544849132887e-295,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.0544340774534354e-194,
            -1.359538242290476e29,
            -3.78544849132887e-295,
            4.0544340774534354e-194,
            -1.359538242290476e29,
            -3.7854484913288694e-295,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            4.0544340774534354e-194,
            -1.359538242290476e29,
            -3.78544849132887e-295,
            -1.4300369392366267e64,
            9.7627664782163e-192,
            -8.311495686726954e-13,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.891558029292057e-29,
            -1.0534799837369889e-304,
            -1.5537776172111544e-161,
            -1.0534799837369889e-304,
            1.891558029292057e-29,
            -1.5537776172111544e-161,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.891558029292057e-29,
            -1.0534799837369889e-304,
            -1.5537776172111544e-161,
            1.891558029292057e-29,
            -1.0534799837369889e-304,
            -1.5537776172111542e-161,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.891558029292057e-29,
            -1.0534799837369889e-304,
            -1.5537776172111544e-161,
            -6.225038807499687e-172,
            -6.185133574594258e244,
            -3.371225875774181e119,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            6.4005022626269135e271,
            7.303088122650307e42,
            -1.4202396625313175e30,
            7.303088122650307e42,
            6.4005022626269135e271,
            -1.4202396625313175e30,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            6.4005022626269135e271,
            7.303088122650307e42,
            -1.4202396625313175e30,
            6.4005022626269135e271,
            7.303088122650307e42,
            -1.4202396625313172e30,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            6.4005022626269135e271,
            7.303088122650307e42,
            -1.4202396625313175e30,
            -3.629441744070413e155,
            -9.086910642694262e19,
            -1.0640724942850892e-110,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            1.252310085001006e-151,
            -3.523250066358668e291,
            4.306660558811275e-175,
            -3.523250066358668e291,
            1.252310085001006e-151,
            4.306660558811275e-175,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.252310085001006e-151,
            -3.523250066358668e291,
            4.306660558811275e-175,
            1.252310085001006e-151,
            -3.523250066358668e291,
            4.3066605588112755e-175,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            1.252310085001006e-151,
            -3.523250066358668e291,
            4.306660558811275e-175,
            1.340890795062786e123,
            6.032146614977095e156,
            6.127320723533213e237,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            6.89434927515376e-150,
            5.176143593320397e-273,
            6.698378651354705e133,
            5.176143593320397e-273,
            6.89434927515376e-150,
            6.698378651354705e133,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            6.89434927515376e-150,
            5.176143593320397e-273,
            6.698378651354705e133,
            6.89434927515376e-150,
            5.176143593320397e-273,
            6.698378651354706e133,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            6.89434927515376e-150,
            5.176143593320397e-273,
            6.698378651354705e133,
            -1.0422423050903368e71,
            1.1130534703651679e269,
            -6.3035278791071536e140,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -3.5815274624992166e168,
            1.7934547774270936e-109,
            1.2652817806303301e115,
            1.7934547774270936e-109,
            -3.5815274624992166e168,
            1.2652817806303301e115,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.5815274624992166e168,
            1.7934547774270936e-109,
            1.2652817806303301e115,
            -3.5815274624992166e168,
            1.7934547774270936e-109,
            1.2652817806303303e115,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -3.5815274624992166e168,
            1.7934547774270936e-109,
            1.2652817806303301e115,
            2.097591016427905e-304,
            -9.24778713323095e137,
            -3.202217380407371e-65,
        ),
    );
    expectEq(
        true,
        CsgExactArithmetic.equalAffine(
            -6.0103302026498684e-96,
            -1.0139201479681181e130,
            -9.562126140165959e20,
            -1.0139201479681181e130,
            -6.0103302026498684e-96,
            -9.562126140165959e20,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -6.0103302026498684e-96,
            -1.0139201479681181e130,
            -9.562126140165959e20,
            -6.0103302026498684e-96,
            -1.0139201479681181e130,
            -9.562126140165958e20,
        ),
    );
    expectEq(
        false,
        CsgExactArithmetic.equalAffine(
            -6.0103302026498684e-96,
            -1.0139201479681181e130,
            -9.562126140165959e20,
            1.8686467922090255e-207,
            1.2222341998598e242,
            -1.568042398582925e-126,
        ),
    );
    {
        const a = [7.023151674959754e-197, 1.3178925597820586e269, -2.3051368183066844e221];
        const b = [2.844442798671976e-181, 7.739065225517168e-285, -6.314602941975614e87];
        const c = [-2.3051368183066844e221, 1.3178925597820586e269, 7.023151674959754e-197];
        const d = [-6.314602941975614e87, 7.739065225517168e-285, 2.844442798671976e-181];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-9.11053725992608e-53, -5.21509584180785e-206, -3.77397318760716e-178];
        const b = [1.4092171352756452e72, -8.881435684317558e141, 7.437121533935839e110];
        const c = [-3.773973187607159e-178, -5.21509584180785e-206, -9.11053725992608e-53];
        const d = [7.437121533935839e110, -8.881435684317558e141, 1.4092171352756452e72];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.6552501870196565e265, -2.653658952189277e-240, -4.7515961432015003e285];
        const b = [3.2635345558538276e-231, 3.968643344889072e-169, -1.8796698335529575e-160];
        const c = [-4.7515961432015e285, -2.653658952189277e-240, -1.6552501870196565e265];
        const d = [-1.8796698335529575e-160, 3.968643344889072e-169, 3.2635345558538276e-231];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [6.562820902061361e-248, 1.91863949839063e242, 3.2661600488474977e-251];
        const b = [3.6565756552985994e203, 1.4602440076281917e-224, -1.9473735349530548e151];
        const c = [3.266160048847498e-251, 1.91863949839063e242, 6.562820902061361e-248];
        const d = [-1.9473735349530548e151, 1.4602440076281917e-224, 3.6565756552985994e203];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [6.5233385059368e-113, 2.2463148668896033e192, -1.781085327556974e-74];
        const b = [-1.547415232678764e-147, 6.630988157383086e208, -2.6533352578177235e-220];
        const c = [-1.781085327556974e-74, 2.2463148668896033e192, 6.5233385059368e-113];
        const d = [-2.6533352578177235e-220, 6.630988157383086e208, -1.547415232678764e-147];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-7.571781158605971e86, -1.1290148106985158e97, 2.462270108201401e-71];
        const b = [4.706135388542235e247, 3.468276250793211e-303, -3.3416506641179006e267];
        const c = [2.4622701082014012e-71, -1.1290148106985158e97, -7.571781158605971e86];
        const d = [-3.3416506641179006e267, 3.468276250793211e-303, 4.706135388542235e247];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.0886238747352265e250, 1.929394089620867e86, -1.4122748482427193e-37];
        const b = [-1.3239112504769962e-70, 4.170472033286872e-50, 1.812586900952522e-177];
        const c = [-1.4122748482427193e-37, 1.929394089620867e86, 1.0886238747352265e250];
        const d = [1.812586900952522e-177, 4.170472033286872e-50, -1.3239112504769962e-70];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-6.0388522904301614e-92, -3.2187323404291255e89, -2.3033468616812835e-92];
        const b = [5.663002478274509e32, 3.595731459281659e-78, -5.315438557673632e-8];
        const c = [-2.3033468616812835e-92, -3.2187323404291255e89, -6.0388522904301614e-92];
        const d = [-5.315438557673632e-8, 3.595731459281659e-78, 5.663002478274509e32];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-7.591408880355168e136, 1.0420089325880833e47, -2.2136957125337786e34];
        const b = [2.5129306267911944e125, -2.643853167990063e254, -3.963591044748313e223];
        const c = [-2.2136957125337786e34, 1.0420089325880833e47, -7.591408880355168e136];
        const d = [-3.963591044748313e223, -2.643853167990063e254, 2.5129306267911944e125];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.2272031184526036e172, 2.6650830853875663e-206, 1.335316062328114e-64];
        const b = [-1.8155846978495016e-37, -1.0446003537089951e-150, -1.6888055896893644e-190];
        const c = [1.3353160623281142e-64, 2.6650830853875663e-206, -2.2272031184526036e172];
        const d = [-1.6888055896893644e-190, -1.0446003537089951e-150, -1.8155846978495016e-37];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.811881439769263e-230, 2.0048629392985727e-257, -1.3963065674502596e-152];
        const b = [-1.0547199453076516e-127, -6.545964631708323e-102, -3.056391316970476e-302];
        const c = [-1.3963065674502596e-152, 2.0048629392985727e-257, -2.811881439769263e-230];
        const d = [-3.056391316970476e-302, -6.545964631708323e-102, -1.0547199453076516e-127];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [3.260367453327192e-207, -6.116540515868539e74, -2.649624929924912e-138];
        const b = [5.981165178809998e218, 2.1978669921919706e-158, -2.994472480197313e-36];
        const c = [-2.6496249299249116e-138, -6.116540515868539e74, 3.260367453327192e-207];
        const d = [-2.994472480197313e-36, 2.1978669921919706e-158, 5.981165178809998e218];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.831616503783565e19, 1.41119908951671e-200, -7.869360230709454e83];
        const b = [-1.5054489655827371e308, 1.8732813867287292e155, -1.47309140514175e36];
        const c = [-7.869360230709454e83, 1.41119908951671e-200, -1.831616503783565e19];
        const d = [-1.47309140514175e36, 1.8732813867287292e155, -1.5054489655827371e308];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-4.9087167303863816e-138, -1.86540181498214e-226, -8.278960017914677e281];
        const b = [-4.5527293924516847e61, 2.495636207357603e-37, -1.0151672204816107e-305];
        const c = [-8.278960017914676e281, -1.86540181498214e-226, -4.9087167303863816e-138];
        const d = [-1.0151672204816107e-305, 2.495636207357603e-37, -4.5527293924516847e61];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-4.309417578875003e-82, 1.8560619128220422e115, -7.40690281276719e45];
        const b = [-5.222752783227147e-278, -2.30001755708124e-100, 5.476479905767886e130];
        const c = [-7.40690281276719e45, 1.8560619128220422e115, -4.309417578875003e-82];
        const d = [5.476479905767886e130, -2.30001755708124e-100, -5.222752783227147e-278];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [2.0885488756778166e-106, -4.68950665081133e-231, 1.4743231542212645e50];
        const b = [-1.0559758603607788e82, -4.433989374766808e230, 1.109612879019346e-143];
        const c = [1.4743231542212647e50, -4.68950665081133e-231, 2.0885488756778166e-106];
        const d = [1.109612879019346e-143, -4.433989374766808e230, -1.0559758603607788e82];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [5.6257998934717676e-34, 1.7359291093473098e-174, 1.2420196683158593e262];
        const b = [-3.08837258890408e124, -8.657583273768199e-189, 8.748635245090715e304];
        const c = [1.2420196683158593e262, 1.7359291093473098e-174, 5.6257998934717676e-34];
        const d = [8.748635245090715e304, -8.657583273768199e-189, -3.08837258890408e124];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [5.871454466806739e-217, 1.3554964748177846e219, 1.9002702405434208e-9];
        const b = [8.982701488770505e-16, 1.4358170698792938e113, 4.2635556552341154e-200];
        const c = [1.900270240543421e-9, 1.3554964748177846e219, 5.871454466806739e-217];
        const d = [4.2635556552341154e-200, 1.4358170698792938e113, 8.982701488770505e-16];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [2.2312008580420897e-249, -2.5508822516328146e-92, 3.0598768128298893e136];
        const b = [-9.086226037978966e192, 1.218046392475836e-36, -1.126183566761844e-31];
        const c = [3.0598768128298893e136, -2.5508822516328146e-92, 2.2312008580420897e-249];
        const d = [-1.126183566761844e-31, 1.218046392475836e-36, -9.086226037978966e192];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-979121.9795403609, -7.789326943762943e257, 4.048820764381433e-141];
        const b = [-2.60830097467994e-104, 6.213531995010937e81, -6.017172674836612e-238];
        const c = [4.048820764381433e-141, -7.789326943762943e257, -979121.9795403609];
        const d = [-6.017172674836612e-238, 6.213531995010937e81, -2.60830097467994e-104];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.826875941671167e-276, 4.69823528562516e-97, 1.3168574444357981e241];
        const b = [-4.4606579026794047e-299, 7.380476849251337e237, -1.8597779590743064e-89];
        const c = [1.3168574444357981e241, 4.69823528562516e-97, -1.826875941671167e-276];
        const d = [-1.8597779590743064e-89, 7.380476849251337e237, -4.4606579026794047e-299];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.2675566561537167e291, -5.4621046334565925e-202, -9.552568327180986e267];
        const b = [4.682440287051097e143, 2.1701388021506703e192, -3.671635318402137e93];
        const c = [-9.552568327180984e267, -5.4621046334565925e-202, -1.2675566561537167e291];
        const d = [-3.671635318402137e93, 2.1701388021506703e192, 4.682440287051097e143];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.2482751628007603e-210, 8.454268985860035e271, 2.80603300927561e201];
        const b = [-1.8864757548839593e-230, 5.973621703861472e-76, -7.300798539024728e27];
        const c = [2.8060330092756106e201, 8.454268985860035e271, 1.2482751628007603e-210];
        const d = [-7.300798539024728e27, 5.973621703861472e-76, -1.8864757548839593e-230];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [5.34320999459547e-50, 9.247997647177687e-136, 3.5689907471018955e272];
        const b = [5.260301329416069e167, 1.3145904771640495e25, 1.9659641241195642e270];
        const c = [3.5689907471018955e272, 9.247997647177687e-136, 5.34320999459547e-50];
        const d = [1.9659641241195642e270, 1.3145904771640495e25, 5.260301329416069e167];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.892968992170194e154, -2.69564981897255e-92, 6.674444244593363e298];
        const b = [1.066980379610045e-65, 1.491954003294374e230, -1.14263867665979e-80];
        const c = [6.674444244593363e298, -2.69564981897255e-92, -1.892968992170194e154];
        const d = [-1.14263867665979e-80, 1.491954003294374e230, 1.066980379610045e-65];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.6501293991664466e-43, 3.3743725459093077e-108, 2.898073628513751e47];
        const b = [3.308319020011984e246, -6.286311513526217e111, -3.999679793393423e-66];
        const c = [2.898073628513751e47, 3.3743725459093077e-108, 1.6501293991664466e-43];
        const d = [-3.999679793393423e-66, -6.286311513526217e111, 3.308319020011984e246];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [4.1081623532298787e89, 3.926766737682831e-198, 1.0642626764982548e-293];
        const b = [-2.2063143574426307e-41, -5.242065073441578e273, 5.359769472938859e-240];
        const c = [1.0642626764982548e-293, 3.926766737682831e-198, 4.1081623532298787e89];
        const d = [5.359769472938859e-240, -5.242065073441578e273, -2.2063143574426307e-41];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [9.625225822842384e175, 1.9524479121844115e-221, -0.0038635696821519056];
        const b = [-1.6769035857582493e-124, 2.857746379832997e275, 7.157841005448532e-177];
        const c = [-0.0038635696821519056, 1.9524479121844115e-221, 9.625225822842384e175];
        const d = [7.157841005448532e-177, 2.857746379832997e275, -1.6769035857582493e-124];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.8630376678436463e-223, 9.551256333504309, -3.5429641714448843e-35];
        const b = [-4.164580193699397e245, -7.764367342470971e155, -1.0688271849429501e-207];
        const c = [-3.542964171444884e-35, 9.551256333504309, 1.8630376678436463e-223];
        const d = [-1.0688271849429501e-207, -7.764367342470971e155, -4.164580193699397e245];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [8.64303402689633e-98, 1.305978847259132e-186, 2.666035415515044e240];
        const b = [3.2210770980769573e272, -4.0507985601491665e269, -1.1020978409434877e231];
        const c = [2.666035415515044e240, 1.305978847259132e-186, 8.64303402689633e-98];
        const d = [-1.1020978409434877e231, -4.0507985601491665e269, 3.2210770980769573e272];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-3.5283351385707193e-96, 4.9501242513793226e-136, -2.3996328443349792e303];
        const b = [4.210975941688312e233, 8.742670152060802e130, -2.0779170804740936e214];
        const c = [-2.3996328443349792e303, 4.9501242513793226e-136, -3.5283351385707193e-96];
        const d = [-2.0779170804740936e214, 8.742670152060802e130, 4.210975941688312e233];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [3.4528066294197293e-240, -3.3202106018855747e-108, 2.5073833677000444e-259];
        const b = [7.384154608071037e-140, -2.275172613661001e-152, 1.0996234827197043e-181];
        const c = [2.5073833677000444e-259, -3.3202106018855747e-108, 3.4528066294197293e-240];
        const d = [1.0996234827197043e-181, -2.275172613661001e-152, 7.384154608071037e-140];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-3.74459860566512e273, 2.5920978926163535e-134, 4.8613569822668976e131];
        const b = [-1.8255035739824162e-7, -6.658630245681853e-230, -6.836581585139932e68];
        const c = [4.861356982266898e131, 2.5920978926163535e-134, -3.74459860566512e273];
        const d = [-6.836581585139932e68, -6.658630245681853e-230, -1.8255035739824162e-7];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-4.0760815944551856e93, -2.7484058970898175e-5, -9.04350695479875e-118];
        const b = [-3.8223797842993345e-159, 2.4960872231332896e-29, -2.6452004199475745e-219];
        const c = [-9.043506954798749e-118, -2.7484058970898175e-5, -4.0760815944551856e93];
        const d = [-2.6452004199475745e-219, 2.4960872231332896e-29, -3.8223797842993345e-159];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-4.0083220745492424e18, 4.450970707513548e279, -7.946397515952286e100];
        const b = [-1.1776459316942659e36, 1.9671517253707615e269, 4.4368544010756515e122];
        const c = [-7.946397515952286e100, 4.450970707513548e279, -4.0083220745492424e18];
        const d = [4.4368544010756515e122, 1.9671517253707615e269, -1.1776459316942659e36];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.592975036403888e91, 2.1491626233618212e257, -6.268379761974452e-228];
        const b = [-6.409378887758653e280, 1.0936379329698934e250, 2.9263368906604104e115];
        const c = [-6.268379761974451e-228, 2.1491626233618212e257, 1.592975036403888e91];
        const d = [2.9263368906604104e115, 1.0936379329698934e250, -6.409378887758653e280];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.1384696323111254e-25, 8.783552848183297e23, -2.650110757466884e-269];
        const b = [3.946862311315536e-163, 2.3056429541433347e120, 5.111638921320054e-85];
        const c = [-2.6501107574668838e-269, 8.783552848183297e23, -2.1384696323111254e-25];
        const d = [5.111638921320054e-85, 2.3056429541433347e120, 3.946862311315536e-163];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.507556793680233e-281, 1.9896001358300308e-197, 1.2276919266177184e50];
        const b = [5.712601827630757e-290, -5.068143169424987e265, -4.803907694528488e-217];
        const c = [1.2276919266177186e50, 1.9896001358300308e-197, -1.507556793680233e-281];
        const d = [-4.803907694528488e-217, -5.068143169424987e265, 5.712601827630757e-290];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [2.4504326202413082e-88, 2.096063640521953e293, -1.951031325859467e55];
        const b = [4.5517612798053304e-26, 6.433447529044753e-305, -7.164997394028447e-93];
        const c = [-1.951031325859467e55, 2.096063640521953e293, 2.4504326202413082e-88];
        const d = [-7.164997394028447e-93, 6.433447529044753e-305, 4.5517612798053304e-26];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [7.143822587531701e192, 2.8974981928330976e267, -1.042913585623514e199];
        const b = [-8.637544579427111e136, 1.1648359959730173e275, -2.022631248359768e66];
        const c = [-1.0429135856235138e199, 2.8974981928330976e267, 7.143822587531701e192];
        const d = [-2.022631248359768e66, 1.1648359959730173e275, -8.637544579427111e136];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.2900713032474572e-307, -1.6095756034120858e91, -1.3451938688780783e-137];
        const b = [5.467555099750595e-297, 1.793252431208181e-234, 3.580526761340242e236];
        const c = [-1.345193868878078e-137, -1.6095756034120858e91, -1.2900713032474572e-307];
        const d = [3.580526761340242e236, 1.793252431208181e-234, 5.467555099750595e-297];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [2.1752943124576324e171, 3.168415027891107e242, -1.0811466136622914e-59];
        const b = [-3.3581443814342123e-224, -1.763740967555296e200, -1.650268549822446e276];
        const c = [-1.0811466136622914e-59, 3.168415027891107e242, 2.1752943124576324e171];
        const d = [-1.650268549822446e276, -1.763740967555296e200, -3.3581443814342123e-224];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-8.997663892824676e-191, -2.6285012252094203e-293, -8.324552727036787e-222];
        const b = [-3.489828945751554e-241, 5.00543241841911e46, 5.758576618610774e119];
        const c = [-8.324552727036785e-222, -2.6285012252094203e-293, -8.997663892824676e-191];
        const d = [5.758576618610774e119, 5.00543241841911e46, -3.489828945751554e-241];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [6.960887285732464e22, -8.772113086155289e-305, 8.62874635548718e-16];
        const b = [-1.0634451662644262e-307, 1.110671924499327e-305, 1.3901096263417496e182];
        const c = [8.62874635548718e-16, -8.772113086155289e-305, 6.960887285732464e22];
        const d = [1.3901096263417496e182, 1.110671924499327e-305, -1.0634451662644262e-307];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-3.1612070164949962e-90, 9.110870389954093e-261, 1.842189492211333e150];
        const b = [-3.436496519396562e140, -6.512971150656734e-203, 2.2967942757473097];
        const c = [1.8421894922113333e150, 9.110870389954093e-261, -3.1612070164949962e-90];
        const d = [2.2967942757473097, -6.512971150656734e-203, -3.436496519396562e140];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [2.6786693637799083e-129, -4.475204322035845e-19, -6.4572837725052805e-248];
        const b = [-2.6886789970159906e-124, 2.2713634248287985e172, -3.814293122367182e-234];
        const c = [-6.45728377250528e-248, -4.475204322035845e-19, 2.6786693637799083e-129];
        const d = [-3.814293122367182e-234, 2.2713634248287985e172, -2.6886789970159906e-124];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [8.887575190517185e-22, -8.151178751308642e-119, 9.268117434826531e117];
        const b = [-1.9509251977672049e-196, -1.347443025658705e303, -1.1481970636793702e-97];
        const c = [9.268117434826531e117, -8.151178751308642e-119, 8.887575190517185e-22];
        const d = [-1.1481970636793702e-97, -1.347443025658705e303, -1.9509251977672049e-196];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [2.920746280761439e92, 2.400243410440474e269, 2.1568976174037177e66];
        const b = [-7.0848901680244535e236, -9.295219562811268e-213, -2.7240333521640612e-42];
        const c = [2.1568976174037177e66, 2.400243410440474e269, 2.920746280761439e92];
        const d = [-2.7240333521640612e-42, -9.295219562811268e-213, -7.0848901680244535e236];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.6499140660516012e272, 1.7499758810304108e248, 8.262865282309773e-125];
        const b = [8.97607013415842e-287, 5.66820967416643e-278, -3.035559239324403e-48];
        const c = [8.262865282309774e-125, 1.7499758810304108e248, -1.6499140660516012e272];
        const d = [-3.035559239324403e-48, 5.66820967416643e-278, 8.97607013415842e-287];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.9744345890841093e-114, -1.492741872113723e36, -0.4482331066015903];
        const b = [-6506276543962.416, 1.0114268433088719e-281, 2.612255751939805e-236];
        const c = [-0.4482331066015903, -1.492741872113723e36, -2.9744345890841093e-114];
        const d = [2.612255751939805e-236, 1.0114268433088719e-281, -6506276543962.416];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.918758273660034e-23, 2.9176268291396825e-281, 1.0678698905156993e208];
        const b = [3.3410371259358873e224, 1.9034014219197178e-75, 1.9329955715137268e248];
        const c = [1.0678698905156993e208, 2.9176268291396825e-281, 1.918758273660034e-23];
        const d = [1.9329955715137268e248, 1.9034014219197178e-75, 3.3410371259358873e224];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-5.28428694540788e-13, -1.0605625805879185e-22, 3.1670283972209684e38];
        const b = [2.8007794388761234e-105, -2.3573692473485564e-61, 7.095352606339755e150];
        const c = [3.1670283972209684e38, -1.0605625805879185e-22, -5.28428694540788e-13];
        const d = [7.095352606339755e150, -2.3573692473485564e-61, 2.8007794388761234e-105];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [2.8047581805829457e297, -4.186773283796094e-10, 2.3527651175442525e-194];
        const b = [-6.228775947748288e-244, 2.8171450163955657e-5, 1.0520085771287694e-78];
        const c = [2.3527651175442528e-194, -4.186773283796094e-10, 2.8047581805829457e297];
        const d = [1.0520085771287694e-78, 2.8171450163955657e-5, -6.228775947748288e-244];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-3.7590191598624615e34, -1.282051338578311e-93, -4.684917426611222e-307];
        const b = [2.0847451857347018e-100, 6.625560017531347e-34, -4.813696844301357e28];
        const c = [-4.6849174266112214e-307, -1.282051338578311e-93, -3.7590191598624615e34];
        const d = [-4.813696844301357e28, 6.625560017531347e-34, 2.0847451857347018e-100];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-5.074951275148107e180, -9.479815987453484e-30, 1.1339833761185473e61];
        const b = [6.555176394852529e26, -2.839981606753104e295, 1.8277136486486964e-66];
        const c = [1.1339833761185475e61, -9.479815987453484e-30, -5.074951275148107e180];
        const d = [1.8277136486486964e-66, -2.839981606753104e295, 6.555176394852529e26];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.165544142915043e81, -1.8621540799981465e-6, 1.5809629951855696e28];
        const b = [5.920276595198038e199, 3.0936277783922486e236, 1.5728303407758465e-173];
        const c = [1.5809629951855698e28, -1.8621540799981465e-6, 1.165544142915043e81];
        const d = [1.5728303407758465e-173, 3.0936277783922486e236, 5.920276595198038e199];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-7.526816018297133e-195, 6.007147523403272e-102, 2.4921318271946455e71];
        const b = [3.621935524879749e107, 4.0416887633166345e-300, -3.050521835359361e67];
        const c = [2.492131827194646e71, 6.007147523403272e-102, -7.526816018297133e-195];
        const d = [-3.050521835359361e67, 4.0416887633166345e-300, 3.621935524879749e107];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-8.510574314841689e33, -1.8027145422527696e109, 1.2824684087244692e148];
        const b = [6.4484419725881e179, -135.50720433878504, 5.2262402262434225e-93];
        const c = [1.2824684087244692e148, -1.8027145422527696e109, -8.510574314841689e33];
        const d = [5.2262402262434225e-93, -135.50720433878504, 6.4484419725881e179];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-5.543605413394652e167, 2.738631389272877e-221, -9.80691042460092e65];
        const b = [-4.3778527573224417e105, -1.0458369540807827e84, -8.960593831194583e101];
        const c = [-9.806910424600918e65, 2.738631389272877e-221, -5.543605413394652e167];
        const d = [-8.960593831194583e101, -1.0458369540807827e84, -4.3778527573224417e105];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [3.78318856240716e-53, -6.490668358853672e-10, 3.8935284264309836e56];
        const b = [-2.2700555053813443e171, 2.561691510681516e-31, 2.5855517117064827e-247];
        const c = [3.8935284264309836e56, -6.490668358853672e-10, 3.78318856240716e-53];
        const d = [2.5855517117064827e-247, 2.561691510681516e-31, -2.2700555053813443e171];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [3.8142602172147015e-51, 7.421594595659825e135, -8.935337014015587e-295];
        const b = [8.250107509151561e53, -4.172054101363691e239, -9.453933847339664e-48];
        const c = [-8.935337014015587e-295, 7.421594595659825e135, 3.8142602172147015e-51];
        const d = [-9.453933847339664e-48, -4.172054101363691e239, 8.250107509151561e53];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-3.469349445681409e248, 2.2873983644950983e-162, -4.097119244185461e-264];
        const b = [-8.719865026570906e-20, 3.371040248084587e34, -4.462332765296434e-19];
        const c = [-4.097119244185461e-264, 2.2873983644950983e-162, -3.469349445681409e248];
        const d = [-4.462332765296434e-19, 3.371040248084587e34, -8.719865026570906e-20];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [4.687749617890354e-22, -2.2518061182499213e244, -3.220850373809829e204];
        const b = [8.307090653331578e-266, -9.666425990248017e45, -2.420496142471001e-259];
        const c = [-3.220850373809829e204, -2.2518061182499213e244, 4.687749617890354e-22];
        const d = [-2.420496142471001e-259, -9.666425990248017e45, 8.307090653331578e-266];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.105249388168026e153, -4.685221934244691e-10, 6.135591072788099e-240];
        const b = [2.1297379886506193e-69, -7.794346351827608e295, -1.426767056550301e119];
        const c = [6.135591072788099e-240, -4.685221934244691e-10, -2.105249388168026e153];
        const d = [-1.426767056550301e119, -7.794346351827608e295, 2.1297379886506193e-69];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-5.263724424008169e-162, -1.9906978126514902e146, 1.5297144039762066e-74];
        const b = [7.18226486961899e38, -5.722543916507476e-145, 1.8497640792305512e92];
        const c = [1.5297144039762068e-74, -1.9906978126514902e146, -5.263724424008169e-162];
        const d = [1.8497640792305512e92, -5.722543916507476e-145, 7.18226486961899e38];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-4.452814721605422e281, 3.6363348146287595e-76, 7.621308454704919e197];
        const b = [2.004679190601659e-166, 2.7294794109541463e-62, 9.348770276356013e214];
        const c = [7.621308454704919e197, 3.6363348146287595e-76, -4.452814721605422e281];
        const d = [9.348770276356013e214, 2.7294794109541463e-62, 2.004679190601659e-166];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.5290394176802015e-43, -7.39188801415021e-284, 6.372523731264915e94];
        const b = [-5.919878256341011e45, 1.593234405426442e-189, -1.2683695256090073e72];
        const c = [6.372523731264916e94, -7.39188801415021e-284, 1.5290394176802015e-43];
        const d = [-1.2683695256090073e72, 1.593234405426442e-189, -5.919878256341011e45];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-3.4385480924015034e149, -9.056803490941364e201, -3.332550661888629e261];
        const b = [-7.885572351785404e161, 8.329136231605234e-123, 6.676600136991137e-95];
        const c = [-3.3325506618886287e261, -9.056803490941364e201, -3.4385480924015034e149];
        const d = [6.676600136991137e-95, 8.329136231605234e-123, -7.885572351785404e161];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [266063002.87299317, 1.3276272886926148e213, -7.616198737276916e-152];
        const b = [3.0364073023941756e117, 6.909467460572568e246, 1.5744131742165056e46];
        const c = [-7.616198737276916e-152, 1.3276272886926148e213, 266063002.87299317];
        const d = [1.5744131742165056e46, 6.909467460572568e246, 3.0364073023941756e117];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.171091888069308e-272, -1.2811292617921663e132, -1.6224947537295233e274];
        const b = [8.226144294959461e175, 4.154107863260864e-16, -2.2872480054647294e38];
        const c = [-1.6224947537295231e274, -1.2811292617921663e132, -2.171091888069308e-272];
        const d = [-2.2872480054647294e38, 4.154107863260864e-16, 8.226144294959461e175];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-7.968144946200964e-261, -1.1816087262538034e307, -1.1407407904877425e-307];
        const b = [5.891679169226417e203, -6.502722769083556e-222, 1.1650990912576762e38];
        const c = [-1.1407407904877425e-307, -1.1816087262538034e307, -7.968144946200964e-261];
        const d = [1.1650990912576762e38, -6.502722769083556e-222, 5.891679169226417e203];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.7945250268734546e136, 1.5373912706533236e-38, -1.1569063818422463e195];
        const b = [-1.4816885642603971e50, 9.993954680565036e215, 7.241430621668595e286];
        const c = [-1.1569063818422461e195, 1.5373912706533236e-38, -1.7945250268734546e136];
        const d = [7.241430621668595e286, 9.993954680565036e215, -1.4816885642603971e50];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-3.595156526776673e82, -4.664447267151087e159, -6.51043286356032e61];
        const b = [3.0271662632399537e57, 1.895286649472281e-65, -2.0932373469918516e248];
        const c = [-6.510432863560319e61, -4.664447267151087e159, -3.595156526776673e82];
        const d = [-2.0932373469918516e248, 1.895286649472281e-65, 3.0271662632399537e57];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.4481646586455712e255, 1.6597385819797676e-242, -3.089879462937397e-98];
        const b = [-2.1312307747650717e51, 7.463363096727181e175, 8.530130208482562e127];
        const c = [-3.089879462937397e-98, 1.6597385819797676e-242, -1.4481646586455712e255];
        const d = [8.530130208482562e127, 7.463363096727181e175, -2.1312307747650717e51];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [3.1423541216543125e-247, -1.3684846637905351e274, 4.536479030158417e-126];
        const b = [-2.167538957355426e-302, -6.529924497168517e146, -6.234888813333569e-268];
        const c = [4.5364790301584176e-126, -1.3684846637905351e274, 3.1423541216543125e-247];
        const d = [-6.234888813333569e-268, -6.529924497168517e146, -2.167538957355426e-302];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [9.795409381245285e-54, 9.823793668971944e151, -4.92454828761703e-241];
        const b = [1.5022282800095988e68, -2.4311857440392476e48, 2.4219920237796577e-247];
        const c = [-4.92454828761703e-241, 9.823793668971944e151, 9.795409381245285e-54];
        const d = [2.4219920237796577e-247, -2.4311857440392476e48, 1.5022282800095988e68];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-4.562518010814288e248, -3.5557782360794046e29, 185383507.26542488];
        const b = [4.919103388017615e-52, -4.306235582618956e94, 4.508489478792208e303];
        const c = [185383507.2654249, -3.5557782360794046e29, -4.562518010814288e248];
        const d = [4.508489478792208e303, -4.306235582618956e94, 4.919103388017615e-52];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [2.3002779858524254e207, -3.80074994528424e17, 5.565944111686189e-275];
        const b = [3.0490753136032496e97, 1.8879160731850635e-181, 1.2853056375701115e115];
        const c = [5.565944111686189e-275, -3.80074994528424e17, 2.3002779858524254e207];
        const d = [1.2853056375701115e115, 1.8879160731850635e-181, 3.0490753136032496e97];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [7.879121991472415e163, -2.7097359739243386e281, -1.6923281780095874e177];
        const b = [-1.022995101542409e-114, 8.146826692416375e197, 3.8879373715380106e107];
        const c = [-1.6923281780095874e177, -2.7097359739243386e281, 7.879121991472415e163];
        const d = [3.8879373715380106e107, 8.146826692416375e197, -1.022995101542409e-114];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [7.421986716065045e-107, -2.81711813212106e250, -2.011020226929847e-116];
        const b = [-2.5434811058129543e-18, -2.610846659640426e-201, 2.266870626989865e41];
        const c = [-2.011020226929847e-116, -2.81711813212106e250, 7.421986716065045e-107];
        const d = [2.266870626989865e41, -2.610846659640426e-201, -2.5434811058129543e-18];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-8.327637125559376e128, -6.096395750639113e43, 5.982939951895994e195];
        const b = [-5.3748387135439255e156, -2.3102302078174787e-73, -1.6953336857325845e49];
        const c = [5.982939951895995e195, -6.096395750639113e43, -8.327637125559376e128];
        const d = [-1.6953336857325845e49, -2.3102302078174787e-73, -5.3748387135439255e156];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [4.822508208145364e-246, 1.2063396237805307e196, 2.940955132944407e200];
        const b = [1.4391214358815326e56, -8.519657776939867e74, 5.912489621782838e-10];
        const c = [2.940955132944407e200, 1.2063396237805307e196, 4.822508208145364e-246];
        const d = [5.912489621782838e-10, -8.519657776939867e74, 1.4391214358815326e56];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-5.681513096265885e-176, 1.4011082287627098e-223, 3.4779007107487747e-69];
        const b = [-2.488286781208963e255, 8.823685294939839e-33, -1.896646366169284e148];
        const c = [3.477900710748775e-69, 1.4011082287627098e-223, -5.681513096265885e-176];
        const d = [-1.896646366169284e148, 8.823685294939839e-33, -2.488286781208963e255];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.076605801536684e-193, 4.8392523022380496e-266, -8.093646228564425e100];
        const b = [2.7071278332792056e221, -1.933030049017639e-213, -5.639620947807869e107];
        const c = [-8.093646228564425e100, 4.8392523022380496e-266, -1.076605801536684e-193];
        const d = [-5.639620947807869e107, -1.933030049017639e-213, 2.7071278332792056e221];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.258982964830631e-244, -1.3384743013882171e116, 2.3633156783489404e-163];
        const b = [9.82242199414542e-248, 8.983957946349264e-284, 1.1374880419422121e44];
        const c = [2.3633156783489407e-163, -1.3384743013882171e116, -1.258982964830631e-244];
        const d = [1.1374880419422121e44, 8.983957946349264e-284, 9.82242199414542e-248];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [5.359979164064273e279, -4.573737890167717e-92, 7.91486595168795e95];
        const b = [7.222822767998194e160, 2.181667436084398e166, -5.1227593391856176e-37];
        const c = [7.914865951687951e95, -4.573737890167717e-92, 5.359979164064273e279];
        const d = [-5.1227593391856176e-37, 2.181667436084398e166, 7.222822767998194e160];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.0000181567592735e-154, -1.1595990657502195e-157, -1.4884394707120486e260];
        const b = [4.10365959439282e-171, 1.055516144453894e21, 3.937422470130277e225];
        const c = [-1.4884394707120483e260, -1.1595990657502195e-157, -2.0000181567592735e-154];
        const d = [3.937422470130277e225, 1.055516144453894e21, 4.10365959439282e-171];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-8.635914171123053e-73, -2.2921151726480557e271, -2.0891583105441252e36];
        const b = [1.054185843816969e-179, -1.0939500700055288e-128, -8.811785408845194e249];
        const c = [-2.0891583105441252e36, -2.2921151726480557e271, -8.635914171123053e-73];
        const d = [-8.811785408845194e249, -1.0939500700055288e-128, 1.054185843816969e-179];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [156370639540.27606, 2.9429601203613635e-146, 1.9864760976756418e26];
        const b = [-3.106153342109028e-87, 6.908319932666851e276, -7.310406837527741e-190];
        const c = [1.9864760976756418e26, 2.9429601203613635e-146, 156370639540.27606];
        const d = [-7.310406837527741e-190, 6.908319932666851e276, -3.106153342109028e-87];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-8.327707664159363e-214, -7.1709067263856245e-25, 2.0967281538795398e256];
        const b = [6.323451552696543e-99, -3.497351123204683e-68, -2.1544948999388707e255];
        const c = [2.09672815387954e256, -7.1709067263856245e-25, -8.327707664159363e-214];
        const d = [-2.1544948999388707e255, -3.497351123204683e-68, 6.323451552696543e-99];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-4.434000212409423e214, 3.1596317347793196e298, -2.161271272942385e128];
        const b = [-3.784635614939183e193, -7.804993747466923e-78, -1.0950971802489213e173];
        const c = [-2.161271272942385e128, 3.1596317347793196e298, -4.434000212409423e214];
        const d = [-1.0950971802489213e173, -7.804993747466923e-78, -3.784635614939183e193];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-5.287925706782275e154, -9.522837337002527e163, 9.834648366289226e286];
        const b = [-2.891338831299114e236, 1.4778898240453427e32, 1.3434992729987785e-253];
        const c = [9.834648366289226e286, -9.522837337002527e163, -5.287925706782275e154];
        const d = [1.3434992729987785e-253, 1.4778898240453427e32, -2.891338831299114e236];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.887930550209533e-14, -3.482801980103146e209, 1.5504064925196305e-152];
        const b = [4.417604954919729e-153, 3.884732345762948e183, 2.2372113393942311e49];
        const c = [1.5504064925196305e-152, -3.482801980103146e209, -2.887930550209533e-14];
        const d = [2.2372113393942311e49, 3.884732345762948e183, 4.417604954919729e-153];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.3314182754832916e294, -1.734454275350144e189, 4.329592554750494e-174];
        const b = [-4.4355181974059187e24, -8.706169315211029e-100, 2.4567238203566192e-163];
        const c = [4.329592554750494e-174, -1.734454275350144e189, -2.3314182754832916e294];
        const d = [2.4567238203566192e-163, -8.706169315211029e-100, -4.4355181974059187e24];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.2234995570875249e-86, 1.0085308407370102e241, -6.209540212636474e-266];
        const b = [-6.709039544735246e-225, 1.5273302145698288e300, 4.7169029299364664e-60];
        const c = [-6.209540212636474e-266, 1.0085308407370102e241, 1.2234995570875249e-86];
        const d = [4.7169029299364664e-60, 1.5273302145698288e300, -6.709039544735246e-225];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-9.891295817215243e-31, 5.500380852985376e-186, 2.16890840282904e-236];
        const b = [5.0828048084171654e107, 4.307204526279087e-59, 4.716024991322685e65];
        const c = [2.1689084028290404e-236, 5.500380852985376e-186, -9.891295817215243e-31];
        const d = [4.716024991322685e65, 4.307204526279087e-59, 5.0828048084171654e107];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-6.7668059596215e19, 1.5641329023416854e146, 3.981277032800193e-138];
        const b = [-3.783978742810052e-228, -2.196131031162198e-287, -2.5099439586551775e-303];
        const c = [3.981277032800193e-138, 1.5641329023416854e146, -6.7668059596215e19];
        const d = [-2.5099439586551775e-303, -2.196131031162198e-287, -3.783978742810052e-228];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.6216567622494302e-49, -2.6659985544087887e-153, 1.8912611304060047e41];
        const b = [-3.217007872489622e242, 6.585757004566405e148, 3.0577087374152077e34];
        const c = [1.891261130406005e41, -2.6659985544087887e-153, -2.6216567622494302e-49];
        const d = [3.0577087374152077e34, 6.585757004566405e148, -3.217007872489622e242];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [2.302528121328767e299, 2.7633778676638103e27, -3.6782505296868256e-145];
        const b = [5.120012824121688e-113, -1.1579994880732844e162, 7.672674438204271e-53];
        const c = [-3.678250529686825e-145, 2.7633778676638103e27, 2.302528121328767e299];
        const d = [7.672674438204271e-53, -1.1579994880732844e162, 5.120012824121688e-113];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.8379018765482298e28, 2.5738619119041146e-299, -5.594060372483769e86];
        const b = [9.633032493128977e171, -1.7772925205265264e-113, 9.861783727346895e-259];
        const c = [-5.594060372483769e86, 2.5738619119041146e-299, -1.8379018765482298e28];
        const d = [9.861783727346895e-259, -1.7772925205265264e-113, 9.633032493128977e171];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [3.5033053059490993e-162, -4.92189139178223e-52, -4.132818686811541e-83];
        const b = [9.504037573335457e120, 5.177088657206619e-203, 0.006244733276833983];
        const c = [-4.132818686811541e-83, -4.92189139178223e-52, 3.5033053059490993e-162];
        const d = [0.006244733276833983, 5.177088657206619e-203, 9.504037573335457e120];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [8.028572691186635e-68, 1.4219705351013025e80, 4.0411688087443907e-14];
        const b = [1.0647583107635628e19, -3.870967311742317e183, 3.057818856172113e126];
        const c = [4.041168808744391e-14, 1.4219705351013025e80, 8.028572691186635e-68];
        const d = [3.057818856172113e126, -3.870967311742317e183, 1.0647583107635628e19];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.6388861417057348e236, 1.3356707897539135e226, -7.451402145967351e282];
        const b = [1.259656252451255e-306, -5.840435249352981e-5, 7.439384762272237e-278];
        const c = [-7.45140214596735e282, 1.3356707897539135e226, -1.6388861417057348e236];
        const d = [7.439384762272237e-278, -5.840435249352981e-5, 1.259656252451255e-306];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.02429579437431e-302, 2.6374754329331488e284, 7.2803653397367e-35];
        const b = [2.1518578519027402e46, 1.4181302469316797e-103, -1.402197223265335e61];
        const c = [7.2803653397367e-35, 2.6374754329331488e284, -2.02429579437431e-302];
        const d = [-1.402197223265335e61, 1.4181302469316797e-103, 2.1518578519027402e46];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.168239879352888e86, -8.846761647083806e105, 1.864791642768753e246];
        const b = [-6.444345914464216e276, -1.4904665937730102e34, 2.212459640083598e131];
        const c = [1.864791642768753e246, -8.846761647083806e105, -1.168239879352888e86];
        const d = [2.212459640083598e131, -1.4904665937730102e34, -6.444345914464216e276];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-3.1248343338616864e296, -7.836965113076932e160, -1.9210199084119362e-258];
        const b = [-9.021063437407076e223, 9.15083939199275e145, 2.0532808576291277e24];
        const c = [-1.9210199084119362e-258, -7.836965113076932e160, -3.1248343338616864e296];
        const d = [2.0532808576291277e24, 9.15083939199275e145, -9.021063437407076e223];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [3.5460897807420124e-244, 4.389573796773729e191, 1.0071707758881822e164];
        const b = [-2.107462531991913e250, -14058363.537870537, 9.451374523810043e40];
        const c = [1.0071707758881824e164, 4.389573796773729e191, 3.5460897807420124e-244];
        const d = [9.451374523810043e40, -14058363.537870537, -2.107462531991913e250];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.4992743992701266e-223, 2.392169523719485e194, -2.9565857111066035e-78];
        const b = [-1.0650275024946256e86, 8.871692759694876e221, 7.26093079336267e-82];
        const c = [-2.9565857111066035e-78, 2.392169523719485e194, -1.4992743992701266e-223];
        const d = [7.26093079336267e-82, 8.871692759694876e221, -1.0650275024946256e86];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-37029629976548.016, 1.649241556997844e128, -2.4503275881830623e-11];
        const b = [-3.818234450345441e16, -5.610261765391521e-215, 2.6255270843430268e222];
        const c = [-2.450327588183062e-11, 1.649241556997844e128, -37029629976548.016];
        const d = [2.6255270843430268e222, -5.610261765391521e-215, -3.818234450345441e16];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.526034505493382e-302, -5.841951201529373e-216, 4.927499960398844e-252];
        const b = [4.696378569587724e-13, -2.875373331834956e-66, -1.0447941061017965e-294];
        const c = [4.927499960398845e-252, -5.841951201529373e-216, 1.526034505493382e-302];
        const d = [-1.0447941061017965e-294, -2.875373331834956e-66, 4.696378569587724e-13];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-4.5199105378897495e-20, -2.948044270866519e247, -7.634426972460252e-159];
        const b = [-4.811027801873958e-29, -4.0690994900774865e222, 1.137686126325878e-149];
        const c = [-7.634426972460251e-159, -2.948044270866519e247, -4.5199105378897495e-20];
        const d = [1.137686126325878e-149, -4.0690994900774865e222, -4.811027801873958e-29];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-7.533108721056443e79, -3.817668408516187e-8, -2.5824635229607975e-308];
        const b = [3.3630769857724696e-134, -5.888143029477434e294, 7.969558911594138e286];
        const c = [-2.5824635229607975e-308, -3.817668408516187e-8, -7.533108721056443e79];
        const d = [7.969558911594138e286, -5.888143029477434e294, 3.3630769857724696e-134];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.5619033323495564e-49, -1.167835175508038e-170, -9.192489923295339e271];
        const b = [-2.16745661817175e-154, 5.413649079168143e278, -8.76415508837041e-239];
        const c = [-9.192489923295339e271, -1.167835175508038e-170, -2.5619033323495564e-49];
        const d = [-8.76415508837041e-239, 5.413649079168143e278, -2.16745661817175e-154];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-8.518256968263574e235, 2.349175345066678e112, 1.420204989257428e152];
        const b = [-2.3637431474487104e148, 4.6482514185072435e-251, -2.0943586573885556e141];
        const c = [1.420204989257428e152, 2.349175345066678e112, -8.518256968263574e235];
        const d = [-2.0943586573885556e141, 4.6482514185072435e-251, -2.3637431474487104e148];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.2862812584337623e-38, -1.7185993325000922e-47, 5.849816700250824e53];
        const b = [1.6372755820222498e197, 9.555870343262285e36, 1.0624989827469976e-190];
        const c = [5.8498167002508245e53, -1.7185993325000922e-47, -2.2862812584337623e-38];
        const d = [1.0624989827469976e-190, 9.555870343262285e36, 1.6372755820222498e197];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.3945483588207292e-295, 2.5366069193357465e-194, -2.7222072533031292e-42];
        const b = [4.751658985949533e-61, -1.0180676619526008e77, 2.574223441907958e-291];
        const c = [-2.7222072533031292e-42, 2.5366069193357465e-194, -1.3945483588207292e-295];
        const d = [2.574223441907958e-291, -1.0180676619526008e77, 4.751658985949533e-61];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.3276953161459138e-272, -5.861164577954027e-154, -4.482125172432278e26];
        const b = [2.247972536741286e-52, -1.486081391800536e118, -2.0725930766279935e154];
        const c = [-4.482125172432278e26, -5.861164577954027e-154, -1.3276953161459138e-272];
        const d = [-2.0725930766279935e154, -1.486081391800536e118, 2.247972536741286e-52];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [5.848944844283431e216, 1.4234210094615736e203, 4032161017526829.0];
        const b = [-1.791785431344588e186, -2.2119864898105388e74, -1.14078723336527e-31];
        const c = [4032161017526829.5, 1.4234210094615736e203, 5.848944844283431e216];
        const d = [-1.14078723336527e-31, -2.2119864898105388e74, -1.791785431344588e186];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [912.56700158039, -9.567236000228817e271, 2.3180483246369073e259];
        const b = [-1.4991235825123143e86, -8.589890000976125e-26, -3.0453767699244856e54];
        const c = [2.3180483246369077e259, -9.567236000228817e271, 912.56700158039];
        const d = [-3.0453767699244856e54, -8.589890000976125e-26, -1.4991235825123143e86];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.681667504411073e184, 2.0918014468410278e226, -9.755366645761248e-238];
        const b = [-1.1503671918306658e-179, -415285.350320021, 4.365412914994951e-173];
        const c = [-9.755366645761247e-238, 2.0918014468410278e226, 1.681667504411073e184];
        const d = [4.365412914994951e-173, -415285.350320021, -1.1503671918306658e-179];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.2044549719447846e95, -3.073706866551623e55, 5.89645340732536e-216];
        const b = [6.827349887712135e-160, 2.384690870642391e293, -4.919101622000417e-230];
        const c = [5.89645340732536e-216, -3.073706866551623e55, -2.2044549719447846e95];
        const d = [-4.919101622000417e-230, 2.384690870642391e293, 6.827349887712135e-160];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [5.5906749606810175e-70, -8.217169291487853e-206, -1.8802707981275317e199];
        const b = [8.959744997218519e-177, -938297380.4369208, -6.6748624043441806e-186];
        const c = [-1.8802707981275317e199, -8.217169291487853e-206, 5.5906749606810175e-70];
        const d = [-6.6748624043441806e-186, -938297380.4369208, 8.959744997218519e-177];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-8.188306291369478e-41, -4.2094316200983425e240, -4.0201957809971724e24];
        const b = [4192662724140.9, -7.435433781321824e-287, -8.797566144065349e-196];
        const c = [-4.020195780997172e24, -4.2094316200983425e240, -8.188306291369478e-41];
        const d = [-8.797566144065349e-196, -7.435433781321824e-287, 4192662724140.9];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-4.480961844742674e-67, -1.6931555774180294e168, -3.538419638392135e-7];
        const b = [-3737317098717.279, 1.1503888887627438e-281, -1.6964531429584368e-140];
        const c = [-3.5384196383921343e-7, -1.6931555774180294e168, -4.480961844742674e-67];
        const d = [-1.6964531429584368e-140, 1.1503888887627438e-281, -3737317098717.279];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.1024691873069044e59, -3.058839104887866e-211, 3.307284205811382e156];
        const b = [1.6139976372998018e241, 6.104460781737956e287, -2.6735178200544814e295];
        const c = [3.307284205811382e156, -3.058839104887866e-211, -2.1024691873069044e59];
        const d = [-2.6735178200544814e295, 6.104460781737956e287, 1.6139976372998018e241];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.8267228831350763e-10, -1.3294525417231263e22, 1.41370785338434e-44];
        const b = [-2.3881370539715597e-10, -6.11693601216639e222, -9.02136125966963e98];
        const c = [1.41370785338434e-44, -1.3294525417231263e22, 1.8267228831350763e-10];
        const d = [-9.02136125966963e98, -6.11693601216639e222, -2.3881370539715597e-10];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [6.5666186473133036e-279, -1.724935346799947e239, 8.259465011350671e-20];
        const b = [3.80512497255547e-47, 9.614801806749995e89, -1.8100955955170025e-295];
        const c = [8.259465011350672e-20, -1.724935346799947e239, 6.5666186473133036e-279];
        const d = [-1.8100955955170025e-295, 9.614801806749995e89, 3.80512497255547e-47];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.3217584433258385e-30, -1049.4592368623485, 8.930612551090334e189];
        const b = [4.959013117912521e-259, -6.019488209032722e-175, 1.0580278806446523e273];
        const c = [8.930612551090336e189, -1049.4592368623485, 1.3217584433258385e-30];
        const d = [1.0580278806446523e273, -6.019488209032722e-175, 4.959013117912521e-259];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [3.5739854034709485e255, -1.4439099842973621e-173, 3.6628671063036774e-301];
        const b = [-6.938521340565919e-306, -8.090974491372738e-83, 8.214725870877582e124];
        const c = [3.6628671063036774e-301, -1.4439099842973621e-173, 3.5739854034709485e255];
        const d = [8.214725870877582e124, -8.090974491372738e-83, -6.938521340565919e-306];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.9427919548364823e-64, 4.6421280350659795e-200, 2.2136410525626715e111];
        const b = [2.698461715045003e-301, 4.59698444510167e161, -4.4047984526472054e76];
        const c = [2.2136410525626715e111, 4.6421280350659795e-200, -1.9427919548364823e-64];
        const d = [-4.4047984526472054e76, 4.59698444510167e161, 2.698461715045003e-301];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.0159990668195278e34, 9.44239367758982e188, 2.6923378463840343e242];
        const b = [-5.7022307854745505e-186, 8.949160031898202e-39, 1.4596342030764408e-260];
        const c = [2.692337846384035e242, 9.44239367758982e188, -1.0159990668195278e34];
        const d = [1.4596342030764408e-260, 8.949160031898202e-39, -5.7022307854745505e-186];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-4.6025785042776995e-112, -3.3982412543555115e-75, -4.990714906012064e-72];
        const b = [-9.926080245062175e-76, 3.267104940685793e58, -6.089146997306804e38];
        const c = [-4.990714906012063e-72, -3.3982412543555115e-75, -4.6025785042776995e-112];
        const d = [-6.089146997306804e38, 3.267104940685793e58, -9.926080245062175e-76];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [8.046655375179653e-65, -7.48875099864728e-219, 1.0477627644075346e-232];
        const b = [-1.4463522290885147e269, -1.126824710146575e-215, -1.463048907309319e26];
        const c = [1.0477627644075348e-232, -7.48875099864728e-219, 8.046655375179653e-65];
        const d = [-1.463048907309319e26, -1.126824710146575e-215, -1.4463522290885147e269];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.3636848483216708e-43, 2.3608033974668065e-210, -8.820539592262534e257];
        const b = [9.500926248167568e300, -4.330886398060141e127, -1.691721554793475e166];
        const c = [-8.820539592262534e257, 2.3608033974668065e-210, 1.3636848483216708e-43];
        const d = [-1.691721554793475e166, -4.330886398060141e127, 9.500926248167568e300];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [6.073626036816549e81, -5.704919016020129e-98, -1.3223347581216187e254];
        const b = [2.453876792396918e-27, 2.5799808764177804e-274, 1.8545666437295152e81];
        const c = [-1.3223347581216185e254, -5.704919016020129e-98, 6.073626036816549e81];
        const d = [1.8545666437295152e81, 2.5799808764177804e-274, 2.453876792396918e-27];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.32582264182545e-107, 489427782.7051516, 1.7304362448639115e56];
        const b = [9.206553920795467e-107, -4.960715641930412e76, -1.0487248718450928e-268];
        const c = [1.7304362448639117e56, 489427782.7051516, 1.32582264182545e-107];
        const d = [-1.0487248718450928e-268, -4.960715641930412e76, 9.206553920795467e-107];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [7.6544135717655945e236, -3.709403352377018e289, 5.385254767690972e-36];
        const b = [5.061863804142171e99, -2.8735475809633844e-73, 9.646705758280166e-174];
        const c = [5.385254767690972e-36, -3.709403352377018e289, 7.6544135717655945e236];
        const d = [9.646705758280166e-174, -2.8735475809633844e-73, 5.061863804142171e99];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-3.0273388197585267e-180, -1.5325972542565537e-49, 1.0786498900414724e-68];
        const b = [-7.939566010440819e51, 382816.60015246866, 5.229246977130859e-277];
        const c = [1.0786498900414724e-68, -1.5325972542565537e-49, -3.0273388197585267e-180];
        const d = [5.229246977130859e-277, 382816.60015246866, -7.939566010440819e51];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-5.373790215488933e286, -1.149925120463921e-224, 6.286501672707296e112];
        const b = [-5.135799429629261e38, -2.4162248881475784e206, -5.707487801905414e118];
        const c = [6.286501672707297e112, -1.149925120463921e-224, -5.373790215488933e286];
        const d = [-5.707487801905414e118, -2.4162248881475784e206, -5.135799429629261e38];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [5.653862523601913e-142, 6.062238983974888e-85, 2.217912971386374e74];
        const b = [-2.6793932224169935e113, 1.2615586281840585e282, -1.8716734721915232e-80];
        const c = [2.2179129713863742e74, 6.062238983974888e-85, 5.653862523601913e-142];
        const d = [-1.8716734721915232e-80, 1.2615586281840585e282, -2.6793932224169935e113];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-6.889089231537293e-293, -2.0347311987569535e171, 3.6264041126628574e229];
        const b = [1.7450262886181504e292, 2.81455495317803e45, -9.457024220117796e-128];
        const c = [3.626404112662858e229, -2.0347311987569535e171, -6.889089231537293e-293];
        const d = [-9.457024220117796e-128, 2.81455495317803e45, 1.7450262886181504e292];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.73895734707881e81, -1.7692403215021547e45, 1.80284161324524e31];
        const b = [-1.0998012389621106e163, 1.5828894912312745e-301, 3.985620037743025e-270];
        const c = [1.80284161324524e31, -1.7692403215021547e45, -2.73895734707881e81];
        const d = [3.985620037743025e-270, 1.5828894912312745e-301, -1.0998012389621106e163];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [9.252774453237434e187, -2.7881572409386137e33, 6651269422.587515];
        const b = [-6.3604058685793494e35, -6.0130747177145856e184, -1.3183918331327326e-32];
        const c = [6651269422.587516, -2.7881572409386137e33, 9.252774453237434e187];
        const d = [-1.3183918331327326e-32, -6.0130747177145856e184, -6.3604058685793494e35];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [8.719589361248847e-12, 6.392844634878481e126, 2.3851459224178676e-182];
        const b = [3.45511137747561e125, -1.489080173002527e157, 3.490719091728427e-215];
        const c = [2.3851459224178676e-182, 6.392844634878481e126, 8.719589361248847e-12];
        const d = [3.490719091728427e-215, -1.489080173002527e157, 3.45511137747561e125];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [9.63186236134496e33, -5.603302192052534e-209, -1.0099855135515438e122];
        const b = [137330845443024.66, -6.886296312932242e98, 3.663281242456154e185];
        const c = [-1.0099855135515436e122, -5.603302192052534e-209, 9.63186236134496e33];
        const d = [3.663281242456154e185, -6.886296312932242e98, 137330845443024.66];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [9.963894764801855e239, 5.540737141593207e-84, -6.715721321589593e-158];
        const b = [2.4875553264364085e-149, 10351057.530601803, 2.1888406572232317e241];
        const c = [-6.715721321589592e-158, 5.540737141593207e-84, 9.963894764801855e239];
        const d = [2.1888406572232317e241, 10351057.530601803, 2.4875553264364085e-149];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [2.209421601661564e-240, -4.7431671159534036e-279, 1.5282246085427338e283];
        const b = [-3.913755715086739e-183, -9.027497487254796e-37, 6.749448464245023e17];
        const c = [1.528224608542734e283, -4.7431671159534036e-279, 2.209421601661564e-240];
        const d = [6.749448464245023e17, -9.027497487254796e-37, -3.913755715086739e-183];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [3.86856580728549e-228, 2.0096923927562048e-224, 1.0356433085953216e198];
        const b = [-1.6438540227909018e-195, 7.608078772381668e-116, 2.647240057410796e252];
        const c = [1.0356433085953218e198, 2.0096923927562048e-224, 3.86856580728549e-228];
        const d = [2.647240057410796e252, 7.608078772381668e-116, -1.6438540227909018e-195];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [2.0492777077659714e278, -4.818062699416467e64, -3.4196119016024177e-53];
        const b = [2.1283384164479734e30, 1.1386044197971944e61, -1.4722718101859814e-137];
        const c = [-3.419611901602417e-53, -4.818062699416467e64, 2.0492777077659714e278];
        const d = [-1.4722718101859814e-137, 1.1386044197971944e61, 2.1283384164479734e30];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-3.1266634428493766e-70, 2.141785954412703e-199, -3.887986011861374];
        const b = [-3.363298910964207e-271, 5.955499516042985e24, 2.659895377376433e287];
        const c = [-3.8879860118613734, 2.141785954412703e-199, -3.1266634428493766e-70];
        const d = [2.659895377376433e287, 5.955499516042985e24, -3.363298910964207e-271];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [6.278283262684477e-197, 3.686117128188168e180, 7.725485968897455e154];
        const b = [3.7399111249041893e49, -4.728864832846642e-308, 1.2123273892551458e-290];
        const c = [7.725485968897455e154, 3.686117128188168e180, 6.278283262684477e-197];
        const d = [1.2123273892551458e-290, -4.728864832846642e-308, 3.7399111249041893e49];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-3.254207654331314e-154, -5.2528429946490375e-11, -6.844661266847007e-262];
        const b = [2.719124369062877e-184, -2.1476628849356913e115, -2.4185566941997108e-64];
        const c = [-6.844661266847005e-262, -5.2528429946490375e-11, -3.254207654331314e-154];
        const d = [-2.4185566941997108e-64, -2.1476628849356913e115, 2.719124369062877e-184];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-3.5673729303152874e-296, 4.691128296463148e247, 9.527457854140752e-287];
        const b = [-9.64451777123454e-207, 2.970562930528403e-270, 1.2440040212332245e163];
        const c = [9.527457854140753e-287, 4.691128296463148e247, -3.5673729303152874e-296];
        const d = [1.2440040212332245e163, 2.970562930528403e-270, -9.64451777123454e-207];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.9807266374656433e-155, 1.199994975063247e-244, 2.635117476552629e132];
        const b = [-5.93407629160581e-281, 4.0582002807967865e27, 7.191693432158694e-236];
        const c = [2.635117476552629e132, 1.199994975063247e-244, -2.9807266374656433e-155];
        const d = [7.191693432158694e-236, 4.0582002807967865e27, -5.93407629160581e-281];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [6.551415432113061e137, 1.1847088896948594e-262, -5.568033497985254e-263];
        const b = [2.619881637807445e138, 4.715589540548789e299, -2.933650653311164e103];
        const c = [-5.568033497985254e-263, 1.1847088896948594e-262, 6.551415432113061e137];
        const d = [-2.933650653311164e103, 4.715589540548789e299, 2.619881637807445e138];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [8.849432163289309e30, -2.297282152300986e118, 1.4650996667511682e-248];
        const b = [-5.2671833258808574e280, 9.673699319198581e109, -3.843021644661447e20];
        const c = [1.4650996667511682e-248, -2.297282152300986e118, 8.849432163289309e30];
        const d = [-3.843021644661447e20, 9.673699319198581e109, -5.2671833258808574e280];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [3.5017617691527496e202, -3.2826899104620757e307, -9.28129474190463e25];
        const b = [1.635926122966809e76, 1.1816101326490265e-24, -1.5069347226870568e-197];
        const c = [-9.28129474190463e25, -3.2826899104620757e307, 3.5017617691527496e202];
        const d = [-1.5069347226870568e-197, 1.1816101326490265e-24, 1.635926122966809e76];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-9.99670995097728e219, -2.584450227218256e-201, -3.8974436667688824e185];
        const b = [-2.9445665138852157e215, 3.759621024222743e184, -2.0319956383311887e65];
        const c = [-3.897443666768882e185, -2.584450227218256e-201, -9.99670995097728e219];
        const d = [-2.0319956383311887e65, 3.759621024222743e184, -2.9445665138852157e215];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.1002561695059921e-160, -4.8063867304838206e-48, 1.303971719179329e-114];
        const b = [7.399099658430509e193, 1.353401191293462e-126, 3.816117871950293e-93];
        const c = [1.303971719179329e-114, -4.8063867304838206e-48, 1.1002561695059921e-160];
        const d = [3.816117871950293e-93, 1.353401191293462e-126, 7.399099658430509e193];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [9.35118268464393e127, -4.853373274800273e169, -9.882023610343293e-111];
        const b = [-1.3657830319504774e145, -6.1166030323840156e-260, -3.394216854917071e-114];
        const c = [-9.882023610343293e-111, -4.853373274800273e169, 9.35118268464393e127];
        const d = [-3.394216854917071e-114, -6.1166030323840156e-260, -1.3657830319504774e145];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.363745428118875e243, -4.699148814937086e74, 4.732784255939144e-274];
        const b = [1094681232127528.1, -3.250542628590023e249, -2.3763081618200166e-306];
        const c = [4.732784255939145e-274, -4.699148814937086e74, -2.363745428118875e243];
        const d = [-2.3763081618200166e-306, -3.250542628590023e249, 1094681232127528.1];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-4.557544948161205e-44, -5.0038190333182345e222, 1.4133904788826123e206];
        const b = [6.3951144892313e192, 5.57316543624028e-289, -3.6687785245424276e305];
        const c = [1.4133904788826123e206, -5.0038190333182345e222, -4.557544948161205e-44];
        const d = [-3.6687785245424276e305, 5.57316543624028e-289, 6.3951144892313e192];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-9.095570976156922e145, -1.5143443119163289e112, -1.7975338669153815e299];
        const b = [-9.099081091208101e148, -1.9616320197288036e127, -2.2730011661808731e-23];
        const c = [-1.7975338669153815e299, -1.5143443119163289e112, -9.095570976156922e145];
        const d = [-2.2730011661808731e-23, -1.9616320197288036e127, -9.099081091208101e148];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [3.766411139148256e180, -1.6414881005630733e-218, -3.4230878325114473e-130];
        const b = [1.1559229224531884e-195, -2.5951807901804853e-88, -4.522168836241905e-83];
        const c = [-3.423087832511447e-130, -1.6414881005630733e-218, 3.766411139148256e180];
        const d = [-4.522168836241905e-83, -2.5951807901804853e-88, 1.1559229224531884e-195];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.4574377697604714e-35, -6.463382266115943e-304, -5.353766442554794e66];
        const b = [-8.795184238463739e-296, -3.943486032852425e-42, -1.5714409605791957e-237];
        const c = [-5.353766442554794e66, -6.463382266115943e-304, 1.4574377697604714e-35];
        const d = [-1.5714409605791957e-237, -3.943486032852425e-42, -8.795184238463739e-296];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-8.851011408386986e300, -1.332242696846038e133, 4.352500668371336e-158];
        const b = [-4.64390276109815e298, 1.6609927963608356e-217, 8.18838899733576e-15];
        const c = [4.352500668371337e-158, -1.332242696846038e133, -8.851011408386986e300];
        const d = [8.18838899733576e-15, 1.6609927963608356e-217, -4.64390276109815e298];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.1814575224444966e-209, 2.264104370531569e-282, -1.1150563239510536e122];
        const b = [3.7650896768625687e-238, 4.678190736307197e-37, -6.106295325163215e-227];
        const c = [-1.1150563239510536e122, 2.264104370531569e-282, -2.1814575224444966e-209];
        const d = [-6.106295325163215e-227, 4.678190736307197e-37, 3.7650896768625687e-238];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.870718348342516e303, 5.355066778565593e79, 7.1047229464199355e143];
        const b = [6.973203988962149e-131, 7.072490275966115e53, -9.359199822145986e-32];
        const c = [7.104722946419936e143, 5.355066778565593e79, -1.870718348342516e303];
        const d = [-9.359199822145986e-32, 7.072490275966115e53, 6.973203988962149e-131];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-6.063833555967313e307, -6.717048847604306e-62, -1.2548226872032069e171];
        const b = [-1.3341206415889674e-76, -2.7693353539388978e113, 1.6512110102836148e-276];
        const c = [-1.2548226872032067e171, -6.717048847604306e-62, -6.063833555967313e307];
        const d = [1.6512110102836148e-276, -2.7693353539388978e113, -1.3341206415889674e-76];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [9.769946237030511e136, -3.229647969875596e120, 2.388838702254801e-234];
        const b = [-1.3302416306003839e268, 9.631195186981824e127, 2.0251918679222008e51];
        const c = [2.388838702254801e-234, -3.229647969875596e120, 9.769946237030511e136];
        const d = [2.0251918679222008e51, 9.631195186981824e127, -1.3302416306003839e268];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [5.6712555716060416e63, -2.3252258676737034e45, 1.8105833827883488e293];
        const b = [4.84080060765394e-55, 1.460923970653984e-82, -2.0094141818246804e-89];
        const c = [1.8105833827883488e293, -2.3252258676737034e45, 5.6712555716060416e63];
        const d = [-2.0094141818246804e-89, 1.460923970653984e-82, 4.84080060765394e-55];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [3.2632386277172146e113, 822030530235.6328, -427611284612865.25];
        const b = [1.430199247570012e-271, -4.8318079396690985e207, 4.15165376702233e194];
        const c = [-427611284612865.25, 822030530235.6328, 3.2632386277172146e113];
        const d = [4.15165376702233e194, -4.8318079396690985e207, 1.430199247570012e-271];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-7.298225078890892e-303, 2.6652225767992804e260, -5.548719138379631e237];
        const b = [5.069126643477791e116, -3.5415066264282298e-6, 8.041388727010467e-249];
        const c = [-5.548719138379631e237, 2.6652225767992804e260, -7.298225078890892e-303];
        const d = [8.041388727010467e-249, -3.5415066264282298e-6, 5.069126643477791e116];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-3.836866139806871e-276, 3.9513642901139294e-227, -7.884740427961133e118];
        const b = [2.2467972444938827e201, 1.731084585090146e71, 6.628627536346151e306];
        const c = [-7.884740427961132e118, 3.9513642901139294e-227, -3.836866139806871e-276];
        const d = [6.628627536346151e306, 1.731084585090146e71, 2.2467972444938827e201];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.6865603446362646e-289, -2.4633077778070447e-256, -9.486279443618531e148];
        const b = [-1.226192064413162e50, 7.946911456150178e132, 1.7781100367343035e-103];
        const c = [-9.486279443618531e148, -2.4633077778070447e-256, -1.6865603446362646e-289];
        const d = [1.7781100367343035e-103, 7.946911456150178e132, -1.226192064413162e50];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [7.266231572077508e-98, -4.0477042677093857e31, 6.937616399247071e152];
        const b = [-4.754847590758826e178, 2.5038188047521913e-96, -2.2441731460955568e287];
        const c = [6.9376163992470715e152, -4.0477042677093857e31, 7.266231572077508e-98];
        const d = [-2.2441731460955568e287, 2.5038188047521913e-96, -4.754847590758826e178];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [2.1178578756912015e259, 6.460555602069993e84, 3.3327683591193764e-250];
        const b = [-4.973469285233587e59, 3.1475194734845973e34, -9.451974977204822e138];
        const c = [3.3327683591193764e-250, 6.460555602069993e84, 2.1178578756912015e259];
        const d = [-9.451974977204822e138, 3.1475194734845973e34, -4.973469285233587e59];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-8.675341192266739e204, 6.082547678266767e-304, -8.14708476982746e-198];
        const b = [1.8119699753428124e-281, 2.4824130679647763e135, -8.644993700747106e182];
        const c = [-8.14708476982746e-198, 6.082547678266767e-304, -8.675341192266739e204];
        const d = [-8.644993700747106e182, 2.4824130679647763e135, 1.8119699753428124e-281];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-8.426717333077443e101, 1.4206059182560479e-39, 2.6502199440586542e90];
        const b = [4.8234896215259915e206, -2.0956110984592537e-215, 1.7699016856366615e308];
        const c = [2.6502199440586542e90, 1.4206059182560479e-39, -8.426717333077443e101];
        const d = [1.7699016856366615e308, -2.0956110984592537e-215, 4.8234896215259915e206];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-8.934474008915047e-99, 48.09964967031308, 4.458404616320994e-190];
        const b = [2.6026000210326077e-37, 2.5645371456930655e191, 6.2645443894576614e88];
        const c = [4.4584046163209947e-190, 48.09964967031308, -8.934474008915047e-99];
        const d = [6.2645443894576614e88, 2.5645371456930655e191, 2.6026000210326077e-37];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.3373209368641604e-258, -1.0564099133882231e-186, 2.3581323666830053e-278];
        const b = [1.597407784139093e302, 5.891198862102948e159, -9.041398953403664e-308];
        const c = [2.3581323666830056e-278, -1.0564099133882231e-186, -1.3373209368641604e-258];
        const d = [-9.041398953403664e-308, 5.891198862102948e159, 1.597407784139093e302];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.3992410330700822e-8, -4.626620926782457e174, 1.303014420956024e-199];
        const b = [4.382978924227435e227, 3.5003833027628964e177, 7.54387973392806e105];
        const c = [1.3030144209560243e-199, -4.626620926782457e174, -1.3992410330700822e-8];
        const d = [7.54387973392806e105, 3.5003833027628964e177, 4.382978924227435e227];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.3846481673997987e-89, -1.0221104126689013e110, -7.450296469501975e-13];
        const b = [3.7547141079424943e-219, -3.403330305898678e219, 3.7467335522460442e84];
        const c = [-7.450296469501974e-13, -1.0221104126689013e110, 1.3846481673997987e-89];
        const d = [3.7467335522460442e84, -3.403330305898678e219, 3.7547141079424943e-219];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-4.048139984788776e229, -3.003344809759648e154, 8.642635222137464e19];
        const b = [1.6219028559052327e-117, -4.885537371297558e-74, 1.1721265360371622e234];
        const c = [8.642635222137466e19, -3.003344809759648e154, -4.048139984788776e229];
        const d = [1.1721265360371622e234, -4.885537371297558e-74, 1.6219028559052327e-117];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.1004224800346131e46, 1.096756575066893e-281, -1.7443687478683024e297];
        const b = [5.757512851923804e233, 3.2781976877151207e-105, -1.0060888516783625e-215];
        const c = [-1.7443687478683024e297, 1.096756575066893e-281, -1.1004224800346131e46];
        const d = [-1.0060888516783625e-215, 3.2781976877151207e-105, 5.757512851923804e233];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.9028372988848833e-154, -1.74492410207272e-102, 1.463924721844572e120];
        const b = [0.06091010532553508, 7.587021635680646e64, 5.396552675099129e-96];
        const c = [1.4639247218445724e120, -1.74492410207272e-102, -1.9028372988848833e-154];
        const d = [5.396552675099129e-96, 7.587021635680646e64, 0.06091010532553508];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [10786751928.8287, 1.1853288997532292e130, -4.492670111102802e-218];
        const b = [1.739797309002742e201, 4.5050435358884367e-302, 2.1185633028958276e307];
        const c = [-4.492670111102802e-218, 1.1853288997532292e130, 10786751928.8287];
        const d = [2.1185633028958276e307, 4.5050435358884367e-302, 1.739797309002742e201];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.2639633496779805e227, 2.716196806183847e-261, 7.014626882321697e-29];
        const b = [-3.564166579112528e148, -3.296632218635107e-137, 1.6100090279922678e-167];
        const c = [7.014626882321698e-29, 2.716196806183847e-261, 1.2639633496779805e227];
        const d = [1.6100090279922678e-167, -3.296632218635107e-137, -3.564166579112528e148];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [7.939925946558683e-235, -3.7027233533221286e292, 6.753519147737518e-172];
        const b = [-7.089709296437491e-227, -6.423563400831095e-247, -2.0189242253811782e77];
        const c = [6.753519147737518e-172, -3.7027233533221286e292, 7.939925946558683e-235];
        const d = [-2.0189242253811782e77, -6.423563400831095e-247, -7.089709296437491e-227];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.5158901265088205e62, -4.0961806717032305e-46, -7.169432748039375e-282];
        const b = [-1.0974641076948373e-278, -3.384179011442793e-87, -6.4627611491937726e159];
        const c = [-7.169432748039373e-282, -4.0961806717032305e-46, 1.5158901265088205e62];
        const d = [-6.4627611491937726e159, -3.384179011442793e-87, -1.0974641076948373e-278];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [3.3471251948763244e169, 5.2602430096905754e215, -3.881685893351429e111];
        const b = [-2.524614767351626e263, 1.6676490307615822e-23, 3.0447688533991904e-279];
        const c = [-3.881685893351429e111, 5.2602430096905754e215, 3.3471251948763244e169];
        const d = [3.0447688533991904e-279, 1.6676490307615822e-23, -2.524614767351626e263];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.2871227217196918e-211, -3.7275643375330056e261, -2.6082364586708715e190];
        const b = [-8.85731329574147e304, 1.077627568400393e180, 7.313001994104029e-50];
        const c = [-2.6082364586708715e190, -3.7275643375330056e261, -1.2871227217196918e-211];
        const d = [7.313001994104029e-50, 1.077627568400393e180, -8.85731329574147e304];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-1.0596266871996139e179, 1.0871331253537209e-200, 2.5717969499986814e53];
        const b = [-1.1810754394808284e-231, 2.603934863644151e189, 8.237920734581789e-17];
        const c = [2.5717969499986814e53, 1.0871331253537209e-200, -1.0596266871996139e179];
        const d = [8.237920734581789e-17, 2.603934863644151e189, -1.1810754394808284e-231];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-8.748833219102145e205, 2.6976745536340795e-196, -1.660373892131431e293];
        const b = [-8.179047905857451e-153, 1.487964387477845e-29, -2.768903444601802e169];
        const c = [-1.660373892131431e293, 2.6976745536340795e-196, -8.748833219102145e205];
        const d = [-2.768903444601802e169, 1.487964387477845e-29, -8.179047905857451e-153];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [8.506705697357003e-246, -4.240380601636514e-195, -1.49040976864946e-277];
        const b = [5.3692832574272384e119, 6.538051122012063e-24, 7.073647690855337e-305];
        const c = [-1.49040976864946e-277, -4.240380601636514e-195, 8.506705697357003e-246];
        const d = [7.073647690855337e-305, 6.538051122012063e-24, 5.3692832574272384e119];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-2.871933925917275e59, 2.108146259197579e102, -9.44155167274745e76];
        const b = [-1.8342793286113592e232, 8.672020809438983e-182, -7.277589685220075e115];
        const c = [-9.441551672747449e76, 2.108146259197579e102, -2.871933925917275e59];
        const d = [-7.277589685220075e115, 8.672020809438983e-182, -1.8342793286113592e232];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-7.661516694592852e-245, -6.0860966409044e-30, 8.499820746678424e-210];
        const b = [-1.3179316038489377e-97, 8.944753214306157e34, 2.6566568362627597e-151];
        const c = [8.499820746678425e-210, -6.0860966409044e-30, -7.661516694592852e-245];
        const d = [2.6566568362627597e-151, 8.944753214306157e34, -1.3179316038489377e-97];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-3.8777665439894106e-20, 7.858770688193581e-152, -1.318558486770615e-198];
        const b = [2.2036118392599602e219, 4.983807748152214e-185, -1.4048018595282816e200];
        const c = [-1.318558486770615e-198, 7.858770688193581e-152, -3.8777665439894106e-20];
        const d = [-1.4048018595282816e200, 4.983807748152214e-185, 2.2036118392599602e219];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [-6.003925833053622e-159, 1.0877140861138239e-125, -9.488688361219043e214];
        const b = [1.1043762811904966e-59, 8.240477092035216e-109, 2.378445273493191e-228];
        const c = [-9.48868836121904e214, 1.0877140861138239e-125, -6.003925833053622e-159];
        const d = [2.378445273493191e-228, 8.240477092035216e-109, 1.1043762811904966e-59];
        expectEq(false, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
    {
        const a = [1.1438410623035173e-244, -9.802784456087416e-156, 1.1194754388591927e71];
        const b = [1.1088299133032705e-255, 1.9190134771135117e225, -1.028682691351719e-128];
        const c = [1.1194754388591927e71, -9.802784456087416e-156, 1.1438410623035173e-244];
        const d = [-1.028682691351719e-128, 1.9190134771135117e225, 1.1088299133032705e-255];
        expectEq(true, CsgExactArithmetic.equalSumProducts(a, b, c, d, 3));
    }
});

test("CsgSolidEquivalenceContract_RecognizesEquivalentCrossFamilySolids", () => {
    const sphere = new SimpleBody();
    const sphericalQuadric = new SimpleBody();
    const complement = new SimpleBody();
    const halfspace = new SimpleBody();
    const linearQuadric = new SimpleBody();
    const wrongHalfspace = new SimpleBody();
    sphere.setGeometry(new Sphere(2));
    sphericalQuadric.setGeometry(new QuadricSurface(new Vector3Dd(3, 3, 3), new Vector3Dd(), new Vector3Dd(), -12));
    complement.setGeometry(new QuadricSurface(new Vector3Dd(-3, -3, -3), new Vector3Dd(), new Vector3Dd(), 12));
    halfspace.setGeometry(new PlanarHalfSpace(1, 2, 0, -3));
    linearQuadric.setGeometry(new QuadricSurface(new Vector3Dd(), new Vector3Dd(), new Vector3Dd(2, 4, 0), -6));
    wrongHalfspace.setGeometry(new QuadricSurface(new Vector3Dd(), new Vector3Dd(), new Vector3Dd(-2, -4, 0), 6));
    const a = new CsgBodyOperand(sphere);
    const b = new CsgBodyOperand(sphericalQuadric);
    const c = new CsgBodyOperand(complement);
    const d = new CsgBodyOperand(halfspace);
    const e = new CsgBodyOperand(linearQuadric);
    const f = new CsgBodyOperand(wrongHalfspace);
    expectTrue(a.isSameSolid(b));
    expectTrue(b.isSameSolid(a));
    expectFalse(a.isSameSolid(c));
    expectTrue(d.isSameSolid(e));
    expectTrue(e.isSameSolid(d));
    expectFalse(d.isSameSolid(f));
});

test("CsgSolidEquivalenceContract_DoesNotEquateRoundedCoincidentBoxEndpoints", () => {
    const a = new SimpleBody();
    const b = new SimpleBody();
    a.setGeometry(new Box(new Vector3Dd(0, 0, 0), new Vector3Dd(1, 1, 1)));
    b.setGeometry(new Box(new Vector3Dd(0, 0, 0), new Vector3Dd(0.5, 1, 1)));
    const translation = JavaMath.scalb(1.0, 53);
    a.setPosition(new Vector3Dd(translation, 0, 0));
    b.setPosition(new Vector3Dd(translation, 0, 0));
    expectDoubleEq(translation + 1, translation + 0.5); // Binary64 endpoints round together.
    const aa = new CsgBodyOperand(a);
    const bb = new CsgBodyOperand(b);
    expectFalse(aa.isSameSolid(bb));
    expectFalse(bb.isSameSolid(aa));
});

test("CsgSolidEquivalenceContract_AxisHalfSpacesAllowDifferentTranslationsAndScales", () => {
    const a = new SimpleBody();
    const b = new SimpleBody();
    const c = new SimpleBody();
    const d = new SimpleBody();
    a.setGeometry(new PlanarHalfSpace(1, 0, 0, -2));
    b.setGeometry(new PlanarHalfSpace(2, 0, 0, -2));
    c.setGeometry(new PlanarHalfSpace(-2, 0, 0, 2));
    d.setGeometry(new QuadricSurface(new Vector3Dd(), new Vector3Dd(), new Vector3Dd(2, 0, 0), -2));
    b.setPosition(new Vector3Dd(1, 9, -4));
    c.setPosition(new Vector3Dd(1, 9, -4));
    d.setPosition(new Vector3Dd(1, -3, 4));
    const aa = new CsgBodyOperand(a);
    const bb = new CsgBodyOperand(b);
    const cc = new CsgBodyOperand(c);
    const dd = new CsgBodyOperand(d);
    expectTrue(aa.isSameSolid(bb));
    expectTrue(bb.isSameSolid(aa));
    expectTrue(aa.isSameSolid(dd));
    expectTrue(dd.isSameSolid(aa));
    expectFalse(aa.isSameSolid(cc));
    b.setPosition(new Vector3Dd(1 + 1e-12, 9, -4));
    expectFalse(aa.isSameSolid(bb));
});

test("CsgSolidEquivalenceContract_QuadricMixedTermsAndLinearSignsTransformExactly", () => {
    const a = new SimpleBody();
    const b = new SimpleBody();
    const c = new SimpleBody();
    a.setGeometry(new QuadricSurface(new Vector3Dd(1, 2, 3), new Vector3Dd(4, 5, 6), new Vector3Dd(7, 8, 9), -10));
    b.setGeometry(
        new QuadricSurface(new Vector3Dd(6, 3, 9), new Vector3Dd(-12, 18, -15), new Vector3Dd(24, -21, 27), -30),
    );
    c.setGeometry(
        new QuadricSurface(new Vector3Dd(6, 3, 9), new Vector3Dd(12, 18, -15), new Vector3Dd(24, -21, 27), -30),
    );
    const rotation = Matrix4x4d.identityMatrix().withVal(0, 0, 0).withVal(0, 1, -1).withVal(1, 0, 1).withVal(1, 1, 0);
    expectEq(AffinePlacementStatus.Valid, b.setAffinePlacement(rotation, rotation.transpose()));
    expectEq(AffinePlacementStatus.Valid, c.setAffinePlacement(rotation, rotation.transpose()));
    const aa = new CsgBodyOperand(a);
    const bb = new CsgBodyOperand(b);
    const cc = new CsgBodyOperand(c);
    expectTrue(aa.isSameSolid(bb));
    expectTrue(bb.isSameSolid(aa));
    expectFalse(aa.isSameSolid(cc));
});

test("CsgPointAttributionContract_ReportsAllCoincidentOwnersAndDifferenceOrientation", () => {
    const outer = new SimpleBody();
    const inner = new SimpleBody();
    const equalInner = new SimpleBody();
    outer.setGeometry(new Sphere(2));
    inner.setGeometry(new Sphere(1));
    equalInner.setGeometry(new Sphere(1));
    const a = new CsgBodyOperand(outer);
    const b = new CsgBodyOperand(inner);
    const c = new CsgBodyOperand(equalInner);
    const united = new CsgTree(CsgOperation.UNION);
    const difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(united.addOperand(b));
    expectTrue(united.addOperand(c));
    expectTrue(difference.addOperand(a));
    expectTrue(difference.addOperand(b));
    expectTrue(difference.addOperand(c));
    const owners = new CsgPointAttribution();
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithAttribution(united, new Vector3Dd(1, 0, 0), 0.01, owners),
    );
    expectTrue(owners.isComplete());
    expectEq(2, owners.getCount());
    expectEq(b, owners.getOwner(0)!.getOperand(0));
    expectEq(c, owners.getOwner(1)!.getOperand(0));
    expectFalse(owners.getOwner(0)!.isReversed());
    expectFalse(owners.getOwner(1)!.isReversed());
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithAttribution(difference, new Vector3Dd(1, 0, 0), 0.01, owners),
    );
    expectEq(2, owners.getCount());
    expectTrue(owners.getOwner(0)!.isReversed());
    expectTrue(owners.getOwner(1)!.isReversed());
    expectEq(null, owners.getOwner(2));
});
test("CsgPointAttributionContract_ExcludesHiddenCancellationAndRejectsAmbiguity", () => {
    const hidden = new SimpleBody();
    const visible = new SimpleBody();
    const nearby = new SimpleBody();
    hidden.setGeometry(new Sphere(1));
    visible.setGeometry(new Sphere(1));
    nearby.setGeometry(new Sphere(1));
    nearby.setPosition(new Vector3Dd(0.0001, 0, 0));
    const a = new CsgBodyOperand(hidden);
    const b = new CsgBodyOperand(visible);
    const c = new CsgBodyOperand(nearby);
    const cancelled = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(cancelled.addOperand(a));
    expectTrue(cancelled.addOperand(a));
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(cancelled));
    expectTrue(root.addOperand(b));
    const owners = new CsgPointAttribution();
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithAttribution(root, new Vector3Dd(1, 0, 0), 0.01, owners),
    );
    expectEq(1, owners.getCount());
    expectEq(b, owners.getOwner(0)!.getOperand(0));
    const ambiguous = new CsgTree(CsgOperation.UNION);
    expectTrue(ambiguous.addOperand(b));
    expectTrue(ambiguous.addOperand(c));
    expectEq(
        GeometryContainment.Indeterminate,
        CsgPointClassification.classifyWithAttribution(ambiguous, new Vector3Dd(1, 0, 0), 0.01, owners),
    );
    expectFalse(owners.isComplete());
    expectEq(0, owners.getCount());
});

test("CsgSolidEquivalenceContract_EllipsoidsSupportExactNonPermutationReparameterization", () => {
    const a = new SimpleBody();
    const b = new SimpleBody();
    const quadric = new SimpleBody();
    const nearby = new SimpleBody();
    a.setGeometry(new Sphere(1));
    b.setGeometry(new Sphere(1));
    nearby.setGeometry(new Sphere(1));
    quadric.setGeometry(new QuadricSurface(new Vector3Dd(1, 1, 1), new Vector3Dd(), new Vector3Dd(), -1));
    const forwardA = new Matrix4x4d().scale(5, 5, 5);
    const inverseA = new Matrix4x4d().scale(0.2, 0.2, 0.2);
    const forwardB = Matrix4x4d.identityMatrix()
        .withVal(0, 0, 3)
        .withVal(0, 1, -4)
        .withVal(1, 0, 4)
        .withVal(1, 1, 3)
        .withVal(2, 2, 5);
    const inverseB = Matrix4x4d.identityMatrix()
        .withVal(0, 0, 0.12)
        .withVal(0, 1, 0.16)
        .withVal(1, 0, -0.16)
        .withVal(1, 1, 0.12)
        .withVal(2, 2, 0.2);
    expectEq(AffinePlacementStatus.Valid, a.setAffinePlacement(forwardA, inverseA));
    expectEq(AffinePlacementStatus.Valid, b.setAffinePlacement(forwardB, inverseB));
    expectEq(AffinePlacementStatus.Valid, quadric.setAffinePlacement(forwardB, inverseB));
    nearby.setScale(new Vector3Dd(5, 5, 4.999999));
    const aa = new CsgBodyOperand(a);
    const bb = new CsgBodyOperand(b);
    const qq = new CsgBodyOperand(quadric);
    const cc = new CsgBodyOperand(nearby);
    expectTrue(aa.isSameSolid(bb));
    expectTrue(bb.isSameSolid(aa));
    expectTrue(aa.isSameSolid(qq));
    expectTrue(qq.isSameSolid(aa));
    expectFalse(aa.isSameSolid(cc));
});

test("CsgSolidEquivalenceContract_AffineBoxesSupportTranslatedLocalOrigins", () => {
    const a = new SimpleBody();
    const b = new SimpleBody();
    const nearby = new SimpleBody();
    a.setGeometry(new Box(new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1)));
    b.setGeometry(new Box(new Vector3Dd(0, 0, 0), new Vector3Dd(2, 2, 2)));
    nearby.setGeometry(new Box(new Vector3Dd(0, 0, 0), new Vector3Dd(2, 2, 2)));
    const shear = Matrix4x4d.identityMatrix().withVal(0, 1, 0.5);
    const unshear = Matrix4x4d.identityMatrix().withVal(0, 1, -0.5);
    const shifted = new Matrix4x4d().translation(-1.5, -1, -1).multiply(shear);
    const shiftedInverse = unshear.multiply(new Matrix4x4d().translation(1.5, 1, 1));
    expectEq(AffinePlacementStatus.Valid, a.setAffinePlacement(shear, unshear));
    expectEq(AffinePlacementStatus.Valid, b.setAffinePlacement(shifted, shiftedInverse));
    expectEq(
        AffinePlacementStatus.Valid,
        nearby.setAffinePlacement(
            new Matrix4x4d().translation(-1.500001, -1, -1).multiply(shear),
            unshear.multiply(new Matrix4x4d().translation(1.500001, 1, 1)),
        ),
    );
    const aa = new CsgBodyOperand(a);
    const bb = new CsgBodyOperand(b);
    const cc = new CsgBodyOperand(nearby);
    expectTrue(aa.isSameSolid(bb));
    expectTrue(bb.isSameSolid(aa));
    expectFalse(aa.isSameSolid(cc));
});

test("CsgSolidEquivalenceContract_ObliquePlanesSupportTranslatedReflectedAndShearedFrames", () => {
    const a = new SimpleBody();
    const b = new SimpleBody();
    const quadric = new SimpleBody();
    const opposite = new SimpleBody();
    a.setGeometry(new PlanarHalfSpace(1, 2, 0, -3));
    b.setGeometry(new PlanarHalfSpace(-1, 2.5, 0, -2));
    quadric.setGeometry(new QuadricSurface(new Vector3Dd(), new Vector3Dd(), new Vector3Dd(-2, 5, 0), -4));
    opposite.setGeometry(new PlanarHalfSpace(1, -2.5, 0, 2));
    const linear = Matrix4x4d.identityMatrix().withVal(0, 0, -1).withVal(0, 1, 0.5);
    const forward = new Matrix4x4d().translation(1, 0, 0).multiply(linear);
    const inverse = linear.multiply(new Matrix4x4d().translation(-1, 0, 0));
    expectEq(AffinePlacementStatus.Valid, b.setAffinePlacement(forward, inverse));
    expectEq(AffinePlacementStatus.Valid, quadric.setAffinePlacement(forward, inverse));
    expectEq(AffinePlacementStatus.Valid, opposite.setAffinePlacement(forward, inverse));
    const aa = new CsgBodyOperand(a);
    const bb = new CsgBodyOperand(b);
    const qq = new CsgBodyOperand(quadric);
    const cc = new CsgBodyOperand(opposite);
    expectTrue(aa.isSameSolid(bb));
    expectTrue(bb.isSameSolid(aa));
    expectTrue(aa.isSameSolid(qq));
    expectTrue(qq.isSameSolid(aa));
    expectFalse(aa.isSameSolid(cc));
});

test("CsgSolidEquivalenceContract_QuadricsAllowGeneralExactAffineReparameterization", () => {
    const a = new SimpleBody();
    const b = new SimpleBody();
    const c = new SimpleBody();
    const sphere = new SimpleBody();
    const shiftedQuadric = new SimpleBody();
    a.setGeometry(new QuadricSurface(new Vector3Dd(1, 2, 3), new Vector3Dd(4, 5, 6), new Vector3Dd(7, 8, 9), -10));
    // new F(-u_x + 0.5 u_y + 1,u_y,u_z) in the reflected/sheared frame.
    b.setGeometry(
        new QuadricSurface(new Vector3Dd(1, 4.25, 3), new Vector3Dd(-5, -5, 8.5), new Vector3Dd(-9, 16.5, 14), -2),
    );
    c.setGeometry(
        new QuadricSurface(new Vector3Dd(1, 4.25, 3), new Vector3Dd(-5, -5, 8.500001), new Vector3Dd(-9, 16.5, 14), -2),
    );
    const linear = Matrix4x4d.identityMatrix().withVal(0, 0, -1).withVal(0, 1, 0.5);
    const forward = new Matrix4x4d().translation(1, 0, 0).multiply(linear);
    const inverse = linear.multiply(new Matrix4x4d().translation(-1, 0, 0));
    expectEq(AffinePlacementStatus.Valid, b.setAffinePlacement(forward, inverse));
    expectEq(AffinePlacementStatus.Valid, c.setAffinePlacement(forward, inverse));
    const aa = new CsgBodyOperand(a);
    const bb = new CsgBodyOperand(b);
    const cc = new CsgBodyOperand(c);
    expectTrue(aa.isSameSolid(bb));
    expectTrue(bb.isSameSolid(aa));
    expectFalse(aa.isSameSolid(cc));
    sphere.setGeometry(new Sphere(1));
    sphere.setPosition(new Vector3Dd(1, 0, 0));
    shiftedQuadric.setGeometry(new QuadricSurface(new Vector3Dd(1, 1, 1), new Vector3Dd(), new Vector3Dd(-2, 0, 0), 0));
    const ss = new CsgBodyOperand(sphere);
    const qq = new CsgBodyOperand(shiftedQuadric);
    expectTrue(ss.isSameSolid(qq));
    expectTrue(qq.isSameSolid(ss));
});

test("CsgExactQuadricContract_IndependentRationalHomogeneousTransforms", () => {
    // Generated independently with Fraction; include inside a test.
    {
        const local = [0, 3, 0, -1, 3, -3, 2, -1, -1, 1];
        const expected = [75, 72, 18, -105, 165, -72, -480, 249, -687, 1152];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 2)
                    .withVal(0, 1, 3)
                    .withVal(0, 2, -3)
                    .withVal(1, 0, 2)
                    .withVal(1, 1, 0)
                    .withVal(1, 2, 1)
                    .withVal(2, 0, -1)
                    .withVal(2, 1, 0)
                    .withVal(2, 2, 2)
                    .withVal(0, 3, 3)
                    .withVal(1, 3, 2)
                    .withVal(2, 3, 1),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-3, 1, 1, 3, 2, 4, -3, 1, -1, 1];
        const expected = [294, 122, 552, -476, -826, 626, -1316, 360, 1608, 76];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 3)
                    .withVal(0, 1, 1)
                    .withVal(0, 2, -3)
                    .withVal(1, 0, 3)
                    .withVal(1, 1, -1)
                    .withVal(1, 2, 1)
                    .withVal(2, 0, 1)
                    .withVal(2, 1, 2)
                    .withVal(2, 2, -2)
                    .withVal(0, 3, -3)
                    .withVal(1, 3, -3)
                    .withVal(2, 3, -2),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-3, -4, 1, 3, 1, 4, -3, -1, 1, 3];
        const expected = [-29, -1, -266, -21, -141, -17, 67, 54, -141, -154];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -2)
                    .withVal(0, 1, -2)
                    .withVal(0, 2, -1)
                    .withVal(1, 0, 1)
                    .withVal(1, 1, 0)
                    .withVal(1, 2, 3)
                    .withVal(2, 0, 0)
                    .withVal(2, 1, 1)
                    .withVal(2, 2, 0)
                    .withVal(0, 3, 2)
                    .withVal(1, 3, 2)
                    .withVal(2, 3, -1),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-4, 0, 2, 3, -4, 2, 3, 2, -4, 3];
        const expected = [-276, -336, -585, -612, -816, -894, 1896, 2094, 2634, -2781];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 1)
                    .withVal(0, 1, -3)
                    .withVal(0, 2, -3)
                    .withVal(1, 0, 1)
                    .withVal(1, 1, 3)
                    .withVal(1, 2, 0)
                    .withVal(2, 0, -2)
                    .withVal(2, 1, 0)
                    .withVal(2, 2, 2)
                    .withVal(0, 3, 0)
                    .withVal(1, 3, -1)
                    .withVal(2, 3, 3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [4, 3, 1, -2, 2, 2, 3, 3, 0, 1];
        const expected = [432, 300, 531, 624, 936, 708, 3120, 3096, 4260, 8700];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -2)
                    .withVal(0, 1, -3)
                    .withVal(0, 2, 0)
                    .withVal(1, 0, 3)
                    .withVal(1, 1, 3)
                    .withVal(1, 2, 3)
                    .withVal(2, 0, -2)
                    .withVal(2, 1, 2)
                    .withVal(2, 2, -2)
                    .withVal(0, 3, -1)
                    .withVal(1, 3, -2)
                    .withVal(2, 3, -2),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [2, 3, 4, 3, 0, 2, 4, -2, 0, 1];
        const expected = [102, 252, 423, 21, -147, -603, 207, 1743, -2526, 3228];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -3)
                    .withVal(0, 1, -1)
                    .withVal(0, 2, 1)
                    .withVal(1, 0, -2)
                    .withVal(1, 1, 3)
                    .withVal(1, 2, 3)
                    .withVal(2, 0, -2)
                    .withVal(2, 1, 2)
                    .withVal(2, 2, 1)
                    .withVal(0, 3, -1)
                    .withVal(1, 3, -3)
                    .withVal(2, 3, 0),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [4, 0, 2, -3, -2, -2, -3, 2, -1, -3];
        const expected = [138, 100, 312, 234, 416, 352, -570, -478, -852, 568];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 2)
                    .withVal(0, 1, 2)
                    .withVal(0, 2, 0)
                    .withVal(1, 0, -2)
                    .withVal(1, 1, 3)
                    .withVal(1, 2, -2)
                    .withVal(2, 0, 0)
                    .withVal(2, 1, -3)
                    .withVal(2, 2, 1)
                    .withVal(0, 3, -1)
                    .withVal(1, 3, 0)
                    .withVal(2, 3, 2),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-2, 3, 1, 1, 1, 0, 0, -1, 1, 0];
        const expected = [52, 40, -2, -106, 274, -2, -896, 178, 518, -1100];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 1)
                    .withVal(0, 1, 1)
                    .withVal(0, 2, 0)
                    .withVal(1, 0, -1)
                    .withVal(1, 1, -2)
                    .withVal(1, 2, 3)
                    .withVal(2, 0, -3)
                    .withVal(2, 1, 2)
                    .withVal(2, 2, 3)
                    .withVal(0, 3, -2)
                    .withVal(1, 3, -3)
                    .withVal(2, 3, 3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-3, -2, -1, 4, 0, 0, 2, -1, 4, -2];
        const expected = [-40, -8, -176, 0, -112, 48, -608, 144, -1552, -3512];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -2)
                    .withVal(0, 1, 3)
                    .withVal(0, 2, 1)
                    .withVal(1, 0, -2)
                    .withVal(1, 1, -3)
                    .withVal(1, 2, -1)
                    .withVal(2, 0, 0)
                    .withVal(2, 1, -1)
                    .withVal(2, 2, -1)
                    .withVal(0, 3, -2)
                    .withVal(1, 3, -3)
                    .withVal(2, 3, -3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-3, -1, 3, -3, 3, -2, 1, 0, -1, 3];
        const expected = [1041, -108, -759, 1332, -906, -1476, 3756, 1656, -1500, 19428];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -3)
                    .withVal(0, 1, 3)
                    .withVal(0, 2, -2)
                    .withVal(1, 0, 3)
                    .withVal(1, 1, -2)
                    .withVal(1, 2, -2)
                    .withVal(2, 0, 3)
                    .withVal(2, 1, 3)
                    .withVal(2, 2, 2)
                    .withVal(0, 3, 1)
                    .withVal(1, 3, -3)
                    .withVal(2, 3, 1),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [0, 2, -2, -2, 2, 4, -3, 1, -2, 3];
        const expected = [-436, -296, -4, 732, 264, -236, -978, 1048, 190, -284];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -2)
                    .withVal(0, 1, 1)
                    .withVal(0, 2, -2)
                    .withVal(1, 0, -2)
                    .withVal(1, 1, 3)
                    .withVal(1, 2, -3)
                    .withVal(2, 0, 0)
                    .withVal(2, 1, -3)
                    .withVal(2, 2, -2)
                    .withVal(0, 3, 1)
                    .withVal(1, 3, 2)
                    .withVal(2, 3, 3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-3, 3, -2, -3, -2, 4, 3, 0, 0, 1];
        const expected = [567, -72, -27, -108, -432, -36, -3240, 432, 1440, 5040];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 1)
                    .withVal(0, 1, -1)
                    .withVal(0, 2, 0)
                    .withVal(1, 0, -3)
                    .withVal(1, 1, 3)
                    .withVal(1, 2, -3)
                    .withVal(2, 0, 3)
                    .withVal(2, 1, 3)
                    .withVal(2, 2, 0)
                    .withVal(0, 3, 3)
                    .withVal(1, 3, 1)
                    .withVal(2, 3, -1),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [0, 3, -4, 4, -4, -2, 1, 4, 2, -2];
        const expected = [-48, -96, -456, -144, -288, -444, -420, -534, -1182, -900];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 3)
                    .withVal(0, 1, -1)
                    .withVal(0, 2, 1)
                    .withVal(1, 0, 2)
                    .withVal(1, 1, 3)
                    .withVal(1, 2, -2)
                    .withVal(2, 0, -2)
                    .withVal(2, 1, -1)
                    .withVal(2, 2, 0)
                    .withVal(0, 3, -3)
                    .withVal(1, 3, 0)
                    .withVal(2, 3, 0),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-1, 3, -1, 2, -4, 1, -4, 0, -3, -1];
        const expected = [-15, 45, -364, -222, -510, -886, 2264, 4624, 2388, -1580];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 3)
                    .withVal(0, 1, -2)
                    .withVal(0, 2, -2)
                    .withVal(1, 0, -3)
                    .withVal(1, 1, -2)
                    .withVal(1, 2, 0)
                    .withVal(2, 0, -2)
                    .withVal(2, 1, 3)
                    .withVal(2, 2, -1)
                    .withVal(0, 3, 0)
                    .withVal(1, 3, -2)
                    .withVal(2, 3, 3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [3, -2, -1, -2, -1, -4, 4, -3, -3, 2];
        const expected = [-75, -475, -10, -755, -180, -710, -2495, -4710, -1475, -6340];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 3)
                    .withVal(0, 1, -2)
                    .withVal(0, 2, 2)
                    .withVal(1, 0, -2)
                    .withVal(1, 1, 2)
                    .withVal(1, 2, 1)
                    .withVal(2, 0, 2)
                    .withVal(2, 1, 3)
                    .withVal(2, 2, -1)
                    .withVal(0, 3, 2)
                    .withVal(1, 3, -3)
                    .withVal(2, 3, -1),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [3, -3, 0, -3, -2, 1, -1, 3, -4, -1];
        const expected = [84, 20, -48, 88, -64, -32, 368, 304, -128, 240];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 1)
                    .withVal(0, 1, -1)
                    .withVal(0, 2, 1)
                    .withVal(1, 0, 1)
                    .withVal(1, 1, -1)
                    .withVal(1, 2, -3)
                    .withVal(2, 0, 0)
                    .withVal(2, 1, -2)
                    .withVal(2, 2, 0)
                    .withVal(0, 3, -3)
                    .withVal(1, 3, 1)
                    .withVal(2, 3, 1),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [2, 3, -3, -3, -1, -2, -2, -1, -4, -2];
        const expected = [-54, 168, 0, 162, 216, 36, 162, -726, -720, -192];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -3)
                    .withVal(0, 1, 1)
                    .withVal(0, 2, -3)
                    .withVal(1, 0, 3)
                    .withVal(1, 1, -3)
                    .withVal(1, 2, 3)
                    .withVal(2, 0, -2)
                    .withVal(2, 1, 0)
                    .withVal(2, 2, 1)
                    .withVal(0, 3, 2)
                    .withVal(1, 3, 2)
                    .withVal(2, 3, 2),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [1, 2, 3, 4, 0, 2, -1, -4, 1, -3];
        const expected = [243, 262, 13, -342, 144, -68, 351, 178, 329, -917];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -2)
                    .withVal(0, 1, 0)
                    .withVal(0, 2, 1)
                    .withVal(1, 0, 0)
                    .withVal(1, 1, 1)
                    .withVal(1, 2, 2)
                    .withVal(2, 0, 3)
                    .withVal(2, 1, -2)
                    .withVal(2, 2, 2)
                    .withVal(0, 3, -2)
                    .withVal(1, 3, -2)
                    .withVal(2, 3, 3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [4, 0, -1, -2, 1, 4, -3, -2, -1, 4];
        const expected = [342, 272, 36, -624, 234, -192, -24, -16, -24, 176];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 1)
                    .withVal(0, 1, 2)
                    .withVal(0, 2, -1)
                    .withVal(1, 0, 0)
                    .withVal(1, 1, 3)
                    .withVal(1, 2, 0)
                    .withVal(2, 0, -1)
                    .withVal(2, 1, 2)
                    .withVal(2, 2, 3)
                    .withVal(0, 3, 0)
                    .withVal(1, 3, 1)
                    .withVal(2, 3, 2),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [0, 2, -4, 2, 3, 3, -2, 2, 2, -4];
        const expected = [208, 5, 248, -81, 478, -82, -1978, 416, -2196, 4380];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -1)
                    .withVal(0, 1, -1)
                    .withVal(0, 2, 3)
                    .withVal(1, 0, -2)
                    .withVal(1, 1, 2)
                    .withVal(1, 2, -1)
                    .withVal(2, 0, 1)
                    .withVal(2, 1, 2)
                    .withVal(2, 2, -3)
                    .withVal(0, 3, 2)
                    .withVal(1, 3, 2)
                    .withVal(2, 3, 3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [3, -2, -3, 4, -2, 3, -2, 3, -1, -1];
        const expected = [-30, -27, -3, -93, 15, 21, 138, 114, -30, -102];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 3)
                    .withVal(0, 1, -1)
                    .withVal(0, 2, 0)
                    .withVal(1, 0, -1)
                    .withVal(1, 1, 0)
                    .withVal(1, 2, 0)
                    .withVal(2, 0, 3)
                    .withVal(2, 1, -1)
                    .withVal(2, 2, -3)
                    .withVal(0, 3, 2)
                    .withVal(1, 3, 1)
                    .withVal(2, 3, 3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [0, 0, -3, -1, 2, 1, -4, -4, -1, 0];
        const expected = [-81, -36, -198, 108, 351, -315, 432, -1341, -2151, -4707];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 2)
                    .withVal(0, 1, 2)
                    .withVal(0, 2, 3)
                    .withVal(1, 0, 0)
                    .withVal(1, 1, 3)
                    .withVal(1, 2, 3)
                    .withVal(2, 0, 3)
                    .withVal(2, 1, 0)
                    .withVal(2, 2, -3)
                    .withVal(0, 3, 0)
                    .withVal(1, 3, -3)
                    .withVal(2, 3, -1),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-1, 2, 2, 1, 1, 2, -2, 1, 1, -1];
        const expected = [112, 180, 82, 284, -192, -242, 228, 282, -194, 116];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -3)
                    .withVal(0, 1, 1)
                    .withVal(0, 2, 2)
                    .withVal(1, 0, 1)
                    .withVal(1, 1, -1)
                    .withVal(1, 2, -3)
                    .withVal(2, 0, -2)
                    .withVal(2, 1, 0)
                    .withVal(2, 2, -2)
                    .withVal(0, 3, -1)
                    .withVal(1, 3, 2)
                    .withVal(2, 3, 3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-4, 0, 3, -4, -1, 2, 2, -1, -1, 0];
        const expected = [-42, -20, -4, 67, 26, -21, 108, -101, -33, -62];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -1)
                    .withVal(0, 1, -1)
                    .withVal(0, 2, 2)
                    .withVal(1, 0, -1)
                    .withVal(1, 1, 0)
                    .withVal(1, 2, 1)
                    .withVal(2, 0, 1)
                    .withVal(2, 1, -3)
                    .withVal(2, 2, 3)
                    .withVal(0, 3, 3)
                    .withVal(1, 3, 1)
                    .withVal(2, 3, 3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [4, 2, 3, 2, -3, -1, 4, -4, 0, -4];
        const expected = [124, 1084, 448, 56, -32, 1312, -352, -1696, -1664, -1088];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -3)
                    .withVal(0, 1, 1)
                    .withVal(0, 2, 3)
                    .withVal(1, 0, 3)
                    .withVal(1, 1, -1)
                    .withVal(1, 2, 1)
                    .withVal(2, 0, -3)
                    .withVal(2, 1, 3)
                    .withVal(2, 2, -2)
                    .withVal(0, 3, 0)
                    .withVal(1, 3, 0)
                    .withVal(2, 3, 1),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-3, 1, 3, -3, 4, -3, -3, 0, -2, 3];
        const expected = [12, 144, 433, -88, -150, 500, 682, -2240, -3890, 8729];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -3)
                    .withVal(0, 1, 1)
                    .withVal(0, 2, 1)
                    .withVal(1, 0, -1)
                    .withVal(1, 1, -3)
                    .withVal(1, 2, 0)
                    .withVal(2, 0, 0)
                    .withVal(2, 1, 2)
                    .withVal(2, 2, 0)
                    .withVal(0, 3, 1)
                    .withVal(1, 3, 3)
                    .withVal(2, 3, 3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [2, -4, 1, 0, 2, 0, 0, 2, 2, -3];
        const expected = [-30, -54, 10, 92, 36, -20, -216, 344, 136, -456];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -2)
                    .withVal(0, 1, 0)
                    .withVal(0, 2, 1)
                    .withVal(1, 0, -2)
                    .withVal(1, 1, -1)
                    .withVal(1, 2, 1)
                    .withVal(2, 0, 0)
                    .withVal(2, 1, -1)
                    .withVal(2, 2, -2)
                    .withVal(0, 3, -2)
                    .withVal(1, 3, 2)
                    .withVal(2, 3, -2),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [4, -3, 2, 0, 4, 0, 4, -4, 2, 4];
        const expected = [-100, 392, 72, 320, 48, 312, -816, 3120, 792, 1044];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 2)
                    .withVal(0, 1, 3)
                    .withVal(0, 2, 0)
                    .withVal(1, 0, 2)
                    .withVal(1, 1, 0)
                    .withVal(1, 2, 0)
                    .withVal(2, 0, -2)
                    .withVal(2, 1, -1)
                    .withVal(2, 2, 3)
                    .withVal(0, 3, -3)
                    .withVal(1, 3, 0)
                    .withVal(2, 3, -3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [0, 3, -4, 2, -4, 0, 2, 3, -1, -3];
        const expected = [-504, -72, 35, -704, 968, 198, -275, -1083, 2687, -3003];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 1)
                    .withVal(0, 1, 2)
                    .withVal(0, 2, 3)
                    .withVal(1, 0, 3)
                    .withVal(1, 1, 0)
                    .withVal(1, 2, -2)
                    .withVal(2, 0, 2)
                    .withVal(2, 1, 3)
                    .withVal(2, 2, -2)
                    .withVal(0, 3, -2)
                    .withVal(1, 3, 2)
                    .withVal(2, 3, 0),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-4, -2, 0, -4, 1, 2, -2, 3, -3, 0];
        const expected = [-106, -114, -200, -156, 320, 160, -224, -1112, 40, -1974];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 1)
                    .withVal(0, 1, 0)
                    .withVal(0, 2, -3)
                    .withVal(1, 0, 3)
                    .withVal(1, 1, 0)
                    .withVal(1, 2, 1)
                    .withVal(2, 0, 0)
                    .withVal(2, 1, -2)
                    .withVal(2, 2, -1)
                    .withVal(0, 3, 2)
                    .withVal(1, 3, -3)
                    .withVal(2, 3, 2),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-3, 4, 0, 4, -3, -3, 3, 0, -2, 0];
        const expected = [820, 1297, 16, 2072, 244, 316, -5100, -6450, -732, 7920];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 2)
                    .withVal(0, 1, 0)
                    .withVal(0, 2, 3)
                    .withVal(1, 0, -2)
                    .withVal(1, 1, 0)
                    .withVal(1, 2, -2)
                    .withVal(2, 0, 3)
                    .withVal(2, 1, -3)
                    .withVal(2, 2, -2)
                    .withVal(0, 3, 3)
                    .withVal(1, 3, 0)
                    .withVal(2, 3, 0),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-1, 4, 1, -3, -1, -3, 3, 1, 2, 4];
        const expected = [445, 1845, 100, 1728, -392, -1161, 5676, 12015, -4479, 27639];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 3)
                    .withVal(0, 1, 1)
                    .withVal(0, 2, -3)
                    .withVal(1, 0, -2)
                    .withVal(1, 1, -3)
                    .withVal(1, 2, 1)
                    .withVal(2, 0, -3)
                    .withVal(2, 1, -2)
                    .withVal(2, 2, -3)
                    .withVal(0, 3, -1)
                    .withVal(1, 3, -2)
                    .withVal(2, 3, 2),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-4, 4, -3, 0, 2, -3, -4, 0, 2, -2];
        const expected = [68, -4, 68, -80, -172, 80, -572, 256, 536, 944];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 2)
                    .withVal(0, 1, 1)
                    .withVal(0, 2, 0)
                    .withVal(1, 0, 0)
                    .withVal(1, 1, 1)
                    .withVal(1, 2, 2)
                    .withVal(2, 0, 2)
                    .withVal(2, 1, -1)
                    .withVal(2, 2, -1)
                    .withVal(0, 3, 2)
                    .withVal(1, 3, -1)
                    .withVal(2, 3, -1),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [3, 4, -1, 1, -3, 1, 2, 1, 1, -3];
        const expected = [49, 81, 16, -138, 16, -60, 274, -258, -28, 361];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 3)
                    .withVal(0, 1, 0)
                    .withVal(0, 2, 1)
                    .withVal(1, 0, 1)
                    .withVal(1, 1, 0)
                    .withVal(1, 2, 1)
                    .withVal(2, 0, 0)
                    .withVal(2, 1, -3)
                    .withVal(2, 2, 1)
                    .withVal(0, 3, -3)
                    .withVal(1, 3, 0)
                    .withVal(2, 3, 2),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [4, -3, 4, 2, 3, 2, 0, -4, 0, -1];
        const expected = [8, -42, -2, 48, 32, -236, -16, 368, 192, -184];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -2)
                    .withVal(0, 1, -1)
                    .withVal(0, 2, 3)
                    .withVal(1, 0, -1)
                    .withVal(1, 1, -1)
                    .withVal(1, 2, 0)
                    .withVal(2, 0, 1)
                    .withVal(2, 1, -1)
                    .withVal(2, 2, 2)
                    .withVal(0, 3, -3)
                    .withVal(1, 3, 0)
                    .withVal(2, 3, 0),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [0, 3, 0, 0, -4, 2, 1, -3, 1, 1];
        const expected = [324, 489, 12, 804, -96, -120, 576, 630, -108, 117];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 3)
                    .withVal(0, 1, 0)
                    .withVal(0, 2, 2)
                    .withVal(1, 0, -2)
                    .withVal(1, 1, 0)
                    .withVal(1, 2, -2)
                    .withVal(2, 0, 2)
                    .withVal(2, 1, -3)
                    .withVal(2, 2, -3)
                    .withVal(0, 3, 3)
                    .withVal(1, 3, -3)
                    .withVal(2, 3, 3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [1, 2, 4, 1, 3, -4, 1, -1, 0, 2];
        const expected = [443, 229, 815, -656, 510, -596, 3884, -3984, 6496, 18116];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -3)
                    .withVal(0, 1, 3)
                    .withVal(0, 2, -2)
                    .withVal(1, 0, -2)
                    .withVal(1, 1, 3)
                    .withVal(1, 2, 3)
                    .withVal(2, 0, 1)
                    .withVal(2, 1, 2)
                    .withVal(2, 2, 1)
                    .withVal(0, 3, -3)
                    .withVal(1, 3, 0)
                    .withVal(2, 3, -3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-3, 2, -4, 0, 1, -3, -3, 0, 3, -4];
        const expected = [-521, -673, -49, 1174, -326, 338, -6006, 6818, -1778, -17537];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -1)
                    .withVal(0, 1, 3)
                    .withVal(0, 2, 1)
                    .withVal(1, 0, -1)
                    .withVal(1, 1, 2)
                    .withVal(1, 2, 2)
                    .withVal(2, 0, -2)
                    .withVal(2, 1, -3)
                    .withVal(2, 2, 3)
                    .withVal(0, 3, -3)
                    .withVal(1, 3, 2)
                    .withVal(2, 3, -2),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-1, -2, 0, -3, 2, 0, -3, -3, 0, 0];
        const expected = [16, -104, -153, 29, 69, 159, 163, -1382, 1137, -4112];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -2)
                    .withVal(0, 1, 0)
                    .withVal(0, 2, -3)
                    .withVal(1, 0, 2)
                    .withVal(1, 1, 3)
                    .withVal(1, 2, -3)
                    .withVal(2, 0, 1)
                    .withVal(2, 1, 3)
                    .withVal(2, 2, -1)
                    .withVal(0, 3, 1)
                    .withVal(1, 3, -3)
                    .withVal(2, 3, 3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-2, 2, 2, -3, 0, 3, 3, 2, 3, -3];
        const expected = [-48, 8, -8, 12, 52, 0, 80, -20, -76, -48];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 2)
                    .withVal(0, 1, -1)
                    .withVal(0, 2, -2)
                    .withVal(1, 0, -2)
                    .withVal(1, 1, 2)
                    .withVal(1, 2, 1)
                    .withVal(2, 0, 2)
                    .withVal(2, 1, -2)
                    .withVal(2, 2, -3)
                    .withVal(0, 3, 0)
                    .withVal(1, 3, 0)
                    .withVal(2, 3, 0),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [1, -4, 0, 1, -1, -2, 3, 3, -3, 4];
        const expected = [-50, -128, 30, -224, 12, 160, -160, 512, 224, 256];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 1)
                    .withVal(0, 1, 3)
                    .withVal(0, 2, 3)
                    .withVal(1, 0, 0)
                    .withVal(1, 1, 1)
                    .withVal(1, 2, -1)
                    .withVal(2, 0, 3)
                    .withVal(2, 1, 1)
                    .withVal(2, 2, 1)
                    .withVal(0, 3, -3)
                    .withVal(1, 3, 1)
                    .withVal(2, 3, -1),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [1, 0, 2, -1, 1, -4, -4, 0, 2, -3];
        const expected = [27, 324, 204, -120, 84, -528, 654, -2616, 2052, 4467];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -2)
                    .withVal(0, 1, 0)
                    .withVal(0, 2, 2)
                    .withVal(1, 0, 0)
                    .withVal(1, 1, 3)
                    .withVal(1, 2, 1)
                    .withVal(2, 0, 1)
                    .withVal(2, 1, 3)
                    .withVal(2, 2, 2)
                    .withVal(0, 3, 1)
                    .withVal(1, 3, 2)
                    .withVal(2, 3, -3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [0, -3, 4, 4, -4, 1, -1, -1, 1, 1];
        const expected = [18, 668, 648, -66, -27, -1530, -48, -641, 1008, 518];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 2)
                    .withVal(0, 1, 1)
                    .withVal(0, 2, 3)
                    .withVal(1, 0, -3)
                    .withVal(1, 1, 3)
                    .withVal(1, 2, 0)
                    .withVal(2, 0, -2)
                    .withVal(2, 1, 3)
                    .withVal(2, 2, -2)
                    .withVal(0, 3, -1)
                    .withVal(1, 3, 2)
                    .withVal(2, 3, 1),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [4, 1, -1, -1, -4, -3, -2, -2, 0, 0];
        const expected = [567, 215, 227, -666, 720, -451, -2844, 1601, -1760, 3602];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 1)
                    .withVal(0, 1, 2)
                    .withVal(0, 2, 0)
                    .withVal(1, 0, -3)
                    .withVal(1, 1, 1)
                    .withVal(1, 2, -3)
                    .withVal(2, 0, -3)
                    .withVal(2, 1, -2)
                    .withVal(2, 2, -3)
                    .withVal(0, 3, 3)
                    .withVal(1, 3, 0)
                    .withVal(2, 3, -1),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-2, -4, 0, -4, -4, 3, -4, 3, 3, 3];
        const expected = [65, 177, 13, 222, 114, 158, 444, 788, 484, 672];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 3)
                    .withVal(0, 1, -2)
                    .withVal(0, 2, 3)
                    .withVal(1, 0, -3)
                    .withVal(1, 1, 1)
                    .withVal(1, 2, -2)
                    .withVal(2, 0, 2)
                    .withVal(2, 1, 1)
                    .withVal(2, 2, 1)
                    .withVal(0, 3, 0)
                    .withVal(1, 3, -3)
                    .withVal(2, 3, 3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [0, -2, 1, -3, 1, -3, -2, 0, -3, -4];
        const expected = [2, -3, 4, 1, -8, 5, -11, 13, 4, -10];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -3)
                    .withVal(0, 1, -1)
                    .withVal(0, 2, -1)
                    .withVal(1, 0, -2)
                    .withVal(1, 1, 1)
                    .withVal(1, 2, -1)
                    .withVal(2, 0, -2)
                    .withVal(2, 1, 0)
                    .withVal(2, 2, -1)
                    .withVal(0, 3, 1)
                    .withVal(1, 3, 3)
                    .withVal(2, 3, 0),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [-4, 3, 3, -4, -4, 3, -4, -3, -3, -4];
        const expected = [-80, 2052, 304, -24, 208, 1752, -856, -3696, -824, -1892];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 3)
                    .withVal(0, 1, -2)
                    .withVal(0, 2, 3)
                    .withVal(1, 0, 2)
                    .withVal(1, 1, 0)
                    .withVal(1, 2, -2)
                    .withVal(2, 0, -3)
                    .withVal(2, 1, -2)
                    .withVal(2, 2, 3)
                    .withVal(0, 3, 0)
                    .withVal(1, 3, 1)
                    .withVal(2, 3, 1),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [3, 1, -1, -4, 1, -4, -1, -3, -2, 3];
        const expected = [13, -55, -99, -22, 22, 150, -150, -414, 582, -647];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -2)
                    .withVal(0, 1, 1)
                    .withVal(0, 2, 0)
                    .withVal(1, 0, -1)
                    .withVal(1, 1, -2)
                    .withVal(1, 2, 2)
                    .withVal(2, 0, -1)
                    .withVal(2, 1, -1)
                    .withVal(2, 2, 2)
                    .withVal(0, 3, 3)
                    .withVal(1, 3, -1)
                    .withVal(2, 3, 3),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [4, 1, -4, 0, 1, 1, 0, -3, -1, 1];
        const expected = [108, -83, 25, -288, -45, -40, 162, 859, 535, -83];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, -2)
                    .withVal(0, 1, 0)
                    .withVal(0, 2, 1)
                    .withVal(1, 0, 1)
                    .withVal(1, 1, 0)
                    .withVal(1, 2, 2)
                    .withVal(2, 0, -1)
                    .withVal(2, 1, -3)
                    .withVal(2, 2, 1)
                    .withVal(0, 3, 2)
                    .withVal(1, 3, 3)
                    .withVal(2, 3, -2),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
    {
        const local = [1, -3, -2, -2, -4, 4, 3, -2, -4, 1];
        const expected = [126, -245, -378, 0, 252, 588, 378, -1617, 1701, -3465];
        const result: number[] = [];
        expectTrue(
            CsgExactQuadric.world(
                local,
                Matrix4x4d.identityMatrix()
                    .withVal(0, 0, 1)
                    .withVal(0, 1, -2)
                    .withVal(0, 2, -1)
                    .withVal(1, 0, 3)
                    .withVal(1, 1, 3)
                    .withVal(1, 2, 0)
                    .withVal(2, 0, 2)
                    .withVal(2, 1, 3)
                    .withVal(2, 2, -2)
                    .withVal(0, 3, 2)
                    .withVal(1, 3, -3)
                    .withVal(2, 3, 0),
                result,
            ),
        );
        for (let i = 0; i < 10; ++i) expectDoubleEq(expected[i], result[i]);
    }
});
test("CsgExactQuadricContract_RejectsUnrepresentableIntermediateAtomically", () => {
    const local = [1, 1, 1, Double.MIN_VALUE, 0, 0, 0, 0, 0, -1];
    const result = [11, 12, 13, 14, 15, 16, 17, 18, 19, 20];
    expectFalse(CsgExactQuadric.world(local, Matrix4x4d.identityMatrix(), result));
    for (let i = 0; i < 10; ++i) expectDoubleEq(11 + i, result[i]);
});
test("CsgPointAttributionContract_RetainsPlacedNestedDifferenceOwners", () => {
    const outer = new SimpleBody();
    const inner = new SimpleBody();
    const duplicate = new SimpleBody();
    outer.setGeometry(new Sphere(2));
    inner.setGeometry(new Sphere(1));
    duplicate.setGeometry(new Sphere(1));
    const a = new CsgBodyOperand(outer);
    const b = new CsgBodyOperand(inner);
    const c = new CsgBodyOperand(duplicate);
    const child = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(child.addOperand(a));
    expectTrue(child.addOperand(b));
    expectTrue(child.addOperand(c));
    const placed = new CsgPlacedExpression(child);
    expectEq(
        AffinePlacementStatus.Valid,
        placed.setAffinePlacement(
            new Matrix4x4d().translation(3, 0, 0).multiply(new Matrix4x4d().scale(2, 1, 1)),
            new Matrix4x4d().scale(0.5, 1, 1).multiply(new Matrix4x4d().translation(-3, 0, 0)),
        ),
    );
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    const owners = new CsgPointAttribution();
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithAttribution(root, new Vector3Dd(5, 0, 0), 0.1, owners),
    );
    expectEq(2, owners.getCount());
    expectEq(placed, owners.getOwner(0)!.getOperand(0));
    expectEq(b, owners.getOwner(0)!.getOperand(1));
    expectEq(placed, owners.getOwner(1)!.getOperand(0));
    expectEq(c, owners.getOwner(1)!.getOperand(1));
    expectTrue(owners.getOwner(0)!.isReversed());
    expectTrue(owners.getOwner(1)!.isReversed());
});

test("CsgPointProofContract_EquivalentUnsupportedLeafUsesCertifiedSiblingInEitherOrder", () => {
    const sphere = new SimpleBody();
    const quadric = new UncertifiedTestBody();
    sphere.setGeometry(new Sphere(1));
    quadric.setGeometry(new QuadricSurface(new Vector3Dd(1, 1, 1), new Vector3Dd(), new Vector3Dd(), -1));
    const a = new CsgBodyOperand(sphere);
    const b = new CsgBodyOperand(quadric);
    expectTrue(a.isSameSolid(b));
    expectEq(GeometryContainment.Unsupported, b.classifyPointCertified(new Vector3Dd(1, 0, 0), 0.01));
    const first = new CsgTree(CsgOperation.UNION);
    const second = new CsgTree(CsgOperation.UNION);
    const difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(first.addOperand(a));
    expectTrue(first.addOperand(b));
    expectTrue(second.addOperand(b));
    expectTrue(second.addOperand(a));
    expectTrue(difference.addOperand(b));
    expectTrue(difference.addOperand(a));
    const owners = new CsgPointAttribution();
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithAttribution(first, new Vector3Dd(1, 0, 0), 0.01, owners),
    );
    expectEq(2, owners.getCount());
    expectEq(a, owners.getOwner(0)!.getOperand(0));
    expectEq(b, owners.getOwner(1)!.getOperand(0));
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithAttribution(second, new Vector3Dd(1, 0, 0), 0.01, owners),
    );
    expectEq(2, owners.getCount());
    expectEq(b, owners.getOwner(0)!.getOperand(0));
    expectEq(a, owners.getOwner(1)!.getOperand(0));
    const proof = new CsgPointProof();
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithProof(second, new Vector3Dd(1, 0, 0), 0.01, proof),
    );
    let found = false;
    for (let i = 0; i < proof.getCount(); ++i)
        if (proof.getEvidence(i)!.isEquivalence()) {
            found = true;
            expectEq(a, proof.getEvidence(i)!.getPath().getOperand(0));
            expectEq(b, proof.getEvidence(i)!.getEquivalentPath().getOperand(0));
        }
    expectTrue(found);
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(difference, new Vector3Dd(1, 0, 0), 0.01));
});
test("CsgPointProofContract_DoesNotTransferAcrossDistinctOrUncertainShapes", () => {
    const sphere = new SimpleBody();
    const quadric = new UncertifiedTestBody();
    const opposite = new UncertifiedTestBody();
    sphere.setGeometry(new Sphere(1));
    quadric.setGeometry(new QuadricSurface(new Vector3Dd(1, 1, 1), new Vector3Dd(), new Vector3Dd(), -1.000001));
    opposite.setGeometry(new QuadricSurface(new Vector3Dd(-1, -1, -1), new Vector3Dd(), new Vector3Dd(), 1));
    const a = new CsgBodyOperand(sphere);
    const b = new CsgBodyOperand(quadric);
    const c = new CsgBodyOperand(opposite);
    const first = new CsgTree(CsgOperation.UNION);
    const second = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(first.addOperand(a));
    expectTrue(first.addOperand(b));
    expectTrue(second.addOperand(a));
    expectTrue(second.addOperand(c));
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(first, new Vector3Dd(1, 0, 0), 0.01));
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(second, new Vector3Dd(1, 0, 0), 0.01));
    const equal = new CsgTree(CsgOperation.UNION);
    const same = new UncertifiedTestBody();
    same.setGeometry(new QuadricSurface(new Vector3Dd(1, 1, 1), new Vector3Dd(), new Vector3Dd(), -1));
    const sameOperand = new CsgBodyOperand(same);
    expectTrue(equal.addOperand(a));
    expectTrue(equal.addOperand(sameOperand));
    expectEq(GeometryContainment.Indeterminate, CsgPointClassification.classify(equal, new Vector3Dd(1, 0, 0), 0));
});

test("CsgPointProofContract_EquivalentLeafRelationKeepsNestedPlacementAndDifferenceParity", () => {
    const outer = new SimpleBody();
    const sphere = new SimpleBody();
    const quadric = new UncertifiedTestBody();
    outer.setGeometry(new Sphere(2));
    sphere.setGeometry(new Sphere(1));
    quadric.setGeometry(new QuadricSurface(new Vector3Dd(1, 1, 1), new Vector3Dd(), new Vector3Dd(), -1));
    const a = new CsgBodyOperand(outer);
    const b = new CsgBodyOperand(sphere);
    const c = new CsgBodyOperand(quadric);
    const child = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(child.addOperand(a));
    expectTrue(child.addOperand(b));
    expectTrue(child.addOperand(c));
    const placed = new CsgPlacedExpression(child);
    expectEq(
        AffinePlacementStatus.Valid,
        placed.setAffinePlacement(
            new Matrix4x4d().translation(3, 0, 0).multiply(new Matrix4x4d().scale(2, 1, 1)),
            new Matrix4x4d().scale(0.5, 1, 1).multiply(new Matrix4x4d().translation(-3, 0, 0)),
        ),
    );
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    const proof = new CsgPointProof();
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithProof(root, new Vector3Dd(5, 0, 0), 0.1, proof),
    );
    let found = false;
    for (let i = 0; i < proof.getCount(); ++i)
        if (proof.getEvidence(i)!.isEquivalence()) {
            found = true;
            const source = proof.getEvidence(i)!.getPath();
            const recipient = proof.getEvidence(i)!.getEquivalentPath();
            expectEq(2, source.getLength());
            expectEq(2, recipient.getLength());
            expectEq(placed, source.getOperand(0));
            expectEq(b, source.getOperand(1));
            expectEq(placed, recipient.getOperand(0));
            expectEq(c, recipient.getOperand(1));
            expectTrue(source.isReversed());
            expectTrue(recipient.isReversed());
        }
    expectTrue(found);
    const owners = new CsgPointAttribution();
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithAttribution(root, new Vector3Dd(5, 0, 0), 0.1, owners),
    );
    expectEq(2, owners.getCount());
    expectTrue(owners.getOwner(0)!.isReversed());
    expectTrue(owners.getOwner(1)!.isReversed());
});

test("CsgSolidEquivalenceContract_IndependentlyBuiltCompositionsPreserveBooleanSemantics", () => {
    const first = new SimpleBody();
    const second = new SimpleBody();
    const third = new SimpleBody();
    first.setGeometry(new Sphere(1));
    second.setGeometry(new Sphere(1));
    third.setGeometry(new Sphere(1));
    second.setPosition(new Vector3Dd(4, 0, 0));
    third.setPosition(new Vector3Dd(8, 0, 0));
    const a = new CsgBodyOperand(first);
    const b = new CsgBodyOperand(second);
    const c = new CsgBodyOperand(third);
    const left = new CsgTree(CsgOperation.UNION);
    const right = new CsgTree(CsgOperation.UNION);
    const wrong = new CsgTree(CsgOperation.UNION);
    expectTrue(left.addOperand(a));
    expectTrue(left.addOperand(b));
    expectTrue(right.addOperand(b));
    expectTrue(right.addOperand(a));
    expectTrue(wrong.addOperand(a));
    expectTrue(wrong.addOperand(c));
    expectTrue(left.isSameSolid(right));
    expectTrue(right.isSameSolid(left));
    expectFalse(left.isSameSolid(wrong));
    const minusA = new CsgTree(CsgOperation.DIFFERENCE);
    const minusB = new CsgTree(CsgOperation.DIFFERENCE);
    const minusWrong = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(minusA.addOperand(a));
    expectTrue(minusA.addOperand(b));
    expectTrue(minusA.addOperand(c));
    expectTrue(minusB.addOperand(a));
    expectTrue(minusB.addOperand(c));
    expectTrue(minusB.addOperand(b));
    expectTrue(minusWrong.addOperand(b));
    expectTrue(minusWrong.addOperand(a));
    expectTrue(minusWrong.addOperand(c));
    expectTrue(minusA.isSameSolid(minusB));
    expectFalse(minusA.isSameSolid(minusWrong));
    const placedA = new CsgPlacedExpression(left);
    const placedB = new CsgPlacedExpression(right);
    const shifted = new CsgPlacedExpression(right);
    const p = new Matrix4x4d().translation(2, 0, 0);
    const inv = new Matrix4x4d().translation(-2, 0, 0);
    expectEq(AffinePlacementStatus.Valid, placedA.setAffinePlacement(p, inv));
    expectEq(AffinePlacementStatus.Valid, placedB.setAffinePlacement(p, inv));
    expectEq(
        AffinePlacementStatus.Valid,
        shifted.setAffinePlacement(new Matrix4x4d().translation(3, 0, 0), new Matrix4x4d().translation(-3, 0, 0)),
    );
    expectTrue(placedA.isSameSolid(placedB));
    expectFalse(placedA.isSameSolid(shifted));
    const composed = new CsgTree(CsgOperation.UNION);
    expectTrue(composed.addOperand(left));
    expectTrue(composed.addOperand(right));
    const owners = new CsgPointAttribution();
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithAttribution(composed, new Vector3Dd(1, 0, 0), 0.01, owners),
    );
    expectEq(2, owners.getCount());
    expectEq(left, owners.getOwner(0)!.getOperand(0));
    expectEq(a, owners.getOwner(0)!.getOperand(1));
    expectEq(right, owners.getOwner(1)!.getOperand(0));
    expectEq(a, owners.getOwner(1)!.getOperand(1));
});

test("CsgSolidEquivalenceContract_DoesNotDeclareCyclicExpressionsEquivalent", () => {
    const a = new CsgCycleTestOperand();
    const b = new CsgCycleTestOperand();
    const first = new CsgPlacedExpression(a);
    const second = new CsgPlacedExpression(b);
    expectFalse(first.isSameSolid(second));
    expectFalse(second.isSameSolid(first));
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(first));
    expectEq(GeometryContainment.InvalidInput, CsgPointClassification.classify(root, new Vector3Dd(), 0.1));
});

test("CsgSolidEquivalenceContract_DoesNotIgnoreUnknownScenePolicy", () => {
    const sphere = new SimpleBody();
    sphere.setGeometry(new Sphere(1));
    const leaf = new CsgBodyOperand(sphere);
    const child = new CsgTree(CsgOperation.UNION);
    expectTrue(child.addOperand(leaf));
    const normal = new CsgPolicyTestOperand(child, false);
    const inverted = new CsgPolicyTestOperand(child, true);
    const first = new CsgTree(CsgOperation.UNION);
    const second = new CsgTree(CsgOperation.UNION);
    expectTrue(first.addOperand(normal));
    expectTrue(second.addOperand(inverted));
    expectFalse(first.isSameSolid(second));
    expectFalse(second.isSameSolid(first));
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(first, new Vector3Dd(), 0.1));
    const leaves = new CsgRayLeafBatch();
    expectEq(
        RaySolidQueryStatus.Unsupported,
        CsgRayLeafQuery.query(
            first,
            Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, 10),
            leaves,
        ),
    );
    expectEq(0, leaves.size());
});

test("CsgSolidEquivalenceContract_SceneSubclassMustOptIntoStructuralTraversal", () => {
    const sphere = new SimpleBody();
    sphere.setGeometry(new Sphere(1));
    const leaf = new CsgBodyOperand(sphere);
    const custom = new CsgPolicyTestTree();
    expectTrue(custom.addOperand(leaf));
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(custom));
    expectFalse(custom.isStructuralTraversalSafe());
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(root, new Vector3Dd(), 0.1));
    const leaves = new CsgRayLeafBatch();
    expectEq(
        RaySolidQueryStatus.Unsupported,
        CsgRayLeafQuery.query(
            root,
            Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, 10),
            leaves,
        ),
    );
});

test("CsgSolidEquivalenceContract_OpaqueExpressionMustOptIntoTraversal", () => {
    const sphere = new SimpleBody();
    sphere.setGeometry(new Sphere(1));
    const leaf = new CsgBodyOperand(sphere);
    const opaque = new CsgOpaqueTestExpression(leaf);
    const wrapped = new CsgPlacedExpression(opaque);
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(wrapped));
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(root, new Vector3Dd(), 0.1));
    const leaves = new CsgRayLeafBatch();
    expectEq(
        RaySolidQueryStatus.Unsupported,
        CsgRayLeafQuery.query(
            root,
            Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, 10),
            leaves,
        ),
    );
    expectEq(0, leaves.size());
});

test("CsgSolidEquivalenceContract_AssociatesAndDeduplicatesBooleanSolidSets", () => {
    const one = new SimpleBody();
    const two = new SimpleBody();
    const three = new SimpleBody();
    one.setGeometry(new Sphere(1));
    two.setGeometry(new Sphere(1));
    three.setGeometry(new Sphere(1));
    two.setPosition(new Vector3Dd(4, 0, 0));
    three.setPosition(new Vector3Dd(8, 0, 0));
    const a = new CsgBodyOperand(one);
    const b = new CsgBodyOperand(two);
    const c = new CsgBodyOperand(three);
    const inner = new CsgTree(CsgOperation.UNION);
    const flat = new CsgTree(CsgOperation.UNION);
    const nested = new CsgTree(CsgOperation.UNION);
    expectTrue(inner.addOperand(c));
    expectTrue(inner.addOperand(b));
    expectTrue(flat.addOperand(a));
    expectTrue(flat.addOperand(b));
    expectTrue(flat.addOperand(c));
    expectTrue(nested.addOperand(inner));
    expectTrue(nested.addOperand(a));
    expectTrue(nested.addOperand(a));
    expectTrue(flat.isSameSolid(nested));
    expectTrue(nested.isSameSolid(flat));
    const innerI = new CsgTree(CsgOperation.INTERSECTION);
    const flatI = new CsgTree(CsgOperation.INTERSECTION);
    const nestedI = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(innerI.addOperand(c));
    expectTrue(innerI.addOperand(b));
    expectTrue(flatI.addOperand(a));
    expectTrue(flatI.addOperand(b));
    expectTrue(flatI.addOperand(c));
    expectTrue(nestedI.addOperand(innerI));
    expectTrue(nestedI.addOperand(a));
    expectTrue(nestedI.addOperand(a));
    expectTrue(flatI.isSameSolid(nestedI));
    const minusFlat = new CsgTree(CsgOperation.DIFFERENCE);
    const minusUnion = new CsgTree(CsgOperation.DIFFERENCE);
    const wrong = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(minusFlat.addOperand(a));
    expectTrue(minusFlat.addOperand(b));
    expectTrue(minusFlat.addOperand(c));
    expectTrue(minusUnion.addOperand(a));
    expectTrue(minusUnion.addOperand(inner));
    expectTrue(wrong.addOperand(b));
    expectTrue(wrong.addOperand(a));
    expectTrue(wrong.addOperand(c));
    expectTrue(minusFlat.isSameSolid(minusUnion));
    expectFalse(minusFlat.isSameSolid(wrong));
    const translated = new CsgPlacedExpression(inner);
    expectEq(
        AffinePlacementStatus.Valid,
        translated.setAffinePlacement(new Matrix4x4d().translation(1, 0, 0), new Matrix4x4d().translation(-1, 0, 0)),
    );
    const different = new CsgTree(CsgOperation.UNION);
    expectTrue(different.addOperand(a));
    expectTrue(different.addOperand(translated));
    expectFalse(flat.isSameSolid(different));
});

test("CsgPointProofContract_EquivalentCompositeCanReuseProofWithoutInventingOwners", () => {
    const sphere = new SimpleBody();
    const quadric = new UncertifiedTestBody();
    sphere.setGeometry(new Sphere(1));
    quadric.setGeometry(new QuadricSurface(new Vector3Dd(1, 1, 1), new Vector3Dd(), new Vector3Dd(), -1));
    const a = new CsgBodyOperand(sphere);
    const b = new CsgBodyOperand(quadric);
    const known = new CsgTree(CsgOperation.UNION);
    const unknown = new CsgTree(CsgOperation.UNION);
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(known.addOperand(a));
    expectTrue(unknown.addOperand(b));
    expectTrue(root.addOperand(unknown));
    expectTrue(root.addOperand(known));
    expectTrue(known.isSameSolid(unknown));
    const proof = new CsgPointProof();
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithProof(root, new Vector3Dd(1, 0, 0), 0.01, proof),
    );
    expectTrue(proof.isAvailable());
    let relation = false;
    for (let i = 0; i < proof.getCount(); ++i)
        if (proof.getEvidence(i)!.isEquivalence()) {
            const fact = proof.getEvidence(i)!;
            if (fact.getPath().getOperand(0) === known && fact.getEquivalentPath().getOperand(0) === unknown)
                relation = true;
        }
    expectTrue(relation);
    const owners = new CsgPointAttribution();
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithAttribution(root, new Vector3Dd(1, 0, 0), 0.01, owners),
    );
    expectFalse(owners.isComplete());
    const witness = new CsgPointWitness();
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyWithWitness(root, new Vector3Dd(1, 0, 0), 0.01, witness),
    );
    expectEq(2, witness.getLength());
    expectEq(known, witness.getOperand(0));
    expectEq(a, witness.getOperand(1));
});

test("CsgDyadicContract_PreservesExtremeTermsCancellationAndNonRepresentableProducts", () => {
    const a = new CsgDyadic();
    const b = new CsgDyadic();
    const c = new CsgDyadic();
    const d = new CsgDyadic();
    const product = new CsgDyadic();
    const rounded = new CsgDyadic();
    const sum = new CsgDyadic();
    const recovered = new CsgDyadic();
    expectTrue(CsgDyadic.fromDouble(0.1, a));
    expectTrue(CsgDyadic.fromDouble(0.1, b));
    expectTrue(CsgDyadic.multiply(a, b, product));
    expectTrue(CsgDyadic.fromDouble(0.01, rounded));
    expectFalse(product.equals(rounded));
    expectTrue(CsgDyadic.fromDouble(Double.MAX_VALUE, a));
    expectTrue(CsgDyadic.fromDouble(Double.MIN_VALUE, b));
    expectTrue(CsgDyadic.add(a, b, sum));
    expectTrue(CsgDyadic.subtract(sum, a, recovered));
    expectTrue(recovered.equals(b));
    expectTrue(CsgDyadic.fromDouble(-0.125, c));
    expectTrue(CsgDyadic.fromDouble(0.75, d));
    const ab = new CsgDyadic();
    const ac = new CsgDyadic();
    const ad = new CsgDyadic();
    const lhs = new CsgDyadic();
    const bc = new CsgDyadic();
    const rhs = new CsgDyadic();
    expectTrue(CsgDyadic.add(b, c, bc));
    expectTrue(CsgDyadic.multiply(a, bc, lhs));
    expectTrue(CsgDyadic.multiply(a, b, ab));
    expectTrue(CsgDyadic.multiply(a, c, ac));
    expectTrue(CsgDyadic.add(ab, ac, rhs));
    expectTrue(lhs.equals(rhs));
    expectTrue(CsgDyadic.multiply(c, d, ad));
    expectTrue(CsgDyadic.subtract(ad, ad, recovered));
    expectEq(0, recovered.getSign());
});
test("CsgSolidEquivalenceContract_DyadicCertificatesHandleUnrepresentableAffineIntermediates", () => {
    const planeA = new SimpleBody();
    const planeB = new SimpleBody();
    const quadricA = new SimpleBody();
    const quadricB = new SimpleBody();
    const sphere = new SimpleBody();
    const translatedQuadric = new SimpleBody();
    const nearby = new SimpleBody();
    const nearbyQuadric = new SimpleBody();
    planeA.setGeometry(new PlanarHalfSpace(0, 1, 0, -1));
    planeB.setGeometry(new PlanarHalfSpace(0, 1, 0.1, -1));
    quadricA.setGeometry(new QuadricSurface(new Vector3Dd(), new Vector3Dd(0, 0, 1), new Vector3Dd(), 0));
    quadricB.setGeometry(new QuadricSurface(new Vector3Dd(0, 0, 0.1), new Vector3Dd(0, 0, 1), new Vector3Dd(), 0));
    nearbyQuadric.setGeometry(
        new QuadricSurface(new Vector3Dd(0, 0, JavaMath.nextAfter(0.1, 1)), new Vector3Dd(0, 0, 1), new Vector3Dd(), 0),
    );
    sphere.setGeometry(new Sphere(1));
    translatedQuadric.setGeometry(
        new QuadricSurface(new Vector3Dd(1, 1, 1), new Vector3Dd(), new Vector3Dd(0, 0, 2), 0),
    );
    nearby.setGeometry(new PlanarHalfSpace(0, 1, JavaMath.nextAfter(0.1, 1), -1));
    const linear = Matrix4x4d.identityMatrix().withVal(0, 1, 0.1).withVal(1, 2, 0.1);
    const inverse = Matrix4x4d.identityMatrix().withVal(0, 1, -0.1).withVal(0, 2, 0.01).withVal(1, 2, -0.1);
    expectEq(AffinePlacementStatus.Valid, planeB.setAffinePlacement(linear, inverse));
    expectEq(AffinePlacementStatus.Valid, quadricB.setAffinePlacement(linear, inverse));
    expectEq(AffinePlacementStatus.Valid, nearbyQuadric.setAffinePlacement(linear, inverse));
    expectEq(AffinePlacementStatus.Valid, nearby.setAffinePlacement(linear, inverse));
    expectEq(AffinePlacementStatus.Valid, sphere.setAffinePlacement(linear, inverse));
    const shifted = new Matrix4x4d().translation(0, 0.1, 1).multiply(linear);
    const shiftedInverse = inverse.multiply(new Matrix4x4d().translation(0, -0.1, -1));
    expectEq(AffinePlacementStatus.Valid, translatedQuadric.setAffinePlacement(shifted, shiftedInverse));
    const pa = new CsgBodyOperand(planeA);
    const pb = new CsgBodyOperand(planeB);
    const qa = new CsgBodyOperand(quadricA);
    const qb = new CsgBodyOperand(quadricB);
    const s = new CsgBodyOperand(sphere);
    const q = new CsgBodyOperand(translatedQuadric);
    const n = new CsgBodyOperand(nearby);
    const nq = new CsgBodyOperand(nearbyQuadric);
    expectTrue(pa.isSameSolid(pb));
    expectTrue(pb.isSameSolid(pa));
    expectFalse(pa.isSameSolid(n));
    expectTrue(qa.isSameSolid(qb));
    expectTrue(qb.isSameSolid(qa));
    expectFalse(qa.isSameSolid(nq));
    expectTrue(s.isSameSolid(q));
    expectTrue(q.isSameSolid(s));
});

test("CsgDyadicContract_IndependentRationalMultiProductFixtures", () => {
    // Generated by generate_csg_dyadic_reference.py; include inside a test.
    const samples: number[][] = [
        [
            -3.7330331283677326e185, 1.3431778985637978e33, 1.6796357772685736e109, -8.165905001053714e-47,
            1.6796357772685736e109, -8.165905001053714e-47, -3.7330331283677326e185, 1.3431778985637978e33,
        ],
        [
            -6.88228623102901e26, 2.3142722448488375e-136, 3.0486356204660422e-235, 5.138516056697074e271,
            3.0486356204660425e-235, 5.138516056697074e271, -6.88228623102901e26, 2.3142722448488375e-136,
        ],
        [
            3.2188631674335675e145, 1.9741116462497116e-299, -2.084091210337539e-298, 6.4042120650732316e206,
            -2.084091210337539e-298, 6.4042120650732316e206, 3.2188631674335675e145, 1.9741116462497116e-299,
        ],
        [
            2.36846315748946e-274, 2.0548061021926064e-103, -3.5891790723540243e-304, 3.241802726895603e-299,
            -3.589179072354024e-304, 3.241802726895603e-299, 2.36846315748946e-274, 2.0548061021926064e-103,
        ],
        [
            7.522506848792077e44, -3.2925642358401827e-109, 9.085889270938353e-280, -1.269005916043006e-68,
            9.085889270938353e-280, -1.269005916043006e-68, 7.522506848792077e44, -3.2925642358401827e-109,
        ],
        [
            4.0170818021491996e-52, -1.503962744460281e211, -5.637015449679619e198, -1.105363892522924e207,
            -5.637015449679618e198, -1.105363892522924e207, 4.0170818021491996e-52, -1.503962744460281e211,
        ],
        [
            -1.607420449968461e-88, 3.9794151738136345e84, -1.7463869717239765e238, 6.657435290204337e136,
            -1.7463869717239765e238, 6.657435290204337e136, -1.607420449968461e-88, 3.9794151738136345e84,
        ],
        [
            -3.3244913950352915e292, 1.472061396140665e126, -5.887159096551133e136, 6.889016089503485e-94,
            -5.887159096551132e136, 6.889016089503485e-94, -3.3244913950352915e292, 1.472061396140665e126,
        ],
        [
            -1.7931245052723795e38, 4.319457734671806e50, -8.173467706760488e-220, -1.0694518975893946e-153,
            -8.173467706760488e-220, -1.0694518975893946e-153, -1.7931245052723795e38, 4.319457734671806e50,
        ],
        [
            -3.0056456484128837e-263, -5.376864731643442e62, -7.241026859746713e-294, 5.537463077613209e210,
            -7.241026859746712e-294, 5.537463077613209e210, -3.0056456484128837e-263, -5.376864731643442e62,
        ],
        [
            6.307958108974099e-253, 3.640655940785684e202, -1.635940074637753e-153, 6.45434916833685e-107,
            -1.635940074637753e-153, 6.45434916833685e-107, 6.307958108974099e-253, 3.640655940785684e202,
        ],
        [
            -2.3799947164120036e-221, 1.6188726120993141e122, -1.470485490906472e30, 6.453203574194402e262,
            -1.4704854909064718e30, 6.453203574194402e262, -2.3799947164120036e-221, 1.6188726120993141e122,
        ],
        [
            8.977275907566851e301, 4.28640749615785e133, -9.206810562443074e-294, 3.821982648295339e84,
            -9.206810562443074e-294, 3.821982648295339e84, 8.977275907566851e301, 4.28640749615785e133,
        ],
        [
            -7.613870453092338e272, 2.077705276296764e-50, -1.0091010449943454e-92, 2.7085849591911402e-142,
            -1.0091010449943452e-92, 2.7085849591911402e-142, -7.613870453092338e272, 2.077705276296764e-50,
        ],
        [
            8.528333841641339e-41, -3.547799029292002e-131, 8.776984003328354e-287, -7.748944438375947e186,
            8.776984003328354e-287, -7.748944438375947e186, 8.528333841641339e-41, -3.547799029292002e-131,
        ],
        [
            6.109796454820813e144, 4.266586416146979e176, 7.903777154805458e-217, -1.287997102726055e-188,
            7.90377715480546e-217, -1.287997102726055e-188, 6.109796454820813e144, 4.266586416146979e176,
        ],
        [
            -1.1202712826106394e-236, -5.5516994081154494e-117, 3.9390542725460825e-283, -1.749722864059525e280,
            3.9390542725460825e-283, -1.749722864059525e280, -1.1202712826106394e-236, -5.5516994081154494e-117,
        ],
        [
            -9.497486183587847e199, 2.7602429443029284e178, -1.7107873274095026e-285, 8.604345316741094e117,
            -1.7107873274095022e-285, 8.604345316741094e117, -9.497486183587847e199, 2.7602429443029284e178,
        ],
        [
            -2.374616858546797e-86, -3.6681622662949025e173, 5.15860540027391e96, -1.6561791056530596e54,
            5.15860540027391e96, -1.6561791056530596e54, -2.374616858546797e-86, -3.6681622662949025e173,
        ],
        [
            -3.312588311631654e163, 1.706315241978033e-247, 9.184995175185509e-211, 6.225709473229865e142,
            9.18499517518551e-211, 6.225709473229865e142, -3.312588311631654e163, 1.706315241978033e-247,
        ],
        [
            1.000422459315142e-63, 1.6412751852368052e77, -5.832126357720083e-232, -0.7334312353753395,
            -5.832126357720083e-232, -0.7334312353753395, 1.000422459315142e-63, 1.6412751852368052e77,
        ],
        [
            -65482465099.075554, 1.897035745721583e270, 3.5323825683995527e252, -2.43655900824245e-65,
            3.532382568399553e252, -2.43655900824245e-65, -65482465099.075554, 1.897035745721583e270,
        ],
        [
            1.4413113835013667e-41, -8.906601388643636e-163, -5.418832536321637e-199, -4.8456361193258775e-79,
            -5.418832536321637e-199, -4.8456361193258775e-79, 1.4413113835013667e-41, -8.906601388643636e-163,
        ],
        [
            -4.181590311130585e-118, 1.0696906100124113e243, 1.6503968188475808e-80, 4.5910909741432645e-29,
            1.650396818847581e-80, 4.5910909741432645e-29, -4.181590311130585e-118, 1.0696906100124113e243,
        ],
        [
            1.2711497002013495e70, 8.367934334463934e-281, 0.23569167554993695, 4.248162141331969e74,
            0.23569167554993695, 4.248162141331969e74, 1.2711497002013495e70, 8.367934334463934e-281,
        ],
        [
            -9.754386476098129e204, 3.1519312572727045e202, 1.3815489276046865e-116, -4.014582763703507e-112,
            1.3815489276046867e-116, -4.014582763703507e-112, -9.754386476098129e204, 3.1519312572727045e202,
        ],
        [
            -3.252244844090893e-145, 3.043787062871693e-77, -1.6587967372102018e142, 1.9558296482896655e254,
            -1.6587967372102018e142, 1.9558296482896655e254, -3.252244844090893e-145, 3.043787062871693e-77,
        ],
        [
            -3.108399423801425e244, -88850929482688.83, 3.218672817889462e23, -0.007678100233466557,
            3.218672817889463e23, -0.007678100233466557, -3.108399423801425e244, -88850929482688.83,
        ],
        [
            1.475096578640742e226, -6.811420893102764e205, -1.4354534697280588e-244, 4.2853306651289776e-135,
            -1.4354534697280588e-244, 4.2853306651289776e-135, 1.475096578640742e226, -6.811420893102764e205,
        ],
        [
            -3.1853171651691833e-7, -1.4960088947107432e-111, -6.236500610335482e26, 2.817430613853865e-49,
            -6.23650061033548e26, 2.817430613853865e-49, -3.1853171651691833e-7, -1.4960088947107432e-111,
        ],
        [
            1.598727974352885e-50, -5.592377590096853e146, 3.894446138903622e131, 2.76030398696421e-231,
            3.894446138903622e131, 2.76030398696421e-231, 1.598727974352885e-50, -5.592377590096853e146,
        ],
        [
            -2.501340154137376e114, -1.6074199254372288e169, 1.5635021045091678e-123, 1.2357735248900158e211,
            1.563502104509168e-123, 1.2357735248900158e211, -2.501340154137376e114, -1.6074199254372288e169,
        ],
        [
            -3.3176328056737914e-287, 4.9096664989048435e-275, 4.949590285712365e21, 2.6816657911795705e-296,
            4.949590285712365e21, 2.6816657911795705e-296, -3.3176328056737914e-287, 4.9096664989048435e-275,
        ],
        [
            -6.076460063495255e127, -2.7939609142997954e-297, 2.505585732349482e182, 1.4808475325713136e-69,
            2.5055857323494822e182, 1.4808475325713136e-69, -6.076460063495255e127, -2.7939609142997954e-297,
        ],
        [
            6.0974104651231084e162, 7.636366026361583e129, -9.132044774287362e137, -2.444398847030967e221,
            -9.132044774287362e137, -2.444398847030967e221, 6.0974104651231084e162, 7.636366026361583e129,
        ],
        [
            4.2604129346660624e-251, -1.4032194537385732e-276, 6.812526392571963e-292, -4.214030125518122e-110,
            6.812526392571964e-292, -4.214030125518122e-110, 4.2604129346660624e-251, -1.4032194537385732e-276,
        ],
        [
            3.507838856064992e-25, 4.58540939070613e-264, -1.057052651972684e274, 9.524413946005908e102,
            -1.057052651972684e274, 9.524413946005908e102, 3.507838856064992e-25, 4.58540939070613e-264,
        ],
        [
            -1.7548102291114e-54, -5.527059606603321e169, -3.2340750892028793e68, -2.651989852314556e45,
            -3.234075089202879e68, -2.651989852314556e45, -1.7548102291114e-54, -5.527059606603321e169,
        ],
        [
            3.345833219447172e-126, -7.75892465538448e-54, 6.030166365258012e87, 6.112064494000076e-65,
            6.030166365258012e87, 6.112064494000076e-65, 3.345833219447172e-126, -7.75892465538448e-54,
        ],
        [
            -3.7149313725271587e74, -5.982857926492088e92, -942469035427.8147, 2.2859981458229022e-302,
            -942469035427.8146, 2.2859981458229022e-302, -3.7149313725271587e74, -5.982857926492088e92,
        ],
        [
            5.4891941478169985e-176, -6.804833034016725e213, 7.121741826588487e53, -8.787057939581328e163,
            7.121741826588487e53, -8.787057939581328e163, 5.4891941478169985e-176, -6.804833034016725e213,
        ],
        [
            6.15036827805588e124, 4.0767463231732826e228, 2.1351025669217544e282, 2.7665069710733335e-269,
            2.1351025669217547e282, 2.7665069710733335e-269, 6.15036827805588e124, 4.0767463231732826e228,
        ],
        [
            3.08615270919752e72, 7.880358033515071e171, 1.619406848544813e137, -1.6137491885675566e-22,
            1.619406848544813e137, -1.6137491885675566e-22, 3.08615270919752e72, 7.880358033515071e171,
        ],
        [
            -4.837402040236611e177, -2.9431820983432674e-62, 6.299905945868717e-164, -8.928215603354051e-277,
            6.299905945868718e-164, -8.928215603354051e-277, -4.837402040236611e177, -2.9431820983432674e-62,
        ],
        [
            -1.2597272380323253e-30, -5.944480184751589e239, 2.282546306127715e-240, 1.1382101679961589e-197,
            2.282546306127715e-240, 1.1382101679961589e-197, -1.2597272380323253e-30, -5.944480184751589e239,
        ],
        [
            -1.0184414517520422e125, 1.6129618311446505e-125, -5.944841689164767e-23, 1.4202452363762171e88,
            -5.944841689164766e-23, 1.4202452363762171e88, -1.0184414517520422e125, 1.6129618311446505e-125,
        ],
        [
            5.5594387286887995e-83, 5.4524996028767574e-279, -2.14723517405454e-92, -1.7657085801698805e179,
            -2.14723517405454e-92, -1.7657085801698805e179, 5.5594387286887995e-83, 5.4524996028767574e-279,
        ],
        [
            2.5990332775664424e-117, -1.1055146068765898e167, 9.465970854649858e-89, 5.24254870967035e-166,
            9.46597085464986e-89, 5.24254870967035e-166, 2.5990332775664424e-117, -1.1055146068765898e167,
        ],
        [
            3.654740592450134e-230, 3.739145761393342e160, 3.0007035001147604e-243, 2.7779163029972123e-235,
            3.0007035001147604e-243, 2.7779163029972123e-235, 3.654740592450134e-230, 3.739145761393342e160,
        ],
        [
            -8.261873893487503e290, 1.193013864899782e94, -3.4813351713706795e-183, 1.1915683148277657e-81,
            -3.481335171370679e-183, 1.1915683148277657e-81, -8.261873893487503e290, 1.193013864899782e94,
        ],
        [
            1.3420102586234615e-262, -7.136689517770563e211, 1.299571919401454e280, -6.236671526191603e-116,
            1.299571919401454e280, -6.236671526191603e-116, 1.3420102586234615e-262, -7.136689517770563e211,
        ],
        [
            9.56694351567766e-190, -1.3056637736172655e-136, 1.1285648350908653e-294, 1.0329988766127329e189,
            1.1285648350908655e-294, 1.0329988766127329e189, 9.56694351567766e-190, -1.3056637736172655e-136,
        ],
        [
            4.508731233354012e-68, 4.317909076224831e69, 1.144423050368453e-30, 1.496286299498546e45,
            1.144423050368453e-30, 1.496286299498546e45, 4.508731233354012e-68, 4.317909076224831e69,
        ],
        [
            -7.715827143852137e72, -2.8512204884307857e44, -1.4496260403762188e-71, 2.2203070216069362e-5,
            -1.4496260403762186e-71, 2.2203070216069362e-5, -7.715827143852137e72, -2.8512204884307857e44,
        ],
        [
            7.391928555330894e-65, 1.9570738224868335e-156, -1.1672283494258652e54, 1.7635552744359235e208,
            -1.1672283494258652e54, 1.7635552744359235e208, 7.391928555330894e-65, 1.9570738224868335e-156,
        ],
        [
            -4.611816888776438e-251, 1.2203888615875463e36, -3.1614705440767003e286, -5.435121399754464e-124,
            -3.1614705440767e286, -5.435121399754464e-124, -4.611816888776438e-251, 1.2203888615875463e36,
        ],
        [
            9.753682263658831e260, 4.511747060510323e158, 1.51794666669188e202, 4.896031018950748e-243,
            1.51794666669188e202, 4.896031018950748e-243, 9.753682263658831e260, 4.511747060510323e158,
        ],
        [
            1.5825390538674875e199, 8.431500561182195e282, 1.1449638582699321e-194, 1.4263229602324993e187,
            1.1449638582699323e-194, 1.4263229602324993e187, 1.5825390538674875e199, 8.431500561182195e282,
        ],
        [
            5.963586075247525e177, -2.1003321933111847e106, -9.189924847070014e114, -5.027025151799858e159,
            -9.189924847070014e114, -5.027025151799858e159, 5.963586075247525e177, -2.1003321933111847e106,
        ],
        [
            -4.297235931595411e38, -3.865789756660913e-163, 4.597361197137829e-73, -1.8540132507241996e-261,
            4.5973611971378296e-73, -1.8540132507241996e-261, -4.297235931595411e38, -3.865789756660913e-163,
        ],
        [
            5.1364737166200596e-61, -2.9254099817403264e87, -1.1943931951599125e279, -6.878290546220363e-130,
            -1.1943931951599125e279, -6.878290546220363e-130, 5.1364737166200596e-61, -2.9254099817403264e87,
        ],
        [
            6.731090225616083e253, 4.025592810671416e112, -8.890908986341237e-212, -8.500372889916737e111,
            -8.890908986341236e-212, -8.500372889916737e111, 6.731090225616083e253, 4.025592810671416e112,
        ],
        [
            4.45308766967458e-307, 2.6515527785280895e247, 2.631351366636861e-55, -6.276099977673153e182,
            2.631351366636861e-55, -6.276099977673153e182, 4.45308766967458e-307, 2.6515527785280895e247,
        ],
        [
            2.59020678025346e131, -1.7984183822969548e-301, -8.648676481502408e153, 2.2584006861835307e-164,
            -8.648676481502406e153, 2.2584006861835307e-164, 2.59020678025346e131, -1.7984183822969548e-301,
        ],
        [
            -8252.478796098592, 3.536596852681953e233, 5.824148126944195e193, 7.658005489755561e236,
            5.824148126944195e193, 7.658005489755561e236, -8252.478796098592, 3.536596852681953e233,
        ],
        [
            5.974165639536908e115, -3.101224383212041e-273, 6.573283082238611e-5, 4.987140942251239e-45,
            6.573283082238612e-5, 4.987140942251239e-45, 5.974165639536908e115, -3.101224383212041e-273,
        ],
        [
            -2.9117676962314614e259, -2.0390608664297871e-240, 6.532768094097536e28, -7.830400616069663e-163,
            6.532768094097536e28, -7.830400616069663e-163, -2.9117676962314614e259, -2.0390608664297871e-240,
        ],
        [
            7.311299578772319e-109, 5.7006580133801026e-241, 8.477555919061297e-123, 208260209.40178218,
            8.477555919061299e-123, 208260209.40178218, 7.311299578772319e-109, 5.7006580133801026e-241,
        ],
        [
            2.666231336854716e65, -2.0246103787991346e149, -5.550211414977764e63, 5.257143453736931e125,
            -5.550211414977764e63, 5.257143453736931e125, 2.666231336854716e65, -2.0246103787991346e149,
        ],
        [
            -2.4592927579866065e97, 1.2262607382815849e-114, 8.061982534126691e161, -1.680766714734466e205,
            8.061982534126692e161, -1.680766714734466e205, -2.4592927579866065e97, 1.2262607382815849e-114,
        ],
        [
            -1.2475516269075381e246, -3.095918825147202e-88, -5.840827138876534e90, -9.952173948200434e180,
            -5.840827138876534e90, -9.952173948200434e180, -1.2475516269075381e246, -3.095918825147202e-88,
        ],
        [
            9.738919385664217e-220, -1.158069337132867e83, 5.127687066058049e285, 8.324210661231141e107,
            5.12768706605805e285, 8.324210661231141e107, 9.738919385664217e-220, -1.158069337132867e83,
        ],
        [
            8.901897350039917e-69, -4.0225145036186585e-9, -1.3764059340959164e109, -3.1889844015431705e-7,
            -1.3764059340959164e109, -3.1889844015431705e-7, 8.901897350039917e-69, -4.0225145036186585e-9,
        ],
        [
            -5.949189048232499e-80, 1.8169468778586446e-129, -1.4129888079680064e259, -2.77561556838743e178,
            -1.4129888079680062e259, -2.77561556838743e178, -5.949189048232499e-80, 1.8169468778586446e-129,
        ],
        [
            1.532659232347459e242, 9.501919523002102e-192, 5.741966212127668e-75, 3.7163310059422663e180,
            5.741966212127668e-75, 3.7163310059422663e180, 1.532659232347459e242, 9.501919523002102e-192,
        ],
        [
            -4.7075267726139986e191, -1.3295816281086817e-104, 2.9037982495124905e86, -7.064380666210105e-153,
            2.903798249512491e86, -7.064380666210105e-153, -4.7075267726139986e191, -1.3295816281086817e-104,
        ],
        [
            3.979222495763611e-204, 2.804060517429806e83, -9.75653777975779e277, -5.692812167284859e-85,
            -9.75653777975779e277, -5.692812167284859e-85, 3.979222495763611e-204, 2.804060517429806e83,
        ],
        [
            4.02687602515712e259, -2.747959607609029e195, 6.351920447335006e230, 6.848372949463723e236,
            6.351920447335007e230, 6.848372949463723e236, 4.02687602515712e259, -2.747959607609029e195,
        ],
        [
            4.608077344274187e-121, 8.03789062165996e71, -4.7315125157485925e276, -1.3388486818860032e129,
            -4.7315125157485925e276, -1.3388486818860032e129, 4.608077344274187e-121, 8.03789062165996e71,
        ],
        [
            -1.7193352784655659e-264, -6.673703661994247e-77, 1.0898731387870534e-44, -2.642163806166883e16,
            1.0898731387870535e-44, -2.642163806166883e16, -1.7193352784655659e-264, -6.673703661994247e-77,
        ],
        [
            1.0231508117219427e-302, -9.935822532476157e-176, -3.333190373928815e-42, 1.2804387142519072e25,
            -3.333190373928815e-42, 1.2804387142519072e25, 1.0231508117219427e-302, -9.935822532476157e-176,
        ],
        [
            -5.259290162186766e-78, 100687895.80885622, 8.258476498504429e156, -2.8154182164412245e-108,
            8.25847649850443e156, -2.8154182164412245e-108, -5.259290162186766e-78, 100687895.80885622,
        ],
        [
            1.197257572163608e-272, -4.137594822824255e-250, -1.3276442041271721e104, 3.307343046540843e-17,
            -1.3276442041271721e104, 3.307343046540843e-17, 1.197257572163608e-272, -4.137594822824255e-250,
        ],
        [
            8.619641008778725e-93, 4.28159896148889e96, -1.6324683056296296e-212, -7.580219982819461e-114,
            -1.6324683056296293e-212, -7.580219982819461e-114, 8.619641008778725e-93, 4.28159896148889e96,
        ],
        [
            -5.852007848229879e142, 2.611364804096896e-191, 5.761459209808505e196, 3.0084208615281866e97,
            5.761459209808505e196, 3.0084208615281866e97, -5.852007848229879e142, 2.611364804096896e-191,
        ],
        [
            5.5017248811815974e91, 3.5844591877712182e149, -3.71480019243805e-107, -6.726132984487575e-223,
            -3.7148001924380496e-107, -6.726132984487575e-223, 5.5017248811815974e91, 3.5844591877712182e149,
        ],
        [
            -1.3483391176689536e-264, -1.6323181805997492e-163, -1.61463013590936e291, 1.901525888438652e-288,
            -1.61463013590936e291, 1.901525888438652e-288, -1.3483391176689536e-264, -1.6323181805997492e-163,
        ],
        [
            -1.3401044036234847e122, 1.7779541711627968e150, -2.1331517011400095e-36, 2.622479839896437e266,
            -2.133151701140009e-36, 2.622479839896437e266, -1.3401044036234847e122, 1.7779541711627968e150,
        ],
        [
            2.0969247157441076e-89, -3.374394079503057e112, 6.014405388571417e231, 1.0978766081195742e56,
            6.014405388571417e231, 1.0978766081195742e56, 2.0969247157441076e-89, -3.374394079503057e112,
        ],
        [
            9.409156716637501e139, -3.9186483660468975e-14, -0.19222798649364556, -5.113125759961135e-257,
            -0.19222798649364553, -5.113125759961135e-257, 9.409156716637501e139, -3.9186483660468975e-14,
        ],
        [
            2.0878586046295927e-207, -8.892006481702776e-223, -7.390629559701448e187, 1.279079527272464e-232,
            -7.390629559701448e187, 1.279079527272464e-232, 2.0878586046295927e-207, -8.892006481702776e-223,
        ],
        [
            4.060016600520147e-59, -2.2659685904722674e-94, -2.1804939028024535e260, 2.2932640748304915e150,
            -2.1804939028024532e260, 2.2932640748304915e150, 4.060016600520147e-59, -2.2659685904722674e-94,
        ],
        [
            5.781814721610255e17, -3.2221629273166714e-40, 3.092391469927914e270, 4.729577769929717e264,
            3.092391469927914e270, 4.729577769929717e264, 5.781814721610255e17, -3.2221629273166714e-40,
        ],
        [
            9.858664188989031e185, 9.669783341656287e-9, 5.880654435459268e-283, 8.849528373500819e106,
            5.8806544354592685e-283, 8.849528373500819e106, 9.858664188989031e185, 9.669783341656287e-9,
        ],
        [
            -1.8636734497890327e-121, 1.7152960147779137e188, -5.814961790527209e-34, -1.4414058904734655e-60,
            -5.814961790527209e-34, -1.4414058904734655e-60, -1.8636734497890327e-121, 1.7152960147779137e188,
        ],
        [
            1.9985588448133906e272, 4.1095738590139414e-85, -3.0263096078008646e264, 4.6901957174961943e111,
            -3.026309607800864e264, 4.6901957174961943e111, 1.9985588448133906e272, 4.1095738590139414e-85,
        ],
        [
            -3.870470974046632e-95, 2.5973044513047174e-292, 2.3924030723632666e270, 1.8070393689673888e-238,
            2.3924030723632666e270, 1.8070393689673888e-238, -3.870470974046632e-95, 2.5973044513047174e-292,
        ],
        [
            1.3276175169104848e-258, 8.969647260272784e29, -1.8199651492928157e133, 9.766871392955753e-214,
            -1.8199651492928155e133, 9.766871392955753e-214, 1.3276175169104848e-258, 8.969647260272784e29,
        ],
        [
            -1.7387919860200754e-223, 3.0317082469467975e-154, -2.2784757169611038e184, 2.0429255632236652e270,
            -2.2784757169611038e184, 2.0429255632236652e270, -1.7387919860200754e-223, 3.0317082469467975e-154,
        ],
        [
            2.60806184274861e-93, -1.5164304459101964e229, 1.0566145437697886e-7, -5.680536038775726e-222,
            1.0566145437697887e-7, -5.680536038775726e-222, 2.60806184274861e-93, -1.5164304459101964e229,
        ],
        [
            5.1855373590943337e272, -6.397097602437697e-233, -1.6553209744021905e170, -5.1314119199605615e-245,
            -1.6553209744021905e170, -5.1314119199605615e-245, 5.1855373590943337e272, -6.397097602437697e-233,
        ],
        [
            1.1714486972885234e-109, 2.6454275683297046e222, -1.5912746084892406e-187, 8.033989387983334e306,
            -1.5912746084892404e-187, 8.033989387983334e306, 1.1714486972885234e-109, 2.6454275683297046e222,
        ],
        [
            8.128548284037932e-65, -1.8873179510526354e-279, 7.694296140814679e167, -3.2378576117746366e-238,
            7.694296140814679e167, -3.2378576117746366e-238, 8.128548284037932e-65, -1.8873179510526354e-279,
        ],
        [
            4.1425279757540214e78, 7.78310795897707e-254, 1.703501557642389e152, -1.2286424851116772e-266,
            1.7035015576423892e152, -1.2286424851116772e-266, 4.1425279757540214e78, 7.78310795897707e-254,
        ],
        [
            2.1471850206457225e263, -3.4575612125832223e-289, -1.8695795880557032e-61, -1.2421211187075396e67,
            -1.8695795880557032e-61, -1.2421211187075396e67, 2.1471850206457225e263, -3.4575612125832223e-289,
        ],
        [
            -1.4580027649863185e-127, -1.4021440209019929e280, 9.006643163548727e201, -1.2952347589890274e269,
            9.006643163548728e201, -1.2952347589890274e269, -1.4580027649863185e-127, -1.4021440209019929e280,
        ],
        [
            2.686658743198016e-135, -4.811131324893322e-188, -2.4364587284794156e220, -6.165762273408137e-13,
            -2.4364587284794156e220, -6.165762273408137e-13, 2.686658743198016e-135, -4.811131324893322e-188,
        ],
        [
            1.9721933544759015e-290, -8.192932356361457e288, -1.945721299588694e235, -2.168140145740072e-103,
            -1.9457212995886937e235, -2.168140145740072e-103, 1.9721933544759015e-290, -8.192932356361457e288,
        ],
        [
            -2.801429566439158e80, -4.114269638068766e-253, -4.3068191436309304e203, 3.2843992375388053e229,
            -4.3068191436309304e203, 3.2843992375388053e229, -2.801429566439158e80, -4.114269638068766e-253,
        ],
        [
            8.77371813564829e-303, 9.149106070377738e167, -6.986669040165376e226, 8.845101104089331e186,
            -6.986669040165375e226, 8.845101104089331e186, 8.77371813564829e-303, 9.149106070377738e167,
        ],
        [
            5.784344428615481e-212, -8.506013268546048e-66, -1.5083479980969318e57, 1.2886242893121817e-172,
            -1.5083479980969318e57, 1.2886242893121817e-172, 5.784344428615481e-212, -8.506013268546048e-66,
        ],
        [
            -2.6538899569381125e-273, -1.6243154512350788e-50, 1.4424900960405123e-154, -6.311478047135594e-57,
            1.4424900960405125e-154, -6.311478047135594e-57, -2.6538899569381125e-273, -1.6243154512350788e-50,
        ],
        [
            -1.3470517552890433e-154, 3.744935676505583e35, -1.0243214160417884e-306, -3.232944545779544e-269,
            -1.0243214160417884e-306, -3.232944545779544e-269, -1.3470517552890433e-154, 3.744935676505583e35,
        ],
        [
            -1.4152710377066477e-14, -2.394794603175053e-299, -3.159454186755865e187, 2.7956204289925067e31,
            -3.1594541867558644e187, 2.7956204289925067e31, -1.4152710377066477e-14, -2.394794603175053e-299,
        ],
        [
            -7.583923243309757e-154, 7.49237154520412e-140, -1.135716977344959e301, -9.197912791404708e-180,
            -1.135716977344959e301, -9.197912791404708e-180, -7.583923243309757e-154, 7.49237154520412e-140,
        ],
        [
            -1.7369131359096e-218, -1.0222558461091326e278, 1.3924015486258168e41, -1.0274688400934376e-177,
            1.392401548625817e41, -1.0274688400934376e-177, -1.7369131359096e-218, -1.0222558461091326e278,
        ],
        [
            -1.0865239372186504e-287, -1.9578550776264366e74, -2.5877990884180432e-211, -1.4162678257724919e-160,
            -2.5877990884180432e-211, -1.4162678257724919e-160, -1.0865239372186504e-287, -1.9578550776264366e74,
        ],
        [
            6.6529528730972096e-15, 2.2655856311395934e-195, 1.6277501742952076e283, 9.846675015157389e281,
            1.6277501742952078e283, 9.846675015157389e281, 6.6529528730972096e-15, 2.2655856311395934e-195,
        ],
        [
            4.0110659172740915e253, -1.7607628934421424e99, 1.9890428195521208e-138, -1.430559916983714e16,
            1.9890428195521208e-138, -1.430559916983714e16, 4.0110659172740915e253, -1.7607628934421424e99,
        ],
        [
            -6.637268963705778e200, 1.6993869238024562e233, -5.142790877562717e-231, -4.5735294823918435e100,
            -5.1427908775627164e-231, -4.5735294823918435e100, -6.637268963705778e200, 1.6993869238024562e233,
        ],
        [
            -2.0333806996165823e261, 487439168526087.94, -2.359279368409551e-144, 1.205296207169888e-289,
            -2.359279368409551e-144, 1.205296207169888e-289, -2.0333806996165823e261, 487439168526087.94,
        ],
        [
            -3.2221753568481957e-259, 4.825018406544986e-163, -3.378833947077976e-107, 1.061198881437449e-91,
            -3.378833947077975e-107, 1.061198881437449e-91, -3.2221753568481957e-259, 4.825018406544986e-163,
        ],
        [
            1.1185197097340268e-216, -3136834285106434.0, 2.1947591610590286e-114, -489.87795984720987,
            2.1947591610590286e-114, -489.87795984720987, 1.1185197097340268e-216, -3136834285106434.0,
        ],
        [
            -2.5664721716920706e222, 7.921497838012075e248, -4.0293112896890584e27, 3.5155493631059297e-252,
            -4.029311289689058e27, 3.5155493631059297e-252, -2.5664721716920706e222, 7.921497838012075e248,
        ],
        [
            -6.293419273132944e-63, -4.395021403660688e-270, -2.6410394647149825e139, -5.754407152318488e-299,
            -2.6410394647149825e139, -5.754407152318488e-299, -6.293419273132944e-63, -4.395021403660688e-270,
        ],
        [
            -6.623637180945829e232, -4.549137154806341e305, 1.273070095068352e85, 1.472973840982109e66,
            1.2730700950683522e85, 1.472973840982109e66, -6.623637180945829e232, -4.549137154806341e305,
        ],
        [
            -2713.1324215419054, 2.774535482458747e139, 5.323689104832685e152, -4.411725380691035e-12,
            5.323689104832685e152, -4.411725380691035e-12, -2713.1324215419054, 2.774535482458747e139,
        ],
        [
            -1.897190263993292e-13, 1.604853344981152e-224, 3.533297150987541e300, -8.932525373453587e259,
            3.5332971509875415e300, -8.932525373453587e259, -1.897190263993292e-13, 1.604853344981152e-224,
        ],
        [
            -2.274700180486575e90, -1.06568323462379e271, 4.684098375476346e251, 3.550921599109519e-197,
            4.684098375476346e251, 3.550921599109519e-197, -2.274700180486575e90, -1.06568323462379e271,
        ],
        [
            -1.9563518718994032e-281, 950730725.211041, -6.134552762372639e44, 5.519665108022997e112,
            -6.1345527623726385e44, 5.519665108022997e112, -1.9563518718994032e-281, 950730725.211041,
        ],
        [
            -3224.0574011308877, 4.8085990998590574e-207, 1.1481446145105924e-201, -3.2281278242041378e-99,
            1.1481446145105924e-201, -3.2281278242041378e-99, -3224.0574011308877, 4.8085990998590574e-207,
        ],
        [
            -1.7037864471127627e-111, -4.259642833919062e188, -1.816105132633651e-128, -1.500120624504697e169,
            -1.8161051326336507e-128, -1.500120624504697e169, -1.7037864471127627e-111, -4.259642833919062e188,
        ],
        [
            1.3386099998869836e57, -2.5468626642680336e-81, -4.3097044393553155e-119, -6.42270192514562e254,
            -4.3097044393553155e-119, -6.42270192514562e254, 1.3386099998869836e57, -2.5468626642680336e-81,
        ],
        [
            2.3402035519888167e-58, 1.964844401423211e139, -7.026242733143304e-98, 2.2542997252429645e76,
            -7.026242733143302e-98, 2.2542997252429645e76, 2.3402035519888167e-58, 1.964844401423211e139,
        ],
        [
            -3475492.151385465, -1.103878789034536e-140, 2.461317217577513e-258, -1.7232629743062158e-133,
            2.461317217577513e-258, -1.7232629743062158e-133, -3475492.151385465, -1.103878789034536e-140,
        ],
        [
            -1.7378981460229556e-60, 2.8775680693210663e-176, 1.2412044213204908e78, -1.0223427078403803e43,
            1.241204421320491e78, -1.0223427078403803e43, -1.7378981460229556e-60, 2.8775680693210663e-176,
        ],
        [
            9.083768688536407e174, 1.5246434557292544e-34, -3.529069422199845e-246, 1.432831557236579e-307,
            -3.529069422199845e-246, 1.432831557236579e-307, 9.083768688536407e174, 1.5246434557292544e-34,
        ],
        [
            1.7010617456963342e-23, 3.931792350407734e264, -3.7451069496221644e-287, 3.3352486104238225e-215,
            -3.745106949622164e-287, 3.3352486104238225e-215, 1.7010617456963342e-23, 3.931792350407734e264,
        ],
        [
            -1.0979346656023493e37, -2.743049796721565e-19, -8.924401637573753e-213, 6.174084633055116e-63,
            -8.924401637573753e-213, 6.174084633055116e-63, -1.0979346656023493e37, -2.743049796721565e-19,
        ],
        [
            4.874125450233013e-51, 1.0844282486864817e231, -1.8704609413775374e-76, 2.1619109366100754e63,
            -1.870460941377537e-76, 2.1619109366100754e63, 4.874125450233013e-51, 1.0844282486864817e231,
        ],
        [
            -3.51884042473732e48, -3.4867979820511046e140, -6.646313830642757e51, -2.4918724130409282e-166,
            -6.646313830642757e51, -2.4918724130409282e-166, -3.51884042473732e48, -3.4867979820511046e140,
        ],
        [
            9.122382661521503e-129, -5.577640233586021e-26, 6.088415091869047e-234, 7.33404705680398e-202,
            6.088415091869048e-234, 7.33404705680398e-202, 9.122382661521503e-129, -5.577640233586021e-26,
        ],
        [
            -4.55229174509384e85, 3.4993887905695178e100, 9.20960733084276e183, 4.4561283644457324e-207,
            9.20960733084276e183, 4.4561283644457324e-207, -4.55229174509384e85, 3.4993887905695178e100,
        ],
        [
            -6.2131815158509175e-148, 4.7427628978497925e-70, -7.9927349889656295e-84, -1.1234260784157577e-284,
            -7.992734988965629e-84, -1.1234260784157577e-284, -6.2131815158509175e-148, 4.7427628978497925e-70,
        ],
        [
            6.640984853769492e-184, 4.0445633781555033e-91, -4.4109824793539816e154, 8.243335841729831e-221,
            -4.4109824793539816e154, 8.243335841729831e-221, 6.640984853769492e-184, 4.0445633781555033e-91,
        ],
        [
            1.839027796921707e-243, 4.509190653085758e-28, -6.043775185131478e-40, -2.99541354580616e57,
            -6.0437751851314775e-40, -2.99541354580616e57, 1.839027796921707e-243, 4.509190653085758e-28,
        ],
        [
            -1.125046262478984e-32, -6.2417443650670714e-173, -7.049555577066181e132, -4.040274235980734e286,
            -7.049555577066181e132, -4.040274235980734e286, -1.125046262478984e-32, -6.2417443650670714e-173,
        ],
        [
            2.6079268800897985e27, 3.927662019827889e168, -3.8947083668226084e-133, -1.9112730087945756e27,
            -3.894708366822608e-133, -1.9112730087945756e27, 2.6079268800897985e27, 3.927662019827889e168,
        ],
        [
            -1.1894777365529756e268, 5.775443593378759e-9, 2.9796701201165618e-123, -1.2500658427252383e-60,
            2.9796701201165618e-123, -1.2500658427252383e-60, -1.1894777365529756e268, 5.775443593378759e-9,
        ],
        [
            8.031092058400493e-213, 9.155702487974959e-163, -3.4068118822056355e28, 1.442481823655805e-176,
            -3.406811882205635e28, 1.442481823655805e-176, 8.031092058400493e-213, 9.155702487974959e-163,
        ],
        [
            2.2671939138303167e-133, 1.1357869420344932e291, 4.002384363680126e21, 9.1876699345964e42,
            4.002384363680126e21, 9.1876699345964e42, 2.2671939138303167e-133, 1.1357869420344932e291,
        ],
        [
            2.3453581256350768e107, -2.0260051109586788e74, 1.0332147372141075e116, 3.153070717013088e-114,
            1.0332147372141077e116, 3.153070717013088e-114, 2.3453581256350768e107, -2.0260051109586788e74,
        ],
        [
            1.8428332226259743e-118, -3.593503103007798e-248, 2.250085728465671e160, -3.2510559935715967e243,
            2.250085728465671e160, -3.2510559935715967e243, 1.8428332226259743e-118, -3.593503103007798e-248,
        ],
        [
            1.0460916217519148e-15, 6.998122057405849e203, -4.6480982069734976e-116, 5.739788346336858e16,
            -4.648098206973497e-116, 5.739788346336858e16, 1.0460916217519148e-15, 6.998122057405849e203,
        ],
        [
            1.8141702499390946e-168, 2.1027492842445315e249, -5.572365339308358e-119, -3.086394818293593e223,
            -5.572365339308358e-119, -3.086394818293593e223, 1.8141702499390946e-168, 2.1027492842445315e249,
        ],
        [
            3.9853500987155445e169, 7.407394228588691e-35, 1.1814880674970375e-274, 1.2643190260678749e154,
            1.1814880674970378e-274, 1.2643190260678749e154, 3.9853500987155445e169, 7.407394228588691e-35,
        ],
        [
            -6.424847391902745e-108, 4.682646913452184e300, 1.1349429175504218e136, -4.673790261773889e17,
            1.1349429175504218e136, -4.673790261773889e17, -6.424847391902745e-108, 4.682646913452184e300,
        ],
        [
            7.437984945572644e-24, 1.1948960220358407e-58, -4.824350778699535e-247, 1.2299856321527603e292,
            -4.8243507786995345e-247, 1.2299856321527603e292, 7.437984945572644e-24, 1.1948960220358407e-58,
        ],
        [
            -1.1911449158530802e-285, 3.268159552285599e102, 3.7217747065154393e192, 1.567589314505809e84,
            3.7217747065154393e192, 1.567589314505809e84, -1.1911449158530802e-285, 3.268159552285599e102,
        ],
        [
            -8.826092990634161e60, -1.113867386635061e-132, -1.6345053355934235e291, -3.515104454998208e-297,
            -1.6345053355934233e291, -3.515104454998208e-297, -8.826092990634161e60, -1.113867386635061e-132,
        ],
        [
            6.417393129467819e-54, 4.9860279138972975e-118, -1.394490027578798e-151, -6.560282533195282e210,
            -1.394490027578798e-151, -6.560282533195282e210, 6.417393129467819e-54, 4.9860279138972975e-118,
        ],
        [
            4.423842743158543e136, -2.8666559940928802e-148, 9.967043212297059e-39, 852545538.8906292,
            9.96704321229706e-39, 852545538.8906292, 4.423842743158543e136, -2.8666559940928802e-148,
        ],
        [
            3.2314535427519497e-234, -1.5532876204565597e-53, 2.895308115599642e-68, 2.960331240545054e-154,
            2.895308115599642e-68, 2.960331240545054e-154, 3.2314535427519497e-234, -1.5532876204565597e-53,
        ],
        [
            -2.7042755953452356e306, -5.319025055318306e118, 5.470491147758734e141, 7.139176763491527e-82,
            5.4704911477587345e141, 7.139176763491527e-82, -2.7042755953452356e306, -5.319025055318306e118,
        ],
        [
            -1.3159049741205859e-14, -7.93551768730825e-191, -7.521288611160933e104, 1.404321692489689e-229,
            -7.521288611160933e104, 1.404321692489689e-229, -1.3159049741205859e-14, -7.93551768730825e-191,
        ],
        [
            2.4862121511439077e-300, -873480459189.382, 8.371556596317403e-174, 2.857680438765775e145,
            8.371556596317405e-174, 2.857680438765775e145, 2.4862121511439077e-300, -873480459189.382,
        ],
        [
            -7.028919279432903e237, -4.127580582217243e-149, -1.0150703732699265e-106, 6.454362577425898e-81,
            -1.0150703732699265e-106, 6.454362577425898e-81, -7.028919279432903e237, -4.127580582217243e-149,
        ],
        [
            1.40288622681591e-132, -2.3319341114520145e-174, -3.450216299449213e298, 3.472303140144625e-141,
            -3.4502162994492125e298, 3.472303140144625e-141, 1.40288622681591e-132, -2.3319341114520145e-174,
        ],
        [
            -3.046499126996855e-278, -2.453980441765807e211, -2.5598744664734913e85, -5.105319028254883e238,
            -2.5598744664734913e85, -5.105319028254883e238, -3.046499126996855e-278, -2.453980441765807e211,
        ],
        [
            1.5465831081811238e108, -1.1949156386620651e114, 1.2926085070211218e278, -1.2281516570040433e266,
            1.292608507021122e278, -1.2281516570040433e266, 1.5465831081811238e108, -1.1949156386620651e114,
        ],
        [
            1.4865710365141617e-253, 2.9529180063165126e-162, 4261938377942656.5, -2.937414168572259e97,
            4261938377942656.5, -2.937414168572259e97, 1.4865710365141617e-253, 2.9529180063165126e-162,
        ],
        [
            1.5795397327411766e60, 1.0875943358265074e-49, 8.598544775916903e44, 6.843307441310566e-27,
            8.598544775916904e44, 6.843307441310566e-27, 1.5795397327411766e60, 1.0875943358265074e-49,
        ],
        [
            -5.344577864769716e124, -5.306191002400368e59, -6.142702026906499e-283, -0.328183038740872,
            -6.142702026906499e-283, -0.328183038740872, -5.344577864769716e124, -5.306191002400368e59,
        ],
        [
            -5.269418807152826e60, 1.0397574594747325e-159, -1.8793983232952184e-171, -9.161872915662464e-21,
            -1.8793983232952182e-171, -9.161872915662464e-21, -5.269418807152826e60, 1.0397574594747325e-159,
        ],
        [
            1.6448176523035907e150, 1.6989372620788125e-149, 1.20894617621669e55, 1.1261709905628447e101,
            1.20894617621669e55, 1.1261709905628447e101, 1.6448176523035907e150, 1.6989372620788125e-149,
        ],
        [
            1.2284585207631136e136, 4.514263938977945e60, 2.9209927189477865e-99, 3.824872397450143e174,
            2.920992718947787e-99, 3.824872397450143e174, 1.2284585207631136e136, 4.514263938977945e60,
        ],
        [
            3.4126395303489363e-60, -2.947116675320706e244, 4.249869742638309e-300, -2.7384644077357527e-54,
            4.249869742638309e-300, -2.7384644077357527e-54, 3.4126395303489363e-60, -2.947116675320706e244,
        ],
        [
            8.648621771793033e214, -1.802778496498488e-268, 4.452528592102956e-248, -7.289790387617135e116,
            4.452528592102957e-248, -7.289790387617135e116, 8.648621771793033e214, -1.802778496498488e-268,
        ],
        [
            5.789363927536056e65, 2.7373660306274307e-221, -1.4336595601980158, 6.452638693720721e85,
            -1.4336595601980158, 6.452638693720721e85, 5.789363927536056e65, 2.7373660306274307e-221,
        ],
        [
            1.2877678091875694e82, 8.498567802118256e129, 1.9802901081055183e267, 1.1309087374130818e194,
            1.9802901081055186e267, 1.1309087374130818e194, 1.2877678091875694e82, 8.498567802118256e129,
        ],
        [
            2.7504278221831486e-116, 1.5044807382042102e305, -1.2317163736555272e-229, -1.7500343425108987e144,
            -1.2317163736555272e-229, -1.7500343425108987e144, 2.7504278221831486e-116, 1.5044807382042102e305,
        ],
        [
            -6.26142811302045e124, 4.135811629618351e25, 1.8642366873096664e211, -1.8157334574028495e-99,
            1.8642366873096666e211, -1.8157334574028495e-99, -6.26142811302045e124, 4.135811629618351e25,
        ],
        [
            2.818331605205e60, -1.5195851010575032e259, 9.856852747625173e-247, -6356800.709419922,
            9.856852747625173e-247, -6356800.709419922, 2.818331605205e60, -1.5195851010575032e259,
        ],
        [
            -5.362962510121071e192, 0.0029316558458984047, -1.5374847279799357e-104, -5.283162670280671e-300,
            -1.5374847279799354e-104, -5.283162670280671e-300, -5.362962510121071e192, 0.0029316558458984047,
        ],
        [
            -2.4929310561531673e-88, -6.590796801334557e238, -2.9138750717635827e-72, -1.4052689874600912e215,
            -2.9138750717635827e-72, -1.4052689874600912e215, -2.4929310561531673e-88, -6.590796801334557e238,
        ],
        [
            -1.0678030602842875e145, 1.2622458199769379e43, 3.4055032472019225e-79, 5.401845398546876e86,
            3.405503247201923e-79, 5.401845398546876e86, -1.0678030602842875e145, 1.2622458199769379e43,
        ],
        [
            3.3643294029120614e205, -1.3557567602242817e139, 6.765139751622382e-87, -4.148124085278616e-183,
            6.765139751622382e-87, -4.148124085278616e-183, 3.3643294029120614e205, -1.3557567602242817e139,
        ],
        [
            -1.5945128286854804e-268, -1.2541948958843695e267, 0.21981549675420428, 2.861881431401957e17,
            0.2198154967542043, 2.861881431401957e17, -1.5945128286854804e-268, -1.2541948958843695e267,
        ],
        [
            -6.202484993469861e200, -1.6505045267631558e79, -7.537954517345172e-256, 7.494070132002147e-234,
            -7.537954517345172e-256, 7.494070132002147e-234, -6.202484993469861e200, -1.6505045267631558e79,
        ],
        [
            -2.048951760809616e216, 1.7228494680001825e-177, -6.58537756121127e-309, 1.4863551457185374e93,
            -6.585377561211266e-309, 1.4863551457185374e93, -2.048951760809616e216, 1.7228494680001825e-177,
        ],
        [
            1.9721712437253475e114, -3.328728985057936e265, 8.940044931381864e-231, -2.9440955569998665e171,
            8.940044931381864e-231, -2.9440955569998665e171, 1.9721712437253475e114, -3.328728985057936e265,
        ],
        [
            2.5510540291028366e125, 1.1242056137389128e234, 1.180741603947171e-226, 7.165840779112482e106,
            1.1807416039471712e-226, 7.165840779112482e106, 2.5510540291028366e125, 1.1242056137389128e234,
        ],
        [
            9.807320581429943e23, -2.606873096471573e155, 5.3296725004844166e73, -5.272148745843779e64,
            5.3296725004844166e73, -5.272148745843779e64, 9.807320581429943e23, -2.606873096471573e155,
        ],
        [
            -1.0572981950490212e-47, 1.1040181039943374e-226, 1.7481503716870023e21, 2.7611300375764593e155,
            1.7481503716870025e21, 2.7611300375764593e155, -1.0572981950490212e-47, 1.1040181039943374e-226,
        ],
        [
            2.6993800038048836e149, 1.56044036590521e-166, -1.906663687265736e285, 1.3154159883343814e-300,
            -1.906663687265736e285, 1.3154159883343814e-300, 2.6993800038048836e149, 1.56044036590521e-166,
        ],
        [
            2.2559982918758704e217, -8.711037166479981e-305, 1.2437909399269474e-134, -5.836919297295284e-142,
            1.2437909399269476e-134, -5.836919297295284e-142, 2.2559982918758704e217, -8.711037166479981e-305,
        ],
        [
            -2.4351661504760568e-290, -2.377434548803516e-303, 1.096923629196126e77, 2.3403089309266898e-135,
            1.096923629196126e77, 2.3403089309266898e-135, -2.4351661504760568e-290, -2.377434548803516e-303,
        ],
        [
            -1.122389833696915e239, 3.102541812652459e74, -4.9434915637874214e-85, -2.0605069543433384e126,
            -4.943491563787421e-85, -2.0605069543433384e126, -1.122389833696915e239, 3.102541812652459e74,
        ],
        [
            2.012174814956868e157, 2.423064748108303e-110, 1.9166216445181129e-209, 7.683509300603249e-304,
            1.9166216445181129e-209, 7.683509300603249e-304, 2.012174814956868e157, 2.423064748108303e-110,
        ],
        [
            1.2798132713493544e-158, 2.057739351932665e149, 4.145896882445747e302, -9.178172669114542e-26,
            4.145896882445748e302, -9.178172669114542e-26, 1.2798132713493544e-158, 2.057739351932665e149,
        ],
        [
            -1.8502239659278896e239, -7.262337490921386e-36, -2.3164242703959212e285, -2.0165436414002532e217,
            6.04526105253243e-202, -4.53861468174234e305, 5.098985810204591e82, 1.8372391780777827e-174,
        ],
        [
            1.8639624157608953e199, -6.291975863978799e156, 2.4060289173269534e166, 9.046659401533527e82,
            -6.169423452139422e-284, 1.6219565611864308e-54, 2.4760524226919062e85, 2.1732399855168969e-134,
        ],
        [
            3.40144417956402e162, 1.8622470007884595e-76, 2.4682820436600653e280, 2.484112269686782e-212,
            -4.3740964050281176e83, -8.409713031486065e129, -9.988095332953018e292, 4.7255631172114126e-186,
        ],
        [
            8.685012890920545e-191, -1.4728516790873216e264, -2.5407790457860523e-96, -9.165924602618608e-32,
            8.49497861715475e-153, -2.71600112221041e171, -4.3668843614519175e-90, -3.1127168723280114e-114,
        ],
        [
            6.618966497932484e-17, -4.589483599524345e163, 2.9522685508051207e-283, -9.001700963632641e269,
            2.387778237718366e239, 1.1218624739396252e46, -9.262305668255279e284, -1.718368072730968e147,
        ],
        [
            5.628569094339972e131, -4.847225035992776e88, 4.309697653569272e174, -9.659473584569498e-50,
            -2.370155930116326e-168, -4.4869071446383794e35, -5.617497274452031e-195, 7.786200650032387e-218,
        ],
        [
            4.606767172359355e-16, 2.8215548084699477e91, -1.3667686922055523e-170, 2.2315207265352273e195,
            -7.582555260406366e-160, 1.586610254659869e64, 4.2923672484015935e-18, -1.133584175370484e150,
        ],
        [
            -1.352397706467425e-220, -9.851148362277871e161, 6.87638145804692e-59, -5.320251756931475e-128,
            -2.3392599325519314e-305, -3.353990854021026e-32, -7.671983944985732e275, 4.16736438771683e-17,
        ],
        [
            2.7460876585183382e-148, 1.7848705222420272e253, 4.1046005461174286e232, -5.337144956466692e-127,
            -5.133421746158347e27, 4.156167032171562e-17, 1.2163654924392914e240, 1.3895598231158826e-191,
        ],
        [
            -7.448456389108523e257, 2.4690513472988983e-235, 3.613812002218747e167, 1.847249199970038e127,
            -2.8004444103179004e161, -7.87019245874299e87, 3.693680579165111e233, -9.546475896136166e-46,
        ],
        [
            3.224553194438898e48, 7.877009672680204e-73, 3.258194009988682e307, 2.1912440595377286e226,
            -6.852437239950332e-191, 1.6381489986242087e51, 0.015044656566104136, 1.1196754895444855e192,
        ],
        [
            4.8900021577693775e-67, -4.7675859354783204e262, -1.8720047484923325e-153, 1.5039825769840833e304,
            1.9953556376255955e-63, 4.2503703695175276e-15, 1.3452972098397218e296, 1.24017485305301e-282,
        ],
        [
            5.521690395380826e-237, -2.9588934052792238e-242, -1.676803325797071e-94, 1.6162876583576348e-74,
            3.1467019904659526e-179, -5.0854864782380375e-71, 3.488564789727038e149, 2.9391072017851023e158,
        ],
        [
            7.231039219945272e-241, -1.5990434860088242e91, 2.7797476019973683e90, 2.0208189244505055e-8,
            -3.203241502124233e136, -1.3513761965442866e156, -4.649252631311328e-194, 4.3978393462249784e197,
        ],
        [
            1.8282239451202697e245, 9.21867928049021e274, -1.1993377490479024e-260, 2.6344066804449636e-73,
            2.0737228858612628e-306, 6.422343753585918e-180, 6.212095030132085e-224, 1.1063499460720328e104,
        ],
        [
            -2.0736018033839075e81, -1.0026856918514236e34, 2.213192350159441e296, 3.915234164451797e110,
            6.955283752595661e-44, -2.352052924922191e-32, 2.6216976309777747e303, 1.1156847465470693e-211,
        ],
        [
            -9.95536363194837e-269, 9.398993668868804e-172, 1.0818878652564259e-69, -1.7729527818054283e249,
            -6.513190053129877e60, 9.112991431321878e-269, 2.4813216527741543e162, -3.708481050590221e161,
        ],
        [
            -2.171884123772015e162, 3.6929888091773107e-210, 1.471535714593331e-82, 1.8300730096947094e-284,
            2.3484637044698166e295, -1.2402680184158271e-243, 8.045894187720407e128, 3.579474375820837e-23,
        ],
        [
            1168299394737.385, 3.2354604846553824e114, 1.4598525036148011e28, -5.536165867034931e-27,
            2.588321264896027e-305, -7.123947741197836e-285, -9.003152303634586e-68, 4.2772735037209325e-180,
        ],
        [
            -1.2778187770584331e194, 2.5247435799418303e283, -7.466752630428773e-45, 3.072656606506617e240,
            4.585410779876105e177, 4.43040680988291e-242, 5.677596002344739e124, -9.967519000385273e-97,
        ],
        [
            -5.724970698189577e-200, -4.0999690618680064e138, -1.2038958068114522e-229, -6.030075612991588e237,
            -9.310696153575794e-158, -3.313751088383738e-95, 1.1561555018149417e-135, 4.2520514218385835e-35,
        ],
        [
            1.2217384135401212e204, 2.3216920897302172e-151, -1.4187734741110428e-76, -6.259762941569559e-248,
            1.054669101263686e-208, -3.595516367498866e-95, -1.6702020891074765e-269, -2.7844345685762056e-303,
        ],
        [
            4.314069502895041e61, -3.24585469568673e-98, 4.900236269208999e269, -1.1036579430899434e109,
            7.960223194854464e-175, -3.247867254047467e-15, -5.922246614355339e40, -3.9117635266054363e43,
        ],
        [
            3.606688160638986e236, 1.240647074037791e-51, -4.7034076901412895e22, -9.096123957505136e-165,
            -2.5422132560286126e30, -7.806646767903173e115, 1.3870160760892849e138, -6.623297617378882e228,
        ],
        [
            -2.2402273202607747e-83, -3.517106094619343e96, 5.533583228267927e179, 7.217784264001632e33,
            1.8868362415367027e-100, -7.621865532912786e193, 1.3724761212035984e-39, -4.210669717525546e-305,
        ],
        [
            -2.540362166620457e-300, 3.423601773896386e-195, 2.8584177831370053e-31, -1.6133440225594176e193,
            8.620309851170647e198, 1.3251792959157007e-260, -3.577706134409812e-240, 8.491969205198465e-294,
        ],
        [
            6.528009101657366e-207, 4.040409760773277e-198, -9.318616318312371e271, -1.1882695389054199e-33,
            -2.7384800708446644e187, 1.9217861157792408e-206, 1.4375452954866447e262, 2.6745512017207186e-95,
        ],
        [
            -3.2443307766824847e-158, -1.4184757833629337e-157, 1.4625736846341136e62, 2.517791870185763e-99,
            2.1116595968055858e-69, 1.3656566585307965e-285, -7.481535905005221e121, -3.0167717273233783e24,
        ],
        [
            2.774628673845787e-110, 5.902829733417547e-143, 9.81129468085382e-185, -1.8177346956585624e136,
            -1.4073834261261872e-301, -6.808368127184954e-114, -1.3481245420281207e75, -1.7968702861456916e257,
        ],
        [
            -3.0970043548216545e-84, 4.6144417482699075e-306, -5.139562311921808e-231, 3.638108980123338e103,
            2.077897787253789e-249, -6.845709291096623e-217, -3.1286270058060436e176, -1.6824562157156742e118,
        ],
        [
            3.9043131199764946e-284, -1.3121102350922629e-98, 7.140285750431867e98, -1.7219916213384e-213,
            -1.6221648929265162e62, 8.848854708558701e98, -9.420243587279732e124, -6.935690930547213e-230,
        ],
        [
            6.475548918872518e210, -2.918190075797672e-168, 4186886790.252887, -7.395011361773564e-118,
            -1.0915562873630316e-94, 1.3804598448959677e43, -1.768965732252906e-187, 1.8725224229176795e-180,
        ],
        [
            1.4052859404561347e-21, -3.5717356169541135e228, -2.069349550399238e68, -6.592020450779297e72,
            2.8904023479693e-60, 7.092387288059518e-159, -1.3772173011109334e44, 2.593599843979769e255,
        ],
        [
            9.788785520605726e-256, 4.2634070519197195e85, -2.4866413485061077e76, 5.547619857123953e41,
            -4.232393363484271e241, -6.15976647120285e299, -1.275306952759952e85, -1.6402947024518086e-172,
        ],
        [
            1.9155358574996415e61, 6.749557099031796e217, -3.803930514774427e-266, -4.8291480937264755e209,
            -3.632341629481933e147, -2.3251837292900605e-62, -1.4020833948465061e-268, -2.9483976392879104e268,
        ],
        [
            -5.40131258707922e69, 1.1083538045424327e141, 3.1151812546382456e-123, -1.917294380871031e299,
            7.102031418582876e-254, -1.3301851538356524e-63, 5.423085086366137e-62, -3.7794598644334565e79,
        ],
        [
            1.179808364482288e172, 5.629531344153826e-292, -4.788318686648078e223, -4.081367615561116e-245,
            -3.735373086635878e-47, 3.6678104466786504e-44, 4.208578485303675e73, 1.4344783428543919e69,
        ],
        [
            -6.173477732314354e260, -6.905701570919647e-190, 3361505427084673.5, -4.759345841481497e114,
            -8.438509820764393e267, -1.1374195920898992e121, 4.853150229317329e-203, -9.654906125561438e-235,
        ],
        [
            -6.443248668509287e-78, -3.537318060927153e145, -1.3418494364602265e48, 1.444550916956021e-200,
            6.774840771933174e66, -4.314983008822866e-251, 3.1619644818917696e106, -2.268180174422838e-222,
        ],
        [
            7.792072831596356e-35, -5.777692966455219e45, 6.996116833598191e-249, -1.3356183267062878e-117,
            2.973868323097363e261, -2.0326725723895108e60, -4535072858992773.0, -3.801565744994078e146,
        ],
        [
            -1.2815954412523339e-105, -1.0430537829359319e288, 8.142198511896114e196, -4213289849.5353103,
            3.7327001683366774e-53, 1.5673790248996094e-190, 6.577675812278701e-150, 3.6642527360851696e-191,
        ],
        [
            -1.3495230217216614e301, -6.8082310084544386e190, 7.69062544314148e33, -7.508372429139844e181,
            -9.234017465153773e-60, -2.9220357488297136e161, -6496.09331088389, -788424279736285.9,
        ],
        [
            -6.230556306110122e197, -6.744106101604732e-236, -5.249143561910097e-19, -9.482739419079432e-6,
            6.135096996977428e-245, 4.304754586207976e176, -4.2304258788745534e23, 2.5256745453286868e-45,
        ],
        [
            -3.9540351001693164e-42, 8.29040545237749e169, -4.5849792492251124e-294, 1.1721530270904409e-270,
            -2.9760655134915265e188, 5.853844120382711e-28, 4.607316794366144e-94, -2.566452578349062e-70,
        ],
        [
            -1.0082143573195243e206, 2.984475106889321e281, -1.2706427944764644e78, -4.496522492285564e299,
            9.817073612546596e91, -9.151955235315138e302, 1.6285964801451255e161, -1.902440495679218e-119,
        ],
        [
            6.501021668752324e215, -3.600613115078052e46, 1.454176978813194e163, -5.043082285046368e290,
            2.7480711023999567e-8, -2.9671632116378964e284, 1.4639152061033872e47, -5.819552024809884e-260,
        ],
        [
            7.345470243844488e53, 9.058634406445347e-107, -3.979059168575828e-129, -1.3900850410418664e-123,
            -1.2264291611451587e89, -1.6614529733988218e-273, 6.007381698997152e137, 1.0593572307991214e-249,
        ],
        [
            1.056622122849082e-83, -3.5309400881789196e122, -1.1826647021202146e191, -9.177343514985997e-5,
            -2.8034720425467336e160, 4.576673933728602e-299, -5.777290401845769e-108, -1.289732481034718e56,
        ],
        [
            2.266407029716307e-14, -7.682411116588704e-7, -2.389555693420435e-252, 1.0617619604280162e-118,
            -152651987458.79852, -6.66701643340944e29, 1.057627090949817e-49, 6.311960856284358e31,
        ],
        [
            3.771142064921072e207, 1.1546362169679819e35, 1.0933255970192515e107, 3.0003406431819904e182,
            2.6441494235573084e-79, 1.8231000940432968e-134, -4.9763568750633465e206, 3.3507811654519636e136,
        ],
        [
            -6.784058133205084e213, 1.9911924969075012e-286, 4.458783079901519e154, 6.839181756171205e270,
            -1.1528531004349722e100, -1.6144306044120997e297, -1.015690870155626e-215, -1.5863577374643627e-158,
        ],
        [
            1.8357528934358526e144, -4.746833068359222e-235, -1.1424842420178811e55, 1.7098516312803093e-11,
            -2.164373949461089e-64, -1.4188393952347354e-274, 2.293463099057556e102, 1.112136785453103e46,
        ],
        [
            -2.843785225828669e212, -8.890625023905968e121, 1.0981552329161681e-127, -4.49600411884298e-250,
            3.7629466155183e-153, 5.445255146687639e-147, -1.3086599415196899e177, -1.0684468691805183e-37,
        ],
        [
            -1.5928557408797546e97, -1.8642124979402793e-214, -2.0280945302038973e95, 2.04497922739383e-82,
            2.1905296333876246e145, -3.4296310864485396e166, 3.1619439412418954e-299, -1.1849453382655345e-159,
        ],
        [
            -3.691206683529862e79, 3.213381211507038e-240, -1.7533079489866345e-130, -3.190758041905072e-221,
            1.2759053592028586e-205, 1.4546335437987354e-293, 1.3151403233685687e-280, 3.6114679169749354e-132,
        ],
        [
            -5.470167392186941e111, 1.9136956084967438e201, 2.4185829564225527e138, 1.2171887341419434e142,
            -2.1561924387867742e-26, -1.2139330349436127e-49, 7.655598838184641e43, 1.836112728135509e-33,
        ],
        [
            -4.7056003989990486e-107, 1.945158464965445e-124, 3.0225756692955806e-57, 4.4064452468557966e-77,
            -6.810218301050884e-32, -341728635370.19543, -2.7399773000387357e152, -1.5042038816422e-84,
        ],
        [
            -3.0806282963052103e184, 1.698027537756885e143, -1.0341119719413259e-79, 1.4168900202249327e86,
            1.1706356773603543e273, 1.6311734454648375e97, -1.9153472523967302e56, 2.943809489743556e-50,
        ],
        [
            3.663552581682457e-146, 6.396176299398844e225, -3.2545712062436747e-283, 1.9225528399196537e65,
            7318287599.750119, 6.752935905750005e-120, 1.0264625516878353e-25, 1.669909182688412e123,
        ],
        [
            2.5395403524099478e-85, 3.039919371935063e-110, -3.097298990860468e134, -3.912422115107602e64,
            1.0923011347991651e161, 2.408652463386256e34, 1.9968913417333217e74, -3.138811395182755e-12,
        ],
        [
            6.744621887419848e18, 2.3749426110491994e-143, -1.760795842657093e-149, 2.562883558912555e268,
            2.9971400091097087e243, -1.9810201628866084e-66, 6.693440378514478e-220, 3.3517876602806133e-93,
        ],
        [
            8.875822450042208e234, -1.2701957201910638e185, -2.850973016562286e-37, -1.7946511863140525e-233,
            1.3979013246913145e-229, 1.0449429449939334e-223, 1.3835840846453416e-154, -4.9851122509449334e132,
        ],
        [
            4.610716423219413e-91, 3.575467084364562e183, -1.46599048358744e-206, 1.7737928113586943e-50,
            2.4120670743632556e-84, -3.367622602081365e-156, 3.2915025070023987e-77, 4.403880106783015e76,
        ],
        [
            -4.458553941146e-233, 1.337806001752559e-66, 2.930001540760009e123, -1.715841760660509e35,
            4.263090167109817e-218, -1.64709806285966e257, 6.942825679511176e267, -5.411202643410597e185,
        ],
        [
            -2.379087449046204e282, -2.5120230060555547e-96, 2.874468954504182e-6, 1.317331440572546e-9,
            -8.676277556132414e231, 1.5504020480661914e175, -1.117488009967553e-138, -7.434101824064763e77,
        ],
        [
            3.9809440209804665e-248, 1.0674818190510325e189, -2.107019355178158e95, 2.8292201256145517e252,
            -1.722730467629566e283, 4.3163721966805134e296, -1.659616500654163e-245, -2.495664038243057e106,
        ],
        [
            -1.0708181711826969e-144, -9.138677205663008e249, 2.7523164117948687e-242, 4746036328511.786,
            1.1728446283311199e278, 5.2113049757214564e-247, -2.207884654800514e189, -1.8618559091806325e204,
        ],
        [
            2.2762668948456666e-248, 2.4246323739457108e-60, 1.6415761894048848e-40, -2.4531175306145666e299,
            -3.6620304286406696e155, 1.8409442084235746e118, -6.435616591528462e-283, -7.850581094585919e164,
        ],
        [
            -3.354785453247383e156, 2.7982188817899118e-61, -9.767080821618565e284, -7.829526328805375e53,
            9.182704132918469e-240, -2.0543600250962603e104, -1.9137748730754866e99, 4.2909146177423285e-156,
        ],
        [
            -1.786122028437698e254, -6.503956498552222e-31, 7.956294058381904e161, 5.964191904447549e-145,
            -7.1214599946471e-24, 4.5113357379232524e-302, -3.3901093842578886e-96, -5.370463022087876e174,
        ],
        [
            -7.159631713883816e293, -3.5632319603273543e-56, 1.3428286202369798e172, 2.921808014145509e-236,
            2.3922711214162055e126, -2.5880045522514326e-123, -1.4829425322934514e195, -4.518087043312364e286,
        ],
        [
            6.947327250918598e-171, -3.4173469102141113e-116, 6.0347145622704714e206, 8.946583691139523e265,
            9.040043461434936e-29, 5.355584732235668e277, -2.6531571585755786e178, -32653818.154089667,
        ],
        [
            8.889641677983859e-246, 6.353999596681178e-74, -6.744017908152492e-211, -4.8956605421809616e-178,
            4.868063737068178e-85, 8.433468054436995e-302, 1.3960279077820723e-45, 3.9953872514386946e-308,
        ],
        [
            -4.6759577245197486e-226, -1.0586104302391704e244, -1.7576245121071147e-69, 2.5310694783677207e-141,
            -1.8847012145162957e304, -5.128154156222867e165, -5.1466153580034426e247, 1.1273801079415065e-228,
        ],
        [
            -6.463739891900919e-45, 3.1034082445635566e-93, 2.4377911990930414e-43, 1.324652844636785e181,
            -8.31510471188406e276, -8.961584544521975e-116, -6.404915840923619e41, 1.2743538244465306e280,
        ],
        [
            -2.8194155501083676e-67, 6.026162965681591e-176, -1.3309725367353045e-133, 4.881814397301172e-183,
            -3.7995925619187896e202, -3.001343755839788e117, 1.5565948413450626e230, -1.0930275768930496e-286,
        ],
        [
            -3.4621895677449056e-115, -1.3451993373344269e141, -5.667517736026117e193, -1.3627378229345482e-134,
            7.068760541225957e-182, 2.5053849981655135e220, 3.963122077166998e153, -1.214220322661424e-155,
        ],
        [
            -9.113816942813045e131, 1.3192326897789558e-174, -1.124503673455557e-134, 2.0864144971367595e279,
            2.3743662319166426e239, -1.7370503730042036e-22, 1.8029685827381678e108, -2.4441309649440025e-70,
        ],
        [
            2.285784729006189e203, -1.0017677791746904e-32, 7.155176377689897e-127, 4.1029889122269305e159,
            1.088250387796752e-53, -1.4161738599446715e-29, 3.583903929697156, -1.319679097041285e31,
        ],
        [
            4.052442680281827e40, -9.345369185341402e-84, -1.5072170865239492e-158, 2.0471570308781046e23,
            -4.23837344150287e-91, -1.482564895744171e-178, -1.5174008614639592e85, 1.0345024103293871e217,
        ],
        [
            -1.2973005416198294e-171, 5.415544751460591e214, 2.1361754295753502e151, -3.733675814675932e-262,
            -2.0114834390821637e165, 3.969052148650303e-156, -1.16570901254779e40, 1.3058878259221235e91,
        ],
        [
            -2.514794025769467e279, -1.0702944450833244e-277, 3.3126417295938643e-187, -0.01810386755845404,
            7.991871990058072e-106, 1.2086316648242356e-40, 1.4229139335640722e-179, -2.1030623466496307e289,
        ],
        [
            -2.0784475305714673e159, -8.88931370355716e-227, -6.718550900231419e-240, 7.732704685299097e-9,
            -3.689483910040857e207, -2.3089363033846483e291, -0.01862280743320969, 5.278995449134648e145,
        ],
        [
            3.1128095395931803e-27, -630182081479906.1, -5.7673453695941806e-282, 1.80061710242996e-201,
            -1.929687448561692e100, -3.924875566427565e-231, -2.240831193261547e186, 7.659365035168623e267,
        ],
        [
            -1.8039414056576864e-148, -3.3246944721634225e231, -69.61361318895719, -5.6909066030236454e-260,
            4.488894617903747e-179, -0.009067205739652582, 1.946537006322745e149, -3.642767576633696e139,
        ],
        [
            3.143814322063343e-206, 8.578839430414377e64, 9.738440411107538e89, -4.988323913989256e33,
            -1.6613938870834958e-214, -5.189111717924899e167, -5.3499076047696815e-135, -7.560478791676783e-293,
        ],
        [
            4.9585526764187177e-206, -9.34660474777862e-103, 1.7607991840666477e-265, -1.3374461055195438e-98,
            1.622366629804142e-181, -2.3728654360361457e111, -1.5150929859673723e-136, 7.582069373346781e-266,
        ],
        [
            -6.798989235493524e280, -7.826828047575624e-250, -1.1746572267367028e-69, 1.6575689689360125e-183,
            -3.2445074725217406e124, -6.97344670833503e-158, -6.445869360815495e-57, 2.899352791134238e-176,
        ],
        [
            -4.949115331417338e-254, -9.976151987173576e50, -2.3042791453944608e-147, -2.2371619979815118e50,
            -1.351489288933185e-63, 1.1397254281245203e-307, -3.490354766350727e78, 5.439750441129213e-287,
        ],
        [
            6.874749990434286e212, 3.884700719969346e-275, 7.401806475118634e221, -2.448001866255576e49,
            1.0029201122068579e217, 4.157752602179982e224, 9.418668546406462e-34, 4.45916599343486e56,
        ],
        [
            3.118199921743793e96, -1.909750763033251e-6, -3.1469826802037176e-111, 3.8387929819566164e-19,
            2.409917988069155e262, 4.619670912206212e90, 3.6429877185275476e35, 1.074506970851378e-46,
        ],
        [
            -6.466263667299916e-74, 3.735543251883152e227, -1.8433362486678314e231, 2.623014976568825e105,
            -2.2472966665607585, -5.547903738091454e162, 1.4820254920101444e117, -7.724228145531335e-19,
        ],
        [
            -1.4835896654431496e231, -1.821759446718445e239, -1.2824426853662623e180, 1.992904659611175e-111,
            2.7982794256936603e-237, 8.913348514317377e269, -1.0987834761910571e-270, -2.6280086859964832e-254,
        ],
        [
            -5.758171977909548e-124, 1.5128338188158316e-85, -1.3708614471283084e161, 2.0889732666780627e138,
            -5.054093652818585e207, 6.1944710340576655e57, 3.050366378888646e34, 1.3566942253592892e-94,
        ],
        [
            8.1793801368355e-247, 2.5033209840912677e-166, 3.7467410312498764e-33, 2.7866540299244583e268,
            -1.0146784015215847e-183, 6.161485381943288e-250, -6.76200735464373e-128, -1.4472757920298082e-104,
        ],
        [
            -8.468256153248943e-262, 3.3248706946868967e244, 2.765698820345754e-129, -1.5949899040258082e168,
            -1.3536711261905563e77, 2.027503938879113e-303, 2.5827707590298837e-267, -8.338119881774683e257,
        ],
        [
            -1.3271945626061713e-263, 2.5553714093976045e103, 2.3877188093210674e-28, 3.682430110097993e101,
            1.5806131682198003e-149, 2.645826026390156e-50, 4.309419293843203e-78, -8.127039139594274e45,
        ],
        [
            -3.555985484715962e-294, 1.4634514743561457e72, 2.715006618374452e154, -1.1078977801411927e-62,
            -1.5453132095509014e119, 9.87367930370354e144, 1.0582933147830172e69, -6.845198607571166e165,
        ],
        [
            3.237365797596925e-168, 2.095597335466844e59, 4.092731953246204e-232, -1.1351332944043114e-16,
            3.0372088098012565e34, -6.605396825667318e237, -6.169875680359674e292, 1.1676258237620457e-160,
        ],
        [
            7.289928414813284e-241, 0.00038870030354658464, -5.345427861522905e218, -2.631150144537471e277,
            3.0573252241189337e-262, 6.324864229404418e-217, 1.9764129226370436e145, 1.6333963666503483e169,
        ],
    ];
    const expected: boolean[] = [
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        true,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
    ];
    for (let caseIndex = 0; caseIndex < 300; ++caseIndex) {
        const x: CsgDyadic[] = Array.from({ length: 8 }, () => new CsgDyadic());
        const l1 = new CsgDyadic();
        const l2 = new CsgDyadic();
        const r1 = new CsgDyadic();
        const r2 = new CsgDyadic();
        const l = new CsgDyadic();
        const r = new CsgDyadic();
        for (let i = 0; i < 8; ++i) expectTrue(CsgDyadic.fromDouble(samples[caseIndex][i], x[i]));
        expectTrue(CsgDyadic.multiply(x[0], x[1], l1));
        expectTrue(CsgDyadic.multiply(x[2], x[3], l2));
        expectTrue(CsgDyadic.multiply(x[4], x[5], r1));
        expectTrue(CsgDyadic.multiply(x[6], x[7], r2));
        expectTrue(CsgDyadic.add(l1, l2, l));
        expectTrue(CsgDyadic.add(r1, r2, r));
        expectEq(expected[caseIndex], l.equals(r));
        const combinedLeft = new CsgDyadic();
        const combinedRight = new CsgDyadic();
        const p00 = new CsgDyadic();
        const p01 = new CsgDyadic();
        const p10 = new CsgDyadic();
        const p11 = new CsgDyadic();
        const t0 = new CsgDyadic();
        const t1 = new CsgDyadic();
        expectTrue(CsgDyadic.multiply(l, r, combinedLeft));
        expectTrue(CsgDyadic.multiply(l1, r1, p00));
        expectTrue(CsgDyadic.multiply(l1, r2, p01));
        expectTrue(CsgDyadic.multiply(l2, r1, p10));
        expectTrue(CsgDyadic.multiply(l2, r2, p11));
        expectTrue(CsgDyadic.add(p00, p01, t0));
        expectTrue(CsgDyadic.add(p10, p11, t1));
        expectTrue(CsgDyadic.add(t0, t1, combinedRight));
        expectTrue(combinedLeft.equals(combinedRight));
    }
});

test("CsgDyadicContract_CapacityAndRoundingFailuresAreAtomic", () => {
    const high = new CsgDyadic();
    const one = new CsgDyadic();
    const sentinel = new CsgDyadic();
    let result = new CsgDyadic();
    expectTrue(CsgDyadic.fromDouble(2, high));
    expectTrue(CsgDyadic.fromDouble(1, one));
    expectTrue(CsgDyadic.fromDouble(17, sentinel));
    for (let i = 0; i < 16; ++i) expectTrue(CsgDyadic.multiply(high, high, high));
    result = sentinel;
    expectFalse(CsgDyadic.add(high, one, result));
    expectTrue(result.equals(sentinel));
    const dense = new CsgDyadic();
    const square = new CsgDyadic();
    expectTrue(CsgDyadic.fromDouble(2, high));
    for (let i = 0; i < 14; ++i) expectTrue(CsgDyadic.multiply(high, high, high));
    expectTrue(CsgDyadic.add(high, one, dense));
    expectTrue(CsgDyadic.multiply(dense, dense, square));
    expectFalse(CsgDyadic.multiply(square, square, result));
    expectTrue(result.equals(sentinel));
});

test("CsgSolidEquivalenceContract_DyadicGramHandlesRoundedRadiusProducts", () => {
    const a = new SimpleBody();
    const b = new SimpleBody();
    const nearby = new SimpleBody();
    a.setGeometry(new Sphere(0.1));
    b.setGeometry(new Sphere(0.1));
    nearby.setGeometry(new Sphere(JavaMath.nextAfter(0.1, 1)));
    const forwardA = new Matrix4x4d().scale(5, 5, 5);
    const inverseA = new Matrix4x4d().scale(0.2, 0.2, 0.2);
    const forwardB = Matrix4x4d.identityMatrix()
        .withVal(0, 0, 3)
        .withVal(0, 1, -4)
        .withVal(1, 0, 4)
        .withVal(1, 1, 3)
        .withVal(2, 2, 5);
    const inverseB = Matrix4x4d.identityMatrix()
        .withVal(0, 0, 0.12)
        .withVal(0, 1, 0.16)
        .withVal(1, 0, -0.16)
        .withVal(1, 1, 0.12)
        .withVal(2, 2, 0.2);
    expectEq(AffinePlacementStatus.Valid, a.setAffinePlacement(forwardA, inverseA));
    expectEq(AffinePlacementStatus.Valid, b.setAffinePlacement(forwardB, inverseB));
    expectEq(AffinePlacementStatus.Valid, nearby.setAffinePlacement(forwardB, inverseB));
    const aa = new CsgBodyOperand(a);
    const bb = new CsgBodyOperand(b);
    const cc = new CsgBodyOperand(nearby);
    expectTrue(aa.isSameSolid(bb));
    expectTrue(bb.isSameSolid(aa));
    expectFalse(aa.isSameSolid(cc));
});
