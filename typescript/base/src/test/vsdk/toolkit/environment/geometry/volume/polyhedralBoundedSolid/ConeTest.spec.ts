import { describe, expect, it } from "vitest";
import { Cone } from "vsdk/toolkit/environment/geometry/volume/Cone.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
describe("ConeTest", () =>
    it("exports a closed B-rep", () => {
        const s = new Cone(2, 0, 3).exportToPolyhedralBoundedSolid(6);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(s)).toBe(true);
        expect(s.getVerticesList()).toHaveLength(7);
    }));
