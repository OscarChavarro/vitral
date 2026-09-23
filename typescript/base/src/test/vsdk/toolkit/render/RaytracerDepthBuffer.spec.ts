import { describe, expect, it } from "vitest";
import type { WorkerExecutionOptions, WorkerExecutor, WorkerTransferValue } from "java/concurrent/WorkerProtocol.js";
import { Math as JavaMath } from "java/lang/Math.js";
import { ColorRgb } from "vsdk/toolkit/common/color/ColorRgb.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Vector4Dd } from "vsdk/toolkit/common/linealAlgebra/Vector4Dd.js";
import { SimpleBackground } from "vsdk/toolkit/environment/background/SimpleBackground.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import type { CameraSnapshot } from "vsdk/toolkit/environment/camera/CameraSnapshot.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { PointLight } from "vsdk/toolkit/environment/light/PointLight.js";
import { RendererConfiguration } from "vsdk/toolkit/environment/material/RendererConfiguration.js";
import { SimpleMaterial } from "vsdk/toolkit/environment/material/SimpleMaterial.js";
import { SimpleBody } from "vsdk/toolkit/environment/scene/SimpleBody.js";
import { SimpleScene } from "vsdk/toolkit/environment/scene/SimpleScene.js";
import type { SimpleSceneSnapshot } from "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.js";
import { RGBImageUncompressed } from "vsdk/toolkit/media/RGBImageUncompressed.js";
import { ZBuffer } from "vsdk/toolkit/media/ZBuffer.js";
import { DepthBufferEncoder } from "vsdk/toolkit/render/raytracing/DepthBufferEncoder.js";
import { DepthBufferMode } from "vsdk/toolkit/render/raytracing/DepthBufferMode.js";
import {
    ParallelRaytracer,
    type ParallelRaytracerTileRequest,
    type ParallelRaytracerTileResult,
} from "vsdk/toolkit/render/raytracing/ParallelRaytracer.js";
import { ParallelRaytracerTileRenderer } from "vsdk/toolkit/render/raytracing/ParallelRaytracerTileRenderer.js";
import { SimpleRaytracer } from "vsdk/toolkit/render/raytracing/SimpleRaytracer.js";

// TypeScript counterpart of Java's `vsdk.toolkit.render.RaytracerDepthBufferTest`:
// checks the depth buffers exported by the raytracers against the OpenGL
// projection of the same camera (`Camera.calculateProjectionMatrix`).

const WIDTH = 81;
const HEIGHT = 57;

interface TestScene {
    readonly camera: Camera;
    readonly snapshot: SimpleSceneSnapshot;
}

function createScene(projectionMode: number): TestScene {
    const scene: SimpleScene = new SimpleScene();
    const camera: Camera = new Camera();
    const background: SimpleBackground = new SimpleBackground();
    const body: SimpleBody = new SimpleBody();

    camera.setPosition(new Vector3Dd(-5, -5, 5));
    camera.setRotation(new Matrix4x4d().eulerAnglesRotation(JavaMath.toRadians(45), JavaMath.toRadians(-35), 0));
    camera.setNearPlaneDistance(0.5);
    camera.setFarPlaneDistance(30);
    camera.setProjectionMode(projectionMode);
    camera.setOrthogonalZoom(0.6);
    background.setColor(0.2, 0.3, 0.4);

    body.setGeometry(new Sphere(1.0));
    body.setPosition(new Vector3Dd(0, 0, 0));
    body.setRotation(new Matrix4x4d());
    body.setRotationInverse(new Matrix4x4d());
    body.setMaterial(new SimpleMaterial().withDiffuse(new ColorRgb(0.8, 0.5, 0.3)));

    scene.addCamera(camera);
    scene.addBackground(background);
    scene.addBody(body);
    scene.addLight(new PointLight(new Vector3Dd(3, -3, 4), new ColorRgb(1, 1, 1)));
    return { camera, snapshot: scene.exportToSimpleSceneSnapshot(WIDTH, HEIGHT) };
}

function raytrace(snapshot: SimpleSceneSnapshot, mode: DepthBufferMode): ZBuffer {
    const image: RGBImageUncompressed = new RGBImageUncompressed();
    const depth: ZBuffer = new ZBuffer(WIDTH, HEIGHT);

    image.init(WIDTH, HEIGHT);
    new SimpleRaytracer().execute(image, new RendererConfiguration(), snapshot, null, depth, mode, 0, 0, WIDTH, HEIGHT);
    return depth;
}

