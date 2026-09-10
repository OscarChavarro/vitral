import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { CubemapBackground } from "vsdk/toolkit/environment/background/CubemapBackground.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { RGBAImageUncompressed } from "vsdk/toolkit/media/RGBAImageUncompressed.js";

function solidImage(r: number, g: number, b: number): RGBAImageUncompressed {
    const image = new RGBAImageUncompressed();
    image.init(2, 2);
    for (let x = 0; x < 2; x++) for (let y = 0; y < 2; y++) image.putPixel(x, y, r, g, b, 255);
    return image;
}

describe("CubemapBackground", () => {
    it("maps each principal direction to its corresponding cube image", () => {
        const front = solidImage(255, 0, 0);
        const right = solidImage(0, 255, 0);
        const back = solidImage(0, 0, 255);
        const left = solidImage(255, 255, 0);
        const down = solidImage(255, 0, 255);
        const up = solidImage(0, 255, 255);
        const camera = new Camera();
        const background = new CubemapBackground(camera, front, right, back, left, down, up);

        expect(background.colorInDireccion(new Vector3Dd(0, 1, 0))).toMatchObject({ rv: 1, gv: 0, bv: 0 });
        expect(background.colorInDireccion(new Vector3Dd(0, -1, 0))).toMatchObject({ rv: 0, gv: 0, bv: 1 });
        expect(background.colorInDireccion(new Vector3Dd(1, 0, 0))).toMatchObject({ rv: 0, gv: 1, bv: 0 });
        expect(background.colorInDireccion(new Vector3Dd(-1, 0, 0))).toMatchObject({ rv: 1, gv: 1, bv: 0 });
        expect(background.colorInDireccion(new Vector3Dd(0, 0, 1))).toMatchObject({ rv: 0, gv: 1, bv: 1 });
        expect(background.colorInDireccion(new Vector3Dd(0, 0, -1))).toMatchObject({ rv: 1, gv: 0, bv: 1 });
        expect(background.getImages()).toEqual([front, right, back, left, down, up]);
        expect(background.getCamera()).toBe(camera);
    });

    it("normalizes directions and permits replacing the camera", () => {
        const image = solidImage(128, 64, 32);
        const background = new CubemapBackground(new Camera(), image, image, image, image, image, image);
        const replacementCamera = new Camera();

        expect(background.colorInDireccion(new Vector3Dd(20, 0, 0))).toMatchObject({
            rv: 128 / 255,
            gv: 64 / 255,
            bv: 32 / 255,
        });
        background.setCamera(replacementCamera);
        expect(background.getCamera()).toBe(replacementCamera);
    });
});
