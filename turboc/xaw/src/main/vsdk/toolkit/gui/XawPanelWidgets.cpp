#include "java/util/TreeMap.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"

// Xt names its strings and objects as the vitral String and Object
// classes: they are renamed while its headers are read (see
// XtIntrinsics.h)
#define String XtString
#define Object XtObject
// First: the X11R6 headers (i.e. Debian 1.1) do not include it themselves
#include <X11/Intrinsic.h>
#include <X11/StringDefs.h>
#include <X11/Shell.h>
#include <X11/Composite.h>
#include <X11/Xaw/AsciiText.h>
#include <X11/Xaw/Command.h>
#include <X11/Xaw/Label.h>
#include <X11/Xaw/MenuButton.h>
#include <X11/Xaw/SimpleMenu.h>
#include <X11/Xaw/SmeBSB.h>
#undef Object
#undef String

#include "vsdk/toolkit/gui/XawPanelWidgets.h"
#include "vsdk/toolkit/gui/XtTextEncoding.h"
#include "vsdk/toolkit/gui/XtWidgetSupport.h"


struct XawTextFieldBinding {
    XtPanelWidgets::ActivateProc activate;
    void* clientData;
};

static TreeMap<Widget, XawTextFieldBinding>& textFieldBindings()
{
    static TreeMap<Widget, XawTextFieldBinding> bindings;
    return bindings;
}

static const char* const INVALID_BACKGROUND = "#ffc8c8";
static const char* const NORMAL_BACKGROUND = "white";

static void activateAction(Widget w, XEvent*, XtString*, Cardinal*)
{
    XawTextFieldBinding* binding = textFieldBindings().get(w);
    if (binding != NULL && binding->activate != NULL)
        binding->activate(w, binding->clientData);
}

static void focusAction(Widget w, XEvent*, XtString*, Cardinal*)
{
    Widget shell = w;
    while (shell != NULL && !XtIsShell(shell)) shell = XtParent(shell);
    if (shell != NULL) XtSetKeyboardFocus(shell, w);
}

static void textFieldDestroyed(Widget w, XtPointer, XtPointer)
{
    textFieldBindings().remove(w);
}

static void registerActions(XtAppContext appContext)
{
    static ArrayList<XtAppContext> registered;
    for ( long i = 0; i < registered.size(); ++i)
        if (registered[i] == appContext) return;
    static XtActionsRec actions[] = {
        { ((char*)("vitralTextFieldActivate")), activateAction },
        { ((char*)("vitralTextFieldFocus")), focusAction }
    };
    XtAppAddActions(appContext, actions, XtNumber(actions));
    registered.add(appContext);
}

/**
An option of an option button. Freed with its entry.
*/
struct XawOptionBinding {
    Widget button;
    String label;
    int index;
    XtPanelWidgets::OptionProc choose;
    void* clientData;
};

static void optionChosen(Widget, XtPointer clientData, XtPointer)
{
    XawOptionBinding* binding = ((XawOptionBinding*)(clientData));
    // Label asks its parent to resize to the new text: keep its geometry
    Dimension width = 0;
    Dimension height = 0;
    XtVaGetValues(binding->button, XtNwidth, &width, XtNheight, &height,
                  NULL);
    const String shownLabel = XtTextEncoding::toLocale(binding->label);
    XtVaSetValues(binding->button, XtNlabel, shownLabel.c_str(),
                  XtNwidth, width, XtNheight, height, NULL);
    if (binding->choose != NULL)
        binding->choose(binding->button, binding->index, binding->clientData);
}

