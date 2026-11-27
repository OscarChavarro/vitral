import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Logger } from "../../common/logging/Logger.js";
import type { Material } from "./Material.js";
import { PhongLobe } from "./PhongLobe.js";
import { ScatteringComponent } from "./ScatteringComponent.js";
import { ScatteringFrame, scatteringArithmetic } from "./ScatteringFrame.js";
import type { SurfaceShadingPoint } from "./SurfaceShadingPoint.js";
import type { Texture } from "./Texture.js";

function isFiniteColor(color: ColorRgb): boolean {
    return Number.isFinite((color.getR() + color.getG() + color.getB()) / 3.0);
}

function copyLobe(lobe: PhongLobe | null): PhongLobe | null {
    return lobe !== null ? new PhongLobe(lobe) : null;
}

function averageOf(color: ColorRgb): number {
    return (color.getR() + color.getG() + color.getB()) / 3.0;
}

// Scattering below which nothing is sampled (RenderPark's EPSILON)
const NO_SCATTERING = 1e-6;

/** Refraction index of a medium in evaluation and sampling: 0 when unknown */
function sampledIndex(medium: PhongMaterial | null): number {
    return medium !== null ? medium.getRefractionIndex() : 0.0;
}

/** Refraction index of a medium in densityOf: vacuum when unknown */
function densityIndex(medium: PhongMaterial | null): number {
    return medium !== null ? medium.getRefractionIndex() : 1.0;
}

/** The part of the scattering a first variate chose */
enum SampledPart {
    TEXTURE_PART,
    REFLECTION_PART,
    TRANSMISSION_PART,
    ABSORPTION,
}

/** Direction sampled within the chosen part, and its density there (0 when none) */
function sampleWithinPart(
    part: SampledPart,
    reflection: PhongLobe | null,
    transmission: PhongLobe | null,
    normal: Vector3Dd,
    inDirection: Vector3Dd,
    inIndex: number,
    outIndex: number,
    reflectionComponents: number,
    transmissionComponents: number,
    x1: number,
    x2: number,
    density: number[],
): Vector3Dd {
    switch (part) {
        case SampledPart.TEXTURE_PART:
            return new ScatteringFrame(normal).sampleCosine(x1, x2, density);
        case SampledPart.REFLECTION_PART:
            if (reflection !== null) {
                return reflection.reflectionSample(inDirection, normal, false, reflectionComponents, x1, x2, density);
            }
            break;
        case SampledPart.TRANSMISSION_PART:
            if (transmission !== null) {
                return transmission.transmissionSample(
                    inIndex,
                    outIndex,
                    inDirection,
                    normal,
                    false,
                    transmissionComponents,
                    x1,
                    x2,
                    density,
                );
            }
            break;
        case SampledPart.ABSORPTION:
            break;
    }
    density[0] = 0.0;
    return new Vector3Dd(0.0, 0.0, 0.0);
}

/** Density of the texture's diffuse scattering: cos / pi */
function textureDensity(out: Vector3Dd, normal: Vector3Dd): number {
    return scatteringArithmetic.dot(normal, out) / Math.PI;
}

/**
Surface material made of Phong lobes, as MGF scenes describe materials: an
optional reflection lobe, an optional transmission lobe, an optional emission
lobe, an optional color texture that replaces the diffuse reflection, a name
and a sidedness (one-sided surfaces scatter and emit on their front only).

It answers the integrated quantities a radiosity solver consumes:
scatteredPower (reflectance plus transmittance by component) and emittance
by component, and the directional ones a raytracer consumes: evaluation,
sampling from supplied variates and the density of a direction, for the
scattering (reflection, transmission and the texture, which scatters as a
diffuse reflector whose albedo is the texture color) and for the emission
(diffuse only). Their conventions are PhongLobe's; the arithmetic is
RenderPark's, so results are bit-identical to its split BSDF and Phong EDF.

Media. Transmission needs the refraction index on both sides of the
surface: the directional scattering functions take the materials of the
medium the ray comes from (inMedium) and of the one beyond (outMedium), and
read their getRefractionIndex(). A null medium in evaluation and sampling
has the index 0 (RenderPark's unset index); in densityOf it has the vacuum
index 1. A material without transmission lobe has the vacuum index.

Ownership and copies. The material owns its lobes and its name; the texture
is shared and read-only. Construction adopts the lobes passed in (null
for an absent lobe). The material is immutable once built. copy() is a deep
copy of the lobes and the name that shares the texture. The geometric
transforms of Material do not change a surface material and return the
material itself. C++ output pointers are one-element holders.
*/
export class PhongMaterial implements Material {
    /**
    The texture color replaces this component of the scattered power
    when a texture is present.
    */
    public static readonly TEXTURED_COMPONENT = ScatteringComponent.REFLECTION_DIFFUSE;

