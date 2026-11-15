// Port of cpp/base/src/test/refraction_tests.cpp (except RefractionReader,
// which reads a file and lives in @vitral/fs: fs/src/test/.../RefractionReader.spec.ts)
import { test } from "vitest";
import {
    expectDoubleEq,
    expectEq,
    expectFalse,
    expectLt,
    expectNe,
    expectNear,
    expectTrue,
} from "../../../../GTestAssertions.js";
import { Math as JavaMath } from "java/lang/Math.js";
import { ColorRgb } from "vsdk/toolkit/common/color/ColorRgb.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { SimpleBackground } from "vsdk/toolkit/environment/background/SimpleBackground.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { AmbientLight } from "vsdk/toolkit/environment/light/AmbientLight.js";
import { RendererConfiguration } from "vsdk/toolkit/environment/material/RendererConfiguration.js";
import { SimpleMaterial } from "vsdk/toolkit/environment/material/SimpleMaterial.js";
import { SimpleBody } from "vsdk/toolkit/environment/scene/SimpleBody.js";
import { SimpleScene } from "vsdk/toolkit/environment/scene/SimpleScene.js";
import { RGBImageUncompressed } from "vsdk/toolkit/media/RGBImageUncompressed.js";
import { RGBPixel } from "vsdk/toolkit/media/RGBPixel.js";
import { TraceWorkspace } from "vsdk/toolkit/render/TraceWorkspace.js";
import { SimpleRaytracer } from "vsdk/toolkit/render/raytracing/SimpleRaytracer.js";
import { SnellRefraction } from "vsdk/toolkit/render/raytracing/SnellRefraction.js";
import { ShadowTransmittance } from "vsdk/toolkit/render/shaders/ShadowTransmittance.js";

function createSphere(position: Vector3Dd, material: SimpleMaterial): SimpleBody {
    const body = new SimpleBody();
    body.setGeometry(new Sphere(1.0));
    body.setPosition(position);
    body.setRotation(new Matrix4x4d());
    body.setRotationInverse(new Matrix4x4d());
    body.setMaterial(new SimpleMaterial(material));
    return body;
}

function glass(kt: number): SimpleMaterial {
    const m = new SimpleMaterial();
    return m.withRefractionCoefficient(kt).withIndexOfRefraction(1.5);
}

test("SnellRefraction_given_normalIncidence_when_refracting_then_directionIsUnchanged", () => {
    const out = [new Vector3Dd()];
    expectTrue(SnellRefraction.refract(new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, 1), 1.0 / 1.5, out));
    expectNear(0.0, out[0]!.x(), 1e-12);
    expectNear(0.0, out[0]!.y(), 1e-12);
    expectNear(-1.0, out[0]!.z(), 1e-12);
});

test("SnellRefraction_given_obliqueRayIntoGlass_when_refracting_then_snellLawHolds", () => {
    const s = Math.sqrt(0.5);
    const out = [new Vector3Dd()];
    // Incoming ray travels along (1, 0, -1); it comes from air into glass.
    expectTrue(SnellRefraction.refract(new Vector3Dd(-s, 0, s), new Vector3Dd(0, 0, 1), 1.0 / 1.5, out));
    expectNear(1.0, out[0]!.length(), 1e-12);
    // n1 sin(theta1) == n2 sin(theta2)
    expectNear(1.0 * s, 1.5 * out[0]!.x(), 1e-12);
    expectLt(out[0]!.z(), 0.0);
});

test("SnellRefraction_given_rayBeyondCriticalAngle_when_refracting_then_totalInternalReflection", () => {
    // Glass to air (eta 1.5): the critical angle is about 41.8 degrees.
    const angle = (60.0 * Math.PI) / 180.0;
    const towardViewer = new Vector3Dd(-Math.sin(angle), 0, Math.cos(angle));
    const out = [new Vector3Dd(7, 7, 7)];
    expectFalse(SnellRefraction.refract(towardViewer, new Vector3Dd(0, 0, 1), 1.5, out));
    expectDoubleEq(7.0, out[0]!.x());
    const mirror = SnellRefraction.reflect(towardViewer, new Vector3Dd(0, 0, 1));
    expectNear(Math.sin(angle), mirror.x(), 1e-12);
    expectNear(Math.cos(angle), mirror.z(), 1e-12);
});

