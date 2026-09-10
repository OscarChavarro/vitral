import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { FixedBackground } from "vsdk/toolkit/environment/background/FixedBackground.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { RGBAImageUncompressed } from "vsdk/toolkit/media/RGBAImageUncompressed.js";

describe("FixedBackground", () => {
    it("retains its image and camera while preserving Java's unimplemented sampler", () => {
        const camera = new Camera();
        const image = new RGBAImageUncompressed();
        image.init(1, 1);
        const replacement = new RGBAImageUncompressed();
        replacement.init(2, 2);
        const background = new FixedBackground(camera, image);

        expect(background.getCamera()).toBe(camera);
        expect(background.getImage()).toBe(image);
        expect(background.colorInDireccion(new Vector3Dd(0, 0, 1))).toBeNull();

        background.setImage(replacement);
        expect(background.getImage()).toBe(replacement);
    });
});
