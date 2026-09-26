#include <map>
#include <string>
#include <vector>

#include <X11/StringDefs.h>
#include <X11/Shell.h>
#include <X11/Composite.h>
#include <X11/Xaw/AsciiText.h>
#include <X11/Xaw/Command.h>
#include <X11/Xaw/Label.h>
#include <X11/Xaw/MenuButton.h>
#include <X11/Xaw/SimpleMenu.h>
#include <X11/Xaw/SmeBSB.h>

#include "vsdk/toolkit/gui/XawPanelWidgets.h"
#include "vsdk/toolkit/gui/XtWidgetSupport.h"

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

/**
An option of an option button. Freed with its entry.
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
    // Label asks its parent to resize to the new text: keep its geometry
    Dimension width = 0;
    Dimension height = 0;
    XtVaGetValues(binding->button, XtNwidth, &width, XtNheight, &height,
                  nullptr);
    XtVaSetValues(binding->button, XtNlabel, binding->label.c_str(),
                  XtNwidth, width, XtNheight, height, nullptr);
    if (binding->choose != nullptr)
        binding->choose(binding->button, binding->index, binding->clientData);
}

void freeOptionBinding(Widget, XtPointer clientData, XtPointer)
{
    delete static_cast<OptionBinding*>(clientData);
}

}

Widget XawPanelWidgets::createPanel(Widget parent, const char* name, int x,
                                    int y, int width, int height, bool managed)
{
    Arg args[4]; Cardinal n = 0;
    XtSetArg(args[n], XtNx, x); ++n;
    XtSetArg(args[n], XtNy, y); ++n;
    XtSetArg(args[n], XtNwidth, width); ++n;
    XtSetArg(args[n], XtNheight, height); ++n;
    return managed ?
        XtCreateManagedWidget(name, compositeWidgetClass, parent, args, n) :
        XtCreateWidget(name, compositeWidgetClass, parent, args, n);
}

Widget XawPanelWidgets::createLabel(Widget parent, const std::string& text,
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

void XawPanelWidgets::setLabel(Widget label, const std::string& text)
{
    // Label asks its parent to resize to the new text: keep its geometry
    Dimension width = 0;
    Dimension height = 0;
    XtVaGetValues(label, XtNwidth, &width, XtNheight, &height, nullptr);
    XtVaSetValues(label, XtNlabel, text.c_str(), XtNwidth, width,
                  XtNheight, height, nullptr);
}

Widget XawPanelWidgets::createTextField(Widget parent, const std::string& text,
                                        XFontSet fontSet,
                                        int x, int y, int width, int height,
                                        ActivateProc activate,
                                        void* clientData)
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

std::string XawPanelWidgets::getText(Widget field)
{
    String value = nullptr;
    XtVaGetValues(field, XtNstring, &value, nullptr);
    return value != nullptr ? std::string(value) : std::string();
}

void XawPanelWidgets::setText(Widget field, const std::string& text)
{
    XtVaSetValues(field, XtNstring, text.c_str(), nullptr);
    setInvalid(field, false);
}

void XawPanelWidgets::setInvalid(Widget field, bool invalid)
{
    XtWidgetSupport::setColor(field, XtNbackground,
                              invalid ? INVALID_BACKGROUND : NORMAL_BACKGROUND);
}

void XawPanelWidgets::setForeground(Widget widget, const char* colorName)
{
    XtWidgetSupport::setColor(widget, XtNforeground, colorName);
}

Widget XawPanelWidgets::createPushButton(Widget parent,
                                         const std::string& label,
                                         XFontSet fontSet,
                                         int x, int y, int width, int height,
                                         ActivateProc activate,
                                         void* clientData)
{
    Arg args[8]; Cardinal n = 0;
    XtSetArg(args[n], XtNlabel, label.c_str()); ++n;
    XtSetArg(args[n], XtNx, x); ++n;
    XtSetArg(args[n], XtNy, y); ++n;
    XtSetArg(args[n], XtNwidth, width); ++n;
    XtSetArg(args[n], XtNheight, height); ++n;
    if (fontSet != nullptr) {
        XtSetArg(args[n], XtNinternational, True); ++n;
        XtSetArg(args[n], XtNfontSet, fontSet); ++n;
    }
    Widget button = XtCreateManagedWidget("panelButton", commandWidgetClass,
                                          parent, args, n);
    XtWidgetSupport::bindProc(button, XtNcallback, activate, clientData);
    return button;
}

Widget XawPanelWidgets::createOptionButton(
    Widget parent, const std::string& placeholder,
    const std::vector<std::string>& options, XFontSet fontSet,
    int x, int y, int width, int height, OptionProc choose, void* clientData)
{
    // Menu buttons find their popup by name
    const std::string menuName = XtWidgetSupport::uniqueName("vitralOptions");
    Arg args[10]; Cardinal n = 0;
    XtSetArg(args[n], XtNlabel, placeholder.c_str()); ++n;
    XtSetArg(args[n], XtNmenuName, menuName.c_str()); ++n;
    XtSetArg(args[n], XtNjustify, XtJustifyLeft); ++n;
    XtSetArg(args[n], XtNx, x); ++n;
    XtSetArg(args[n], XtNy, y); ++n;
    XtSetArg(args[n], XtNwidth, width); ++n;
    XtSetArg(args[n], XtNheight, height); ++n;
    if (fontSet != nullptr) {
        XtSetArg(args[n], XtNinternational, True); ++n;
        XtSetArg(args[n], XtNfontSet, fontSet); ++n;
    }
    Widget button = XtCreateManagedWidget("optionButton",
        menuButtonWidgetClass, parent, args, n);

    // The menu is a popup child of its button, destroyed with it; it uses
    // the visual of the shell (the GLX one is not the default)
    XtWidgetSupport::ShellVisual shellVisual =
        XtWidgetSupport::shellVisualOf(button);
    Arg menuArgs[3]; Cardinal menuN = 0;
    if (shellVisual.depth != 0) {
        XtSetArg(menuArgs[menuN], XtNvisual, shellVisual.visual); ++menuN;
        XtSetArg(menuArgs[menuN], XtNdepth, shellVisual.depth); ++menuN;
        XtSetArg(menuArgs[menuN], XtNcolormap, shellVisual.colormap); ++menuN;
    }
    Widget menu = XtCreatePopupShell(menuName.c_str(), simpleMenuWidgetClass,
                                     button, menuArgs, menuN);
    for (size_t i = 0; i < options.size(); ++i) {
        Arg itemArgs[3]; Cardinal itemN = 0;
        XtSetArg(itemArgs[itemN], XtNlabel, options[i].c_str()); ++itemN;
        if (fontSet != nullptr) {
            XtSetArg(itemArgs[itemN], XtNinternational, True); ++itemN;
            XtSetArg(itemArgs[itemN], XtNfontSet, fontSet); ++itemN;
        }
        Widget item = XtCreateManagedWidget("optionItem", smeBSBObjectClass,
                                            menu, itemArgs, itemN);
        OptionBinding* binding = new OptionBinding;
        binding->button = button;
        binding->label = options[i];
        binding->index = static_cast<int>(i);
        binding->choose = choose;
        binding->clientData = clientData;
        XtAddCallback(item, XtNcallback, optionChosen, binding);
        XtAddCallback(item, XtNdestroyCallback, freeOptionBinding, binding);
    }
    return button;
}
