#include <string.h>

// Xt names its strings and objects as the vitral String and Object
// classes: they are renamed while its headers are read (see
// XtIntrinsics.h)
#define String XtString
#define Object XtObject
#include <Xm/Xm.h>
#include <Xm/DrawingA.h>
#include <Xm/Protocols.h>
#include <Xm/RowColumn.h>
#include <Xm/Separator.h>
#include <Xm/ToggleB.h>
#undef Object
#undef String

#include "vsdk/toolkit/gui/XmWidgetSet.h"
#include "vsdk/toolkit/gui/XmWidgetSupport.h"
#include "java/util/ArrayList.txx"


struct XmNotifyBinding {
    XtWidgetSet::NotifyProc notify;
    void* clientData;
};

static void notify(Widget, XtPointer clientData, XtPointer)
{
    XmNotifyBinding* binding = ((XmNotifyBinding*)(clientData));
    if (binding->notify != NULL)
        binding->notify(binding->clientData);
}

static void freeNotifyBinding(Widget, XtPointer clientData, XtPointer)
{
    delete ((XmNotifyBinding*)(clientData));
}

/**
An item of a context menu. Freed with its toggle.
*/
struct XmChoiceBinding {
    XtWidgetSet::ChoiceProc choose;
    int index;
    void* clientData;
};

static void itemChosen(Widget, XtPointer clientData, XtPointer)
{
    XmChoiceBinding* binding = ((XmChoiceBinding*)(clientData));
    if (binding->choose != NULL)
        binding->choose(binding->index, binding->clientData);
}

static void freeChoiceBinding(Widget, XtPointer clientData, XtPointer)
{
    delete ((XmChoiceBinding*)(clientData));
}

/**
@return the popup menu (RowColumn) of its MenuShell
*/
static Widget menuOf(Widget menuShell)
{
    WidgetList children = NULL;
    Cardinal count = 0;
    XtVaGetValues(menuShell, XmNchildren, &children, XmNnumChildren, &count,
                  NULL);
    return count > 0 ? children[0] : NULL;
}


const char* XmWidgetSet::getName() const
{
    return "Motif";
}

XtPanelWidgets* XmWidgetSet::getPanelWidgets()
{
    return &panelWidgets;
}

XtGuiRenderer* XmWidgetSet::getGuiRenderer()
{
    return &guiRenderer;
}

Widget XmWidgetSet::createDrawingArea(Widget parent, const char* name,
                                      int x, int y, int width, int height)
{
    Arg args[8]; Cardinal n = 0;
    XtSetArg(args[n], XmNx, x); ++n;
    XtSetArg(args[n], XmNy, y); ++n;
    XtSetArg(args[n], XmNwidth, width); ++n;
    XtSetArg(args[n], XmNheight, height); ++n;
    XtSetArg(args[n], XmNmarginWidth, 0); ++n;
    XtSetArg(args[n], XmNmarginHeight, 0); ++n;
    XtSetArg(args[n], XmNresizePolicy, XmRESIZE_NONE); ++n;
    // It takes part in the traversal: that is how it gets the keys
    XtSetArg(args[n], XmNtraversalOn, True); ++n;
    Widget area = XtCreateManagedWidget(name, xmDrawingAreaWidgetClass,
                                        parent, args, n);
    // Tab keys belong to the application, not to the traversal of Motif
    XtOverrideTranslations(area, XtParseTranslationTable(
        "<Key>osfNextField: DrawingAreaInput()\n"
        "<Key>osfPrevField: DrawingAreaInput()\n"
        "~Ctrl <Key>Tab: DrawingAreaInput()\n"
        "Shift <Key>Tab: DrawingAreaInput()"));
    return area;
}

void XmWidgetSet::setKeyboardFocus(Widget shell, Widget target)
{
    // Before the shell is realized, the traversal starts at the target
    Widget parent = XtParent(target);
    if (parent != NULL && XmIsManager(parent))
        XtVaSetValues(parent, XmNinitialFocus, target, NULL);
    if (XtIsRealized(shell))
        XmProcessTraversal(target, XmTRAVERSE_CURRENT);
}

void XmWidgetSet::setWindowCloseHandler(Widget shell, NotifyProc close,
                                        void* clientData)
{
    XmWidgetSupport::requireMotifShell(shell);
    XmNotifyBinding* binding = new XmNotifyBinding;
    binding->notify = close;
    binding->clientData = clientData;
    XtVaSetValues(shell, XmNdeleteResponse, XmDO_NOTHING, NULL);
    Atom wmDeleteWindow = XmInternAtom(XtDisplay(shell),
        ((char*)("WM_DELETE_WINDOW")), False);
    XmAddWMProtocolCallback(shell, wmDeleteWindow, notify, binding);
    XtAddCallback(shell, XmNdestroyCallback, freeNotifyBinding, binding);
}

Widget XmWidgetSet::createChoiceMenu(Widget owner,
                                     const ArrayList<MenuChoice>& items,
                                     XFontSet fontSet, ChoiceProc choose,
                                     void* clientData)
{
    Widget menu = XmWidgetSupport::createPopupMenu(owner, "choiceMenu");
    XmRenderTable fonts = XmWidgetSupport::createRenderTable(owner, fontSet);
    for ( long i = 0; i < items.size(); ++i) {
        if (items[i].separator) {
            XtCreateManagedWidget("separator", xmSeparatorWidgetClass, menu,
                                  NULL, 0);
            continue;
        }
        XmString label = XmWidgetSupport::createString(items[i].label);
        Arg args[4]; Cardinal n = 0;
        XtSetArg(args[n], XmNlabelString, label); ++n;
        XtSetArg(args[n], XmNset, items[i].checked ? XmSET : XmUNSET); ++n;
        // Unchecked items show no check mark
        XtSetArg(args[n], XmNvisibleWhenOff, False); ++n;
        if (fonts != NULL) {
            XtSetArg(args[n], XmNrenderTable, fonts); ++n;
        }
        Widget item = XtCreateManagedWidget("choiceMenuItem",
            xmToggleButtonWidgetClass, menu, args, n);
        XmStringFree(label);
        XmChoiceBinding* binding = new XmChoiceBinding;
        binding->choose = choose;
        binding->index = ((int)(i));
        binding->clientData = clientData;
        XtAddCallback(item, XmNvalueChangedCallback, itemChosen, binding);
        XtAddCallback(item, XmNdestroyCallback, freeChoiceBinding, binding);
    }
    if (fonts != NULL) XmRenderTableFree(fonts);
    return XtParent(menu);
}

void XmWidgetSet::popupChoiceMenu(Widget menuShell, int rootX, int rootY,
                                  NotifyProc poppedDown, void* clientData)
{
    Widget menu = menuOf(menuShell);
    if (menu == NULL) return;

    XmNotifyBinding* binding = new XmNotifyBinding;
    binding->notify = poppedDown;
    binding->clientData = clientData;
    XtRemoveAllCallbacks(menu, XmNunmapCallback);
    XtAddCallback(menu, XmNunmapCallback, notify, binding);
    XtAddCallback(menu, XmNdestroyCallback, freeNotifyBinding, binding);

    // Motif places popup menus at the root position of a button event
    XButtonPressedEvent position;
    memset(&position, 0, sizeof(position));
    position.type = ButtonPress;
    position.x_root = rootX;
    position.y_root = rootY;
    XmMenuPosition(menu, &position);
    XtManageChild(menu);
}
