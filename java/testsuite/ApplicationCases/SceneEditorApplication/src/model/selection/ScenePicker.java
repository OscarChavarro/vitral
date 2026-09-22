package model.selection;

import java.util.List;

import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.scene.SimpleBody;
import model.Scene;

/**
Selects the things of a scene (bodies and lights) under a pixel of the active
camera viewport. Bodies are picked with their geometry and lights with a
sphere of the size of their gizmo (see `LightPicker`). It does not depend on
any GUI or rendering technology.
*/
public class ScenePicker
{
    private final Scene scene;

    /**
    @param scene scene whose things are picked and whose selection is updated
    */
    public ScenePicker(Scene scene)
    {
        this.scene = scene;
    }

    /**
    Selects the nearest thing (body or light) under a pixel of the active
    camera viewport. Without `composite` the previous selection is discarded,
    with it the picked thing changes its selection state.
    @param x pixel column in the viewport
    @param y pixel row in the viewport
    @param composite true to modify the current selection
    @return the ray fired through the pixel
    */
    public Ray selectObjectWithMouse(int x, int y, boolean composite)
    {
        Camera camera = scene.activeCamera;
        Ray r;
        SimpleBody gi;

        camera.updateVectors();
        r = camera.generateRay(x, y);

        Ray selectedRay = Ray.copyOf(r);

        double nearestDistance = Float.MAX_VALUE;
        int nearestBody = -1;
        int nearestLight = -1;

        int i;

        SelectionSet selectedThings = scene.selectedThings;
        SelectionSet selectedLights = scene.selectedLights;
        selectedThings.sync();
        selectedLights.sync();

        List<SimpleBody> things = scene.scene.getSimpleBodies();
        for ( i = 0; i < things.size(); i++ ) {
            gi = things.get(i);
            Ray hit = gi.doIntersectionFirstHit(r);
            if ( hit != null && hit.getT() < nearestDistance ) {
                nearestDistance = hit.getT();
                nearestBody = i;
            }
        }

        List<Light> lights = scene.scene.getLights();
        for ( i = 0; i < lights.size(); i++ ) {
            double t = LightPicker.pick(r, camera, lights.get(i),
                scene.getLightGizmoScale());
            if ( t >= 0 && t < nearestDistance ) {
                nearestDistance = t;
                nearestBody = -1;
                nearestLight = i;
            }
        }

        if ( !composite ) {
            selectedThings.unselectAll();
            selectedLights.unselectAll();
            selectedThings.select(nearestBody);
            selectedLights.select(nearestLight);
        }
        else {
            selectedThings.change(nearestBody);
            selectedLights.change(nearestLight);
        }
        return selectedRay;
    }
}
