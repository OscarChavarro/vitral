// Port of the RefractionReader case of cpp/base/src/test/refraction_tests.cpp
// (the other cases are in base/src/test/.../render/raytracing/Refraction.spec.ts).
import { test } from "vitest";
import { expectDoubleEq, expectEq } from "../../../../../../../base/src/test/GTestAssertions.js";
import { SimpleScene } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleScene";
import { ReaderMitScene } from "../../../../../main/vsdk/toolkit/io/geometry/ReaderMitScene.js";
import { VITRAL_REPOSITORY_ROOT } from "../../../../ReaderTestPaths.js";

test("RefractionReader_given_mitSurfaceWithIndex_when_reading_then_materialKeepsIt", () => {
    const scene = new SimpleScene();
    const reader = new ReaderMitScene();
    reader.importEnvironment(VITRAL_REPOSITORY_ROOT + "/etc/geometry/mitscenes/glass_lens.ray", scene);
    const bodies = scene.getSimpleBodies();
    expectEq(3, bodies.size());
    expectDoubleEq(0.0, bodies.get(1).getMaterial()!.getRefractionCoefficient());
    expectDoubleEq(0.9, bodies.get(2).getMaterial()!.getRefractionCoefficient());
    expectDoubleEq(1.5, bodies.get(2).getMaterial()!.getIndexOfRefraction());
});
