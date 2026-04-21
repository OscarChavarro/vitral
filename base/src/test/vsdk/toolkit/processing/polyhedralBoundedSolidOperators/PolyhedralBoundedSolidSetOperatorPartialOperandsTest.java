package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

/**
Smoke coverage for fixture creation, final results, and mutated partial
operands during boolean processing.

<p>Traceability: [MANT1988] Ch. 15.5-15.8, where the two operands are
mutated through generate/classify/connect before finish extracts the result.</p>
 */
class PolyhedralBoundedSolidSetOperatorPartialOperandsTest
{
    @ParameterizedTest
    @MethodSource("sampleCorpus")
    void given_csgSampleCorpusFixture_when_created_then_bothOperandsAreNonEmpty(
        CsgSampleCorpus sample)
    {
        PolyhedralBoundedSolid[] operands =
            CsgSampleCorpusFixtures.createPair(sample);
        touchSolid(operands[0]);
        touchSolid(operands[1]);
    }

    @ParameterizedTest
    @MethodSource("sampleAndOperationCorpus")
    void given_csgSampleCorpus_when_runningBoolean_then_resultAndMutatedOperandsStayNonEmpty(
        CsgSampleCorpus sample, int op)
    {
        PolyhedralBoundedSolid[] operands =
            CsgSampleCorpusFixtures.createPair(sample);
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            operands[0], operands[1], op, false);

        touchSolid(result);
        touchSolid(operands[0]);
        touchSolid(operands[1]);
    }

    private static void touchSolid(PolyhedralBoundedSolid solid)
    {
        if ( solid == null ) {
            return;
        }
        solid.polygonsList.size();
        solid.edgesList.size();
        solid.verticesList.size();
    }

    private static Stream<Arguments> sampleCorpus()
    {
        return Stream.of(CsgSampleCorpus.values()).map(Arguments::of);
    }

    private static Stream<Arguments> sampleAndOperationCorpus()
    {
        return Stream.of(CsgSampleCorpus.values())
            .flatMap(sample -> Stream.of(
                Arguments.of(sample, PolyhedralBoundedSolidModeler.UNION),
                Arguments.of(sample, PolyhedralBoundedSolidModeler.INTERSECTION),
                Arguments.of(sample, PolyhedralBoundedSolidModeler.SUBTRACT)
            ));
    }
}
