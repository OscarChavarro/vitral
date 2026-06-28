package vsdk.toolkit.render.android;

// Java basic classes
import java.util.ArrayList;

// Android GUI classes
import android.view.Menu;
import android.view.SubMenu;

// Vitral classes
import vsdk.toolkit.gui.widget.Widget;
import vsdk.toolkit.gui.widget.WidgetCommandExecutor;
import vsdk.toolkit.gui.widget.WidgetMenu;
import vsdk.toolkit.gui.widget.WidgetMenuElement;
import vsdk.toolkit.gui.widget.WidgetMenuItem;
import vsdk.toolkit.gui.PresentationElement;

public class AndroidGuiRenderer extends PresentationElement {
    public static int currentMenuId = -1;
    public static int currentCommandId = 1;

    public static boolean
    executeCommandById(Widget inOutContext, int id, WidgetCommandExecutor executor)
    {
        String command = executor.getCommandFromId(id);
        return executor.executeMenuCommand(command);
    }

    public static void emptyMenubar(Menu inOutParentMenu)
    {
        inOutParentMenu.clear();
    }

    /**
    @param inOutContext
    @param inOutParentMenu
    @param executor
    @return
    */
    public static SubMenu buildMenubar(
        Widget inOutContext,
        Menu inOutParentMenu,
        WidgetCommandExecutor executor)
    {
        // Activate menu for new devices without menu button, on Android
        // versions 4.0 and up: put it on the action bar
        SubMenu popup;
        //inOutParentMenu.add(1001, 1666, 1004, "Buscar");
        popup = inOutParentMenu.addSubMenu(1001, 1002, 1003, "MENU");
        buildMenubar(inOutContext, popup, executor);


        return popup;
    }
    
    /**
    Creates the contents for a PopUp menu. Should be replaced by standard
    Vitral based GUI.
    */
    private static void buildMenubar(
        Widget inOutContext,
        SubMenu inOutPopupMenu,
        WidgetCommandExecutor executor) {

        if ( inOutContext == null ) {
            inOutPopupMenu.addSubMenu(0, 100, 0, "NO GUI SPECIFIED!");
            return;
        }

        WidgetMenu menubar = inOutContext.getMenubar();
        if ( menubar == null ) {
            inOutPopupMenu.addSubMenu(0, 100, 0, "NO MENUBAR IN GUI FILE!");
            return;
        }

        ArrayList<WidgetMenuElement> children;
        int i;
        WidgetMenuElement element;
        WidgetMenu menu;

        children = menubar.getChildren();

        for ( i = 0; i < children.size(); i++ ) {
            element = children.get(i);
            if ( element instanceof WidgetMenu ) {
                menu = (WidgetMenu)element;
                buildPopupMenu(inOutContext, inOutPopupMenu, menu.getName(), executor);
            }
        }
    }

    private static void buildPopupMenu(
        Widget inOutContext, SubMenu parent, String name, WidgetCommandExecutor executor) {
        WidgetMenu menu = inOutContext.getPopup(name);
        SubMenu currentAndroidMenuWidget;

        currentAndroidMenuWidget = parent.addSubMenu(1, currentMenuId, 1, name);
        currentMenuId--;

        if ( menu == null ) {
            System.out.println("  - Popup menu not found on GUI");
        } else {
            ArrayList<WidgetMenuElement> children;
            int i;
            WidgetMenuElement element;

            children = menu.getChildren();
            for (i = 0; i < children.size(); i++) {
                element = children.get(i);

                if ( element instanceof WidgetMenu ) {
                    WidgetMenu submenu = (WidgetMenu) element;
                    buildPopupMenu(
                        inOutContext,
                        currentAndroidMenuWidget,
                        submenu.getName(),
                        executor);
                } else if ( element instanceof WidgetMenuItem ) {
                    WidgetMenuItem option = (WidgetMenuItem) element;
                    if ( option.isSeparator() ) {
                        currentAndroidMenuWidget.add(0, currentCommandId, 0, "------");
                        currentCommandId++;
                    } else {
                        currentAndroidMenuWidget.add(0, currentCommandId, 0, option.getName());
                        executor.addIdToCommandCache(currentCommandId, option.getCommandName());
                        currentCommandId++;
                    }
                }
            }
        }
    }
}
