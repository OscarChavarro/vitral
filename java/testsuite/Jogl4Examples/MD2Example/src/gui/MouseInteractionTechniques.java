package gui;

import model.DebuggerModel;
import vsdk.toolkit.gui.MouseEvent;

public class MouseInteractionTechniques {
    private final DebuggerModel model;

    public MouseInteractionTechniques(DebuggerModel model)
    {
        this.model = model;
    }

    public boolean processMouseEnteredEvent(MouseEvent mouseEvent)
    {
        return false;
    }

    public boolean processMouseExitedEvent(MouseEvent mouseEvent)
    {
        return false;
    }

    public boolean processMousePressedEvent(MouseEvent mouseEvent)
    {
        return false;
    }

    public boolean processMouseReleasedEvent(MouseEvent mouseEvent)
    {
        return false;
    }

    public boolean processMouseClickedEvent(MouseEvent mouseEvent)
    {
        return false;
    }

    public boolean processMouseMovedEvent(MouseEvent mouseEvent)
    {
        return false;
    }

    public boolean processMouseDraggedEvent(MouseEvent mouseEvent)
    {
        return false;
    }

    public boolean processMouseWheelEvent(MouseEvent mouseEvent)
    {
        System.out.println(".");
        return false;
    }
}
