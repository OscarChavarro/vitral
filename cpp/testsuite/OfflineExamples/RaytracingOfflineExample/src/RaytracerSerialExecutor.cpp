#include "RaytracerSerialExecutor.h"
#include "vsdk/toolkit/gui/feedback/ProgressMonitor.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleSceneSnapshot.h"
#include "vsdk/toolkit/render/raytracing/SimpleRaytracer.h"
void RaytracerSerialExecutor::run(SimpleRaytracer* visualizationEngine,
                                  RGBImageUncompressed* resultingImage,
                                  const RendererConfiguration* rendererConfiguration,
                                  SimpleSceneSnapshot* sceneSnapshot,
                                  ProgressMonitor* reporter)
{
    visualizationEngine->execute(resultingImage, rendererConfiguration, sceneSnapshot, reporter, (ZBuffer*)0);
}
