// Port of cpp/base/src/test/csg_evaluator_tests.cpp
import { test } from "vitest";
import {
    expectDoubleEq,
    expectEq,
    expectFalse,
    expectGe,
    expectGt,
    expectLe,
    expectNe,
    expectNear,
    expectTrue,
} from "../../../../../GTestAssertions.js";
import { AffinePlacementStatus } from "vsdk/toolkit/environment/scene/AffinePlacementStatus.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { CsgBody } from "vsdk/toolkit/environment/scene/csg/CsgBody.js";
import { CsgBodyEvaluator } from "vsdk/toolkit/environment/scene/csg/CsgBodyEvaluator.js";
import { CsgBodyOperand } from "vsdk/toolkit/environment/scene/csg/CsgBodyOperand.js";
import { CsgCrossingMembershipFilter } from "vsdk/toolkit/environment/scene/csg/CsgCrossingMembershipFilter.js";
import { CsgMembershipTest } from "vsdk/toolkit/environment/scene/csg/CsgMembershipTest.js";
import { CsgMembershipRule } from "vsdk/toolkit/environment/scene/csg/CsgMembershipRule.js";
import { CsgOperation } from "vsdk/toolkit/environment/scene/csg/CsgOperation.js";
import { CsgPlacedExpression } from "vsdk/toolkit/environment/scene/csg/CsgPlacedExpression.js";
import { CsgPointClassification } from "vsdk/toolkit/environment/scene/csg/CsgPointClassification.js";
import { CsgPointProof } from "vsdk/toolkit/environment/scene/csg/CsgPointProof.js";
import { CsgRayClassification } from "vsdk/toolkit/environment/scene/csg/CsgRayClassification.js";
import { CsgRaySurfaceEvents } from "vsdk/toolkit/environment/scene/csg/CsgRaySurfaceEvents.js";
import { CsgRayWorkspace } from "vsdk/toolkit/environment/scene/csg/CsgRayWorkspace.js";
import { CsgRothRayClassifier } from "vsdk/toolkit/environment/scene/csg/CsgRothRayClassifier.js";
import { CsgSegmentClassification } from "vsdk/toolkit/environment/scene/csg/CsgSegmentClassification.js";
import { CsgTestOperand } from "../../../../../CsgTestOperand.js";
import { CsgTree } from "vsdk/toolkit/environment/scene/csg/CsgTree.js";
import { CsgVersionStamp } from "vsdk/toolkit/environment/scene/csg/CsgVersionStamp.js";
import { GeometryBounds } from "vsdk/toolkit/environment/geometry/element/GeometryBounds.js";
import { GeometryContainment } from "vsdk/toolkit/environment/geometry/element/GeometryContainment.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { PlanarHalfSpace } from "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayCrossingQuery } from "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.js";
import { RayHit } from "vsdk/toolkit/environment/geometry/element/RayHit.js";
import { RaySolidClassification } from "vsdk/toolkit/environment/geometry/element/RaySolidClassification.js";
import { RaySolidQueryStatus } from "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.js";
import { SimpleBody } from "vsdk/toolkit/environment/scene/SimpleBody.js";
import { SimpleMaterial } from "vsdk/toolkit/environment/material/SimpleMaterial.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { UncertifiedTestBody } from "../../../../../UncertifiedTestBody.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";

const alongX = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0));

