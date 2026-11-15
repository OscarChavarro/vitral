package vsdk.toolkit.environment.material;

import java.io.File;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.RendererConfigurationController;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.GTestAssertions.expectDoubleEq;
import static vsdk.toolkit.GTestAssertions.expectEq;

/**
Port of `cpp/base/src/test/microfaceted_material_csv_tests.cpp`; each test
method is named `<cpp suite>_<cpp test>`.
*/
class MicroFacetedMaterialCsvTest
{
    @Test
    void MicroFacetedMaterialCsv_ParsesNumericFieldsBeforeTemporaryStringsExpire() throws Exception
    {
        // cpp mkstemp("/tmp/vitral-microfacet-XXXXXX").
        File temporary = File.createTempFile("vitral-microfacet-", "", new File("/tmp"));
        final String filename = temporary.getPath();
        PrintStream out = new PrintStream(temporary, "ISO-8859-1");
        out.print("material_name,roughness,alpha,kd,ks,f0_r,f0_g,f0_b,eta_r,eta_g,eta_b,kappa_r,kappa_g,kappa_b,fresnel_model,ndf_model,geometry_model\n");
        out.print("Copper, 0.25 ,0.0625,0.125,0.875,0.926255,0.719206,0.503129,0.27,0.68,1.32,3.61,2.62,2.29,1,1,1\n");
        out.print("Aluminium,0.4,0.16,0.2,0.7,0.9,0.8,0.7,1.35,0.97,0.62,7.47,6.4,5.3,0,0,0\n");
        out.close();
        MicroFacetedMaterial material = new MicroFacetedMaterial(filename, "Copper");
        MicroFacetedMaterial aluminium = new MicroFacetedMaterial(filename, "Aluminium");
        temporary.delete();
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
    }

    @Test
    void RendererConfigurationF7_CyclesEveryShadingMode()
    {
        RendererConfiguration quality = new RendererConfiguration();
        quality.setShadingType(RendererConfiguration.SHADING_TYPE_PHONG);
        RendererConfigurationController controller = new RendererConfigurationController(quality);
        KeyEvent event = new KeyEvent();
        event.keycode = KeyEvent.KEY_F7;
        assertThat(controller.processKeyPressedEvent(event)).isTrue();
        expectEq(RendererConfiguration.SHADING_TYPE_COOK_TERRANCE, quality.getShadingType());
        assertThat(controller.processKeyPressedEvent(event)).isTrue();
        expectEq(RendererConfiguration.SHADING_TYPE_NOLIGHT, quality.getShadingType());
        assertThat(controller.processKeyPressedEvent(event)).isTrue();
        expectEq(RendererConfiguration.SHADING_TYPE_FLAT, quality.getShadingType());
        assertThat(controller.processKeyPressedEvent(event)).isTrue();
        expectEq(RendererConfiguration.SHADING_TYPE_GOURAUD, quality.getShadingType());
        assertThat(controller.processKeyPressedEvent(event)).isTrue();
        expectEq(RendererConfiguration.SHADING_TYPE_PHONG, quality.getShadingType());
    }
}