function unproject(inverseProjection: Matrix4x4d, x: number, y: number, windowDepth: number): Vector3Dd {
    const ndcX: number = (2.0 * (x + 0.5)) / WIDTH - 1.0;
    const ndcY: number = 1.0 - (2.0 * (y + 0.5)) / HEIGHT;
    const ndcZ: number = 2.0 * windowDepth - 1.0;
    const p: Vector4Dd = inverseProjection.multiply(new Vector4Dd(ndcX, ndcY, ndcZ, 1.0)).dividedByW();
    return new Vector3Dd(p.x(), p.y(), p.z());
}

function checkOpenGlDepthMatchesProjection(projectionMode: number): void {
    // Arrange
    const { camera, snapshot } = createScene(projectionMode);
    const inverseProjection: Matrix4x4d = camera.calculateProjectionMatrix().invert();
    const cameraSnapshot: CameraSnapshot = snapshot.getCameraSnapshot();

    // Action
    const glDepth: ZBuffer = raytrace(snapshot, DepthBufferMode.OPENGL_DEPTH);
    const distance: ZBuffer = raytrace(snapshot, DepthBufferMode.RAY_DISTANCE);

    // Assert
    let hits = 0;
    let misses = 0;
    for (let y = 0; y < HEIGHT; y++) {
        for (let x = 0; x < WIDTH; x++) {
            const d: number = glDepth.getZ(x, y);
            const t: number = distance.getZ(x, y);
            if (!Number.isFinite(t)) {
                misses++;
                expect(d).toBe(1.0);
                continue;
            }
            hits++;
            expect(d).toBeGreaterThanOrEqual(0.0);
            expect(d).toBeLessThanOrEqual(1.0);
            const p: Vector3Dd = unproject(inverseProjection, x, y, d);
            // The point is on the sphere...
            expect(Math.abs(p.length() - 1.0)).toBeLessThan(2e-3);
            // ... at the ray distance (from the eye, or from the image plane
            // in orthogonal projection)
            const expectedDistance: number =
                projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL
                    ? p.subtract(cameraSnapshot.getEyePosition()).dotProduct(cameraSnapshot.getFront())
                    : p.subtract(cameraSnapshot.getEyePosition()).length();
            expect(Math.abs(t - expectedDistance)).toBeLessThan(2e-3);
        }
    }
    expect(hits).toBeGreaterThan(100);
    expect(misses).toBeGreaterThan(200);
}

/** A worker that runs in this thread: the test does not depend on a platform. */
class InProcessWorker implements WorkerExecutor<ParallelRaytracerTileRequest, ParallelRaytracerTileResult> {
    private readonly renderer: ParallelRaytracerTileRenderer = new ParallelRaytracerTileRenderer(
        (_descriptor: WorkerTransferValue, xSize: number, ySize: number) => {
            expect(xSize).toBe(WIDTH);
            expect(ySize).toBe(HEIGHT);
            return createScene(Camera.PROJECTION_MODE_PERSPECTIVE).snapshot;
        },
    );

    public execute(
        input: ParallelRaytracerTileRequest,
        options?: WorkerExecutionOptions,
    ): Promise<ParallelRaytracerTileResult> {
        return Promise.resolve(this.renderer.renderTile(input, (value) => options?.onNotice?.(value)));
    }

    public terminate(): void {}
}

