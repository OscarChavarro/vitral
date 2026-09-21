package framework.gui;

import vsdk.toolkit.gui.viewport.Viewport;

/**
Receives the requests that `ViewportSetInteractionTechniques` derives from
user interaction, so the GUI technology in use can present them (i.e. a popup
menu).
*/
public interface ViewportSetInteractionListener
{
    /**
    The user clicked the title of the selected viewport: the menu to change its
    projection location should be presented.
    @param viewport the viewport whose title was clicked
    @param x horizontal position where the menu should appear, in pixels of
    the viewport set area (origin at its upper left corner)
    @param y vertical position where the menu should appear, in the same
    coordinates: just below the title
    */
    void projectionLocationMenuRequested(Viewport viewport, int x, int y);
}
