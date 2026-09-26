#ifndef __OPEN_GL_1_MESH_BUILDER__
#define __OPEN_GL_1_MESH_BUILDER__

#include <vector>

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"

/**
Accumulates the triangles of a tessellated surface and builds an
`OpenGL1MeshRenderer::Mesh` from them. Unlike `OpenGL4MeshBuilder`, no
tangents nor binormals are built, as there is no bump mapping in OpenGL 1.2.
*/
class OpenGL1MeshBuilder {
public:
    explicit OpenGL1MeshBuilder(double characteristicSize);

    /**
    Adds one triangle; vertices are given counterclockwise seen from the side
    the normals point to.
    */
    void addTriangle(
        const Vector3Dd& p0, const Vector3Dd& n0, double u0, double v0,
        const Vector3Dd& p1, const Vector3Dd& n1, double u1, double v1,
        const Vector3Dd& p2, const Vector3Dd& n2, double u2, double v2);

    /**
    Adds a quad as two triangles; vertices are given counterclockwise seen
    from the side the normals point to.
    */
    void addQuad(
        const Vector3Dd& p0, const Vector3Dd& n0, double u0, double v0,
        const Vector3Dd& p1, const Vector3Dd& n1, double u1, double v1,
        const Vector3Dd& p2, const Vector3Dd& n2, double u2, double v2,
        const Vector3Dd& p3, const Vector3Dd& n3, double u3, double v3);

    /**
    Adds the side of a frustum of cone around the Z axis, from the ring
    (z0, r0) to the ring (z1, r1).
    */
    void addFrustum(double z0, double r0, double z1, double r1, int slices);

    /**
    Adds a flat ring (or disk, when the inner radius is zero) perpendicular
    to the Z axis.
    */
    void addDisk(double z, double innerRadius, double outerRadius,
                 bool facingUp, int slices);

    /**
    Makes the built mesh visible from both sides, as needed by open surfaces
    (i.e. a patch or a height field): each triangle gets a back side copy
    with the opposite winding and normals.
    */
    void setDoubleSided(bool doubleSided);

    /**
    @return a new mesh with the triangles added, owned by the caller
    */
    OpenGL1MeshRenderer::Mesh* build();

private:
    /// x y z, nx ny nz, u v of each vertex
    static const int VERTEX_SIZE = 8;
    std::vector<float> vertices;
    double characteristicSize;
    bool doubleSided;

    void addVertex(const Vector3Dd& p, const Vector3Dd& n, double u, double v);
    void addBackSides(int frontCount);
};

#endif
