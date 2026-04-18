import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import vsdk.toolkit.gui.AwtSystem;

public class PolygonClippingMouseInteractionTechniques
{
    public void processMouseEntered(PolygonClippingDebuggerModel model)
    {
        if ( model.canvas != null ) {
            model.canvas.requestFocusInWindow();
        }
    }

    public void processMouseExited(PolygonClippingDebuggerModel model)
    {
    }

    public boolean processMousePressed(PolygonClippingDebuggerModel model,
        MouseEvent e)
    {
        return model.cameraController.processMousePressedEvent(
            AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseReleased(PolygonClippingDebuggerModel model,
        MouseEvent e)
    {
        return model.cameraController.processMouseReleasedEvent(
            AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseClicked(PolygonClippingDebuggerModel model,
        MouseEvent e)
    {
        return model.cameraController.processMouseClickedEvent(
            AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseMoved(PolygonClippingDebuggerModel model,
        MouseEvent e)
    {
        return model.cameraController.processMouseMovedEvent(
            AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseDragged(PolygonClippingDebuggerModel model,
        MouseEvent e)
    {
        return model.cameraController.processMouseDraggedEvent(
            AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseWheelMoved(PolygonClippingDebuggerModel model,
        MouseWheelEvent e)
    {
        return model.cameraController.processMouseWheelEvent(
            AwtSystem.awt2vsdkEvent(e));
    }
}
