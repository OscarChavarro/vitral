import { ColorRgba } from "../../common/color/ColorRgba.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { SolidTextureStatistics } from "../../common/statistics/SolidTextureStatistics.js";
import type { RGBAColorPalette } from "../RGBAColorPalette.js";
import { ProceduralNoise } from "./procedural/ProceduralNoise.js";

/**
Java's private nested `CRandom`. `ProceduralNoise` declares its own copy of the
same generator, and so does this file, because the Java sources do; see the
copy there for why the multiplication is split at 2^16.
*/
class _CRandom {
    private static readonly MULTIPLIER = 1103515245;
    private static readonly INCREMENT = 12345;
    private static readonly MODULUS = 0x80000000;

    private state: number;

    public constructor(seed: number) {
        this.state = seed & 0x7fffffff;
    }

    public next(): number {
        const high: number = Math.floor(this.state / 0x10000);
        const low: number = this.state % 0x10000;
        const product: number = ((high * _CRandom.MULTIPLIER) % 0x8000) * 0x10000 + low * _CRandom.MULTIPLIER;
        this.state = (product + _CRandom.INCREMENT) % _CRandom.MODULUS;
        return Math.floor(this.state / 0x10000) & 0x7fff;
    }
}

/**
Port of `vsdk.toolkit.media.solidTexture.TextureUtils`.

The shared services of the solid-texture family: the lazily built
{@link ProceduralNoise}, the `floor` and `abs` helpers the texture fixtures
call by name, the palette lookup, and the wave-source table
{@link BumpTextureFixture} rides on.

Java's `TextureUtils(SolidTextureStatistics)` constructor and its no-argument
one differ only in whether the noise generator is built eagerly with counters
attached; TypeScript spells that as one optional parameter.
*/
export class TextureUtils {
    private static readonly WAVE_RANDOM_MASK = 0x7fff;
    private static readonly WAVE_RANDOM_DIVISOR = TextureUtils.WAVE_RANDOM_MASK;

    private proceduralNoise: ProceduralNoise | null = null;
    private frequencyInstance: Float64Array = new Float64Array(0);
    private waveSourcesInstance: Vector3Dd[] = [];

    public constructor(stats: SolidTextureStatistics | null = null) {
        if (stats !== null) {
            this.proceduralNoise = new ProceduralNoise(stats);
        }
    }

    public initialize(stats: SolidTextureStatistics): void {
        this.proceduralNoise = new ProceduralNoise(stats);
    }

    public getProceduralNoise(): ProceduralNoise {
        if (this.proceduralNoise === null) {
            this.proceduralNoise = new ProceduralNoise();
        }
        return this.proceduralNoise;
    }

    public static floorInline(x: number): number {
        return x >= 0.0 ? Math.floor(x) : 0.0 - Math.floor(0.0 - x) - 1.0;
    }

    public static fabsInline(x: number): number {
        return x < 0.0 ? 0.0 - x : x;
    }

    public waveFrequency(): Float64Array {
        return this.frequencyInstance;
    }

    public waveSources(): Vector3Dd[] {
        return this.waveSourcesInstance;
    }

    public static computeColor(color: ColorRgba, colorMap: RGBAColorPalette, value: number): void {
        color.set(colorMap.evalLinear(value));
    }

    public initializeNoise(numberOfWaves: number): void {
        const noise: ProceduralNoise = this.getProceduralNoise();
        noise.initialize();
        const random = new _CRandom(0);
        for (let i = 0; i < 4096; i++) {
            random.next();
        }
        this.frequencyInstance = new Float64Array(numberOfWaves);
        this.waveSourcesInstance = new Array<Vector3Dd>(numberOfWaves);

        for (let i = 0; i < numberOfWaves; i++) {
            const point: Vector3Dd = noise.differentialNoise(i, 0.0, 0.0).normalized();
            this.waveSourcesInstance[i] = point;
            this.frequencyInstance[i] =
                (random.next() & TextureUtils.WAVE_RANDOM_MASK) / TextureUtils.WAVE_RANDOM_DIVISOR + 0.01;
        }
    }
}
