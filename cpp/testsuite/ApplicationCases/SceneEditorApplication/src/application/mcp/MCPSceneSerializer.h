#ifndef __MCP_SCENE_SERIALIZER__
#define __MCP_SCENE_SERIALIZER__

#include "java/lang/String.h"
#include "java/lang/StringBuilder.h"

class Geometry;
class Light;
class Matrix4x4d;
class ParametricBiCubicPatch;
class ParametricCurve;
class PolyhedralBoundedSolid;
class Scene;
class SimpleBody;
class SimpleMaterial;
class TriangleMesh;

/**
Presents the bodies and lights of the scene as JSON, for the tools
`scene.describe` and `scene.serialize` of the automation service (see
`MCP.md`).

Each body reports its placement (position, scale and rotation) and the
parameters of its geometry, the ones its constructor takes, so it can be
created again: i.e. the radii and height of a `Cone`, or the radii of a
`Torus`. Geometries defined by data rather than by a few numbers (boundary
representations, meshes, curves, patches) report only a summary of it in the
description, and the full data when serialized.

C++ port note: numbers are written as `java::StringBuilder` writes them,
which may differ from Java's `Double.toString` in the last digits shown.
*/
class MCPSceneSerializer {
public:
    /**
    @param scene scene to present (referenced)
    @param full false for a description (parameters, and summaries of the
    data of the geometries defined by data), true for a serialization (also
    the materials, and the full data of every geometry)
    */
    MCPSceneSerializer(Scene* scene, bool full);

    /**
    @param index index of the only body to present, or a negative value for
    all of them (lights are presented only in that case)
    @return the bodies and lights as JSON
    @throws java::IllegalArgumentException if the index is out of range
    */
    java::String toJson(int index);

private:
    Scene* scene;
    bool full;

    void appendBody(java::StringBuilder& sb, int index, SimpleBody* body);
    static void appendRotation(java::StringBuilder& sb,
                               const Matrix4x4d& rotation);
    static java::String degrees(double radians);
    void appendParameters(java::StringBuilder& sb, Geometry* geometry);
    void appendCurve(java::StringBuilder& sb, const ParametricCurve& curve);
    static java::String curveTypeName(int type);
    void appendPatch(java::StringBuilder& sb, ParametricBiCubicPatch& patch);
    void appendSolid(java::StringBuilder& sb, PolyhedralBoundedSolid& solid);
    void appendMesh(java::StringBuilder& sb, TriangleMesh& mesh);
    static void appendMaterial(java::StringBuilder& sb,
                               const SimpleMaterial* material);
    void appendLight(java::StringBuilder& sb, int index, Light* light);
};

#endif
