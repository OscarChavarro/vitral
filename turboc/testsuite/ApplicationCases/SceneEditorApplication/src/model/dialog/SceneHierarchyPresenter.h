#ifndef __SCENE_HIERARCHY_PRESENTER__
#define __SCENE_HIERARCHY_PRESENTER__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Runnable.h"
#include "java/util/function/Consumer.h"
#include "java/util/function/Function.h"
#include "java/util/function/Supplier.h"
#include "java/util/ArrayList.h"

#include "java/lang/String.h"
#include "vsdk/toolkit/gui/dialog/components/DialogTreeNode.h"

class ApplicationModel;
class Entity;
class SimpleBody;

/**
Editor-specific hierarchy, selection and history behavior of the scene
selector dialog (`scene.selector` of `etc/gui/dialogs.json`); no GUI types,
as `SceneHierarchyPresenter` of the Java application. Each GUI technology
shows the tree it builds and reports to it the rows the user selects and
the actions of the dialog.

The tree has a root (not shown) with the lights, the cameras and the
geometry objects of the scene. Selecting rows selects their elements in
the scene, recording the change in the scene history.

C++ port note: the messages come from the I18N context of the model (the
`Widget` the Java application gives), so this class can be used along with
Xt (whose `Widget` type collides with the vitral class). The rows of a
hierarchy identify their elements through this presenter: they are valid
until the next call to `hierarchy`.
*/
class SceneHierarchyPresenter {
private:
    enum Kind { BODY, LIGHT, CAMERA, GIZMO };

    struct Entry {
        Kind kind;
        Entity* element;
    };

    ApplicationModel* model;
    Runnable* repaint;
    /// Identities of the rows of the last hierarchy: each one is allocated
    /// on its own, so their addresses are stable.
    ArrayList<Entry*> entries;

    SceneHierarchyPresenter(const SceneHierarchyPresenter& other);
    SceneHierarchyPresenter& operator=(const SceneHierarchyPresenter& other);

    void clearEntries();
    const Entry* entryOf(const DialogTreeNode* node) const;
    void addEntries(DialogTreeNode& parent, Kind kind,
                    const ArrayList<Entity*>& elements);

public:
    /**
    @param model application model (its scene, history and messages)
    @param repaint asks the application to repaint its views (not owned,
    null for none)
    */
    SceneHierarchyPresenter(ApplicationModel* model, Runnable* repaint);
    ~SceneHierarchyPresenter();

    /**
    @return the message of the key, or the fallback if there is none
    */
    String text(const char* key, const char* fallback) const;

    /**
    @return the tree of the scene, invalidating the rows of the previous one
    */
    DialogTreeNode hierarchy();

    /**
    @return true if the element of the row is selected in the scene
    */
    BOOLEAN isSelected(const DialogTreeNode* node) const;

    /**
    Selects in the scene the elements of the rows (and only them), recording
    the change in the scene history, and repaints.
    */
    void select(const ArrayList<const DialogTreeNode*>& nodes);

    /**
    Deletes the selected elements of the scene (undoable) and repaints.
    */
    void deleteSelected();

    /**
    @return the bodies of the rows
    */
    ArrayList<SimpleBody*> selectedBodies(
        const ArrayList<const DialogTreeNode*>& nodes) const;
    BOOLEAN anyHidden(const ArrayList<SimpleBody*>& bodies) const;

    /**
    Hides or shows the bodies, and repaints.
    */
    void setHidden(const ArrayList<SimpleBody*>& bodies, BOOLEAN hidden);
};

#endif
