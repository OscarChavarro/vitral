#include "java/io/File.h"
#include "java/util/ArrayList.txx"
#include "../model/ShadersModel.h"
#include "SoftwareRaycaster.h"
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/media/IndexedColorImageUncompressed.h"
#include "vsdk/toolkit/media/NormalMap.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/background/SimpleBackground.h"
#include "vsdk/toolkit/environment/camera/CameraSnapshot.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/AmbientLight.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/render/raytracing/ParallelRaytracer.h"
static const Vector3Dd DEFAULT_BUMP_SCALE(1.0, 1.0, 1.0);

SoftwareRaycaster::SoftwareRaycaster()
    : parallelRaytracer(new ParallelRaytracer()),
      bumpNormalMap(0)
{
    try {
        IndexedColorImageUncompressed* bumpMap = ImagePersistence::importIndexedColor(
            java::File("../../../../etc/bumpmaps/earth.bw"));
        if ( bumpMap != 0 ) {
            bumpNormalMap = new NormalMap();
            bumpNormalMap->importBumpMap(bumpMap, DEFAULT_BUMP_SCALE);
            delete bumpMap;
        }
    }
    catch (...) {
        if ( bumpNormalMap != 0 ) {
            delete bumpNormalMap;
            bumpNormalMap = 0;
        }
        delete parallelRaytracer;
        parallelRaytracer = 0;
        Logger::reportMessage("SoftwareRaycaster", Logger::ERROR, "SoftwareRaycaster", "Failed loading software bump map");
        throw VSDKFatalException("Failed loading software bump map");
    }
}

SoftwareRaycaster::~SoftwareRaycaster()
{
    delete parallelRaytracer;
    parallelRaytracer = 0;
    if ( bumpNormalMap != 0 ) {
        delete bumpNormalMap;
        bumpNormalMap = 0;
    }
}

void SoftwareRaycaster::invalidateSnapshot() {}

SimpleSceneSnapshot* SoftwareRaycaster::buildSceneSnapshot(
    ShadersModel* model,
    Camera* activeCamera,
    const Matrix4x4d& modelRotation,
    RGBImageUncompressed* outputImage)
{
    const int viewportWidth = outputImage->getXSize();
    const int viewportHeight = outputImage->getYSize();
    CameraSnapshot* cameraSnapshot = activeCamera->exportToCameraSnapshot(
        viewportWidth,
        viewportHeight);

    SimpleBody* sphereBody = new SimpleBody();
    sphereBody->setGeometry(new Sphere(model->sphere->getRadius()));

    sphereBody->setMaterial(model->createActiveMaterialCopy());

    sphereBody->setTexture(model->textureMap ? model->textureMap->clone() : 0);
    sphereBody->setNormalMap(bumpNormalMap ? bumpNormalMap->clone() : 0);
    sphereBody->setRotation(modelRotation);

    java::ArrayList<SimpleBody*> bodies;
    bodies.add(sphereBody);

    java::ArrayList<Light*> lights;
    Light* ambientLight = new AmbientLight(ColorRgb(1, 1, 1));
    ambientLight->setId(0);
    lights.add(ambientLight);
    Light* pointLight = model->light->copy();
    pointLight->setId(1);
    lights.add(pointLight);

    SimpleBackground* background = new SimpleBackground();
    background->setColor(0, 0, 0);

    return new SimpleSceneSnapshot(bodies, lights, background, cameraSnapshot);
}

void SoftwareRaycaster::render(
    ShadersModel* model,
    Camera* activeCamera,
    const Matrix4x4d& modelRotation)
{
    if ( !model || !model->softwareFrameImage || !activeCamera ) return;

    RGBImageUncompressed* outputImage = model->softwareFrameImage;
    SimpleSceneSnapshot* snapshot = buildSceneSnapshot(
        model,
        activeCamera,
        modelRotation,
        outputImage);

    try {
        parallelRaytracer->execute(
            outputImage,
            &model->quality,
            snapshot,
            false);
    }
    catch (...) {
        delete snapshot;
        throw;
    }

    delete snapshot;
}
