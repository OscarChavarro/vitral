package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

import static org.assertj.core.api.Assertions.assertThat;

class PolyhedralBoundedSolidValidationEngineTest
{
    @Test
    void given_mantFixture_when_validateIntermediate_then_returnsTrueAndMarksSolidValid()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createMant1986_1Solid();

        // Action
        boolean result = PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid);

        // Assert
        assertThat(result).isTrue();
        assertThat(solid.isValid()).isTrue();
    }

    @Test
    void given_validBoxSolid_when_validateStrict_then_returnsTrueAndMarksSolidValid()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);

        // Action
        boolean result = PolyhedralBoundedSolidValidationEngine
            .validateStrict(solid);

        // Assert
        assertThat(result).isTrue();
        assertThat(solid.isValid()).isTrue();
    }

    @Test
    void given_loopWithoutStartHalfEdge_when_validateStrict_then_returnsFalseAndMarksSolidInvalid()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidFace face = solid.polygonsList.get(0);
        _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(0);
        loop.boundaryStartHalfEdge = null;

        // Action
        boolean result = PolyhedralBoundedSolidValidationEngine
            .validateStrict(solid);

        // Assert
        assertThat(result).isFalse();
        assertThat(solid.isValid()).isFalse();
    }
}
