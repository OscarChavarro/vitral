#include <cmath>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "model/SceneLightFactory.h"

namespace {
/// Random positions keep this fraction of the viewport as margin, so the
/// light gizmo is not cut by the viewport borders: |u|, |v| <= 0.4
const double MAX_SCREEN_OFFSET = 0.4;
/// The first light is placed at the upper left quadrant of the viewport
const double FIRST_LIGHT_U = -0.25;
const double FIRST_LIGHT_V = 0.25;
/// Depth range for random lights, as a factor of the camera focal
/// distance, so lights are created near the region being looked at
const double MIN_DEPTH_FACTOR = 0.5;
const double MAX_DEPTH_FACTOR = 1.5;
/// Light colors have all their channels in [MIN_LIGHT_CHANNEL, 1]
const double MIN_LIGHT_CHANNEL = 0.6;
}

SceneLightFactory::SceneLightFactory()
{
}

SceneLightFactory::SceneLightFactory(long long randomSeed)
    : random(randomSeed)
{
}

PointLight* SceneLightFactory::createLight(
    const java::ArrayList<Light*>& existingLights, ViewportSet* viewportSet)
{
    bool first = existingLights.size() == 0;
    Viewport* viewport = chooseViewport(viewportSet, first);

    if ( viewport == nullptr ) {
        return nullptr;
    }
    Camera* camera = viewport->getActiveCamera();

    if ( first ) {
        return new PointLight(
            pointInFrustum(camera, FIRST_LIGHT_U, FIRST_LIGHT_V,
                calculateFocalDistance(camera)),
            ColorRgb(1, 1, 1));
    }
    double u = randomInRange(-MAX_SCREEN_OFFSET, MAX_SCREEN_OFFSET);
    double v = randomInRange(-MAX_SCREEN_OFFSET, MAX_SCREEN_OFFSET);
    double focal = calculateFocalDistance(camera);
    double depth = randomInRange(
        clampDepth(camera, focal * MIN_DEPTH_FACTOR),
        clampDepth(camera, focal * MAX_DEPTH_FACTOR));

    return new PointLight(pointInFrustum(camera, u, v, depth),
        createRandomLightColor());
}

ColorRgb SceneLightFactory::createRandomLightColor()
{
    // Arguments are evaluated in a fixed order, as in Java
    double r = randomInRange(MIN_LIGHT_CHANNEL, 1.0);
    double g = randomInRange(MIN_LIGHT_CHANNEL, 1.0);
    double b = randomInRange(MIN_LIGHT_CHANNEL, 1.0);
    return ColorRgb(r, g, b);
}

Viewport* SceneLightFactory::chooseViewport(ViewportSet* viewportSet,
                                            bool first)
{
    Viewport* selected = viewportSet->getSelectedViewport();

    if ( first && selected != nullptr && selected->isActive() ) {
        return selected;
    }
    int activeCount = viewportSet->countActiveViewports();

    if ( activeCount < 1 ) {
        return nullptr;
    }
    int chosen = first ? 0 : random.nextInt(activeCount);
    const java::ArrayList<Viewport*>& viewports = viewportSet->getViewports();
    long i;

    for ( i = 0; i < viewports.size(); i++ ) {
        Viewport* viewport = viewports.get(i);
        if ( !viewport->isActive() ) continue;
        if ( chosen == 0 ) {
            return viewport;
        }
        chosen--;
    }
    return nullptr;
}

double SceneLightFactory::calculateFocalDistance(const Camera* camera)
{
    return camera->getFocusedPosition().subtract(
        camera->getPosition()).length();
}

double SceneLightFactory::clampDepth(const Camera* camera, double depth)
{
    double nearDistance = camera->getNearPlaneDistance();
    double farDistance = camera->getFarPlaneDistance();

    return std::fmax(nearDistance, std::fmin(farDistance, depth));
}

Vector3Dd SceneLightFactory::pointInFrustum(Camera* camera, double u,
                                            double v, double depth)
{
    // Calculates the world point that projects at a given place of the
    // viewport of a camera, at a given depth along the camera viewing
    // direction. This is valid for perspective and orthogonal cameras, as
    // it uses the camera's own ray generation, and a point with a depth
    // between the near and far planes is inside the frustum by construction.
    camera->updateVectors();

    double width = camera->getViewportXSize();
    double height = camera->getViewportYSize();
    // std::floor(v + 0.5) mimics Java's Math.round
    int x = (int)std::floor(width * (0.5 + u) + 0.5);
    int y = (int)std::floor(height * (0.5 - v) - 1 + 0.5);
    Ray ray = camera->generateRay(x, y);
    Vector3Dd front = camera->getFront().normalized();
    double alongFront = ray.getDirection().dotProduct(front);

    return ray.getOrigin().add(ray.getDirection().multiply(depth / alongFront));
}

double SceneLightFactory::randomInRange(double min, double max)
{
    return min + random.nextDouble() * (max - min);
}
