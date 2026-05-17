#include "SoftwareRaycaster.h"
#include "../model/ShadersModel.h"

#include <vector>

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/background/SimpleBackground.h"
#include "vsdk/toolkit/environment/light/LightType.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
#include "vsdk/toolkit/render/SimpleRaytracer.h"

void SoftwareRaycaster::invalidateSnapshot() {}

void SoftwareRaycaster::render(
    ShadersModel* model,
    vsdk::toolkit::environment::camera::Camera* activeCamera,
    const Matrix4x4d& modelRotation)
{
    if (!model || !model->softwareFrameImage || !activeCamera) return;

    const int w = model->softwareFrameImage->getXSize();
    const int h = model->softwareFrameImage->getYSize();
    activeCamera->updateViewportResize(w, h);

    std::vector<SimpleBody*> bodies;
    std::vector<Light*> lights;

    SimpleBody* sphereBody = new SimpleBody();
    sphereBody->setGeometry(new Sphere(model->sphere->getRadius()));
    sphereBody->setMaterial(new SimpleMaterial(model->material));
    sphereBody->setTexture(model->textureMap ? model->textureMap->clone() : 0);
    sphereBody->setNormalMap(model->bumpNormalMap ? model->bumpNormalMap->clone() : 0);
    sphereBody->setRotation(modelRotation);
    bodies.push_back(sphereBody);

    Light* ambient = new Light(LightType::AMBIENT, Vector3Dd(0,0,0), ColorRgb(1,1,1));
    ambient->setId(0);
    lights.push_back(ambient);

    Light* point = new Light(model->light->getLightType(), model->light->getPosition(), model->light->getSpecular());
    point->setId(1);
    lights.push_back(point);

    SimpleBackground* bg = new SimpleBackground();
    bg->setColor(0,0,0);

    SimpleSceneSnapshot* snapshot = new SimpleSceneSnapshot(
        bodies,
        lights,
        bg,
        activeCamera->exportToCameraSnapshot(w, h));

    SimpleRaytracer raytracer;
    raytracer.execute(model->softwareFrameImage, &model->quality, snapshot, 0);

    delete snapshot;
    delete bg;
    for (size_t i = 0; i < lights.size(); i++) delete lights[i];
    for (size_t i = 0; i < bodies.size(); i++) delete bodies[i];
}
