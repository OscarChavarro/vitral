#ifndef __UNDO_REDO_INTERACTION_TECHNIQUE__
#define __UNDO_REDO_INTERACTION_TECHNIQUE__

#include "java/lang/String.h"
#include "vsdk/toolkit/gui/KeyEvent.h"
#include "gui/history/UndoRedoCommand.h"

class EditHistory;
class UndoQueue;
class UndoableOperation;
class Viewport;

/**
Keyboard interaction technique for undo and redo. It processes only vitral
events, so callers must convert the events of the GUI technology in use
before (the key of a Ctrl chord must be in `KeyEvent::keycode`, since its
character is a control one):

- `Ctrl+Z` / `Ctrl+Y`: undo / redo over the global history of the scene
  (creation, deletion and transformation of bodies, lights and cameras).
- `Ctrl+Shift+Z` / `Ctrl+Shift+Y`: undo / redo over the history of the view
  of the selected viewport (camera placement, projection, display
  settings).
*/
class UndoRedoInteractionTechnique {
public:
    /**
    What an undo or redo command did.
    */
    class Result {
    private:
        UndoRedoCommand::Value commandValue;
        UndoableOperation* operationValue;
        java::String messageValue;

    public:
        Result()
            : commandValue(UndoRedoCommand::UNDO_SCENE),
              operationValue(nullptr), messageValue("") {}

        /**
        @param command the executed command
        @param operation the undone or redone operation (owned by its
        queue), or null if there was nothing to undo or redo
        @param message description of the result, for the user
        */
        Result(UndoRedoCommand::Value command, UndoableOperation* operation,
               const java::String& message)
            : commandValue(command), operationValue(operation),
              messageValue(message) {}

        UndoRedoCommand::Value command() const { return commandValue; }
        UndoableOperation* operation() const { return operationValue; }
        const java::String& message() const { return messageValue; }

        /**
        @return true if an operation was undone or redone
        */
        bool isDone() const
        {
            return operationValue != nullptr;
        }
    };

private:
    EditHistory* history;

    static java::String describe(UndoRedoCommand::Value command,
                                 const UndoableOperation* operation,
                                 const UndoQueue* queue,
                                 const java::String& target);

public:
    /**
    @param history edition history to undo and redo
    */
    explicit UndoRedoInteractionTechnique(EditHistory* history);

    /**
    @param event key press
    @param outCommand the undo/redo command of the key chord
    @return false if the key chord is not an undo/redo one (the Java
    version returns null)
    */
    static bool recognize(const KeyEvent& event,
                          UndoRedoCommand::Value* outCommand);

    /**
    Executes the undo/redo command of a key press, if it is one.
    @param event key press
    @param viewport the selected viewport, whose view history is used by
    the viewport commands
    @param outResult what the command did
    @return false if the key is not an undo/redo one (the Java version
    returns null)
    */
    bool processKeyPressedEvent(const KeyEvent& event, Viewport* viewport,
                                Result* outResult);

    /**
    @param command command to execute
    @param viewport the selected viewport, whose view history is used by
    the viewport commands
    @return what the command did
    */
    Result execute(UndoRedoCommand::Value command, Viewport* viewport);
};

#endif
