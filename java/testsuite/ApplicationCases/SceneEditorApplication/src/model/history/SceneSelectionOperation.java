package model.history;

import java.util.ArrayList;

import model.Scene;
import model.selection.SelectionSet;

/** Undoable selection change made by the scene tree. */
public final class SceneSelectionOperation implements UndoableOperation
{
    private final Scene scene;
    private final String name;
    private final boolean[] beforeBodies;
    private final boolean[] beforeLights;
    private final boolean[] beforeGizmos;
    private final boolean[] afterBodies;
    private final boolean[] afterLights;
    private final boolean[] afterGizmos;

    public SceneSelectionOperation(String name, Scene scene, boolean[] beforeBodies,
        boolean[] beforeLights, boolean[] beforeGizmos, boolean[] afterBodies,
        boolean[] afterLights, boolean[] afterGizmos)
    {
        this.name = name;
        this.scene = scene;
        this.beforeBodies = beforeBodies;
        this.beforeLights = beforeLights;
        this.beforeGizmos = beforeGizmos;
        this.afterBodies = afterBodies;
        this.afterLights = afterLights;
        this.afterGizmos = afterGizmos;
    }

    @Override public void undo() { apply(beforeBodies, beforeLights, beforeGizmos); }
    @Override public void redo() { apply(afterBodies, afterLights, afterGizmos); }
    @Override public String getName() { return name; }

    private void apply(boolean[] bodies, boolean[] lights, boolean[] gizmos)
    {
        apply(scene.selectedThings, bodies);
        apply(scene.selectedLights, lights);
        apply(scene.selectedDebugThingGroups, gizmos);
    }

    private static void apply(SelectionSet selection, boolean[] state)
    {
        selection.sync();
        selection.unselectAll();
        for ( int i = 0; i < state.length && i < selection.size(); i++ ) {
            if ( state[i] ) selection.select(i);
        }
    }

    public static boolean[] capture(SelectionSet selection)
    {
        selection.sync();
        boolean[] result = new boolean[selection.size()];
        for ( int i = 0; i < result.length; i++ ) result[i] = selection.isSelected(i);
        return result;
    }

    public static boolean isSame(boolean[] a, boolean[] b)
    {
        if ( a.length != b.length ) return false;
        for ( int i = 0; i < a.length; i++ ) if ( a[i] != b[i] ) return false;
        return true;
    }
}
