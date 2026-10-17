package gui.awt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.gui.widget.Widget;
import model.ApplicationModel;
import model.Scene;
import model.history.SceneSelectionOperation;
import model.selection.SceneSelectionEditor;

/** The multiple-selection scene tree shown with the H key. */
public class AwtSelectorDialog extends JDialog {
    private enum Kind { BODY, LIGHT, CAMERA, GIZMO }
    private record Entry(Kind kind, Object element, String label) {
        @Override public String toString() { return label; }
    }
    private final ApplicationModel model;
    private final Widget messages;
    private final Runnable repaint;
    private final JTree tree = new JTree();
    private boolean updating;

    public AwtSelectorDialog(JFrame owner, ApplicationModel model, Widget messages, Runnable repaint) {
        super(owner, false);
        this.model = model; this.messages = messages; this.repaint = repaint;
        setTitle(text("IDM_SELECTOR_TITLE", "Scene selector"));
        setMinimumSize(new Dimension(390, 460)); setSize(450, 560);
        setLocationRelativeTo(owner); setLayout(new BorderLayout());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        // The model root is only structural: the four requested categories
        // are the visible roots of the selector.
        tree.setRootVisible(false);
        tree.addTreeSelectionListener(event -> applyTreeSelection());
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        tree.getActionMap().put("delete", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { deleteSelected(); }
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) { showPopup(event); }
            @Override public void mouseReleased(MouseEvent event) { showPopup(event); }
        });
        add(new JScrollPane(tree), BorderLayout.CENTER);
        rebuild();
    }

    public final void rebuild() {
        Scene scene = model.getScene();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Scene");
        DefaultMutableTreeNode lights = branch("IDM_SELECTOR_LIGHTS", "Lights");
        DefaultMutableTreeNode cameras = branch("IDM_SELECTOR_CAMERAS", "Cameras");
        DefaultMutableTreeNode bodies = branch("IDM_SELECTOR_OBJECTS", "Geometry objects");
        root.add(lights); root.add(cameras); root.add(bodies);
        addEntries(lights, Kind.LIGHT, scene.scene.getLights(), scene);
        addEntries(cameras, Kind.CAMERA, scene.scene.getCameras(), scene);
        addEntries(bodies, Kind.BODY, scene.scene.getSimpleBodies(), scene);
        updating = true; tree.setModel(new DefaultTreeModel(root));
        for ( int i = 0; i < root.getChildCount(); i++ ) tree.expandPath(
            new TreePath(((DefaultMutableTreeNode)root.getChildAt(i)).getPath()));
        reflectSelection(); updating = false;
    }
    private DefaultMutableTreeNode branch(String key, String fallback) {
        return new DefaultMutableTreeNode(text(key, fallback));
    }
    private void addEntries(DefaultMutableTreeNode parent, Kind kind, ArrayList<?> elements, Scene scene) {
        Map<String, Integer> unnamed = new HashMap<String, Integer>();
        for ( Object element : elements ) {
            String label = labelFor(kind, element, unnamed);
            if ( kind == Kind.BODY && scene.isHidden((SimpleBody)element) )
                label += " (" + text("IDM_SELECTOR_HIDDEN", "hidden") + ")";
            parent.add(new DefaultMutableTreeNode(new Entry(kind, element, label)));
        }
    }
    private String labelFor(Kind kind, Object element, Map<String, Integer> unnamed) {
        String name = switch ( kind ) {
            case BODY -> ((SimpleBody)element).getName(); case LIGHT -> ((Light)element).getName();
            case CAMERA -> ((Camera)element).getName(); case GIZMO -> ((SimpleBodyGroup)element).getName();
        };
        if ( name != null && !name.isBlank() ) return name;
        String type = kind == Kind.BODY ? ((SimpleBody)element).getGeometry().getClass().getSimpleName()
            : element.getClass().getSimpleName();
        return type + " " + unnamed.merge(type, 1, Integer::sum);
    }
    private void reflectSelection() {
        ArrayList<TreePath> paths = new ArrayList<TreePath>();
        collectSelected(paths, (DefaultMutableTreeNode)tree.getModel().getRoot(), model.getScene());
        tree.setSelectionPaths(paths.toArray(new TreePath[0]));
    }
    private void collectSelected(ArrayList<TreePath> paths, DefaultMutableTreeNode node, Scene scene) {
        Object value = node.getUserObject();
        if ( value instanceof Entry entry && isSelected(entry, scene) ) paths.add(new TreePath(node.getPath()));
        for ( int i = 0; i < node.getChildCount(); i++ )
            collectSelected(paths, (DefaultMutableTreeNode)node.getChildAt(i), scene);
    }
    private boolean isSelected(Entry entry, Scene scene) {
        return switch ( entry.kind ) {
            case BODY -> selected(scene.scene.getSimpleBodies(), entry.element, scene.selectedThings);
            case LIGHT -> selected(scene.scene.getLights(), entry.element, scene.selectedLights);
            case GIZMO -> selected(scene.debugThingGroups, entry.element, scene.selectedDebugThingGroups);
            default -> false;
        };
    }
    private boolean selected(ArrayList<?> list, Object element, model.selection.SelectionSet selection) {
        selection.sync(); return selection.isSelected(list.indexOf(element));
    }
    private void applyTreeSelection() {
        if ( updating ) return;
        Scene scene = model.getScene();
        boolean[] beforeB = SceneSelectionOperation.capture(scene.selectedThings);
        boolean[] beforeL = SceneSelectionOperation.capture(scene.selectedLights);
        boolean[] beforeG = SceneSelectionOperation.capture(scene.selectedDebugThingGroups);
        scene.selectedThings.unselectAll(); scene.selectedLights.unselectAll(); scene.selectedDebugThingGroups.unselectAll();
        TreePath[] paths = tree.getSelectionPaths();
        if ( paths != null ) for ( TreePath path : paths ) selectEntry(path, scene);
        boolean[] afterB = SceneSelectionOperation.capture(scene.selectedThings);
        boolean[] afterL = SceneSelectionOperation.capture(scene.selectedLights);
        boolean[] afterG = SceneSelectionOperation.capture(scene.selectedDebugThingGroups);
        if ( !SceneSelectionOperation.isSame(beforeB, afterB) || !SceneSelectionOperation.isSame(beforeL, afterL) || !SceneSelectionOperation.isSame(beforeG, afterG) )
            model.getEditHistory().getSceneHistory().getQueue().record(new SceneSelectionOperation(
                text("IDM_SELECTOR_SELECTION", "Select scene elements"), scene,
                beforeB, beforeL, beforeG, afterB, afterL, afterG));
        repaint.run();
    }
    private void selectEntry(TreePath path, Scene scene) {
        Object value = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
        if ( !(value instanceof Entry entry) ) return;
        switch ( entry.kind ) {
            case BODY -> scene.selectedThings.select(scene.scene.getSimpleBodies().indexOf(entry.element));
            case LIGHT -> scene.selectedLights.select(scene.scene.getLights().indexOf(entry.element));
            case GIZMO -> scene.selectedDebugThingGroups.select(scene.debugThingGroups.indexOf(entry.element));
            default -> { }
        }
    }
    private void deleteSelected() {
        Scene scene = model.getScene();
        if ( scene.selectedThings.numberOfSelections() == 0 && scene.selectedLights.numberOfSelections() == 0 && scene.selectedDebugThingGroups.numberOfSelections() == 0 ) return;
        model.getEditHistory().getSceneHistory().perform(text("IDM_SELECTOR_DELETE", "Delete selected"), () -> new SceneSelectionEditor(scene).deleteSelected());
        rebuild(); repaint.run();
    }
    private void showPopup(MouseEvent event) {
        if ( !event.isPopupTrigger() ) return;
        TreePath path = tree.getPathForLocation(event.getX(), event.getY());
        if ( path != null && !tree.isPathSelected(path) ) tree.setSelectionPath(path);
        ArrayList<SimpleBody> bodies = selectedBodies(); if ( bodies.isEmpty() ) return;
        boolean anyHidden = bodies.stream().anyMatch(model.getScene()::isHidden);
        JMenuItem item = new JMenuItem(text(anyHidden ? "IDM_SELECTOR_SHOW" : "IDM_SELECTOR_HIDE", anyHidden ? "Show objects" : "Hide objects"));
        item.addActionListener(e -> { for ( SimpleBody body : bodies ) model.getScene().setHidden(body, !anyHidden); rebuild(); repaint.run(); });
        JPopupMenu menu = new JPopupMenu(); menu.add(item); menu.show(tree, event.getX(), event.getY());
    }
    private ArrayList<SimpleBody> selectedBodies() {
        ArrayList<SimpleBody> result = new ArrayList<SimpleBody>(); TreePath[] paths = tree.getSelectionPaths();
        if ( paths != null ) for ( TreePath path : paths ) {
            Object value = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
            if ( value instanceof Entry entry && entry.kind == Kind.BODY ) result.add((SimpleBody)entry.element);
        }
        return result;
    }
    private String text(String key, String fallback) {
        String value = messages == null ? null : messages.getMessage(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
