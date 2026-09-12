import { Camera, Matrix4x4d, Vector3Dd } from '@vitral/base';
import { WebSystem, type WebKeyEvent, type WebMouseEvent } from '@vitral/webgl';

/**
 * Pointer and keyboard camera handling shared by the WebGL example modules.
 *
 * The Java examples under `java/testsuite/Jogl4Examples` delegate this to a
 * `vsdk.toolkit.gui.CameraController` implementation, `CameraControllerAquynza`
 * in every case ported so far. That family lives in `61_gui.txt`, the source
 * group of the plan's decision-gated Phase 38, and is not ported yet; until it
 * is, the browser examples share this adapter, which keeps the `processX(...)`
 * shape of the Java `CameraController` contract — each handler answers whether
 * the view needs to be repainted — so that swapping in the ported controller
 * later is a change of construction, not of the example modules.
 */
export class ExampleCameraInteraction {
  private dragging = false;
  private lastPointerX = 0;
  private lastPointerY = 0;
  private orbitAzimuth = 0;
  private orbitElevation = 0.2;
  private orbitRadius = 5.5;
  private minOrbitRadius = 2.0;
  private maxOrbitRadius = 30.0;
  private focus = new Vector3Dd(0, 0, 1.2);
  private readonly deltaMovement = 0.25;

  constructor(private readonly camera: Camera) {
    this.updateCamera();
  }

  /**
   * Takes the camera's current position and focus as the orbit state, instead
   * of overwriting them on the next interaction.
   *
   * The examples whose scene is a fixture compiled into the program start from
   * this adapter's own default view, which is why the constructor imposes one.
   * A program that frames its own scene first — `MeshExample`, which calls
   * `MeshModel.configureInitialViewAndLightToScene()` after reading a mesh —
   * needs the opposite, and this is where the two meet. The zoom limits move
   * with the adopted distance, since a fixed 2..30 range means nothing for a
   * mesh of arbitrary size.
   */
  adoptCameraState(): void {
    this.focus = this.camera.getFocusedPosition();
    const offset = this.camera.getPosition().subtract(this.focus);
    this.orbitRadius = offset.length();
    if (this.orbitRadius < 1e-6) {
      this.orbitRadius = 1.0;
    }
    this.minOrbitRadius = this.orbitRadius / 100.0;
    this.maxOrbitRadius = this.orbitRadius * 100.0;

    const horizontalRadius = Math.hypot(offset.x(), offset.y());
    this.orbitElevation = Math.atan2(offset.z(), horizontalRadius);
    this.orbitAzimuth = Math.atan2(offset.x(), -offset.y());
  }

  processPointerDown(event: PointerEvent, canvas: HTMLCanvasElement): boolean {
    const vitralEvent = WebSystem.web2vsdkMouseEvent(event, canvas);
    this.lastPointerX = vitralEvent.x;
    this.lastPointerY = vitralEvent.y;
    this.dragging = true;
    return false;
  }

  processPointerMove(event: PointerEvent, canvas: HTMLCanvasElement): boolean {
    if (!this.dragging) return false;
    const vitralEvent = WebSystem.web2vsdkMouseEvent(event, canvas);
    const dx = vitralEvent.x - this.lastPointerX;
    const dy = vitralEvent.y - this.lastPointerY;
    this.lastPointerX = vitralEvent.x;
    this.lastPointerY = vitralEvent.y;
    this.orbitAzimuth -= dx * 0.006;
    this.orbitElevation = this.clamp(this.orbitElevation + dy * 0.006, -1.2, 1.2);
    this.updateCamera();
    return true;
  }

  processPointerUp(): boolean {
    this.dragging = false;
    return false;
  }

  processWheel(event: WheelEvent, canvas: HTMLCanvasElement): boolean {
    const vitralEvent: WebMouseEvent = WebSystem.web2vsdkWheelEvent(event, canvas);
    this.orbitRadius = this.clamp(
      this.orbitRadius * (1 + vitralEvent.clicks * 0.001),
      this.minOrbitRadius,
      this.maxOrbitRadius,
    );
    this.updateCamera();
    return true;
  }

  processKeyPressed(vitralEvent: WebKeyEvent): boolean {
    return this.processCameraKeyPressed(vitralEvent.keycode);
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

  private clamp(value: number, min: number, max: number): number {
    return Math.max(min, Math.min(max, value));
  }
}
