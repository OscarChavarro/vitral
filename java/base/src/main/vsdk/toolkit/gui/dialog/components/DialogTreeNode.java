package vsdk.toolkit.gui.dialog.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A hierarchy row with identity separate from its display label. */
public final class DialogTreeNode {
    private final String label;
    private final Object identity;
    private final ArrayList<DialogTreeNode> children = new ArrayList<DialogTreeNode>();

    public DialogTreeNode(String label, Object identity) {
        this.label = label;
        this.identity = identity;
    }
    public String getLabel() { return label; }
    public Object getIdentity() { return identity; }
    public List<DialogTreeNode> getChildren() { return Collections.unmodifiableList(children); }
    public void add(DialogTreeNode child) { children.add(child); }
    @Override public String toString() { return label; }
}
