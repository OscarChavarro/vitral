package gui.awt;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.ButtonGroup;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportSet;
import vsdk.toolkit.gui.viewport.ViewportSetCommands;
import vsdk.toolkit.gui.viewport.ViewportSetInteractionTechniques;
import vsdk.toolkit.gui.widget.Widget;
import vsdk.toolkit.gui.widget.WidgetMenu;
import vsdk.toolkit.gui.widget.WidgetMenuElement;
import vsdk.toolkit.gui.widget.WidgetMenuItem;

/**
Awt/Swing presentation of the `VIEWPORT_SET_PROJECTION_LOCATION` popup of the
viewport set: a popup menu to change the projection location of a viewport
(Perspective, Top, ...), with the texts of the I18N context of the viewport
set (so it always shows the language currently selected by the user). The
projection currently used is marked.

The menu is built each time it is requested. It is a heavyweight popup, since
it is shown over a heavyweight canvas (i.e. OpenGL), and it is posted over the
root pane of the window, which is what allows Swing to give it the keyboard
focus: the user can move with the up and down keys and select with enter or
space (escape closes it). Clicking outside closes the menu without applying
anything, and that click is not processed by the canvas (see
`consumesMouseEvent`). When the menu closes, the keyboard focus goes back to
the canvas.
*/
public class AwtProjectionLocationPopup
{
    // The press that closes the popup is delivered to the canvas right after
    private static final long OUTSIDE_PRESS_WINDOW_MILLIS = 300;

    private final ViewportSet viewportSet;
    private final ViewportSetInteractionTechniques techniques;
    private final Component canvas;
    private JPopupMenu popup;
    private long closedAtMillis;
    private boolean swallowingClick;

    /**
    @param viewportSet the set whose I18N context gives the texts
    @param techniques the techniques that execute the chosen command
    @param canvas the component where the viewport set is presented; it
    receives the keyboard focus back when the menu closes
    */
    public AwtProjectionLocationPopup(ViewportSet viewportSet,
                                      ViewportSetInteractionTechniques techniques,
                                      Component canvas)
    {
        this.viewportSet = viewportSet;
        this.techniques = techniques;
        this.canvas = canvas;
        this.popup = null;
        this.closedAtMillis = 0;
        this.swallowingClick = false;
    }

    /**
    @return true if the menu is currently shown
    */
    public boolean isVisible()
    {
        return popup != null && popup.isVisible();
    }

    /**
    Shows the menu to change the projection location of a viewport.
    @param viewport the viewport to change
    @param canvasX horizontal position, in coordinates of the canvas
    @param canvasY vertical position, in coordinates of the canvas
    @return false if the menu can not be shown because the I18N context does
    not define it
    */
    public boolean show(Viewport viewport, int canvasX, int canvasY)
    {
        Widget context = viewportSet.getI18nContext();
        WidgetMenu definition = null;

        if ( context != null ) {
            definition = context.getPopup(ViewportSetCommands.POPUP_PROJECTION_LOCATION);
        }
        if ( definition == null ) {
            return false;
        }
        if ( isVisible() ) {
            popup.setVisible(false);
        }

        popup = new JPopupMenu();
        popup.setLightWeightPopupEnabled(false);
        JRadioButtonMenuItem current = fillMenu(definition, viewport);
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
            {
                closedAtMillis = System.currentTimeMillis();
                // Give the focus back once Swing has finished with the menu
                SwingUtilities.invokeLater(() -> canvas.requestFocusInWindow());
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e)
            {
            }
        });

        // Posted over the root pane: Swing gives it the keyboard focus
        JRootPane root = SwingUtilities.getRootPane(canvas);
        Component invoker = root == null ? canvas : root;
        Point location = SwingUtilities.convertPoint(canvas, canvasX, canvasY, invoker);
        popup.show(invoker, location.x, location.y);

        // Keyboard navigation starts at the projection in use
        if ( current != null ) {
            MenuSelectionManager.defaultManager().setSelectedPath(
                new MenuElement[] {popup, current});
        }
        swallowingClick = false;
        return true;
    }

    private JRadioButtonMenuItem fillMenu(WidgetMenu definition, Viewport viewport)
    {
        ButtonGroup group = new ButtonGroup();
        String currentCommand = viewport.getProjectionLocationCommand();
        JRadioButtonMenuItem currentItem = null;

        for ( WidgetMenuElement element : definition.getChildren() ) {
            if ( !(element instanceof WidgetMenuItem) ) {
                continue;
            }
            WidgetMenuItem definitionItem = (WidgetMenuItem)element;
            if ( definitionItem.isSeparator() ) {
                popup.addSeparator();
                continue;
            }

            final String command = definitionItem.getCommandName();
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(definitionItem.getName());
            if ( definitionItem.getMnemonic() != 0 ) {
                item.setMnemonic(KeyEvent.getExtendedKeyCodeForChar(definitionItem.getMnemonic()));
            }
            if ( command.equals(currentCommand) ) {
                item.setSelected(true);
                currentItem = item;
            }
            item.addActionListener(e -> {
                techniques.processCommand(command, viewport);
                canvas.repaint();
            });
            group.add(item);
            popup.add(item);
        }
        return currentItem;
    }

    /**
    A click outside the menu closes it, and that click must not act over the
    canvas (i.e. selecting another viewport): the user only wanted to leave the
    menu. Mouse handlers of the canvas must ask this before processing the
    mouse presses, releases, clicks and drags: the events of a press that
    closed the menu are consumed.
    @param event
    @return true if the event must be ignored
    */
    public boolean consumesMouseEvent(MouseEvent event)
    {
        switch ( event.getID() ) {
          case MouseEvent.MOUSE_PRESSED:
            swallowingClick = closedAtMillis != 0 &&
                System.currentTimeMillis() - closedAtMillis <= OUTSIDE_PRESS_WINDOW_MILLIS;
            closedAtMillis = 0;
            return swallowingClick;
          case MouseEvent.MOUSE_RELEASED:
          case MouseEvent.MOUSE_DRAGGED:
            return swallowingClick;
          case MouseEvent.MOUSE_CLICKED:
            boolean swallowed = swallowingClick;
            swallowingClick = false;
            return swallowed;
          default:
            return false;
        }
    }
}
