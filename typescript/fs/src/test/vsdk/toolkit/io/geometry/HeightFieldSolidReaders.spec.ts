// Port of the reader cases of cpp/base/src/test/heightfield_solid_tests.cpp;
// the solid cases are in @vitral/base's HeightFieldSolid.spec.ts.
import * as fs from "node:fs";
import { test } from "vitest";
import {
    expectDoubleEq,
    expectEq,
    expectGt,
    expectNear,
    expectThrow,
    expectTrue,
} from "../../../../../../../base/src/test/GTestAssertions.js";
import { Exception } from "@vitral/base/java/lang/Exception";
import { VSDKFatalException } from "@vitral/base/vsdk/toolkit/common/VSDKFatalException";
import { Vector3Dd } from "@vitral/base/vsdk/toolkit/common/linealAlgebra/Vector3Dd";
import { Ray } from "@vitral/base/vsdk/toolkit/environment/geometry/element/Ray";
import { RayHit } from "@vitral/base/vsdk/toolkit/environment/geometry/element/RayHit";
import { HeightFieldSolid } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/HeightFieldSolid";
import { SimpleScene } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleScene";
import { ReaderAltimetry } from "../../../../../main/vsdk/toolkit/io/geometry/ReaderAltimetry.js";
import { ReaderMitScene } from "../../../../../main/vsdk/toolkit/io/geometry/ReaderMitScene.js";
import { TEMP_DIR, VITRAL_REPOSITORY_ROOT } from "../../../../ReaderTestPaths.js";

test("ReaderAltimetryFormat_ReadsGridsAndRejectsMalformedFiles", () => {
    const path = TEMP_DIR + "reader_altimetry.alt";
    fs.writeFileSync(path, "# comment\naltimetry 3 2 # columns rows\n0 1 2\n3 4 5\n");
    const solid = ReaderAltimetry.importHeightField(path);
    expectEq(solid.getColumnCount(), 3);
    expectEq(solid.getRowCount(), 2);
    expectDoubleEq(solid.getHeight(2, 1), 5);
    expectDoubleEq(solid.getMinimum().y(), 0);
    expectDoubleEq(solid.getMaximum().y(), 5);
    expectDoubleEq(solid.getMaximum().x(), 2);
    expectTrue(solid.hasCurrentBlocks());
    const malformed = [
        "altimetry 3 2\n0 1 2\n3 4\n",
        "altimetry 3 2\n0 1 2\n3 4 5 6\n",
        "altimetry 1 2\n0\n1\n",
        "heights 2 2\n0 0 0 0\n",
        "altimetry 2 2\n0 x 0 0\n",
    ];
    for (let i = 0; i < 5; ++i) {
        fs.writeFileSync(path, malformed[i]!);
        expectThrow(VSDKFatalException, () => ReaderAltimetry.importHeightField(path), String(i));
    }
    fs.rmSync(path);
});

test("ReaderMitPrimitives_HeightfieldSceneReadsAltimetryRelativeToTheScene", () => {
    const scene = new SimpleScene();
    const reader = new ReaderMitScene();
    reader.importEnvironment(VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/heightfield_terrain.ray", scene);
    const bodies = scene.getSimpleBodies();
    expectEq(2, bodies.size());
    const terrainGeometry = bodies.get(0).getGeometry();
    expectTrue(terrainGeometry instanceof HeightFieldSolid);
    const terrain = terrainGeometry as HeightFieldSolid;
    expectEq(65, terrain.getColumnCount());
    expectDoubleEq(0.6, terrain.getMinimum().y());
    // Straight down onto the grid centre (local 32, 32) lands on its height.
    const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
    expectTrue(
        bodies.get(0).doIntersectionFirstHit(Ray.fromAffine(new Vector3Dd(0, 0, 10), new Vector3Dd(0, 0, -1)), hit),
    );
    expectNear(10 - 0.15 * terrain.getHeight(32, 32), hit.getHitDistance(), 1e-9);
    expectGt(hit.normal.z(), 0);
});

test("ReaderMitPrimitives_MalformedHeightfieldLinesAreRejected", () => {
    const altimetry = TEMP_DIR + "reader_mit_hf.alt";
    fs.writeFileSync(altimetry, "altimetry 2 2\n0 1\n1 2\n");
    const camera = "eye 0 5 5\nlookat 0 0 0\nup 0 1 0\n";
    const bodies = [
        "heightfield\n",
        "heightfield missing.alt\n",
        "heightfield reader_mit_hf.alt water 3\n",
        "heightfield reader_mit_hf.alt level 1\n",
    ];
    const path = TEMP_DIR + "reader_mit_hf.ray";
    for (let i = 0; i < 4; ++i) {
        fs.writeFileSync(path, camera + bodies[i]!);
        const scene = new SimpleScene();
        const reader = new ReaderMitScene();
        expectThrow(Exception, () => reader.importEnvironment(path, scene), String(i));
    }
    fs.writeFileSync(path, camera + "heightfield reader_mit_hf.alt water 0.5\n");
    const scene = new SimpleScene();
    const reader = new ReaderMitScene();
    reader.importEnvironment(path, scene);
    expectEq(1, scene.getSimpleBodies().size());
    fs.rmSync(path);
    fs.rmSync(altimetry);
});
