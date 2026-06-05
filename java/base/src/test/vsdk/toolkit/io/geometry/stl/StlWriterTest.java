package vsdk.toolkit.io.geometry.stl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTestFixtures;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;
import vsdk.toolkit.io.PersistenceElement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StlWriterTest
{
    private static final double EPSILON = 1.0e-5;

    @Test
    void given_boxSolid_when_exporting_then_writesBinaryStlWithTwelveFacets()
        throws Exception
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Action
        StlWriter.exportSolid(solid, output);

        // Assert
        byte[] bytes = output.toByteArray();
        assertThat(bytes.length).isEqualTo(84 + 12 * 50);

        ParsedStl stl = parseBinaryStl(bytes);
        assertThat(stl.triangleCount).isEqualTo(12);
        assertThat(stl.facets).hasSize(12);
        assertThat(stl.header).contains("Vitral");

        int i;
        for ( i = 0; i < stl.facets.size(); i++ ) {
            Facet facet = stl.facets.get(i);
            assertThat(facet.attributeByteCount).isZero();
            assertThat(facet.normal.length()).isCloseTo(1.0,
                org.assertj.core.data.Offset.offset(1.0e-4));

            Vector3Dd cross = facet.b.subtract(facet.a)
                .crossProduct(facet.c.subtract(facet.a));
            assertThat(cross.length()).isGreaterThan(0.0);
            assertThat(cross.normalized().dotProduct(facet.normal))
                .isGreaterThan(0.999);
        }
    }

    @Test
    void given_sameSolid_when_exportingWithScaleFactor_then_verticesScaleAndNormalsStayEqual()
        throws Exception
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        ByteArrayOutputStream unscaledOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream scaledOutput = new ByteArrayOutputStream();

        // Action
        StlWriter.exportSolid(solid, unscaledOutput, 1.0);
        StlWriter.exportSolid(solid, scaledOutput, 1000.0);

        // Assert
        ParsedStl unscaled = parseBinaryStl(unscaledOutput.toByteArray());
        ParsedStl scaled = parseBinaryStl(scaledOutput.toByteArray());

        assertThat(scaled.triangleCount).isEqualTo(unscaled.triangleCount);
        int i;
        for ( i = 0; i < unscaled.facets.size(); i++ ) {
            Facet a = unscaled.facets.get(i);
            Facet b = scaled.facets.get(i);
            assertVectorClose(b.normal, a.normal, EPSILON);
            assertVectorClose(b.a, a.a.multiply(1000.0), 1.0e-3);
            assertVectorClose(b.b, a.b.multiply(1000.0), 1.0e-3);
            assertVectorClose(b.c, a.c.multiply(1000.0), 1.0e-3);
        }
    }

    @Test
    void given_rotatedSolid_when_exporting_then_preservesOriginalVertexPositionsAndClosedEdges()
        throws Exception
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        Matrix4x4d rotation = new Matrix4x4d();
        rotation = rotation.eulerAnglesRotation(0.37, 0.41, 0.19);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, rotation);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Action
        StlWriter.exportSolid(solid, output);

        // Assert
        ParsedStl stl = parseBinaryStl(output.toByteArray());
        ArrayList<String> expectedVertices = new ArrayList<>();
        int i;
        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            expectedVertices.add(asFloatKey(solid.getVerticesList().get(i).position));
        }

        for ( i = 0; i < stl.facets.size(); i++ ) {
            Facet facet = stl.facets.get(i);
            assertThat(expectedVertices).contains(asFloatKey(facet.a));
            assertThat(expectedVertices).contains(asFloatKey(facet.b));
            assertThat(expectedVertices).contains(asFloatKey(facet.c));
        }
        assertThat(countBoundaryEdges(stl)).isZero();
    }

    @Test
    void given_invalidScaleFactor_when_exporting_then_rejectsArgument()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Action / Assert
        assertThatThrownBy(() -> StlWriter.exportSolid(solid, output, 0.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("scaleFactor");
    }

    @Test
    void given_solidWithCollapsedEdge_when_exporting_then_reportsDegenerateEdge()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidEdge edge = solid.getEdgesList().get(0);
        _PolyhedralBoundedSolidVertex start = edge.rightHalf.startingVertex;
        _PolyhedralBoundedSolidVertex end = edge.leftHalf.startingVertex;
        end.position = start.position;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Action / Assert
        assertThatThrownBy(() -> StlWriter.exportSolid(solid, output))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("edge")
            .hasMessageContaining("degenerate");
    }

    @Test
    void given_solidWithNonPlanarFace_when_exporting_then_reportsFaceCoplanarityFailure()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(0);
        _PolyhedralBoundedSolidHalfEdge halfEdge =
            face.boundariesList.get(0).boundaryStartHalfEdge;
        _PolyhedralBoundedSolidVertex vertex = halfEdge.startingVertex;
        vertex.position = vertex.position.withZ(vertex.position.z() + 0.25);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Action / Assert
        assertThatThrownBy(() -> StlWriter.exportSolid(solid, output))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not coplanar");
    }

    private static ParsedStl parseBinaryStl(byte[] bytes)
        throws Exception
    {
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        byte[] headerBytes = new byte[80];
        PersistenceElement.readBytes(input, headerBytes);
        long triangleCount = PersistenceElement.readLongLE(input);
        ArrayList<Facet> facets = new ArrayList<>();

        long i;
        for ( i = 0; i < triangleCount; i++ ) {
            Vector3Dd normal = readVector(input);
            Vector3Dd a = readVector(input);
            Vector3Dd b = readVector(input);
            Vector3Dd c = readVector(input);
            int attributeByteCount = PersistenceElement.readSignedShortLE(input);
            facets.add(new Facet(normal, a, b, c, attributeByteCount));
        }

        return new ParsedStl(new String(headerBytes).trim(), triangleCount, facets);
    }

    private static Vector3Dd readVector(ByteArrayInputStream input)
        throws Exception
    {
        return new Vector3Dd(
            PersistenceElement.readFloatLE(input),
            PersistenceElement.readFloatLE(input),
            PersistenceElement.readFloatLE(input));
    }

    private static void assertVectorClose(Vector3Dd actual, Vector3Dd expected,
                                          double epsilon)
    {
        assertThat(actual.epsilonEquals(expected, epsilon)).isTrue();
    }

    private static int countBoundaryEdges(ParsedStl stl)
    {
        java.util.HashMap<String, Integer> edgeCounts = new java.util.HashMap<>();
        int i;
        for ( i = 0; i < stl.facets.size(); i++ ) {
            Facet facet = stl.facets.get(i);
            increment(edgeCounts, edgeKey(facet.a, facet.b));
            increment(edgeCounts, edgeKey(facet.b, facet.c));
            increment(edgeCounts, edgeKey(facet.c, facet.a));
        }

        int boundaryEdges = 0;
        for ( int count : edgeCounts.values() ) {
            if ( count == 1 ) {
                boundaryEdges++;
            }
        }
        return boundaryEdges;
    }

    private static void increment(java.util.HashMap<String, Integer> counts,
                                  String key)
    {
        counts.put(key, counts.getOrDefault(key, 0) + 1);
    }

    private static String edgeKey(Vector3Dd a, Vector3Dd b)
    {
        String aKey = asFloatKey(a);
        String bKey = asFloatKey(b);
        if ( aKey.compareTo(bKey) <= 0 ) {
            return aKey + "|" + bKey;
        }
        return bKey + "|" + aKey;
    }

    private static String asFloatKey(Vector3Dd vector)
    {
        return Float.floatToIntBits((float)vector.x()) + ","
            + Float.floatToIntBits((float)vector.y()) + ","
            + Float.floatToIntBits((float)vector.z());
    }

    private static final class ParsedStl {
        final String header;
        final long triangleCount;
        final List<Facet> facets;

        ParsedStl(String header, long triangleCount, List<Facet> facets)
        {
            this.header = header;
            this.triangleCount = triangleCount;
            this.facets = facets;
        }
    }

    private static final class Facet {
        final Vector3Dd normal;
        final Vector3Dd a;
        final Vector3Dd b;
        final Vector3Dd c;
        final int attributeByteCount;

        Facet(Vector3Dd normal, Vector3Dd a, Vector3Dd b,
              Vector3Dd c, int attributeByteCount)
        {
            this.normal = normal;
            this.a = a;
            this.b = b;
            this.c = c;
            this.attributeByteCount = attributeByteCount;
        }
    }
}