    private readonly name: string;
    private readonly sided: boolean;
    private readonly reflection: PhongLobe | null;
    private readonly transmission: PhongLobe | null;
    private readonly emission: PhongLobe | null;
    private readonly texture: Texture | null;
    private readonly storedRefractionIndexReal: number;
    private readonly storedRefractionIndexImaginary: number;

    public constructor(
        inName: string | null,
        inReflection: PhongLobe | null,
        inTransmission: PhongLobe | null,
        inEmission: PhongLobe | null,
        inTexture: Texture | null,
        inSided: boolean,
    );
    /** As above, with the complex refraction index of the transmission lobe */
    public constructor(
        inName: string | null,
        inReflection: PhongLobe | null,
        inTransmission: PhongLobe | null,
        inEmission: PhongLobe | null,
        inTexture: Texture | null,
        inSided: boolean,
        inRefractionIndexReal: number,
        inRefractionIndexImaginary: number,
    );
    public constructor(
        inName: string | null,
        inReflection: PhongLobe | null,
        inTransmission: PhongLobe | null,
        inEmission: PhongLobe | null,
        inTexture: Texture | null,
        inSided: boolean,
        inRefractionIndexReal = 1.0,
        inRefractionIndexImaginary = 0.0,
    ) {
        this.name = inName !== null ? inName : "";
        this.sided = inSided;
        this.reflection = inReflection;
        this.transmission = inTransmission;
        this.emission = inEmission;
        this.texture = inTexture;
        this.storedRefractionIndexReal = inRefractionIndexReal;
        this.storedRefractionIndexImaginary = inRefractionIndexImaginary;
    }

    public getName(): string {
        return this.name;
    }

    public isSided(): boolean {
        return this.sided;
    }

    public isTextured(): boolean {
        return this.texture !== null;
    }

    public hasEmission(): boolean {
        return this.emission !== null;
    }

    public getReflection(): PhongLobe | null {
        return this.reflection;
    }

    public getTransmission(): PhongLobe | null {
        return this.transmission;
    }

    public getEmission(): PhongLobe | null {
        return this.emission;
    }

    public getTexture(): Texture | null {
        return this.texture;
    }

    /**
    Real part of the refraction index seen by a ray that crosses the
    surface: the transmission lobe's, 1 (vacuum) without one
    */
    public getRefractionIndex(): number {
        return this.transmission !== null ? this.storedRefractionIndexReal : 1.0;
    }

    /** Imaginary part of the transmission lobe's refraction index, 0 without one */
    public getRefractionIndexImaginary(): number {
        return this.transmission !== null ? this.storedRefractionIndexImaginary : 0.0;
    }

    /** Stored refraction index, also without transmission lobe (for persistence) */
    public getStoredRefractionIndexReal(): number {
        return this.storedRefractionIndexReal;
    }

    public getStoredRefractionIndexImaginary(): number {
        return this.storedRefractionIndexImaginary;
    }

