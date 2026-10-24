#include <math.h>
#include <stdio.h>
#include <stdlib.h>

#include "java/util/ArrayList.txx"
#include "CommandOptionsProcessor.h"
#include "ImageExporter.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/statistics/RaytraceStatistics.h"
#include "vsdk/toolkit/gui/feedback/ProgressMonitorConsole.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/processing/StopWatch.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/camera/CameraSnapshot.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
#include "vsdk/toolkit/io/geometry/ReaderMitScene.h"
#include "vsdk/toolkit/render/raytracing/ParallelRaytracer.h"
#include "vsdk/toolkit/render/raytracing/SimpleRaytracer.h"
#include "java/lang/Exception.h"
static const char* SCENE_SAMPLES_PATH = "../../../../etc/geometry/mitscenes/";
static const int ELAPSED_TIME_DECIMALS = 3;
static const int EXIT_CODE_READ_ERROR = -1;
static const int EXIT_CODE_IMAGE_ERROR = 1;
static const int EXIT_CODE_ARGUMENT_ERROR = 2;

static void optimizeRendererConfigurationForScene(SimpleScene* scene, RendererConfiguration* rendererConfiguration)
{
    BOOLEAN hasTextures = FALSE;
    BOOLEAN hasNormalMaps = FALSE;
    ArrayList<SimpleBody*>& bodies = scene->getSimpleBodies();

    for (long int i = 0; i < bodies.size(); i++) {
        SimpleBody* body = bodies[i];
        if ( body->getTexture() != 0 ) hasTextures = TRUE;
        if ( body->getNormalMap() != 0 ) hasNormalMaps = TRUE;
        if ( hasTextures && hasNormalMaps ) break;
    }

    rendererConfiguration->setTexture(hasTextures);
    rendererConfiguration->setBumpMap(hasNormalMaps);
}

// Changes the camera viewport keeping the horizontal field of view of the
// scene, so a different aspect ratio does not stretch the image
static void resizeCameraViewport(Camera* camera, int xSize, int ySize)
{
    double oldAspect = camera->getViewportXSize() / camera->getViewportYSize();
    double newAspect = ((double)xSize) / ((double)ySize);
    double verticalHalfAngle = camera->getFov() * M_PI / 360.0;
    double horizontalHalfAngle = atan(tan(verticalHalfAngle) * oldAspect);
    verticalHalfAngle = atan(tan(horizontalHalfAngle) / newAspect);
    camera->setFov((verticalHalfAngle * 360.0) / M_PI);
    camera->updateViewportResize(xSize, ySize);
}

static void offlineExecution(const String& fileName,
                             BOOLEAN save,
                             const String& outputFileName,
                             BOOLEAN parallel,
                             int xResolution,
                             int yResolution)
{
    SimpleScene scene;

    printf("Loading scene from %s: \n", fileName.c_str());

    try {
        ReaderMitScene readerMitScene;
        readerMitScene.importEnvironment(fileName.c_str(), &scene);
    }
#ifndef VSDK_WITHOUT_EXCEPTIONS
    catch (const Exception& e) {
        fprintf(stderr, "Error reading %s: %s\n", fileName.c_str(), e.getMessage().c_str());
        fprintf(stderr, "There are scene samples on %s\n", SCENE_SAMPLES_PATH);
        exit(EXIT_CODE_READ_ERROR);
    }
#endif

    printf("Scene loaded OK!\n");

    RGBImageUncompressed resultingImage;
    Camera* activeCamera = scene.getActiveCamera();
    if ( xResolution > 0 && yResolution > 0 ) {
        resizeCameraViewport(activeCamera, xResolution, yResolution);
    }
    if ( !resultingImage.initNoFill((int)activeCamera->getViewportXSize(),
                                    (int)activeCamera->getViewportYSize()) ) {
        fprintf(stderr, "Error creating image!\n");
        exit(EXIT_CODE_IMAGE_ERROR);
    }

    ProgressMonitorConsole reporter;
    RendererConfiguration rendererConfiguration;
    optimizeRendererConfigurationForScene(&scene, &rendererConfiguration);

    SimpleRaytracer visualizationEngine;
    CameraSnapshot* cameraSnapshot =
        activeCamera->exportToCameraSnapshot(resultingImage.getXSize(), resultingImage.getYSize());
    SimpleSceneSnapshot* sceneSnapshot =
        scene.exportToSimpleSceneSnapshot(cameraSnapshot, scene.getActiveBackground());

    StopWatch clock;

    clock.start();
    if ( parallel ) {
        ParallelRaytracer parallelRaytracer;
        parallelRaytracer.execute(&resultingImage,
                                  &rendererConfiguration,
                                  sceneSnapshot,
                                  TRUE);
        parallelRaytracer.dispose();
    }
    else {
        visualizationEngine.execute(&resultingImage,
                                    &rendererConfiguration,
                                    sceneSnapshot,
                                    &reporter,
                                    (ZBuffer*)0);
    }
    clock.stop();

    printf("Image generated in %s seconds.\n",
        VSDK::formatDouble(clock.getElapsedRealTime(), ELAPSED_TIME_DECIMALS).c_str());
    RaytraceStatistics::printSummary();

    if ( save ) {
        ImageExporter imageExporter;
        if ( !imageExporter.exportImage(outputFileName, &resultingImage) ) {
            fprintf(stderr, "Error saving output image!\n");
            exit(EXIT_CODE_IMAGE_ERROR);
        }
    }

    delete sceneSnapshot;
}

int main(int argc, char** argv)
{
    ArrayList<String> args;
    for (int i = 1; i < argc; i++) {
        args.add(argv[i]);
    }

    CommandOptionsProcessor options = CommandOptionsProcessor::process(args);
    if ( options.shouldShowHelp() ) {
        CommandOptionsProcessor::printUsage();
        if ( argc > 1 ) {
            return EXIT_CODE_ARGUMENT_ERROR;
        }
        return 0;
    }

    offlineExecution(options.getSceneFile(),
                     options.shouldSave(),
                     options.getOutputFile(),
                     options.shouldUseParallelExecutor(),
                     options.shouldUseSceneResolution() ? 0 : options.getXResolution(),
                     options.shouldUseSceneResolution() ? 0 : options.getYResolution());
    return 0;
}