describe("RaytracerDepthBuffer", () => {
    it("given perspective camera when exporting OpenGL depth then unprojects to hit surface", () => {
        checkOpenGlDepthMatchesProjection(Camera.PROJECTION_MODE_PERSPECTIVE);
    });

    it("given orthogonal camera when exporting OpenGL depth then unprojects to hit surface", () => {
        checkOpenGlDepthMatchesProjection(Camera.PROJECTION_MODE_ORTHOGONAL);
    });

    it("given depth mode NONE when raytracing then depth buffer is untouched", () => {
        // Arrange
        const { snapshot } = createScene(Camera.PROJECTION_MODE_PERSPECTIVE);

        // Action
        const depth: ZBuffer = raytrace(snapshot, DepthBufferMode.NONE);

        // Assert
        expect(Array.from(depth.getZBuffer()).every((value) => value === 0)).toBe(true);
    });

    it("given depth mode when raytracing in parallel then depth equals serial", async () => {
        // Arrange
        const { snapshot } = createScene(Camera.PROJECTION_MODE_PERSPECTIVE);
        const parallel: ParallelRaytracer = new ParallelRaytracer(() => new InProcessWorker(), 3);
        const image: RGBImageUncompressed = new RGBImageUncompressed();
        image.init(WIDTH, HEIGHT);

        // Action / Assert
        expect(parallel.getDepthBufferMode()).toBe(DepthBufferMode.NONE);
        await parallel.execute(image, new RendererConfiguration(), "test scene", false);
        expect(parallel.getDepthBuffer()).toBeNull();

        for (const mode of [DepthBufferMode.RAY_DISTANCE, DepthBufferMode.OPENGL_DEPTH]) {
            const serial: ZBuffer = raytrace(snapshot, mode);
            parallel.setDepthBufferMode(mode);
            await parallel.execute(image, new RendererConfiguration(), "test scene", false);
            const depth: ZBuffer | null = parallel.getDepthBuffer();
            expect(depth).not.toBeNull();
            expect(depth!.getXSize()).toBe(WIDTH);
            expect(depth!.getYSize()).toBe(HEIGHT);
            expect(Array.from(depth!.getZBuffer())).toEqual(Array.from(serial.getZBuffer()));
        }
        parallel.setDepthBufferMode(DepthBufferMode.NONE);
        expect(parallel.getDepthBuffer()).toBeNull();
        parallel.dispose();
    });

    it("given custom depth range when raytracing in parallel then depth is remapped", async () => {
        // Arrange
        const { snapshot } = createScene(Camera.PROJECTION_MODE_PERSPECTIVE);
        const parallel: ParallelRaytracer = new ParallelRaytracer(() => new InProcessWorker(), 2);
        const image: RGBImageUncompressed = new RGBImageUncompressed();
        const standard: ZBuffer = raytrace(snapshot, DepthBufferMode.OPENGL_DEPTH);
        image.init(WIDTH, HEIGHT);
        parallel.setDepthBufferMode(DepthBufferMode.OPENGL_DEPTH);
        parallel.setOpenGlDepthRange(0.25, 0.75);

        // Action
        await parallel.execute(image, new RendererConfiguration(), "test scene", false);

        // Assert
        const remapped: Float32Array = parallel.getDepthBuffer()!.getZBuffer();
        const expected: Float32Array = standard.getZBuffer();
        for (let i = 0; i < expected.length; i++) {
            expect(Math.abs(remapped[i]! - (0.25 + 0.5 * expected[i]!))).toBeLessThan(1e-6);
        }
        parallel.dispose();
    });

    it("given encoder when converting plane distances then matches OpenGL limits", () => {
        // Arrange
        const { camera } = createScene(Camera.PROJECTION_MODE_PERSPECTIVE);
        const perspective: CameraSnapshot = camera.exportToCameraSnapshot(WIDTH, HEIGHT);
        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        const orthogonal: CameraSnapshot = camera.exportToCameraSnapshot(WIDTH, HEIGHT);

        for (const snapshot of [perspective, orthogonal]) {
            // Action
            const encoder: DepthBufferEncoder = new DepthBufferEncoder(DepthBufferMode.OPENGL_DEPTH, snapshot);
            const identity: DepthBufferEncoder = new DepthBufferEncoder(DepthBufferMode.RAY_DISTANCE, snapshot);

            // Assert
            expect(Math.abs(encoder.eyeDepthToWindowDepth(0.5))).toBeLessThan(1e-12);
            expect(Math.abs(encoder.eyeDepthToWindowDepth(30) - 1.0)).toBeLessThan(1e-12);
            expect(encoder.eyeDepthToWindowDepth(0.1)).toBe(0.0);
            expect(encoder.eyeDepthToWindowDepth(100)).toBe(1.0);
            expect(encoder.eyeDepthToWindowDepth(5)).toBeLessThan(encoder.eyeDepthToWindowDepth(6));
            expect(encoder.encode(snapshot.getEyePosition(), snapshot.getFront(), Number.POSITIVE_INFINITY)).toBe(1.0);
            expect(identity.encode(snapshot.getEyePosition(), snapshot.getFront(), 7.25)).toBe(7.25);
        }
    });
});
