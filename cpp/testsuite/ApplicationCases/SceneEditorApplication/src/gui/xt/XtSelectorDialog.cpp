#include <X11/StringDefs.h>
#include <X11/Shell.h>
#include <X11/Composite.h>
#include <X11/Xaw/Command.h>

#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtSelectorDialog.h"

namespace {
const int DIALOG_WIDTH = 526;
const int DIALOG_HEIGHT = 530;

void addButton(Widget parent, const char* label, XFontSet fontSet, int x,
               int y)
{
    Arg args[7]; Cardinal n = 0;
    XtSetArg(args[n], XtNlabel, label); ++n;
    XtSetArg(args[n], XtNinternational, True); ++n;
    XtSetArg(args[n], XtNfontSet, fontSet); ++n;
    XtSetArg(args[n], XtNx, x); ++n;
    XtSetArg(args[n], XtNy, y); ++n;
    XtSetArg(args[n], XtNwidth, 120); ++n;
    XtSetArg(args[n], XtNheight, 28); ++n;
    XtCreateManagedWidget("selectorButton", commandWidgetClass, parent, args, n);
}
}

XtSelectorDialog::XtSelectorDialog(XtApplicationHost* host)
    : dialog(nullptr), wmDeleteWindow(None)
{
    dialog = host->createDialogShell("selectorDialog",
                                     transientShellWidgetClass, "");
    XtAddEventHandler(dialog, NoEventMask, True, &XtSelectorDialog::shellEvent,
                      this);
    Arg args[2]; Cardinal n = 0;
    XtSetArg(args[n], XtNwidth, DIALOG_WIDTH); ++n;
    XtSetArg(args[n], XtNheight, DIALOG_HEIGHT); ++n;
    Widget mainFrameWidget = XtCreateManagedWidget("selectorFrame",
        compositeWidgetClass, dialog, args, n);

    // Central area (north) and bottom area (south), as the Swing layout
    addButton(mainFrameWidget, "Test central", host->getPanelFontSet(),
              (DIALOG_WIDTH - 120) / 2, 8);
    addButton(mainFrameWidget, "Test bottom", host->getPanelFontSet(),
              (DIALOG_WIDTH - 120) / 2, DIALOG_HEIGHT - 36);
}

XtSelectorDialog::~XtSelectorDialog()
{
    if ( dialog != nullptr ) {
        XtDestroyWidget(dialog);
    }
}

void XtSelectorDialog::setVisible(bool visible)
{
    if ( !visible ) {
        XtPopdown(dialog);
        return;
    }
    XtPopup(dialog, XtGrabNone);
    if ( wmDeleteWindow == None ) {
        wmDeleteWindow = XInternAtom(XtDisplay(dialog), "WM_DELETE_WINDOW",
                                     False);
        XSetWMProtocols(XtDisplay(dialog), XtWindow(dialog), &wmDeleteWindow,
                        1);
    }
    XRaiseWindow(XtDisplay(dialog), XtWindow(dialog));
}

void XtSelectorDialog::shellEvent(Widget, XtPointer clientData, XEvent* event,
                                  Boolean*)
{
    XtSelectorDialog* self = static_cast<XtSelectorDialog*>(clientData);
    if ( event->type == ClientMessage &&
         static_cast<Atom>(event->xclient.data.l[0]) == self->wmDeleteWindow ) {
        XtPopdown(self->dialog);
    }
}
