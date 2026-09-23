#ifndef __SCENE_PICKER__
#define __SCENE_PICKER__

class Ray;
class Scene;

/**
Selects the things of a scene (bodies and lights) under a pixel of the
active camera viewport. Bodies are picked with their geometry and lights
with a sphere of the size of their gizmo (see `LightPicker`). It does not
depend on any GUI or rendering technology.
*/
class ScenePicker {
private:
    Scene* scene;

public:
    /**
    @param scene scene whose things are picked and whose selection is updated
    */
    explicit ScenePicker(Scene* scene);

    /**
    Selects the nearest thing (body or light) under a pixel of the active
    camera viewport. Without `composite` the previous selection is
    discarded, with it the picked thing changes its selection state.
    @param x pixel column in the viewport
    @param y pixel row in the viewport
    @param composite true to modify the current selection
    @return the ray fired through the pixel
    */
    Ray selectObjectWithMouse(int x, int y, bool composite);
};

#endif
