#include <map>
#include <string>

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

namespace {

template <class T>
std::vector<Entity*> entities(java::ArrayList<T*>& list)
{
    std::vector<Entity*> result;
    for ( long i = 0; i < list.size(); i++ ) {
        result.push_back(list.get(i));
    }
    return result;
}

template <class T>
int indexOf(java::ArrayList<T*>& list, const Entity* element)
{
    for ( long i = 0; i < list.size(); i++ ) {
        if ( list.get(i) == element ) {
            return static_cast<int>(i);
        }
    }
    return -1;
}

bool isBlank(const java::String& text)
{
    return std::string(text.c_str()).find_first_not_of(" \t\r\n") ==
        std::string::npos;
}

}

SceneHierarchyPresenter::SceneHierarchyPresenter(
    ApplicationModel* model, const std::function<void()>& repaint)
    : model(model), repaint(repaint)
{
}

java::String SceneHierarchyPresenter::text(const char* key,
                                           const char* fallback) const
{
    Widget* messages = model->getI18nContext();
    java::String value = messages != nullptr ?
        messages->getMessage(key) : java::String("");
    // A missing message is its own key in the C++ context (null in Java)
    return isBlank(value) || value.equals(key) ? java::String(fallback) : value;
}

DialogTreeNode SceneHierarchyPresenter::hierarchy()
{
    Scene* scene = model->getScene();
    entries.clear();
    DialogTreeNode root("Scene", nullptr);
    addEntries(root.add(DialogTreeNode(text("IDM_SELECTOR_LIGHTS", "Lights"),
                                       nullptr)),
               LIGHT, entities(scene->scene->getLights()));
    addEntries(root.add(DialogTreeNode(text("IDM_SELECTOR_CAMERAS", "Cameras"),
                                       nullptr)),
               CAMERA, entities(scene->scene->getCameras()));
    addEntries(root.add(DialogTreeNode(
                   text("IDM_SELECTOR_OBJECTS", "Geometry objects"), nullptr)),
               BODY, entities(scene->scene->getSimpleBodies()));
    return root;
}

void SceneHierarchyPresenter::addEntries(DialogTreeNode& parent, Kind kind,
                                         const std::vector<Entity*>& elements)
{
    Scene* scene = model->getScene();
    std::map<std::string, int> unnamed;
    for ( size_t i = 0; i < elements.size(); i++ ) {
        Entity* element = elements[i];
        java::String name;
        switch ( kind ) {
          case BODY: name = static_cast<SimpleBody*>(element)->getName(); break;
          case LIGHT: name = static_cast<Light*>(element)->getName(); break;
          case CAMERA: name = static_cast<Camera*>(element)->getName(); break;
          case GIZMO: name = static_cast<SimpleBodyGroup*>(element)->getName(); break;
        }
        java::String label = name;
        if ( isBlank(name) ) {
            Geometry* geometry = kind == BODY ?
                static_cast<SimpleBody*>(element)->getGeometry() : nullptr;
            std::string type = geometry != nullptr ?
                geometry->getClassSimpleName().c_str() :
                element->getClassSimpleName().c_str();
            label = (type + " " + std::to_string(++unnamed[type])).c_str();
        }
        if ( kind == BODY && scene->isHidden(static_cast<SimpleBody*>(element)) ) {
            label = label + " (" + text("IDM_SELECTOR_HIDDEN", "hidden") + ")";
        }
        Entry entry;
        entry.kind = kind;
        entry.element = element;
        entries.push_back(entry);
        parent.add(DialogTreeNode(label, &entries.back()));
    }
}

const SceneHierarchyPresenter::Entry* SceneHierarchyPresenter::entryOf(
    const DialogTreeNode* node) const
{
    if ( node == nullptr ) {
        return nullptr;
    }
    for ( size_t i = 0; i < entries.size(); i++ ) {
        if ( &entries[i] == node->getIdentity() ) {
            return &entries[i];
        }
    }
    return nullptr;
}

bool SceneHierarchyPresenter::isSelected(const DialogTreeNode* node) const
{
    const Entry* entry = entryOf(node);
    if ( entry == nullptr ) {
        return false;
    }
    Scene* scene = model->getScene();
    SelectionSet* selection = nullptr;
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
        return false;
    }
    selection->sync();
    return index >= 0 && selection->isSelected(index);
}

void SceneHierarchyPresenter::select(
    const std::vector<const DialogTreeNode*>& nodes)
{
    Scene* scene = model->getScene();
    std::vector<bool> beforeB = SceneSelectionOperation::capture(scene->selectedThings);
    std::vector<bool> beforeL = SceneSelectionOperation::capture(scene->selectedLights);
    std::vector<bool> beforeG =
        SceneSelectionOperation::capture(scene->selectedDebugThingGroups);
    scene->selectedThings->unselectAll();
    scene->selectedLights->unselectAll();
    scene->selectedDebugThingGroups->unselectAll();
    for ( size_t i = 0; i < nodes.size(); i++ ) {
        const Entry* entry = entryOf(nodes[i]);
        if ( entry == nullptr ) continue;
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
    std::vector<bool> afterB = SceneSelectionOperation::capture(scene->selectedThings);
    std::vector<bool> afterL = SceneSelectionOperation::capture(scene->selectedLights);
    std::vector<bool> afterG =
        SceneSelectionOperation::capture(scene->selectedDebugThingGroups);
    if ( !SceneSelectionOperation::isSame(beforeB, afterB) ||
         !SceneSelectionOperation::isSame(beforeL, afterL) ||
         !SceneSelectionOperation::isSame(beforeG, afterG) ) {
        model->getEditHistory()->getSceneHistory()->getQueue()->record(
            new SceneSelectionOperation(
                text("IDM_SELECTOR_SELECTION", "Select scene elements"), scene,
                beforeB, beforeL, beforeG, afterB, afterL, afterG));
    }
    if ( repaint ) repaint();
}

void SceneHierarchyPresenter::deleteSelected()
{
    Scene* scene = model->getScene();
    if ( scene->selectedThings->numberOfSelections() == 0 &&
         scene->selectedLights->numberOfSelections() == 0 &&
         scene->selectedDebugThingGroups->numberOfSelections() == 0 ) {
        return;
    }
    model->getEditHistory()->getSceneHistory()->perform(
        text("IDM_SELECTOR_DELETE", "Delete selected"),
        [scene]() { SceneSelectionEditor(scene).deleteSelected(); });
    if ( repaint ) repaint();
}

std::vector<SimpleBody*> SceneHierarchyPresenter::selectedBodies(
    const std::vector<const DialogTreeNode*>& nodes) const
{
    std::vector<SimpleBody*> result;
    for ( size_t i = 0; i < nodes.size(); i++ ) {
        const Entry* entry = entryOf(nodes[i]);
        if ( entry != nullptr && entry->kind == BODY ) {
            result.push_back(static_cast<SimpleBody*>(entry->element));
        }
    }
    return result;
}

bool SceneHierarchyPresenter::anyHidden(
    const std::vector<SimpleBody*>& bodies) const
{
    for ( size_t i = 0; i < bodies.size(); i++ ) {
        if ( model->getScene()->isHidden(bodies[i]) ) {
            return true;
        }
    }
    return false;
}

void SceneHierarchyPresenter::setHidden(const std::vector<SimpleBody*>& bodies,
                                        bool hidden)
{
    for ( size_t i = 0; i < bodies.size(); i++ ) {
        model->getScene()->setHidden(bodies[i], hidden);
    }
    if ( repaint ) repaint();
}
