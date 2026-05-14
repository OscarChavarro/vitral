package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTestFixtures;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;

/**
Acceptance tests for the V/F classifier after removal of the "borrowed
wMANT2008" dead branch ([MANT1988] Ch. 15.6.1, problem 15.4).

<p>Each scenario forces the boolean pipeline to exercise the coplanar
vertex/face path — i.e., a vertex of one operand lies exactly on a face
of the other, or two faces overlap on a shared plane — and verifies the
resulting B-rep is valid and has the expected topology. The classifier
under test is
{@link _PolyhedralBoundedSolidSetVertexFaceClassifier}, whose
{@code vertexFaceReclassifyOnEdges}/{@code vertexFaceInsertNullEdges}
families collapsed to a single "no-peek" implementation in §5.1 of
plan-csg-boolean-fix-stage2.</p>

<p>Mapping to Mäntylä's figures:
<ul>
<li>Figure 15.9 — face-touching coplanar pair → covered by
{@code touchingBoxesUnion/Intersection}.</li>
<li>Figure 15.10 — vertex of A on edge of B's coplanar face → covered by
{@code halfOffsetTouchingBoxesUnion}.</li>
<li>Figure 15.11 — partial coplanar face overlap → covered by
{@code partiallyOverlappingFacesDifference}.</li>
<li>Figure 15.12 — coplanar vertex with sectors crossing the boundary →
covered by {@code lShapedExternalContactUnion}.</li>
</ul>
</p>
 */
class VertexFaceClassifierCoplanarTest
{
    private static Method vertexFaceClassifyMethod;

    @BeforeAll
    static void resolveReflectionHandles() throws Exception
    {
        vertexFaceClassifyMethod =
            _PolyhedralBoundedSolidSetVertexFaceClassifier.class
                .getDeclaredMethod("vertexFaceClassify",
                    vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid
                        .nodes._PolyhedralBoundedSolidVertex.class,
                    vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid
                        .nodes._PolyhedralBoundedSolidFace.class,
                    int.class, int.class,
                    PolyhedralBoundedSolid.class, PolyhedralBoundedSolid.class);
        vertexFaceClassifyMethod.setAccessible(true);
    }

    @Test
    void given_classifier_when_inspectingApi_then_borrowedBranchIsRemoved()
        throws Exception
    {
        // Regression guard: the §5.1 cleanup must leave no method named with
        // the legacy "Borrowed" or "NoPeekVersion" suffix in the V/F classifier.
        Method[] declared =
            _PolyhedralBoundedSolidSetVertexFaceClassifier.class
                .getDeclaredMethods();
        ArrayList<String> survivors = new ArrayList<String>();
        for ( Method m : declared ) {
            String name = m.getName();
            if ( name.endsWith("Borrowed") || name.endsWith("NoPeekVersion") ) {
                survivors.add(name);
            }
        }
        assertThat(survivors)
            .as("V/F classifier still carries legacy 'borrowed wMANT2008' methods")
            .isEmpty();

        // The chosen replacement method must exist with the canonical name.
        assertThat(vertexFaceClassifyMethod).isNotNull();
    }

    @Test
    void given_touchingBoxes_when_runningUnion_then_resultIsValidTwoShellPair()
    {
        // Two unit cubes sharing only a single face produce a touching-only
        // result: the pipeline correctly preserves both shells (Euler=4)
        // because the V/F classifier classifies the shared face as boundary
        // rather than interior.
        PolyhedralBoundedSolid[] pair =
            PolyhedralBoundedSolidTestFixtures.createTouchingBoxPair();

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            pair[0], pair[1], PolyhedralBoundedSolidModeler.UNION, false);

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(result.getVerticesList().size()
            - result.getEdgesList().size()
            + result.getPolygonsList().size())
            .as("Euler characteristic for two-shell union")
            .isEqualTo(4);
        double[] minmax = result.getMinMax();
        assertThat(minmax[0]).isEqualTo(-0.5);
        assertThat(minmax[3]).isEqualTo(1.5);
    }

    @Test
    void given_halfOffsetTouchingBoxes_when_runningUnion_then_brepIsValid()
    {
        // Two unit cubes sharing only part of their x=0.5 face (B is shifted
        // half a unit in z), so several vertices of B fall on the interior of
        // A's face — classic figure-15.10 vertex-on-edge case.
        PolyhedralBoundedSolid solidA = createBox(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        PolyhedralBoundedSolid solidB = createBox(1.0, 1.0, 1.0, 1.0, 0.0, 0.5);

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            solidA, solidB, PolyhedralBoundedSolidModeler.UNION, false);

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        // L-shaped extrusion: 12 vertices, 18 edges, 8 faces (V-E+F = 2).
        assertThat(result.getVerticesList().size()
            - result.getEdgesList().size()
            + result.getPolygonsList().size())
            .as("Euler characteristic for L-shaped union")
            .isEqualTo(2);
    }

    @Test
    void given_partiallyOverlappingFaces_when_runningIntersection_then_contactSliceProduced()
    {
        // Stacked half-overlap pair: shared face is a sub-rectangle of A's
        // top face and B's bottom face → figure 15.11 partial coplanar overlap.
        PolyhedralBoundedSolid[] pair = CsgSampleCorpusFixtures.createPair(
            CsgSampleCorpus.STACKED_BLOCKS);

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            pair[0], pair[1], PolyhedralBoundedSolidModeler.INTERSECTION,
            false);

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        // Contact lamina: 2 coincident polygons (top and bottom of zero-thickness slab)
        assertThat(result.getPolygonsList().size()).isEqualTo(2);
        assertThat(result.getVerticesList().size()).isEqualTo(4);
    }

    @Test
    void given_lShapedExternalContact_when_runningUnion_then_brepIsValid()
    {
        // L-shaped pair from MANT1988 §15.1 — its UNION exercises the
        // coplanar V/F path at the inner-corner vertex (figure 15.12 analog).
        PolyhedralBoundedSolid[] pair =
            SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1();

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            pair[0], pair[1], PolyhedralBoundedSolidModeler.UNION, false);

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(result.getVerticesList().size()
            - result.getEdgesList().size()
            + result.getPolygonsList().size())
            .as("Euler characteristic for MANT1988 §15.1 union")
            .isEqualTo(2);
    }

    private static PolyhedralBoundedSolid createBox(
        double sx, double sy, double sz, double tx, double ty, double tz)
    {
        Box box = new Box(new Vector3Dd(sx, sy, sz));
        PolyhedralBoundedSolid solid = box.exportToPolyhedralBoundedSolid();
        Matrix4x4d translation = new Matrix4x4d();
        translation = translation.translation(tx, ty, tz);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, translation);
        return solid;
    }
}
