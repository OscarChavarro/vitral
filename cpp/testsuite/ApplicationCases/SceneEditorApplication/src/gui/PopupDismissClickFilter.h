#ifndef __POPUP_DISMISS_CLICK_FILTER__
#define __POPUP_DISMISS_CLICK_FILTER__

/**
A click outside a popup menu closes it, and that click must not act over the
drawing area (i.e. selecting another viewport): the user only wanted to leave
the menu. The GUI technology notifies here when the menu closes and asks,
for each mouse event of the drawing area, if it must be ignored: the press
arriving shortly after the menu closed, and the drag, release and click that
follow it, are consumed. It does not depend on any GUI technology.
*/
class PopupDismissClickFilter {
public:
    /**
    Mouse events of the drawing area, as seen by this filter.
    */
    enum class MouseEventKind {
        PRESS,
        DRAG,
        RELEASE,
        CLICK,
        OTHER
    };

private:
    long long closedAtMillis;
    bool swallowingClick;

public:
    PopupDismissClickFilter();

    /**
    Must be called when the popup is shown.
    */
    void popupShown();

    /**
    Must be called when the popup closes.
    @param nowMillis current time, in milliseconds
    */
    void popupClosed(long long nowMillis);

    /**
    @param kind kind of the mouse event of the drawing area
    @param nowMillis current time, in milliseconds
    @return true if the event must be ignored by the drawing area
    */
    bool consumes(MouseEventKind kind, long long nowMillis);
};

#endif
