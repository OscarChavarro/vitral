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
import { VSDK, Vector3Dd } from '@vitral/base';
import { WebSystem } from '@vitral/webgl';
import { ExampleCameraInteraction } from '../_shared/example-camera-interaction';
import { ExampleRendererConfigurationInteraction } from '../_shared/example-renderer-configuration-interaction';
import { PolygonClippingKeyboardInteractionTechniques } from './gui/polygon-clipping-keyboard-interaction-techniques';
import { PolygonClippingMouseInteractionTechniques } from './gui/polygon-clipping-mouse-interaction-techniques';
import { PolygonClippingReader } from './io/polygon-clipping-reader';
import { PolygonClippingDebuggerModel } from './model/polygon-clipping-debugger-model';
import { PolygonClippingModelingTools } from './model/polygon-clipping-modeling-tools';
import { WebGLPolygonClippingRenderer } from './render/webgl-polygon-clipping-renderer';

/**
 * TypeScript/WebGL port of `java/testsuite/Jogl4Examples/PolygonClippingExample`.
 *
 * The Java program is a `JFrame` owning a JOGL `GLCanvas`; here the frame is
 * this Angular module inside the testsuite container and the drawable is its
 * `<canvas>`, so the `GLEventListener` callbacks of
 * `render.JoglPolygonClippingRenderer` map onto the component lifecycle:
 * `init` onto the first `ngOnChanges`, `reshape` onto the resize observer,
 * `display` onto `drawFrame`, and `dispose` onto `ngOnDestroy`. Java's `JFrame`
 * close and `System.exit(0)` become the `deactivate` output, which returns the
 * container to its explorer.
 *
 * This is the module that exercises the Weiler--Atherton clipper: two contour
 * sets are read, the four boolean operations are run over them, and the result
 * is shown on three panels beside the annotated input, with the intersection
 * vertices the clipper paired drawn in their own colour.
 *
 * Java's `main` builds the model, parses a command line and, when that command
 * line asked for the offline mode, renders one frame to `output.png` and
 * returns without opening a window. Neither `options.CommandLineOptions` nor
 * `render.JoglPolygonClippingOfflineRenderer` is ported: they are the batch
 * path, and a module inside the container is the interactive program. Every
 * setting they carry is reachable from the keyboard, which is where the Java
 * program puts them too.
 *
 * Four boundaries are specific to this program:
 *
 *   - `rebuildScene` reads two files each time it runs, which is on startup
 *     and on every `[1]`, `[2]` and `[3]` keystroke. A frame must not wait on
 *     a network read, so `io.PolygonClippingReader` fetches every file the
 *     fixture table names once, when the module opens, and `rebuildScene`
 *     stays synchronous.
 *   - Java's `toggleFullscreenMode` disposes the `JFrame` and builds a new
 *     one, undecorated, on the default screen device, with a separate path for
 *     macOS. A page asks the browser instead, and the browser owns the state
 *     the three `fullScreenMode` fields of the Java model keep.
 *   - `focusCameraOnCurrentScene` asks the camera controller, when it is an
 *     orbiter, to take the scene centre as its point of interest. The
 *     container's `ExampleCameraInteraction` stands in for
 *     `CameraControllerOrbiter` until the plan's Phase 38, and
 *     `adoptCameraState()` is where it takes the framing the model just
 *     computed.
 *   - The snapshot the `[H]` key asks for is written by `ImagePersistence`
 *     beside the Java program; here the same image, under the same
 *     `frameNNNN.png` name, is handed to the browser as a download.
 */
