package model.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.ApplicationModel;
import model.Scene;
import model.history.SceneSelectionOperation;
import model.selection.SceneSelectionEditor;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.gui.dialog.components.DialogTreeNode;
import vsdk.toolkit.gui.widget.Widget;

/** Editor-specific hierarchy, selection and history behavior; no Swing types. */
public final class SceneHierarchyPresenter {
    private enum Kind { BODY, LIGHT, CAMERA, GIZMO }
    private record Entry(Kind kind, Object element) { }
    private final ApplicationModel model;
    private final Widget messages;
    private final Runnable repaint;

    public SceneHierarchyPresenter(ApplicationModel model, Widget messages, Runnable repaint) {
        this.model = model; this.messages = messages; this.repaint = repaint;
    }
    public String text(String key, String fallback) {
        String value = messages == null ? null : messages.getMessage(key);
        return value == null || value.isBlank() ? fallback : value;
    }
    public DialogTreeNode hierarchy() {
        Scene scene = model.getScene();
        DialogTreeNode root = new DialogTreeNode("Scene", null);
        DialogTreeNode lights = new DialogTreeNode(text("IDM_SELECTOR_LIGHTS", "Lights"), null);
        DialogTreeNode cameras = new DialogTreeNode(text("IDM_SELECTOR_CAMERAS", "Cameras"), null);
        DialogTreeNode bodies = new DialogTreeNode(text("IDM_SELECTOR_OBJECTS", "Geometry objects"), null);
        root.add(lights); root.add(cameras); root.add(bodies);
        addEntries(lights, Kind.LIGHT, scene.scene.getLights(), scene);
        addEntries(cameras, Kind.CAMERA, scene.scene.getCameras(), scene);
        addEntries(bodies, Kind.BODY, scene.scene.getSimpleBodies(), scene);
        return root;
    }
    private void addEntries(DialogTreeNode parent, Kind kind, ArrayList<?> elements, Scene scene) {
        Map<String, Integer> unnamed = new HashMap<String, Integer>();
        for (Object element : elements) {
            String label = labelFor(kind, element, unnamed);
            if (kind == Kind.BODY && scene.isHidden((SimpleBody)element))
                label += " (" + text("IDM_SELECTOR_HIDDEN", "hidden") + ")";
            parent.add(new DialogTreeNode(label, new Entry(kind, element)));
        }
    }
    private String labelFor(Kind kind, Object element, Map<String, Integer> unnamed) {
        String name = switch (kind) {
            case BODY -> ((SimpleBody)element).getName();
            case LIGHT -> ((Light)element).getName();
            case CAMERA -> ((Camera)element).getName();
            case GIZMO -> ((SimpleBodyGroup)element).getName();
        };
        if (name != null && !name.isBlank()) return name;
        String type = kind == Kind.BODY ? ((SimpleBody)element).getGeometry().getClass().getSimpleName()
            : element.getClass().getSimpleName();
        return type + " " + unnamed.merge(type, 1, Integer::sum);
    }
    public boolean isSelected(DialogTreeNode node) {
        if (!(node.getIdentity() instanceof Entry entry)) return false;
        Scene scene = model.getScene();
        return switch (entry.kind) {
            case BODY -> selected(scene.scene.getSimpleBodies(), entry.element, scene.selectedThings);
            case LIGHT -> selected(scene.scene.getLights(), entry.element, scene.selectedLights);
            case GIZMO -> selected(scene.debugThingGroups, entry.element, scene.selectedDebugThingGroups);
            default -> false;
        };
    }
    private boolean selected(ArrayList<?> list, Object element, model.selection.SelectionSet selection) {
        selection.sync(); return selection.isSelected(list.indexOf(element));
    }
    public void select(List<DialogTreeNode> nodes) {
        Scene scene = model.getScene();
        boolean[] beforeB = SceneSelectionOperation.capture(scene.selectedThings);
        boolean[] beforeL = SceneSelectionOperation.capture(scene.selectedLights);
        boolean[] beforeG = SceneSelectionOperation.capture(scene.selectedDebugThingGroups);
        scene.selectedThings.unselectAll(); scene.selectedLights.unselectAll(); scene.selectedDebugThingGroups.unselectAll();
        for (DialogTreeNode node : nodes) selectEntry(node, scene);
        boolean[] afterB = SceneSelectionOperation.capture(scene.selectedThings);
        boolean[] afterL = SceneSelectionOperation.capture(scene.selectedLights);
        boolean[] afterG = SceneSelectionOperation.capture(scene.selectedDebugThingGroups);
        if (!SceneSelectionOperation.isSame(beforeB, afterB) || !SceneSelectionOperation.isSame(beforeL, afterL)
            || !SceneSelectionOperation.isSame(beforeG, afterG))
            model.getEditHistory().getSceneHistory().getQueue().record(new SceneSelectionOperation(
                text("IDM_SELECTOR_SELECTION", "Select scene elements"), scene,
                beforeB, beforeL, beforeG, afterB, afterL, afterG));
        repaint.run();
    }
    private void selectEntry(DialogTreeNode node, Scene scene) {
        if (!(node.getIdentity() instanceof Entry entry)) return;
        switch (entry.kind) {
            case BODY -> scene.selectedThings.select(scene.scene.getSimpleBodies().indexOf(entry.element));
            case LIGHT -> scene.selectedLights.select(scene.scene.getLights().indexOf(entry.element));
            case GIZMO -> scene.selectedDebugThingGroups.select(scene.debugThingGroups.indexOf(entry.element));
            default -> { }
        }
    }
    public void deleteSelected() {
        Scene scene = model.getScene();
        if (scene.selectedThings.numberOfSelections() == 0 && scene.selectedLights.numberOfSelections() == 0
            && scene.selectedDebugThingGroups.numberOfSelections() == 0) return;
        model.getEditHistory().getSceneHistory().perform(text("IDM_SELECTOR_DELETE", "Delete selected"),
            () -> new SceneSelectionEditor(scene).deleteSelected());
        repaint.run();
    }
    public ArrayList<SimpleBody> selectedBodies(List<DialogTreeNode> nodes) {
        ArrayList<SimpleBody> result = new ArrayList<SimpleBody>();
        for (DialogTreeNode node : nodes)
            if (node.getIdentity() instanceof Entry entry && entry.kind == Kind.BODY)
                result.add((SimpleBody)entry.element);
        return result;
    }
    public boolean anyHidden(List<SimpleBody> bodies) {
        return bodies.stream().anyMatch(model.getScene()::isHidden);
    }
    public void setHidden(List<SimpleBody> bodies, boolean hidden) {
        for (SimpleBody body : bodies) model.getScene().setHidden(body, hidden);
        repaint.run();
    }
}
