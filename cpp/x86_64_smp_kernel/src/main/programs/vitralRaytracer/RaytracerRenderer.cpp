#include "java/lang/Exception.h"
#include "programs/vitralRaytracer/RaytracerRenderer.h"
#include <math.h>
#include <stdio.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/processing/StopWatch.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/camera/CameraSnapshot.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
#include "vsdk/toolkit/io/geometry/ReaderMitScene.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "vsdk/toolkit/render/raytracing/ParallelRaytracer.h"
#include "vsdk/toolkit/render/raytracing/SimpleRaytracer.h"

void
RaytracerRenderer::optimizeRendererConfigurationForScene(SimpleScene *scene,
                                                         RendererConfiguration *rendererConfiguration) {
    bool hasTextures = false;
    bool hasNormalMaps = false;
    java::ArrayList<SimpleBody *> &bodies = scene->getSimpleBodies();
    for (long int i = 0; i < bodies.size(); i++) {
        if (bodies[i]->getTexture() != 0) {
            hasTextures = true;
        }
        if (bodies[i]->getNormalMap() != 0) {
            hasNormalMaps = true;
        }
    }
    rendererConfiguration->setTexture(hasTextures);
    rendererConfiguration->setBumpMap(hasNormalMaps);
}

void
RaytracerRenderer::fitCameraToImage(Camera *camera, int width, int height) {
    const double toRadians = 3.14159265358979323846 / 180.0;
    double oldAspect = camera->getViewportXSize() / camera->getViewportYSize();
    double halfWidthTangent = tan(camera->getFov() * toRadians / 2.0) * oldAspect;
    camera->setFov(2.0 * atan(halfWidthTangent * height / width) / toRadians);
    camera->updateViewportResize(width, height);
}

int
RaytracerRenderer::render(const char *path, int width, int height, int parallel, uint8_t *rgb,
                          ProgressMonitor *reporter, double *seconds, char *error, unsigned errorSize) {
    int ok = 0;
    SimpleScene *scene = new SimpleScene;
    try {
        ReaderMitScene reader;
        reader.importEnvironment(path, scene);
    } catch (const java::Exception &e) {
        snprintf(error, errorSize, "cannot read %s: %s", path, e.getMessage().c_str());
        delete scene;
        return 0;
    } catch (...) {
        snprintf(error, errorSize, "cannot read %s", path);
        delete scene;
        return 0;
    }
    Camera *camera = scene->getActiveCamera();
    RGBImageUncompressed *img = new RGBImageUncompressed;
    if (!camera) {
        snprintf(error, errorSize, "%s has no camera", path);
    } else if (!img->initNoFill(width, height)) {
        snprintf(error, errorSize, "not enough memory for a %dx%d image", width, height);
    } else {
        fitCameraToImage(camera, width, height);
        RendererConfiguration configuration;
        optimizeRendererConfigurationForScene(scene, &configuration);
        SimpleRaytracer *engine = new SimpleRaytracer;
        CameraSnapshot *cameraSnapshot = camera->exportToCameraSnapshot(width, height);
        SimpleSceneSnapshot *snapshot =
            scene->exportToSimpleSceneSnapshot(cameraSnapshot, scene->getActiveBackground());
        StopWatch clock;
        clock.start();
        if (parallel) {
            ParallelRaytracer parallelRaytracer;
            parallelRaytracer.execute(img, &configuration, snapshot, true);
            parallelRaytracer.dispose();
        } else {
            engine->execute(img, &configuration, snapshot, reporter, (ZBuffer *)0);
        }
        clock.stop();
        *seconds = clock.getElapsedRealTime();
        RGBPixel p;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img->getPixelRgb(x, y, &p);
                uint8_t *dst = rgb + ((unsigned)y * (unsigned)width + (unsigned)x) * 3;
                dst[0] = (uint8_t)p.r;
                dst[1] = (uint8_t)p.g;
                dst[2] = (uint8_t)p.b;
            }
        }
        delete snapshot;
        delete engine;
        ok = 1;
    }
    delete img;
    delete scene;
    return ok;
}
