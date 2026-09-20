package framework.gui;

import framework.model.Viewport;
import framework.model.ViewportSet;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.MouseEvent;

/**
Interaction techniques over a `ViewportSet`: selection of the viewport under
the pointer, layout control and per-viewport display commands. It processes
only vitral events, so callers must convert events from the GUI technology
in use (i.e. with `AwtSystem`) before calling it.

Mouse events must have coordinates in pixels of the `ViewportSet` area, with
origin at its upper left corner.

Keyboard commands:
  - `.` selects the next viewport, `,` selects the next layout style (the
    last one shows only the selected viewport) and Alt+`w` maximizes /
    restores the selected viewport.
  - Over the selected viewport: `g` toggles the grid, `t`, `l`, `f`, `b` and
    `p` select the Top, Left, Front, Bottom and Perspective cameras, `9`
    toggles the render mode and `0` cycles the requested size.
*/
public class ViewportSetInteractionTechniques
{
    private final ViewportSet viewportSet;

    public ViewportSetInteractionTechniques(ViewportSet viewportSet)
    {
        this.viewportSet = viewportSet;
    }

    public ViewportSet getViewportSet()
    {
        return viewportSet;
    }

    /**
    @param event
    @return true if the event was a viewport set command and was processed
    */
    public boolean processKeyPressedEvent(KeyEvent event)
    {
        if ( event == null ) {
            return false;
        }

        switch ( event.unicode_id ) {
          case '.':
            viewportSet.selectNextViewport();
            return true;
          case ',':
            viewportSet.selectNextLayoutStyle();
            return true;
          case 'w':
            if ( (event.modifierMask & KeyEvent.MASK_ALT) != 0 ) {
                viewportSet.toggleFullViewport();
                return true;
            }
            return false;
          default:
            return processSelectedViewportCommand(event);
        }
    }

    private boolean processSelectedViewportCommand(KeyEvent event)
    {
        Viewport viewport = viewportSet.getSelectedViewport();

        if ( viewport == null ) {
            return false;
        }

        if ( event.keycode == KeyEvent.KEY_9 ) {
            viewport.toggleRenderMode();
            return true;
        }
        if ( event.keycode == KeyEvent.KEY_NUM0 ) {
            viewport.cycleRequestedSize();
            return true;
        }

        switch ( event.unicode_id ) {
          case 'g':
            viewport.toggleGrid();
            return true;
          case 't':
            viewport.setActiveCamera(viewport.getTopCamera());
            return true;
          case 'l':
            viewport.setActiveCamera(viewport.getLeftCamera());
            return true;
          case 'f':
            viewport.setActiveCamera(viewport.getFrontCamera());
            return true;
          case 'b':
            viewport.setActiveCamera(viewport.getBottomCamera());
            return true;
          case 'p':
            viewport.setActiveCamera(viewport.getPerspectiveCamera());
            return true;
          case '0':
            viewport.cycleRequestedSize();
            return true;
          default:
            return false;
        }
    }

    /**
    @param event
    @return the active viewport under the pointer, or null if none
    */
    public Viewport findViewportAt(MouseEvent event)
    {
        return viewportSet.findViewportAt(event.getX(), event.getY());
    }

    /**
    Selects the viewport under the pointer.
    @param event
    @return true if there is a viewport under the pointer
    */
    public boolean processMousePressedEvent(MouseEvent event)
    {
        return selectViewportUnderPointer(event);
    }

    public boolean processMouseReleasedEvent(MouseEvent event)
    {
        return selectViewportUnderPointer(event);
    }

    public boolean processMouseClickedEvent(MouseEvent event)
    {
        return selectViewportUnderPointer(event);
    }

    public boolean processMouseDraggedEvent(MouseEvent event)
    {
        return selectViewportUnderPointer(event);
    }

    private boolean selectViewportUnderPointer(MouseEvent event)
    {
        Viewport viewport = findViewportAt(event);

        if ( viewport == null ) {
            return false;
        }
        return viewportSet.selectViewport(viewport);
    }

    /**
    Creates a copy of a mouse event, with its coordinates translated from the
    `ViewportSet` area to the given viewport (both with origin at the upper
    left corner).
    @param event
    @param viewport
    @return the translated event
    */
    public MouseEvent toViewportEvent(MouseEvent event, Viewport viewport)
    {
        MouseEvent translated = new MouseEvent();

        translated.setX(viewportSet.toViewportX(viewport, event.getX()));
        translated.setY(viewportSet.toViewportY(viewport, event.getY()));
        translated.setButton(event.getButton());
        translated.setModifiers(event.getModifiers());
        translated.setClicks(event.getClicks());
        return translated;
    }
}
