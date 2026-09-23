#ifndef __UNDO_REDO_COMMAND__
#define __UNDO_REDO_COMMAND__

/**
Commands over the edition history: undo and redo over the global history of
the scene, or over the history of the view of the selected viewport.

C++ port note: the Java enum with fields is a class with an enumeration and
static methods taking the command.
*/
class UndoRedoCommand {
public:
    enum Value {
        /** Ctrl+Z: undo the last operation over the scene */
        UNDO_SCENE,
        /** Ctrl+Y: redo the last undone operation over the scene */
        REDO_SCENE,
        /** Ctrl+Shift+Z: undo the last change of the view of the selected
        viewport */
        UNDO_VIEWPORT,
        /** Ctrl+Shift+Y: redo the last undone change of the view of the
        selected viewport */
        REDO_VIEWPORT
    };

    /**
    @return true if the command works over the history of a viewport, false
    if it works over the history of the scene
    */
    static bool isViewportCommand(Value command)
    {
        return command == UNDO_VIEWPORT || command == REDO_VIEWPORT;
    }

    /**
    @return true for undo, false for redo
    */
    static bool isUndoCommand(Value command)
    {
        return command == UNDO_SCENE || command == UNDO_VIEWPORT;
    }

private:
    UndoRedoCommand() {}
};

#endif
