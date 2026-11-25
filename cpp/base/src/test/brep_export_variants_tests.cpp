#include <cmath>
#include <gtest/gtest.h>

#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/geometry/volume/Tube.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

/*
Variants of the boundary representations exported by the primitives: a
sphere made of quadrilaterals (besides the default triangles) and the open
lamina of the lateral surface of a cylinder, a frustum and a cone (besides
the default closed solid), and a washer (or a disk) split into
quadrilaterals (or triangles) instead of two faces with a hole.
*/
namespace {

int loopSize(_PolyhedralBoundedSolidLoop* loop)
{
    _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
    _PolyhedralBoundedSolidHalfEdge* he = start;
    int count = 0;
    do {
        count++;
        he = he->next();
    } while ( he != start );
    return count;
}

Vector3Dd faceCentroid(_PolyhedralBoundedSolidFace* face)
{
    _PolyhedralBoundedSolidHalfEdge* start =
        face->boundariesList.get(0)->boundaryStartHalfEdge;
    _PolyhedralBoundedSolidHalfEdge* he = start;
    Vector3Dd sum(0, 0, 0);
    int count = 0;
    do {
        sum = sum.add(he->startingVertex->position);
        count++;
        he = he->next();
    } while ( he != start );
    return sum.multiply(1.0 / count);
}

void expectTopology(PolyhedralBoundedSolid* solid, int vertices, int edges,
    int faces)
{
    PolyhedralBoundedSolidTopologySummary topology =
        PolyhedralBoundedSolidTopologySummary::from(solid);
    EXPECT_EQ(topology.getVertexCount(), vertices);
    EXPECT_EQ(topology.getEdgeCount(), edges);
    EXPECT_EQ(topology.getFaceCount(), faces);
    EXPECT_EQ(topology.getShellCount(), 1);
}

/**
Checks that the faces with ids in [1, 1] U [3, n+1] point away from the
axis and the other ones towards it, that every face is planar and has the
expected number of vertices.
*/
void expectOpenLaminaSides(PolyhedralBoundedSolid* solid, int n,
    int verticesPerFace)
{
    for ( long i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        EXPECT_EQ(face->boundariesList.size(), 1);
        EXPECT_EQ(loopSize(face->boundariesList.get(0)), verticesPerFace);
        EXPECT_TRUE(PolyhedralBoundedSolidGeometricValidator::
            validateFaceIsPlanar(face));
        Vector3Dd normal = face->containingPlaneNormal();
        Vector3Dd centroid = faceCentroid(face);
        double radial = normal.x() * centroid.x() + normal.y() * centroid.y();
        bool outer = face->id == 1 || (face->id >= 3 && face->id <= n + 1);
        EXPECT_EQ(radial > 0, outer) << "face " << face->id;
    }
}

}

TEST(BrepExportVariantsTest, given_sphere_when_exportedByDefault_then_bandsAreTriangles)
{
    Sphere sphere(2.0);
    PolyhedralBoundedSolid* solid = sphere.exportToPolyhedralBoundedSolid(8, 4);

    // 2 poles + 3 parallels of 8 vertices; 2 fans of 8 triangles and 2 bands
    // of 8 quadrilaterals split into 16 triangles
    expectTopology(solid, 26, 72, 48);
    for ( long i = 0; i < solid->getPolygonsList().size(); i++ ) {
        EXPECT_EQ(loopSize(solid->getPolygonsList().get(i)
            ->boundariesList.get(0)), 3);
    }
    delete solid;
}

TEST(BrepExportVariantsTest, given_sphere_when_exportedWithQuadrilaterals_then_bandsArePlanarQuads)
{
    int meridians = 16;
    int parallels = 8;
    Sphere sphere(2.0);
    PolyhedralBoundedSolid* solid = sphere.exportToPolyhedralBoundedSolid(
        meridians, parallels, true);

    int vertices = 2 + (parallels - 1) * meridians;
    int faces = 2 * meridians + (parallels - 2) * meridians;
    expectTopology(solid, vertices, vertices + faces - 2, faces);
    int triangles = 0;
    int quadrilaterals = 0;
    for ( long i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        int size = loopSize(face->boundariesList.get(0));
        if ( size == 3 ) {
            triangles++;
        }
        else if ( size == 4 ) {
            quadrilaterals++;
        }
        EXPECT_TRUE(PolyhedralBoundedSolidGeometricValidator::
            validateFaceIsPlanar(face));
        // Outward normals
        EXPECT_GT(face->containingPlaneNormal().dotProduct(
            faceCentroid(face)), 0.0);
    }
    EXPECT_EQ(triangles, 2 * meridians);
    EXPECT_EQ(quadrilaterals, (parallels - 2) * meridians);
    java::String msg;
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateStrict(solid,
        &msg)) << msg.c_str();
    delete solid;
}

TEST(BrepExportVariantsTest, given_cylinder_when_exportedByDefault_then_isClosedSolid)
{
    int n = 12;
    Cone cylinder(1.5, 1.5, 3.0);
    PolyhedralBoundedSolid* solid = cylinder.exportToPolyhedralBoundedSolid(n, 1);

    expectTopology(solid, 2 * n, 3 * n, n + 2);
    delete solid;
}

