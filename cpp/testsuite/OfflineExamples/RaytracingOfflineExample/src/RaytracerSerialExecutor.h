#ifndef __RAYTRACERSERIALEXECUTOR__
#define __RAYTRACERSERIALEXECUTOR__

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
