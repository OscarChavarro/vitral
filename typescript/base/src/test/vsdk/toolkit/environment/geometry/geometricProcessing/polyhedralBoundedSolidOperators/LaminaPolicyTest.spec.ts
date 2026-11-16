import { describe, expect, it } from "vitest";

import { StringBuilder } from "java/lang/StringBuilder.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { OrientedBox, PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";

const translation = PolyhedralBoundedSolidVolumeOracle.translation;

/**
Lamina policy (decision of 2026-11-16): a lamina, two faces back to back
with zero volume, is a valid result. The intersection of two solids that
share part of a face is returned as such a lamina, not as the regularised
empty set, because it is topologically sound and easy to display. A
collapsed lamina, whose area is below EPSILON scaled by the square of the
model scale, is not valid. Tolerances follow the size of the model, so a
small lamina is valid at its own scale; collapsed means a sliver.
 */
describe("LaminaPolicyTest", () => {
    /**
    A right triangle lamina with legs `length` and `height` (area
    length height / 2) in the plane z = 0. With length 1 the model scale is
    about 1.
    */
    function triangleLamina(length: number, height: number): PolyhedralBoundedSolid {
        const solid = new PolyhedralBoundedSolid();

        PolyhedralBoundedSolidEulerOperators.mvfs(solid, new Vector3Dd(0, 0, 0), 1, 1);
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 2, new Vector3Dd(length, 0, 0));
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 2, 3, new Vector3Dd(0, height, 0));
        PolyhedralBoundedSolidEulerOperators.smef(solid, 1, 3, 1, 2);
        return solid;
    }

    it("given_blocksSharingPartOfAFace_when_intersecting_then_validLamina", () => {
        const a = new OrientedBox(2, 2, 1, translation(0, 0, 0)).toSolid();
        const b = new OrientedBox(1, 1, 1, translation(0.5, 0.5, 1.0)).toSolid();

        const result = PolyhedralBoundedSolidModeler.setOp(a, b, PolyhedralBoundedSolidModeler.INTERSECTION);
        const summary = PolyhedralBoundedSolidTopologySummary.from(result);

        expect(PolyhedralBoundedSolidModeler.isEmptyResult(result)).toBe(false);
        expect(result.getPolygonsList().size()).toBe(2);
        expect(summary.getShellCount()).toBe(1);
        expect(Math.abs(PolyhedralBoundedSolidVolumeOracle.solidVolume(result) - 0.0)).toBeLessThanOrEqual(1.0e-12);
        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(result, new StringBuilder())).toBe(true);
    });

    it("given_laminaWithRealArea_when_validatedStrictly_then_valid", () => {
        const lamina = triangleLamina(1.0, 1.0e-2);

        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(lamina, new StringBuilder())).toBe(true);
    });

    it("given_collapsedLamina_when_validatedStrictly_then_rejected", () => {
        const lamina = triangleLamina(1.0, 1.0e-7);
        const report = new StringBuilder();

        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(lamina, report)).toBe(false);
        // A sliver this thin has no measurable plane either; either report
        // rejects it.
        const text = report.toString();
        expect(text.includes("is collapsed") || text.includes("was not able to compute containing plane"), text).toBe(
            true,
        );
    });
});
