import { IllegalArgumentException } from "../../../../java/lang/IllegalArgumentException.js";
import { IllegalStateException } from "../../../../java/lang/IllegalStateException.js";
import { platformPrintln } from "../../../../java/lang/_PlatformConsole.js";
import type { WorkerExecutor, WorkerTransferValue } from "../../../../java/concurrent/WorkerProtocol.js";
import type { ArrayList } from "../../../../java/util/ArrayList.js";
import { ConcurrentLinkedQueue } from "../../../../java/util/concurrent/ConcurrentLinkedQueue.js";
import type { RendererConfiguration } from "../../environment/material/RendererConfiguration.js";
import { ParallelProgressMonitorConsumer } from "../../gui/feedback/parallel/ParallelProgressMonitorConsumer.js";
import { ParallelProgressMonitorEvent } from "../../gui/feedback/parallel/ParallelProgressMonitorEvent.js";
import { ParallelProgressMonitorProducer } from "../../gui/feedback/parallel/ParallelProgressMonitorProducer.js";
import type { RGBImageUncompressed } from "../../media/RGBImageUncompressed.js";
import type { RasterTileArea } from "./RasterTileArea.js";
import { RasterTileGenerationStrategy } from "./RasterTileGenerationStrategy.js";
import { RasterTileGenerator } from "./RasterTileGenerator.js";

/**
One band of the image, as the owner asks a worker to render it (see
`ParallelRaytracerTileRenderer`).
*/
export interface ParallelRaytracerTileRequest {
    /** Whatever the worker needs to rebuild the scene (i.e. a scene file name) */
    readonly sceneDescriptor: WorkerTransferValue;
    /** Changes on every `execute`: a worker rebuilds the scene when it changes */
    readonly sceneVersion: number;
    /** False to render without sending progress notices */
    readonly reportProgress: boolean;
    readonly xSize: number;
    readonly ySize: number;
    /** The only `RendererConfiguration` bits `SimpleRaytracer` reads */
    readonly shadingType: number;
    readonly texture: boolean;
    readonly bumpMap: boolean;
    readonly x0: number;
    readonly y0: number;
    readonly dx: number;
    readonly dy: number;
    readonly [key: string]: WorkerTransferValue;
}

/** The pixels of one rendered band, returned by a worker. */
export interface ParallelRaytracerTileResult {
    readonly y0: number;
    readonly dy: number;
    readonly bytes: Uint8Array;
    readonly [key: string]: WorkerTransferValue;
}

/** Creates one worker; the platform decides how (Web Worker, Node worker...). */
export type ParallelRaytracerWorkerFactory = () => WorkerExecutor<
    ParallelRaytracerTileRequest,
    ParallelRaytracerTileResult
>;

/**
TypeScript counterpart of Java's `vsdk.toolkit.render.raytracing.ParallelRaytracer`.

As in Java, it raytraces an image with as many threads as processors are
available: the image is split in horizontal bands (see `RasterTileGenerator`),
several per thread, and each thread takes the next pending band when it
finishes one, so threads stay busy even if some areas of the image are much
more expensive than others. The threads are created once and reused by every
call, so it suits interactive use; `dispose` stops them.

Differences forced by the platform, where threads are workers without a
shared heap and without a blocking join:
  - The workers come from a `ParallelRaytracerWorkerFactory`, since
    `@vitral/base` does not know which platform it runs on. Each worker module
    installs a `ParallelRaytracerTileRenderer`.
  - The scene snapshot can not be shared, so `execute` receives a
    `sceneDescriptor` from which each worker rebuilds the scene once per
    `execute` (and reuses it for all its bands) instead of the snapshot
    itself.
  - A worker can not poll the owner's queue nor write into the owner's image:
    the owner polls for it, and splices the bytes of each band the worker
    returns. Bands never overlap, so the image is the one Java builds in place.
  - `execute` answers a promise where Java blocks.
*/
export class ParallelRaytracer {
    /** Bands per thread: more bands balance the load better */
    private static readonly BANDS_PER_THREAD = 8;

    private readonly numberOfThreads: number;
    private workers: WorkerExecutor<ParallelRaytracerTileRequest, ParallelRaytracerTileResult>[] | null = null;
    private sceneVersion = 0;

    /**
    @param workerFactory creates each of the workers
    @param numberOfThreads number of workers to render with (at least 1); by
    default, one per available processor
    */
    public constructor(
        private readonly workerFactory: ParallelRaytracerWorkerFactory,
        numberOfThreads: number = ParallelRaytracer.availableProcessors(),
    ) {
        if (!Number.isInteger(numberOfThreads) || numberOfThreads <= 0) {
            throw new IllegalArgumentException("numberOfThreads must be > 0");
        }
        this.numberOfThreads = numberOfThreads;
    }

    /**
    @return the number of processors the platform reports (`hardwareConcurrency`,
    available both in browsers and in Node), or 1 if it reports none
    */
    public static availableProcessors(): number {
        const count: number | undefined =
            typeof navigator !== "undefined" ? navigator.hardwareConcurrency : undefined;
        return count !== undefined && count > 0 ? count : 1;
    }

    /** @return number of workers used to render */
    public getNumberOfThreads(): number {
        return this.numberOfThreads;
    }

    private getWorkers(): WorkerExecutor<ParallelRaytracerTileRequest, ParallelRaytracerTileResult>[] {
        if (this.workers === null) {
            this.workers = [];
            for (let i = 0; i < this.numberOfThreads; i++) {
                this.workers.push(this.workerFactory());
            }
        }
        return this.workers;
    }

