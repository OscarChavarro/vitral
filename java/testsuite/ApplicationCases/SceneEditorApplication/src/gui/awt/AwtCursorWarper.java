package gui.awt;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;

/**
AWT specific part of the wrapping of the cursor while dragging (see
`TranslateGizmoInteractionTechnique`): it places the pointer of the system
over a position of a component. Other platforms should provide an equivalent
class.

It is kept apart from the platform independent techniques because AWT is the
only way to move the pointer in this platform. In some systems the
application needs a permission to control the pointer (i.e. accessibility on
macOS): without it the pointer does not move, which the techniques survive.
*/
public class AwtCursorWarper
{
    private Robot robot;

    public AwtCursorWarper()
    {
        robot = null;
        if ( GraphicsEnvironment.isHeadless() ) {
            return;
        }
        try {
            robot = new Robot();
        }
        catch ( AWTException | SecurityException ex ) {
            Logger.reportMessage(this, VSDK.WARNING, "AwtCursorWarper",
                "The pointer can not be placed, so it will not wrap around " +
                "the viewport while dragging: " + ex);
        }
    }

    /**
    @return true if the pointer of the system can be placed by this object
    */
    public boolean isAvailable()
    {
        return robot != null;
    }

    /**
    Places the pointer of the system over a position of a component.
    @param component a component showing on screen
    @param x horizontal position in component's coordinates
    @param y vertical position in component's coordinates
    @return true if the pointer was placed
    */
    public boolean warp(Component component, int x, int y)
    {
        if ( robot == null || component == null || !component.isShowing() ) {
            return false;
        }
        Point origin = component.getLocationOnScreen();

        robot.mouseMove(origin.x + x, origin.y + y);
        return true;
    }
}
