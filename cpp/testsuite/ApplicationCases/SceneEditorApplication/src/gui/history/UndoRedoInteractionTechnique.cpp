#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "model/history/EditHistory.h"
#include "model/history/UndoQueue.h"
#include "model/history/UndoableOperation.h"
#include "gui/history/UndoRedoInteractionTechnique.h"

namespace {
/// Control characters of Ctrl+Z and Ctrl+Y, as GUI technologies report them
const char CONTROL_Z = 0x1A;
const char CONTROL_Y = 0x19;
}

UndoRedoInteractionTechnique::UndoRedoInteractionTechnique(
    EditHistory* history)
    : history(history)
{
}

bool UndoRedoInteractionTechnique::recognize(
    const KeyEvent& event, UndoRedoCommand::Value* outCommand)
{
    bool shift;
    bool z;
    bool y;

    if ( (event.modifierMask & KeyEvent::MASK_CTRL) == 0 ||
         (event.modifierMask & KeyEvent::MASK_ALT) != 0 ) {
        return false;
    }
    shift = (event.modifierMask & KeyEvent::MASK_SHIFT) != 0;
    z = event.keycode == KeyEvent::KEY_z || event.keycode == KeyEvent::KEY_Z;
    y = event.keycode == KeyEvent::KEY_y || event.keycode == KeyEvent::KEY_Y;
    if ( event.keycode == KeyEvent::KEY_NONE ) {
        // Technologies that do not give the key of Ctrl chords
        z = event.unicodeId == CONTROL_Z;
        y = event.unicodeId == CONTROL_Y;
    }
    if ( z ) {
        *outCommand = shift ? UndoRedoCommand::UNDO_VIEWPORT :
                              UndoRedoCommand::UNDO_SCENE;
        return true;
    }
    if ( y ) {
        *outCommand = shift ? UndoRedoCommand::REDO_VIEWPORT :
                              UndoRedoCommand::REDO_SCENE;
        return true;
    }
    return false;
}

bool UndoRedoInteractionTechnique::processKeyPressedEvent(
    const KeyEvent& event, Viewport* viewport, Result* outResult)
{
    UndoRedoCommand::Value command;

    if ( !recognize(event, &command) ) {
        return false;
    }
    *outResult = execute(command, viewport);
    return true;
}

UndoRedoInteractionTechnique::Result UndoRedoInteractionTechnique::execute(
    UndoRedoCommand::Value command, Viewport* viewport)
{
    UndoableOperation* operation;
    UndoQueue* queue;
    java::String target;
    bool undo = UndoRedoCommand::isUndoCommand(command);

    if ( UndoRedoCommand::isViewportCommand(command) ) {
        if ( viewport == nullptr ) {
            return Result(command, nullptr,
                java::String("There is no selected viewport to ") +
                (undo ? "undo" : "redo"));
        }
        target = java::String("view of ") + viewport->getTitle();
        queue = history->getViewportHistory()->getQueue(viewport);
        operation = undo ?
            history->getViewportHistory()->undo(viewport) :
            history->getViewportHistory()->redo(viewport);
    }
    else {
        target = "scene";
        queue = history->getSceneHistory()->getQueue();
        operation = undo ?
            history->getSceneHistory()->undo() :
            history->getSceneHistory()->redo();
    }
    return Result(command, operation,
                  describe(command, operation, queue, target));
}

java::String UndoRedoInteractionTechnique::describe(
    UndoRedoCommand::Value command, const UndoableOperation* operation,
    const UndoQueue* queue, const java::String& target)
{
    bool undo = UndoRedoCommand::isUndoCommand(command);
    java::String verb = undo ? "undo" : "redo";

    if ( operation == nullptr ) {
        return java::String("Nothing to ") + verb + " in the " + target;
    }
    return java::String(undo ? "Undo" : "Redo") + " in the " + target +
        ": " + operation->getName() + " (" +
        java::String::valueOf(queue->getUndoCount()) + " to undo, " +
        java::String::valueOf(queue->getRedoCount()) + " to redo)";
}
