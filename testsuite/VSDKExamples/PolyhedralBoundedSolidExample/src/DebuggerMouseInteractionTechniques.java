import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import vsdk.toolkit.gui.AwtSystem;

public class DebuggerMouseInteractionTechniques
{
    public void processMouseEntered(DebuggerModel model)
    {
        if ( model.canvas != null ) {
            model.canvas.requestFocusInWindow();
        }
    }

    public void processMouseExited(DebuggerModel model)
    {
        ;
    }

    public boolean processMousePressed(DebuggerModel model, MouseEvent e)
    {
        return model.cameraController.processMousePressedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseReleased(DebuggerModel model, MouseEvent e)
    {
        return model.cameraController.processMouseReleasedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseClicked(DebuggerModel model, MouseEvent e)
    {
        return model.cameraController.processMouseClickedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseMoved(DebuggerModel model, MouseEvent e)
    {
        return model.cameraController.processMouseMovedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseDragged(DebuggerModel model, MouseEvent e)
    {
        return model.cameraController.processMouseDraggedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseWheelMoved(DebuggerModel model, MouseWheelEvent e)
    {
        System.out.println(".");
        return model.cameraController.processMouseWheelEvent(AwtSystem.awt2vsdkEvent(e));
    }
}
