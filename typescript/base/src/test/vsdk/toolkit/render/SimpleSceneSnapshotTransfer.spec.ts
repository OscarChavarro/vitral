import { describe, expect, it } from "vitest";
import { Math as JavaMath } from "java/lang/Math.js";
import { ColorRgb } from "vsdk/toolkit/common/color/ColorRgb.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { FixedBackground } from "vsdk/toolkit/environment/background/FixedBackground.js";
import { SimpleBackground } from "vsdk/toolkit/environment/background/SimpleBackground.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { ParametricCurve } from "vsdk/toolkit/environment/geometry/curve/ParametricCurve.js";
import { Geometry } from "vsdk/toolkit/environment/geometry/Geometry.js";
import { FunctionalExplicitSurface } from "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.js";
import { InfinitePlane } from "vsdk/toolkit/environment/geometry/surface/InfinitePlane.js";
import { ParametricBiCubicPatch } from "vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.js";
import { Arrow } from "vsdk/toolkit/environment/geometry/volume/Arrow.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { Cone } from "vsdk/toolkit/environment/geometry/volume/Cone.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { Torus } from "vsdk/toolkit/environment/geometry/volume/Torus.js";
import { VoxelVolume } from "vsdk/toolkit/environment/geometry/volume/VoxelVolume.js";
import { Voxelization } from "vsdk/toolkit/environment/geometry/geometricProcessing/Voxelization.js";
import { AmbientLight } from "vsdk/toolkit/environment/light/AmbientLight.js";
import { PointLight } from "vsdk/toolkit/environment/light/PointLight.js";
import { RendererConfiguration } from "vsdk/toolkit/environment/material/RendererConfiguration.js";
import { SimpleMaterial } from "vsdk/toolkit/environment/material/SimpleMaterial.js";
import { SimpleBody } from "vsdk/toolkit/environment/scene/SimpleBody.js";
import { SimpleScene } from "vsdk/toolkit/environment/scene/SimpleScene.js";
import type { SimpleSceneSnapshot } from "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.js";
import { NormalMap } from "vsdk/toolkit/media/NormalMap.js";
import { IndexedColorImageUncompressed } from "vsdk/toolkit/media/IndexedColorImageUncompressed.js";
import { RGBAImageUncompressed } from "vsdk/toolkit/media/RGBAImageUncompressed.js";
import { RGBImageUncompressed } from "vsdk/toolkit/media/RGBImageUncompressed.js";
import { SimpleRaytracer } from "vsdk/toolkit/render/raytracing/SimpleRaytracer.js";
import {
    decodeSimpleSceneSnapshot,
    encodeSimpleSceneSnapshot,
} from "vsdk/toolkit/render/raytracing/SimpleSceneSnapshotTransfer.js";
import type { EntityEvent } from "vsdk/toolkit/common/EntityEvent.js";

function body(geometry: Geometry, position: Vector3Dd): SimpleBody {
    const b: SimpleBody = new SimpleBody();
    b.setGeometry(geometry);
    b.setPosition(position);
    b.setRotation(new Matrix4x4d().eulerAnglesRotation(0.3, 0.2, 0.1));
    b.setRotationInverse(new Matrix4x4d().eulerAnglesRotation(0.3, 0.2, 0.1).inverse());
    b.setMaterial(new SimpleMaterial().withDiffuse(new ColorRgb(0.8, 0.5, 0.3)).withDoubleSided(true));
    return b;
}

function checker(size: number): RGBImageUncompressed {
    const image: RGBImageUncompressed = new RGBImageUncompressed();
    image.init(size, size);
    for (let y = 0; y < size; y++) {
        for (let x = 0; x < size; x++) {
            const on: boolean = ((x >> 2) + (y >> 2)) % 2 === 0;
            image.putPixel(x, y, on ? 255 : 20, on ? 200 : 40, on ? 100 : 60);
        }
    }
    return image;
}