static void freeOptionBinding(Widget, XtPointer clientData, XtPointer)
{
    delete ((XawOptionBinding*)(clientData));
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

Widget XawPanelWidgets::createLabel(Widget parent, const String& text,
                                    XFontSet fontSet, Justify justify,
                                    int x, int y, int width, int height)
{
    XtJustify xtJustify = justify == LEFT ? XtJustifyLeft :
        justify == RIGHT ? XtJustifyRight : XtJustifyCenter;
    const String shownText = XtTextEncoding::toLocale(text);
    Arg args[9]; Cardinal n = 0;
    XtSetArg(args[n], XtNlabel, shownText.c_str()); ++n;
    XtSetArg(args[n], XtNjustify, xtJustify); ++n;
    XtSetArg(args[n], XtNx, x); ++n;
    XtSetArg(args[n], XtNy, y); ++n;
    XtSetArg(args[n], XtNwidth, width); ++n;
    XtSetArg(args[n], XtNheight, height); ++n;
    XtSetArg(args[n], XtNborderWidth, 0); ++n;
    if (fontSet != NULL) {
        XtSetArg(args[n], XtNinternational, True); ++n;
        XtSetArg(args[n], XtNfontSet, fontSet); ++n;
    }
    return XtCreateManagedWidget("panelLabel", labelWidgetClass, parent,
                                 args, n);
}

void XawPanelWidgets::setLabel(Widget label, const String& text)
{
    // Label asks its parent to resize to the new text: keep its geometry
    Dimension width = 0;
    Dimension height = 0;
    XtVaGetValues(label, XtNwidth, &width, XtNheight, &height, NULL);
    const String shownText = XtTextEncoding::toLocale(text);
    XtVaSetValues(label, XtNlabel, shownText.c_str(), XtNwidth, width,
                  XtNheight, height, NULL);
}

Widget XawPanelWidgets::createTextField(Widget parent, const String& text,
                                        XFontSet fontSet,
                                        int x, int y, int width, int height,
                                        ActivateProc activate,
                                        void* clientData)
{
    registerActions(XtWidgetToApplicationContext(parent));
    const String shownText = XtTextEncoding::toLocale(text);
    Arg args[11]; Cardinal n = 0;
    XtSetArg(args[n], XtNstring, shownText.c_str()); ++n;
    XtSetArg(args[n], XtNeditType, XawtextEdit); ++n;
    XtSetArg(args[n], XtNtype, XawAsciiString); ++n;
    XtSetArg(args[n], XtNuseStringInPlace, False); ++n;
    XtSetArg(args[n], XtNx, x); ++n;
    XtSetArg(args[n], XtNy, y); ++n;
    XtSetArg(args[n], XtNwidth, width); ++n;
    XtSetArg(args[n], XtNheight, height); ++n;
    XtSetArg(args[n], XtNresize, XawtextResizeNever); ++n;
    if (fontSet != NULL) {
        XtSetArg(args[n], XtNinternational, True); ++n;
        XtSetArg(args[n], XtNfontSet, fontSet); ++n;
    }
    Widget field = XtCreateManagedWidget("panelTextField",
        asciiTextWidgetClass, parent, args, n);
    XtOverrideTranslations(field, XtParseTranslationTable(
        "<Key>Return: vitralTextFieldActivate()\n"
        "<Key>KP_Enter: vitralTextFieldActivate()\n"
        "<Btn1Down>: vitralTextFieldFocus() select-start()"));
    XawTextFieldBinding binding = { activate, clientData };
    textFieldBindings().put(field, binding);
    XtAddCallback(field, XtNdestroyCallback, textFieldDestroyed, NULL);
    return field;
}

String XawPanelWidgets::getText(Widget field)
{
    XtString value = NULL;
    XtVaGetValues(field, XtNstring, &value, NULL);
    return value != NULL ? XtTextEncoding::fromLocale(String(value)) :
        String();
}

void XawPanelWidgets::setText(Widget field, const String& text)
{
    const String shownText = XtTextEncoding::toLocale(text);
    XtVaSetValues(field, XtNstring, shownText.c_str(), NULL);
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
                                         const String& label,
                                         XFontSet fontSet,
                                         int x, int y, int width, int height,
                                         ActivateProc activate,
                                         void* clientData)
{
    const String shownLabel = XtTextEncoding::toLocale(label);
    Arg args[8]; Cardinal n = 0;
    XtSetArg(args[n], XtNlabel, shownLabel.c_str()); ++n;
    XtSetArg(args[n], XtNx, x); ++n;
    XtSetArg(args[n], XtNy, y); ++n;
    XtSetArg(args[n], XtNwidth, width); ++n;
    XtSetArg(args[n], XtNheight, height); ++n;
    if (fontSet != NULL) {
        XtSetArg(args[n], XtNinternational, True); ++n;
        XtSetArg(args[n], XtNfontSet, fontSet); ++n;
    }
    Widget button = XtCreateManagedWidget("panelButton", commandWidgetClass,
                                          parent, args, n);
    XtWidgetSupport::bindProc(button, XtNcallback, activate, clientData);
    return button;
}

Widget XawPanelWidgets::createOptionButton(
    Widget parent, const String& placeholder,
    const ArrayList<String>& options, XFontSet fontSet,
    int x, int y, int width, int height, OptionProc choose, void* clientData)
{
    // Menu buttons find their popup by name
    const String menuName = XtWidgetSupport::uniqueName("vitralOptions");
    const String shownPlaceholder = XtTextEncoding::toLocale(placeholder);
    Arg args[10]; Cardinal n = 0;
    XtSetArg(args[n], XtNlabel, shownPlaceholder.c_str()); ++n;
    XtSetArg(args[n], XtNmenuName, menuName.c_str()); ++n;
    XtSetArg(args[n], XtNjustify, XtJustifyLeft); ++n;
    XtSetArg(args[n], XtNx, x); ++n;
    XtSetArg(args[n], XtNy, y); ++n;
    XtSetArg(args[n], XtNwidth, width); ++n;
    XtSetArg(args[n], XtNheight, height); ++n;
    if (fontSet != NULL) {
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
    for ( long i = 0; i < options.size(); ++i) {
        const String shownOption = XtTextEncoding::toLocale(options[i]);
        Arg itemArgs[3]; Cardinal itemN = 0;
        XtSetArg(itemArgs[itemN], XtNlabel, shownOption.c_str()); ++itemN;
        if (fontSet != NULL) {
            XtSetArg(itemArgs[itemN], XtNinternational, True); ++itemN;
            XtSetArg(itemArgs[itemN], XtNfontSet, fontSet); ++itemN;
        }
        Widget item = XtCreateManagedWidget("optionItem", smeBSBObjectClass,
                                            menu, itemArgs, itemN);
        XawOptionBinding* binding = new XawOptionBinding;
        binding->button = button;
        binding->label = options[i];
        binding->index = ((int)(i));
        binding->choose = choose;
        binding->clientData = clientData;
        XtAddCallback(item, XtNcallback, optionChosen, binding);
        XtAddCallback(item, XtNdestroyCallback, freeOptionBinding, binding);
    }
    return button;
}
