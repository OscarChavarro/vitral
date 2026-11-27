// Port of cpp/base/src/test/phong_scattering_tests.cpp
import { describe, it } from "vitest";
import { ColorRgb } from "vsdk/toolkit/common/color/ColorRgb.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PhongLobe } from "vsdk/toolkit/environment/material/PhongLobe.js";
import { PhongMaterial } from "vsdk/toolkit/environment/material/PhongMaterial.js";
import { ScatteringComponent } from "vsdk/toolkit/environment/material/ScatteringComponent.js";
import { ScatteringFrame, scatteringArithmetic } from "vsdk/toolkit/environment/material/ScatteringFrame.js";
import { SurfaceShadingPoint } from "vsdk/toolkit/environment/material/SurfaceShadingPoint.js";
import {
    expectEq,
    expectFalse,
    expectFloatEq,
    expectGe,
    expectGt,
    expectLt,
    expectNear,
    expectTrue,
} from "../../../../GTestAssertions.js";

// Directions leave the surface (`in` back to the light), see PhongLobe.
// Directional functions of the Phong scattering model (plan step 2b of
// RenderPark's VITRAL alignment): conventions, normalization, sampling
// against density, roulette and component masks.

const UP = new Vector3Dd(0.0, 0.0, 1.0);

// A deterministic low-discrepancy pair: Hammersley points
function radicalInverse(i: number): number {
    let inverse = 0.0;
    let base = 0.5;
    while (i !== 0) {
        if ((i & 1) !== 0) {
            inverse += base;
        }
        base *= 0.5;
        i >>>= 1;
    }
    return inverse;
}

function unit(x: number, y: number, z: number): Vector3Dd {
    return scatteringArithmetic.normalize(new Vector3Dd(x, y, z), 1e-6);
}

function pointWithNormal(normal: Vector3Dd): SurfaceShadingPoint {
    return new SurfaceShadingPoint(normal, true, new Vector3Dd(), false);
}

describe("ScatteringFrame", () => {
    it("IsOrthonormalAndSamplesTheUpperHemisphere", () => {
        const axis = unit(0.3, -0.5, 0.8);
        const frame = new ScatteringFrame(axis);
        expectNear(0.0, scatteringArithmetic.dot(frame.getX(), frame.getZ()), 1e-6);
        expectNear(0.0, scatteringArithmetic.dot(frame.getY(), frame.getZ()), 1e-6);
        expectNear(0.0, scatteringArithmetic.dot(frame.getX(), frame.getY()), 1e-6);
        expectNear(1.0, scatteringArithmetic.dot(frame.getX(), frame.getX()), 1e-6);
        for (let i = 0; i < 256; i++) {
            const density = [0];
            const d = frame.sampleCosine((i + 0.5) / 256.0, radicalInverse(i), density);
            const cosTheta = scatteringArithmetic.dot(d, axis);
            expectGe(cosTheta, -1e-6);
            expectNear(cosTheta / Math.PI, density[0]!, 1e-6);
        }
    });

    it("IdealDirections", () => {
        const inDirection = unit(1.0, 0.0, -1.0);
        const mirror = ScatteringFrame.idealReflection(inDirection, UP);
        expectNear(Math.sqrt(0.5), mirror.x(), 1e-6);
        expectNear(Math.sqrt(0.5), mirror.z(), 1e-6);
        const totalInternalReflection = [true];
        // Same index on both sides: the ray goes straight on
        const through = ScatteringFrame.idealRefraction(inDirection, UP, 1.5, 1.5, totalInternalReflection);
        expectFalse(totalInternalReflection[0]!);
        expectNear(inDirection.x(), through.x(), 1e-6);
        expectNear(inDirection.z(), through.z(), 1e-6);
        // Grazing ray from glass to air: total internal reflection
        const grazing = unit(1.0, 0.0, -0.2);
        ScatteringFrame.idealRefraction(grazing, UP, 1.5, 1.0, totalInternalReflection);
        expectTrue(totalInternalReflection[0]!);
    });
});

