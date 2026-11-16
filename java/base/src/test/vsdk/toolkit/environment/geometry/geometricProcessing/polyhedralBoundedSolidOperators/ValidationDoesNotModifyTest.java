package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidRepair;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

import static org.assertj.core.api.Assertions.assertThat;

/**
Validation only reads (plan-stage11 Phase C). Until then every validation
rebuilt the vertex back-pointers and removed the vertices it found without
half-edges, so a validator could turn an invalid solid into a valid one and
report it as valid.

<p>The snapshot compared here is the whole structure: the order of the
face, loop, half-edge, edge and vertex lists, every link between nodes,
every id and every position.</p>
 */
class ValidationDoesNotModifyTest
{
    @Test
    void given_validSolids_when_everyValidatorRuns_then_nothingChanges()
    {
        for ( CsgSampleCorpus sample : CsgSampleCorpus.values() ) {
            PolyhedralBoundedSolid[] pair =
                CsgSampleCorpusFixtures.createPair(sample);
            assertUntouched(sample + "/A", pair[0], pair[1]);
            assertUntouched(sample + "/B", pair[1], pair[0]);
        }
    }

    @Test
    void given_booleanResult_when_everyValidatorRuns_then_nothingChanges()
    {
        PolyhedralBoundedSolid star = PrismFixtures.prism(
            PrismFixtures.regularPolygon(5, 1.0), 1.0);
        PolyhedralBoundedSolid prism = PrismFixtures.prism(
            PrismFixtures.regularPolygon(9, 0.8), 1.2);
        PolyhedralBoundedSolidModeler.applyTransformation(prism,
            PolyhedralBoundedSolidVolumeOracle.translation(0.2, 0.1, 0.3)
                .multiply(PolyhedralBoundedSolidVolumeOracle.rotation(0.6,
                    1, 2, 3)));
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            star, prism, PolyhedralBoundedSolidModeler.SUBTRACT);

