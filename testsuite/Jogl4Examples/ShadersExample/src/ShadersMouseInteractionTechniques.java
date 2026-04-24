import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import com.jogamp.opengl.awt.GLCanvas;

import vsdk.toolkit.gui.AwtSystem;

public class ShadersMouseInteractionTechniques
{
    public void processMouseEntered(GLCanvas canvas)
    {
        if ( canvas != null ) {
            canvas.requestFocusInWindow();
        }
    }

    public void processMouseExited()
    {
    }

    public boolean processMousePressed(ShadersModel model, MouseEvent e)
    {
        return model.getCameraController().processMousePressedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseReleased(ShadersModel model, MouseEvent e)
    {
        return model.getCameraController().processMouseReleasedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseClicked(ShadersModel model, MouseEvent e)
    {
        return model.getCameraController().processMouseClickedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseMoved(ShadersModel model, MouseEvent e)
    {
        return model.getCameraController().processMouseMovedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseDragged(ShadersModel model, MouseEvent e)
    {
        return model.getCameraController().processMouseDraggedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    public boolean processMouseWheelMoved(ShadersModel model, MouseWheelEvent e)
    {
        return model.getCameraController().processMouseWheelEvent(AwtSystem.awt2vsdkEvent(e));
    }
}
