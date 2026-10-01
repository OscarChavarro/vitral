import type { EntityTransformState } from './entity-transform-state';
import type { UndoableOperation } from './undoable-operation';

/**
 * Port of `model.history.SceneTransformationOperation`.
 *
 * Change of the placement (translation, rotation, scale) of some elements of a
 * scene: bodies, lights or cameras. It keeps the placements before and after
 * the change of each changed element.
 *
 * Operations done by the keyboard over the same elements in a short time can
 * be merged (see `absorb`), so an auto-repeated key is a single step.
 */
export class SceneTransformationOperation implements UndoableOperation {
  /// Maximum time between two mergeable operations to be merged
  static readonly MERGE_INTERVAL_MILLISECONDS = 1000;

  private readonly name: string;
  private readonly before: EntityTransformState[];
  private after: EntityTransformState[];
  private readonly mergeable: boolean;
  private lastChangeTime: number;

  /**
   * @param name name of the user action
   * @param before placements before the change, one per changed element
   * @param after placements after the change, in the same order
   * @param mergeable true if following operations over the same elements
   * can be merged into this one
   */
  constructor(
    name: string,
    before: readonly EntityTransformState[],
    after: readonly EntityTransformState[],
    mergeable: boolean,
  ) {
    this.name = name;
    this.before = [...before];
    this.after = [...after];
    this.mergeable = mergeable;
    this.lastChangeTime = Date.now();
  }

  undo(): void {
    for (const state of this.before) {
      state.restore();
    }
  }

  redo(): void {
    for (const state of this.after) {
      state.restore();
    }
  }

  getName(): string {
    return this.name;
  }

  /**
   * @return number of entities whose placement changed
   */
  getEntityCount(): number {
    return this.before.length;
  }

  absorb(next: UndoableOperation): boolean {
    if (
      !(next instanceof SceneTransformationOperation) ||
      !this.mergeable ||
      !next.mergeable ||
      this.name !== next.name ||
      next.lastChangeTime - this.lastChangeTime > SceneTransformationOperation.MERGE_INTERVAL_MILLISECONDS ||
      !this.sameElements(next)
    ) {
      return false;
    }
    this.after = next.after;
    this.lastChangeTime = next.lastChangeTime;
    return true;
  }

  private sameElements(other: SceneTransformationOperation): boolean {
    if (other.before.length !== this.before.length) {
      return false;
    }
    for (let i = 0; i < this.before.length; i++) {
      if (other.before[i].getEntity() !== this.before[i].getEntity()) {
        return false;
      }
    }
    return true;
  }
}
