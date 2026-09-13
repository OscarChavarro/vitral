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
import { WebSystem } from '@vitral/webgl';
import { ExampleCameraInteraction } from '../_shared/example-camera-interaction';
import { ExampleRendererConfigurationInteraction } from '../_shared/example-renderer-configuration-interaction';
import { DebuggerAnimationController } from './animation/debugger-animation-controller';
import { KeyboardInteractionTechniques } from './gui/keyboard-interaction-techniques';
import { MouseInteractionTechniques } from './gui/mouse-interaction-techniques';
import { DebuggerReader } from './io/debugger-reader';
import { DebuggerModel } from './model/debugger-model';
import { WebGLDebuggerRenderer } from './render/webgl-debugger-renderer';

/**
 * TypeScript/WebGL port of `java/testsuite/Jogl4Examples/MD2Example`
 * (`Md2MeshExample`).
 *
 * The Java program is a `JFrame` owning a JOGL `GLCanvas`; here the frame is
 * this Angular module inside the testsuite container and the drawable is its
 * `<canvas>`, so the `GLEventListener` callbacks map onto the component
 * lifecycle: `init` onto the first `ngOnChanges`, `reshape` onto the resize
 * observer, `display` onto `drawFrame`, and `dispose` onto `ngOnDestroy`.
 * Java's `JFrame` close and `System.exit(0)` become the `deactivate` output,
 * which returns the container to its explorer.
 *
 * This is the module that exercises MD2 reading and playback: a Quake II model
 * is a stack of vertex frames grouped into named animations, and the animation
 * generator ported alongside it drives the frame interpolation at
 * twenty-four ticks a second while the HUD names the animation being played.
 *
 * Two things the Java program does at startup are answered where the container
 * can answer them:
 *
 *   - `init()` names `etc/md2/samourai.md2` and `etc/md2/samourai.jpg` relative
 *     to the program's own directory. A browser has no such directory, so both
 *     resources are named by URL, and the dialog reached by right-clicking
 *     `MD2Example` in the explorer tree is where they are given; re-entering
 *     them reloads the model, which is what re-running the Java program with
 *     other assets does.
 *   - Java checks `GL_VERSION` for OpenGL 4.1 and throws otherwise; the
 *     browser counterpart is the availability of a WebGL2 context, which is
 *     what `initialize` checks.
 *
 * Java reports a failed read with `Logger.reportMessageWithException` and
 * `System.exit(0)`; a module inside the container cannot end the process, so
 * the same failure is reported in the status overlay and the module stays up.
 */
@Component({
  selector: 'app-md2-example',
  templateUrl: './md2-example.html',
  styleUrl: './md2-example.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MD2Example implements OnChanges, OnDestroy {
  @ViewChild('canvas', { static: true })
  private readonly canvasRef!: ElementRef<HTMLCanvasElement>;

  @Input({ required: true })
  md2Url: string | null = null;

  @Input()
  textureUrl: string | null = null;

  @Output()
  readonly deactivate = new EventEmitter<void>();

  protected readonly statusMessage = signal<string | null>(null);

  private readonly model = new DebuggerModel();
  private readonly cameraController = new ExampleCameraInteraction(this.model.getCamera());
  private readonly qualityController = new ExampleRendererConfigurationInteraction(
    this.model.getQualitySelection(),
  );
  private readonly mouseInteractionTechniques = new MouseInteractionTechniques(
    this.cameraController,
  );
  private readonly keyboardInteractionTechniques = new KeyboardInteractionTechniques(
    this.model,
    this.cameraController,
    this.qualityController,
  );
  private readonly reader = new DebuggerReader();
  private readonly renderer = new WebGLDebuggerRenderer(this.model);
  private readonly animationController = new DebuggerAnimationController();

  private gl: WebGL2RenderingContext | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private rendering = false;
  private repaintRequested = false;
  private loadedUrl: string | null = null;

  async ngOnChanges(): Promise<void> {
    if (this.md2Url === null) {
      return;
    }

    const requestedUrl = this.md2Url;
    const requestedTextureUrl = this.textureUrl;
    const requestedKey = requestedUrl + '\n' + (requestedTextureUrl ?? '');
    try {
      if (this.gl === null) {
        await this.initialize();
      }
      if (requestedKey !== this.loadedUrl) {
        await this.loadModel(requestedUrl, requestedTextureUrl);
        this.loadedUrl = requestedKey;
      }
      // Java's `init(GLAutoDrawable)` starts the animation thread once the
      // context is ready.
      this.animationController.start(this.model.getMd2Mesh(), () => this.draw());
      this.focusCanvas();
      this.draw();
    } catch (error) {
      this.statusMessage.set(
        error instanceof Error ? error.message : 'WebGL MD2 example initialization failed',
      );
    }
  }

  ngOnDestroy(): void {
    this.animationController.stop();
    this.resizeObserver?.disconnect();
    if (this.gl !== null) {
      this.renderer.dispose(this.gl);
      this.gl = null;
    }
  }

  /**
   * Java's `init()`, which reads the model through `io.DebuggerReader` and
   * exits the process on `IOException`.
   */
  private async loadModel(md2Url: string, textureUrl: string | null): Promise<void> {
    this.statusMessage.set('Loading ' + md2Url + ' ...');

    try {
      await this.reader.readMd2WithTexture(md2Url, textureUrl ?? '', this.model.getMd2Mesh());
    } catch (error) {
      console.error('Input/Output error');
      console.error(error instanceof Error ? error.message : String(error));
      this.statusMessage.set('Failed to read "' + md2Url + '".');
      return;
    }

    if (this.model.getMd2Mesh().numFrames === 0) {
      this.statusMessage.set(
        'No frames were read from "' + md2Url + '". A Quake II MD2 version 8 resource is expected.',
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

    if (!this.keyboardInteractionTechniques.processKeyPressedEvent(vitralEvent)) {
      return;
    }

    event.preventDefault();
    this.draw();
  }

  protected onKeyUp(event: KeyboardEvent): void {
    if (
      this.keyboardInteractionTechniques.processKeyReleasedEvent(WebSystem.web2vsdkKeyEvent(event))
    ) {
      this.draw();
    }
  }

  private focusCanvas(): void {
    setTimeout(() => this.canvasRef.nativeElement.focus({ preventScroll: true }));
  }

  /**
   * Java asks for a `GL4` profile with 64 depth bits and then checks that the
   * context really is OpenGL 4.1 or newer. The browser counterpart is the
   * availability of a WebGL2 context with a depth buffer; WebGL does not let a
   * page choose the depth-buffer precision. Java's `init` then allocates
   * buffers; this port compiles the renderer's shader programs as well, which
   * is why it is asynchronous.
   */
  private async initialize(): Promise<void> {
    const canvas = this.canvasRef.nativeElement;
    const gl = canvas.getContext('webgl2', { antialias: true, depth: true });
    if (gl === null) {
      throw new Error('MD2Example requires WebGL2 support.');
    }

    this.gl = gl;
    await this.renderer.init(gl);
    this.resizeObserver = new ResizeObserver(() => this.draw());
    this.resizeObserver.observe(canvas);
  }

  private draw(): void {
    this.repaintRequested = true;
    if (this.rendering) return;

    void this.renderLoop().catch((error: unknown) => {
      this.statusMessage.set(
        error instanceof Error ? error.message : 'WebGL MD2 example render failed',
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
    await this.renderer.display(gl);
  }
}
