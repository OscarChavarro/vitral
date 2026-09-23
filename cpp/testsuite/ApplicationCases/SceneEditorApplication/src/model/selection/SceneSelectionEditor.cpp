#include <typeinfo>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "model/Scene.h"
#include "model/selection/SceneSelectionEditor.h"
#include "model/selection/SelectionSet.h"

namespace {
void disposeRemoved(const java::ArrayList<Entity*>& removed)
{
    long i;
    for ( i = 0; i < removed.size(); i++ ) {
        Entity* element = removed.get(i);
        SimpleBody* body = dynamic_cast<SimpleBody*>(element);
        if ( body != nullptr && body->getGeometry() != nullptr ) {
            body->getGeometry()->dispose();
        }
        if ( element != nullptr ) {
            element->dispose();
        }
    }
}
}

SceneSelectionEditor::SceneSelectionEditor(Scene* scene) : scene(scene)
{
}

bool SceneSelectionEditor::computeSelectionCentroid(Vector3Dd* outCentroid)
{
    Vector3Dd sum;
    int count = 0;
    int i;

    scene->selectedThings->sync();
    for ( i = 0; i < scene->selectedThings->size(); i++ ) {
        if ( !scene->selectedThings->isSelected(i) ) continue;
        sum = sum.add(scene->scene->getSimpleBodies().get(i)->getPosition());
        count++;
    }
    scene->selectedLights->sync();
    for ( i = 0; i < scene->selectedLights->size(); i++ ) {
        if ( !scene->selectedLights->isSelected(i) ) continue;
        sum = sum.add(scene->scene->getLights().get(i)->getPosition());
        count++;
    }
    if ( count == 0 ) {
        return false;
    }
    *outCentroid = sum.multiply(1.0 / count);
    return true;
}

Matrix4x4d SceneSelectionEditor::createTranslationGizmoMatrix(
    const Vector3Dd& position)
{
    Matrix4x4d composed;

    composed = composed.withVal(0, 3, position.x());
    composed = composed.withVal(1, 3, position.y());
    composed = composed.withVal(2, 3, position.z());
    return composed;
}

Matrix4x4d SceneSelectionEditor::createRotationGizmoMatrix(
    const SimpleBody* body)
{
    return body->getRotation().withTranslation(body->getPosition());
}

void SceneSelectionEditor::applyTranslationToSelectedObjects(
    const Vector3Dd& oldCentroid, const Vector3Dd& newCentroid)
{
    SimpleBody* gi;
    Light* light;
    Vector3Dd delta = newCentroid.subtract(oldCentroid);
    int i;

    for ( i = 0; i < scene->selectedThings->size(); i++ ) {
        if ( !scene->selectedThings->isSelected(i) ) continue;
        gi = scene->scene->getSimpleBodies().get(i);

        gi->setPosition(gi->getPosition().add(delta));
    }
    for ( i = 0; i < scene->selectedLights->size(); i++ ) {
        if ( !scene->selectedLights->isSelected(i) ) continue;
        light = scene->scene->getLights().get(i);

        light->setPosition(light->getPosition().add(delta));
    }
}

SimpleBody* SceneSelectionEditor::getFirstSelectedBody()
{
    int firstThingSelected = scene->selectedThings->firstSelected();

    if ( firstThingSelected < 0 ) {
        return nullptr;
    }
    return scene->scene->getSimpleBodies().get(firstThingSelected);
}

void SceneSelectionEditor::selectPrevious()
{
    scene->selectedLights->unselectAll();
    if ( scene->selectedDebugThingGroups->numberOfSelections() < 1 ) {
        scene->selectedThings->selectPrevious();
    }
    if ( scene->selectedThings->numberOfSelections() < 1 ) {
        scene->selectedDebugThingGroups->selectPrevious();
    }
}

void SceneSelectionEditor::selectNext()
{
    scene->selectedLights->unselectAll();
    if ( scene->selectedDebugThingGroups->numberOfSelections() < 1 ) {
        scene->selectedThings->selectNext();
    }
    if ( scene->selectedThings->numberOfSelections() < 1 ) {
        scene->selectedDebugThingGroups->selectNext();
    }
}

void SceneSelectionEditor::deleteSelected()
{
    long i;

    java::ArrayList<Entity*> removedThings =
        scene->selectedThings->removeSelected();
    java::ArrayList<Entity*> removedLights =
        scene->selectedLights->removeSelected();
    for ( i = scene->debugThingGroups.size()-1; i >= 0; i-- ) {
        if ( scene->selectedDebugThingGroups->isSelected((int)i) ) {
            scene->debugThingGroups.remove(i);
        }
    }
    scene->selectedThings->sync();

    disposeRemoved(removedThings);
    disposeRemoved(removedLights);
}

java::String SceneSelectionEditor::describeSelection()
{
    java::String msg = "";
    int n;

    //-----------------------------------------------------------------
    scene->selectedThings->sync();
    n = scene->selectedThings->numberOfSelections();
    if ( n == 0 ) {
        msg += "All things are UNSELECTED";
    }
    else if ( n == 1 ) {
        int f = scene->selectedThings->firstSelected();
        Geometry* g = scene->scene->getSimpleBodies().get(f)->getGeometry();
        msg = java::String("Thing [") + java::String::valueOf(f) +
            "] selected, which is a [" +
            (g != nullptr ? typeid(*g).name() : "null") + "]";
    }
    else {
        msg += java::String::valueOf(n) + " things selected";
    }

    //-----------------------------------------------------------------
    scene->selectedLights->sync();
    n = scene->selectedLights->numberOfSelections();
    if ( n == 1 ) {
        msg += java::String("; Light [") +
            java::String::valueOf(scene->selectedLights->firstSelected()) +
            "] selected";
    }
    else if ( n > 1 ) {
        msg += java::String("; ") + java::String::valueOf(n) +
            " lights selected";
    }

    //-----------------------------------------------------------------
    scene->selectedDebugThingGroups->sync();
    n = scene->selectedDebugThingGroups->numberOfSelections();
    if ( n == 0 ) {
        msg += "; All visual debug groups are UNSELECTED";
    }
    else if ( n == 1 ) {
        int f = scene->selectedDebugThingGroups->firstSelected();
        msg += java::String("; Debug group [") + java::String::valueOf(f) +
            "] selected.";
    }
    else {
        msg += java::String("; ") + java::String::valueOf(n) +
            " debug groups selected";
    }
    return msg;
}
