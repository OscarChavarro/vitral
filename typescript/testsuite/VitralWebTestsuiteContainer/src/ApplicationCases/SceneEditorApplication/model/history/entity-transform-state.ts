import type { Entity } from '@vitral/base';

/**
 * Port of `model.history.EntityTransformState`.
 *
 * Captured placement of one entity of a scene (a body, a light or a camera),
 * which can be given back to the entity later (Memento pattern). Scene elements
 * are vitral `Entity`s, which give each port of the toolkit the same base for
 * identity and serialization.
 */
export interface EntityTransformState {
  /**
   * @return the entity whose placement was captured
   */
  getEntity(): Entity;

  /**
   * Gives back the captured placement to the entity.
   */
  restore(): void;

  /**
   * @param other state captured from the same entity
   * @return true if both states are the same placement
   */
  isSameState(other: EntityTransformState): boolean;
}
