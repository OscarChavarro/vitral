#ifndef __XT_SELECTOR_DIALOG__
#define __XT_SELECTOR_DIALOG__

#include <X11/Intrinsic.h>

class XtApplicationHost;

/**
Dialog to select objects of the scene (`h` key), as `AwtSelectorDialog`:
like it, only its layout (a central and a bottom area with test buttons)
exists yet. Closing it only hides it.
*/
class XtSelectorDialog {
public:
    explicit XtSelectorDialog(XtApplicationHost* host);
    ~XtSelectorDialog();

    /**
    @param visible true to show the dialog, false to hide it
    */
    void setVisible(bool visible);

private:
    Widget dialog;
    Atom wmDeleteWindow;

    static void shellEvent(Widget, XtPointer clientData, XEvent* event,
                           Boolean*);

    XtSelectorDialog(const XtSelectorDialog& other);
    XtSelectorDialog& operator=(const XtSelectorDialog& other);
};

#endif
