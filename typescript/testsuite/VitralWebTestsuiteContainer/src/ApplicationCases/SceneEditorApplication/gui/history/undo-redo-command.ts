/**
 * Port of `gui.history.UndoRedoCommand`.
 *
 * Commands over the edition history: undo and redo over the global history of
 * the scene, or over the history of the view of the selected viewport. Java's
 * enum constants are the instances below.
 */
export class UndoRedoCommand {
  /** Ctrl+Z: undo the last operation over the scene */
  static readonly UNDO_SCENE = new UndoRedoCommand('UNDO_SCENE', false, true);
  /** Ctrl+Y: redo the last undone operation over the scene */
  static readonly REDO_SCENE = new UndoRedoCommand('REDO_SCENE', false, false);
  /** Ctrl+Shift+Z: undo the last change of the view of the selected viewport */
  static readonly UNDO_VIEWPORT = new UndoRedoCommand('UNDO_VIEWPORT', true, true);
  /** Ctrl+Shift+Y: redo the last undone change of the view of the selected viewport */
  static readonly REDO_VIEWPORT = new UndoRedoCommand('REDO_VIEWPORT', true, false);

  private constructor(
    private readonly constantName: string,
    private readonly viewportCommand: boolean,
    private readonly undoCommand: boolean,
  ) {}

  /**
   * @return Java's `Enum.name()`
   */
  name(): string {
    return this.constantName;
  }

  /**
   * @return true if the command works over the history of a viewport, false
   * if it works over the history of the scene
   */
  isViewportCommand(): boolean {
    return this.viewportCommand;
  }

  /**
   * @return true for undo, false for redo
   */
  isUndoCommand(): boolean {
    return this.undoCommand;
  }
}
