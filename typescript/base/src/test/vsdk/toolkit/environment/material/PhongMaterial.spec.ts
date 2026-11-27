// Port of cpp/base/src/test/phong_material_tests.cpp
import { describe, it } from "vitest";
import { ColorRgb } from "vsdk/toolkit/common/color/ColorRgb.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PhongLobe } from "vsdk/toolkit/environment/material/PhongLobe.js";
import { PhongMaterial } from "vsdk/toolkit/environment/material/PhongMaterial.js";
import { ScatteringComponent } from "vsdk/toolkit/environment/material/ScatteringComponent.js";
import { Texture } from "vsdk/toolkit/environment/material/Texture.js";
import { expectDoubleEq, expectEq, expectFalse, expectNe, expectTrue } from "../../../../GTestAssertions.js";

function expectColor(r: number, g: number, b: number, color: ColorRgb): void {
    expectDoubleEq(r, color.getR());
    expectDoubleEq(g, color.getG());
    expectDoubleEq(b, color.getB());
}

describe("PhongLobe", () => {
    it("NonDiffuseColorIsGlossyBelowTheSpecularThreshold", () => {
        const glossy = new PhongLobe(new ColorRgb(0.5, 0.25, 0.125), new ColorRgb(0.1, 0.2, 0.3), 249.99);
        expectFalse(glossy.isSpecular());
        expectColor(0.5, 0.25, 0.125, glossy.integrated(ScatteringComponent.DIFFUSE));
        expectColor(0.1, 0.2, 0.3, glossy.integrated(ScatteringComponent.GLOSSY));
        expectColor(0.0, 0.0, 0.0, glossy.integrated(ScatteringComponent.SPECULAR));
        expectColor(0.5 + 0.1, 0.25 + 0.2, 0.125 + 0.3, glossy.integrated(ScatteringComponent.ALL));
    });

    it("NonDiffuseColorIsSpecularFromTheThresholdOn", () => {
        const specular = new PhongLobe(
            new ColorRgb(0.5, 0.25, 0.125),
            new ColorRgb(0.1, 0.2, 0.3),
            PhongLobe.LOWEST_SPECULAR_EXPONENT,
        );
        expectTrue(specular.isSpecular());
        expectColor(0.0, 0.0, 0.0, specular.integrated(ScatteringComponent.GLOSSY));
        expectColor(0.1, 0.2, 0.3, specular.integrated(ScatteringComponent.SPECULAR));
        expectColor(0.0, 0.0, 0.0, specular.integrated(ScatteringComponent.NONE));
    });
});

describe("ScatteringComponent", () => {
    it("SplitsReflectionAndTransmissionBits", () => {
        const both = ScatteringComponent.REFLECTION_GLOSSY | ScatteringComponent.TRANSMISSION_DIFFUSE;
        expectEq(ScatteringComponent.GLOSSY, ScatteringComponent.reflectionComponents(both));
        expectEq(ScatteringComponent.DIFFUSE, ScatteringComponent.transmissionComponents(both));
    });
});

describe("PhongMaterial", () => {
    it("ScatteredPowerAddsReflectionAndTransmissionByComponent", () => {
        const material = new PhongMaterial(
            "glass",
            new PhongLobe(new ColorRgb(0.5, 0.5, 0.5), new ColorRgb(0.25, 0.25, 0.25), 10.0),
            new PhongLobe(new ColorRgb(0.125, 0.0, 0.0), new ColorRgb(0.0, 0.0, 0.0625), 300.0),
            null,
            null,
            true,
        );
        expectColor(0.5, 0.5, 0.5, material.scatteredPower(ScatteringComponent.REFLECTION_DIFFUSE, null));
        expectColor(
            0.5 + 0.125,
            0.5,
            0.5,
            material.scatteredPower(
                ScatteringComponent.REFLECTION_DIFFUSE | ScatteringComponent.TRANSMISSION_DIFFUSE,
                null,
            ),
        );
        expectColor(0.0, 0.0, 0.0625, material.scatteredPower(ScatteringComponent.TRANSMISSION_SPECULAR, null));
        expectColor(0.0, 0.0, 0.0, material.emittance(ScatteringComponent.ALL));
        expectFalse(material.hasEmission());
        expectTrue(material.isSided());
        expectEq("glass", material.getName());
    });

    it("TextureReplacesTheDiffuseReflection", () => {
        const texel = [255, 0, 51];
        const texture = new Texture(1, 1, 3, texel);
        const material = new PhongMaterial(
            "painted",
            new PhongLobe(new ColorRgb(0.5, 0.5, 0.5), new ColorRgb(0.25, 0.25, 0.25), 10.0),
            null,
            null,
            texture,
            false,
        );
        const coordinate = new Vector3Dd(0.5, 0.5, 0.0);
        const textured = material.scatteredPower(ScatteringComponent.REFLECTION_DIFFUSE, coordinate);
        const expected = texture.evaluateColor(0.5, 0.5);
        expectColor(expected.getR(), expected.getG(), expected.getB(), textured);
        const withGlossy = material.scatteredPower(
            ScatteringComponent.REFLECTION_DIFFUSE | ScatteringComponent.REFLECTION_GLOSSY,
            coordinate,
        );
        expectColor(expected.getR() + 0.25, expected.getG() + 0.25, expected.getB() + 0.25, withGlossy);
        expectColor(0.0, 0.0, 0.0, material.scatteredPower(ScatteringComponent.REFLECTION_DIFFUSE, null));
    });

    it("EmittanceAndDeepCopy", () => {
        const light = new PhongMaterial(
            "lamp",
            null,
            null,
            new PhongLobe(new ColorRgb(10.0, 9.0, 8.0), new ColorRgb(0.0, 0.0, 0.0), 0.0),
            null,
            false,
        );
        expectColor(10.0, 9.0, 8.0, light.emittance(ScatteringComponent.DIFFUSE));
        expectColor(0.0, 0.0, 0.0, light.scatteredPower(ScatteringComponent.REFLECTION_DIFFUSE, null));

        const copy = light.copy();
        const phongCopy = copy as PhongMaterial;
        expectNe(light.getEmission(), phongCopy.getEmission());
        expectColor(10.0, 9.0, 8.0, phongCopy.emittance(ScatteringComponent.DIFFUSE));
        expectEq("lamp", phongCopy.getName());
        expectEq(copy, phongCopy.translate(null));
    });
});
