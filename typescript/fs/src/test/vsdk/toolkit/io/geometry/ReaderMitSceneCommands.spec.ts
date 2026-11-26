// Port of cpp/base/src/test/reader_mit_scene_commands_tests.cpp
import * as fs from "node:fs";
import { test } from "vitest";
import {
    expectDoubleEq,
    expectEq,
    expectFalse,
    expectGt,
    expectLe,
    expectLt,
    expectNe,
    expectNear,
    expectThrow,
    expectTrue,
} from "../../../../../../../base/src/test/GTestAssertions.js";
import { Exception } from "@vitral/base/java/lang/Exception";
import { Vector3Dd } from "@vitral/base/vsdk/toolkit/common/linealAlgebra/Vector3Dd";
import { CubemapBackground } from "@vitral/base/vsdk/toolkit/environment/background/CubemapBackground";
import { Ray } from "@vitral/base/vsdk/toolkit/environment/geometry/element/Ray";
import { RayHit } from "@vitral/base/vsdk/toolkit/environment/geometry/element/RayHit";
import { QuadMesh } from "@vitral/base/vsdk/toolkit/environment/geometry/surface/QuadMesh";
import { Box } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/Box";
import { Cone } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/Cone";
import { Prism } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/Prism";
import { Torus } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/Torus";
import { PolyhedralBoundedSolid } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid";
import { SimpleScene } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleScene";
import { CsgBody } from "@vitral/base/vsdk/toolkit/environment/scene/csg/CsgBody";
import { ReaderMitScene } from "../../../../../main/vsdk/toolkit/io/geometry/ReaderMitScene.js";
import { TEMP_DIR, VITRAL_REPOSITORY_ROOT } from "../../../../ReaderTestPaths.js";

