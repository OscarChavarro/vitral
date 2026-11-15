// Port of cpp/base/src/test/csg_point_workspace_tests.cpp
import { test } from "vitest";
import { expectDoubleEq, expectEq, expectGt, expectLt, expectTrue } from "../../../../../GTestAssertions.js";
import { AffinePlacementStatus } from "vsdk/toolkit/environment/scene/AffinePlacementStatus.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { CsgBodyOperand } from "vsdk/toolkit/environment/scene/csg/CsgBodyOperand.js";
import { CsgCrossingMembershipFilter } from "vsdk/toolkit/environment/scene/csg/CsgCrossingMembershipFilter.js";
import { CsgOperation } from "vsdk/toolkit/environment/scene/csg/CsgOperation.js";
import { CsgPlacedExpression } from "vsdk/toolkit/environment/scene/csg/CsgPlacedExpression.js";
import { CsgPointClassification } from "vsdk/toolkit/environment/scene/csg/CsgPointClassification.js";
import { CsgPointWorkspace } from "vsdk/toolkit/environment/scene/csg/CsgPointWorkspace.js";
import { CsgRaySurfaceEvents } from "vsdk/toolkit/environment/scene/csg/CsgRaySurfaceEvents.js";
import { CsgTree } from "vsdk/toolkit/environment/scene/csg/CsgTree.js";
import { Double } from "java/lang/Double.js";
import { GeometryContainment } from "vsdk/toolkit/environment/geometry/element/GeometryContainment.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayCrossingQuery } from "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.js";
import { RaySolidQueryStatus } from "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.js";
import { SimpleBody } from "vsdk/toolkit/environment/scene/SimpleBody.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { UncertifiedTestBody } from "../../../../../UncertifiedTestBody.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
// Random trees with repeated operands, a sheared placed subtree, an uncertain
// operand and points on and near boundaries: the containment-only query must
// give the result of the full classification for every point and tolerance.
test("CsgPointWorkspaceContract_ContainmentMatchesFullClassificationOnRandomTrees", () => {
    let seed = 97531;
    const bodyCount = 5;
    const bodies: SimpleBody[] = Array.from({ length: bodyCount }, () => new SimpleBody());
    for (let i = 0; i < bodyCount; ++i) {
        if (i % 2 === 0) bodies[i].setGeometry(new Sphere(0.6 + 0.1 * i));
        else bodies[i].setGeometry(new Box(1.0 + 0.2 * i, 1.4, 0.9 + 0.1 * i));
    }
    const opaque = new UncertifiedTestBody();
    opaque.setGeometry(new Sphere(0.7));
    const tolerances = [0, 1e-6, 0.05];
    const shear = [
        [1.3, 0.2, 0, 0.1],
        [0, 0.8, 0.3, -0.2],
        [0.1, 0, 1.1, 0.05],
        [0, 0, 0, 1],
    ];
    const forward = new Matrix4x4d(shear);
    const workspace = new CsgPointWorkspace();
    let compared = 0;
    let definite = 0;
    for (let trial = 0; trial < 120; ++trial) {
        for (let i = 0; i < bodyCount; ++i) {
            seed = (Math.imul(seed, 1103515245) + 12345) >>> 0;
            const x = ((seed >>> 8) % 2000) / 1000.0 - 1.0;
            seed = (Math.imul(seed, 1103515245) + 12345) >>> 0;
            const y = ((seed >>> 8) % 2000) / 2000.0 - 0.5;
            bodies[i].setPosition(new Vector3Dd(x, y, 0.1 * i - 0.2));
        }
        opaque.setPosition(new Vector3Dd(0.3 * (trial % 5) - 0.6, 0, 0));
        const o0 = new CsgBodyOperand(bodies[0]);
        const o1 = new CsgBodyOperand(bodies[1]);
        const o2 = new CsgBodyOperand(bodies[2]);
        const o3 = new CsgBodyOperand(bodies[3]);
        const o4 = new CsgBodyOperand(bodies[4]);
        const again = new CsgBodyOperand(bodies[1]);
        const u = new CsgBodyOperand(opaque);
        const ops = [CsgOperation.UNION, CsgOperation.INTERSECTION, CsgOperation.DIFFERENCE];
        const inner = new CsgTree(ops[trial % 3]);
        const middle = new CsgTree(ops[Math.trunc(trial / 3) % 3]);
        const root = new CsgTree(ops[Math.trunc(trial / 9) % 3]);
        expectTrue(inner.addOperand(o0));
        expectTrue(inner.addOperand(o1));
        expectTrue(inner.addOperand(again));
        if (trial % 4 === 0) expectTrue(inner.addOperand(u));
        const placed = new CsgPlacedExpression(inner);
        expectEq(
            AffinePlacementStatus.Valid,
            placed.setAffinePlacement(
                trial % 2 ? forward : Matrix4x4d.identityMatrix(),
                trial % 2 ? forward.inverse() : Matrix4x4d.identityMatrix(),
            ),
        );
        expectTrue(middle.addOperand(o3));
        expectTrue(middle.addOperand(placed));
        expectTrue(root.addOperand(middle));
        expectTrue(root.addOperand(o4));
        expectTrue(root.addOperand(o2));
        if (trial % 6 === 0) expectTrue(root.addOperand(o2));
        for (let k = 0; k < 40; ++k) {
            let p = new Vector3Dd();
            if (k < 20) {
                seed = (Math.imul(seed, 1103515245) + 12345) >>> 0;
                const x = ((seed >>> 8) % 3000) / 1000.0 - 1.5;
                seed = (Math.imul(seed, 1103515245) + 12345) >>> 0;
                const y = ((seed >>> 8) % 2000) / 1000.0 - 1.0;
                p = new Vector3Dd(x, y, 0.05);
            } else {
                // Points on the spheres and box faces.
                const b = bodies[k % bodyCount];
                const r = k % 2 === 0 ? 0.6 + 0.1 * (k % bodyCount) : 0.5 * (1.0 + 0.2 * (k % bodyCount));
                p = b.getPosition().add(new Vector3Dd(r, 0, 0));
            }
            for (let t = 0; t < 3; ++t) {
                const full = CsgPointClassification.classify(root, p, tolerances[t]);
                const lean = CsgPointClassification.classifyContainment(root, p, tolerances[t], workspace);
                expectEq(full, lean);
                ++compared;
                if (
                    full === GeometryContainment.Inside ||
                    full === GeometryContainment.Outside ||
                    full === GeometryContainment.Boundary
                )
                    ++definite;
                expectEq(0, workspace.results.size());
                expectEq(0, workspace.path.size());
            }
        }
    }
    expectGt(definite, Math.trunc(compared / 2));
    expectLt(definite, compared);
}, 300_000);

