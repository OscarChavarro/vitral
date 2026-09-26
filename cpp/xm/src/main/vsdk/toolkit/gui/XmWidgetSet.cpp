#include <cstring>

#include <Xm/Xm.h>
#include <Xm/DrawingA.h>
#include <Xm/Protocols.h>
#include <Xm/RowColumn.h>
#include <Xm/Separator.h>
#include <Xm/ToggleB.h>

#include "vsdk/toolkit/gui/XmWidgetSet.h"
#include "vsdk/toolkit/gui/XmWidgetSupport.h"

namespace {

struct NotifyBinding {
    XtWidgetSet::NotifyProc notify;
    void* clientData;
};

void notify(Widget, XtPointer clientData, XtPointer)
{
    NotifyBinding* binding = static_cast<NotifyBinding*>(clientData);
    if (binding->notify != nullptr)
        binding->notify(binding->clientData);
}

void freeNotifyBinding(Widget, XtPointer clientData, XtPointer)
{
    delete static_cast<NotifyBinding*>(clientData);
}

/**
An item of a context menu. Freed with its toggle.
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

/**
@return the popup menu (RowColumn) of its MenuShell
*/
Widget menuOf(Widget menuShell)
{
    WidgetList children = nullptr;
    Cardinal count = 0;
    XtVaGetValues(menuShell, XmNchildren, &children, XmNnumChildren, &count,
                  nullptr);
    return count > 0 ? children[0] : nullptr;
}

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
    if (parent != nullptr && XmIsManager(parent))
        XtVaSetValues(parent, XmNinitialFocus, target, nullptr);
    if (XtIsRealized(shell))
        XmProcessTraversal(target, XmTRAVERSE_CURRENT);
}

void XmWidgetSet::setWindowCloseHandler(Widget shell, NotifyProc close,
                                        void* clientData)
{
    XmWidgetSupport::requireMotifShell(shell);
    NotifyBinding* binding = new NotifyBinding;
    binding->notify = close;
    binding->clientData = clientData;
    XtVaSetValues(shell, XmNdeleteResponse, XmDO_NOTHING, nullptr);
    Atom wmDeleteWindow = XmInternAtom(XtDisplay(shell),
        const_cast<char*>("WM_DELETE_WINDOW"), False);
    XmAddWMProtocolCallback(shell, wmDeleteWindow, notify, binding);
    XtAddCallback(shell, XmNdestroyCallback, freeNotifyBinding, binding);
}

Widget XmWidgetSet::createChoiceMenu(Widget owner,
                                     const std::vector<MenuChoice>& items,
                                     XFontSet fontSet, ChoiceProc choose,
                                     void* clientData)
{
    Widget menu = XmWidgetSupport::createPopupMenu(owner, "choiceMenu");
    XmRenderTable fonts = XmWidgetSupport::createRenderTable(owner, fontSet);
    for (size_t i = 0; i < items.size(); ++i) {
        if (items[i].separator) {
            XtCreateManagedWidget("separator", xmSeparatorWidgetClass, menu,
                                  nullptr, 0);
            continue;
        }
        XmString label = XmWidgetSupport::createString(items[i].label);
        Arg args[4]; Cardinal n = 0;
        XtSetArg(args[n], XmNlabelString, label); ++n;
        XtSetArg(args[n], XmNset, items[i].checked ? XmSET : XmUNSET); ++n;
        // Unchecked items show no check mark
        XtSetArg(args[n], XmNvisibleWhenOff, False); ++n;
        if (fonts != nullptr) {
            XtSetArg(args[n], XmNrenderTable, fonts); ++n;
        }
        Widget item = XtCreateManagedWidget("choiceMenuItem",
            xmToggleButtonWidgetClass, menu, args, n);
        XmStringFree(label);
        ChoiceBinding* binding = new ChoiceBinding;
        binding->choose = choose;
        binding->index = static_cast<int>(i);
        binding->clientData = clientData;
        XtAddCallback(item, XmNvalueChangedCallback, itemChosen, binding);
        XtAddCallback(item, XmNdestroyCallback, freeChoiceBinding, binding);
    }
    if (fonts != nullptr) XmRenderTableFree(fonts);
    return XtParent(menu);
}

void XmWidgetSet::popupChoiceMenu(Widget menuShell, int rootX, int rootY,
                                  NotifyProc poppedDown, void* clientData)
{
    Widget menu = menuOf(menuShell);
    if (menu == nullptr) return;

    NotifyBinding* binding = new NotifyBinding;
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
