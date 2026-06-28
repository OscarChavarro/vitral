package vsdk.toolkit.gui.widget;

import java.util.HashMap;
import vsdk.toolkit.gui.PresentationElement;

public abstract class WidgetCommandExecutor extends PresentationElement {
    protected HashMap<Integer, String> commandCache;

    public WidgetCommandExecutor()
    {
        commandCache = new HashMap<Integer, String>();
    }

    public void addIdToCommandCache(int id, String command)
    {
        Integer number = Integer.valueOf(id);
        commandCache.put(number, command);
    }

    public String getCommandFromId(int id)
    {
        Integer number = Integer.valueOf(id);
        return commandCache.get(number);
    }

    public abstract boolean executeMenuCommand(String inIdCommand);
}
