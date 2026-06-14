package gui;

import vsdk.toolkit.gui.MouseEvent;
import vsdk.toolkit.gui.CameraController;

public class MeshMouseInteractionTechniques {
    private final CameraController cameraController;

    public MeshMouseInteractionTechniques(CameraController cameraController)
    {
        this.cameraController = cameraController;
    }

    public boolean processMousePressedEvent(MouseEvent event)
    {
        return cameraController.processMousePressedEvent(event);
    }

    public boolean processMouseReleasedEvent(MouseEvent event)
    {
        return cameraController.processMouseReleasedEvent(event);
    }

    public boolean processMouseClickedEvent(MouseEvent event)
    {
        return cameraController.processMouseClickedEvent(event);
    }

    public boolean processMouseMovedEvent(MouseEvent event)
    {
        return cameraController.processMouseMovedEvent(event);
    }

    public boolean processMouseDraggedEvent(MouseEvent event)
    {
        return cameraController.processMouseDraggedEvent(event);
    }

    public boolean processMouseWheelEvent(MouseEvent event)
    {
        System.out.println(".");
        return cameraController.processMouseWheelEvent(event);
    }
}
