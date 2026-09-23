package gui.history;

/**
Commands over the edition history: undo and redo over the global history of
the scene, or over the history of the view of the selected viewport.
*/
public enum UndoRedoCommand
{
    /** Ctrl+Z: undo the last operation over the scene */
    UNDO_SCENE(false, true),
    /** Ctrl+Y: redo the last undone operation over the scene */
    REDO_SCENE(false, false),
    /** Ctrl+Shift+Z: undo the last change of the view of the selected viewport */
    UNDO_VIEWPORT(true, true),
    /** Ctrl+Shift+Y: redo the last undone change of the view of the selected viewport */
    REDO_VIEWPORT(true, false);

    private final boolean viewportCommand;
    private final boolean undoCommand;

    UndoRedoCommand(boolean viewportCommand, boolean undoCommand)
    {
        this.viewportCommand = viewportCommand;
        this.undoCommand = undoCommand;
    }

    /**
    @return true if the command works over the history of a viewport, false
    if it works over the history of the scene
    */
    public boolean isViewportCommand()
    {
        return viewportCommand;
    }

    /**
    @return true for undo, false for redo
    */
    public boolean isUndoCommand()
    {
        return undoCommand;
    }
}
