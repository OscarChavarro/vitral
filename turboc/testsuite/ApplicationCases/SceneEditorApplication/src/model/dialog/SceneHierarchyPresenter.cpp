#include <string.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/TreeMap.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/Cpp98Support.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "model/ApplicationModel.h"
#include "model/Scene.h"
#include "model/dialog/SceneHierarchyPresenter.h"
#include "model/history/EditHistory.h"
#include "model/history/SceneHistory.h"
#include "model/history/SceneSelectionOperation.h"
#include "model/history/UndoQueue.h"
#include "model/selection/SceneSelectionEditor.h"
#include "model/selection/SelectionSet.h"


template <class T>
static ArrayList<Entity*> entities(ArrayList<T*>& list)
{
    ArrayList<Entity*> result;
    for ( long i = 0; i < list.size(); i++ ) {
        result.add(list.get(i));
    }
    return result;
}

template <class T>
static int indexOf(ArrayList<T*>& list, const Entity* element)
{
    for ( long i = 0; i < list.size(); i++ ) {
        if ( list.get(i) == element ) {
            return ((int)(i));
        }
    }
    return -1;
}

/**
Deletes the selected elements of a scene.
*/
class DeleteSelectedAction : public Runnable {
public:
    explicit DeleteSelectedAction(Scene* scene) : scene(scene) {}
    virtual void run() { SceneSelectionEditor(scene).deleteSelected(); }
private:
    Scene* scene;
};

static BOOLEAN isBlank(const String& text)
{
    for ( const char* c = text.c_str(); *c != '\0'; c++ ) {
        if ( strchr(" \t\r\n", *c) == NULL ) {
            return FALSE;
        }
    }
    return TRUE;
}


SceneHierarchyPresenter::SceneHierarchyPresenter(
    ApplicationModel* model, Runnable* repaint)
    : model(model), repaint(repaint)
{
}

SceneHierarchyPresenter::~SceneHierarchyPresenter()
{
    clearEntries();
}

void SceneHierarchyPresenter::clearEntries()
{
    for ( long i = 0; i < entries.size(); i++ ) {
        delete entries[i];
    }
    entries.clear();
}

String SceneHierarchyPresenter::text(const char* key,
                                           const char* fallback) const
{
    Widget* messages = model->getI18nContext();
    String value = messages != NULL ?
        messages->getMessage(key) : String("");
    // A missing message is its own key in the C++ context (null in Java)
    return isBlank(value) || value.equals(key) ? String(fallback) : value;
}

DialogTreeNode SceneHierarchyPresenter::hierarchy()
{
    Scene* scene = model->getScene();
    clearEntries();
    DialogTreeNode root("Scene", NULL);
    addEntries(root.add(DialogTreeNode(text("IDM_SELECTOR_LIGHTS", "Lights"),
                                       NULL)),
               LIGHT, entities(scene->scene->getLights()));
    addEntries(root.add(DialogTreeNode(text("IDM_SELECTOR_CAMERAS", "Cameras"),
                                       NULL)),
               CAMERA, entities(scene->scene->getCameras()));
    addEntries(root.add(DialogTreeNode(
                   text("IDM_SELECTOR_OBJECTS", "Geometry objects"), NULL)),
               BODY, entities(scene->scene->getSimpleBodies()));
    return root;
}

void SceneHierarchyPresenter::addEntries(DialogTreeNode& parent, Kind kind,
                                         const ArrayList<Entity*>& elements)
{
    Scene* scene = model->getScene();
    TreeMap<String, int> unnamed;
    for ( long i = 0; i < elements.size(); i++ ) {
        Entity* element = elements[i];
        String name;
        switch ( kind ) {
          case BODY: name = ((SimpleBody*)(element))->getName(); break;
          case LIGHT: name = ((Light*)(element))->getName(); break;
          case CAMERA: name = ((Camera*)(element))->getName(); break;
          case GIZMO: name = ((SimpleBodyGroup*)(element))->getName(); break;
        }
        String label = name;
        if ( isBlank(name) ) {
            Geometry* geometry = kind == BODY ?
                ((SimpleBody*)(element))->getGeometry() : NULL;
            String type = geometry != NULL ?
                geometry->getClassSimpleName().c_str() :
                element->getClassSimpleName().c_str();
            int* count = &unnamed.putIfAbsent(type, 0);
            label = (type + " " + Cpp98Support::toString(++(*count))).c_str();
        }
        if ( kind == BODY && scene->isHidden(((SimpleBody*)(element))) ) {
            label = label + " (" + text("IDM_SELECTOR_HIDDEN", "hidden") + ")";
        }
        Entry* entry = new Entry();
        entry->kind = kind;
        entry->element = element;
        entries.add(entry);
        parent.add(DialogTreeNode(label, entry));
    }
}

const SceneHierarchyPresenter::Entry* SceneHierarchyPresenter::entryOf(
    const DialogTreeNode* node) const
{
    if ( node == NULL ) {
        return NULL;
    }
    for ( long i = 0; i < entries.size(); i++ ) {
        if ( entries[i] == node->getIdentity() ) {
            return entries[i];
        }
    }
    return NULL;
}

