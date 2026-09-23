#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "model/Scene.h"
#include "model/history/SceneElementKind.h"
#include "model/selection/SelectionSet.h"

EntityListView SceneElementKind::getList(Value kind, Scene* scene)
{
    switch ( kind ) {
      case BODY:
        return EntityListView(&scene->scene->getSimpleBodies());
      case LIGHT:
        return EntityListView(&scene->scene->getLights());
      case CAMERA:
        return EntityListView(&scene->scene->getCameras());
      default:
        return EntityListView(&scene->debugThingGroups);
    }
}

SelectionSet* SceneElementKind::getSelection(Value kind, Scene* scene)
{
    switch ( kind ) {
      case BODY:
        return scene->selectedThings;
      case LIGHT:
        return scene->selectedLights;
      case DEBUG_GROUP:
        return scene->selectedDebugThingGroups;
      default:
        return nullptr;
    }
}

void SceneElementKind::insert(Value kind, Scene* scene, int index,
                              Entity* element, bool selected)
{
    SelectionSet* selection = getSelection(kind, scene);

    if ( selection != nullptr ) {
        selection->insertElement(index, element, selected);
    }
    else {
        getList(kind, scene).insert(index, element);
    }
}

void SceneElementKind::remove(Value kind, Scene* scene, int index)
{
    SelectionSet* selection = getSelection(kind, scene);

    if ( selection != nullptr ) {
        selection->removeElement(index);
    }
    else {
        getList(kind, scene).remove(index);
    }
}
