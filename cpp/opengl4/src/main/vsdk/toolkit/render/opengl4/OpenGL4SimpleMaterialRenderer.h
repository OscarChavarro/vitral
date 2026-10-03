#ifndef __OPEN_GL_4_SIMPLE_MATERIAL_RENDERER__
#define __OPEN_GL_4_SIMPLE_MATERIAL_RENDERER__

#include "vsdk/toolkit/environment/material/SimpleMaterial.h"

/**
Keeps the material used by the renderers that take it from the active one
instead of as a parameter (i.e. `OpenGL4PolyhedralBoundedSolidRenderer`).

C++ counterpart of Java's `Jogl4SimpleMaterialRenderer`.
*/
class OpenGL4SimpleMaterialRenderer {
public:
    /**
    @param material material to activate (copied), or null for the default
    material
    */
    static void activate(const SimpleMaterial* material);

    /**
    @return a copy of the active material
    */
    static SimpleMaterial getActiveMaterial();

private:
    static SimpleMaterial activeMaterial;

    OpenGL4SimpleMaterialRenderer() {}
};

#endif