test("CsgRothRayClassifierContract_CombinesTableThreeOperatorsWithOwners", () => {
    const left = new SimpleBody();
    const right = new SimpleBody();
    left.setGeometry(new Sphere(1));
    right.setGeometry(new Sphere(1));
    right.setPosition(new Vector3Dd(1.5, 0, 0));
    const a = new CsgBodyOperand(left);
    const b = new CsgBodyOperand(right);
    const unionTree = new CsgTree(CsgOperation.UNION);
    const intersection = new CsgTree(CsgOperation.INTERSECTION);
    const difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(unionTree.addOperand(a));
    expectTrue(unionTree.addOperand(b));
    expectTrue(intersection.addOperand(a));
    expectTrue(intersection.addOperand(b));
    expectTrue(difference.addOperand(a));
    expectTrue(difference.addOperand(b));
    const result = new CsgRayClassification();

    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRothRayClassifier.classify(unionTree, alongX, new RayCrossingQuery(0, 20), result),
    );
    const u = result.getSolid();
    expectEq(2, u.boundaries.size());
    expectEq(3, u.insideSegments.size());
    expectDoubleEq(2, u.boundaries.get(0)!.t);
    expectDoubleEq(5.5, u.boundaries.get(1)!.t);
    expectFalse(u.insideSegments.get(0));
    expectTrue(u.insideSegments.get(1));
    expectFalse(u.insideSegments.get(2));
    expectEq(2, result.getOwnerGroupCount());
    expectEq(1, result.getOwnerCount(0));
    expectEq(0, result.getOwner(0, 0)!.leafIndex);
    expectEq(1, result.getOwnerCount(1));
    expectEq(1, result.getOwner(1, 0)!.leafIndex);
    expectEq(a, result.getLeaves().get(0)!.getPath().getOperand(0));

    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRothRayClassifier.classify(intersection, alongX, new RayCrossingQuery(0, 20), result),
    );
    expectEq(2, result.getSolid().boundaries.size());
    expectDoubleEq(3.5, result.getSolid().boundaries.get(0)!.t);
    expectDoubleEq(4, result.getSolid().boundaries.get(1)!.t);
    expectEq(1, result.getOwner(0, 0)!.leafIndex);
    expectEq(0, result.getOwner(1, 0)!.leafIndex);

    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRothRayClassifier.classify(difference, alongX, new RayCrossingQuery(0, 20), result),
    );
    const d = result.getSolid();
    expectEq(2, d.boundaries.size());
    expectDoubleEq(2, d.boundaries.get(0)!.t);
    expectDoubleEq(3.5, d.boundaries.get(1)!.t);
    expectFalse(d.boundaries.get(0)!.normalReversed);
    expectTrue(d.boundaries.get(1)!.normalReversed);
    expectEq(1, result.getOwner(1, 0)!.leafIndex);
    expectTrue(result.getLeaves().get(1)!.getPath().isReversed());
});

test("CsgRothRayClassifierContract_CoincidentEventsGiveRegularizedResult", () => {
    const first = new SimpleBody();
    const second = new SimpleBody();
    const same = new SimpleBody();
    first.setGeometry(new Box(2, 2, 2));
    second.setGeometry(new Box(2, 2, 2));
    same.setGeometry(new Box(2, 2, 2));
    second.setPosition(new Vector3Dd(2, 0, 0));
    const a = new CsgBodyOperand(first);
    const b = new CsgBodyOperand(second);
    const c = new CsgBodyOperand(same);
    const result = new CsgRayClassification();

    const touching = new CsgTree(CsgOperation.UNION);
    expectTrue(touching.addOperand(a));
    expectTrue(touching.addOperand(b));
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRothRayClassifier.classify(touching, alongX, new RayCrossingQuery(0, 20), result),
    );
    expectEq(4.0, result.getLeaves().get(0)!.getClassification().boundaries.get(1)!.t);
    expectEq(4.0, result.getLeaves().get(1)!.getClassification().boundaries.get(0)!.t);
    expectEq(2, result.getSolid().boundaries.size());
    expectDoubleEq(2, result.getSolid().boundaries.get(0)!.t);
    expectDoubleEq(6, result.getSolid().boundaries.get(1)!.t);

    const faceContact = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(faceContact.addOperand(a));
    expectTrue(faceContact.addOperand(b));
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRothRayClassifier.classify(faceContact, alongX, new RayCrossingQuery(0, 20), result),
    );
    expectEq(0, result.getSolid().boundaries.size());
    expectEq(1, result.getSolid().insideSegments.size());
    expectFalse(result.getSolid().insideSegments.get(0));

    const identical = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(identical.addOperand(a));
    expectTrue(identical.addOperand(c));
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRothRayClassifier.classify(identical, alongX, new RayCrossingQuery(0, 20), result),
    );
    expectEq(2, result.getSolid().boundaries.size());
    // No single face changes the intersection; both are joint owners.
    expectEq(2, result.getOwnerCount(0));
    expectEq(2, result.getOwnerCount(1));

    const emptyDifference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(emptyDifference.addOperand(a));
    expectTrue(emptyDifference.addOperand(c));
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRothRayClassifier.classify(emptyDifference, alongX, new RayCrossingQuery(0, 20), result),
    );
    expectEq(0, result.getSolid().boundaries.size());
    expectFalse(result.getSolid().insideSegments.get(0));
});