    /**
    Integrated reflectance plus transmittance of the requested components
    (ScatteringComponent mask covering both lobes). When the material is
    textured and TEXTURED_COMPONENT is requested, the texture color at
    textureCoordinate (its x and y) is added first and that component is
    then left out of the reflection lobe. A null textureCoordinate means
    the coordinate is unknown: a warning is reported and the texture adds
    nothing.
    */
    public scatteredPower(components: number, textureCoordinate: Vector3Dd | null): ColorRgb {
        let r = 0.0;
        let g = 0.0;
        let b = 0.0;

        if (this.texture !== null && (components & PhongMaterial.TEXTURED_COMPONENT) !== 0) {
            if (textureCoordinate === null) {
                Logger.reportMessage(this, Logger.WARNING, "scatteredPower", "Couldn't get texture coordinates");
            } else {
                const textureColor = this.texture.evaluateColor(textureCoordinate.x(), textureCoordinate.y());
                r += textureColor.getR();
                g += textureColor.getG();
                b += textureColor.getB();
            }
            components &= ~PhongMaterial.TEXTURED_COMPONENT;
        }

        if (this.reflection !== null) {
            const reflectance = this.reflection.integrated(ScatteringComponent.reflectionComponents(components));
            if (!isFiniteColor(reflectance)) {
                Logger.reportMessage(this, Logger.FATAL_ERROR, "scatteredPower", "Reflectance is not finite");
            }
            r += reflectance.getR();
            g += reflectance.getG();
            b += reflectance.getB();
        }

        if (this.transmission !== null) {
            const transmittance = this.transmission.integrated(ScatteringComponent.transmissionComponents(components));
            if (!isFiniteColor(transmittance)) {
                Logger.reportMessage(this, Logger.FATAL_ERROR, "scatteredPower", "Transmittance is not finite");
            }
            r += transmittance.getR();
            g += transmittance.getG();
            b += transmittance.getB();
        }

        return new ColorRgb(r, g, b);
    }

    /**
    Integrated emittance of the requested components (ScatteringComponent
    DIFFUSE, GLOSSY, SPECULAR), black when the material does not emit.
    */
    public emittance(components: number): ColorRgb {
        if (this.emission === null) {
            return new ColorRgb(0.0, 0.0, 0.0);
        }
        return this.emission.integrated(components);
    }

    public copy(): Material {
        return new PhongMaterial(
            this.name,
            copyLobe(this.reflection),
            copyLobe(this.transmission),
            copyLobe(this.emission),
            this.texture,
            this.sided,
            this.storedRefractionIndexReal,
            this.storedRefractionIndexImaginary,
        );
    }

    public translate(_vector: Vector3Dd | null): Material {
        return this;
    }

    public rotate(_vector: Vector3Dd | null): Material {
        return this;
    }

    public scale(_vector: Vector3Dd | null): Material {
        return this;
    }

    /** Texture color at the point; black without texture or texture coordinate (reported) */
    private textureColor(point: SurfaceShadingPoint): ColorRgb {
        if (this.texture === null) {
            return new ColorRgb(0.0, 0.0, 0.0);
        }
        if (!point.hasTextureCoordinate()) {
            Logger.reportMessage(this, Logger.WARNING, "textureColor", "Couldn't get texture coordinates");
            return new ColorRgb(0.0, 0.0, 0.0);
        }
        return this.texture.evaluateColor(point.getTextureCoordinate().x(), point.getTextureCoordinate().y());
    }

    /**
    Average albedos of the texture, of the reflection without the textured
    component, and of the transmission, and the components left to each lobe
    */
    private samplingProbabilities(
        point: SurfaceShadingPoint,
        components: number,
        texturePart: number[],
        reflectionPart: number[],
        transmissionPart: number[],
        reflectionComponents: number[],
        transmissionComponents: number[],
    ): void {
        texturePart[0] = 0.0;
        if (this.texture !== null && (components & PhongMaterial.TEXTURED_COMPONENT) !== 0) {
            texturePart[0] = averageOf(this.textureColor(point));
            components &= ~PhongMaterial.TEXTURED_COMPONENT;
        }
        reflectionComponents[0] = ScatteringComponent.reflectionComponents(components);
        transmissionComponents[0] = ScatteringComponent.transmissionComponents(components);
        reflectionPart[0] =
            this.reflection !== null ? averageOf(this.reflection.integrated(reflectionComponents[0])) : 0.0;
        transmissionPart[0] =
            this.transmission !== null ? averageOf(this.transmission.integrated(transmissionComponents[0])) : 0.0;
    }

