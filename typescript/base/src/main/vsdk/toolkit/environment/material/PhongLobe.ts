import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Df } from "../../common/linealAlgebra/Vector3Df.js";
import { ScatteringComponent } from "./ScatteringComponent.js";
import { ScatteringFrame, scatteringArithmetic } from "./ScatteringFrame.js";

const f = Math.fround;
const { dot, scale } = scatteringArithmetic;

// Scattered power below which nothing is sampled (RenderPark's EPSILON)
const NO_SCATTERING = 1e-6;

const M_1_PI = 1 / Math.PI;

function averageOf(color: ColorRgb): number {
    return f((color.getR() + color.getG() + color.getB()) / 3.0);
}

/** `java::Math::pow(float, float)` of the C++ port */
function powf(a: number, e: number): number {
    return f(Math.pow(a, e));
}

/**
Immutable Phong lobe as MGF materials define one: a diffuse color Kd, a
non-diffuse color Ks and a Phong exponent Ns. The non-diffuse part counts as
the SPECULAR component when Ns reaches LOWEST_SPECULAR_EXPONENT and as the
GLOSSY component below it. The same lobe describes reflection, transmission
or emission; PhongMaterial composes them.

The exponent is stored in single precision, as RenderPark stores it, so that
the glossy/specular threshold classifies every material the same way.

Directional functions (RenderPark's modified Phong model, which is symmetric
and energy conserving): with kd = Kd / pi and ks = Ks (Ns + 2) / (2 pi), the
lobe evaluates to kd + ks cos(a)^Ns, a being the angle between the outgoing
direction and the ideal (mirror or refracted) one. Kd and Ks are the energy
scattered for perpendicular incidence, so Kd + Ks <= 1 conserves energy.
Sampling picks the diffuse part with probability avg(Kd) / (avg(Kd) +
avg(Ks)), cosine distributed around the normal, or the non-diffuse part,
distributed as cos^Ns around the ideal direction (density (Ns + 1) cos^Ns /
(2 pi): it differs from the (Ns + 2) of the evaluation); the density of a
direction is the mixture of both. With Russian roulette the first variate
also decides absorption: the direction survives with probability avg(Kd) +
avg(Ks), and the density returned is then not divided by it.

Conventions: `in` and `out` both leave the surface: `in` points back to
where the incoming light (or importance) comes from, `out` to where it
goes; `normal` is the shading normal, on either side (the densities use
the normal turned to the side of `in`; the transmission density swaps the
indices when in . normal < 0). Densities are per unit solid angle; the
diffuse transmission density keeps RenderPark's sign, cos(theta) / pi with
theta measured from the normal on the side of `in`, so it is negative; the caller supplies the variates x1 and x2. A density of 0 means no
direction was produced (absorption or a direction below the surface).
The arithmetic, its precision and its order are RenderPark's, so results are
bit-identical to RenderPark's BRDF and BTDF. C++ output pointers are
one-element holders.
*/
export class PhongLobe {
    public static readonly LOWEST_SPECULAR_EXPONENT = f(250.0);

    private readonly diffuse: ColorRgb;
    private readonly nonDiffuse: ColorRgb;
    private readonly exponent: number;
    private readonly averageDiffuse: number;
    private readonly averageNonDiffuse: number;

    public constructor(inDiffuse: ColorRgb, inNonDiffuse: ColorRgb, inExponent: number);
    public constructor(other: PhongLobe);
    public constructor(a: ColorRgb | PhongLobe, inNonDiffuse?: ColorRgb, inExponent?: number) {
        if (a instanceof PhongLobe) {
            this.diffuse = a.diffuse;
            this.nonDiffuse = a.nonDiffuse;
            this.exponent = a.exponent;
            this.averageDiffuse = a.averageDiffuse;
            this.averageNonDiffuse = a.averageNonDiffuse;
            return;
        }
        this.diffuse = new ColorRgb(a);
        this.nonDiffuse = new ColorRgb(inNonDiffuse!);
        this.exponent = f(inExponent!);
        this.averageDiffuse = averageOf(a);
        this.averageNonDiffuse = averageOf(inNonDiffuse!);
    }

    public getDiffuse(): ColorRgb {
        return this.diffuse;
    }

    public getNonDiffuse(): ColorRgb {
        return this.nonDiffuse;
    }

    public getExponent(): number {
        return this.exponent;
    }

    public isSpecular(): boolean {
        return this.exponent >= PhongLobe.LOWEST_SPECULAR_EXPONENT;
    }

    /** Average diffuse color when the diffuse component is requested, else 0 */
    private sampledDiffuse(components: number): number {
        return (components & ScatteringComponent.DIFFUSE) !== 0 ? this.averageDiffuse : 0.0;
    }

