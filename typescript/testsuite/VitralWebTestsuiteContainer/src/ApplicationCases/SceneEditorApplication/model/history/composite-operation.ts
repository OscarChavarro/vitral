import type { UndoableOperation } from './undoable-operation';

/**
 * Port of `model.history.CompositeOperation`.
 *
 * Several operations done by a single user action (i.e. a command that
 * deletes some things and moves others), undone and redone as one step.
 */
export class CompositeOperation implements UndoableOperation {
  private readonly name: string;
  private readonly operations: UndoableOperation[];

  /**
   * @param name name of the user action
   * @param operations operations in the order they were done
   */
  constructor(name: string, operations: readonly UndoableOperation[]) {
    this.name = name;
    this.operations = [...operations];
  }

  undo(): void {
    for (let i = this.operations.length - 1; i >= 0; i--) {
      this.operations[i].undo();
    }
  }

  redo(): void {
    for (const operation of this.operations) {
      operation.redo();
    }
  }

  getName(): string {
    return this.name;
  }
}