@Component({
  selector: 'app-polygon-clipping-example',
  templateUrl: './polygon-clipping-example.html',
  styleUrl: './polygon-clipping-example.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PolygonClippingExample implements OnChanges, OnDestroy {
  @ViewChild('canvas', { static: true })
  private readonly canvasRef!: ElementRef<HTMLCanvasElement>;

  @Input({ required: true })
  polygonsBaseUrl: string | null = null;

  @Output()
  readonly deactivate = new EventEmitter<void>();

  protected readonly statusMessage = signal<string | null>(null);

  private readonly model = new PolygonClippingDebuggerModel();
  private readonly cameraController = new ExampleCameraInteraction(this.model.getCamera());
  private readonly qualityController = new ExampleRendererConfigurationInteraction(
    this.model.getQuality(),
  );
  private readonly mouseInteractionTechniques = new PolygonClippingMouseInteractionTechniques(
    this.cameraController,
  );
  private readonly keyboardInteractionTechniques = new PolygonClippingKeyboardInteractionTechniques(
    this.model,
    this.cameraController,
    this.qualityController,
  );
  private readonly renderer = new WebGLPolygonClippingRenderer(this.model);
  private readonly reader = new PolygonClippingReader();

  private gl: WebGL2RenderingContext | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private polygonTexts: ReadonlyMap<string, string> = new Map<string, string>();
  private rendering = false;
  private repaintRequested = false;
  private loadedKey: string | null = null;

  async ngOnChanges(): Promise<void> {
    if (this.polygonsBaseUrl === null) {
      return;
    }

    const requestedKey: string = this.polygonsBaseUrl;
    try {
      if (this.gl === null) {
        await this.initialize();
      }
      if (requestedKey !== this.loadedKey) {
        await this.loadResources(this.polygonsBaseUrl);
        this.loadedKey = requestedKey;
      }
      this.focusCanvas();
      this.draw();
    } catch (error) {
      this.statusMessage.set(
        error instanceof Error
          ? error.message
          : 'WebGL polygon clipping example initialization failed',
      );
    }
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    if (this.gl !== null) {
      this.renderer.dispose(this.gl);
      this.gl = null;
    }
  }

  /**
   * Java's `main` builds the scene and frames the camera on it before the
   * window opens, and its `Actions.rebuildScene` does both again on every
   * fixture or operation change. The contour files are fetched first, once,
   * for the reason recorded on the class.
   */
  private async loadResources(polygonsBaseUrl: string): Promise<void> {
    this.statusMessage.set('Loading the polygon fixtures ...');
    try {
      this.polygonTexts = await this.reader.read(polygonsBaseUrl);
    } catch (error) {
      console.error('Failed loading polygon fixtures for PolygonClippingExample');
      console.error(error instanceof Error ? error.message : String(error));
      this.statusMessage.set(
        'Failed loading the polygon fixtures: ' +
          (error instanceof Error ? error.message : String(error)),
      );
      return;
    }
    this.rebuildScene();
    this.focusCameraOnCurrentScene();
    this.statusMessage.set(null);
  }

  /**
   * Java's `PolygonClippingExample.rebuildScene(model)`: clear the error
   * state, run the modeling tools, and report whatever they throw in the
   * model's own error state, which the HUD prints.
   */
  private rebuildScene(): void {
    try {
      this.model.clearErrorState();
      PolygonClippingModelingTools.rebuildScene(this.model, this.polygonTexts);
    } catch (error) {
      this.model.setErrorState(this.formatBuildErrorMessage(error));
    }
  }

  /**
   * Java's `PolygonClippingExample.focusCameraOnCurrentScene(model)`: keep the
   * current eye-to-focus distance, or twenty when there is none, and place the
   * eye that far along negative Y from the scene centre, looking at it with Z
   * up. Java then hands the centre to the orbiter as its point of interest;
   * the stand-in adopts the camera it has just been given instead.
   */
  private focusCameraOnCurrentScene(): void {
    const center: Vector3Dd = PolygonClippingModelingTools.calculateSceneCenter(this.model);
    const eye: Vector3Dd = this.model.getCamera().getPosition();
    const focus: Vector3Dd = this.model.getCamera().getFocusedPosition();
    let distance: number = eye.subtract(focus).length();
    if (distance < VSDK.EPSILON) {
      distance = 20.0;
    }
    this.model
      .getCamera()
      .setPosition(new Vector3Dd(center.x(), center.y() - distance, center.z()));
    this.model.getCamera().setFocusedPositionMaintainingOrthogonality(center);
    this.model.getCamera().setUpMaintainingOrthogonality(new Vector3Dd(0, 0, 1));
    this.cameraController.adoptCameraState();
  }

  private formatBuildErrorMessage(error: unknown): string {
    const name: string = error instanceof Error ? error.name : 'Error';
    const message: string = error instanceof Error ? error.message : String(error);
    return (
      'Build error [' +
      this.model.getCurrentTestCase().name +
      ']: ' +
      name +
      (message.length > 0 ? ' - ' + message : '')
    );
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
      rebuildScene: () => {
        this.rebuildScene();
        this.focusCameraOnCurrentScene();
      },
      toggleFullscreen: () => this.toggleFullscreenMode(),
      requestSnapshot: () => this.model.setTakeSnapshot(true),
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
   * Java's `toggleFullscreenMode`, which rebuilds its `JFrame` undecorated on
   * the default screen device and keeps the windowed bounds to come back to.
   * A page asks the browser for the same thing on the canvas itself, and the
   * browser keeps that state, so the model needs none of it. The request may
   * be refused when it does not come from a user gesture, which a keystroke
   * is, so the rejection is only reported.
   */
  private toggleFullscreenMode(): void {
    const canvas: HTMLCanvasElement = this.canvasRef.nativeElement;
    const request: Promise<void> =
      document.fullscreenElement === canvas
        ? document.exitFullscreen()
        : canvas.requestFullscreen();
    void request.catch((error: unknown) => {
      console.error(
        '[PolygonClippingExample] ' + (error instanceof Error ? error.message : String(error)),
      );
    });
  }

  private focusCanvas(): void {
    setTimeout(() => this.canvasRef.nativeElement.focus({ preventScroll: true }));
  }

  /**
   * Java asks for a `GL4bc` profile, falling back to `GL4`, with 64 depth
   * bits. The browser counterpart is the availability of a WebGL2 context with
   * a depth buffer; WebGL does not let a page choose the depth-buffer
   * precision. The shader programs both renderers need are compiled here,
   * which is why this is asynchronous — see the drawing-buffer rule recorded
   * for the WebGL example programs.
   */
  private async initialize(): Promise<void> {
    const canvas = this.canvasRef.nativeElement;
    const gl = canvas.getContext('webgl2', { antialias: true, depth: true });
    if (gl === null) {
      throw new Error('PolygonClippingExample requires WebGL2 support.');
    }

    this.gl = gl;
    await this.renderer.init(gl, canvas.width, canvas.height);
    this.resizeObserver = new ResizeObserver(() => this.draw());
    this.resizeObserver.observe(canvas);
  }

  private draw(): void {
    this.repaintRequested = true;
    if (this.rendering) return;

    void this.renderLoop().catch((error: unknown) => {
      this.statusMessage.set(
        error instanceof Error ? error.message : 'WebGL polygon clipping example render failed',
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

  /** Java's `reshape` followed by its `display`, driven by the canvas size. */
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
    this.renderer.reshape(gl, width, height);

    await this.renderer.display(gl, width, height);
  }
}
