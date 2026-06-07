#include "SoftwareRaycaster.h"
#include "../model/ShadersModel.h"

#include <java/lang/Math.h>
#include <atomic>
#include <thread>
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "java/util/ArrayList.txx"
#if defined(_SC_NPROCESSORS_ONLN)
#include <unistd.h>
#endif

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/background/SimpleBackground.h"
#include "vsdk/toolkit/environment/camera/CameraSnapshot.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/LightType.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "java/io/File.h"
#include "vsdk/toolkit/media/IndexedColorImageUncompressed.h"
#include "vsdk/toolkit/media/NormalMap.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/render/SimpleRaytracer.h"
#include "vsdk/toolkit/render/Tile.h"
#include "vsdk/toolkit/render/TileGenerator.h"

static const Vector3Dd DEFAULT_BUMP_SCALE(1.0, 1.0, 1.0);

static int detectCpuCount()
{
#if defined(_SC_NPROCESSORS_ONLN)
    long count = sysconf(_SC_NPROCESSORS_ONLN);
    if ( count > 0 ) return (int)count;
#endif
    return 1;
}

SoftwareRaycaster::SoftwareRaycaster()
    : numberOfThreads(java::Math::max(1, detectCpuCount())),
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
        Logger::reportMessage("SoftwareRaycaster", Logger::ERROR, "SoftwareRaycaster", "Failed loading software bump map");
        throw VSDKFatalException("Failed loading software bump map");
    }
}

SoftwareRaycaster::~SoftwareRaycaster()
{
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
    Light* ambientLight = new Light(LightType::AMBIENT, Vector3Dd(0, 0, 0), ColorRgb(1, 1, 1));
    ambientLight->setId(0);
    lights.add(ambientLight);
    Light* pointLight = new Light(
        model->light->getLightType(),
        model->light->getPosition(),
        model->light->getSpecular());
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
        TileGenerator tileGenerator(
            TileGenerationStrategy::LINEAR,
            outputImage,
            outputImage->getXSize(),
            outputImage->getYSize(),
            numberOfThreads);
        const java::ArrayList<Tile>& tiles = tileGenerator.getTiles();
        const int workerCount = java::Math::max(1, numberOfThreads);
        std::atomic<size_t> nextTileIndex(0);
        std::thread* workers = new std::thread[workerCount];
        std::atomic<bool> failed(false);
        std::exception_ptr firstError;

        for ( int w = 0; w < workerCount; w++ ) {
            workers[w] = std::thread([&]() {
                SimpleRaytracer raytracer;
                try {
                    while ( true ) {
                        size_t tileIndex = nextTileIndex.fetch_add(1);
                        if ( (long int)tileIndex >= tiles.size() ) {
                            break;
                        }
                        Tile tile = tiles.get((long int)tileIndex);
                        raytracer.execute(
                            outputImage,
                            &model->quality,
                            snapshot,
                            0,
                            0,
                            tile.getX0(),
                            tile.getY0(),
                            tile.getX1(),
                            tile.getY1());
                    }
                }
                catch (...) {
                    if ( !failed.exchange(true) ) {
                        firstError = std::current_exception();
                    }
                }
            });
        }

        for ( int i = 0; i < workerCount; i++ ) {
            workers[i].join();
        }
        delete[] workers;

        if ( firstError ) {
            std::rethrow_exception(firstError);
        }
    }
    catch (...) {
        delete snapshot;
        throw;
    }

    delete snapshot;
}
