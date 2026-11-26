package vsdk.toolkit.environment.geometry.volume;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectGt;
import static vsdk.toolkit.GTestAssertions.expectNear;
import static vsdk.toolkit.GTestAssertions.expectTrue;

/**
Port of `cpp/base/src/test/brep_export_variants_tests.cpp`; each test method
is named `<cpp suite>_<cpp test>`.

Variants of the boundary representations exported by the primitives: a
sphere made of quadrilaterals (besides the default triangles) and the open
lamina of the lateral surface of a cylinder, a frustum and a cone (besides
the default closed solid), and a washer (or a disk) split into
quadrilaterals (or triangles) instead of two faces with a hole.
*/
class BrepExportVariantsTest
{
    private static int loopSize(_PolyhedralBoundedSolidLoop loop)
    {
        _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge he = start;
        int count = 0;
        do {
            count++;
            he = he.next();
        } while ( he != start );
        return count;
    }

    private static Vector3Dd faceCentroid(_PolyhedralBoundedSolidFace face)
    {
        _PolyhedralBoundedSolidHalfEdge start =
            face.boundariesList.get(0).boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge he = start;
        Vector3Dd sum = new Vector3Dd(0, 0, 0);
        int count = 0;
        do {
            sum = sum.add(he.startingVertex.position);
            count++;
            he = he.next();
        } while ( he != start );
        return sum.multiply(1.0 / count);
    }

    /** The `containingPlaneNormal` of the C++ face. */
    private static Vector3Dd containingPlaneNormal(_PolyhedralBoundedSolidFace face)
    {
        return face.getContainingPlane().getNormal();
    }

    private static void expectTopology(PolyhedralBoundedSolid solid,
        int vertices, int edges, int faces)
    {
        PolyhedralBoundedSolidTopologySummary topology =
            PolyhedralBoundedSolidTopologySummary.from(solid);
        expectEq(topology.getVertexCount(), vertices);
        expectEq(topology.getEdgeCount(), edges);
        expectEq(topology.getFaceCount(), faces);
        expectEq(topology.getShellCount(), 1);
    }

