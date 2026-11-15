// Port of the reader cases of cpp/base/src/test/subdivided_bezier_patch_tests.cpp;
// the surface cases are in @vitral/base's SubdividedBezierPatch.spec.ts.
import * as fs from "node:fs";
import { test } from "vitest";
import {
    expectEq,
    expectGt,
    expectNear,
    expectThrow,
    expectTrue,
} from "../../../../../../../base/src/test/GTestAssertions.js";
import { Exception } from "@vitral/base/java/lang/Exception";
import { Vector3Dd } from "@vitral/base/vsdk/toolkit/common/linealAlgebra/Vector3Dd";
import { Ray } from "@vitral/base/vsdk/toolkit/environment/geometry/element/Ray";
import { RayHit } from "@vitral/base/vsdk/toolkit/environment/geometry/element/RayHit";
import { BezierPatchSubdivision } from "@vitral/base/vsdk/toolkit/environment/geometry/surface/BezierPatchSubdivision";
import { SubdividedBezierPatch } from "@vitral/base/vsdk/toolkit/environment/geometry/surface/SubdividedBezierPatch";
import { SimpleScene } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleScene";
import { ReaderMitScene } from "../../../../../main/vsdk/toolkit/io/geometry/ReaderMitScene.js";
import { TEMP_DIR, VITRAL_REPOSITORY_ROOT } from "../../../../ReaderTestPaths.js";

test("ReaderMitPrimitives_PatchSceneBuildsDomeAndSheet", () => {
    const scene = new SimpleScene();
    const reader = new ReaderMitScene();
    reader.importEnvironment(VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/bezier_patches.ray", scene);
    const bodies = scene.getSimpleBodies();
    expectEq(6, bodies.size());
    const quadrant = bodies.get(1).getGeometry();
    expectTrue(quadrant instanceof SubdividedBezierPatch);
    expectEq(BezierPatchSubdivision.PrecomputedGrid, (quadrant as SubdividedBezierPatch).getSubdivision());
    expectEq(10, (quadrant as SubdividedBezierPatch).getUSteps());
    const sheet = bodies.get(5).getGeometry();
    expectTrue(sheet instanceof SubdividedBezierPatch);
    expectEq(BezierPatchSubdivision.AdaptiveTree, (sheet as SubdividedBezierPatch).getSubdivision());
    // Straight down onto the dome's pole at height 1.4.
    const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
    let found = false;
    for (let i = 1; i <= 4 && !found; ++i) {
        found = bodies
            .get(i)
            .doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(-1.52, 0.03, 5), new Vector3Dd(0, 0, -1)), hit);
    }
    expectTrue(found);
    expectNear(5 - 1.4, hit.getHitDistance(), 0.02);
    expectGt(hit.normal.z(), 0.9);
});

test("ReaderMitPrimitives_MalformedPatchBlocksAreRejected", () => {
    let controls = "";
    for (let i = 0; i < 16; ++i) controls += "control " + String(i % 4) + " " + String(Math.trunc(i / 4)) + " 0\n";
    const bodies = [
        "patch curved 4 4\n" + controls + "end\n",
        "patch grid 0 4\n" + controls + "end\n",
        "patch grid 4 4\ncontrol 0 0 0\nend\n",
        "patch grid 4 4\n" + controls + "control 0 0 0\nend\n",
        "patch tree 3 3 flatness\n" + controls + "end\n",
        "csg union\npatch grid 2 2\n" + controls + "end\nend\n",
    ];
    const camera = "eye 0 5 5\nlookat 0 0 0\nup 0 1 0\n";
    const path = TEMP_DIR + "reader_mit_patch_malformed.ray";
    for (let i = 0; i < 6; ++i) {
        fs.writeFileSync(path, camera + bodies[i]!);
        const scene = new SimpleScene();
        const reader = new ReaderMitScene();
        expectThrow(Exception, () => reader.importEnvironment(path, scene), String(i));
    }
    fs.writeFileSync(path, camera + "patch tree 3 3 flatness 0.05\n" + controls + "end\n");
    const scene = new SimpleScene();
    const reader = new ReaderMitScene();
    reader.importEnvironment(path, scene);
    expectEq(1, scene.getSimpleBodies().size());
    fs.rmSync(path);
});