TEST(BrepExportVariantsTest, given_cylinder_when_exportedWithoutCaps_then_isOpenLaminaOfQuads)
{
    int n = 12;
    Cone cylinder(1.5, 1.5, 3.0);
    PolyhedralBoundedSolid* solid = cylinder.exportToPolyhedralBoundedSolid(n,
        1, false);

    // Two coincident sides of n quadrilaterals; each vertical edge appears
    // twice (once per side): V - E + F = 0, genus 1
    expectTopology(solid, 2 * n, 4 * n, 2 * n);
    expectOpenLaminaSides(solid, n, 4);
    for ( long i = 0; i < solid->getVerticesList().size(); i++ ) {
        Vector3Dd p = solid->getVerticesList().get(i)->position;
        EXPECT_NEAR(std::hypot(p.x(), p.y()), 1.5, 1e-9);
        EXPECT_TRUE(p.z() == 0.0 || p.z() == 3.0);
    }
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateIntermediate(
        solid));
    delete solid;
}

TEST(BrepExportVariantsTest, given_frustum_when_exportedWithoutCaps_then_isOpenLaminaOfPlanarQuads)
{
    int n = 10;
    Cone frustum(2.0, 0.5, 1.0);
    PolyhedralBoundedSolid* solid = frustum.exportToPolyhedralBoundedSolid(n,
        3, false);

    expectTopology(solid, 2 * n, 4 * n, 2 * n);
    expectOpenLaminaSides(solid, n, 4);
    for ( long i = 0; i < solid->getVerticesList().size(); i++ ) {
        Vector3Dd p = solid->getVerticesList().get(i)->position;
        double expectedRadius = p.z() == 0.0 ? 2.0 : 0.5;
        EXPECT_NEAR(std::hypot(p.x(), p.y()), expectedRadius, 1e-9);
    }
    delete solid;
}

TEST(BrepExportVariantsTest, given_cone_when_exportedWithoutCaps_then_isOpenLaminaOfTriangleFans)
{
    int n = 8;
    Cone cone(1.0, 0.0, 2.0);
    PolyhedralBoundedSolid* solid = cone.exportToPolyhedralBoundedSolid(n, 1,
        false);

    // Base ring plus one apex per side; n edges on the ring and n to each
    // apex: V - E + F = 2, a lamina with the topology of a sphere
    expectTopology(solid, n + 2, 3 * n, 2 * n);
    expectOpenLaminaSides(solid, n, 3);
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateIntermediate(
        solid));
    delete solid;
}

/**
Checks that the faces with ids in [1, 1] U [3, n+1] face +Z and the other
ones -Z, that every face is planar, lies on the plane z = h and has the
expected number of vertices.
*/
static void expectFlatLaminaSides(PolyhedralBoundedSolid* solid, int n,
    int verticesPerFace, double h)
{
    for ( long i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        EXPECT_EQ(face->boundariesList.size(), 1);
        EXPECT_EQ(loopSize(face->boundariesList.get(0)), verticesPerFace);
        EXPECT_TRUE(PolyhedralBoundedSolidGeometricValidator::
            validateFaceIsPlanar(face));
        EXPECT_NEAR(faceCentroid(face).z(), h, 1e-12);
        bool up = face->id == 1 || (face->id >= 3 && face->id <= n + 1);
        EXPECT_NEAR(face->containingPlaneNormal().z(), up ? 1.0 : -1.0,
            1e-9) << "face " << face->id;
    }
}

TEST(BrepExportVariantsTest, given_washer_when_builtWithQuadrilaterals_then_isLaminaOfRadialQuadrilaterals)
{
    int n = 12;
    PolyhedralBoundedSolid* solid = Tube::createWasherLamina(0.5, 2.0, 0.25,
        n, true);

    // Same vertices as the washer, n radial edges per side: V - E + F = 0
    expectTopology(solid, 2 * n, 4 * n, 2 * n);
    expectFlatLaminaSides(solid, n, 4, 0.25);
    for ( int k = 0; k < n; k++ ) {
        Vector3Dd external = solid->findVertex(k + 1)->position;
        Vector3Dd internal =
            solid->findVertex(n + 1 + ((n - k) % n))->position;
        EXPECT_NEAR(internal.x(), external.x() * 0.25, 1e-12);
        EXPECT_NEAR(internal.y(), external.y() * 0.25, 1e-12);
    }
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateIntermediate(
        solid));
    delete solid;
}

TEST(BrepExportVariantsTest, given_disk_when_builtWithQuadrilaterals_then_isLaminaOfTriangleFans)
{
    int n = 10;
    PolyhedralBoundedSolid* solid = Tube::createWasherLamina(0.0, 1.5, -1.0,
        n, true);

    expectTopology(solid, n + 2, 3 * n, 2 * n);
    expectFlatLaminaSides(solid, n, 3, -1.0);
    EXPECT_EQ(solid->findVertex(n + 1)->position.x(), 0.0);
    EXPECT_EQ(solid->findVertex(n + 2)->position.z(), -1.0);
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateIntermediate(
        solid));
    delete solid;
}

TEST(BrepExportVariantsTest, given_washer_when_builtByDefault_then_hasTwoFacesWithHoles)
{
    PolyhedralBoundedSolid* solid = Tube::createWasherLamina(0.5, 2.0, 0.0,
        8);

    expectTopology(solid, 16, 16, 2);
    EXPECT_EQ(solid->findFace(1)->boundariesList.size(), 2);
    EXPECT_EQ(solid->findFace(2)->boundariesList.size(), 2);
    delete solid;
}