    /**
    Raytraces the scene into the whole image, resolving when it is complete.
    @param resultingImage image to fill; its size gives the resolution
    @param rendererConfiguration quality settings
    @param sceneDescriptor what each worker needs to rebuild the scene
    @param reportProgress true to report in the console the number of
    threads and the progress
    */
    public async execute(
        resultingImage: RGBImageUncompressed,
        rendererConfiguration: RendererConfiguration,
        sceneDescriptor: WorkerTransferValue,
        reportProgress: boolean,
    ): Promise<void> {
        if (resultingImage.getXSize() <= 0 || resultingImage.getYSize() <= 0) {
            return;
        }
        const tileGenerator: RasterTileGenerator = new RasterTileGenerator(
            RasterTileGenerationStrategy.LINEAR,
            resultingImage,
            resultingImage.getXSize(),
            resultingImage.getYSize(),
            this.numberOfThreads * ParallelRaytracer.BANDS_PER_THREAD,
        );
        const generatedTiles: ArrayList<RasterTileArea> = tileGenerator.getTiles();
        const pendingTiles: ConcurrentLinkedQueue<RasterTileArea> = new ConcurrentLinkedQueue<RasterTileArea>(
            generatedTiles,
        );

        let producer: ParallelProgressMonitorProducer | null = null;
        let consumerThread: Promise<void> | null = null;
        // The scene may have changed since the previous image
        this.sceneVersion++;

        if (reportProgress) {
            platformPrintln("Starting parallel raytracing with " + this.numberOfThreads + " threads.");
            const progressEvents: ConcurrentLinkedQueue<ParallelProgressMonitorEvent> =
                new ConcurrentLinkedQueue<ParallelProgressMonitorEvent>();
            producer = new ParallelProgressMonitorProducer(progressEvents);
            producer.init(ParallelRaytracer.calculateTotalProgressElements(generatedTiles));
            consumerThread = new ParallelProgressMonitorConsumer(progressEvents).run();
        }

        try {
            await Promise.all(
                this.getWorkers().map((worker) =>
                    this.drainTiles(
                        worker,
                        pendingTiles,
                        resultingImage,
                        rendererConfiguration,
                        sceneDescriptor,
                        producer,
                    ),
                ),
            );
            if (!pendingTiles.isEmpty()) {
                throw new IllegalStateException("Parallel raytracing finished with pending tiles");
            }
        } finally {
            if (producer !== null && consumerThread !== null) {
                producer.finish();
                await consumerThread;
            }
        }
    }

    /** Stops the workers. The next `execute` creates them again. */
    public dispose(): void {
        if (this.workers !== null) {
            for (const worker of this.workers) {
                worker.terminate();
            }
            this.workers = null;
        }
    }

    private static calculateTotalProgressElements(generatedTiles: ArrayList<RasterTileArea>): bigint {
        let totalElements = 0n;

        for (let i = 0; i < generatedTiles.size(); i++) {
            totalElements += BigInt(generatedTiles.get(i).getDy());
        }
        return totalElements;
    }

    /**
    Counterpart of Java's `TileWorker.call()`: keep taking bands off the shared
    queue until it runs dry. The polling happens here rather than inside the
    worker because the queue lives in this heap.
    */
    private async drainTiles(
        worker: WorkerExecutor<ParallelRaytracerTileRequest, ParallelRaytracerTileResult>,
        pendingTiles: ConcurrentLinkedQueue<RasterTileArea>,
        resultingImage: RGBImageUncompressed,
        rendererConfiguration: RendererConfiguration,
        sceneDescriptor: WorkerTransferValue,
        progressReporter: ParallelProgressMonitorProducer | null,
    ): Promise<void> {
        let tile: RasterTileArea | undefined;

        while ((tile = pendingTiles.poll()) !== undefined) {
            if (tile.getX0() !== 0 || tile.getDx() !== resultingImage.getXSize()) {
                throw new IllegalStateException(
                    "The worker band transfer only supports full-width tiles, which is what " +
                        "RasterTileGenerationStrategy.LINEAR produces",
                );
            }
            const result: ParallelRaytracerTileResult = await worker.execute(
                {
                    sceneDescriptor,
                    sceneVersion: this.sceneVersion,
                    reportProgress: progressReporter !== null,
                    xSize: resultingImage.getXSize(),
                    ySize: resultingImage.getYSize(),
                    shadingType: rendererConfiguration.getShadingType(),
                    texture: rendererConfiguration.isTextureSet(),
                    bumpMap: rendererConfiguration.isBumpMapSet(),
                    x0: tile.getX0(),
                    y0: tile.getY0(),
                    dx: tile.getDx(),
                    dy: tile.getDy(),
                },
                progressReporter === null ? undefined : { onNotice: () => progressReporter.update(0, 0, 0) },
            );
            ParallelRaytracer.spliceBand(resultingImage, result);
        }
    }

    /** Copies a rendered band back into the shared result image. */
    private static spliceBand(resultingImage: RGBImageUncompressed, result: ParallelRaytracerTileResult): void {
        const ySize: number = resultingImage.getYSize();
        const rowStride: number = resultingImage.getXSize() * 3;
        const raw: Uint8Array = resultingImage.getRawImageDirectBuffer();
        const start: number = (ySize - (result.y0 + result.dy)) * rowStride;
        raw.set(result.bytes, start);
    }
}
