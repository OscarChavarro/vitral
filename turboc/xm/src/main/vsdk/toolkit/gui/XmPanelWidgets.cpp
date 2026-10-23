#include <string.h>

// Xt names its strings and objects as the vitral String and Object
// classes: they are renamed while its headers are read (see
// XtIntrinsics.h)
#define String XtString
#define Object XtObject
#include <Xm/Xm.h>
#include <Xm/BulletinB.h>
#include <Xm/Label.h>
#include <Xm/PushB.h>
#include <Xm/RowColumn.h>
#include <Xm/TextF.h>
#undef Object
#undef String

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/XmPanelWidgets.h"
#include "vsdk/toolkit/gui/XmWidgetSupport.h"
#include "vsdk/toolkit/gui/XtTextEncoding.h"
#include "vsdk/toolkit/gui/XtWidgetSupport.h"
#include "java/util/ArrayList.txx"


static const char* const INVALID_BACKGROUND = "#ffc8c8";
static const char* const NORMAL_BACKGROUND = "white";
/// The foreground of the default Motif colors may be a light one
static const char* const TEXT_FOREGROUND = "black";

/**
An option of an option button. Freed with its menu item.
*/
struct XmOptionBinding {
    Widget button;
    String label;
    int index;
    XtPanelWidgets::OptionProc choose;
    void* clientData;
};

static void optionChosen(Widget, XtPointer clientData, XtPointer)
{
    XmOptionBinding* binding = ((XmOptionBinding*)(clientData));
    // The button does not recompute its size: its geometry stays
    XmWidgetSupport::setLabelString(binding->button, binding->label);
    if (binding->choose != NULL)
        binding->choose(binding->button, binding->index, binding->clientData);
}

static void freeOptionBinding(Widget, XtPointer clientData, XtPointer)
{
    delete ((XmOptionBinding*)(clientData));
}

/**
Posts the menu of an option button below it.
*/
static void postOptions(Widget button, XtPointer clientData, XtPointer)
{
    Widget menu = ((Widget)(clientData));
    Dimension height = 0;
    XtVaGetValues(button, XmNheight, &height, NULL);
    Position rootX = 0;
    Position rootY = 0;
    XtTranslateCoords(button, 0, ((Position)(height)), &rootX, &rootY);
    // Motif places popup menus at the root position of a button event
    XButtonPressedEvent position;
    memset(&position, 0, sizeof(position));
    position.type = ButtonPress;
    position.x_root = rootX;
    position.y_root = rootY;
    XmMenuPosition(menu, &position);
    XtManageChild(menu);
}


Widget XmPanelWidgets::createPanel(Widget parent, const char* name, int x,
                                   int y, int width, int height, BOOLEAN managed)
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

Widget XmPanelWidgets::createLabel(Widget parent, const String& text,
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
    if (fonts != NULL) {
        XtSetArg(args[n], XmNrenderTable, fonts); ++n;
    }
    Widget widget = XtCreateManagedWidget("panelLabel", xmLabelWidgetClass,
                                          parent, args, n);
    XmStringFree(label);
    if (fonts != NULL) XmRenderTableFree(fonts);
    return widget;
}

void XmPanelWidgets::setLabel(Widget label, const String& text)
{
    // The label does not recompute its size: its geometry stays
    XmWidgetSupport::setLabelString(label, text);
}

Widget XmPanelWidgets::createTextField(Widget parent, const String& text,
                                       XFontSet fontSet,
                                       int x, int y, int width, int height,
                                       ActivateProc activate,
                                       void* clientData)
{
    XmRenderTable fonts = XmWidgetSupport::createRenderTable(parent, fontSet);
    const String localeText = XtTextEncoding::toLocale(text);
    Arg args[9]; Cardinal n = 0;
    XtSetArg(args[n], XmNvalue, localeText.c_str()); ++n;
    XtSetArg(args[n], XmNx, x); ++n;
    XtSetArg(args[n], XmNy, y); ++n;
    XtSetArg(args[n], XmNwidth, width); ++n;
    XtSetArg(args[n], XmNheight, height); ++n;
    // The rows of the panels are thin
    XtSetArg(args[n], XmNmarginHeight, 1); ++n;
    XtSetArg(args[n], XmNhighlightThickness, 1); ++n;
    if (fonts != NULL) {
        XtSetArg(args[n], XmNrenderTable, fonts); ++n;
    }
    Widget field = XtCreateManagedWidget("panelTextField",
        xmTextFieldWidgetClass, parent, args, n);
    if (fonts != NULL) XmRenderTableFree(fonts);
    setInvalid(field, FALSE);
    XtWidgetSupport::bindProc(field, XmNactivateCallback, activate,
                              clientData);
    return field;
}

String XmPanelWidgets::getText(Widget field)
{
    char* value = XmTextFieldGetString(field);
    String text = value != NULL ? XtTextEncoding::fromLocale(value) :
        String("");
    XtFree(value);
    return text;
}

void XmPanelWidgets::setText(Widget field, const String& text)
{
    const String localeText = XtTextEncoding::toLocale(text);
    XmTextFieldSetString(field, ((char*)(localeText.c_str())));
    setInvalid(field, FALSE);
}

void XmPanelWidgets::setInvalid(Widget field, BOOLEAN invalid)
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
                                        const String& label,
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
    if (fonts != NULL) {
        XtSetArg(args[n], XmNrenderTable, fonts); ++n;
    }
    Widget button = XtCreateManagedWidget("panelButton",
        xmPushButtonWidgetClass, parent, args, n);
    XmStringFree(text);
    if (fonts != NULL) XmRenderTableFree(fonts);
    XtWidgetSupport::bindProc(button, XmNactivateCallback, activate,
                              clientData);
    return button;
}

Widget XmPanelWidgets::createOptionButton(
    Widget parent, const String& placeholder,
    const ArrayList<String>& options, XFontSet fontSet,
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
    if (fonts != NULL) {
        XtSetArg(args[n], XmNrenderTable, fonts); ++n;
    }
    Widget button = XtCreateManagedWidget("optionButton",
        xmPushButtonWidgetClass, parent, args, n);
    XmStringFree(text);
    if (fonts != NULL) XmRenderTableFree(fonts);

    // A popup child of the button, destroyed with it
    Widget menu = XmWidgetSupport::createPopupMenu(button, "optionMenu");
    for ( long i = 0; i < options.size(); ++i) {
        Widget item = XmWidgetSupport::createMenuItem(menu, options[i],
                                                      fontSet);
        XmOptionBinding* binding = new XmOptionBinding;
        binding->button = button;
        binding->label = options[i];
        binding->index = ((int)(i));
        binding->choose = choose;
        binding->clientData = clientData;
        XtAddCallback(item, XmNactivateCallback, optionChosen, binding);
        XtAddCallback(item, XmNdestroyCallback, freeOptionBinding, binding);
    }
    XtAddCallback(button, XmNactivateCallback, postOptions, menu);
    return button;
}