    /**
    Scattering (BSDF) value for the requested components (ScatteringComponent
    mask over both lobes). The transmission part is added only when the
    material has a reflection lobe, as RenderPark does.
    */
    public scatteringEvaluate(
        point: SurfaceShadingPoint,
        inMedium: PhongMaterial | null,
        outMedium: PhongMaterial | null,
        inDirection: Vector3Dd,
        out: Vector3Dd,
        components: number,
    ): ColorRgb {
        let r = 0.0;
        let g = 0.0;
        let b = 0.0;
        if (!point.hasShadingNormal()) {
            Logger.reportMessage(this, Logger.WARNING, "scatteringEvaluate", "Couldn't determine shading normal");
            return new ColorRgb(r, g, b);
        }
        const normal = point.getShadingNormal();

        if (this.texture !== null && (components & PhongMaterial.TEXTURED_COMPONENT) !== 0) {
            const textureValue = 1.0 / Math.PI;
            const color = this.textureColor(point);
            r += textureValue * color.getR();
            g += textureValue * color.getG();
            b += textureValue * color.getB();
            components &= ~PhongMaterial.TEXTURED_COMPONENT;
        }

        if (this.reflection !== null) {
            const reflected = this.reflection.reflectionEvaluate(
                inDirection,
                out,
                normal,
                ScatteringComponent.reflectionComponents(components),
            );
            r += reflected.getR();
            g += reflected.getG();
            b += reflected.getB();
            if (this.transmission !== null) {
                const transmitted = this.transmission.transmissionEvaluate(
                    sampledIndex(inMedium),
                    sampledIndex(outMedium),
                    inDirection,
                    out,
                    normal,
                    ScatteringComponent.transmissionComponents(components),
                );
                r += transmitted.getR();
                g += transmitted.getG();
                b += transmitted.getB();
            } else {
                r += 0.0;
                g += 0.0;
                b += 0.0;
            }
        }
        return new ColorRgb(r, g, b);
    }

    /**
    Samples a scattered direction: the first variate chooses the texture,
    the reflection or the transmission in proportion to their average
    albedos (and absorption, with Russian roulette), then the chosen part
    samples the direction; density[0] is the density of the direction summed
    over all parts, 0 when no direction was produced
    */
    public scatteringSample(
        point: SurfaceShadingPoint,
        inMedium: PhongMaterial | null,
        outMedium: PhongMaterial | null,
        inDirection: Vector3Dd,
        russianRoulette: boolean,
        components: number,
        x1: number,
        x2: number,
        density: number[],
    ): Vector3Dd {
        density[0] = 0;
        if (!point.hasShadingNormal()) {
            Logger.reportMessage(this, Logger.WARNING, "scatteringSample", "Couldn't determine shading normal");
            return new Vector3Dd(0.0, 0.0, 1.0);
        }
        const normal = point.getShadingNormal();

        const texturePartHolder: number[] = [0];
        const reflectionPartHolder: number[] = [0];
        const transmissionPartHolder: number[] = [0];
        const reflectionComponents: number[] = [0];
        const transmissionComponents: number[] = [0];
        this.samplingProbabilities(
            point,
            components,
            texturePartHolder,
            reflectionPartHolder,
            transmissionPartHolder,
            reflectionComponents,
            transmissionComponents,
        );
        let texturePart = texturePartHolder[0]!;
        let reflectionPart = reflectionPartHolder[0]!;
        let transmissionPart = transmissionPartHolder[0]!;
        const scattering = texturePart + reflectionPart + transmissionPart;
        if (scattering < NO_SCATTERING) {
            return new Vector3Dd(0.0, 0.0, 0.0);
        }

        if (!russianRoulette) {
            // No absorption: the parts are normalized
            texturePart /= scattering;
            reflectionPart /= scattering;
            transmissionPart /= scattering;
        }

        // The first variate chooses the part, then is rescaled into [0, 1)
        let mode = SampledPart.ABSORPTION;
        if (x1 < texturePart) {
            mode = SampledPart.TEXTURE_PART;
            x1 /= texturePart;
        } else {
            x1 -= texturePart;
            if (x1 < reflectionPart) {
                mode = SampledPart.REFLECTION_PART;
                x1 /= reflectionPart;
            } else {
                x1 -= reflectionPart;
                if (x1 < transmissionPart) {
                    mode = SampledPart.TRANSMISSION_PART;
                    x1 /= transmissionPart;
                }
            }
        }

        const inIndex = sampledIndex(inMedium);
        const outIndex = sampledIndex(outMedium);

        const p: number[] = [0];
        const out = sampleWithinPart(
            mode,
            this.reflection,
            this.transmission,
            normal,
            inDirection,
            inIndex,
            outIndex,
            reflectionComponents[0]!,
            transmissionComponents[0]!,
            x1,
            x2,
            p,
        );
        if (mode === SampledPart.ABSORPTION) {
            density[0] = 0;
            return out;
        }
        if (p[0]! < NO_SCATTERING) {
            return out;
        }
        density[0] =
            (mode === SampledPart.TEXTURE_PART
                ? texturePart
                : mode === SampledPart.REFLECTION_PART
                  ? reflectionPart
                  : transmissionPart) * p[0]!;

        // Density of the same direction through the parts not chosen
        if (mode !== SampledPart.TEXTURE_PART) {
            p[0] = textureDensity(out, normal);
            density[0] = density[0] + texturePart * p[0];
        }
        const survival: number[] = [0];
        if (mode !== SampledPart.REFLECTION_PART) {
            if (this.reflection === null) {
                p[0] = 0.0;
            } else {
                this.reflection.reflectionDensity(inDirection, out, normal, reflectionComponents[0]!, p, survival);
            }
            density[0] = density[0] + reflectionPart * p[0]!;
        }
        if (mode !== SampledPart.TRANSMISSION_PART) {
            if (this.transmission === null) {
                p[0] = 0.0;
            } else {
                this.transmission.transmissionDensity(
                    inIndex,
                    outIndex,
                    inDirection,
                    out,
                    normal,
                    transmissionComponents[0]!,
                    p,
                    survival,
                );
            }
            density[0] = density[0] + transmissionPart * p[0]!;
        }
        return out;
    }

