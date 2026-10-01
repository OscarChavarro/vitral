import type { Viewport } from '@vitral/base';
import type { UndoableOperation } from './undoable-operation';
import type { ViewportState } from './viewport-state';

/**
 * Port of `model.history.ViewportStateOperation`.
 *
 * Change of how a viewport shows the scene: camera movement, projection
 * location (perspective / parallel projections), render mode and display
 * settings. It keeps the states of the viewport before and after the change.
 *
 * Operations done by the keyboard over the same viewport in a short time can be
 * merged (see `absorb`), so an auto-repeated camera key is a single step.
 */
export class ViewportStateOperation implements UndoableOperation {
  /// Maximum time between two mergeable operations to be merged
  static readonly MERGE_INTERVAL_MILLISECONDS = 1000;

  private readonly name: string;
  private readonly before: ViewportState;
  private after: ViewportState;
  private readonly mergeable: boolean;
  private lastChangeTime: number;

  /**
   * @param name name of the user action
   * @param before state of the viewport before the change
   * @param after state of the same viewport after the change
   * @param mergeable true if following operations over the same viewport can
   * be merged into this one
   */
  constructor(name: string, before: ViewportState, after: ViewportState, mergeable: boolean) {
    this.name = name;
    this.before = before;
    this.after = after;
    this.mergeable = mergeable;
    this.lastChangeTime = Date.now();
  }

  /**
   * @return the changed viewport
   */
  getViewport(): Viewport {
    return this.before.getViewport();
  }

  undo(): void {
    this.before.restore();
  }

  redo(): void {
    this.after.restore();
  }

  getName(): string {
    return this.name;
  }

  absorb(next: UndoableOperation): boolean {
    if (
      !(next instanceof ViewportStateOperation) ||
      !this.mergeable ||
      !next.mergeable ||
      this.name !== next.name ||
      next.getViewport() !== this.getViewport() ||
      next.lastChangeTime - this.lastChangeTime > ViewportStateOperation.MERGE_INTERVAL_MILLISECONDS
    ) {
      return false;
    }
    this.after = next.after;
    this.lastChangeTime = next.lastChangeTime;
    return true;
  }
}
