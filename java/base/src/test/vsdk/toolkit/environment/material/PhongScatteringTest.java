package vsdk.toolkit.environment.material;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Df;

import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectFalse;
import static vsdk.toolkit.GTestAssertions.expectGe;
import static vsdk.toolkit.GTestAssertions.expectGt;
import static vsdk.toolkit.GTestAssertions.expectLt;
import static vsdk.toolkit.GTestAssertions.expectNear;
import static vsdk.toolkit.GTestAssertions.expectTrue;

/**
Port of `cpp/base/src/test/phong_scattering_tests.cpp`; each test method is
named `<cpp suite>_<cpp test>`.

Directions leave the surface (`in` back to the light), see PhongLobe.
Directional functions of the Phong scattering model (plan step 2b of
RenderPark's VITRAL alignment): conventions, normalization, sampling
against density, roulette and component masks.
*/
class PhongScatteringTest
{
    private static final Vector3Df UP = new Vector3Df(0.0F, 0.0F, 1.0F);

    // A deterministic low-discrepancy pair: Hammersley points
    private static double radicalInverse(int i)
    {
        double inverse = 0.0;
        double base = 0.5;
        while ( i != 0 ) {
            if ( (i & 1) != 0 ) {
                inverse += base;
            }
            base *= 0.5;
            i >>>= 1;
        }
        return inverse;
    }

    private static Vector3Df unit(float x, float y, float z)
    {
        return ScatteringArithmetic.normalize(new Vector3Df(x, y, z), 1e-6F);
    }

    private static SurfaceShadingPoint pointWithNormal(Vector3Df normal)
    {
        return new SurfaceShadingPoint(normal, true, new Vector3Df(), false);
    }

    @Test
    void ScatteringFrame_IsOrthonormalAndSamplesTheUpperHemisphere()
    {
        final Vector3Df axis = unit(0.3F, -0.5F, 0.8F);
        final ScatteringFrame frame = new ScatteringFrame(axis);
        expectNear(0.0, ScatteringArithmetic.dot(frame.getX(), frame.getZ()), 1e-6);
        expectNear(0.0, ScatteringArithmetic.dot(frame.getY(), frame.getZ()), 1e-6);
        expectNear(0.0, ScatteringArithmetic.dot(frame.getX(), frame.getY()), 1e-6);
        expectNear(1.0, ScatteringArithmetic.dot(frame.getX(), frame.getX()), 1e-6);
        for ( int i = 0; i < 256; i++ ) {
            double[] density = {0.0};
            final Vector3Df d = frame.sampleCosine((i + 0.5) / 256.0, radicalInverse(i), density);
            final double cosTheta = ScatteringArithmetic.dot(d, axis);
            expectGe(cosTheta, -1e-6);
            expectNear(cosTheta / Math.PI, density[0], 1e-6);
        }
    }

    @Test
    void ScatteringFrame_IdealDirections()
    {
        final Vector3Df in = unit(1.0F, 0.0F, -1.0F);
        final Vector3Df mirror = ScatteringFrame.idealReflection(in, UP);
        expectNear(Math.sqrt(0.5), mirror.x(), 1e-6);
        expectNear(Math.sqrt(0.5), mirror.z(), 1e-6);
        boolean[] totalInternalReflection = {true};
        // Same index on both sides: the ray goes straight on
        final Vector3Df through = ScatteringFrame.idealRefraction(in, UP, 1.5F, 1.5F, totalInternalReflection);
        expectFalse(totalInternalReflection[0]);
        expectNear(in.x(), through.x(), 1e-6);
        expectNear(in.z(), through.z(), 1e-6);
        // Grazing ray from glass to air: total internal reflection
        final Vector3Df grazing = unit(1.0F, 0.0F, -0.2F);
        ScatteringFrame.idealRefraction(grazing, UP, 1.5F, 1.0F, totalInternalReflection);
        expectTrue(totalInternalReflection[0]);
    }

    @Test
    void PhongLobe_PureDiffuseEvaluatesToKdOverPiAndReflectsKd()
    {
        final PhongLobe lobe = new PhongLobe(new ColorRgb(0.6, 0.3, 0.15), new ColorRgb(0.0, 0.0, 0.0), 0.0F);
        final Vector3Df in = unit(0.2F, 0.1F, 1.0F);
        final ColorRgb above = lobe.reflectionEvaluate(in, unit(0.5F, 0.5F, 0.7F), UP, ScatteringComponent.ALL);
        expectNear(0.6 / Math.PI, above.getR(), 1e-12);
        final ColorRgb below = lobe.reflectionEvaluate(in, unit(0.5F, 0.5F, -0.7F), UP, ScatteringComponent.ALL);
        expectEq(0.0, below.getR());
        // Integrated reflectance: E[f cos / p] over cosine samples
        final ScatteringFrame frame = new ScatteringFrame(UP);
        double sum = 0.0;
        final int n = 4096;
        for ( int i = 0; i < n; i++ ) {
            double[] density = {0.0};
            final Vector3Df out = frame.sampleCosine((i + 0.5) / n, radicalInverse(i), density);
            sum += lobe.reflectionEvaluate(in, out, UP, ScatteringComponent.ALL).getR()
                * ScatteringArithmetic.dot(out, UP) / density[0];
        }
        expectNear(0.6, sum / n, 1e-3);
    }

