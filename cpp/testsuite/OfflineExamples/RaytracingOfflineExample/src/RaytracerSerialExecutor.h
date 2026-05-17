#ifndef RAYTRACING_OFFLINE_RAYTRACERSERIALEXECUTOR_H
#define RAYTRACING_OFFLINE_RAYTRACERSERIALEXECUTOR_H

#include "RaytracerExecutor.h"

class RaytracerSerialExecutor : public RaytracerExecutor {
public:
    virtual void run(SimpleRaytracer* visualizationEngine,
                     RGBImageUncompressed* resultingImage,
                     const RendererConfiguration* rendererConfiguration,
                     SimpleSceneSnapshot* sceneSnapshot,
                     ProgressMonitor* reporter) override;
};

#endif
