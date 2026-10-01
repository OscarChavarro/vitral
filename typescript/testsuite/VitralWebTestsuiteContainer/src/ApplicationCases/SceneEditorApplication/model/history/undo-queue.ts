import { absorbInto, type UndoableOperation } from './undoable-operation';

/**
 * Port of `model.history.UndoQueue`.
 *
 * History of the operations done over something (the scene, or the view of a
 * viewport), to undo and redo them. It is a list with a cursor: the operations
 * before the cursor are done (the last one is the next to undo), the ones after
 * it were undone (the first one is the next to redo). Recording a new operation
 * discards the undone ones, and the oldest operations are forgotten when the
 * capacity is exceeded.
 */
export class UndoQueue {
  /// Default maximum number of operations kept
  static readonly DEFAULT_CAPACITY = 256;

  private readonly operations: UndoableOperation[];
  private readonly capacity: number;
  /// Number of operations currently done
  private cursor: number;

  /**
   * @param capacity maximum number of operations kept (at least 1)
   */
  constructor(capacity: number = UndoQueue.DEFAULT_CAPACITY) {
    this.operations = [];
    this.capacity = Math.max(1, capacity);
    this.cursor = 0;
  }

  /**
   * Adds an operation already done by the user. The undone operations are
   * discarded: a new edition starts a new branch of the history.
   * @param operation operation to record; null is ignored
   */
  record(operation: UndoableOperation | null): void {
    if (operation === null) {
      return;
    }
    while (this.operations.length > this.cursor) {
      this.operations.pop();
    }
    if (this.cursor > 0 && absorbInto(this.operations[this.cursor - 1], operation)) {
      return;
    }
    this.operations.push(operation);
    this.cursor++;
    while (this.operations.length > this.capacity) {
      this.operations.shift();
      this.cursor--;
    }
  }

  canUndo(): boolean {
    return this.cursor > 0;
  }

  canRedo(): boolean {
    return this.cursor < this.operations.length;
  }

  /**
   * Reverts the last done operation.
   * @return the undone operation, or null if there was nothing to undo
   */
  undo(): UndoableOperation | null {
    if (!this.canUndo()) {
      return null;
    }
    this.cursor--;
    const operation: UndoableOperation = this.operations[this.cursor];
    operation.undo();
    return operation;
  }

  /**
   * Applies again the last undone operation.
   * @return the redone operation, or null if there was nothing to redo
   */
  redo(): UndoableOperation | null {
    if (!this.canRedo()) {
      return null;
    }
    const operation: UndoableOperation = this.operations[this.cursor];
    operation.redo();
    this.cursor++;
    return operation;
  }

  /**
   * @return the name of the next operation to undo, or null if there is none
   */
  getUndoName(): string | null {
    return this.canUndo() ? this.operations[this.cursor - 1].getName() : null;
  }

  /**
   * @return the name of the next operation to redo, or null if there is none
   */
  getRedoName(): string | null {
    return this.canRedo() ? this.operations[this.cursor].getName() : null;
  }

  /**
   * @return number of operations that can be undone
   */
  getUndoCount(): number {
    return this.cursor;
  }

  /**
   * @return number of operations that can be redone
   */
  getRedoCount(): number {
    return this.operations.length - this.cursor;
  }

  /**
   * Forgets every operation.
   */
  clear(): void {
    this.operations.length = 0;
    this.cursor = 0;
  }
}