    @Test
    void PhongLobe_GlossyEvaluationAndSamplingUseTheirOwnNormalizations()
    {
        // Evaluation peaks at Ks (Ns + 2) / (2 pi); the sampled density at (Ns + 1) / (2 pi)
        final float ns = 20.0F;
        final PhongLobe lobe = new PhongLobe(new ColorRgb(0.0, 0.0, 0.0), new ColorRgb(0.5, 0.5, 0.5), ns);
        final Vector3Df in = UP;
        final ColorRgb peak = lobe.reflectionEvaluate(in, UP, UP, ScatteringComponent.GLOSSY);
        expectNear(0.5 * (ns + 2.0) / (2.0 * Math.PI), peak.getR(), 1e-5);
        double[] density = {0.0};
        double[] survival = {0.0};
        lobe.reflectionDensity(in, UP, UP, ScatteringComponent.GLOSSY, density, survival);
        expectNear((ns + 1.0) / (2.0 * Math.PI), density[0], 1e-4);
        expectNear(0.5, survival[0], 1e-7);
        // The density integrates to 1 over the hemisphere (cosine-sampled estimate)
        final ScatteringFrame frame = new ScatteringFrame(UP);
        double integral = 0.0;
        final int n = 1 << 16;
        for ( int i = 0; i < n; i++ ) {
            double[] cosineDensity = {0.0};
            final Vector3Df out = frame.sampleCosine((i + 0.5) / n, radicalInverse(i), cosineDensity);
            lobe.reflectionDensity(in, out, UP, ScatteringComponent.GLOSSY, density, survival);
            integral += density[0] / cosineDensity[0];
        }
        expectNear(1.0, integral / n, 2e-2);
    }

    @Test
    void PhongLobe_SampledDensityMatchesTheIndependentDensity()
    {
        final PhongLobe mixture = new PhongLobe(new ColorRgb(0.4, 0.3, 0.2), new ColorRgb(0.2, 0.2, 0.2), 12.0F);
        final Vector3Df in = unit(0.4F, -0.3F, 0.8F);
        int produced = 0;
        for ( int i = 0; i < 512; i++ ) {
            double[] sampled = {0.0};
            final Vector3Df out = mixture.reflectionSample(
                in, UP, false, ScatteringComponent.ALL, (i + 0.5) / 512.0, radicalInverse(i), sampled);
            if ( sampled[0] == 0.0 ) {
                continue;
            }
            produced++;
            double[] density = {0.0};
            double[] survival = {0.0};
            mixture.reflectionDensity(in, out, UP, ScatteringComponent.ALL, density, survival);
            expectNear(sampled[0], density[0], 1e-4 * (1.0 + density[0]));
        }
        expectGt(produced, 400);
    }

    @Test
    void PhongLobe_RouletteAbsorbsAndDisabledComponentsSampleNothing()
    {
        final PhongLobe lobe = new PhongLobe(new ColorRgb(0.3, 0.3, 0.3), new ColorRgb(0.0, 0.0, 0.0), 0.0F);
        final Vector3Df in = UP;
        double[] density = {0.0};
        // Survival probability 0.3: a first variate above it is absorbed
        lobe.reflectionSample(in, UP, true, ScatteringComponent.ALL, 0.5, 0.5, density);
        expectEq(0.0, density[0]);
        // Below it the direction survives and the density is not divided by the survival
        final Vector3Df out = lobe.reflectionSample(in, UP, true, ScatteringComponent.ALL, 0.15, 0.5, density);
        expectNear(0.3 * ScatteringArithmetic.dot(out, UP) / Math.PI, density[0], 1e-6);
        // Requested component absent: nothing scattered
        lobe.reflectionSample(in, UP, false, ScatteringComponent.GLOSSY, 0.1, 0.5, density);
        expectEq(0.0, density[0]);
        // Zero power lobe
        final PhongLobe black = new PhongLobe(new ColorRgb(0.0, 0.0, 0.0), new ColorRgb(0.0, 0.0, 0.0), 0.0F);
        black.reflectionSample(in, UP, false, ScatteringComponent.ALL, 0.1, 0.5, density);
        expectEq(0.0, density[0]);
    }

