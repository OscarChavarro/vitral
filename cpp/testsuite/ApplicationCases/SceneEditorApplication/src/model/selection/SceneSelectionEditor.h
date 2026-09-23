#ifndef __SCENE_SELECTION_EDITOR__
#define __SCENE_SELECTION_EDITOR__

#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Scene;
class SimpleBody;

/**
Editing operations over the selected things (bodies, lights and debug
groups) of a scene. It does not depend on any GUI or rendering technology.
*/
class SceneSelectionEditor {
private:
    Scene* scene;

public:
    explicit SceneSelectionEditor(Scene* scene);

    /**
    Calculates the centroid of the group of selected things (bodies and
    lights), as the mean of their positions. With a single selected thing
    this is its position.
    @param outCentroid the centroid, when something is selected
    @return false if no thing is selected (the Java version returns null)
    */
    bool computeSelectionCentroid(Vector3Dd* outCentroid);

    /**
    @param position where the translation gizmo is to be placed
    @return a pure translation matrix for the translation gizmo
    */
    static Matrix4x4d createTranslationGizmoMatrix(const Vector3Dd& position);

    /**
    @param body body the rotation gizmo is to be placed at
    @return a matrix with the orientation and position of the body, for the
    rotation gizmo
    */
    static Matrix4x4d createRotationGizmoMatrix(const SimpleBody* body);

    /**
    Moves rigidly the group of selected things (bodies and lights), so that
    the group centroid goes from oldCentroid to newCentroid. Relative
    positions are preserved.
    */
    void applyTranslationToSelectedObjects(const Vector3Dd& oldCentroid,
                                           const Vector3Dd& newCentroid);

    /**
    @return the first selected body, or null if no body is selected
    */
    SimpleBody* getFirstSelectedBody();

    /**
    Selects the previous body of the scene or, if there are no bodies to
    select, the previous debug group. Lights are unselected.
    */
    void selectPrevious();

    /**
    Selects the next body of the scene or, if there are no bodies to select,
    the next debug group. Lights are unselected.
    */
    void selectNext();

    /**
    Removes from the scene the selected bodies, lights and debug groups. The
    removed bodies (with their geometries) and lights are disposed, so their
    subscribers (i.e. editors) learn that they were deleted (C++ port note:
    they are not deleted, as the edit history can put them back).
    */
    void deleteSelected();

    /**
    @return a one line description of the current selection, for the user
    */
    java::String describeSelection();
};

#endif
