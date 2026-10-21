#ifndef __RAYTRACER_RENDERER__
#define __RAYTRACER_RENDERER__
#include <stdint.h>

class Camera;
class ProgressMonitor;
class RendererConfiguration;
class SimpleScene;

class RaytracerRenderer {
public:
    static int render(const char *path, int width, int height, int parallel, uint8_t *rgb, ProgressMonitor *reporter,
                      double *seconds, char *error, unsigned errorSize);

private:
    static void optimizeRendererConfigurationForScene(SimpleScene *scene, RendererConfiguration *rendererConfiguration);
    static void fitCameraToImage(Camera *camera, int width, int height);
};

#endif
