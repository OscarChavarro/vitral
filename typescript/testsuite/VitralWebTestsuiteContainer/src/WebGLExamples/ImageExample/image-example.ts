import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  OnDestroy,
  Output,
  ViewChild,
  signal,
} from '@angular/core';
import {
  Camera,
  Matrix4x4d,
  RGBAImageCompressed,
  RGBAImageUncompressed,
  type Image,
} from '@vitral/base';
import {
  WebGLCameraRenderer,
  WebGLImageRenderer,
  WebGLMatrixRenderer,
  WebGLSimpleCorridorSample,
  WebImagePersistence,
  WebSystem,
} from '@vitral/webgl';
import { ExampleCameraInteraction } from '../_shared/example-camera-interaction';

/**
 * TypeScript/WebGL port of `java/testsuite/Jogl4Examples/ImageExample`.
 *
 * The Java program is a `JFrame` owning a JOGL `GLCanvas`; here the frame is
 * this Angular module inside the testsuite container and the drawable is its
 * `<canvas>`, so the `GLEventListener` callbacks map onto the component
 * lifecycle: `init` onto `ngAfterViewInit`, `reshape` onto the resize
 * observer, `display` onto `drawFrame`, and `dispose` onto `ngOnDestroy`.
 * Java's `JFrame` close and `System.exit(0)` become the `deactivate` output,
 * which returns the container to its explorer.
 *
 * The two images and everything drawn with them are the Java ones:
 * `etc/images/render.jpg` read through the platform's own JPEG decoder, and
 * `etc/textures/earth.dds` read as DXT1 blocks that stay compressed all the
 * way to the GPU. Java names them with a relative file path; the container
 * publishes the same `etc/` tree as a static asset, so the same two resources
 * are named by URL.
 *
 * Because a browser reaches a GLSL source and an image resource over `fetch`,
 * loading and every drawing operation that needs a shader are asynchronous.
 */
