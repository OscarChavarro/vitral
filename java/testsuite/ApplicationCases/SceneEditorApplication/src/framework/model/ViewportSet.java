package framework.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
A `ViewportSet` models one rectangular drawing area of the application (for
example one canvas / one display / one screen) that is partitioned in one or
more `Viewport`s. The set knows which viewport is selected (the one receiving
interaction), how the viewports are arranged (layout style, or one viewport
maximized) and the size in pixels of the whole area.

An application can have several `ViewportSet`s, conceptually one for each
display available to the user (see `ApplicationModel`).

This class is a plain model object: it depends only on JDK and vitral-base
classes, and knows nothing about the GUI or rendering technology used to
present it.

The layout arrangements are the ones defined for 2, 3 and 4 viewports; the
percent-based area of each viewport has its origin at the lower left corner of
the set area. If the set has more viewports than the supported layouts, only
the first one is shown, maximized.
*/
public class ViewportSet
{
    private static final double T1 = 1.0 / 3.0;
    private static final double T2 = 2.0 / 3.0;
    private static final double H = 0.5;

    // Each layout has one {startX, startY, sizeX, sizeY} row per viewport
    private static final double[][][] LAYOUTS_2 = {
        {{0, 0, H, 1}, {H, 0, H, 1}},
        {{0, H, 1, H}, {0, 0, 1, H}}
    };

    private static final double[][][] LAYOUTS_3 = {
        {{0, 0, H, H}, {0, H, H, H}, {H, 0, H, 1}},
        {{0, 0, H, 1}, {H, 0, H, H}, {H, H, H, H}},
        {{0, 0, H, H}, {H, 0, H, H}, {0, H, 1, H}},
        {{0, 0, 1, H}, {0, H, H, H}, {H, H, H, H}},
        {{0, 0, T1, 1}, {T1, 0, T1, 1}, {T2, 0, T1, 1}},
        {{0, 0, 1, T1}, {0, T1, 1, T1}, {0, T2, 1, T1}}
    };

    private static final double[][][] LAYOUTS_4 = {
        {{0, 0, T1, T1}, {T1, 0, T2, 1}, {0, T2, T1, T1}, {0, T1, T1, T1}},
        {{0, 0, H, H}, {H, 0, H, H}, {0, H, H, H}, {H, H, H, H}},
        {{0, 0, H, 1}, {H, 0, H, T1}, {H, T1, H, T1}, {H, T2, H, T1}},
        {{0, 0, T1, H}, {T1, 0, T1, H}, {T2, 0, T1, H}, {0, H, 1, H}},
        {{0, 0, 1, H}, {0, H, T1, H}, {T1, H, T1, H}, {T2, H, T1, H}}
    };

    private String name;
    private final List<Viewport> viewports;
    private int selectedViewportIndex;
    private int layoutStyle;
    private boolean fullViewport;
    private int sizeXInPixels;
    private int sizeYInPixels;

    public ViewportSet()
    {
        name = "Viewport set";
        viewports = new ArrayList<Viewport>();
        selectedViewportIndex = 0;
        layoutStyle = 0;
        fullViewport = false;
        sizeXInPixels = 0;
        sizeYInPixels = 0;
    }

