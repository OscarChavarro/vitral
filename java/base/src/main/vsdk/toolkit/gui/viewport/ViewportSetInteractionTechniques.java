package vsdk.toolkit.gui.viewport;

import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.MouseEvent;

/**
Interaction techniques over a `ViewportSet`: selection of the viewport under
the pointer, layout control and per-viewport display commands. It processes
only vitral events, so callers must convert events from the GUI technology
in use (i.e. with `AwtSystem`) before calling it.

Mouse events must have coordinates in pixels of the `ViewportSet` area, with
origin at its upper left corner.

Mouse: pressing over a viewport selects it. If it was already selected,
pressing and releasing over its title requests the menu to change its
projection location, through the `ViewportSetInteractionListener`.

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
    private ViewportSetInteractionListener listener;
    private boolean titlePressArmed;

    public ViewportSetInteractionTechniques(ViewportSet viewportSet)
    {
        this.viewportSet = viewportSet;
        this.listener = null;
        this.titlePressArmed = false;
    }

    /**
    @param listener who receives the requests derived from interaction, or
    null for none
    */
    public void setListener(ViewportSetInteractionListener listener)
    {
        this.listener = listener;
    }

    public ViewportSet getViewportSet()
    {
        return viewportSet;
    }

    /**
    Processes one of the standard commands of the viewport set (see
    `ViewportSetCommands`), i.e. from its popup menus, over the selected
    viewport.
    @param command the id of the command, starting with `IDV_`
    @return true if the command was a viewport set one and was processed
    */
    public boolean processCommand(String command)
    {
        return processCommand(command, viewportSet.getSelectedViewport());
    }

    /**
    Processes one of the standard commands of the viewport set over the given
    viewport.
    @param command the id of the command, starting with `IDV_`
    @param viewport
    @return true if the command was a viewport set one and was processed
    */
    public boolean processCommand(String command, Viewport viewport)
    {
        if ( viewport == null ) {
            return false;
        }
        return viewport.selectProjectionLocation(command);
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
    @param event pointer position, in the `ViewportSet` area
    @return true if the pointer is over the title (HUD) of a viewport, that
    is, over an area where clicking has an effect
    */
    public boolean isPointerOverTitle(MouseEvent event)
    {
        Viewport viewport = findViewportAt(event);

        return viewport != null && isOverTitle(event, viewport);
    }

    /**
    Selects the viewport under the pointer. If it was already the selected
    one and the press is over its title, the following click over the title
    will request the projection location menu.
    @param event
    @return true if there is a viewport under the pointer
    */
    public boolean processMousePressedEvent(MouseEvent event)
    {
        Viewport viewport = findViewportAt(event);

        // Must be evaluated before the selection changes
        titlePressArmed = viewport != null &&
            event.getButton() == MouseEvent.BUTTON1 &&
            viewportSet.isSelected(viewport) &&
            isOverTitle(event, viewport);

        return selectViewportUnderPointer(event);
    }

    /**
    Selects the viewport under the pointer. If the press was over the title
    of the already selected viewport and the button is released over it too,
    the projection location menu is requested. (It is done on the release, and
    not on the click event, because GUI technologies do not generate clicks if
    the pointer moves slightly between the press and the release.)
    @param event
    @return true if there is a viewport under the pointer
    */
    public boolean processMouseReleasedEvent(MouseEvent event)
    {
        boolean titleClick = titlePressArmed;
        Viewport viewport = findViewportAt(event);

        titlePressArmed = false;
        boolean selected = selectViewportUnderPointer(event);

        if ( titleClick && viewport != null && listener != null &&
             viewportSet.isSelected(viewport) && isOverTitle(event, viewport) ) {
            // Just below the title, aligned with it
            int x = viewport.getPixelStartX() + viewport.getTitleAreaStartX();
            int viewportTop = viewportSet.getSizeYInPixels() -
                (viewport.getPixelStartY() + viewport.getPixelSizeY());
            int y = viewportTop + viewport.getTitleAreaStartY() +
                viewport.getTitleAreaSizeY();
            listener.projectionLocationMenuRequested(viewport, x, y);
        }
        return selected;
    }

    public boolean processMouseClickedEvent(MouseEvent event)
    {
        titlePressArmed = false;
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

    private boolean isOverTitle(MouseEvent event, Viewport viewport)
    {
        return viewport.isOverTitle(
            viewportSet.toViewportX(viewport, event.getX()),
            viewportSet.toViewportY(viewport, event.getY()));
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
