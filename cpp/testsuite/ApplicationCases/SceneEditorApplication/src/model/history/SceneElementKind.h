#ifndef __SCENE_ELEMENT_KIND__
#define __SCENE_ELEMENT_KIND__

#include "model/selection/EntityListView.h"

class Entity;
class Scene;
class SelectionSet;

/**
Kinds of elements of a scene whose creation and deletion are undoable: each
kind is kept in its own list of the scene, most of them with a selection set
over it.

C++ port note: the Java enum with methods is a class with an enumeration and
static methods taking the kind.
*/
class SceneElementKind {
public:
    enum Value {
        BODY,
        LIGHT,
        CAMERA,
        DEBUG_GROUP
    };
    static const int COUNT = 4;

    /**
    @param kind kind of the elements
    @param scene scene holding the elements
    @return a view of the list of the scene holding the elements of the kind
    */
    static EntityListView getList(Value kind, Scene* scene);

    /**
    @return the selection set over the elements of this kind, or null if
    this kind can not be selected (scene cameras)
    */
    static SelectionSet* getSelection(Value kind, Scene* scene);

    /**
    Inserts an element in its list, keeping the selection aligned.
    @param kind kind of the elements
    @param scene scene holding the elements
    @param index position of the element in the list
    @param element element to insert
    @param selected true if the element must be selected
    */
    static void insert(Value kind, Scene* scene, int index, Entity* element,
                       bool selected);

    /**
    Removes an element from its list (without deleting it), keeping the
    selection aligned.
    */
    static void remove(Value kind, Scene* scene, int index);

private:
    SceneElementKind() {}
};

#endif
