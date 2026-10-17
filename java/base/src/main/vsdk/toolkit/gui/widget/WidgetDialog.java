package vsdk.toolkit.gui.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private ArrayList<WidgetElement> widgetElementList;
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
    private String titleKey;
    private boolean modal;
    private final ArrayList<Element> elements = new ArrayList<Element>();

    /** Ordered, toolkit-independent content of a dialog. */
    public static final class Element {
        private final String kind;
        private final String id;
        private final String labelKey;
        private final String binding;
        private final String action;
        private final ArrayList<Element> children;

        public Element(String kind, String id, String labelKey, String binding,
                       String action, List<Element> children) {
            this.kind = kind;
            this.id = id;
            this.labelKey = labelKey;
            this.binding = binding;
            this.action = action;
            this.children = new ArrayList<Element>(children);
        }
        public String getKind() { return kind; }
        public String getId() { return id; }
        public String getLabelKey() { return labelKey; }
        public String getBinding() { return binding; }
        public String getAction() { return action; }
        public List<Element> getChildren() { return Collections.unmodifiableList(children); }
    }

    public String getTitleKey() { return titleKey; }
    public void setTitleKey(String titleKey) { this.titleKey = titleKey; }
    public boolean isModal() { return modal; }
    public void setModal(boolean modal) { this.modal = modal; }
    public List<Element> getElements() { return Collections.unmodifiableList(elements); }
    public void addElement(Element element) { elements.add(element); }
    public Element getElementByAction(String action) {
        for (Element element : elements) {
            Element found = findAction(element, action);
            if (found != null) return found;
        }
        return null;
    }
    private static Element findAction(Element element, String action) {
        if (action.equals(element.getAction())) return element;
        for (Element child : element.getChildren()) {
            Element found = findAction(child, action);
            if (found != null) return found;
        }
        return null;
    }

    public WidgetDialog() {
        widgetElementList = new ArrayList<WidgetElement>();
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
        return widgetElementList;
    }

    public void setWidgetElementList(ArrayList<WidgetElement> widgetElementList) {
        this.widgetElementList = widgetElementList;
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
        return widgetElementList;
    }

    public void setChildren(ArrayList<WidgetElement> widgetElementList) {
        this.widgetElementList = widgetElementList;
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
        for (int j = 0; j < widgetElementList.size(); j++) {
            msg = msg + "    " + widgetElementList.get(j).toString() + "\n";
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
