#ifndef __DRAWING_AREA_HOST__
#define __DRAWING_AREA_HOST__

#include "java/lang/String.h"

class BodyEditFeedbackProvider;
class RGBImageUncompressed;

/**
Services that the drawing area renderers need from the GUI technology
hosting the drawing surface, so rendering classes do not depend on it.
*/
class DrawingAreaHost {
public:
    virtual ~DrawingAreaHost() {}

    /**
    Called at the beginning of each frame, so the host can update what
    depends on the component presenting the surface (size, screen
    resolution).
    */
    virtual void beforeFrame() = 0;

    /**
    @return true if the GUI is in full screen mode
    */
    virtual bool isFullScreenGuiMode() = 0;

    /**
    @return the editor of the selected body (which presents feedback over
    it), or null if there is none
    */
    virtual BodyEditFeedbackProvider* getBodyEditFeedbackProvider() = 0;

    /**
    Computes the raytraced image of the scene for a viewport in CPU render
    mode, leaving it in the application model.
    */
    virtual void raytraceImage() = 0;

    /**
    @param image an image obtained from the renderer, to be presented to the
    user (owned by the caller)
    */
    virtual void showImage(RGBImageUncompressed* image) = 0;

    /**
    @param message text for the status message of the application
    */
    virtual void showStatusMessage(const java::String& message) = 0;
};

#endif
