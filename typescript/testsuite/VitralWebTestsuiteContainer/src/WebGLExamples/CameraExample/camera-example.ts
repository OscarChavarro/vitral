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
import { Camera, Matrix4x4d } from '@vitral/base';
import {
  WebGLCameraRenderer,
  WebGLMatrixRenderer,
  WebGLSimpleCorridorSample,
  WebSystem,
} from '@vitral/webgl';
import { ExampleCameraInteraction } from '../_shared/example-camera-interaction';

@Component({
  selector: 'app-camera-example',
  templateUrl: './camera-example.html',
  styleUrl: './camera-example.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CameraExample implements AfterViewInit, OnDestroy {
  @ViewChild('canvas', { static: true })
  private readonly canvasRef!: ElementRef<HTMLCanvasElement>;

  @Output()
  readonly deactivate = new EventEmitter<void>();

  protected readonly statusMessage = signal<string | null>(null);

  private readonly camera = new Camera();
  private readonly cameraController = new ExampleCameraInteraction(this.camera);
  private readonly corridor = new WebGLSimpleCorridorSample();
  private gl: WebGL2RenderingContext | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private rendering = false;
  private repaintRequested = false;

  async ngAfterViewInit(): Promise<void> {
    try {
      this.initialize();
      this.focusCanvas();
      await this.prepareGlResources();
      this.draw();
    } catch (error) {
      this.statusMessage.set(
        error instanceof Error ? error.message : 'WebGL camera example initialization failed',
      );
    }
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    if (this.gl) {
      this.corridor.dispose(this.gl);
      WebGLCameraRenderer.dispose(this.gl);
      this.gl = null;
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

  private initialize(): void {
    const canvas = this.canvasRef.nativeElement;
    const gl = canvas.getContext('webgl2', { antialias: true });
    if (!gl) {
      throw new Error('This example requires WebGL2 support.');
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
   * The rest of what a JOGL program does in `init(GLAutoDrawable)`: the
   * corridor's shader program is compiled here, because a browser reaches a
   * GLSL source over `fetch` and a frame that awaited one would be presented
   * and cleared halfway through.
   */
  private async prepareGlResources(): Promise<void> {
    const gl = this.gl;
    if (!gl) return;
    await this.corridor.prepare(gl);
  }

  private draw(): void {
    this.repaintRequested = true;
    if (this.rendering) return;

    void this.renderLoop().catch((error: unknown) => {
      this.statusMessage.set(
        error instanceof Error ? error.message : 'WebGL camera example render failed',
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

    this.camera.updateViewportResize(width, height);

    const gl = this.gl;
    gl.viewport(0, 0, width, height);
    gl.clearColor(0, 0, 0, 1);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
    gl.enable(gl.DEPTH_TEST);

    const projection = WebGLCameraRenderer.activate(gl, this.camera);
    await this.corridor.drawGL(gl, projection);
    WebGLMatrixRenderer.draw(gl, projection, Matrix4x4d.identityMatrix());
  }
}