    /** Average non-diffuse color when its component (glossy or specular) is requested, else 0 */
    private sampledNonDiffuse(components: number): number {
        const nonDiffuseComponent = this.isSpecular() ? ScatteringComponent.SPECULAR : ScatteringComponent.GLOSSY;
        return (components & nonDiffuseComponent) !== 0 ? this.averageNonDiffuse : 0.0;
    }

    /**
    Sum of the colors of the requested components (ScatteringComponent
    DIFFUSE, GLOSSY, SPECULAR): the integrated reflectance, transmittance
    or emittance of the lobe. Accumulated in double precision starting from
    zero, diffuse first.
    */
    public integrated(components: number): ColorRgb {
        let r = 0.0;
        let g = 0.0;
        let b = 0.0;

        if ((components & ScatteringComponent.DIFFUSE) !== 0) {
            r += this.diffuse.getR();
            g += this.diffuse.getG();
            b += this.diffuse.getB();
        }

        const nonDiffuseComponent = this.isSpecular() ? ScatteringComponent.SPECULAR : ScatteringComponent.GLOSSY;
        if ((components & nonDiffuseComponent) !== 0) {
            r += this.nonDiffuse.getR();
            g += this.nonDiffuse.getG();
            b += this.nonDiffuse.getB();
        }

        return new ColorRgb(r, g, b);
    }

    /** ks factor cos(a)^Ns (Ns + 2) / (2 pi), in single precision */
    private nonDiffuseFactor(cosAlpha: number): number {
        let factor = powf(cosAlpha, this.exponent); // cos(a)^Ns
        factor = f(factor * f(f(this.exponent + 2.0) / f(2.0 * f(Math.PI)))); // Ks -> ks
        return factor;
    }

    /** Reflected radiance factor (BRDF value) of the requested components */
    public reflectionEvaluate(inDirection: Vector3Df, out: Vector3Df, normal: Vector3Df, components: number): ColorRgb {
        let r = 0.0;
        let g = 0.0;
        let b = 0.0;
        const inReversed = scale(-1.0, inDirection);

        if (dot(out, normal) < 0) {
            // Below the surface: transmission, not reflection
            return new ColorRgb(0.0, 0.0, 0.0);
        }

        if ((components & ScatteringComponent.DIFFUSE) !== 0 && this.averageDiffuse > 0.0) {
            r += M_1_PI * this.diffuse.getR();
            g += M_1_PI * this.diffuse.getG();
            b += M_1_PI * this.diffuse.getB();
        }

        const nonDiffuseComponent = this.isSpecular() ? ScatteringComponent.SPECULAR : ScatteringComponent.GLOSSY;
        if ((components & nonDiffuseComponent) !== 0 && this.averageNonDiffuse > 0.0) {
            const idealReflected = ScatteringFrame.idealReflection(inReversed, normal);
            const cosAlpha = dot(idealReflected, out);
            if (cosAlpha > 0) {
                const factor = this.nonDiffuseFactor(cosAlpha);
                r += factor * this.nonDiffuse.getR();
                g += factor * this.nonDiffuse.getG();
                b += factor * this.nonDiffuse.getB();
            }
        }
        return new ColorRgb(r, g, b);
    }

    /** Samples a reflected direction; its density goes to density[0] */
    public reflectionSample(
        inDirection: Vector3Df,
        normal: Vector3Df,
        russianRoulette: boolean,
        components: number,
        x1: number,
        x2: number,
        density: number[],
    ): Vector3Df {
        const inReversed = scale(-1.0, inDirection);
        density[0] = 0;

        const localDiffuse = this.sampledDiffuse(components);
        const localNonDiffuse = this.sampledNonDiffuse(components);
        const scatteredPower = localDiffuse + localNonDiffuse;
        if (scatteredPower < NO_SCATTERING) {
            return new Vector3Df(0.0, 0.0, 0.0);
        }

        if (russianRoulette) {
            if (x1 > scatteredPower) {
                // Absorption
                return new Vector3Df(0.0, 0.0, 0.0);
            }
            x1 /= scatteredPower;
        }

        const idealDirection = ScatteringFrame.idealReflection(inReversed, normal);
        // The first variate chooses the part, then is rescaled into [0, 1) within it
        const diffusePart = x1 < localDiffuse / scatteredPower;
        const xPart = diffusePart
            ? x1 / (localDiffuse / scatteredPower)
            : (x1 - localDiffuse / scatteredPower) / (localNonDiffuse / scatteredPower);
        const diffuseDensity: number[] = [0.0];
        const nonDiffuseDensity: number[] = [0.0];
        // Diffuse part cosine distributed around the normal; glossy or specular
        // part cos^Ns distributed around the mirror direction
        const newDirection = diffusePart
            ? new ScatteringFrame(normal).sampleCosine(xPart, x2, diffuseDensity)
            : new ScatteringFrame(idealDirection).samplePhong(this.exponent, xPart, x2, nonDiffuseDensity);

        if (diffusePart) {
            const cosAlpha = dot(idealDirection, newDirection);
            if (cosAlpha > 0) {
                nonDiffuseDensity[0] = ((this.exponent + 1.0) * powf(cosAlpha, this.exponent)) / (2.0 * Math.PI);
            }
        } else {
            const cosTheta = dot(normal, newDirection);
            if (cosTheta <= 0) {
                return newDirection;
            }
            diffuseDensity[0] = cosTheta / Math.PI;
        }

        density[0] = localDiffuse * diffuseDensity[0]! + localNonDiffuse * nonDiffuseDensity[0]!;
        if (!russianRoulette) {
            density[0] = density[0] / scatteredPower;
        }
        return newDirection;
    }

