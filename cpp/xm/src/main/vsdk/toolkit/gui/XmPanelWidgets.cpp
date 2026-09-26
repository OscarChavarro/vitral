#include <cstring>

#include <Xm/Xm.h>
#include <Xm/BulletinB.h>
#include <Xm/Label.h>
#include <Xm/PushB.h>
#include <Xm/RowColumn.h>
#include <Xm/TextF.h>

#include "vsdk/toolkit/gui/XmPanelWidgets.h"
#include "vsdk/toolkit/gui/XmWidgetSupport.h"
#include "vsdk/toolkit/gui/XtWidgetSupport.h"

namespace {

const char* const INVALID_BACKGROUND = "#ffc8c8";
const char* const NORMAL_BACKGROUND = "white";
/// The foreground of the default Motif colors may be a light one
const char* const TEXT_FOREGROUND = "black";

/**
An option of an option button. Freed with its menu item.
*/
struct OptionBinding {
    Widget button;
    std::string label;
    int index;
    XtPanelWidgets::OptionProc choose;
    void* clientData;
};

void optionChosen(Widget, XtPointer clientData, XtPointer)
{
    OptionBinding* binding = static_cast<OptionBinding*>(clientData);
    // The button does not recompute its size: its geometry stays
    XmWidgetSupport::setLabelString(binding->button, binding->label);
    if (binding->choose != nullptr)
        binding->choose(binding->button, binding->index, binding->clientData);
}

void freeOptionBinding(Widget, XtPointer clientData, XtPointer)
{
    delete static_cast<OptionBinding*>(clientData);
}

/**
Posts the menu of an option button below it.
*/
void postOptions(Widget button, XtPointer clientData, XtPointer)
{
    Widget menu = static_cast<Widget>(clientData);
    Dimension height = 0;
    XtVaGetValues(button, XmNheight, &height, nullptr);
    Position rootX = 0;
    Position rootY = 0;
    XtTranslateCoords(button, 0, static_cast<Position>(height), &rootX, &rootY);
    // Motif places popup menus at the root position of a button event
    XButtonPressedEvent position;
    memset(&position, 0, sizeof(position));
    position.type = ButtonPress;
    position.x_root = rootX;
    position.y_root = rootY;
    XmMenuPosition(menu, &position);
    XtManageChild(menu);
}

}

Widget XmPanelWidgets::createPanel(Widget parent, const char* name, int x,
                                   int y, int width, int height, bool managed)
{
    // Panels are the first Motif widgets of the windows
    if (XtIsShell(parent)) XmWidgetSupport::requireMotifShell(parent);
    Arg args[9]; Cardinal n = 0;
    XtSetArg(args[n], XmNx, x); ++n;
    XtSetArg(args[n], XmNy, y); ++n;
    XtSetArg(args[n], XmNwidth, width); ++n;
    XtSetArg(args[n], XmNheight, height); ++n;
    XtSetArg(args[n], XmNmarginWidth, 0); ++n;
    XtSetArg(args[n], XmNmarginHeight, 0); ++n;
    XtSetArg(args[n], XmNshadowThickness, 0); ++n;
    XtSetArg(args[n], XmNresizePolicy, XmRESIZE_NONE); ++n;
    XtSetArg(args[n], XmNallowOverlap, True); ++n;
    return managed ?
        XtCreateManagedWidget(name, xmBulletinBoardWidgetClass, parent,
                              args, n) :
        XtCreateWidget(name, xmBulletinBoardWidgetClass, parent, args, n);
}

Widget XmPanelWidgets::createLabel(Widget parent, const std::string& text,
                                   XFontSet fontSet, Justify justify,
                                   int x, int y, int width, int height)
{
    unsigned char alignment = justify == LEFT ? XmALIGNMENT_BEGINNING :
        justify == RIGHT ? XmALIGNMENT_END : XmALIGNMENT_CENTER;
    XmString label = XmWidgetSupport::createString(text);
    XmRenderTable fonts = XmWidgetSupport::createRenderTable(parent, fontSet);
    Arg args[9]; Cardinal n = 0;
    XtSetArg(args[n], XmNlabelString, label); ++n;
    XtSetArg(args[n], XmNalignment, alignment); ++n;
    XtSetArg(args[n], XmNrecomputeSize, False); ++n;
    XtSetArg(args[n], XmNx, x); ++n;
    XtSetArg(args[n], XmNy, y); ++n;
    XtSetArg(args[n], XmNwidth, width); ++n;
    XtSetArg(args[n], XmNheight, height); ++n;
    if (fonts != nullptr) {
        XtSetArg(args[n], XmNrenderTable, fonts); ++n;
    }
    Widget widget = XtCreateManagedWidget("panelLabel", xmLabelWidgetClass,
                                          parent, args, n);
    XmStringFree(label);
    if (fonts != nullptr) XmRenderTableFree(fonts);
    return widget;
}

void XmPanelWidgets::setLabel(Widget label, const std::string& text)
{
    // The label does not recompute its size: its geometry stays
    XmWidgetSupport::setLabelString(label, text);
}

