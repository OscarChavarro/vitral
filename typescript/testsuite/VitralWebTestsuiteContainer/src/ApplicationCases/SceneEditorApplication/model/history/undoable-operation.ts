/**
 * Port of `model.history.UndoableOperation`.
 *
 * An edition done by the user that can be undone and done again (Command
 * pattern). Operations are kept, in the order they were done, by an
 * `UndoQueue`. They are plain model objects: they do not depend on any GUI or
 * rendering technology.
 *
 * Java's `default` method `absorb` is optional here: an operation without it
 * absorbs nothing (see `absorbInto`).
 */
export interface UndoableOperation {
  /**
   * Reverts the effect of the operation.
   */
  undo(): void;

  /**
   * Applies again the effect of the operation, after an `undo`.
   */
  redo(): void;

  /**
   * @return short name of the operation, to be shown to the user
   */
  getName(): string;

  /**
   * Absorbs an operation recorded just after this one, so both become a single
   * step of the history (i.e. the key presses of an auto-repeated key moving
   * the same camera). By default operations absorb nothing.
   * @param next operation recorded just after this one
   * @return true if `next` was absorbed into this operation, so it must not be
   * recorded
   */
  absorb?(next: UndoableOperation): boolean;
}

/**
 * Java's default `UndoableOperation.absorb`.
 * @param operation operation that may absorb the next one
 * @param next operation recorded just after it
 * @return true if `next` was absorbed
 */
export function absorbInto(operation: UndoableOperation, next: UndoableOperation): boolean {
  return operation.absorb !== undefined ? operation.absorb(next) : false;
}
