package gui;

/**
A click outside a popup menu closes it, and that click must not act over the
drawing area (i.e. selecting another viewport): the user only wanted to leave
the menu. The GUI technology notifies here when the menu closes and asks,
for each mouse event of the drawing area, if it must be ignored: the press
arriving shortly after the menu closed, and the drag, release and click that
follow it, are consumed. It does not depend on any GUI technology.
*/
public class PopupDismissClickFilter
{
    /** The press that closes the popup is delivered to the canvas right after */
    private static final long OUTSIDE_PRESS_WINDOW_MILLIS = 300;

    /**
    Mouse events of the drawing area, as seen by this filter.
    */
    public enum MouseEventKind
    {
        PRESS,
        DRAG,
        RELEASE,
        CLICK,
        OTHER
    }

    private long closedAtMillis;
    private boolean swallowingClick;

    public PopupDismissClickFilter()
    {
        closedAtMillis = 0;
        swallowingClick = false;
    }

    /**
    Must be called when the popup is shown.
    */
    public void popupShown()
    {
        swallowingClick = false;
    }

    /**
    Must be called when the popup closes.
    @param nowMillis current time, in milliseconds
    */
    public void popupClosed(long nowMillis)
    {
        closedAtMillis = nowMillis;
    }

    /**
    @param kind kind of the mouse event of the drawing area
    @param nowMillis current time, in milliseconds
    @return true if the event must be ignored by the drawing area
    */
    public boolean consumes(MouseEventKind kind, long nowMillis)
    {
        switch ( kind ) {
          case PRESS:
            swallowingClick = closedAtMillis != 0 &&
                nowMillis - closedAtMillis <= OUTSIDE_PRESS_WINDOW_MILLIS;
            closedAtMillis = 0;
            return swallowingClick;
          case DRAG:
          case RELEASE:
            return swallowingClick;
          case CLICK:
            boolean swallowed = swallowingClick;
            swallowingClick = false;
            return swallowed;
          default:
            return false;
        }
    }
}
