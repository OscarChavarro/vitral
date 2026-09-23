#ifndef __VIEWPORT_STATE_OPERATION__
#define __VIEWPORT_STATE_OPERATION__

#include "model/history/UndoableOperation.h"

class Viewport;
class ViewportState;

/**
Change of how a viewport shows the scene: camera movement, projection
location (perspective / parallel projections), render mode and display
settings. It keeps (and owns) the states of the viewport before and after
the change.

Operations done by the keyboard over the same viewport in a short time can
be merged (see `absorb`), so an auto-repeated camera key is a single step.
*/
class ViewportStateOperation : public UndoableOperation {
public:
    /// Maximum time between two mergeable operations to be merged
    static const long long MERGE_INTERVAL_MILLISECONDS = 1000;

private:
    java::String name;
    ViewportState* before;
    ViewportState* after;
    bool mergeable;
    long long lastChangeTime;

    ViewportStateOperation(const ViewportStateOperation& other);
    ViewportStateOperation& operator=(const ViewportStateOperation& other);

public:
    /**
    @param name name of the user action
    @param before state of the viewport before the change (owned)
    @param after state of the same viewport after the change (owned)
    @param mergeable true if following operations over the same viewport
    can be merged into this one
    */
    ViewportStateOperation(const java::String& name, ViewportState* before,
                           ViewportState* after, bool mergeable);
    virtual ~ViewportStateOperation();

    /**
    @return the changed viewport
    */
    Viewport* getViewport() const;
    virtual void undo() override;
    virtual void redo() override;
    virtual java::String getName() const override;
    virtual bool absorb(UndoableOperation* next) override;
};

#endif
