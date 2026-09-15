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
import {
  ByteArrayOutputStream,
  StlWriter,
  VSDK,
  Vector3Dd,
  type PolyhedralBoundedSolid,
} from '@vitral/base';
import { WebSystem } from '@vitral/webgl';
import { SolidAnimationController } from './animation/solid-animation-controller';
import { DebuggerKeyboardInteractionTechniques } from './gui/debugger-keyboard-interaction-techniques';
import { DebuggerMouseInteractionTechniques } from './gui/debugger-mouse-interaction-techniques';
import { saveBytesAs } from './io/browser-file-output';
import { PolyhedralBoundedSolidReader } from './io/polyhedral-bounded-solid-reader';
import { DebuggerModel } from './models/debugger-model';
import type { GeneralModelsResources } from './models/general-models-builder';
import type { SolidModelNames } from './models/solid-model-names';
import { PolyhedralBoundedSolidModelingTools } from './polyhedral-bounded-solid-modeling-tools';
import { WebGLDebuggerRenderer } from './render/webgl-debugger-renderer';

/**
 * TypeScript/WebGL port of `java/testsuite/Jogl4Examples/PolyhedralBoundedSolidExample`:
 * every model and every view of the interactive Java debugger, including the
 * models built by boolean set operations and by plane splitting.
 *
 * The Java program is `PolyhedralBoundedSolidExample.main`, which builds the
 * model and hands it to `InteractiveDebugger`, a `JFrame` owning a JOGL
 * `GLCanvas`. Here the frame is this Angular module inside the testsuite
 * container and the drawable is its `<canvas>`, so this component is both:
 * `buildSolidWithRecovery` and `formatBuildErrorMessage` are `main`'s class's,
 * and the listener methods, `rebuildSolid`, `exportCurrentSolidToStl`,
 * `calculateSolidCenter`, `recenterOrbiterAfterModelChange` and
 * `toggleFullscreenMode` are `InteractiveDebugger`'s. The `GLEventListener`
 * callbacks of `render.Jogl4DebuggerRenderer` map onto the component
 * lifecycle: `init` onto the first `ngOnChanges`, `reshape` onto the resize
 * observer, `display` onto `drawFrame`, and `dispose` onto `ngOnDestroy`.
 * Java's window close and `System.exit(0)` become the `deactivate` output.
 *
 * What the program shows is the half-edge solid of the selected model — built
 * with Euler operators, primitives, sweeps, gluing, set operations, a plane
 * split, a glyph or a STEP import — with its faces shaded, its edges, points, normals and bounds switched from
 * the quality keys, its lights as billboards, the boundary loops of the
 * selected face (or of all of them) as curved arrows with the face filled in
 * red, the edge-visibility debug view, the Appel hidden-line result, and a HUD
 * carrying the selection, the model, the switches, the vertex and face ids and
 * the build error.
 *
 * `main`'s command line, its `polySolidModel` property and its offline,
 * screenshot, motif-sweep and Appel-dump paths are not ported, nor are
 * `options.CommandLineOptions` and `render.Jogl4HeadlessRenderer`: they are the
 * batch path, and a module inside the container is the interactive program,
 * which `main` enters when the command line asks for nothing else.
 * `render.Jogl4HudOperandsRenderer` is not ported either: nothing in the Java
 * program constructs it, `Jogl4DebuggerRenderer` carrying its own copy of the
 * inset drawing, which is ported there.
 *
 * Five boundaries are specific to this program:
 *
 *   - `GeneralModelsBuilder` reads a STEP file and a font while it builds. A
 *     build must not wait on a network read, so `io.PolyhedralBoundedSolidReader`
 *     fetches both once, when the module opens, and the builder stays
 *     synchronous, as Java's is.
 *   - `main` switches the kernel's logger to report without exiting and to
 *     raise fatal errors as exceptions; the same two settings are applied when
 *     the module opens.
 *   - `toggleFullscreenMode` rebuilds the `JFrame`, with a separate path for
 *     macOS; a page asks the browser for its fullscreen element, as the
 *     `PolygonClippingExample` module records.
 *   - The orbiter's point of interest, which `recenterOrbiterAfterModelChange`
 *     moves, is the focused position of the camera in the container's stand-in
 *     for `CameraControllerOrbiter`, which `adoptCameraState()` takes.
 *   - `exportCurrentSolidToStl` writes `output.stl` with a `FileOutputStream`
 *     beside the program; the same bytes, written by the ported `StlWriter`
 *     into a `ByteArrayOutputStream`, are handed to the browser as a download
 *     under that name.
 */
