#ifndef __OGL1_SUBDIV_BEZ_PATCH_RNDR__
#define __OGL1_SUBDIV_BEZ_PATCH_RNDR__

#include "java/lang/String.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"
class SubdividedBezierPatch;

/**
Builds the OpenGL 1.2 meshes of subdivided Bezier patches, drawn by
`OpenGL1GeometryRenderer` (as `Jogl4SubdividedBezierPatchRenderer` of the
Java OpenGL 4 port): the tessellation of the Bezier surface of the patch by
`BezierPatchTessellator`, visible from both sides.
*/
class OpenGL1SubdividedBezierPatchRenderer {
private:
    OpenGL1SubdividedBezierPatchRenderer();

public:
    /// Steps along u and v of the tessellation
    enum { TESSELLATION_STEPS = 24 };

    /**
    @return a string that identifies the mesh of the patch, by identity
    */
    static String meshKey(SubdividedBezierPatch& patch);

    /**
    @return a new mesh, owned by the caller, or null if the patch can not be
    tessellated
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(SubdividedBezierPatch& patch);
};

#endif
