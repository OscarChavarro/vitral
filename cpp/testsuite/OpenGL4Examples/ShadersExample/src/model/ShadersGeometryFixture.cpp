#include <cmath>
#include "java/util/ArrayList.txx"
#include "ShadersGeometryFixture.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/geometry/surface/SubdividedBezierPatch.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobComponent.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/HeightFieldSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/PolynomialSurface.h"
#include "vsdk/toolkit/environment/geometry/volume/QuadricSurface.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/csg/CsgBody.h"
#include "vsdk/toolkit/processing/BezierPatchTessellator.h"
#include "vsdk/toolkit/processing/SolidBoundaryTessellator.h"

ShadersGeometryFixture::ShadersGeometryFixture(ShadersGeometry inKind)
    : kind(inKind), drawableMesh(nullptr)
{
    switch ( kind ) {
        case ShadersGeometry::QUARTIC: buildQuartic(); break;
        case ShadersGeometry::BLOB: buildBlob(); break;
        case ShadersGeometry::HEIGHT_FIELD: buildHeightField(); break;
        case ShadersGeometry::CSG: buildCsg(); break;
        case ShadersGeometry::PATCH: buildPatch(); break;
        case ShadersGeometry::SPHERE: break;
    }
}

ShadersGeometryFixture::~ShadersGeometryFixture()
{
    delete drawableMesh;
    for ( long int i = 0; i < geometries.size(); i++ ) delete geometries[i];
}

ShadersGeometry ShadersGeometryFixture::getKind() const { return kind; }

const SolidBoundaryTessellationReport& ShadersGeometryFixture::getTessellationReport() const
{
    return report;
}

/// Rounded cube x^4 + y^4 + z^4 - 0.4 < 0, a quartic PolynomialSurface
void ShadersGeometryFixture::buildQuartic()
{
    PolynomialSurface* quartic = new PolynomialSurface(4, false);
    double* c = quartic->getCoefficients();
    c[PolynomialSurface::getTermIndex(4, 4, 0, 0)] = 1.0;
    c[PolynomialSurface::getTermIndex(4, 0, 4, 0)] = 1.0;
    c[PolynomialSurface::getTermIndex(4, 0, 0, 4)] = 1.0;
    c[PolynomialSurface::getTermIndex(4, 0, 0, 0)] = -0.4;
    geometries.add(quartic);
    region.setFinite(Vector3Dd(-0.85, -0.85, -0.85), Vector3Dd(0.85, 0.85, 0.85));
}

/// Four merged blob components
void ShadersGeometryFixture::buildBlob()
{
    BlobSolid* blob = new BlobSolid(0.5, false);
    blob->addComponent(BlobComponent(Vector3Dd(-0.45, 0, -0.3), 0.8, 1.0));
    blob->addComponent(BlobComponent(Vector3Dd(0.45, 0, -0.3), 0.8, 1.0));
    blob->addComponent(BlobComponent(Vector3Dd(0, 0, 0.45), 0.8, 1.0));
    blob->addComponent(BlobComponent(Vector3Dd(0, -0.35, 0), 0.6, 1.0));
    geometries.add(blob);
    GeometryBounds bounds;
    blob->getBounds(bounds);
    region = bounds.inflated(0.05);
}

/**
Hills on a 33 x 33 grid, laid with its height along world z and scaled into
a 1.6 unit square by the body's placement.
*/
void ShadersGeometryFixture::buildHeightField()
{
    const long int size = 33;
    HeightFieldSolid* field = new HeightFieldSolid(size, size, Vector3Dd(1, 0, 1), Vector3Dd(size - 2, 1, size - 2));
    for ( long int x = 0; x < size; x++ ) {
        for ( long int z = 0; z < size; z++ ) {
            const double u = (x - 16.0) / 16.0, v = (z - 16.0) / 16.0;
            const double h = 0.45 + 0.3 * std::cos(3.0 * u) * std::cos(2.5 * v) +
                0.15 * std::sin(5.0 * (u + v));
            field->setHeight(x, z, h);
        }
    }
    field->updateBlocks();
    geometries.add(field);
    region.setFinite(Vector3Dd(-0.82, -0.82, -0.45), Vector3Dd(0.82, 0.82, 0.35));
}

/**
The classic CSG sample: the intersection of a cube and a sphere, minus three
cylinders along the axes, evaluated with Roth's classification.
*/
void ShadersGeometryFixture::buildCsg()
{
    geometries.add(new Box(1.3, 1.3, 1.3));
    geometries.add(new Sphere(0.85));
    const double r2 = 0.33 * 0.33;
    geometries.add(new QuadricSurface(Vector3Dd(1, 1, 0), Vector3Dd(0, 0, 0), Vector3Dd(0, 0, 0), -r2));
    geometries.add(new QuadricSurface(Vector3Dd(0, 1, 1), Vector3Dd(0, 0, 0), Vector3Dd(0, 0, 0), -r2));
    geometries.add(new QuadricSurface(Vector3Dd(1, 0, 1), Vector3Dd(0, 0, 0), Vector3Dd(0, 0, 0), -r2));
    region.setFinite(Vector3Dd(-0.72, -0.72, -0.72), Vector3Dd(0.72, 0.72, 0.72));
}

