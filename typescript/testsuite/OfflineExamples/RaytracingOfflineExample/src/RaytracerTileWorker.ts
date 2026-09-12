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
import { ProgressMonitor } from "@vitral/base/vsdk/toolkit/gui/feedback/ProgressMonitor";
import { RGBImageUncompressed } from "@vitral/base/vsdk/toolkit/media/RGBImageUncompressed";
import { RendererConfiguration } from "@vitral/base/vsdk/toolkit/environment/material/RendererConfiguration";
import { SimpleRaytracer } from "@vitral/base/vsdk/toolkit/render/raytracing/SimpleRaytracer";
import { SimpleScene } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleScene";
import type { CameraSnapshot } from "@vitral/base/vsdk/toolkit/environment/camera/CameraSnapshot";
import type { SimpleSceneSnapshot } from "@vitral/base/vsdk/toolkit/environment/scene/SimpleSceneSnapshot";
import type { WorkerTransferValue } from "@vitral/base/java/concurrent/WorkerProtocol";
// The barrel installs these `RendererConfiguration` methods as a side effect;
// a worker that does not load the barrel must ask for them explicitly.
import "@vitral/base/vsdk/toolkit/environment/material/RendererConfigurationBehavior";

/**
Worker-side half of `RaytracerParallelExecutor`.

Java's `RaytracerParallelExecutor.TileWorker` is a `Callable<Void>` running on a
thread of the same JVM: it shares the scene snapshot and writes its pixels
straight into the one `RGBImageUncompressed` every thread sees. A Node worker
has its own heap and shares nothing, so this module instead loads the scene
itself once per worker, renders each tile into its own copy of the image, and
returns just the bytes of the rendered band; the owner splices them into the
result. The rendered pixels are identical either way, because a tile only ever
reads the scene and only ever writes its own rows.
*/
interface TileRequest {
    readonly sceneFile: string;
    readonly texture: boolean;
    readonly bumpMap: boolean;
    readonly x0: number;
    readonly y0: number;
    readonly dx: number;
    readonly dy: number;
    readonly [key: string]: WorkerTransferValue;
}

interface TileResult {
    readonly y0: number;
    readonly dy: number;
    readonly bytes: Uint8Array;
    readonly [key: string]: WorkerTransferValue;
}

/** Forwards every `update` of the raytracer as one progress notice. */
class NotifyingProgressMonitor extends ProgressMonitor {
    public constructor(private readonly notify: (value: WorkerTransferValue) => void) {
        super();
    }
    public begin(): void {}
    public end(): void {}
    public update(_minValue: number, _maxValue: number, _currentValue: number): void {
        this.notify(1);
    }
    public getCurrentPercent(): number {
        return 0;
    }
}

interface LoadedScene {
    readonly sceneFile: string;
    readonly image: RGBImageUncompressed;
    readonly configuration: RendererConfiguration;
    readonly snapshot: SimpleSceneSnapshot;
    readonly raytracer: SimpleRaytracer;
}

let loaded: LoadedScene | undefined;

function loadScene(request: TileRequest): LoadedScene {
    if (loaded !== undefined && loaded.sceneFile === request.sceneFile) {
        return loaded;
    }

    const scene: SimpleScene = new SimpleScene();
    const input: FileInputStream = new FileInputStream(request.sceneFile);
    try {
        new ReaderMitScene().importEnvironment(input, scene);
    } finally {
        input.close();
    }

    const camera = scene.getActiveCamera();
    const image: RGBImageUncompressed = new RGBImageUncompressed();
    image.initNoFill(Math.trunc(camera.getViewportXSize()), Math.trunc(camera.getViewportYSize()));

    const configuration: RendererConfiguration = new RendererConfiguration();
    configuration.setTexture(request.texture);
    configuration.setBumpMap(request.bumpMap);

    const cameraSnapshot: CameraSnapshot = camera.exportToCameraSnapshot(image.getXSize(), image.getYSize());
    const snapshot: SimpleSceneSnapshot = scene.exportToSimpleSceneSnapshot(
        cameraSnapshot,
        scene.getActiveBackground(),
    );

    loaded = {
        sceneFile: request.sceneFile,
        image,
        configuration,
        snapshot,
        raytracer: new SimpleRaytracer(),
    };
    return loaded;
}

installNodeWorkerRuntime<TileRequest, TileResult>((request, _signal, notify) => {
    const context: LoadedScene = loadScene(request);
    const monitor: NotifyingProgressMonitor = new NotifyingProgressMonitor(notify);

    context.raytracer.execute(
        context.image,
        context.configuration,
        context.snapshot,
        monitor,
        null,
        request.x0,
        request.y0,
        request.x0 + request.dx,
        request.y0 + request.dy,
    );

    // `RGBImageUncompressed` stores row `y` at `(ySize - 1 - y) * rowStride`,
    // so a full-width band is one contiguous byte range. `RasterTileGenerator`
    // in LINEAR mode only ever produces full-width bands, which the owner
    // checks before handing a tile over.
    const ySize: number = context.image.getYSize();
    const rowStride: number = context.image.getXSize() * 3;
    const raw: Uint8Array = context.image.getRawImageDirectBuffer();
    const start: number = (ySize - (request.y0 + request.dy)) * rowStride;
    const end: number = (ySize - request.y0) * rowStride;

    return { y0: request.y0, dy: request.dy, bytes: raw.slice(start, end) };
});
