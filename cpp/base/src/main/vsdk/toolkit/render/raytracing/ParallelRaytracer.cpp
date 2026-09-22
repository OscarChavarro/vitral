#include <cstdio>

#include "java/util/ArrayList.txx"
#include "java/util/concurrent/Callable.h"
#include "java/util/concurrent/ConcurrentLinkedQueue.h"
#include "java/util/concurrent/ExecutorService.h"
#include "java/util/concurrent/Executors.h"
#include "java/util/concurrent/Future.h"
#include "java/util/concurrent/Void.h"
#include <pthread.h>
#include <unistd.h>
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"
#include "vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorConsumer.h"
#include "vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorEvent.h"
#include "vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorProducer.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
#include "vsdk/toolkit/render/raytracing/ParallelRaytracer.h"
#include "vsdk/toolkit/render/raytracing/RasterTileArea.h"
#include "vsdk/toolkit/render/raytracing/RasterTileGenerationStrategy.h"
#include "vsdk/toolkit/render/raytracing/RasterTileGenerator.h"
#include "vsdk/toolkit/render/raytracing/SimpleRaytracer.h"

struct ParallelRaytracerConsumerThreadData {
    ParallelProgressMonitorConsumer* consumer;
};

class ParallelRaytracerTileWorker : public java::Callable<java::Void> {
  private:
    java::ConcurrentLinkedQueue<RasterTileArea>* pendingTiles;
    RGBImageUncompressed* resultingImage;
    const RendererConfiguration* rendererConfiguration;
    SimpleSceneSnapshot* sceneSnapshot;
    ProgressMonitor* progressReporter;

  public:
    ParallelRaytracerTileWorker(java::ConcurrentLinkedQueue<RasterTileArea>* pendingTiles,
               RGBImageUncompressed* resultingImage,
               const RendererConfiguration* rendererConfiguration,
               SimpleSceneSnapshot* sceneSnapshot,
               ProgressMonitor* progressReporter)
        : pendingTiles(pendingTiles),
          resultingImage(resultingImage),
          rendererConfiguration(rendererConfiguration),
          sceneSnapshot(sceneSnapshot),
          progressReporter(progressReporter)
    {
    }

    virtual java::Void call() override
    {
        RasterTileArea tile(resultingImage, 0, 0,
            resultingImage->getXSize(), resultingImage->getYSize());
        SimpleRaytracer raytracer;

        while ( pendingTiles->poll(&tile) ) {
            raytracer.execute(resultingImage,
                              rendererConfiguration,
                              sceneSnapshot,
                              progressReporter,
                              0,
                              tile.getX0(),
                              tile.getY0(),
                              tile.getX1(),
                              tile.getY1());
        }
        return java::Void();
    }
};

ParallelRaytracer::ParallelRaytracer()
    : numberOfThreads(availableProcessors()),
      executorService(0)
{
}

ParallelRaytracer::ParallelRaytracer(int numberOfThreads)
    : numberOfThreads(numberOfThreads),
      executorService(0)
{
    if ( numberOfThreads <= 0 ) {
        Logger::reportMessage("ParallelRaytracer", Logger::ERROR, "ParallelRaytracer",
            "numberOfThreads must be > 0");
        throw VSDKFatalException("numberOfThreads must be > 0");
    }
}

ParallelRaytracer::~ParallelRaytracer()
{
    dispose();
}

int ParallelRaytracer::availableProcessors()
{
#ifdef VITRAL_WITH_POSIX_THREADS
    long cpuCount = sysconf(_SC_NPROCESSORS_ONLN);
    return cpuCount > 0 ? (int)cpuCount : 1;
#else
    // Without POSIX threads the pool runs each task in the caller
    return 1;
#endif
}

int ParallelRaytracer::getNumberOfThreads() const
{
    return numberOfThreads;
}

