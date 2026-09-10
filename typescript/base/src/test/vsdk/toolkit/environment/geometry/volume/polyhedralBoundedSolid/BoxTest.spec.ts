import { describe, expect, it } from "vitest";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
describe("BoxTest", () =>
    it("exports a closed B-rep", () => {
        const s = new Box(2, 4, 6).exportToPolyhedralBoundedSolid();
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(s)).toBe(true);
        expect(s.getEdgesList()).toHaveLength(12);
    }));
