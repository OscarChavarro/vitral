package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid._PolyhedralBoundedSolidTopologicalValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;

import static org.assertj.core.api.Assertions.assertThat;

/**
Checks half-edge topology invariants for valid and intentionally damaged
bounded solids.

<p>Traceability: [MANT1988] Ch. 10.2-10.4, especially the solid, face,
loop, edge, half-edge, and vertex incidence structure.</p>
 */
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
        _PolyhedralBoundedSolidEdge edge = solid.getEdgesList().get(0);
        edge.leftHalf = null;

        // Action
        boolean result = _PolyhedralBoundedSolidTopologicalValidator
            .validateTopologicalIntegrity(solid);

        // Assert
        assertThat(result).isFalse();
    }
}
