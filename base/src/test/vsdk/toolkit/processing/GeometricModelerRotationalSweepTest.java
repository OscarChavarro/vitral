package vsdk.toolkit.processing;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

import static org.assertj.core.api.Assertions.assertThat;

class GeometricModelerRotationalSweepTest
{
    @Test
    void given_wireWithOneEndpointOnXAxis_when_rotationalSweep_then_returnsValidSolid()
    {
        // Arrange
        PolyhedralBoundedSolid wire = createWireProfile(
            new Vector3D(0.0, 0.0, 0.0),
            new Vector3D(0.8, 0.35, 0.0),
            new Vector3D(1.2, 0.5, 0.0));

        // Action
        GeometricModeler.rotationalSweepExtrudeWireAroundXAxis(wire, 16);

        // Assert
        assertThat(wire.polygonsList.size()).isGreaterThan(0);
        assertThat(wire.edgesList.size()).isGreaterThan(0);
        assertThat(wire.verticesList.size()).isGreaterThan(3);
    }

    @Test
    void given_wireWithBothEndpointsOnXAxis_when_rotationalSweep_then_returnsValidSolid()
    {
        // Arrange
        PolyhedralBoundedSolid wire = createWireProfile(
            new Vector3D(0.0, 0.0, 0.0),
            new Vector3D(0.6, 0.4, 0.0),
            new Vector3D(1.1, 0.0, 0.0));

        // Action
        GeometricModeler.rotationalSweepExtrudeWireAroundXAxis(wire, 18);

        // Assert
        assertThat(wire.polygonsList.size()).isGreaterThan(0);
        assertThat(wire.edgesList.size()).isGreaterThan(0);
        assertThat(wire.verticesList.size()).isGreaterThan(3);
    }

    private static PolyhedralBoundedSolid createWireProfile(Vector3D p0,
                                                            Vector3D p1,
                                                            Vector3D p2)
    {
        PolyhedralBoundedSolid wire = new PolyhedralBoundedSolid();
        wire.mvfs(p0, 1, 1);
        wire.smev(1, 1, 2, p1);
        wire.smev(1, 2, 3, p2);
        return wire;
    }

}
