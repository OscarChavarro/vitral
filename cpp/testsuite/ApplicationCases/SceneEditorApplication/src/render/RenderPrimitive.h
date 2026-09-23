#ifndef __RENDER_PRIMITIVE__
#define __RENDER_PRIMITIVE__

#include <memory>

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"

class Geometry;

/**
One geometry to draw, with its transformation and material. Lists of
primitives are the technology independent way for the application to
describe feedback geometry (debug entities, editor overlays): each rendering
technology draws them with its own renderer, so they are portable to new
technologies by writing only that renderer.

C++ port note: a primitive is a value; its geometry, which can be shared by
several primitives (as garbage collected references in Java), is held by a
`std::shared_ptr`. Geometries owned by someone else (i.e. a body) are given
with the non owning constructor.
*/
class RenderPrimitive {
private:
    std::shared_ptr<Geometry> geometry;
    Matrix4x4d transform;
    SimpleMaterial material;

public:
    RenderPrimitive() {}

    /**
    @param geometry geometry to draw, shared by its primitives
    @param transform transformation from geometry to world coordinates
    @param material material of the geometry
    */
    RenderPrimitive(const std::shared_ptr<Geometry>& geometry,
                    const Matrix4x4d& transform,
                    const SimpleMaterial& material)
        : geometry(geometry), transform(transform), material(material) {}

    /**
    @param geometry geometry to draw, owned by someone else, that must
    outlive the primitive
    @param transform transformation from geometry to world coordinates
    @param material material of the geometry
    */
    static RenderPrimitive referencing(Geometry* geometry,
                                       const Matrix4x4d& transform,
                                       const SimpleMaterial& material)
    {
        return RenderPrimitive(
            std::shared_ptr<Geometry>(geometry, [](Geometry*) {}),
            transform, material);
    }

    Geometry* getGeometry() const
    {
        return geometry.get();
    }

    const Matrix4x4d& getTransform() const
    {
        return transform;
    }

    const SimpleMaterial& getMaterial() const
    {
        return material;
    }
};

#endif