    /**
    Density with which scatteringSample (without Russian roulette)
    produces `out`, and the survival probability of the roulette
    */
    public scatteringDensity(
        point: SurfaceShadingPoint,
        inMedium: PhongMaterial | null,
        outMedium: PhongMaterial | null,
        inDirection: Vector3Dd,
        out: Vector3Dd,
        components: number,
        density: number[],
        survival: number[],
    ): void {
        density[0] = survival[0] = 0.0;
        if (!point.hasShadingNormal()) {
            Logger.reportMessage(this, Logger.WARNING, "scatteringDensity", "Couldn't determine shading normal");
            return;
        }
        const normal = point.getShadingNormal();

        const texturePart: number[] = [0];
        const reflectionPart: number[] = [0];
        const transmissionPart: number[] = [0];
        const reflectionComponents: number[] = [0];
        const transmissionComponents: number[] = [0];
        this.samplingProbabilities(
            point,
            components,
            texturePart,
            reflectionPart,
            transmissionPart,
            reflectionComponents,
            transmissionComponents,
        );
        const scattering = texturePart[0]! + reflectionPart[0]! + transmissionPart[0]!;
        if (scattering < NO_SCATTERING) {
            return;
        }
        survival[0] = scattering;

        const inIndex = densityIndex(inMedium);
        const outIndex = densityIndex(outMedium);
        const p: number[] = [textureDensity(out, normal)];
        const pSurvival: number[] = [0];
        density[0] = texturePart[0]! * p[0]!;
        if (this.reflection === null) {
            p[0] = 0.0;
        } else {
            this.reflection.reflectionDensity(inDirection, out, normal, reflectionComponents[0]!, p, pSurvival);
        }
        density[0] = density[0] + reflectionPart[0]! * p[0]!;
        if (this.transmission === null) {
            p[0] = 0.0;
        } else {
            this.transmission.transmissionDensity(
                inIndex,
                outIndex,
                inDirection,
                out,
                normal,
                transmissionComponents[0]!,
                p,
                pSurvival,
            );
        }
        density[0] = density[0] + transmissionPart[0]! * p[0]!;
        density[0] = density[0] / scattering;
    }

