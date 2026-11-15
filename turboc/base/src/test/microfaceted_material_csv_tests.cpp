#include <stdio.h>
#include "VitralTest.h"
#include "vsdk/toolkit/environment/material/MicroFacetedMaterial.h"

// C++ port note: Turbo C++ 3.0 has no mkstemp nor unistd.h; the table goes to
// a fixed file of the current folder, removed with the C library
TEST(MicroFacetedMaterialCsv, ParsesNumericFieldsBeforeTemporaryStringsExpire) {
    char filename[] = "MFCSVTST.TMP";
    FILE* out = fopen(filename, "w");
    ASSERT_NE((FILE*)NULL, out);
    fprintf(out, "material_name,roughness,alpha,kd,ks,f0_r,f0_g,f0_b,eta_r,eta_g,eta_b,kappa_r,kappa_g,kappa_b,fresnel_model,ndf_model,geometry_model\n");
    fprintf(out, "Copper, 0.25 ,0.0625,0.125,0.875,0.926255,0.719206,0.503129,0.27,0.68,1.32,3.61,2.62,2.29,1,1,1\n");
    fprintf(out, "Aluminium,0.4,0.16,0.2,0.7,0.9,0.8,0.7,1.35,0.97,0.62,7.47,6.4,5.3,0,0,0\n");
    fclose(out);
    MicroFacetedMaterial material(filename, "Copper");
    MicroFacetedMaterial aluminium(filename, "Aluminium");
    remove(filename);
    EXPECT_DOUBLE_EQ(0.25, material.getRoughness());
    EXPECT_DOUBLE_EQ(0.0625, material.getAlpha());
    EXPECT_DOUBLE_EQ(0.125, material.getKd());
    EXPECT_DOUBLE_EQ(0.875, material.getKs());
    EXPECT_DOUBLE_EQ(0.926255, material.getFresnelF0().r());
    EXPECT_DOUBLE_EQ(0.719206, material.getFresnelF0().g());
    EXPECT_DOUBLE_EQ(0.503129, material.getFresnelF0().b());
    EXPECT_DOUBLE_EQ(0.27, material.getEta().r());
    EXPECT_DOUBLE_EQ(2.62, material.getKappa().g());
    EXPECT_EQ(1, material.getFresnelModel());
    EXPECT_EQ(1, material.getNdfModel());
    EXPECT_EQ(1, material.getGeometryModel());
    EXPECT_DOUBLE_EQ(0.4, aluminium.getRoughness());
    EXPECT_DOUBLE_EQ(0.16, aluminium.getAlpha());
    EXPECT_DOUBLE_EQ(1.35, aluminium.getEta().r());
    EXPECT_DOUBLE_EQ(7.47, aluminium.getKappa().r());
    EXPECT_EQ(0, aluminium.getFresnelModel());
}

#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
#include "vsdk/toolkit/gui/KeyEvent.h"

TEST(RendererConfigurationF7, CyclesEveryShadingMode) {
    RendererConfiguration quality;
    quality.setShadingType(RendererConfiguration::SHADING_TYPE_PHONG);
    RendererConfigurationController controller(&quality);
    KeyEvent event;
    event.keycode = KeyEvent::KEY_F7;
    ASSERT_TRUE(controller.processKeyPressedEvent(event));
    EXPECT_EQ((int)(RendererConfiguration::SHADING_TYPE_COOK_TERRANCE), quality.getShadingType());
    ASSERT_TRUE(controller.processKeyPressedEvent(event));
    EXPECT_EQ((int)(RendererConfiguration::SHADING_TYPE_NOLIGHT), quality.getShadingType());
    ASSERT_TRUE(controller.processKeyPressedEvent(event));
    EXPECT_EQ((int)(RendererConfiguration::SHADING_TYPE_FLAT), quality.getShadingType());
    ASSERT_TRUE(controller.processKeyPressedEvent(event));
    EXPECT_EQ((int)(RendererConfiguration::SHADING_TYPE_GOURAUD), quality.getShadingType());
    ASSERT_TRUE(controller.processKeyPressedEvent(event));
    EXPECT_EQ((int)(RendererConfiguration::SHADING_TYPE_PHONG), quality.getShadingType());
}

VITRAL_TEST_MAIN()
