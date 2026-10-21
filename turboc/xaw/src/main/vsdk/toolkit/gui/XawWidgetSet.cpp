// Xt names its strings and objects as the vitral String and Object
// classes: they are renamed while its headers are read (see
// XtIntrinsics.h)
#define String XtString
#define Object XtObject
#include <X11/StringDefs.h>
#include <X11/Shell.h>
#include <X11/Xaw/SimpleMenu.h>
#include <X11/Xaw/SmeBSB.h>
#include <X11/Xaw/SmeLine.h>
#undef Object
#undef String

#include "vsdk/toolkit/gui/XawWidgetSet.h"
#include "vsdk/toolkit/gui/XtWidgetSupport.h"


/**
A close handler of a shell. Freed with the shell.
*/
struct XawCloseBinding {
    XtWidgetSet::NotifyProc close;
    void* clientData;
    Atom wmDeleteWindow;
};

static void closeRequested(Widget, XtPointer clientData, XEvent* event, Boolean*)
{
    XawCloseBinding* binding = ((XawCloseBinding*)(clientData));
    if (event->type == ClientMessage &&
        ((Atom)(event->xclient.data.l[0])) == binding->wmDeleteWindow &&
        binding->close != NULL) {
        binding->close(binding->clientData);
    }
}

static void declareProtocols(Widget shell, XawCloseBinding* binding)
{
    Atom protocols[] = { binding->wmDeleteWindow };
    XSetWMProtocols(XtDisplay(shell), XtWindow(shell), protocols, 1);
}

/**
The protocols are a property of the window: declared once it exists.
*/
static void declareProtocolsOnMap(Widget shell, XtPointer clientData, XEvent* event,
                           Boolean*)
{
    if (event->type != MapNotify) return;
    declareProtocols(shell, ((XawCloseBinding*)(clientData)));
    XtRemoveEventHandler(shell, StructureNotifyMask, False,
                         declareProtocolsOnMap, clientData);
}

static void freeCloseBinding(Widget, XtPointer clientData, XtPointer)
{
    delete ((XawCloseBinding*)(clientData));
}

/**
Selects the keys in the shell, so they reach Xt, which delivers them to
its focus widget.
*/
static void ignoreEvent(Widget, XtPointer, XEvent*, Boolean*)
{
}

/**
An item of a context menu. Freed with its entry.
*/
struct XawChoiceBinding {
    XtWidgetSet::ChoiceProc choose;
    int index;
    void* clientData;
};

static void itemChosen(Widget, XtPointer clientData, XtPointer)
{
    XawChoiceBinding* binding = ((XawChoiceBinding*)(clientData));
    if (binding->choose != NULL)
        binding->choose(binding->index, binding->clientData);
}

static void freeChoiceBinding(Widget, XtPointer clientData, XtPointer)
{
    delete ((XawChoiceBinding*)(clientData));
}

struct XawPopdownBinding {
    XtWidgetSet::NotifyProc poppedDown;
    void* clientData;
};

static void menuPoppedDown(Widget menu, XtPointer clientData, XtPointer)
{
    XawPopdownBinding* binding = ((XawPopdownBinding*)(clientData));
    XtUngrabPointer(menu, CurrentTime);
    if (binding->poppedDown != NULL)
        binding->poppedDown(binding->clientData);
}

static void freePopdownBinding(Widget, XtPointer clientData, XtPointer)
{
    delete ((XawPopdownBinding*)(clientData));
}

static void freeBitmap(Widget widget, XtPointer clientData, XtPointer)
{
    XFreePixmap(XtDisplay(widget), ((Pixmap)(clientData)));
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
                      ignoreEvent, NULL);
    XtSetKeyboardFocus(shell, target);
}

void XawWidgetSet::setWindowCloseHandler(Widget shell, NotifyProc close,
                                         void* clientData)
{
    XawCloseBinding* binding = new XawCloseBinding;
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
        ((const char*)(checkMarkBits)), 8, 8);
    XtAddCallback(menu, XtNdestroyCallback, freeBitmap,
                  ((XtPointer)(checkMark)));

    for (size_t i = 0; i < items.size(); ++i) {
        if (items[i].separator) {
            XtCreateManagedWidget("separator", smeLineObjectClass, menu,
                                  NULL, 0);
            continue;
        }
        Arg args[6]; Cardinal n = 0;
        XtSetArg(args[n], XtNlabel, items[i].label.c_str()); ++n;
        XtSetArg(args[n], XtNleftMargin, 16); ++n;
        if (fontSet != NULL) {
            XtSetArg(args[n], XtNinternational, True); ++n;
            XtSetArg(args[n], XtNfontSet, fontSet); ++n;
        }
        if (items[i].checked) {
            XtSetArg(args[n], XtNleftBitmap, checkMark); ++n;
        }
        Widget item = XtCreateManagedWidget("choiceMenuItem",
            smeBSBObjectClass, menu, args, n);
        XawChoiceBinding* binding = new XawChoiceBinding;
        binding->choose = choose;
        binding->index = ((int)(i));
        binding->clientData = clientData;
        XtAddCallback(item, XtNcallback, itemChosen, binding);
        XtAddCallback(item, XtNdestroyCallback, freeChoiceBinding, binding);
    }
    return menu;
}

void XawWidgetSet::popupChoiceMenu(Widget menu, int rootX, int rootY,
                                   NotifyProc poppedDown, void* clientData)
{
    XawPopdownBinding* binding = new XawPopdownBinding;
    binding->poppedDown = poppedDown;
    binding->clientData = clientData;
    XtRemoveAllCallbacks(menu, XtNpopdownCallback);
    XtAddCallback(menu, XtNpopdownCallback, menuPoppedDown, binding);
    XtAddCallback(menu, XtNdestroyCallback, freePopdownBinding, binding);

    XtVaSetValues(menu, XtNx, ((Position)(rootX)),
                  XtNy, ((Position)(rootY)), NULL);
    XtPopup(menu, XtGrabExclusive);
    // Without owner events, every pointer event (over the menu, the canvas
    // or anywhere) goes to the menu: an outside release closes it
    XtGrabPointer(menu, False,
                  ButtonPressMask | ButtonReleaseMask | PointerMotionMask |
                  EnterWindowMask | LeaveWindowMask,
                  GrabModeAsync, GrabModeAsync, None, None, CurrentTime);
}
