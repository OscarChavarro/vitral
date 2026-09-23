package gui.history;

import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.viewport.Viewport;

import model.history.EditHistory;
import model.history.UndoQueue;
import model.history.UndoableOperation;

/**
Keyboard interaction technique for undo and redo. It processes only vitral
events, so callers must convert the events of the GUI technology in use
before (the key of a Ctrl chord must be in `KeyEvent.keycode`, since its
character is a control one; see `gui.awt.AwtKeyEventMapper`):

- `Ctrl+Z` / `Ctrl+Y`: undo / redo over the global history of the scene
  (creation, deletion and transformation of bodies, lights and cameras).
- `Ctrl+Shift+Z` / `Ctrl+Shift+Y`: undo / redo over the history of the view
  of the selected viewport (camera placement, projection, display settings).
*/
public class UndoRedoInteractionTechnique
{
    /**
    What an undo or redo command did.
    @param command the executed command
    @param operation the undone or redone operation, or null if there was
    nothing to undo or redo
    @param message description of the result, for the user
    */
    public record Result(UndoRedoCommand command, UndoableOperation operation,
                         String message)
    {
        /**
        @return true if an operation was undone or redone
        */
        public boolean isDone()
        {
            return operation != null;
        }
    }

    /// Control characters of Ctrl+Z and Ctrl+Y, as GUI technologies report them
    private static final char CONTROL_Z = 0x1A;
    private static final char CONTROL_Y = 0x19;

    private final EditHistory history;

    /**
    @param history edition history to undo and redo
    */
    public UndoRedoInteractionTechnique(EditHistory history)
    {
        this.history = history;
    }

    /**
    @param event key press
    @return the undo/redo command of the key chord, or null if it is not one
    */
    public static UndoRedoCommand recognize(KeyEvent event)
    {
        boolean shift;
        boolean z;
        boolean y;

        if ( event == null ||
             (event.modifierMask & KeyEvent.MASK_CTRL) == 0 ||
             (event.modifierMask & KeyEvent.MASK_ALT) != 0 ) {
            return null;
        }
        shift = (event.modifierMask & KeyEvent.MASK_SHIFT) != 0;
        z = event.keycode == KeyEvent.KEY_z || event.keycode == KeyEvent.KEY_Z;
        y = event.keycode == KeyEvent.KEY_y || event.keycode == KeyEvent.KEY_Y;
        if ( event.keycode == KeyEvent.KEY_NONE ) {
            // Technologies that do not give the key of Ctrl chords
            z = event.unicode_id == CONTROL_Z;
            y = event.unicode_id == CONTROL_Y;
        }
        if ( z ) {
            return shift ? UndoRedoCommand.UNDO_VIEWPORT : UndoRedoCommand.UNDO_SCENE;
        }
        if ( y ) {
            return shift ? UndoRedoCommand.REDO_VIEWPORT : UndoRedoCommand.REDO_SCENE;
        }
        return null;
    }

    /**
    Executes the undo/redo command of a key press, if it is one.
    @param event key press
    @param viewport the selected viewport, whose view history is used by the
    viewport commands
    @return what the command did, or null if the key is not an undo/redo one
    */
    public Result processKeyPressedEvent(KeyEvent event, Viewport viewport)
    {
        UndoRedoCommand command = recognize(event);

        if ( command == null ) {
            return null;
        }
        return execute(command, viewport);
    }

    /**
    @param command command to execute
    @param viewport the selected viewport, whose view history is used by the
    viewport commands
    @return what the command did
    */
    public Result execute(UndoRedoCommand command, Viewport viewport)
    {
        UndoableOperation operation;
        UndoQueue queue;
        String target;

        if ( command.isViewportCommand() ) {
            if ( viewport == null ) {
                return new Result(command, null, "There is no selected viewport to " +
                    (command.isUndoCommand() ? "undo" : "redo"));
            }
            target = "view of " + viewport.getTitle();
            queue = history.getViewportHistory().getQueue(viewport);
            operation = command.isUndoCommand() ?
                history.getViewportHistory().undo(viewport) :
                history.getViewportHistory().redo(viewport);
        }
        else {
            target = "scene";
            queue = history.getSceneHistory().getQueue();
            operation = command.isUndoCommand() ?
                history.getSceneHistory().undo() :
                history.getSceneHistory().redo();
        }
        return new Result(command, operation, describe(command, operation, queue, target));
    }

    private static String describe(UndoRedoCommand command,
                                   UndoableOperation operation,
                                   UndoQueue queue, String target)
    {
        String verb = command.isUndoCommand() ? "undo" : "redo";

        if ( operation == null ) {
            return "Nothing to " + verb + " in the " + target;
        }
        return (command.isUndoCommand() ? "Undo" : "Redo") + " in the " + target +
            ": " + operation.getName() + " (" + queue.getUndoCount() +
            " to undo, " + queue.getRedoCount() + " to redo)";
    }
}
