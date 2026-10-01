import { Logger, VSDK, type Viewport, type ViewportSet } from '@vitral/base';
import { UndoQueue } from './undo-queue';
import type { UndoableOperation } from './undoable-operation';
import { ViewportState } from './viewport-state';
import { ViewportStateOperation } from './viewport-state-operation';

/**
 * Port of `model.history.ViewportHistory`.
 *
 * Undo/redo histories of the views: one queue per viewport, holding the changes
 * of how that viewport shows the scene (see `ViewportStateOperation`).
 *
 * User actions are recorded as in `SceneHistory`: `begin` captures every
 * viewport of a viewport set and `end` records, in the queue of each viewport
 * that changed, one operation. Brackets can be nested; only the outermost one
 * records. Java's `IdentityHashMap`s are `Map`s, which compare object keys by
 * identity and keep insertion order.
 */
export class ViewportHistory {
  private readonly queues: Map<Viewport, UndoQueue>;
  private readonly before: Map<Viewport, ViewportState>;
  private depth: number;

  constructor() {
    this.queues = new Map<Viewport, UndoQueue>();
    this.before = new Map<Viewport, ViewportState>();
    this.depth = 0;
  }

  /**
   * @param viewport a viewport
   * @return the queue of operations done over the view of the viewport
   * (created empty the first time)
   */
  getQueue(viewport: Viewport): UndoQueue {
    let queue: UndoQueue | undefined = this.queues.get(viewport);
    if (queue === undefined) {
      queue = new UndoQueue();
      this.queues.set(viewport, queue);
    }
    return queue;
  }

  /**
   * @return true if a user action is being recorded
   */
  isRecording(): boolean {
    return this.depth > 0;
  }

  /**
   * Starts recording a user action over the viewports of a set.
   * @param viewportSet the viewports that the action can change
   */
  begin(viewportSet: ViewportSet | null): void {
    if (this.depth === 0) {
      this.before.clear();
      if (viewportSet !== null) {
        for (const viewport of viewportSet.getViewports()) {
          this.before.set(viewport, ViewportState.capture(viewport));
        }
      }
    }
    this.depth++;
  }

  /**
   * Ends recording a user action. If it is the outermost one, each viewport
   * whose view changed since `begin` gets one operation in its queue, named
   * after what changed (see `ViewportState.describeChangeTo`).
   * @param mergeable true if the operation can be merged with the one of the
   * next action over the same viewport
   * @return number of viewports whose change was recorded
   */
  end(mergeable: boolean): number {
    let recorded = 0;

    if (this.depth === 0) {
      Logger.reportMessage(this, VSDK.WARNING, 'end',
        'Viewport history end() without begin(): the action is not recorded');
      return 0;
    }
    this.depth--;
    if (this.depth > 0) {
      return 0;
    }
    for (const [viewport, start] of this.before) {
      const after: ViewportState = ViewportState.capture(viewport);

      if (!start.isSameState(after)) {
        this.getQueue(viewport).record(
          new ViewportStateOperation(start.describeChangeTo(after), start, after, mergeable),
        );
        recorded++;
      }
    }
    this.before.clear();
    return recorded;
  }

  /**
   * @param viewport a viewport
   * @return the undone operation, or null if there was nothing to undo or an
   * action is being recorded
   */
  undo(viewport: Viewport | null): UndoableOperation | null {
    if (viewport === null || this.isRecording()) {
      return null;
    }
    return this.getQueue(viewport).undo();
  }

  /**
   * @param viewport a viewport
   * @return the redone operation, or null if there was nothing to redo or an
   * action is being recorded
   */
  redo(viewport: Viewport | null): UndoableOperation | null {
    if (viewport === null || this.isRecording()) {
      return null;
    }
    return this.getQueue(viewport).redo();
  }

  /**
   * Forgets the histories of every viewport.
   */
  clear(): void {
    this.queues.clear();
    this.before.clear();
    this.depth = 0;
  }
}
