#include <cmath>
#include <gtest/gtest.h>
#include "vsdk/toolkit/environment/material/PhongMaterial.h"
#include "vsdk/toolkit/environment/material/ScatteringFrame.h"

// Directions leave the surface (`in` back to the light), see PhongLobe.
// Directional functions of the Phong scattering model (plan step 2b of
// RenderPark's VITRAL alignment): conventions, normalization, sampling
// against density, roulette and component masks.

namespace {

const Vector3Dd UP(0.0, 0.0, 1.0);

// A deterministic low-discrepancy pair: Hammersley points
double radicalInverse(unsigned int i) {
    double inverse = 0.0;
    double base = 0.5;
    while ( i != 0 ) {
        if ( i & 1U ) {
            inverse += base;
        }
        base *= 0.5;
        i >>= 1;
    }
    return inverse;
}

Vector3Dd unit(double x, double y, double z) {
    return scatteringArithmetic::normalize(Vector3Dd(x, y, z), 1e-6);
}

SurfaceShadingPoint pointWithNormal(const Vector3Dd &normal) {
    return SurfaceShadingPoint(normal, true, Vector3Dd(), false);
}

}

TEST(ScatteringFrame, IsOrthonormalAndSamplesTheUpperHemisphere) {
    const Vector3Dd axis = unit(0.3, -0.5, 0.8);
    const ScatteringFrame frame(axis);
    EXPECT_NEAR(0.0, scatteringArithmetic::dot(frame.getX(), frame.getZ()), 1e-6);
    EXPECT_NEAR(0.0, scatteringArithmetic::dot(frame.getY(), frame.getZ()), 1e-6);
    EXPECT_NEAR(0.0, scatteringArithmetic::dot(frame.getX(), frame.getY()), 1e-6);
    EXPECT_NEAR(1.0, scatteringArithmetic::dot(frame.getX(), frame.getX()), 1e-6);
    for ( unsigned int i = 0; i < 256; i++ ) {
        double density;
        const Vector3Dd d = frame.sampleCosine((i + 0.5) / 256.0, radicalInverse(i), &density);
        const double cosTheta = scatteringArithmetic::dot(d, axis);
        EXPECT_GE(cosTheta, -1e-6);
        EXPECT_NEAR(cosTheta / M_PI, density, 1e-6);
    }
}

TEST(ScatteringFrame, IdealDirections) {
    const Vector3Dd in = unit(1.0, 0.0, -1.0);
    const Vector3Dd mirror = ScatteringFrame::idealReflection(in, UP);
    EXPECT_NEAR(std::sqrt(0.5), mirror.x(), 1e-6);
    EXPECT_NEAR(std::sqrt(0.5), mirror.z(), 1e-6);
    bool totalInternalReflection = true;
    // Same index on both sides: the ray goes straight on
    const Vector3Dd through = ScatteringFrame::idealRefraction(in, UP, 1.5, 1.5, &totalInternalReflection);
    EXPECT_FALSE(totalInternalReflection);
    EXPECT_NEAR(in.x(), through.x(), 1e-6);
    EXPECT_NEAR(in.z(), through.z(), 1e-6);
    // Grazing ray from glass to air: total internal reflection
    const Vector3Dd grazing = unit(1.0, 0.0, -0.2);
    ScatteringFrame::idealRefraction(grazing, UP, 1.5, 1.0, &totalInternalReflection);
    EXPECT_TRUE(totalInternalReflection);
}

TEST(PhongLobe, PureDiffuseEvaluatesToKdOverPiAndReflectsKd) {
    const PhongLobe lobe(ColorRgb(0.6, 0.3, 0.15), ColorRgb(0.0, 0.0, 0.0), 0.0);
    const Vector3Dd in = unit(0.2, 0.1, 1.0);
    const ColorRgb above = lobe.reflectionEvaluate(in, unit(0.5, 0.5, 0.7), UP, ScatteringComponent::ALL);
    EXPECT_NEAR(0.6 / M_PI, above.getR(), 1e-12);
    const ColorRgb below = lobe.reflectionEvaluate(in, unit(0.5, 0.5, -0.7), UP, ScatteringComponent::ALL);
    EXPECT_EQ(0.0, below.getR());
    // Integrated reflectance: E[f cos / p] over cosine samples
    const ScatteringFrame frame(UP);
    double sum = 0.0;
    const unsigned int n = 4096;
    for ( unsigned int i = 0; i < n; i++ ) {
        double density;
        const Vector3Dd out = frame.sampleCosine((i + 0.5) / n, radicalInverse(i), &density);
        sum += lobe.reflectionEvaluate(in, out, UP, ScatteringComponent::ALL).getR()
            * scatteringArithmetic::dot(out, UP) / density;
    }
    EXPECT_NEAR(0.6, sum / n, 1e-3);
}

