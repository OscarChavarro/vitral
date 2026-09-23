#ifndef __VIEWPORT_HISTORY__
#define __VIEWPORT_HISTORY__

#include "java/util/ArrayList.h"

class UndoQueue;
class UndoableOperation;
class Viewport;
class ViewportSet;
class ViewportState;

/**
Undo/redo histories of the views: one queue per viewport, holding the changes
of how that viewport shows the scene (see `ViewportStateOperation`).

User actions are recorded as in `SceneHistory`: `begin` captures every
viewport of a viewport set and `end` records, in the queue of each viewport
that changed, one operation. Brackets can be nested; only the outermost one
records.

C++ port note: the identity maps of the Java version are kept as parallel
lists (there are few viewports).
*/
class ViewportHistory {
private:
    java::ArrayList<Viewport*> queueKeys;
    java::ArrayList<UndoQueue*> queues;
    java::ArrayList<Viewport*> beforeKeys;
    java::ArrayList<ViewportState*> before;
    int depth;

    void clearBefore();

    ViewportHistory(const ViewportHistory& other);
    ViewportHistory& operator=(const ViewportHistory& other);

public:
    ViewportHistory();
    virtual ~ViewportHistory();

    /**
    @param viewport a viewport
    @return the queue of operations done over the view of the viewport
    (created empty the first time)
    */
    UndoQueue* getQueue(Viewport* viewport);

    /**
    @return true if a user action is being recorded
    */
    bool isRecording() const;

    /**
    Starts recording a user action over the viewports of a set.
    @param viewportSet the viewports that the action can change
    */
    void begin(ViewportSet* viewportSet);

    /**
    Ends recording a user action. If it is the outermost one, each viewport
    whose view changed since `begin` gets one operation in its queue, named
    after what changed (see `ViewportState::describeChangeTo`).
    @param mergeable true if the operation can be merged with the one of the
    next action over the same viewport
    @return number of viewports whose change was recorded
    */
    int end(bool mergeable);

    /**
    @return the undone operation, or null if there was nothing to undo or an
    action is being recorded
    */
    UndoableOperation* undo(Viewport* viewport);

    /**
    @return the redone operation, or null if there was nothing to redo or an
    action is being recorded
    */
    UndoableOperation* redo(Viewport* viewport);

    /**
    Forgets the histories of every viewport.
    */
    void clear();
};

#endif