    @Test
    void PhongLobe_DiffuseTransmissionCrossesTheSurface()
    {
        final PhongLobe lobe = new PhongLobe(new ColorRgb(0.5, 0.5, 0.5), new ColorRgb(0.0, 0.0, 0.0), 0.0F);
        final Vector3Df in = UP;
        expectNear(0.5 / Math.PI, lobe.transmissionEvaluate(1.0F, 1.5F, in, unit(0.1F, 0.0F, -1.0F), UP,
            ScatteringComponent.ALL).getR(), 1e-12);
        expectEq(0.0, lobe.transmissionEvaluate(1.0F, 1.5F, in, unit(0.1F, 0.0F, 1.0F), UP,
            ScatteringComponent.ALL).getR());
        double[] density = {0.0};
        final Vector3Df out = lobe.transmissionSample(1.0F, 1.5F, in, UP, false, ScatteringComponent.ALL, 0.3, 0.6, density);
        expectLt(ScatteringArithmetic.dot(out, UP), 0.0F);
        double[] independent = {0.0};
        double[] survival = {0.0};
        lobe.transmissionDensity(1.0F, 1.5F, in, out, UP, ScatteringComponent.ALL, independent, survival);
        expectNear(density[0], -independent[0], 1e-6);  // RenderPark's diffuse transmission density is negative
    }

    @Test
    void PhongMaterial_SampledScatteringDensityMatchesScatteringDensity()
    {
        final PhongMaterial glass = new PhongMaterial(
            "glass",
            new PhongLobe(new ColorRgb(0.3, 0.3, 0.3), new ColorRgb(0.2, 0.2, 0.2), 30.0F),
            new PhongLobe(new ColorRgb(0.0, 0.0, 0.0), new ColorRgb(0.4, 0.4, 0.4), 30.0F),
            null, null, false, 1.5F, 0.0F);
        final PhongMaterial air = new PhongMaterial("air", null, null, null, null, false);
        expectEq(1.5F, glass.getRefractionIndex());
        expectEq(1.0F, air.getRefractionIndex());
        final SurfaceShadingPoint point = pointWithNormal(UP);
        final Vector3Df in = unit(0.3F, 0.2F, 0.9F);
        final int all = ScatteringComponent.REFLECTION_DIFFUSE | ScatteringComponent.REFLECTION_GLOSSY
            | ScatteringComponent.TRANSMISSION_GLOSSY;
        int produced = 0;
        for ( int i = 0; i < 512; i++ ) {
            double[] sampled = {0.0};
            final Vector3Df out = glass.scatteringSample(
                point, air, glass, in, false, all, (i + 0.5) / 512.0, radicalInverse(i), sampled);
            if ( sampled[0] == 0.0 ) {
                continue;
            }
            produced++;
            double[] density = {0.0};
            double[] survival = {0.0};
            glass.scatteringDensity(point, air, glass, in, out, all, density, survival);
            expectNear(sampled[0], density[0], 1e-4 * (1.0 + density[0]));
            expectNear((0.3 + 0.2 + 0.4), survival[0], 1e-6);
        }
        expectGt(produced, 300);
    }

    @Test
    void PhongMaterial_ComponentsSumToTheWholeEvaluation()
    {
        final PhongMaterial material = new PhongMaterial(
            "plastic",
            new PhongLobe(new ColorRgb(0.5, 0.4, 0.3), new ColorRgb(0.2, 0.2, 0.2), 8.0F),
            null, null, null, false);
        final SurfaceShadingPoint point = pointWithNormal(UP);
        final Vector3Df in = unit(0.2F, 0.0F, 1.0F);
        final Vector3Df out = unit(0.25F, 0.05F, 1.0F);
        ColorRgb[] parts = new ColorRgb[6];
        final ColorRgb sum = material.scatteringEvaluateComponents(
            point, null, null, in, out, ScatteringComponent.REFLECTION_DIFFUSE | ScatteringComponent.REFLECTION_GLOSSY, parts);
        final ColorRgb whole = material.scatteringEvaluate(
            point, null, null, in, out, ScatteringComponent.REFLECTION_DIFFUSE | ScatteringComponent.REFLECTION_GLOSSY);
        expectNear(whole.getR(), sum.getR(), 1e-12);
        expectNear(0.5 / Math.PI, parts[0].getR(), 1e-12);
        expectEq(0.0, parts[2].getR());
    }

    @Test
    void PhongMaterial_DiffuseEmission()
    {
        final PhongMaterial lamp = new PhongMaterial(
            "lamp", null, null,
            new PhongLobe(new ColorRgb(10.0, 10.0, 10.0), new ColorRgb(0.0, 0.0, 0.0), 0.0F),
            null, false);
        final SurfaceShadingPoint point = pointWithNormal(UP);
        double[] density = {0.0};
        final ColorRgb radiance = lamp.emissionEvaluate(point, unit(0.0F, 0.6F, 0.8F), ScatteringComponent.DIFFUSE, density);
        expectNear(10.0 / Math.PI, radiance.getR(), 1e-5);
        expectNear(0.8 / Math.PI, density[0], 1e-6);
        expectEq(0.0, lamp.emissionEvaluate(point, unit(0.0F, 0.6F, -0.8F), ScatteringComponent.DIFFUSE, density).getR());
        ColorRgb[] sampledRadiance = {new ColorRgb()};
        final Vector3Df out = lamp.emissionSample(point, ScatteringComponent.DIFFUSE, 0.25, 0.5, sampledRadiance, density);
        expectNear(ScatteringArithmetic.dot(out, UP) / Math.PI, density[0], 1e-6);
        expectNear(radiance.getR(), sampledRadiance[0].getR(), 1e-12);
    }
}