    /**
    Checks that the faces with ids in [1, 1] U [3, n+1] point away from the
    axis and the other ones towards it, that every face is planar and has the
    expected number of vertices.
    */
    private static void expectOpenLaminaSides(PolyhedralBoundedSolid solid,
        int n, int verticesPerFace)
    {
        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            expectEq(face.boundariesList.size(), 1);
            expectEq(loopSize(face.boundariesList.get(0)), verticesPerFace);
            expectTrue(PolyhedralBoundedSolidGeometricValidator
                .validateFaceIsPlanar(face));
            Vector3Dd normal = containingPlaneNormal(face);
            Vector3Dd centroid = faceCentroid(face);
            double radial = normal.x() * centroid.x() + normal.y() * centroid.y();
            boolean outer = face.id == 1 || (face.id >= 3 && face.id <= n + 1);
            expectEq(radial > 0, outer, "face " + face.id);
        }
    }

    /**
    Checks that the faces with ids in [1, 1] U [3, n+1] face +Z and the other
    ones -Z, that every face is planar, lies on the plane z = h and has the
    expected number of vertices.
    */
    private static void expectFlatLaminaSides(PolyhedralBoundedSolid solid,
        int n, int verticesPerFace, double h)
    {
        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            expectEq(face.boundariesList.size(), 1);
            expectEq(loopSize(face.boundariesList.get(0)), verticesPerFace);
            expectTrue(PolyhedralBoundedSolidGeometricValidator
                .validateFaceIsPlanar(face));
            expectNear(faceCentroid(face).z(), h, 1e-12);
            boolean up = face.id == 1 || (face.id >= 3 && face.id <= n + 1);
            expectNear(containingPlaneNormal(face).z(), up ? 1.0 : -1.0,
                1e-9, "face " + face.id);
        }
    }

    @Test
    void BrepExportVariantsTest_given_sphere_when_exportedByDefault_then_bandsAreTriangles()
    {
        Sphere sphere = new Sphere(2.0);
        PolyhedralBoundedSolid solid = sphere.exportToPolyhedralBoundedSolid(8, 4);

        // 2 poles + 3 parallels of 8 vertices; 2 fans of 8 triangles and 2 bands
        // of 8 quadrilaterals split into 16 triangles
        expectTopology(solid, 26, 72, 48);
        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            expectEq(loopSize(solid.getPolygonsList().get(i)
                .boundariesList.get(0)), 3);
        }
    }

    @Test
    void BrepExportVariantsTest_given_sphere_when_exportedWithQuadrilaterals_then_bandsArePlanarQuads()
    {
        int meridians = 16;
        int parallels = 8;
        Sphere sphere = new Sphere(2.0);
        PolyhedralBoundedSolid solid = sphere.exportToPolyhedralBoundedSolid(
            meridians, parallels, true);

        int vertices = 2 + (parallels - 1) * meridians;
        int faces = 2 * meridians + (parallels - 2) * meridians;
        expectTopology(solid, vertices, vertices + faces - 2, faces);
        int triangles = 0;
        int quadrilaterals = 0;
        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            int size = loopSize(face.boundariesList.get(0));
            if ( size == 3 ) {
                triangles++;
            }
            else if ( size == 4 ) {
                quadrilaterals++;
            }
            expectTrue(PolyhedralBoundedSolidGeometricValidator
                .validateFaceIsPlanar(face));
            // Outward normals
            expectGt(containingPlaneNormal(face).dotProduct(
                faceCentroid(face)), 0.0);
        }
        expectEq(triangles, 2 * meridians);
        expectEq(quadrilaterals, (parallels - 2) * meridians);
        StringBuilder msg = new StringBuilder();
        expectTrue(PolyhedralBoundedSolidValidationEngine.validateStrict(solid,
            msg), msg.toString());
    }

    @Test
    void BrepExportVariantsTest_given_cylinder_when_exportedByDefault_then_isClosedSolid()
    {
        int n = 12;
        Cone cylinder = new Cone(1.5, 1.5, 3.0);
        PolyhedralBoundedSolid solid = cylinder.exportToPolyhedralBoundedSolid(n, 1);

        expectTopology(solid, 2 * n, 3 * n, n + 2);
    }

    @Test
    void BrepExportVariantsTest_given_cylinder_when_exportedWithoutCaps_then_isOpenLaminaOfQuads()
    {
        int n = 12;
        Cone cylinder = new Cone(1.5, 1.5, 3.0);
        PolyhedralBoundedSolid solid = cylinder.exportToPolyhedralBoundedSolid(n,
            1, false);

        // Two coincident sides of n quadrilaterals; each vertical edge appears
        // twice (once per side): V - E + F = 0, genus 1
        expectTopology(solid, 2 * n, 4 * n, 2 * n);
        expectOpenLaminaSides(solid, n, 4);
        for ( int i = 0; i < solid.getVerticesList().size(); i++ ) {
            Vector3Dd p = solid.getVerticesList().get(i).position;
            expectNear(Math.hypot(p.x(), p.y()), 1.5, 1e-9);
            expectTrue(p.z() == 0.0 || p.z() == 3.0);
        }
        expectTrue(PolyhedralBoundedSolidValidationEngine.validateIntermediate(
            solid));
    }

    @Test
    void BrepExportVariantsTest_given_frustum_when_exportedWithoutCaps_then_isOpenLaminaOfPlanarQuads()
    {
        int n = 10;
        Cone frustum = new Cone(2.0, 0.5, 1.0);
        PolyhedralBoundedSolid solid = frustum.exportToPolyhedralBoundedSolid(n,
            3, false);

        expectTopology(solid, 2 * n, 4 * n, 2 * n);
        expectOpenLaminaSides(solid, n, 4);
        for ( int i = 0; i < solid.getVerticesList().size(); i++ ) {
            Vector3Dd p = solid.getVerticesList().get(i).position;
            double expectedRadius = p.z() == 0.0 ? 2.0 : 0.5;
            expectNear(Math.hypot(p.x(), p.y()), expectedRadius, 1e-9);
        }
    }

    @Test
    void BrepExportVariantsTest_given_cone_when_exportedWithoutCaps_then_isOpenLaminaOfTriangleFans()
    {
        int n = 8;
        Cone cone = new Cone(1.0, 0.0, 2.0);
        PolyhedralBoundedSolid solid = cone.exportToPolyhedralBoundedSolid(n, 1,
            false);

        // Base ring plus one apex per side; n edges on the ring and n to each
        // apex: V - E + F = 2, a lamina with the topology of a sphere
        expectTopology(solid, n + 2, 3 * n, 2 * n);
        expectOpenLaminaSides(solid, n, 3);
        expectTrue(PolyhedralBoundedSolidValidationEngine.validateIntermediate(
            solid));
    }

    @Test
    void BrepExportVariantsTest_given_washer_when_builtWithQuadrilaterals_then_isLaminaOfRadialQuadrilaterals()
    {
        int n = 12;
        PolyhedralBoundedSolid solid = Tube.createWasherLamina(0.5, 2.0, 0.25,
            n, true);

        // Same vertices as the washer, n radial edges per side: V - E + F = 0
        expectTopology(solid, 2 * n, 4 * n, 2 * n);
        expectFlatLaminaSides(solid, n, 4, 0.25);
        for ( int k = 0; k < n; k++ ) {
            Vector3Dd external = solid.findVertex(k + 1).position;
            Vector3Dd internal =
                solid.findVertex(n + 1 + ((n - k) % n)).position;
            expectNear(internal.x(), external.x() * 0.25, 1e-12);
            expectNear(internal.y(), external.y() * 0.25, 1e-12);
        }
        expectTrue(PolyhedralBoundedSolidValidationEngine.validateIntermediate(
            solid));
    }

    @Test
    void BrepExportVariantsTest_given_disk_when_builtWithQuadrilaterals_then_isLaminaOfTriangleFans()
    {
        int n = 10;
        PolyhedralBoundedSolid solid = Tube.createWasherLamina(0.0, 1.5, -1.0,
            n, true);

        expectTopology(solid, n + 2, 3 * n, 2 * n);
        expectFlatLaminaSides(solid, n, 3, -1.0);
        expectEq(solid.findVertex(n + 1).position.x(), 0.0);
        expectEq(solid.findVertex(n + 2).position.z(), -1.0);
        expectTrue(PolyhedralBoundedSolidValidationEngine.validateIntermediate(
            solid));
    }

    @Test
    void BrepExportVariantsTest_given_washer_when_builtByDefault_then_hasTwoFacesWithHoles()
    {
        PolyhedralBoundedSolid solid = Tube.createWasherLamina(0.5, 2.0, 0.0,
            8);

        expectTopology(solid, 16, 16, 2);
        expectEq(solid.findFace(1).boundariesList.size(), 2);
        expectEq(solid.findFace(2).boundariesList.size(), 2);
    }
}
