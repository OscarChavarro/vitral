package vsdk.toolkit.processing;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.surface.BezierPatchSubdivision;
import vsdk.toolkit.environment.geometry.surface.SubdividedBezierPatch;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.volume.BlobComponent;
import vsdk.toolkit.environment.geometry.volume.BlobSolid;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.csg.CsgBody;
import vsdk.toolkit.environment.scene.csg.CsgBodyEvaluator;
import vsdk.toolkit.environment.scene.csg.CsgOperation;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectGt;
import static vsdk.toolkit.GTestAssertions.expectLt;
import static vsdk.toolkit.GTestAssertions.expectNear;
import static vsdk.toolkit.GTestAssertions.expectTrue;

/**
Port of `cpp/base/src/test/solid_boundary_tessellator_tests.cpp`; each test
method is named `<cpp suite>_<cpp test>`.
*/
class SolidBoundaryTessellatorTest
{
    // A placed sphere: a closed, outward mesh of genus 0 whose vertices lie on the
    // sphere, with the area and volume of the sphere within the resolution.
    @Test
    void SolidBoundaryTessellatorContract_SphereMeshIsClosedOutwardAndOnTheSurface()
    {
        SimpleBody ball = new SimpleBody();
        ball.setGeometry(new Sphere(1.0));
        ball.setPosition(new Vector3Dd(0.3, -0.2, 0.1));
        GeometryBounds region = new GeometryBounds();
        assertThat(region.setFinite(new Vector3Dd(-0.8, -1.3, -1.0), new Vector3Dd(1.4, 0.9, 1.2))).isTrue();
        SolidBoundaryTessellationReport report = new SolidBoundaryTessellationReport();
        TriangleMesh mesh = SolidBoundaryTessellator.tessellate(ball, region, 32, report);
        assertThat(mesh).isNotNull();
        expectTrue(TessellationMeshCheck.isClosedAndOriented(mesh));
        expectEq(2, TessellationMeshCheck.eulerCharacteristic(mesh));
        expectNear(4.0*Math.PI/3.0, TessellationMeshCheck.enclosedVolume(mesh), 0.03*4.0*Math.PI/3.0);
        expectNear(4.0*Math.PI, TessellationMeshCheck.area(mesh), 0.05*4.0*Math.PI);
        expectEq(0, report.midpointCrossings);
        expectEq(0, report.regionCrossings);
        expectEq(0, report.pointClassifiedLines);
        expectEq(report.triangles, mesh.getNumTriangles());
        expectEq(report.boundaryVertices + report.cellVertices, mesh.getNumVertices());
        expectGt(report.boundaryVertices, 0.95*mesh.getNumVertices());
        final double[] normals = mesh.getVertexNormals();
        int onSurface = 0;
        for (int v = 0; v < mesh.getNumVertices(); v++) {
            final Vector3Dd p = TessellationMeshCheck.vertex(mesh, v).subtract(ball.getPosition());
            expectNear(1.0, p.length(), report.cellSize);
            if (Math.abs(p.length() - 1.0) < 1e-9) onSurface++;
            // Vertex normals are the sphere's outward normals.
            final Vector3Dd n = new Vector3Dd(normals[v*3], normals[v*3 + 1], normals[v*3 + 2]);
            expectGt(n.dotProduct(p.normalized()), 0.9);
        }
        expectEq(report.boundaryVertices, onSurface);
    }

    // A CSG difference: the cavity is a second shell facing into it.
    @Test
    void SolidBoundaryTessellatorContract_CsgDifferenceKeepsTheCavity()
    {
        for (int e = 0; e < 2; e++) {
            CsgBody body = new CsgBody(CsgOperation.DIFFERENCE, e == 0 ? CsgBodyEvaluator.ROTH : CsgBodyEvaluator.CROSSING_MEMBERSHIP);
            SimpleBody outer = new SimpleBody();
            outer.setGeometry(new Box(2.0, 2.0, 2.0));
            SimpleBody hole = new SimpleBody();
            hole.setGeometry(new Sphere(0.6));
            assertThat(body.addLeaf(0, outer)).isTrue();
            assertThat(body.addLeaf(0, hole)).isTrue();
            GeometryBounds region = new GeometryBounds();
            assertThat(region.setFinite(new Vector3Dd(-1.23, -1.23, -1.23), new Vector3Dd(1.23, 1.23, 1.23))).isTrue();
            SolidBoundaryTessellationReport report = new SolidBoundaryTessellationReport();
            TriangleMesh mesh = SolidBoundaryTessellator.tessellate(body, region, 24, report);
            assertThat(mesh).isNotNull();
            expectTrue(TessellationMeshCheck.isClosedAndOriented(mesh), "" + e);
            // Two spheres' worth of Euler characteristic: the box shell and the cavity.
            expectEq(4, TessellationMeshCheck.eulerCharacteristic(mesh), "" + e);
            final double expected = 8.0 - 4.0*Math.PI*0.216/3.0;
            expectNear(expected, TessellationMeshCheck.enclosedVolume(mesh), 0.04*expected, "" + e);
            expectEq(0, report.regionCrossings, "" + e);
            expectGt(report.exactCrossings, 0, "" + e);
        }
    }

