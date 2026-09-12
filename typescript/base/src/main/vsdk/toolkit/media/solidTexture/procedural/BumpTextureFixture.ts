import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { TextureUtils } from "../TextureUtils.js";
import type { ProceduralNoise } from "./ProceduralNoise.js";

/**
Port of `vsdk.toolkit.media.solidTexture.procedural.BumpTextureFixture`.

The normal-perturbing counterpart of {@link ColorTextureFixture}: ripples and
waves ride the wave-source table `TextureUtils.initializeNoise` fills, and
bumps, dents and wrinkles ride the differential noise directly.

Java offers each function twice, once returning the new normal and once writing
it through a one-element array, which is how Java spells an out parameter. Only
the returning flavor is ported, since the other carries no behavior of its own.
*/
export class BumpTextureFixture {
    public constructor(
        private readonly proceduralNoise: ProceduralNoise,
        private readonly textureUtils: TextureUtils,
    ) {}

    public ripples(
        x: number,
        y: number,
        z: number,
        bumpAmount: number,
        frequency: number,
        phase: number,
        numberOfWaves: number,
        normal: Vector3Dd,
    ): Vector3Dd {
        let out: Vector3Dd = normal;
        for (let i = 0; i < numberOfWaves; i++) {
            let point: Vector3Dd = new Vector3Dd(x, y, z).subtract(this.textureUtils.waveSources()[i]!);
            let length: number = point.dotProduct(point);
            if (length === 0.0) {
                length = 1.0;
            }
            length = Math.sqrt(length);
            const index: number = length * frequency + phase;
            const scalar: number = this.proceduralNoise.cycloidal(index) * bumpAmount;
            point = point.multiply(scalar / length / numberOfWaves);
            out = out.add(point);
        }
        return out.normalized();
    }

    public waves(
        x: number,
        y: number,
        z: number,
        bumpAmount: number,
        frequency: number,
        phase: number,
        numberOfWaves: number,
        normal: Vector3Dd,
    ): Vector3Dd {
        let out: Vector3Dd = normal;
        for (let i = 0; i < numberOfWaves; i++) {
            let point: Vector3Dd = new Vector3Dd(x, y, z).subtract(this.textureUtils.waveSources()[i]!);
            let length: number = point.dotProduct(point);
            if (length === 0.0) {
                length = 1.0;
            }
            length = Math.sqrt(length);
            const index: number = length * frequency * this.textureUtils.waveFrequency()[i]! + phase;
            const scalar: number =
                (this.proceduralNoise.cycloidal(index) * bumpAmount) / this.textureUtils.waveFrequency()[i]!;
            point = point.multiply(scalar / length / numberOfWaves);
            out = out.add(point);
        }
        return out.normalized();
    }

    public bumps(x: number, y: number, z: number, bumpAmount: number, normal: Vector3Dd): Vector3Dd {
        if (bumpAmount === 0.0) {
            return normal;
        }
        const bumpTurbulence: Vector3Dd = this.proceduralNoise.differentialNoise(x, y, z).multiply(bumpAmount);
        return normal.add(bumpTurbulence).normalized();
    }

    public dents(x: number, y: number, z: number, bumpAmount: number, normal: Vector3Dd): Vector3Dd {
        if (bumpAmount === 0.0) {
            return normal;
        }
        let noise: number = this.proceduralNoise.noise(x, y, z);
        noise = noise * noise * noise * bumpAmount;
        const stuccoTurbulence: Vector3Dd = this.proceduralNoise.differentialNoise(x, y, z).multiply(noise);
        return normal.add(stuccoTurbulence).normalized();
    }

    public wrinkles(x: number, y: number, z: number, bumpAmount: number, normal: Vector3Dd): Vector3Dd {
        if (bumpAmount === 0.0) {
            return normal;
        }
        let rx = 0.0;
        let ry = 0.0;
        let rz = 0.0;
        for (let i = 0; i < 10; i++) {
            const scale: number = Math.pow(2.0, i);
            const value: Vector3Dd = this.proceduralNoise.differentialNoise(x * scale, y * scale, z * scale);
            rx += TextureUtils.fabsInline(value.x() / scale);
            ry += TextureUtils.fabsInline(value.y() / scale);
            rz += TextureUtils.fabsInline(value.z() / scale);
        }
        const result: Vector3Dd = new Vector3Dd(rx, ry, rz).multiply(bumpAmount);
        return normal.add(result).normalized();
    }
}