describe("PhongLobe", () => {
    it("PureDiffuseEvaluatesToKdOverPiAndReflectsKd", () => {
        const lobe = new PhongLobe(new ColorRgb(0.6, 0.3, 0.15), new ColorRgb(0.0, 0.0, 0.0), 0.0);
        const inDirection = unit(0.2, 0.1, 1.0);
        const above = lobe.reflectionEvaluate(inDirection, unit(0.5, 0.5, 0.7), UP, ScatteringComponent.ALL);
        expectNear(0.6 / Math.PI, above.getR(), 1e-12);
        const below = lobe.reflectionEvaluate(inDirection, unit(0.5, 0.5, -0.7), UP, ScatteringComponent.ALL);
        expectEq(0.0, below.getR());
        // Integrated reflectance: E[f cos / p] over cosine samples
        const frame = new ScatteringFrame(UP);
        let sum = 0.0;
        const n = 4096;
        for (let i = 0; i < n; i++) {
            const density = [0];
            const out = frame.sampleCosine((i + 0.5) / n, radicalInverse(i), density);
            sum +=
                (lobe.reflectionEvaluate(inDirection, out, UP, ScatteringComponent.ALL).getR() *
                    scatteringArithmetic.dot(out, UP)) /
                density[0]!;
        }
        expectNear(0.6, sum / n, 1e-3);
    });

    it("GlossyEvaluationAndSamplingUseTheirOwnNormalizations", () => {
        // Evaluation peaks at Ks (Ns + 2) / (2 pi); the sampled density at (Ns + 1) / (2 pi)
        const ns = 20.0;
        const lobe = new PhongLobe(new ColorRgb(0.0, 0.0, 0.0), new ColorRgb(0.5, 0.5, 0.5), ns);
        const inDirection = UP;
        const peak = lobe.reflectionEvaluate(inDirection, UP, UP, ScatteringComponent.GLOSSY);
        expectNear((0.5 * (ns + 2.0)) / (2.0 * Math.PI), peak.getR(), 1e-5);
        const density = [0];
        const survival = [0];
        lobe.reflectionDensity(inDirection, UP, UP, ScatteringComponent.GLOSSY, density, survival);
        expectNear((ns + 1.0) / (2.0 * Math.PI), density[0]!, 1e-4);
        expectNear(0.5, survival[0]!, 1e-7);
        // The density integrates to 1 over the hemisphere (cosine-sampled estimate)
        const frame = new ScatteringFrame(UP);
        let integral = 0.0;
        const n = 1 << 16;
        for (let i = 0; i < n; i++) {
            const cosineDensity = [0];
            const out = frame.sampleCosine((i + 0.5) / n, radicalInverse(i), cosineDensity);
            lobe.reflectionDensity(inDirection, out, UP, ScatteringComponent.GLOSSY, density, survival);
            integral += density[0]! / cosineDensity[0]!;
        }
        expectNear(1.0, integral / n, 2e-2);
    });

    it("SampledDensityMatchesTheIndependentDensity", () => {
        const mixture = new PhongLobe(new ColorRgb(0.4, 0.3, 0.2), new ColorRgb(0.2, 0.2, 0.2), 12.0);
        const inDirection = unit(0.4, -0.3, 0.8);
        let produced = 0;
        for (let i = 0; i < 512; i++) {
            const sampled = [0];
            const out = mixture.reflectionSample(
                inDirection,
                UP,
                false,
                ScatteringComponent.ALL,
                (i + 0.5) / 512.0,
                radicalInverse(i),
                sampled,
            );
            if (sampled[0] === 0.0) {
                continue;
            }
            produced++;
            const density = [0];
            const survival = [0];
            mixture.reflectionDensity(inDirection, out, UP, ScatteringComponent.ALL, density, survival);
            expectNear(sampled[0]!, density[0]!, 1e-4 * (1.0 + density[0]!));
        }
        expectGt(produced, 400);
    });

    it("RouletteAbsorbsAndDisabledComponentsSampleNothing", () => {
        const lobe = new PhongLobe(new ColorRgb(0.3, 0.3, 0.3), new ColorRgb(0.0, 0.0, 0.0), 0.0);
        const inDirection = UP;
        const density = [0];
        // Survival probability 0.3: a first variate above it is absorbed
        lobe.reflectionSample(inDirection, UP, true, ScatteringComponent.ALL, 0.5, 0.5, density);
        expectEq(0.0, density[0]);
        // Below it the direction survives and the density is not divided by the survival
        const out = lobe.reflectionSample(inDirection, UP, true, ScatteringComponent.ALL, 0.15, 0.5, density);
        expectNear((0.3 * scatteringArithmetic.dot(out, UP)) / Math.PI, density[0]!, 1e-6);
        // Requested component absent: nothing scattered
        lobe.reflectionSample(inDirection, UP, false, ScatteringComponent.GLOSSY, 0.1, 0.5, density);
        expectEq(0.0, density[0]);
        // Zero power lobe
        const black = new PhongLobe(new ColorRgb(0.0, 0.0, 0.0), new ColorRgb(0.0, 0.0, 0.0), 0.0);
        black.reflectionSample(inDirection, UP, false, ScatteringComponent.ALL, 0.1, 0.5, density);
        expectEq(0.0, density[0]);
    });

    it("DiffuseTransmissionCrossesTheSurface", () => {
        const lobe = new PhongLobe(new ColorRgb(0.5, 0.5, 0.5), new ColorRgb(0.0, 0.0, 0.0), 0.0);
        const inDirection = UP;
        expectNear(
            0.5 / Math.PI,
            lobe.transmissionEvaluate(1.0, 1.5, inDirection, unit(0.1, 0.0, -1.0), UP, ScatteringComponent.ALL).getR(),
            1e-12,
        );
        expectEq(
            0.0,
            lobe.transmissionEvaluate(1.0, 1.5, inDirection, unit(0.1, 0.0, 1.0), UP, ScatteringComponent.ALL).getR(),
        );
        const density = [0];
        const out = lobe.transmissionSample(
            1.0,
            1.5,
            inDirection,
            UP,
            false,
            ScatteringComponent.ALL,
            0.3,
            0.6,
            density,
        );
        expectLt(scatteringArithmetic.dot(out, UP), 0.0);
        const independent = [0];
        const survival = [0];
        lobe.transmissionDensity(1.0, 1.5, inDirection, out, UP, ScatteringComponent.ALL, independent, survival);
        expectNear(density[0]!, -independent[0]!, 1e-6); // RenderPark's diffuse transmission density is negative
    });
});

