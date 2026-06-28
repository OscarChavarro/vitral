package vsdk.toolkit.gui.widget;

import java.util.ArrayList;

public class WidgetButtonGroup extends WidgetElement
{
    private final ArrayList<WidgetCommand> commandReferenceList;
    private String name;

    private boolean showText;
    private boolean showIcons;
    private boolean showTitle;
    private int direction;

    public static final int HORIZONTAL = 1;
    public static final int VERTICAL = 2;

    public WidgetButtonGroup(Widget parent)
    {
        commandReferenceList = new ArrayList<WidgetCommand>();
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

    public ArrayList<WidgetCommand> getCommands()
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
        WidgetCommand command = context.getCommandByName(commandName);

        if ( command != null ) {
            commandReferenceList.add(command);
        }
    }
}
