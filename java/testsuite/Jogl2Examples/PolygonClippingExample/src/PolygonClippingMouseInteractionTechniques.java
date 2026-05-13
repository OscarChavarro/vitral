import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import com.jogamp.opengl.awt.GLCanvas;
import vsdk.toolkit.gui.AwtSystem;

public class PolygonClippingMouseInteractionTechniques
{
    public void processMouseEntered(GLCanvas canvas)
    {
        if ( canvas != null ) {
            canvas.requestFocusInWindow();
        }
    }

    public void processMouseExited(PolygonClippingDebuggerModel model)
    {
    }

    public boolean processMousePressed(PolygonClippingDebuggerModel model,
        MouseEvent e)
    {
        return model.getCameraController().processMousePressedEvent(
            AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseReleased(PolygonClippingDebuggerModel model,
        MouseEvent e)
    {
        return model.getCameraController().processMouseReleasedEvent(
            AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseClicked(PolygonClippingDebuggerModel model,
        MouseEvent e)
    {
        return model.getCameraController().processMouseClickedEvent(
            AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseMoved(PolygonClippingDebuggerModel model,
        MouseEvent e)
    {
        return model.getCameraController().processMouseMovedEvent(
            AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseDragged(PolygonClippingDebuggerModel model,
        MouseEvent e)
    {
        return model.getCameraController().processMouseDraggedEvent(
            AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseWheelMoved(PolygonClippingDebuggerModel model,
        MouseWheelEvent e)
    {
        return model.getCameraController().processMouseWheelEvent(
            AwtSystem.awt2vsdkEvent(e));
    }
}
