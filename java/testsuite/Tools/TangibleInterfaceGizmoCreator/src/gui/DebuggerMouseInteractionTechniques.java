package gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import models.TangibleInterfaceGizmosModel;
import vsdk.toolkit.gui.AwtSystem;

public class DebuggerMouseInteractionTechniques
{
    public void processMouseEntered(TangibleInterfaceGizmosModel model)
    {
        if ( model.getCanvas() != null ) {
            model.getCanvas().requestFocusInWindow();
        }
    }

    public void processMouseExited(TangibleInterfaceGizmosModel model)
    {
    }

    public boolean processMousePressed(TangibleInterfaceGizmosModel model, MouseEvent e)
    {
        return model.getCameraController().processMousePressedEvent(
            AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseReleased(TangibleInterfaceGizmosModel model, MouseEvent e)
    {
        return model.getCameraController().processMouseReleasedEvent(
            AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseClicked(TangibleInterfaceGizmosModel model, MouseEvent e)
    {
        return model.getCameraController().processMouseClickedEvent(
            AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseMoved(TangibleInterfaceGizmosModel model, MouseEvent e)
    {
        return model.getCameraController().processMouseMovedEvent(
            AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseDragged(TangibleInterfaceGizmosModel model, MouseEvent e)
    {
        return model.getCameraController().processMouseDraggedEvent(
            AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseWheelMoved(TangibleInterfaceGizmosModel model, MouseWheelEvent e)
    {
        return model.getCameraController().processMouseWheelEvent(
            AwtSystem.awt2vsdkEvent(e));
    }
}