describe("PhongMaterial", () => {
    it("SampledScatteringDensityMatchesScatteringDensity", () => {
        const glass = new PhongMaterial(
            "glass",
            new PhongLobe(new ColorRgb(0.3, 0.3, 0.3), new ColorRgb(0.2, 0.2, 0.2), 30.0),
            new PhongLobe(new ColorRgb(0.0, 0.0, 0.0), new ColorRgb(0.4, 0.4, 0.4), 30.0),
            null,
            null,
            false,
            1.5,
            0.0,
        );
        const air = new PhongMaterial("air", null, null, null, null, false);
        expectFloatEq(1.5, glass.getRefractionIndex());
        expectFloatEq(1.0, air.getRefractionIndex());
        const point = pointWithNormal(UP);
        const inDirection = unit(0.3, 0.2, 0.9);
        const all =
            ScatteringComponent.REFLECTION_DIFFUSE |
            ScatteringComponent.REFLECTION_GLOSSY |
            ScatteringComponent.TRANSMISSION_GLOSSY;
        let produced = 0;
        for (let i = 0; i < 512; i++) {
            const sampled = [0];
            const out = glass.scatteringSample(
                point,
                air,
                glass,
                inDirection,
                false,
                all,
                (i + 0.5) / 512.0,
                radicalInverse(i),
                sampled,
            );
            if (sampled[0] === 0.0) {
                continue;
            }
            produced++;
            const density = [0];
            const survival = [0];
            glass.scatteringDensity(point, air, glass, inDirection, out, all, density, survival);
            expectNear(sampled[0]!, density[0]!, 1e-4 * (1.0 + density[0]!));
            expectNear(0.3 + 0.2 + 0.4, survival[0]!, 1e-6);
        }
        expectGt(produced, 300);
    });

    it("ComponentsSumToTheWholeEvaluation", () => {
        const material = new PhongMaterial(
            "plastic",
            new PhongLobe(new ColorRgb(0.5, 0.4, 0.3), new ColorRgb(0.2, 0.2, 0.2), 8.0),
            null,
            null,
            null,
            false,
        );
        const point = pointWithNormal(UP);
        const inDirection = unit(0.2, 0.0, 1.0);
        const out = unit(0.25, 0.05, 1.0);
        const parts: ColorRgb[] = new Array<ColorRgb>(6);
        const sum = material.scatteringEvaluateComponents(
            point,
            null,
            null,
            inDirection,
            out,
            ScatteringComponent.REFLECTION_DIFFUSE | ScatteringComponent.REFLECTION_GLOSSY,
            parts,
        );
        const whole = material.scatteringEvaluate(
            point,
            null,
            null,
            inDirection,
            out,
            ScatteringComponent.REFLECTION_DIFFUSE | ScatteringComponent.REFLECTION_GLOSSY,
        );
        expectNear(whole.getR(), sum.getR(), 1e-12);
        expectNear(0.5 / Math.PI, parts[0]!.getR(), 1e-12);
        expectEq(0.0, parts[2]!.getR());
    });

    it("DiffuseEmission", () => {
        const lamp = new PhongMaterial(
            "lamp",
            null,
            null,
            new PhongLobe(new ColorRgb(10.0, 10.0, 10.0), new ColorRgb(0.0, 0.0, 0.0), 0.0),
            null,
            false,
        );
        const point = pointWithNormal(UP);
        const density = [0];
        const radiance = lamp.emissionEvaluate(point, unit(0.0, 0.6, 0.8), ScatteringComponent.DIFFUSE, density);
        expectNear(10.0 / Math.PI, radiance.getR(), 1e-5);
        expectNear(0.8 / Math.PI, density[0]!, 1e-6);
        expectEq(0.0, lamp.emissionEvaluate(point, unit(0.0, 0.6, -0.8), ScatteringComponent.DIFFUSE, density).getR());
        const sampledRadiance: ColorRgb[] = [new ColorRgb()];
        const out = lamp.emissionSample(point, ScatteringComponent.DIFFUSE, 0.25, 0.5, sampledRadiance, density);
        expectNear(scatteringArithmetic.dot(out, UP) / Math.PI, density[0]!, 1e-6);
        expectNear(radiance.getR(), sampledRadiance[0]!.getR(), 1e-12);
    });
});
