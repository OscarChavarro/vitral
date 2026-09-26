#include <X11/StringDefs.h>
#include <X11/Shell.h>
#include <X11/Xutil.h>

#include "vsdk/toolkit/render/xlib/XlibRGBImageUncompressedRenderer.h"
#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtImageControlWindow.h"
#include "gui/xt/XtUiFactory.h"
#include "vsdk/toolkit/gui/XtPanelWidgets.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"
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
      windowWidget(nullptr), workArea(nullptr), ximage(nullptr)
{
    windowWidget = host->createDialogShell(
        "imageControlWindow", topLevelShellWidgetClass, "Image control tool");
    host->getUiFactory()->getWidgetSet()->setWindowCloseHandler(
        windowWidget, &XtImageControlWindow::closeRequested, this);

    XtPanelWidgets* widgets = host->getPanelWidgets();
    Widget frame = widgets->createPanel(windowWidget, "imageControlFrame",
                                        0, 0, WINDOW_WIDTH, WINDOW_HEIGHT,
                                        true);

    Arg areaArgs[4]; Cardinal areaN = 0;
    XtSetArg(areaArgs[areaN], XtNx, 0); ++areaN;
    XtSetArg(areaArgs[areaN], XtNy, 0); ++areaN;
    XtSetArg(areaArgs[areaN], XtNwidth, WINDOW_WIDTH); ++areaN;
    XtSetArg(areaArgs[areaN], XtNheight, WINDOW_HEIGHT - STATUS_HEIGHT); ++areaN;
    workArea = XtCreateManagedWidget("imageArea", coreWidgetClass, frame,
                                     areaArgs, areaN);
    XtAddEventHandler(workArea, ExposureMask, False,
                      &XtImageControlWindow::workAreaEvent, this);

    statusMessage = widgets->createLabel(frame,
        "Image control window ready", host->getPanelFontSet(),
        XtPanelWidgets::LEFT, 3, WINDOW_HEIGHT - STATUS_HEIGHT + 1,
        WINDOW_WIDTH - 6, STATUS_HEIGHT - 2);

    XtPopup(windowWidget, XtGrabNone);
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

void XtImageControlWindow::closeRequested(void* clientData)
{
    XtPopdown(static_cast<XtImageControlWindow*>(clientData)->windowWidget);
}