/// A wavy open Bezier patch facing the camera
void ShadersGeometryFixture::buildPatch()
{
    Vector3Dd net[4][4];
    for ( int i = 0; i < 4; i++ ) {
        for ( int j = 0; j < 4; j++ ) {
            const double x = -0.9 + 0.6 * i, z = -0.9 + 0.6 * j;
            const double y = ((i + j) % 2 == 0 ? -0.35 : 0.35) * ((i == 0 || i == 3) ? 0.5 : 1.0);
            net[i][j] = Vector3Dd(x, y, z);
        }
    }
    geometries.add(new SubdividedBezierPatch(BezierPatchSubdivision::UniformGrid, 16, 16, 0.0, net));
}

SimpleBody* ShadersGeometryFixture::createBody(SimpleMaterial* material,
    const Matrix4x4d& rotation) const
{
    SimpleBody* body = nullptr;
    if ( kind == ShadersGeometry::CSG ) {
        CsgBody* csg = new CsgBody(CsgOperation::DIFFERENCE, CsgBodyEvaluator::ROTH);
        const long int core = csg->addNode(0, CsgOperation::INTERSECTION);
        for ( long int i = 0; i < geometries.size(); i++ ) {
            SimpleBody* leaf = new SimpleBody();
            leaf->setGeometryReference(geometries[i]);
            csg->addLeaf(i < 2 ? core : 0, leaf);
        }
        body = csg;
    }
    else {
        body = new SimpleBody();
        body->setGeometryReference(geometries[0]);
    }
    if ( material != nullptr ) body->setMaterial(material);
    Matrix4x4d forward = Matrix4x4d::identityMatrix();
    if ( kind == ShadersGeometry::HEIGHT_FIELD ) {
        // Grid x to world x, grid z to world -y, height y to world z.
        const double s = 1.6 / 30.0, hz = 0.7;
        const double m[4][4] = {
            { s, 0, 0, -16.0 * s },
            { 0, 0, -s, 16.0 * s },
            { 0, hz, 0, -0.4 },
            { 0, 0, 0, 1 } };
        forward = Matrix4x4d(m);
    }
    const Matrix4x4d placed = rotation.multiply(forward);
    body->setAffinePlacement(placed, placed.inverse());
    return body;
}

TriangleMesh* ShadersGeometryFixture::getDrawableMesh()
{
    if ( drawableMesh != nullptr ) return drawableMesh;
    if ( kind == ShadersGeometry::PATCH ) {
        drawableMesh = BezierPatchTessellator::tessellate(
            *static_cast<SubdividedBezierPatch*>(geometries[0]), PATCH_STEPS, PATCH_STEPS);
        return drawableMesh;
    }
    SimpleBody* body = createBody(nullptr, Matrix4x4d::identityMatrix());
    drawableMesh = SolidBoundaryTessellator::tessellate(*body, region, SOLID_RESOLUTION, report);
    delete body;
    return drawableMesh;
}

const char* ShadersGeometryFixture::nameOf(ShadersGeometry geometry)
{
    switch ( geometry ) {
        case ShadersGeometry::SPHERE: return "sphere";
        case ShadersGeometry::QUARTIC: return "quartic";
        case ShadersGeometry::BLOB: return "blob";
        case ShadersGeometry::HEIGHT_FIELD: return "heightfield";
        case ShadersGeometry::CSG: return "csg";
        case ShadersGeometry::PATCH: return "patch";
    }
    return "sphere";
}

bool ShadersGeometryFixture::parse(const java::String& name, ShadersGeometry& result)
{
    for ( ShadersGeometry g = ShadersGeometry::SPHERE; ; g = next(g) ) {
        if ( name == nameOf(g) ) {
            result = g;
            return true;
        }
        if ( next(g) == ShadersGeometry::SPHERE ) return false;
    }
}

ShadersGeometry ShadersGeometryFixture::next(ShadersGeometry geometry)
{
    switch ( geometry ) {
        case ShadersGeometry::SPHERE: return ShadersGeometry::QUARTIC;
        case ShadersGeometry::QUARTIC: return ShadersGeometry::BLOB;
        case ShadersGeometry::BLOB: return ShadersGeometry::HEIGHT_FIELD;
        case ShadersGeometry::HEIGHT_FIELD: return ShadersGeometry::CSG;
        case ShadersGeometry::CSG: return ShadersGeometry::PATCH;
        case ShadersGeometry::PATCH: return ShadersGeometry::SPHERE;
    }
    return ShadersGeometry::SPHERE;
}
