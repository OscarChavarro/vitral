package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;

import static org.assertj.core.api.Assertions.assertThat;

/**
Acceptance tests for the boolean-input preflight stage (Level 1 of the
stage-2 hardening plan, §3.5).

<p>These tests verify that the solid construction pipeline (sphere, cylinder,
star, moon) produces geometrically and topologically valid solids before the
boolean pipeline starts.  They serve as the baseline for the claim that
preprocessing bugs do not contaminate the Intersect/Classify/Connect phases.</p>

<p>Traceability: §3.1 (validateBooleanInputs), §3.2 (Newell faceeq),
§3.3 (IdNamespace), §3.4 (snap in generators), plan stage-2 2026-05-13.</p>
*/
class PolyhedralBoundedSolidPreflightTest
{
    @Test
    void given_kurlanderBowlAndFirstStar_when_validateBooleanInputs_then_passes()
    {
        PolyhedralBoundedSolid[] operands =
            CsgKurlanderBowlFixture.createBowlAndFirstStarOperands();
        PolyhedralBoundedSolid bowl = operands[0];
        PolyhedralBoundedSolid star = operands[1];

        StringBuilder msg = new StringBuilder();
        boolean valid = PolyhedralBoundedSolidValidationEngine
            .validateBooleanInputs(bowl, star, msg);

        assertThat(valid)
            .as("validateBooleanInputs should pass for bowl+star: " + msg)
            .isTrue();
    }

    @Test
    void given_moonAndShell_when_validateBooleanInputs_then_passes()
    {
        PolyhedralBoundedSolid[] operands =
            CsgKurlanderBowlFixture.createShellAndFirstMoonOperands();
        PolyhedralBoundedSolid shell = operands[0];
        PolyhedralBoundedSolid moon  = operands[1];

        StringBuilder msg = new StringBuilder();
        boolean valid = PolyhedralBoundedSolidValidationEngine
            .validateBooleanInputs(shell, moon, msg);

        assertThat(valid)
            .as("validateBooleanInputs should pass for shell+moon: " + msg)
            .isTrue();
    }

    @Test
    void given_sphere16x8_when_inspectingFaces_then_allCoplanarWithinEpsilon()
    {
        Sphere sphere = new Sphere(1.0);
        PolyhedralBoundedSolid solid = sphere.exportToPolyhedralBoundedSolid();

        assertThat(solid.getPolygonsList().size())
            .as("sphere 16x8 must produce faces")
            .isGreaterThan(0);

        boolean valid = PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid);

        assertThat(valid)
            .as("sphere 16x8 must pass validateIntermediate (all faces planar)")
            .isTrue();
    }

    @Test
    void given_twoCylindersWithSameRadius_when_inspectingEachSolid_then_noCoincidentVertices()
    {
        PolyhedralBoundedSolid cylA = PolyhedralBoundedSolidModeler
            .createCircularLamina(0.0, 0.0, 0.15, 0.0, 30);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            cylA, cylA.findFace(1),
            new vsdk.toolkit.common.linealAlgebra.Matrix4x4d()
                .translation(0.0, 0.0, 0.5));

        PolyhedralBoundedSolid cylB = PolyhedralBoundedSolidModeler
            .createCircularLamina(0.11, 0.0, 0.15, 0.06, 30);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            cylB, cylB.findFace(1),
            new vsdk.toolkit.common.linealAlgebra.Matrix4x4d()
                .translation(0.0, 0.0, 0.5));

        PolyhedralBoundedSolidNumericPolicy.ToleranceContext ctxA =
            PolyhedralBoundedSolidNumericPolicy.forSolid(cylA);
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext ctxB =
            PolyhedralBoundedSolidNumericPolicy.forSolid(cylB);

        StringBuilder msgA = new StringBuilder();
        StringBuilder msgB = new StringBuilder();
        boolean noCoincidentA = PolyhedralBoundedSolidGeometricValidator
            .validateNoCoincidentVertices(cylA, ctxA, msgA);
        boolean noCoincidentB = PolyhedralBoundedSolidGeometricValidator
            .validateNoCoincidentVertices(cylB, ctxB, msgB);

        assertThat(noCoincidentA)
            .as("cylA must have no coincident vertices: " + msgA)
            .isTrue();
        assertThat(noCoincidentB)
            .as("cylB must have no coincident vertices: " + msgB)
            .isTrue();
    }
}
