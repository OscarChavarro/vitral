import { describe, expect, it } from "vitest";

import { StringBuilder } from "java/lang/StringBuilder.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { PolyhedralBoundedSolidGeometricValidator } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.js";
import { PolyhedralBoundedSolidNumericPolicy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { CsgKurlanderBowlFixture } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.js";

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
describe("PolyhedralBoundedSolidPreflightTest", () => {
    /**
    Harness accommodation: JUnit has no per-test time budget; the Kurlander
    fixtures need more than vitest's 5 s default.
    */
    const KURLANDER_TIMEOUT_MS = 600_000;

    it(
        "given_kurlanderBowlAndFirstStar_when_validateBooleanInputs_then_passes",
        () => {
            const operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands();
            const bowl = operands[0]!;
            const star = operands[1]!;

            const msg = new StringBuilder();
            const valid = PolyhedralBoundedSolidValidationEngine.validateBooleanInputs(bowl, star, msg);

            expect(valid, "validateBooleanInputs should pass for bowl+star: " + msg).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it(
        "given_moonAndShell_when_validateBooleanInputs_then_passes",
        () => {
            const operands = CsgKurlanderBowlFixture.createShellAndFirstMoonOperands();
            const shell = operands[0]!;
            const moon = operands[1]!;

            const msg = new StringBuilder();
            const valid = PolyhedralBoundedSolidValidationEngine.validateBooleanInputs(shell, moon, msg);

            expect(valid, "validateBooleanInputs should pass for shell+moon: " + msg).toBe(true);
        },
        KURLANDER_TIMEOUT_MS,
    );

    it("given_sphere16x8_when_inspectingFaces_then_allCoplanarWithinEpsilon", () => {
        const sphere = new Sphere(1.0);
        const solid = sphere.exportToPolyhedralBoundedSolid();

        expect(solid.getPolygonsList().size(), "sphere 16x8 must produce faces").toBeGreaterThan(0);

        const valid = PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);

        expect(valid, "sphere 16x8 must pass validateIntermediate (all faces planar)").toBe(true);
    });

    it("given_twoCylindersWithSameRadius_when_inspectingEachSolid_then_noCoincidentVertices", () => {
        const cylA = PolyhedralBoundedSolidModeler.createCircularLamina(0.0, 0.0, 0.15, 0.0, 30);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            cylA,
            cylA.findFace(1)!,
            new Matrix4x4d().translation(0.0, 0.0, 0.5),
        );

        const cylB = PolyhedralBoundedSolidModeler.createCircularLamina(0.11, 0.0, 0.15, 0.06, 30);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            cylB,
            cylB.findFace(1)!,
            new Matrix4x4d().translation(0.0, 0.0, 0.5),
        );

        const ctxA = PolyhedralBoundedSolidNumericPolicy.forSolid(cylA);
        const ctxB = PolyhedralBoundedSolidNumericPolicy.forSolid(cylB);

        const msgA = new StringBuilder();
        const msgB = new StringBuilder();
        const noCoincidentA = PolyhedralBoundedSolidGeometricValidator.validateNoCoincidentVertices(cylA, ctxA, msgA);
        const noCoincidentB = PolyhedralBoundedSolidGeometricValidator.validateNoCoincidentVertices(cylB, ctxB, msgB);

        expect(noCoincidentA, "cylA must have no coincident vertices: " + msgA).toBe(true);
        expect(noCoincidentB, "cylB must have no coincident vertices: " + msgB).toBe(true);
    });
});
