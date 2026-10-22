// Before any other header that could include the Xt ones
#include "vsdk/toolkit/gui/XtIntrinsics.h"

#include <string.h>
#include "java/util/ArrayList.txx"

#include "vsdk/toolkit/common/Cpp98Support.h"
#include "vsdk/toolkit/gui/CommandListener.h"
#include "vsdk/toolkit/gui/XtWidgetSupport.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/render/xlib/XlibRGBAImageUncompressedRenderer.h"


/**
A command of a menu item or a button, executed by its listener. Freed with
its widget.
*/
struct CommandBinding {
    CommandListener* executor;
    String command;
};

static void executeCommand(XtWidget, XtPointer clientData, XtPointer)
{
    CommandBinding* binding = ((CommandBinding*)(clientData));
    if ( binding != NULL && binding->executor != NULL ) {
        binding->executor->executeCommand(binding->command);
    }
}

static void freeCommandBinding(XtWidget, XtPointer clientData, XtPointer)
{
    delete ((CommandBinding*)(clientData));
}

struct ProcBinding {
    XtWidgetSupport::WidgetProc proc;
    void* clientData;
};

static void callProc(XtWidget widget, XtPointer clientData, XtPointer)
{
    ProcBinding* binding = ((ProcBinding*)(clientData));
    if ( binding != NULL && binding->proc != NULL ) {
        binding->proc(widget, binding->clientData);
    }
}

static void freeProcBinding(XtWidget, XtPointer clientData, XtPointer)
{
    delete ((ProcBinding*)(clientData));
}

static void freePixmap(XtWidget widget, XtPointer clientData, XtPointer)
{
    XFreePixmap(XtDisplay(widget), ((Pixmap)(clientData)));
}


XtWidget XtWidgetSupport::shellOf(XtWidget widget)
{
    XtWidget shell = widget;
    while ( shell != NULL && !XtIsShell(shell) ) {
        shell = XtParent(shell);
    }
    return shell;
}

XtWidgetSupport::ShellVisual XtWidgetSupport::shellVisualOf(XtWidget widget)
{
    XtWidget shell = shellOf(widget);
    ShellVisual result;
    result.visual = NULL;
    result.depth = 0;
    result.colormap = 0;
    if ( shell != NULL ) {
        XtVaGetValues(shell, XtNvisual, &result.visual, XtNdepth,
                      &result.depth, XtNcolormap, &result.colormap, NULL);
    }
    if ( result.visual == NULL ) {
        result.visual = DefaultVisualOfScreen(XtScreen(widget));
    }
    return result;
}

void XtWidgetSupport::bindCommand(XtWidget widget, const char* callbackName,
                                  CommandListener* executor,
                                  const String& command)
{
    CommandBinding* binding = new CommandBinding;
    binding->executor = executor;
    binding->command = command;
    XtAddCallback(widget, callbackName, &executeCommand, binding);
    XtAddCallback(widget, XtNdestroyCallback, &freeCommandBinding, binding);
}

void XtWidgetSupport::bindProc(XtWidget widget, const char* callbackName,
                               WidgetProc proc, void* clientData)
{
    ProcBinding* binding = new ProcBinding;
    binding->proc = proc;
    binding->clientData = clientData;
    XtAddCallback(widget, callbackName, &callProc, binding);
    XtAddCallback(widget, XtNdestroyCallback, &freeProcBinding, binding);
}

Pixmap XtWidgetSupport::createIconPixmap(XtWidget button,
                                         const RGBAImageUncompressed& icon)
{
    Pixel backgroundPixel = 0;
    Colormap colormap = 0;
    int depth = 0;
    XtVaGetValues(button, XtNbackground, &backgroundPixel,
                  XtNcolormap, &colormap, XtNdepth, &depth, NULL);
    XColor background;
    background.pixel = backgroundPixel;
    XQueryColor(XtDisplay(button), colormap, &background);
    Pixmap pixmap = XlibRGBAImageUncompressedRenderer::exportToPixmap(
        XtDisplay(button), RootWindowOfScreen(XtScreen(button)),
        shellVisualOf(button).visual, depth, icon, background);
    if ( pixmap != None ) {
        XtAddCallback(button, XtNdestroyCallback, &freePixmap,
                      ((XtPointer)(pixmap)));
    }
    return pixmap;
}

void XtWidgetSupport::removeAll(XtWidget container)
{
    WidgetList children = NULL;
    Cardinal count = 0;
    XtVaGetValues(container, XtNchildren, &children,
                  XtNnumChildren, &count, NULL);
    // Destroying changes the list of children: work over a copy
    ArrayList<XtWidget> copy;
    for ( Cardinal c = 0; c < count; c++ ) {
        copy.add(children[c]);
    }
    for ( long i = 0; i < copy.size(); i++ ) {
        XtDestroyWidget(copy[i]);
    }
}

void XtWidgetSupport::setColor(XtWidget widget, const char* resource,
                               const char* colorName)
{
    XtVaSetValues(widget, XtVaTypedArg, resource, XtRString, colorName,
                  ((int)(strlen(colorName) + 1)), NULL);
}

String XtWidgetSupport::uniqueName(const char* prefix)
{
    static unsigned int sequence = 0;
    return prefix + Cpp98Support::toString(sequence++);
}
