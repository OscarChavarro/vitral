import { ImageProcessing, RGBImageUncompressed, ZBuffer, type Camera } from '@vitral/base';
import type { DrawingArea } from '../model/drawing-area';
import { CameraState } from '../model/history/camera-state';
import type { Scene } from '../model/scene';

/**
 * Port of `render.RaytracedViewportFrames`.
 *
 * Computes the raytraced images of the viewports in CPU render mode, which are
 * drawn again in each frame, avoiding work that does not change the result:
 *
 *   - The image held in the output buffers is presented again, without
 *     raytracing, while the camera, the size and the content of the scene (see
 *     `DrawingArea.getContentGeneration`) do not change: i.e. when the drawing
 *     area is drawn again after closing a menu, or when only overlays (gizmos,
 *     selection) change. As the buffers are shared by all the viewports, only
 *     the last one raytraced benefits from it.
 *   - While a gesture is in course (see
 *     `DrawingArea.isInteractiveGestureInCourse`) the image is raytraced at a
 *     lower resolution, chosen from the time the last images took, so each
 *     frame takes about `INTERACTIVE_FRAME_NANOSECONDS`, and it is enlarged
 *     (nearest pixel) to the size of the viewport. The exact image is
 *     raytraced in the first frame after the gesture.
 */
export class RaytracedViewportFrames {
  /// Time wanted for the raytracing of a frame during a gesture
  static readonly INTERACTIVE_FRAME_NANOSECONDS: number = 66.0e6;

  /// Resolution divisors (in each axis) usable during a gesture
  private static readonly DIVISORS: readonly number[] = [1, 2, 3, 4, 6, 8, 12, 16];

  /// Part of `INTERACTIVE_FRAME_NANOSECONDS` the pixels of the image get at
  /// least: when the fixed costs of an image take most of the frame, a
  /// smaller image would barely make it faster
  private static readonly MINIMUM_PIXEL_BUDGET: number = 0.25;

  //- Description of the image held in the output buffers ---------------
  private heldImageValid: boolean = false;
  private heldCamera: Camera | null = null;
  private heldCameraState: CameraState | null = null;
  private heldWidth: number = 0;
  private heldHeight: number = 0;
  private heldDivisor: number = 1;
  private heldGeneration: number = 0;

  //- Cost estimation ---------------------------------------------------
  /// Time to raytrace a pixel in the last exact images, without the fixed
  /// cost of an image, or 0 if unknown
  private nanosecondsPerPixel: number = 0.0;
  /// Time to raytrace an image besides its pixels (preparing the scene,
  /// dispatching the work to the workers...), estimated from the reduced
  /// images
  private fixedNanoseconds: number = 0.0;
  /// Time to enlarge the last reduced images, by pixel of the viewport
  private nanosecondsPerEnlargedPixel: number = 0.0;

  //- Buffers for the images of reduced resolution ----------------------
  private reducedImage: RGBImageUncompressed | null = null;
  private reducedDepth: ZBuffer | null = null;

  /**
   * The output buffers were changed by other means (i.e. resized, or used to
   * raytrace a full image): their content can not be presented again.
   */
  invalidate(): void {
    this.heldImageValid = false;
    this.heldCamera = null;
    this.heldCameraState = null;
  }

  /**
   * Leaves in the output buffers the raytraced image of the scene seen from
   * its active camera, and its depth.
   * @param scene scene to raytrace, with the camera of the viewport active
   * @param drawingArea gives the content generation and the gesture state
   * @param image output image, already of the size of the viewport
   * @param depth output depth, of the same size of the image
   */
  async raytrace(
    scene: Scene,
    drawingArea: DrawingArea,
    image: RGBImageUncompressed,
    depth: ZBuffer,
  ): Promise<void> {
    const camera: Camera = scene.activeCamera;
    const width: number = image.getXSize();
    const height: number = image.getYSize();
    const generation: number = drawingArea.getContentGeneration();
    let divisor: number = 1;

    if (drawingArea.isInteractiveGestureInCourse()) {
      divisor = this.chooseDivisor(width, height);
    }

    const cameraState: CameraState = CameraState.capture(camera);
    if (
      this.heldImageValid &&
      this.heldCamera === camera &&
      this.heldCameraState!.isSameState(cameraState) &&
      this.heldWidth === width &&
      this.heldHeight === height &&
      this.heldDivisor <= divisor &&
      this.heldGeneration === generation
    ) {
      // The held image is as detailed as the one asked, or more
      return;
    }

    // Java's System.nanoTime()
    const start: number = performance.now() * 1.0e6;
    if (divisor === 1) {
      RaytracedViewportFrames.prefillBackground(scene, image);
      await scene.raytraceViewport(image, depth);
      const elapsed: number = performance.now() * 1.0e6 - start;
      // The fixed cost of the image is not a cost of its pixels
      this.nanosecondsPerPixel = RaytracedViewportFrames.smoothedCost(
        this.nanosecondsPerPixel,
        Math.max(elapsed - this.fixedNanoseconds, 0.5 * elapsed),
        width * height,
      );
    } else {
      const reducedWidth: number = Math.max(1, Math.trunc(width / divisor));
      const reducedHeight: number = Math.max(1, Math.trunc(height / divisor));
      this.ensureReducedBuffers(reducedWidth, reducedHeight);
      RaytracedViewportFrames.prefillBackground(scene, this.reducedImage!);
      await scene.raytraceViewport(this.reducedImage!, this.reducedDepth!);
      const raytraced: number = performance.now() * 1.0e6;
      RaytracedViewportFrames.enlarge(this.reducedImage!, this.reducedDepth!, image, depth);
      // What the pixels do not explain is the fixed cost of the image
      this.fixedNanoseconds = RaytracedViewportFrames.smoothedCost(
        this.fixedNanoseconds,
        Math.max(0.0, raytraced - start - this.nanosecondsPerPixel * reducedWidth * reducedHeight),
        1.0,
      );
      this.nanosecondsPerEnlargedPixel = RaytracedViewportFrames.smoothedCost(
        this.nanosecondsPerEnlargedPixel,
        performance.now() * 1.0e6 - raytraced,
        width * height,
      );
    }

    this.heldImageValid = true;
    this.heldCamera = camera;
    this.heldCameraState = cameraState;
    this.heldWidth = width;
    this.heldHeight = height;
    this.heldDivisor = divisor;
    this.heldGeneration = generation;
  }

