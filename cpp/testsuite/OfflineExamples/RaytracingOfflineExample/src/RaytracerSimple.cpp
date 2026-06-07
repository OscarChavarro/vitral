#include "CommandOptionsProcessor.h"
#include "ImageExporter.h"
#include "RaytracerExecutor.h"
#include "RaytracerParallelExecutor.h"
#include "RaytracerSerialExecutor.h"

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/statistics/RaytraceStatistics.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/camera/CameraSnapshot.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
#include "vsdk/toolkit/gui/feedback/ProgressMonitorConsole.h"
#include "vsdk/toolkit/io/geometry/ReaderMitScene.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/processing/StopWatch.h"
#include "vsdk/toolkit/render/raytracing/SimpleRaytracer.h"

#include <cstdlib>
#include <exception>
#include <cstdio>
#include "java/util/ArrayList.txx"

static const char* SCENE_SAMPLES_PATH = "../../../../etc/geometry/mitscenes/";
static const int ELAPSED_TIME_DECIMALS = 3;
static const int EXIT_CODE_READ_ERROR = -1;
static const int EXIT_CODE_IMAGE_ERROR = 1;
static const int EXIT_CODE_ARGUMENT_ERROR = 2;

static void optimizeRendererConfigurationForScene(SimpleScene* scene, RendererConfiguration* rendererConfiguration)
{
    bool hasTextures = false;
    bool hasNormalMaps = false;
    java::ArrayList<SimpleBody*>& bodies = scene->getSimpleBodies();

    for (long int i = 0; i < bodies.size(); i++) {
        SimpleBody* body = bodies[i];
        if ( body->getTexture() != 0 ) hasTextures = true;
        if ( body->getNormalMap() != 0 ) hasNormalMaps = true;
        if ( hasTextures && hasNormalMaps ) break;
    }

    rendererConfiguration->setTexture(hasTextures);
    rendererConfiguration->setBumpMap(hasNormalMaps);
}

static void offlineExecution(const java::String& fileName,
                             bool save,
                             const java::String& outputFileName,
                             bool parallel)
{
    SimpleScene scene;

    printf("Loading scene from %s: \n", fileName.c_str());

    try {
        ReaderMitScene readerMitScene;
        readerMitScene.importEnvironment(fileName.c_str(), &scene);
    }
    catch (const std::exception& e) {
        fprintf(stderr, "Error reading %s: %s\n", fileName.c_str(), e.what());
        fprintf(stderr, "There are scene samples on %s\n", SCENE_SAMPLES_PATH);
        std::exit(EXIT_CODE_READ_ERROR);
    }

    printf("Scene loaded OK!\n");

    RGBImageUncompressed resultingImage;
    Camera* activeCamera = scene.getActiveCamera();
    if ( !resultingImage.initNoFill((int)activeCamera->getViewportXSize(),
                                    (int)activeCamera->getViewportYSize()) ) {
        fprintf(stderr, "Error creating image!\n");
        std::exit(EXIT_CODE_IMAGE_ERROR);
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
    RaytracerExecutor* raytracerExecutor =
        parallel ? (RaytracerExecutor*)new RaytracerParallelExecutor() : (RaytracerExecutor*)new RaytracerSerialExecutor();

    clock.start();
    raytracerExecutor->run(&visualizationEngine,
                           &resultingImage,
                           &rendererConfiguration,
                           sceneSnapshot,
                           &reporter);
    clock.stop();

    printf("Image generated in %s seconds.\n",
        VSDK::formatDouble(clock.getElapsedRealTime(), ELAPSED_TIME_DECIMALS).c_str());
    RaytraceStatistics::printSummary();

    if ( save ) {
        ImageExporter imageExporter;
        if ( !imageExporter.exportImage(outputFileName, &resultingImage) ) {
            fprintf(stderr, "Error saving output image!\n");
            std::exit(EXIT_CODE_IMAGE_ERROR);
        }
    }

    delete raytracerExecutor;
    delete sceneSnapshot;
}

int main(int argc, char** argv)
{
    java::ArrayList<java::String> args;
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
                     options.shouldUseParallelExecutor());
    return 0;
}
