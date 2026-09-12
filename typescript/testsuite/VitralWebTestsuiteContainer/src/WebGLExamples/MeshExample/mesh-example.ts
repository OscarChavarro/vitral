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
import { SimpleScene, TangibleInterfaceNetworkClient } from '@vitral/base';
import { WebEnvironmentPersistence, WebSystem } from '@vitral/webgl';
import { ExampleCameraInteraction } from '../_shared/example-camera-interaction';
import { ExampleRendererConfigurationInteraction } from '../_shared/example-renderer-configuration-interaction';
import { AnimationController } from './animation/animation-controller';
import { MeshKeyboardInteractionTechniques } from './gui/mesh-keyboard-interaction-techniques';
import { MeshMouseInteractionTechniques } from './gui/mesh-mouse-interaction-techniques';
import { TangibleInterfaceInteractionTechniques } from './gui/tangible-interface-interaction-techniques';
import { MeshModel } from './model/mesh-model';
import { WebGLDebuggerRenderer } from './render/webgl-debugger-renderer';

/**
 * TypeScript/WebGL port of `java/testsuite/Jogl4Examples/MeshExample`.
 *
 * The Java program is a `JFrame` owning a JOGL `GLCanvas`; here the frame is
 * this Angular module inside the testsuite container and the drawable is its
 * `<canvas>`, so the `GLEventListener` callbacks map onto the component
 * lifecycle: `init` onto `ngAfterViewInit`, `reshape` onto the resize
 * observer, `display` onto `drawFrame`, and `dispose` onto `ngOnDestroy`.
 * Java's `JFrame` close and `System.exit(0)` become the `deactivate` output,
 * which returns the container to its explorer.
 *
 * Two things the Java program does at startup have no browser counterpart and
 * are answered where the container can answer them:
 *
 *   - `MeshExample(String fileName)` reads its mesh from a `java.io.File`,
 *     asking `awt.FileSelectorDialog` for one when the command line named
 *     none. A browser has no file system, so the resource is always named by
 *     URL and the selector is the container's own modal dialog, reached by
 *     right-clicking `MeshExample` in the explorer tree. The URL arrives here
 *     as the `meshUrl` input, and re-entering it reloads the scene, which is
 *     what re-running the Java program with another file does.
 *   - `options.CommandLineOptions` carries `-tangibleServer`; a page has no
 *     command line, so that argument is asked for in the same dialog, next to
 *     a checkbox saying whether to open the connection at all.
 */
