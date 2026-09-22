#ifndef __PARALLEL_RAYTRACER__
#define __PARALLEL_RAYTRACER__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/render/raytracing/RasterTileArea.h"

namespace java {
class ExecutorService;
}
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleSceneSnapshot;

/**
Raytraces an image with as many threads as processors are available to the
process. The image is split in horizontal bands (see `RasterTileGenerator`),
several per thread, and each thread takes the next pending band when it
finishes one, so threads stay busy even if some areas of the image are much
more expensive than others. Each thread uses its own `SimpleRaytracer` over the
same (read only) scene snapshot, and writes disjoint rows of the image.

The threads are created once and reused by every call, so this class suits
interactive use (one image per frame). `dispose` (and the destructor) stops
them.

C++ counterpart of Java's `vsdk.toolkit.render.raytracing.ParallelRaytracer`.
*/
class ParallelRaytracer {
  private:
    /** Bands per thread: more bands balance the load better */
    static const int BANDS_PER_THREAD = 8;

    int numberOfThreads;
    java::ExecutorService* executorService;

    java::ExecutorService* getExecutorService();
    static int availableProcessors();
    static long long calculateTotalProgressElements(
        const java::ArrayList<RasterTileArea>& generatedTiles);
    static void* progressConsumerMain(void* arg);

    // Threads are owned: copying would share them
    ParallelRaytracer(const ParallelRaytracer& other);
    ParallelRaytracer& operator=(const ParallelRaytracer& other);

  public:
    /**
    Creates a raytracer with one thread per available processor.
    */
    ParallelRaytracer();

    /**
    @param numberOfThreads number of threads to render with (at least 1)
    */
    explicit ParallelRaytracer(int numberOfThreads);

    ~ParallelRaytracer();

    /**
    @return number of threads used to render
    */
    int getNumberOfThreads() const;

    /**
    Raytraces the scene into the whole image, returning when it is complete.
    @param resultingImage image to fill; its size gives the resolution
    @param rendererConfiguration quality settings
    @param sceneSnapshot scene to render, seen from its camera snapshot
    @param reportProgress true to report in the console the number of
    threads and the progress
    */
    void execute(
        RGBImageUncompressed* resultingImage,
        const RendererConfiguration* rendererConfiguration,
        SimpleSceneSnapshot* sceneSnapshot,
        bool reportProgress);

    /**
    Stops the threads. The next `execute` creates them again.
    */
    void dispose();
};

#endif