/** A scene with one body per kind of geometry the scene editor creates */
function createEditorScene(background: "simple" | "fixed"): SimpleScene {
    const scene: SimpleScene = new SimpleScene();
    const camera: Camera = new Camera();
    camera.setPosition(new Vector3Dd(-9, -9, 7));
    camera.setRotation(new Matrix4x4d().eulerAnglesRotation(JavaMath.toRadians(45), JavaMath.toRadians(-30), 0));
    scene.addCamera(camera);
    if (background === "simple") {
        const simple: SimpleBackground = new SimpleBackground();
        simple.setColor(0.49, 0.49, 0.49);
        scene.addBackground(simple);
    }
    else {
        const image: RGBAImageUncompressed = new RGBAImageUncompressed();
        image.init(8, 8);
        scene.addBackground(new FixedBackground(camera, image));
    }

    const textured: SimpleBody = body(new Sphere(1.0), new Vector3Dd(0, 0, 0));
    textured.setTexture(checker(32));
    const bump: IndexedColorImageUncompressed = new IndexedColorImageUncompressed();
    bump.init(16, 16);
    for (let y = 0; y < 16; y++) {
        for (let x = 0; x < 16; x++) {
            bump.putPixel(x, y, (x * 16 + y * 7) % 256);
        }
    }
    const normalMap: NormalMap = new NormalMap();
    normalMap.importBumpMap(bump, new Vector3Dd(1, 1, 0.2));
    textured.setNormalMap(normalMap);
    // A listener of the GUI must not travel
    textured.addEntityListener({ notifyEntityEvent: (_event: EntityEvent): void => {} });
    scene.addBody(textured);

    scene.addBody(body(new Cone(1, 0, 2), new Vector3Dd(3, 0, 0)));
    scene.addBody(body(new Cone(1, 1, 2), new Vector3Dd(-3, 0, 0)));
    scene.addBody(body(new Box(1, 3, 2), new Vector3Dd(0, 3, 0)));
    scene.addBody(body(new Arrow(0.7, 0.3, 0.05, 0.1), new Vector3Dd(0, -3, 0)));
    scene.addBody(body(new Torus(2, 1), new Vector3Dd(4, 4, 0)));
    scene.addBody(body(new InfinitePlane(new Vector3Dd(-0.2, 0, 1), new Vector3Dd(0, 0, -1)), new Vector3Dd(0, 0, 0)));

    const surface: FunctionalExplicitSurface = new FunctionalExplicitSurface("cos((PI*x)/2)");
    surface.setBounds(-2, -2, -2, 2, 2, 2);
    surface.setTesselationHint(20, 20);
    scene.addBody(body(surface, new Vector3Dd(-4, 4, 0)));

    const contour: ParametricCurve = new ParametricCurve();
    contour.addPoint([new Vector3Dd(0, 0, 0), new Vector3Dd(0, -1, 0), new Vector3Dd(1, 0, 0)], ParametricCurve.HERMITE);
    contour.addPoint([new Vector3Dd(1, 0, 0), new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0)], ParametricCurve.HERMITE);
    contour.addPoint([new Vector3Dd(1, 1, 0.4), new Vector3Dd(0, 1, 0), new Vector3Dd(-1, 0, 0)], ParametricCurve.HERMITE);
    contour.addPoint([new Vector3Dd(0, 1, 0), new Vector3Dd(-1, 0, 0), new Vector3Dd(0, -1, 0)], ParametricCurve.HERMITE);
    contour.addPoint(contour.getPoint(0), ParametricCurve.HERMITE);
    const patch: ParametricBiCubicPatch = new ParametricBiCubicPatch();
    patch.buildFergusonPatch(contour);
    patch.setApproximationSteps(20);
    scene.addBody(body(patch, new Vector3Dd(-4, -4, 0)));
    scene.addBody(body(contour, new Vector3Dd(4, -4, 0)));

    const brep = new Box(0.9, 0.9, 0.9).exportToPolyhedralBoundedSolid();
    scene.addBody(body(brep, new Vector3Dd(6, 0, 0)));

    const volume: VoxelVolume = new VoxelVolume();
    volume.init(16, 16, 16);
    Voxelization.doVoxelization(new Sphere(0.5), volume,
        VoxelVolume.getTransformFromVoxelFrameToMinMax(Array.from(new Sphere(0.5).getMinMax())), null);
    scene.addBody(body(volume, new Vector3Dd(-6, 0, 0)));

    const ambient: AmbientLight = new AmbientLight(new ColorRgb(0.2, 0.2, 0.2));
    ambient.setId(0);
    scene.addLight(ambient);
    const point: PointLight = new PointLight(new Vector3Dd(3, -3, 6), new ColorRgb(1, 1, 1));
    point.setId(1);
    scene.addLight(point);
    return scene;
}

function render(snapshot: SimpleSceneSnapshot, width: number, height: number): RGBImageUncompressed {
    const image: RGBImageUncompressed = new RGBImageUncompressed();
    image.init(width, height);
    new SimpleRaytracer().execute(image, new RendererConfiguration(), snapshot, null);
    return image;
}

describe("SimpleSceneSnapshotTransfer", () => {
    it.each(["simple", "fixed"] as const)(
        "given an editor scene with %s background when carried to a worker then it raytraces the same image",
        (background: "simple" | "fixed") => {
            // Arrange
            const width = 64;
            const height = 48;
            const snapshot: SimpleSceneSnapshot = createEditorScene(background).exportToSimpleSceneSnapshot(width, height);

            // Act
            const descriptor = encodeSimpleSceneSnapshot(snapshot);
            const cloned = structuredClone(descriptor);
            const rebuilt: SimpleSceneSnapshot = decodeSimpleSceneSnapshot(cloned);

            // Assert
            const expected: RGBImageUncompressed = render(snapshot, width, height);
            const actual: RGBImageUncompressed = render(rebuilt, width, height);
            let different = 0;
            for (let y = 0; y < height; y++) {
                for (let x = 0; x < width; x++) {
                    const a = expected.getPixel(x, y);
                    const b = actual.getPixel(x, y);
                    if (a.r !== b.r || a.g !== b.g || a.b !== b.b) {
                        different++;
                    }
                }
            }
            expect(different).toBe(0);
            expect(rebuilt.getSimpleBodies().size()).toBe(snapshot.getSimpleBodies().size());
        },
    );

    it("given a body with listeners when carried then the listeners stay behind", () => {
        // Arrange
        const snapshot: SimpleSceneSnapshot = createEditorScene("simple").exportToSimpleSceneSnapshot(8, 8);

        // Act
        const rebuilt: SimpleSceneSnapshot = decodeSimpleSceneSnapshot(structuredClone(encodeSimpleSceneSnapshot(snapshot)));

        // Assert: notifying the rebuilt body reaches nobody
        expect(() => rebuilt.getSimpleBodies().get(0).update()).not.toThrow();
    });
});
