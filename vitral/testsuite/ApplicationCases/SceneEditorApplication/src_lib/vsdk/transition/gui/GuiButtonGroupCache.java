//===========================================================================
//= Initial version: august 9 2007

package vsdk.transition.gui;

import java.util.ArrayList;
import java.util.Iterator;

public class GuiButtonGroupCache
{
    private ArrayList<GuiCommandCache> commandReferenceList;
    private GuiCache context;
    private String name;

    private boolean showText;
    private boolean showIcons;
    private boolean showTitle;
    private int direction;
    //private int side;

    public static final int HORIZONTAL = 1;
    public static final int VERTICAL = 2;

    public GuiButtonGroupCache(GuiCache parent)
    {
        commandReferenceList = new ArrayList<GuiCommandCache>();
        context = parent;
    }

    public void setShowText(boolean f)
    {
        showText = f;
    }

    public void setShowIcons(boolean f)
    {
        showIcons = f;
    }

    public void setTitle(boolean f)
    {
        showTitle = f;
    }

    public void setDirection(int d)
    {
        direction = d;
    }

    public int getDirection()
    {
        return direction;
    }

    public boolean isShowTextSet()
    {
        return showText;
    }

    public boolean isShowIconsSet()
    {
        return showIcons;
    }

    public boolean isShowTitleSet()
    {
        return showTitle;
    }

    public ArrayList<GuiCommandCache> getCommands()
    {
        return commandReferenceList;
    }

    public void setName(String n)
    {
        name = n;
    }

    public String getName()
    {
        return name;
    }

    public void addCommandByName(String commandName)
    {
        GuiCommandCache command = context.getCommandByName(commandName);

        if ( command != null ) {
            commandReferenceList.add(command);
    }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
