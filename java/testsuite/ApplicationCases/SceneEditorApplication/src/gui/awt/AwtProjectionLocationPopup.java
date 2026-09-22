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

import gui.PopupDismissClickFilter;

/**
Awt/Swing presentation of the menu of a viewport: the
`VIEWPORT_SET_PROJECTION_LOCATION` popup of the viewport set, to change the
projection location of a viewport (Perspective, Top, ...), followed, after a
separator, by the `VIEWPORT_SET_RENDER_MODE` popup, to render it with the GPU
or the CPU (raytracing). Texts come from the I18N context of the viewport set
(so it always shows the language currently selected by the user). The
projection and the render mode currently used are marked.

The menu is built each time it is requested. It is a heavyweight popup, since
it is shown over a heavyweight canvas (i.e. OpenGL), and it is posted over the
root pane of the window, which is what allows Swing to give it the keyboard
focus: the user can move with the up and down keys and select with enter or
space (escape closes it). Clicking outside closes the menu without applying
anything, and that click is not processed by the canvas (see
`consumesMouseEvent` and `PopupDismissClickFilter`). When the menu closes, the keyboard focus goes back to
the canvas.
*/
public class AwtProjectionLocationPopup
{
    private final ViewportSet viewportSet;
    private final ViewportSetInteractionTechniques techniques;
    private final Component canvas;
    private JPopupMenu popup;
    private final PopupDismissClickFilter dismissClickFilter;

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
        this.dismissClickFilter = new PopupDismissClickFilter();
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
        JRadioButtonMenuItem current = fillMenu(definition, viewport,
            viewport.getProjectionLocationCommand());
        WidgetMenu renderModeDefinition =
            context.getPopup(ViewportSetCommands.POPUP_RENDER_MODE);
        if ( renderModeDefinition != null ) {
            popup.addSeparator();
            fillMenu(renderModeDefinition, viewport, viewport.getRenderModeCommand());
        }
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
            {
                dismissClickFilter.popupClosed(System.currentTimeMillis());
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
        dismissClickFilter.popupShown();
        return true;
    }

    /**
    Adds the items of a popup definition as a group of radio items.
    @param definition popup of the I18N context
    @param viewport viewport the commands act over
    @param currentCommand command of the item to mark as selected
    @return the item marked as selected, or null if none
    */
    private JRadioButtonMenuItem fillMenu(WidgetMenu definition, Viewport viewport,
                                          String currentCommand)
    {
        ButtonGroup group = new ButtonGroup();
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
    Mouse handlers of the canvas must ask this before processing the mouse
    presses, releases, clicks and drags: the events of a press that closed
    the menu are consumed (see `PopupDismissClickFilter`).
    @param event mouse event of the canvas
    @return true if the event must be ignored
    */
    public boolean consumesMouseEvent(MouseEvent event)
    {
        PopupDismissClickFilter.MouseEventKind kind;

        switch ( event.getID() ) {
          case MouseEvent.MOUSE_PRESSED:
            kind = PopupDismissClickFilter.MouseEventKind.PRESS;
            break;
          case MouseEvent.MOUSE_RELEASED:
            kind = PopupDismissClickFilter.MouseEventKind.RELEASE;
            break;
          case MouseEvent.MOUSE_DRAGGED:
            kind = PopupDismissClickFilter.MouseEventKind.DRAG;
            break;
          case MouseEvent.MOUSE_CLICKED:
            kind = PopupDismissClickFilter.MouseEventKind.CLICK;
            break;
          default:
            kind = PopupDismissClickFilter.MouseEventKind.OTHER;
            break;
        }
        return dismissClickFilter.consumes(kind, System.currentTimeMillis());
    }
}