    /**
    Density with which reflectionSample (without Russian roulette) produces
    `out`, and the survival probability of the roulette; the normal is first
    turned to the side of `in`
    */
    public reflectionDensity(
        inDirection: Vector3Df,
        out: Vector3Df,
        normal: Vector3Df,
        components: number,
        density: number[],
        survival: number[],
    ): void {
        const inReversed = scale(-1.0, inDirection);
        density[0] = 0;
        survival[0] = 0;

        // The normal on the side of 'in'
        const cosIn = dot(inDirection, normal);
        const goodNormal = cosIn >= 0 ? normal : scale(-1.0, normal);
        const cosTheta = dot(goodNormal, out);
        if (cosTheta < 0) {
            return;
        }

        const localDiffuse = this.sampledDiffuse(components);
        const localNonDiffuse = this.sampledNonDiffuse(components);
        const scatteredPower = localDiffuse + localNonDiffuse;
        if (scatteredPower < NO_SCATTERING) {
            return;
        }

        let diffuseDensity = 0.0;
        if (this.averageDiffuse > 0) {
            diffuseDensity = cosTheta / Math.PI;
        }

        let nonDiffuseDensity = 0.0;
        if (this.averageNonDiffuse > 0) {
            const idealDirection = ScatteringFrame.idealReflection(inReversed, goodNormal);
            const cosAlpha = dot(idealDirection, out);
            if (cosAlpha > 0) {
                nonDiffuseDensity = ((this.exponent + 1.0) * Math.pow(cosAlpha, this.exponent)) / (2.0 * Math.PI);
            }
        }

        // RenderPark weighs both parts by their averages here, requested or not
        density[0] =
            (this.averageDiffuse * diffuseDensity + this.averageNonDiffuse * nonDiffuseDensity) / scatteredPower;
        survival[0] = scatteredPower;
    }

    /**
    Transmitted radiance factor (BTDF value) of the requested components,
    with the real refraction indices on the side of `in` and beyond
    */
    public transmissionEvaluate(
        inIndex: number,
        outIndex: number,
        inDirection: Vector3Df,
        out: Vector3Df,
        normal: Vector3Df,
        components: number,
    ): ColorRgb {
        const inReversed = scale(-1.0, inDirection);
        let r = 0.0;
        let g = 0.0;
        let b = 0.0;

        if ((components & ScatteringComponent.DIFFUSE) !== 0 && this.averageDiffuse > 0) {
            // Diffuse transmission always crosses the boundary
            const isReflection = dot(normal, out) >= 0;
            if (!isReflection) {
                r += M_1_PI * this.diffuse.getR();
                g += M_1_PI * this.diffuse.getG();
                b += M_1_PI * this.diffuse.getB();
            }
        }

        const nonDiffuseComponent = this.isSpecular() ? ScatteringComponent.SPECULAR : ScatteringComponent.GLOSSY;
        if ((components & nonDiffuseComponent) !== 0 && this.averageNonDiffuse > 0) {
            const totalInternalReflection: boolean[] = [false];
            const idealRefracted = ScatteringFrame.idealRefraction(
                inReversed,
                normal,
                inIndex,
                outIndex,
                totalInternalReflection,
            );
            const cosAlpha = dot(idealRefracted, out);
            if (cosAlpha > 0) {
                const factor = this.nonDiffuseFactor(cosAlpha);
                r += factor * this.nonDiffuse.getR();
                g += factor * this.nonDiffuse.getG();
                b += factor * this.nonDiffuse.getB();
            }
        }
        return new ColorRgb(r, g, b);
    }

