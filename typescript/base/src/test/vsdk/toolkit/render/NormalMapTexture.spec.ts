import { describe, expect, it } from "vitest";
import { NormalMap } from "vsdk/toolkit/media/NormalMap.js";
import { NormalMapTexture } from "vsdk/toolkit/media/NormalMapTexture.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { SimpleBody } from "vsdk/toolkit/environment/scene/SimpleBody.js";

describe("normal map texture", () => {
    it("preserves flat normals and bottom-up row order, and clears with the map", () => {
        const map = new NormalMap();
        map.init(1, 2);
        map.putNormal(0, 0, new Vector3Dd(0, 0, 1));
        map.putNormal(0, 1, new Vector3Dd(1, 0, 0));
        const texture = new NormalMapTexture(map);
        expect(Array.from(texture.samples)).toEqual([1, 0.5, 0.5, 0.5, 0.5, 1]);
        const body = new SimpleBody();
        body.setNormalMap(map);
        expect(body.getNormalMapRgb()).toBeInstanceOf(NormalMapTexture);
        body.setNormalMap(null);
        expect(body.getNormalMapRgb()).toBeNull();
    });
});
