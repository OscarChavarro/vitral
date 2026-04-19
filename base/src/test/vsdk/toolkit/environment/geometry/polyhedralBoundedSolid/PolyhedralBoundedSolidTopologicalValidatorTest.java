package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid._PolyhedralBoundedSolidTopologicalValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;

import static org.assertj.core.api.Assertions.assertThat;

class PolyhedralBoundedSolidTopologicalValidatorTest
{
    @Test
    void given_validBoxSolid_when_validateTopologicalIntegrity_then_returnsTrue()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);

        // Action
        boolean result = _PolyhedralBoundedSolidTopologicalValidator
            .validateTopologicalIntegrity(solid);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void given_edgeWithMissingHalfEdge_when_validateTopologicalIntegrity_then_returnsFalse()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidEdge edge = solid.edgesList.get(0);
        edge.leftHalf = null;

        // Action
        boolean result = _PolyhedralBoundedSolidTopologicalValidator
            .validateTopologicalIntegrity(solid);

        // Assert
        assertThat(result).isFalse();
    }
}
