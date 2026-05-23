#include "RaytracerParallelExecutor.h"

#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"
#include "vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorConsumer.h"
#include "vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorEvent.h"
#include "vsdk/toolkit/gui/feedback/parallel/ParallelProgressMonitorProducer.h"
#include "java/util/concurrent/Callable.h"
#include "java/util/concurrent/ConcurrentLinkedQueue.h"
#include "java/util/concurrent/ExecutorService.h"
#include "java/util/concurrent/Executors.h"
#include "java/util/concurrent/Future.h"
#include "java/util/concurrent/Void.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/render/SimpleRaytracer.h"
#include "vsdk/toolkit/render/Tile.h"
#include "vsdk/toolkit/render/TileGenerationStrategy.h"
#include "vsdk/toolkit/render/TileGenerator.h"

#include <pthread.h>
#include <unistd.h>
#include <iostream>
#include <vector>
#include <stdexcept>

namespace {

struct ConsumerThreadData {
    ParallelProgressMonitorConsumer* consumer;
};

void* progressConsumerMain(void* arg)
{
    ConsumerThreadData* data = reinterpret_cast<ConsumerThreadData*>(arg);
    data->consumer->run();
    return 0;
}

class TileWorker : public java::Callable<java::Void> {
private:
    java::ConcurrentLinkedQueue<Tile>* pendingTiles;
    RGBImageUncompressed* resultingImage;
    const RendererConfiguration* rendererConfiguration;
    SimpleSceneSnapshot* sceneSnapshot;
    ProgressMonitor* progressReporter;

public:
    TileWorker(java::ConcurrentLinkedQueue<Tile>* pendingTiles,
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
        Tile tile(resultingImage, 0, 0, resultingImage->getXSize(), resultingImage->getYSize());
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

long long calculateTotalProgressElements(const std::vector<Tile>& generatedTiles)
{
    long long total = 0;
    for (size_t i = 0; i < generatedTiles.size(); i++) {
        total += generatedTiles[i].getDy();
    }
    return total;
}

}

void RaytracerParallelExecutor::run(SimpleRaytracer*,
                                    RGBImageUncompressed* resultingImage,
                                    const RendererConfiguration* rendererConfiguration,
                                    SimpleSceneSnapshot* sceneSnapshot,
                                    ProgressMonitor*)
{
#ifdef VITRAL_WITH_POSIX_THREADS
    long cpuCount = sysconf(_SC_NPROCESSORS_ONLN);
    int numberOfThreads = cpuCount > 0 ? (int)cpuCount : 1;
#else
    int numberOfThreads = 1;
#endif

    TileGenerator tileGenerator(TileGenerationStrategy::LINEAR,
                                resultingImage,
                                resultingImage->getXSize(),
                                resultingImage->getYSize(),
                                numberOfThreads);
    std::cout << "Starting parallel raytracing with " << numberOfThreads << " threads." << std::endl;

    std::vector<Tile> generatedTiles = tileGenerator.getTiles();
    java::ConcurrentLinkedQueue<Tile> pendingTiles(generatedTiles);
    java::ExecutorService* executorService =
        java::Executors::newFixedThreadPool(numberOfThreads);

    java::ConcurrentLinkedQueue<ParallelProgressMonitorEvent> progressEvents;
    ParallelProgressMonitorProducer producer(&progressEvents);
    ParallelProgressMonitorConsumer consumer(&progressEvents);

    producer.init(calculateTotalProgressElements(generatedTiles));

#ifdef VITRAL_WITH_POSIX_THREADS
    pthread_t consumerThread;
    ConsumerThreadData consumerData;
    consumerData.consumer = &consumer;
    pthread_create(&consumerThread, 0, &progressConsumerMain, &consumerData);
#else
    consumer.run();
#endif

    std::vector<java::Future<java::Void> > futures;
    futures.reserve((size_t)numberOfThreads);
    for (int i = 0; i < numberOfThreads; i++) {
        futures.push_back(executorService->submit(new TileWorker(
            &pendingTiles,
            resultingImage,
            rendererConfiguration,
            sceneSnapshot,
            &producer)));
    }

    for (size_t i = 0; i < futures.size(); i++) {
        futures[i].get();
    }

    producer.finish();
    executorService->shutdownNow();
    delete executorService;

#ifdef VITRAL_WITH_POSIX_THREADS
    pthread_join(consumerThread, 0);
#endif

    if ( !pendingTiles.isEmpty() ) {
        throw std::runtime_error("Parallel raytracing finished with pending tiles");
    }
}