TEST(PhongLobe, GlossyEvaluationAndSamplingUseTheirOwnNormalizations) {
    // Evaluation peaks at Ks (Ns + 2) / (2 pi); the sampled density at (Ns + 1) / (2 pi)
    const double ns = 20.0;
    const PhongLobe lobe(ColorRgb(0.0, 0.0, 0.0), ColorRgb(0.5, 0.5, 0.5), ns);
    const Vector3Dd in = UP;
    const ColorRgb peak = lobe.reflectionEvaluate(in, UP, UP, ScatteringComponent::GLOSSY);
    EXPECT_NEAR(0.5 * (ns + 2.0) / (2.0 * M_PI), peak.getR(), 1e-5);
    double density;
    double survival;
    lobe.reflectionDensity(in, UP, UP, ScatteringComponent::GLOSSY, &density, &survival);
    EXPECT_NEAR((ns + 1.0) / (2.0 * M_PI), density, 1e-4);
    EXPECT_NEAR(0.5, survival, 1e-7);
    // The density integrates to 1 over the hemisphere (cosine-sampled estimate)
    const ScatteringFrame frame(UP);
    double integral = 0.0;
    const unsigned int n = 1 << 16;
    for ( unsigned int i = 0; i < n; i++ ) {
        double cosineDensity;
        const Vector3Dd out = frame.sampleCosine((i + 0.5) / n, radicalInverse(i), &cosineDensity);
        lobe.reflectionDensity(in, out, UP, ScatteringComponent::GLOSSY, &density, &survival);
        integral += density / cosineDensity;
    }
    EXPECT_NEAR(1.0, integral / n, 2e-2);
}

TEST(PhongLobe, SampledDensityMatchesTheIndependentDensity) {
    const PhongLobe mixture(ColorRgb(0.4, 0.3, 0.2), ColorRgb(0.2, 0.2, 0.2), 12.0);
    const Vector3Dd in = unit(0.4, -0.3, 0.8);
    int produced = 0;
    for ( unsigned int i = 0; i < 512; i++ ) {
        double sampled;
        const Vector3Dd out = mixture.reflectionSample(
            in, UP, false, ScatteringComponent::ALL, (i + 0.5) / 512.0, radicalInverse(i), &sampled);
        if ( sampled == 0.0 ) {
            continue;
        }
        produced++;
        double density;
        double survival;
        mixture.reflectionDensity(in, out, UP, ScatteringComponent::ALL, &density, &survival);
        EXPECT_NEAR(sampled, density, 1e-4 * (1.0 + density));
    }
    EXPECT_GT(produced, 400);
}

TEST(PhongLobe, RouletteAbsorbsAndDisabledComponentsSampleNothing) {
    const PhongLobe lobe(ColorRgb(0.3, 0.3, 0.3), ColorRgb(0.0, 0.0, 0.0), 0.0);
    const Vector3Dd in = UP;
    double density;
    // Survival probability 0.3: a first variate above it is absorbed
    lobe.reflectionSample(in, UP, true, ScatteringComponent::ALL, 0.5, 0.5, &density);
    EXPECT_EQ(0.0, density);
    // Below it the direction survives and the density is not divided by the survival
    const Vector3Dd out = lobe.reflectionSample(in, UP, true, ScatteringComponent::ALL, 0.15, 0.5, &density);
    EXPECT_NEAR(0.3 * scatteringArithmetic::dot(out, UP) / M_PI, density, 1e-6);
    // Requested component absent: nothing scattered
    lobe.reflectionSample(in, UP, false, ScatteringComponent::GLOSSY, 0.1, 0.5, &density);
    EXPECT_EQ(0.0, density);
    // Zero power lobe
    const PhongLobe black(ColorRgb(0.0, 0.0, 0.0), ColorRgb(0.0, 0.0, 0.0), 0.0);
    black.reflectionSample(in, UP, false, ScatteringComponent::ALL, 0.1, 0.5, &density);
    EXPECT_EQ(0.0, density);
}

TEST(PhongLobe, DiffuseTransmissionCrossesTheSurface) {
    const PhongLobe lobe(ColorRgb(0.5, 0.5, 0.5), ColorRgb(0.0, 0.0, 0.0), 0.0);
    const Vector3Dd in = UP;
    EXPECT_NEAR(0.5 / M_PI, lobe.transmissionEvaluate(1.0, 1.5, in, unit(0.1, 0.0, -1.0), UP,
        ScatteringComponent::ALL).getR(), 1e-12);
    EXPECT_EQ(0.0, lobe.transmissionEvaluate(1.0, 1.5, in, unit(0.1, 0.0, 1.0), UP,
        ScatteringComponent::ALL).getR());
    double density;
    const Vector3Dd out = lobe.transmissionSample(1.0, 1.5, in, UP, false, ScatteringComponent::ALL, 0.3, 0.6, &density);
    EXPECT_LT(scatteringArithmetic::dot(out, UP), 0.0);
    double independent;
    double survival;
    lobe.transmissionDensity(1.0, 1.5, in, out, UP, ScatteringComponent::ALL, &independent, &survival);
    EXPECT_NEAR(density, -independent, 1e-6);  // RenderPark's diffuse transmission density is negative
}

