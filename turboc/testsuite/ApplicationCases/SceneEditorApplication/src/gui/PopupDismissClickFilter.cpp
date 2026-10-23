#include "vsdk/toolkit/common/VSDK.h"
#include "gui/PopupDismissClickFilter.h"

/** The press that closes the popup is delivered to the canvas right after */
static const long long OUTSIDE_PRESS_WINDOW_MILLIS = 300;

PopupDismissClickFilter::PopupDismissClickFilter()
    : closedAtMillis(0), swallowingClick(FALSE)
{
}

void PopupDismissClickFilter::popupShown()
{
    swallowingClick = FALSE;
}

void PopupDismissClickFilter::popupClosed(long long nowMillis)
{
    closedAtMillis = nowMillis;
}

BOOLEAN PopupDismissClickFilter::consumes(MouseEventKind kind,
                                       long long nowMillis)
{
    switch ( kind ) {
      case MouseEventKind::PRESS:
        swallowingClick = closedAtMillis != 0 &&
            nowMillis - closedAtMillis <= OUTSIDE_PRESS_WINDOW_MILLIS;
        closedAtMillis = 0;
        return swallowingClick;
      case MouseEventKind::DRAG:
      case MouseEventKind::RELEASE:
        // Some widget sets (Motif) take the press that closes their menus:
        // only the rest of that click reaches the drawing area
        if ( !swallowingClick && closedAtMillis != 0 &&
             nowMillis - closedAtMillis <= OUTSIDE_PRESS_WINDOW_MILLIS ) {
            swallowingClick = TRUE;
            closedAtMillis = 0;
        }
        return swallowingClick;
      case MouseEventKind::CLICK: {
        BOOLEAN swallowed = swallowingClick;
        swallowingClick = FALSE;
        return swallowed;
      }
      default:
        return FALSE;
    }
}