test("CsgRothRayClassifierContract_InteriorStartUnboundedOperandAndFailures", () => {
    const ball = new SimpleBody();
    const half = new SimpleBody();
    ball.setGeometry(new Sphere(1));
    half.setGeometry(new PlanarHalfSpace(1, 0, 0, 0));
    const a = new CsgBodyOperand(ball);
    const h = new CsgBodyOperand(half);
    const result = new CsgRayClassification();
    const inside = new CsgTree(CsgOperation.UNION);
    expectTrue(inside.addOperand(a));
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRothRayClassifier.classify(
            inside,
            Ray.fromAffine(new Vector3Dd(0, 0, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, 5),
            result,
        ),
    );
    expectEq(1, result.getSolid().boundaries.size());
    expectTrue(result.getSolid().insideSegments.get(0));
    expectDoubleEq(1, result.getSolid().boundaries.get(0)!.t);

    const cut = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(cut.addOperand(h));
    expectTrue(cut.addOperand(a));
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRothRayClassifier.classify(cut, alongX, new RayCrossingQuery(0, 20), result),
    );
    const halfStartsInside = result.getLeaves().get(0)!.getClassification().insideSegments.get(0);
    expectEq(halfStartsInside, result.getSolid().insideSegments.get(0));
    for (let i = 0; i + 1 < result.getSolid().insideSegments.size(); ++i)
        expectNe(result.getSolid().insideSegments.get(i), result.getSolid().insideSegments.get(i + 1));

    const uncertain = new CsgTestOperand(GeometryContainment.Indeterminate);
    const partial = new CsgTree(CsgOperation.UNION);
    expectTrue(partial.addOperand(a));
    expectTrue(partial.addOperand(uncertain));
    expectEq(
        RaySolidQueryStatus.Unsupported,
        CsgRothRayClassifier.classify(partial, alongX, new RayCrossingQuery(0, 20), result),
    );
    expectEq(0, result.getSolid().insideSegments.size());
    expectEq(0, result.getLeaves().size());
    expectEq(
        RaySolidQueryStatus.InvalidInput,
        CsgRothRayClassifier.classify(inside, alongX, new RayCrossingQuery(2, 2), result),
    );
});

test("CsgRothRayClassifierContract_PlacedNestedDifferenceKeepsLeafProvenance", () => {
    const outer = new SimpleBody();
    const hole = new SimpleBody();
    outer.setGeometry(new Sphere(2));
    hole.setGeometry(new Sphere(1));
    const a = new CsgBodyOperand(outer);
    const b = new CsgBodyOperand(hole);
    const shell = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(shell.addOperand(a));
    expectTrue(shell.addOperand(b));
    const placed = new CsgPlacedExpression(shell);
    expectEq(
        AffinePlacementStatus.Valid,
        placed.setAffinePlacement(
            new Matrix4x4d().translation(3, 0, 0).multiply(new Matrix4x4d().scale(2, 1, 1)),
            new Matrix4x4d().scale(0.5, 1, 1).multiply(new Matrix4x4d().translation(-3, 0, 0)),
        ),
    );
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    const result = new CsgRayClassification();
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRothRayClassifier.classify(
            root,
            Ray.fromAffine(new Vector3Dd(-5, 0, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, 20),
            result,
        ),
    );
    const s = result.getSolid();
    expectEq(4, s.boundaries.size());
    const expected = [4, 6, 10, 12];
    for (let i = 0; i < 4; ++i) expectDoubleEq(expected[i], s.boundaries.get(i)!.t);
    expectFalse(s.boundaries.get(0)!.normalReversed);
    expectTrue(s.boundaries.get(1)!.normalReversed);
    expectTrue(s.boundaries.get(2)!.normalReversed);
    expectFalse(s.boundaries.get(3)!.normalReversed);
    const holeLeaf = result.getLeaves().get(result.getOwner(1, 0)!.leafIndex)!;
    expectEq(2, holeLeaf.getPath().getLength());
    expectEq(placed, holeLeaf.getPath().getOperand(0));
    expectEq(b, holeLeaf.getPath().getOperand(1));
    expectEq(1, holeLeaf.getPlacementCount());
});

