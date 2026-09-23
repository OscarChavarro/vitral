package model.history;

import java.util.ArrayList;

import vsdk.toolkit.common.Entity;

import model.Scene;
import model.selection.SelectionSet;

/**
Kinds of elements of a scene whose creation and deletion are undoable: each
kind is kept in its own list of the scene, most of them with a selection set
over it.
*/
public enum SceneElementKind
{
    BODY,
    LIGHT,
    CAMERA,
    DEBUG_GROUP;

    /**
    @param scene scene holding the elements
    @return the list of the scene holding the elements of this kind
    */
    public ArrayList<? extends Entity> getList(Scene scene)
    {
        switch ( this ) {
          case BODY:
            return scene.scene.getSimpleBodies();
          case LIGHT:
            return scene.scene.getLights();
          case CAMERA:
            return scene.scene.getCameras();
          default:
            return scene.debugThingGroups;
        }
    }

    /**
    @param scene scene holding the elements
    @return the selection set over the elements of this kind, or null if this
    kind can not be selected (scene cameras)
    */
    public SelectionSet getSelection(Scene scene)
    {
        switch ( this ) {
          case BODY:
            return scene.selectedThings;
          case LIGHT:
            return scene.selectedLights;
          case DEBUG_GROUP:
            return scene.selectedDebugThingGroups;
          default:
            return null;
        }
    }

    /**
    Inserts an element in its list, keeping the selection aligned.
    @param scene scene holding the elements
    @param index position of the element in the list
    @param element element to insert
    @param selected true if the element must be selected
    */
    @SuppressWarnings("unchecked")
    public void insert(Scene scene, int index, Entity element, boolean selected)
    {
        SelectionSet selection = getSelection(scene);

        if ( selection != null ) {
            selection.insertElement(index, element, selected);
        }
        else {
            ((ArrayList<Entity>)getList(scene)).add(index, element);
        }
    }

    /**
    Removes an element from its list, keeping the selection aligned.
    @param scene scene holding the elements
    @param index position of the element in the list
    */
    public void remove(Scene scene, int index)
    {
        SelectionSet selection = getSelection(scene);

        if ( selection != null ) {
            selection.removeElement(index);
        }
        else {
            getList(scene).remove(index);
        }
    }
}
