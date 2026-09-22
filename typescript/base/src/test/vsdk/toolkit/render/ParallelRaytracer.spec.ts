import { describe, expect, it } from "vitest";
import type { WorkerExecutionOptions, WorkerExecutor, WorkerTransferValue } from "java/concurrent/WorkerProtocol.js";
import { Math as JavaMath } from "java/lang/Math.js";
import { ColorRgb } from "vsdk/toolkit/common/color/ColorRgb.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { SimpleBackground } from "vsdk/toolkit/environment/background/SimpleBackground.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import type { Geometry } from "vsdk/toolkit/environment/geometry/Geometry.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { PointLight } from "vsdk/toolkit/environment/light/PointLight.js";
import { RendererConfiguration } from "vsdk/toolkit/environment/material/RendererConfiguration.js";
import { SimpleMaterial } from "vsdk/toolkit/environment/material/SimpleMaterial.js";
import { SimpleBody } from "vsdk/toolkit/environment/scene/SimpleBody.js";
import { SimpleScene } from "vsdk/toolkit/environment/scene/SimpleScene.js";
import type { SimpleSceneSnapshot } from "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.js";
import { RGBImageUncompressed } from "vsdk/toolkit/media/RGBImageUncompressed.js";
import {
    ParallelRaytracer,
    type ParallelRaytracerTileRequest,
    type ParallelRaytracerTileResult,
} from "vsdk/toolkit/render/raytracing/ParallelRaytracer.js";
import { ParallelRaytracerTileRenderer } from "vsdk/toolkit/render/raytracing/ParallelRaytracerTileRenderer.js";
import { SimpleRaytracer } from "vsdk/toolkit/render/raytracing/SimpleRaytracer.js";

// TypeScript counterpart of Java's `vsdk.toolkit.render.ParallelRaytracerTest`

function createBody(geometry: Geometry, position: Vector3Dd): SimpleBody {
    const body: SimpleBody = new SimpleBody();

    body.setGeometry(geometry);
    body.setPosition(position);
    body.setRotation(new Matrix4x4d());
    body.setRotationInverse(new Matrix4x4d());
    body.setMaterial(new SimpleMaterial().withDiffuse(new ColorRgb(0.8, 0.5, 0.3)));
    return body;
}

function createSnapshot(width: number, height: number): SimpleSceneSnapshot {
    const scene: SimpleScene = new SimpleScene();
    const camera: Camera = new Camera();
    const background: SimpleBackground = new SimpleBackground();

    camera.setPosition(new Vector3Dd(-5, -5, 5));
    camera.setRotation(new Matrix4x4d().eulerAnglesRotation(JavaMath.toRadians(45), JavaMath.toRadians(-35), 0));
    background.setColor(0.2, 0.3, 0.4);
    scene.addCamera(camera);
    scene.addBackground(background);
    scene.addBody(createBody(new Sphere(1.0), new Vector3Dd(0, 0, 0)));
    scene.addBody(createBody(new Box(1, 2, 1), new Vector3Dd(1.5, 0, 0)));
    scene.addLight(new PointLight(new Vector3Dd(3, -3, 4), new ColorRgb(1, 1, 1)));
    return scene.exportToSimpleSceneSnapshot(width, height);
}

/** A worker that runs in this thread: the test does not depend on a platform. */
class InProcessWorker implements WorkerExecutor<ParallelRaytracerTileRequest, ParallelRaytracerTileResult> {
    public terminated = false;
    private readonly renderer: ParallelRaytracerTileRenderer = new ParallelRaytracerTileRenderer(
        (_descriptor: WorkerTransferValue, xSize: number, ySize: number) => createSnapshot(xSize, ySize),
    );

    public execute(
        input: ParallelRaytracerTileRequest,
        options?: WorkerExecutionOptions,
    ): Promise<ParallelRaytracerTileResult> {
        return Promise.resolve(this.renderer.renderTile(input, (value) => options?.onNotice?.(value)));
    }

    public terminate(): void {
        this.terminated = true;
    }
}

describe("ParallelRaytracer", () => {
    it("given scene when raytracing in parallel then image equals serial raytracing", async () => {
        // Arrange
        const width = 97;
        const height = 61;
        const quality: RendererConfiguration = new RendererConfiguration();
        const serialImage: RGBImageUncompressed = new RGBImageUncompressed();
        const parallelImage: RGBImageUncompressed = new RGBImageUncompressed();
        const workers: InProcessWorker[] = [];
        const parallelRaytracer: ParallelRaytracer = new ParallelRaytracer(() => {
            const worker: InProcessWorker = new InProcessWorker();
            workers.push(worker);
            return worker;
        }, 4);
        serialImage.init(width, height);
        parallelImage.init(width, height);

        // Action
        new SimpleRaytracer().execute(serialImage, quality, createSnapshot(width, height), null);
        await parallelRaytracer.execute(parallelImage, quality, "test scene", false);
        // Threads are reused between images
        await parallelRaytracer.execute(parallelImage, quality, "test scene", false);
        parallelRaytracer.dispose();

        // Assert
        let differentPixels = 0;
        let objectPixels = 0;
        const backgroundPixel = serialImage.getPixel(0, 0);
        for (let y = 0; y < height; y++) {
            for (let x = 0; x < width; x++) {
                const expected = serialImage.getPixel(x, y);
                const actual = parallelImage.getPixel(x, y);
                if (expected.r !== actual.r || expected.g !== actual.g || expected.b !== actual.b) {
                    differentPixels++;
                }
                if (expected.r !== backgroundPixel.r || expected.g !== backgroundPixel.g ||
                    expected.b !== backgroundPixel.b) {
                    objectPixels++;
                }
            }
        }
        expect(objectPixels).toBeGreaterThan(100);
        expect(differentPixels).toBe(0);
        expect(workers).toHaveLength(4);
        expect(workers.every((worker) => worker.terminated)).toBe(true);
    });

    it("given default constructor when created then uses one thread per processor", () => {
        // Arrange / Action
        const raytracer: ParallelRaytracer = new ParallelRaytracer(() => new InProcessWorker());

        // Assert
        expect(raytracer.getNumberOfThreads()).toBe(ParallelRaytracer.availableProcessors());
        expect(raytracer.getNumberOfThreads()).toBe(navigator.hardwareConcurrency);
    });
});
