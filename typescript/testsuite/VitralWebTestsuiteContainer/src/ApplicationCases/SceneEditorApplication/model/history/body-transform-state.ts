import type { Entity, Matrix4x4d, SimpleBody, Vector3Dd } from '@vitral/base';
import type { EntityTransformState } from './entity-transform-state';

/**
 * Port of `model.history.BodyTransformState`.
 *
 * Position, orientation and scale of a body. Vectors and matrices of vitral are
 * immutable, so they are kept without copying them.
 */
export class BodyTransformState implements EntityTransformState {
  private readonly body: SimpleBody;
  private readonly position: Vector3Dd;
  private readonly rotation: Matrix4x4d | null;
  private readonly scale: Vector3Dd;

  private constructor(body: SimpleBody) {
    this.body = body;
    this.position = body.getPosition();
    this.rotation = body.getRotation();
    this.scale = body.getScale();
  }

  /**
   * @param body body whose placement is captured
   * @return the current placement of the body
   */
  static capture(body: SimpleBody): BodyTransformState {
    return new BodyTransformState(body);
  }

  getEntity(): Entity {
    return this.body;
  }

  restore(): void {
    this.body.setScale(this.scale);
    if (this.rotation !== null) {
      // The inverse rotation is derived from this one
      this.body.setRotation(this.rotation);
    }
    this.body.setPosition(this.position);
  }

  isSameState(other: EntityTransformState): boolean {
    if (!(other instanceof BodyTransformState)) {
      return false;
    }
    return (
      other.body === this.body &&
      BodyTransformState.equal(other.position, this.position) &&
      BodyTransformState.equal(other.rotation, this.rotation) &&
      BodyTransformState.equal(other.scale, this.scale)
    );
  }

  private static equal(
    a: { equals(other: unknown): boolean } | null,
    b: { equals(other: unknown): boolean } | null,
  ): boolean {
    return a === null ? b === null : a.equals(b);
  }
}