// Independent check: each segment's membership must match the certified point
// classification of the expression at the segment midpoint.
test("CsgRothRayClassifierContract_SegmentsMatchPointClassificationOnRandomTrees", () => {
    let seed = 12345;
    const bodyCount = 6;
    const bodies: SimpleBody[] = Array.from({ length: bodyCount }, () => new SimpleBody());
    for (let i = 0; i < bodyCount; ++i) {
        if (i % 2 === 0) bodies[i].setGeometry(new Sphere(0.6 + 0.1 * i));
        else bodies[i].setGeometry(new Box(1.0 + 0.2 * i, 1.4, 0.9 + 0.1 * i));
    }
    let checked = 0;
    for (let trial = 0; trial < 200; ++trial) {
        for (let i = 0; i < bodyCount; ++i) {
            seed = (Math.imul(seed, 1103515245) + 12345) >>> 0;
            const x = ((seed >>> 8) % 2000) / 1000.0 - 1.0;
            seed = (Math.imul(seed, 1103515245) + 12345) >>> 0;
            const y = ((seed >>> 8) % 2000) / 2000.0 - 0.5;
            bodies[i].setPosition(new Vector3Dd(x, y, 0.1 * i - 0.25));
        }
        const o0 = new CsgBodyOperand(bodies[0]);
        const o1 = new CsgBodyOperand(bodies[1]);
        const o2 = new CsgBodyOperand(bodies[2]);
        const o3 = new CsgBodyOperand(bodies[3]);
        const o4 = new CsgBodyOperand(bodies[4]);
        const o5 = new CsgBodyOperand(bodies[5]);
        const ops: CsgOperation[] = [CsgOperation.UNION, CsgOperation.INTERSECTION, CsgOperation.DIFFERENCE];
        const inner = new CsgTree(ops[trial % 3]);
        const middle = new CsgTree(ops[Math.trunc(trial / 3) % 3]);
        const root = new CsgTree(ops[Math.trunc(trial / 9) % 3]);
        expectTrue(inner.addOperand(o0));
        expectTrue(inner.addOperand(o1));
        expectTrue(inner.addOperand(o2));
        expectTrue(middle.addOperand(o3));
        expectTrue(middle.addOperand(inner));
        expectTrue(root.addOperand(middle));
        expectTrue(root.addOperand(o4));
        expectTrue(root.addOperand(o5));
        seed = (Math.imul(seed, 1103515245) + 12345) >>> 0;
        const oy = ((seed >>> 8) % 1000) / 1000.0 - 0.5;
        const ray = Ray.fromAffine(new Vector3Dd(-4, oy, 0.05), new Vector3Dd(1, 0.03, -0.02));
        const minT = 0;
        const maxT = 8;
        const result = new CsgRayClassification();
        expectEq(
            RaySolidQueryStatus.Supported,
            CsgRothRayClassifier.classify(root, ray, new RayCrossingQuery(minT, maxT), result),
        );
        const s = result.getSolid();
        expectEq(s.boundaries.size() + 1, s.insideSegments.size());
        for (let i = 0; i < s.insideSegments.size(); ++i) {
            const a = i === 0 ? minT : s.boundaries.get(i - 1)!.t;
            const b = i === s.boundaries.size() ? maxT : s.boundaries.get(i)!.t;
            if (b - a < 1e-6) continue;
            const t = 0.5 * (a + b);
            const p = ray.getOrigin().add(ray.getDirection().multiply(t));
            const c = CsgPointClassification.classify(root, p, 0);
            if (c !== GeometryContainment.Inside && c !== GeometryContainment.Outside) continue;
            expectEq(c === GeometryContainment.Inside, s.insideSegments.get(i));
            ++checked;
        }
        for (let i = 0; i < s.boundaries.size(); ++i) {
            expectGt(result.getOwnerCount(i), 0);
        }
    }
    expectGt(checked, 400);
});

test("CsgCrossingMembershipFilterContract_PovHeuristicKeepsUnionInternalSurfaces", () => {
    const left = new SimpleBody();
    const right = new SimpleBody();
    left.setGeometry(new Sphere(1));
    right.setGeometry(new Sphere(1));
    right.setPosition(new Vector3Dd(1.5, 0, 0));
    const a = new CsgBodyOperand(left);
    const b = new CsgBodyOperand(right);
    const unionTree = new CsgTree(CsgOperation.UNION);
    const intersection = new CsgTree(CsgOperation.INTERSECTION);
    const difference = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(unionTree.addOperand(a));
    expectTrue(unionTree.addOperand(b));
    expectTrue(intersection.addOperand(a));
    expectTrue(intersection.addOperand(b));
    expectTrue(difference.addOperand(a));
    expectTrue(difference.addOperand(b));
    const events = new CsgRaySurfaceEvents();

    // Union keeps every operand event; Roth reports only 2 and 5.5.
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgCrossingMembershipFilter.select(unionTree, alongX, new RayCrossingQuery(0, 20), 1e-9, events),
    );
    expectEq(4, events.size());
    const unionT = [2, 3.5, 4, 5.5];
    for (let i = 0; i < 4; ++i) expectDoubleEq(unionT[i], events.getT(i));

    expectEq(
        RaySolidQueryStatus.Supported,
        CsgCrossingMembershipFilter.select(intersection, alongX, new RayCrossingQuery(0, 20), 1e-9, events),
    );
    expectEq(2, events.size());
    expectDoubleEq(3.5, events.getT(0));
    expectDoubleEq(4, events.getT(1));
    expectEq(1, events.get(0)!.leafIndex);
    expectEq(0, events.get(1)!.leafIndex);

    expectEq(
        RaySolidQueryStatus.Supported,
        CsgCrossingMembershipFilter.select(difference, alongX, new RayCrossingQuery(0, 20), 1e-9, events),
    );
    expectEq(2, events.size());
    expectDoubleEq(2, events.getT(0));
    expectDoubleEq(3.5, events.getT(1));
    const hole = events.get(1)!;
    expectTrue(
        events.getLeaves().get(hole.leafIndex)!.getClassification().boundaries.get(hole.boundaryIndex)!.normalReversed,
    );
});

