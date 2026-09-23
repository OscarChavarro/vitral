#ifndef __EDIT_HISTORY__
#define __EDIT_HISTORY__

#include <functional>

#include "model/history/SceneHistory.h"
#include "model/history/ViewportHistory.h"

class Scene;

/**
Edition history of the editor: the global history of the scene (creation,
deletion and placement of bodies, lights and cameras of the scene) and one
history per viewport (how each viewport shows the scene). Which user
actions are recorded, and which history is undone or redone, is decided by
the interaction techniques of the GUI (see `gui/history`).
*/
class EditHistory {
private:
    SceneHistory sceneHistory;
    ViewportHistory viewportHistory;

public:
    /**
    @param sceneSource gives the scene currently edited
    */
    explicit EditHistory(const std::function<Scene*()>& sceneSource)
        : sceneHistory(sceneSource)
    {
    }

    /**
    @return the global history of the scene
    */
    SceneHistory* getSceneHistory()
    {
        return &sceneHistory;
    }

    /**
    @return the histories of the views of the viewports
    */
    ViewportHistory* getViewportHistory()
    {
        return &viewportHistory;
    }

    /**
    @return true if a user action is being recorded in any history
    */
    bool isRecording() const
    {
        return sceneHistory.isRecording() || viewportHistory.isRecording();
    }
};

#endif
