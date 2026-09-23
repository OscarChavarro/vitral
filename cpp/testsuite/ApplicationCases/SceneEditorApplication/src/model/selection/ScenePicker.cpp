#include <cfloat>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "model/Scene.h"
#include "model/selection/LightPicker.h"
#include "model/selection/ScenePicker.h"
#include "model/selection/SelectionSet.h"

ScenePicker::ScenePicker(Scene* scene) : scene(scene)
{
}

Ray ScenePicker::selectObjectWithMouse(int x, int y, bool composite)
{
    Camera* camera = scene->activeCamera;
    SimpleBody* gi;

    camera->updateVectors();
    Ray r = camera->generateRay(x, y);

    Ray selectedRay = Ray::copyOf(r);

    double nearestDistance = FLT_MAX;
    int nearestBody = -1;
    int nearestLight = -1;

    int i;

    SelectionSet* selectedThings = scene->selectedThings;
    SelectionSet* selectedLights = scene->selectedLights;
    selectedThings->sync();
    selectedLights->sync();

    java::ArrayList<SimpleBody*>& things = scene->scene->getSimpleBodies();
    for ( i = 0; i < things.size(); i++ ) {
        gi = things.get(i);
        Ray* hit = gi->doIntersectionFirstHit(r);
        if ( hit != nullptr && hit->getT() < nearestDistance ) {
            nearestDistance = hit->getT();
            nearestBody = i;
        }
        delete hit;
    }

    java::ArrayList<Light*>& lights = scene->scene->getLights();
    for ( i = 0; i < lights.size(); i++ ) {
        double t = LightPicker::pick(r, camera, lights.get(i),
            scene->getLightGizmoScale());
        if ( t >= 0 && t < nearestDistance ) {
            nearestDistance = t;
            nearestBody = -1;
            nearestLight = i;
        }
    }

    if ( !composite ) {
        selectedThings->unselectAll();
        selectedLights->unselectAll();
        selectedThings->select(nearestBody);
        selectedLights->select(nearestLight);
    }
    else {
        selectedThings->change(nearestBody);
        selectedLights->change(nearestLight);
    }
    return selectedRay;
}
