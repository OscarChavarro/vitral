package vsdk.toolkit.processing;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

import static org.assertj.core.api.Assertions.assertThat;

class CurveModelerGlyphWeldTest
{
    @Test
    void given_contourWithNearDuplicateClosingVertex_when_buildBrep_then_noCoincidentVertexPair()
    {
        ParametricCurve curve = new ParametricCurve();
        curve.setApproximationSteps(8);
        addCorner(curve, 0.326563, 0.640625);
        addCorner(curve, 0.214063, 0.301563);
        addCorner(curve, 0.443750, 0.301563);
        addCorner(curve, 0.329688, 0.640625);
        addCorner(curve, 0.326563, 0.640625);

        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidModeler.createBrepFromParametricCurve(curve);

        assertThat(hasCoincidentVertexPair(solid, 1.0e-3)).isFalse();
        assertThat(solid.getVerticesList().size()).isEqualTo(3);
    }

    @Test
    void given_squareContourWithoutNearDuplicates_when_buildBrep_then_preservesAllVertices()
    {
        ParametricCurve curve = new ParametricCurve();
        curve.setApproximationSteps(8);
        addCorner(curve, 0.0, 0.0);
        addCorner(curve, 1.0, 0.0);
        addCorner(curve, 1.0, 1.0);
        addCorner(curve, 0.0, 1.0);
        addCorner(curve, 0.0, 0.0);

        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidModeler.createBrepFromParametricCurve(curve);

        assertThat(solid.getVerticesList().size()).isEqualTo(4);
    }

    private static void addCorner(ParametricCurve curve, double x, double y)
    {
        Vector3Dd[] point = new Vector3Dd[1];
        point[0] = new Vector3Dd(x, y, 0.0);
        curve.addPoint(point, ParametricCurve.CORNER);
    }

    private static boolean hasCoincidentVertexPair(
        PolyhedralBoundedSolid solid,
        double tolerance)
    {
        int i;
        int j;

        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex firstVertex =
                solid.getVerticesList().get(i);
            for ( j = i + 1; j < solid.getVerticesList().size(); j++ ) {
                _PolyhedralBoundedSolidVertex secondVertex =
                    solid.getVerticesList().get(j);
                if ( Vector3Dd.distance(firstVertex.position,
                    secondVertex.position) < tolerance ) {
                    return true;
                }
            }
        }
        return false;
    }
}
