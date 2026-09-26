#include <X11/StringDefs.h>
#include <X11/Shell.h>
#include <X11/Xaw/SimpleMenu.h>
#include <X11/Xaw/SmeBSB.h>
#include <X11/Xaw/SmeLine.h>

#include "vsdk/toolkit/gui/XawWidgetSet.h"
#include "vsdk/toolkit/gui/XtWidgetSupport.h"

namespace {

/**
A close handler of a shell. Freed with the shell.
*/
struct CloseBinding {
    XtWidgetSet::NotifyProc close;
    void* clientData;
    Atom wmDeleteWindow;
};

void closeRequested(Widget, XtPointer clientData, XEvent* event, Boolean*)
{
    CloseBinding* binding = static_cast<CloseBinding*>(clientData);
    if (event->type == ClientMessage &&
        static_cast<Atom>(event->xclient.data.l[0]) == binding->wmDeleteWindow &&
        binding->close != nullptr) {
        binding->close(binding->clientData);
    }
}

void declareProtocols(Widget shell, CloseBinding* binding)
{
    Atom protocols[] = { binding->wmDeleteWindow };
    XSetWMProtocols(XtDisplay(shell), XtWindow(shell), protocols, 1);
}

/**
The protocols are a property of the window: declared once it exists.
*/
void declareProtocolsOnMap(Widget shell, XtPointer clientData, XEvent* event,
                           Boolean*)
{
    if (event->type != MapNotify) return;
    declareProtocols(shell, static_cast<CloseBinding*>(clientData));
    XtRemoveEventHandler(shell, StructureNotifyMask, False,
                         declareProtocolsOnMap, clientData);
}

void freeCloseBinding(Widget, XtPointer clientData, XtPointer)
{
    delete static_cast<CloseBinding*>(clientData);
}

/**
Selects the keys in the shell, so they reach Xt, which delivers them to
its focus widget.
*/
void ignoreEvent(Widget, XtPointer, XEvent*, Boolean*)
{
}

/**
An item of a context menu. Freed with its entry.
*/
struct ChoiceBinding {
    XtWidgetSet::ChoiceProc choose;
    int index;
    void* clientData;
};

void itemChosen(Widget, XtPointer clientData, XtPointer)
{
    ChoiceBinding* binding = static_cast<ChoiceBinding*>(clientData);
    if (binding->choose != nullptr)
        binding->choose(binding->index, binding->clientData);
}

void freeChoiceBinding(Widget, XtPointer clientData, XtPointer)
{
    delete static_cast<ChoiceBinding*>(clientData);
}

struct PopdownBinding {
    XtWidgetSet::NotifyProc poppedDown;
    void* clientData;
};

void menuPoppedDown(Widget menu, XtPointer clientData, XtPointer)
{
    PopdownBinding* binding = static_cast<PopdownBinding*>(clientData);
    XtUngrabPointer(menu, CurrentTime);
    if (binding->poppedDown != nullptr)
        binding->poppedDown(binding->clientData);
}

void freePopdownBinding(Widget, XtPointer clientData, XtPointer)
{
    delete static_cast<PopdownBinding*>(clientData);
}

void freeBitmap(Widget widget, XtPointer clientData, XtPointer)
{
    XFreePixmap(XtDisplay(widget), reinterpret_cast<Pixmap>(clientData));
}

}

const char* XawWidgetSet::getName() const
{
    return "Xaw";
}

XtPanelWidgets* XawWidgetSet::getPanelWidgets()
{
    return &panelWidgets;
}

XtGuiRenderer* XawWidgetSet::getGuiRenderer()
{
    return &guiRenderer;
}

Widget XawWidgetSet::createDrawingArea(Widget parent, const char* name,
                                       int x, int y, int width, int height)
{
    Arg args[4]; Cardinal n = 0;
    XtSetArg(args[n], XtNx, x); ++n;
    XtSetArg(args[n], XtNy, y); ++n;
    XtSetArg(args[n], XtNwidth, width); ++n;
    XtSetArg(args[n], XtNheight, height); ++n;
    return XtCreateManagedWidget(name, coreWidgetClass, parent, args, n);
}

void XawWidgetSet::setKeyboardFocus(Widget shell, Widget target)
{
    // Keys typed anywhere in the window (i.e. with the pointer over a
    // panel) go to the target: Xt ignores the handler if already added
    XtAddEventHandler(shell, KeyPressMask | KeyReleaseMask, False,
                      ignoreEvent, nullptr);
    XtSetKeyboardFocus(shell, target);
}

