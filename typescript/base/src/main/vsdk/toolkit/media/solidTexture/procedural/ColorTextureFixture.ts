import { ColorRgba } from "../../../common/color/ColorRgba.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { RGBAColorPalette } from "../../RGBAColorPalette.js";
import { TextureUtils } from "../TextureUtils.js";
import type { ProceduralNoise } from "./ProceduralNoise.js";

/**
Port of `vsdk.toolkit.media.solidTexture.procedural.ColorTextureFixture`.

The classic solid-texture color functions — agate, bozo, brick, checker,
gradient, granite, marble, spotted, wood, leopard and onion. Each of them
*adds* into the color it is given rather than assigning it, which is the
contract the Java code has and which the caller relies on when it composes two
of them; `addMappedColor` answers whether a palette took over, and the hard
coded color ramps below are the fallback for a null palette.

Java's `%` on doubles is a remainder that keeps the sign of the dividend, and
so is TypeScript's, so `onion` and `brick` need no adjustment.
*/
export class ColorTextureFixture {
    public constructor(
        private readonly proceduralNoise: ProceduralNoise,
        private readonly textureUtils: TextureUtils,
    ) {}

    public agate(
        x: number,
        y: number,
        z: number,
        octaves: number,
        colorMap: RGBAColorPalette | null,
        color: ColorRgba,
    ): void {
        let noise: number =
            this.proceduralNoise.cycloidal(1.3 * this.proceduralNoise.turbulence(x, y, z, octaves) + 1.1 * z) + 1.0;
        noise *= 0.5;
        noise = Math.pow(noise, 0.77);

        if (ColorTextureFixture.addMappedColor(color, colorMap, noise)) {
            return;
        }

        const hue: number = 1.0 - noise;
        if (noise < 0.5) {
            ColorTextureFixture.add(color, 1.0 - noise / 10.0, 1.0 - noise / 5.0, hue, 0.0);
        } else if (noise < 0.6) {
            ColorTextureFixture.add(color, 0.9, 0.7, hue, 0.0);
        } else {
            ColorTextureFixture.add(color, 0.6 + hue, 0.3 + hue, hue, 0.0);
        }
    }

    public bozo(
        x: number,
        y: number,
        z: number,
        turbulence: number,
        octaves: number,
        colorMap: RGBAColorPalette | null,
        color: ColorRgba,
    ): void {
        if (turbulence !== 0.0) {
            const t: Vector3Dd = this.proceduralNoise.differentialTurbulence(x, y, z, octaves);
            x += t.x() * turbulence;
            y += t.y() * turbulence;
            z += t.z() * turbulence;
        }

        const noise: number = this.proceduralNoise.noise(x, y, z);
        if (ColorTextureFixture.addMappedColor(color, colorMap, noise)) {
            return;
        }
        if (noise < 0.4) {
            ColorTextureFixture.add(color, 1.0, 1.0, 1.0, 0.0);
        } else if (noise < 0.6) {
            ColorTextureFixture.add(color, 0.0, 1.0, 0.0, 0.0);
        } else if (noise < 0.8) {
            ColorTextureFixture.add(color, 0.0, 0.0, 1.0, 0.0);
        } else {
            ColorTextureFixture.add(color, 1.0, 0.0, 0.0, 0.0);
        }
    }

    public brick(
        x: number,
        y: number,
        z: number,
        color: ColorRgba,
        color1: ColorRgba,
        color2: ColorRgba,
        mortar: number,
    ): void {
        const xr: number = Math.abs(x % 1.0);
        const yr: number = Math.abs(y % 1.0);
        const zr: number = Math.abs(z % 1.0);
        color.set(color2);
        if (xr > 0.0 && xr < mortar) {
            color.set(color1);
            return;
        }
        if (yr > 0.0 && yr < mortar) {
            color.set(color1);
            return;
        }
        if (zr > 0.0 && zr < mortar) {
            color.set(color1);
        }
    }

    public checker(
        x: number,
        y: number,
        z: number,
        color: ColorRgba,
        color1: ColorRgba,
        color2: ColorRgba,
        smallTolerance: number,
    ): void {
        x += smallTolerance;
        y += smallTolerance;
        z += smallTolerance;
        const index: number = Math.trunc(
            TextureUtils.floorInline(x) + TextureUtils.floorInline(y) + TextureUtils.floorInline(z),
        );
        ColorTextureFixture.addColor(color, (index & 1) !== 0 ? color1 : color2);
    }

    public gradient(
        x: number,
        y: number,
        z: number,
        turbulence: number,
        colorMap: RGBAColorPalette | null,
        textureGradient: Vector3Dd,
        octaves: number,
        color: ColorRgba,
    ): void {
        if (turbulence !== 0.0) {
            const t: Vector3Dd = this.proceduralNoise.differentialTurbulence(x, y, z, octaves);
            x += t.x() * turbulence;
            y += t.y() * turbulence;
            z += t.z() * turbulence;
        }
        if (colorMap === null) {
            return;
        }

        let value = 0.0;
        if (textureGradient.x() !== 0.0) {
            x = TextureUtils.fabsInline(x);
            value += x - TextureUtils.floorInline(x);
        }
        if (textureGradient.y() !== 0.0) {
            y = TextureUtils.fabsInline(y);
            value += y - TextureUtils.floorInline(y);
        }
        if (textureGradient.z() !== 0.0) {
            z = TextureUtils.fabsInline(z);
            value += z - TextureUtils.floorInline(z);
        }
        value = value > 1.0 ? value % 1.0 : value;
        ColorTextureFixture.addMappedColor(color, colorMap, value);
    }

