package application.render.jogl;

import vsdk.toolkit.media.RGBImageUncompressed;

import application.gui.ModifyPanel;

/**
Services that `Jogl4DrawingAreaRenderer` needs from the GUI technology
hosting the drawing surface, so rendering classes do not depend on it.
*/
public interface Jogl4DrawingAreaHost
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
    @return the panel modifying the selected body (which draws feedback over
    it), or null if there is none
    */
    ModifyPanel getModifyPanel();

    /**
    Computes the raytraced image of the scene, leaving it in the application
    model.
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
