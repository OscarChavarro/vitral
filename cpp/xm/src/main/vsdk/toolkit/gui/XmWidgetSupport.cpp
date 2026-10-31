// Before any other header that could include the Xt ones
#include "java/lang/String.h"
#include "vsdk/toolkit/gui/XmIntrinsics.h"

#include <cstdio>
#include <cstdlib>

#include "vsdk/toolkit/gui/XmWidgetSupport.h"
#include "vsdk/toolkit/gui/XtWidgetSupport.h"

namespace {

/**
@return the number of arguments set: the visual, depth and colormap of the
shell of the widget, if it has them
*/
Cardinal setShellVisualArgs(XtWidget widget, Arg* args)
{
    XtWidgetSupport::ShellVisual shellVisual =
        XtWidgetSupport::shellVisualOf(widget);
    Cardinal n = 0;
    if ( shellVisual.depth != 0 ) {
        XtSetArg(args[n], XmNvisual, shellVisual.visual); ++n;
        XtSetArg(args[n], XmNdepth, shellVisual.depth); ++n;
        XtSetArg(args[n], XmNcolormap, shellVisual.colormap); ++n;
    }
    return n;
}

}

void XmWidgetSupport::requireMotifShell(XtWidget shell)
{
    if ( !XmIsVendorShell(shell) ) {
        fprintf(stderr, "The shells do not have the Motif VendorShell: the "
                "program must be linked with libXm before libXt\n");
        exit(1);
    }
}

XmString XmWidgetSupport::createString(const java::String& text)
{
    return XmStringCreateLocalized(const_cast<char*>(text.c_str()));
}

java::String XmWidgetSupport::toText(XmString string)
{
    if ( string == nullptr ) {
        return java::String();
    }
    char* text = static_cast<char*>(XmStringUnparse(
        string, nullptr, XmCHARSET_TEXT, XmCHARSET_TEXT, nullptr, 0,
        XmOUTPUT_ALL));
    java::String result = text != nullptr ? text : "";
    XtFree(text);
    return result;
}

XmRenderTable XmWidgetSupport::createRenderTable(XtWidget widget,
                                                 XFontSet fontSet)
{
    if ( fontSet == nullptr ) {
        return nullptr;
    }
    Arg args[3]; Cardinal n = 0;
    XtSetArg(args[n], XmNfontType, XmFONT_IS_FONTSET); ++n;
    XtSetArg(args[n], XmNfont, fontSet); ++n;
    XtSetArg(args[n], XmNloadModel, XmLOAD_IMMEDIATE); ++n;
    XmRendition rendition = XmRenditionCreate(
        widget, const_cast<char*>(XmFONTLIST_DEFAULT_TAG), args, n);
    XmRenderTable table = XmRenderTableAddRenditions(nullptr, &rendition, 1,
                                                     XmMERGE_REPLACE);
    XmRenditionFree(rendition);
    return table;
}

void XmWidgetSupport::setLabelString(XtWidget widget, const java::String& text)
{
    XmString string = createString(text);
    XtVaSetValues(widget, XmNlabelString, string, nullptr);
    XmStringFree(string);
}

void XmWidgetSupport::setFontSet(XtWidget widget, XFontSet fontSet)
{
    XmRenderTable table = createRenderTable(widget, fontSet);
    if ( table == nullptr ) {
        return;
    }
    XtVaSetValues(widget, XmNrenderTable, table, nullptr);
    XmRenderTableFree(table);
}

XtWidget XmWidgetSupport::createPulldownMenu(XtWidget parent, const char* name)
{
    Arg args[3];
    Cardinal n = setShellVisualArgs(parent, args);
    return XmCreatePulldownMenu(parent, const_cast<char*>(name), args, n);
}

XtWidget XmWidgetSupport::createPopupMenu(XtWidget owner, const char* name)
{
    Arg args[4];
    Cardinal n = setShellVisualArgs(owner, args);
    // Posted by the application only (not by the third button of the owner)
    XtSetArg(args[n], XmNpopupEnabled, XmPOPUP_DISABLED); ++n;
    return XmCreatePopupMenu(owner, const_cast<char*>(name), args, n);
}

XtWidget XmWidgetSupport::createMenuItem(XtWidget menu,
                                         const java::String& text,
                                         XFontSet fontSet)
{
    XmString label = createString(text);
    XmRenderTable fonts = createRenderTable(menu, fontSet);
    Arg args[2]; Cardinal n = 0;
    XtSetArg(args[n], XmNlabelString, label); ++n;
    if ( fonts != nullptr ) {
        XtSetArg(args[n], XmNrenderTable, fonts); ++n;
    }
    XtWidget item = XtCreateManagedWidget("menuItem", xmPushButtonWidgetClass,
                                          menu, args, n);
    XmStringFree(label);
    if ( fonts != nullptr ) {
        XmRenderTableFree(fonts);
    }
    return item;
}
