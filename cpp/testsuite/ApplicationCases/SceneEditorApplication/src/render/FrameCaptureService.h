#ifndef __FRAME_CAPTURE_SERVICE__
#define __FRAME_CAPTURE_SERVICE__

class ApplicationModel;
class DrawingArea;
class DrawingAreaHost;
class FrameBufferSource;
class RGBImageUncompressed;
class ViewportSet;

/**
Captures the content of the frame buffers when the user requested it
through the `DrawingArea`: the color buffer, the depth buffer, and the
export of the frame (or of the selected viewport) to image files. Captured
images are left in the application model and presented through the
`DrawingAreaHost`. The buffers are read through a `FrameBufferSource`, so
this class does not depend on the rendering technology.
*/
class FrameCaptureService {
private:
    ApplicationModel* model;
    DrawingArea* drawingArea;
    ViewportSet* viewportSet;
    DrawingAreaHost* host;

    static RGBImageUncompressed* cropImage(const RGBImageUncompressed* source,
                                           int startX, int startY,
                                           int width, int height);

public:
    FrameCaptureService(ApplicationModel* model, DrawingAreaHost* host);

    /**
    Captures the color buffer, if it was requested and the view being drawn
    is the selected one.
    @param source frame buffer of the view being drawn
    @param selectedView true if the view being drawn is the selected one
    */
    void copyColorBufferIfNeeded(FrameBufferSource* source,
                                 bool selectedView);

    /**
    Captures the depth buffer, if it was requested.
    @param source frame buffer of the view being drawn
    */
    void copyZBufferIfNeeded(FrameBufferSource* source);

    /**
    @return true if the user requested to export the next frame to files
    */
    bool isFrameExportPending() const;

    /**
    Exports to files the frame just drawn, if it was requested.
    @param source frame buffer, set to the whole viewport set area
    */
    void exportPendingFrame(FrameBufferSource* source);
};

#endif
