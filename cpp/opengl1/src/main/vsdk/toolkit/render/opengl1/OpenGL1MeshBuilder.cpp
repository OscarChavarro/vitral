#include <cmath>

#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"

OpenGL1MeshBuilder::OpenGL1MeshBuilder(double characteristicSize)
    : characteristicSize(characteristicSize), doubleSided(false)
{
}

void OpenGL1MeshBuilder::addTriangle(
    const Vector3Dd& p0, const Vector3Dd& n0, double u0, double v0,
    const Vector3Dd& p1, const Vector3Dd& n1, double u1, double v1,
    const Vector3Dd& p2, const Vector3Dd& n2, double u2, double v2)
{
    addVertex(p0, n0, u0, v0);
    addVertex(p1, n1, u1, v1);
    addVertex(p2, n2, u2, v2);
}

void OpenGL1MeshBuilder::addQuad(
    const Vector3Dd& p0, const Vector3Dd& n0, double u0, double v0,
    const Vector3Dd& p1, const Vector3Dd& n1, double u1, double v1,
    const Vector3Dd& p2, const Vector3Dd& n2, double u2, double v2,
    const Vector3Dd& p3, const Vector3Dd& n3, double u3, double v3)
{
    addTriangle(p0, n0, u0, v0, p1, n1, u1, v1, p2, n2, u2, v2);
    addTriangle(p0, n0, u0, v0, p2, n2, u2, v2, p3, n3, u3, v3);
}

void OpenGL1MeshBuilder::addFrustum(double z0, double r0, double z1, double r1,
                                    int slices)
{
    double dz = z1 - z0;
    double dr = r0 - r1;
    double normalLength = std::sqrt(dz * dz + dr * dr);

    if ( normalLength < 1e-12 ) {
        return;
    }
    double nz = dr / normalLength;
    double nr = dz / normalLength;

    for ( int i = 0; i < slices; i++ ) {
        double a0 = 2 * M_PI * i / slices;
        double a1 = 2 * M_PI * (i + 1) / slices;
        double c0 = std::cos(a0), s0 = std::sin(a0);
        double c1 = std::cos(a1), s1 = std::sin(a1);
        Vector3Dd n0(nr * c0, nr * s0, nz);
        Vector3Dd n1(nr * c1, nr * s1, nz);
        double u0 = (double)i / slices;
        double u1 = (double)(i + 1) / slices;

        if ( r1 < 1e-12 ) {
            Vector3Dd apex(0, 0, z1);
            Vector3Dd nApex(nr * std::cos((a0 + a1) / 2),
                            nr * std::sin((a0 + a1) / 2), nz);
            addTriangle(
                Vector3Dd(r0 * c0, r0 * s0, z0), n0, u0, 0,
                Vector3Dd(r0 * c1, r0 * s1, z0), n1, u1, 0,
                apex, nApex, (u0 + u1) / 2, 1);
        }
        else {
            addQuad(
                Vector3Dd(r0 * c0, r0 * s0, z0), n0, u0, 0,
                Vector3Dd(r0 * c1, r0 * s1, z0), n1, u1, 0,
                Vector3Dd(r1 * c1, r1 * s1, z1), n1, u1, 1,
                Vector3Dd(r1 * c0, r1 * s0, z1), n0, u0, 1);
        }
    }
}

