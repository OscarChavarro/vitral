import type { WorkerTransferValue } from "../../../../java/concurrent/WorkerProtocol.js";
import { RendererConfiguration } from "../../environment/material/RendererConfiguration.js";
import type { SimpleSceneSnapshot } from "../../environment/scene/SimpleSceneSnapshot.js";
import { ProgressMonitor } from "../../gui/feedback/ProgressMonitor.js";
import { RGBImageUncompressed } from "../../media/RGBImageUncompressed.js";
import type { ParallelRaytracerTileRequest, ParallelRaytracerTileResult } from "./ParallelRaytracer.js";
import { SimpleRaytracer } from "./SimpleRaytracer.js";

/**
Rebuilds, inside a worker, the scene named by a scene descriptor, seen from a
camera exported for an image of the given size.
*/
export type ParallelRaytracerSceneLoader = (
    sceneDescriptor: WorkerTransferValue,
    xSize: number,
    ySize: number,
) => SimpleSceneSnapshot;

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

/**
Worker-side half of `ParallelRaytracer`. Java has no counterpart: its
`TileWorker` is a `Callable` on a thread of the same JVM that shares the scene
snapshot and writes straight into the one result image. A worker shares
nothing, so this renders each band into its own copy of the image and returns
just the bytes of that band; the owner splices them into the result. The
pixels are identical either way, because a band only reads the scene and only
writes its own rows.

The scene is rebuilt (through the loader) once per `ParallelRaytracer.execute`
call, and reused for every band of that call.

A worker module installs it with the runtime of its platform, i.e. in Node:
`installNodeWorkerRuntime((request, _signal, notify) => renderer.renderTile(request, notify))`.
Worker modules should import it by its deep specifier, not the barrel, to keep
their start-up fast.
*/
export class ParallelRaytracerTileRenderer {
    private sceneVersion: number | null = null;
    private snapshot: SimpleSceneSnapshot | null = null;
    private image: RGBImageUncompressed | null = null;
    private readonly raytracer: SimpleRaytracer = new SimpleRaytracer();

    /**
    @param loadScene rebuilds the scene of a descriptor
    */
    public constructor(private readonly loadScene: ParallelRaytracerSceneLoader) {}

    /**
    Renders one band.
    @param request the band and the scene to render
    @param notify sends a progress notice to the owner
    @return the bytes of the rendered band
    */
    public renderTile(
        request: ParallelRaytracerTileRequest,
        notify: (value: WorkerTransferValue) => void,
    ): ParallelRaytracerTileResult {
        if (this.sceneVersion !== request.sceneVersion || this.snapshot === null) {
            this.snapshot = this.loadScene(request.sceneDescriptor, request.xSize, request.ySize);
            this.sceneVersion = request.sceneVersion;
        }
        if (this.image === null || this.image.getXSize() !== request.xSize ||
            this.image.getYSize() !== request.ySize) {
            this.image = new RGBImageUncompressed();
            this.image.initNoFill(request.xSize, request.ySize);
        }

        const configuration: RendererConfiguration = new RendererConfiguration();
        configuration.setShadingType(request.shadingType);
        configuration.setTexture(request.texture);
        configuration.setBumpMap(request.bumpMap);

        this.raytracer.execute(
            this.image,
            configuration,
            this.snapshot,
            request.reportProgress ? new NotifyingProgressMonitor(notify) : null,
            null,
            request.x0,
            request.y0,
            request.x0 + request.dx,
            request.y0 + request.dy,
        );

        // `RGBImageUncompressed` stores row `y` at `(ySize - 1 - y) * rowStride`,
        // so a full-width band is one contiguous byte range. `RasterTileGenerator`
        // in LINEAR mode only ever produces full-width bands, which the owner
        // checks before handing a band over.
        const rowStride: number = request.xSize * 3;
        const raw: Uint8Array = this.image.getRawImageDirectBuffer();
        const start: number = (request.ySize - (request.y0 + request.dy)) * rowStride;
        const end: number = (request.ySize - request.y0) * rowStride;

        return { y0: request.y0, dy: request.dy, bytes: raw.slice(start, end) };
    }
}
