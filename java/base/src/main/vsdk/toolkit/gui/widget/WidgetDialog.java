package vsdk.toolkit.gui.widget;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;

/**
This class plays a role of internal node on an n-ary tree in the composite
design pattern.
*/
public class WidgetDialog extends WidgetElement {

    public static final int ORIENTATION_HORIZONTAL = 0x01;
    public static final int ORIENTATION_VERTICAL = 0x02;
    private String id;
    private String name;
    private int orientation;
    private ArrayList<WidgetElement> children;
    /// In the importing from file process, a dialog could be "incomplete",
    /// due to having a variable reference to a variable that is not loaded
    /// yet.  In such situation, a two pass processing is performed:
    /// On first pass does not create neither associate any variable, but store
    /// its incomplete references (names) on this ArrayList. On second pass
    /// the factory traverse this list in order to add current variables
    /// from context. Check WidgetPersistence.importAquynzaWidgetDialog method.
    private ArrayList<String> pendingVariableNames;
    private ArrayList<String> pendingCommandNames;
    private ArrayList<String> pendingDialogNames;
    private ArrayList<String> pendingDialogRefNames;
    private boolean collapsable;

    public WidgetDialog() {
        children = new ArrayList<WidgetElement>();
        pendingVariableNames = new ArrayList<String>();
        pendingCommandNames = new ArrayList<String>();
        pendingDialogNames = new ArrayList<String>();
        pendingDialogRefNames = new ArrayList<String>();
        orientation = ORIENTATION_VERTICAL;
        collapsable = false;
    }

    public boolean isCollapsable() {
        return collapsable;
    }

    public void setCollapsable(boolean collapsable) {
        this.collapsable = collapsable;
    }


    public ArrayList<String> getPendingCommandNames() {
        return pendingCommandNames;
    }

    public void setPendingCommandNames(ArrayList<String> pendingCommandNames) {
        this.pendingCommandNames = pendingCommandNames;
    }

    public ArrayList<WidgetElement> getWidgetElementList() {
        return children;
    }

    public void setWidgetElementList(ArrayList<WidgetElement> widgetElementList) {
        this.children = widgetElementList;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public ArrayList<String> getPendingVariableNames() {
        return pendingVariableNames;
    }

    public void setPendingVariableNames(ArrayList<String> pendingVariableNames) {
        this.pendingVariableNames = pendingVariableNames;
    }

    public ArrayList<String> getPendingDialogNames() {
        return pendingDialogNames;
    }

    public void setPendingDialogNames(ArrayList<String> pendingDialogNames) {
        this.pendingDialogNames = pendingDialogNames;
    }

    public ArrayList<WidgetElement> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<WidgetElement> children) {
        this.children = children;
    }

    public ArrayList<String> getPendingDialogRefNames() {
        return pendingDialogRefNames;
    }

    public void setPendingDialogRefNames(ArrayList<String> pendingDialogRefNames) {
        this.pendingDialogRefNames = pendingDialogRefNames;
    }

    /**
     * Given a variableName, this method asks the context for a WidgetVariable
     * pointer in order to reference the given variable. If that variable
     * doesn't exist, an exception is thrown.
     */
    public void associateVariable(String variableName) {
        Logger.reportMessage(this, VSDK.FATAL_ERROR, "associateVariable",
                "Variable " + variableName + " not found!");
    }

    public void getPendingDialogNames(ArrayList<String> pendingDialogNames) {
        this.pendingDialogNames = pendingDialogNames;
    }

    @Override
    public String toString() {
        WidgetDialog dialog;
        String msg = "";

        msg = msg + "    DIALOG: " + this.getId() + "\n";
        for (int j = 0; j < children.size(); j++) {
            msg = msg + "    " + children.get(j).toString() + "\n";
        }
        for (int j = 0; j < pendingCommandNames.size(); j++) {
            msg = msg + "    Commandname: " + pendingCommandNames.get(j) + "\n";
        }

        for (int j = 0; j < pendingDialogRefNames.size(); j++) {
            msg = msg + "    DialogRefNames: " + pendingDialogRefNames.get(j) + "\n";
        }

        for (int j = 0; j < pendingVariableNames.size(); j++) {
            msg = msg + "    VariableNames: " + pendingVariableNames.get(j) + "\n";
        }
        return msg;
    }
}
