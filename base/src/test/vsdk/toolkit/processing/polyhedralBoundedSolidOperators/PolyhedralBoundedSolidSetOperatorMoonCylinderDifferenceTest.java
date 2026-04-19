package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;

import static org.assertj.core.api.Assertions.assertThat;

class PolyhedralBoundedSolidSetOperatorMoonCylinderDifferenceTest
{
    private static final double CYLINDER_RADIUS = 0.5;
    private static final double CYLINDER_HEIGHT = 1.0;
    private static final double BASE_X = 0.55;
    private static final double BASE_Y = 0.55;
    private static final double BASE_Z = 0.05;
    private static final double CUT_DX = 0.275;
    private static final double CUT_DZ = 0.15;
    private static final double EPSILON = 1.0e-6;

    @Test
    void given_equalRadiusConeExportCylinder_when_validating_then_itIsAValidPolyhedralSolid()
    {
        PolyhedralBoundedSolid cylinder = createDemoCylinder();

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(cylinder)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateStrict(cylinder)).isTrue();
    }

    @Test
    void given_offsetDemoCylinders_when_difference_then_itCreatesAValidMoonPrism()
    {
        PolyhedralBoundedSolid cylinderA = createDemoCylinder();
        PolyhedralBoundedSolid cylinderB = createDemoCylinder();
        int originalVertexCount = cylinderA.verticesList.size();

        Matrix4x4 translation = new Matrix4x4();
        translation.translation(CUT_DX, 0.0, CUT_DZ);
        cylinderB.applyTransformation(translation);

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            cylinderA, cylinderB, PolyhedralBoundedSolidModeler.SUBTRACT, false);

        assertThat(result).isNotNull();
        assertThat(result.polygonsList.size()).isGreaterThan(0);
        assertThat(result.verticesList.size()).isGreaterThan(originalVertexCount);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(result)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateStrict(result)).isTrue();
        assertThat(countVerticesAwayFromCylinderCaps(result)).isGreaterThan(0);
    }

    private static PolyhedralBoundedSolid createDemoCylinder()
    {
        PolyhedralBoundedSolid solid = new Cone(
            CYLINDER_RADIUS, CYLINDER_RADIUS, CYLINDER_HEIGHT)
                .exportToPolyhedralBoundedSolid();
        Matrix4x4 move = new Matrix4x4();
        move.translation(BASE_X, BASE_Y, BASE_Z);
        solid.applyTransformation(move);
        return solid;
    }

    private static int countVerticesAwayFromCylinderCaps(
        PolyhedralBoundedSolid solid)
    {
        int count = 0;
        double minInteriorZ = BASE_Z + EPSILON;
        double maxInteriorZ = BASE_Z + CYLINDER_HEIGHT - EPSILON;

        int i;
        for ( i = 0; i < solid.verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex vertex = solid.verticesList.get(i);
            double z = vertex.position.z();
            if ( z > minInteriorZ && z < maxInteriorZ ) {
                count++;
            }
        }
        return count;
    }
}
