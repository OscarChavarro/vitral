#ifndef __RENDER_PRIMITIVE__
#define __RENDER_PRIMITIVE__

#include <stddef.h>

#include "vsdk/toolkit/common/memoryManagement/SharedPointer.h"

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"

#include "vsdk/toolkit/environment/geometry/Geometry.h"

/**
One geometry to draw, with its transformation and material. Lists of
primitives are the technology independent way for the application to
describe feedback geometry (debug entities, editor overlays): each rendering
technology draws them with its own renderer, so they are portable to new
technologies by writing only that renderer.

C++ port note: a primitive is a value; its geometry, which can be shared by
several primitives (as garbage collected references in Java), is held by a
`SharedPointer`. Geometries owned by someone else (i.e. a body) are given
with the non owning constructor (and only referenced).
*/
class RenderPrimitive {
private:
    SharedPointer<Geometry> geometry;
    /// Geometry owned by someone else, or null
    Geometry* referencedGeometry;
    Matrix4x4d transform;
    SimpleMaterial material;

public:
    RenderPrimitive() : referencedGeometry(NULL) {}

    /**
    @param geometry geometry to draw, shared by its primitives
    @param transform transformation from geometry to world coordinates
    @param material material of the geometry
    */
    RenderPrimitive(const SharedPointer<Geometry>& geometry,
                    const Matrix4x4d& transform,
                    const SimpleMaterial& material)
        : geometry(geometry), referencedGeometry(NULL), transform(transform),
          material(material) {}

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
        RenderPrimitive primitive(SharedPointer<Geometry>(), transform,
                                  material);
        primitive.referencedGeometry = geometry;
        return primitive;
    }

    Geometry* getGeometry() const
    {
        return referencedGeometry != NULL ?
            referencedGeometry : geometry.get();
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
