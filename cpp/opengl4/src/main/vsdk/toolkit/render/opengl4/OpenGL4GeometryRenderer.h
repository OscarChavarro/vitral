#ifndef __OPEN_GL_4_GEOMETRY_RENDERER__
#define __OPEN_GL_4_GEOMETRY_RENDERER__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

class Camera;
class Geometry;
class Light;
class RendererConfiguration;
class SimpleMaterial;
class RGBImageUncompressed;

/**
Renders the geometries of the toolkit with the GL4 pipeline. Each geometry
has its own `OpenGL4*Renderer`, in the directory that mirrors the package of
the geometry (`curve`, `surface`, `volume`), that tessellates it into a mesh
(`meshKey` and `buildMesh`). This class dispatches the drawing to them, and
keeps the meshes (cached by their keys), that are drawn by
`OpenGL4MeshRenderer`, so every geometry honors every bit of the
`RendererConfiguration` in the same way (surfaces, wires, points, normals,
bounding volume, texture, bump map and shading).

Some geometries are drawn by their renderers in their own way: a `Sphere` by
`OpenGL4SphereRenderer`, a `ParametricCurve` (it has no surface) as lines by
`OpenGL4ParametricCurveRenderer`, a `TriangleMeshGroup` mesh by mesh, a
`PlanarHalfSpace` by its boundary plane, and a `PolyhedralBoundedSolid` by
`OpenGL4PolyhedralBoundedSolidRenderer` (without texture nor normal map).
Any other volume is drawn, as the last resort, from its boundary
representation (see `OpenGL4VolumeRenderer`).

C++ counterpart of Java's `Jogl4GeometryRenderer`. C++ port note: the OpenGL
context is the current one (there is no `gl` parameter).
*/
class OpenGL4GeometryRenderer {
public:
    /**
    @param geometry a geometry
    @return true if this renderer can draw the geometry
    */
    static bool isSupported(Geometry* geometry);

    /**
    Draws a geometry lit by a single light.
    See the overload taking a list of lights.

    @param light light of the scene, or null to use a light at the camera
    */
    static void draw(Geometry* geometry, Camera* camera, Light* light,
        const SimpleMaterial* material, const RendererConfiguration* quality,
        RGBImageUncompressed* textureMap, RGBImageUncompressed* normalMap,
        const Matrix4x4d& localTransform);

    /**
    Draws a geometry.

    @param geometry geometry to draw
    @param camera camera that views the geometry
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the geometry
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from geometry space to world space
    */
    static void draw(Geometry* geometry, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    Releases the OpenGL resources of the meshes. PRE: the OpenGL context that
    created them is current.
    */
    static void dispose();
};

#endif
