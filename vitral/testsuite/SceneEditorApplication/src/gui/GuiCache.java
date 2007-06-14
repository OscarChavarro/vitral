package gui;

import java.util.ArrayList;
import java.util.Iterator;

public class GuiCache
{
    private GuiMenuCache menubar;
    private ArrayList<GuiMenuCache> popupMenuList;

    public GuiCache()
    {
        menubar = null;
        popupMenuList = new ArrayList<GuiMenuCache>();
    }

    public void setMenubar(GuiMenuCache m)
    {
        menubar = m;
    }

    public GuiMenuCache getMenubar()
    {
        return menubar;
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

    public String toString()
    {
        String msg = "= GuiCache report =========================================================\n";
        msg = msg + "Gui cache structure contains " + popupMenuList.size() +
        " popup submenu structures registered\n";
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
