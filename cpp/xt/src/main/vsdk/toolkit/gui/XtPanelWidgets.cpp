#include <cstring>
#include <map>
#include <vector>

#include <X11/StringDefs.h>
#include <X11/Shell.h>
#include <X11/Xaw/AsciiText.h>
#include <X11/Xaw/Label.h>

#include "vsdk/toolkit/gui/XtPanelWidgets.h"

namespace {

struct TextFieldBinding {
    XtPanelWidgets::ActivateProc activate;
    void* clientData;
};

std::map<Widget, TextFieldBinding>& textFieldBindings()
{
    static std::map<Widget, TextFieldBinding> bindings;
    return bindings;
}

const char* const INVALID_BACKGROUND = "#ffc8c8";
const char* const NORMAL_BACKGROUND = "white";

void activateAction(Widget w, XEvent*, String*, Cardinal*)
{
    std::map<Widget, TextFieldBinding>::iterator it =
        textFieldBindings().find(w);
    if (it != textFieldBindings().end() && it->second.activate != nullptr)
        it->second.activate(w, it->second.clientData);
}

void focusAction(Widget w, XEvent*, String*, Cardinal*)
{
    Widget shell = w;
    while (shell != nullptr && !XtIsShell(shell)) shell = XtParent(shell);
    if (shell != nullptr) XtSetKeyboardFocus(shell, w);
}

void textFieldDestroyed(Widget w, XtPointer, XtPointer)
{
    textFieldBindings().erase(w);
}

void registerActions(XtAppContext appContext)
{
    static std::vector<XtAppContext> registered;
    for (size_t i = 0; i < registered.size(); ++i)
        if (registered[i] == appContext) return;
    static XtActionsRec actions[] = {
        { const_cast<char*>("vitralTextFieldActivate"), activateAction },
        { const_cast<char*>("vitralTextFieldFocus"), focusAction }
    };
    XtAppAddActions(appContext, actions, XtNumber(actions));
    registered.push_back(appContext);
}

void setColor(Widget widget, const char* resource, const char* colorName)
{
    XtVaSetValues(widget, XtVaTypedArg, resource, XtRString, colorName,
                  static_cast<int>(strlen(colorName) + 1), nullptr);
}

}

void XtPanelWidgets::removeAll(Widget container)
{
    WidgetList children = nullptr;
    Cardinal count = 0;
    XtVaGetValues(container, XtNchildren, &children,
                  XtNnumChildren, &count, nullptr);
    // Destroying changes the list of children: work over a copy
    std::vector<Widget> copy(children, children + count);
    for (size_t i = 0; i < copy.size(); ++i)
        XtDestroyWidget(copy[i]);
}

Widget XtPanelWidgets::createLabel(Widget parent, const std::string& text,
                                   XFontSet fontSet, Justify justify,
                                   int x, int y, int width, int height)
{
    XtJustify xtJustify = justify == LEFT ? XtJustifyLeft :
        justify == RIGHT ? XtJustifyRight : XtJustifyCenter;
    Arg args[9]; Cardinal n = 0;
    XtSetArg(args[n], XtNlabel, text.c_str()); ++n;
    XtSetArg(args[n], XtNjustify, xtJustify); ++n;
    XtSetArg(args[n], XtNx, x); ++n;
    XtSetArg(args[n], XtNy, y); ++n;
    XtSetArg(args[n], XtNwidth, width); ++n;
    XtSetArg(args[n], XtNheight, height); ++n;
    XtSetArg(args[n], XtNborderWidth, 0); ++n;
    if (fontSet != nullptr) {
        XtSetArg(args[n], XtNinternational, True); ++n;
        XtSetArg(args[n], XtNfontSet, fontSet); ++n;
    }
    return XtCreateManagedWidget("panelLabel", labelWidgetClass, parent,
                                 args, n);
}

void XtPanelWidgets::setLabel(Widget label, const std::string& text)
{
    // Label asks its parent to resize to the new text: keep its geometry
    Dimension width = 0;
    Dimension height = 0;
    XtVaGetValues(label, XtNwidth, &width, XtNheight, &height, nullptr);
    XtVaSetValues(label, XtNlabel, text.c_str(), XtNwidth, width,
                  XtNheight, height, nullptr);
}

Widget XtPanelWidgets::createTextField(Widget parent, const std::string& text,
                                       XFontSet fontSet,
                                       int x, int y, int width, int height,
                                       ActivateProc activate, void* clientData)
{
    registerActions(XtWidgetToApplicationContext(parent));
    Arg args[11]; Cardinal n = 0;
    XtSetArg(args[n], XtNstring, text.c_str()); ++n;
    XtSetArg(args[n], XtNeditType, XawtextEdit); ++n;
    XtSetArg(args[n], XtNtype, XawAsciiString); ++n;
    XtSetArg(args[n], XtNuseStringInPlace, False); ++n;
    XtSetArg(args[n], XtNx, x); ++n;
    XtSetArg(args[n], XtNy, y); ++n;
    XtSetArg(args[n], XtNwidth, width); ++n;
    XtSetArg(args[n], XtNheight, height); ++n;
    XtSetArg(args[n], XtNresize, XawtextResizeNever); ++n;
    if (fontSet != nullptr) {
        XtSetArg(args[n], XtNinternational, True); ++n;
        XtSetArg(args[n], XtNfontSet, fontSet); ++n;
    }
    Widget field = XtCreateManagedWidget("panelTextField",
        asciiTextWidgetClass, parent, args, n);
    XtOverrideTranslations(field, XtParseTranslationTable(
        "<Key>Return: vitralTextFieldActivate()\n"
        "<Key>KP_Enter: vitralTextFieldActivate()\n"
        "<Btn1Down>: vitralTextFieldFocus() select-start()"));
    TextFieldBinding binding = { activate, clientData };
    textFieldBindings()[field] = binding;
    XtAddCallback(field, XtNdestroyCallback, textFieldDestroyed, nullptr);
    return field;
}

std::string XtPanelWidgets::getText(Widget field)
{
    String value = nullptr;
    XtVaGetValues(field, XtNstring, &value, nullptr);
    return value != nullptr ? std::string(value) : std::string();
}

void XtPanelWidgets::setText(Widget field, const std::string& text)
{
    XtVaSetValues(field, XtNstring, text.c_str(), nullptr);
    setInvalid(field, false);
}

void XtPanelWidgets::setInvalid(Widget field, bool invalid)
{
    setColor(field, XtNbackground,
             invalid ? INVALID_BACKGROUND : NORMAL_BACKGROUND);
}

void XtPanelWidgets::setForeground(Widget widget, const char* colorName)
{
    setColor(widget, XtNforeground, colorName);
}
