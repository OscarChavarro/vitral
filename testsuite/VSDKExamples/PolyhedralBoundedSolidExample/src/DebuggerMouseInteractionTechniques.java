import java.awt.event.MouseEvent;
import models.DebuggerModel;
import java.awt.event.MouseWheelEvent;

import vsdk.toolkit.gui.AwtSystem;

public class DebuggerMouseInteractionTechniques
{
    public void processMouseEntered(DebuggerModel model)
    {
        if ( model.getCanvas() != null ) {
            model.getCanvas().requestFocusInWindow();
        }
    }

    public void processMouseExited(DebuggerModel model)
    {
        ;
    }

    public boolean processMousePressed(DebuggerModel model, MouseEvent e)
    {
        return model.getCameraController().processMousePressedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseReleased(DebuggerModel model, MouseEvent e)
    {
        return model.getCameraController().processMouseReleasedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseClicked(DebuggerModel model, MouseEvent e)
    {
        return model.getCameraController().processMouseClickedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseMoved(DebuggerModel model, MouseEvent e)
    {
        return model.getCameraController().processMouseMovedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseDragged(DebuggerModel model, MouseEvent e)
    {
        return model.getCameraController().processMouseDraggedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseWheelMoved(DebuggerModel model, MouseWheelEvent e)
    {
        System.out.println(".");
        return model.getCameraController().processMouseWheelEvent(AwtSystem.awt2vsdkEvent(e));
    }
}