    public granite(x: number, y: number, z: number, colorMap: RGBAColorPalette | null, color: ColorRgba): void {
        let noise = 0.0;
        let freq = 1.0;
        for (let i = 0; i < 6; freq *= 2.0, i++) {
            const temp: number = 0.5 - this.proceduralNoise.noise(x * 4.0 * freq, y * 4.0 * freq, z * 4.0 * freq);
            noise += TextureUtils.fabsInline(temp) / freq;
        }
        if (!ColorTextureFixture.addMappedColor(color, colorMap, noise)) {
            ColorTextureFixture.add(color, noise, noise, noise, 0.0);
        }
    }

    public marble(
        x: number,
        y: number,
        z: number,
        turbulence: number,
        octaves: number,
        colorMap: RGBAColorPalette | null,
        color: ColorRgba,
    ): void {
        const noise: number = this.proceduralNoise.triangleWave(
            x + this.proceduralNoise.turbulence(x, y, z, octaves) * turbulence,
        );
        if (ColorTextureFixture.addMappedColor(color, colorMap, noise)) {
            return;
        }
        if (noise < 0.0) {
            ColorTextureFixture.add(color, 0.9, 0.8, 0.8, 0.0);
        } else if (noise < 0.9) {
            const hue: number = 0.8 - noise * 0.8;
            ColorTextureFixture.add(color, 0.9, hue, hue, 0.0);
        }
    }

    public spotted(x: number, y: number, z: number, colorMap: RGBAColorPalette | null, color: ColorRgba): void {
        const noise: number = this.proceduralNoise.noise(x, y, z);
        if (!ColorTextureFixture.addMappedColor(color, colorMap, noise)) {
            ColorTextureFixture.add(color, noise, noise, noise, 0.0);
        }
    }

    public wood(
        x: number,
        y: number,
        z: number,
        turbulence: number,
        octaves: number,
        colorMap: RGBAColorPalette | null,
        color: ColorRgba,
    ): void {
        const t: Vector3Dd = this.proceduralNoise.differentialTurbulence(x, y, z, octaves);
        const pointX: number = this.proceduralNoise.cycloidal((x + t.x()) * turbulence) + x;
        const pointY: number = this.proceduralNoise.cycloidal((y + t.y()) * turbulence) + y;
        const noise: number = this.proceduralNoise.triangleWave(new Vector3Dd(pointX, pointY, 0.0).length());

        if (ColorTextureFixture.addMappedColor(color, colorMap, noise)) {
            return;
        }
        if (noise > 0.6) {
            ColorTextureFixture.add(color, 0.4, 0.133, 0.066, 0.0);
        } else {
            ColorTextureFixture.add(color, 0.666, 0.312, 0.2, 0.0);
        }
    }

    public leopard(
        x: number,
        y: number,
        z: number,
        turbulence: number,
        octaves: number,
        colorMap: RGBAColorPalette | null,
        color: ColorRgba,
    ): void {
        if (turbulence !== 0.0) {
            const t: Vector3Dd = this.proceduralNoise.differentialTurbulence(x, y, z, octaves);
            x += t.x() * turbulence;
            y += t.y() * turbulence;
            z += t.z() * turbulence;
        }
        const temp: number = (Math.sin(x) + Math.sin(y) + Math.sin(z)) / 3.0;
        const noise: number = temp * temp;
        if (!ColorTextureFixture.addMappedColor(color, colorMap, noise)) {
            ColorTextureFixture.add(color, noise, noise, noise, 0.0);
        }
    }

    public onion(
        x: number,
        y: number,
        z: number,
        turbulence: number,
        octaves: number,
        colorMap: RGBAColorPalette | null,
        color: ColorRgba,
    ): void {
        if (turbulence !== 0.0) {
            const t: Vector3Dd = this.proceduralNoise.differentialTurbulence(x, y, z, octaves);
            x += t.x() * turbulence;
            y += t.y() * turbulence;
            z += t.z() * turbulence;
        }
        const noise: number = Math.sqrt(x * x + y * y + z * z) % 1.0;
        if (!ColorTextureFixture.addMappedColor(color, colorMap, noise)) {
            ColorTextureFixture.add(color, noise, noise, noise, 0.0);
        }
    }

    private static addMappedColor(color: ColorRgba, colorMap: RGBAColorPalette | null, value: number): boolean {
        if (colorMap === null) {
            return false;
        }
        const newColor = new ColorRgba();
        TextureUtils.computeColor(newColor, colorMap, value);
        ColorTextureFixture.addColor(color, newColor);
        return true;
    }

    private static addColor(color: ColorRgba, v: ColorRgba): void {
        ColorTextureFixture.add(color, v.getR(), v.getG(), v.getB(), v.getA());
    }

    private static add(color: ColorRgba, r: number, g: number, b: number, a: number): void {
        color.setR(color.getR() + r);
        color.setG(color.getG() + g);
        color.setB(color.getB() + b);
        color.setA(color.getA() + a);
    }
}
