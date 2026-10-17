package gui.awt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import model.ApplicationModel;
import model.dialog.SceneHierarchyPresenter;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.dialog.components.DialogTreeNode;
import vsdk.toolkit.gui.widget.Widget;
import vsdk.toolkit.gui.widget.WidgetDialog;

/** Swing event and tree adapter for the shared scene selector definition. */
public class AwtSelectorDialog extends JDialog {
    private final SceneHierarchyPresenter presenter;
    private final WidgetDialog definition;
    private final JTree tree = new JTree();
    private boolean updating;

    public AwtSelectorDialog(JFrame owner, ApplicationModel model, Widget messages, Runnable repaint) {
        super(owner, false);
        definition = messages.getDialogById("scene.selector");
        if (definition == null) throw new IllegalStateException("Missing scene.selector dialog");
        presenter = new SceneHierarchyPresenter(model, messages, repaint);
        setName(definition.getId());
        setTitle(messages.getMessage(definition.getTitleKey()));
        setMinimumSize(new Dimension(390, 460)); setSize(450, 560);
        setLocationRelativeTo(owner); setLayout(new BorderLayout());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.setRootVisible(false);
        tree.addTreeSelectionListener(event -> applyTreeSelection());
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        tree.getActionMap().put("delete", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                performAction("deleteSelected");
            }
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) { showPopup(event); }
            @Override public void mouseReleased(MouseEvent event) { showPopup(event); }
        });
        add(new JScrollPane(tree), BorderLayout.CENTER);
        rebuild();
    }
    public final void rebuild() {
        DefaultMutableTreeNode root = buildNode(presenter.hierarchy());
        updating = true; tree.setModel(new DefaultTreeModel(root));
        for (int i = 0; i < root.getChildCount(); i++) tree.expandPath(
            new TreePath(((DefaultMutableTreeNode)root.getChildAt(i)).getPath()));
        ArrayList<TreePath> paths = new ArrayList<TreePath>();
        collectSelected(paths, root);
        tree.setSelectionPaths(paths.toArray(new TreePath[0])); updating = false;
    }
    private DefaultMutableTreeNode buildNode(DialogTreeNode node) {
        DefaultMutableTreeNode result = new DefaultMutableTreeNode(node);
        for (DialogTreeNode child : node.getChildren()) result.add(buildNode(child));
        return result;
    }
    private void collectSelected(ArrayList<TreePath> paths, DefaultMutableTreeNode node) {
        if (presenter.isSelected((DialogTreeNode)node.getUserObject())) paths.add(new TreePath(node.getPath()));
        for (int i = 0; i < node.getChildCount(); i++)
            collectSelected(paths, (DefaultMutableTreeNode)node.getChildAt(i));
    }
    private ArrayList<DialogTreeNode> selectedNodes() {
        ArrayList<DialogTreeNode> result = new ArrayList<DialogTreeNode>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null) for (TreePath path : paths)
            result.add((DialogTreeNode)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject());
        return result;
    }
    private void applyTreeSelection() {
        if (!updating) presenter.select(selectedNodes());
    }
    public void selectBody(int index) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getModel().getRoot();
        DefaultMutableTreeNode bodies = (DefaultMutableTreeNode)root.getChildAt(2);
        if (index < 0) tree.clearSelection();
        else tree.setSelectionPath(new TreePath(((DefaultMutableTreeNode)bodies.getChildAt(index)).getPath()));
    }
    public void performAction(String action) {
        if (definition.getElementByAction(action) == null)
            throw new IllegalArgumentException("Unknown selector action: " + action);
        if ("deleteSelected".equals(action)) { presenter.deleteSelected(); rebuild(); }
        else if ("toggleVisibility".equals(action)) {
            ArrayList<SimpleBody> bodies = presenter.selectedBodies(selectedNodes());
            if (!bodies.isEmpty()) { presenter.setHidden(bodies, !presenter.anyHidden(bodies)); rebuild(); }
        }
    }
    private void showPopup(MouseEvent event) {
        if (!event.isPopupTrigger()) return;
        TreePath path = tree.getPathForLocation(event.getX(), event.getY());
        if (path != null && !tree.isPathSelected(path)) tree.setSelectionPath(path);
        ArrayList<SimpleBody> bodies = presenter.selectedBodies(selectedNodes());
        if (bodies.isEmpty()) return;
        boolean anyHidden = presenter.anyHidden(bodies);
        JMenuItem item = new JMenuItem(presenter.text(anyHidden ? "IDM_SELECTOR_SHOW" : "IDM_SELECTOR_HIDE",
            anyHidden ? "Show objects" : "Hide objects"));
        item.addActionListener(e -> performAction("toggleVisibility"));
        JPopupMenu menu = new JPopupMenu(); menu.add(item); menu.show(tree, event.getX(), event.getY());
    }
}
