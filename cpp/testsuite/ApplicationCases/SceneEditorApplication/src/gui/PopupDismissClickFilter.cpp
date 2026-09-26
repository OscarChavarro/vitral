#include "gui/PopupDismissClickFilter.h"

namespace {
/** The press that closes the popup is delivered to the canvas right after */
const long long OUTSIDE_PRESS_WINDOW_MILLIS = 300;
}

PopupDismissClickFilter::PopupDismissClickFilter()
    : closedAtMillis(0), swallowingClick(false)
{
}

void PopupDismissClickFilter::popupShown()
{
    swallowingClick = false;
}

void PopupDismissClickFilter::popupClosed(long long nowMillis)
{
    closedAtMillis = nowMillis;
}

bool PopupDismissClickFilter::consumes(MouseEventKind kind,
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
            swallowingClick = true;
            closedAtMillis = 0;
        }
        return swallowingClick;
      case MouseEventKind::CLICK: {
        bool swallowed = swallowingClick;
        swallowingClick = false;
        return swallowed;
      }
      default:
        return false;
    }
}