test("ShadowTransmittance_given_blockers_when_evaluating_then_opaqueStopsAndGlassFilters", () => {
    const scene = new SimpleScene();
    const opaque = new SimpleMaterial();
    scene.addBody(createSphere(new Vector3Dd(0, 0, 3), glass(0.9)));
    scene.addBody(createSphere(new Vector3Dd(0, 0, 6), glass(0.5)));
    scene.addBody(createSphere(new Vector3Dd(0, 0, 20), opaque));
    const bodies = scene.getSimpleBodies();
    const workspace = new TraceWorkspace();
    const shadowRay = new Ray(new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 1));

    // One glass body before the light.
    expectDoubleEq(0.9, ShadowTransmittance.evaluate(shadowRay, 4.5, bodies, workspace, false));
    // Two glass bodies multiply their transmission coefficients.
    expectDoubleEq(0.45, ShadowTransmittance.evaluate(shadowRay, 10.0, bodies, workspace, false));
    // An opaque body stops the light.
    expectDoubleEq(0.0, ShadowTransmittance.evaluate(shadowRay, 30.0, bodies, workspace, false));
    // Bodies behind the light do not cast shadows.
    expectDoubleEq(1.0, ShadowTransmittance.evaluate(shadowRay, 1.0, bodies, workspace, false));
});

test("RefractionRender_given_glassWithIndexOfAir_when_raytracing_then_sphereIsInvisible", () => {
    const width = 41;
    const height = 41;
    const scene = new SimpleScene();
    const camera = new Camera();
    camera.setPosition(new Vector3Dd(-5, -5, 5));
    camera.setRotation(new Matrix4x4d().eulerAnglesRotation(JavaMath.toRadians(45), JavaMath.toRadians(-35), 0));
    const background = new SimpleBackground();
    background.setColor(0.2, 0.6, 0.4);
    scene.addCamera(camera);
    scene.addBackground(background);
    scene.addLight(new AmbientLight(new ColorRgb(1, 1, 1)));
    let clear = new SimpleMaterial();
    clear = clear
        .withAmbient(new ColorRgb(0, 0, 0))
        .withDiffuse(new ColorRgb(0, 0, 0))
        .withSpecular(new ColorRgb(0, 0, 0))
        .withRefractionCoefficient(1.0)
        .withIndexOfRefraction(1.0);
    scene.addBody(createSphere(new Vector3Dd(0, 0, 0), clear));
    let snapshot = scene.exportToSimpleSceneSnapshot(width, height);
    const quality = new RendererConfiguration();
    const image = new RGBImageUncompressed();
    image.init(width, height);

    const raytracer = new SimpleRaytracer();
    raytracer.execute(image, quality, snapshot, null, null);

    const corner = new RGBPixel();
    const center = new RGBPixel();
    image.getPixelRgb(0, 0, corner);
    image.getPixelRgb(Math.trunc(width / 2), Math.trunc(height / 2), center);
    expectEq(corner.getR(), center.getR());
    expectEq(corner.getG(), center.getG());
    expectEq(corner.getB(), center.getB());

    // Control: the same sphere with a little ambient reflectance is seen at the center.
    scene
        .getSimpleBodies()
        .get(0)
        .setMaterial(new SimpleMaterial(clear.withAmbient(new ColorRgb(1, 0, 0))));
    snapshot = scene.exportToSimpleSceneSnapshot(width, height);
    raytracer.execute(image, quality, snapshot, null, null);
    image.getPixelRgb(Math.trunc(width / 2), Math.trunc(height / 2), center);
    expectNe(corner.getR(), center.getR());
});
