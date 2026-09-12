import { availableParallelism } from "node:os";

import {
    ConcurrentLinkedQueue,
    IllegalStateException,
    ParallelProgressMonitorConsumer,
    ParallelProgressMonitorEvent,
    ParallelProgressMonitorProducer,
    RasterTileArea,
    RasterTileGenerationStrategy,
    RasterTileGenerator,
    type ArrayList,
    type ProgressMonitor,
    type RGBImageUncompressed,
    type RendererConfiguration,
    type SimpleRaytracer,
    type SimpleSceneSnapshot,
    type WorkerTransferValue,
} from "@vitral/base";
import { NodeWorkerExecutor } from "@vitral/fs";

import type { RaytracerExecutor } from "./RaytracerExecutor.js";

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

/**
Node counterpart of Java's `RaytracerParallelExecutor`.

The structure is the Java one: `RasterTileGenerator` in LINEAR mode produces
one band per available processor, the bands go into a
`ConcurrentLinkedQueue`, as many workers as processors drain that queue, and a
`ParallelProgressMonitorProducer` / `ParallelProgressMonitorConsumer` pair
turns the workers' per-row events into a single console bar.

Two runtime boundaries, both forced by `node:worker_threads` having no shared
heap and no blocking join:
  - A worker cannot poll the shared queue itself, because the queue lives in
    the owner's heap. The owner polls instead and hands each worker its next
    tile, which preserves the same work-stealing order.
  - A worker cannot write into the one result image. It returns the bytes of
    the band it rendered and the owner splices them in. Tiles never overlap,
    so the resulting image is the same one the JVM threads would have built
    in place.

The scene itself is not sent across: every worker reads the same scene file,
which is both cheaper than cloning the object graph and bit-for-bit
deterministic.

Because a worker call is a promise, `run` is `async` and `RaytracerSimple`
awaits it where Java would block on `Future.get()`.
*/
export class RaytracerParallelExecutor implements RaytracerExecutor {
    /**
    The scene file and shading flags the workers must reproduce. Java has no
    counterpart: its workers share the already-parsed snapshot.
    */
    public constructor(
        private readonly sceneFile: string,
        private readonly workerModuleUrl: URL,
    ) {}

    public async run(
        _visualizationEngine: SimpleRaytracer,
        resultingImage: RGBImageUncompressed,
        rendererConfiguration: RendererConfiguration,
        _sceneSnapshot: SimpleSceneSnapshot,
        _reporter: ProgressMonitor,
    ): Promise<void> {
        const numberOfThreads: number = availableParallelism();
        const tileGenerator: RasterTileGenerator = new RasterTileGenerator(
            RasterTileGenerationStrategy.LINEAR,
            resultingImage,
            resultingImage.getXSize(),
            resultingImage.getYSize(),
            numberOfThreads,
        );
        console.log("Starting parallel raytracing with " + numberOfThreads + " threads.");
        const generatedTiles: ArrayList<RasterTileArea> = tileGenerator.getTiles();
        const pendingTiles: ConcurrentLinkedQueue<RasterTileArea> = new ConcurrentLinkedQueue<RasterTileArea>(
            generatedTiles,
        );
        const executors: NodeWorkerExecutor<TileRequest, TileResult>[] = [];
        for (let i = 0; i < numberOfThreads; i++) {
            executors.push(new NodeWorkerExecutor<TileRequest, TileResult>(this.workerModuleUrl));
        }
        const progressEvents: ConcurrentLinkedQueue<ParallelProgressMonitorEvent> =
            new ConcurrentLinkedQueue<ParallelProgressMonitorEvent>();
        const producer: ParallelProgressMonitorProducer = new ParallelProgressMonitorProducer(progressEvents);
        const consumer: ParallelProgressMonitorConsumer = new ParallelProgressMonitorConsumer(progressEvents);
        producer.init(this.calculateTotalProgressElements(generatedTiles));
        const consumerThread: Promise<void> = consumer.run();

        try {
            await Promise.all(
                executors.map((executor) =>
                    this.drainTiles(executor, pendingTiles, resultingImage, rendererConfiguration, producer),
                ),
            );
            if (!pendingTiles.isEmpty()) {
                throw new IllegalStateException("Parallel raytracing finished with pending tiles");
            }
        } finally {
            producer.finish();
            for (const executor of executors) {
                executor.terminate();
            }
            await consumerThread;
        }
    }

    private calculateTotalProgressElements(generatedTiles: ArrayList<RasterTileArea>): bigint {
        let totalElements = 0n;

        for (let i = 0; i < generatedTiles.size(); i++) {
            totalElements += BigInt(generatedTiles.get(i).getDy());
        }
        return totalElements;
    }

    /**
    Counterpart of Java's `TileWorker.call()`: keep taking tiles off the shared
    queue until it runs dry. The polling happens here rather than inside the
    worker because the queue lives in this heap.
    */
    private async drainTiles(
        executor: NodeWorkerExecutor<TileRequest, TileResult>,
        pendingTiles: ConcurrentLinkedQueue<RasterTileArea>,
        resultingImage: RGBImageUncompressed,
        rendererConfiguration: RendererConfiguration,
        progressReporter: ParallelProgressMonitorProducer,
    ): Promise<void> {
        let tile: RasterTileArea | undefined;

        while ((tile = pendingTiles.poll()) !== undefined) {
            if (tile.getX0() !== 0 || tile.getDx() !== resultingImage.getXSize()) {
                throw new IllegalStateException(
                    "The worker band transfer only supports full-width tiles, which is what " +
                        "RasterTileGenerationStrategy.LINEAR produces",
                );
            }
            const result: TileResult = await executor.execute(
                {
                    sceneFile: this.sceneFile,
                    texture: rendererConfiguration.isTextureSet(),
                    bumpMap: rendererConfiguration.isBumpMapSet(),
                    x0: tile.getX0(),
                    y0: tile.getY0(),
                    dx: tile.getDx(),
                    dy: tile.getDy(),
                },
                { onNotice: () => progressReporter.update(0, 0, 0) },
            );
            RaytracerParallelExecutor.spliceBand(resultingImage, result);
        }
    }

    /** Copies a rendered band back into the shared result image. */
    private static spliceBand(resultingImage: RGBImageUncompressed, result: TileResult): void {
        const ySize: number = resultingImage.getYSize();
        const rowStride: number = resultingImage.getXSize() * 3;
        const raw: Uint8Array = resultingImage.getRawImageDirectBuffer();
        const start: number = (ySize - (result.y0 + result.dy)) * rowStride;
        raw.set(result.bytes, start);
    }
}
