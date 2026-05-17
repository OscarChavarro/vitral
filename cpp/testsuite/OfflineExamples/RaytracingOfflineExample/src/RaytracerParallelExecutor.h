#ifndef RAYTRACING_OFFLINE_RAYTRACERPARALLELEXECUTOR_H
#define RAYTRACING_OFFLINE_RAYTRACERPARALLELEXECUTOR_H

#include "RaytracerExecutor.h"

class RaytracerParallelExecutor : public RaytracerExecutor {
public:
    virtual void run(SimpleRaytracer* visualizationEngine,
                     RGBImageUncompressed* resultingImage,
                     const RendererConfiguration* rendererConfiguration,
                     SimpleSceneSnapshot* sceneSnapshot,
                     ProgressMonitor* reporter) override;
};

#endif