TEST(PhongMaterial, SampledScatteringDensityMatchesScatteringDensity) {
    const PhongMaterial glass(
        "glass",
        new PhongLobe(ColorRgb(0.3, 0.3, 0.3), ColorRgb(0.2, 0.2, 0.2), 30.0),
        new PhongLobe(ColorRgb(0.0, 0.0, 0.0), ColorRgb(0.4, 0.4, 0.4), 30.0),
        nullptr, nullptr, false, 1.5, 0.0);
    const PhongMaterial air("air", nullptr, nullptr, nullptr, nullptr, false);
    EXPECT_FLOAT_EQ(1.5, glass.getRefractionIndex());
    EXPECT_FLOAT_EQ(1.0, air.getRefractionIndex());
    const SurfaceShadingPoint point = pointWithNormal(UP);
    const Vector3Dd in = unit(0.3, 0.2, 0.9);
    const int all = ScatteringComponent::REFLECTION_DIFFUSE | ScatteringComponent::REFLECTION_GLOSSY
        | ScatteringComponent::TRANSMISSION_GLOSSY;
    int produced = 0;
    for ( unsigned int i = 0; i < 512; i++ ) {
        double sampled;
        const Vector3Dd out = glass.scatteringSample(
            point, &air, &glass, in, false, all, (i + 0.5) / 512.0, radicalInverse(i), &sampled);
        if ( sampled == 0.0 ) {
            continue;
        }
        produced++;
        double density;
        double survival;
        glass.scatteringDensity(point, &air, &glass, in, out, all, &density, &survival);
        EXPECT_NEAR(sampled, density, 1e-4 * (1.0 + density));
        EXPECT_NEAR((0.3 + 0.2 + 0.4), survival, 1e-6);
    }
    EXPECT_GT(produced, 300);
}

TEST(PhongMaterial, ComponentsSumToTheWholeEvaluation) {
    const PhongMaterial material(
        "plastic",
        new PhongLobe(ColorRgb(0.5, 0.4, 0.3), ColorRgb(0.2, 0.2, 0.2), 8.0),
        nullptr, nullptr, nullptr, false);
    const SurfaceShadingPoint point = pointWithNormal(UP);
    const Vector3Dd in = unit(0.2, 0.0, 1.0);
    const Vector3Dd out = unit(0.25, 0.05, 1.0);
    ColorRgb parts[6];
    const ColorRgb sum = material.scatteringEvaluateComponents(
        point, nullptr, nullptr, in, out, ScatteringComponent::REFLECTION_DIFFUSE | ScatteringComponent::REFLECTION_GLOSSY, parts);
    const ColorRgb whole = material.scatteringEvaluate(
        point, nullptr, nullptr, in, out, ScatteringComponent::REFLECTION_DIFFUSE | ScatteringComponent::REFLECTION_GLOSSY);
    EXPECT_NEAR(whole.getR(), sum.getR(), 1e-12);
    EXPECT_NEAR(0.5 / M_PI, parts[0].getR(), 1e-12);
    EXPECT_EQ(0.0, parts[2].getR());
}

TEST(PhongMaterial, DiffuseEmission) {
    const PhongMaterial lamp(
        "lamp", nullptr, nullptr,
        new PhongLobe(ColorRgb(10.0, 10.0, 10.0), ColorRgb(0.0, 0.0, 0.0), 0.0),
        nullptr, false);
    const SurfaceShadingPoint point = pointWithNormal(UP);
    double density;
    const ColorRgb radiance = lamp.emissionEvaluate(&point, unit(0.0, 0.6, 0.8), ScatteringComponent::DIFFUSE, &density);
    EXPECT_NEAR(10.0 / M_PI, radiance.getR(), 1e-5);
    EXPECT_NEAR(0.8 / M_PI, density, 1e-6);
    EXPECT_EQ(0.0, lamp.emissionEvaluate(&point, unit(0.0, 0.6, -0.8), ScatteringComponent::DIFFUSE, &density).getR());
    ColorRgb sampledRadiance;
    const Vector3Dd out = lamp.emissionSample(&point, ScatteringComponent::DIFFUSE, 0.25, 0.5, &sampledRadiance, &density);
    EXPECT_NEAR(scatteringArithmetic::dot(out, UP) / M_PI, density, 1e-6);
    EXPECT_NEAR(radiance.getR(), sampledRadiance.getR(), 1e-12);
}
