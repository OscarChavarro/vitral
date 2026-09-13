import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  ViewChild,
  signal,
} from '@angular/core';
import { Matrix4x4d, Vector3Dd, type RGBImageUncompressed } from '@vitral/base';
import { WebGLImageRenderer, WebGLSphereRenderer, WebSystem } from '@vitral/webgl';
import { ExampleCameraInteraction } from '../_shared/example-camera-interaction';
import { ExampleRendererConfigurationInteraction } from '../_shared/example-renderer-configuration-interaction';
import { Animation } from './gui/animation';
import { ShadersKeyboardInteractionTechniques } from './gui/shaders-keyboard-interaction-techniques';
import { ShadersMouseInteractionTechniques } from './gui/shaders-mouse-interaction-techniques';
import { ShadersReader } from './io/shaders-reader';
import { ShadersModel } from './model/shaders-model';
import { WebGLHudRenderer } from './render/webgl-hud-renderer';
import { SoftwareRaycaster } from './render/software-raycaster';

/**
 * TypeScript/WebGL port of `java/testsuite/Jogl4Examples/ShadersExample`.
 *
 * The Java program is a `JFrame` owning a JOGL `GLCanvas`; here the frame is
 * this Angular module inside the testsuite container and the drawable is its
 * `<canvas>`, so the `GLEventListener` callbacks map onto the component
 * lifecycle: `init` onto the first `ngOnChanges`, `reshape` onto the resize
 * observer, `display` onto `drawFrame`, and `dispose` onto `ngOnDestroy`.
 * Java's `JFrame` close and `System.exit(0)` become the `deactivate` output,
 * which returns the container to its explorer.
 *
 * This is the module that exercises the shader library in full — every
 * program `WebGLRendererConfigurationShaderSelector` can select is reachable
 * from its keyboard — and the one that puts the GPU path beside the CPU
 * raytracer on the same scene, with `[.]` switching between them.
 *
 * Java's `init` refuses a context below OpenGL 4.1. A browser has no such
 * version to read: WebGL2 is GLSL ES 3.00, and every GLSL file of the Vitral
 * tree compiles and links under it after `WebGLShaderPreprocessor` translates
 * it, which is what is checked instead — the absence of a WebGL2 context is
 * the refusal.
 *
 * Two boundaries are specific to this program.
 *
 * The sphere rotation and the light rotation are driven by Java's
 * `javax.swing.Timer` at 30 frames a second, one timer serving both and
 * started and stopped by `animationStateChanged`. A page has no Swing timer,
 * so the host timer takes its place at the same period, with the same
 * `Animation.tick` and the same elapsed-time light step; the coalescing Java
 * asks the timer for is what the component's own repaint flag already does.
 *
 * Java's `drawSoftwareHud` unloads and redraws the raytraced frame as a
 * texture every time, because the CPU raytracer rewrites the buffer underneath
 * it. That is kept; what changes is that the raytracing itself no longer
 * blocks, since `render.SoftwareRaycaster` runs it on Web Workers.
 */
