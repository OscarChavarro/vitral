package vsdk.toolkit.gui.widget;

// Java classes
import java.util.ArrayList;
import java.util.HashMap;

// VSDK classes
import vsdk.toolkit.gui.PresentationElement;
import vsdk.toolkit.gui.widget.variable.WidgetVariable;

/**
In order to understand this class, the following concepts must be taken into
account: - WidgetVariable - WidgetCommand - Reflection (introspection) design
pattern - Menubars, buttons bars, and other are based upon WidgetCommands -
Dialogs are based upon WidgetVariables and WidgetCommands - Dialogs and menus are
hierarchical
*/
public class Widget extends PresentationElement {
    // Basic / fundamental / atomic elements

    private ArrayList<WidgetCommand> commandList;
    private ArrayList<WidgetVariable> variableList;
    private HashMap<String, String> messagesTable;
    // Composite elements
    private WidgetMenu menubar;
    private ArrayList<WidgetMenu> popupMenuList;
    private ArrayList<WidgetButtonGroup> buttonGroupList;
    private ArrayList<WidgetDialog> dialogList;

    public ArrayList<WidgetDialog> getDialogList() {
        return dialogList;
    }

    public void setDialogList(ArrayList<WidgetDialog> dialogList) {
        this.dialogList = dialogList;
    }

    public Widget() {
        menubar = null;
        popupMenuList = new ArrayList<WidgetMenu>();
        commandList = new ArrayList<WidgetCommand>();
        buttonGroupList = new ArrayList<WidgetButtonGroup>();
        messagesTable = new HashMap<String, String>();
        /*
         * TODO variableList should be hashMap
         */
        variableList = new ArrayList<WidgetVariable>();
        dialogList = new ArrayList<WidgetDialog>();
    }

    public void addMessage(String id, String message) {
        messagesTable.put(id, message);
    }

    public String getMessage(String id) {
        String msg;

        msg = messagesTable.get(id);

        if (msg == null) {
            return id;
        }
        return msg;
    }

    public void setMenubar(WidgetMenu m) {
        menubar = m;
    }

    public WidgetMenu getMenubar() {
        return menubar;
    }

    public WidgetCommand getCommandByName(String name) {
        WidgetCommand command = null;
        WidgetCommand candidate;
        int i;

        for (i = 0; i < commandList.size(); i++) {
            candidate = commandList.get(i);
            if (candidate.getId().equals(name)) {
                command = candidate;
                break;
            }
        }
        return command;
    }

    public WidgetButtonGroup getButtonGroup(String name)
    {
        if (name == null) {
            return null;
        }

        WidgetButtonGroup group = null, candidate;
        int i;

        for (i = 0; i < buttonGroupList.size(); i++) {
            candidate = buttonGroupList.get(i);
            if ( candidate.getName().equals(name) ) {
                group = candidate;
                break;
            }
        }
        return group;
    }

    public WidgetMenu getPopup(String name) {
        WidgetMenu menu = null;
        WidgetMenu candidate;

        int i;
        for (i = 0; i < popupMenuList.size(); i++) {
            candidate = popupMenuList.get(i);
            if (candidate.getName().equals(name)) {
                menu = candidate;
                break;
            }
        }
        return menu;
    }

    public void addPopupMenu(WidgetMenu p) {
        popupMenuList.add(p);
    }

    public void addCommand(WidgetCommand c) {
        commandList.add(c);
    }

    public void addButtonGroup(WidgetButtonGroup b) {
        buttonGroupList.add(b);
    }

    public void addDialog(WidgetDialog dialog) {
        dialogList.add(dialog);
    }

    public void addVariable(WidgetVariable variable) {
        variableList.add(variable);
    }

    public WidgetVariable getVariableByName(String name){
        int i;
        for(i = 0; i < variableList.size(); i++){
            if(variableList.get(i).getName().equals(name)){
                return variableList.get(i);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        String msg = "= Widget report =========================================================\n";
        msg = msg + "Widget cache structure contains " + popupMenuList.size()
                + " popup submenu structures registered\n";
        msg = msg + "Widget cache structure contains " + commandList.size()
                + " commands registered\n";

        int i;
        WidgetCommand command;
        for (i = 0; i < commandList.size(); i++) {
            command = commandList.get(i);
            msg = msg + command;
        }

        if (menubar == null) {
            msg = msg + "There is NO menubar!";
        } else {
            msg = msg + "There is a menubar active, called \"" + menubar.getName() + "\"\n";
            msg = msg + "Dumping menubar tree structure...\n";
            msg = msg + menubar;
        }

        /**
         * TODO print lists of WidgetDialogs and variables pending
         */

        //-----------------------------------------------------------------------------------
        WidgetDialog dialog;
        for (i = 0; i < dialogList.size(); i++) {
            dialog = dialogList.get(i);
            msg = msg + dialog.toString();
        }

        WidgetVariable variable;
        for (i = 0; i < variableList.size(); i++) {
            variable = variableList.get(i);
            msg = msg + variable.toString();
        }
        //-----------------------------------------------------------------------------------
        msg = msg + "===========================================================================\n";

        return msg;
    }
}