test("CsgCrossingMembershipFilterContract_BoundaryMembershipKeepsCoincidentFacesAndUncertaintyFails", () => {
    // An uncertified sibling only matters to the certified membership test.
    const previous = CsgCrossingMembershipFilter.getMembershipTest();
    CsgCrossingMembershipFilter.setMembershipTest(CsgMembershipTest.CERTIFIED_BALL);
    try {
        const first = new SimpleBody();
        const same = new SimpleBody();
        first.setGeometry(new Box(2, 2, 2));
        same.setGeometry(new Box(2, 2, 2));
        const a = new CsgBodyOperand(first);
        const c = new CsgBodyOperand(same);
        const identical = new CsgTree(CsgOperation.INTERSECTION);
        expectTrue(identical.addOperand(a));
        expectTrue(identical.addOperand(c));
        const events = new CsgRaySurfaceEvents();
        // Both coincident faces satisfy the boundary-inclusive membership; Roth
        // reports a single boundary for each pair.
        expectEq(
            RaySolidQueryStatus.Supported,
            CsgCrossingMembershipFilter.select(identical, alongX, new RayCrossingQuery(0, 20), 1e-9, events),
        );
        expectEq(4, events.size());
        expectDoubleEq(2, events.getT(0));
        expectDoubleEq(2, events.getT(1));
        expectEq(0, events.get(0)!.leafIndex);
        expectEq(1, events.get(1)!.leafIndex);

        const opaque = new UncertifiedTestBody();
        opaque.setGeometry(new Sphere(1));
        opaque.setPosition(new Vector3Dd(0.5, 0, 0));
        const u = new CsgBodyOperand(opaque);
        const uncertain = new CsgTree(CsgOperation.INTERSECTION);
        expectTrue(uncertain.addOperand(a));
        expectTrue(uncertain.addOperand(u));
        expectEq(
            RaySolidQueryStatus.Unsupported,
            CsgCrossingMembershipFilter.select(uncertain, alongX, new RayCrossingQuery(0, 20), 1e-9, events),
        );
        expectEq(0, events.size());

        // A proven exclusion by another sibling makes the uncertain one irrelevant.
        const far = new SimpleBody();
        far.setGeometry(new Sphere(0.5));
        far.setPosition(new Vector3Dd(0, 5, 0));
        const f = new CsgBodyOperand(far);
        const excluded = new CsgTree(CsgOperation.INTERSECTION);
        expectTrue(excluded.addOperand(a));
        expectTrue(excluded.addOperand(u));
        expectTrue(excluded.addOperand(f));
        const probe = new CsgTree(CsgOperation.UNION);
        expectTrue(probe.addOperand(a));
        expectEq(
            RaySolidQueryStatus.Supported,
            CsgCrossingMembershipFilter.select(excluded, alongX, new RayCrossingQuery(0, 20), 1e-9, events),
        );
        expectEq(0, events.size());
        expectEq(
            RaySolidQueryStatus.InvalidInput,
            CsgCrossingMembershipFilter.select(probe, alongX, new RayCrossingQuery(0, 20), -1, events),
        );
    } finally {
        CsgCrossingMembershipFilter.setMembershipTest(previous);
    }
});

