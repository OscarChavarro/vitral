import type { Camera, Entity, Vector3Dd } from '@vitral/base';
import type { EntityTransformState } from './entity-transform-state';

/**
 * Port of `model.history.CameraState`.
 *
 * Placement and projection of a camera: eye position, reference frame (up,
 * front, left), focused point, projection mode, field of view, orthogonal zoom
 * and clipping planes. It is restored through the setters of `Camera`, so
 * whoever caches data derived from the camera sees it modified. The size of the
 * viewport the camera projects to is not part of the state: it belongs to the
 * viewport.
 *
 * Vectors are compared with a tolerance: `Camera.updateVectors`, called each
 * time a camera is drawn, normalizes its frame again, which changes the last
 * bits of vectors nobody moved.
 */
export class CameraState implements EntityTransformState {
  /// Relative tolerance for vectors to be the same
  private static readonly VECTOR_TOLERANCE = 1.0e-9;

  private readonly camera: Camera;
  private readonly position: Vector3Dd;
  private readonly up: Vector3Dd;
  private readonly front: Vector3Dd;
  private readonly left: Vector3Dd;
  private readonly focusedPosition: Vector3Dd;
  private readonly projectionMode: number;
  private readonly fov: number;
  private readonly orthogonalZoom: number;
  private readonly nearPlaneDistance: number;
  private readonly farPlaneDistance: number;

  private constructor(camera: Camera) {
    this.camera = camera;
    this.position = camera.getPosition();
    this.up = camera.getUp();
    this.front = camera.getFront();
    this.left = camera.getLeft();
    this.focusedPosition = camera.getFocusedPosition();
    this.projectionMode = camera.getProjectionMode();
    this.fov = camera.getFov();
    this.orthogonalZoom = camera.getOrthogonalZoom();
    this.nearPlaneDistance = camera.getNearPlaneDistance();
    this.farPlaneDistance = camera.getFarPlaneDistance();
  }

  /**
   * @param camera camera whose state is captured
   * @return the current state of the camera
   */
  static capture(camera: Camera): CameraState {
    return new CameraState(camera);
  }

  getEntity(): Entity {
    return this.camera;
  }

  restore(): void {
    this.camera.setPosition(this.position);
    // Gives back the front direction and the focal distance...
    this.camera.setFocusedPositionDirect(this.focusedPosition);
    // ... and the exact frame, which is not recomputed from the former
    this.camera.setUpDirect(this.up);
    this.camera.setLeftDirect(this.left);
    this.camera.setProjectionMode(this.projectionMode);
    this.camera.setFov(this.fov);
    this.camera.setOrthogonalZoom(this.orthogonalZoom);
    this.camera.setNearPlaneDistance(this.nearPlaneDistance);
    this.camera.setFarPlaneDistance(this.farPlaneDistance);
    this.camera.updateVectors();
  }

  isSameState(other: EntityTransformState): boolean {
    if (!(other instanceof CameraState)) {
      return false;
    }
    // Java's `Double.compare(a, b) == 0`: NaN equals NaN, and 0.0 differs
    // from -0.0, which is what `Object.is` tells
    return (
      other.camera === this.camera &&
      CameraState.sameVector(other.position, this.position) &&
      CameraState.sameVector(other.up, this.up) &&
      CameraState.sameVector(other.front, this.front) &&
      CameraState.sameVector(other.left, this.left) &&
      CameraState.sameVector(other.focusedPosition, this.focusedPosition) &&
      other.projectionMode === this.projectionMode &&
      Object.is(other.fov, this.fov) &&
      Object.is(other.orthogonalZoom, this.orthogonalZoom) &&
      Object.is(other.nearPlaneDistance, this.nearPlaneDistance) &&
      Object.is(other.farPlaneDistance, this.farPlaneDistance)
    );
  }

  private static sameVector(a: Vector3Dd, b: Vector3Dd): boolean {
    const scale: number = Math.max(1.0, Math.max(a.length(), b.length()));

    return a.subtract(b).length() <= CameraState.VECTOR_TOLERANCE * scale;
  }
}
