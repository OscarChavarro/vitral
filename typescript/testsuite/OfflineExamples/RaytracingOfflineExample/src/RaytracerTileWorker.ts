/*
Deep module specifiers rather than the `@vitral/base` / `@vitral/fs` barrels.
This is a worker entry module: with the barrels, booting one worker per core on
a 72-core host took 19.7 s of module compilation, against 1.3 s through these
specifiers. Every module the worker reaches transitively is written the same
way, for the same reason.
*/
import { installNodeWorkerRuntime } from "@vitral/fs/java/concurrent/NodeWorkerRuntime";
import { FileInputStream } from "@vitral/fs/java/io/FileInputStream";
import { ReaderMitScene } from "@vitral/fs/vsdk/toolkit/io/geometry/ReaderMitScene";
import { SimpleScene } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleScene";
import { ParallelRaytracerTileRenderer } from "@vitral/base/vsdk/toolkit/render/raytracing/ParallelRaytracerTileRenderer";
import type { SimpleSceneSnapshot } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleSceneSnapshot";
import type { WorkerTransferValue } from "@vitral/base/java/concurrent/WorkerProtocol";
import type {
    ParallelRaytracerTileRequest,
    ParallelRaytracerTileResult,
} from "@vitral/base/vsdk/toolkit/render/raytracing/ParallelRaytracer";
// The barrel installs these `RendererConfiguration` methods as a side effect;
// a worker that does not load the barrel must ask for them explicitly.
import "@vitral/base/vsdk/toolkit/environment/material/RendererConfigurationBehavior";

/**
Worker entry module of the `ParallelRaytracer` used by `RaytracerSimple`.

Java's workers are threads that share the already-parsed scene snapshot. A
Node worker has its own heap, so the scene descriptor `RaytracerSimple` sends
is the scene file name, and each worker reads that file once (the
`ParallelRaytracerTileRenderer` caches it), which is both cheaper than cloning
the object graph and bit-for-bit deterministic.
*/
function loadScene(sceneDescriptor: WorkerTransferValue, xSize: number, ySize: number): SimpleSceneSnapshot {
    const scene: SimpleScene = new SimpleScene();
    const input: FileInputStream = new FileInputStream(String(sceneDescriptor));
    try {
        new ReaderMitScene().importEnvironment(input, scene);
    } finally {
        input.close();
    }

    const camera = scene.getActiveCamera();
    return scene.exportToSimpleSceneSnapshot(
        camera.exportToCameraSnapshot(xSize, ySize),
        scene.getActiveBackground(),
    );
}

const renderer: ParallelRaytracerTileRenderer = new ParallelRaytracerTileRenderer(loadScene);

installNodeWorkerRuntime<ParallelRaytracerTileRequest, ParallelRaytracerTileResult>((request, _signal, notify) =>
    renderer.renderTile(request, notify),
);
