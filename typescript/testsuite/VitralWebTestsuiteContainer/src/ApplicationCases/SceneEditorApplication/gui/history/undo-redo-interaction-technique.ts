import { KeyEvent, type Viewport } from '@vitral/base';
import type { EditHistory } from '../../model/history/edit-history';
import type { UndoQueue } from '../../model/history/undo-queue';
import type { UndoableOperation } from '../../model/history/undoable-operation';
import { UndoRedoCommand } from './undo-redo-command';

/**
 * What an undo or redo command did (Java's record
 * `UndoRedoInteractionTechnique.Result`).
 */
export class UndoRedoResult {
  /**
   * @param commandValue the executed command
   * @param operationValue the undone or redone operation, or null if there was
   * nothing to undo or redo
   * @param messageValue description of the result, for the user
   */
  constructor(
    private readonly commandValue: UndoRedoCommand,
    private readonly operationValue: UndoableOperation | null,
    private readonly messageValue: string,
  ) {}

  command(): UndoRedoCommand {
    return this.commandValue;
  }

  operation(): UndoableOperation | null {
    return this.operationValue;
  }

  message(): string {
    return this.messageValue;
  }

  /**
   * @return true if an operation was undone or redone
   */
  isDone(): boolean {
    return this.operationValue !== null;
  }
}

/**
 * Port of `gui.history.UndoRedoInteractionTechnique`.
 *
 * Keyboard interaction technique for undo and redo. It processes only vitral
 * events, so callers must convert the events of the GUI technology in use
 * before (the key of a Ctrl chord must be in `KeyEvent.keycode`, since its
 * character is a control one; see `gui/html/html-key-event-mapper.ts`):
 *
 * - `Ctrl+Z` / `Ctrl+Y`: undo / redo over the global history of the scene
 *   (creation, deletion and transformation of bodies, lights and cameras).
 * - `Ctrl+Shift+Z` / `Ctrl+Shift+Y`: undo / redo over the history of the view
 *   of the selected viewport (camera placement, projection, display settings).
 */
export class UndoRedoInteractionTechnique {
  /// Control characters of Ctrl+Z and Ctrl+Y, as GUI technologies report them
  private static readonly CONTROL_Z = 0x1a;
  private static readonly CONTROL_Y = 0x19;

  private readonly history: EditHistory;

  /**
   * @param history edition history to undo and redo
   */
  constructor(history: EditHistory) {
    this.history = history;
  }

  /**
   * @param event key press
   * @return the undo/redo command of the key chord, or null if it is not one
   */
  static recognize(event: KeyEvent | null): UndoRedoCommand | null {
    if (
      event === null ||
      (event.modifierMask & KeyEvent.MASK_CTRL) === 0 ||
      (event.modifierMask & KeyEvent.MASK_ALT) !== 0
    ) {
      return null;
    }
    const shift: boolean = (event.modifierMask & KeyEvent.MASK_SHIFT) !== 0;
    let z: boolean = event.keycode === KeyEvent.KEY_z || event.keycode === KeyEvent.KEY_Z;
    let y: boolean = event.keycode === KeyEvent.KEY_y || event.keycode === KeyEvent.KEY_Y;
    if (event.keycode === KeyEvent.KEY_NONE) {
      // Technologies that do not give the key of Ctrl chords
      z = event.unicodeId === UndoRedoInteractionTechnique.CONTROL_Z;
      y = event.unicodeId === UndoRedoInteractionTechnique.CONTROL_Y;
    }
    if (z) {
      return shift ? UndoRedoCommand.UNDO_VIEWPORT : UndoRedoCommand.UNDO_SCENE;
    }
    if (y) {
      return shift ? UndoRedoCommand.REDO_VIEWPORT : UndoRedoCommand.REDO_SCENE;
    }
    return null;
  }

  /**
   * Executes the undo/redo command of a key press, if it is one.
   * @param event key press
   * @param viewport the selected viewport, whose view history is used by the
   * viewport commands
   * @return what the command did, or null if the key is not an undo/redo one
   */
  processKeyPressedEvent(event: KeyEvent, viewport: Viewport | null): UndoRedoResult | null {
    const command: UndoRedoCommand | null = UndoRedoInteractionTechnique.recognize(event);

    if (command === null) {
      return null;
    }
    return this.execute(command, viewport);
  }

  /**
   * @param command command to execute
   * @param viewport the selected viewport, whose view history is used by the
   * viewport commands
   * @return what the command did
   */
  execute(command: UndoRedoCommand, viewport: Viewport | null): UndoRedoResult {
    let operation: UndoableOperation | null;
    let queue: UndoQueue;
    let target: string;

    if (command.isViewportCommand()) {
      if (viewport === null) {
        return new UndoRedoResult(
          command,
          null,
          'There is no selected viewport to ' + (command.isUndoCommand() ? 'undo' : 'redo'),
        );
      }
      target = 'view of ' + viewport.getTitle();
      queue = this.history.getViewportHistory().getQueue(viewport);
      operation = command.isUndoCommand()
        ? this.history.getViewportHistory().undo(viewport)
        : this.history.getViewportHistory().redo(viewport);
    } else {
      target = 'scene';
      queue = this.history.getSceneHistory().getQueue();
      operation = command.isUndoCommand()
        ? this.history.getSceneHistory().undo()
        : this.history.getSceneHistory().redo();
    }
    return new UndoRedoResult(command, operation, UndoRedoInteractionTechnique.describe(command, operation, queue, target));
  }

  private static describe(
    command: UndoRedoCommand,
    operation: UndoableOperation | null,
    queue: UndoQueue,
    target: string,
  ): string {
    const verb: string = command.isUndoCommand() ? 'undo' : 'redo';

    if (operation === null) {
      return 'Nothing to ' + verb + ' in the ' + target;
    }
    return (
      (command.isUndoCommand() ? 'Undo' : 'Redo') +
      ' in the ' +
      target +
      ': ' +
      operation.getName() +
      ' (' +
      queue.getUndoCount() +
      ' to undo, ' +
      queue.getRedoCount() +
      ' to redo)'
    );
  }
}
