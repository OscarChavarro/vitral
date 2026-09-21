package vsdk.toolkit.gui.viewport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;
import vsdk.toolkit.gui.widget.Widget;
import vsdk.toolkit.gui.widget.WidgetCommand;
import vsdk.toolkit.gui.widget.WidgetMenu;
import vsdk.toolkit.gui.widget.WidgetMenuElement;
import vsdk.toolkit.gui.widget.WidgetMenuItem;

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

The layout arrangements are the ones defined for 2, 3 and 4 viewports, plus a
last one for each of them where only the selected viewport is visible (see
`getLayoutStyleCount`); the percent-based area of each viewport has its origin
at the lower left corner of the set area. If the set has more viewports than the supported layouts, only
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
    private ColorRgb titleColor;
    private ColorRgb selectedTitleColor;
    private ViewportElementScaler elementScaler;
    private Widget i18nContext;

    public ViewportSet()
    {
        name = "Viewport set";
        viewports = new ArrayList<Viewport>();
        selectedViewportIndex = 0;
        layoutStyle = 0;
        fullViewport = false;
        sizeXInPixels = 0;
        sizeYInPixels = 0;
        titleColor = new ColorRgb(1, 1, 1);
        selectedTitleColor = new ColorRgb(1, 1, 0);
        elementScaler = new ViewportElementScaler();
        i18nContext = null;
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
    @return the number of layout styles available for the current number of
    viewports: the defined arrangements plus the one showing only the selected
    viewport (a set with a single viewport has just one)
    */
    public int getLayoutStyleCount()
    {
        switch ( viewports.size() ) {
          case 2:
            return LAYOUTS_2.length + 1;
          case 3:
            return LAYOUTS_3.length + 1;
          case 4:
            return LAYOUTS_4.length + 1;
          default:
            return 1;
        }
    }

    /**
    Selects the next arrangement of the viewports and updates the layout.
    The last arrangement shows only the selected viewport.
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

    /**
    @return the color used to draw the name of the viewports that are not
    selected (white by default)
    */
    public ColorRgb getTitleColor()
    {
        return titleColor;
    }

    /**
    @param titleColor the color for the names of non selected viewports; a
    null value is ignored
    */
    public void setTitleColor(ColorRgb titleColor)
    {
        if ( titleColor != null ) {
            this.titleColor = titleColor;
        }
    }

    /**
    @return the color used to draw the name of the selected viewport (yellow
    by default)
    */
    public ColorRgb getSelectedTitleColor()
    {
        return selectedTitleColor;
    }

    /**
    @param selectedTitleColor the color for the name of the selected viewport;
    a null value is ignored
    */
    public void setSelectedTitleColor(ColorRgb selectedTitleColor)
    {
        if ( selectedTitleColor != null ) {
            this.selectedTitleColor = selectedTitleColor;
        }
    }

    /**
    @return the I18N context (the GUI definition, in the language currently
    selected by the user) used to present the texts of this set, or null if
    there is none
    */
    public Widget getI18nContext()
    {
        return i18nContext;
    }

    /**
    Sets the I18N context used to present the texts of this set. It must be
    updated whenever the user changes the language, so the set is presented
    with the messages of the new one. With a null context the default texts
    are used.
    @param i18nContext
    */
    public void setI18nContext(Widget i18nContext)
    {
        this.i18nContext = i18nContext;
    }

    /**
    @param viewport a viewport of this set
    @return the name to present for the viewport: the text of its projection
    location in the `VIEWPORT_SET_PROJECTION_LOCATION` popup of the I18N
    context (falling back to the text of the command, and to the default name
    of the camera if the context does not define them)
    */
    public String getTitleFor(Viewport viewport)
    {
        String command = viewport.getProjectionLocationCommand();
        String name = findMenuItemName(ViewportSetCommands.POPUP_PROJECTION_LOCATION, command);

        if ( name == null && i18nContext != null ) {
            WidgetCommand widgetCommand = i18nContext.getCommandByName(command);
            if ( widgetCommand != null ) {
                name = widgetCommand.getName();
            }
        }
        if ( name == null || name.length() == 0 ) {
            return viewport.getTitle();
        }
        return name;
    }

    private String findMenuItemName(String popupName, String command)
    {
        if ( i18nContext == null ) {
            return null;
        }

        WidgetMenu popup = i18nContext.getPopup(popupName);
        if ( popup == null ) {
            return null;
        }

        for ( WidgetMenuElement element : popup.getChildren() ) {
            if ( element instanceof WidgetMenuItem ) {
                WidgetMenuItem item = (WidgetMenuItem)element;
                if ( !item.isSeparator() && command.equals(item.getCommandName()) ) {
                    return item.getName();
                }
            }
        }
        return null;
    }

    /**
    @return the scaler that gives the size of the texts of this set for the
    resolution of the screen where the set is presented
    */
    public ViewportElementScaler getElementScaler()
    {
        return elementScaler;
    }

    /**
    @param elementScaler the scaler for the elements of this set; a null value is
    ignored
    */
    public void setElementScaler(ViewportElementScaler elementScaler)
    {
        if ( elementScaler != null ) {
            this.elementScaler = elementScaler;
        }
    }

    /**
    @param viewport a viewport of this set
    @return the color to draw the name of the given viewport, depending on
    whether it is the selected one
    */
    public ColorRgb getTitleColorFor(Viewport viewport)
    {
        if ( isSelected(viewport) ) {
            return selectedTitleColor;
        }
        return titleColor;
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
    Translates a x coordinate given in pixels of the given viewport to pixels
    of the set area. It is the inverse of `toViewportX`.
    @param viewport
    @param x x relative to the left of the viewport
    @return x in the set area
    */
    public int toSetX(Viewport viewport, int x)
    {
        return x + viewport.getPixelStartX();
    }

    /**
    Translates a y coordinate given in pixels of the given viewport (origin
    at its upper left corner) to pixels of the set area (same convention). It
    is the inverse of `toViewportY`.
    @param viewport
    @param y y relative to the top of the viewport
    @return y in the set area
    */
    public int toSetY(Viewport viewport, int y)
    {
        return y - viewport.getPixelSizeY() +
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
            showOnlySelectedViewport();
            return;
        }

        switch ( n ) {
          case 1:
            viewports.get(0).setActive(true);
            viewports.get(0).setPercentArea(0, 0, 1, 1);
            break;
          case 2:
            applyLayout(LAYOUTS_2);
            break;
          case 3:
            applyLayout(LAYOUTS_3);
            break;
          case 4:
            applyLayout(LAYOUTS_4);
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

    /**
    Applies the layout selected by `layoutStyle` among the given ones. After
    the last defined layout there is one more style, where only the selected
    viewport is visible.
    */
    private void applyLayout(double[][][] layouts)
    {
        int style = layoutStyle % (layouts.length + 1);

        if ( style == layouts.length ) {
            showOnlySelectedViewport();
        }
        else {
            applyLayout(layouts[style]);
        }
    }

    /**
    Shows the selected viewport using all the area, hiding the others.
    */
    private void showOnlySelectedViewport()
    {
        int i;

        for ( i = 0; i < viewports.size(); i++ ) {
            Viewport viewport = viewports.get(i);
            viewport.setActive(i == selectedViewportIndex);
            if ( i == selectedViewportIndex ) {
                viewport.setPercentArea(0, 0, 1, 1);
            }
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
