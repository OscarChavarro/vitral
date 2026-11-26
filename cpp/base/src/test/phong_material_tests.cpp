#include <gtest/gtest.h>
#include "vsdk/toolkit/environment/material/PhongMaterial.h"

namespace {

void expectColor(double r, double g, double b, const ColorRgb &color) {
    EXPECT_DOUBLE_EQ(r, color.getR());
    EXPECT_DOUBLE_EQ(g, color.getG());
    EXPECT_DOUBLE_EQ(b, color.getB());
}

}

TEST(PhongLobe, NonDiffuseColorIsGlossyBelowTheSpecularThreshold) {
    const PhongLobe glossy(ColorRgb(0.5, 0.25, 0.125), ColorRgb(0.1, 0.2, 0.3), 249.99);
    EXPECT_FALSE(glossy.isSpecular());
    expectColor(0.5, 0.25, 0.125, glossy.integrated(ScatteringComponent::DIFFUSE));
    expectColor(0.1, 0.2, 0.3, glossy.integrated(ScatteringComponent::GLOSSY));
    expectColor(0.0, 0.0, 0.0, glossy.integrated(ScatteringComponent::SPECULAR));
    expectColor(0.5 + 0.1, 0.25 + 0.2, 0.125 + 0.3, glossy.integrated(ScatteringComponent::ALL));
}

TEST(PhongLobe, NonDiffuseColorIsSpecularFromTheThresholdOn) {
    const PhongLobe specular(ColorRgb(0.5, 0.25, 0.125), ColorRgb(0.1, 0.2, 0.3), PhongLobe::LOWEST_SPECULAR_EXPONENT);
    EXPECT_TRUE(specular.isSpecular());
    expectColor(0.0, 0.0, 0.0, specular.integrated(ScatteringComponent::GLOSSY));
    expectColor(0.1, 0.2, 0.3, specular.integrated(ScatteringComponent::SPECULAR));
    expectColor(0.0, 0.0, 0.0, specular.integrated(ScatteringComponent::NONE));
}

TEST(ScatteringComponent, SplitsReflectionAndTransmissionBits) {
    const int both = ScatteringComponent::REFLECTION_GLOSSY | ScatteringComponent::TRANSMISSION_DIFFUSE;
    EXPECT_EQ(ScatteringComponent::GLOSSY, ScatteringComponent::reflectionComponents(both));
    EXPECT_EQ(ScatteringComponent::DIFFUSE, ScatteringComponent::transmissionComponents(both));
}

TEST(PhongMaterial, ScatteredPowerAddsReflectionAndTransmissionByComponent) {
    const PhongMaterial material(
        "glass",
        new PhongLobe(ColorRgb(0.5, 0.5, 0.5), ColorRgb(0.25, 0.25, 0.25), 10.0),
        new PhongLobe(ColorRgb(0.125, 0.0, 0.0), ColorRgb(0.0, 0.0, 0.0625), 300.0),
        nullptr,
        nullptr,
        true);
    expectColor(0.5, 0.5, 0.5, material.scatteredPower(ScatteringComponent::REFLECTION_DIFFUSE, nullptr));
    expectColor(0.5 + 0.125, 0.5, 0.5,
        material.scatteredPower(ScatteringComponent::REFLECTION_DIFFUSE | ScatteringComponent::TRANSMISSION_DIFFUSE, nullptr));
    expectColor(0.0, 0.0, 0.0625, material.scatteredPower(ScatteringComponent::TRANSMISSION_SPECULAR, nullptr));
    expectColor(0.0, 0.0, 0.0, material.emittance(ScatteringComponent::ALL));
    EXPECT_FALSE(material.hasEmission());
    EXPECT_TRUE(material.isSided());
    EXPECT_STREQ("glass", material.getName());
}

TEST(PhongMaterial, TextureReplacesTheDiffuseReflection) {
    const unsigned char texel[3] = {255, 0, 51};
    const Texture texture(1, 1, 3, texel);
    const PhongMaterial material(
        "painted",
        new PhongLobe(ColorRgb(0.5, 0.5, 0.5), ColorRgb(0.25, 0.25, 0.25), 10.0),
        nullptr,
        nullptr,
        &texture,
        false);
    const Vector3Dd coordinate(0.5, 0.5, 0.0);
    const ColorRgb textured = material.scatteredPower(ScatteringComponent::REFLECTION_DIFFUSE, &coordinate);
    const ColorRgb expected = texture.evaluateColor(0.5, 0.5);
    expectColor(expected.getR(), expected.getG(), expected.getB(), textured);
    const ColorRgb withGlossy = material.scatteredPower(
        ScatteringComponent::REFLECTION_DIFFUSE | ScatteringComponent::REFLECTION_GLOSSY, &coordinate);
    expectColor(expected.getR() + 0.25, expected.getG() + 0.25, expected.getB() + 0.25, withGlossy);
    expectColor(0.0, 0.0, 0.0, material.scatteredPower(ScatteringComponent::REFLECTION_DIFFUSE, nullptr));
}

TEST(PhongMaterial, EmittanceAndDeepCopy) {
    const PhongMaterial light(
        "lamp",
        nullptr,
        nullptr,
        new PhongLobe(ColorRgb(10.0, 9.0, 8.0), ColorRgb(0.0, 0.0, 0.0), 0.0),
        nullptr,
        false);
    expectColor(10.0, 9.0, 8.0, light.emittance(ScatteringComponent::DIFFUSE));
    expectColor(0.0, 0.0, 0.0, light.scatteredPower(ScatteringComponent::REFLECTION_DIFFUSE, nullptr));

    Material *copy = light.copy();
    const PhongMaterial *phongCopy = static_cast<const PhongMaterial *>(copy);
    EXPECT_NE(light.getEmission(), phongCopy->getEmission());
    expectColor(10.0, 9.0, 8.0, phongCopy->emittance(ScatteringComponent::DIFFUSE));
    EXPECT_STREQ("lamp", phongCopy->getName());
    EXPECT_EQ(copy, copy->translate(nullptr));
    delete copy;
}