  /**
   * @return the smallest divisor of the resolution whose image is expected
   * to be raytraced and enlarged in `INTERACTIVE_FRAME_NANOSECONDS`, or whose
   * pixels are expected to take `MINIMUM_PIXEL_BUDGET` of that time if the
   * fixed costs leave less than that to them
   */
  private chooseDivisor(width: number, height: number): number {
    if (this.nanosecondsPerPixel <= 0.0) {
      return 1;
    }
    const pixels: number = width * height;
    for (const divisor of RaytracedViewportFrames.DIVISORS) {
      let fixed: number = this.fixedNanoseconds;
      if (divisor > 1) {
        fixed += this.nanosecondsPerEnlargedPixel * pixels;
      }
      const budget: number = Math.max(
        RaytracedViewportFrames.INTERACTIVE_FRAME_NANOSECONDS - fixed,
        RaytracedViewportFrames.MINIMUM_PIXEL_BUDGET *
          RaytracedViewportFrames.INTERACTIVE_FRAME_NANOSECONDS,
      );
      if ((this.nanosecondsPerPixel * pixels) / (divisor * divisor) <= budget) {
        return divisor;
      }
    }
    return RaytracedViewportFrames.DIVISORS[RaytracedViewportFrames.DIVISORS.length - 1]!;
  }

  /**
   * @param current cost known before, or 0 if unknown
   * @param elapsedNanoseconds time measured for `pixels` pixels
   * @param pixels number of pixels measured (1 for a cost by image)
   * @return the cost of a pixel, smoothed: it depends on what the pixels show
   */
  private static smoothedCost(current: number, elapsedNanoseconds: number, pixels: number): number {
    if (pixels <= 0.0) {
      return current;
    }
    const sample: number = elapsedNanoseconds / pixels;
    if (current <= 0.0) {
      return sample;
    }
    return 0.5 * current + 0.5 * sample;
  }

  private ensureReducedBuffers(width: number, height: number): void {
    if (
      this.reducedImage === null ||
      this.reducedImage.getXSize() !== width ||
      this.reducedImage.getYSize() !== height
    ) {
      this.reducedImage = new RGBImageUncompressed();
      this.reducedImage.init(width, height);
      this.reducedDepth = new ZBuffer(width, height);
    }
  }

  /**
   * The fixed background is copied to the image before raytracing it, as the
   * application has always done.
   */
  private static prefillBackground(scene: Scene, image: RGBImageUncompressed): void {
    if (scene.selectedBackground === 1 && scene.fixedBackground !== null) {
      ImageProcessing.resize(scene.fixedBackground.getImage(), image);
    }
  }

  /**
   * Copies a small image and its depth over larger ones, repeating each pixel
   * (nearest pixel), so overlays are still depth tested against it.
   */
  private static enlarge(
    source: RGBImageUncompressed,
    sourceDepth: ZBuffer,
    target: RGBImageUncompressed,
    targetDepth: ZBuffer,
  ): void {
    const sw: number = source.getXSize();
    const sh: number = source.getYSize();
    const tw: number = target.getXSize();
    const th: number = target.getYSize();
    // Rows are stored in the order of the image: the last one first
    const sourcePixels: Uint8Array = source.getRawImageDirectBuffer();
    const targetPixels: Uint8Array = target.getRawImageDirectBuffer();
    const sourceZ: Float32Array = sourceDepth.getZBuffer();
    const targetZ: Float32Array = targetDepth.getZBuffer();
    const columns: Int32Array = new Int32Array(tw);

    for (let x: number = 0; x < tw; x++) {
      columns[x] = Math.min(sw - 1, Math.trunc((x * sw) / tw));
    }
    let lastSourceY: number = -1;
    let lastRow: number = 0;
    for (let y: number = 0; y < th; y++) {
      const sy: number = Math.min(sh - 1, Math.trunc((y * sh) / th));
      const row: number = (th - 1 - y) * tw * 3;
      if (sy === lastSourceY) {
        // Same source row: the enlarged one is repeated
        targetPixels.copyWithin(row, lastRow, lastRow + tw * 3);
      } else {
        const base: number = (sh - 1 - sy) * sw * 3;
        for (let x: number = 0; x < tw; x++) {
          const s: number = base + columns[x]! * 3;
          targetPixels[row + 3 * x] = sourcePixels[s]!;
          targetPixels[row + 3 * x + 1] = sourcePixels[s + 1]!;
          targetPixels[row + 3 * x + 2] = sourcePixels[s + 2]!;
        }
        lastSourceY = sy;
        lastRow = row;
      }
      const zBase: number = sy * sw;
      const zTarget: number = y * tw;
      for (let x: number = 0; x < tw; x++) {
        targetZ[zTarget + x] = sourceZ[zBase + columns[x]!]!;
      }
    }
  }
}