test("CsgPointWorkspaceContract_OperandContainmentAndInvalidInput", () => {
    const ball = new SimpleBody();
    ball.setGeometry(new Sphere(1));
    const a = new CsgBodyOperand(ball);
    const workspace = new CsgPointWorkspace();
    expectEq(
        GeometryContainment.Inside,
        CsgPointClassification.classifyOperandContainment(a, new Vector3Dd(0, 0, 0), 0, workspace),
    );
    expectEq(
        GeometryContainment.Boundary,
        CsgPointClassification.classifyOperandContainment(a, new Vector3Dd(1, 0, 0), 1e-9, workspace),
    );
    expectEq(
        GeometryContainment.Outside,
        CsgPointClassification.classifyOperandContainment(a, new Vector3Dd(3, 0, 0), 0, workspace),
    );
    expectEq(
        GeometryContainment.InvalidInput,
        CsgPointClassification.classifyOperandContainment(a, new Vector3Dd(0, 0, 0), -1, workspace),
    );
    const tree = new CsgTree(CsgOperation.UNION);
    expectTrue(tree.addOperand(a));
    expectEq(
        GeometryContainment.InvalidInput,
        CsgPointClassification.classifyContainment(tree, new Vector3Dd(0, Double.NaN, 0), 0, workspace),
    );
});

// The heuristic gives the same events with a reused workspace as with a fresh
// one, query after query.
test("CsgPointWorkspaceContract_HeuristicSelectionReusesTheWorkspace", () => {
    const a = new SimpleBody();
    const b = new SimpleBody();
    const c = new SimpleBody();
    a.setGeometry(new Sphere(2));
    b.setGeometry(new Sphere(1));
    c.setGeometry(new Sphere(1));
    c.setPosition(new Vector3Dd(1.5, 0, 0));
    const oa = new CsgBodyOperand(a);
    const ob = new CsgBodyOperand(b);
    const oc = new CsgBodyOperand(c);
    const inner = new CsgTree(CsgOperation.UNION);
    const root = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(inner.addOperand(ob));
    expectTrue(inner.addOperand(oc));
    expectTrue(root.addOperand(oa));
    expectTrue(root.addOperand(inner));
    const workspace = new CsgPointWorkspace();
    const fresh = new CsgRaySurfaceEvents();
    const reused = new CsgRaySurfaceEvents();
    for (let i = 0; i < 20; ++i) {
        const ray = Ray.fromAffine(new Vector3Dd(-5, 0.1 * (i % 7) - 0.3, 0.05 * (i % 3)), new Vector3Dd(1, 0, 0));
        expectEq(
            RaySolidQueryStatus.Supported,
            CsgCrossingMembershipFilter.select(root, ray, new RayCrossingQuery(0, 20), 1e-9, fresh),
        );
        expectEq(
            RaySolidQueryStatus.Supported,
            CsgCrossingMembershipFilter.select(root, ray, new RayCrossingQuery(0, 20), 1e-9, workspace, reused),
        );
        expectEq(fresh.size(), reused.size());
        for (let k = 0; k < fresh.size(); ++k) {
            expectEq(fresh.get(k)!.leafIndex, reused.get(k)!.leafIndex);
            expectEq(fresh.get(k)!.boundaryIndex, reused.get(k)!.boundaryIndex);
            expectDoubleEq(fresh.getT(k), reused.getT(k));
        }
        // Difference: both outer crossings plus the hole crossings inside the ball.
        expectEq(4, reused.size());
    }
});
