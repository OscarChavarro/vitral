package vsdk.toolkit.environment.material;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Df;

import static vsdk.toolkit.GTestAssertions.expectDoubleEq;
import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectFalse;
import static vsdk.toolkit.GTestAssertions.expectTrue;

/**
Port of `cpp/base/src/test/phong_material_tests.cpp`; each test method is
named `<cpp suite>_<cpp test>`.
*/
class PhongMaterialTest
{
    private static void expectColor(double r, double g, double b, ColorRgb color)
    {
        expectDoubleEq(r, color.getR());
        expectDoubleEq(g, color.getG());
        expectDoubleEq(b, color.getB());
    }

    @Test
    void PhongLobe_NonDiffuseColorIsGlossyBelowTheSpecularThreshold()
    {
        final PhongLobe glossy = new PhongLobe(new ColorRgb(0.5, 0.25, 0.125), new ColorRgb(0.1, 0.2, 0.3), 249.99F);
        expectFalse(glossy.isSpecular());
        expectColor(0.5, 0.25, 0.125, glossy.integrated(ScatteringComponent.DIFFUSE));
        expectColor(0.1, 0.2, 0.3, glossy.integrated(ScatteringComponent.GLOSSY));
        expectColor(0.0, 0.0, 0.0, glossy.integrated(ScatteringComponent.SPECULAR));
        expectColor(0.5 + 0.1, 0.25 + 0.2, 0.125 + 0.3, glossy.integrated(ScatteringComponent.ALL));
    }

    @Test
    void PhongLobe_NonDiffuseColorIsSpecularFromTheThresholdOn()
    {
        final PhongLobe specular = new PhongLobe(new ColorRgb(0.5, 0.25, 0.125), new ColorRgb(0.1, 0.2, 0.3), PhongLobe.LOWEST_SPECULAR_EXPONENT);
        expectTrue(specular.isSpecular());
        expectColor(0.0, 0.0, 0.0, specular.integrated(ScatteringComponent.GLOSSY));
        expectColor(0.1, 0.2, 0.3, specular.integrated(ScatteringComponent.SPECULAR));
        expectColor(0.0, 0.0, 0.0, specular.integrated(ScatteringComponent.NONE));
    }

    @Test
    void ScatteringComponent_SplitsReflectionAndTransmissionBits()
    {
        final int both = ScatteringComponent.REFLECTION_GLOSSY | ScatteringComponent.TRANSMISSION_DIFFUSE;
        expectEq(ScatteringComponent.GLOSSY, ScatteringComponent.reflectionComponents(both));
        expectEq(ScatteringComponent.DIFFUSE, ScatteringComponent.transmissionComponents(both));
    }

    @Test
    void PhongMaterial_ScatteredPowerAddsReflectionAndTransmissionByComponent()
    {
        final PhongMaterial material = new PhongMaterial(
            "glass",
            new PhongLobe(new ColorRgb(0.5, 0.5, 0.5), new ColorRgb(0.25, 0.25, 0.25), 10.0F),
            new PhongLobe(new ColorRgb(0.125, 0.0, 0.0), new ColorRgb(0.0, 0.0, 0.0625), 300.0F),
            null,
            null,
            true);
        expectColor(0.5, 0.5, 0.5, material.scatteredPower(ScatteringComponent.REFLECTION_DIFFUSE, null));
        expectColor(0.5 + 0.125, 0.5, 0.5,
            material.scatteredPower(ScatteringComponent.REFLECTION_DIFFUSE | ScatteringComponent.TRANSMISSION_DIFFUSE, null));
        expectColor(0.0, 0.0, 0.0625, material.scatteredPower(ScatteringComponent.TRANSMISSION_SPECULAR, null));
        expectColor(0.0, 0.0, 0.0, material.emittance(ScatteringComponent.ALL));
        expectFalse(material.hasEmission());
        expectTrue(material.isSided());
        expectEq("glass", material.getName());
    }

    @Test
    void PhongMaterial_TextureReplacesTheDiffuseReflection()
    {
        final byte[] texel = {(byte)255, (byte)0, (byte)51};
        final Texture texture = new Texture(1, 1, 3, texel);
        final PhongMaterial material = new PhongMaterial(
            "painted",
            new PhongLobe(new ColorRgb(0.5, 0.5, 0.5), new ColorRgb(0.25, 0.25, 0.25), 10.0F),
            null,
            null,
            texture,
            false);
        final Vector3Df coordinate = new Vector3Df(0.5F, 0.5F, 0.0F);
        final ColorRgb textured = material.scatteredPower(ScatteringComponent.REFLECTION_DIFFUSE, coordinate);
        final ColorRgb expected = texture.evaluateColor(0.5F, 0.5F);
        expectColor(expected.getR(), expected.getG(), expected.getB(), textured);
        final ColorRgb withGlossy = material.scatteredPower(
            ScatteringComponent.REFLECTION_DIFFUSE | ScatteringComponent.REFLECTION_GLOSSY, coordinate);
        expectColor(expected.getR() + 0.25, expected.getG() + 0.25, expected.getB() + 0.25, withGlossy);
        expectColor(0.0, 0.0, 0.0, material.scatteredPower(ScatteringComponent.REFLECTION_DIFFUSE, null));
    }

    @Test
    void PhongMaterial_EmittanceAndDeepCopy()
    {
        final PhongMaterial light = new PhongMaterial(
            "lamp",
            null,
            null,
            new PhongLobe(new ColorRgb(10.0, 9.0, 8.0), new ColorRgb(0.0, 0.0, 0.0), 0.0F),
            null,
            false);
        expectColor(10.0, 9.0, 8.0, light.emittance(ScatteringComponent.DIFFUSE));
        expectColor(0.0, 0.0, 0.0, light.scatteredPower(ScatteringComponent.REFLECTION_DIFFUSE, null));

        final Material copy = light.copy();
        final PhongMaterial phongCopy = (PhongMaterial)copy;
        expectTrue(light.getEmission() != phongCopy.getEmission());
        expectColor(10.0, 9.0, 8.0, phongCopy.emittance(ScatteringComponent.DIFFUSE));
        expectEq("lamp", phongCopy.getName());
        expectTrue(copy == copy.translate(null));
    }
}
