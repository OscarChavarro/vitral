//===========================================================================

package vsdk.transition.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

public class GuiCache
{
    private GuiMenuCache menubar;
    private ArrayList<GuiMenuCache> popupMenuList;
    private ArrayList<GuiCommandCache> commandList;
    private ArrayList<GuiButtonGroupCache> buttonGroupList;
    private HashMap<String, String> messagesTable;

    public GuiCache()
    {
        menubar = null;
        popupMenuList = new ArrayList<GuiMenuCache>();
        commandList = new ArrayList<GuiCommandCache>();
        buttonGroupList = new ArrayList<GuiButtonGroupCache>();
        messagesTable = new HashMap<String, String>();
    }

    public void addMessage(String id, String message)
    {
        messagesTable.put(id, message);
    }

    public String getMessage(String id)
    {
        String msg;

        msg = messagesTable.get(id);

        if ( msg == null ) {
            return id;
        }
        return msg;
    }

    public void setMenubar(GuiMenuCache m)
    {
        menubar = m;
    }

    public GuiMenuCache getMenubar()
    {
        return menubar;
    }

    public GuiCommandCache getCommandByName(String name)
    {
        GuiCommandCache command = null;
        GuiCommandCache candidate = null;
        Iterator i;

        for ( i = commandList.iterator(); i.hasNext(); ) {
            candidate = (GuiCommandCache)i.next();
            if ( candidate.getId().equals(name) ) {
                command = candidate;
                break;
            }
        }
        return command;
    }

    public GuiButtonGroupCache getButtonGroup(String name) {
        if ( name == null ) {
            return null;
        }

        GuiButtonGroupCache group = null, candidate;
        Iterator i;

        for ( i = buttonGroupList.iterator(); i.hasNext(); ) {
            candidate = (GuiButtonGroupCache)i.next();
            if ( candidate.getName().equals(name) ) {
                group = candidate;
                break;
            }
        }
        return group;
    }


    public GuiMenuCache getPopup(String name)
    {
        GuiMenuCache menu = null;
        GuiMenuCache candidate;

        Iterator i;
        for ( i = popupMenuList.iterator(); i.hasNext(); ) {
            candidate = (GuiMenuCache)i.next();
            if ( candidate.getName().equals(name) ) {
                menu = candidate;
                break;
            }
        }
        return menu;
    }

    public void addPopupMenu(GuiMenuCache p)
    {
        popupMenuList.add(p);
    }

    public void addCommand(GuiCommandCache c)
    {
        commandList.add(c);
    }

    public void addButtonGroup(GuiButtonGroupCache b)
    {
        buttonGroupList.add(b);
    }

    public String toString()
    {
        String msg = "= GuiCache report =========================================================\n";
        msg = msg + "Gui cache structure contains " + popupMenuList.size() +
            " popup submenu structures registered\n";
        msg = msg + "Gui cache structure contains " + commandList.size() +
            " commands registered\n";

        Iterator i;
        GuiCommandCache command;
        for ( i = commandList.iterator(); i.hasNext(); ) {
            command = (GuiCommandCache)i.next();
            msg = msg + command;
        }

        if ( menubar == null ) {
            msg = msg + "There is NO menubar!";
          }
          else {
            msg = msg + "There is a menubar active, called \"" + menubar.getName() + "\"\n";
            msg = msg + "Dumping menubar tree structure...\n";
            msg = msg + menubar;
        }
        msg = msg + "===========================================================================\n";
        return msg;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
