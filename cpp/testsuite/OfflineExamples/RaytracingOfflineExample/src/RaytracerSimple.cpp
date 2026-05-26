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
#include "vsdk/toolkit/render/SimpleRaytracer.h"

#include <fstream>
#include <iostream>
#include <memory>
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

    std::cout << "Loading scene from " << fileName << ": \n";
    std::ifstream is(fileName.c_str());
    if ( !is.good() ) {
        std::cerr << "Error reading " << fileName << "\n";
        std::cerr << "There are scene samples on " << SCENE_SAMPLES_PATH << "\n";
        std::exit(EXIT_CODE_READ_ERROR);
    }

    try {
        ReaderMitScene readerMitScene;
        readerMitScene.importEnvironment(is, &scene);
    }
    catch (const std::exception& e) {
        std::cerr << "Error reading " << fileName << ": " << e.what() << "\n";
        std::cerr << "There are scene samples on " << SCENE_SAMPLES_PATH << "\n";
        std::exit(EXIT_CODE_READ_ERROR);
    }

    std::cout << "Scene loaded OK!\n";

    RGBImageUncompressed resultingImage;
    Camera* activeCamera = scene.getActiveCamera();
    if ( !resultingImage.initNoFill((int)activeCamera->getViewportXSize(),
                                    (int)activeCamera->getViewportYSize()) ) {
        std::cerr << "Error creating image!\n";
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
    std::unique_ptr<RaytracerExecutor> raytracerExecutor(
        parallel ? (RaytracerExecutor*)new RaytracerParallelExecutor() : (RaytracerExecutor*)new RaytracerSerialExecutor());

    clock.start();
    raytracerExecutor->run(&visualizationEngine,
                           &resultingImage,
                           &rendererConfiguration,
                           sceneSnapshot,
                           &reporter);
    clock.stop();

    std::cout << "Image generated in "
              << VSDK::formatDouble(clock.getElapsedRealTime(), ELAPSED_TIME_DECIMALS)
              << " seconds.\n";
    RaytraceStatistics::printSummary();

    if ( save ) {
        ImageExporter imageExporter;
        if ( !imageExporter.exportImage(outputFileName, &resultingImage) ) {
            std::cerr << "Error saving output image!\n";
            std::exit(EXIT_CODE_IMAGE_ERROR);
        }
    }

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