test("CsgVersionStampContract_ResultsDetectEditsOfBorrowedOperands", () => {
    const left = new SimpleBody();
    const right = new SimpleBody();
    left.setGeometry(new Sphere(1));
    right.setGeometry(new Sphere(1));
    right.setPosition(new Vector3Dd(1.5, 0, 0));
    const a = new CsgBodyOperand(left);
    const b = new CsgBodyOperand(right);
    const inner = new CsgTree(CsgOperation.UNION);
    expectTrue(inner.addOperand(a));
    expectTrue(inner.addOperand(b));
    const placed = new CsgPlacedExpression(inner);
    const root = new CsgTree(CsgOperation.UNION);
    expectTrue(root.addOperand(placed));
    const result = new CsgRayClassification();
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRothRayClassifier.classify(root, alongX, new RayCrossingQuery(0, 20), result),
    );
    expectTrue(result.getVersions().isComplete());
    expectTrue(result.getVersions().isCurrent());
    right.setPosition(new Vector3Dd(1.4, 0, 0));
    expectFalse(result.getVersions().isCurrent());
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRothRayClassifier.classify(root, alongX, new RayCrossingQuery(0, 20), result),
    );
    expectTrue(result.getVersions().isCurrent());
    expectEq(
        AffinePlacementStatus.Valid,
        placed.setAffinePlacement(new Matrix4x4d().translation(1, 0, 0), new Matrix4x4d().translation(-1, 0, 0)),
    );
    expectFalse(result.getVersions().isCurrent());
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgRothRayClassifier.classify(root, alongX, new RayCrossingQuery(0, 20), result),
    );
    const extra = new SimpleBody();
    extra.setGeometry(new Sphere(0.5));
    const c = new CsgBodyOperand(extra);
    expectTrue(inner.addOperand(c));
    expectFalse(result.getVersions().isCurrent());

    const proof = new CsgPointProof();
    expectEq(
        GeometryContainment.Inside,
        CsgPointClassification.classifyWithProof(root, new Vector3Dd(1, 0, 0), 0.01, proof),
    );
    expectTrue(proof.isAvailable());
    expectTrue(proof.getVersions().isCurrent());
    left.setPosition(new Vector3Dd(0, 0.1, 0));
    expectFalse(proof.getVersions().isCurrent());

    const events = new CsgRaySurfaceEvents();
    expectEq(
        RaySolidQueryStatus.Supported,
        CsgCrossingMembershipFilter.select(root, alongX, new RayCrossingQuery(0, 20), 1e-9, events),
    );
    expectTrue(events.getVersions().isCurrent());
    extra.setPosition(new Vector3Dd(0, 0.2, 0));
    expectFalse(events.getVersions().isCurrent());

    // A view that does not track its edits can never be proven current.
    const untracked = new CsgTestOperand(GeometryContainment.Inside);
    const opaque = new CsgTree(CsgOperation.UNION);
    expectTrue(opaque.addOperand(a));
    expectTrue(opaque.addOperand(untracked));
    const stamp = new CsgVersionStamp();
    expectTrue(stamp.capture(opaque));
    expectTrue(stamp.isCaptured());
    expectFalse(stamp.isComplete());
    expectFalse(stamp.isCurrent());
});

