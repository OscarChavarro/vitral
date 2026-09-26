#include <X11/StringDefs.h>
#include <X11/Shell.h>
#include <X11/Composite.h>
#include <X11/Xutil.h>

#include "vsdk/toolkit/render/xlib/XlibRGBImageUncompressedRenderer.h"
#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtImageControlWindow.h"
#include "vsdk/toolkit/gui/XtPanelWidgets.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"

namespace {
const int WINDOW_WIDTH = 640;
const int WINDOW_HEIGHT = 480;
const int STATUS_HEIGHT = 24;
const int IMAGE_MARGIN = 10;
}

XtImageControlWindow::XtImageControlWindow(XtApplicationHost* host,
                                           RGBImageUncompressed* image)
    : statusMessage(nullptr), host(host), controlledImage(image),
      windowWidget(nullptr), workArea(nullptr), ximage(nullptr),
      wmDeleteWindow(None)
{
    windowWidget = host->createDialogShell(
        "imageControlWindow", topLevelShellWidgetClass, "Image control tool");
    XtAddEventHandler(windowWidget, NoEventMask, True,
                      &XtImageControlWindow::shellEvent, this);

    Arg args[4]; Cardinal n = 0;
    XtSetArg(args[n], XtNwidth, WINDOW_WIDTH); ++n;
    XtSetArg(args[n], XtNheight, WINDOW_HEIGHT); ++n;
    Widget frame = XtCreateManagedWidget("imageControlFrame",
        compositeWidgetClass, windowWidget, args, n);

    Arg areaArgs[4]; Cardinal areaN = 0;
    XtSetArg(areaArgs[areaN], XtNx, 0); ++areaN;
    XtSetArg(areaArgs[areaN], XtNy, 0); ++areaN;
    XtSetArg(areaArgs[areaN], XtNwidth, WINDOW_WIDTH); ++areaN;
    XtSetArg(areaArgs[areaN], XtNheight, WINDOW_HEIGHT - STATUS_HEIGHT); ++areaN;
    workArea = XtCreateManagedWidget("imageArea", coreWidgetClass, frame,
                                     areaArgs, areaN);
    XtAddEventHandler(workArea, ExposureMask, False,
                      &XtImageControlWindow::workAreaEvent, this);

    statusMessage = XtPanelWidgets::createLabel(frame,
        "Image control window ready", host->getPanelFontSet(),
        XtPanelWidgets::LEFT, 3, WINDOW_HEIGHT - STATUS_HEIGHT + 1,
        WINDOW_WIDTH - 6, STATUS_HEIGHT - 2);

    XtPopup(windowWidget, XtGrabNone);
    wmDeleteWindow = XInternAtom(XtDisplay(windowWidget), "WM_DELETE_WINDOW",
                                 False);
    XSetWMProtocols(XtDisplay(windowWidget), XtWindow(windowWidget),
                    &wmDeleteWindow, 1);
}

XtImageControlWindow::~XtImageControlWindow()
{
    if ( ximage != nullptr ) {
        XDestroyImage(ximage);
    }
    if ( windowWidget != nullptr ) {
        XtDestroyWidget(windowWidget);
    }
}

void XtImageControlWindow::setImage(RGBImageUncompressed* image)
{
    controlledImage = image;
}

void XtImageControlWindow::updateXImage()
{
    if ( ximage != nullptr ) {
        XDestroyImage(ximage);
        ximage = nullptr;
    }
    if ( controlledImage == nullptr ) {
        return;
    }
    Visual* visual = nullptr;
    int depth = 0;
    XtVaGetValues(windowWidget, XtNvisual, &visual, XtNdepth, &depth, nullptr);
    if ( visual == nullptr ) {
        visual = DefaultVisualOfScreen(XtScreen(windowWidget));
    }
    ximage = XlibRGBImageUncompressedRenderer::exportToXImage(XtDisplay(windowWidget), visual,
                                              depth, *controlledImage);
}

void XtImageControlWindow::redrawImage()
{
    // The image is copied: the model may change it later
    updateXImage();
    XtPopup(windowWidget, XtGrabNone);
    XRaiseWindow(XtDisplay(windowWidget), XtWindow(windowWidget));
    paint();
}

void XtImageControlWindow::paint()
{
    if ( !XtIsRealized(workArea) ) {
        return;
    }
    Display* display = XtDisplay(workArea);
    Window window = XtWindow(workArea);
    XClearWindow(display, window);
    if ( ximage == nullptr ) {
        return;
    }
    GC gc = XCreateGC(display, window, 0, nullptr);
    XPutImage(display, window, gc, ximage, 0, 0, IMAGE_MARGIN, IMAGE_MARGIN,
              ximage->width, ximage->height);
    XFreeGC(display, gc);
}

void XtImageControlWindow::workAreaEvent(Widget, XtPointer clientData,
                                         XEvent* event, Boolean*)
{
    if ( event->type == Expose && event->xexpose.count == 0 ) {
        static_cast<XtImageControlWindow*>(clientData)->paint();
    }
}

void XtImageControlWindow::shellEvent(Widget, XtPointer clientData,
                                      XEvent* event, Boolean*)
{
    XtImageControlWindow* self = static_cast<XtImageControlWindow*>(clientData);
    if ( event->type == ClientMessage &&
         static_cast<Atom>(event->xclient.data.l[0]) == self->wmDeleteWindow ) {
        XtPopdown(self->windowWidget);
    }
}
