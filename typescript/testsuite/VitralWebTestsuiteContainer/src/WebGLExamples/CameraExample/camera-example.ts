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
import { Camera, Matrix4x4d, Vector3Dd } from '@vitral/base';
import {
  WebGLCameraRenderer,
  WebGLMatrixRenderer,
  WebGLSimpleCorridorSample,
  WebSystem,
} from '@vitral/webgl';

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
  private readonly corridor = new WebGLSimpleCorridorSample();
  private gl: WebGL2RenderingContext | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private dragging = false;
  private lastPointerX = 0;
  private lastPointerY = 0;
  private rendering = false;
  private repaintRequested = false;
  private orbitAzimuth = 0;
  private orbitElevation = 0.2;
  private orbitRadius = 5.5;
  private focus = new Vector3Dd(0, 0, 1.2);
  private deltaMovement = 0.25;

  async ngAfterViewInit(): Promise<void> {
    try {
      this.initialize();
      this.focusCanvas();
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
    const vitralEvent = WebSystem.web2vsdkMouseEvent(event, this.canvasRef.nativeElement);
    this.lastPointerX = vitralEvent.x;
    this.lastPointerY = vitralEvent.y;
    this.dragging = true;
  }

  protected onPointerMove(event: PointerEvent): void {
    if (!this.dragging) return;
    const vitralEvent = WebSystem.web2vsdkMouseEvent(event, this.canvasRef.nativeElement);
    const dx = vitralEvent.x - this.lastPointerX;
    const dy = vitralEvent.y - this.lastPointerY;
    this.lastPointerX = vitralEvent.x;
    this.lastPointerY = vitralEvent.y;
    this.orbitAzimuth -= dx * 0.006;
    this.orbitElevation = this.clamp(this.orbitElevation + dy * 0.006, -1.2, 1.2);
    this.updateCamera();
    this.draw();
  }

  protected onPointerUp(event: PointerEvent): void {
    this.dragging = false;
    if (this.canvasRef.nativeElement.hasPointerCapture(event.pointerId)) {
      this.canvasRef.nativeElement.releasePointerCapture(event.pointerId);
    }
  }

  protected onWheel(event: WheelEvent): void {
    event.preventDefault();
    const vitralEvent = WebSystem.web2vsdkWheelEvent(event, this.canvasRef.nativeElement);
    this.orbitRadius = this.clamp(this.orbitRadius * (1 + vitralEvent.clicks * 0.001), 2.0, 30.0);
    this.updateCamera();
    this.draw();
  }

  protected onKeyDown(event: KeyboardEvent): void {
    const vitralEvent = WebSystem.web2vsdkKeyEvent(event);

    if (vitralEvent.keycode === 'KEY_ESC') {
      event.preventDefault();
      event.stopPropagation();
      this.deactivate.emit();
      return;
    }

    if (!this.processCameraKeyPressed(vitralEvent.keycode)) {
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
    this.updateCamera();
  }

  private updateCamera(): void {
    const horizontalRadius = this.orbitRadius * Math.cos(this.orbitElevation);
    const position = new Vector3Dd(
      this.focus.x() + horizontalRadius * Math.sin(this.orbitAzimuth),
      this.focus.y() - horizontalRadius * Math.cos(this.orbitAzimuth),
      this.focus.z() + this.orbitRadius * Math.sin(this.orbitElevation),
    );
    this.camera.setPosition(position);
    this.camera.setFocusedPositionMaintainingOrthogonality(this.focus);
  }

  private processCameraKeyPressed(keycode: string): boolean {
    let eyePosition = this.camera.getPosition();
    let focusedPosition = this.camera.getFocusedPosition();
    let rotation = this.camera.getRotation();
    let projectionMode = this.camera.getProjectionMode();
    let fov = this.camera.getFov();
    let orthogonalZoom = this.camera.getOrthogonalZoom();
    let nearPlaneDistance = this.camera.getNearPlaneDistance();
    let farPlaneDistance = this.camera.getFarPlaneDistance();

    let yaw = rotation.obtainEulerYawAngle();
    let pitch = rotation.obtainEulerPitchAngle();
    let roll = rotation.obtainEulerRollAngle();
    const angleInc = this.angleIncrementForFov(fov);
    const epsilon = 0.0001;
    let updated = false;

    switch (keycode) {
      case 'KEY_UP':
        pitch = Math.max(pitch - angleInc, this.degreesToRadians(-90));
        updated = true;
        break;
      case 'KEY_DOWN':
        pitch = Math.min(pitch + angleInc, this.degreesToRadians(90));
        updated = true;
        break;
      case 'KEY_LEFT':
        yaw += angleInc;
        while (yaw >= this.degreesToRadians(360)) yaw -= this.degreesToRadians(360);
        updated = true;
        break;
      case 'KEY_RIGHT':
        yaw -= angleInc;
        while (yaw < 0) yaw += this.degreesToRadians(360);
        updated = true;
        break;

      case 'KEY_x':
        eyePosition = eyePosition.withX(eyePosition.x() - this.deltaMovement);
        focusedPosition = focusedPosition.withX(focusedPosition.x() - this.deltaMovement);
        updated = true;
        break;
      case 'KEY_X':
        eyePosition = eyePosition.withX(eyePosition.x() + this.deltaMovement);
        focusedPosition = focusedPosition.withX(focusedPosition.x() + this.deltaMovement);
        updated = true;
        break;
      case 'KEY_y':
        eyePosition = eyePosition.withY(eyePosition.y() - this.deltaMovement);
        focusedPosition = focusedPosition.withY(focusedPosition.y() - this.deltaMovement);
        updated = true;
        break;
      case 'KEY_Y':
        eyePosition = eyePosition.withY(eyePosition.y() + this.deltaMovement);
        focusedPosition = focusedPosition.withY(focusedPosition.y() + this.deltaMovement);
        updated = true;
        break;
      case 'KEY_z':
        eyePosition = eyePosition.withZ(eyePosition.z() - this.deltaMovement);
        focusedPosition = focusedPosition.withZ(focusedPosition.z() - this.deltaMovement);
        updated = true;
        break;
      case 'KEY_Z':
        eyePosition = eyePosition.withZ(eyePosition.z() + this.deltaMovement);
        focusedPosition = focusedPosition.withZ(focusedPosition.z() + this.deltaMovement);
        updated = true;
        break;

      case 'KEY_S':
        roll -= this.degreesToRadians(5);
        while (roll < 0) roll += this.degreesToRadians(360);
        updated = true;
        break;
      case 'KEY_s':
        roll += this.degreesToRadians(5);
        while (roll > this.degreesToRadians(360)) roll -= this.degreesToRadians(360);
        updated = true;
        break;

      case 'KEY_A':
        if (projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL) {
          orthogonalZoom /= 2;
        } else if (fov < 0.1 - epsilon) {
          fov += 0.1;
        } else if (fov < 1 - epsilon) {
          fov++;
        } else if (fov < 175 - epsilon) {
          fov += 5;
        }
        updated = true;
        break;
      case 'KEY_a':
        if (projectionMode === Camera.PROJECTION_MODE_ORTHOGONAL) {
          orthogonalZoom *= 2;
        } else if (fov > 5 + epsilon) {
          fov -= 5;
        } else if (fov > 1 + epsilon) {
          fov--;
        } else if (fov > 0.1 + epsilon) {
          fov -= 0.1;
        }
        updated = true;
        break;

      case 'KEY_N':
        nearPlaneDistance = this.augmentLogarithmic(nearPlaneDistance, epsilon);
        updated = true;
        break;
      case 'KEY_n':
        nearPlaneDistance = this.diminishLogarithmic(nearPlaneDistance, epsilon);
        updated = true;
        break;

      case 'KEY_F':
        farPlaneDistance = this.augmentLogarithmic(farPlaneDistance, epsilon);
        updated = true;
        break;
      case 'KEY_f':
        farPlaneDistance = this.diminishLogarithmic(farPlaneDistance, epsilon);
        updated = true;
        break;

      case 'KEY_p':
        projectionMode =
          projectionMode === Camera.PROJECTION_MODE_PERSPECTIVE
            ? Camera.PROJECTION_MODE_ORTHOGONAL
            : Camera.PROJECTION_MODE_PERSPECTIVE;
        updated = true;
        break;

      case 'KEY_i':
        console.info(this.camera.toString());
        return false;
    }

    if (!updated) {
      return false;
    }

    rotation = new Matrix4x4d().eulerAnglesRotation(yaw, pitch, roll);
    this.camera.setPosition(eyePosition);
    this.camera.setFocusedPositionMaintainingOrthogonality(focusedPosition);
    this.camera.setRotation(rotation);
    this.camera.setOrthogonalZoom(orthogonalZoom);
    this.camera.setFov(fov);
    this.camera.setProjectionMode(projectionMode);
    this.camera.setNearPlaneDistance(nearPlaneDistance);
    this.camera.setFarPlaneDistance(farPlaneDistance);
    this.syncOrbitFromCamera();
    return true;
  }

  private syncOrbitFromCamera(): void {
    const position = this.camera.getPosition();
    this.focus = this.camera.getFocusedPosition();
    const offset = position.subtract(this.focus);
    const radius = offset.length();
    if (radius <= 0.0001) {
      return;
    }
    this.orbitRadius = radius;
    this.orbitElevation = Math.asin(this.clamp(offset.z() / radius, -1, 1));
    this.orbitAzimuth = Math.atan2(offset.x(), -offset.y());
  }

  private angleIncrementForFov(fov: number): number {
    if (fov > 90) return this.degreesToRadians(10);
    if (fov > 45) return this.degreesToRadians(5);
    if (fov > 15) return this.degreesToRadians(2.5);
    if (fov > 5) return this.degreesToRadians(1);
    return this.degreesToRadians(0.1);
  }

  private augmentLogarithmic(value: number, epsilon: number): number {
    if (value < 0.001) return value + 0.0001;
    if (value < 0.01) return value + 0.001;
    if (value < 0.1 - epsilon) return value + 0.01;
    if (value < 1 - epsilon) return value + 0.1;
    if (value < 10 - epsilon) return value + 1;
    if (value < 100 - epsilon) return value + 10;
    if (value < 1000 - epsilon) return value + 100;
    if (value < 10000 - epsilon) return value + 1000;
    if (value < 100000 - epsilon) return value + 10000;
    if (value < 1000000 - epsilon) return value + 100000;
    if (value < 10000000 - epsilon) return value * 2;
    return 10000000;
  }

  private diminishLogarithmic(value: number, epsilon: number): number {
    if (value > 10000000 + epsilon) return value / 2;
    if (value > 1000000 + epsilon) return value - 1000000;
    if (value > 100000 + epsilon) return value - 100000;
    if (value > 10000 + epsilon) return value - 10000;
    if (value > 1000 + epsilon) return value - 1000;
    if (value > 100 + epsilon) return value - 100;
    if (value > 10 + epsilon) return value - 10;
    if (value > 1 + epsilon) return value - 1;
    if (value > 0.1 + epsilon) return value - 0.1;
    if (value > 0.01 + epsilon) return value - 0.01;
    if (value > 0.001 + epsilon) return value - 0.001;
    if (value > 0.0001 + epsilon) return value - 0.0001;
    return 0.0001;
  }

  private degreesToRadians(degrees: number): number {
    return (degrees * Math.PI) / 180;
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

  private clamp(value: number, min: number, max: number): number {
    return Math.max(min, Math.min(max, value));
  }
}