        assertUntouched("prism - prism", result, star);
    }

    /**
    A vertex whose emanating half-edge was lost is reported as invalid and
    left as it is; the explicit repair fixes it and says so.
    */
    @Test
    void given_brokenBackPointer_when_validated_then_reportedAndNotRepaired()
    {
        PolyhedralBoundedSolid solid = PrismFixtures.prism(
            PrismFixtures.regularPolygon(6, 1.0), 1.0);
        _PolyhedralBoundedSolidVertex vertex = solid.getVerticesList().get(3);
        vertex.emanatingHalfEdge = null;
        String broken = snapshot(solid);

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isFalse();
        assertThat(PolyhedralBoundedSolidValidationEngine.validateStrict(
            solid, new StringBuilder())).isFalse();
        assertThat(vertex.emanatingHalfEdge).isNull();
        assertThat(snapshot(solid)).isEqualTo(broken);

        PolyhedralBoundedSolidRepair.Report report =
            PolyhedralBoundedSolidRepair.remakeEmanatingHalfEdgeReferences(
                solid);

        assertThat(report.getRelinkedVertices()).isEqualTo(1);
        assertThat(report.getRemovedVertices()).isZero();
        assertThat(vertex.emanatingHalfEdge).isNotNull();
        assertThat(PolyhedralBoundedSolidValidationEngine.validateStrict(
            solid, new StringBuilder())).isTrue();
        assertThat(PolyhedralBoundedSolidRepair
            .remakeEmanatingHalfEdgeReferences(solid).isClean()).isTrue();
    }

    /**
    validateBooleanInputs used to weld the coincident vertices it found.
    Now an operand with an edge of zero length is reported and left alone.
    */
    @Test
    void given_coincidentVertices_when_booleanInputsAreValidated_then_reportedAndNotWelded()
    {
        PolyhedralBoundedSolid solid = PrismFixtures.prism(
            PrismFixtures.regularPolygon(6, 1.0), 1.0);
        PolyhedralBoundedSolid other = PrismFixtures.prism(
            PrismFixtures.regularPolygon(4, 1.0), 1.0);
        _PolyhedralBoundedSolidEdge edge = solid.getEdgesList().get(0);
        _PolyhedralBoundedSolidHalfEdge he = edge.rightHalf;
        vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid
            .PolyhedralBoundedSolidEulerOperators.lmev(solid, he,
                he.mirrorHalfEdge().next(), solid.getMaxVertexId() + 1,
                he.startingVertex.position);
        String before = snapshot(solid);
        StringBuilder message = new StringBuilder();

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateBooleanInputs(solid, other, message)).isFalse();

        assertThat(message.toString()).contains("coincident vertices");
        assertThat(snapshot(solid)).isEqualTo(before);
    }

    private static void assertUntouched(String name,
        PolyhedralBoundedSolid solid, PolyhedralBoundedSolid other)
    {
        String before = snapshot(solid);
        String otherBefore = snapshot(other);

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        assertThat(snapshot(solid)).as(name + " validateIntermediate")
            .isEqualTo(before);
        PolyhedralBoundedSolidValidationEngine.validateStrict(solid);
        assertThat(snapshot(solid)).as(name + " validateStrict")
            .isEqualTo(before);
        PolyhedralBoundedSolidValidationEngine.validateStrict(solid,
            new StringBuilder());
        assertThat(snapshot(solid)).as(name + " validateStrict with message")
            .isEqualTo(before);
        PolyhedralBoundedSolidValidationEngine.validateBooleanInputs(solid,
            other, new StringBuilder());
        assertThat(snapshot(solid)).as(name + " validateBooleanInputs")
            .isEqualTo(before);
        assertThat(snapshot(other)).as(name + " validateBooleanInputs, other")
            .isEqualTo(otherBefore);
    }

    /** Every list order, link, id and position of the solid, as text. */
    static String snapshot(PolyhedralBoundedSolid solid)
    {
        StringBuilder text = new StringBuilder();
        int i;
        int j;
        int k;

        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            text.append("F").append(face.id)
                .append(face.parentSolid == solid ? "" : " foreign");
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(j);
                text.append(" L[start=").append(halfEdge(
                    loop.boundaryStartHalfEdge))
                    .append(loop.parentFace == face ? "" : " foreign");
                for ( k = 0; k < loop.halfEdgesList.size(); k++ ) {
                    _PolyhedralBoundedSolidHalfEdge he =
                        loop.halfEdgesList.get(k);
                    text.append(' ').append(halfEdge(he))
                        .append(he.parentLoop == loop ? "" : "!")
                        .append('v').append(he.startingVertex == null ?
                            "null" : "" + he.startingVertex.id)
                        .append('e').append(he.parentEdge == null ?
                            "null" : "" + he.parentEdge.id);
                }
                text.append(']');
            }
            text.append('\n');
        }
        for ( i = 0; i < solid.getEdgesList().size(); i++ ) {
            _PolyhedralBoundedSolidEdge edge = solid.getEdgesList().get(i);
            text.append("E").append(edge.id).append(' ')
                .append(halfEdge(edge.rightHalf)).append(' ')
                .append(halfEdge(edge.leftHalf)).append('\n');
        }
        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex v = solid.getVerticesList().get(i);
            text.append("V").append(v.id).append(' ')
                .append(halfEdge(v.emanatingHalfEdge)).append(' ')
                .append(Double.doubleToLongBits(v.position.x())).append(' ')
                .append(Double.doubleToLongBits(v.position.y())).append(' ')
                .append(Double.doubleToLongBits(v.position.z()))
                .append('\n');
        }
        text.append("maxF=").append(solid.getMaxFaceId()).append(" maxV=")
            .append(solid.getMaxVertexId());
        return text.toString();
    }

    private static String halfEdge(_PolyhedralBoundedSolidHalfEdge he)
    {
        return he == null ? "null" : "h" + he.id;
    }
}
