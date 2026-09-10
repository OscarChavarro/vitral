import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayHit } from "vsdk/toolkit/environment/geometry/element/RayHit.js";
import { Torus } from "vsdk/toolkit/environment/geometry/volume/Torus.js";
describe("TorusTest", () =>
    it("intersects its inner ray and supplies surface data", () => {
        const t = new Torus(3, 1),
            ray = new Ray(new Vector3Dd(), new Vector3Dd(1, 0, 0)),
            hit = new RayHit();
        expect(t.doIntersectionFirstHit(ray, hit)).toBe(true);
        expect(hit.ray()!.getT()).toBeCloseTo(2);
        expect(hit.p!.x()).toBeCloseTo(2);
        expect(hit.n!.x()).toBeCloseTo(-1);
    }));
