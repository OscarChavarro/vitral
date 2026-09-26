// Before any other header that could include the Xt ones
#include "vsdk/toolkit/gui/XtIntrinsics.h"

#include <cstring>
#include <vector>

#include "vsdk/toolkit/gui/CommandListener.h"
#include "vsdk/toolkit/gui/XtWidgetSupport.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/render/xlib/XlibRGBAImageUncompressedRenderer.h"

namespace {

/**
A command of a menu item or a button, executed by its listener. Freed with
its widget.
*/
struct CommandBinding {
    CommandListener* executor;
    java::String command;
};

void executeCommand(XtWidget, XtPointer clientData, XtPointer)
{
    CommandBinding* binding = static_cast<CommandBinding*>(clientData);
    if ( binding != nullptr && binding->executor != nullptr ) {
        binding->executor->executeCommand(binding->command);
    }
}

void freeCommandBinding(XtWidget, XtPointer clientData, XtPointer)
{
    delete static_cast<CommandBinding*>(clientData);
}

struct ProcBinding {
    XtWidgetSupport::WidgetProc proc;
    void* clientData;
};

void callProc(XtWidget widget, XtPointer clientData, XtPointer)
{
    ProcBinding* binding = static_cast<ProcBinding*>(clientData);
    if ( binding != nullptr && binding->proc != nullptr ) {
        binding->proc(widget, binding->clientData);
    }
}

void freeProcBinding(XtWidget, XtPointer clientData, XtPointer)
{
    delete static_cast<ProcBinding*>(clientData);
}

void freePixmap(XtWidget widget, XtPointer clientData, XtPointer)
{
    XFreePixmap(XtDisplay(widget), reinterpret_cast<Pixmap>(clientData));
}

}

XtWidget XtWidgetSupport::shellOf(XtWidget widget)
{
    XtWidget shell = widget;
    while ( shell != nullptr && !XtIsShell(shell) ) {
        shell = XtParent(shell);
    }
    return shell;
}

XtWidgetSupport::ShellVisual XtWidgetSupport::shellVisualOf(XtWidget widget)
{
    XtWidget shell = shellOf(widget);
    ShellVisual result;
    result.visual = nullptr;
    result.depth = 0;
    result.colormap = 0;
    if ( shell != nullptr ) {
        XtVaGetValues(shell, XtNvisual, &result.visual, XtNdepth,
                      &result.depth, XtNcolormap, &result.colormap, nullptr);
    }
    if ( result.visual == nullptr ) {
        result.visual = DefaultVisualOfScreen(XtScreen(widget));
    }
    return result;
}

void XtWidgetSupport::bindCommand(XtWidget widget, const char* callbackName,
                                  CommandListener* executor,
                                  const java::String& command)
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
                  XtNcolormap, &colormap, XtNdepth, &depth, nullptr);
    XColor background;
    background.pixel = backgroundPixel;
    XQueryColor(XtDisplay(button), colormap, &background);
    Pixmap pixmap = XlibRGBAImageUncompressedRenderer::exportToPixmap(
        XtDisplay(button), RootWindowOfScreen(XtScreen(button)),
        shellVisualOf(button).visual, depth, icon, background);
    if ( pixmap != None ) {
        XtAddCallback(button, XtNdestroyCallback, &freePixmap,
                      reinterpret_cast<XtPointer>(pixmap));
    }
    return pixmap;
}

void XtWidgetSupport::removeAll(XtWidget container)
{
    WidgetList children = nullptr;
    Cardinal count = 0;
    XtVaGetValues(container, XtNchildren, &children,
                  XtNnumChildren, &count, nullptr);
    // Destroying changes the list of children: work over a copy
    std::vector<XtWidget> copy(children, children + count);
    for ( size_t i = 0; i < copy.size(); i++ ) {
        XtDestroyWidget(copy[i]);
    }
}

void XtWidgetSupport::setColor(XtWidget widget, const char* resource,
                               const char* colorName)
{
    XtVaSetValues(widget, XtVaTypedArg, resource, XtRString, colorName,
                  static_cast<int>(strlen(colorName) + 1), nullptr);
}

std::string XtWidgetSupport::uniqueName(const char* prefix)
{
    static unsigned int sequence = 0;
    return prefix + std::to_string(sequence++);
}
