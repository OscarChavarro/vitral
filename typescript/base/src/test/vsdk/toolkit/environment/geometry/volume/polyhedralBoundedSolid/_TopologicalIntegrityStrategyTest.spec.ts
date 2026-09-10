import { describe, expect, it } from "vitest";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidNumericPolicy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { _TopologicalIntegrityStrategy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_TopologicalIntegrityStrategy.js";
import { _PolyhedralBoundedSolidFace } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import { _PolyhedralBoundedSolidLoop } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
describe("_TopologicalIntegrityStrategyTest", () =>
    it("rejects an empty boundary", () => {
        const s = new PolyhedralBoundedSolid(),
            f = new _PolyhedralBoundedSolidFace(s, 1);
        new _PolyhedralBoundedSolidLoop(f);
        expect(
            new _TopologicalIntegrityStrategy().validate(s, PolyhedralBoundedSolidNumericPolicy.defaultContext(), []),
        ).toBe(false);
    }));
