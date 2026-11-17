#ifndef __OGL1_TUBE_RNDR__
#define __OGL1_TUBE_RNDR__

#include "java/lang/String.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"
class OpenGL1MeshBuilder;
class Tube;

/**
Builds the OpenGL 1.2 meshes of tubes, drawn by `OpenGL1GeometryRenderer`
(as `Jogl4TubeRenderer` of the Java OpenGL 4 port): a `Tube` is tessellated
by `buildMesh` into its external wall, its internal wall (with normals
towards the axis) and its annular caps, and the mesh is cached by
`OpenGL1GeometryRenderer`, identified by the parameters of the tube. A
washer (height 0) is a double sided annulus.
*/
class OpenGL1TubeRenderer {
public:
    /// Divisions around the axis
    enum { SLICES = 32 };

private:
    OpenGL1TubeRenderer();

    /**
    The internal wall faces the axis: its quads go clockwise seen from
    outside the hole, with normals pointing to the axis.
    */
    static void addInternalWall(OpenGL1MeshBuilder& builder, double r,
        double h);

public:
    /**
    @param tube a tube
    @return a string that identifies the mesh of the tube
    */
    static String meshKey(const Tube& tube);

    /**
    @param tube tube to tessellate
    @return the mesh of the walls and the caps of the tube, owned by the
    caller
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(const Tube& tube);
};

#endif
