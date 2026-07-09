#ifndef __RAYTRACER_SERIAL_EXECUTOR__
#define __RAYTRACER_SERIAL_EXECUTOR__

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
