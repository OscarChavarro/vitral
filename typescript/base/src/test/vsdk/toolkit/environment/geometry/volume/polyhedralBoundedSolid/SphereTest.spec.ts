import { describe, expect, it } from "vitest";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
describe("SphereTest", () =>
    it("exports a closed B-rep", () =>
        expect(
            PolyhedralBoundedSolidValidationEngine.validateIntermediate(
                new Sphere(2).exportToPolyhedralBoundedSolid(6, 4),
            ),
        ).toBe(true)));