@Component({
  selector: 'app-polyhedral-bounded-solid-example',
  templateUrl: './polyhedral-bounded-solid-example.html',
  styleUrl: './polyhedral-bounded-solid-example.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PolyhedralBoundedSolidExample implements OnChanges, OnDestroy {
  private static readonly STL_EXPORT_SCALE_FACTOR = 1.0 / 100.0;

  @ViewChild('canvas', { static: true })
  private readonly canvasRef!: ElementRef<HTMLCanvasElement>;

  @Input({ required: true })
  stepFileUrl: string | null = null;

  @Input({ required: true })
  fontFileUrl: string | null = null;

  @Output()
  readonly deactivate = new EventEmitter<void>();

  protected readonly statusMessage = signal<string | null>(null);

  private readonly model = new DebuggerModel();
  private readonly keyboardInteractionTechniques = new DebuggerKeyboardInteractionTechniques();
  private readonly mouseInteractionTechniques = new DebuggerMouseInteractionTechniques();
  private readonly joglDebuggerRenderer = new WebGLDebuggerRenderer(this.model);
  private readonly solidAnimationController = new SolidAnimationController();
  private readonly reader = new PolyhedralBoundedSolidReader();

  private gl: WebGL2RenderingContext | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private resources: GeneralModelsResources | null = null;
  private rendering = false;
  private repaintRequested = false;
  private loadedKey: string | null = null;

  async ngOnChanges(): Promise<void> {
    if (this.stepFileUrl === null || this.fontFileUrl === null) {
      return;
    }

    const requestedKey: string = this.stepFileUrl + '\n' + this.fontFileUrl;
    try {
      if (this.gl === null) {
        await this.initialize();
      }
      if (requestedKey !== this.loadedKey) {
        await this.loadResources(this.stepFileUrl, this.fontFileUrl);
        this.loadedKey = requestedKey;
      }
      this.focusCanvas();
      this.repaintCanvas();
    } catch (error) {
      this.statusMessage.set(
        error instanceof Error
          ? error.message
          : 'WebGL polyhedral bounded solid example initialization failed',
      );
    }
  }

  ngOnDestroy(): void {
    this.solidAnimationController.stop();
    this.resizeObserver?.disconnect();
    if (this.gl !== null) {
      this.joglDebuggerRenderer.dispose(this.gl);
      this.gl = null;
    }
  }

  /**
   * `main`'s part before the window opens: the logger settings and the first
   * build. The files the build reads are fetched first, for the reason
   * recorded on the class.
   */
  private async loadResources(stepFileUrl: string, fontFileUrl: string): Promise<void> {
    this.statusMessage.set('Loading the STEP solid and the font ...');
    // Keep the debugger process alive and surface fatal kernel issues as exceptions.
    VSDK.setWithSystemExit(false);
    VSDK.setWithFatalExceptions(true);

    this.resources = await this.reader.read(stepFileUrl, fontFileUrl);
    this.statusMessage.set('Building ' + this.model.getSolidModelName() + ' ...');
    await new Promise<void>((resolve) => setTimeout(resolve));
    this.buildSolidWithRecovery();
    this.statusMessage.set(null);
  }

  /** `PolyhedralBoundedSolidExample.buildSolidWithRecovery(model)`. */
  private buildSolidWithRecovery(): void {
    try {
      this.model.clearErrorState();
      this.model.setSolid(
        PolyhedralBoundedSolidModelingTools.buildSolid(this.model, this.resources!),
      );
      if (this.model.getSolid() === null) {
        throw new Error('Solid builder returned null');
      }
      this.model.clampFaceIndex();
    } catch (e) {
      this.model.setErrorState(this.formatBuildErrorMessage(e));
    }
  }

  /** `PolyhedralBoundedSolidExample.formatBuildErrorMessage(model, e)`. */
  private formatBuildErrorMessage(e: unknown): string {
    let msg = '';
    msg += 'Build error';
    if (this.model.getSolidModelName() !== null) {
      msg += ' [' + this.model.getSolidModelName() + ']';
    }
    msg += ': ' + (e instanceof Error ? e.name : 'Error');
    const message: string = e instanceof Error ? e.message : String(e);
    if (message !== null && message.length !== 0) {
      msg += ' - ' + message;
    }
    return msg;
  }

  /** `InteractiveDebugger.rebuildSolid()`. */
  private rebuildSolid(): void {
    this.buildSolidWithRecovery();
  }

  /** `InteractiveDebugger.repaintCanvas()`. */
  private repaintCanvas(): void {
    this.draw();
  }

  /** `InteractiveDebugger.exportCurrentSolidToStl()`. */
  private exportCurrentSolidToStl(): void {
    const solid: PolyhedralBoundedSolid | null = this.model.getSolid();
    if (solid === null) {
      console.error('[PolyhedralBoundedSolidExample] No selected solid available for STL export');
      return;
    }

    const outputFile = 'output.stl';
    try {
      const outputStream = new ByteArrayOutputStream();
      StlWriter.exportSolid(
        solid,
        outputStream,
        PolyhedralBoundedSolidExample.STL_EXPORT_SCALE_FACTOR,
      );
      saveBytesAs(outputFile, outputStream.toByteArray(), 'model/stl');
      console.log('[PolyhedralBoundedSolidExample] Exported ' + outputFile);
    } catch (e) {
      console.error(
        '[PolyhedralBoundedSolidExample] STL export failed: ' +
          (e instanceof Error ? e.message : String(e)),
      );
    }
  }

  /** `InteractiveDebugger.calculateSolidCenter()`. */
  private calculateSolidCenter(): Vector3Dd {
    const solid: PolyhedralBoundedSolid | null = this.model.getSolid();
    if (solid === null) {
      return new Vector3Dd(0, 0, 0);
    }

    const minMax: Float64Array | null = solid.getMinMax();
    if (minMax === null || minMax.length < 6) {
      return new Vector3Dd(0, 0, 0);
    }

    return new Vector3Dd(
      (minMax[0]! + minMax[3]!) / 2.0,
      (minMax[1]! + minMax[4]!) / 2.0,
      (minMax[2]! + minMax[5]!) / 2.0,
    );
  }

  /**
   * `InteractiveDebugger.recenterOrbiterAfterModelChange`: when the model
   * changed, keep the eye's offset from the point of interest and move both to
   * the new solid's centre. The stand-in's point of interest is the camera's
   * focused position; see the class comment.
   */
  private recenterOrbiterAfterModelChange(
    previousModelName: SolidModelNames,
    previousPointOfInterest: Vector3Dd,
  ): void {
    if (previousModelName === this.model.getSolidModelName()) {
      return;
    }
    if (this.model.getSolid() === null) {
      return;
    }

    const previousEye: Vector3Dd = this.model.getCamera().getPosition();
    const relativeVector: Vector3Dd = previousEye.subtract(previousPointOfInterest);
    const newPointOfInterest: Vector3Dd = this.calculateSolidCenter();
    const newEye: Vector3Dd = newPointOfInterest.add(relativeVector);

    this.model.getCamera().setPosition(newEye);
    this.model.getCamera().setFocusedPositionMaintainingOrthogonality(newPointOfInterest);
    this.model.getCameraController().adoptCameraState();
  }

  /**
   * `InteractiveDebugger.toggleFullscreenMode`, which rebuilds its `JFrame`
   * undecorated on the default screen device. A page asks the browser for the
   * same thing on the canvas itself; the request may be refused when it does
   * not come from a user gesture, which a keystroke is, so a rejection is only
   * reported.
   */
  private toggleFullscreenMode(): void {
    const canvas: HTMLCanvasElement = this.canvasRef.nativeElement;
    const request: Promise<void> =
      document.fullscreenElement === canvas
        ? document.exitFullscreen()
        : canvas.requestFullscreen();
    void request.catch((error: unknown) => {
      console.error(
        '[PolyhedralBoundedSolidExample] ' +
          (error instanceof Error ? error.message : String(error)),
      );
    });
  }

  protected onPointerDown(event: PointerEvent): void {
    this.focusCanvas();
    this.canvasRef.nativeElement.setPointerCapture(event.pointerId);
    if (
      this.mouseInteractionTechniques.processMousePressed(
        this.model,
        event,
        this.canvasRef.nativeElement,
      )
    ) {
      this.repaintCanvas();
    }
  }

  protected onPointerMove(event: PointerEvent): void {
    if (
      this.mouseInteractionTechniques.processMouseDragged(
        this.model,
        event,
        this.canvasRef.nativeElement,
      )
    ) {
      this.repaintCanvas();
    }
  }

  protected onPointerUp(event: PointerEvent): void {
    if (this.mouseInteractionTechniques.processMouseReleased(this.model)) {
      this.repaintCanvas();
    }
    if (this.canvasRef.nativeElement.hasPointerCapture(event.pointerId)) {
      this.canvasRef.nativeElement.releasePointerCapture(event.pointerId);
    }
  }

  protected onWheel(event: WheelEvent): void {
    event.preventDefault();
    if (
      this.mouseInteractionTechniques.processMouseWheelMoved(
        this.model,
        event,
        this.canvasRef.nativeElement,
      )
    ) {
      this.repaintCanvas();
    }
  }

  /** `InteractiveDebugger.keyPressed`. */
  protected onKeyDown(event: KeyboardEvent): void {
    if (this.resources === null) {
      return;
    }
    const vitralEvent = WebSystem.web2vsdkKeyEvent(event);
    const previousModelName: SolidModelNames = this.model.getSolidModelName();
    const previousPointOfInterest: Vector3Dd = this.model.getCamera().getFocusedPosition();

    if (vitralEvent.keycode !== 'KEY_NONE') {
      event.preventDefault();
    }
    if (
      this.keyboardInteractionTechniques.processPressed(this.model, vitralEvent, {
        requestExit: () => this.deactivate.emit(),
        rebuildSolid: () => this.rebuildSolid(),
        toggleFullscreen: () => this.toggleFullscreenMode(),
        toggleSolidAnimation: () => this.solidAnimationController.toggleAnimation(this.model),
        requestScreenshot: () => {
          this.joglDebuggerRenderer.requestScreenshot('screenshot.png');
          this.repaintCanvas();
        },
        requestStlExport: () => this.exportCurrentSolidToStl(),
      })
    ) {
      this.recenterOrbiterAfterModelChange(previousModelName, previousPointOfInterest);
      this.repaintCanvas();
    }
  }

  /** `InteractiveDebugger.keyReleased`. */
  protected onKeyUp(event: KeyboardEvent): void {
    if (
      this.keyboardInteractionTechniques.processReleased(
        this.model,
        WebSystem.web2vsdkKeyEvent(event),
      )
    ) {
      this.repaintCanvas();
    }
  }

  private focusCanvas(): void {
    setTimeout(() => this.canvasRef.nativeElement.focus({ preventScroll: true }));
  }

  /**
   * `InteractiveDebugger.createGUI`: Java asks for a `GL4bc` profile, falling
   * back to `GL4`, with 64 depth bits; the browser counterpart is a WebGL2
   * context with a depth buffer, whose precision a page cannot choose. The
   * canvas is registered with the model, the renderer compiles what it needs
   * here — see the drawing-buffer rule recorded for the WebGL example
   * programs — and the animation controller is started.
   */
  private async initialize(): Promise<void> {
    const canvas = this.canvasRef.nativeElement;
    const gl = canvas.getContext('webgl2', { antialias: true, depth: true });
    if (gl === null) {
      throw new Error('PolyhedralBoundedSolidExample requires WebGL2 support.');
    }

    this.gl = gl;
    this.model.setCanvas(canvas);
    await this.joglDebuggerRenderer.init(gl, canvas.width, canvas.height);
    this.resizeObserver = new ResizeObserver(() => this.repaintCanvas());
    this.resizeObserver.observe(canvas);
    this.solidAnimationController.start(this.model, () => this.repaintCanvas());
  }

  private draw(): void {
    if (this.resources === null) {
      return;
    }
    this.repaintRequested = true;
    if (this.rendering) return;

    void this.renderLoop().catch((error: unknown) => {
      this.statusMessage.set(
        error instanceof Error
          ? error.message
          : 'WebGL polyhedral bounded solid example render failed',
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
    this.joglDebuggerRenderer.reshape(gl, width, height);

    await this.joglDebuggerRenderer.display(gl, width, height);
  }
}