java::ExecutorService* ParallelRaytracer::getExecutorService()
{
    if ( executorService == 0 ) {
        executorService = java::Executors::newFixedThreadPool(numberOfThreads);
    }
    return executorService;
}

void* ParallelRaytracer::progressConsumerMain(void* arg)
{
    ParallelRaytracerConsumerThreadData* data =
        reinterpret_cast<ParallelRaytracerConsumerThreadData*>(arg);
    data->consumer->run();
    return 0;
}

long long ParallelRaytracer::calculateTotalProgressElements(
    const java::ArrayList<RasterTileArea>& generatedTiles)
{
    long long totalElements = 0;

    for ( long int i = 0; i < generatedTiles.size(); i++ ) {
        totalElements += generatedTiles.get(i).getDy();
    }
    return totalElements;
}

void ParallelRaytracer::execute(
    RGBImageUncompressed* resultingImage,
    const RendererConfiguration* rendererConfiguration,
    SimpleSceneSnapshot* sceneSnapshot,
    bool reportProgress)
{
    if ( resultingImage->getXSize() <= 0 || resultingImage->getYSize() <= 0 ) {
        return;
    }
    RasterTileGenerator tileGenerator(
        RasterTileGenerationStrategy::LINEAR,
        resultingImage,
        resultingImage->getXSize(),
        resultingImage->getYSize(),
        numberOfThreads * BANDS_PER_THREAD);
    java::ArrayList<RasterTileArea> generatedTiles = tileGenerator.getTiles();
    java::ConcurrentLinkedQueue<RasterTileArea> pendingTiles(generatedTiles);

    java::ConcurrentLinkedQueue<ParallelProgressMonitorEvent> progressEvents;
    ParallelProgressMonitorProducer producer(&progressEvents);
    ParallelProgressMonitorConsumer consumer(&progressEvents);
    ProgressMonitor* progressReporter = 0;

#ifdef VITRAL_WITH_POSIX_THREADS
    pthread_t consumerThread;
    ParallelRaytracerConsumerThreadData consumerData;
    consumerData.consumer = &consumer;
#endif

    if ( reportProgress ) {
        printf("Starting parallel raytracing with %d threads.\n", numberOfThreads);
        producer.init(calculateTotalProgressElements(generatedTiles));
        progressReporter = &producer;
#ifdef VITRAL_WITH_POSIX_THREADS
        pthread_create(&consumerThread, 0, &progressConsumerMain, &consumerData);
#endif
    }

    java::ArrayList<java::Future<java::Void> > futures;
    for ( int i = 0; i < numberOfThreads; i++ ) {
        futures.add(getExecutorService()->submit(new ParallelRaytracerTileWorker(
            &pendingTiles,
            resultingImage,
            rendererConfiguration,
            sceneSnapshot,
            progressReporter)));
    }

    bool failed = false;
    java::String failure;
    for ( long int i = 0; i < futures.size(); i++ ) {
        try {
            futures[i].get();
        }
        catch ( const std::exception& e ) {
            if ( !failed ) {
                failed = true;
                failure = java::String("Parallel raytracing failed: ").concat(e.what());
            }
        }
    }

    if ( reportProgress ) {
        producer.finish();
#ifdef VITRAL_WITH_POSIX_THREADS
        pthread_join(consumerThread, 0);
#else
        consumer.run();
#endif
    }

    if ( failed ) {
        Logger::reportMessage("ParallelRaytracer", Logger::ERROR, "execute", failure.toCString());
        throw VSDKFatalException(failure);
    }
    if ( !pendingTiles.isEmpty() ) {
        Logger::reportMessage("ParallelRaytracer", Logger::ERROR, "execute",
            "Parallel raytracing finished with pending tiles");
        throw VSDKFatalException("Parallel raytracing finished with pending tiles");
    }
}

void ParallelRaytracer::dispose()
{
    if ( executorService != 0 ) {
        executorService->shutdownNow();
        delete executorService;
        executorService = 0;
    }
}