    /**
    scatteringEvaluate of each requested component on its own, into
    componentValues[i] for component 1 << i (black for the others, six
    entries); returns their sum
    */
    public scatteringEvaluateComponents(
        point: SurfaceShadingPoint,
        inMedium: PhongMaterial | null,
        outMedium: PhongMaterial | null,
        inDirection: Vector3Dd,
        out: Vector3Dd,
        components: number,
        componentValues: ColorRgb[],
    ): ColorRgb {
        let r = 0.0;
        let g = 0.0;
        let b = 0.0;
        for (let i = 0; i < 2 * ScatteringComponent.COMPONENTS_PER_LOBE; i++) {
            const component = 1 << i;
            if ((components & component) !== 0) {
                const value = this.scatteringEvaluate(point, inMedium, outMedium, inDirection, out, component);
                componentValues[i] = value;
                r += value.getR();
                g += value.getG();
                b += value.getB();
            } else {
                componentValues[i] = new ColorRgb(0.0, 0.0, 0.0);
            }
        }
        return new ColorRgb(r, g, b);
    }

    /**
    Emitted radiance into `out` of the requested emission components (only
    the diffuse one emits: Kd / pi), and, when density is not null, the
    density of `out` under emissionSample. Black when the point or its
    normal is unknown (reported), or `out` is below the surface.
    */
    public emissionEvaluate(
        point: SurfaceShadingPoint | null,
        out: Vector3Dd,
        components: number,
        density: number[] | null,
    ): ColorRgb {
        if (density !== null) {
            density[0] = 0.0;
        }
        if (point === null || !point.hasShadingNormal()) {
            Logger.reportMessage(this, Logger.WARNING, "emissionEvaluate", "Couldn't determine shading normal");
            return new ColorRgb(0.0, 0.0, 0.0);
        }
        const cosL = scatteringArithmetic.dot(out, point.getShadingNormal());
        if (cosL < 0.0 || this.emission === null) {
            return new ColorRgb(0.0, 0.0, 0.0);
        }
        if ((components & ScatteringComponent.DIFFUSE) !== 0) {
            // kd = Kd / pi, with 1 / pi in single precision as RenderPark stores it
            const toRadiance = 1.0 / Math.PI;
            if (density !== null) {
                density[0] = cosL / Math.PI;
            }
            return new ColorRgb(
                0.0 + toRadiance * this.emission.getDiffuse().getR(),
                0.0 + toRadiance * this.emission.getDiffuse().getG(),
                0.0 + toRadiance * this.emission.getDiffuse().getB(),
            );
        }
        return new ColorRgb(0.0, 0.0, 0.0);
    }

    /**
    Samples an emitted direction (cosine distributed around the normal when
    the diffuse component is requested; (0, 0, 1) otherwise or when the
    normal is unknown); the radiance along it and its density go to the
    non-null holders
    */
    public emissionSample(
        point: SurfaceShadingPoint | null,
        components: number,
        xi1: number,
        xi2: number,
        radiance: ColorRgb[] | null,
        density: number[] | null,
    ): Vector3Dd {
        if (radiance !== null) {
            radiance[0] = new ColorRgb(0.0, 0.0, 0.0);
        }
        if (density !== null) {
            density[0] = 0.0;
        }
        if ((components & ScatteringComponent.DIFFUSE) === 0) {
            return new Vector3Dd(0.0, 0.0, 1.0);
        }
        if (point === null || !point.hasShadingNormal()) {
            Logger.reportMessage(this, Logger.WARNING, "emissionSample", "Couldn't determine shading normal");
            return new Vector3Dd(0.0, 0.0, 1.0);
        }
        const sampleDensity: number[] = [0];
        const direction = new ScatteringFrame(point.getShadingNormal()).sampleCosine(xi1, xi2, sampleDensity);
        if (density !== null) {
            density[0] = sampleDensity[0]!;
        }
        if (radiance !== null && this.emission !== null) {
            const toRadiance = 1.0 / Math.PI;
            radiance[0] = new ColorRgb(
                toRadiance * this.emission.getDiffuse().getR(),
                toRadiance * this.emission.getDiffuse().getG(),
                toRadiance * this.emission.getDiffuse().getB(),
            );
        }
        return direction;
    }
}