test("CsgRayWorkspaceContract_ReusedStorageGivesFreshResults", () => {
    const outer = new SimpleBody();
    const hole = new SimpleBody();
    const side = new SimpleBody();
    outer.setGeometry(new Sphere(2));
    hole.setGeometry(new Sphere(1));
    side.setGeometry(new Box(1, 1, 1));
    side.setPosition(new Vector3Dd(1.5, 0, 0));
    const a = new CsgBodyOperand(outer);
    const b = new CsgBodyOperand(hole);
    const c = new CsgBodyOperand(side);
    const shell = new CsgTree(CsgOperation.DIFFERENCE);
    const small = new CsgTree(CsgOperation.UNION);
    expectTrue(shell.addOperand(a));
    expectTrue(shell.addOperand(b));
    expectTrue(shell.addOperand(c));
    expectTrue(small.addOperand(b));
    const workspace = new CsgRayWorkspace();
    const reused = new CsgRayClassification();
    const order: CsgTree[] = [shell, small, shell, small, shell];
    for (let i = 0; i < 5; ++i) {
        const fresh = new CsgRayClassification();
        const ray = Ray.fromAffine(new Vector3Dd(-5, 0.1 * i, 0), new Vector3Dd(1, 0, 0));
        expectEq(
            RaySolidQueryStatus.Supported,
            CsgRothRayClassifier.classify(order[i], ray, new RayCrossingQuery(0, 20), fresh),
        );
        expectEq(
            RaySolidQueryStatus.Supported,
            CsgRothRayClassifier.classify(order[i], ray, new RayCrossingQuery(0, 20), workspace, reused),
        );
        expectEq(fresh.getSolid().boundaries.size(), reused.getSolid().boundaries.size());
        expectEq(fresh.getSolid().insideSegments.size(), reused.getSolid().insideSegments.size());
        expectEq(fresh.getLeaves().size(), reused.getLeaves().size());
        for (let k = 0; k < fresh.getSolid().boundaries.size(); ++k) {
            expectEq(fresh.getSolid().boundaries.get(k)!.t, reused.getSolid().boundaries.get(k)!.t);
            expectEq(
                fresh.getSolid().boundaries.get(k)!.normalReversed,
                reused.getSolid().boundaries.get(k)!.normalReversed,
            );
            expectEq(
                fresh.getSolid().boundaries.get(k)!.getInstanceCount(),
                reused.getSolid().boundaries.get(k)!.getInstanceCount(),
            );
            expectEq(fresh.getOwnerCount(k), reused.getOwnerCount(k));
            expectEq(fresh.getOwner(k, 0)!.leafIndex, reused.getOwner(k, 0)!.leafIndex);
        }
        for (let k = 0; k < fresh.getLeaves().size(); ++k)
            expectEq(fresh.getLeaves().get(k)!.getPath().getLength(), reused.getLeaves().get(k)!.getPath().getLength());
    }
    const uncertain = new CsgTestOperand(GeometryContainment.Indeterminate);
    const failing = new CsgTree(CsgOperation.UNION);
    expectTrue(failing.addOperand(a));
    expectTrue(failing.addOperand(uncertain));
    expectEq(
        RaySolidQueryStatus.Unsupported,
        CsgRothRayClassifier.classify(failing, alongX, new RayCrossingQuery(0, 20), workspace, reused),
    );
    expectEq(0, reused.getLeaves().size());
    expectEq(0, reused.getSolid().insideSegments.size());
    expectFalse(reused.getVersions().isCaptured());
});
test("CsgBodyContract_DifferenceHitsCarryLeafNormalsMaterialsAndPlacement", () => {
    const body = new CsgBody(CsgOperation.DIFFERENCE, CsgBodyEvaluator.ROTH);
    const outer = new SimpleBody();
    outer.setGeometry(new Sphere(2));
    outer.setMaterial(new SimpleMaterial());
    const hole = new SimpleBody();
    hole.setGeometry(new Sphere(1));
    hole.setMaterial(new SimpleMaterial());
    hole.setPosition(new Vector3Dd(1.5, 0, 0));
    expectTrue(body.addLeaf(0, outer));
    expectTrue(body.addLeaf(0, hole));
    expectFalse(body.addLeaf(3, new SimpleBody()));
    expectEq(-1, body.addNode(5, CsgOperation.UNION));
    expectEq(2, body.getLeafCount());
    const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
    expectTrue(body.doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(-5, 0, 0), new Vector3Dd(1, 0, 0)), hit));
    expectNear(3, hit.getHitDistance(), 1e-12);
    expectNear(-1, hit.normal.x(), 1e-12);
    expectNear(-2, hit.point.x(), 1e-12);
    expectEq(outer.getMaterial(), hit.material);
    hit.reset(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
    expectTrue(body.doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(5, 0, 0), new Vector3Dd(-1, 0, 0)), hit));
    expectNear(4.5, hit.getHitDistance(), 1e-12);
    expectNear(1, hit.normal.x(), 1e-12);
    expectEq(hole.getMaterial(), hit.material);
    body.setPosition(new Vector3Dd(0, 0, 10));
    expectFalse(body.doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(-5, 0, 0), new Vector3Dd(1, 0, 0)), hit));
    hit.reset(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
    expectTrue(body.doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(-5, 0, 10), new Vector3Dd(1, 0, 0)), hit));
    expectNear(3, hit.getHitDistance(), 1e-12);
    expectNear(10, hit.point.z(), 1e-12);
    const bounds = new GeometryBounds();
    body.getWorldBounds(bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    expectLe(bounds.getMinimum().z(), 8);
    expectGe(bounds.getMaximum().z(), 12);
    expectEq(GeometryContainment.Inside, body.classifyPointCertified(new Vector3Dd(-1, 0, 10), 1e-9));
    expectEq(GeometryContainment.Outside, body.classifyPointCertified(new Vector3Dd(1.5, 0, 10), 1e-9));
    const solid = new RaySolidClassification();
    expectEq(
        RaySolidQueryStatus.Supported,
        body.classifyRay(
            Ray.fromAffine(new Vector3Dd(-5, 0, 10), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, 20),
            solid,
        ),
    );
    expectEq(2, solid.boundaries.size());
    expectNear(3, solid.boundaries.get(0)!.t, 1e-12);
    expectNear(5.5, solid.boundaries.get(1)!.t, 1e-12);
});

test("CsgBodyContract_EvaluatorsDifferOnNestedUnionInternalSurfaces", () => {
    const bodies: CsgBody[] = [
        new CsgBody(CsgOperation.DIFFERENCE, CsgBodyEvaluator.ROTH),
        new CsgBody(CsgOperation.DIFFERENCE, CsgBodyEvaluator.CROSSING_MEMBERSHIP),
    ];
    for (let i = 0; i < 2; ++i) {
        const outer = new SimpleBody();
        outer.setGeometry(new Sphere(5));
        const left = new SimpleBody();
        left.setGeometry(new Sphere(2));
        left.setPosition(new Vector3Dd(-1, 0, 0));
        const right = new SimpleBody();
        right.setGeometry(new Sphere(2));
        right.setPosition(new Vector3Dd(1, 0, 0));
        expectTrue(bodies[i].addLeaf(0, outer));
        const holes = bodies[i].addNode(0, CsgOperation.UNION);
        expectEq(1, holes);
        expectTrue(bodies[i].addLeaf(holes, left));
        expectTrue(bodies[i].addLeaf(holes, right));
        const outside = new RayHit(RayHit.DETAIL_NONE, false);
        expectTrue(
            bodies[i].doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(-10, 0, 0), new Vector3Dd(1, 0, 0)), outside),
        );
        expectNear(5, outside.getHitDistance(), 1e-12);
    }
    const roth = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
    const pov = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
    const fromHole = Ray.fromAffine(new Vector3Dd(0, 0, 0), new Vector3Dd(1, 0, 0));
    expectTrue(bodies[0].doIntersectionFirstHit(fromHole, roth));
    expectTrue(bodies[1].doIntersectionFirstHit(fromHole, pov));
    expectNear(3, roth.getHitDistance(), 1e-12);
    expectNear(1, pov.getHitDistance(), 1e-12);
    expectNear(-1, roth.normal.x(), 1e-12);
    expectNear(-1, pov.normal.x(), 1e-12);
});

