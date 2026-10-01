import {
  BrowserWorkerExecutor,
  ParallelRaytracer,
  encodeSimpleSceneSnapshot,
  type DepthBufferMode,
  type ParallelRaytracerTileRequest,
  type ParallelRaytracerTileResult,
  type RendererConfiguration,
  type RGBImageUncompressed,
  type SimpleSceneSnapshot,
  type ZBuffer,
} from '@vitral/base';
import type { SceneRaytracingBackend } from '../model/scene';

/**
 * The `SceneRaytracingBackend` of the page: Java's static `ParallelRaytracer`
 * of `Scene`, whose threads are Web Workers here (one per processor, capped as
 * `ShadersExample` caps them: a worker is an OS thread and a module
 * instantiation, too costly to start one per core of a large host). The
 * snapshot travels to the workers encoded with `SimpleSceneSnapshotTransfer`,
 * and `scene-raytracer-tile.worker.ts` rebuilds it.
 */
export class WebSceneRaytracingBackend implements SceneRaytracingBackend {
  private static readonly MAX_WORKERS = 8;

  private readonly raytracer: ParallelRaytracer;

  constructor() {
    this.raytracer = new ParallelRaytracer(
      () =>
        new BrowserWorkerExecutor<ParallelRaytracerTileRequest, ParallelRaytracerTileResult>(
          new Worker(new URL('./scene-raytracer-tile.worker', import.meta.url), { type: 'module' }),
        ),
      Math.min(WebSceneRaytracingBackend.MAX_WORKERS, ParallelRaytracer.availableProcessors()),
    );
  }

  async execute(
    image: RGBImageUncompressed,
    depth: ZBuffer | null,
    depthMode: DepthBufferMode,
    quality: RendererConfiguration,
    snapshot: SimpleSceneSnapshot,
    interactiveReport: boolean,
  ): Promise<void> {
    await this.raytracer.execute(image, depth, depthMode, quality, encodeSimpleSceneSnapshot(snapshot),
      interactiveReport);
  }

  /**
   * Stops the workers.
   */
  dispose(): void {
    this.raytracer.dispose();
  }
}
