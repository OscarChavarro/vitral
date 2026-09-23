package model.history;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.scene.SimpleBody;

import model.Scene;
import model.selection.SelectionSet;

/**
What a user action can change of a scene, captured before and after the
action to find out the operations it did: which elements each list of the
scene holds (bodies, lights, cameras and debug groups, with their selection
state) and the placement of each body, light and camera. Only references and
immutable values are kept, so a capture is cheap.

Elements (vitral `Entity`s) are compared by identity: the scene lists may
hold entities that are `equals` without being the same one.
*/
public class SceneSnapshot
{
    private final Scene scene;
    private final EnumMap<SceneElementKind, ArrayList<Entity>> elements;
    private final EnumMap<SceneElementKind, boolean[]> selections;
    private final IdentityHashMap<Entity, EntityTransformState> transforms;

    private SceneSnapshot(Scene scene)
    {
        this.scene = scene;
        this.elements = new EnumMap<SceneElementKind, ArrayList<Entity>>(SceneElementKind.class);
        this.selections = new EnumMap<SceneElementKind, boolean[]>(SceneElementKind.class);
        this.transforms = new IdentityHashMap<Entity, EntityTransformState>();
    }

    /**
    @param scene scene to capture
    @return the current state of the scene
    */
    public static SceneSnapshot capture(Scene scene)
    {
        SceneSnapshot snapshot = new SceneSnapshot(scene);

        for ( SceneElementKind kind : SceneElementKind.values() ) {
            ArrayList<Entity> list = new ArrayList<Entity>(kind.getList(scene));
            SelectionSet selection = kind.getSelection(scene);
            boolean[] selected = new boolean[list.size()];
            int i;

            if ( selection != null ) {
                selection.sync();
                for ( i = 0; i < selected.length; i++ ) {
                    selected[i] = selection.isSelected(i);
                }
            }
            snapshot.elements.put(kind, list);
            snapshot.selections.put(kind, selected);
        }
        for ( SimpleBody body : scene.scene.getSimpleBodies() ) {
            snapshot.transforms.put(body, BodyTransformState.capture(body));
        }
        for ( Light light : scene.scene.getLights() ) {
            snapshot.transforms.put(light, LightTransformState.capture(light));
        }
        for ( Camera camera : scene.scene.getCameras() ) {
            snapshot.transforms.put(camera, CameraState.capture(camera));
        }
        return snapshot;
    }

    /**
    @return the captured scene
    */
    public Scene getScene()
    {
        return scene;
    }

    /**
    Finds out the operations that go from this state of the scene to a later
    one.
    @param later state of the same scene captured after the user action
    @param name name of the user action
    @param mergeable true if the placement changes of the action can be merged
    with the ones of a following action over the same elements
    @return the operation done by the action, or null if nothing changed
    */
    public UndoableOperation operationTo(SceneSnapshot later, String name,
                                         boolean mergeable)
    {
        ArrayList<UndoableOperation> operations = new ArrayList<UndoableOperation>();
        UndoableOperation transformation;

        if ( later == null || later.scene != scene ) {
            return null;
        }
        transformation = transformationTo(later, name, mergeable);
        if ( transformation != null ) {
            operations.add(transformation);
        }
        for ( SceneElementKind kind : SceneElementKind.values() ) {
            UndoableOperation membership = membershipTo(later, kind, name);

            if ( membership != null ) {
                operations.add(membership);
            }
        }
        if ( operations.isEmpty() ) {
            return null;
        }
        if ( operations.size() == 1 ) {
            return operations.get(0);
        }
        return new CompositeOperation(name, operations);
    }

    /**
    @return the placement changes of the elements present in both states, or
    null if there are none
    */
    private UndoableOperation transformationTo(SceneSnapshot later, String name,
                                               boolean mergeable)
    {
        ArrayList<EntityTransformState> before = new ArrayList<EntityTransformState>();
        ArrayList<EntityTransformState> after = new ArrayList<EntityTransformState>();

        for ( SceneElementKind kind : SceneElementKind.values() ) {
            for ( Entity element : elements.get(kind) ) {
                EntityTransformState oldState = transforms.get(element);
                EntityTransformState newState = later.transforms.get(element);

                if ( oldState != null && newState != null &&
                     !oldState.isSameState(newState) ) {
                    before.add(oldState);
                    after.add(newState);
                }
            }
        }
        if ( before.isEmpty() ) {
            return null;
        }
        return new SceneTransformationOperation(name, before, after, mergeable);
    }

    /**
    @return the creation and deletion of elements of a kind, or null if the
    same elements are present in both states
    */
    private UndoableOperation membershipTo(SceneSnapshot later,
                                           SceneElementKind kind, String name)
    {
        List<SceneMembershipOperation.Entry> removed =
            entriesMissingIn(later, kind);
        List<SceneMembershipOperation.Entry> inserted =
            later.entriesMissingIn(this, kind);

        if ( removed.isEmpty() && inserted.isEmpty() ) {
            return null;
        }
        return new SceneMembershipOperation(name, scene, kind, removed, inserted);
    }

    /**
    @return the elements of a kind in this state that are not in the other
    one, sorted by position
    */
    private List<SceneMembershipOperation.Entry> entriesMissingIn(
        SceneSnapshot other, SceneElementKind kind)
    {
        ArrayList<Entity> list = elements.get(kind);
        boolean[] selected = selections.get(kind);
        Map<Entity, Boolean> otherElements = new IdentityHashMap<Entity, Boolean>();
        ArrayList<SceneMembershipOperation.Entry> missing =
            new ArrayList<SceneMembershipOperation.Entry>();
        int i;

        for ( Entity element : other.elements.get(kind) ) {
            otherElements.put(element, Boolean.TRUE);
        }
        for ( i = 0; i < list.size(); i++ ) {
            if ( !otherElements.containsKey(list.get(i)) ) {
                missing.add(new SceneMembershipOperation.Entry(i, list.get(i), selected[i]));
            }
        }
        return missing;
    }
}
