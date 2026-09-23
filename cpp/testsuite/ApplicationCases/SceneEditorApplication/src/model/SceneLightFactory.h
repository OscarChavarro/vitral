#ifndef __SCENE_LIGHT_FACTORY__
#define __SCENE_LIGHT_FACTORY__

#include "java/util/ArrayList.h"
#include "java/util/Random.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Camera;
class Light;
class PointLight;
class Viewport;
class ViewportSet;

/**
Creates the point lights of the scene editor, following these rules:
  - A scene starts without lights.
  - Every new light is placed inside the view volume (frustum) of the camera
    of one of the visible viewports, so its gizmo can be seen when created.
  - The first light of the scene is white and is placed at a fixed screen
    position of the selected viewport.
  - Every other light gets a random light color and a random position, inside
    the frustum of a randomly chosen visible viewport.
This is a plain model class: it knows nothing about the GUI or rendering
technology.
*/
class SceneLightFactory {
private:
    java::Random random;

    ColorRgb createRandomLightColor();
    Viewport* chooseViewport(ViewportSet* viewportSet, bool first);
    static double calculateFocalDistance(const Camera* camera);
    static double clampDepth(const Camera* camera, double depth);
    static Vector3Dd pointInFrustum(Camera* camera, double u, double v,
                                    double depth);
    double randomInRange(double min, double max);

public:
    SceneLightFactory();

    /**
    @param randomSeed seed of the source of randomness, for reproducible
    results (the Java version receives the `Random` itself)
    */
    explicit SceneLightFactory(long long randomSeed);

    /**
    Builds the next point light for a scene. The light is not added to the
    scene: that is up to the caller.
    @param existingLights lights currently in the scene, used to know if the
    new one is the first
    @param viewportSet viewports whose cameras define where a light can be
    created
    @return a new light (owned by the caller), or null if there is no camera
    to place it in view
    */
    PointLight* createLight(const java::ArrayList<Light*>& existingLights,
                            ViewportSet* viewportSet);
};

#endif
