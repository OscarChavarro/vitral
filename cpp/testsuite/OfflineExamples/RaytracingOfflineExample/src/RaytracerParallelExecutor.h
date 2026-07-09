#ifndef __RAYTRACER_PARALLEL_EXECUTOR__
#define __RAYTRACER_PARALLEL_EXECUTOR__

#include "java/util/ArrayList.h"
#include "RaytracerExecutor.h"
#include "vsdk/toolkit/render/raytracing/RasterTileArea.h"
class RaytracerParallelExecutor : public RaytracerExecutor {
public:
    virtual void run(SimpleRaytracer* visualizationEngine,
                     RGBImageUncompressed* resultingImage,
                     const RendererConfiguration* rendererConfiguration,
                     SimpleSceneSnapshot* sceneSnapshot,
                     ProgressMonitor* reporter) override;

private:
    static void* progressConsumerMain(void* arg);
    static long long calculateTotalProgressElements(const java::ArrayList<RasterTileArea>& generatedTiles);
};

#endif
