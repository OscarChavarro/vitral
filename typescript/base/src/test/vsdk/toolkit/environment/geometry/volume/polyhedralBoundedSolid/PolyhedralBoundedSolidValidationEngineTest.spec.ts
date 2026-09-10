import { describe, expect, it } from "vitest";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
describe("PolyhedralBoundedSolidValidationEngineTest", () =>
    it("validates strict and boolean prerequisites", () => {
        const s = new Box(2, 2, 2).exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidValidationEngine.resetStrictValidationInvocationCount();
        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(s)).toBe(true);
        expect(PolyhedralBoundedSolidValidationEngine.getStrictValidationInvocationCount()).toBe(1);
        expect(PolyhedralBoundedSolidValidationEngine.validateBooleanInputs(s, s)).toBe(true);
    }));
