//===========================================================================

package vsdk.transition.gui;

import java.util.ArrayList;
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
        int i;

        for ( i = 0; i < commandList.size(); i++ ) {
            candidate = commandList.get(i);
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
        int i;

        for ( i = 0; i < buttonGroupList.size(); i++ ) {
            candidate = buttonGroupList.get(i);
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

        int i;
        for ( i = 0; i < popupMenuList.size(); i++ ) {
            candidate = popupMenuList.get(i);
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

        int i;
        GuiCommandCache command;
        for ( i = 0; i < commandList.size(); i++ ) {
            command = commandList.get(i);
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