    /**
    Creates a set with the standard four viewports arrangement: Left,
    Perspective, Top and Front, with the perspective one selected.
    @param name
    @return a new set, with its layout already updated
    */
    public static ViewportSet createStandardSet(String name)
    {
        ViewportSet set = new ViewportSet();
        int numViews = 4;
        int i;

        set.setName(name);
        for ( i = 0; i < numViews; i++ ) {
            Viewport viewport = new Viewport();
            viewport.applyDefaultConfiguration(numViews, i);
            set.addViewport(viewport);
        }
        set.setSelectedViewportIndex(1);
        set.updateLayout();
        return set;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
    @return a read-only view of the viewports; use `addViewport` and
    `removeViewport` to change the list
    */
    public List<Viewport> getViewports()
    {
        return Collections.unmodifiableList(viewports);
    }

    public int getViewportCount()
    {
        return viewports.size();
    }

    public Viewport getViewport(int index)
    {
        return viewports.get(index);
    }

    /**
    Adds a viewport at the end of the set and updates the layout.
    @param viewport
    */
    public void addViewport(Viewport viewport)
    {
        if ( viewport == null ) {
            return;
        }
        viewports.add(viewport);
        updateLayout();
    }

    /**
    Removes the viewport at the given index (if any) and updates the layout.
    @param index
    @return the removed viewport, or null if the index is not valid
    */
    public Viewport removeViewport(int index)
    {
        if ( index < 0 || index >= viewports.size() ) {
            return null;
        }
        Viewport removed = viewports.remove(index);
        updateLayout();
        return removed;
    }

    public int getSelectedViewportIndex()
    {
        return selectedViewportIndex;
    }

    public void setSelectedViewportIndex(int selectedViewportIndex)
    {
        this.selectedViewportIndex = selectedViewportIndex;
    }

    /**
    @return the selected viewport, or null if the set is empty
    */
    public Viewport getSelectedViewport()
    {
        if ( viewports.isEmpty() ) {
            return null;
        }
        clampSelection();
        return viewports.get(selectedViewportIndex);
    }

    public boolean isSelected(Viewport viewport)
    {
        return viewport != null && viewport == getSelectedViewport();
    }

    /**
    Makes the given viewport the selected one, if it belongs to this set.
    @param viewport
    @return true if the selection was applied
    */
    public boolean selectViewport(Viewport viewport)
    {
        int index = viewports.indexOf(viewport);
        if ( index < 0 ) {
            return false;
        }
        selectedViewportIndex = index;
        return true;
    }

    /**
    Selects the next viewport, cyclically, and updates the layout (which, if
    a viewport is maximized, shows the new selected one).
    */
    public void selectNextViewport()
    {
        if ( viewports.isEmpty() ) {
            return;
        }
        selectedViewportIndex = (selectedViewportIndex + 1) % viewports.size();
        updateLayout();
    }

    public int getLayoutStyle()
    {
        return layoutStyle;
    }

    public void setLayoutStyle(int layoutStyle)
    {
        this.layoutStyle = Math.max(0, layoutStyle);
    }

    /**
    Selects the next arrangement of the viewports and updates the layout.
    */
    public void selectNextLayoutStyle()
    {
        layoutStyle++;
        if ( layoutStyle < 0 ) {
            layoutStyle = 0;
        }
        updateLayout();
    }

    public boolean isFullViewport()
    {
        return fullViewport;
    }

    public void setFullViewport(boolean fullViewport)
    {
        this.fullViewport = fullViewport;
    }

    /**
    Maximizes the selected viewport, or restores the layout if it is already
    maximized, and updates the layout.
    */
    public void toggleFullViewport()
    {
        fullViewport = !fullViewport;
        updateLayout();
    }

    public int getSizeXInPixels()
    {
        return sizeXInPixels;
    }

    public void setSizeXInPixels(int sizeXInPixels)
    {
        this.sizeXInPixels = sizeXInPixels;
    }

    public int getSizeYInPixels()
    {
        return sizeYInPixels;
    }

    public void setSizeYInPixels(int sizeYInPixels)
    {
        this.sizeYInPixels = sizeYInPixels;
    }

    /**
    Sets the size in pixels of the area containing the viewports and updates
    the pixel area of each one.
    @param sizeXInPixels
    @param sizeYInPixels
    */
    public void resize(int sizeXInPixels, int sizeYInPixels)
    {
        this.sizeXInPixels = sizeXInPixels;
        this.sizeYInPixels = sizeYInPixels;
        updatePixelAreas();
    }

    /**
    Recalculates the pixel area of every viewport from the current size in
    pixels of the set.
    */
    public void updatePixelAreas()
    {
        for ( Viewport viewport : viewports ) {
            viewport.updatePixelArea(sizeXInPixels, sizeYInPixels);
        }
    }

    public int countActiveViewports()
    {
        int n = 0;

        for ( Viewport viewport : viewports ) {
            if ( viewport.isActive() ) {
                n++;
            }
        }
        return n;
    }

    /**
    Finds the active viewport under a point given in pixels of the set area,
    with origin at the upper left corner. If several viewports contain the
    point (points over shared borders), the last one in the list is returned.
    @param x
    @param y
    @return the viewport under the point, or null if none
    */
    public Viewport findViewportAt(int x, int y)
    {
        if ( sizeXInPixels <= 0 || sizeYInPixels <= 0 ) {
            return null;
        }

        double xPercent = ((double)x) / ((double)sizeXInPixels);
        double yPercent = 1 - ((double)y) / ((double)sizeYInPixels);
        Viewport found = null;

        for ( Viewport viewport : viewports ) {
            if ( viewport.isActive() && viewport.contains(xPercent, yPercent) ) {
                found = viewport;
            }
        }
        return found;
    }

    /**
    Translates a x coordinate given in pixels of the set area to pixels of the
    given viewport.
    @param viewport
    @param x
    @return x relative to the left of the viewport
    */
    public int toViewportX(Viewport viewport, int x)
    {
        return x - viewport.getPixelStartX();
    }

    /**
    Translates a y coordinate given in pixels of the set area (origin at the
    upper left corner) to pixels of the given viewport (same convention).
    @param viewport
    @param y
    @return y relative to the top of the viewport
    */
    public int toViewportY(Viewport viewport, int y)
    {
        return y + viewport.getPixelSizeY() -
            (sizeYInPixels - viewport.getPixelStartY());
    }

    /**
    Updates the percent-based area and the active status of the viewports
    according to the selection, the maximized status and the layout style.
    */
    public void updateLayout()
    {
        int n = viewports.size();
        int i;

        clampSelection();
        if ( n == 0 ) {
            return;
        }

        if ( fullViewport ) {
            for ( i = 0; i < n; i++ ) {
                Viewport viewport = viewports.get(i);
                viewport.setActive(i == selectedViewportIndex);
                if ( i == selectedViewportIndex ) {
                    viewport.setPercentArea(0, 0, 1, 1);
                }
            }
            return;
        }

        switch ( n ) {
          case 1:
            viewports.get(0).setActive(true);
            viewports.get(0).setPercentArea(0, 0, 1, 1);
            break;
          case 2:
            applyLayout(LAYOUTS_2[layoutStyle % LAYOUTS_2.length]);
            break;
          case 3:
            applyLayout(LAYOUTS_3[layoutStyle % LAYOUTS_3.length]);
            break;
          case 4:
            applyLayout(LAYOUTS_4[layoutStyle % LAYOUTS_4.length]);
            break;
          default:
            // Not supported layout: first viewport is shown maximized
            selectedViewportIndex = 0;
            for ( i = 0; i < n; i++ ) {
                viewports.get(i).setActive(i == 0);
            }
            viewports.get(0).setPercentArea(0, 0, 1, 1);
            break;
        }
    }

    private void applyLayout(double[][] areas)
    {
        int i;

        for ( i = 0; i < areas.length; i++ ) {
            Viewport viewport = viewports.get(i);
            viewport.setActive(true);
            viewport.setPercentArea(areas[i][0], areas[i][1], areas[i][2], areas[i][3]);
        }
    }

    private void clampSelection()
    {
        if ( selectedViewportIndex < 0 || selectedViewportIndex >= viewports.size() ) {
            selectedViewportIndex = 0;
        }
    }
}
