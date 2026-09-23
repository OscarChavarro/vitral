package model.history;

import java.util.function.Supplier;

import model.Scene;

/**
Edition history of the editor: the global history of the scene (creation,
deletion and placement of bodies, lights and cameras of the scene) and one
history per viewport (how each viewport shows the scene). Which user
actions are recorded, and which history is undone or redone, is decided by
the interaction techniques of the GUI (see `gui.history`).
*/
public class EditHistory
{
    private final SceneHistory sceneHistory;
    private final ViewportHistory viewportHistory;

    /**
    @param sceneSource gives the scene currently edited
    */
    public EditHistory(Supplier<Scene> sceneSource)
    {
        sceneHistory = new SceneHistory(sceneSource);
        viewportHistory = new ViewportHistory();
    }

    /**
    @return the global history of the scene
    */
    public SceneHistory getSceneHistory()
    {
        return sceneHistory;
    }

    /**
    @return the histories of the views of the viewports
    */
    public ViewportHistory getViewportHistory()
    {
        return viewportHistory;
    }

    /**
    @return true if a user action is being recorded in any history
    */
    public boolean isRecording()
    {
        return sceneHistory.isRecording() || viewportHistory.isRecording();
    }
}
