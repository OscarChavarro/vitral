import type { Entity, Light, Vector3Dd } from '@vitral/base';
import type { EntityTransformState } from './entity-transform-state';

/**
 * Port of `model.history.LightTransformState`.
 *
 * Position of a light (the placement the editor can change).
 */
export class LightTransformState implements EntityTransformState {
  private readonly light: Light;
  private readonly position: Vector3Dd | null;

  private constructor(light: Light) {
    this.light = light;
    this.position = light.getPosition();
  }

  /**
   * @param light light whose placement is captured
   * @return the current placement of the light
   */
  static capture(light: Light): LightTransformState {
    return new LightTransformState(light);
  }

  getEntity(): Entity {
    return this.light;
  }

  restore(): void {
    if (this.position !== null) {
      this.light.setPosition(this.position);
    }
  }

  isSameState(other: EntityTransformState): boolean {
    if (!(other instanceof LightTransformState)) {
      return false;
    }
    return (
      other.light === this.light &&
      (this.position === null ? other.position === null : this.position.equals(other.position))
    );
  }
}
