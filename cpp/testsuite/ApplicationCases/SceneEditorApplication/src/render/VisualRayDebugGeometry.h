#ifndef __VISUAL_RAY_DEBUG_GEOMETRY__
#define __VISUAL_RAY_DEBUG_GEOMETRY__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "render/RenderPrimitive.h"

class ApplicationModel;
class Ray;
class RendererConfiguration;
class Scene;

/**
Builds the geometry presenting the visual debug ray of the application
model: the ray, its normal at the hit point and its successive reflections.
It does not draw: renderers of each technology draw the `RenderPrimitive`s
it builds (see `buildPrimitives`).
*/
class VisualRayDebugGeometry {
private:
    ApplicationModel* model;
    Scene* scene;
    RendererConfiguration* rendererConfiguration;
    SimpleMaterial visualDebugMaterial;

    void addSegment(java::ArrayList<RenderPrimitive>& primitives,
                    const Vector3Dd& start, const Vector3Dd& end,
                    bool follow, double w, double tip,
                    const SimpleMaterial& segmentMaterial) const;
    void addRay(java::ArrayList<RenderPrimitive>& primitives, const Ray& ray,
                int level) const;

    VisualRayDebugGeometry(const VisualRayDebugGeometry& other);
    VisualRayDebugGeometry& operator=(const VisualRayDebugGeometry& other);

public:
    explicit VisualRayDebugGeometry(ApplicationModel* model);
    virtual ~VisualRayDebugGeometry();

    /**
    @return the rendering configuration for the primitives
    */
    RendererConfiguration* getRendererConfiguration() const;

    /**
    @return the primitives presenting the visual debug ray, or an empty list
    if it is not enabled in the application model
    */
    java::ArrayList<RenderPrimitive> buildPrimitives() const;
};

#endif