void OpenGL1MeshBuilder::addDisk(double z, double innerRadius,
                                 double outerRadius, bool facingUp, int slices)
{
    Vector3Dd n(0, 0, facingUp ? 1 : -1);

    for ( int i = 0; i < slices; i++ ) {
        double a0 = 2 * M_PI * i / slices;
        double a1 = 2 * M_PI * (i + 1) / slices;
        double c0 = std::cos(a0), s0 = std::sin(a0);
        double c1 = std::cos(a1), s1 = std::sin(a1);
        Vector3Dd o0(outerRadius * c0, outerRadius * s0, z);
        Vector3Dd o1(outerRadius * c1, outerRadius * s1, z);
        double uo0 = 0.5 + 0.5 * c0, vo0 = 0.5 + 0.5 * s0;
        double uo1 = 0.5 + 0.5 * c1, vo1 = 0.5 + 0.5 * s1;

        if ( innerRadius < 1e-12 ) {
            Vector3Dd center(0, 0, z);
            if ( facingUp ) {
                addTriangle(center, n, 0.5, 0.5, o0, n, uo0, vo0, o1, n, uo1, vo1);
            }
            else {
                addTriangle(center, n, 0.5, 0.5, o1, n, uo1, vo1, o0, n, uo0, vo0);
            }
        }
        else {
            Vector3Dd i0(innerRadius * c0, innerRadius * s0, z);
            Vector3Dd i1(innerRadius * c1, innerRadius * s1, z);
            if ( facingUp ) {
                addQuad(i0, n, 0.5, 0.5, o0, n, uo0, vo0, o1, n, uo1, vo1,
                        i1, n, 0.5, 0.5);
            }
            else {
                addQuad(i0, n, 0.5, 0.5, i1, n, 0.5, 0.5, o1, n, uo1, vo1,
                        o0, n, uo0, vo0);
            }
        }
    }
}

void OpenGL1MeshBuilder::setDoubleSided(bool doubleSided)
{
    this->doubleSided = doubleSided;
}

OpenGL1MeshRenderer::Mesh* OpenGL1MeshBuilder::build()
{
    int frontCount = (int)(vertices.size() / VERTEX_SIZE);

    if ( doubleSided ) {
        addBackSides(frontCount);
    }

    int count = (int)(vertices.size() / VERTEX_SIZE);
    OpenGL1MeshRenderer::Mesh* mesh =
        new OpenGL1MeshRenderer::Mesh(characteristicSize);

    mesh->positions.resize((size_t)count * 3);
    mesh->normals.resize((size_t)count * 3);
    mesh->uvs.resize((size_t)count * 2);
    for ( int i = 0; i < count; i++ ) {
        const float* v = &vertices[(size_t)i * VERTEX_SIZE];
        for ( int c = 0; c < 3; c++ ) {
            mesh->positions[i * 3 + c] = v[c];
            mesh->normals[i * 3 + c] = v[3 + c];
        }
        mesh->uvs[i * 2] = v[6];
        mesh->uvs[i * 2 + 1] = v[7];
    }
    mesh->vertexCount = count;
    mesh->setFrontVertexCount(frontCount);
    return mesh;
}

void OpenGL1MeshBuilder::addVertex(const Vector3Dd& p, const Vector3Dd& n,
                                   double u, double v)
{
    Vector3Dd normal = n.normalized();
    const float vertex[VERTEX_SIZE] = {
        (float)p.x(), (float)p.y(), (float)p.z(),
        (float)normal.x(), (float)normal.y(), (float)normal.z(),
        (float)u, (float)v
    };
    vertices.insert(vertices.end(), vertex, vertex + VERTEX_SIZE);
}

/**
Appends a back side copy of the first triangles: vertices in reverse order
and normals negated.
*/
void OpenGL1MeshBuilder::addBackSides(int frontCount)
{
    vertices.reserve(vertices.size() * 2);
    for ( int t = 0; t + 2 < frontCount; t += 3 ) {
        for ( int k = 2; k >= 0; k-- ) {
            size_t start = (size_t)(t + k) * VERTEX_SIZE;
            float v[VERTEX_SIZE];
            for ( int c = 0; c < VERTEX_SIZE; c++ ) {
                v[c] = vertices[start + c];
            }
            for ( int c = 3; c < 6; c++ ) {
                v[c] = -v[c];
            }
            vertices.insert(vertices.end(), v, v + VERTEX_SIZE);
        }
    }
}
