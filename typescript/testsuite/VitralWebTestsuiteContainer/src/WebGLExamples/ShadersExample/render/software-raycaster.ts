import {
  BrowserWorkerExecutor,
  MicroFacetedMaterial,
  RasterTileGenerationStrategy,
  RasterTileGenerator,
  RendererConfiguration,
  type Camera,
  type Matrix4x4d,
  type RasterTileArea,
  type RGBImageUncompressed,
} from '@vitral/base';
import type { ShadersModel } from '../model/shaders-model';
import type { ShadersTileRequest, ShadersTileResult } from './software-raycaster-protocol';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/ShadersExample/src/render/SoftwareRaycaster.java`.
 *
 * The CPU path: one `RasterTileGenerator` in `LINEAR` mode splits the frame
 * into as many bands as there are workers, and every worker takes the next
 * band off a shared pending queue until the queue is empty. The tiles, the
 * strategy, the worker count taken from the host's processor count, and the
 * scene snapshot each tile is traced against are Java's.
 *
 * Java runs those workers as JVM threads over one shared
 * `RGBImageUncompressed`; a browser has no threads, and its Web Workers share
 * no heap. Two things follow, and only two. The workers are
 * `BrowserWorkerExecutor`s, each handed the next tile by this owner rather
 * than polling a `ConcurrentLinkedQueue` itself — the queue is the same, the
 * polling has simply moved to the side that can see all the workers. And each
 * one returns the bytes of its band instead of writing into the shared image,
 * which this class splices into the frame. The pixels are identical, because
 * a tile only ever reads the scene and only ever writes its own rows; that is
 * the same property that lets Java's threads share the image.
 *
 * `invalidateSnapshot()` keeps Java's comment and its emptiness: the snapshot
 * is rebuilt on every render, so there is nothing to invalidate.
 *
 * Java's `loadBumpNormalMap` reads the bump map from disk in the constructor.
 * Here `io.ShadersReader` has already read it, so what this class holds is the
 * bytes it sends to the workers, and the `NormalMap` is built on their side
 * from the very same file with the same unit scale.
 *
 * Java's `render` returns when every tile is done, because `Future.get()`
 * blocks. Nothing blocks in a page, so this one is asynchronous; the component
 * treats a frame as finished when it resolves.
 */
export class SoftwareRaycaster {
  private static readonly MAX_WORKERS = 8;

  private readonly numberOfThreads: number;
  private readonly workers: BrowserWorkerExecutor<ShadersTileRequest, ShadersTileResult>[] = [];
  private generation = 0;
  private resources: {
    textureWidth: number;
    textureHeight: number;
    textureBytes: Uint8Array;
    bumpWidth: number;
    bumpHeight: number;
    bumpBytes: Uint8Array;
    microFacetCsvText: string;
    microFacetCsvName: string;
  } | null = null;

  constructor() {
    // Java's `Runtime.getRuntime().availableProcessors()`, capped.
    //
    // A JVM thread is nearly free to start, so Java takes the processor count
    // as it comes; a Web Worker is an OS thread *and* a module instantiation,
    // and the Node tile worker of `RaytracingOfflineExample` measured what one
    // per core costs on a large host — 19.7 s of module compilation on 72
    // cores. That is a start-up cost an interactive `[.]` cannot pay, so the
    // pool is capped here. No pixel depends on the number: it decides only how
    // `RasterTileGenerator` bands the frame, and the bands partition the same
    // work whatever their count.
    this.numberOfThreads = Math.min(
      SoftwareRaycaster.MAX_WORKERS,
      Math.max(1, navigator.hardwareConcurrency || 1),
    );
  }

  /**
   * What Java's constructor reads from disk, handed over instead. The texture
   * and the bump map are copied once and travel to every worker; bumping the
   * generation is what tells a worker its cached copies are stale.
   */
  setResources(model: ShadersModel, microFacetCsvText: string, microFacetCsvName: string): void {
    const textureMap: RGBImageUncompressed | null = model.getTextureMap();
    const bumpNormalMap = model.getBumpNormalMap();
    if (textureMap === null || bumpNormalMap === null) {
      return;
    }
    // The bump map itself, not the normal field: the worker runs Java's own
    // `NormalMap.importBumpMap` on it, so both sides build the same object.
    const bumpImage = model.getBumpMapFile();
    if (bumpImage === null) {
      return;
    }
    const bumpRaw: Int8Array = bumpImage.getRawImage();

    this.generation++;
    this.resources = {
      textureWidth: textureMap.getXSize(),
      textureHeight: textureMap.getYSize(),
      textureBytes: new Uint8Array(textureMap.getRawImageDirectBuffer()),
      bumpWidth: bumpImage.getXSize(),
      bumpHeight: bumpImage.getYSize(),
      bumpBytes: new Uint8Array(bumpRaw.buffer, bumpRaw.byteOffset, bumpRaw.length).slice(),
      microFacetCsvText,
      microFacetCsvName,
    };
  }

  invalidateSnapshot(): void {
    // No-op by design: the snapshot is rebuilt on every software render
    // to keep camera/light/object transforms fully synchronized with the
    // interactive OpenGL path.
  }

  async render(
    model: ShadersModel,
    activeCamera: Camera,
    modelRotation: Matrix4x4d,
  ): Promise<void> {
    const outputImage: RGBImageUncompressed | null = model.getSoftwareFrameImage();
    if (outputImage === null || this.resources === null) {
      return;
    }

    const width: number = outputImage.getXSize();
    const height: number = outputImage.getYSize();
    const tileGenerator = new RasterTileGenerator(
      RasterTileGenerationStrategy.LINEAR,
      outputImage,
      width,
      height,
      this.numberOfThreads,
    );
    const pendingTiles: RasterTileArea[] = [...tileGenerator.getTiles()];

    this.ensureWorkers();

    const baseRequest = this.buildBaseRequest(model, activeCamera, modelRotation, width, height);
    const raw: Uint8Array = outputImage.getRawImageDirectBuffer();
    const rowStride: number = width * 3;

    // Java's `for (i = 0; i < numberOfThreads; i++) executorService.submit(...)`
    // followed by `future.get()` on each: one runner per worker, each draining
    // the shared queue, and the render is over when they all are.
    const runners: Promise<void>[] = [];
    let workerIndex: number;
    for (workerIndex = 0; workerIndex < this.workers.length; workerIndex++) {
      const worker = this.workers[workerIndex]!;
      runners.push(
        (async (): Promise<void> => {
          for (;;) {
            const tile: RasterTileArea | undefined = pendingTiles.shift();
            if (tile === undefined) {
              return;
            }
            const result: ShadersTileResult = await worker.execute({
              ...baseRequest,
              x0: tile.getX0(),
              y0: tile.getY0(),
              dx: tile.getDx(),
              dy: tile.getDy(),
            });
            raw.set(result.bytes, (height - (result.y0 + result.dy)) * rowStride);
          }
        })(),
      );
    }

    await Promise.all(runners);
  }

  /**
   * Java has no counterpart: its thread pool is created and shut down inside
   * every `render` call, while a Web Worker costs a module compilation to
   * start, so the pool here outlives a frame and is torn down with the module.
   */
  dispose(): void {
    for (const worker of this.workers) {
      worker.terminate();
    }
    this.workers.length = 0;
  }

  private ensureWorkers(): void {
    while (this.workers.length < this.numberOfThreads) {
      const worker = new Worker(new URL('./raytracer-tile.worker', import.meta.url), {
        type: 'module',
      });
      this.workers.push(new BrowserWorkerExecutor<ShadersTileRequest, ShadersTileResult>(worker));
    }
  }

  private buildBaseRequest(
    model: ShadersModel,
    activeCamera: Camera,
    modelRotation: Matrix4x4d,
    width: number,
    height: number,
  ): ShadersTileRequest {
    const resources = this.resources!;
    const quality: RendererConfiguration = model.getQuality();
    const activeMaterial = model.getActiveMaterialForCurrentShading();
    const lightPosition = model.getLight().getPosition();
    const lightColor = model.getLight().getEmission();

    return {
      generation: this.generation,
      width,
      height,
      x0: 0,
      y0: 0,
      dx: 0,
      dy: 0,
      cameraPosition: Float64Array.from([
        activeCamera.getPosition().x(),
        activeCamera.getPosition().y(),
        activeCamera.getPosition().z(),
      ]),
      cameraRotation: activeCamera.getRotation().exportToDoubleArrayRowOrder(),
      cameraFov: activeCamera.getFov(),
      sphereRadius: model.getSphere().getRadius(),
      modelRotation: modelRotation.exportToDoubleArrayRowOrder(),
      lightPosition: Float64Array.from([lightPosition.x(), lightPosition.y(), lightPosition.z()]),
      lightColor: Float64Array.from([lightColor.r(), lightColor.g(), lightColor.b()]),
      qualityTexture: quality.isTextureSet(),
      qualityBumpMap: quality.isBumpMapSet(),
      qualityShadingType: quality.getShadingType(),
      microFacetCsvText: resources.microFacetCsvText,
      microFacetCsvName: resources.microFacetCsvName,
      microFacetMaterialName:
        activeMaterial instanceof MicroFacetedMaterial ? activeMaterial.getName() : null,
      textureWidth: resources.textureWidth,
      textureHeight: resources.textureHeight,
      textureBytes: resources.textureBytes,
      bumpWidth: resources.bumpWidth,
      bumpHeight: resources.bumpHeight,
      bumpBytes: resources.bumpBytes,
    };
  }
}
