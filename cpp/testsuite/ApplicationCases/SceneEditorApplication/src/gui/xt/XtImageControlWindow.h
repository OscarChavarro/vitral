#ifndef __XT_IMAGE_CONTROL_WINDOW__
#define __XT_IMAGE_CONTROL_WINDOW__

#include <X11/Intrinsic.h>

class RGBImageUncompressed;
class XtApplicationHost;

/**
Window presenting an image obtained by the editor (the raytraced image, the
depth maps of the rasterizer), as `AwtImageControlWindow` does for Swing:
the image at the upper left corner and a status bar. Closing it only hides
it: it is shown again with the next image.

C++ port note: the Swing window repeats the menubar of the application; the
Xt one leaves it in the main window.
*/
class XtImageControlWindow {
public:
    /**
    @param host application showing the window
    @param image image to present (referenced: it stays in the model)
    */
    XtImageControlWindow(XtApplicationHost* host, RGBImageUncompressed* image);
    ~XtImageControlWindow();

    void setImage(RGBImageUncompressed* image);

    /**
    Shows the window, presenting the current contents of the image.
    */
    void redrawImage();

    Widget statusMessage;

private:
    XtApplicationHost* host;
    RGBImageUncompressed* controlledImage;
    Widget windowWidget;
    Widget workArea;
    XImage* ximage;
    Atom wmDeleteWindow;

    void updateXImage();
    void paint();

    static void workAreaEvent(Widget, XtPointer clientData, XEvent* event,
                              Boolean*);
    static void shellEvent(Widget, XtPointer clientData, XEvent* event,
                           Boolean*);

    XtImageControlWindow(const XtImageControlWindow& other);
    XtImageControlWindow& operator=(const XtImageControlWindow& other);
};

#endif