Widget XmPanelWidgets::createTextField(Widget parent, const std::string& text,
                                       XFontSet fontSet,
                                       int x, int y, int width, int height,
                                       ActivateProc activate,
                                       void* clientData)
{
    XmRenderTable fonts = XmWidgetSupport::createRenderTable(parent, fontSet);
    Arg args[9]; Cardinal n = 0;
    XtSetArg(args[n], XmNvalue, text.c_str()); ++n;
    XtSetArg(args[n], XmNx, x); ++n;
    XtSetArg(args[n], XmNy, y); ++n;
    XtSetArg(args[n], XmNwidth, width); ++n;
    XtSetArg(args[n], XmNheight, height); ++n;
    // The rows of the panels are thin
    XtSetArg(args[n], XmNmarginHeight, 1); ++n;
    XtSetArg(args[n], XmNhighlightThickness, 1); ++n;
    if (fonts != nullptr) {
        XtSetArg(args[n], XmNrenderTable, fonts); ++n;
    }
    Widget field = XtCreateManagedWidget("panelTextField",
        xmTextFieldWidgetClass, parent, args, n);
    if (fonts != nullptr) XmRenderTableFree(fonts);
    setInvalid(field, false);
    XtWidgetSupport::bindProc(field, XmNactivateCallback, activate,
                              clientData);
    return field;
}

std::string XmPanelWidgets::getText(Widget field)
{
    char* value = XmTextFieldGetString(field);
    std::string text = value != nullptr ? value : "";
    XtFree(value);
    return text;
}

void XmPanelWidgets::setText(Widget field, const std::string& text)
{
    XmTextFieldSetString(field, const_cast<char*>(text.c_str()));
    setInvalid(field, false);
}

void XmPanelWidgets::setInvalid(Widget field, bool invalid)
{
    XtWidgetSupport::setColor(field, XmNbackground,
                              invalid ? INVALID_BACKGROUND : NORMAL_BACKGROUND);
    XtWidgetSupport::setColor(field, XmNforeground, TEXT_FOREGROUND);
}

void XmPanelWidgets::setForeground(Widget widget, const char* colorName)
{
    XtWidgetSupport::setColor(widget, XmNforeground, colorName);
}

Widget XmPanelWidgets::createPushButton(Widget parent,
                                        const std::string& label,
                                        XFontSet fontSet,
                                        int x, int y, int width, int height,
                                        ActivateProc activate,
                                        void* clientData)
{
    XmString text = XmWidgetSupport::createString(label);
    XmRenderTable fonts = XmWidgetSupport::createRenderTable(parent, fontSet);
    Arg args[9]; Cardinal n = 0;
    XtSetArg(args[n], XmNlabelString, text); ++n;
    XtSetArg(args[n], XmNrecomputeSize, False); ++n;
    XtSetArg(args[n], XmNtraversalOn, False); ++n;
    XtSetArg(args[n], XmNx, x); ++n;
    XtSetArg(args[n], XmNy, y); ++n;
    XtSetArg(args[n], XmNwidth, width); ++n;
    XtSetArg(args[n], XmNheight, height); ++n;
    if (fonts != nullptr) {
        XtSetArg(args[n], XmNrenderTable, fonts); ++n;
    }
    Widget button = XtCreateManagedWidget("panelButton",
        xmPushButtonWidgetClass, parent, args, n);
    XmStringFree(text);
    if (fonts != nullptr) XmRenderTableFree(fonts);
    XtWidgetSupport::bindProc(button, XmNactivateCallback, activate,
                              clientData);
    return button;
}

Widget XmPanelWidgets::createOptionButton(
    Widget parent, const std::string& placeholder,
    const std::vector<std::string>& options, XFontSet fontSet,
    int x, int y, int width, int height, OptionProc choose, void* clientData)
{
    // A button of the given size showing the choice, as the Athena one: a
    // Motif option menu sizes itself to its longest option
    XmString text = XmWidgetSupport::createString(placeholder);
    XmRenderTable fonts = XmWidgetSupport::createRenderTable(parent, fontSet);
    Arg args[10]; Cardinal n = 0;
    XtSetArg(args[n], XmNlabelString, text); ++n;
    XtSetArg(args[n], XmNalignment, XmALIGNMENT_BEGINNING); ++n;
    XtSetArg(args[n], XmNrecomputeSize, False); ++n;
    XtSetArg(args[n], XmNtraversalOn, False); ++n;
    XtSetArg(args[n], XmNx, x); ++n;
    XtSetArg(args[n], XmNy, y); ++n;
    XtSetArg(args[n], XmNwidth, width); ++n;
    XtSetArg(args[n], XmNheight, height); ++n;
    if (fonts != nullptr) {
        XtSetArg(args[n], XmNrenderTable, fonts); ++n;
    }
    Widget button = XtCreateManagedWidget("optionButton",
        xmPushButtonWidgetClass, parent, args, n);
    XmStringFree(text);
    if (fonts != nullptr) XmRenderTableFree(fonts);

    // A popup child of the button, destroyed with it
    Widget menu = XmWidgetSupport::createPopupMenu(button, "optionMenu");
    for (size_t i = 0; i < options.size(); ++i) {
        Widget item = XmWidgetSupport::createMenuItem(menu, options[i],
                                                      fontSet);
        OptionBinding* binding = new OptionBinding;
        binding->button = button;
        binding->label = options[i];
        binding->index = static_cast<int>(i);
        binding->choose = choose;
        binding->clientData = clientData;
        XtAddCallback(item, XmNactivateCallback, optionChosen, binding);
        XtAddCallback(item, XmNdestroyCallback, freeOptionBinding, binding);
    }
    XtAddCallback(button, XmNactivateCallback, postOptions, menu);
    return button;
}