test("ReaderMitCommands_ClassicPrimitivesRotationAndCubemapAreRead", () => {
    const scene = new SimpleScene();
    const reader = new ReaderMitScene();
    reader.importEnvironment(VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/object.ray", scene);
    const bodies = scene.getSimpleBodies();
    expectEq(12, bodies.size());
    const cube = bodies.get(1).getGeometry();
    expectTrue(cube instanceof Box);
    expectDoubleEq(1.0, (cube as Box).getSize().x());
    expectDoubleEq(-2.0, bodies.get(1).getPosition().x());
    expectTrue(bodies.get(2).getGeometry() instanceof Cone);
    // `rotation 0 0.3 0` turns every later primitive about its own origin.
    expectGt(Math.abs(bodies.get(1).getRotation().get(0, 2)) + Math.abs(bodies.get(1).getRotation().get(2, 0)), 0.1);
    expectTrue(scene.getActiveBackground() instanceof CubemapBackground);
    // The six cubemap faces are scene resources.
    expectEq(6, scene.getImages().size());
});

test("ReaderMitCommands_MeshPolyhedralAndTorusBodiesHaveRealHits", () => {
    const scenes = ["quadmesh_test.ray", "balls_featured_object.ray", "balls_torus.ray"];
    for (let i = 0; i < 3; ++i) {
        const scene = new SimpleScene();
        const reader = new ReaderMitScene();
        const path = VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/" + scenes[i]!;
        reader.importEnvironment(path, scene);
        const bodies = scene.getSimpleBodies();
        expectLe(2, bodies.size(), scenes[i]);
        const center = bodies.get(1);
        if (i === 0) expectTrue(center.getGeometry() instanceof QuadMesh);
        if (i === 1) expectTrue(center.getGeometry() instanceof PolyhedralBoundedSolid);
        if (i === 2) expectTrue(center.getGeometry() instanceof Torus);
        // Every object sits at the origin and is hit from above.
        const height = i === 2 ? 0.13 : 0.0;
        const hit = new RayHit(RayHit.DETAIL_POINT, false);
        const origin = i === 2 ? new Vector3Dd(0.36, 0, 5) : new Vector3Dd(0, 0, 5);
        expectTrue(center.doIntersectionFirstHit(new Ray(origin, new Vector3Dd(0, 0, -1)), hit), scenes[i]);
        expectLt(hit.getHitDistance(), 5.0, scenes[i]);
        if (i === 2) expectNear(5 - height, hit.getHitDistance(), 1e-6);
    }
});

test("ReaderMitCommands_TexturesAndBumpMapsAreSharedSceneResources", () => {
    const path = TEMP_DIR + "reader_mit_textures.ray";
    fs.writeFileSync(
        path,
        "eye 0 -5 0\nup 0 0 1\nlookat 0 0 0\n" +
            'texture "' +
            VITRAL_REPOSITORY_ROOT +
            '/etc/textures/miniearth.png"\n' +
            'bumpmap "' +
            VITRAL_REPOSITORY_ROOT +
            '/etc/bumpmaps/earth.bw" 1 1 0.2\n' +
            "sphere -2 0 0 1\nsphere 2 0 0 1\n" +
            'texture "' +
            VITRAL_REPOSITORY_ROOT +
            '/etc/textures/miniearth.png"\n' +
            "notexture\nnobumpmap\nsphere 0 0 2 1\n",
    );
    const scene = new SimpleScene();
    const reader = new ReaderMitScene();
    reader.importEnvironment(path, scene);
    fs.rmSync(path);
    const bodies = scene.getSimpleBodies();
    expectEq(3, bodies.size());
    expectNe(null, bodies.get(0).getTexture());
    expectEq(bodies.get(0).getTexture(), bodies.get(1).getTexture());
    expectNe(null, bodies.get(0).getNormalMap());
    expectEq(bodies.get(0).getNormalMap(), bodies.get(1).getNormalMap());
    expectEq(null, bodies.get(2).getTexture());
    expectEq(null, bodies.get(2).getNormalMap());
    // A repeated texture file is loaded once.
    expectEq(1, scene.getImages().size());
    expectEq(1, scene.getNormalMaps().size());
    // Removing a body leaves the shared resources to the scene.
    bodies.remove(0);
    expectEq(scene.getImages().get(0), bodies.get(0).getTexture());
});

test("ReaderMitCommands_ResourcesResolveFromTheSceneDirectory", () => {
    const scene = new SimpleScene();
    const reader = new ReaderMitScene();
    // bumpMapping.ray names its files relative to its own directory.
    reader.importEnvironment(VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/bumpMapping.ray", scene);
    expectEq(1, scene.getSimpleBodies().size());
    expectNe(null, scene.getSimpleBodies().get(0).getTexture());
    expectNe(null, scene.getSimpleBodies().get(0).getNormalMap());
});

test("ReaderMitCommands_UnknownAndMalformedCommandsAreRejected", () => {
    const bodies = [
        "spheer 0 0 0 1\n",
        "cube 0 0 0 -1\n",
        "texture /nonexistent/file.png\n",
        "quads\nv 0 0 0\nq 0 1 2 3\nend\n",
        "rotation 1 2\n",
        "v 0 0 0\n",
        "light 1 1 1 spot 0 0 0\n",
        "appel 4 0 0 0\n",
        "quads\nv 0 0 0\n",
        "csg union\ntorus 0 0 0 1 0.2\nend\n",
    ];
    const path = TEMP_DIR + "reader_mit_commands_malformed.ray";
    for (let i = 0; i < 10; ++i) {
        fs.writeFileSync(path, "eye 0 -5 0\nup 0 0 1\nlookat 0 0 0\n" + bodies[i]!);
        const scene = new SimpleScene();
        const reader = new ReaderMitScene();
        expectThrow(Exception, () => reader.importEnvironment(path, scene), String(i));
    }
    fs.rmSync(path);
});

test("ReaderMitCommands_PrismsAreParametricBodiesAndCsgOperands", () => {
    const scene = new SimpleScene();
    const reader = new ReaderMitScene();
    reader.importEnvironment(VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/prism_csg.ray", scene);
    const bodies = scene.getSimpleBodies();
    expectEq(4, bodies.size());
    const star = bodies.get(1).getGeometry();
    expectTrue(star instanceof Prism);
    expectEq(10, (star as Prism).getNumberOfSides());
    expectDoubleEq(0.6, (star as Prism).getHeight());
    expectDoubleEq(-2.3, bodies.get(1).getPosition().x());
    expectTrue(bodies.get(2) instanceof CsgBody);
    expectTrue(bodies.get(3) instanceof CsgBody);

    // The star prism is hit on its top cap; its notch between two points is not
    const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
    expectTrue(bodies.get(1).doIntersectionFirstHit(new Ray(new Vector3Dd(-2.3, 0, 5), new Vector3Dd(0, 0, -1)), hit));
    expectNear(5 - 0.6, hit.getHitDistance(), 1e-9);
    expectFalse(
        bodies
            .get(1)
            .doIntersectionFirstHit(new Ray(new Vector3Dd(-2.3 - 0.411449, 0.566312, 5), new Vector3Dd(0, 0, -1)), hit),
    );
    // The spherical bite of the difference: the ray down the axis reaches the
    // bottom of the bite, at z = 1.4 - 0.75
    expectTrue(bodies.get(2).doIntersectionFirstHit(new Ray(new Vector3Dd(0, 0, 5), new Vector3Dd(0, 0, -1)), hit));
    expectNear(5 - 0.65, hit.getHitDistance(), 1e-9);
});

test("ReaderMitCommands_MalformedPrismsAreRejected", () => {
    const lines = [
        "prism 0 0 0 1 2 0 0 1 0\n",
        "prism 0 0 0 -1 3 0 0 1 0 0 1\n",
        "prism 0 0 0 1 3 0 0 1 0\n",
        "prism 0 0 0 1 4 0 0 1 1 1 0 0 1\n",
        "prism 0 0 0 1 3 0 0 1 0 0 1 7\n",
    ];
    const path = TEMP_DIR + "reader_mit_prism_malformed.ray";
    for (let i = 0; i < 5; ++i) {
        fs.writeFileSync(path, "eye 0 -5 0\nup 0 0 1\nlookat 0 0 0\n" + lines[i]!);
        const scene = new SimpleScene();
        const reader = new ReaderMitScene();
        expectThrow(Exception, () => reader.importEnvironment(path, scene), String(i));
    }
    fs.rmSync(path);
});
