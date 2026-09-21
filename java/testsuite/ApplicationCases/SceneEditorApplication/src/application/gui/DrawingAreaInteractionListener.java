package application.gui;

/**
Receives the requests that `DrawingAreaInteractionTechniques` derives from
user interaction, so the GUI technology in use can present them (pointer
shape, messages, dialogs...).
*/
public interface DrawingAreaInteractionListener
{
    /**
    @param cursor the pointer shape to present over the drawing area
    */
    void cursorRequested(PointerCursor cursor);

    /**
    The pointer must be placed at a position (infinite drag of a gizmo).
    @param surfaceX horizontal position, in pixels of the viewport set area
    @param surfaceY vertical position, in pixels of the viewport set area
    */
    void cursorWarpRequested(int surfaceX, int surfaceY);

    /**
    The drawing area content changed and must be drawn again.
    */
    void repaintRequested();

    /**
    @param message text for the status message of the application
    */
    void statusMessageRequested(String message);

    /**
    The selection of things changed: panels showing the selected target should
    be updated.
    */
    void selectionChanged();

    /**
    A raytraced image of the scene was requested.
    */
    void raytracingRequested();

    /**
    The object selector dialog was requested.
    */
    void selectorDialogRequested();

    /**
    The user requested to close the application.
    */
    void closeRequested();

    /**
    The user requested to toggle the full screen GUI mode.
    */
    void fullScreenGuiToggleRequested();
}