BOOLEAN SceneHierarchyPresenter::isSelected(const DialogTreeNode* node) const
{
    const Entry* entry = entryOf(node);
    if ( entry == NULL ) {
        return FALSE;
    }
    Scene* scene = model->getScene();
    SelectionSet* selection = NULL;
    int index = -1;
    switch ( entry->kind ) {
      case BODY:
        selection = scene->selectedThings;
        index = indexOf(scene->scene->getSimpleBodies(), entry->element);
        break;
      case LIGHT:
        selection = scene->selectedLights;
        index = indexOf(scene->scene->getLights(), entry->element);
        break;
      case GIZMO:
        selection = scene->selectedDebugThingGroups;
        index = indexOf(scene->debugThingGroups, entry->element);
        break;
      default:
        return FALSE;
    }
    selection->sync();
    return index >= 0 && selection->isSelected(index);
}

void SceneHierarchyPresenter::select(
    const ArrayList<const DialogTreeNode*>& nodes)
{
    Scene* scene = model->getScene();
    ArrayList<BOOLEAN> beforeB = SceneSelectionOperation::capture(scene->selectedThings);
    ArrayList<BOOLEAN> beforeL = SceneSelectionOperation::capture(scene->selectedLights);
    ArrayList<BOOLEAN> beforeG =
        SceneSelectionOperation::capture(scene->selectedDebugThingGroups);
    scene->selectedThings->unselectAll();
    scene->selectedLights->unselectAll();
    scene->selectedDebugThingGroups->unselectAll();
    for ( long i = 0; i < nodes.size(); i++ ) {
        const Entry* entry = entryOf(nodes[i]);
        if ( entry == NULL ) continue;
        int index;
        switch ( entry->kind ) {
          case BODY:
            index = indexOf(scene->scene->getSimpleBodies(), entry->element);
            if ( index >= 0 ) scene->selectedThings->select(index);
            break;
          case LIGHT:
            index = indexOf(scene->scene->getLights(), entry->element);
            if ( index >= 0 ) scene->selectedLights->select(index);
            break;
          case GIZMO:
            index = indexOf(scene->debugThingGroups, entry->element);
            if ( index >= 0 ) scene->selectedDebugThingGroups->select(index);
            break;
          default:
            break;
        }
    }
    ArrayList<BOOLEAN> afterB = SceneSelectionOperation::capture(scene->selectedThings);
    ArrayList<BOOLEAN> afterL = SceneSelectionOperation::capture(scene->selectedLights);
    ArrayList<BOOLEAN> afterG =
        SceneSelectionOperation::capture(scene->selectedDebugThingGroups);
    if ( !SceneSelectionOperation::isSame(beforeB, afterB) ||
         !SceneSelectionOperation::isSame(beforeL, afterL) ||
         !SceneSelectionOperation::isSame(beforeG, afterG) ) {
        model->getEditHistory()->getSceneHistory()->getQueue()->record(
            new SceneSelectionOperation(
                text("IDM_SELECTOR_SELECTION", "Select scene elements"), scene,
                beforeB, beforeL, beforeG, afterB, afterL, afterG));
    }
    if ( repaint != NULL ) repaint->run();
}

void SceneHierarchyPresenter::deleteSelected()
{
    Scene* scene = model->getScene();
    if ( scene->selectedThings->numberOfSelections() == 0 &&
         scene->selectedLights->numberOfSelections() == 0 &&
         scene->selectedDebugThingGroups->numberOfSelections() == 0 ) {
        return;
    }
    DeleteSelectedAction deleteAction(scene);
    model->getEditHistory()->getSceneHistory()->perform(
        text("IDM_SELECTOR_DELETE", "Delete selected"), &deleteAction);
    if ( repaint != NULL ) repaint->run();
}

ArrayList<SimpleBody*> SceneHierarchyPresenter::selectedBodies(
    const ArrayList<const DialogTreeNode*>& nodes) const
{
    ArrayList<SimpleBody*> result;
    for ( long i = 0; i < nodes.size(); i++ ) {
        const Entry* entry = entryOf(nodes[i]);
        if ( entry != NULL && entry->kind == BODY ) {
            result.add(((SimpleBody*)(entry->element)));
        }
    }
    return result;
}

BOOLEAN SceneHierarchyPresenter::anyHidden(
    const ArrayList<SimpleBody*>& bodies) const
{
    for ( long i = 0; i < bodies.size(); i++ ) {
        if ( model->getScene()->isHidden(bodies[i]) ) {
            return TRUE;
        }
    }
    return FALSE;
}

void SceneHierarchyPresenter::setHidden(const ArrayList<SimpleBody*>& bodies,
                                        BOOLEAN hidden)
{
    for ( long i = 0; i < bodies.size(); i++ ) {
        model->getScene()->setHidden(bodies[i], hidden);
    }
    if ( repaint != NULL ) repaint->run();
}
