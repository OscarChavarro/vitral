import {
  BrowserWorkerExecutor,
  MicroFacetedMaterial,
  ParallelRaytracer,
  RGBImageUncompressed,
  type Camera,
  type Matrix4x4d,
  type ParallelRaytracerTileRequest,
  type ParallelRaytracerTileResult,
} from '@vitral/base';
import type { ShadersModel } from '../model/shaders-model';
import type { ShadersSceneDescriptor } from './software-raycaster-protocol';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/ShadersExample/src/render/SoftwareRaycaster.java`.
 *
 * The CPU path, as in Java, is a `ParallelRaytracer` of `@vitral/base`: it
 * splits the frame in bands, several per worker, and every worker takes the
 * next band off a shared pending queue until the queue is empty. The worker
 * count taken from the host's processor count, and the scene snapshot each
 * band is traced against, are Java's.
 *
 * Java runs those workers as JVM threads over one shared
 * `RGBImageUncompressed`; a browser has no threads, and its Web Workers share
 * no heap. `ParallelRaytracer` deals with that (see its notes); what is left
 * here is to say how to create a worker — a `BrowserWorkerExecutor` over
 * `raytracer-tile.worker` — and to describe the scene
 * (`ShadersSceneDescriptor`) the workers rebuild the snapshot from.
 *
 * `invalidateSnapshot()` keeps Java's comment and its emptiness: the snapshot
 * is rebuilt on every render, so there is nothing to invalidate.
 *
 * Java's `loadBumpNormalMap` reads the bump map from disk in the constructor.
 * Here `io.ShadersReader` has already read it, so what this class holds is the
 * bytes it sends to the workers, and the `NormalMap` is built on their side
 * from the very same file with the same unit scale.
 *
 * Java's `render` returns when every band is done, because `Future.get()`
 * blocks. Nothing blocks in a page, so this one is asynchronous; the component
 * treats a frame as finished when it resolves.
 *
 * That asynchrony is also why this class keeps a back buffer Java does not
 * need. Java raytraces inside `display`, so the drawable swaps only once the
 * frame is whole and the window keeps showing the previous one meanwhile. A
 * page yields to the browser while the workers run, and the model's image is
 * the one the component draws, so the workers write into a private image of
 * the same size instead, and only a finished frame is copied into the model's
 * image — the front buffer. A frame the user never sees half-built, and an
 * earlier frame on screen while the next one is traced.
 */
export class SoftwareRaycaster {
  private static readonly MAX_WORKERS = 8;

  private readonly parallelRaytracer: ParallelRaytracer;
  private generation = 0;
  private backBuffer: RGBImageUncompressed | null = null;
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
    this.parallelRaytracer = new ParallelRaytracer(
      () =>
        new BrowserWorkerExecutor<ParallelRaytracerTileRequest, ParallelRaytracerTileResult>(
          new Worker(new URL('./raytracer-tile.worker', import.meta.url), { type: 'module' }),
        ),
      Math.min(SoftwareRaycaster.MAX_WORKERS, ParallelRaytracer.availableProcessors()),
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

  /**
   * Traces a frame into the back buffer and, once every tile is in, presents
   * it by copying it into the model's software frame image. Answers whether a
   * frame was presented, which it is not when there is nothing to render yet.
   */
  async render(
    model: ShadersModel,
    activeCamera: Camera,
    modelRotation: Matrix4x4d,
  ): Promise<boolean> {
    const frontImage: RGBImageUncompressed | null = model.getSoftwareFrameImage();
    if (frontImage === null || this.resources === null) {
      return false;
    }

    const width: number = frontImage.getXSize();
    const height: number = frontImage.getYSize();
    const outputImage: RGBImageUncompressed = this.ensureBackBuffer(width, height);
    await this.parallelRaytracer.execute(
      outputImage,
      model.getQuality(),
      this.buildSceneDescriptor(model, activeCamera, modelRotation),
      false,
    );

    frontImage.getRawImageDirectBuffer().set(outputImage.getRawImageDirectBuffer());
    return true;
  }

  /**
   * Stops the workers of the `ParallelRaytracer`, which, as in Java, outlive
   * a frame; they are torn down with the module.
   */
  dispose(): void {
    this.parallelRaytracer.dispose();
    this.backBuffer = null;
  }

  private ensureBackBuffer(width: number, height: number): RGBImageUncompressed {
    if (
      this.backBuffer !== null &&
      this.backBuffer.getXSize() === width &&
      this.backBuffer.getYSize() === height
    ) {
      return this.backBuffer;
    }
    const image = new RGBImageUncompressed();
    if (!image.init(width, height)) {
      throw new Error('Could not allocate software back buffer ' + width + 'x' + height);
    }
    this.backBuffer = image;
    return image;
  }

  private buildSceneDescriptor(
    model: ShadersModel,
    activeCamera: Camera,
    modelRotation: Matrix4x4d,
  ): ShadersSceneDescriptor {
    const resources = this.resources!;
    const activeMaterial = model.getActiveMaterialForCurrentShading();
    const lightPosition = model.getLight().getPosition();
    const lightColor = model.getLight().getEmission();

    return {
      generation: this.generation,
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
