package render;

import vsdk.toolkit.media.RGBImageUncompressed;

/**
Services that the drawing area renderers need from the GUI technology
hosting the drawing surface, so rendering classes do not depend on it.
*/
public interface DrawingAreaHost
{
    /**
    Called at the beginning of each frame, so the host can update what depends
    on the component presenting the surface (size, screen resolution).
    */
    void beforeFrame();

    /**
    @return true if the GUI is in full screen mode
    */
    boolean isFullScreenGuiMode();

    /**
    @return the editor of the selected body (which presents feedback over
    it), or null if there is none
    */
    BodyEditFeedbackProvider getBodyEditFeedbackProvider();

    /**
    Computes the raytraced image of the scene for a viewport in CPU render
    mode, leaving it in the application model.
    */
    void raytraceImage();

    /**
    @param image an image obtained from the renderer, to be presented to the
    user
    */
    void showImage(RGBImageUncompressed image);

    /**
    @param message text for the status message of the application
    */
    void showStatusMessage(String message);
}
