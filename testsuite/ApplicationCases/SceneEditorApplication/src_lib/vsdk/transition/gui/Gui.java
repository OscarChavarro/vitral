//===========================================================================

package vsdk.transition.gui;

import java.util.ArrayList;
import java.util.HashMap;

public class Gui
{
    private GuiMenu menubar;
    private ArrayList<GuiMenu> popupMenuList;
    private ArrayList<GuiCommand> commandList;
    private ArrayList<GuiButtonGroup> buttonGroupList;
    private HashMap<String, String> messagesTable;

    public Gui()
    {
        menubar = null;
        popupMenuList = new ArrayList<GuiMenu>();
        commandList = new ArrayList<GuiCommand>();
        buttonGroupList = new ArrayList<GuiButtonGroup>();
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

    public void setMenubar(GuiMenu m)
    {
        menubar = m;
    }

    public GuiMenu getMenubar()
    {
        return menubar;
    }

    public GuiCommand getCommandByName(String name)
    {
        GuiCommand command = null;
        GuiCommand candidate = null;
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

    public GuiButtonGroup getButtonGroup(String name) {
        if ( name == null ) {
            return null;
        }

        GuiButtonGroup group = null, candidate;
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


    public GuiMenu getPopup(String name)
    {
        GuiMenu menu = null;
        GuiMenu candidate;

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

    public void addPopupMenu(GuiMenu p)
    {
        popupMenuList.add(p);
    }

    public void addCommand(GuiCommand c)
    {
        commandList.add(c);
    }

    public void addButtonGroup(GuiButtonGroup b)
    {
        buttonGroupList.add(b);
    }

    public String toString()
    {
        String msg = "= Gui report =========================================================\n";
        msg = msg + "Gui cache structure contains " + popupMenuList.size() +
            " popup submenu structures registered\n";
        msg = msg + "Gui cache structure contains " + commandList.size() +
            " commands registered\n";

        int i;
        GuiCommand command;
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