    // A blob of two merged components is one closed shell.
    @Test
    void SolidBoundaryTessellatorContract_MergedBlobIsOneShell()
    {
        BlobSolid blob = new BlobSolid(0.6, false);
        blob.addComponent(new BlobComponent(new Vector3Dd(-0.8, 0, 0), 1.5, 1.0));
        blob.addComponent(new BlobComponent(new Vector3Dd(0.8, 0, 0), 1.5, 1.0));
        SimpleBody body = new SimpleBody();
        body.setGeometry(blob);
        GeometryBounds region = new GeometryBounds();
        body.getSolidWorldBounds(region);
        assertThat(region.getKind()).isEqualTo(GeometryBounds.Kind.Finite);
        SolidBoundaryTessellationReport report = new SolidBoundaryTessellationReport();
        TriangleMesh mesh = SolidBoundaryTessellator.tessellate(body, region.inflated(0.1), 40, report);
        assertThat(mesh).isNotNull();
        expectTrue(TessellationMeshCheck.isClosedAndOriented(mesh));
        expectEq(2, TessellationMeshCheck.eulerCharacteristic(mesh));
        expectGt(TessellationMeshCheck.enclosedVolume(mesh), 0);
        expectEq(0, report.regionCrossings);
        for (int v = 0; v < mesh.getNumVertices(); v++) {
            final Vector3Dd p = TessellationMeshCheck.vertex(mesh, v);
            // Every vertex is within a cell of the threshold surface.
            expectLt(Math.abs(blob.evaluateField(p)), 1.0, "" + v);
        }
    }

    // A solid larger than the region is closed by the region's sides.
    @Test
    void SolidBoundaryTessellatorContract_RegionClosesASolidThatLeavesIt()
    {
        SimpleBody ball = new SimpleBody();
        ball.setGeometry(new Sphere(3.0));
        GeometryBounds region = new GeometryBounds();
        assertThat(region.setFinite(new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1))).isTrue();
        SolidBoundaryTessellationReport report = new SolidBoundaryTessellationReport();
        TriangleMesh mesh = SolidBoundaryTessellator.tessellate(ball, region, 8, report);
        assertThat(mesh).isNotNull();
        expectTrue(TessellationMeshCheck.isClosedAndOriented(mesh));
        expectNear(8.0, TessellationMeshCheck.enclosedVolume(mesh), 1e-9);
        expectGt(report.regionCrossings, 0);
        expectEq(0, report.exactCrossings);
    }

    @Test
    void SolidBoundaryTessellatorContract_InvalidRequestsAndEmptyResults()
    {
        SimpleBody ball = new SimpleBody();
        ball.setGeometry(new Sphere(1.0));
        SolidBoundaryTessellationReport report = new SolidBoundaryTessellationReport();
        GeometryBounds region = new GeometryBounds();
        assertThat(SolidBoundaryTessellator.tessellate(ball, region, 8, report)).isNull();
        region.setUnbounded();
        assertThat(SolidBoundaryTessellator.tessellate(ball, region, 8, report)).isNull();
        assertThat(region.setFinite(new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, 1))).isTrue();
        assertThat(SolidBoundaryTessellator.tessellate(ball, region, 0, report)).isNull();
        assertThat(SolidBoundaryTessellator.tessellate(ball, region,
            SolidBoundaryTessellator.MAXIMUM_RESOLUTION + 1, report)).isNull();
        // A region away from the solid gives an empty mesh.
        assertThat(region.setFinite(new Vector3Dd(5, 5, 5), new Vector3Dd(6, 6, 6))).isTrue();
        TriangleMesh mesh = SolidBoundaryTessellator.tessellate(ball, region, 8, report);
        assertThat(mesh).isNotNull();
        expectEq(0, mesh.getNumTriangles());
    }

    // The patch mesh samples the Bezier surface and faces its u x v side.
    @Test
    void BezierPatchTessellatorContract_VerticesLieOnTheBezierSurface()
    {
        Vector3Dd[][] net = new Vector3Dd[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                net[i][j] = new Vector3Dd(i, j, (i == 1 || i == 2) && (j == 1 || j == 2) ? 0.6 : 0.2*i);
            }
        }
        SubdividedBezierPatch patch = new SubdividedBezierPatch(BezierPatchSubdivision.UniformGrid, 4, 4, 0.0, net);
        TriangleMesh mesh = BezierPatchTessellator.tessellate(patch, 6, 9);
        assertThat(mesh).isNotNull();
        expectEq(7*10, mesh.getNumVertices());
        expectEq(2*6*9, mesh.getNumTriangles());
        final double[] uvs = mesh.getVertexUvs();
        final double[] faceNormals = mesh.getTriangleNormals();
        final int[] indices = mesh.getTriangleIndexes();
        for (int v = 0; v < mesh.getNumVertices(); v++) {
            final Vector3Dd expected = patch.evaluate(uvs[v*2], uvs[v*2 + 1]);
            expectLt(TessellationMeshCheck.vertex(mesh, v).subtract(expected).length(), 1e-12);
        }
        for (int t = 0; t < mesh.getNumTriangles(); t++) {
            double u = 0, v = 0;
            for (int k = 0; k < 3; k++) {
                u += uvs[indices[t*3 + k]*2]/3.0;
                v += uvs[indices[t*3 + k]*2 + 1]/3.0;
            }
            final Vector3Dd n = new Vector3Dd(faceNormals[t*3], faceNormals[t*3 + 1], faceNormals[t*3 + 2]);
            expectGt(n.dotProduct(BezierPatchTessellator.surfaceNormal(patch, u, v)), 0.9, "" + t);
        }
        // The vertex normals are S_u x S_v, here +z at the center of the flat-sided net.
        final Vector3Dd center = BezierPatchTessellator.surfaceNormal(patch, 0.5, 0.5);
        expectGt(center.z(), 0.9);
        assertThat(BezierPatchTessellator.tessellate(patch, 0, 3)).isNull();
        assertThat(BezierPatchTessellator.tessellate(patch, 3, BezierPatchTessellator.MAXIMUM_STEPS + 1)).isNull();
    }
}
