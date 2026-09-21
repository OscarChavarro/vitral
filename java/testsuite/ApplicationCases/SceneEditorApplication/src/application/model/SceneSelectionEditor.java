package application.model;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.scene.SimpleBody;

import application.framework.Scene;

/**
Editing operations over the selected things (bodies, lights and debug groups)
of a scene. It does not depend on any GUI or rendering technology.
*/
public class SceneSelectionEditor
{
    private final Scene scene;

    public SceneSelectionEditor(Scene scene)
    {
        this.scene = scene;
    }

    /**
    Calculates the centroid of the group of selected things (bodies and
    lights), as the mean of their positions. With a single selected thing this
    is its position.
    @return the centroid, or null if no thing is selected
    */
    public Vector3Dd computeSelectionCentroid()
    {
        Vector3Dd sum = new Vector3Dd();
        int count = 0;
        int i;

        scene.selectedThings.sync();
        for ( i = 0; i < scene.selectedThings.size(); i++ ) {
            if ( !scene.selectedThings.isSelected(i) ) continue;
            sum = sum.add(scene.scene.getSimpleBodies().get(i).getPosition());
            count++;
        }
        scene.selectedLights.sync();
        for ( i = 0; i < scene.selectedLights.size(); i++ ) {
            if ( !scene.selectedLights.isSelected(i) ) continue;
            sum = sum.add(scene.scene.getLights().get(i).getPosition());
            count++;
        }
        if ( count == 0 ) {
            return null;
        }
        return sum.multiply(1.0 / count);
    }

    /**
    @param position where the translation gizmo is to be placed
    @return a pure translation matrix for the translation gizmo
    */
    public static Matrix4x4d createTranslationGizmoMatrix(Vector3Dd position)
    {
        Matrix4x4d composed = new Matrix4x4d();

        composed = composed.withVal(0, 3, position.x());
        composed = composed.withVal(1, 3, position.y());
        composed = composed.withVal(2, 3, position.z());
        return composed;
    }

    /**
    @param body body the rotation gizmo is to be placed at
    @return a matrix with the orientation and position of the body, for the
    rotation gizmo
    */
    public static Matrix4x4d createRotationGizmoMatrix(SimpleBody body)
    {
        return new Matrix4x4d(body.getRotation()).withTranslation(body.getPosition());
    }

    /**
    Moves rigidly the group of selected things (bodies and lights), so that the group centroid
    goes from oldCentroid to newCentroid. Relative positions are preserved.
    @param oldCentroid centroid of the group before the movement
    @param newCentroid centroid of the group after the movement
    */
    public void applyTranslationToSelectedObjects(Vector3Dd oldCentroid,
                                                  Vector3Dd newCentroid)
    {
        SimpleBody gi;
        Light light;
        Vector3Dd delta = newCentroid.subtract(oldCentroid);
        int i;

        for ( i = 0; i < scene.selectedThings.size(); i++ ) {
            if ( !scene.selectedThings.isSelected(i) ) continue;
            gi = scene.scene.getSimpleBodies().get(i);

            gi.setPosition(gi.getPosition().add(delta));
        }
        for ( i = 0; i < scene.selectedLights.size(); i++ ) {
            if ( !scene.selectedLights.isSelected(i) ) continue;
            light = scene.scene.getLights().get(i);

            light.setPosition(light.getPosition().add(delta));
        }
    }

    /**
    @return the first selected body, or null if no body is selected
    */
    public SimpleBody getFirstSelectedBody()
    {
        int firstThingSelected = scene.selectedThings.firstSelected();

        if ( firstThingSelected < 0 ) {
            return null;
        }
        return scene.scene.getSimpleBodies().get(firstThingSelected);
    }

    /**
    Selects the previous body of the scene or, if there are no bodies to
    select, the previous debug group. Lights are unselected.
    */
    public void selectPrevious()
    {
        scene.selectedLights.unselectAll();
        if ( scene.selectedDebugThingGroups.numberOfSelections() < 1 ) {
            scene.selectedThings.selectPrevious();
        }
        if ( scene.selectedThings.numberOfSelections() < 1 ) {
            scene.selectedDebugThingGroups.selectPrevious();
        }
    }

    /**
    Selects the next body of the scene or, if there are no bodies to select,
    the next debug group. Lights are unselected.
    */
    public void selectNext()
    {
        scene.selectedLights.unselectAll();
        if ( scene.selectedDebugThingGroups.numberOfSelections() < 1 ) {
            scene.selectedThings.selectNext();
        }
        if ( scene.selectedThings.numberOfSelections() < 1 ) {
            scene.selectedDebugThingGroups.selectNext();
        }
    }

    /**
    Removes from the scene the selected bodies, lights and debug groups.
    */
    public void deleteSelected()
    {
        int i;

        scene.selectedThings.removeSelected();
        scene.selectedLights.removeSelected();
        for ( i = scene.debugThingGroups.size()-1; i >= 0; i-- ) {
            if ( scene.selectedDebugThingGroups.isSelected(i) ) {
                scene.debugThingGroups.remove(i);
            }
        }
        scene.selectedThings.sync();
    }

    /**
    @return a one line description of the current selection, for the user
    */
    public String describeSelection()
    {
        String msg = "";
        int n;

        //-----------------------------------------------------------------
        scene.selectedThings.sync();
        n = scene.selectedThings.numberOfSelections();
        if ( n == 0 ) {
            msg += "All things are UNSELECTED";
        }
        else if ( n == 1 ) {
            int f = scene.selectedThings.firstSelected();
            msg = "Thing [" + f + "] selected, which is a [" +
                ((SimpleBody)(scene.scene.getSimpleBodies().get(f))).getGeometry().getClass().getName()
                + "]";
        }
        else {
            msg += "" + n + " things selected";
        }

        //-----------------------------------------------------------------
        scene.selectedLights.sync();
        n = scene.selectedLights.numberOfSelections();
        if ( n == 1 ) {
            msg += "; Light [" + scene.selectedLights.firstSelected() + "] selected";
        }
        else if ( n > 1 ) {
            msg += "; " + n + " lights selected";
        }

        //-----------------------------------------------------------------
        scene.selectedDebugThingGroups.sync();
        n = scene.selectedDebugThingGroups.numberOfSelections();
        if ( n == 0 ) {
            msg += "; All visual debug groups are UNSELECTED";
        }
        else if ( n == 1 ) {
            int f = scene.selectedDebugThingGroups.firstSelected();
            msg += "; Debug group [" + f + "] selected.";
        }
        else {
            msg += "; " + n + " debug groups selected";
        }
        return msg;
    }
}