@Component({
  selector: 'app-shaders-example',
  templateUrl: './shaders-example.html',
  styleUrl: './shaders-example.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShadersExample implements OnChanges, OnDestroy {
  private static readonly FULL_ROTATION_RADIANS = 2.0 * Math.PI;
  private static readonly LIGHT_ROTATION_PERIOD_SECONDS = 8.0;
  private static readonly LIGHT_ANGULAR_SPEED_RAD_PER_SECOND =
    ShadersExample.FULL_ROTATION_RADIANS / ShadersExample.LIGHT_ROTATION_PERIOD_SECONDS;

  @ViewChild('canvas', { static: true })
  private readonly canvasRef!: ElementRef<HTMLCanvasElement>;

  @Input({ required: true })
  textureUrl: string | null = null;

  @Input({ required: true })
  bumpMapUrl: string | null = null;

  @Input({ required: true })
  microFacetCsvUrl: string | null = null;

  @Output()
  readonly deactivate = new EventEmitter<void>();

  protected readonly statusMessage = signal<string | null>(null);

  private readonly model = new ShadersModel();
  private readonly cameraController = new ExampleCameraInteraction(this.model.getCamera());
  private readonly qualityController = new ExampleRendererConfigurationInteraction(
    this.model.getQuality(),
  );
  private readonly mouseInteractionTechniques = new ShadersMouseInteractionTechniques(
    this.cameraController,
  );
  private readonly keyboardInteractionTechniques = new ShadersKeyboardInteractionTechniques(
    this.model,
    this.cameraController,
    this.qualityController,
  );
  private readonly animation = new Animation();
  private readonly softwareRaycaster = new SoftwareRaycaster();
  private readonly hudRenderer = new WebGLHudRenderer();
  private readonly reader = new ShadersReader();

  private gl: WebGL2RenderingContext | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private animationTimerId: ReturnType<typeof setInterval> | null = null;
  private lastRenderingMode = this.model.getRenderingMode();
  private lightAnimationAngleRadians = 0.0;
  private lastLightTickMillis = -1;
  private rendering = false;
  private repaintRequested = false;
  private loadedKey: string | null = null;

  async ngOnChanges(): Promise<void> {
    if (this.textureUrl === null || this.bumpMapUrl === null || this.microFacetCsvUrl === null) {
      return;
    }

    const requestedKey = this.textureUrl + '\n' + this.bumpMapUrl + '\n' + this.microFacetCsvUrl;
    try {
      if (this.gl === null) {
        await this.initialize();
      }
      if (requestedKey !== this.loadedKey) {
        await this.loadResources(this.textureUrl, this.bumpMapUrl, this.microFacetCsvUrl);
        this.loadedKey = requestedKey;
      }
      this.focusCanvas();
      this.draw();
    } catch (error) {
      this.statusMessage.set(
        error instanceof Error ? error.message : 'WebGL shaders example initialization failed',
      );
    }
  }

  ngOnDestroy(): void {
    this.stopAnimationTimer();
    this.softwareRaycaster.dispose();
    this.resizeObserver?.disconnect();
    if (this.gl !== null) {
      // Java's `dispose` releases the sphere renderer, the three images it has
      // uploaded, the image and camera renderers and the HUD, once, behind a
      // `glResourcesReleased` flag; the component is torn down once, which is
      // that flag.
      WebGLSphereRenderer.dispose(this.gl);
      this.unloadImage(this.model.getTextureMap());
      this.unloadImage(this.model.getBumpMapHeightRgb());
      this.unloadImage(this.model.getSoftwareFrameImage());
      WebGLImageRenderer.dispose(this.gl);
      this.hudRenderer.dispose(this.gl);
      this.gl = null;
    }
  }

  /**
   * Java's three `ImagePersistence` reads inside `ShadersModel.initializeDefaults`
   * and `SoftwareRaycaster.loadBumpNormalMap`, which throw an
   * `IllegalStateException` that ends the program when a resource is missing. A
   * module inside the container cannot end the process, so the failure is
   * reported in the status overlay and the module stays up.
   */
  private async loadResources(
    textureUrl: string,
    bumpMapUrl: string,
    microFacetCsvUrl: string,
  ): Promise<void> {
    this.statusMessage.set('Loading the shading resources ...');
    try {
      const resources = await this.reader.read(textureUrl, bumpMapUrl, microFacetCsvUrl);
      this.model.setResources(resources);
      this.softwareRaycaster.setResources(
        this.model,
        resources.microFacetCsvText,
        resources.microFacetCsvName,
      );
    } catch (error) {
      console.error('Failed loading textures for ShadersExample');
      console.error(error instanceof Error ? error.message : String(error));
      this.statusMessage.set(
        'Failed loading the shading resources: ' +
          (error instanceof Error ? error.message : String(error)),
      );
      return;
    }
    this.model.configureInitialView();
    this.cameraController.adoptCameraState();
    this.statusMessage.set(null);
  }

  protected onPointerDown(event: PointerEvent): void {
    this.focusCanvas();
    this.canvasRef.nativeElement.setPointerCapture(event.pointerId);
    if (
      this.mouseInteractionTechniques.processPointerDownEvent(event, this.canvasRef.nativeElement)
    ) {
      this.draw();
    }
  }

  protected onPointerMove(event: PointerEvent): void {
    if (
      this.mouseInteractionTechniques.processPointerMoveEvent(event, this.canvasRef.nativeElement)
    ) {
      this.draw();
    }
  }

  protected onPointerUp(event: PointerEvent): void {
    if (this.mouseInteractionTechniques.processPointerUpEvent()) {
      this.draw();
    }
    if (this.canvasRef.nativeElement.hasPointerCapture(event.pointerId)) {
      this.canvasRef.nativeElement.releasePointerCapture(event.pointerId);
    }
  }

  protected onWheel(event: WheelEvent): void {
    event.preventDefault();
    if (
      this.mouseInteractionTechniques.processMouseWheelEvent(event, this.canvasRef.nativeElement)
    ) {
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

    const repaint = this.keyboardInteractionTechniques.processPressed(vitralEvent, {
      requestExit: () => this.deactivate.emit(),
      animationStateChanged: () => this.applyAnimationState(),
    });
    if (!repaint) {
      return;
    }

    event.preventDefault();
    this.draw();
  }

  protected onKeyUp(event: KeyboardEvent): void {
    if (this.keyboardInteractionTechniques.processReleased(WebSystem.web2vsdkKeyEvent(event))) {
      this.draw();
    }
  }

  /**
   * Java's `Actions.animationStateChanged`: the one timer runs while either
   * animation is on and is stopped, with the sphere animation reset, when both
   * are off.
   */
  private applyAnimationState(): void {
    if (this.model.isAnimationEnabled() || this.model.isLightAnimationEnabled()) {
      this.animation.reset();
      this.startAnimationTimer();
    } else {
      this.stopAnimationTimer();
      this.animation.reset();
    }
  }

  private startAnimationTimer(): void {
    if (this.animationTimerId !== null) {
      return;
    }
    this.animationTimerId = setInterval(() => {
      this.animation.tick(this.model);
      this.animateLightIfNeeded();
      this.softwareRaycaster.invalidateSnapshot();
      this.draw();
    }, Animation.FRAME_DELAY_MILLIS);
  }

  private stopAnimationTimer(): void {
    if (this.animationTimerId !== null) {
      clearInterval(this.animationTimerId);
      this.animationTimerId = null;
    }
  }

  /**
   * Java's `animateLightIfNeeded`, with `performance.now()` in place of
   * `System.nanoTime()`: one full turn of the light about the negative Y axis
   * every eight seconds, advanced by elapsed time and clamped at a quarter of
   * a second.
   */
  private animateLightIfNeeded(): void {
    if (!this.model.isLightAnimationEnabled()) {
      this.lastLightTickMillis = -1;
      return;
    }

    const now: number = performance.now();
    if (this.lastLightTickMillis < 0) {
      this.lastLightTickMillis = now;
      return;
    }

    let elapsedSeconds: number = (now - this.lastLightTickMillis) / 1000.0;
    this.lastLightTickMillis = now;
    if (elapsedSeconds < 0.0) {
      return;
    }
    if (elapsedSeconds > 0.25) {
      elapsedSeconds = 0.25;
    }

    this.lightAnimationAngleRadians +=
      ShadersExample.LIGHT_ANGULAR_SPEED_RAD_PER_SECOND * elapsedSeconds;
    const rotation: Matrix4x4d = new Matrix4x4d().axisRotation(
      this.lightAnimationAngleRadians,
      0.0,
      -1.0,
      0.0,
    );
    const baseLightPosition = new Vector3Dd(1.0, -3.0, 1.0);
    this.model.getLight().setPosition(rotation.multiply(baseLightPosition));
  }

  private focusCanvas(): void {
    setTimeout(() => this.canvasRef.nativeElement.focus({ preventScroll: true }));
  }

  private unloadImage(image: RGBImageUncompressed | null): void {
    if (this.gl !== null && image !== null) {
      WebGLImageRenderer.unload(this.gl, image);
    }
  }

  /**
   * Java asks for a `GL4` profile with 32 depth bits and refuses a context
   * below 4.1. The browser counterpart is the availability of a WebGL2
   * context with a depth buffer; WebGL does not let a page choose the
   * depth-buffer precision. Java's `init` then only checks the version; this
   * port compiles the shader programs both renderers will need instead, which
   * is why it is asynchronous — see the drawing-buffer rule recorded for the
   * WebGL example programs.
   */
  private async initialize(): Promise<void> {
    const canvas = this.canvasRef.nativeElement;
    const gl = canvas.getContext('webgl2', { antialias: true, depth: true });
    if (gl === null) {
      throw new Error('ShadersExample requires WebGL2 support.');
    }

    this.gl = gl;
    await this.hudRenderer.prepare(gl);
    this.resizeObserver = new ResizeObserver(() => this.draw());
    this.resizeObserver.observe(canvas);
  }

  private draw(): void {
    this.repaintRequested = true;
    if (this.rendering) return;

    void this.renderLoop().catch((error: unknown) => {
      this.statusMessage.set(
        error instanceof Error ? error.message : 'WebGL shaders example render failed',
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

  /**
   * Java's `reshape` followed by its `display`.
   *
   * `reshape` sets the viewport, resizes the camera and the software frame
   * image, invalidates the raycaster snapshot and unloads the image it
   * replaced; all of that is here, driven by the canvas size instead of by a
   * `GLAutoDrawable` surface size.
   */
  private async drawFrame(): Promise<void> {
    const gl = this.gl;
    if (gl === null) return;

    const canvas = this.canvasRef.nativeElement;
    const width = Math.max(1, Math.floor(canvas.clientWidth * window.devicePixelRatio));
    const height = Math.max(1, Math.floor(canvas.clientHeight * window.devicePixelRatio));
    if (canvas.width !== width || canvas.height !== height) {
      canvas.width = width;
      canvas.height = height;
    }
    gl.viewport(0, 0, width, height);

    const previousSoftwareImage: RGBImageUncompressed | null = this.model.getSoftwareFrameImage();
    this.model.updateSoftwareViewportAndCamera(width, height);
    this.softwareRaycaster.invalidateSnapshot();
    if (
      previousSoftwareImage !== null &&
      previousSoftwareImage !== this.model.getSoftwareFrameImage()
    ) {
      WebGLImageRenderer.unload(gl, previousSoftwareImage);
    }

    gl.enable(gl.DEPTH_TEST);
    gl.clearColor(0, 0, 0, 1);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

    const modelRotation: Matrix4x4d = new Matrix4x4d().axisRotation(
      this.model.getSphereRotationAngleRadians(),
      0.0,
      0.0,
      1.0,
    );

    if (this.model.getRenderingMode() === 'SOFTWARE') {
      if (this.lastRenderingMode !== 'SOFTWARE') {
        this.softwareRaycaster.invalidateSnapshot();
      }
      // Keep CPU and GPU paths aligned: same camera + same model transform.
      //
      // Java splits this call in two: `display` makes it only when the sphere
      // animation is off, and `renderSoftwareFrame` makes it from the timer
      // callback when it is on, because a Swing timer runs off the paint
      // thread and can raytrace ahead of the next repaint. Here the timer asks
      // for a repaint and this method is the only place a frame is produced,
      // so the two cases meet in one call with the same arguments.
      await this.softwareRaycaster.render(this.model, this.model.getCamera(), modelRotation);
      await this.drawSoftwareHud(gl, this.model.getSoftwareFrameImage());
      await this.hudRenderer.draw(gl, this.model);
      this.lastRenderingMode = 'SOFTWARE';
      return;
    }
    this.lastRenderingMode = 'OPENGL_4_1';

    await WebGLSphereRenderer.draw(
      gl,
      this.model.getSphere(),
      this.model.getCamera(),
      this.model.getLight(),
      this.model.getActiveMaterialForCurrentShading(),
      this.model.getQuality(),
      this.model.getTextureMap(),
      this.model.getBumpMapHeightRgb(),
      modelRotation,
      this.model.getSphereMeridians(),
      this.model.getSphereParallels(),
    );
    await this.hudRenderer.draw(gl, this.model);
  }

  /**
   * Java's `drawSoftwareHud`: force a texture re-upload because the underlying
   * RGB image buffer is rewritten by the software raytracer every frame, then
   * draw it across the viewport.
   */
  private async drawSoftwareHud(
    gl: WebGL2RenderingContext,
    image: RGBImageUncompressed | null,
  ): Promise<void> {
    if (image === null) {
      return;
    }

    WebGLImageRenderer.unload(gl, image);
    await WebGLImageRenderer.draw(gl, image);
  }
}
