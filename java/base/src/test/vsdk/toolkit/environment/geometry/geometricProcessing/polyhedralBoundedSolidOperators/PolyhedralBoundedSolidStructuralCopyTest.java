package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidStructuralCopy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;

/**
Structural copy ([MANT1988] Sec. 15.8, "scopy"): the copy has the same
topology, geometry and identifiers as the source, passes the same
validation, and shares no node with it, so changing one leaves the other
intact.
 */
class PolyhedralBoundedSolidStructuralCopyTest
{
    static Stream<Arguments> corpusSolids()
    {
        Stream.Builder<Arguments> rows = Stream.builder();
        for ( CsgSampleCorpus sample : CsgSampleCorpus.values() ) {
            rows.add(Arguments.of(sample + "/A", sample, 0));
            rows.add(Arguments.of(sample + "/B", sample, 1));
        }
        return rows.build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("corpusSolids")
    void given_corpusSolid_when_copied_then_copyIsEqualAndIndependent(
        String name, CsgSampleCorpus sample, int which)
    {
        PolyhedralBoundedSolid source =
            CsgSampleCorpusFixtures.createPair(sample)[which];
        boolean sourceStrict = PolyhedralBoundedSolidValidationEngine
            .validateStrict(source, new StringBuilder());

        PolyhedralBoundedSolid copy =
            PolyhedralBoundedSolidStructuralCopy.copy(source);

        assertThat(PolyhedralBoundedSolidTopologySummary.from(copy))
            .hasToString(PolyhedralBoundedSolidTopologySummary.from(source)
                .toString());
        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(copy))
            .isEqualTo(PolyhedralBoundedSolidVolumeOracle.solidVolume(source));
        assertThat(PolyhedralBoundedSolidValidationEngine.validateStrict(
            copy, new StringBuilder())).isEqualTo(sourceStrict);
        assertThat(copy.getMaxVertexId()).isEqualTo(source.getMaxVertexId());
        assertThat(copy.getMaxFaceId()).isEqualTo(source.getMaxFaceId());
        int i;
        for ( i = 0; i < source.getVerticesList().size(); i++ ) {
            assertThat(copy.getVerticesList().get(i))
                .isNotSameAs(source.getVerticesList().get(i));
            assertThat(copy.getVerticesList().get(i).position)
                .isNotSameAs(source.getVerticesList().get(i).position);
            assertThat(copy.getVerticesList().get(i).id)
                .isEqualTo(source.getVerticesList().get(i).id);
        }
        for ( i = 0; i < source.getPolygonsList().size(); i++ ) {
            assertThat(copy.getPolygonsList().get(i).parentSolid)
                .isSameAs(copy);
        }
    }

    @Test
    void given_copy_when_sourceIsChanged_then_copyIsUntouched()
    {
        PolyhedralBoundedSolid source =
            CsgSampleCorpusFixtures.createPair(CsgSampleCorpus.HOLLOW_BRICK)[0];
        PolyhedralBoundedSolid copy =
            PolyhedralBoundedSolidStructuralCopy.copy(source);
        double volume = PolyhedralBoundedSolidVolumeOracle.solidVolume(copy);
        int faces = copy.getPolygonsList().size();
        int i;

        for ( i = 0; i < source.getVerticesList().size(); i++ ) {
            source.getVerticesList().get(i).position = source.getVerticesList()
                .get(i).position.multiply(3.0);
        }
        source.getPolygonsList().get(0).boundariesList.get(0)
            .halfEdgesList.get(0).startingVertex = null;

        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(copy))
            .isEqualTo(volume);
        assertThat(copy.getPolygonsList().size()).isEqualTo(faces);
        assertThat(PolyhedralBoundedSolidValidationEngine.validateStrict(
            copy, new StringBuilder())).isTrue();
    }

    @Test
    void given_null_when_copied_then_null()
    {
        assertThat(PolyhedralBoundedSolidStructuralCopy.copy(null)).isNull();
    }
}
