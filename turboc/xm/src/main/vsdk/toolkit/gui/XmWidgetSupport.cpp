// Before any other header that could include the Xt ones
#include "vsdk/toolkit/gui/XmIntrinsics.h"

#include <stdio.h>
#include <stdlib.h>

#include "vsdk/toolkit/gui/XmWidgetSupport.h"
#include "vsdk/toolkit/gui/XtTextEncoding.h"
#include "vsdk/toolkit/gui/XtWidgetSupport.h"
#include "java/util/ArrayList.txx"


/**
@return the number of arguments set: the visual, depth and colormap of the
shell of the widget, if it has them
*/
static Cardinal setShellVisualArgs(XtWidget widget, Arg* args)
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


void XmWidgetSupport::requireMotifShell(XtWidget shell)
{
    if ( !XmIsVendorShell(shell) ) {
        fprintf(stderr, "The shells do not have the Motif VendorShell: the "
                "program must be linked with libXm before libXt\n");
        exit(1);
    }
}

XmString XmWidgetSupport::createString(const String& text)
{
    const String localeText = XtTextEncoding::toLocale(text);
    return XmStringCreateLocalized(((char*)(localeText.c_str())));
}

String XmWidgetSupport::toText(XmString string)
{
    if ( string == NULL ) {
        return String();
    }
#if XmVersion >= 2000
    char* text = ((char*)(XmStringUnparse(
        string, NULL, XmCHARSET_TEXT, XmCHARSET_TEXT, NULL, 0,
        XmOUTPUT_ALL)));
#else
    // Motif 1.2: the text of its default charset segments
    char* text = NULL;
    if ( !XmStringGetLtoR(string, ((char*)(XmFONTLIST_DEFAULT_TAG)),
                          &text) ) {
        text = NULL;
    }
#endif
    String result = text != NULL ? XtTextEncoding::fromLocale(text) :
        String("");
    XtFree(text);
    return result;
}

XmRenderTable XmWidgetSupport::createRenderTable(XtWidget widget,
                                                 XFontSet fontSet)
{
    if ( fontSet == NULL ) {
        return NULL;
    }
#if XmVersion >= 2000
    Arg args[3]; Cardinal n = 0;
    XtSetArg(args[n], XmNfontType, XmFONT_IS_FONTSET); ++n;
    XtSetArg(args[n], XmNfont, fontSet); ++n;
    XtSetArg(args[n], XmNloadModel, XmLOAD_IMMEDIATE); ++n;
    XmRendition rendition = XmRenditionCreate(
        widget, ((char*)(XmFONTLIST_DEFAULT_TAG)), args, n);
    XmRenderTable table = XmRenderTableAddRenditions(NULL, &rendition, 1,
                                                     XmMERGE_REPLACE);
    XmRenditionFree(rendition);
    return table;
#else
    // Motif 1.2: a font list with the font set as its default entry,
    // shared by the widgets of the font set (see XmWidgetSupport.h)
    (void)widget;
    static ArrayList<XFontSet> fontSets;
    static ArrayList<XmFontList> fontLists;
    for ( long i = 0; i < fontSets.size(); i++ ) {
        if ( fontSets[i] == fontSet ) {
            return fontLists[i];
        }
    }
    XmFontListEntry entry = XmFontListEntryCreate(
        ((char*)(XmFONTLIST_DEFAULT_TAG)), XmFONT_IS_FONTSET,
        ((XtPointer)(fontSet)));
    XmFontList list = XmFontListAppendEntry(NULL, entry);
    fontSets.add(fontSet);
    fontLists.add(list);
    return list;
#endif
}

void XmWidgetSupport::setLabelString(XtWidget widget, const String& text)
{
    XmString string = createString(text);
    XtVaSetValues(widget, XmNlabelString, string, NULL);
    XmStringFree(string);
}

void XmWidgetSupport::setFontSet(XtWidget widget, XFontSet fontSet)
{
    XmRenderTable table = createRenderTable(widget, fontSet);
    if ( table == NULL ) {
        return;
    }
    XtVaSetValues(widget, XmNrenderTable, table, NULL);
    XmRenderTableFree(table);
}

XtWidget XmWidgetSupport::createPulldownMenu(XtWidget parent, const char* name)
{
    Arg args[3];
    Cardinal n = setShellVisualArgs(parent, args);
    return XmCreatePulldownMenu(parent, ((char*)(name)), args, n);
}

XtWidget XmWidgetSupport::createPopupMenu(XtWidget owner, const char* name)
{
    Arg args[4];
    Cardinal n = setShellVisualArgs(owner, args);
    // Posted by the application only (not by the third button of the owner)
    XtSetArg(args[n], XmNpopupEnabled, XmPOPUP_DISABLED); ++n;
    return XmCreatePopupMenu(owner, ((char*)(name)), args, n);
}

XtWidget XmWidgetSupport::createMenuItem(XtWidget menu,
                                         const String& text,
                                         XFontSet fontSet)
{
    XmString label = createString(text);
    XmRenderTable fonts = createRenderTable(menu, fontSet);
    Arg args[2]; Cardinal n = 0;
    XtSetArg(args[n], XmNlabelString, label); ++n;
    if ( fonts != NULL ) {
        XtSetArg(args[n], XmNrenderTable, fonts); ++n;
    }
    XtWidget item = XtCreateManagedWidget("menuItem", xmPushButtonWidgetClass,
                                          menu, args, n);
    XmStringFree(label);
    if ( fonts != NULL ) {
        XmRenderTableFree(fonts);
    }
    return item;
}
