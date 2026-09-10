import { describe, expect, it } from "vitest";
import { Arrow } from "vsdk/toolkit/environment/geometry/volume/Arrow.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
describe("ArrowTest", () =>
    it("exports a closed B-rep", () =>
        expect(
            PolyhedralBoundedSolidValidationEngine.validateIntermediate(
                new Arrow(2, 3, 1, 0.8).exportToPolyhedralBoundedSolid(6),
            ),
        ).toBe(true)));
