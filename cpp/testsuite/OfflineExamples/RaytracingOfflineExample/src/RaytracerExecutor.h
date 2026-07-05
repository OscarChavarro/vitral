#ifndef __RAYTRACEREXECUTOR__
#define __RAYTRACEREXECUTOR__

class SimpleRaytracer;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleSceneSnapshot;
class ProgressMonitor;

class RaytracerExecutor {
public:
    virtual ~RaytracerExecutor() {}
    virtual void run(SimpleRaytracer* visualizationEngine,
                     RGBImageUncompressed* resultingImage,
                     const RendererConfiguration* rendererConfiguration,
                     SimpleSceneSnapshot* sceneSnapshot,
                     ProgressMonitor* reporter) = 0;
};

#endif