void XawWidgetSet::setWindowCloseHandler(Widget shell, NotifyProc close,
                                         void* clientData)
{
    CloseBinding* binding = new CloseBinding;
    binding->close = close;
    binding->clientData = clientData;
    binding->wmDeleteWindow = XInternAtom(XtDisplay(shell), "WM_DELETE_WINDOW",
                                          False);
    XtAddEventHandler(shell, NoEventMask, True, closeRequested, binding);
    XtAddCallback(shell, XtNdestroyCallback, freeCloseBinding, binding);
    if (XtIsRealized(shell)) {
        declareProtocols(shell, binding);
    }
    else {
        XtAddEventHandler(shell, StructureNotifyMask, False,
                          declareProtocolsOnMap, binding);
    }
}

Widget XawWidgetSet::createChoiceMenu(Widget owner,
                                      const std::vector<MenuChoice>& items,
                                      XFontSet fontSet, ChoiceProc choose,
                                      void* clientData)
{
    static const unsigned char checkMarkBits[] = {
        0x00, 0x80, 0xc0, 0x61, 0x33, 0x1e, 0x0c, 0x00
    };

    XtWidgetSupport::ShellVisual shellVisual =
        XtWidgetSupport::shellVisualOf(owner);
    Arg menuArgs[3]; Cardinal menuN = 0;
    if (shellVisual.depth != 0) {
        XtSetArg(menuArgs[menuN], XtNvisual, shellVisual.visual); ++menuN;
        XtSetArg(menuArgs[menuN], XtNdepth, shellVisual.depth); ++menuN;
        XtSetArg(menuArgs[menuN], XtNcolormap, shellVisual.colormap); ++menuN;
    }
    Widget menu = XtCreatePopupShell("choiceMenu", simpleMenuWidgetClass,
                                     owner, menuArgs, menuN);
    // Entries follow the pointer without a button pressed, too
    XtOverrideTranslations(menu, XtParseTranslationTable(
        "<Motion>: highlight()"));

    Display* display = XtDisplay(owner);
    Pixmap checkMark = XCreateBitmapFromData(
        display, RootWindow(display, DefaultScreen(display)),
        reinterpret_cast<const char*>(checkMarkBits), 8, 8);
    XtAddCallback(menu, XtNdestroyCallback, freeBitmap,
                  reinterpret_cast<XtPointer>(checkMark));

    for (size_t i = 0; i < items.size(); ++i) {
        if (items[i].separator) {
            XtCreateManagedWidget("separator", smeLineObjectClass, menu,
                                  nullptr, 0);
            continue;
        }
        Arg args[6]; Cardinal n = 0;
        XtSetArg(args[n], XtNlabel, items[i].label.c_str()); ++n;
        XtSetArg(args[n], XtNleftMargin, 16); ++n;
        if (fontSet != nullptr) {
            XtSetArg(args[n], XtNinternational, True); ++n;
            XtSetArg(args[n], XtNfontSet, fontSet); ++n;
        }
        if (items[i].checked) {
            XtSetArg(args[n], XtNleftBitmap, checkMark); ++n;
        }
        Widget item = XtCreateManagedWidget("choiceMenuItem",
            smeBSBObjectClass, menu, args, n);
        ChoiceBinding* binding = new ChoiceBinding;
        binding->choose = choose;
        binding->index = static_cast<int>(i);
        binding->clientData = clientData;
        XtAddCallback(item, XtNcallback, itemChosen, binding);
        XtAddCallback(item, XtNdestroyCallback, freeChoiceBinding, binding);
    }
    return menu;
}

void XawWidgetSet::popupChoiceMenu(Widget menu, int rootX, int rootY,
                                   NotifyProc poppedDown, void* clientData)
{
    PopdownBinding* binding = new PopdownBinding;
    binding->poppedDown = poppedDown;
    binding->clientData = clientData;
    XtRemoveAllCallbacks(menu, XtNpopdownCallback);
    XtAddCallback(menu, XtNpopdownCallback, menuPoppedDown, binding);
    XtAddCallback(menu, XtNdestroyCallback, freePopdownBinding, binding);

    XtVaSetValues(menu, XtNx, static_cast<Position>(rootX),
                  XtNy, static_cast<Position>(rootY), nullptr);
    XtPopup(menu, XtGrabExclusive);
    // Without owner events, every pointer event (over the menu, the canvas
    // or anywhere) goes to the menu: an outside release closes it
    XtGrabPointer(menu, False,
                  ButtonPressMask | ButtonReleaseMask | PointerMotionMask |
                  EnterWindowMask | LeaveWindowMask,
                  GrabModeAsync, GrabModeAsync, None, None, CurrentTime);
}