@Component({
  selector: 'app-mesh-example',
  templateUrl: './mesh-example.html',
  styleUrl: './mesh-example.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshExample implements OnChanges, OnDestroy {
  @ViewChild('canvas', { static: true })
  private readonly canvasRef!: ElementRef<HTMLCanvasElement>;

  @Input({ required: true })
  meshUrl: string | null = null;

  /**
   * What `options.CommandLineOptions` takes from `-tangibleServer`. A blank
   * value leaves `MeshModel` its own default, as Java's setter does.
   */
  @Input()
  tangibleServiceUrl: string | null = null;

  /**
   * Whether to open the tangible-interface connection at all. Java always
   * opens it; see the explorer dialog for why the browser asks first.
   */
  @Input()
  tangibleEnabled = false;

  @Output()
  readonly deactivate = new EventEmitter<void>();

  protected readonly statusMessage = signal<string | null>(null);

  private readonly model = new MeshModel();
  private readonly cameraController = new ExampleCameraInteraction(this.model.getCamera());
  private readonly qualityController = new ExampleRendererConfigurationInteraction(
    this.model.getQualitySelection(),
  );
  private readonly mouseInteractionTechniques = new MeshMouseInteractionTechniques(
    this.cameraController,
  );
  private readonly keyboardInteractionTechniques = new MeshKeyboardInteractionTechniques(
    this.model,
    this.cameraController,
    this.qualityController,
  );
  private readonly renderer = new WebGLDebuggerRenderer(this.model);
  private readonly animationController = new AnimationController();

  private tangibleInterfaceClient: TangibleInterfaceNetworkClient | null = null;
  private connectedTangibleServiceUrl: string | null = null;
  private gl: WebGL2RenderingContext | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private rendering = false;
  private repaintRequested = false;
  private loadedUrl: string | null = null;

  async ngOnChanges(): Promise<void> {
    if (this.meshUrl === null) {
      return;
    }

    const requestedUrl = this.meshUrl;
    try {
      if (this.gl === null) {
        this.initialize();
      }
      if (requestedUrl !== this.loadedUrl) {
        await this.loadScene(requestedUrl);
        this.loadedUrl = requestedUrl;
      }
      this.applyTangibleInterfaceSelection();
      this.animationController.start(this.model, () => this.draw());
      this.focusCanvas();
      this.draw();
    } catch (error) {
      this.statusMessage.set(
        error instanceof Error ? error.message : 'WebGL mesh example initialization failed',
      );
    }
  }

  ngOnDestroy(): void {
    this.animationController.stop();
    this.disconnectTangibleInterfaceClient();
    this.resizeObserver?.disconnect();
    if (this.gl !== null) {
      this.renderer.dispose(this.gl);
      this.gl = null;
    }
  }

  /**
   * Java's `EnvironmentPersistence.importEnvironment(file, model.getScene())`
   * followed by `model.configureInitialViewAndLightToScene()`. Java reports a
   * failed read on `System.err` and calls `System.exit(0)`; a module inside
   * the container cannot end the process, so the same failure is reported to
   * the user in the status overlay and the module stays up.
   */
  private async loadScene(meshUrl: string): Promise<void> {
    const scene: SimpleScene = this.model.getScene();
    scene.getSimpleBodies().clear();
    this.statusMessage.set('Loading ' + meshUrl + ' ...');

    try {
      await WebEnvironmentPersistence.importEnvironment(meshUrl, scene);
    } catch (error) {
      console.error('Failed to read file.');
      console.error(error instanceof Error ? error.message : String(error));
      this.statusMessage.set('Failed to read "' + meshUrl + '".');
      return;
    }

    if (scene.getSimpleBodies().isEmpty()) {
      this.statusMessage.set(
        'No geometry was imported from "' + meshUrl + '". Only the .obj format is ported so far.',
      );
      return;
    }

    this.model.configureInitialViewAndLightToScene();
    this.cameraController.adoptCameraState();
    this.statusMessage.set(null);
  }

  /**
   * Java's `main` sets the service URL from `-tangibleServer`, prints it,
   * builds the client, adds the techniques as its listener and calls `run()`,
   * once and unconditionally. Here the same steps run whenever the user's
   * selection changes, because the explorer dialog can be reopened on a live
   * module: an unchecked box closes the connection, and a new URL replaces it.
   *
   * `TangibleInterfaceNetworkClient.run()` opens a `WebSocket` instead of
   * blocking a thread, so nothing here waits for the server to answer.
   */
  private applyTangibleInterfaceSelection(): void {
    if (!this.tangibleEnabled) {
      this.disconnectTangibleInterfaceClient();
      return;
    }

    // As in Java, a blank value leaves the model its own default.
    this.model.setTangibleServiceUrl(this.tangibleServiceUrl);
    const serviceUrl = this.model.getTangibleServiceUrl();
    if (this.connectedTangibleServiceUrl === serviceUrl) {
      return;
    }

    this.disconnectTangibleInterfaceClient();
    console.log('Searching tangible interface server on ' + serviceUrl);

    this.tangibleInterfaceClient = new TangibleInterfaceNetworkClient(serviceUrl);
    this.tangibleInterfaceClient.addListener(
      new TangibleInterfaceInteractionTechniques(this.model, () => this.draw()),
    );
    this.tangibleInterfaceClient.run();
    this.connectedTangibleServiceUrl = serviceUrl;
  }

  /**
   * Java has no counterpart: its client lives as long as the JVM. A module
   * inside the container is torn down, and repointed, while the page lives on.
   */
  private disconnectTangibleInterfaceClient(): void {
    this.tangibleInterfaceClient?.disconnect();
    this.tangibleInterfaceClient = null;
    this.connectedTangibleServiceUrl = null;
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
   * Java asks for a `GL4` profile with 64 depth bits. The browser counterpart
   * is the availability of a WebGL2 context with a depth buffer; WebGL does
   * not let a page choose the depth-buffer precision.
   */
  private initialize(): void {
    const canvas = this.canvasRef.nativeElement;
    const gl = canvas.getContext('webgl2', { antialias: true, depth: true });
    if (gl === null) {
      throw new Error('MeshExample requires WebGL2 support.');
    }

    this.gl = gl;
    this.renderer.init(gl);
    this.resizeObserver = new ResizeObserver(() => this.draw());
    this.resizeObserver.observe(canvas);
  }

  private draw(): void {
    this.repaintRequested = true;
    if (this.rendering) return;

    void this.renderLoop().catch((error: unknown) => {
      this.statusMessage.set(
        error instanceof Error ? error.message : 'WebGL mesh example render failed',
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

    this.renderer.reshape(gl, 0, 0, width, height);
    await this.renderer.display(gl);
  }
}