@Component({
  selector: 'app-image-example',
  templateUrl: './image-example.html',
  styleUrl: './image-example.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageExample implements AfterViewInit, OnDestroy {
  private static readonly IMAGE_DEPTH_BIAS_FACTOR = -1.0;
  private static readonly IMAGE_DEPTH_BIAS_UNITS = -8.0;

  @ViewChild('canvas', { static: true })
  private readonly canvasRef!: ElementRef<HTMLCanvasElement>;

  @Output()
  readonly deactivate = new EventEmitter<void>();

  protected readonly statusMessage = signal<string | null>(null);

  private readonly camera = new Camera();
  private readonly cameraController = new ExampleCameraInteraction(this.camera);
  private readonly corridor = new WebGLSimpleCorridorSample();
  private renderImage: Image | null = null;
  private earthImage: Image | null = null;

  private gl: WebGL2RenderingContext | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private rendering = false;
  private repaintRequested = false;
  private glResourcesReleased = false;

  async ngAfterViewInit(): Promise<void> {
    try {
      this.initialize();
      this.focusCanvas();
      this.renderImage = await this.loadImage('/etc/images/render.jpg');
      this.earthImage = await this.loadImage('/etc/textures/earth.dds');
      await this.prepareGlResources();
      this.draw();
    } catch (error) {
      this.statusMessage.set(
        error instanceof Error ? error.message : 'WebGL image example initialization failed',
      );
    }
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();

    const gl = this.gl;
    if (gl && !this.glResourcesReleased) {
      WebGLImageRenderer.unload(gl, this.renderImage);
      WebGLImageRenderer.unload(gl, this.earthImage);
      this.corridor.dispose(gl);
      WebGLCameraRenderer.dispose(gl);
      WebGLImageRenderer.dispose(gl);
      this.glResourcesReleased = true;
    }
    this.gl = null;
  }

  /**
   * Java reports a failed read on `System.err` and calls `System.exit(1)`.
   * A module inside the container cannot end the process, so the same failure
   * is reported to the user in the status overlay and the module stays up.
   */
  private async loadImage(imageUrl: string): Promise<Image | null> {
    try {
      return await WebImagePersistence.importRGB(imageUrl);
    } catch (error) {
      console.error('Error: could not read image file "' + imageUrl + '".');
      console.error(error instanceof Error ? error.message : String(error));
      this.statusMessage.set('Error: could not read image file "' + imageUrl + '".');
      return null;
    }
  }

  protected onPointerDown(event: PointerEvent): void {
    this.focusCanvas();
    this.canvasRef.nativeElement.setPointerCapture(event.pointerId);
    if (this.cameraController.processPointerDown(event, this.canvasRef.nativeElement)) {
      this.draw();
    }
  }

  protected onPointerMove(event: PointerEvent): void {
    if (this.cameraController.processPointerMove(event, this.canvasRef.nativeElement)) {
      this.draw();
    }
  }

  protected onPointerUp(event: PointerEvent): void {
    if (this.cameraController.processPointerUp()) {
      this.draw();
    }
    if (this.canvasRef.nativeElement.hasPointerCapture(event.pointerId)) {
      this.canvasRef.nativeElement.releasePointerCapture(event.pointerId);
    }
  }

  protected onWheel(event: WheelEvent): void {
    event.preventDefault();
    if (this.cameraController.processWheel(event, this.canvasRef.nativeElement)) {
      this.draw();
    }
  }

  protected onKeyDown(event: KeyboardEvent): void {
    const vitralEvent = WebSystem.web2vsdkKeyEvent(event);

    if (vitralEvent.keycode === 'KEY_ESC') {
      event.preventDefault();
      event.stopPropagation();
      this.deactivate.emit();
      return;
    }

    if (!this.cameraController.processKeyPressed(vitralEvent)) {
      return;
    }

    event.preventDefault();
    this.draw();
  }

  private focusCanvas(): void {
    setTimeout(() => this.canvasRef.nativeElement.focus({ preventScroll: true }));
  }

  /**
   * Java asks for a `GL4` profile with 64 depth bits and then refuses a
   * context below OpenGL 4.1. The browser counterpart of both checks is the
   * availability of a WebGL2 context with a depth buffer; WebGL does not let a
   * page choose the depth-buffer precision.
   */
  private initialize(): void {
    const canvas = this.canvasRef.nativeElement;
    const gl = canvas.getContext('webgl2', { antialias: true, depth: true });
    if (!gl) {
      throw new Error('ImageExample requires WebGL2 support.');
    }

    this.gl = gl;
    this.configureModel();
    this.resizeObserver = new ResizeObserver(() => this.draw());
    this.resizeObserver.observe(canvas);
    this.draw();
  }

  private configureModel(): void {
    this.camera.setNearPlaneDistance(0.05);
    this.camera.setFarPlaneDistance(100);
  }

  /**
   * The rest of what a JOGL program does in `init(GLAutoDrawable)`: every
   * shader program a frame needs is compiled here, because a browser reaches a
   * GLSL source over `fetch` and a frame that awaited one would be presented
   * and cleared halfway through.
   */
  private async prepareGlResources(): Promise<void> {
    const gl = this.gl;
    if (!gl) return;
    await this.corridor.prepare(gl);
    await WebGLImageRenderer.prepare(gl);
  }

  private async drawTexturedPolygon(
    gl: WebGL2RenderingContext,
    projection: Matrix4x4d,
    image: Image | null,
    x0: number,
    y0: number,
    width: number,
    height: number,
  ): Promise<void> {
    const texture = WebGLImageRenderer.activate(gl, image);
    if (texture === null) {
      return;
    }

    const x1 = x0 + width;
    const y1 = y0 + height;

    const positions = new Float32Array([
      x0,
      y0,
      0.01,
      x1,
      y0,
      0.01,
      x1,
      y1,
      0.01,
      x0,
      y0,
      0.01,
      x1,
      y1,
      0.01,
      x0,
      y1,
      0.01,
    ]);

    const uvCoordinates = new Float32Array([
      0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0,
    ]);

    gl.disable(gl.CULL_FACE);
    // Java sets glPolygonMode(GL_FRONT_AND_BACK, GL_FILL); WebGL rasterizes
    // filled polygons only and has no polygon-mode entry point.

    await WebGLImageRenderer.drawTexturedQuad(
      gl,
      texture,
      projection,
      positions,
      uvCoordinates,
      1.0,
      1.0,
      1.0,
    );
  }

  private async drawWorldImages(gl: WebGL2RenderingContext, projection: Matrix4x4d): Promise<void> {
    if (this.renderImage === null) {
      return;
    }
    const renderWidth = this.renderImage.getXSize() / this.renderImage.getYSize();

    await this.drawTexturedPolygon(gl, projection, this.renderImage, 0.0, 0.0, renderWidth, 1.0);
    await this.drawTexturedPolygon(gl, projection, this.earthImage, 0.0, -1.0, 1.0, 1.0);
  }

  private async drawWorldImagesDepthBiased(
    gl: WebGL2RenderingContext,
    projection: Matrix4x4d,
  ): Promise<void> {
    gl.enable(gl.DEPTH_TEST);
    gl.depthFunc(gl.LEQUAL);
    gl.enable(gl.POLYGON_OFFSET_FILL);
    gl.polygonOffset(ImageExample.IMAGE_DEPTH_BIAS_FACTOR, ImageExample.IMAGE_DEPTH_BIAS_UNITS);

    await this.drawWorldImages(gl, projection);

    gl.disable(gl.POLYGON_OFFSET_FILL);
    gl.depthFunc(gl.LESS);
  }

  private async drawHudImage(
    gl: WebGL2RenderingContext,
    image: Image | null,
    upperLeft: boolean,
  ): Promise<void> {
    const texture = WebGLImageRenderer.activate(gl, image);
    if (texture === null || image === null) {
      return;
    }

    const viewport = gl.getParameter(gl.VIEWPORT) as Int32Array;
    const viewportWidth = Math.max(viewport[2] ?? 1, 1);
    const viewportHeight = Math.max(viewport[3] ?? 1, 1);

    const width = 2.0 * (image.getXSize() / viewportWidth);
    const height = 2.0 * (image.getYSize() / viewportHeight);

    const x0 = -1.0;
    const y0 = upperLeft ? 1.0 - height : -1.0;
    const x1 = x0 + width;
    const y1 = y0 + height;

    const positions = new Float32Array([
      x0,
      y0,
      0.0,
      x1,
      y0,
      0.0,
      x1,
      y1,
      0.0,
      x0,
      y0,
      0.0,
      x1,
      y1,
      0.0,
      x0,
      y1,
      0.0,
    ]);

    const uvCoordinates = new Float32Array([
      0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0,
    ]);

    gl.disable(gl.CULL_FACE);
    const withAlpha =
      image instanceof RGBAImageUncompressed || image instanceof RGBAImageCompressed;
    if (withAlpha) {
      gl.enable(gl.BLEND);
      gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
    }

    await WebGLImageRenderer.drawTexturedQuad(
      gl,
      texture,
      Matrix4x4d.identityMatrix(),
      positions,
      uvCoordinates,
      1.0,
      1.0,
      1.0,
    );

    if (withAlpha) {
      gl.disable(gl.BLEND);
    }
  }

  private async drawHud(gl: WebGL2RenderingContext): Promise<void> {
    gl.disable(gl.DEPTH_TEST);
    await this.drawHudImage(gl, this.renderImage, false);
    await this.drawHudImage(gl, this.earthImage, true);
    gl.enable(gl.DEPTH_TEST);
  }

  private async drawObjectsGL(gl: WebGL2RenderingContext): Promise<void> {
    const projection = WebGLCameraRenderer.activate(gl, this.camera);

    await this.corridor.drawGL(gl, projection);
    WebGLMatrixRenderer.draw(gl, projection, Matrix4x4d.identityMatrix());
    await this.drawWorldImagesDepthBiased(gl, projection);
    await this.drawHud(gl);
  }

  private draw(): void {
    this.repaintRequested = true;
    if (this.rendering) return;

    void this.renderLoop().catch((error: unknown) => {
      this.statusMessage.set(
        error instanceof Error ? error.message : 'WebGL image example render failed',
      );
    });
  }

  private async renderLoop(): Promise<void> {
    this.rendering = true;
    try {
      while (this.repaintRequested) {
        this.repaintRequested = false;
        await this.drawFrame();
      }
    } finally {
      this.rendering = false;
    }
  }

  private async drawFrame(): Promise<void> {
    if (!this.gl) return;

    const canvas = this.canvasRef.nativeElement;
    const width = Math.max(1, Math.floor(canvas.clientWidth * window.devicePixelRatio));
    const height = Math.max(1, Math.floor(canvas.clientHeight * window.devicePixelRatio));
    if (canvas.width !== width || canvas.height !== height) {
      canvas.width = width;
      canvas.height = height;
    }

    // reshape(...)
    const gl = this.gl;
    gl.viewport(0, 0, width, height);
    this.camera.updateViewportResize(width, height);

    // display(...)
    gl.disable(gl.BLEND);
    gl.enable(gl.DEPTH_TEST);
    gl.clearColor(0, 0, 0, 1);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

    await this.drawObjectsGL(gl);
  }
}
