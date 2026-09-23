#ifndef __SCENE_EDITOR_SCENE__
#define __SCENE_EDITOR_SCENE__

#include "java/util/ArrayList.h"

class Camera;
class CubemapBackground;
class FixedBackground;
class Geometry;
class ParallelRaytracer;
class Ray;
class RayHit;
class RendererConfiguration;
class RGBImageUncompressed;
class SelectionSet;
class SimpleBackground;
class SimpleBody;
class SimpleBodyGroup;
class SimpleMaterial;
class SimpleScene;
class ZBuffer;

/**
Scene edited by the application: the vitral `SimpleScene` (bodies, lights,
scene cameras and backgrounds), the editor camera, the backgrounds that can
be selected, the debug body groups and the selection sets over them.

C++ port notes: fields are public as in the Java version. The scene owns
everything it references; elements removed from its lists (i.e. by the edit
history, which can put them back) are not deleted.
*/
class Scene {
private:
    int acumObject;
    /// Size factor of light gizmos, shared by rendering and picking
    double lightGizmoScale;

    /// Shared by all scenes: its threads (one per processor) are reused
    static ParallelRaytracer* getRaytracer();
    void raytrace(RGBImageUncompressed* outViewport, ZBuffer* outDepth,
                  bool interactiveReport);

    Scene(const Scene& other);
    Scene& operator=(const Scene& other);

public:
    SimpleScene* scene;

    //- 1. Camera ----------------------------------------------------------
    Camera* camera;
    Camera* activeCamera;

    //- 3. Background ------------------------------------------------------
    SimpleBackground* simpleBackground;
    CubemapBackground* cubemapBackground;
    FixedBackground* fixedBackground;
    int selectedBackground;

    //- 4. Objects ---------------------------------------------------------
    /// Test corridor is drawn by the rendering technology, not stored here
    bool showCorridor;
    java::ArrayList<SimpleBodyGroup*> debugThingGroups;

    SelectionSet* selectedThings;
    SelectionSet* selectedLights;
    SelectionSet* selectedDebugThingGroups;

    // Others
    RendererConfiguration* qualityTemplate;

    Scene();
    virtual ~Scene();

    /**
    @return size factor of light gizmos, used both to draw and to pick them
    */
    double getLightGizmoScale() const;

    /**
    @param lightGizmoScale new size factor of light gizmos
    */
    void setLightGizmoScale(double lightGizmoScale);

    bool buildCubemap();
    bool buildFixedmap();
    static SimpleMaterial defaultMaterial();

    /**
    @param g geometry of the new body; the body takes its ownership
    @return the new body, owned by the scene
    */
    SimpleBody* addThing(Geometry* g);
    bool doIntersectionFirstHit(const Ray& r, RayHit* info);

    /**
    Selects the next background, cycling over the three available ones.
    */
    void rotateBackground();
    void activateSelectedBackground();
    void print();

    /**
    Raytraces the scene from the active camera, reporting the progress and
    the time in the console, and exports the result to `output.jpg`.
    @param outViewport image to fill; its size gives the resolution
    */
    void raytrace(RGBImageUncompressed* outViewport);

    /**
    Raytraces the scene from the active camera, silently: used to present a
    viewport in CPU render mode, once per frame.
    @param outViewport image to fill; its size gives the resolution
    */
    void raytraceViewport(RGBImageUncompressed* outViewport);

    /**
    Raytraces the scene from the active camera, silently, exporting also the
    depth of each pixel as OpenGL would store it for the same camera, so the
    image can be composited with rasterized elements (grid, gizmos...).
    @param outViewport image to fill; its size gives the resolution
    @param outDepth depth buffer of the size of the image to fill with
    window space depth values in [0, 1]
    */
    void raytraceViewport(RGBImageUncompressed* outViewport,
                          ZBuffer* outDepth);
};

#endif