test("CsgMembershipRuleContract_FoldStopsOnlyWhenLaterOperandsCannotChangeIt", () => {
    const operations: CsgOperation[] = [CsgOperation.UNION, CsgOperation.INTERSECTION, CsgOperation.DIFFERENCE];
    for (let k = 0; k < 3; k++) {
        for (let accumulated = 0; accumulated < 2; accumulated++) {
            const value = accumulated === 1;
            const unchanged =
                CsgMembershipRule.combine(operations[k], value, false) === value &&
                CsgMembershipRule.combine(operations[k], value, true) === value;
            expectEq(CsgMembershipRule.isDecided(operations[k], value), unchanged);
        }
    }
});

test("CsgSegmentClassificationContract_MergesLeafSegmentsByTableThree", () => {
    // Left solid [1,3], right solid [2,4]; payloads name the events.
    const left = new CsgSegmentClassification<number>(false, 2);
    const right = new CsgSegmentClassification<number>(false, 2);
    left.append(1, 10);
    left.append(3, 11);
    right.append(2, 20);
    right.append(4, 21);
    expectTrue(left.getEvents().get(0)!.isEntering());
    expectFalse(left.getEvents().get(1)!.isEntering());
    const u = CsgSegmentClassification.merge<number>(left, right, CsgOperation.UNION);
    expectEq(2, u.getEvents().size());
    expectEq(10, u.getEvents().get(0)!.getPayload());
    expectEq(21, u.getEvents().get(1)!.getPayload());
    const i = CsgSegmentClassification.merge<number>(left, right, CsgOperation.INTERSECTION);
    expectEq(2, i.getEvents().size());
    expectEq(20, i.getEvents().get(0)!.getPayload());
    expectEq(11, i.getEvents().get(1)!.getPayload());
    const d = CsgSegmentClassification.merge<number>(left, right, CsgOperation.DIFFERENCE);
    expectEq(2, d.getEvents().size());
    expectEq(10, d.getEvents().get(0)!.getPayload());
    expectTrue(d.getEvents().get(0)!.isEntering());
    expectEq(20, d.getEvents().get(1)!.getPayload());
    expectFalse(d.getEvents().get(1)!.isEntering());
    expectFalse(d.isInitialInside());
});

test("CsgSegmentClassificationContract_InitialStateAndTiesTakeTheLeftEvent", () => {
    // A ray starting inside the left solid, leaving it where the right one ends.
    const left = new CsgSegmentClassification<number>(true, 1);
    const right = new CsgSegmentClassification<number>(true, 1);
    left.append(2, 1);
    right.append(2, 2);
    const u = CsgSegmentClassification.merge<number>(left, right, CsgOperation.UNION);
    expectTrue(u.isInitialInside());
    expectEq(1, u.getEvents().size());
    expectEq(2, u.getEvents().get(0)!.getPayload());
    const i = CsgSegmentClassification.merge<number>(left, right, CsgOperation.INTERSECTION);
    expectEq(1, i.getEvents().size());
    expectEq(1, i.getEvents().get(0)!.getPayload());
});

test("CsgTreeContract_OperationEditsAreStructureEdits", () => {
    const tree = new CsgTree(CsgOperation.UNION);
    const before: number[] = [0];
    const after: number[] = [0];
    expectTrue(tree.queryStructureVersion(before));
    tree.setOperation(CsgOperation.INTERSECTION);
    expectTrue(tree.queryStructureVersion(after));
    expectEq(CsgOperation.INTERSECTION, tree.getOperation());
    expectNe(before[0], after[0]);
});

test("CsgBodyOperandContract_StructuralTraversalFollowsThePlacedGeometryOptIn", () => {
    const plain = new SimpleBody();
    plain.setGeometry(new Sphere(1));
    const composed = new CsgBody(CsgOperation.UNION, CsgBodyEvaluator.ROTH);
    const plainView = new CsgBodyOperand(plain);
    const composedView = new CsgBodyOperand(composed);
    expectTrue(plain.isStructuralTraversalSafe());
    expectTrue(plainView.isStructuralTraversalSafe());
    expectFalse(composedView.isStructuralTraversalSafe());
});