    /** Samples a transmitted direction; its density goes to density[0] */
    public transmissionSample(
        inIndex: number,
        outIndex: number,
        inDirection: Vector3Df,
        normal: Vector3Df,
        russianRoulette: boolean,
        components: number,
        x1: number,
        x2: number,
        density: number[],
    ): Vector3Df {
        const inReversed = scale(-1.0, inDirection);
        density[0] = 0;

        const localDiffuse = this.sampledDiffuse(components);
        const localNonDiffuse = this.sampledNonDiffuse(components);
        const scatteredPower = localDiffuse + localNonDiffuse;
        if (scatteredPower < NO_SCATTERING) {
            return new Vector3Df(0.0, 0.0, 0.0);
        }

        if (russianRoulette) {
            if (x1 > scatteredPower) {
                // Absorption
                return new Vector3Df(0.0, 0.0, 0.0);
            }
            x1 /= scatteredPower;
        }

        const totalInternalReflection: boolean[] = [false];
        const idealDirection = ScatteringFrame.idealRefraction(
            inReversed,
            normal,
            inIndex,
            outIndex,
            totalInternalReflection,
        );
        const inverseNormal = scale(-1.0, normal);
        // The first variate chooses the part, then is rescaled into [0, 1) within it
        const diffusePart = x1 < localDiffuse / scatteredPower;
        const xPart = diffusePart
            ? x1 / (localDiffuse / scatteredPower)
            : (x1 - localDiffuse / scatteredPower) / (localNonDiffuse / scatteredPower);
        const diffuseDensity: number[] = [0.0];
        const nonDiffuseDensity: number[] = [0.0];
        // Diffuse part cosine distributed in the transmitted hemisphere; glossy
        // or specular part cos^Ns distributed around the refracted direction
        const newDirection = diffusePart
            ? new ScatteringFrame(inverseNormal).sampleCosine(xPart, x2, diffuseDensity)
            : new ScatteringFrame(idealDirection).samplePhong(this.exponent, xPart, x2, nonDiffuseDensity);

        if (diffusePart) {
            const cosAlpha = dot(idealDirection, newDirection);
            if (cosAlpha > 0) {
                nonDiffuseDensity[0] = ((this.exponent + 1.0) * powf(cosAlpha, this.exponent)) / (2.0 * Math.PI);
            }
        } else {
            // A direction on the side of the normal is taken as total internal reflection
            const cosTheta = dot(normal, newDirection);
            if (cosTheta > 0) {
                diffuseDensity[0] = cosTheta / Math.PI;
            }
        }

        density[0] = localDiffuse * diffuseDensity[0]! + localNonDiffuse * nonDiffuseDensity[0]!;
        if (!russianRoulette) {
            density[0] = density[0] / scatteredPower;
        }
        return newDirection;
    }

    /** Density with which transmissionSample produces `out`, and the roulette survival */
    public transmissionDensity(
        inIndex: number,
        outIndex: number,
        inDirection: Vector3Df,
        out: Vector3Df,
        normal: Vector3Df,
        components: number,
        density: number[],
        survival: number[],
    ): void {
        const inReversed = scale(-1.0, inDirection);
        density[0] = 0;
        survival[0] = 0;

        // The normal on the side of 'in'
        const cosIn = dot(inDirection, normal);
        const goodNormal = cosIn >= 0 ? normal : scale(-1.0, normal);
        const cosTheta = dot(goodNormal, out);

        // Diffuse transmission only for a direction that crossed the surface
        const localDiffuse =
            (components & ScatteringComponent.DIFFUSE) !== 0 && cosTheta < 0 ? this.averageDiffuse : 0.0;
        const localNonDiffuse = this.sampledNonDiffuse(components);
        const scatteredPower = localDiffuse + localNonDiffuse;
        if (scatteredPower < NO_SCATTERING) {
            return;
        }

        let diffuseDensity: number;
        if (localDiffuse > 0) {
            diffuseDensity = cosTheta / Math.PI;
        } else {
            diffuseDensity = 0.0;
        }

        let nonDiffuseDensity = 0.0;
        if (localNonDiffuse > 0) {
            const totalInternalReflection: boolean[] = [false];
            // With the normal turned around, the two sides swap
            const idealDirection =
                cosIn >= 0
                    ? ScatteringFrame.idealRefraction(
                          inReversed,
                          goodNormal,
                          inIndex,
                          outIndex,
                          totalInternalReflection,
                      )
                    : ScatteringFrame.idealRefraction(
                          inReversed,
                          goodNormal,
                          outIndex,
                          inIndex,
                          totalInternalReflection,
                      );
            const cosAlpha = dot(idealDirection, out);
            if (cosAlpha > 0) {
                nonDiffuseDensity = ((this.exponent + 1.0) * Math.pow(cosAlpha, this.exponent)) / (2.0 * Math.PI);
            }
        }

        density[0] = (localDiffuse * diffuseDensity + localNonDiffuse * nonDiffuseDensity) / scatteredPower;
        survival[0] = scatteredPower;
    }
}
