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
