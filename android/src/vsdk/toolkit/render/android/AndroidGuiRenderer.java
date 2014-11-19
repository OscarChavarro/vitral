//===========================================================================
package vsdk.toolkit.render.android;

// Java basic classes
import java.util.ArrayList;

// Android GUI classes
import android.view.Menu;
import android.view.SubMenu;

// Vitral classes
import vsdk.toolkit.gui.Gui;
import vsdk.toolkit.gui.GuiCommandExecutor;
import vsdk.toolkit.gui.GuiMenu;
import vsdk.toolkit.gui.GuiMenuElement;
import vsdk.toolkit.gui.GuiMenuItem;
import vsdk.toolkit.gui.PresentationElement;

/**
*/
public class AndroidGuiRenderer extends PresentationElement {
    public static int currentMenuId = -1;
    public static int currentCommandId = 1;

    public static boolean
    executeCommandById(Gui inOutContext, int id, GuiCommandExecutor executor)
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
        Gui inOutContext,
        Menu inOutParentMenu,
        GuiCommandExecutor executor)
    {
        // Activate menu for new devices without menu button, on Android
        // versions 4.0 and up: put it on the action bar
        SubMenu popup;
        inOutParentMenu.add(1001, 1666, 1004, "Buscar");
        popup = inOutParentMenu.addSubMenu(1001, 1002, 1003, "Popup");
        buildMenubar(inOutContext, popup, executor);


        return popup;
    }
    
    /**
    Creates the contents for a PopUp menu. Should be replaced by standard
    Vitral based GUI.
    */
    private static void buildMenubar(
        Gui inOutContext, 
        SubMenu inOutPopupMenu,
        GuiCommandExecutor executor) {
        
        if ( inOutContext == null ) {
            inOutPopupMenu.addSubMenu(0, 100, 0, "NO GUI SPECIFIED!");
            return;
        }
        
        GuiMenu menubar = inOutContext.getMenubar();
        if ( menubar == null ) {
            inOutPopupMenu.addSubMenu(0, 100, 0, "NO MENUBAR IN GUI FILE!");
            return;
        }

        ArrayList<GuiMenuElement> children;
        int i;
        GuiMenuElement element;
        GuiMenu menu;
        
        children = menubar.getChildren();
        
        for ( i = 0; i < children.size(); i++ ) {
            element = children.get(i);
            if ( element instanceof GuiMenu ) {
                menu = (GuiMenu)element;
                buildPopupMenu(inOutContext, inOutPopupMenu, menu.getName(), executor);
            }
        }
    }

    private static void buildPopupMenu(
        Gui inOutContext, SubMenu parent, String name, GuiCommandExecutor executor) {
        GuiMenu menu = inOutContext.getPopup(name);
        SubMenu currentAndroidMenuWidget;

        currentAndroidMenuWidget = parent.addSubMenu(1, currentMenuId, 1, name);
        currentMenuId--;
        
        if ( menu == null ) {
            System.out.println("  - Popup menu not found on GUI");
        } else {
            ArrayList<GuiMenuElement> children;
            int i;
            GuiMenuElement element;

            children = menu.getChildren();
            for (i = 0; i < children.size(); i++) {
                element = children.get(i);
                
                if ( element instanceof GuiMenu ) {
                    GuiMenu submenu = (GuiMenu) element;
                    buildPopupMenu(
                        inOutContext, 
                        currentAndroidMenuWidget, 
                        submenu.getName(), 
                        executor);
                } else if ( element instanceof GuiMenuItem ) {
                    GuiMenuItem option = (GuiMenuItem) element;
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

//===========================================================================
//= EOF                                                                     =
//===========================================================================
