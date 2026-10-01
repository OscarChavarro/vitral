import { Logger, VSDK } from '@vitral/base';
import type { Scene } from '../scene';
import { SceneSnapshot } from './scene-snapshot';
import { UndoQueue } from './undo-queue';
import type { UndoableOperation } from './undoable-operation';

/**
 * Port of `model.history.SceneHistory`.
 *
 * Global undo/redo history of the scene: creation, deletion and placement
 * changes of bodies, lights, scene cameras and debug groups.
 *
 * User actions are recorded by bracketing them with `begin` and `end`: the
 * scene is captured at `begin` and compared at `end`, and what changed becomes
 * one operation of the queue. Brackets can be nested (i.e. a key pressed during
 * a mouse drag): only the outermost one records, so the whole gesture is a
 * single step.
 */
export class SceneHistory {
  private readonly sceneSource: () => Scene | null;
  private readonly queue: UndoQueue;
  private before: SceneSnapshot | null;
  private depth: number;

  /**
   * @param sceneSource gives the scene currently edited
   */
  constructor(sceneSource: () => Scene | null) {
    this.sceneSource = sceneSource;
    this.queue = new UndoQueue();
    this.before = null;
    this.depth = 0;
  }

  /**
   * @return the queue of operations done over the scene
   */
  getQueue(): UndoQueue {
    return this.queue;
  }

  /**
   * @return true if a user action is being recorded
   */
  isRecording(): boolean {
    return this.depth > 0;
  }

  /**
   * Starts recording a user action over the scene.
   */
  begin(): void {
    if (this.depth === 0) {
      const scene: Scene | null = this.sceneSource();

      this.before = scene === null ? null : SceneSnapshot.capture(scene);
    }
    this.depth++;
  }

  /**
   * Ends recording a user action. If it is the outermost one, what changed in
   * the scene since its `begin` is recorded as one operation.
   * @param name name of the action, to be shown to the user
   * @param mergeable true if its placement changes can be merged with the
   * ones of the next action over the same elements (see
   * `SceneTransformationOperation`)
   * @return true if an operation was recorded
   */
  end(name: string, mergeable: boolean): boolean {
    if (this.depth === 0) {
      Logger.reportMessage(
        this,
        VSDK.WARNING,
        'end',
        'Scene history end() without begin(): the action "' + name + '" is not recorded',
      );
      return false;
    }
    this.depth--;
    if (this.depth > 0) {
      return false;
    }
    const start: SceneSnapshot | null = this.before;
    this.before = null;
    const scene: Scene | null = this.sceneSource();
    if (start === null || scene === null || start.getScene() !== scene) {
      // The whole scene was replaced: the action can not be undone
      return false;
    }
    const operation: UndoableOperation | null = start.operationTo(SceneSnapshot.capture(scene), name, mergeable);
    if (operation === null) {
      return false;
    }
    this.queue.record(operation);
    return true;
  }

  /**
   * Executes an action recording it as one operation.
   * @param name name of the action
   * @param action what changes the scene
   * @return true if an operation was recorded
   */
  perform(name: string, action: () => void): boolean {
    this.begin();
    try {
      action();
    } catch (e) {
      // What the action did before failing is still recorded
      this.end(name, false);
      throw e;
    }
    return this.end(name, false);
  }

  /**
   * @return the undone operation, or null if there was nothing to undo or an
   * action is being recorded
   */
  undo(): UndoableOperation | null {
    if (this.isRecording()) {
      return null;
    }
    return this.queue.undo();
  }

  /**
   * @return the redone operation, or null if there was nothing to redo or an
   * action is being recorded
   */
  redo(): UndoableOperation | null {
    if (this.isRecording()) {
      return null;
    }
    return this.queue.redo();
  }

  /**
   * Forgets the history (i.e. when the scene is replaced).
   */
  clear(): void {
    this.queue.clear();
    this.before = null;
    this.depth = 0;
  }
}
