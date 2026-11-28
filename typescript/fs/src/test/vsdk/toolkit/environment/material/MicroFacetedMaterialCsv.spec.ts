// Port of cpp/base/src/test/microfaceted_material_csv_tests.cpp
import * as fs from "node:fs";
import * as path from "node:path";
import { test } from "vitest";
import { expectDoubleEq, expectEq, expectTrue } from "../../../../../../../base/src/test/GTestAssertions.js";
import { MicroFacetedMaterial } from "@vitral/base/vsdk/toolkit/environment/material/MicroFacetedMaterial";
import { RendererConfiguration } from "@vitral/base/vsdk/toolkit/environment/material/RendererConfiguration";
import { KeyEvent } from "@vitral/base/vsdk/toolkit/gui/KeyEvent";
import { RendererConfigurationController } from "@vitral/base/vsdk/toolkit/gui/RendererConfigurationController";
import { MicrofacetCsvLoader } from "../../../../../main/vsdk/toolkit/environment/material/MicrofacetCsvLoader.js";

test("MicroFacetedMaterialCsv_ParsesNumericFieldsBeforeTemporaryStringsExpire", () => {
    // cpp mkstemp("/tmp/vitral-microfacet-XXXXXX").
    const directory = fs.mkdtempSync("/tmp/vitral-microfacet-");
    const filename = path.join(directory, "materials.csv");
    fs.writeFileSync(
        filename,
        "material_name,roughness,alpha,kd,ks,f0_r,f0_g,f0_b,eta_r,eta_g,eta_b,kappa_r,kappa_g,kappa_b,fresnel_model,ndf_model,geometry_model\n" +
            "Copper, 0.25 ,0.0625,0.125,0.875,0.926255,0.719206,0.503129,0.27,0.68,1.32,3.61,2.62,2.29,1,1,1\n" +
            "Aluminium,0.4,0.16,0.2,0.7,0.9,0.8,0.7,1.35,0.97,0.62,7.47,6.4,5.3,0,0,0\n",
        "latin1",
    );
    // Java's `new MicroFacetedMaterial(filename, name)` is MicrofacetCsvLoader.load.
    const material: MicroFacetedMaterial = MicrofacetCsvLoader.load(filename, "Copper");
    const aluminium: MicroFacetedMaterial = MicrofacetCsvLoader.load(filename, "Aluminium");
    fs.rmSync(directory, { recursive: true });
    expectDoubleEq(0.25, material.getRoughness());
    expectDoubleEq(0.0625, material.getAlpha());
    expectDoubleEq(0.125, material.getKd());
    expectDoubleEq(0.875, material.getKs());
    expectDoubleEq(0.926255, material.getFresnelF0().r());
    expectDoubleEq(0.719206, material.getFresnelF0().g());
    expectDoubleEq(0.503129, material.getFresnelF0().b());
    expectDoubleEq(0.27, material.getEta().r());
    expectDoubleEq(2.62, material.getKappa().g());
    expectEq(1, material.getFresnelModel());
    expectEq(1, material.getNdfModel());
    expectEq(1, material.getGeometryModel());
    expectDoubleEq(0.4, aluminium.getRoughness());
    expectDoubleEq(0.16, aluminium.getAlpha());
    expectDoubleEq(1.35, aluminium.getEta().r());
    expectDoubleEq(7.47, aluminium.getKappa().r());
    expectEq(0, aluminium.getFresnelModel());
});

test("RendererConfigurationF7_CyclesEveryShadingMode", () => {
    const quality = new RendererConfiguration();
    quality.setShadingType(RendererConfiguration.SHADING_TYPE_PHONG);
    const controller = new RendererConfigurationController(quality);
    const event = new KeyEvent();
    event.keycode = KeyEvent.KEY_F7;
    expectTrue(controller.processKeyPressedEvent(event));
    expectEq(RendererConfiguration.SHADING_TYPE_COOK_TERRANCE, quality.getShadingType());
    expectTrue(controller.processKeyPressedEvent(event));
    expectEq(RendererConfiguration.SHADING_TYPE_NOLIGHT, quality.getShadingType());
    expectTrue(controller.processKeyPressedEvent(event));
    expectEq(RendererConfiguration.SHADING_TYPE_FLAT, quality.getShadingType());
    expectTrue(controller.processKeyPressedEvent(event));
    expectEq(RendererConfiguration.SHADING_TYPE_GOURAUD, quality.getShadingType());
    expectTrue(controller.processKeyPressedEvent(event));
    expectEq(RendererConfiguration.SHADING_TYPE_PHONG, quality.getShadingType());
});
