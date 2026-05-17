#ifndef RAYTRACING_OFFLINE_RAYTRACEREXECUTOR_H
#define RAYTRACING_OFFLINE_RAYTRACEREXECUTOR_H

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
